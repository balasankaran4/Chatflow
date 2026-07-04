import { useState } from "react";

import "../Auth.css";

function Register({
  onRegister,
  switchToLogin,
}) {

  const [name, setName] =
    useState("");

  const [email, setEmail] =
    useState("");

  const [password, setPassword] =
    useState("");

  const handleRegister =
    async () => {

      try {
        await onRegister(
          name,
          email,
          password
        );

        switchToLogin();

      } catch {

        alert(
          "Server error"
        );
      }
    };

  return (

    <div className="auth-container">

      {/* LEFT */}

      <div className="auth-left">

        <div className="glow glow1"></div>

        <div className="glow glow2"></div>

        <div className="brand-content">

          <h1>
            ChatFlow
          </h1>

          <p>
            Create your account and
            experience the next
            generation realtime
            messaging app.
          </p>

          <div className="feature-pill">

            🚀 Fast & Secure

          </div>

          <div className="feature-pill second">

            ❤️ Beautiful Experience

          </div>

        </div>

      </div>

      {/* RIGHT */}

      <div className="auth-right">

        <div className="auth-card">

          <h2>
            Create Account
          </h2>

          <span>
            Join ChatFlow
          </span>

          <div className="input-group">

            <input

              type="text"

              required

              value={name}

              onChange={(e) =>
                setName(
                  e.target.value
                )
              }
            />

            <label>
              Full Name
            </label>

          </div>

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
            onClick={
              handleRegister
            }
          >

            Register

          </button>

          <p
            className="switch-auth"

            onClick={
              switchToLogin
            }
          >

            Already have account?

          </p>

        </div>

      </div>

    </div>
  );
}

export default Register;