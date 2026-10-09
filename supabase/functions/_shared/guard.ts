// Shared safety rails for AI Edge Functions. No secrets are ever logged or returned.
import { createClient } from "npm:@supabase/supabase-js@2";

export const MAX_BODY_BYTES = 32 * 1024;

const JSON_HEADERS = { "Content-Type": "application/json", "Cache-Control": "no-store" };

export function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: JSON_HEADERS });
}

/** Generic, safe error body. Never include exception text, stack traces or upstream payloads. */
export function errorResponse(status: number, code: string, requestId: string): Response {
  return json(status, { requestId, error: code });
}

export async function readLimitedJson(req: Request): Promise<unknown> {
  const declared = Number(req.headers.get("content-length") ?? "0");
  if (declared > MAX_BODY_BYTES) throw new HttpError(413, "payload_too_large");
  const reader = req.body?.getReader();
  if (!reader) throw new HttpError(400, "empty_body");
  const chunks: Uint8Array[] = [];
  let total = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > MAX_BODY_BYTES) throw new HttpError(413, "payload_too_large");
    chunks.push(value);
  }
  const merged = new Uint8Array(total);
  let offset = 0;
  for (const c of chunks) {
    merged.set(c, offset);
    offset += c.byteLength;
  }
  try {
    return JSON.parse(new TextDecoder().decode(merged));
  } catch {
    throw new HttpError(400, "invalid_json");
  }
}

export class HttpError extends Error {
  constructor(public status: number, public code: string) {
    super(code);
  }
}

/** Validates the caller's Supabase JWT with the auth server and returns the user id. */
export async function requireUser(req: Request): Promise<string> {
  const header = req.headers.get("authorization") ?? "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!token) throw new HttpError(401, "unauthorized");
  const client = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data, error } = await client.auth.getUser(token);
  if (error || !data.user) throw new HttpError(401, "unauthorized");
  return data.user.id;
}

// Per-isolate minute limiter: cheap first line of defence. The daily quota in Postgres is authoritative.
const perMinute = new Map<string, { windowStart: number; count: number }>();
const PER_MINUTE_LIMIT = 10;

export function checkMinuteLimit(userId: string, now = Date.now()): void {
  const entry = perMinute.get(userId);
  if (!entry || now - entry.windowStart >= 60_000) {
    perMinute.set(userId, { windowStart: now, count: 1 });
    return;
  }
  entry.count += 1;
  if (entry.count > PER_MINUTE_LIMIT) throw new HttpError(429, "rate_limited");
}

/** Atomically consumes one unit of the user's daily AI quota (service role only). */
export async function consumeDailyQuota(userId: string): Promise<void> {
  const limit = Number(Deno.env.get("AI_DAILY_LIMIT") ?? "50");
  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data, error } = await admin.rpc("consume_ai_quota", { p_user: userId, p_limit: limit });
  if (error) throw new HttpError(503, "quota_unavailable");
  if (data !== true) throw new HttpError(429, "daily_limit_reached");
}
