# Simulations to check theoretical formulas to compute variance etc. for Atkinson index and haves.fraction.
# Run from R with source("atkinson-variance.R", print.eval=TRUE) .  options may be commented out if other
# values are preferred; these were chosen for good display of the results from the simulate function.

library(boot)
library(rlist)
options(width=150)
options(scipen=6, digits=4)

TINY_FRACTION <- 1e-9
ZERO_INTERVAL <- c(0, 0)
ONE_INTERVAL <- c(1, 1)
AGGREGATE_ROW_NAMES <- c("atkinson.index", "atkinson.variance",
                         "haves.fraction", "haves.fraction.variance",
                         "relative.diff.ci.left.endpoint",
                         "relative.diff.ci.right.endpoint",
                         "atkinson.index.interval.coverage",
                         "converted.interval.coverage",
                         "haves.fraction.interval.coverage")
AGGREGATE_COL_NAMES <- c("count", "min", "mean", "max", "variance")

# Calculate Atkinson index for continuous lognormal distribution from variance.
# See http://darp.lse.ac.uk/papersDB/Cowell_measuringinequality3.pdf appendix
# A.3, page 153.
#
# variance  square of standard deviation lognormal distribution parameter
# epsilon   Atkinson index parameter
# returns   Atkinson index for the continuous distribution
atkinson.index.lognormal <- function(variance, epsilon) {
  1 - exp(-0.5 * epsilon * variance)
}

# Compute sums of powers needed for Atkinson index and variance calculation.
#
# sample   data vector of positive length, values nonnegative and not all zero
# epsilon  Atkinson index parameter
# returns  vector consisting of count and sums
sums.of.powers  <- function(sample, epsilon) {
  count <- length(sample)
  sum.1 <- sum(sample)
  sum.1.minus.epsilon <- sum(sample ^ (1 - epsilon))
  sum.2 <- sum(sample ^ 2)
  sum.2.minus.epsilon <- sum(sample ^ (2 - epsilon))
  sum.2.minus.2.epsilon <- sum(sample ^ (2 - 2 * epsilon))
  # The following vector is referred to as "sums" in several places
  c(count, sum.1, sum.1.minus.epsilon, sum.2, sum.2.minus.epsilon,
    sum.2.minus.2.epsilon)
}

# Compute Atkinson index given sums of powers.
#
# sums     vector consisting of count and sums
# epsilon  Atkinson index parameter
# returns  Atkinson index
atkinson.index <- function(sums, epsilon) {
  count <- sums[1]
  sum.1 <- sums[2]
  sum.1.minus.epsilon <- sums[3]
  1 - (sum.1.minus.epsilon / count) ^ (1 / (1 - epsilon)) / (sum.1 / count)
}

# Compute Atkinson index theoretical approximate variance given sums of powers.
#
# sums     vector consisting of count and sums
# epsilon  Atkinson index parameter
# returns  Atkinson index variance
atkinson.variance <- function(sums, epsilon) {
  count  <- sums[1]
  mean.sum.1 <- sums[2] / count
  mean.1.minus.epsilon <- sums[3] / count
  mean.sum.2 <- sums[4] / count
  mean.2.minus.epsilon <- sums[5] / count
  sum.2.minus.2.epsilon <- sums[6] / count
  cov.11 <- sum.2.minus.2.epsilon - mean.1.minus.epsilon ^ 2
  cov.12 <- mean.2.minus.epsilon - mean.sum.1 * mean.1.minus.epsilon
  cov.22 <- mean.sum.2 - mean.sum.1 ^ 2
  (cov.11 * mean.1.minus.epsilon ^ (2 * epsilon / (1 - epsilon))
        / ((1 - epsilon) ^ 2 * mean.sum.1 ^ 2)
      - 2 * cov.12 * mean.1.minus.epsilon ^ ((1 + epsilon) / (1 - epsilon))
        / ((1 - epsilon) * mean.sum.1 ^ 3)
      + cov.22 * mean.1.minus.epsilon ^ (2 / (1 - epsilon))
        / mean.sum.1 ^ 4) / count
}

# Compute Atkinson index and variance.
#
# x        data vector of positive length, values nonnegative and not all zero
# epsilon  Atkinson index parameter
# returns  vector consisting of Atkinson index and its variance
atkinson.calc <- function(sample, epsilon) {
  sums <- sums.of.powers(sample, epsilon)
  c(atkinson.index(sums, epsilon), atkinson.variance(sums, epsilon))
}

