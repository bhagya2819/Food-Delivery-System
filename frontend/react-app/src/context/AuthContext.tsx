import Keycloak from 'keycloak-js';
import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react';

const keycloak = new Keycloak({
  url: 'http://localhost:9990',
  realm: 'food-delivery',
  clientId: 'food-delivery-web',
});

interface AuthContextType {
  initialized: boolean;
  authenticated: boolean;
  token: string | null;
  login: () => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType>({
  initialized: false,
  authenticated: false,
  token: null,
  login: () => {},
  logout: () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const runRef = useRef(false);

  useEffect(() => {
    if (runRef.current) return;
    runRef.current = true;

    keycloak
      .init({ onLoad: 'check-sso', silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html' })
      .then((auth) => {
        setAuthenticated(auth);
        if (auth && keycloak.token) {
          localStorage.setItem('token', keycloak.token);
        }
      })
      .catch(() => {
        setAuthenticated(false);
      })
      .finally(() => {
        setInitialized(true);
      });

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).catch(() => {
        setAuthenticated(false);
        localStorage.removeItem('token');
      });
    };
  }, []);

  const login = () => keycloak.login();
  const logout = () => {
    keycloak.logout();
    localStorage.removeItem('token');
    setAuthenticated(false);
  };

  return (
    <AuthContext.Provider
      value={{
        initialized,
        authenticated,
        token: keycloak.token || null,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
