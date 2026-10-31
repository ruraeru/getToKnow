import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "getToKnow",
  description: "물어봐야만 알 수 있는 서로의 이야기",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#f4f5f7",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className="h-full antialiased">
      <head>
        {/* Pretendard isn't on Google Fonts; dynamic subset only loads the Hangul ranges a page uses */}
        <link
          rel="stylesheet"
          crossOrigin="anonymous"
          href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css"
        />
      </head>
      <body className="min-h-full">
        <main className="mx-auto max-w-md px-4 pb-12 pt-[max(1rem,env(safe-area-inset-top))]">{children}</main>
      </body>
    </html>
  );
}
