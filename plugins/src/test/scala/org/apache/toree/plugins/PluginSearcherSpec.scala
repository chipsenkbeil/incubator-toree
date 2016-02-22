package org.apache.toree.plugins

import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}
import test.utils.GlobalPluginInfo

class PluginSearcherSpec extends FunSpec with Matchers with OneInstancePerTest {
  private class DirectTestPlugin extends Plugin
  private class IndirectTestPlugin extends DirectTestPlugin
  private trait TraitPlugin extends Plugin
  private abstract class AbstractPlugin extends Plugin

  describe("PluginSearcher") {
    describe("#internal") {
      it("should find any plugins directly extending the Plugin class") {
        val expected = classOf[DirectTestPlugin].getName

        val actual = GlobalPluginInfo.internal.map(_.name)

        actual should contain (expected)
      }

      it("should find any plugins indirectly extending the Plugin class") {
        val expected = classOf[IndirectTestPlugin].getName

        val actual = GlobalPluginInfo.internal.map(_.name)

        actual should contain (expected)
      }

      it("should not include any traits or abstract classes") {
        val expected = Seq(
          classOf[TraitPlugin].getName,
          classOf[AbstractPlugin].getName
        )

        val actual = GlobalPluginInfo.internal.map(_.name)

        actual should not contain atLeastOneOf (expected.head, expected.tail)
      }
    }

    describe("#search") {
      // TODO: Provide a resource jar to test
      ignore("should find plugins from a jar") {}

      // TODO: Provide a resource zip to test
      ignore("should find plugins from a zip") {}

      // TODO: Provide a resource directory to test
      ignore("should find plugins from a directory") {}
    }
  }
}
