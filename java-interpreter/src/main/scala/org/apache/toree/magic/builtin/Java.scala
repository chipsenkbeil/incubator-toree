/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */
package org.apache.toree.magic.builtin

import org.apache.toree.interpreter.{ExecuteAborted, ExecuteError}
import org.apache.toree.kernel.interpreter.java.{JavaException, JavaInterpreter}
import org.apache.toree.kernel.protocol.v5.MIMEType
import org.apache.toree.magic.dependencies.IncludeKernel
import org.apache.toree.magic.{CellMagic, CellMagicOutput}

/**
 * Represents the magic interface to use the Java interpreter.
 */
class Java extends CellMagic with IncludeKernel {
  override def execute(code: String): CellMagicOutput = {
    val java = kernel.interpreter("Java")

    if (java.isEmpty || java.get == null)
      throw new JavaException("Java is not available!")

    java.get match {
      case javaInterpreter: JavaInterpreter =>
        val (_, output) = javaInterpreter.interpret(code)
        output match {
          case Left(executeOutput) =>
            CellMagicOutput(MIMEType.PlainText -> executeOutput)
          case Right(executeFailure) => executeFailure match {
            case executeAborted: ExecuteAborted =>
              throw new JavaException("Java code was aborted!")
            case executeError: ExecuteError =>
              throw new JavaException(executeError.value)
          }
        }
      case otherInterpreter =>
        val className = otherInterpreter.getClass.getName
        throw new JavaException(s"Invalid Java interpreter: $className")
    }
  }
}

