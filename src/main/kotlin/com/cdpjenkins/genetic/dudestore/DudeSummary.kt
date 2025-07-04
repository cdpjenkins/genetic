package com.cdpjenkins.genetic.dudestore

data class DudeSummaryList(
    val dudes: List<DudeSummary>
)

data class DudeSummary(
    var name: String,
    var numGenerations: Int
) {
    constructor() : this("", 0)
}
