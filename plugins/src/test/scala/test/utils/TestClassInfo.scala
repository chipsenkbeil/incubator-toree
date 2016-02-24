package test.utils

import java.io.File

import org.clapper.classutil.Modifier.Modifier
import org.clapper.classutil.{ClassInfo, FieldInfo, MethodInfo}

case class TestClassInfo(
  superClassName: String = "",
  interfaces: List[String] = Nil,
  location: File = null,
  methods: Set[MethodInfo] = Set(),
  fields: Set[FieldInfo] = Set(),
  signature: String = "",
  modifiers: Set[Modifier] = Set(),
  name: String = ""
) extends ClassInfo

