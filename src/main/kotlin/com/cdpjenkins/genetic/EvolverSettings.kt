class EvolverSettings(val name: String) {
    val initialGenomeSize = 0
    val maxGenomeSize = 600.0

    val masterImageFile = "cow_half_size.jpg"

    val minAlpha = 32
    val maxAlpha = 64
    val colourMutateAmount = 20
    val pointMutateRange = 10
    val newShapeProbabilityFactor = 0.01
    val avgShapesToMutate = 10.0
}
