package org.apache.toree.plugins

import org.apache.toree.plugins.dependencies.Dependency
import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}

class ImplicitsSpec extends FunSpec with Matchers with OneInstancePerTest {
  describe("Implicits") {
    describe("#$dep") {
      it("should convert values to dependencies with generated names") {
        import scala.reflect.runtime.universe._
        import org.apache.toree.plugins.Implicits._

        val value = new Object

        val d: Dependency[_] = value

        d.name should not be (empty)
        d.`type` should be (typeOf[Object])
        d.value should be (value)
      }

      it("should convert tuples of (string, value) to dependencies with the specified names") {
        import scala.reflect.runtime.universe._
        import org.apache.toree.plugins.Implicits._

        val name = "some name"
        val value = new Object

        val d: Dependency[_] = name -> value

        d.name should be (name)
        d.`type` should be (typeOf[Object])
        d.value should be (value)
      }
    }
  }
}
