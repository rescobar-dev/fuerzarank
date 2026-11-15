package cl.fuerzarank.core

enum class StrengthLevel(val label: String, val colorHex: String, val form: PetForm) {
    PESIMO("PÉSIMO", "#4A4A4A", PetForm.BEBE),
    BAJO("BAJO", "#B0B0B0", PetForm.BEBE),
    PROMEDIO("PROMEDIO", "#3FAE49", PetForm.ATLETICO),
    ALTO("ALTO", "#2F7BE5", PetForm.ATLETICO),
    MUY_ALTO("MUY ALTO", "#8E44AD", PetForm.GUERRERO),
    PRO("PRO", "#F28C28", PetForm.GUERRERO),
    ELITE("ÉLITE", "#D4AF37", PetForm.LEGENDARIO),
    DIOS("DIOS", "#D62828", PetForm.LEGENDARIO)
}

enum class PetForm { BEBE, ATLETICO, GUERRERO, LEGENDARIO }
enum class PetSpecies { GORILA, LEON, LOBO }

enum class OneRmFormula { EPLEY, BRZYCKI }

data class OneRm(val kg: Double, val isEstimate: Boolean)

/** Estándar por ejercicio: umbrales de fuerza relativa (carga 1RM / peso corporal) para los niveles 2..8. */
data class ExerciseStandard(val exerciseId: String, val thresholds: List<Double>, val provisional: Boolean = true) {
    init { require(thresholds.size == 7 && thresholds == thresholds.sorted()) }
}

object StrengthCalculator {
    /** reps == 1 -> máximo real (no estimado). Más de 12 reps: demasiado impreciso, se rechaza. */
    fun estimateOneRm(loadKg: Double, reps: Int, formula: OneRmFormula = OneRmFormula.EPLEY): OneRm {
        require(loadKg > 0) { "La carga debe ser positiva" }
        require(reps in 1..12) { "Repeticiones fuera de rango fiable (1-12)" }
        if (reps == 1) return OneRm(loadKg, isEstimate = false)
        val v = when (formula) {
            OneRmFormula.EPLEY -> loadKg * (1 + reps / 30.0)
            OneRmFormula.BRZYCKI -> loadKg * 36.0 / (37 - reps)
        }
        return OneRm(v, isEstimate = true)
    }

    fun relativeStrength(oneRmKg: Double, bodyweightKg: Double): Double {
        require(bodyweightKg > 0)
        return oneRmKg / bodyweightKg
    }

    fun classify(relative: Double, standard: ExerciseStandard): StrengthLevel {
        val idx = standard.thresholds.count { relative >= it }
        return StrengthLevel.entries[idx]
    }

    /** Estándares PROVISIONALES de ejemplo (no validados científicamente). Deben revisarse antes de publicar. */
    val provisionalStandards: Map<String, ExerciseStandard> = mapOf(
        "press_banca" to ExerciseStandard("press_banca", listOf(0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0)),
        "sentadilla" to ExerciseStandard("sentadilla", listOf(0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5)),
        "peso_muerto" to ExerciseStandard("peso_muerto", listOf(1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0))
    )

    /** Nivel general = mediana de los niveles por ejercicio (aprox. simple). */
    fun overall(levels: List<StrengthLevel>): StrengthLevel? =
        levels.sortedBy { it.ordinal }.let { if (it.isEmpty()) null else it[it.size / 2] }
}
