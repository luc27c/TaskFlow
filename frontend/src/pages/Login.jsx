import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../services/api';

function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const data = await authApi.login(email, password);
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify({
        email: data.email,
        firstName: data.firstName,
        lastName: data.lastName,
      }));
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-amber-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo/Brand */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 bg-teal-600 rounded-2xl mb-4 shadow-md">
            <svg className="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-stone-800">TaskFlow</h1>
          <p className="text-stone-500 mt-1">Your email, summarized</p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl p-7 shadow-md border border-stone-100">
          <h2 className="text-xl font-semibold text-stone-800 mb-1">Welcome back!</h2>
          <p className="text-stone-400 text-sm mb-6">Sign in to manage your workflows</p>

          {error && (
            <div className="bg-red-50 border-l-4 border-red-400 text-red-700 px-4 py-3 rounded-r-md mb-5 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-stone-700 text-sm font-medium mb-1.5">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-stone-800 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 focus:bg-white transition"
                placeholder="you@example.com"
                required
              />
            </div>

            <div>
              <label className="block text-stone-700 text-sm font-medium mb-1.5">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-stone-800 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 focus:bg-white transition"
                placeholder="••••••••"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 bg-teal-600 hover:bg-teal-700 disabled:bg-teal-400 text-white font-semibold rounded-xl transition duration-150 flex items-center justify-center gap-2 shadow-sm"
            >
              {loading ? (
                <>
                  <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Signing in...
                </>
              ) : (
                'Sign in'
              )}
            </button>
          </form>

          <div className="mt-6 text-center text-sm">
            <span className="text-stone-500">New here? </span>
            <Link to="/register" className="text-teal-600 font-medium hover:text-teal-700">
              Create an account
            </Link>
          </div>
        </div>

        <p className="text-center text-stone-400 text-xs mt-6">Get daily email summaries powered by AI</p>
      </div>
    </div>
  );
}

export default Login;
