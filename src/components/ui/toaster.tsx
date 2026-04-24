"use client";

import { useEffect, useState } from "react";
import { OSE_TOAST_EVENT, type ToastPayload } from "@/lib/toast-client";
import { Toast, ToastDescription, ToastProvider, ToastTitle, ToastViewport } from "@/components/ui/toast";

export function Toaster() {
  const [toast, setToast] = useState<ToastPayload | null>(null);

  useEffect(() => {
    function onToast(event: Event) {
      setToast((event as CustomEvent<ToastPayload>).detail);
    }

    window.addEventListener(OSE_TOAST_EVENT, onToast);
    return () => window.removeEventListener(OSE_TOAST_EVENT, onToast);
  }, []);

  return (
    <ToastProvider>
      <Toast open={Boolean(toast)} onOpenChange={(open) => !open && setToast(null)}>
        {toast ? <ToastTitle>{toast.title}</ToastTitle> : null}
        {toast?.description ? <ToastDescription>{toast.description}</ToastDescription> : null}
      </Toast>
      <ToastViewport />
    </ToastProvider>
  );
}
