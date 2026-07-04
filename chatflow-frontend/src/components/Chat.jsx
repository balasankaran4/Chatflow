import {
  useEffect,
  useRef,
  useState,
} from "react";

import "../Chat.css";

import { Client } from "@stomp/stompjs";

import SockJS from "sockjs-client/dist/sockjs";

import EmojiPicker from "emoji-picker-react";

function Chat({
  currentUser,
  onLogout,
}) {

  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState([]);
  const [users, setUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState("");
  const [typing, setTyping] = useState(false);
  const [showEmoji, setShowEmoji] = useState(false);
  const [image, setImage] = useState(null);

  const messagesEndRef = useRef(null);
  const stompClient = useRef(null);

  useEffect(() => {
    fetch("http://localhost:8080/auth/users")
      .then((res) => res.json())
      .then((data) => {
        const filteredUsers = data.filter((user) => user.email !== currentUser);
        setUsers(filteredUsers);

        if (filteredUsers.length > 0) {
          setSelectedUser(filteredUsers[0].email);
        }
      });
  }, [currentUser]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  const loadMessages = () => {
    if (!selectedUser) return;

    fetch(`http://localhost:8080/messages?sender=${currentUser}&receiver=${selectedUser}`)
      .then((res) => res.json())
      .then((data) => {
        setMessages(data);

        setTimeout(() => {
          scrollToBottom();
        }, 100);
      })
      .catch((error) => {
        console.log(error);
      });
  };

  useEffect(() => {
    if (!selectedUser) return;

    loadMessages();

    const client = new Client({
      webSocketFactory: () => new SockJS("http://localhost:8080/chat"),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log("WebSocket Connected");

        client.subscribe("/topic/messages", (msg) => {
          const receivedMessage = JSON.parse(msg.body);

          if (
            (receivedMessage.sender === currentUser && receivedMessage.receiver === selectedUser) ||
            (receivedMessage.sender === selectedUser && receivedMessage.receiver === currentUser)
          ) {
            setMessages((prev) => [
              ...prev,
              receivedMessage,
            ]);

            setTimeout(() => {
              scrollToBottom();
            }, 100);
          }
        });
      },
      onStompError: (frame) => {
        console.log("Broker Error", frame);
      },
    });

    client.activate();
    stompClient.current = client;

    return () => {
      client.deactivate();
    };
  }, [selectedUser, currentUser]);

  const sendMessage = () => {
    console.log(
      "sendMessage called - message:",
      message,
      "image:",
      image ? "yes" : "no"
    );

    if (message.trim() === "" && !image) {
      console.warn("Message empty and no image");
      return;
    }

    if (!stompClient.current) {
      console.error("WebSocket not connected");
      alert("Not connected. Please refresh.");
      return;
    }

    const currentTime = new Date().toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });

    const newMessage = {
      sender: currentUser,
      receiver: selectedUser,
      text: message || "",
      image: image || "",
      time: currentTime,
      typing: false,
    };

    console.log("Publishing message:", newMessage);

    try {
      stompClient.current.publish({
        destination: "/app/sendMessage",
        body: JSON.stringify(newMessage),
      });

      console.log("Message sent successfully");
    } catch (error) {
      console.error("Error publishing message:", error);
      alert("Failed to send message: " + error.message);
    }

    setMessage("");
    setImage(null);
    setShowEmoji(false);
  };

  return (
    <div className="chat-app">
      <div className="sidebar">
        <div className="sidebar-glow"></div>

        <div className="sidebar-top">
          <div>
            <h1 className="logo">ChatFlow</h1>
            <p className="current-user">{currentUser}</p>
          </div>

          <div className="profile-orb">{currentUser[0]?.toUpperCase()}</div>
        </div>

        <div className="search-wrapper">
          <input
            type="text"
            placeholder="Search conversations..."
            className="search-box"
          />
        </div>

        <div className="orbit-section">
          <div className="orbit-title">Active Now</div>

          <div className="active-horizon-slider">
            {users.slice(0, 8).map((user, index) => (
              <div key={index} className="orbit-user">
                <div className="horizon-avatar">{user.name[0]?.toUpperCase()}</div>
                <span>{user.name}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="user-list">
          {users.map((user, index) => (
            <div
              key={index}
              className={selectedUser === user.email ? "chat-user active" : "chat-user"}
              onClick={() => setSelectedUser(user.email)}
            >
              <div className="card-glow"></div>
              <div className="avatar">{user.name[0]?.toUpperCase()}</div>
              <div className="user-details">
                <h3>{user.name}</h3>
                <p>Online</p>
              </div>
              <div className="signal-line"></div>
            </div>
          ))}
        </div>
      </div>

      <div className="chat-area">
        <div className="chat-background-glow"></div>

        <div className="chat-header">
          <div className="header-left">
            <div className="avatar large-avatar">{selectedUser[0]?.toUpperCase()}</div>

            <div>
              <h2>{selectedUser}</h2>
              <span>Online • Secure Channel</span>
            </div>
          </div>

          <div className="header-actions">
            <button className="glass-action">✦</button>
            <button className="logout-btn" onClick={onLogout}>Logout</button>
          </div>
        </div>

        <div className="messages">
          {typing && <div className="typing-text">typing...</div>}

          {messages.map((msg, index) => (
            <div
              key={index}
              className={msg.sender === currentUser ? "message my-message" : "message other-message"}
            >
              <div className="message-accent"></div>

              {msg.text && msg.text !== "" && <p>{msg.text}</p>}

              {msg.image && msg.image !== "" && (
                <img src={msg.image} alt="chat" className="chat-image" />
              )}

              <span className="msg-time">{msg.time}</span>
            </div>
          ))}

          <div ref={messagesEndRef}></div>
        </div>

        <div className="message-input">
          <label className="image-upload">
            📷
            <input
              type="file"
              hidden
              accept="image/*"
              onChange={(e) => {
                const file = e.target.files[0];

                if (file) {
                  const formData = new FormData();
                  formData.append("file", file);

                  console.log("Uploading file:", file.name, file.size);

                  fetch("http://localhost:8080/messages/upload", {
                    method: "POST",
                    body: formData,
                  })
                    .then((res) => {
                      console.log("Upload response status:", res.status);
                      if (!res.ok) {
                        throw new Error("Upload failed: " + res.statusText);
                      }
                      return res.json();
                    })
                    .then((data) => {
                      console.log("Upload response:", data);
                      if (data.imageUrl) {
                        setImage(data.imageUrl);
                        console.log("Image set:", data.imageUrl);
                      } else {
                        console.error("No imageUrl in response", data);
                        alert("Failed to upload image");
                      }
                    })
                    .catch((error) => {
                      console.error("Upload error:", error);
                      alert("Upload failed: " + error.message);
                    });
                }
              }}
            />
          </label>

          <div className="emoji-box">
            <button
              type="button"
              className="emoji-btn"
              onClick={() => setShowEmoji(!showEmoji)}
            >
              😀
            </button>

            {showEmoji && (
              <div className="emoji-picker">
                <EmojiPicker
                  onEmojiClick={(emojiData) => {
                    setMessage((prev) => prev + emojiData.emoji);
                  }}
                />
              </div>
            )}
          </div>

          {image && (
            <div className="image-preview-container">
              <div className="image-preview">
                <img src={image} alt="preview" />
                <button
                  type="button"
                  className="remove-image-btn"
                  onClick={() => {
                    setImage(null);
                    console.log("Image removed");
                  }}
                >
                  ✕
                </button>
              </div>
            </div>
          )}

          <input
            type="text"
            placeholder="Transmit message..."
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                sendMessage();
              }
            }}
          />

          <button
            type="button"
            className="send-button"
            onClick={sendMessage}
          >
            Send
          </button>
        </div>
      </div>
    </div>
  );
}

export default Chat;
