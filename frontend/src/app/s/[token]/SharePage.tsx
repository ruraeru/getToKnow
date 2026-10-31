"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { api, type Unlocked } from "@/lib/api";
import { tint } from "@/lib/tints";

type ShareInfo = { nickname: string; lockQuestion: string };

/** Public page behind a share link: answer the lock question to see the answers. */
export default function SharePage() {
  const { token } = useParams<{ token: string }>();
  const [info, setInfo] = useState<ShareInfo | null>(null);
  const [unlocked, setUnlocked] = useState<Unlocked | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api<ShareInfo>(`/api/share/${token}`)
      .then(setInfo)
      .catch((e) => setError(e.message));
  }, [token]);

  async function unlock(form: FormData) {
    setError("");
    try {
      setUnlocked(await api<Unlocked>(`/api/share/${token}/unlock`, "POST", { guess: form.get("guess") }));
    } catch (e) {
      setError((e as Error).message);
    }
  }

  if (unlocked) {
    return (
      <div className="pt-4">
        <section className="tile bg-me-soft text-me-ink">
          <h1 className="text-[26px] font-bold leading-snug tracking-tight">
            {unlocked.nickname}님에 대해
            <br />
            알게 된 것들
          </h1>
          <p className="mt-6 font-bold">
            <span className="text-[40px] leading-none">{unlocked.answers.length}</span>
            <span className="text-lg opacity-70"> 개의 답변</span>
          </p>
        </section>
        {unlocked.answers.length === 0 ? (
          <p className="card mt-3 text-muted">아직 작성한 답변이 없어요</p>
        ) : (
          <dl className="mt-3 flex flex-col gap-3">
            {unlocked.answers.map((a, i) => (
              <div key={a.question} className={`tile p-5 ${tint(i)}`}>
                <dt className="text-[15px] font-medium opacity-80">{a.question}</dt>
                <dd className="mt-1.5 whitespace-pre-wrap text-xl font-bold">{a.answer}</dd>
              </div>
            ))}
          </dl>
        )}
      </div>
    );
  }

  if (!info) return <p className={`pt-20 text-center ${error ? "text-negative" : "text-muted"}`}>{error || "불러오는 중..."}</p>;

  return (
    <div className="pt-4">
      <section className="tile bg-me-soft text-me-ink">
        <h1 className="text-[26px] font-bold leading-snug tracking-tight">
          {info.nickname}님의 답변이
          <br />
          잠겨 있어요
        </h1>
        <p className="mt-2 text-[15px] font-medium">아래 질문의 정답을 맞히면 전부 볼 수 있어요.</p>
      </section>
      <form action={unlock} className="card mt-3 flex flex-col gap-4">
        <div>
          <label htmlFor="guess" className="mb-3 block text-xl font-bold">
            {info.lockQuestion}
          </label>
          <input id="guess" name="guess" required autoComplete="off" placeholder="정답" className="input" />
        </div>
        {error && <p role="alert" className="text-sm font-medium text-negative">{error}</p>}
        <button className="btn">열어보기</button>
      </form>
    </div>
  );
}
