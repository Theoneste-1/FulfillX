"use client";

import { Shell } from "@/components/Shell";
import { api } from "@/lib/api";
import { useEffect, useState } from "react";

export default function InventoryPage() {
  const [rows, setRows] = useState<Array<Record<string, unknown>>>([]);
  useEffect(() => {
    api<{ content: Array<Record<string, unknown>> } | Array<Record<string, unknown>>>("/api/v1/inventory?size=50")
      .then((data) => setRows(Array.isArray(data) ? data : data.content ?? []))
      .catch(() => setRows([]));
  }, []);
  return (
    <Shell>
      <h2 className="mb-4 text-lg">Stock by warehouse</h2>
      <div className="overflow-hidden rounded-2xl border border-line">
        <table className="w-full text-left text-sm">
          <thead className="bg-panel font-mono text-xs uppercase text-mist">
            <tr>
              <th className="px-4 py-3">SKU</th>
              <th>On hand</th>
              <th>Reserved</th>
              <th>Available</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, i) => (
              <tr key={i} className="border-t border-line">
                <td className="px-4 py-3 font-mono">{String(row.sku)}</td>
                <td>{String(row.quantityOnHand)}</td>
                <td>{String(row.quantityReserved)}</td>
                <td className="text-mint">{String(row.available)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Shell>
  );
}
