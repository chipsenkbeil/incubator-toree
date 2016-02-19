package org.apache.toree.plugins.types

import org.apache.toree.plugins.annotations._

/**
 * Created by senkwich on 2/17/16.
 */
class DummyPlugin2 extends DummyPlugin {
  @Init
  override def run(): Unit = {println("cheese")}

  @Init def run2(x: java.lang.Integer): Int = x

  @Events(names = Array(""))
  override def me(): Unit = ???

  @Event(name = "")
  override def you(): Unit = ???

  @Destroy
  override def other(): Unit = ???
}
