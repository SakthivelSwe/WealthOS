package app.privatemoney.domain.imports

import java.security.MessageDigest
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DuplicateCandidate(
    val date: LocalDate,
    val amountMinor: Long,
    val merchant: String?,
    val reference: String?,
    val accountId: String?,
    val description: String? = null,
)

enum class Likelihood { NONE, POSSIBLE, LIKELY }

/**
 * Multi-signal duplicate detection. It only ever labels a suspicion; nothing is deleted or skipped
 * without the user confirming it.
 */
object DuplicateDetector {

    fun compare(a: DuplicateCandidate, b: DuplicateCandidate): Likelihood {
        if (a.amountMinor != b.amountMinor) return Likelihood.NONE
        if (a.accountId != null && b.accountId != null && a.accountId != b.accountId) return Likelihood.NONE

        val refA = normalize(a.reference)
        val refB = normalize(b.reference)
        if (refA.isNotEmpty() && refA == refB) return Likelihood.LIKELY

        val dayGap = kotlin.math.abs(ChronoUnit.DAYS.between(a.date, b.date))
        if (dayGap > 1) return Likelihood.NONE

        val textA = normalize(a.merchant ?: a.description)
        val textB = normalize(b.merchant ?: b.description)
        if (textA.isEmpty() || textB.isEmpty()) {
            return if (dayGap == 0L) Likelihood.POSSIBLE else Likelihood.NONE
        }
        val similarity = similarity(textA, textB)
        return when {
            dayGap == 0L && similarity >= 0.8 -> Likelihood.LIKELY
            similarity >= 0.6 -> Likelihood.POSSIBLE
            else -> Likelihood.NONE
        }
    }

    fun normalize(text: String?): String =
        text.orEmpty().lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    /** 1.0 for identical strings, 0.0 for completely different (normalised Levenshtein). */
    fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0
        return 1.0 - levenshtein(a, b).toDouble() / maxLen
    }

    private fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    /** Stable fingerprint of a statement row so re-importing the same file is detected. */
    fun rowFingerprint(accountId: String, c: DuplicateCandidate): String {
        val canonical = listOf(accountId, c.date.toString(), c.amountMinor.toString(), normalize(c.merchant ?: c.description), normalize(c.reference))
            .joinToString("|")
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
