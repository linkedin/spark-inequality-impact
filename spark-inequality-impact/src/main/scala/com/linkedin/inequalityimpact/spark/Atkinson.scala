package com.linkedin.inequalityimpact.spark

import org.apache.spark.sql.types._

/*
 * &nbsp; is used in scaladoc between parameter name and description when the
 * scaladoc processor would otherwise generate html with no space between a
 * name and description (see https://github.com/scala/bug/issues/7816).
 */

/**
  * Data associated with a single Atkinson index calculation.
  *
  * @param index                        &nbsp; Atkinson index
  * @param variance                     &nbsp; asymptotic approximate variance
  * @param confidenceIntervalHalfWidth  &nbsp; via normal approximation, using the
  *                                     asymptotic variance - about
  *                                     Constants.confidenceIntervalLevel (95%) of
  *                                     samples from a hypothetical very large
  *                                     population from which was drawn the large
  *                                     sample for which Atkinson data was
  *                                     calculated, should lie in the range
  *                                     index +- confidenceIntervalHalfWidth
  * @param confidenceIntervalLower      &nbsp; index - confidenceIntervalHalfWidth
  * @param confidenceIntervalUpper      &nbsp; index + confidenceIntervalHalfWidth
  * @param epsilon                      &nbsp; Atkinson index parameter
  * @param n                            &nbsp; size of the dataset used for calculation
  * @param sum                          &nbsp; sum of the data items
  * @param sum1MinusEpsilon             &nbsp; sum of (item to the 1 - epsilon power)
  * @param sumOfSquares                 &nbsp; sum of squares of data items
  * @param sum2MinusEpsilon             &nbsp; sum of (item to the 2 - epsilon power)
  * @param sum2Minus2Epsilon            &nbsp; sum of (item to the 2 - 2 * epsilon power)
  */
@SerialVersionUID(1L)
case class Atkinson(
  val index: Double,
  val variance: Double,
  val confidenceIntervalHalfWidth: Double,
  val confidenceIntervalLower: Double,
  val confidenceIntervalUpper: Double,
  val epsilon: Double,
  val n: Long,
  val sum: Double,
  val sum1MinusEpsilon: Double,
  val sumOfSquares: Double,
  val sum2MinusEpsilon: Double,
  val sum2Minus2Epsilon: Double) extends Ordered[Atkinson] with Serializable {
  /**
    * Standard compare method, basis of Ordered[Atkinson] methods.  This is made
    * compatible with equals and hashCode by extending the ordering beyond the
    * natural one on index, to break ties with all the other fields.  The tie
    * breaking ordering isn't meaningful but only serves to give compatibility.
    * <p>
    * The ordering is only meaningful if both the Atkinson objects have the same
    * epsilon.
    *
    * @param that the other instance to compare to this one
    * @return     -1, 0, +1 depending on whether that is greater, equal, or
    *             less than this
    */
  def compare(that: Atkinson): Int = {
    val indexSign = (this.index - that.index).signum
    val varianceSign = (this.variance - that.variance).signum
    val confidenceIntervalHalfWidthSign = (this.confidenceIntervalHalfWidth
      - that.confidenceIntervalHalfWidth).signum
    val confidenceIntervalLowerSign = (this.confidenceIntervalLower
      - that.confidenceIntervalLower).signum
    val confidenceIntervalUpperSign = (this.confidenceIntervalUpper
      - that.confidenceIntervalUpper).signum
    val epsilonSign = (this.epsilon - that.epsilon).signum
    val nSign = (this.n - that.n).signum
    val sumSign = (this.sum - that.sum).signum
    val sum1MinusEpsilonSign = (this.sum1MinusEpsilon
      - that.sum1MinusEpsilon).signum
    val sumOfSquaresSign = (this.sumOfSquares
      - that.sumOfSquares).signum
    val sum2MinusEpsilonSign = (this.sum2MinusEpsilon
      - that.sum2MinusEpsilon).signum
    val sum2Minus2EpsilonSign = (this.sum2Minus2Epsilon
      - that.sum2Minus2Epsilon).signum
    if (indexSign != 0) {
      indexSign
    } else if (varianceSign != 0) {
      varianceSign
    } else if (confidenceIntervalHalfWidthSign != 0) {
      confidenceIntervalHalfWidthSign
    } else if (confidenceIntervalLowerSign != 0) {
      confidenceIntervalLowerSign
    } else if (confidenceIntervalUpperSign != 0) {
      confidenceIntervalUpperSign
    } else if (epsilonSign != 0) {
      epsilonSign
    } else if (nSign != 0) {
      nSign
    } else if (sumSign != 0) {
      sumSign
    } else if (sum1MinusEpsilonSign != 0) {
      sum1MinusEpsilonSign
    } else if (sumOfSquaresSign != 0) {
      sumOfSquaresSign
    } else if (sum2MinusEpsilonSign != 0) {
      sum2MinusEpsilonSign
    } else {
      sum2Minus2EpsilonSign
    }
  }

  /** Display the most important data of the class. */
  override def toString(): String = f"Atkinson index ${index}%6.4f, ${Constants.confidenceIntervalLevel * 100}%2.0f%% confidence interval ${confidenceIntervalLower}%7.4f .. ${confidenceIntervalUpper}%7.4f, variance ${variance}%8.2e, n ${n}%10d, epsilon ${epsilon}%4.2f"

  /** Display all the fields of the class, with their names. */
  def toStringAll(): String = f"Atkinson index ${index}%6.4f, ${Constants.confidenceIntervalLevel * 100}%2.0f%% confidence interval half width ${confidenceIntervalHalfWidth}%7.4f and interval ${confidenceIntervalLower}%7.4f .. ${confidenceIntervalUpper}%7.4f, epsilon ${epsilon}%4.2f, n ${n}%9d, sum ${sum}%10.4e, sum1MinusEpsilon ${sum1MinusEpsilon}%10.4e, sumOfSquares ${sumOfSquares}%10.4e, sum2MinusEpsilon ${sum2MinusEpsilon}%10.4e, sum2Minus2Epsilon ${sum2Minus2Epsilon}%10.4e"
}

object Atkinson {
  def schema(): StructType = new StructType()
    .add("index", DoubleType)
    .add("variance", DoubleType)
    .add("confidenceIntervalHalfWidth", DoubleType)
    .add("confidenceIntervalLower", DoubleType)
    .add("confidenceIntervalUpper", DoubleType)
    .add("epsilon", DoubleType)
    .add("n", LongType)
    .add("sum", DoubleType)
    .add("sum1MinusEpsilon", DoubleType)
    .add("sumOfSquares", DoubleType)
    .add("sum2MinusEpsilon", DoubleType)
    .add("sum2Minus2Epsilon", DoubleType)
}
