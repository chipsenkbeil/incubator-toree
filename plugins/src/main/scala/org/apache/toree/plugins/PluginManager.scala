package org.apache.toree.plugins

import java.io.File
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import org.apache.toree.plugins.dependencies._
import org.apache.toree.plugins.annotations._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
 * Represents a manager of plugins to be loaded/executed/unloaded.
 *
 * @param pluginClassLoader The main classloader for loading plugins
 * @param pluginSearcher The search utility to find plugin classes
 * @param dependencyManager The dependency manager for plugins
 */
class PluginManager(
  private val pluginClassLoader: PluginClassLoader =
    new PluginClassLoader(Nil, classOf[PluginManager].getClassLoader),
  private val pluginSearcher: PluginSearcher = new PluginSearcher,
  val dependencyManager: DependencyManager = new DependencyManager
) {
  /** Represents logger used by plugin manager. */
  private val logger = LoggerFactory.getLogger(this.getClass)

  /** Represents internal plugins. */
  private lazy val internalPlugins: Map[String, Class[_]] =
    pluginSearcher.internal
      .map(_.name)
      .map(n => n -> pluginClassLoader.loadClass(n))
      .toMap

  /** Represents external plugins that can be loaded/unloaded. */
  private lazy val externalPlugins: collection.mutable.Map[String, Class[_]] =
    new ConcurrentHashMap[String, Class[_]]().asScala

  /** Represents all active (loaded and created) plugins. */
  private lazy val activePlugins: collection.mutable.Map[String, Plugin] =
    new ConcurrentHashMap[String, Plugin]().asScala

  /**
   * Returns whether or not the specified plugin is active.
   *
   * @param name The fully-qualified name of the plugin class
   * @return True if actively loaded, otherwise false
   */
  def isActive(name: String): Boolean = activePlugins.contains(name)

  /**
   * Returns a new iterator over active plugins contained by this manager.
   *
   * @return The iterator of active plugins
   */
  def plugins: Iterable[Plugin] = activePlugins.values

  /**
   * Initializes the plugin manager, performing the expensive task of searching
   * for all internal plugins, creating them, and initializing them.
   */
  def initialize(): Unit = {
    initializePluginCollection(internalPlugins, DependencyManager.Empty)
  }

  /**
   * Loads and initializes plugins from the provided paths.
   *
   * @param paths The file paths from which to load new plugins
   */
  def loadPlugins(paths: File*): Unit = {
    // Add all paths to search path of our plugin class loader
    paths.map(_.toURI.toURL).foreach(pluginClassLoader.addURL)

    // Search for plugins in our new paths, then add loaded plugins to list
    // NOTE: Iterator returned from plugin searcher, so avoid building a
    //       large collection by performing all tasks together
    val newPlugins = pluginSearcher.search(paths: _*).map(ci => {
      // Add valid path to class loader
      pluginClassLoader.addURL(ci.location.toURI.toURL)

      // Load class
      val klass = pluginClassLoader.loadClass(ci.name)

      // Add to external plugin list
      externalPlugins.put(ci.name, klass)

      // Return structure for map
      ci.name -> klass
    })

    // Initialize new external plugins
    initializePluginCollection(newPlugins.toMap, DependencyManager.Empty)
  }

  /**
   * Creates and initializes a collection of plugins that may/may not have
   * dependencies on one another.
   *
   * @param plugins The map of plugins (name, class) to create and initialize
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @return True if all plugins successfully created and initialized,
   *         otherwise false
   */
  private def initializePluginCollection(
    plugins: Map[String, Class[_]],
    scopedDependencyManager: DependencyManager
  ): Boolean = {
    val pluginBundles = createPluginInstances(plugins).values.flatMap(p =>
      p.initMethods.map(m => (p, m)).map(b => (p.name, b))
    ).toSeq
    val pluginNames = pluginBundles.map(_._1)
    val results = pluginNames.zip(invokePluginMethods(
      pluginBundles.map(_._2),
      scopedDependencyManager
    ))

    // Mark success/failure
    results.groupBy(_._1).map { case (pluginName, g) =>
      val failures = g.map(_._2).flatMap(_.failed.toOption)
      val success = failures.isEmpty

      if (success) logger.debug(s"Successfully initialized plugin $pluginName!")
      else logger.warn(s"Initialization failed for plugin $pluginName!")

      // Log any specific failures for the plugin
      failures.foreach(ex => logger.error(pluginName, ex))

      success
    }.forall(_ == true)
  }

  /**
   * Initializes a plugin by invoking any init methods.
   *
   * @param name The fully-qualified class name of the plugin
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @return True if able to initialize, otherwise false
   */
  def initializePlugin(
    name: String,
    scopedDependencyManager: DependencyManager = DependencyManager.Empty
  ): Boolean = findPlugin(name).exists(p =>
    initializePlugin(p, scopedDependencyManager)
  )

  /**
   * Initializes a plugin by invoking any init methods.
   *
   * @param plugin The plugin instance to initialize
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @return True if able to initialize, otherwise false
   */
  def initializePlugin(
    plugin: Plugin,
    scopedDependencyManager: DependencyManager
  ): Boolean = {
    val results = invokePluginMethods(
      plugin,
      plugin.initMethods,
      scopedDependencyManager
    )
    val success = results.forall(_.isSuccess)

    // Report status
    val name = plugin.name
    if (success) logger.debug(s"Successfully initialized plugin $name!")
    else logger.warn(s"Failed to initialize plugin $name!")

    success
  }

  /**
   * Finds a plugin with the matching name.
   *
   * @param name The fully-qualified class name of the plugin
   * @return Some plugin if found, otherwise None
   */
  def findPlugin(name: String): Option[Plugin] = plugins.find(_.name == name)

  /**
   * Destroys a plugin by invoking any destroy methods.
   *
   * @param name The fully-qualified class name of the plugin
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @param destroyOnFailure If true, will destroy the plugin even if the
   *                         teardown methods fail
   * @return True if able to destroy, otherwise false
   */
  def destroyPlugin(
    name: String,
    scopedDependencyManager: DependencyManager = DependencyManager.Empty,
    destroyOnFailure: Boolean = true
  ): Boolean = findPlugin(name).exists(p =>
    destroyPlugin(p, scopedDependencyManager, destroyOnFailure)
  )

  /**
   * Destroys a plugin by invoking any destroy methods.
   *
   * @param plugin The plugin instance to destroy
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @param destroyOnFailure If true, will destroy the plugin even if the
   *                         teardown methods fail
   * @return True if able to destroy, otherwise false
   */
  def destroyPlugin(
    plugin: Plugin,
    scopedDependencyManager: DependencyManager,
    destroyOnFailure: Boolean
  ): Boolean = {
    val results = invokePluginMethods(
      plugin,
      plugin.destroyMethods,
      scopedDependencyManager
    )
    val success = results.forall(_.isSuccess)

    // Report status
    val name = plugin.name
    if (success) logger.debug(s"Successfully destroyed plugin $name!")
    else if (destroyOnFailure) logger.warn(
      s"Failed to invoke some teardown methods, but destroyed plugin $name!"
    ) else logger.warn(s"Failed to destroy plugin $name!")

    // If successful or forced, remove the plugin from our active list
    if (success || destroyOnFailure) activePlugins.remove(name)

    success || destroyOnFailure
  }

  /**
   * Sends an event to all plugins actively listening for that event.
   *
   * @param eventName The name of the event
   * @param scopedDependencies The dependencies to provide directly to event
   *                           handlers
   * @return The collection of results
   */
  def fireEvent(
    eventName: String,
    scopedDependencies: Dependency[_ <: AnyRef]*
  ): Seq[Try[AnyRef]] = {
    val dependencyManager = new DependencyManager
    scopedDependencies.foreach(d => dependencyManager.add(d))
    fireEvent(eventName, dependencyManager)
  }

  /**
   * Sends an event to all plugins actively listening for that event.
   *
   * @param eventName The name of the event
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @return The collection of results
   */
  def fireEvent(
    eventName: String,
    scopedDependencyManager: DependencyManager = DependencyManager.Empty
  ): Seq[Try[AnyRef]] = {
    val bundles = plugins.flatMap(p =>
      p.eventMethodMap.getOrElse(eventName, Nil).map(m => (p, m))
    ).toSeq

    invokePluginMethods(bundles, scopedDependencyManager)
  }

  /**
   * Attempts to invoke all methods against a specific plugin. This is a naive
   * implementation that continually invokes methods until either all methods
   * are successful or failures are detected (needing dependencies that other
   * methods do not provide).
   *
   * @param plugin The plugin whose methods to invoke
   * @param methods The methods of the plugin to invoke
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @return The collection of results (in same order as bundles)
   */
  private def invokePluginMethods(
    plugin: Plugin,
    methods: Seq[Method],
    scopedDependencyManager: DependencyManager
  ): Seq[Try[AnyRef]] = {
    val bundles = methods.map(m => (plugin, m))
    val results = invokePluginMethods(bundles, scopedDependencyManager).zip(
      methods.map(_.getName)
    )

    val name = plugin.name

    // Report status for each invocation
    results.foreach { case (result, methodName) => result match {
      case Success(r) =>
        logger.debug(s"Successfully invoked $name.$methodName, returning $r")
      case Failure(ex) =>
        logger.error(s"Failed to invoke $name.$methodName", ex)
    } }

    results.map(_._1)
  }

  /**
   * Attempts to invoke all bundled (plugin, method) tuples. This is a naive
   * implementation that continually invokes bundles until either all bundles
   * are complete or failures are detected (needing dependencies that other
   * bundles do not provide).
   *
   * @param bundles The collection of tupled (plugin, method) entities to invoke
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @return The collection of results (in same order as bundles)
   */
  private def invokePluginMethods(
    bundles: Seq[(Plugin, Method)],
    scopedDependencyManager: DependencyManager
  ): Seq[Try[AnyRef]] = {
    // Continue trying to invoke plugins until we finish them all or
    // we reach a state where no plugin can be completed
    val completedBundles = Array.ofDim[Try[AnyRef]](bundles.size)
    @volatile var remainingBundles = bundles.zipWithIndex
    @volatile var done = false
    while (!done) {
      // Process all bundles, adding any successful to completed and leaving
      // any failures to be processed again
      val newRemainingBundles = remainingBundles.map { case (b, i) =>
        val result = Try(invokePluginMethod(b._1, b._2, scopedDependencyManager))
        if (result.isSuccess) completedBundles.update(i, result)
        (b, i, result)
      }.filter(_._3.isFailure)

      // If no change detected, we have failed to process all bundles
      if (remainingBundles.size == newRemainingBundles.size) {
        // Place last failure for each bundle in our completed list
        newRemainingBundles.foreach { case (_, i, r) =>
          completedBundles.update(i, r)
        }
        done = true
      } else {
        // Update remaining bundles to past failures
        remainingBundles = newRemainingBundles.map(t => (t._1, t._2))
        done = remainingBundles.isEmpty
      }
    }
    completedBundles
  }

  /**
   * Invokes the provided method against the provided plugin by loading all
   * needed dependencies and providing them as arguments to the method.
   *
   * @param plugin The plugin whose method to invoke
   * @param method The method of the plugin to invoke
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @return The result from invoking the plugin
   */
  private def invokePluginMethod(
    plugin: Plugin,
    method: Method,
    scopedDependencyManager: DependencyManager
  ): AnyRef = {
    // Get dependency info (if has specific name or just use class)
    val depInfo = method.getParameterAnnotations
      .zip(method.getParameterTypes)
      .map { case (annotations, parameterType) =>
        (annotations.collect {
          case dn: DepName => dn
        }.lastOption.map(_.name()), parameterType)
      }

    // Load dependencies for plugin method
    val dependencies = depInfo.map { case (name, c) => name match {
      case Some(n) =>
        val dep = scopedDependencyManager.find(n).orElse(
          dependencyManager.find(n)
        )
        if (dep.isEmpty) throw new DepNameNotFoundException(n)

        // Verify found dep has acceptable class
        val depClass: Class[_] = dep.get.valueClass
        if (!c.isAssignableFrom(depClass))
          throw new DepUnexpectedClassException(n, c, depClass)

        dep.get
      case None =>
        val scopedDeps = scopedDependencyManager.findByValueClass(c)
        val deps =
          if (scopedDeps.nonEmpty) scopedDeps
          else dependencyManager.findByValueClass(c)
        if (deps.isEmpty) throw new DepClassNotFoundException(c)
        deps.last
    } }

    // Validate arguments
    val arguments: Seq[AnyRef] = dependencies.map(_.value.asInstanceOf[AnyRef])

    // Invoke plugin method
    method.invoke(plugin, arguments: _*)
  }

  /**
   * Creates new instances of provided plugins if NOT already in the active
   * plugin list.
   *
   * @param pluginClasses The map of class names and class information
   * @return Map containing all newly-created plugins (any plugin skipped due
   *         to being active is not included)
   */
  private def createPluginInstances(
    pluginClasses: Map[String, Class[_]]
  ): Map[String, Plugin] = {
    pluginClasses.filter { case (name, _) =>
      val active = isActive(name)
      if (active) logger.warn(s"Skipping $name as already actively loaded!")
      !active
    }.flatMap { case (name, klass) =>
      logger.debug(s"Loading $name as plugin")

      // Assume that each plugin has an empty constructor
      val tryInstance = Try(klass.newInstance())

      // Log failures
      tryInstance.failed.foreach(ex => logger.error(s"Failed to load $name", ex))

      // Attempt to cast as plugin type to add to active plugins
      tryInstance.map {
        case p: Plugin  =>
          p.pluginManager_=(this)
          activePlugins.put(name, p)
          Some((name, p))
        case x          =>
          logger.warn(s"Unknown plugin type '${x.getClass.getName}', ignoring!")
          None
      }.getOrElse(None)
    }
  }
}
