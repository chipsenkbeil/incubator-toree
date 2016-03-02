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

resolvers += "repo.bodar.com" at "http://repo.bodar.com"

fork := true

libraryDependencies ++= Seq(
  "com.googlecode.totallylazy" % "totallylazy" % "1.81" // Apache 2.0
)

// REMOVE WHEN DONE
val sparkVersion = "1.5.1"

libraryDependencies ++= Seq( "org.apache.spark" %% "spark-core" % sparkVersion excludeAll( // Apache v2
    // Exclude netty (org.jboss.netty is for 3.2.2.Final only)
    ExclusionRule(
      organization = "org.jboss.netty",
      name = "netty"
    )
  ),
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.apache.spark" %% "spark-graphx" % sparkVersion,
  "org.apache.spark" %% "spark-repl" % sparkVersion
)
