"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { api, type Me, type QuestionView } from "@/lib/api";
import { tint } from "@/lib/tints";

export default function Home() {
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);
  const [questions, setQuestions] = useState<QuestionView[]>([]);
  const [answered, setAnswered] = useState<Set<number>>(new Set());
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    const [m, q] = await Promise.all([api<Me>("/api/me"), api<QuestionView[]>("/api/questions")]);
    setMe(m);
    setQuestions(q);
    setAnswered(new Set(q.filter((x) => x.answer).map((x) => x.id)));
  }, []);

  useEffect(() => {
    if (!localStorage.getItem("token")) router.replace("/login");
    // setState only runs after the fetch resolves, not synchronously
    // eslint-disable-next-line react-hooks/set-state-in-effect
    else load().catch((e) => setError(e.message));
  }, [router, load]);

  function logout() {
    localStorage.removeItem("token");
    router.replace("/login");
  }

  if (!me) return <p className={`pt-20 text-center ${error ? "text-negative" : "text-muted"}`}>{error || "불러오는 중..."}</p>;

  const total = questions.length;
  const done = answered.size;
  return (
    <>
      <header className="flex h-12 items-center justify-between px-1">
        <span className="font-extrabold tracking-tight">getToKnow</span>
        <button onClick={logout} className="link py-2">
          로그아웃
        </button>
      </header>

      <section className="tile mt-2 bg-me-soft text-me-ink">
        <h1 className="text-[26px] font-bold leading-snug tracking-tight">
          {me.nickname}님에 대해
          <br />
          물어봐야 아는 것들
        </h1>
        <div className="mt-8 flex items-end justify-between">
          <p className="font-bold">
            <span className="text-[40px] leading-none">{done}</span>
            <span className="text-lg opacity-70"> / {total}</span>
          </p>
          <p className="text-sm font-semibold">{done < total ? `${total - done}개 남았어요` : "모두 답했어요"}</p>
        </div>
        <div
          role="progressbar"
          aria-label="답한 질문"
          aria-valuemin={0}
          aria-valuemax={total}
          aria-valuenow={done}
          className="mt-3 h-2.5 overflow-hidden rounded-full bg-white/80"
        >
          <div className="h-full rounded-full bg-me transition-[width] duration-500" style={{ width: `${(done / (total || 1)) * 100}%` }} />
        </div>
      </section>

      <ShareBox me={me} onChange={setMe} />

      <section className="mt-8">
        <h2 className="px-1 text-lg font-bold">
          질문 <span className="text-muted">{total}</span>
        </h2>
        <ul className="mt-3 flex flex-col gap-3">
          {questions.map((q, i) => (
            <AnswerItem
              key={q.id}
              q={q}
              color={tint(i)}
              onSaved={() => setAnswered((s) => new Set(s).add(q.id))}
            />
          ))}
        </ul>
      </section>
    </>
  );
}

