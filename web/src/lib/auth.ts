const TOKEN_KEY = "laborflow_token";
const USER_KEY = "laborflow_user";

export type LoginUser = {
  token?: string | null;
  userId: string;
  employeeId?: string | null;
  username: string;
  nickname?: string | null;
  roles: string[];
};

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function getLoginUser(): LoginUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as LoginUser;
  } catch {
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

export function setLoginUser(user: LoginUser) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
