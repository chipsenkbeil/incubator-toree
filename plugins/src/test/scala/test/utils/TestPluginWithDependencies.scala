package test.utils

import org.apache.toree.plugins.Plugin
import org.apache.toree.plugins.annotations.{Destroy, Event, Events, Init}

/**
 * Test plugin that provides an implementation to a plugin.
 *
 * @note Exists in global space instead of nested in test classes due to the
 *       fact that Scala creates a non-nullary constructor when a class is
 *       nested.
 */
class TestPluginWithDependencies extends Plugin {
  type Callback = (TestPluginDependency) => Unit
  private var initCallbacks = collection.mutable.Seq[Callback]()
  private var eventCallbacks = collection.mutable.Seq[Callback]()
  private var eventsCallbacks = collection.mutable.Seq[Callback]()
  private var destroyCallbacks = collection.mutable.Seq[Callback]()

  def addInitCallback(callback: Callback) = initCallbacks :+= callback
  def addEventCallback(callback: Callback) = eventCallbacks :+= callback
  def addEventsCallback(callback: Callback) = eventsCallbacks :+= callback
  def addDestroyCallback(callback: Callback) = destroyCallbacks :+= callback

  @Init def initMethod(d: TestPluginDependency) = initCallbacks.foreach(_(d))

  @Event(name = "event1")
  def eventMethod(d: TestPluginDependency) = eventCallbacks.foreach(_(d))

  @Events(names = Array("event2", "event3"))
  def eventsMethod(d: TestPluginDependency) = eventsCallbacks.foreach(_(d))

  @Destroy def destroyMethod(d: TestPluginDependency) =
    destroyCallbacks.foreach(_(d))
}

case class TestPluginDependency(value: Int)

object TestPluginWithDependencies {
  val DefaultEvent = "event1"
  val DefaultEvents1 = "event2"
  val DefaultEvents2 = "event3"
}
