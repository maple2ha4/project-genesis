export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}
let authorization = "";
let csrf = "";
export function credentials(username: string, password: string) {
  authorization =
    "Basic " +
    btoa(
      String.fromCharCode(
        ...new TextEncoder().encode(username + ":" + password),
      ),
    );
  csrf = "";
}
export async function api<T>(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const response = await fetch("/api" + path, {
    method,
    credentials: "same-origin",
    headers: {
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
      ...(authorization ? { Authorization: authorization } : {}),
      ...(method !== "GET" ? { "X-CSRF-TOKEN": csrf } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    let message = "网络请求失败，请重试";
    try {
      message = (await response.json()).message || message;
    } catch {}
    throw new ApiError(
      response.status === 403 ? "安全会话已过期，请重新加载工作室" : message,
      response.status,
    );
  }
  return response.json();
}
export async function session() {
  const s = await api<{ csrf: string }>("/session");
  csrf = s.csrf;
}
