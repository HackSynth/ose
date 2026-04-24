export function isTauri(): boolean {
  return typeof window !== "undefined" && "__TAURI_INTERNALS__" in window;
}

export async function openExternal(url: string): Promise<void> {
  if (isTauri()) {
    const { open } = await import("@tauri-apps/plugin-shell");
    await open(url);
    return;
  }

  window.open(url, "_blank", "noopener,noreferrer");
}
