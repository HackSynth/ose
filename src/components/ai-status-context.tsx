"use client";

import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";

export type AIStatus = { configured: boolean; provider: string | null; model: string | null; endpoint: string | null; source: "user" | "env" | null };

const DEFAULT: AIStatus = { configured: false, provider: null, model: null, endpoint: null, source: null };

type AIStatusContextValue = AIStatus & { refresh: () => Promise<void> };

const AIStatusContext = createContext<AIStatusContextValue>({ ...DEFAULT, refresh: async () => {} });

export function AIStatusProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AIStatus>(DEFAULT);

  const refresh = useCallback(async () => {
    try {
      const response = await fetch("/api/ai/status", { cache: "no-store" });
      if (!response.ok) return;
      const data = (await response.json()) as AIStatus;
      setStatus(data);
    } catch {
      // ignore transient failures
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return <AIStatusContext.Provider value={{ ...status, refresh }}>{children}</AIStatusContext.Provider>;
}

export function useAIStatus(): AIStatusContextValue {
  return useContext(AIStatusContext);
}
