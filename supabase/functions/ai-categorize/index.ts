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
import { validateCategorizeOutput, validateCategorizeRequest } from "../_shared/validate.ts";

const SCHEMA_HINT =
  'Suggest a category for each item using ONLY the provided category ids. Respond with JSON: {"suggestions": [{"id": string, "categoryId": string, "confidence": number between 0 and 1}]}. Omit items you are unsure about.';

Deno.serve(async (req: Request): Promise<Response> => {
  const requestId = crypto.randomUUID();
  if (req.method !== "POST") return errorResponse(405, "method_not_allowed", requestId);
  try {
    const userId = await requireUser(req);
    checkMinuteLimit(userId);
    const body = validateCategorizeRequest(await readLimitedJson(req));
    await consumeDailyQuota(userId);

    const userText = [
      "USER FINANCIAL DATA",
      "-------------------",
      "Categories: " + body.categories.map((c) => `${c.id}=${sanitizeUntrusted(c.name, 60)}`).join("; "),
      "Items:",
      ...body.items.map((i) => `${i.id}: ${sanitizeUntrusted(i.merchant, 120)}`),
      "-------------------",
    ].join("\n");

    const raw = await callGemini(`${SYSTEM_RULES} ${SCHEMA_HINT}`, userText);
    // IDs are checked against the request; suggestions are advice only and never saved by the server.
    const suggestions = validateCategorizeOutput(raw, body);
    return json(200, {
      requestId,
      model: modelName(),
      suggestions,
      disclaimer: "Suggestions only. Nothing is saved until you confirm.",
    });
  } catch (e) {
    if (e instanceof HttpError) return errorResponse(e.status, e.code, requestId);
    return errorResponse(500, "internal_error", requestId);
  }
});
