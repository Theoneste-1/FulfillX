"use client";

import { Shell } from "@/components/Shell";
import { api } from "@/lib/api";
import { useEffect, useState } from "react";

type Order = {
  id: string;
  orderNumber: string;
  status: string;
  totalAmount: number;
  currency: string;
  createdAt: string;
};

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  useEffect(() => {
    api<{ content: Order[] } | Order[]>("/api/v1/orders?size=50")
      .then((data) => setOrders(Array.isArray(data) ? data : data.content ?? []))
      .catch(() => setOrders([]));
  }, []);

  return (
    <Shell>
      <h2 className="mb-4 text-lg font-medium">Live orders</h2>
      <div className="overflow-hidden rounded-2xl border border-line">
        <table className="w-full text-left text-sm">
          <thead className="bg-panel font-mono text-xs uppercase text-mist">
            <tr>
              <th className="px-4 py-3">Number</th>
              <th>Status</th>
              <th>Amount</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id} className="border-t border-line">
                <td className="px-4 py-3 font-mono">{order.orderNumber}</td>
                <td>{order.status}</td>
                <td>
                  {order.currency} {order.totalAmount}
                </td>
                <td className="text-mist">{order.createdAt}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Shell>
  );
}
