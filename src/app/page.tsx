import Link from "next/link";
import { auth } from "@/lib/auth";
import { redirect } from "next/navigation";
import { DecorativeBackground } from "@/components/decorative-background";
import { LandingActions } from "@/components/landing-actions";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

export const metadata = { title: "OSE 软考备考 | AI 驱动的软件设计师备考平台", description: "AI 驱动的题库练习、模拟考试、薄弱分析，助你高效通关软件设计师考试。" };

export default async function HomePage() {
  const session = await auth();
  if (session?.user) redirect("/dashboard");
  return <main className="ose-page px-5 py-10"><DecorativeBackground /><div className="relative z-10 mx-auto max-w-7xl"><nav className="flex items-center justify-between rounded-[1.5rem] bg-white/80 px-5 py-4 shadow-soft"><div className="text-3xl font-black text-navy">OSE <span className="text-sm text-muted">软考备考</span></div><Button asChild><Link href="/login">登录</Link></Button></nav><section className="py-20 text-center"><h1 className="text-5xl font-black tracking-tight text-navy md:text-7xl">OSE 软考备考</h1><p className="mt-5 text-2xl font-extrabold text-primary">软件设计师·智能备考平台</p><p className="mx-auto mt-6 max-w-2xl text-lg font-semibold leading-8 text-muted">AI 驱动的题库练习、模拟考试、薄弱分析，助你高效通关。</p><LandingActions /></section><section id="features" className="grid gap-5 md:grid-cols-3"><Card className="p-7 text-center"><div className="text-5xl">📚</div><h2 className="mt-4 text-2xl font-black text-navy">智能题库</h2><p className="mt-2 font-semibold text-muted">选择题 + 案例分析，覆盖 10 大知识点。</p></Card><Card className="p-7 text-center"><div className="text-5xl">💡</div><h2 className="mt-4 text-2xl font-black text-navy">AI 辅导</h2><p className="mt-2 font-semibold text-muted">题目讲解、案例批改、随时问答。</p></Card><Card className="p-7 text-center"><div className="text-5xl">📈</div><h2 className="mt-4 text-2xl font-black text-navy">学情分析</h2><p className="mt-2 font-semibold text-muted">定位薄弱点，生成个性化学习计划。</p></Card></section><section className="mt-12 grid gap-4 rounded-[2rem] bg-white/80 p-7 text-center shadow-soft md:grid-cols-3"><div><p className="text-4xl font-black text-primary">35+</p><p className="font-bold text-muted">题库题目</p></div><div><p className="text-4xl font-black text-primary">2类</p><p className="font-bold text-muted">选择题+案例分析</p></div><div><p className="text-4xl font-black text-primary">10</p><p className="font-bold text-muted">大知识点</p></div></section><footer className="mt-12 text-center font-bold text-muted">OSE - Open Software Exam | GitHub</footer></div></main>;
}
