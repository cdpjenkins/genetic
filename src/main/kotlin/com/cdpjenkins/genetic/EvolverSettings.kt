class EvolverSettings(val name: String) {
    val initialGenomeSize = 0
    val maxGenomeSize = 1000.0

    val masterImageFile = "cow_half_size.jpg"

    val minAlpha = 32
    val maxAlpha = 64
    val colourMutateAmount = 8
    val pointMutateRange = 3
    val newShapeProbabilityFactor = 0.005
    val avgShapesToMutate = 10.0
}
