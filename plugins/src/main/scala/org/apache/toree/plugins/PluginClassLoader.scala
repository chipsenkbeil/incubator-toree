package org.apache.toree.plugins

import java.net.{URLClassLoader, URL}

/**
 * Represents a class loader used to manage classes used as plugins.
 *
 * @param urls The initial collection of URLs pointing to paths to load
 *             plugin classes
 * @param parentLoader The parent loader to use as a fallback to load plugin
 *                     classes
 */
class PluginClassLoader(
  private val urls: Seq[URL],
  private val parentLoader: ClassLoader
) extends URLClassLoader(urls.toArray, parentLoader) {
  /**
   * Adds a new URL to be included when loading plugins. If the url is already
   * in the class loader, it is ignored.
   *
   * @param url The url pointing to the new plugin classes to load
   */
  override def addURL(url: URL): Unit = {
    if (!this.getURLs.contains(url)) super.addURL(url)
  }
}
