package test.utils

/**
 * Class that is not a plugin, but has an empty constructor.
 *
 * @note Exists in global space instead of nested in test classes due to the
 *       fact that Scala creates a non-nullary constructor when a class is
 *       nested.
 */
class NotAPlugin
