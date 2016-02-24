package test.utils

import org.clapper.classutil.FieldInfo
import org.clapper.classutil.Modifier.Modifier

case class TestFieldInfo(
  signature: String = "",
  descriptor: String = "",
  exceptions: List[String] = Nil,
  modifiers: Set[Modifier] = Set(),
  name: String = "",
  value: Option[Object] = None
) extends FieldInfo

