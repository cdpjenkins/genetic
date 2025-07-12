class EvolverSettings(
    val name: String,
    val initialGenomeSize: Int,
    val maxGenomeSize: Double,
    val minAlpha: Int,
    val maxAlpha: Int,
    val colourMutateAmount: Int,
    val pointMutateRange: Int,
    val newShapeProbabilityFactor: Double,
    val avgShapesToMutate: Double
) {
    companion object {
        fun default(name: String) = EvolverSettings(
            name,
            0,
            1000.0,
            32,
            64,
            8,
            3,
            0.005,
            10.0
        )
    }
}