# Compute haves fraction from Atkinson index, given epsilon.
#
# atkinson.index must be in [0, 1]
# epsilon        Atkinson index parameter
# returns        haves fraction
# assume 0 <= atkinson.index <= 1
atkinson.to.haves.fraction  <- function(atkinson.index, epsilon) {
  if (atkinson.index == 1.0)
    TINY_FRACTION
  else
    (1 - atkinson.index) ^ ((1 - epsilon) / epsilon)
}

# Compute haves fraction variance for samples from a population of two classes:
# haves, who all have the same income, and have-nots, who all have zero income.
# Please note: this variance would be expected to be much larger than the
# actual variance of haves fractions computed with samples from a much
# smoother distribution such as lognormal.
#
# population.size size of the population from which samples are taken
# sample.size     size of the samples
# haves.fraction  haves fraction of the population
# returns sample haves fraction variance
haves.fraction.variance  <- function(population.size, sample.size,
                                     haves.fraction) {
    haves.fraction * (1 - haves.fraction) * ((population.size - sample.size)
        / ((population.size - 1) * sample.size))
}

# Compute confidence interval at the given level, symmetric about the mean,
# for a normally distributed population.  Clipped to [0, 1].
#
# level     confidence level expressed as for a significance level, smaller
#           is higher confidence and will result in a larger interval
# mean      of the distribution
# variance  of the distribution
# returns   vector of left and right endpoints of confidence interval,
#           which is forced to lie in [0, 1].
clipped.normal.confidence.interval <- function(level, mean, variance) {
  standard.deviation <- sqrt(variance)
  pmax(pmin(c(qnorm(0.5 * level, mean, standard.deviation),
              qnorm(1 - 0.5 * level, mean, standard.deviation)),
            ONE_INTERVAL),
       ZERO_INTERVAL)
}

# Calculate simulation results for one sample.
#
# population  from which to draw the sample
# population.atkinson.index atkinson index computed for population
# population.haves.fraction haves fraction computed for population
# sample.size size of the sample to draw
# epsilon     Atkinson index parameter
# level       confidence level
# returns     vector consisting of Atkinson index and variance, haves fraction
#             and variance, relative differences going from transformed
#             Atkinson index normal approximation confidence interval left and
#             right endpoints to haves fraction normal approximation confidence
#             left and right endpoints, and coverages of atkinson and haves
#             fraction intervals.
one.sample  <- function(population, population.atkinson.index,
                        population.haves.fraction, sample.size, epsilon, level) {
  population.size <- length(population)
  sample <- sample(population, sample.size)
  atkinson.results <- atkinson.calc(sample, epsilon)
  atkinson.index <- atkinson.results[1]
  atkinson.variance <- atkinson.results[2]
  haves.fraction <- atkinson.to.haves.fraction(atkinson.index, epsilon)
  haves.fraction.variance <- haves.fraction.variance(population.size,
                                                     sample.size, haves.fraction)
  atkinson.index.interval <- clipped.normal.confidence.interval(level,
                                                                atkinson.index,
                                                                atkinson.variance)
  # Switching of indices 1, 2 is due to the reversal of order by the transformation.
  converted.interval <- c(atkinson.to.haves.fraction(atkinson.index.interval[2], epsilon),
                          atkinson.to.haves.fraction(atkinson.index.interval[1], epsilon))
  haves.fraction.interval <- clipped.normal.confidence.interval(level, haves.fraction,
                                                                haves.fraction.variance)
  diff.interval <- haves.fraction.interval - converted.interval
  rel.diff.interval <- diff.interval / haves.fraction.interval
  atkinson.index.interval.coverage <- if (atkinson.index.interval[1] <= population.atkinson.index
                                          && population.atkinson.index <= atkinson.index.interval[2]) 1 else 0
  converted.interval.coverage <- if (converted.interval[1] <= population.haves.fraction
                                     && population.haves.fraction <= converted.interval[2]) 1 else 0
  haves.fraction.interval.coverage <- if (haves.fraction.interval[1] <= population.haves.fraction
                                          && population.haves.fraction <= haves.fraction.interval[2]) 1 else 0
  # rel.diff.interval is a vector of size 2, giving rise to 2 elements of result.
  result <- c(atkinson.index, atkinson.variance, haves.fraction,
              haves.fraction.variance, rel.diff.interval,
              atkinson.index.interval.coverage, converted.interval.coverage, haves.fraction.interval.coverage)
  names(result) <- AGGREGATE_ROW_NAMES
  result
}

