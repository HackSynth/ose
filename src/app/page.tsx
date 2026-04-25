import Link from 'next/link';
import { redirect } from 'next/navigation';
import {
  BarChart3,
  Bot,
  Brain,
  CheckCircle2,
  Clock3,
  Database,
  Github,
  GraduationCap,
  Laptop,
  LockKeyhole,
  Server,
  Sparkles,
} from 'lucide-react';

import { LandingActions } from '@/components/landing-actions';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { auth } from '@/lib/auth';

export const metadata = {
  title: 'OSE | Open-source AI-powered exam preparation',
  description:
    "Open-source AI-powered exam preparation platform for China's Software Professional Qualification Exam.",
};

const repoUrl = 'https://github.com/hacksynth/ose';

const features = [
  {
    icon: GraduationCap,
    title: 'Smart question bank',
    description:
      'Multiple-choice and case analysis workflows mapped to the Software Designer syllabus.',
  },
  {
    icon: Bot,
    title: 'AI-assisted learning',
    description: 'Explain, grade, generate questions, diagnose weak areas, and create study plans.',
  },
  {
    icon: BarChart3,
    title: 'Learning analytics',
    description: 'Knowledge heatmaps, weak-area diagnosis, predicted scores, and pass probability.',
  },
  {
    icon: Clock3,
    title: 'Mock exams',
    description: 'Timed exam sessions with answer sheets, scoring, and review reports.',
  },
  {
    icon: Server,
    title: 'Self-hosted',
    description: 'Run locally, on Docker, on a VPS, or behind your own reverse proxy.',
  },
  {
    icon: LockKeyhole,
    title: 'Privacy first',
    description: 'Keep learning records and provider keys in your own deployment and database.',
  },
];

const providers = ['Claude', 'OpenAI', 'Gemini', 'Custom API'];

