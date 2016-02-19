package org.apache.toree.plugins

import org.apache.toree.plugins.dependencies.Dependency

/** Example of using plugin manager. */
object Main extends App {
  val pm = new PluginManager
  pm.initialize()

  // Illustrate firing valid and invalid event names
  println(pm.fireEvent("test", Dependency.fromValue("value")))
  println(pm.fireEvent("test2"))

  // Allows converting values to dependencies to provide to fire event automagically
  import org.apache.toree.plugins.Implicits._

  // Bad type
  println(pm.fireEvent("test", new Object))

  // Good type
  println(pm.fireEvent("test", "hi"))

  // Bad type
  println(pm.fireEvent("test3", "chip" -> "hi"))

  // Does not like primitives
  println(pm.fireEvent("test3", "chip" -> scala.Int.box(3)))

  // Straight primitives get implicited to a tuple instead of event name/value
  println(pm.fireEvent("test4", "chip" -> true))

  // Notes from Corey:
  //
  // 1. Init/Destroy annotations over specific plugin methods because you
  //    can inherit multiple traits with init/destroy methods, so calling
  //    super is not an option
  //
  // 2. Dependency injection handles circular case as if there is no change
  //    in number of remaining dependencies between two rounds of running
  //    dependencies we mark the plugins as failed and use last failure
  //
  // 3. LineMagic/CellMagic traits implement plugin and have hidden methods
  //    that receive magic events, check that magic is correct name, and
  //    invoke an interface method like execute(code: String)
  //
  // 4. Can provide "one time" dependencies for firing an event that are not
  //    global to all other events
}

import org.apache.toree.plugins.annotations._
trait DummyPlugin extends Plugin {
  @Init def run()
  @Destroy def other()
  @Events(names = Array("")) def me()
  @Event(name = "") def you()

  @Event(name = "magic") final protected def _magicInvoke() = {}
}

class DummyPlugin2 extends DummyPlugin {
  @Init override def run(): Unit = {println("cheese")}

  @Init def run2(x: java.lang.Integer): Int = x

  @Events(names = Array("")) override def me(): Unit = ???

  @Event(name = "") override def you(): Unit = ???

  @Destroy override def other(): Unit = ???
}

class DummyPlugin3 extends DummyPlugin2 {
  @Init override def run(): Unit = {
    val xyz: AnyRef = new Object
    register(new Integer(3))
  }

  @Event(name = "test4") def pickle(@DepName(name = "chip") x: Boolean): Boolean = x
  @Event(name = "test3") def fish(@DepName(name = "chip") x: Int): Int = x
  @Event(name = "test") def potato(value: String): String = value
}
