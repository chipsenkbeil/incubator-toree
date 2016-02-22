package test.utils

import org.apache.toree.plugins.PluginSearcher

/**
 * Contains global internal plugin information, since it is expensive to
 * retrieve.
 */
object GlobalPluginInfo {
  // NOTE: This is being shared across tests to reduce time taken (slow)
  lazy val internal = (new PluginSearcher).internal
}
