const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export type Me = {
  nickname: string;
  shareToken: string;
  /** null until set; the share link stays closed until then */
  lockQuestion: string | null;
};

export type QuestionView = { id: number; content: string; answer: string | null };

export type Unlocked = { nickname: string; answers: { question: string; answer: string }[] };

export async function api<T = void>(path: string, method = "GET", body?: unknown): Promise<T> {
  const token = localStorage.getItem("token");
  const res = await fetch(API_URL + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token && { Authorization: `Bearer ${token}` }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (res.status === 401 && token) {
    localStorage.removeItem("token");
    location.href = "/login";
  }
  if (!res.ok) {
    const err = await res.json().catch(() => null);
    throw new Error(err?.message || "요청에 실패했어요");
  }
  const text = await res.text();
  return text ? JSON.parse(text) : (undefined as T);
}
