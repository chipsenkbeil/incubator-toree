package test.utils

import org.apache.toree.plugins.Plugin
import org.apache.toree.plugins.annotations.{Destroy, Event, Events, Init}

import RegisteringTestPlugin._

/**
 * Test plugin that registers dependencies.
 *
 * @note Exists in global space instead of nested in test classes due to the
 *       fact that Scala creates a non-nullary constructor when a class is
 *       nested.
 */
class RegisteringTestPlugin extends Plugin {
  type Callback = () => Unit
  private var initCallbacks = collection.mutable.Seq[Callback]()
  private var eventCallbacks = collection.mutable.Seq[Callback]()
  private var eventsCallbacks = collection.mutable.Seq[Callback]()
  private var destroyCallbacks = collection.mutable.Seq[Callback]()

  def addInitCallback(callback: Callback) = initCallbacks :+= callback
  def addEventCallback(callback: Callback) = eventCallbacks :+= callback
  def addEventsCallback(callback: Callback) = eventsCallbacks :+= callback
  def addDestroyCallback(callback: Callback) = destroyCallbacks :+= callback

  @Init def initMethod() = {
    register(InitDepName, TestPluginDependency(996))
    initCallbacks.foreach(_())
  }

  @Event(name = "event1") def eventMethod() = {
    register(EventDepName, TestPluginDependency(997))
    eventCallbacks.foreach(_())
  }

  @Events(names = Array("event2", "event3")) def eventsMethod() = {
    register(EventsDepName, TestPluginDependency(998))
    eventsCallbacks.foreach(_ ())
  }

  @Destroy def destroyMethod() = {
    register(DestroyDepName, TestPluginDependency(999))
    destroyCallbacks.foreach(_())
  }
}

object RegisteringTestPlugin {
  val DefaultEvent = "event1"
  val DefaultEvents1 = "event2"
  val DefaultEvents2 = "event3"
  val InitDepName = "init-dep"
  val EventDepName = "event-dep"
  val EventsDepName = "events-dep"
  val DestroyDepName = "destroy-dep"
}
