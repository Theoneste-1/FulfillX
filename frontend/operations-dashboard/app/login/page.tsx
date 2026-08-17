"use client";

import { api, setToken } from "@/lib/api";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("logistics@fulfillx.com");
  const [password, setPassword] = useState("Operator123!");
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      const tokens = await api<{ accessToken: string }>("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setToken(tokens.accessToken);
      router.push("/");
    } catch {
      setError("Login failed. Check that the gateway is running on :8080.");
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <form onSubmit={onSubmit} className="w-full max-w-md rounded-3xl border border-line bg-panel p-8">
        <p className="font-mono text-xs uppercase tracking-[0.3em] text-mint">FulfillX</p>
        <h1 className="mt-2 text-2xl font-semibold">Operator sign-in</h1>
        <p className="mt-2 text-sm text-mist">Use a seeded logistics, warehouse, or admin account.</p>
        <label className="mt-6 block text-sm text-mist">Email</label>
        <input
          className="mt-1 w-full rounded-xl border border-line bg-ink px-3 py-2 outline-none focus:border-mint"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <label className="mt-4 block text-sm text-mist">Password</label>
        <input
          type="password"
          className="mt-1 w-full rounded-xl border border-line bg-ink px-3 py-2 outline-none focus:border-mint"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {error && <p className="mt-3 text-sm text-rose">{error}</p>}
        <button className="mt-6 w-full rounded-xl bg-amber py-2 font-medium text-ink">Enter command center</button>
      </form>
    </div>
  );
}
