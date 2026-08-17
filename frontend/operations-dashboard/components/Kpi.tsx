"use client";

export function Kpi({
  label,
  value,
  tone = "default",
}: {
  label: string;
  value: string;
  tone?: "default" | "warn" | "bad" | "good";
}) {
  const color =
    tone === "warn" ? "text-amber" : tone === "bad" ? "text-rose" : tone === "good" ? "text-mint" : "text-white";
  return (
    <div className="rounded-2xl border border-line bg-panel/80 p-4">
      <p className="font-mono text-[11px] uppercase tracking-widest text-mist">{label}</p>
      <p className={`mt-2 font-mono text-3xl ${color}`}>{value}</p>
    </div>
  );
}
