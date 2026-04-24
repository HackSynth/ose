"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { signIn } from "next-auth/react";
import { BookOpenCheck, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { isValidEmail } from "@/lib/validate";

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState(() => (searchParams.get("expired") === "1" ? "登录已过期，请重新登录" : ""));
  const [loading, setLoading] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    const formData = new FormData(event.currentTarget);
    const email = String(formData.get("email") ?? "").trim().toLowerCase();
    const password = String(formData.get("password") ?? "");
    if (!isValidEmail(email)) {
      setError("邮箱格式不正确");
      return;
    }
    if (!password) {
      setError("请输入密码");
      return;
    }
    setLoading(true);
    const callbackUrl = searchParams.get("callbackUrl") || "/dashboard";
    try {
      const result = await signIn("credentials", {
        email,
        password,
        redirect: false,
        callbackUrl,
      });
      if (result?.error) {
        setError("邮箱或密码不正确，请重试");
        return;
      }
      router.push(callbackUrl);
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
        <CardTitle className="text-3xl font-black">OSE 软考备考</CardTitle>
        <CardDescription className="text-base font-bold">软件设计师·智能备考平台</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-5" onSubmit={onSubmit}>
          <div className="space-y-2">
            <Label htmlFor="email">邮箱</Label>
            <Input id="email" name="email" type="email" placeholder="you@example.com" required autoComplete="email" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">密码</Label>
            <Input id="password" name="password" type="password" placeholder="请输入密码" required autoComplete="current-password" minLength={6} />
          </div>
          {error ? <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm font-bold text-red-600">{error}</p> : null}
          <p className="text-right text-sm font-bold text-muted"><Link href="/reset-password" className="text-primary hover:text-primary-dark">忘记密码？</Link></p>
          <Button className="w-full" size="lg" type="submit" disabled={loading}>
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            登录并开始学习
          </Button>
        </form>
        <p className="mt-6 text-center text-sm font-semibold text-muted">
          还没有账号？ <Link className="font-black text-primary hover:text-primary-dark" href="/register">立即注册</Link>
        </p>
      </CardContent>
    </Card>
  );
}
