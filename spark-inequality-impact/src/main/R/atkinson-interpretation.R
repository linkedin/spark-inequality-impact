# Produces data for graph of conversion from Atkinson index to fraction of "haves".
# Writes file atkinson-to-haves.pdf, which is used by spark-inequality-impact.tex.
# Run from R with source("atkinson-interpretation.R", print.eval=TRUE)

TINY_FRACTION <- 1e-9

# Compute Atkinson index
#
# data nonnegative data, not all zero, for which to compute the Atkinson index
# epsilon Atkinson index parameter in range [0, 1)
# returns Atkinson index
atkinson <- function(data, epsilon) {
  population.size <- length(data)
  sum <- sum(data)
  sum.1.minus.epsilon <- sum(data ^ (1 - epsilon))
  1 - (sum.1.minus.epsilon / population.size) ^ (1 / (1 - epsilon)) / (sum / population.size)
}

# Compute Atkinson index from haves fraction.
#
# fraction of population that have income, in range [0, 1]
# epsilon Atkinson index parameter in range [0, 1).
# returns Atkinson index
haves.fraction.to.atkinson <- function(fraction, epsilon) {
  if (fraction == 0.0)
    0.0
  else
    1 - fraction ^ (epsilon / (1 - epsilon))
}

# Compute Atkinson index from number of haves and total number
#
# population.size size of population containing the haves
# n.haves number of haves
# epsilon Atkinson index parameter in range [0, 1).
# returns Atkinson index
haves.to.atkinson  <- function(population.size, n.haves, epsilon) {
  haves.fraction.to.atkinson(n.haves / population.size, epsilon)
}

# Compute haves fraction from Atkinson index.
#
# atkinson.index in range [0, 1]
# epsilong Atkinson index parameter
# return haves fraction
atkinson.to.haves.fraction  <- function(atkinson.index, epsilon) {
  if (atkinson.index == 1.0)
    TINY_FRACTION
  else
    (1 - atkinson.index) ^ ((1 - epsilon) / epsilon)
}

# Compute number of haves from Atkinson index.
#
# population.size size of population for which to compute number of haves.
# atkinson.index in range [0, 1]
# epsilon Atkinson index parameterk in range [0, 1)
# return 2-vector containing the number of haves and the population size.
atkinson.to.haves  <- function(population.size, atkinson.index, epsilon) {
  fraction <- atkinson.to.haves.fraction(atkinson.index, epsilon)
  if (fraction == TINY_FRACTION)
    c(1, population.size)
  else
    c(round(fraction * population.size), population.size)
}

# Create graph in pdf file for inclusion in LaTeX documentation.

epsilon.indices <- c(1, 2, 3)
epsilon.values <- c(0.5, 0.2, 0.1)  # this order so that plot (vs lines) will get largest y range
epsilon.colors <- c('red', 'green', 'blue')
functions <- c(plot, lines, lines)
population.size <- 100
n.haves.seq  <- seq(1, population.size)

pdf('atkinson-to-haves.pdf')
for (i in epsilon.indices) {
  epsilon <- epsilon.values[[i]]
  color <- epsilon.colors[[i]]
  f <- functions[[i]]
  f(sapply(n.haves.seq, function(n.haves) haves.to.atkinson(population.size, n.haves, epsilon)),
    n.haves.seq / population.size, type='l', lty=i,
    main="Fraction of Haves vs. Atkinson Index",
    ylab="Fraction of Haves (not including 0, equality of misery)",
    xlab="Atkinson Index\nepsilon 0.1 blue, 0.2 green, 0.5 red",
    col=color)
}
dev.off()

# Rudimentary tests of the functions.
floating.point.epsilon <- 1e-7
population.size <- 10
n.haves <- 2
epsilon <- 0.2
data <- c(rep.int(1, n.haves), rep.int(0, population.size - n.haves))

atkinson.index <- atkinson(data, epsilon)
atkinson.index.from.haves <- haves.to.atkinson(population.size, n.haves, epsilon)
atkinson.index.from.haves.fraction <- haves.fraction.to.atkinson(n.haves / population.size, epsilon)
expected.atkinson.index <- 1 - (n.haves / population.size)^(epsilon / (1 - epsilon))
if (abs(atkinson.index - expected.atkinson.index) > floating.point.epsilon
    || abs(atkinson.index.from.haves - expected.atkinson.index) > floating.point.epsilon
    || abs(atkinson.index.from.haves.fraction - expected.atkinson.index) > floating.point.epsilon) {
  stop("atkinson index bad")
}

haves.fraction <- atkinson.to.haves.fraction(atkinson.index.from.haves.fraction, epsilon)
haves <- atkinson.to.haves(population.size, atkinson.index.from.haves, epsilon)
if (abs(haves.fraction - n.haves / population.size) > floating.point.epsilon
    || haves != n.haves) {
  stop("haves bad")
}

print("tests ok")