export default async function HomePage() {
  const session = await auth();
  if (session?.user) redirect('/dashboard');

  return (
    <main className="min-h-screen bg-[#fbfaf7] text-slate-950">
      <header className="border-b border-slate-200 bg-white/90">
        <nav className="mx-auto flex max-w-7xl items-center justify-between px-5 py-4">
          <Link href="/" className="flex items-center gap-3 font-black text-slate-950">
            <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-slate-950 text-sm text-white">
              OSE
            </span>
            <span className="leading-tight">
              Open Software Exam
              <span className="block text-xs font-bold text-slate-500">软考 AI 备考平台</span>
            </span>
          </Link>
          <div className="hidden items-center gap-2 md:flex">
            <a
              href={repoUrl}
              className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-bold text-slate-700 transition-colors hover:border-slate-300 hover:bg-slate-50"
            >
              <Github className="h-4 w-4" aria-hidden="true" />
              Star on GitHub
            </a>
            <Button asChild size="sm">
              <Link href="/login">Sign in</Link>
            </Button>
          </div>
          <Button asChild size="sm" className="md:hidden">
            <Link href="/login">Sign in</Link>
          </Button>
        </nav>
      </header>

      <section className="mx-auto grid max-w-7xl gap-10 px-5 py-14 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:py-20">
        <div>
          <div className="flex flex-wrap gap-2">
            <span className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-sm font-bold text-emerald-700">
              Open Source
            </span>
            <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-sm font-bold text-slate-700">
              AGPL-3.0
            </span>
            <span className="rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-sm font-bold text-blue-700">
              Web + Desktop
            </span>
          </div>
          <h1 className="mt-7 max-w-4xl text-5xl font-black leading-[1.02] tracking-normal text-slate-950 md:text-7xl">
            AI-powered exam preparation, built in the open.
          </h1>
          <p className="mt-6 max-w-2xl text-lg font-semibold leading-8 text-slate-600">
            OSE helps Software Designer candidates practice questions, master weak areas, run
            realistic mock exams, and use their preferred AI provider without giving up control of
            their data.
          </p>
          <LandingActions />
          <div className="mt-8 flex flex-wrap items-center gap-3 text-sm font-bold text-slate-500">
            <span className="inline-flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-emerald-600" aria-hidden="true" />
              Self-hostable
            </span>
            <span className="inline-flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-emerald-600" aria-hidden="true" />
              Multi-provider AI
            </span>
            <span className="inline-flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-emerald-600" aria-hidden="true" />
              Community question bank
            </span>
          </div>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft">
          <div className="rounded-lg border border-slate-200 bg-slate-950 p-4 text-white">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <p className="text-sm font-bold text-slate-300">Dashboard preview</p>
                <p className="text-2xl font-black">Pass probability 78%</p>
              </div>
              <Sparkles className="h-6 w-6 text-emerald-300" aria-hidden="true" />
            </div>
            <div className="grid gap-3 sm:grid-cols-3">
              {['Practice', 'Mock exam', 'Weak areas'].map((item, index) => (
                <div key={item} className="rounded-lg bg-white/10 p-3">
                  <p className="text-xs font-bold text-slate-300">{item}</p>
                  <p className="mt-2 text-2xl font-black">{[35, 2, 10][index]}</p>
                </div>
              ))}
            </div>
            <div className="mt-4 rounded-lg bg-white p-4 text-slate-950">
              <div className="mb-3 flex items-center justify-between">
                <p className="font-black">Knowledge mastery</p>
                <Brain className="h-5 w-5 text-blue-600" aria-hidden="true" />
              </div>
              <div className="grid grid-cols-8 gap-2">
                {Array.from({ length: 32 }).map((_, index) => (
                  <span
                    key={index}
                    className={[
                      'h-7 rounded-md',
                      index % 5 === 0
                        ? 'bg-rose-200'
                        : index % 3 === 0
                          ? 'bg-amber-200'
                          : 'bg-emerald-300',
                    ].join(' ')}
                  />
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="features" className="border-y border-slate-200 bg-white py-14">
        <div className="mx-auto max-w-7xl px-5">
          <div className="max-w-2xl">
            <p className="text-sm font-black uppercase tracking-wider text-slate-500">Features</p>
            <h2 className="mt-3 text-3xl font-black text-slate-950 md:text-4xl">
              Everything needed for a serious preparation workflow.
            </h2>
          </div>
          <div className="mt-8 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {features.map((feature) => (
              <Card key={feature.title} className="rounded-lg p-6 shadow-none hover:translate-y-0">
                <feature.icon className="h-6 w-6 text-slate-950" aria-hidden="true" />
                <h3 className="mt-4 text-xl font-black text-slate-950">{feature.title}</h3>
                <p className="mt-2 leading-7 text-slate-600">{feature.description}</p>
              </Card>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto grid max-w-7xl gap-8 px-5 py-14 lg:grid-cols-2">
        <div>
          <p className="text-sm font-black uppercase tracking-wider text-slate-500">AI providers</p>
          <h2 className="mt-3 text-3xl font-black text-slate-950">Bring your preferred model.</h2>
          <p className="mt-4 leading-8 text-slate-600">
            Use hosted frontier models or connect an OpenAI-compatible endpoint for local and
            regional model deployments.
          </p>
          <div className="mt-6 grid grid-cols-2 gap-3">
            {providers.map((provider) => (
              <div
                key={provider}
                className="rounded-lg border border-slate-200 bg-white p-4 font-black"
              >
                {provider}
              </div>
            ))}
          </div>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-6">
          <div className="flex items-center gap-3">
            <Database className="h-6 w-6 text-emerald-600" aria-hidden="true" />
            <h2 className="text-2xl font-black text-slate-950">Self-hosted by default</h2>
          </div>
          <p className="mt-4 leading-8 text-slate-600">
            OSE runs with SQLite for local development and supports PostgreSQL for production. The
            desktop app stores data locally, while Docker and VPS deployments keep data under your
            control.
          </p>
          <div className="mt-6 flex items-center gap-3 rounded-lg bg-slate-50 p-4">
            <Laptop className="h-5 w-5 text-slate-700" aria-hidden="true" />
            <span className="font-bold text-slate-700">Next.js, Prisma, Tailwind, Tauri</span>
          </div>
        </div>
      </section>

      <section className="border-t border-slate-200 bg-slate-950 py-14 text-white">
        <div className="mx-auto max-w-7xl px-5">
          <div className="grid gap-8 md:grid-cols-[1fr_auto] md:items-center">
            <div>
              <p className="text-sm font-black uppercase tracking-wider text-slate-400">
                Powered by the community
              </p>
              <h2 className="mt-3 text-3xl font-black">
                Built for learners, teachers, and builders.
              </h2>
              <p className="mt-4 max-w-2xl leading-8 text-slate-300">
                Contributions can improve the question bank, deployment experience, AI prompts,
                accessibility, documentation, and support for more exam tracks.
              </p>
            </div>
            <Button asChild variant="secondary">
              <a href={repoUrl}>
                <Github className="h-4 w-4" aria-hidden="true" />
                Contribute on GitHub
              </a>
            </Button>
          </div>
          <div className="mt-8 flex -space-x-2">
            {['OC', 'AI', 'DB', 'UX', 'QA', 'CN'].map((name) => (
              <div
                key={name}
                className="flex h-11 w-11 items-center justify-center rounded-full border-2 border-slate-950 bg-white text-xs font-black text-slate-950"
                aria-label={`Contributor placeholder ${name}`}
              >
                {name}
              </div>
            ))}
          </div>
        </div>
      </section>

      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-5 py-6 text-sm font-bold text-slate-600 md:flex-row md:items-center md:justify-between">
          <p>OSE - Open Software Exam</p>
          <div className="flex flex-wrap gap-4">
            <a href={repoUrl} className="transition-colors hover:text-slate-950">
              GitHub
            </a>
            <a
              href={`${repoUrl}/tree/main/docs`}
              className="transition-colors hover:text-slate-950"
            >
              Documentation
            </a>
            <a
              href={`${repoUrl}/blob/main/CONTRIBUTING.md`}
              className="transition-colors hover:text-slate-950"
            >
              Contributing
            </a>
          </div>
        </div>
      </footer>
    </main>
  );
}
