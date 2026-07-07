import { useState } from "react";

import "../Auth.css";

function Register({
  onRegister,
  switchToLogin,
}) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleRegister = async () => {
    try {
      await onRegister(name, email, password);
      switchToLogin();
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
            Create your account to send requests, accept trusted contacts, manage
            privacy, and chat through the new inspect-inspired experience.
          </p>
          <div className="feature-pill">Encrypted message storage</div>
          <div className="feature-pill second">Contact approval flow</div>
        </div>
      </div>

      <div className="auth-right">
        <div className="auth-card">
          <h2>Create account</h2>
          <span>Join ChatFlow</span>

          <div className="input-group">
            <input
              type="text"
              required
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
            <label>Full name</label>
          </div>

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

          <button type="button" onClick={handleRegister}>Register</button>

          <p className="switch-auth" onClick={switchToLogin}>
            Already have an account?
          </p>
        </div>
      </div>
    </div>
  );
}

export default Register;
