package org.apache.toree.plugins

import java.io.File

import org.apache.toree.plugins.types.Plugin
import org.clapper.classutil.{ClassInfo, ClassFinder}

import scala.annotation.tailrec

/**
 * Represents the search utility for locating plugin classes.
 */
class PluginSearcher {
  /** Contains all internal plugins for the system. */
  lazy val internal: Seq[ClassInfo] = findPluginClasses(ClassFinder()).toSeq

  /**
   * Searches in the provided paths (jars/zips/directories) for plugin classes.
   *
   * @param paths The paths to search through
   *
   * @return An iterator over plugin class information
   */
  def search(paths: Seq[File]): Iterator[ClassInfo] = {
    findPluginClasses(ClassFinder(paths))
  }

  /**
   * Searches for classes implementing in the plugin interface, directly or
   * indirectly.
   *
   * @param classFinder The class finder from which to retrieve class information
   *
   * @return An iterator over plugin class information
   */
  private def findPluginClasses(classFinder: ClassFinder): Iterator[ClassInfo] = {
    val stream = classFinder.getClasses()
    val classMap = ClassFinder.classInfoMap(stream.toIterator)
    concreteSubclasses(classOf[Plugin].getName, classMap)
  }

  /** Patched search that also traverses interfaces. */
  private def concreteSubclasses(ancestor: String, classes: Map[String, ClassInfo]): Iterator[ClassInfo] = {
    @tailrec def classMatches(ancestorClassInfo: ClassInfo, classesToCheck: Seq[ClassInfo]): Boolean = {
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

    // Find the ancestor class
    classes.get(ancestor) match {
      case None     =>
        Iterator.empty
      case Some(ci) =>
        classes.values.toIterator
          .filter(_.isConcrete)
          .filter(c => classMatches(ci, Seq(c)))
    }
  }
}
