import { useState } from "react";

import "../Auth.css";

function Login({
  onLogin,
  goRegister,
}) {

  const [email, setEmail] =
    useState("");

  const [password, setPassword] =
    useState("");

  const handleLogin = async () => {

    try {
      await onLogin(email, password);

    } catch {

      alert(
        "Server error"
      );
    }
  };

  return (

    <div className="auth-container">

      {/* LEFT SIDE */}

      <div className="auth-left">

        <div className="glow glow1"></div>

        <div className="glow glow2"></div>

        <div className="brand-content">

          <h1>
            ChatFlow
          </h1>

          <p>
            Connect instantly with
            friends in a modern
            realtime messaging
            experience.
          </p>

          <div className="feature-pill">

            💬 Realtime Messaging

          </div>

          <div className="feature-pill second">

            🔥 Modern UI Experience

          </div>

        </div>

      </div>

      {/* RIGHT SIDE */}

      <div className="auth-right">

        <div className="auth-card">

          <h2>
            Welcome Back
          </h2>

          <span>
            Login to continue
          </span>

          <div className="input-group">

            <input

              type="email"

              required

              value={email}

              onChange={(e) =>
                setEmail(
                  e.target.value
                )
              }
            />

            <label>
              Email Address
            </label>

          </div>

          <div className="input-group">

            <input

              type="password"

              required

              value={password}

              onChange={(e) =>
                setPassword(
                  e.target.value
                )
              }
            />

            <label>
              Password
            </label>

          </div>

          <button
            onClick={handleLogin}
          >

            Login

          </button>

          <p
            className="switch-auth"

            onClick={
              goRegister
            }
          >

            Create account

          </p>

        </div>

      </div>

    </div>
  );
}

export default Login;