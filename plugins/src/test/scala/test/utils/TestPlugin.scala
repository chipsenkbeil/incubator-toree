package test.utils

import org.apache.toree.plugins.Plugin
import org.apache.toree.plugins.annotations.{Destroy, Events, Event, Init}

/**
 * Test plugin that provides an implementation to a plugin.
 *
 * @note Exists in global space instead of nested in test classes due to the
 *       fact that Scala creates a non-nullary constructor when a class is
 *       nested.
 */
class TestPlugin extends Plugin {
  type Callback = () => Unit
  private var initCallbacks = collection.mutable.Seq[Callback]()
  private var eventCallbacks = collection.mutable.Seq[Callback]()
  private var eventsCallbacks = collection.mutable.Seq[Callback]()
  private var destroyCallbacks = collection.mutable.Seq[Callback]()

  def addInitCallback(callback: Callback) = initCallbacks :+= callback
  def addEventCallback(callback: Callback) = eventCallbacks :+= callback
  def addEventsCallback(callback: Callback) = eventsCallbacks :+= callback
  def addDestroyCallback(callback: Callback) = destroyCallbacks :+= callback

  @Init def initMethod() = initCallbacks.foreach(_())
  @Event(name = "event1") def eventMethod() = eventCallbacks.foreach(_())
  @Events(names = Array("event2", "event3")) def eventsMethod() =
    eventsCallbacks.foreach(_())
  @Destroy def destroyMethod() = destroyCallbacks.foreach(_())
}

object TestPlugin {
  val DefaultEvent = "event1"
  val DefaultEvents1 = "event2"
  val DefaultEvents2 = "event3"
}
