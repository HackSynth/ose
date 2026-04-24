"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Loader2, Save, Trash2 } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAIStatus } from "@/components/ai-status-context";

type Settings = {
  provider: string | null;
  model: string | null;
  baseUrl: string | null;
  apiKeyMasked: string | null;
  hasApiKey: boolean;
};

const DEFAULT_SETTINGS: Settings = { provider: null, model: null, baseUrl: null, apiKeyMasked: null, hasApiKey: false };

const PROVIDERS = [
  { value: "", label: "使用服务器环境变量" },
  { value: "openai", label: "OpenAI" },
  { value: "claude", label: "Claude" },
  { value: "gemini", label: "Gemini" },
  { value: "custom", label: "自定义 (OpenAI 兼容)" },
];

export function AISettingsCard() {
  const status = useAIStatus();
  const [settings, setSettings] = useState<Settings>(DEFAULT_SETTINGS);
  const [apiKeyDraft, setApiKeyDraft] = useState("");
  const [message, setMessage] = useState<{ kind: "ok" | "error"; text: string } | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch("/api/profile/ai-settings", { cache: "no-store" });
      if (!response.ok) return;
      const data = (await response.json()) as Settings;
      setSettings(data);
      setApiKeyDraft("");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setMessage(null);
    try {
      const payload: Record<string, unknown> = {
        provider: settings.provider || "",
        model: settings.model || "",
        baseUrl: settings.baseUrl || "",
      };
      if (apiKeyDraft.trim()) payload.apiKey = apiKeyDraft.trim();
      const response = await fetch("/api/profile/ai-settings", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setMessage({ kind: "error", text: (data as { message?: string }).message || "保存失败" });
        return;
      }
      setMessage({ kind: "ok", text: "已保存" });
      await load();
      await status.refresh();
    } catch {
      setMessage({ kind: "error", text: "网络异常，请稍后再试" });
    } finally {
      setSaving(false);
    }
  }

  async function clearConfig() {
    if (!confirm("确认清空个人 AI 配置并回落到服务器环境变量？")) return;
    setSaving(true);
    try {
      const response = await fetch("/api/profile/ai-settings", { method: "DELETE" });
      if (!response.ok) {
        setMessage({ kind: "error", text: "清空失败" });
        return;
      }
      setMessage({ kind: "ok", text: "已清空个人配置" });
      setSettings(DEFAULT_SETTINGS);
      setApiKeyDraft("");
      await status.refresh();
    } finally {
      setSaving(false);
    }
  }

  async function clearApiKey() {
    setSaving(true);
    try {
      const response = await fetch("/api/profile/ai-settings", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ provider: settings.provider || "", model: settings.model || "", baseUrl: settings.baseUrl || "", apiKey: null }),
      });
      if (!response.ok) return;
      await load();
      await status.refresh();
      setMessage({ kind: "ok", text: "已清空 API Key" });
    } finally {
      setSaving(false);
    }
  }

  return (
    <Card className="p-6 hover:translate-y-0">
      <p className="text-sm font-black text-primary">AI 设置</p>
      <h2 className="mt-2 text-2xl font-black text-navy">智能辅助功能</h2>
      <div className="mt-4 grid gap-3 sm:grid-cols-3">
        <div className="rounded-3xl bg-softBlue p-4">
          <p className="font-black text-muted">当前供应商</p>
          <p className="mt-2 text-2xl font-black text-navy">{status.provider ?? "未配置"}</p>
        </div>
        <div className="rounded-3xl bg-softGreen p-4">
          <p className="font-black text-muted">当前模型</p>
          <p className="mt-2 text-lg font-black text-navy">{status.model ?? "未配置"}</p>
        </div>
        <div className="rounded-3xl bg-softYellow p-4">
          <p className="font-black text-muted">配置来源</p>
          <p className="mt-2 text-lg font-black text-navy">{status.source === "user" ? "个人配置" : status.source === "env" ? "服务器环境变量" : "未配置"}</p>
        </div>
      </div>
      <div className="mt-3 rounded-3xl bg-softRose p-4">
        <p className="font-black text-muted">当前 Endpoint</p>
        <p className="mt-2 break-all font-bold text-navy">{status.endpoint ?? "未配置"}</p>
      </div>

      <form onSubmit={onSubmit} className="mt-6 space-y-4">
        <div className="grid gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="ai-provider">供应商</Label>
            <select id="ai-provider" className="ose-input w-full" value={settings.provider ?? ""} onChange={(event) => setSettings((prev) => ({ ...prev, provider: event.target.value || null }))} disabled={loading || saving}>
              {PROVIDERS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="ai-model">模型名称（可选）</Label>
            <Input id="ai-model" value={settings.model ?? ""} onChange={(event) => setSettings((prev) => ({ ...prev, model: event.target.value }))} placeholder="留空使用默认值，例如 claude-sonnet-4-5-20250929" disabled={loading || saving} maxLength={200} />
          </div>
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="ai-base-url">Base URL（代理或 custom 时必填）</Label>
            <Input id="ai-base-url" value={settings.baseUrl ?? ""} onChange={(event) => setSettings((prev) => ({ ...prev, baseUrl: event.target.value }))} placeholder="https://your-proxy.example.com/v1" disabled={loading || saving} maxLength={500} />
          </div>
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="ai-api-key">API Key {settings.hasApiKey ? <span className="ml-2 text-xs font-bold text-muted">已保存：{settings.apiKeyMasked}</span> : null}</Label>
            <div className="flex gap-2">
              <Input id="ai-api-key" value={apiKeyDraft} onChange={(event) => setApiKeyDraft(event.target.value)} placeholder={settings.hasApiKey ? "留空即保持现有 API Key" : "粘贴你的 API Key"} disabled={loading || saving} type="password" maxLength={500} autoComplete="off" />
              {settings.hasApiKey ? <Button type="button" variant="secondary" onClick={clearApiKey} disabled={loading || saving}>清空 Key</Button> : null}
            </div>
            <p className="text-xs font-bold text-muted">API Key 会明文保存到本地数据库，仅你自己可见。若不想持久化，请改用服务器环境变量。</p>
          </div>
        </div>

        {message ? (
          <p className={`rounded-2xl px-4 py-3 text-sm font-bold ${message.kind === "ok" ? "bg-softGreen text-green-800" : "bg-red-50 text-red-600"}`}>{message.text}</p>
        ) : null}

        <div className="flex flex-wrap items-center gap-3">
          <Button type="submit" disabled={loading || saving}>{saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}保存设置</Button>
          <Button type="button" variant="ghost" onClick={clearConfig} disabled={loading || saving}><Trash2 className="h-4 w-4" />清空个人配置</Button>
        </div>
      </form>
    </Card>
  );
}
