
import {
  createContext,
  useContext,
  useMemo,
  useState
} from 'react';

import { api } from '../api/client';

const AuthContext = createContext(null);

function getRole(token, response) {
  if (response.role) {
    return String(response.role)
      .replace('ROLE_', '')
      .toUpperCase();
  }

  try {
    const payload = JSON.parse(
      atob(
        token.split('.')[1]
          .replace(/-/g, '+')
          .replace(/_/g, '/')
      )
    );

    const roles =
      payload.roles ||
      payload.authorities ||
      ['EMPLOYEE'];

    const role = Array.isArray(roles)
      ? roles[0]
      : roles;

    return String(role)
      .replace('ROLE_', '')
      .toUpperCase();

  } catch {
    return 'EMPLOYEE';
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(
    localStorage.getItem('tf_token')
  );

  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(
        localStorage.getItem('tf_user') || 'null'
      );
    } catch {
      return null;
    }
  });

  const login = async (email, password) => {
    try {
      const data = await api.login({
        email,
        password
      });

      const jwt =
        data.accessToken ||
        data.token ||
        data.jwt;

      if (!jwt) {
        throw new Error(
          'Login response has no token. Check LoginResponse DTO.'
        );
      }

      const nextUser = {
        email: data.email || email,
        name: data.name || email.split('@')[0],
        role: getRole(jwt, data)
      };

      localStorage.setItem('tf_token', jwt);
      localStorage.setItem(
        'tf_user',
        JSON.stringify(nextUser)
      );

      setToken(jwt);
      setUser(nextUser);

      return nextUser;

    } catch (error) {
      localStorage.removeItem('tf_token');
      localStorage.removeItem('tf_user');

      setToken(null);
      setUser(null);

      throw error;
    }
  };

  const logout = () => {
    localStorage.removeItem('tf_token');
    localStorage.removeItem('tf_user');

    setToken(null);
    setUser(null);
  };

  const value = useMemo(
    () => ({
      token,
      user,
      login,
      logout,
      isAuthenticated: !!token
    }),
    [token, user]
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
