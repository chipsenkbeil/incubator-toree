package org.apache.toree.plugins

import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}

class PluginSpec extends FunSpec with Matchers with OneInstancePerTest {
  describe("Plugin") {
    describe("#name") {
      it("should be the name of the class implementing the plugin") {
        fail()
      }
    }

    describe("#initMethods") {
      it("should return any method annotated with @init including from ancestors") {
        fail()
      }
    }

    describe("#destroyMethods") {
      it("should return any method annotated with @destroy including from ancestors") {
        fail()
      }
    }

    describe("#eventMethods") {
      it("should return any method annotated with @event including from ancestors") {
        fail()
      }
    }

    describe("#eventsMethods") {
      it("should return any method annotated with @events including from ancestors") {
        fail()
      }
    }

    describe("#eventMethodMap") {
      it("should return a map of event names to their annotated methods") {
        fail()
      }
    }

    describe("#invoke") {
      it("should invoke the specified method with the provided arguments") {
        fail()
      }

      it("should support invoking protected methods") {
        fail()
      }

      it("should support invoking private methods") {
        fail()
      }

      it("should throw an exception if the arguments do not match") {
        fail()
      }

      it("should throw an exception if no method with the specified name is found") {
        fail()
      }

      it("should throw an exception if the name exists but the parameter types are wrong") {
        fail()
      }
    }

    describe("#register") {
      it("should not allow registering a dependency if the plugin manager is not set") {
        fail()
      }

      it("should create a new name for the dependency if not specified") {
        fail()
      }

      it("should add the dependency using the provided name") {
        fail()
      }
    }
  }
}
