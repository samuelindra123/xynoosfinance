import type { Metadata } from 'next';
import { PageIntro } from '@/components/site';
import { ReleaseContent } from '@/components/releases';
export const metadata: Metadata = { title: 'Rilis' };
export default function ReleasesPage() { return <div className="light-page"><div className="shell"><PageIntro eyebrow="PERJALANAN PRODUK" title="Selalu bergerak maju." description="Ikuti pembaruan XynoosFinance dan temukan versi yang paling sesuai untukmu."/><ReleaseContent mode="releases"/></div></div> }
