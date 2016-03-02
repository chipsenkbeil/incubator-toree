package org.apache.toree

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.toree.kernel.interpreter.java.JavaInterpreter

object Main2 extends App {
  println("Starting")

  val conf = new SparkConf()
  conf.setMaster("local[*]")
  conf.setAppName("test")
  val sc = new SparkContext(conf)
  val ji = new JavaInterpreter

  ji.start()
  ji.bindSparkContext(sc)
  val lines = Seq(
    """import org.apache.spark.api.java.*;""",
    """import org.apache.spark.api.java.function.*;""",
    """import java.util.List;""",
    """import java.util.Arrays;""",
    """List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);""",
    """JavaRDD<Integer> lines = sc.parallelize(data);""",

    // Either this or the next line fails with task not serializable
    """JavaRDD<Integer> lineLengths = lines.map(new org.apache.spark.api.java.function.Function<Integer, Integer>() {
        public Integer call(Integer i) { return i + 1; }
    });""",

    """int totalLength = lineLengths.reduce(new Function2<Integer, Integer, Integer>() {
       public Integer call(Integer a, Integer b) { return a + b; }
    });""",

    """totalLength.toString();"""
  )
  lines.map(ji.interpret(_: String)).foreach(println)
  println("Total length: " + ji.read("totalLength"))
}