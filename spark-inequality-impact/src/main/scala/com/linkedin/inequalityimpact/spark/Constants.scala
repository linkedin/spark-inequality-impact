package com.linkedin.inequalityimpact.spark

/**
  * Constants.
  */
object Constants extends Serializable {
  /** "Haves" fraction (see AtkinsonMathFunctions) used for Atkinson Index value 1. */
  val tinyHavesFraction = 1e-9

  /** The level for confidence interval calculations. */
  val confidenceIntervalLevel = 0.95
}
