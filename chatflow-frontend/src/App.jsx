import "./App.css";

import { useState } from "react";

import Chat from "./components/Chat";
import Login from "./components/Login";
import Register from "./components/Register";

const API_BASE = "http://localhost:8080";

function App() {
  const [isLogin, setIsLogin] = useState(true);
  const [currentUser, setCurrentUser] = useState(() => localStorage.getItem("currentUser") || "");
  const [isAuthenticated, setIsAuthenticated] = useState(() => (
    Boolean(localStorage.getItem("token") && localStorage.getItem("currentUser"))
  ));

  const handleLogin = async (email, password) => {
    try {
      const response = await fetch(`${API_BASE}/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
          password,
        }),
      });

      const data = await response.json();

      if (response.ok) {
        localStorage.setItem("token", data.token);
        localStorage.setItem("currentUser", data.user.email);
        setCurrentUser(data.user.email);
        setIsAuthenticated(true);
        return;
      }

      alert(data.message || "Login failed.");
    } catch {
      alert("Server Error");
    }
  };

  const handleRegister = async (name, email, password) => {
    try {
      const response = await fetch(`${API_BASE}/auth/register`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          name,
          email,
          password,
        }),
      });

      const data = await response.json();
      alert(data.message || "Registration finished.");

      if (response.ok) {
        setIsLogin(true);
      }
    } catch {
      alert("Server Error");
    }
  };

  if (isAuthenticated) {
    return (
      <Chat
        currentUser={currentUser}
        onLogout={() => {
          localStorage.removeItem("token");
          localStorage.removeItem("currentUser");
          setCurrentUser("");
          setIsAuthenticated(false);
          setIsLogin(true);
        }}
      />
    );
  }

  return isLogin ? (
    <Login
      onLogin={handleLogin}
      goRegister={() => setIsLogin(false)}
    />
  ) : (
    <Register
      onRegister={handleRegister}
      switchToLogin={() => setIsLogin(true)}
    />
  );
}

export default App;