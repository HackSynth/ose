"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { BookOpenCheck, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export function PasswordResetConfirmForm({ token }: { token: string }) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ kind: "ok" | "error"; text: string } | null>(null);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    const formData = new FormData(event.currentTarget);
    const newPassword = String(formData.get("newPassword") ?? "");
    const confirmPassword = String(formData.get("confirmPassword") ?? "");
    if (newPassword.length < 6 || newPassword.length > 128) {
      setMessage({ kind: "error", text: "新密码长度需在 6-128 之间" });
      return;
    }
    if (newPassword !== confirmPassword) {
      setMessage({ kind: "error", text: "两次输入的新密码不一致" });
      return;
    }
    setLoading(true);
    try {
      const response = await fetch("/api/auth/password-reset", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ mode: "confirm", token, newPassword }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setMessage({ kind: "error", text: (data as { message?: string }).message || "重置失败" });
        return;
      }
      setMessage({ kind: "ok", text: "密码已重置，请使用新密码登录" });
      setTimeout(() => router.push("/login"), 1200);
    } catch {
      setMessage({ kind: "error", text: "网络异常，请稍后再试" });
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card className="w-full max-w-md border-white/80 bg-white/95">
      <CardHeader className="items-center text-center">
        <div className="mb-2 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary text-white shadow-soft">
          <BookOpenCheck className="h-7 w-7" />
        </div>
        <CardTitle className="text-3xl font-black">设置新密码</CardTitle>
        <CardDescription className="text-base font-bold">输入两次新密码以完成重置</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-5" onSubmit={onSubmit}>
          <div className="space-y-2">
            <Label htmlFor="newPassword">新密码</Label>
            <Input id="newPassword" name="newPassword" type="password" placeholder="6-128 位" required minLength={6} maxLength={128} autoComplete="new-password" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="confirmPassword">确认新密码</Label>
            <Input id="confirmPassword" name="confirmPassword" type="password" required minLength={6} maxLength={128} autoComplete="new-password" />
          </div>
          {message ? <p className={`rounded-2xl px-4 py-3 text-sm font-bold ${message.kind === "ok" ? "bg-softGreen text-green-800" : "bg-red-50 text-red-600"}`}>{message.text}</p> : null}
          <Button className="w-full" size="lg" type="submit" disabled={loading}>
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            重置密码
          </Button>
        </form>
        <p className="mt-6 text-center text-sm font-semibold text-muted">
          <Link className="font-black text-primary hover:text-primary-dark" href="/login">返回登录</Link>
        </p>
      </CardContent>
    </Card>
  );
}
