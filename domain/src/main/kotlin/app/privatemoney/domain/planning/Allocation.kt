package app.privatemoney.domain.planning

enum class FrameworkType {
    SIX_JAR,
    THREE_BOX,
    FIFTY_THIRTY_TWENTY,
    CUSTOM
}

data class FrameworkDefinition(
    val type: FrameworkType,
    val order: List<String>,
    val bps: Map<String, Int>,
    val emergencyJar: String,
    val sinkJar: String,
    val reduceOrder: List<String>,
    val savingsJars: List<String>,
    val spendJar: String?,
) {
    init {
        require(order.isNotEmpty()) { "Framework order must not be empty" }
        require(bps.values.sum() == 10000) { "Basis points must sum to 10000 (got ${bps.values.sum()})" }
        require(emergencyJar in order) { "Emergency jar must be in order list" }
        require(sinkJar in order) { "Sink jar must be in order list" }
        require(reduceOrder.all { it in order }) { "All reduce order jars must be in order list" }
        require(savingsJars.all { it in order }) { "All savings jars must be in order list" }
        if (spendJar != null) {
            require(spendJar in order) { "Spend jar must be in order list" }
        }
    }

    companion object {
        val SIX_JAR = FrameworkDefinition(
            type = FrameworkType.SIX_JAR,
            order = listOf("NEEDS", "FREEDOM_SIP", "EDUCATION", "PLAY", "LONG_TERM", "GIVING"),
            bps = mapOf(
                "NEEDS" to 5500,
                "FREEDOM_SIP" to 1000,
                "EDUCATION" to 1000,
                "PLAY" to 1000,
                "LONG_TERM" to 1000,
                "GIVING" to 500
            ),
            emergencyJar = "LONG_TERM",
            sinkJar = "FREEDOM_SIP",
            reduceOrder = listOf("PLAY", "EDUCATION", "GIVING"),
            savingsJars = listOf("FREEDOM_SIP", "LONG_TERM"),
            spendJar = "PLAY"
        )

        val THREE_BOX = FrameworkDefinition(
            type = FrameworkType.THREE_BOX,
            order = listOf("NEEDS", "EMERGENCY", "BUFFER", "MEDIUM_TERM", "LONG_TERM_SIP"),
            bps = mapOf(
                "NEEDS" to 5000,
                "EMERGENCY" to 1000,
                "BUFFER" to 1000,
                "MEDIUM_TERM" to 1000,
                "LONG_TERM_SIP" to 2000
            ),
            emergencyJar = "EMERGENCY",
            sinkJar = "LONG_TERM_SIP",
            reduceOrder = listOf("MEDIUM_TERM", "BUFFER"),
            savingsJars = listOf("EMERGENCY", "MEDIUM_TERM", "LONG_TERM_SIP"),
            spendJar = null
        )

        val FIFTY_THIRTY_TWENTY = FrameworkDefinition(
            type = FrameworkType.FIFTY_THIRTY_TWENTY,
            order = listOf("NEEDS", "WANTS", "SAVINGS"),
            bps = mapOf(
                "NEEDS" to 5000,
                "WANTS" to 3000,
                "SAVINGS" to 2000
            ),
            emergencyJar = "SAVINGS",
            sinkJar = "SAVINGS",
            reduceOrder = listOf("WANTS"),
            savingsJars = listOf("SAVINGS"),
            spendJar = "WANTS"
        )

        fun fromType(type: FrameworkType): FrameworkDefinition = when (type) {
            FrameworkType.SIX_JAR -> SIX_JAR
            FrameworkType.THREE_BOX -> THREE_BOX
            FrameworkType.FIFTY_THIRTY_TWENTY -> FIFTY_THIRTY_TWENTY
            FrameworkType.CUSTOM -> SIX_JAR
        }
    }
}

data class AllocationResult(
    val jars: Map<String, Long>,
    val flags: List<String>,
    val dailySafePaise: Long,
    val weeklySafePaise: Long,
)

object AllocationEngine {

    /**
     * Largest-remainder allocation. Produces integer amounts summing exactly to total.
     * Ties broken by jar order.
     */
    fun largestRemainder(total: Long, bps: Map<String, Int>, order: List<String>): Map<String, Long> {
        val base = mutableMapOf<String, Long>()
        val rem = mutableMapOf<String, Long>()
        for (j in order) {
            val b = (bps[j] ?: 0).toLong()
            base[j] = (total * b) / 10000L
            rem[j] = (total * b) % 10000L
        }
        val allocatedSum = base.values.sum()
        val left = (total - allocatedSum).toInt()
        val sortedJars = order.sortedWith(
            compareByDescending<String> { rem[it] ?: 0L }
                .thenBy { order.indexOf(it) }
        )
        for (i in 0 until left) {
            val j = sortedJars[i]
            base[j] = (base[j] ?: 0L) + 1L
        }
        return base
    }

