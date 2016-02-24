package integration

import org.apache.toree.plugins.{PluginManager, Plugin}
import org.apache.toree.plugins.annotations.Init
import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}

class PluginManagerSpecForIntegration extends FunSpec with Matchers
  with OneInstancePerTest
{
  private val pluginManager = new PluginManager

  describe("PluginManager") {
    it("should be able to initialize plugins with dependencies provided by other plugins") {
      val cpa = pluginManager.loadPlugin("", classOf[ConsumePluginA]).get
      val rpa = pluginManager.loadPlugin("", classOf[RegisterPluginA]).get

      val results = pluginManager.initializePlugins(Seq(cpa, rpa))

      results.flatMap(_._2).forall(_.isSuccess) should be (true)
    }

    it("should fail when plugins have circular dependencies") {
      val cp = pluginManager.loadPlugin("", classOf[CircularPlugin]).get

      val results = pluginManager.initializePlugins(Seq(cp))

      results.flatMap(_._2).forall(_.isFailure) should be (true)
    }

    it("should be able to handle non-circular dependencies within the same plugin") {
      val ncp = pluginManager.loadPlugin("", classOf[NonCircularPlugin]).get

      val results = pluginManager.initializePlugins(Seq(ncp))

      results.flatMap(_._2).forall(_.isSuccess) should be (true)
    }
  }
}

private class DepA
private class DepB

private class CircularPlugin extends Plugin {
  @Init def initMethodA(depA: DepA) = register(new DepB)
  @Init def initMethodB(depB: DepB) = register(new DepA)
}

private class NonCircularPlugin extends Plugin {
  @Init def initMethodB(depB: DepB) = {}
  @Init def initMethodA(depA: DepA) = register(new DepB)
  @Init def initMethod() = register(new DepA)
}

private class RegisterPluginA extends Plugin {
  @Init def initMethod() = register(new DepA)
}

private class ConsumePluginA extends Plugin {
  @Init def initMethod(depA: DepA) = {}
}