# Functions for streaming calculation of count, min, mean, max, variance
# for atkinson index, atkinson variance, haves, haves variance, relative
# difference in haves vs. converted atkinson confidence interval
# left and right endpoints, fraction ("coverage") of whole data in
# sample confidence intervals for atkinson and haves fraction.  The driver
# function would call aggregate.init to get an initialized aggregation
# object (currently a matrix), aggregate.row.update to update each of
# several quantities from a one.sample simulation execution, and
# aggregate.row.result for each kind of data for which the aggregation
# object holds summary info.  Then the driver function would make use
# of the summary info in the aggregation object.

# Returns an initialized aggregation object, currently a matrix
# with columns for count, min, mean, max, and variance, and
# rows for Atkinson index, its variance, haves fraction, its variance,
# relative difference of confidence interval left and right endpoints, and
# fractions ("coverage") of whole data in confidence intervals for atkinson
# and haves fraction.  # Note that the aggregation object may store intermediary
# data, so that the column names may not relect the contents until after
# (Currently this applies only to the variance column, which contains
# sums of squares until aggregate.row.result divides by count.)
aggregate.init <- function() {
  matrix(c(0.0, .Machine$double.xmax, 0.0, -.Machine$double.xmax, 0.0,
           0.0, .Machine$double.xmax, 0.0, -.Machine$double.xmax, 0.0,
           0.0, .Machine$double.xmax, 0.0, -.Machine$double.xmax, 0.0,
           0.0, .Machine$double.xmax, 0.0, -.Machine$double.xmax, 0.0,
           0.0, .Machine$double.xmax, 0.0, -.Machine$double.xmax, 0.0,
           0.0, .Machine$double.xmax, 0.0, -.Machine$double.xmax, 0.0,
           0.0, .Machine$double.xmax, 0.0, -.Machine$double.xmax, 0.0,
           0.0, .Machine$double.xmax, 0.0, -.Machine$double.xmax, 0.0,
           0.0, .Machine$double.xmax, 0.0, -.Machine$double.xmax, 0.0),
         nrow=length(AGGREGATE_ROW_NAMES), ncol=length(AGGREGATE_COL_NAMES),
         byrow=TRUE, dimnames=list(AGGREGATE_ROW_NAMES, AGGREGATE_COL_NAMES))
}

# Returns an aggregation object (matrix) with updated row.
#
# row      number of the row to be updated
# value    with which to update the summary info in the row
# returns  an updated aggregation object
aggregate.row.update <- function(row, value) {
  count <- row["count"] + 1
  min <- min(row["min"], value)
  max <- max(row["max"], value)
  mean <- row["mean"]
  sum.squares <- row["variance"]
  diff1 <- value - mean
  mean  <- mean + diff1 / count
  diff2 <- value - mean
  sum.squares <- sum.squares + diff1 * diff2
  c(count, min, mean, max, sum.squares)
}

# Returns an aggregation object (matrix) with finalized row.
#
# row      number of the row to be finalized
# returns  an aggregation object with this row finalized
aggregate.row.result <- function(row) {
  count <- row["count"]
  min <- row["min"]
  mean <- row["mean"]
  max <- row["max"]
  variance <- row["variance"] / count
  c(count, min, mean, max, variance)
}

