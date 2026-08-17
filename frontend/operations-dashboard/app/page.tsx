"use client";

import { Shell } from "@/components/Shell";
import { Kpi } from "@/components/Kpi";
import { api, type Alert, type DailyPoint, type Overview, type StatusSlice, type WarehouseUtil } from "@/lib/api";
import { useEffect, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export default function CommandPage() {
  const [overview, setOverview] = useState<Overview | null>(null);
  const [daily, setDaily] = useState<DailyPoint[]>([]);
  const [status, setStatus] = useState<StatusSlice[]>([]);
  const [warehouses, setWarehouses] = useState<WarehouseUtil[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    try {
      const [o, d, s, w, a] = await Promise.all([
        api<Overview>("/api/v1/analytics/overview"),
        api<DailyPoint[] | { content: DailyPoint[] }>("/api/v1/analytics/orders/daily"),
        api<StatusSlice[] | { content?: StatusSlice[]; statuses?: StatusSlice[] }>(
          "/api/v1/analytics/orders/by-status"
        ),
        api<WarehouseUtil[] | { content: WarehouseUtil[] }>("/api/v1/analytics/warehouses/utilization"),
        api<{ alerts: Alert[] } | Alert[]>("/api/v1/analytics/alerts"),
      ]);
      setOverview(o);
      setDaily(Array.isArray(d) ? d : d.content ?? []);
      setStatus(Array.isArray(s) ? s : s.statuses ?? s.content ?? []);
      setWarehouses(Array.isArray(w) ? w : w.content ?? []);
      setAlerts(Array.isArray(a) ? a : a.alerts ?? []);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load analytics");
    }
  }

  useEffect(() => {
    load();
    const id = setInterval(load, 8000);
    return () => clearInterval(id);
  }, []);

  return (
    <Shell>
      {error && (
        <p className="mb-4 rounded-xl border border-rose/40 bg-rose/10 px-4 py-3 text-sm text-rose">
          {error}. Sign in as logistics@fulfillx.com / Operator123!
        </p>
      )}
      <section className="grid grid-cols-2 gap-4 md:grid-cols-5">
        <Kpi label="Orders" value={fmt(overview?.ordersTotal)} />
        <Kpi label="Pending" value={fmt(overview?.ordersPending)} tone="warn" />
        <Kpi label="Delayed" value={fmt(overview?.ordersDelayed)} tone="bad" />
        <Kpi label="Failed" value={fmt(overview?.ordersFailed)} tone="bad" />
        <Kpi label="Revenue" value={money(overview?.revenue, overview?.currency)} tone="good" />
      </section>

      <section className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="rounded-2xl border border-line bg-panel p-4">
          <h2 className="mb-4 font-medium">Orders / day</h2>
          <div className="h-64">
            <ResponsiveContainer>
              <LineChart data={daily}>
                <CartesianGrid stroke="#1c2a3f" />
                <XAxis dataKey="date" stroke="#9fb0c8" tick={{ fontSize: 11 }} />
                <YAxis stroke="#9fb0c8" tick={{ fontSize: 11 }} />
                <Tooltip contentStyle={{ background: "#0e1624", border: "1px solid #1c2a3f" }} />
                <Line type="monotone" dataKey="ordersCreated" stroke="#3ee0c5" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="ordersCompleted" stroke="#f5b942" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
        <div className="rounded-2xl border border-line bg-panel p-4">
          <h2 className="mb-4 font-medium">Orders by status</h2>
          <div className="h-64">
            <ResponsiveContainer>
              <BarChart data={status} layout="vertical">
                <CartesianGrid stroke="#1c2a3f" />
                <XAxis type="number" stroke="#9fb0c8" />
                <YAxis type="category" dataKey="status" stroke="#9fb0c8" width={120} tick={{ fontSize: 11 }} />
                <Tooltip contentStyle={{ background: "#0e1624", border: "1px solid #1c2a3f" }} />
                <Bar dataKey="count" fill="#f5b942" radius={[0, 6, 6, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </section>

      <section className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="rounded-2xl border border-line bg-panel p-4">
          <h2 className="mb-4 font-medium">Warehouse utilization</h2>
          <ul className="space-y-3">
            {warehouses.map((wh) => (
              <li key={wh.warehouseId}>
                <div className="mb-1 flex justify-between font-mono text-sm">
                  <span>{wh.code ?? wh.warehouseId.slice(0, 8)}</span>
                  <span>{Math.round((wh.utilization ?? 0) * 100)}%</span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-ink">
                  <div
                    className="h-full bg-mint"
                    style={{ width: `${Math.min(100, (wh.utilization ?? 0) * 100)}%` }}
                  />
                </div>
              </li>
            ))}
            {warehouses.length === 0 && <p className="text-sm text-mist">Waiting for warehouse events…</p>}
          </ul>
        </div>
        <div className="rounded-2xl border border-amber/30 bg-panel p-4">
          <h2 className="mb-4 text-amber">Alerts</h2>
          <ul className="space-y-3">
            {alerts.map((alert, i) => (
              <li key={i} className="border-b border-line pb-2 text-sm last:border-0">
                <span className="font-mono text-xs text-rose">{alert.severity}</span>
                <p>{alert.message}</p>
              </li>
            ))}
            {alerts.length === 0 && <p className="text-sm text-mist">No open operational alerts.</p>}
          </ul>
        </div>
      </section>
    </Shell>
  );
}

function fmt(n?: number) {
  return (n ?? 0).toLocaleString();
}

function money(n?: number, currency = "USD") {
  return new Intl.NumberFormat("en-US", { style: "currency", currency, maximumFractionDigits: 0 }).format(n ?? 0);
}
