package com.linkedin.inequalityimpact.spark

import org.apache.spark.sql.types._

/*
 * &nbsp; is used in scaladoc between parameter name and description when the
 * scaladoc processor would otherwise generate html with no space between a
 * name and description (see https://github.com/scala/bug/issues/7816).
 */

/**
  * Internal data associated with a single Atkinson index calculation.
  *
  * @param n                  &nbsp; size of the dataset used for calculation
  * @param sum                &nbsp; sum of the data items
  * @param sum1MinusEpsilon   &nbsp; sum of (item to the 1 - epsilon power)
  * @param sumOfSquares       &nbsp; sum of squares of data items
  * @param sum2MinusEpsilon   &nbsp; sum of (item to the 2 - epsilon power)
  * @param sum2Minus2Epsilon  &nbsp; sum of (item to the 2 - 2 * epsilon power)
  */
case class AtkinsonBuffer(
  val n: Long,
  val sum: Double,
  val sum1MinusEpsilon: Double,
  val sumOfSquares: Double,
  val sum2MinusEpsilon: Double,
  val sum2Minus2Epsilon: Double) {
  /** Display all the fields of the class, with their names. */
  override def toString(): String = s"n ${n}, sum ${sum}, sum1MinusEpsilon ${sum1MinusEpsilon}, sumOfSquares ${sumOfSquares}, sum2MinusEpsilon ${sum2MinusEpsilon}, sum2Minus2Epsilon ${sum2Minus2Epsilon}"
}

object AtkinsonBuffer {
  def schema(): StructType = new StructType()
    .add("n", LongType)
    .add("sum", DoubleType)
    .add("sum1MinusEpsilon", DoubleType)
    .add("sumOfSquares", DoubleType)
    .add("sum2MinusEpsilon", DoubleType)
    .add("sum2Minus2Epsilon", DoubleType)
}
