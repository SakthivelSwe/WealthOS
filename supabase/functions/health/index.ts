// Liveness probe. Public by design, reveals nothing about configuration or users.
Deno.serve((): Response =>
  new Response(JSON.stringify({ status: "ok" }), {
    status: 200,
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
  })
);
