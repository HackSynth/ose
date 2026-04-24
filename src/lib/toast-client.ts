"use client";

export type ToastPayload = {
  title: string;
  description?: string;
};

export const OSE_TOAST_EVENT = "ose-toast";

export function showToast(payload: ToastPayload) {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new CustomEvent<ToastPayload>(OSE_TOAST_EVENT, { detail: payload }));
}
