package com.cdpjenkins.genetic.util

class WeightedSelector(val randomSource: RandomSource) {
    fun select(options: List<Pair<String, Double>>): String {
        require(options.isNotEmpty()) { "options list must not be empty" }

        val partialSums = partialSumsOf(options.map { it.second })
        val randomVal = randomSource.getRandom(partialSums.last())

        val (index, _) = partialSums.withIndex().find { (_, x) -> randomVal <= x } ?: throw IllegalArgumentException("urgh")

        return options[index].first
    }
}

fun interface RandomSource {
    fun getRandom(max: Double): Double
}

internal fun partialSumsOf(nums: List<Double>): List<Double> =
    nums.fold(
        emptyList<Double>(),
        { acc, num ->
            val newSum = (acc.lastOrNull() ?: 0.0) + num
            acc + newSum
        }
    )
