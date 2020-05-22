package com.linkedin.inequalityimpact.spark

/*
 * &nbsp; is used in scaladoc between parameter name and description when the
 * scaladoc processor would otherwise generate html with no space between a
 * name and description (see https://github.com/scala/bug/issues/7816).
 */

/**
  * Data associated with the comparison of Atkinson indices (calculated with the
  * same epsilon) from two samples identified as treatment and control.  The
  * comparison gives the difference of treatment minus control Atkinson indices.
  * Do not use the default case class constructor, but instead the apply method
  * (called as though it were a constructor without using "new") taking only two
  * Atkinson objects, as in the AtkinsonComparison object below.
  *
  * @param treatment                &nbsp; treatment Atkinson object
  * @param control                  &nbsp; control Atkinson object
  * @param diff                     &nbsp; difference treatment - control Atkinson indices
  * @param variance                 &nbsp; asymptotic estimate variance of difference
  * @param pValue                   &nbsp; estimated pValue of difference
  * @param confidenceIntervalLower  &nbsp; lower limit of estimated
  *                                 Constants.confidenceIntervalLevel (95%)
  *                                 confidence interval for diff
  * @param confidenceIntervalUpper  &nbsp; upper limit of estimated
  *                                 Constants.confidenceIntervalLevel (95%)
  *                                 confidence interval for diff
  */
@SerialVersionUID(1L)
case class AtkinsonComparison (
  val treatment: Atkinson,
  val control: Atkinson,
  val diff: Double,
  val variance: Double,
  val pValue: Double,
  val confidenceIntervalLower: Double,
  val confidenceIntervalUpper: Double) extends Serializable {
  if (!AtkinsonComparison.almostEqual(treatment.epsilon, control.epsilon)
    || treatment.variance < 0.0 || control.variance < 0.0
    || treatment.n <= 0 || control.n <= 0) {
    throw new IllegalArgumentException(s"variances must be nonnegative, values of n positive, and epsilons equal.  variance treatment, control = ${treatment.variance}, ${control.variance}; n treatment, control = ${treatment.n}, ${control.n}; epsilon treatment, control = ${treatment.epsilon}, ${control.epsilon}")
  }
  if (treatment.n < AtkinsonComparison.largeEnoughN
    || control.n < AtkinsonComparison.largeEnoughN) {
    System.err.println(s"Your sample size(s) (n treatment, control = ${treatment.n}, ${control.n}) may be smaller than desirable, depending how much accuracy you need and the distribution of the data.  The computations of the p-value and the confidence interval are based on the variance calculation, which uses an asymptotic approximation.  You might consider using a permutation variance calcultation.  This might someday be a part of the spark-inequality-impact software, depending on demand and development resources.")
  }

  /*
   * Disable copy methpod since it could be used to create invalid SmallDates
   * via e.g. someSmallDate.copy(date = -1).
   */
  private def copy(): Unit = ()

  /** Display all fields other than from the two Atkinson objects, with their names, pretty-printed. */
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def toString(): String = f"Atkinson index diff ${diff}%7.4f ${AtkinsonComparison.pValueStars(pValue)}%4s pValue ${pValue}%8.2e, ${Constants.confidenceIntervalLevel * 100}%2.0f%% confidence interval [${confidenceIntervalLower}%7.4f, ${confidenceIntervalUpper}%7.4f], variance ${variance}%8.2e, diff = treatment ${treatment.index}%6.4f - control ${control.index}%6.4f"
}

object AtkinsonComparison {
  /** Used to check floating point equality. */
  val floatingPointEpsilon = 1e-12
  /**
    * Somewhat arbitrarily chosen sample size beyond which we may expect the
    * asymptotic variance estimate to be ok, depending on the population
    * distribution.  This is 2% below 50000, allowing for some variation in
    * random selection nominally of 50000 samples.
    */
  val largeEnoughN = 49000

  /**
    * Check for floating point equality (difference less than floatingPointEpsilon.
    *
    * @param x1 one floating point number
    * @param x2 another floating point number
    * @return   true when the absolute value of their difference is no more than
    *           floatingPointEpsilon
    */
  def almostEqual(x1: Double, x2: Double): Boolean = {
    Math.abs(x2 - x1) <= floatingPointEpsilon
  }

  /**
    * Convert pValue to commonly used strings.
    *
    * @param pValue to convert to a string, to be meaningful must be nonnegative
    * @return       the string
    */
  def pValueStars(pValue: Double): String = {
    if (pValue > 0.05) {
      "ns  "
    } else if (pValue > 0.01) {
      "*   "
    } else if (pValue > 0.001) {
      "**  "
    } else if (pValue > 0.0001) {
      "*** "
    } else {
      "****"
    }
  }

  /**
    * Use this (via e.g. val c = AtkinsonComparison(treatment, control) instead
    * of the default case class constructor.
    * <p>
    * Create an AtkinsonComparison object comparing a treatment Atkinson object
    * to a control Atkinson object.  For example, the diff will be the Atkinson
    * index of the treatment minus that of the control.
    *
    * @param treatment object containing results of aggregating treatment data
    * @param control object containing results of aggregating control data
    * @return the comparison object.
    */
  def apply(treatment: Atkinson, control: Atkinson): AtkinsonComparison = {
    val diff = treatment.index - control.index
    val variance = treatment.variance + control.variance
    val pValue
    = AtkinsonMathFunctions.computeApproximateTwoSidedPValue(treatment.index,
      treatment.variance, control.index, control.variance)
    val (confidenceIntervalLower, confidenceIntervalUpper)
    = AtkinsonMathFunctions.computeApproximateDifferenceConfidenceInterval(
      treatment.index, treatment.variance, control.index, control.variance)
    new AtkinsonComparison(treatment, control, diff, variance, pValue,
      confidenceIntervalLower, confidenceIntervalUpper)
  }
}
