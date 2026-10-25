package app.privatemoney.domain.imports

enum class PaymentRail {
    UPI_P2M,
    UPI_P2A,
    UPI_P2P,
    UPI_AUTOPAY,
    IMPS,
    NEFT,
    RTGS,
    ACH_NACH,
    CARD_POS,
    ATM,
    NON_UPI
}

data class DecodedNarration(
    val cleanMerchantOrPayee: String,
    val referenceOrRrn: String?,
    val vpa: String?,
    val paymentRail: PaymentRail,
    val isRecurringMandate: Boolean,
    val isSelfTransfer: Boolean,
    val formatName: String,
)

object NarrationDecoder {

    private val UPI_MODES = setOf("P2M", "P2A", "P2P", "DR", "CR", "MANDATE", "AUTOPAY", "REV")
    private val RRN_REGEX = Regex("[0-9]{9,16}")

    fun decode(rawNarration: String): DecodedNarration {
        val s = rawNarration.trim()
        if (s.isEmpty()) {
            return DecodedNarration(
                cleanMerchantOrPayee = "Unknown",
                referenceOrRrn = null,
                vpa = null,
                paymentRail = PaymentRail.NON_UPI,
                isRecurringMandate = false,
                isSelfTransfer = false,
                formatName = "EMPTY"
            )
        }

        val lower = s.lowercase()
        val isSelf = lower.contains("self transfer") ||
            lower.contains("own a/c") ||
            lower.contains("own account") ||
            lower.contains("to own") ||
            lower.contains("to self")

        // 1. Slash UPI: UPI/<mode?>/<rrn 9-16 digits>/<payee>/...
        if (s.startsWith("UPI/", ignoreCase = true)) {
            val parts = s.split("/")
            if (parts.size >= 3) {
                var idx = 1
                var mode: String? = null
                if (parts[idx].uppercase() in UPI_MODES) {
                    mode = parts[idx].uppercase()
                    idx++
                }

                val rrnCandidate = parts.getOrNull(idx)?.trim()
                val rrn = if (rrnCandidate != null && RRN_REGEX.matches(rrnCandidate)) {
                    idx++
                    rrnCandidate
                } else null

                val payee = parts.getOrNull(idx)?.trim().orEmpty()
                val isMandate = mode == "MANDATE" || mode == "AUTOPAY" || lower.contains("mandate") || lower.contains("autopay")
                val rail = when {
                    isMandate -> PaymentRail.UPI_AUTOPAY
                    mode == "P2M" -> PaymentRail.UPI_P2M
                    mode == "P2A" -> PaymentRail.UPI_P2A
                    mode == "P2P" -> PaymentRail.UPI_P2P
                    else -> PaymentRail.UPI_P2M
                }

                val cleanPayee = cleanMerchantName(payee.ifBlank { s })
                return DecodedNarration(
                    cleanMerchantOrPayee = cleanPayee,
                    referenceOrRrn = rrn,
                    vpa = null,
                    paymentRail = rail,
                    isRecurringMandate = isMandate,
                    isSelfTransfer = isSelf,
                    formatName = "UPI_SLASH"
                )
            }
        }

        // 2. Hyphen UPI: UPI-<payee>-<vpa>-<ifsc?>-<rrn>-...
        if (s.startsWith("UPI-", ignoreCase = true)) {
            val parts = s.split("-")
            if (parts.size >= 3) {
                val payee = parts[1].trim()
                val vpa = parts.getOrNull(2)?.trim()?.takeIf { it.contains("@") }
                val rrn = parts.firstOrNull { RRN_REGEX.matches(it.trim()) }?.trim()
                val isMandate = lower.contains("mandate") || lower.contains("autopay")
                return DecodedNarration(
                    cleanMerchantOrPayee = cleanMerchantName(payee),
                    referenceOrRrn = rrn,
                    vpa = vpa,
                    paymentRail = if (isMandate) PaymentRail.UPI_AUTOPAY else PaymentRail.UPI_P2M,
                    isRecurringMandate = isMandate,
                    isSelfTransfer = isSelf,
                    formatName = "UPI_HYPHEN"
                )
            }
        }

        // 3. Slash Bank Channels: NEFT/CMS/<ref>/<payee>, IMPS/<ref>/<payee>, ACH/<payee>
        if (s.contains("/")) {
            val slashParts = s.split("/").map { it.trim() }
            val firstUpper = slashParts.first().uppercase()
            if (firstUpper in setOf("NEFT", "IMPS", "RTGS", "ACH", "NACH")) {
                var idx = 1
                while (idx < slashParts.size && slashParts[idx].uppercase() in setOf("CMS", "BIL", "INF", "P2A", "P2P", "P2M", "DR", "CR")) {
                    idx++
                }
                var ref: String? = null
                val candidateRef = slashParts.getOrNull(idx)
                if (candidateRef != null && Regex("^[A-Za-z0-9]{8,24}$").matches(candidateRef)) {
                    ref = candidateRef
                    idx++
                }
                val rawPayee = slashParts.slice(idx until slashParts.size).joinToString(" ")
                val cleanPayee = cleanMerchantName(rawPayee.ifBlank { s }, ref)
                val rail = when (firstUpper) {
                    "NEFT" -> PaymentRail.NEFT
                    "IMPS" -> PaymentRail.IMPS
                    "RTGS" -> PaymentRail.RTGS
                    "ACH", "NACH" -> PaymentRail.ACH_NACH
                    else -> PaymentRail.NON_UPI
                }
                return DecodedNarration(
                    cleanMerchantOrPayee = cleanPayee,
                    referenceOrRrn = ref ?: extractFirst12Digit(s),
                    vpa = null,
                    paymentRail = rail,
                    isRecurringMandate = (rail == PaymentRail.ACH_NACH) || lower.contains("mandate") || lower.contains("autopay"),
                    isSelfTransfer = isSelf,
                    formatName = firstUpper
                )
            }
        }

        // 4. Other IMPS / NEFT / RTGS / ACH
        if (lower.contains("neft")) {
            val ref = extractReference(s, "NEFT")
            return DecodedNarration(
                cleanMerchantOrPayee = cleanBankParticulars(s, "NEFT", ref),
                referenceOrRrn = ref,
                vpa = null,
                paymentRail = PaymentRail.NEFT,
                isRecurringMandate = false,
                isSelfTransfer = isSelf,
                formatName = "NEFT"
            )
        }
        if (lower.contains("imps")) {
            val ref = extractReference(s, "IMPS")
            return DecodedNarration(
                cleanMerchantOrPayee = cleanBankParticulars(s, "IMPS", ref),
                referenceOrRrn = ref,
                vpa = null,
                paymentRail = PaymentRail.IMPS,
                isRecurringMandate = false,
                isSelfTransfer = isSelf,
                formatName = "IMPS"
            )
        }
        if (lower.contains("rtgs")) {
            val ref = extractReference(s, "RTGS")
            return DecodedNarration(
                cleanMerchantOrPayee = cleanBankParticulars(s, "RTGS", ref),
                referenceOrRrn = ref,
                vpa = null,
                paymentRail = PaymentRail.RTGS,
                isRecurringMandate = false,
                isSelfTransfer = isSelf,
                formatName = "RTGS"
            )
        }
        if (lower.contains("ach") || lower.contains("nach") || lower.contains("ecs")) {
            val isMandate = true
            val ref = extractFirst12Digit(s)
            return DecodedNarration(
                cleanMerchantOrPayee = cleanMerchantName(s, ref),
                referenceOrRrn = ref,
                vpa = null,
                paymentRail = PaymentRail.ACH_NACH,
                isRecurringMandate = isMandate,
                isSelfTransfer = isSelf,
                formatName = "ACH_NACH"
            )
        }

        // 4. Card POS / ATM
        if (lower.startsWith("pos ") || lower.contains(" pos ") || lower.contains("card")) {
            return DecodedNarration(
                cleanMerchantOrPayee = cleanMerchantName(s.replace(Regex("(?i)^pos\\s+\\d+\\s+"), "")),
                referenceOrRrn = extractFirst12Digit(s),
                vpa = null,
                paymentRail = PaymentRail.CARD_POS,
                isRecurringMandate = false,
                isSelfTransfer = isSelf,
                formatName = "CARD_POS"
            )
        }
        if (lower.contains("atm") || lower.contains("wdl")) {
            return DecodedNarration(
                cleanMerchantOrPayee = "ATM Cash Withdrawal",
                referenceOrRrn = extractFirst12Digit(s),
                vpa = null,
                paymentRail = PaymentRail.ATM,
                isRecurringMandate = false,
                isSelfTransfer = isSelf,
                formatName = "ATM"
            )
        }

        // 5. Generic fallback
        val rrn = extractFirst12Digit(s)
        val cleanPayee = cleanMerchantName(s)
        return DecodedNarration(
            cleanMerchantOrPayee = cleanPayee,
            referenceOrRrn = rrn,
            vpa = null,
            paymentRail = PaymentRail.NON_UPI,
            isRecurringMandate = lower.contains("mandate") || lower.contains("autopay") || lower.contains("sub"),
            isSelfTransfer = isSelf,
            formatName = "GENERIC_FALLBACK"
        )
    }

