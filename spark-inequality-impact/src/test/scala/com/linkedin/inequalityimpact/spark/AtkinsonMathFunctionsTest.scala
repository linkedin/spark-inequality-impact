package com.linkedin.inequalityimpact.spark


import org.testng.annotations.Test

/** Test AtkinsonMathFunctions. */
class AtkinsonMathFunctionsTest {
  val floatingPointEpsilon = 1e-9

  def assertAlmostEqual(x: Double, y: Double) {
    assert(Math.abs(y - x) < floatingPointEpsilon)
  }

  /** Test Atkinson index and its theoretical approximate variance. */
  @Test
  def indexAndVarianceComputationTest() {
    val epsilon = 0.2
    val data = List.range(0, 10)
    val n = data.length
    val sum = data.sum.toDouble
    val sum1MinusEpsilon = data.map(Math.pow(_, 1 - epsilon)).sum
    val sumOfSquares = data.map(Math.pow(_, 2)).sum
    val sum2MinusEpsilon = data.map(Math.pow(_, 2 - epsilon)).sum
    val sum2Minus2Epsilon = data.map(Math.pow(_, 2 - 2 * epsilon)).sum
    val expectedIndex = 0.05564198
    val expectedVariance = 0.00077334536
    val resultIndex = AtkinsonMathFunctions.computeIndex(n, sum,
      sum1MinusEpsilon, epsilon)
    val resultVariance =
      AtkinsonMathFunctions.computeTheoreticalVariance(n, sum,
        sum1MinusEpsilon, sumOfSquares, sum2MinusEpsilon, sum2Minus2Epsilon, epsilon)

    assertAlmostEqual(resultIndex, expectedIndex)
    assertAlmostEqual(resultVariance, expectedVariance)
  }

  /** Test normal distribution confidence interval. */
  @Test
  def normalConfidenceIntervalTest(): Unit = {
    val mean = 0.1
    val variance = 0.8
    val expectedConfidenceIntervalBottom = -1.653045081
    val (resultConfidenceIntervalBottom, resultConfidenceIntervalTop) =
      AtkinsonMathFunctions.normalConfidenceInterval(mean, variance)

    assertAlmostEqual(resultConfidenceIntervalBottom
      + resultConfidenceIntervalTop,  2.0 * mean)
    assertAlmostEqual(resultConfidenceIntervalBottom,
      expectedConfidenceIntervalBottom)
  }


  /** Test approximate difference confidence interval. */
  @Test
  def testApproximateDifferenceConfidenceInterval(): Unit = {
    val treatmentMean = 0.8
    val treatmentVariance = 0.03
    val controlMean = 0.6
    val controlVariance = 0.04
    val diffMeans = treatmentMean - controlMean
    val expectedConfidenceIntervalBottom = -0.3185577282
    val expectedConfidenceIntervalTop = 0.7185577282
    val (resultConfidenceIntervalBottom, resultConfidenceIntervalTop) =
      AtkinsonMathFunctions.computeApproximateDifferenceConfidenceInterval(treatmentMean,
        treatmentVariance, controlMean, controlVariance)

    assertAlmostEqual(resultConfidenceIntervalTop,
      expectedConfidenceIntervalTop)
    assertAlmostEqual(resultConfidenceIntervalBottom,
      expectedConfidenceIntervalBottom)
  }

  /** Test approximate two-sided hypothesis test p-value. */
  @Test
  def approximateTwoSidedPValueTest(): Unit = {
    val treatmentIndex = 0.8
    val treatmentVariance = 0.03
    val controlIndex = 0.6
    val controlVariance = 0.04
    val expectedPValue = 0.449691798
    val resultPValue =
      AtkinsonMathFunctions.computeApproximateTwoSidedPValue(treatmentIndex,
        treatmentVariance, controlIndex, controlVariance)

    assertAlmostEqual(resultPValue, expectedPValue)
  }

  /** Test Atkinson index to haves fraction conversion. */
  @Test
  def computeHavesFractionTest(): Unit = {
    val atkinsonIndex = 0.12345
    val epsilon = 0.2
    val expectedHavesFraction = 0.5903462058
    val resultHavesFraction =
      AtkinsonMathFunctions.computeHavesFraction(atkinsonIndex, epsilon)

    assertAlmostEqual(resultHavesFraction, expectedHavesFraction)
  }
}
