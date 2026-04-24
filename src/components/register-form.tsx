"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { signIn } from "next-auth/react";
import { BookOpenCheck, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { isValidEmail } from "@/lib/validate";

export function RegisterForm() {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setMessage("");
    const formData = new FormData(event.currentTarget);
    const name = String(formData.get("name") ?? "").trim();
    const email = String(formData.get("email") ?? "").trim().toLowerCase();
    const password = String(formData.get("password") ?? "");
    if (!name) {
      setError("请输入姓名");
      return;
    }
    if (!isValidEmail(email)) {
      setError("邮箱格式不正确");
      return;
    }
    if (password.length < 6 || password.length > 128) {
      setError("密码长度需在 6-128 位之间");
      return;
    }
    setLoading(true);
    try {
      const response = await fetch("/api/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, email, password }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setError(data.message || "注册失败，请稍后重试");
        return;
      }
      setMessage("注册成功，正在进入仪表盘");
      const signInResult = await signIn("credentials", { email, password, redirect: false });
      if (signInResult?.error) {
        setError("注册成功但自动登录失败，请返回登录页");
        return;
      }
      router.push("/dashboard");
      router.refresh();
    } catch {
      setError("网络异常，请稍后再试");
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
        <CardTitle className="text-3xl font-black">创建 OSE 账号</CardTitle>
        <CardDescription className="text-base font-bold">软件设计师·智能备考平台</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-5" onSubmit={onSubmit}>
          <div className="space-y-2">
            <Label htmlFor="name">姓名</Label>
            <Input id="name" name="name" placeholder="请输入你的昵称" required maxLength={50} autoComplete="name" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email">邮箱</Label>
            <Input id="email" name="email" type="email" placeholder="you@example.com" required autoComplete="email" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">密码</Label>
            <Input id="password" name="password" type="password" placeholder="至少 6 位" required autoComplete="new-password" minLength={6} maxLength={128} />
          </div>
          {error ? <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm font-bold text-red-600">{error}</p> : null}
          {message ? <p className="rounded-2xl bg-green-50 px-4 py-3 text-sm font-bold text-green-700">{message}</p> : null}
          <Button className="w-full" size="lg" type="submit" disabled={loading}>
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            注册并进入学习
          </Button>
        </form>
        <p className="mt-6 text-center text-sm font-semibold text-muted">
          已有账号？ <Link className="font-black text-primary hover:text-primary-dark" href="/login">返回登录</Link>
        </p>
      </CardContent>
    </Card>
  );
}
