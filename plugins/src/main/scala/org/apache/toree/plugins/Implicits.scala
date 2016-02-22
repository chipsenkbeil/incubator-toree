package org.apache.toree.plugins

import org.apache.toree.plugins.dependencies.Dependency

import scala.reflect.runtime.universe.TypeTag

/**
 * Contains plugin implicit methods.
 */
object Implicits {
  import scala.language.implicitConversions

  implicit def $dep[T <: AnyRef : TypeTag](bundle: (String, T)): Dependency[T] =
    Dependency.fromValueWithName(bundle._1, bundle._2)

  implicit def $dep[T <: AnyRef : TypeTag](value: T): Dependency[T] =
    Dependency.fromValue(value)
}
