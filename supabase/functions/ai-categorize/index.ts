import {
  checkMinuteLimit,
  consumeDailyQuota,
  errorResponse,
  HttpError,
  json,
  readLimitedJson,
  requireUser,
} from "../_shared/guard.ts";
import { callGemini, modelName, sanitizeUntrusted, SYSTEM_RULES } from "../_shared/gemini.ts";

const VALID_JARS = new Set(["NEEDS", "FREEDOM_SIP", "EDUCATION", "PLAY", "LONG_TERM", "GIVING"]);
const VALID_TAGS = new Set(["NEED", "PLANNED_WANT", "REGRET_LEAK", "UNPLANNED_TRANSFER"]);

const CATEGORIZE_SCHEMA = `
Respond with JSON matching:
{
  "suggestions": [
    {
      "id": "string (matching input id)",
      "category": "string (suggested category name, e.g. Groceries, Dining, Fuel, Utilities)",
      "jarBucket": "NEEDS | FREEDOM_SIP | EDUCATION | PLAY | LONG_TERM | GIVING",
      "suggestedTag": "NEED | PLANNED_WANT | REGRET_LEAK | UNPLANNED_TRANSFER",
      "isSubscriptionMandate": boolean,
      "cleanMerchantName": "string (clean brand/merchant name)",
      "confidence": number between 0 and 1
    }
  ]
}
Omit items you are uncertain about. Never invent IDs not present in the input.
`;

Deno.serve(async (req: Request): Promise<Response> => {
  const requestId = crypto.randomUUID();
  if (req.method !== "POST") return errorResponse(405, "method_not_allowed", requestId);

  try {
    const userId = await requireUser(req);
    checkMinuteLimit(userId);
    const body = await readLimitedJson(req);
    await consumeDailyQuota(userId);

    const rawItems = Array.isArray(body.items) ? body.items.slice(0, 30) : [];
    if (rawItems.length === 0) {
      return json(200, { requestId, suggestions: [] });
    }

    const items = rawItems.map((item: any, idx: number) => ({
      id: String(item.id || `item_${idx}`),
      merchantString: sanitizeUntrusted(String(item.merchantString || item.merchant || ""), 120),
      amountBucket: sanitizeUntrusted(String(item.amountBucket || "SMALL"), 20),
      rail: sanitizeUntrusted(String(item.rail || "UPI_P2M"), 20),
    }));

    const userText = [
      "USER FINANCIAL DATA (MERCHANT STRINGS ONLY - NO AMOUNTS OR PERSONAL NAMES)",
      "--------------------------------------------------------------------------",
      ...items.map((i) => `ID: ${i.id} | Merchant: "${i.merchantString}" | Bucket: ${i.amountBucket} | Rail: ${i.rail}`),
      "--------------------------------------------------------------------------",
    ].join("\n");

    const geminiRes = await callGemini(`${SYSTEM_RULES}\n${CATEGORIZE_SCHEMA}`, userText);
    const parsedData: any = geminiRes.data;

    // Validate against enum sets and discard invalid items (Spec §9.1)
    const validSuggestions = [];
    const itemIds = new Set(items.map((i) => i.id));

    if (Array.isArray(parsedData?.suggestions)) {
      for (const s of parsedData.suggestions) {
        if (!s || !itemIds.has(s.id)) continue;
        const jarBucket = String(s.jarBucket || "PLAY").toUpperCase();
        const suggestedTag = String(s.suggestedTag || "PLANNED_WANT").toUpperCase();

        validSuggestions.push({
          id: s.id,
          category: String(s.category || "General Spend").slice(0, 50),
          jarBucket: VALID_JARS.has(jarBucket) ? jarBucket : "PLAY",
          suggestedTag: VALID_TAGS.has(suggestedTag) ? suggestedTag : "PLANNED_WANT",
          isSubscriptionMandate: Boolean(s.isSubscriptionMandate),
          cleanMerchantName: String(s.cleanMerchantName || s.category || "Merchant").slice(0, 60),
          confidence: typeof s.confidence === "number" ? Math.min(1, Math.max(0, s.confidence)) : 0.8,
        });
      }
    }

    return json(200, {
      requestId,
      model: modelName(),
      suggestions: validSuggestions,
      disclaimer: "Suggestions only. Regret leak is user judgement. Nothing is saved until confirmed.",
    });
  } catch (e) {
    if (e instanceof HttpError) return errorResponse(e.status, e.code, requestId);
    return errorResponse(500, "internal_error", requestId);
  }
});
