package org.apache.toree.plugins.types

import org.apache.toree.plugins.annotations._

/**
 * Created by senkwich on 2/17/16.
 */
trait DummyPlugin extends Plugin {
  @Init def run()
  @Destroy def other()
  @Events(names = Array("")) def me()
  @Event(name = "") def you()

  @Event(name = "magic") final protected def _magicInvoke() = {}
}
