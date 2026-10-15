import { getToken, isTokenExpired } from "../auth/Token.jsx"

export async function apiRequest(url, options = {}) {
  const { skipAuth = false, body, headers: extraHeaders, ...rest } = options;
  const isJsonBody = body != null && typeof body !== 'string' && !(body instanceof FormData);
  const headers = {
    ...(isJsonBody ? { 'Content-Type': 'application/json' } : {}),
    ...(extraHeaders || {}),
  };

  const token = skipAuth ? null : getToken();
  if (token && isTokenExpired(token)) {
    window.dispatchEvent(new Event('auth:expired'));
    throw new Error('인증이 만료되었습니다.');
  }
  if (token && !headers.Authorization)
    headers.Authorization = `Bearer ${token}`;

  const response = await fetch(url, { ...rest, headers, body: isJsonBody ? JSON.stringify(body) : body });
  if (response.status === 401 && token)
    window.dispatchEvent(new Event('auth:expired'));

  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : {};
  }
  catch {
    throw new Error(`서버가 JSON이 아닌 응답을 반환했습니다. (HTTP ${response.status})`);
  }
  if (!response.ok) {
    const detail = data.message || data.msg || data.errors?.map((item) => item.reason || item.message).join(', ');
    const error = new Error(detail || `요청에 실패했습니다. (HTTP ${response.status})`);
    error.status = response.status;
    error.data = data;
    throw error;
  }
  return data;
}

export const api = {
  get:   (url, opts) => apiRequest(url, opts),
  post:  (url, body, opts) => apiRequest(url, { ...opts, method: 'POST', body }),
  patch: (url, body, opts) => apiRequest(url, { ...opts, method: 'PATCH', body }),
  del:   (url, body, opts) => apiRequest(url, { ...opts, method: 'DELETE', body }),
};