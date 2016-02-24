package org.apache.toree.plugins

/**
 * Represents an error that occurs when trying to load a plugin of an unknown
 * type.
 *
 * @param name The full class name of the plugin
 */
class UnknownPluginTypeException(name: String)
  extends Throwable(s"Unknown plugin type: $name")
