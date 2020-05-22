package com.linkedin.inequalityimpact.spark

import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.ChiSquaredDistribution

/*
 * &nbsp; is used in scaladoc between parameter name and description when the
 * scaladoc processor would otherwise generate html with no space between a
 * name and description (see https://github.com/scala/bug/issues/7816).
 */

/**
  * Math functions for computations related to the Atkinson index.
  */
object AtkinsonMathFunctions extends Serializable {
  /**
    * The standard normal 0.5 * (1 - Constants.confidenceIntervalLevel) (2.5%)
    * quantile.  This is for use in calculating Constants.confidenceIntervalLevel
    * (95%) confidence intervals, defined by equal probabilities below and above
    * the interval adding up to 1 - the level; hence the multiplication by 0.5.
    */
  val standardLevelQuantile = (new NormalDistribution)
    .inverseCumulativeProbability(0.5 * (1.0 - Constants.confidenceIntervalLevel))

  /**
    * Compute the Atkinson index of a collection of data items given their
    * number, sum, and sum of (1 - epsilon) powers.  The data items should
    * all be non-negative.
    *
    * @param n                 &nbsp; number of data items, must be non-negative,
    *                          if zero, the computed index will be zero
    * @param sum               &nbsp; sum of data items, must be non-negative, must be
    *                          positive when sum1MinusEpsilon is positive
    * @param sum1MinusEpsilon  &nbsp; sum of data items each to the power 1 - epsilon,
    *                          must be non-negative
    * @param epsilon           &nbsp; Atkinson index parameter 0 <= epsilon < 1
    * @return                  &nbsp; Atkinson index
    */
  def computeIndex(n: Long, sum: Double, sum1MinusEpsilon: Double,
    epsilon: Double): Double = {
    if (n < 0) {
      throw new IllegalArgumentException(s"n (${n}) must be non-negative.")
    }
    if (epsilon < 0.0 || epsilon >= 1.0) {
      throw new IllegalArgumentException(s"Must have 0 <= epsilon (${epsilon}) < 1.")
    }
    if (sum < 0.0 || sum1MinusEpsilon < 0.0) {
      throw new IllegalArgumentException(s"sum (${sum}) and sum1MinusEpsilon (${sum1MinusEpsilon}) must be non-negative (all data items should be also).")
    }
    if (sum == 0.0 && sum1MinusEpsilon > 0.0) {
      throw new IllegalArgumentException(s"sum (${sum}) must be positive if sum1MinusEpsilon (${sum1MinusEpsilon}) is.")
    }

    if (epsilon == 0.0 || n == 0 || sum == 0.0 && sum1MinusEpsilon == 0.0) {
      0.0
    } else {
      1 - (Math.pow(sum1MinusEpsilon / n, 1 / (1 - epsilon)) / (sum / n))
    }
  }

