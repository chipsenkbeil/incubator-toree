package org.apache.toree.plugins.dependencies

import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._
import scala.reflect.runtime.universe.{Type, TypeTag}

/**
 * Contains helpers and contants associated with the dependency manager.
 */
object DependencyManager {
  /** Represents an empty dependency manager. */
  val Empty = new DependencyManager {
    // Prevent adding dependencies
    override def add[T <: AnyRef](dependency: Dependency[T]): Unit = {}
  }
}

/**
 * Represents manager of dependencies by name and type.
 */
class DependencyManager {
  private val dependencies: collection.mutable.Map[String, Dependency[_ <: AnyRef]] =
    new ConcurrentHashMap[String, Dependency[_ <: AnyRef]]().asScala

  /**
   * Returns a map of dependency names to values.
   *
   * @return The map of dependency names and values
   */
  def toMap: Map[String, Any] =
    dependencies.values.map(d => d.name -> d.value).toMap

  /**
   * Returns a sequence of dependencies contained by this manager.
   *
   * @return The sequence of dependency objects
   */
  def toSeq: Seq[Dependency[_ <: AnyRef]] = dependencies.values.toSeq

  /**
   * Adds a new dependency to the manager.
   *
   * @param value The value of the dependency
   * @tparam T The dependency's type
   */
  def add[T <: AnyRef : TypeTag](value: T): Unit =
    add(java.util.UUID.randomUUID().toString, value)

  /**
   * Adds a new dependency to the manager.
   *
   * @param name The name of the dependency
   * @param value The value of the dependency
   * @param typeTag The type information collected about the dependency
   * @tparam T The dependency's type
   */
  def add[T <: AnyRef](name: String, value: T)(implicit typeTag: TypeTag[T]): Unit =
    add(Dependency(name, typeTag.tpe, value))

  /**
   * Adds a new dependency to the manager.
   *
   * @param dependency The dependency construct containing all relevant info
   * @tparam T The dependency's type
   */
  def add[T <: AnyRef](dependency: Dependency[T]): Unit = {
    require(!dependencies.contains(dependency.name))
    dependencies.put(dependency.name, dependency)
  }

  /**
   * Finds a dependency with the matching name in this manager.
   *
   * @param name The name of the dependency
   *
   * @return Some dependency if found, otherwise None
   */
  def find(name: String): Option[Dependency[_]] = dependencies.get(name)

  /**
   * Finds all dependencies whose type matches or is a subclass of the
   * specified type.
   *
   * @param `type` The type to match against each dependency's type
   *
   * @return The collection of matching dependencies
   */
  def findByType(`type`: Type): Seq[Dependency[_]] =
    dependencies.values.filter(_.`type` <:< `type`).toSeq

  /**
   * Finds all dependencies whose type class representation matches or is a
   * subclass of the specified class.
   *
   * @param klass The class to match against the dependency's
   *              type class representation
   *
   * @return The collection of matching dependencies
   */
  def findByTypeClass(klass: Class[_]): Seq[Dependency[_]] =
    dependencies.values.filter(d =>
      klass.isAssignableFrom(d.typeClass(klass.getClassLoader))
    ).toSeq

  /**
   * Finds all dependencies whose value class representation matches or is a
   * subclass of the specified class.
   *
   * @param klass The class to match against the dependency's
   *              value class representation
   *
   * @return The collection of matching dependencies
   */
  def findByValueClass(klass: Class[_]): Seq[Dependency[_]] =
    dependencies.values.filter(d => klass.isAssignableFrom(d.valueClass)).toSeq

  /**
   * Removes the dependency with the specified name.
   *
   * @param name The name of the dependency
   *
   * @return Some dependency if removed, otherwise None
   */
  def remove(name: String): Option[Dependency[_]] =
    dependencies.remove(name)

  /**
   * Removes all dependencies whose type matches or is a subclass of the
   * specified type.
   *
   * @param `type` The type to match against each dependency's type
   *
   * @return The collection of matching dependencies
   */
  def removeByType(`type`: Type): Seq[Dependency[_]] =
    findByType(`type`).map(_.name).flatMap(remove)

  /**
   * Removes all dependencies whose type class representation matches or is a
   * subclass of the specified class.
   *
   * @param klass The class to match against the dependency's
   *              type class representation
   *
   * @return The collection of matching dependencies
   */
  def removeByTypeClass(klass: Class[_]): Seq[Dependency[_]] =
    findByTypeClass(klass).map(_.name).flatMap(remove)

  /**
   * Removes all dependencies whose value class representation matches or is a
   * subclass of the specified class.
   *
   * @param klass The class to match against the dependency's
   *              value class representation
   *
   * @return The collection of matching dependencies
   */
  def removeByValueClass(klass: Class[_]): Seq[Dependency[_]] =
    findByValueClass(klass).map(_.name).flatMap(remove)
}
