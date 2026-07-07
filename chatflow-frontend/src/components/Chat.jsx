import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import EmojiPicker from "emoji-picker-react";
import SockJS from "sockjs-client/dist/sockjs";

import "../Chat.css";
import ImageInspector from "./ImageInspector";

const API_BASE = "http://localhost:8080";

function matchesConversation(message, firstUser, secondUser) {
  return Boolean(message && firstUser && secondUser && (
    (message.sender === firstUser && message.receiver === secondUser) ||
    (message.sender === secondUser && message.receiver === firstUser)
  ));
}

function mergeMessages(currentMessages, nextMessage) {
  const exists = currentMessages.some((message) => message.id === nextMessage.id);
  const nextMessages = exists
    ? currentMessages.map((message) => (message.id === nextMessage.id ? { ...message, ...nextMessage } : message))
    : [...currentMessages, nextMessage];

  return nextMessages.sort((left, right) => (left.createdAt || 0) - (right.createdAt || 0));
}

function getInitial(value) {
  return (value || "?").trim()[0]?.toUpperCase() || "?";
}

function isAcceptedContact(user) {
  return Boolean(user?.canChat && !user.blocked && !user.blockedBy);
}

function matchesSearch(user, searchValue) {
  const query = searchValue.trim().toLowerCase();

  if (!query) {
    return true;
  }

  return (user.name || "").toLowerCase().includes(query) || (user.email || "").toLowerCase().includes(query);
}

function pickConversationUser(users, previousUser) {
  const acceptedContacts = users.filter(isAcceptedContact);

  if (previousUser && acceptedContacts.some((user) => user.email === previousUser)) {
    return previousUser;
  }

  return acceptedContacts[0]?.email || "";
}

function getRelationshipState(user) {
  if (user.incomingRequest) {
    return { label: "Reply in requests", className: "incoming" };
  }

  if (user.outgoingRequest) {
    return { label: "Request sent", className: "outgoing" };
  }

  if (user.canChat) {
    return { label: "Connected", className: "unlocked" };
  }

  return { label: "Send request", className: "idle" };
}

