// Liveness probe conforming to Spec §9.3.
// Reports schema version and function versions without exposing any user data.
Deno.serve((): Response =>
  new Response(
    JSON.stringify({
      status: "ok",
      schema_version: "20261024000000_wealthos_v2",
      functions_version: "3.0.0",
      model_runtime: "gemini-2.5-flash",
      timestamp: new Date().toISOString(),
    }),
    {
      status: 200,
      headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
    }
  )
);
