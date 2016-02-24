package org.apache.toree.plugins

import java.io.File
import org.clapper.classutil.{ClassInfo, ClassFinder}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.Try

/**
 * Represents the search utility for locating plugin classes.
 */
class PluginSearcher {
  /** Represents logger used by plugin searcher. */
  private val logger = LoggerFactory.getLogger(this.getClass)

  /** Contains all internal plugins for the system. */
  lazy val internal: Seq[ClassInfo] = findPluginClasses(newClassFinder()).toSeq

  /**
   * Searches in the provided paths (jars/zips/directories) for plugin classes.
   *
   * @param paths The paths to search through
   * @return An iterator over plugin class information
   */
  def search(paths: File*): Iterator[ClassInfo] = {
    findPluginClasses(newClassFinder(paths))
  }

  /**
   * Creates a new class finder using the JVM classpath.
   *
   * @return The new class finder
   */
  protected def newClassFinder(): ClassFinder = ClassFinder()

  /**
   * Creates a new class finder for the given paths.
   *
   * @param paths The paths within which to search for classes
   *
   * @return The new class finder
   */
  protected def newClassFinder(paths: Seq[File]): ClassFinder = ClassFinder(paths)

  /**
   * Searches for classes implementing in the plugin interface, directly or
   * indirectly.
   *
   * @param classFinder The class finder from which to retrieve class information
   * @return An iterator over plugin class information
   */
  private def findPluginClasses(classFinder: ClassFinder): Iterator[ClassInfo] = {
    val tryStream = Try(classFinder.getClasses())
    tryStream.failed.foreach(logger.error(
      s"Failed to find plugins from classpath: ${classFinder.classpath.mkString(",")}",
      _: Throwable
    ))
    val stream = tryStream.getOrElse(Stream.empty)
    val classMap = ClassFinder.classInfoMap(stream.toIterator)
    concreteSubclasses(classOf[Plugin].getName, classMap)
  }

  /** Patched search that also traverses interfaces. */
  private def concreteSubclasses(
    ancestor: String,
    classes: Map[String, ClassInfo]
  ): Iterator[ClassInfo] = {
    @tailrec def classMatches(
      ancestorClassInfo: ClassInfo,
      classesToCheck: Seq[ClassInfo]
    ): Boolean = {
      if (classesToCheck.isEmpty) false
      else if (classesToCheck.exists(_.name == ancestorClassInfo.name)) true
      else if (classesToCheck.exists(_.superClassName == ancestorClassInfo.name)) true
      else if (classesToCheck.exists(_ implements ancestorClassInfo.name)) true
      else {
        val superClasses = classesToCheck.map(_.superClassName).flatMap(classes.get)
        val interfaces = classesToCheck.flatMap(_.interfaces).flatMap(classes.get)
        classMatches(ancestorClassInfo, superClasses ++ interfaces)
      }
    }

    classes.get(ancestor).map(ci => {
      classes.values.toIterator
        .filter(_.isConcrete)
        .filter(c => classMatches(ci, Seq(c)))
    }).getOrElse(Iterator.empty)
  }
}
