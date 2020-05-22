# spark-inequality-impact

README.md revised 2020 May 22.

## Overview

spark-inequality-impact computes the Atkinson index inequality measure, at scale, under Apache Spark,<br>
using its UDAF (user defined aggregation function) on a DataFrame.  In addition, it computes an<br>
asymptotically approximate variance for the Atkinson index, and related confidence intervals<br>
and p-values, allowing inference on the Atkinson index when comparing two populations.

## Notice

Copyright 2020 LinkedIn Corporation
All Rights Reserved.

Licensed under the BSD 2-Clause License (the "License"). See [LICENSE](LICENSE) in the project root for license information.

## Setup

Step-by-step instructions are given in [setup.md](./setup.md).  To run the examples, cd to the root of the local copy<br>
of the repo, the spark-inequality-impact directory (not to be confused with its subdirectory of the same name).

## Compute Atkinson index measure of inequality
```bash
    # From the command prompt, start Spark running locally with 2 threads,
    # telling it where to get the spark-inequality-impact jar file.
    spark-shell --master 'local[2]' --jars ./spark-inequality-impact/build/libs/spark-inequality-impact.jar
```
```scala
    // Tell Spark where to get the spark-inequality-impact software,
    // including an AtkinsonAggregator UDAF and its return value class Atkinson.
    import com.linkedin.inequalityimpact.spark.{Atkinson, AtkinsonAggregator}

    // Read the simple test data file that contains 8 zeros, 2 ones,
    // and construct from it a Spark DataFrame.
    val inputFile = sc.textFile("input.txt")
    val dataFrame = inputFile.flatMap(line => line.split(" ")).toDF

    // Choose the Atkinson index epsilon parameter, which governs relative
    // sensitivity to inequality in different parts of the population
    // distribution, and create an aggregator (UDAF instance).
    val epsilon = 0.2
    val aggregator = new AtkinsonAggregator(epsilon)

    // Use the aggregator on the DataFrame to compute the Atkinson index
    // and approximate variance, which are in an Atkinson object.  This can be
    // done at scale, for instance by running Spark under Yarn on a hadoop cluster.
    // The approximation of the variance would be reasonable only for a larger
    // amount of data, say 50,000 rows.
    val result = (dataFrame.agg(aggregator(col("value")).as("result"))
                           .select("result.*").as[Atkinson].first)

    // Atkinson index 0.3313, 95% confidence interval 0.1240..0.5385,
    // variance 1.12e-02, n 10, epsilon 0.20

    // Extract number of data items (rows in the DataFrame), Atkinson index,
    // and its approximate variance from the Aggregator object.
    val computedN = result.n
    val computedIndex = result.index
    val computedVariance = result.variance

    // Quit Spark shell, included here to show how to get back to the command
    // prompt.  No need to quit if you want to do other things.
    :q
```
- Summary: use UDAF for DataFrame with non-negative double "value" column,
- result stored in an Atkinson object which
  - contains the Atkinson index
  - contains an asymptotically approximate variance of the Atkinson index, estimated<br>
    from the presumably large sample drawn from a hypothetically very large population
  - is comparable by the Atkinson index

## Compare Atkinson indices from two sets of data
```bash
    # From the command prompt, start Spark running locally with 2 threads,
    # telling it where to get the spark-inequality-impact jar file.
    spark-shell --master 'local[2]' --jars ./spark-inequality-impact/build/libs/spark-inequality-impact.jar
```
```scala
    // Tell Spark where to get the spark-inequality-impact software,
    // including an AtkinsonAggregator UDAF, its return value class Atkinson,
    // and the AtkinsonComparison object to use for inference.
    import com.linkedin.inequalityimpact.spark.{Atkinson, AtkinsonAggregator,
      AtkinsonComparison}

    // Read in synthetic income csv test data with two columns, "treatment"
    // and "control", as a DataFrame.
    val dataFrame = (spark.read.format("csv").option("header", "true")
      .load("example_income_data.csv.gz"))

    // Choose the Atkinson index epsilon parameter, which governs relative
    // sensitivity to inequality in different parts of the population
    // distribution, and create an aggregator (UDAF instance).
    val epsilon = 0.2
    val aggregator = new AtkinsonAggregator(epsilon)

    // Use the aggregator twice, once for treatment and once for control, to
    // compute the Atkinson index and variance, in Atkinson objects.  This can
    // be done at scale, for instance by running Spark under Yarn on a hadoop
    // cluster.  The approximation of the variance should be ok for the test
    // data, since it has about 50,000 rows each for treatment and control
    // rows. The seemingly extra parentheses are to split statements over lines.
    val treatmentDataFrame = (dataFrame.filter(col("variant") === "treatment")
                                       .select(col("income")))
    val controlDataFrame = (dataFrame.filter(col("variant") === "control")
                                     .select(col("income")))
    val treatmentAtkinson = (treatmentDataFrame.agg(aggregator(col("income"))
                                                    .as("result"))
                            .select("result.*").as[Atkinson].first)
    val controlAtkinson = (controlDataFrame.agg(aggregator(col("income"))
                                                .as("result"))
                          .select("result.*").as[Atkinson].first)

    // Compare the Atkinson indices.  The comparison object contains the
    // difference of the Atkinson indices, a visual indicator of the degree
    // of significance, the p-value, a 95% confidence interval for the
    // difference, and the variance of the difference.
    val treatmentVsControl = AtkinsonComparison(treatmentAtkinson, controlAtkinson)

    // Atkinson index diff -0.0140 **** pValue 0.00e+00,
    // 95% confidence interval [-0.0164, -0.0116], variance 1.49e-06,
    // diff = treatment 0.0808 - control 0.0948

    // Quit Spark shell, included here to show how to get back to the command
    // prompt.  No need to quit if you want to do other things.
    :q
```
- Summary: get AtkinsonComparison object from two Atkinson objects.  When comparing<br>
  Atkinson indices of samples from two populations, you may want to know not only which<br>
  is larger and by how much, but also how likely (or not) it is that the populations<br>
  have the same Atkinson index and the difference arose by chance in sampling.  The result<br>
  of Atkinson.compareWith is an AtkinsonComparison object, which contains, as well as the<br>
  difference of the indices, a p-value (translated by its toString method for display into<br>
  "ns" for not statistically significant, or the usual asterisks to indicate degree of<br>
  significance), and a 95% confidence interval for the difference.  AtkinsonComparison objects
  - contain the difference of the Atkinson indices from the two populations
  - contain an asymptotic approximate variance for the difference
  - contain an approximate 95% confidence interval for the difference
  - contain an approximate two-sided p-value for rejection of the hypothesis<br>that the objects came from samples of populations with equal Atkinson indices