function ShareBox({ me, onChange }: { me: Me; onChange: (me: Me) => void }) {
  const [editing, setEditing] = useState(!me.lockQuestion);
  const [notice, setNotice] = useState("");
  const url = typeof location === "undefined" ? "" : `${location.origin}/s/${me.shareToken}`;

  async function saveLock(form: FormData) {
    setNotice("");
    try {
      onChange(await api<Me>("/api/me/lock", "PUT", Object.fromEntries(form)));
      setEditing(false);
    } catch (e) {
      setNotice((e as Error).message);
    }
  }

  async function share() {
    if (navigator.share) {
      await navigator.share({ title: `${me.nickname}님의 답변`, url }).catch(() => {});
    } else if (navigator.clipboard) {
      await navigator.clipboard.writeText(url);
      setNotice("링크를 복사했어요");
    } else {
      setNotice("링크를 길게 눌러 복사해주세요");
    }
  }

  return (
    <section className="tile mt-3 bg-you-soft text-you-ink">
      <h2 className="text-lg font-bold">링크로 공유하기</h2>
      {editing ? (
        <form action={saveLock} className="mt-1 flex flex-col gap-4">
          <p className="text-[15px]">잠금 질문의 정답을 맞힌 사람만 내 답변을 전부 볼 수 있어요.</p>
          <div>
            <label htmlFor="lockQuestion" className="label">잠금 질문</label>
            <input
              id="lockQuestion"
              name="lockQuestion"
              required
              maxLength={200}
              defaultValue={me.lockQuestion ?? ""}
              placeholder="우리가 처음 만난 곳은?"
              className="input bg-white focus:border-you"
            />
          </div>
          <div>
            <label htmlFor="lockAnswer" className="label">정답</label>
            <input
              id="lockAnswer"
              name="lockAnswer"
              required
              maxLength={100}
              placeholder="강남역"
              aria-describedby="lockAnswer-rule"
              className="input bg-white focus:border-you"
            />
            <p id="lockAnswer-rule" className="mt-2 text-sm">띄어쓰기와 대소문자는 구분하지 않아요</p>
          </div>
          <button className="btn bg-you">잠금 저장</button>
          {me.lockQuestion && (
            <button type="button" onClick={() => setEditing(false)} className="link -mt-1 py-1 text-you-ink">
              취소
            </button>
          )}
        </form>
      ) : (
        <>
          <div className="mt-4 rounded-[20px] bg-white p-4 text-ink">
            <p className="text-sm text-muted">잠금 질문</p>
            <p className="mt-1 text-[17px] font-bold">{me.lockQuestion}</p>
            <p className="mt-3 select-all break-all text-sm text-muted">{url}</p>
          </div>
          <button onClick={share} className="btn mt-4 bg-you">
            링크 보내기
          </button>
          <button onClick={() => setEditing(true)} className="link mt-3 block w-full py-1 text-center text-you-ink">
            잠금 질문 바꾸기
          </button>
        </>
      )}
      {notice && (
        <p role="status" className="mt-3 text-center text-sm font-semibold">
          {notice}
        </p>
      )}
    </section>
  );
}

function AnswerItem({ q, color, onSaved }: { q: QuestionView; color: string; onSaved: () => void }) {
  const [saved, setSaved] = useState(q.answer ?? "");
  const [draft, setDraft] = useState(saved);
  const [justSaved, setJustSaved] = useState(false);
  const [error, setError] = useState("");
  const id = `answer-${q.id}`;
  const dirty = draft.trim() !== "" && draft.trim() !== saved;

  async function save() {
    setError("");
    try {
      await api(`/api/questions/${q.id}/answer`, "PUT", { content: draft });
      setSaved(draft.trim());
      setJustSaved(true);
      onSaved();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <li className={`tile p-5 ${color}`}>
      <div className="mb-3 flex items-start justify-between gap-3">
        <label htmlFor={id} className="text-[17px] font-bold">
          {q.content}
        </label>
        {saved && <span className="shrink-0 rounded-full bg-white/80 px-2.5 py-0.5 text-xs font-semibold">답함</span>}
      </div>
      <textarea
        id={id}
        value={draft}
        onChange={(e) => {
          setDraft(e.target.value);
          setJustSaved(false);
        }}
        maxLength={500}
        rows={1}
        placeholder="여기에 답을 적어요"
        className="input resize-none bg-white/80 text-ink field-sizing-content focus:border-current"
      />
      {(dirty || justSaved || error) && (
        <div className="mt-3 flex min-h-10 items-center justify-end gap-3">
          {error && <p className="mr-auto text-sm font-medium text-negative">{error}</p>}
          {justSaved && !dirty && <p className="text-sm font-medium">저장했어요</p>}
          {dirty && (
            <button onClick={save} className="btn-sm">
              저장
            </button>
          )}
        </div>
      )}
    </li>
  );
}
