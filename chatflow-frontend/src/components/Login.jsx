import { useState } from "react";

import "../Auth.css";

function Login({
  onLogin,
  goRegister,
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = async () => {
    try {
      await onLogin(email, password);
    } catch {
      alert("Server error");
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-left">
        <div className="glow glow1"></div>
        <div className="glow glow2"></div>

        <div className="brand-content">
          <h1>ChatFlow</h1>
          <p>
            Request-based private messaging with encrypted storage, profile controls,
            and a cleaner inspect-inspired interface.
          </p>
          <div className="feature-pill">Realtime messaging</div>
          <div className="feature-pill second">Privacy and requests</div>
        </div>
      </div>

      <div className="auth-right">
        <div className="auth-card">
          <h2>Welcome back</h2>
          <span>Login to continue chatting</span>

          <div className="input-group">
            <input
              type="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
            <label>Email address</label>
          </div>

          <div className="input-group">
            <input
              type="password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
            <label>Password</label>
          </div>

          <button type="button" onClick={handleLogin}>Login</button>

          <p className="switch-auth" onClick={goRegister}>
            Create account
          </p>
        </div>
      </div>
    </div>
  );
}

export default Login;
