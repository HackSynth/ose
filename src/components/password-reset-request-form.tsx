"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { BookOpenCheck, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { isValidEmail } from "@/lib/validate";

export function PasswordResetRequestForm() {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ kind: "ok" | "error"; text: string } | null>(null);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    const formData = new FormData(event.currentTarget);
    const email = String(formData.get("email") ?? "").trim().toLowerCase();
    if (!isValidEmail(email)) {
      setMessage({ kind: "error", text: "邮箱格式不正确" });
      return;
    }
    setLoading(true);
    try {
      const response = await fetch("/api/auth/password-reset", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ mode: "request", email }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setMessage({ kind: "error", text: (data as { message?: string }).message || "请求失败" });
        return;
      }
      setMessage({ kind: "ok", text: (data as { message?: string }).message || "已生成重置链接" });
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
        <CardTitle className="text-3xl font-black">重置密码</CardTitle>
        <CardDescription className="text-base font-bold">输入注册邮箱，我们会生成一条重置链接</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-5" onSubmit={onSubmit}>
          <div className="space-y-2">
            <Label htmlFor="email">邮箱</Label>
            <Input id="email" name="email" type="email" placeholder="you@example.com" required autoComplete="email" />
          </div>
          {message ? <p className={`rounded-2xl px-4 py-3 text-sm font-bold ${message.kind === "ok" ? "bg-softGreen text-green-800" : "bg-red-50 text-red-600"}`}>{message.text}</p> : null}
          <p className="rounded-2xl bg-primary-soft px-4 py-3 text-xs font-bold text-primary">
            个人部署：重置链接会打印到服务器日志（终端），请在后端控制台查看后复制打开。
          </p>
          <Button className="w-full" size="lg" type="submit" disabled={loading}>
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            发送重置链接
          </Button>
        </form>
        <p className="mt-6 text-center text-sm font-semibold text-muted">
          想起密码了？ <Link className="font-black text-primary hover:text-primary-dark" href="/login">返回登录</Link>
        </p>
      </CardContent>
    </Card>
  );
}
