"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { api } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [signup, setSignup] = useState(false);
  const [error, setError] = useState("");

  async function submit(form: FormData) {
    setError("");
    try {
      const { token } = await api<{ token: string }>(
        signup ? "/api/auth/signup" : "/api/auth/login",
        "POST",
        Object.fromEntries(form),
      );
      localStorage.setItem("token", token);
      router.replace("/");
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <div className="pt-6">
      <section className="tile bg-me-soft text-me-ink">
        <h1 className="text-[32px] font-extrabold tracking-tight">getToKnow</h1>
        <p className="mt-2 text-[17px] font-medium leading-relaxed">
          물어봐야만 알 수 있는 것들을 적고,
          <br />
          정답을 아는 사람에게만 보여줘요.
        </p>
        {/* A sample of what a shared answer looks like */}
        <div className="mt-6 rounded-[20px] bg-white p-4" aria-hidden>
          <p className="text-sm text-muted">좋아하는 색깔은?</p>
          <p className="mt-1 text-lg font-bold text-ink">하늘색. 근데 옷은 검정만 입어</p>
        </div>
        <div className="mt-2 ml-auto w-fit rounded-[20px] bg-you-soft px-4 py-3 text-you-ink" aria-hidden>
          <p className="text-sm font-semibold">처음 알았어!</p>
        </div>
      </section>

      <form action={submit} className="card mt-4 flex flex-col gap-4">
        <h2 className="text-lg font-bold">{signup ? "가입하기" : "로그인"}</h2>
        <div>
          <label htmlFor="email" className="label">이메일</label>
          <input id="email" name="email" type="email" required autoComplete="email" className="input" />
        </div>
        <div>
          <label htmlFor="password" className="label">비밀번호</label>
          <input
            id="password"
            name="password"
            type="password"
            required
            minLength={signup ? 8 : undefined}
            autoComplete={signup ? "new-password" : "current-password"}
            aria-describedby={signup ? "password-rule" : undefined}
            className="input"
          />
          {signup && <p id="password-rule" className="mt-2 text-sm text-muted">8자 이상</p>}
        </div>
        {signup && (
          <div>
            <label htmlFor="nickname" className="label">닉네임</label>
            <input id="nickname" name="nickname" required maxLength={20} className="input" />
          </div>
        )}
        {error && <p role="alert" className="text-sm font-medium text-negative">{error}</p>}
        <button className="btn mt-2">{signup ? "가입하기" : "로그인"}</button>
      </form>

      <button onClick={() => setSignup(!signup)} className="link mt-5 block w-full py-2 text-center">
        {signup ? "이미 계정이 있어요" : "처음이에요, 가입할래요"}
      </button>
    </div>
  );
}
