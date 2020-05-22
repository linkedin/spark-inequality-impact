# Generate synthetic income data for the example in README.md.
# There are 100000 data points initially with incomes sampled
# from a lognormal distribution with mean log equal to log(40000)
# and standard deviation of the log equal to 1.  Then about
# half of these are randomly chosen to be control, and the others
# treatment.  To each treatment income is added 5000.  The
# result is a data frame with id (1:100000), variant (control
# or treatment), and income columns.
# Run from R with source("generate-income-data.R", print.eval=TRUE)

​set.seed(2020)
n <- 100000
mean_log <- log(40000)
standard_deviation_log <- 1.0
treatment_addend <- 5000
n_significant_digits <- 4

round_income <- function(income) {
  round(signif(income, digits=n_significant_digits))
}

income <- round_income(rlnorm(n, meanlog=mean_log, sdlog=standard_deviation_log))
variant <- ifelse(runif(n) < 0.5, "control", "treatment")
id <- 1:n
result <- data.frame(id, variant, income)

display_summary <- function(data_frame, variant, title) {
  print(title)

  income <- result[result$variant == variant, "income"]
  print(summary(income))
  print(quantile(income, c(0.01, 0.1, 0.25, 0.5, 0.75, 0.9, 0.99)))
}

display_summary(result, "control", "control")
display_summary(result, "treatment", "treatment before addition")

treatment_income <- result[result$variant == "treatment", "income"]
result[result$variant == "treatment", "income"]  <- treatment_income + treatment_addend
display_summary(result, "treatment", "treatment after addition")

write.csv(result, "./example_income_data.csv", row.names=FALSE)

# Display output from run used to produce data given in project root directory
#[1] "control"
#   Min. 1st Qu.  Median    Mean 3rd Qu.    Max.
#    762   20450   39815   65679   78118 2560000
#       1%       10%       25%       50%       75%       90%       99%
#  3892.45  11069.00  20450.00  39815.00  78117.50 144000.00 399451.00
#[1] "treatment before addition"
#   Min. 1st Qu.  Median    Mean 3rd Qu.    Max.
#    612   20300   39620   65413   78200 1896000
#       1%       10%       25%       50%       75%       90%       99%
#  3964.96  11150.00  20300.00  39620.00  78200.00 143800.00 409451.00
#[1] "treatment after addition"
#   Min. 1st Qu.  Median    Mean 3rd Qu.    Max.
#   5612   25300   44620   70413   83200 1901000
#       1%       10%       25%       50%       75%       90%       99%
#  8964.96  16150.00  25300.00  44620.00  83200.00 148800.00 414451.00
