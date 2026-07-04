import "./App.css";

import { useState } from "react";

import Login from "./components/Login";
import Register from "./components/Register";
import Chat from "./components/Chat";

function App() {

  const [isLogin, setIsLogin] = useState(true);

  const [isAuthenticated, setIsAuthenticated] = useState(false);

  const [currentUser, setCurrentUser] = useState("");

  // LOGIN

  const handleLogin = async (email, password) => {

    try {

      const response = await fetch("http://localhost:8080/auth/login", {

        method: "POST",

        headers: {
          "Content-Type": "application/json",
        },

        body: JSON.stringify({
          email,
          password,
        }),
      });

      const data = await response.text();

      if (response.ok) {

        localStorage.setItem("token", data);

        setCurrentUser(email);

        setIsAuthenticated(true);

      } else {

        alert(data);
      }

    } catch (error) {

      alert("Server Error");
    }
  };

  // REGISTER

  const handleRegister = async (name, email, password) => {

    try {

      const response = await fetch("http://localhost:8080/auth/register", {

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

      const data = await response.text();

      alert(data);

      setIsLogin(true);

    } catch (error) {

      alert("Server Error");
    }
  };

  // CHAT PAGE

  if (isAuthenticated) {

    return (

      <Chat
        currentUser={currentUser}

        onLogout={() => {

          localStorage.removeItem("token");

          setIsAuthenticated(false);
        }}
      />
    );
  }

  return (

    <>
      {
        isLogin
          ? (
            <Login
              onLogin={handleLogin}
              goRegister={() => setIsLogin(false)}
            />
          )
          : (
            <Register
              onRegister={handleRegister}
              switchToLogin={() => setIsLogin(true)}
            />
          )
      }
    </>
  );
}

export default App;