## Mathematical functions

To use the mathematical functions, after creating one or two DataFrame objects<br>
with your own data, and computing their Atkinson indices and variances as above,<br>
execute the following line in the spark shell:
```scala
    import com.linkedin.inequalityimpact.spark.AtkinsonMathFunctions._
```
This will make the following functions available.  See also [AtkinsonMathFunctions.scala](./spark-inequality-impact/src/main/scala/com/linkedin/inequalityimpact/spark/AtkinsonMathFunctions.scala)<br>
source code and (after building as in [setup.md](./setup.md)) the scaladoc at<br>
`./spark-inequality-impact/build/docs/scaladoc/index.html`<br>for function specs, and [AtkinsonMathFunctionsTest.scala](./spark-inequality-impact/src/test/scala/com/linkedin/inequalityimpact/spark/AtkinsonMathFunctionsTest.scala) for test examples<br>of their use, from which you can copy and paste for your purposes.

- computeIndex - intended for internal use by an AtkinsonAggregator object
- computeTheoreticalVariance - intended for internal use by an AtkinsonAggregator object
- normalConfidenceIntervalHalfWidth - computes half-width of confidence interval for a<br>normally distributed random variable, and can be used in our context as a good approximation<br>for large samples
- normalConfidenceInterval - computes confidence interval for a normally distributed<br>random variable, and can be used in our context as a good approximation for large samples
- computeApproximateDifferenceConfidenceInterval - computes approximate confidence<br>interval for the difference of two normally distributed random variables, and can be used in our<br> context as a good approximation for large samples
- computeApproximateDifferenceTwoSidedPValue - computes approximate two-sided p-value<br>for the difference of two normally distributed random variables, and can be used in our context<br>as a good approximation for large samples
- computeHavesFraction computes the fraction of "haves" in an Atkinson-equivalent<br>representative population of "haves" and "have-nots" for a given Atkinson index and epsilon.


## Documentation and ancillary code

- [README.md](./README.md) - this file
- [setup.md](./setup.md) - which gives step-by-step setup instructions
- [spark-inequality-impact.pdf](./spark-inequality-impact/src/main/tex/spark-inequality-impact.pdf) - gives theoretical background including for interpretation<br>of the Atkinson index in terms of the fraction of "haves" in an Atkinson-equivalent<br>representative population of "haves" and "have-nots"
- [atkinson-interpretation.R](./spark-inequality-impact/src/main/R/atkinson-interpretation.R) - code to convert between Atkinson index and fraction of "haves"
- [atkinson-variance.R](./spark-inequality-impact/src/test/R/atkinson-variance.R) - simulations for several distributions vs. calculated values of<br>approximate variance, etc.  This can be a starting point to see whether sample sizes are large enough for variance-related approximations to be adequate.
- [generate-income-data.R](./spark-inequality-impact/src/test/R/generate-income-data.R) - generate synthetic income data used, after compression with gzip, by the treatment vs. control example in README.md

## Reference

- Guillaume Saint-Jacques, Amir Sepehri, Nicole Li, and Igor Perisic,<br>
[Fairness through experimentation: Inequality in a/b testing as an approach to responsible design](https://arxiv.org/pdf/2002.05819.pdf)<br>
gives broader background, more connections and details; in particular, asymptotic distribution of<br>the Atkinson index.
