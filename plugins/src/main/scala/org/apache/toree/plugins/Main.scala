package org.apache.toree.plugins

/**
 * Created by senkwich on 2/17/16.
 */
object Main extends App {
  val pm = new PluginManager
  pm.initialize()

  println(pm.fireEvent("test"))
  println(pm.fireEvent("test2"))
  println(pm.fireEvent("test"))

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
