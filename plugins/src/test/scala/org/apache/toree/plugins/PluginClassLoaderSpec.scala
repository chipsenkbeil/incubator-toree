package org.apache.toree.plugins

import java.io.File

import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}

class PluginClassLoaderSpec extends FunSpec with Matchers
  with OneInstancePerTest
{
  describe("PluginClassLoader") {
    describe("#addURL") {
      it("should add the url if not already in the loader") {
        val expected = Seq(new File("/some/file").toURI.toURL)

        val pluginClassLoader = new PluginClassLoader(Nil, null)

        // Will add for first time
        expected.foreach(pluginClassLoader.addURL)

        val actual = pluginClassLoader.getURLs

        actual should contain theSameElementsAs (expected)
      }

      it("should not add the url if already in the loader") {
        val expected = Seq(new File("/some/file").toURI.toURL)

        val pluginClassLoader = new PluginClassLoader(expected, null)

        // Will not add again
        expected.foreach(pluginClassLoader.addURL)

        val actual = pluginClassLoader.getURLs

        actual should contain theSameElementsAs (expected)
      }
    }
  }
}
