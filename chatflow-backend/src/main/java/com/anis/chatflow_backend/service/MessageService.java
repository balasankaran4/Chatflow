package com.anis.chatflow_backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.anis.chatflow_backend.dto.MessageStatusEvent;
import com.anis.chatflow_backend.model.Message;
import com.anis.chatflow_backend.repository.MessageRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final CryptoService cryptoService;

    public MessageService(MessageRepository messageRepository, UserService userService, CryptoService cryptoService) {
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.cryptoService = cryptoService;
    }

    public Message saveMessage(Message incomingMessage) {
        String sender = userService.normalizeEmail(incomingMessage.getSender());
        String receiver = userService.normalizeEmail(incomingMessage.getReceiver());
        String text = safeTrim(incomingMessage.getText());
        String image = safeTrim(incomingMessage.getImage());
        String time = safeTrim(incomingMessage.getTime());

        if (sender.isBlank() || receiver.isBlank()) {
            throw new IllegalArgumentException("Sender and receiver are required.");
        }

        if (text.isBlank() && image.isBlank()) {
            throw new IllegalArgumentException("Message text or image is required.");
        }

        if (!userService.canSendMessages(sender, receiver)) {
            throw new IllegalArgumentException("Chat is available only after the request is accepted and neither user is blocked.");
        }

        Message storedMessage = new Message();
        storedMessage.setConversationId(userService.buildConversationId(sender, receiver));
        storedMessage.setSenderHash(userService.hashEmail(sender));
        storedMessage.setReceiverHash(userService.hashEmail(receiver));
        storedMessage.setSenderEncrypted(cryptoService.encrypt(sender));
        storedMessage.setReceiverEncrypted(cryptoService.encrypt(receiver));
        storedMessage.setTextEncrypted(cryptoService.encrypt(text));
        storedMessage.setImageEncrypted(cryptoService.encrypt(image));
        storedMessage.setTimeEncrypted(cryptoService.encrypt(time));
        storedMessage.setDelivered(false);
        storedMessage.setSeen(false);
        storedMessage.setCreatedAt(System.currentTimeMillis());

        return toView(messageRepository.save(storedMessage));
    }

    public List<Message> getConversation(String currentUserEmail, String otherUserEmail) {
        String conversationId = userService.buildConversationId(currentUserEmail, otherUserEmail);
        Map<String, Message> messagesById = new LinkedHashMap<>();

        messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::ensureEncryptedMessage)
                .forEach(message -> messagesById.put(message.getId(), message));

        messageRepository.findAll().stream()
                .filter(this::isLegacyMessage)
                .filter(message -> legacyMatches(message, currentUserEmail, otherUserEmail))
                .map(this::ensureEncryptedMessage)
                .forEach(message -> messagesById.put(message.getId(), message));

        return messagesById.values().stream()
                .sorted(Comparator.comparingLong(Message::getCreatedAt))
                .map(this::toView)
                .toList();
    }

    public List<MessageStatusEvent> markDelivered(String currentUserEmail, String otherUserEmail) {
        String currentHash = userService.hashEmail(currentUserEmail);
        String conversationId = userService.buildConversationId(currentUserEmail, otherUserEmail);

        List<Message> pendingMessages = messageRepository
                .findByConversationIdAndReceiverHashAndDeliveredFalse(conversationId, currentHash);

        if (pendingMessages.isEmpty()) {
            return List.of();
        }

        for (Message message : pendingMessages) {
            message.setDelivered(true);
        }

        return buildStatusEvents(messageRepository.saveAll(pendingMessages));
    }

    public List<MessageStatusEvent> markSeen(String currentUserEmail, String otherUserEmail) {
        String currentHash = userService.hashEmail(currentUserEmail);
        String conversationId = userService.buildConversationId(currentUserEmail, otherUserEmail);

        List<Message> pendingMessages = messageRepository
                .findByConversationIdAndReceiverHashAndSeenFalse(conversationId, currentHash);

        if (pendingMessages.isEmpty()) {
            return List.of();
        }

        for (Message message : pendingMessages) {
            message.setDelivered(true);
            message.setSeen(true);
        }

        return buildStatusEvents(messageRepository.saveAll(pendingMessages));
    }

    private List<MessageStatusEvent> buildStatusEvents(List<Message> messages) {
        List<MessageStatusEvent> events = new ArrayList<>();
        for (Message message : messages) {
            Message view = toView(ensureEncryptedMessage(message));
            events.add(new MessageStatusEvent(
                    view.getId(),
                    view.getSender(),
                    view.getReceiver(),
                    view.isDelivered(),
                    view.isSeen()));
        }
        return events;
    }

    private Message ensureEncryptedMessage(Message message) {
        if (!isLegacyMessage(message) && hasText(message.getConversationId())) {
            return message;
        }

        String sender = userService.normalizeEmail(hasText(message.getLegacySender())
                ? message.getLegacySender()
                : cryptoService.decrypt(message.getSenderEncrypted()));
        String receiver = userService.normalizeEmail(hasText(message.getLegacyReceiver())
                ? message.getLegacyReceiver()
                : cryptoService.decrypt(message.getReceiverEncrypted()));
        String text = hasText(message.getLegacyText()) ? message.getLegacyText() : cryptoService.decrypt(message.getTextEncrypted());
        String image = hasText(message.getLegacyImage()) ? message.getLegacyImage() : cryptoService.decrypt(message.getImageEncrypted());
        String time = hasText(message.getLegacyTime()) ? message.getLegacyTime() : cryptoService.decrypt(message.getTimeEncrypted());

        if (!hasText(sender) || !hasText(receiver)) {
            return message;
        }

        message.setConversationId(userService.buildConversationId(sender, receiver));
        message.setSenderHash(userService.hashEmail(sender));
        message.setReceiverHash(userService.hashEmail(receiver));
        message.setSenderEncrypted(cryptoService.encrypt(sender));
        message.setReceiverEncrypted(cryptoService.encrypt(receiver));
        message.setTextEncrypted(cryptoService.encrypt(safeTrim(text)));
        message.setImageEncrypted(cryptoService.encrypt(safeTrim(image)));
        message.setTimeEncrypted(cryptoService.encrypt(safeTrim(time)));
        message.setLegacySender(null);
        message.setLegacyReceiver(null);
        message.setLegacyText(null);
        message.setLegacyImage(null);
        message.setLegacyTime(null);

        if (message.getCreatedAt() == 0) {
            message.setCreatedAt(System.currentTimeMillis());
        }

        return messageRepository.save(message);
    }

    private Message toView(Message storedMessage) {
        Message encryptedMessage = ensureEncryptedMessage(storedMessage);
        Message view = new Message();
        view.setId(encryptedMessage.getId());
        view.setSender(cryptoService.decrypt(encryptedMessage.getSenderEncrypted()));
        view.setReceiver(cryptoService.decrypt(encryptedMessage.getReceiverEncrypted()));
        view.setText(cryptoService.decrypt(encryptedMessage.getTextEncrypted()));
        view.setImage(cryptoService.decrypt(encryptedMessage.getImageEncrypted()));
        view.setTime(cryptoService.decrypt(encryptedMessage.getTimeEncrypted()));
        view.setDelivered(encryptedMessage.isDelivered());
        view.setSeen(encryptedMessage.isSeen());
        view.setCreatedAt(encryptedMessage.getCreatedAt());
        return view;
    }

    private boolean legacyMatches(Message message, String currentUserEmail, String otherUserEmail) {
        String sender = userService.normalizeEmail(message.getLegacySender());
        String receiver = userService.normalizeEmail(message.getLegacyReceiver());
        String currentUser = userService.normalizeEmail(currentUserEmail);
        String otherUser = userService.normalizeEmail(otherUserEmail);

        return (sender.equals(currentUser) && receiver.equals(otherUser))
                || (sender.equals(otherUser) && receiver.equals(currentUser));
    }

    private boolean isLegacyMessage(Message message) {
        return hasText(message.getLegacySender()) || hasText(message.getLegacyReceiver())
                || hasText(message.getLegacyText()) || hasText(message.getLegacyImage())
                || hasText(message.getLegacyTime());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}