    /**
     * Deterministic salary allocation conforming to WEALTHOS_FINAL_SPEC.md v3 §5.3 / Appendix B.
     */
    fun allocate(
        salaryPaise: Long,
        framework: FrameworkDefinition = FrameworkDefinition.SIX_JAR,
        baselinePaise: Long = 0L,
        emiPaise: Long = 0L,
        emergencyBalancePaise: Long = 0L,
        emergencyTargetPaise: Long = baselinePaise * 7L,
        creepLock: Boolean = true,
        bufferPaise: Long = 150000L,
        shiftPlayToLongTerm: Boolean = false,
        daysInCycle: Int = 30
    ): AllocationResult {
        require(salaryPaise >= 0L) { "Salary must be non-negative" }
        require(daysInCycle >= 1) { "Days in cycle must be at least 1" }

        val order = framework.order
        val bps = framework.bps.toMutableMap()
        val mandatory = baselinePaise + emiPaise

        if (mandatory >= salaryPaise) {
            val jars = order.associateWith { 0L }.toMutableMap()
            jars["NEEDS"] = salaryPaise
            return AllocationResult(jars, listOf("SHORTFALL"), 0L, 0L)
        }

        if (shiftPlayToLongTerm && framework.type == FrameworkType.SIX_JAR) {
            bps["PLAY"] = (bps["PLAY"] ?: 1000) - 500
            bps["LONG_TERM"] = (bps["LONG_TERM"] ?: 1000) + 500
        }

        val shares = largestRemainder(salaryPaise, bps, order)
        val needsShare = shares["NEEDS"] ?: 0L
        val needs = if (creepLock) {
            maxOf(mandatory, minOf(needsShare, mandatory + bufferPaise))
        } else {
            maxOf(mandatory, needsShare)
        }

        val jars = shares.toMutableMap()
        jars["NEEDS"] = needs
        val flags = mutableListOf<String>()
        val full = emergencyBalancePaise >= emergencyTargetPaise
        val diff = needsShare - needs

        if (diff > 0L) {
            val destJar = if (full) framework.sinkJar else framework.emergencyJar
            jars[destJar] = (jars[destJar] ?: 0L) + diff
        } else if (diff < 0L) {
            var deficit = -diff
            for (j in framework.reduceOrder) {
                val cur = jars[j] ?: 0L
                val t = minOf(cur, deficit)
                jars[j] = cur - t
                deficit -= t
            }
            if (deficit > 0L) {
                flags.add("SAVINGS_REDUCED")
                val nonNeedsReversed = order.filter { it != "NEEDS" }.reversed()
                for (j in nonNeedsReversed) {
                    val cur = jars[j] ?: 0L
                    val t = minOf(cur, deficit)
                    jars[j] = cur - t
                    deficit -= t
                }
            }
        }

        if (full && framework.emergencyJar != framework.sinkJar) {
            val emergencyAmt = jars[framework.emergencyJar] ?: 0L
            jars[framework.sinkJar] = (jars[framework.sinkJar] ?: 0L) + emergencyAmt
            jars[framework.emergencyJar] = 0L
        }

        val totalSavings = framework.savingsJars.sumOf { jars[it] ?: 0L }
        if (totalSavings * 100L < 7L * salaryPaise) {
            flags.add("BELOW_SAVINGS_FLOOR")
        }

        var daily = 0L
        var weekly = 0L
        val spendJar = framework.spendJar
        if (spendJar != null) {
            val spendAmount = jars[spendJar] ?: 0L
            daily = spendAmount / daysInCycle.toLong()
            weekly = (spendAmount * 7L) / daysInCycle.toLong()
            if (spendAmount == 0L) {
                flags.add("SPEND_JAR_EMPTY")
            }
        }

        // Invariants
        check(jars.values.all { it >= 0L }) { "Invariant violated: negative jar in $jars" }
        check(jars.values.sum() == salaryPaise) { "Invariant violated: jars sum ${jars.values.sum()} != salary $salaryPaise" }
        check(jars["NEEDS"]!! >= mandatory) { "Invariant violated: NEEDS ${jars["NEEDS"]} < mandatory $mandatory" }

        return AllocationResult(jars, flags, daily, weekly)
    }
}
