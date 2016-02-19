package org.apache.toree.plugins.dependencies

/** Represents a generic dependency exception. */
sealed class DependencyException(message: String) extends Throwable(message)

/**
 * Represents a dependency exception where the dependency with the desired
 * name was not found.
 *
 * @param name The name of the missing dependency
 */
class DepNameNotFoundException(name: String) extends DependencyException(
  s"Dependency with name '$name' not found!"
)

/**
 * Represents a dependency exception where the dependency with the desired
 * class type was not found.
 *
 * @param klass The class from which the dependency extends
 */
class DepClassNotFoundException(klass: Class[_]) extends DependencyException(
  s"Dependency extending class '${klass.getName}' not found!"
)

/**
 * Represents a dependency exception where the dependency with the desired
 * name was found but the class did not match.
 *
 * @param name The name of the dependency
 * @param expected The desired dependency class
 * @param actual The class from which the found dependency extends
 */
class DepUnexpectedClassException(name: String, expected: Class[_], actual: Class[_]) extends DependencyException(
  s"Dependency found called '$name', but expected '$expected' and had class '$actual'!"
)