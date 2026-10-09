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
import { validateAnalyzeOutput, validateAnalyzeRequest } from "../_shared/validate.ts";

const SCHEMA_HINT =
  'Respond with JSON: {"insights": string[], "recommendations": string[], "warnings": string[], "confidence": number between 0 and 1}.';

Deno.serve(async (req: Request): Promise<Response> => {
  const requestId = crypto.randomUUID();
  if (req.method !== "POST") return errorResponse(405, "method_not_allowed", requestId);
  try {
    const userId = await requireUser(req);
    checkMinuteLimit(userId);
    const body = validateAnalyzeRequest(await readLimitedJson(req));
    await consumeDailyQuota(userId);

    // Trusted instructions and untrusted data are kept in separate, clearly delimited sections.
    const data = sanitizeUntrusted(JSON.stringify(body.aggregates), 16_000);
    const userText = [
      "USER FINANCIAL DATA",
      "-------------------",
      `Period: ${sanitizeUntrusted(body.periodLabel, 80)}`,
      `Currency: ${body.currency}`,
      `Aggregates (JSON): ${data}`,
      "-------------------",
      "QUESTION FROM THE USER (also untrusted text)",
      sanitizeUntrusted(body.question, 500),
    ].join("\n");

    const result = validateAnalyzeOutput(await callGemini(`${SYSTEM_RULES} ${SCHEMA_HINT}`, userText));
    return json(200, {
      requestId,
      model: modelName(),
      ...result,
      disclaimer: "This is an automated analysis of your recorded transactions, not professional financial advice.",
    });
  } catch (e) {
    if (e instanceof HttpError) return errorResponse(e.status, e.code, requestId);
    return errorResponse(500, "internal_error", requestId); // no details are ever returned or logged
  }
});
