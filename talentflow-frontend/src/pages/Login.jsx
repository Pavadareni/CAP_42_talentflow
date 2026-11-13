
import { useState } from 'react';
import { ArrowRight, Network, ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();

    setError('');

    if (!email.trim() || !password) {
      setError('Please enter your work email and password.');
      return;
    }

    setLoading(true);

    try {
      await login(email.trim().toLowerCase(), password);
    } catch (err) {
      const status = err.response?.status;

      if (status === 401) {
        setError(
          'Incorrect email or password. Please check your credentials and try again.'
        );
      } else if (status === 403) {
        setError(
          'Your account is disabled or you do not have permission to sign in.'
        );
      } else if (!err.response) {
        setError(
          'Unable to connect to TalentFlow. Please check your connection.'
        );
      } else {
        setError(
          err.response?.data?.message ||
          'Unable to sign in. Please try again later.'
        );
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">

      {/* LEFT BRANDING SECTION */}

      <section className="login-visual">

        <div className="login-brand">
          <i>
            <Network size={23} />
          </i>
          <b>TalentFlow</b>
        </div>

        <div className="hero">

          <small>WORKFORCE INTELLIGENCE PLATFORM</small>

          <h1>
            Put the right skills on the right projects.
          </h1>

          <p>
            Make workforce planning clearer with intelligent allocation,
            transparent decisions and real-time capacity visibility.
          </p>

          <div className="trust">
            <ShieldCheck size={19} />
            Role-based access and auditable approvals
          </div>

        </div>

        <div className="footnote">
          EMPLOYEE SKILLS-TO-PROJECT ALLOCATION OPTIMIZER
        </div>

      </section>

      {/* RIGHT LOGIN SECTION */}

      <section className="login-form">

        <form onSubmit={handleSubmit}>

          <small className="eyebrow">WELCOME BACK</small>

          <h2>Sign in to TalentFlow</h2>

          <p>Enter your work account details to continue.</p>

          <label htmlFor="email">Work email</label>

          <input
            id="email"
            type="email"
            placeholder="employee@company.com"
            autoComplete="username"
            value={email}
            onChange={e => {
              setEmail(e.target.value);
              setError('');
            }}
            required
          />

          <label htmlFor="password">Password</label>

          <input
            id="password"
            type="password"
            placeholder="Enter your password"
            autoComplete="current-password"
            value={password}
            onChange={e => {
              setPassword(e.target.value);
              setError('');
            }}
            required
          />

          {error && (
            <div className="error" role="alert">
              {error}
            </div>
          )}

          <button
            className="primary full"
            type="submit"
            disabled={loading}
          >
            <span>{loading ? 'Signing in...' : 'Sign in'}</span>
            <ArrowRight size={19} />
          </button>

          <div className="secure">
            <ShieldCheck size={16} />
            Your session is protected by secure authentication.
          </div>

        </form>

      </section>

    </div>
  );
}
