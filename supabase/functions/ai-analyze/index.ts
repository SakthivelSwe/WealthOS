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

const COACH_SCHEMA_HINT = `
Respond with JSON matching:
{
  "cashflowLeakDiagnosis": "string: diagnosis of cash flow, 7-day velocity, and leak patterns",
  "paydayTransferPlan": "string: step-by-step instructions for moving money in first 48h using provided amounts",
  "allocationFrameworkContext": "string: reference frameworks (e.g. 6-Jar, index SIP, 60:40) as general principles",
  "emergencyAndSevenSevenSeven": "string: assessment of 7-7-7 health rules and emergency runway status",
  "tamilSummary": "string (optional): 2-sentence summary in simple Tamil",
  "confidence": 0.95
}
`;

const MARKET_PULSE_SCHEMA_HINT = `
Respond with JSON matching:
{
  "niftyLevelAndTrend": "string: current Nifty 50 and Sensex levels and recent trend",
  "goldRateContext": "string: current 22K and 24K gold rate per gram in India/user city with historical context",
  "rbiRepoRateAndInflation": "string: current RBI policy repo rate and CPI inflation overview",
  "generalFrameworkGuidance": "string: non-personalized asset allocation commentary citing reference video rules",
  "asOfTimestamp": "string: timestamp of retrieved market data",
  "confidence": 0.9
}
`;

Deno.serve(async (req: Request): Promise<Response> => {
  const requestId = crypto.randomUUID();
  if (req.method !== "POST") return errorResponse(405, "method_not_allowed", requestId);

  try {
    const userId = await requireUser(req);
    checkMinuteLimit(userId);
    const body = await readLimitedJson(req);
    await consumeDailyQuota(userId);

    const mode = body.mode === "market_pulse" ? "market_pulse" : "coach";
    const isMarketPulse = mode === "market_pulse";

    const salaryRupees = Number(body.salaryRupees) || 0;
    const fixedBaselineRupees = Number(body.fixedBaselineRupees) || 0;
    const currentLiquidBalanceRupees = Number(body.currentLiquidBalanceRupees) || 0;
    const dailySafeUpiRupees = Number(body.dailySafeUpiRupees) || 0;
    const sevenDayBurnPercent = Number(body.sevenDayBurnPercent) || 0;
    const regretLeakTotalRupees = Number(body.regretLeakTotalRupees) || 0;
    const topLeakMerchants = Array.isArray(body.topLeakMerchants) ? body.topLeakMerchants.slice(0, 5) : [];
    const sevenSevenSevenStatus = sanitizeUntrusted(String(body.sevenSevenSevenStatus || "AMBER"), 30);
    const emergencyRunwayMonths = Number(body.emergencyRunwayMonths) || 0;
    const equityToDebtRatio = sanitizeUntrusted(String(body.equityToDebtRatio || "60:40"), 20);
    const userCity = sanitizeUntrusted(String(body.userCity || "Chennai"), 50);

    const promptData = [
      "USER FINANCIAL DATA (AGGREGATES ONLY)",
      "------------------------------------",
      `Mode: ${mode}`,
      `Salary: ₹${salaryRupees}`,
      `Fixed Baseline (Essentials): ₹${fixedBaselineRupees}`,
      `Current Liquid Balance: ₹${currentLiquidBalanceRupees}`,
      `Daily Safe Spend Allowance: ₹${dailySafeUpiRupees}`,
      `7-Day Discretionary Velocity: ${sevenDayBurnPercent}%`,
      `User-Tagged Joyless Leaks: ₹${regretLeakTotalRupees}`,
      `Emergency Runway: ${emergencyRunwayMonths} months`,
      `7-7-7 Health Status: ${sevenSevenSevenStatus}`,
      `Target Asset Split: ${equityToDebtRatio}`,
      `User City: ${userCity}`,
      `Top Merchants: ${topLeakMerchants.map((m: any) => `${m.name}: ₹${m.totalRupees || 0}`).join(", ")}`,
      "------------------------------------",
    ].join("\n");

    const schemaHint = isMarketPulse ? MARKET_PULSE_SCHEMA_HINT : COACH_SCHEMA_HINT;
    const systemPrompt = `${SYSTEM_RULES}\n${schemaHint}`;

    const geminiRes = await callGemini(systemPrompt, promptData, {
      enableGrounding: isMarketPulse,
    });

    return json(200, {
      requestId,
      model: modelName(),
      mode,
      data: geminiRes.data,
      sources: geminiRes.sources || [],
      timestamp: new Date().toISOString(),
      disclaimer: "Educational, not financial advice. No numbers were recomputed by AI.",
    });
  } catch (e) {
    if (e instanceof HttpError) return errorResponse(e.status, e.code, requestId);
    return errorResponse(500, "internal_error", requestId);
  }
});
