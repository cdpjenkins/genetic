package com.cdpjenkins.genetic.dudestore

data class DudeSummary(
    var name: String,
    var numGenerations: Int
) {
    constructor() : this("", 0)
}
