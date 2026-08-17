"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { getToken } from "@/lib/api";
import { useEffect } from "react";

const links = [
  { href: "/", label: "Command" },
  { href: "/orders", label: "Orders" },
  { href: "/warehouses", label: "Warehouses" },
  { href: "/inventory", label: "Inventory" },
  { href: "/shipments", label: "Shipments" },
];

export function Shell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!getToken()) router.replace("/login");
  }, [router]);

  return (
    <div className="scanline min-h-screen">
      <header className="flex items-center justify-between border-b border-line px-6 py-4">
        <div>
          <p className="font-mono text-[11px] uppercase tracking-[0.28em] text-mint">FulfillX // logistics</p>
          <h1 className="text-xl font-semibold">Operations Command Center</h1>
        </div>
        <nav className="flex gap-2">
          {links.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`rounded-full px-3 py-1.5 text-sm ${
                pathname === link.href ? "bg-amber text-ink" : "text-mist hover:bg-panel"
              }`}
            >
              {link.label}
            </Link>
          ))}
        </nav>
      </header>
      <main className="px-6 py-6">{children}</main>
    </div>
  );
}
