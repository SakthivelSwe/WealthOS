// Strict validators for requests and model output. Anything unexpected is rejected, never "fixed".
import { HttpError } from "./guard.ts";

type Obj = Record<string, unknown>;

function isObj(v: unknown): v is Obj {
  return typeof v === "object" && v !== null && !Array.isArray(v);
}

export function stringList(v: unknown, maxItems: number, maxLen: number): string[] {
  if (!Array.isArray(v) || v.length > maxItems) throw new HttpError(502, "ai_bad_response");
  return v.map((s) => {
    if (typeof s !== "string" || s.length === 0 || s.length > maxLen) throw new HttpError(502, "ai_bad_response");
    return s;
  });
}

export interface AnalyzeResult {
  insights: string[];
  recommendations: string[];
  warnings: string[];
  confidence: number;
}

export function validateAnalyzeOutput(raw: unknown): AnalyzeResult {
  if (!isObj(raw)) throw new HttpError(502, "ai_bad_response");
  const c = raw.confidence;
  if (typeof c !== "number" || !Number.isFinite(c) || c < 0 || c > 1) throw new HttpError(502, "ai_bad_response");
  return {
    insights: stringList(raw.insights ?? [], 8, 600),
    recommendations: stringList(raw.recommendations ?? [], 6, 600),
    warnings: stringList(raw.warnings ?? [], 6, 400),
    confidence: c,
  };
}

export interface AnalyzeRequest {
  question: string;
  periodLabel: string;
  currency: string;
  aggregates: Obj;
}

export function validateAnalyzeRequest(raw: unknown): AnalyzeRequest {
  if (!isObj(raw)) throw new HttpError(400, "invalid_request");
  const { question, periodLabel, currency, aggregates } = raw;
  if (typeof question !== "string" || question.length < 1 || question.length > 500) throw new HttpError(400, "invalid_request");
  if (typeof periodLabel !== "string" || periodLabel.length > 80) throw new HttpError(400, "invalid_request");
  if (typeof currency !== "string" || !/^[A-Z]{3}$/.test(currency)) throw new HttpError(400, "invalid_request");
  if (!isObj(aggregates) || JSON.stringify(aggregates).length > 16_000) throw new HttpError(400, "invalid_request");
  return { question, periodLabel, currency, aggregates };
}

export interface CategorizeRequest {
  items: { id: string; merchant: string }[];
  categories: { id: string; name: string }[];
}

export function validateCategorizeRequest(raw: unknown): CategorizeRequest {
  if (!isObj(raw) || !Array.isArray(raw.items) || !Array.isArray(raw.categories)) throw new HttpError(400, "invalid_request");
  if (raw.items.length < 1 || raw.items.length > 50 || raw.categories.length < 1 || raw.categories.length > 80) {
    throw new HttpError(400, "invalid_request");
  }
  const items = raw.items.map((i) => {
    if (!isObj(i) || typeof i.id !== "string" || i.id.length > 64 || typeof i.merchant !== "string" || i.merchant.length > 120) {
      throw new HttpError(400, "invalid_request");
    }
    return { id: i.id, merchant: i.merchant };
  });
  const categories = raw.categories.map((c) => {
    if (!isObj(c) || typeof c.id !== "string" || c.id.length > 64 || typeof c.name !== "string" || c.name.length > 60) {
      throw new HttpError(400, "invalid_request");
    }
    return { id: c.id, name: c.name };
  });
  return { items, categories };
}

export interface Suggestion {
  id: string;
  categoryId: string;
  confidence: number;
}

/** Drops nothing silently: any id not present in the request invalidates the whole response. */
export function validateCategorizeOutput(raw: unknown, req: CategorizeRequest): Suggestion[] {
  if (!isObj(raw) || !Array.isArray(raw.suggestions) || raw.suggestions.length > req.items.length) {
    throw new HttpError(502, "ai_bad_response");
  }
  const itemIds = new Set(req.items.map((i) => i.id));
  const categoryIds = new Set(req.categories.map((c) => c.id));
  const seen = new Set<string>();
  return raw.suggestions.map((s) => {
    if (!isObj(s) || typeof s.id !== "string" || typeof s.categoryId !== "string" || typeof s.confidence !== "number") {
      throw new HttpError(502, "ai_bad_response");
    }
    if (!itemIds.has(s.id) || !categoryIds.has(s.categoryId) || seen.has(s.id)) throw new HttpError(502, "ai_bad_response");
    if (!Number.isFinite(s.confidence) || s.confidence < 0 || s.confidence > 1) throw new HttpError(502, "ai_bad_response");
    seen.add(s.id);
    return { id: s.id, categoryId: s.categoryId, confidence: s.confidence };
  });
}