  /**
    * Compute the theoretical approximate variance of the Atkinson index of a
    * collection of data items given their number and sums of the following
    * powers: 1, 1 - epsilon, 2, 2 - epsilon, 2 - 2 * epsilon.  The data items
    * should all be non-negative, and not all zero.  What we call the
    * "theoretical" variance is an asymptotic approximation for large n, using
    * the delta method.  More details, including a link to some details of
    * the use of the delta method, are in spark-inequality-impact.pdf.
    *
    * @param n                  &nbsp; number of data items, must be positive
    * @param sum                &nbsp; sum of data items, must be positive
    * @param sum1MinusEpsilon   &nbsp; sum of data items each to the power 1 - epsilon,
    *                           must be positive
    * @param sumOfSquares       &nbsp; sum of squares of data items,
    *                           must be positive
    * @param sum2MinusEpsilon   &nbsp; sum of data items each to the power 2 - epsilon,
    *                           must be positive
    * @param sum2Minus2Epsilon  &nbsp; sum of data items each to power 2 - 2 * epsilon,
    *                           must be positive
    * @param epsilon            &nbsp; Atkinson index parameter 0 <= epsilon < 1
    * @return                   &nbsp; theoretical variance
    */
  def computeTheoreticalVariance(n: Long, sum: Double, sum1MinusEpsilon: Double,
    sumOfSquares: Double, sum2MinusEpsilon: Double, sum2Minus2Epsilon: Double,
    epsilon: Double): Double = {
    if (n <= 0) {
      throw new IllegalArgumentException(s"n (${n}) must be positive.")
    }
    if (epsilon < 0.0 || epsilon >= 1.0) {
      throw new IllegalArgumentException(s"Must have 0 <= epsilon (${epsilon}) < 1.")
    }
    if (sum <= 0.0 || sum1MinusEpsilon <= 0.0 || sumOfSquares <= 0.0
      || sum2MinusEpsilon <= 0.0 || sum2Minus2Epsilon <= 0.0) {
      throw new IllegalArgumentException(s"sum (${sum}), sum1MinusEpsilon (${sum1MinusEpsilon}), sumOfSquares (${sumOfSquares}), sum2MinusEpsilon (${sum2MinusEpsilon}), and sum2Minus2Epsilon (${sum2Minus2Epsilon}) must be positive (also, all data items should be non-negative).")
    }

    if (epsilon == 0.0) {
      0.0
    } else {
      /* Means of powers of data items. */
      val mean = sum / n
      val mean1MinusEpsilon = sum1MinusEpsilon / n
      val meanOfSquares = sumOfSquares / n
      val mean2MinusEpsilon = sum2MinusEpsilon / n
      val mean2Minus2Epsilon = sum2Minus2Epsilon / n

      /* First term of variance-covariance matrix. */
      val sum11 = mean2Minus2Epsilon - Math.pow(mean1MinusEpsilon, 2)

      /* Second term of variance-covariance matrix. */
      val sum12 = mean2MinusEpsilon - mean * mean1MinusEpsilon

      /* Third term of variance-covariance matrix. */
      val sumOfSquaresSquared = meanOfSquares - Math.pow(mean, 2)

      /* Final estimated variance of the Atkinson Index. */
      val estimatedVariance = (
        sum11 * Math.pow(mean1MinusEpsilon, 2 * epsilon / (1 - epsilon))
            / (Math.pow(1 - epsilon, 2) * Math.pow(mean, 2))
          - 2 * sum12 * Math.pow(mean1MinusEpsilon, (1 + epsilon)
            / (1 - epsilon)) / ((1 - epsilon) * Math.pow(mean, 3))
          + sumOfSquaresSquared * Math.pow(mean1MinusEpsilon, 2 / (1 - epsilon))
            / Math.pow(mean, 4)) / n

      estimatedVariance
    }
  }

  /**
    * Compute a confidence interval half-width at the
    * Constants.confidenceIntervalLevel (95%) level for a normal distribution
    * with given variance.  The corresponding confidence interval would be the
    * mean +- the half width.
    * <p>
    * This can be applied to get an approximate confidence interval half-width for
    * an Atkinson index, by assuming that the number of data items is large enough
    * that the Atkinson index has an approximate normal distribution with mean
    * equal to the Atkinson index and variance equal to the theoretical variance
    * as calculated by computeTheoreticalVariance.
    *
    * @param variance  of the distribution, must be nonnegative.
    * @return          the confidence interval half width
    */
  def normalConfidenceIntervalHalfWidth(variance: Double):
      Double = {
    if (variance < 0.0) {
      throw new IllegalArgumentException(s"variance (${variance}) must be nonnegative.")
    }
    Math.abs(Math.sqrt(variance) * standardLevelQuantile)
  }

  /**
    * Compute a confidence interval at the Constants.confidenceIntervalLevel
    * (95%) level for a normal distribution with given mean and variance.
    * <p>
    * This can be applied to get an approximate confidence interval for an
    * Atkinson index, by assuming that the number of data items is large enough
    * that the Atkinson index has an approximate normal distribution with mean
    * equal to the Atkinson index and variance equal to the theoretical variance
    * as calculated by computeTheoreticalVariance.
    *
    * @param mean      of the distribution
    * @param variance  of the distribution, must be nonnegative
    * @return          the confidence interval
    */
  def normalConfidenceInterval(mean: Double, variance: Double):
      (Double, Double) = {
    val halfWidth = normalConfidenceIntervalHalfWidth(variance)
    (mean - halfWidth, mean + halfWidth)
  }

