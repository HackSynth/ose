import type { Metadata } from 'next';
import { Nunito } from 'next/font/google';
import type { Viewport } from 'next';
import './globals.css';
import { Toaster } from '@/components/ui/toaster';
import { PWARegister } from '@/components/pwa-register';

const nunito = Nunito({
  subsets: ['latin'],
  variable: '--font-body',
  weight: ['400', '500', '600', '700', '800', '900'],
});

export const metadata: Metadata = {
  title: { default: 'OSE 软考备考', template: '%s | OSE' },
  description: 'OSE - Open Software Exam，一个开源的软考（软件设计师）备考系统。',
  manifest: '/manifest.webmanifest',
  applicationName: 'OSE',
  appleWebApp: {
    capable: true,
    title: 'OSE',
    statusBarStyle: 'default',
  },
  icons: {
    icon: [{ url: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' }],
    apple: [{ url: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' }],
  },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  viewportFit: 'cover',
  themeColor: '#f97316',
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN">
      <body className={nunito.variable}>
        {children}
        <Toaster />
        <PWARegister />
      </body>
    </html>
  );
}