function Chat({ currentUser, onLogout }) {
  const [activePage, setActivePage] = useState("chat");
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState([]);
  const [users, setUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState("");
  const [showEmoji, setShowEmoji] = useState(false);
  const [image, setImage] = useState("");
  const [inspectedImage, setInspectedImage] = useState(null);
  const [profile, setProfile] = useState(null);
  const [profileForm, setProfileForm] = useState({ name: "", profileImage: "" });
  const [contactSearch, setContactSearch] = useState("");
  const [peopleSearch, setPeopleSearch] = useState("");
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);

  const messagesEndRef = useRef(null);
  const emojiPickerRef = useRef(null);
  const actionMenuRef = useRef(null);
  const stompClient = useRef(null);

  const acceptedContacts = users.filter(isAcceptedContact);
  const selectedUserSummary = acceptedContacts.find((user) => user.email === selectedUser) || null;
  const filteredContacts = acceptedContacts.filter((user) => matchesSearch(user, contactSearch));
  const filteredPeople = users
    .filter((user) => !isAcceptedContact(user) && !user.blocked && !user.blockedBy)
    .filter((user) => matchesSearch(user, peopleSearch));
  const incomingRequests = profile?.incomingRequests || [];
  const outgoingRequests = profile?.outgoingRequests || [];
  const blockedUsers = profile?.blockedUsers || [];

  const scrollToBottom = () => messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });

  const syncProfile = (nextProfile) => {
    setProfile(nextProfile);
    setProfileForm({ name: nextProfile?.name || "", profileImage: nextProfile?.profileImage || "" });
  };

  const getDisplayName = (email) => {
    if (email === currentUser) {
      return profile?.name || "You";
    }

    return users.find((user) => user.email === email)?.name || email;
  };

  const getTickClassName = (chatMessage) => {
    if (chatMessage.seen) {
      return "tick-seen";
    }

    return chatMessage.delivered ? "tick-delivered" : "tick-sent";
  };

  const getTickText = (chatMessage) => (chatMessage.seen || chatMessage.delivered ? "\u2713\u2713" : "\u2713");
  const loadUsers = async () => {
    const response = await fetch(`${API_BASE}/auth/users?currentUser=${encodeURIComponent(currentUser)}`);
    const data = await response.json();
    setUsers(data);
    setSelectedUser((previousUser) => pickConversationUser(data, previousUser));
    return data;
  };

  useEffect(() => {
    let active = true;

    const loadInitialData = async () => {
      try {
        const [profileResponse, usersResponse] = await Promise.all([
          fetch(`${API_BASE}/auth/profile?email=${encodeURIComponent(currentUser)}`),
          fetch(`${API_BASE}/auth/users?currentUser=${encodeURIComponent(currentUser)}`),
        ]);
        const [profileData, usersData] = await Promise.all([profileResponse.json(), usersResponse.json()]);

        if (!active) {
          return;
        }

        syncProfile(profileData);
        setUsers(usersData);
        setSelectedUser((previousUser) => pickConversationUser(usersData, previousUser));
      } catch (error) {
        console.log(error);
        alert("Unable to load profile data.");
      }
    };

    loadInitialData();

    return () => {
      active = false;
    };
  }, [currentUser]);

  useEffect(() => {
    let active = true;

    const loadConversation = async () => {
      if (!selectedUserSummary) {
        if (active) {
          setMessages([]);
        }
        return;
      }

      if (active) {
        setLoadingMessages(true);
      }

      try {
        const response = await fetch(
          `${API_BASE}/messages?sender=${encodeURIComponent(currentUser)}&receiver=${encodeURIComponent(selectedUserSummary.email)}`
        );
        const data = await response.json();

        if (!active) {
          return;
        }

        setMessages(data);
        setTimeout(scrollToBottom, 100);
        await fetch(`${API_BASE}/messages/status/seen`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ currentUserEmail: currentUser, otherUserEmail: selectedUserSummary.email }),
        });
      } catch (error) {
        console.log(error);
      } finally {
        if (active) {
          setLoadingMessages(false);
        }
      }
    };

    loadConversation();

    return () => {
      active = false;
    };
  }, [selectedUserSummary, currentUser]);

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === "Escape") {
        setInspectedImage(null);
        setShowEmoji(false);
        setMenuOpen(false);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  useEffect(() => {
    if (!showEmoji) {
      return undefined;
    }

    const handlePointerDown = (event) => {
      if (!emojiPickerRef.current?.contains(event.target)) {
        setShowEmoji(false);
      }
    };

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, [showEmoji]);

  useEffect(() => {
    if (!menuOpen) {
      return undefined;
    }

    const handlePointerDown = (event) => {
      if (!actionMenuRef.current?.contains(event.target)) {
        setMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, [menuOpen]);

  useEffect(() => {
    const postStatus = async (path, otherUserEmail) => {
      await fetch(`${API_BASE}/messages/status/${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ currentUserEmail: currentUser, otherUserEmail }),
      });
    };

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE}/chat`),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe("/topic/messages", (frame) => {
          const liveMessage = JSON.parse(frame.body);

          if (liveMessage.receiver === currentUser) {
            if (liveMessage.sender === selectedUser) {
              postStatus("seen", liveMessage.sender).catch((error) => console.log(error));
            } else {
              postStatus("delivered", liveMessage.sender).catch((error) => console.log(error));
            }
          }

          if (matchesConversation(liveMessage, currentUser, selectedUser)) {
            setMessages((previousMessages) => mergeMessages(previousMessages, liveMessage));
            setTimeout(scrollToBottom, 60);
          }
        });

        client.subscribe("/topic/message-status", (frame) => {
          const statusEvent = JSON.parse(frame.body);
          setMessages((previousMessages) => previousMessages.map((chatMessage) => (
            chatMessage.id === statusEvent.messageId
              ? { ...chatMessage, delivered: statusEvent.delivered, seen: statusEvent.seen }
              : chatMessage
          )));
        });
      },
      onStompError: (frame) => console.log("Broker Error", frame),
    });

    client.activate();
    stompClient.current = client;

    return () => {
      client.deactivate();
    };
  }, [currentUser, selectedUser]);
  const uploadImageFile = async (file) => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${API_BASE}/messages/upload`, { method: "POST", body: formData });
    const data = await response.json();

    if (!response.ok || !data.imageUrl) {
      throw new Error(data.error || "Upload failed.");
    }

    return data.imageUrl;
  };

  const handleSendMessage = async () => {
    if (!selectedUserSummary || (!message.trim() && !image)) {
      return;
    }

    const currentTime = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    const response = await fetch(`${API_BASE}/messages`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        sender: currentUser,
        receiver: selectedUserSummary.email,
        text: message.trim(),
        image,
        time: currentTime,
      }),
    });
    const data = await response.json();

    if (!response.ok) {
      alert(data.message || "Unable to send message.");
      return;
    }

    setMessages((previousMessages) => mergeMessages(previousMessages, data));
    setMessage("");
    setImage("");
    setShowEmoji(false);
    setTimeout(scrollToBottom, 60);
  };

  const runProfileAction = async (path, targetEmail, successMessage, options = {}) => {
    const response = await fetch(`${API_BASE}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ currentUserEmail: currentUser, targetEmail }),
    });
    const data = await response.json();

    if (!response.ok) {
      alert(data.message || "Action failed.");
      return;
    }

    syncProfile(data);
    await loadUsers();
    setStatusMessage(successMessage);
    setMenuOpen(false);

    if (options.openChat) {
      setSelectedUser(targetEmail);
      setActivePage("chat");
    }
  };

  const saveProfile = async (profileOverrides = {}) => {
    const nextForm = { ...profileForm, ...profileOverrides };
    const response = await fetch(`${API_BASE}/auth/profile`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: currentUser,
        name: nextForm.name,
        bio: profile?.bio || "",
        profileImage: nextForm.profileImage,
      }),
    });
    const data = await response.json();

    if (!response.ok) {
      alert(data.message || "Unable to save profile.");
      return;
    }

    syncProfile(data);
    await loadUsers();
    setStatusMessage("Profile updated.");
  };

  const handleProfileImageUpload = async (file) => {
    try {
      const imageUrl = await uploadImageFile(file);
      setProfileForm((previousState) => ({ ...previousState, profileImage: imageUrl }));
    } catch (error) {
      alert(error.message);
    }
  };

  const openChat = (email) => {
    setSelectedUser(email);
    setActivePage("chat");
    setMenuOpen(false);
  };

  const renderAvatar = (user, className) => (
    <div className={className}>
      {user?.profileImage ? (
        <img src={user.profileImage} alt={user.name || user.email || "Profile"} />
      ) : (
        <span>{getInitial(user?.name || user?.email || currentUser)}</span>
      )}
    </div>
  );

  const renderActionMenu = () => (
    <div className="top-menu" ref={actionMenuRef}>
      <button
        type="button"
        className="menu-button"
        onClick={() => setMenuOpen((previous) => !previous)}
        aria-label="Open menu"
      >
        {"\u22EE"}
      </button>

      {menuOpen ? (
        <div className="action-menu">
          <button
            type="button"
            onClick={() => {
              setActivePage("settings");
              setMenuOpen(false);
            }}
          >
            Settings
          </button>

          {selectedUserSummary ? (
            <button
              type="button"
              onClick={() => runProfileAction("/auth/block", selectedUserSummary.email, "Contact blocked.").catch((error) => console.log(error))}
            >
              Block contact
            </button>
          ) : null}

          <button type="button" className="danger-menu-item" onClick={onLogout}>Logout</button>
        </div>
      ) : null}
    </div>
  );

  const renderSidebar = () => (
    <aside className="sidebar">
      <div className="sidebar-topbar">
        <div className="sidebar-profile-mini">
          {renderAvatar({ name: profile?.name, email: currentUser, profileImage: profile?.profileImage }, "self-avatar")}
          <div>
            <p className="section-kicker">ChatFlow</p>
            <h1 className="logo">{profile?.name || "My profile"}</h1>
            <p className="current-user">Private messages</p>
          </div>
        </div>
      </div>

      {statusMessage ? <div className="status-notice">{statusMessage}</div> : null}

      <div className="sidebar-tool-row">
        <button type="button" className="secondary-button full-button" onClick={() => setActivePage("search")}>
          Find contacts
        </button>
      </div>

      <div className="search-wrapper">
        <input
          type="text"
          placeholder="Search accepted contacts"
          className="search-box"
          value={contactSearch}
          onChange={(event) => setContactSearch(event.target.value)}
        />
      </div>

      <div className="sidebar-section-title">
        <span>Chats</span>
        <span>{filteredContacts.length}</span>
      </div>

      <div className="user-list">
        {filteredContacts.length > 0 ? filteredContacts.map((user) => (
          <button
            type="button"
            key={user.email}
            className={selectedUser === user.email && activePage === "chat" ? "chat-user active" : "chat-user"}
            onClick={() => openChat(user.email)}
          >
            {renderAvatar(user, "chat-user-avatar")}
            <div className="chat-user-body">
              <div className="chat-user-headline"><h3>{user.name || user.email}</h3></div>
              <p>Tap to open chat</p>
            </div>
          </button>
        )) : (
          <div className="sidebar-empty">
            <strong>No accepted contacts</strong>
            <p>Use Find contacts or accept a request first.</p>
          </div>
        )}
      </div>
    </aside>
  );
  const renderChatPage = () => (
    <section className="chat-area">
      {selectedUserSummary ? (
        <>
          <header className="chat-header">
            <div className="chat-header-left">
              {renderAvatar(selectedUserSummary, "chat-header-avatar")}
              <div>
                <h2>{selectedUserSummary.name || selectedUserSummary.email}</h2>
                <p>Private conversation</p>
              </div>
            </div>

            <div className="chat-header-actions">
              <span className="status-chip success">Connected</span>
              {renderActionMenu()}
            </div>
          </header>

          <div className="messages">
            {loadingMessages ? <p className="empty-state">Loading conversation...</p> : null}

            {!loadingMessages && messages.length === 0 ? (
              <div className="empty-state-card">
                <h3>No messages yet</h3>
                <p>Start the conversation. Messages are stored encrypted on the server.</p>
              </div>
            ) : null}

            {messages.map((chatMessage) => {
              const mine = chatMessage.sender === currentUser;

              return (
                <div
                  key={chatMessage.id || `${chatMessage.sender}-${chatMessage.createdAt}`}
                  className={mine ? "message-row mine" : "message-row theirs"}
                >
                  <div className={mine ? "message-bubble message-bubble-mine" : "message-bubble message-bubble-theirs"}>
                    {chatMessage.text ? <p>{chatMessage.text}</p> : null}

                    {chatMessage.image ? (
                      <button
                        type="button"
                        className="chat-image-button"
                        onClick={() => setInspectedImage(chatMessage)}
                        aria-label="Open image inspector"
                      >
                        <img src={chatMessage.image} alt="shared chat" className="chat-image" />
                      </button>
                    ) : null}

                    <div className="message-meta">
                      <span>{chatMessage.time}</span>
                      {mine ? <span className={`message-ticks ${getTickClassName(chatMessage)}`}>{getTickText(chatMessage)}</span> : null}
                    </div>
                  </div>
                </div>
              );
            })}

            <div ref={messagesEndRef}></div>
          </div>

          <div className="message-input-shell">
            <div className="message-input">
              <label className="icon-button upload-button">
                Upload
                <input
                  type="file"
                  hidden
                  accept="image/*"
                  onChange={async (event) => {
                    const file = event.target.files?.[0];

                    if (!file) {
                      return;
                    }

                    try {
                      const imageUrl = await uploadImageFile(file);
                      setImage(imageUrl);
                    } catch (error) {
                      alert(error.message);
                    }
                  }}
                />
              </label>

              <div className="emoji-box" ref={emojiPickerRef}>
                <button type="button" className="icon-button" onClick={() => setShowEmoji((previous) => !previous)}>
                  Emoji
                </button>

                {showEmoji ? (
                  <div className="emoji-picker">
                    <EmojiPicker
                      onEmojiClick={(emojiData) => {
                        setMessage((previous) => previous + emojiData.emoji);
                        setShowEmoji(false);
                      }}
                    />
                  </div>
                ) : null}
              </div>

              {image ? (
                <div className="image-preview-container">
                  <div className="image-preview">
                    <img src={image} alt="preview" />
                    <button type="button" className="remove-image-btn" onClick={() => setImage("")}>{"\u2715"}</button>
                  </div>
                </div>
              ) : null}

              <input
                type="text"
                placeholder="Type a private message"
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    handleSendMessage().catch((error) => console.log(error));
                  }
                }}
              />

              <button type="button" className="primary-button send-button" onClick={() => handleSendMessage().catch((error) => console.log(error))}>
                Send
              </button>
            </div>
          </div>
        </>
      ) : (
        <div className="page-shell empty-chat-shell">
          <div className="page-header">
            <div>
              <p className="section-kicker">Chats</p>
              <h2>Select a contact</h2>
            </div>
            {renderActionMenu()}
          </div>

          <div className="empty-state-card centered-empty-state">
            <h3>No accepted chat selected</h3>
            <p>Only accepted contacts appear in the left panel. Find contacts or approve requests to start chatting.</p>
            <div className="empty-action-row">
              <button type="button" className="primary-button" onClick={() => setActivePage("search")}>Find contacts</button>
              <button type="button" className="secondary-button" onClick={() => setActivePage("requests")}>Requests</button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
  const renderSettingsPage = () => (
    <section className="page-shell">
      <div className="page-header">
        <div>
          <p className="section-kicker">Account</p>
          <h2>Settings</h2>
        </div>
        {renderActionMenu()}
      </div>

      <div className="settings-list-page">
        <button type="button" className="settings-option-row" onClick={() => setActivePage("profile")}>
          {renderAvatar({ name: profile?.name, email: currentUser, profileImage: profile?.profileImage }, "option-avatar")}
          <div>
            <strong>Profile photo and name</strong>
            <p>Change your display picture or name</p>
          </div>
          <span>{"\u203A"}</span>
        </button>

        <button type="button" className="settings-option-row" onClick={() => setActivePage("requests")}>
          <div className="option-icon">{incomingRequests.length + outgoingRequests.length}</div>
          <div>
            <strong>Friend requests</strong>
            <p>Accept, decline, or review pending contacts</p>
          </div>
          <span>{"\u203A"}</span>
        </button>

        <button type="button" className="settings-option-row" onClick={() => setActivePage("blocked")}>
          <div className="option-icon">{blockedUsers.length}</div>
          <div>
            <strong>Blocked contacts</strong>
            <p>Manage privacy and unblock contacts</p>
          </div>
          <span>{"\u203A"}</span>
        </button>

        <button type="button" className="settings-option-row" onClick={() => setActivePage("search")}>
          <div className="option-icon">+</div>
          <div>
            <strong>Find contacts</strong>
            <p>Search people before sending a request</p>
          </div>
          <span>{"\u203A"}</span>
        </button>
      </div>
    </section>
  );

  const renderProfilePage = () => (
    <section className="page-shell">
      <div className="page-header">
        <button type="button" className="back-button" onClick={() => setActivePage("settings")}>{"\u2190"}</button>
        <div>
          <p className="section-kicker">Settings</p>
          <h2>Profile</h2>
        </div>
        {renderActionMenu()}
      </div>

      <div className="detail-page-card profile-detail-card">
        <div className="profile-photo-stage">
          {renderAvatar({ name: profileForm.name || profile?.name, email: currentUser, profileImage: profileForm.profileImage }, "profile-page-avatar")}
        </div>

        <label className="settings-field">
          <span>Name</span>
          <input
            type="text"
            value={profileForm.name}
            onChange={(event) => setProfileForm((previousState) => ({ ...previousState, name: event.target.value }))}
          />
        </label>

        <label className="settings-field">
          <span>Profile image URL</span>
          <input
            type="text"
            value={profileForm.profileImage}
            onChange={(event) => setProfileForm((previousState) => ({ ...previousState, profileImage: event.target.value }))}
          />
        </label>

        <div className="settings-actions-row">
          <label className="secondary-button upload-button">
            Upload photo
            <input
              type="file"
              hidden
              accept="image/*"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) {
                  handleProfileImageUpload(file).catch((error) => console.log(error));
                }
              }}
            />
          </label>

          <button
            type="button"
            className="secondary-button"
            onClick={() => saveProfile({ profileImage: "" }).catch((error) => console.log(error))}
            disabled={!profileForm.profileImage}
          >
            Delete photo
          </button>

          <button type="button" className="primary-button" onClick={() => saveProfile().catch((error) => console.log(error))}>
            Save profile
          </button>
        </div>
      </div>
    </section>
  );
  const renderRequestsPage = () => (
    <section className="page-shell">
      <div className="page-header">
        <button type="button" className="back-button" onClick={() => setActivePage("settings")}>{"\u2190"}</button>
        <div>
          <p className="section-kicker">Privacy</p>
          <h2>Requests</h2>
        </div>
        {renderActionMenu()}
      </div>

      <div className="detail-page-grid">
        <section className="detail-page-card">
          <div className="detail-card-title"><h3>Incoming</h3><span>{incomingRequests.length}</span></div>
          <div className="plain-list">
            {incomingRequests.length > 0 ? incomingRequests.map((user) => (
              <div key={user.email} className="plain-list-row">
                {renderAvatar(user, "list-avatar")}
                <strong>{user.name || user.email}</strong>
                <div className="row-actions">
                  <button
                    type="button"
                    className="primary-button small-button"
                    onClick={() => runProfileAction("/auth/requests/accept", user.email, "Request accepted.", { openChat: true }).catch((error) => console.log(error))}
                  >
                    Accept
                  </button>
                  <button
                    type="button"
                    className="secondary-button small-button"
                    onClick={() => runProfileAction("/auth/requests/reject", user.email, "Request declined.").catch((error) => console.log(error))}
                  >
                    Decline
                  </button>
                </div>
              </div>
            )) : <p className="settings-empty">No pending requests.</p>}
          </div>
        </section>

        <section className="detail-page-card">
          <div className="detail-card-title"><h3>Sent</h3><span>{outgoingRequests.length}</span></div>
          <div className="plain-list">
            {outgoingRequests.length > 0 ? outgoingRequests.map((user) => (
              <div key={user.email} className="plain-list-row">
                {renderAvatar(user, "list-avatar")}
                <strong>{user.name || user.email}</strong>
                <div className="row-actions">
                  <span className="status-chip muted">Waiting</span>
                  <button
                    type="button"
                    className="secondary-button small-button"
                    onClick={() => runProfileAction("/auth/requests/reject", user.email, "Request canceled.").catch((error) => console.log(error))}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )) : <p className="settings-empty">No sent requests.</p>}
          </div>
        </section>
      </div>
    </section>
  );

  const renderSearchPage = () => (
    <section className="page-shell">
      <div className="page-header">
        <button type="button" className="back-button" onClick={() => setActivePage("settings")}>{"\u2190"}</button>
        <div>
          <p className="section-kicker">Contacts</p>
          <h2>Find contacts</h2>
        </div>
        {renderActionMenu()}
      </div>

      <div className="detail-page-card">
        <input
          type="text"
          className="search-box page-search"
          placeholder="Search people by name or email"
          value={peopleSearch}
          onChange={(event) => setPeopleSearch(event.target.value)}
        />

        <div className="plain-list people-search-list">
          {filteredPeople.length > 0 ? filteredPeople.map((user) => {
            const relationship = getRelationshipState(user);

            return (
              <div key={user.email} className="plain-list-row">
                {renderAvatar(user, "list-avatar")}
                <strong>{user.name || user.email}</strong>
                <div className="row-actions">
                  <span className={`relationship-pill ${relationship.className}`}>{relationship.label}</span>
                  {!user.incomingRequest && !user.outgoingRequest ? (
                    <button
                      type="button"
                      className="primary-button small-button"
                      onClick={() => runProfileAction("/auth/requests/send", user.email, "Request sent.").catch((error) => console.log(error))}
                    >
                      Request
                    </button>
                  ) : null}
                  {user.incomingRequest ? (
                    <button type="button" className="secondary-button small-button" onClick={() => setActivePage("requests")}>
                      Review
                    </button>
                  ) : null}
                </div>
              </div>
            );
          }) : <p className="settings-empty">No people found here.</p>}
        </div>
      </div>
    </section>
  );

  const renderBlockedPage = () => (
    <section className="page-shell">
      <div className="page-header">
        <button type="button" className="back-button" onClick={() => setActivePage("settings")}>{"\u2190"}</button>
        <div>
          <p className="section-kicker">Privacy</p>
          <h2>Blocked contacts</h2>
        </div>
        {renderActionMenu()}
      </div>

      <div className="detail-page-card">
        <div className="plain-list">
          {blockedUsers.length > 0 ? blockedUsers.map((user) => (
            <div key={user.email} className="plain-list-row">
              {renderAvatar(user, "list-avatar")}
              <strong>{user.name || user.email}</strong>
              <div className="row-actions">
                <button
                  type="button"
                  className="secondary-button small-button"
                  onClick={() => runProfileAction("/auth/unblock", user.email, "Contact unblocked.").catch((error) => console.log(error))}
                >
                  Unblock
                </button>
              </div>
            </div>
          )) : <p className="settings-empty">No blocked contacts.</p>}
        </div>
      </div>
    </section>
  );

  const renderMainContent = () => {
    if (activePage === "settings") return renderSettingsPage();
    if (activePage === "profile") return renderProfilePage();
    if (activePage === "requests") return renderRequestsPage();
    if (activePage === "blocked") return renderBlockedPage();
    if (activePage === "search") return renderSearchPage();
    return renderChatPage();
  };

  return (
    <div className="chat-app">
      {renderSidebar()}
      {renderMainContent()}

      <ImageInspector
        currentUser={currentUser}
        message={inspectedImage}
        onClose={() => setInspectedImage(null)}
        resolveDisplayName={getDisplayName}
      />
    </div>
  );
}

export default Chat;