    private fun extractFirst12Digit(s: String): String? {
        val m = Regex("\\b[0-9]{9,16}\\b").find(s)
        return m?.value
    }

    private fun extractReference(s: String, prefix: String): String? {
        val m = Regex("(?i)(?:$prefix)[\\s/:-]*([A-Za-z0-9]{8,24})").find(s)
        return m?.groupValues?.getOrNull(1) ?: extractFirst12Digit(s)
    }

    private fun cleanBankParticulars(s: String, channel: String, reference: String? = null): String {
        val withoutChannel = s.replace(Regex("(?i)$channel[/-]?[A-Za-z0-9]*[/-]?"), " ").trim()
        return cleanMerchantName(withoutChannel, reference)
    }

    fun cleanMerchantName(s: String, reference: String? = null): String {
        var str = s
        if (!reference.isNullOrBlank()) {
            str = str.replace(reference, " ")
        }
        val cleaned = str
            .replace(Regex("(?i)\\b(upi|pos|atm|ach|nach|neft|imps|rtgs|cms|bil|inf)\\b"), " ")
            .replace(Regex("[0-9]{8,}"), " ") // Strip long numbers
            .replace(Regex("[/\\-_:*]+"), " ")
            .trim()

        val tokens = cleaned.split(Regex("\\s+")).filter { it.length > 1 }
        return tokens.take(4).joinToString(" ").ifBlank { s.take(30) }
    }
}
