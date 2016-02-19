package org.apache.toree.plugins.dependencies

import scala.reflect.runtime.universe.Type

/**
 * Represents a dependency.
 *
 * @param name The name of the dependency
 * @param `type` The type of the dependency
 * @param value The value of the dependency
 */
case class Dependency[T <: AnyRef](
  name: String,
  `type`: Type,
  value: T
) {
  /**
   * Returns the Java class representation of this dependency's type.
   *
   * @param classLoader The class loader to use when acquiring the Java class
   *
   * @return The Java class instance
   */
  def typeClass(classLoader: ClassLoader): Class[_] = {
    import scala.reflect.runtime.universe._
    val m = runtimeMirror(classLoader)
    m.runtimeClass(`type`.typeSymbol.asClass)
  }

  /** Represents the class for the dependency's value. */
  val valueClass = value.getClass
}
