package app.privatemoney.cloud

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Versioned, tamper-evident backup envelope.
 */
data class BackupMetadata(
    val version: Int = 1,
    val appVersion: String = "0.2.0",
    val timestamp: Long = System.currentTimeMillis(),
    val accountCount: Int = 0,
    val txnCount: Int = 0,
    val goalCount: Int = 0,
    val debtCount: Int = 0,
    val chitFundCount: Int = 0,
    val recurringCount: Int = 0,
    val checksumSha256: String = "",
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("version", version)
        put("appVersion", appVersion)
        put("timestamp", timestamp)
        put("accountCount", accountCount)
        put("txnCount", txnCount)
        put("goalCount", goalCount)
        put("debtCount", debtCount)
        put("chitFundCount", chitFundCount)
        put("recurringCount", recurringCount)
        put("checksumSha256", checksumSha256)
    }

    companion object {
        fun fromJson(json: JSONObject): BackupMetadata = BackupMetadata(
            version = json.optInt("version", 1),
            appVersion = json.optString("appVersion", "0.2.0"),
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            accountCount = json.optInt("accountCount", 0),
            txnCount = json.optInt("txnCount", 0),
            goalCount = json.optInt("goalCount", 0),
            debtCount = json.optInt("debtCount", 0),
            chitFundCount = json.optInt("chitFundCount", 0),
            recurringCount = json.optInt("recurringCount", 0),
            checksumSha256 = json.optString("checksumSha256", ""),
        )
    }
}

data class BackupPayload(
    val metadata: BackupMetadata,
    val rawJsonString: String,
)

data class RestorePreview(
    val metadata: BackupMetadata,
    val accountsToRestore: Int,
    val txnsToRestore: Int,
    val existingTxnsToSkip: Int,
    val goalsToRestore: Int,
    val debtsToRestore: Int,
)
