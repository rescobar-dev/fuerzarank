package cl.fuerzarank.core

import java.time.LocalDate

data class PersonalRecord(
    val id: String,
    val profileId: String,
    val exerciseId: String,
    val loadKg: Double,
    val reps: Int,
    val date: LocalDate,
    val bodyweightKg: Double,
    val oneRmKg: Double,
    val oneRmIsEstimate: Boolean,
    val level: StrengthLevel?,
    val technique: String = "estándar",
    val notes: String? = null,
    val videoRef: String? = null,
    val isDemo: Boolean = false
)

enum class PrSort { LOAD, ONE_RM }

object PrService {
    fun create(
        id: String, profileId: String, exerciseId: String, loadKg: Double, reps: Int,
        date: LocalDate, bodyweightKg: Double, technique: String = "estándar",
        notes: String? = null, isDemo: Boolean = false
    ): PersonalRecord {
        val rm = StrengthCalculator.estimateOneRm(loadKg, reps)
        val std = StrengthCalculator.provisionalStandards[exerciseId]
        val level = std?.let { StrengthCalculator.classify(StrengthCalculator.relativeStrength(rm.kg, bodyweightKg), it) }
        return PersonalRecord(id, profileId, exerciseId, loadKg, reps, date, bodyweightKg, rm.kg, rm.isEstimate, level, technique, notes, null, isDemo)
    }

    /** Top 3 por usuario y ejercicio. Nunca modifica ni borra el historial. */
    fun top3(all: List<PersonalRecord>, profileId: String, exerciseId: String, sort: PrSort): List<PersonalRecord> =
        all.filter { it.profileId == profileId && it.exerciseId == exerciseId }
            .sortedWith(
                when (sort) {
                    PrSort.LOAD -> compareByDescending<PersonalRecord> { it.loadKg }.thenByDescending { it.reps }
                    PrSort.ONE_RM -> compareByDescending { it.oneRmKg }
                }.thenByDescending { it.date }
            ).take(3)

    /** ¿Es un PR nuevo respecto al historial previo? (por 1RM) */
    fun isNewPr(history: List<PersonalRecord>, candidate: PersonalRecord): Boolean =
        history.filter { it.profileId == candidate.profileId && it.exerciseId == candidate.exerciseId }
            .none { it.oneRmKg >= candidate.oneRmKg }

    fun demoBenchPress(profileId: String) = create(
        "demo-bench", profileId, "press_banca", 140.0, 1, LocalDate.of(2026, 11, 15), 90.0,
        notes = "Dato de demostración editable, no verificado", isDemo = true
    )
}
