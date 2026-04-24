"use client";

import { useEffect, useRef } from "react";

export function SessionGuard() {
  const redirectingRef = useRef(false);

  useEffect(() => {
    async function ping() {
      if (redirectingRef.current) return;
      try {
        const response = await fetch("/api/auth/ping", { cache: "no-store" });
        if (response.status === 401 && !redirectingRef.current) {
          redirectingRef.current = true;
          const callbackUrl = window.location.pathname + window.location.search;
          window.location.href = `/login?expired=1&callbackUrl=${encodeURIComponent(callbackUrl)}`;
        }
      } catch {
        // offline / transient — ignore
      }
    }
    function onVisibility() {
      if (document.visibilityState === "visible") ping();
    }
    document.addEventListener("visibilitychange", onVisibility);
    const interval = window.setInterval(ping, 5 * 60 * 1000);
    return () => {
      document.removeEventListener("visibilitychange", onVisibility);
      window.clearInterval(interval);
    };
  }, []);

  return null;
}
