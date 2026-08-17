"use client";

import { Shell } from "@/components/Shell";
import { api } from "@/lib/api";
import { useEffect, useState } from "react";

export default function ShipmentsPage() {
  const [rows, setRows] = useState<Array<Record<string, unknown>>>([]);
  useEffect(() => {
    api<{ content: Array<Record<string, unknown>> } | Array<Record<string, unknown>>>("/api/v1/shipments?size=50")
      .then((data) => setRows(Array.isArray(data) ? data : data.content ?? []))
      .catch(() => setRows([]));
  }, []);
  return (
    <Shell>
      <h2 className="mb-4 text-lg">Shipments in motion</h2>
      <div className="overflow-hidden rounded-2xl border border-line">
        <table className="w-full text-left text-sm">
          <thead className="bg-panel font-mono text-xs uppercase text-mist">
            <tr>
              <th className="px-4 py-3">Tracking</th>
              <th>Status</th>
              <th>Carrier</th>
              <th>ETA</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={String(row.id)} className="border-t border-line">
                <td className="px-4 py-3 font-mono">{String(row.trackingNumber)}</td>
                <td>{String(row.status)}</td>
                <td>{String(row.carrier)}</td>
                <td className="text-mist">{String(row.estimatedDelivery ?? "")}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Shell>
  );
}
