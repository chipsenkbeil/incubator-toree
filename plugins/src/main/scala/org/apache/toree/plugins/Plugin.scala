package org.apache.toree.plugins

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

import org.apache.toree.annotations.Internal

import scala.reflect.runtime.universe.TypeTag

/**
 * Represents the generic plugin interface.
 */
trait Plugin {
  /** Plugin manager containing the plugin */
  @Internal private var _pluginManager: PluginManager = null

  /** Represents the name of the plugin. */
  final val name: String = getClass.getName

  /** Sets the plugin manager pointer for this plugin. */
  @Internal private[plugins] final def pluginManager_=(_pluginManager: PluginManager) = {
    require(this._pluginManager == null, "Plugin manager cannot be reassigned!")
    this._pluginManager = _pluginManager
  }

  /** Returns the plugin manager pointer for this plugin. */
  @Internal private[plugins] final def pluginManager = _pluginManager

  /** Represents all @init methods in the plugin. */
  @Internal private[plugins] final lazy val initMethods: Seq[Method] = {
    allMethods.filter(_.isAnnotationPresent(classOf[annotations.Init]))
  }

  /** Represents all @destroy methods in the plugin. */
  @Internal private[plugins] final lazy val destroyMethods: Seq[Method] = {
    allMethods.filter(_.isAnnotationPresent(classOf[annotations.Destroy]))
  }

  /** Represents all @event methods in the plugin. */
  @Internal private[plugins] final lazy val eventMethods: Seq[Method] = {
    allMethods.filter(_.isAnnotationPresent(classOf[annotations.Event]))
  }

  /** Represents all @events methods in the plugin. */
  @Internal private[plugins] final lazy val eventsMethods: Seq[Method] = {
    allMethods.filter(_.isAnnotationPresent(classOf[annotations.Events]))
  }

  /** Represents all public/protected methods contained by this plugin. */
  private final lazy val allMethods: Seq[Method] = getClass.getMethods

  /** Represents mapping of event names to associated plugin methods. */
  @Internal private[plugins] final lazy val eventMethodMap: Map[String, Seq[Method]] = {
    import scala.collection.JavaConverters._
    val _eventMethodMap = new ConcurrentHashMap[String, Seq[Method]]().asScala
    val allEventMethods = (eventMethods ++ eventsMethods).distinct

    def add(name: String, m: Method) = _eventMethodMap.put(
      name,
      _eventMethodMap.getOrElse(name, Nil) :+ m
    )

    // Find all methods that are listening for at least one of the provided
    // events
    allEventMethods.foreach(m => {
      val eventNames =
        Option(m.getAnnotation(classOf[annotations.Event]))
          .map(_.name()).map(Seq(_)).getOrElse(Nil) ++
        Option(m.getAnnotation(classOf[annotations.Events])).map(_.names())
          .map(_.toSeq).getOrElse(Nil)

      eventNames.foreach(add(_: String, m))
    })

    _eventMethodMap.toMap
  }

  /**
   * Registers a new dependency to be associated with this plugin.
   *
   * @param value The value of the dependency
   * @tparam T The dependency's type
   */
  protected def register[T <: AnyRef : TypeTag](value: T): Unit = {
    register(java.util.UUID.randomUUID().toString, value)
  }

  /**
   * Registers a new dependency to be associated with this plugin.
   *
   * @param name The name of the dependency
   * @param value The value of the dependency
   * @param typeTag The type information for the dependency
   * @tparam T The dependency's type
   */
  protected def register[T <: AnyRef](name: String, value: T)(implicit typeTag: TypeTag[T]): Unit = {
    assert(_pluginManager != null, "Internal plugin manager reference invalid!")
    _pluginManager.dependencyManager.add(name, value)
  }
}