  /**
    * Compute an approximate confidence interval at the
    * Constants.confidenceIntervalLevel (95%) level, for the difference,
    * mean1 - mean2, of means of samples from two normal distributions.
    * If the results come from an A/B test, 1 is treatment and 2 control.
    * <p>
    * This can be applied to get an approximate confidence interval for the
    * difference of Atkinson indices from samples of two populations, by
    * assuming that the numbers n of data items are large enough that various
    * approximations hold.  Set the means to the indices and the variances
    * to the theoretical variances.
    *
    * @param mean1      mean of the first sample (e.g. treatment)
    * @param variance1  variance of the first sample, must be nonnegative
    * @param mean2      mean of the second sample (e.g. control)
    * @param variance2  variance of the second sample, must be nonnegative
    * @return           2-tuple (bottom of confidence interval, top of confidence interval)
    */
  def computeApproximateDifferenceConfidenceInterval(mean1: Double, variance1: Double,
    mean2: Double, variance2: Double) = {
    if (variance1 < 0.0 || variance2 < 0.0) {
      throw new IllegalArgumentException(s"variance1 (${variance1}), variance2 (${variance2}) must be nonnegative.")
    }

    normalConfidenceInterval(mean1 - mean2, variance1 + variance2)
  }

  /**
    * Calculate an approximate two-sided sample p-value for the test of the
    * hypothesis that two samples are from normal distributions  with equal
    * means.  For this 2-sided test the order of the two samples doesn't matter,
    * but if it did and this were an A/B test, you could consider 1 the
    * treatment and 2 the control.
    * <>p>
    * This can be applied to get an approximate p-value for the hypothesis
    * that Atkinson indices from samples of two populations are equal, by
    * assuming that the numbers n of data items are large enough that various
    * approximations hold.  Set the means to the indices and the variances
    * to the theoretical variances.
    *
    * @param mean1      mean of the first sample (e.g. treatment)
    * @param variance1  variance of the first sample, must be nonnegative
    * @param mean2      mean of the second sample (e.g. control)
    * @param variance2  variance of the second sample, must be nonnegative
    * @return           p-value
    */
  def computeApproximateTwoSidedPValue(mean1: Double, variance1: Double,
    mean2: Double, variance2: Double) = {
    if (variance1 < 0.0 || variance2 < 0.0) {
      throw new IllegalArgumentException(s"variance1 ${variance1}, variance2 ${variance2} must be nonnegative.")
    }

    val standardDistribution = new NormalDistribution()
    val diff = mean1 - mean2
    val diffVariance = variance1 + variance2
    if (diffVariance > 0.0) {
      standardDistribution.cumulativeProbability(-Math.abs(diff)
        / Math.sqrt(diffVariance)) * 2.0
    } else {
      if (diff != 0) {
        0.0
      } else {
        1.0
      }
    }
  }

  /** Compute the fraction of "haves" in population in which "haves" have equal,
    * nonzero incomes, and "have-nots" have zero income, corresponding to
    * given atkinsonIndex and epsilon.
    *
    * @param atkinsonIndex  &nbsp; to convert, must be between 0 and 1, inclusive
    * @param epsilon        &nbsp; Atkinson index parameter 0 <= epsilon < 1
    * @return               &nbsp; corresponding fraction of "haves"
    */
  def computeHavesFraction(atkinsonIndex: Double, epsilon: Double): Double = {
    if (atkinsonIndex.isNaN || atkinsonIndex < 0.0 || atkinsonIndex > 1.0) {
      throw new IllegalArgumentException(s"atkinsonIndex (${atkinsonIndex}) must be strictly between 0 and 1.")
    }
    if (epsilon < 0.0 || epsilon >= 1.0) {
      throw new IllegalArgumentException(s"Must have 0 <= epsilon (${epsilon}) < 1.")
    }

    assert((0.0 <= atkinsonIndex && atkinsonIndex <= 1.0
      && 0.0 < epsilon && epsilon < 1.0), s"atkinsonIndex ${atkinsonIndex}, epsilon ${epsilon}")
    if (atkinsonIndex == 1.0) {
      Constants.tinyHavesFraction
    } else {
      Math.pow(1.0 - atkinsonIndex, (1.0 - epsilon) / epsilon)
    }
  }
}
