import { Suspense } from "react";
import SharePage from "./SharePage";

// Cache Components: the dynamic token is only known at request time
export default function Page() {
  return (
    <Suspense fallback={<p className="pt-20 text-center text-muted">불러오는 중...</p>}>
      <SharePage />
    </Suspense>
  );
}
