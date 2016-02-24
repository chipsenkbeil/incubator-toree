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
   *
   * @return The collection of loaded plugins
   */
  def initialize(): Seq[Plugin] = {
    val newPlugins = internalPlugins.flatMap(t =>
      loadPlugin(t._1, t._2).toOption
    ).toSeq
    initializePlugins(newPlugins, DependencyManager.Empty)
    newPlugins
  }

  /**
   * Loads (but does not initialize) plugins from the provided paths.
   *
   * @param paths The file paths from which to load new plugins
   *
   * @return The collection of loaded plugins
   */
  def loadPlugins(paths: File*): Seq[Plugin] = {
    // Search for plugins in our new paths, then add loaded plugins to list
    // NOTE: Iterator returned from plugin searcher, so avoid building a
    //       large collection by performing all tasks together
    @volatile var newPlugins = collection.mutable.Seq[Plugin]()
    pluginSearcher.search(paths: _*).foreach(ci => {
      // Add valid path to class loader
      pluginClassLoader.addURL(ci.location.toURI.toURL)

      // Load class
      val klass = pluginClassLoader.loadClass(ci.name)

      // Add to external plugin list
      externalPlugins.put(ci.name, klass)

      // Load the plugin using the given name and class
      loadPlugin(ci.name, klass).foreach(newPlugins :+= _)
    })
    newPlugins
  }

  /**
   * Loads the plugin using the specified name.
   *
   * @param name The name of the plugin
   * @param klass The class representing the plugin
   * @return The new plugin instance if no plugin with the specified name
   *         exists, otherwise the plugin instance with the name
   */
  def loadPlugin(name: String, klass: Class[_]): Try[Plugin] = {
    if (isActive(name)) {
      logger.warn(s"Skipping $name as already actively loaded!")
      Success(activePlugins(name))
    } else {
      logger.debug(s"Loading $name as plugin")

      // Assume that each plugin has an empty constructor
      val tryInstance = Try(klass.newInstance())

      // Log failures
      tryInstance.failed.foreach(ex =>
        logger.error(s"Failed to load plugin $name", ex))

      // Attempt to cast as plugin type to add to active plugins
      tryInstance.transform({
        case p: Plugin  =>
          p.pluginManager_=(this)
          activePlugins.put(p.name, p)
          Success(p)
        case x          =>
          val name = x.getClass.getName
          logger.warn(s"Unknown plugin type '$name', ignoring!")
          Failure(new UnknownPluginTypeException(name))
      }, f => Failure(f))
    }
  }

  /**
   * Initializes a collection of plugins that may/may not have
   * dependencies on one another.
   *
   * @param plugins The collection of plugins to initialize
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @return Map of plugin names to results of each initialize callback invoked
   *         for that plugin
   */
  def initializePlugins(
    plugins: Seq[Plugin],
    scopedDependencyManager: DependencyManager = DependencyManager.Empty
  ): Map[String, Seq[Try[AnyRef]]] = {
    val pluginBundles = plugins.flatMap(p =>
      p.initMethods.map(m => (p, m)).map(b => (p.name, b))
    )
    val pluginNames = pluginBundles.map(_._1)
    val results = pluginNames.zip(invokePluginMethods(
      pluginBundles.map(_._2),
      scopedDependencyManager
    ))

    // Mark success/failure
    val groupedResults = results.groupBy(_._1)
    groupedResults.foreach { case (pluginName, g) =>
      val failures = g.map(_._2).flatMap(_.failed.toOption)
      val success = failures.isEmpty

      if (success) logger.debug(s"Successfully initialized plugin $pluginName!")
      else logger.warn(s"Initialization failed for plugin $pluginName!")

      // Log any specific failures for the plugin
      failures.foreach(ex => logger.error(pluginName, ex))
    }

    groupedResults.mapValues(_.map(_._2))
  }

  /**
   * Destroys a collection of plugins that may/may not have
   * dependencies on one another.
   *
   * @param plugins The collection of plugins to destroy
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @param destroyOnFailure If true, destroys the plugin even if its destroy
   *                         callback fails
   * @return Map of plugin names to results of each destroy callback invoked
   *         for that plugin
   */
  def destroyPlugins(
    plugins: Seq[Plugin],
    scopedDependencyManager: DependencyManager = DependencyManager.Empty,
    destroyOnFailure: Boolean = true
  ): Map[String, Seq[Try[AnyRef]]] = {
    val pluginBundles = plugins.flatMap(p =>
      p.destroyMethods.map(m => (p, m)).map(b => (p.name, b))
    )
    val pluginNames = pluginBundles.map(_._1)
    val results = pluginNames.zip(invokePluginMethods(
      pluginBundles.map(_._2),
      scopedDependencyManager
    ))

    // Perform check to remove destroyed plugins
    val groupedResults = results.groupBy(_._1)
    groupedResults.foreach { case (pluginName, g) =>
      val failures = g.map(_._2).flatMap(_.failed.toOption)
      val success = failures.isEmpty

      if (success) logger.debug(s"Successfully destroyed plugin $pluginName!")
      else if (destroyOnFailure) logger.debug(
        s"Failed to invoke some teardown methods, but destroyed plugin $pluginName!"
      )
      else logger.warn(s"Failed to destroy plugin $pluginName!")

      // If successful or forced, remove the plugin from our active list
      if (success || destroyOnFailure) activePlugins.remove(pluginName)

      // Log any specific failures for the plugin
      failures.foreach(ex => logger.error(pluginName, ex))
    }

    groupedResults.mapValues(_.map(_._2))
  }

  /**
   * Finds a plugin with the matching name.
   *
   * @param name The fully-qualified class name of the plugin
   * @return Some plugin if found, otherwise None
   */
  def findPlugin(name: String): Option[Plugin] = plugins.find(_.name == name)

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
}
