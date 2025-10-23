import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EvolverSettings(
    val name: String,
    val initialGenomeSize: Int,
    val maxGenomeSize: Int,
    val minAlpha: Int,
    val maxAlpha: Int,
    val colourMutateAmount: Int,
    val pointMutateRange: Int,
    val newShapeProbabilityFactor: Double,
    val avgShapesToMutate: Double,
    val version: Int? = null
) {
    companion object {
        fun default(name: String) = EvolverSettings(
            name,
            0,
            1000,
            32,
            64,
            8,
            3,
            0.005,
            10.0,
            1
        )
    }
}
