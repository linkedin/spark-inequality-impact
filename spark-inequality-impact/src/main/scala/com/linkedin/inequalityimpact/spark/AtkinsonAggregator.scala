package com.linkedin.inequalityimpact.spark

import org.apache.spark.sql.Row
import org.apache.spark.sql.expressions.MutableAggregationBuffer
import org.apache.spark.sql.types._

/**
  * Spark User-Defined aggregation function (UDAF), to compute the Atkinson Index and its variance.
  *
  * @param epsilon  Atkinson index parameter
  */
class AtkinsonAggregator(val epsilon: Double) extends
    org.apache.spark.sql.expressions.UserDefinedAggregateFunction {

  /** Input schema of UDAF. */
  override def inputSchema: org.apache.spark.sql.types.StructType =
    StructType(StructField("value", DoubleType) :: Nil)

  /**
    * Accumulator buffer schema has fields a subset of those of the Atkinson
    * case class.
    */
  override def bufferSchema: StructType = AtkinsonBuffer.schema

  /** Output schema has fields the same as those of the Atkinson case class. */
  override def dataType: DataType = Atkinson.schema

  /** This is a deterministic algorithm. */
  override def deterministic: Boolean = true

  /**
    * Initialize accumulators in a buffer object to zero.
    *
    * @param buffer a buffer object used to accumulate results
    */
  override def initialize(buffer: MutableAggregationBuffer): Unit = {
    buffer(0) = 0L    // n
    buffer(1) = 0.0   // sum
    buffer(2) = 0.0   // sum1MinusEpsilon
    buffer(3) = 0.0   // sumOfSquares
    buffer(4) = 0.0   // sum2MinusEpsilon
    buffer(5) = 0.0   // sum2Minus2Epsilon
  }

  /**
    * Update accumulators in a buffer object when presented with new data item.
    *
    * @param buffer a buffer object used to accumulate results
    * @param input a new data item to add into the accumulators
    */
  override def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    val x = input.getAs[Double](0)
    buffer(0) = buffer.getAs[Long](0) + 1
    buffer(1) = buffer.getAs[Double](1) + x
    buffer(2) = buffer.getAs[Double](2) + Math.pow(x, 1 - epsilon)
    buffer(3) = buffer.getAs[Double](3) + Math.pow(x, 2)
    buffer(4) = buffer.getAs[Double](4) + Math.pow(x, 2 - epsilon)
    buffer(5) = buffer.getAs[Double](5) + Math.pow(x, 2 - 2 * epsilon)
  }

  /**
    * Merge accumulator buffer objects.
    *
    * @param buffer1 a buffer object used to accumulate results
    * @param buffer2 a buffer object used to accumulate results
    */
  override def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    buffer1(0) = buffer1.getAs[Long](0) + buffer2.getAs[Long](0)
    buffer1(1) = buffer1.getAs[Double](1) + buffer2.getAs[Double](1)
    buffer1(2) = buffer1.getAs[Double](2) + buffer2.getAs[Double](2)
    buffer1(3) = buffer1.getAs[Double](3) + buffer2.getAs[Double](3)
    buffer1(4) = buffer1.getAs[Double](4) + buffer2.getAs[Double](4)
    buffer1(5) = buffer1.getAs[Double](5) + buffer2.getAs[Double](5)
  }

  /**
    * When accumulation is over, compute the actual index and variance.
    *
    * @param buffer  the full accumulator
    * @return        tuple with fields as in the output schema
    */
  override def evaluate(buffer: Row): Any = {
    val n = buffer.getAs[Long](0)
    val sum = buffer.getAs[Double](1)
    val sum1MinusEpsilon = buffer.getAs[Double](2)
    val sumOfSquares = buffer.getAs[Double](3)
    val sum2MinusEpsilon = buffer.getAs[Double](4)
    val sum2Minus2Epsilon = buffer.getAs[Double](5)
    val index = AtkinsonMathFunctions.computeIndex(n, sum,
      sum1MinusEpsilon, epsilon)
    val variance = AtkinsonMathFunctions.computeTheoreticalVariance(n,
      sum, sum1MinusEpsilon, sumOfSquares, sum2MinusEpsilon, sum2Minus2Epsilon,
      epsilon)
    val confidenceIntervalHalfWidth
    = AtkinsonMathFunctions.normalConfidenceIntervalHalfWidth(variance)
    val confidenceInterval
    = AtkinsonMathFunctions.normalConfidenceInterval(index, variance)
    (index, variance, confidenceIntervalHalfWidth, confidenceInterval._1,
      confidenceInterval._2, epsilon, n, sum, sum1MinusEpsilon, sumOfSquares,
      sum2MinusEpsilon, sum2Minus2Epsilon)
  }
}
