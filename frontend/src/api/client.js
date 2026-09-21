const TOKEN_KEY = 'eats-a-deal-token';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export async function apiRequest(url, options = {}) {
  const headers = {
    ...(options.body && typeof options.body !== 'string' ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {}),
  };
  const token = getToken();
  if (token && !headers.Authorization) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(url, { ...options, headers });
  const text = await response.text();
  let data = {};
  try { data = text ? JSON.parse(text) : {}; } catch {
    throw new Error(`서버가 JSON이 아닌 응답을 반환했습니다. (HTTP ${response.status})`);
  }
  if (!response.ok) {
    const detail = data.message || data.msg || data.errors?.map((item) => item.reason || item.message).join(', ');
    const error = new Error(detail || `요청에 실패했습니다. (HTTP ${response.status})`);
    error.status = response.status;
    throw error;
  }
  return data;
}
