/**
 * SSO Auth Helper — React / Vanilla JS
 * Drop this file into Cesium, CLM, or Helix.
 *
 * Usage:
 *   import { SSOAuth } from './sso-auth';
 *   const auth = new SSOAuth({ ssoBaseUrl: 'http://localhost:8080' });
 *
 *   await auth.requireLogin();      // call this at app startup
 *   const user = auth.getUsername();
 *   auth.logout();
 */

const SSO_BASE_URL = 'http://localhost:8080'; // change to deployed URL in production
const TOKEN_KEY = 'sso_jwt_token';
const USERNAME_KEY = 'sso_username';

export class SSOAuth {
  constructor({ ssoBaseUrl = SSO_BASE_URL } = {}) {
    this.ssoBaseUrl = ssoBaseUrl;
  }

  /**
   * Call this at the start of your app.
   * - If token exists and is valid → lets the user through.
   * - If token is missing/expired → redirects to SSO login page.
   */
  async requireLogin() {
    // Check if SSO redirected back with a token in the URL
    const urlToken = this._extractTokenFromUrl();
    if (urlToken) {
      localStorage.setItem(TOKEN_KEY, urlToken);
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    const token = localStorage.getItem(TOKEN_KEY);

    if (!token) {
      this._redirectToLogin();
      return false;
    }

    const result = await this.validate(token);
    if (!result.valid) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USERNAME_KEY);
      this._redirectToLogin();
      return false;
    }

    localStorage.setItem(USERNAME_KEY, result.username);
    return true;
  }

  /**
   * Validate a token against the SSO /validate endpoint.
   * Returns { valid: true, username: '...' } or { valid: false }
   */
  async validate(token) {
    try {
      const res = await fetch(`${this.ssoBaseUrl}/validate`, {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}` },
      });
      return await res.json();
    } catch {
      return { valid: false };
    }
  }

  /** Returns the logged-in username, or null */
  getUsername() {
    return localStorage.getItem(USERNAME_KEY);
  }

  /** Returns the stored JWT token, or null */
  getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  /** Clears local session and redirects to SSO logout */
  logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    window.location.href = `${this.ssoBaseUrl}/logout`;
  }

  // ── private helpers ──────────────────────────────────────────────

  _redirectToLogin() {
    const returnUrl = encodeURIComponent(window.location.href);
    window.location.href = `${this.ssoBaseUrl}/login?redirect=${returnUrl}`;
  }

  _extractTokenFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get('token');
  }
}

// ── React hook (optional, only used in React apps) ─────────────────

/**
 * React hook — wraps SSOAuth for use in functional components.
 *
 * Example:
 *   const { username, loading } = useSSO();
 *   if (loading) return <p>Checking login...</p>;
 *   return <p>Welcome, {username}</p>;
 */
export function useSSO(ssoBaseUrl = SSO_BASE_URL) {
  // Dynamic import so non-React apps don't break
  const React = globalThis.React;
  if (!React) throw new Error('useSSO requires React in scope');

  const [state, setState] = React.useState({ loading: true, username: null });
  const auth = React.useMemo(() => new SSOAuth({ ssoBaseUrl }), [ssoBaseUrl]);

  React.useEffect(() => {
    auth.requireLogin().then((ok) => {
      if (ok) setState({ loading: false, username: auth.getUsername() });
    });
  }, [auth]);

  return { ...state, auth };
}
