package com.linkedin.inequalityimpact.spark

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.testng.Assert
import org.testng.annotations.Test

/** Test Atkinson and AtkinsonComparison. */
class AtkinsonComparisonTest {
  val floatingPointEpsilon = 1e-9
  val nCores = 2
  val conf = new SparkConf().setMaster(s"local[${nCores}]").setAppName("AtkinsonComparisonTest")
  val spark: SparkSession = SparkSession.builder.config(conf).getOrCreate()
  import spark.implicits._

  val sc: SparkContext = spark.sparkContext

  def assertAlmostEqual(x: Double, y: Double) {
    assert(Math.abs(y - x) < floatingPointEpsilon)
  }

  @Test
  def AtkinsonComparisonTest() {
    val epsilon = 0.2

    /* From calculations in R for sample equal to integers from 0 through 9 inclusive. */
    val treatmentIndex = 0.0556419803143
    val treatmentVariance = 0.0007733453644
    val treatmentConfidenceIntervalHalfWidth = 0.05450481053
    val treatmentConfidenceIntervalLower = 0.00113716978
    val treatmentConfidenceIntervalUpper = 0.11014679085
    val treatmentN = 10
    val treatmentSum = 45.0
    val treatmentSum1MinusEpsilon = 31.8184741476
    val treatmentSumOfSquares = 285.0
    val treatmentSum2MinusEpsilon = 194.7329798233
    val treatmentSum2Minus2Epsilon = 133.7251657824
    val treatmentAtkinson = Atkinson(treatmentIndex, treatmentVariance,
      treatmentConfidenceIntervalHalfWidth, treatmentConfidenceIntervalLower,
      treatmentConfidenceIntervalUpper, epsilon, treatmentN, treatmentSum,
      treatmentSum1MinusEpsilon, treatmentSumOfSquares, treatmentSum2MinusEpsilon,
      treatmentSum2Minus2Epsilon)

    /* From calculations in R for sample equal to integers from 10 through 19 inclusive. */
    val controlIndex = 0.003984784681071
    val controlVariance = 0.000001396297952
    val controlConfidenceIntervalHalfWidth = 0.002315992462
    val controlConfidenceIntervalLower = 0.001668792219
    val controlConfidenceIntervalUpper = 0.006300777143
    val controlN = 10
    val controlSum = 145.0
    val controlSum1MinusEpsilon = 84.6656369665
    val controlSumOfSquares = 2185.0
    val controlSum2MinusEpsilon = 1266.4234994331
    val controlSum2Minus2Epsilon = 735.0540077612
    val controlAtkinson = Atkinson(controlIndex, controlVariance,
      controlConfidenceIntervalHalfWidth, controlConfidenceIntervalLower,
      controlConfidenceIntervalUpper, epsilon, controlN, controlSum,
      controlSum1MinusEpsilon, controlSumOfSquares, controlSum2MinusEpsilon,
      controlSum2Minus2Epsilon)

    /* test ordering */
    assert(treatmentAtkinson.compare(controlAtkinson) ==  1)
    assert(controlAtkinson.compare(treatmentAtkinson) == -1)
    assert(treatmentAtkinson.compare(treatmentAtkinson) ==  0)
    assert(treatmentAtkinson > controlAtkinson)
    assert(controlAtkinson < treatmentAtkinson)
    assert(treatmentAtkinson == treatmentAtkinson)

    /* Examine the output! */

    println(treatmentAtkinson.toString)
    println(treatmentAtkinson.toStringAll)
    println(controlAtkinson.toString)
    println(controlAtkinson.toStringAll)
    val comparison = AtkinsonComparison(treatmentAtkinson, controlAtkinson)
    println(comparison)

    val expectedDiff = treatmentAtkinson.index - controlAtkinson.index
    val expectedDiffVariance = treatmentAtkinson.variance + controlAtkinson.variance
    val expectedPValue = 0.06346915442
    val expectedHalfWidth = 0.05455399337
    val expectedLower = expectedDiff - expectedHalfWidth
    val expectedUpper = expectedDiff + expectedHalfWidth

    assertAlmostEqual(comparison.diff, expectedDiff)
    assertAlmostEqual(comparison.variance, expectedDiffVariance)
    assertAlmostEqual(comparison.pValue, expectedPValue)
    assertAlmostEqual(comparison.confidenceIntervalLower, expectedLower)
    assertAlmostEqual(comparison.confidenceIntervalUpper, expectedUpper)

    /* test exception thrown when n non-positive. */
    val controlAtkinsonN0 = Atkinson(controlIndex, controlVariance, controlConfidenceIntervalHalfWidth,
      controlConfidenceIntervalLower, controlConfidenceIntervalUpper, epsilon, 0, controlSum,
      controlSum2MinusEpsilon, controlSumOfSquares, controlSum2MinusEpsilon, controlSum2Minus2Epsilon)
    try {
      val comparison = AtkinsonComparison(treatmentAtkinson, controlAtkinsonN0)
      Assert.fail("should throw exception when n is nonpositive")
    } catch {
      case e: IllegalArgumentException => println(e)
      case _: Throwable => Assert.fail("should throw IllegalArgumentException")
    }
    try {
      val comparison = AtkinsonComparison(controlAtkinsonN0, treatmentAtkinson)
      Assert.fail("should throw exception when n is nonpositive")
    } catch {
      case e: IllegalArgumentException => println(e)
      case _: Throwable => Assert.fail("should throw IllegalArgumentException")
    }

    /* test exception thrown when variance negative */
    val controlAtkinsonVarianceNegative = Atkinson(controlIndex, -1.0, controlConfidenceIntervalHalfWidth,
      controlConfidenceIntervalLower, controlConfidenceIntervalUpper, epsilon, controlN, controlSum,
      controlSum2MinusEpsilon, controlSumOfSquares, controlSum2MinusEpsilon, controlSum2Minus2Epsilon)
    try {
      val comparison = AtkinsonComparison(treatmentAtkinson, controlAtkinsonVarianceNegative)
      Assert.fail("should throw exception when variance is negative")
    } catch {
      case e: IllegalArgumentException => println(e)
      case _: Throwable => Assert.fail("should throw IllegalArgumentException")
    }
    try {
      val comparison = AtkinsonComparison(controlAtkinsonVarianceNegative, treatmentAtkinson)
      Assert.fail("should throw exception when variance is negative")
    } catch {
      case e: IllegalArgumentException => println(e)
      case _: Throwable => Assert.fail("should throw IllegalArgumentException")
    }
  }
}