# Run a simulation by repeatedly getting pseudo-randomly generated population
# of binomial, gamma, or lognormal distribution, for each population repeatedly
# sampling and computing results for the sample, updating an aggregation object
# with them, then finally returning summary results.
#
# distribution            "lognormal" or "binomial"
# distribution.parameters list, for binomial p is the probability parameter, for
#                         lognormal mean.log is the mean of log data and
#                         std.dev.log is the standard deviation of log data, for
#                         gamma shape and scale are the parameters
# population.size         size of the populations
# sample.size             size of the samples
# n.outer                 number of populations to try
# n.inner                 number of samples to draw from each popukation
# epsilon                 Atkinson index parameter
# level                   confidence level, smaller is higher confidence
# returns                 list containing parameter list and summary info matrix
simulate <- function(distribution, distribution.parameters, population.size,
                     sample.size, n.outer, n.inner, epsilon, level) {
  population <- vector()
  for (i in seq(n.outer)) {
    if (distribution == "binomial") {
      population  <- rbinom(n=population.size, size=1,
                            prob=distribution.parameters$p)
    } else if (distribution == "gamma") {
      population  <- rgamma(n=population.size,
                            shape=distribution.parameters$shape,
                            scale=distribution.parameters$scale)
    } else if (distribution == "lognormal") {
      population  <- rlnorm(n=population.size,
                            meanlog=distribution.parameters$mean.log,
                            sdlog=distribution.parameters$std.dev.log)
    } else {
      stop("distribution must be binomial, gamma, or lognormal")
    }

    population.atkinson.results <- atkinson.calc(population, epsilon)
    population.atkinson.index <- population.atkinson.results[1]
    population.haves.fraction <- atkinson.to.haves.fraction(population.atkinson.index,
                                                            epsilon)
    aggregate <- aggregate.init()

    for (j in seq(n.inner)) {
      sample.results <- one.sample(population, population.atkinson.index,
                                   population.haves.fraction, sample.size,
                                   epsilon, level)
      for (row in 1:nrow(aggregate)) {
        aggregate[row,] <- aggregate.row.update(aggregate[row,], sample.results[row])
      }
    }
   }
  for (row in 1:nrow(aggregate)) {
      aggregate[row,] <- aggregate.row.result(aggregate[row,])
  }

  parameters <- list(distribution=distribution,
                     distribution.parameters=distribution.parameters,
                     population.size=population.size, sample.size=sample.size,
                     n.outer=n.outer, n.inner=n.inner, epsilon=epsilon,
                     level=level)
  list(parameters=list.flatten(parameters), aggregate=aggregate)
}

#distribution <- "binomial"
#distribution.parameters <- list(p=0.2)

# See http://www.vcharite.univ-mrs.fr/PP/lubrano/cours/Lecture-4.pdf figure 10,
# page 31 for plots of gamma distribution density functions.  shape value
# from trial and error to produce mean haves fraction 0.2.

#distribution <- "gamma"
#distribution.parameters <- list(shape=0.114, scale=1.0)

# See http://www.vcharite.univ-mrs.fr/PP/lubrano/cours/Lecture-4.pdf figure 5,
# page 22 for plots of lognormal distribution density functions.  std.dev
# calculated from 0.3313 = 1 - exp(-0.5 * 0.2 * std.dev^2)., as in the
# function atkinson.index.lognormal.

distribution <- "lognormal"
distribution.parameters <- list(mean.log=0.0, std.dev.log=2.006)

population.size <- 20000
sample.size <- 2000
n.outer  <- 100
n.inner <- 1000
epsilon <- 0.2
level <- 0.05

simulate(distribution, distribution.parameters, population.size, sample.size,
         n.outer, n.inner, epsilon, level)

# Example output follows, distribution parameters chosen so that haves fraction
# is close to 0.20.  With epsilon 0.2, haves fraction 0.20 corresponds to
# Atkinson index 0.3313.

# For the binomial distribution, actual vs. calculated (theoretical) atkinson
# index variances, and actual vs. calculated haves.fraction variances, were
# close (within 10%), and the transformed and actual haves fraction confidence
# confidence interval coverages were close to each other and to the desired 95%.

# $parameters$distribution
# [1] "binomial"
# $parameters$distribution.parameters.p
# [1] 0.2
# $parameters$population.size
# [1] 20000
# $parameters$sample.size
# [1] 2000
# $parameters$n.outer
# [1] 100
# $parameters$n.inner
# [1] 1000
# $parameters$epsilon
# [1] 0.2
# $parameters$level
# [1] 0.05
#                                   count         min        mean         max  variance
# atkinson.index                    1000  0.31281097  0.33112965  0.36266028 4.997e-05
# atkinson.variance                 1000  0.00005142  0.00005589  0.00006424 3.167e-12
# haves.fraction                    1000  0.16500000  0.20028950  0.22300000 7.106e-05
# haves.fraction.variance           1000  0.00006200  0.00007205  0.00007798 5.198e-12
# relative.diff.ci.left.endpoint    1000  0.00162333  0.00180493  0.00186529 8.805e-10
# relative.diff.ci.right.endpoint   1000 -0.00801281 -0.00685000 -0.00625474 5.808e-08
# atkinson.index.interval.coverage  1000  0.00000000  0.96800000  1.00000000 3.098e-02
# converted.interval.coverage       1000  0.00000000  0.96800000  1.00000000 3.098e-02
# haves.fraction.interval.coverage  1000  0.00000000  0.95700000  1.00000000 4.115e-02

# For the gamma distribution, actual vs. calculated (theoretical) atkinson
# index variances, and actual vs. calculated haves.fraction variances, were
# close (within 10%), and the transformed and actual haves fraction confidence
# confidence interval coverages were close to each other and to the desired 95%.

