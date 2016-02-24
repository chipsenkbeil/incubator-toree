package test.utils

import org.clapper.classutil.MethodInfo
import org.clapper.classutil.Modifier.Modifier

private case class TestMethodInfo(
  signature: String = "",
  descriptor: String = "",
  exceptions: List[String] = Nil,
  modifiers: Set[Modifier] = Set(),
  name: String = ""
) extends MethodInfo

