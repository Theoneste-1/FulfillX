declare global {
  interface Window {
    __FULFILLX_API_BASE__?: string;
  }
}

export function getApiBase(): string {
  if (typeof window !== "undefined" && window.__FULFILLX_API_BASE__) {
    return window.__FULFILLX_API_BASE__;
  }
  return process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";
}

export const API_BASE = getApiBase();

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("fulfillx.token");
}

export function setToken(token: string) {
  localStorage.setItem("fulfillx.token", token);
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const res = await fetch(`${getApiBase()}${path}`, { ...init, headers, cache: "no-store" });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export type Overview = {
  ordersTotal: number;
  ordersPending: number;
  ordersDelayed: number;
  ordersFailed: number;
  revenue: number;
  currency: string;
  generatedAt: string;
};

export type DailyPoint = { date: string; ordersCreated: number; ordersCompleted: number; revenue: number };
export type StatusSlice = { status: string; count: number };
export type WarehouseUtil = { warehouseId: string; code?: string; utilization: number; ordersProcessed: number };
export type Alert = { severity: string; code: string; message: string; at: string };
export type ProductRow = { sku?: string; productId: string; unitsSold: number };
