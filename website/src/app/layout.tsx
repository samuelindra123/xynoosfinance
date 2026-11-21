import type { Metadata } from 'next';
import { Header, Footer } from '@/components/site';
import './globals.css';
export const metadata: Metadata = { title: { default: 'XynoosFinance — Kendali yang terasa lebih sederhana', template: '%s — XynoosFinance' }, description: 'Aplikasi Android untuk mencatat, merencanakan, dan memahami keuangan pribadi maupun bisnis.' };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="id"><body><Header/><main>{children}</main><Footer/></body></html> }
