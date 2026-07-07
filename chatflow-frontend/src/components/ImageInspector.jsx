function ImageInspector({
  currentUser,
  message,
  onClose,
  resolveDisplayName,
}) {
  if (!message) {
    return null;
  }

  const senderLabel = message.sender === currentUser ? "You" : resolveDisplayName(message.sender);
  const avatarText = senderLabel[0]?.toUpperCase() || "?";

  return (
    <div className="image-inspector-backdrop" onClick={onClose}>
      <div className="image-inspector-shell" onClick={(event) => event.stopPropagation()}>
        <div className="image-inspector-topbar">
          <div className="image-inspector-user">
            <button
              type="button"
              className="image-inspector-back"
              onClick={onClose}
              aria-label="Back"
            >
              {"\u2190"}
            </button>

            <div className="image-inspector-avatar">{avatarText}</div>

            <div className="image-inspector-title">
              <h3>{senderLabel}</h3>
              <p>{message.time || "Shared image"}</p>
            </div>
          </div>

          <button
            type="button"
            className="image-inspector-close"
            onClick={onClose}
            aria-label="Close image viewer"
          >
            {"\u2715"}
          </button>
        </div>

        <div className="image-inspector-body">
          <div className="image-inspector-viewport">
            <img
              src={message.image}
              alt="shared chat preview"
              className="image-inspector-image"
            />
          </div>
        </div>
      </div>
    </div>
  );
}

export default ImageInspector;