package com.linkedin.inequalityimpact.spark

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.testng.annotations.Test

/** Test AtkinsonAggregator. */
class AtkinsonAggregatorTest {
  val floatingPointEpsilon = 1e-9
  val nCores = 2
  val conf = new SparkConf().setMaster(s"local[${nCores}]").setAppName("AtkinsonAggregatorTest")
  val spark: SparkSession = SparkSession.builder.config(conf).getOrCreate()
  import spark.implicits._

  val sc: SparkContext = spark.sparkContext

  def assertAlmostEqual(x: Double, y: Double) {
    assert(Math.abs(y - x) < floatingPointEpsilon)
  }

  @Test
  def AtkinsonAggregatorTest() {
    val epsilon = 0.2
    val atkinsonAggregator = new AtkinsonAggregator(epsilon)

    /*
     * Test the Atkinson Index UDAF.  Create data which a list from 0 to 9, and check
     * that it returns the right N, index, and variance.
     */
    val nDataItems = 10
    val dataList = List.range(0, nDataItems)
    val data = sc.parallelize(dataList.map(_.toDouble)).toDF
    val expectedIndex = 0.0556419803
    val expectedVariance = 0.00077334536
    val expectedConfidenceIntervalHalfWidth = 0.05450481053
    val expectedConfidenceIntervalLower = 0.00113716978
    val expectedConfidenceIntervalUpper = 0.11014679085
    val expectedSum = 45.0
    val expectedSum1MinusEpsilon = 31.8184741476
    val expectedSumOfSquares = 285.0
    val expectedSum2MinusEpsilon = 194.7329798233
    val expectedSum2Minus2Epsilon = 133.7251657824
    val aggregationResult = data.agg(atkinsonAggregator($"value")
      .as("aggregationResult")).select("aggregationResult.*").as[Atkinson].first

    val resultIndex = aggregationResult.index
    val resultVariance = aggregationResult.variance
    val resultConfidenceIntervalHalfWidth = aggregationResult.confidenceIntervalHalfWidth
    val resultConfidenceIntervalLower = aggregationResult.confidenceIntervalLower
    val resultConfidenceIntervalUpper = aggregationResult.confidenceIntervalUpper
    val resultEpsilon = aggregationResult.epsilon
    val resultN = aggregationResult.n
    val resultSum = aggregationResult.sum
    val resultSum1MinusEpsilon = aggregationResult.sum1MinusEpsilon
    val resultSumOfSquares = aggregationResult.sumOfSquares
    val resultSum2MinusEpsilon = aggregationResult.sum2MinusEpsilon
    val resultSum2Minus2Epsilon = aggregationResult.sum2Minus2Epsilon

    println(aggregationResult.toString)
    println(aggregationResult.toStringAll)

    assertAlmostEqual(resultIndex, expectedIndex)
    assertAlmostEqual(resultVariance, expectedVariance)
    assertAlmostEqual(resultConfidenceIntervalHalfWidth, expectedConfidenceIntervalHalfWidth)
    assertAlmostEqual(resultConfidenceIntervalLower, expectedConfidenceIntervalLower)
    assertAlmostEqual(resultConfidenceIntervalUpper, expectedConfidenceIntervalUpper)
    assertAlmostEqual(resultEpsilon, epsilon)
    assert(resultN == nDataItems)
    assertAlmostEqual(resultN, nDataItems)
    assertAlmostEqual(resultSum, expectedSum)
    assertAlmostEqual(resultSum1MinusEpsilon, expectedSum1MinusEpsilon)
    assertAlmostEqual(resultSumOfSquares, expectedSumOfSquares)
    assertAlmostEqual(resultSum2MinusEpsilon, expectedSum2MinusEpsilon)
    assertAlmostEqual(resultSum2Minus2Epsilon, expectedSum2Minus2Epsilon)
  }
}
