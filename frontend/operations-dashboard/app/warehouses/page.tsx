"use client";

import { Shell } from "@/components/Shell";
import { api } from "@/lib/api";
import { useEffect, useState } from "react";

export default function WarehousesPage() {
  const [rows, setRows] = useState<Array<Record<string, unknown>>>([]);
  useEffect(() => {
    api<{ content: Array<Record<string, unknown>> } | Array<Record<string, unknown>>>("/api/v1/warehouses")
      .then((data) => setRows(Array.isArray(data) ? data : data.content ?? []))
      .catch(() => setRows([]));
  }, []);
  return (
    <Shell>
      <h2 className="mb-4 text-lg">Warehouse network</h2>
      <div className="grid gap-4 md:grid-cols-3">
        {rows.map((row) => (
          <article key={String(row.id)} className="rounded-2xl border border-line bg-panel p-4">
            <p className="font-mono text-mint">{String(row.code)}</p>
            <h3 className="text-lg">{String(row.name)}</h3>
            <p className="text-sm text-mist">
              {String(row.city)}, {String(row.country)} · {String(row.status)}
            </p>
            <p className="mt-2 font-mono text-sm">
              workload {String(row.currentWorkload ?? 0)} / {String(row.capacity ?? 0)}
            </p>
          </article>
        ))}
      </div>
    </Shell>
  );
}