# $parameters$distribution
# [1] "gamma"
# $parameters$distribution.parameters.shape
# [1] 0.114
# $parameters$distribution.parameters.scale
# [1] 1
# $parameters$population.size
# [1] 20000
# $parameters$sample.size
# [1] 2000
# $parameters$n.outer
# [1] 100
# $parameters$n.inner
# [1] 1000
# $parameters$epsilon
# [1] 0.2
# $parameters$level
# [1] 0.05
#                                   count         min        mean        max  variance
# atkinson.index                    1000  0.31265648  0.33460348 0.36081201 4.545e-05
# atkinson.variance                 1000  0.00003461  0.00005307 0.00008321 6.023e-11
# haves.fraction                    1000  0.16692233  0.19615049 0.22320061 6.306e-05
# haves.fraction.variance           1000  0.00006258  0.00007093 0.00007803 4.714e-12
# relative.diff.ci.left.endpoint    1000 -0.01500995 -0.00143242 0.01969864 3.427e-05
# relative.diff.ci.right.endpoint   1000 -0.02442911 -0.00388871 0.00920541 3.251e-05
# atkinson.index.interval.coverage  1000  0.00000000  0.95600000 1.00000000 4.206e-02
# converted.interval.coverage       1000  0.00000000  0.95600000 1.00000000 4.206e-02
# haves.fraction.interval.coverage  1000  0.00000000  0.95700000 1.00000000 4.115e-02

# For the lognormal distribution, the actual atkinson index variance was about
# 11% larger than the mean calculated (theoretical) atkinson index variance, the
# actual haves fraction variance was about 15 times the mean calculated value,
# the atkinson index confidence interval coverage was about 0.84 vs. 0.95, the
# transformed confidence interval coverage the same, and the haves fraction
# confidence interval coverage was about 0.38.

# $parameters$distribution
# [1] "lognormal"
# $parameters$distribution.parameters.mean.log
# [1] 0
# $parameters$distribution.parameters.std.dev.log
# [1] 2.006
# $parameters$population.size
# [1] 20000
# $parameters$sample.size
# [1] 2000
# $parameters$n.outer
# [1] 100
# $parameters$n.inner
# [1] 1000
# $parameters$epsilon
# [1] 0.2
# $parameters$level
# [1] 0.05
# aggregate
#                                  count         min        mean         max  variance
# atkinson.index                    1000  0.26278885  0.32633645  0.41139462 7.378e-04
# atkinson.variance                 1000  0.00007006  0.00066205  0.00351789 3.313e-07
# haves.fraction                    1000  0.12003196  0.20794540  0.29537082 1.070e-03
# haves.fraction.variance           1000  0.00004753  0.00007364  0.00009366 7.497e-11
# relative.diff.ci.left.endpoint    1000  0.02169671  0.18162897  0.49488181 9.591e-03
# relative.diff.ci.right.endpoint   1000 -0.75959661 -0.21835474 -0.02471656 1.967e-02
# atkinson.index.interval.coverage  1000  0.00000000  0.84000000  1.00000000 1.344e-01
# converted.interval.coverage       1000  0.00000000  0.84000000  1.00000000 1.344e-01
# haves.fraction.interval.coverage  1000  0.00000000  0.38000000  1.00000000 2.356e-01

# Trying several more sample sizes for the lognormal distribution with other
# parameters as above, the actual Atkinson variance relative to theoretical
# variance decreased as sample size increased, going from 11% more to 7% less,
# 30% less,and 80% less as the sample size increased from 2k to 4K, 8K, and 16K.
# The ratio of actual to calculated haves fraction variance went from 15 to 26,
# 20, and 6.  The confidence interval coverages went from 84% and 38% for
# Atkinson index and haves fraction respectively, to 82% and 26%, 92% and 31%,
# and 100% and 57%.

# These numbers varied, some considerably, on repeated runs.  For example, with
# 4K samples again, 7% less became 16% less, 26 times became 7 times, and 82%, 26%
# became 94%, 51%.

# With sample size 20K, the actual Atkinson index variance was 1e-33 whereas
# calculated was 9.3e-5.  The actual haves fraction variance was 1e-33 and
# calculated was 0.  The confidence interval coverages for Atkinson index was
# 100% and for haves fraction 91%.

# With population size 2M and sample size 50K, the Atkinson index confidence
# interval coverage was 91%.  For haves fraction it was 26%, and the ratio of
# actual to calculated haves fraction variance was 43.
