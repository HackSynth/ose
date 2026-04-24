import Link from "next/link";
import { Button } from "@/components/ui/button";
import { DecorativeBackground } from "@/components/decorative-background";

export default function NotFound() {
  return <main className="ose-page flex min-h-screen items-center justify-center px-5"><DecorativeBackground /><div className="relative z-10 max-w-xl rounded-[2rem] bg-white/90 p-10 text-center shadow-soft"><div className="text-6xl">🌱</div><h1 className="mt-5 text-5xl font-black text-navy">页面走丢啦</h1><p className="mt-4 font-semibold leading-8 text-muted">没关系，学习路上偶尔迷路也很正常。回到仪表盘继续今天的进步吧。</p><Button asChild className="mt-6"><Link href="/dashboard">回到学习首页</Link></Button></div></main>;
}
