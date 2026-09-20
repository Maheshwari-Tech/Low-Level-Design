import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:4105"),
  title: {
    default: "LLD Atlas — Learn Low-Level Design",
    template: "%s · LLD Atlas",
  },
  description: "A complete low-level design learning path with Senior/Staff interview questions, runnable Java and Python, SOLID, design patterns, concurrency, UML, and exact provenance.",
  openGraph: {
    title: "LLD Atlas",
    description: "Learn. Design. Inspect the source.",
    type: "website",
    images: [{ url: "/og.png", width: 1200, height: 630, alt: "LLD Atlas — Learn low-level design from principles to source." }],
  },
  twitter: { card: "summary_large_image", title: "LLD Atlas", description: "Learn. Design. Inspect the source.", images: ["/og.png"] },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body>{children}</body>
    </html>
  );
}
