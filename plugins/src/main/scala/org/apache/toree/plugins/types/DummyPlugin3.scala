package org.apache.toree.plugins.types

import org.apache.toree.plugins.annotations.{Event, Init}

/**
 * Created by senkwich on 2/17/16.
 */
class DummyPlugin3 extends DummyPlugin2 {
  @Init override
  def run(): Unit = {
    val xyz: AnyRef = new Object
    register(new Integer(3))
  }

  @Event(name = "test") def potato(): Int = 3
}
