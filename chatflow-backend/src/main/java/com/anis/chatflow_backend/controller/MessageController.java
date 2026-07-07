package com.anis.chatflow_backend.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.anis.chatflow_backend.dto.ConversationStatusRequest;
import com.anis.chatflow_backend.dto.MessageStatusEvent;
import com.anis.chatflow_backend.model.Message;
import com.anis.chatflow_backend.service.MessageService;

@RestController
@RequestMapping("/messages")
@CrossOrigin(origins = "http://localhost:5173")
public class MessageController {

    private static final Map<String, byte[]> IMAGE_STORE = new ConcurrentHashMap<>();
    private static final Map<String, String> IMAGE_MIME_TYPES = new ConcurrentHashMap<>();

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(MessageService messageService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public ResponseEntity<?> saveMessage(@RequestBody Message message) {
        try {
            Message savedMessage = messageService.saveMessage(message);
            messagingTemplate.convertAndSend("/topic/messages", savedMessage);
            return ResponseEntity.ok(savedMessage);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping
    public List<Message> getMessages(@RequestParam String sender, @RequestParam String receiver) {
        return messageService.getConversation(sender, receiver);
    }

    @PostMapping("/status/delivered")
    public ResponseEntity<List<MessageStatusEvent>> markDelivered(@RequestBody ConversationStatusRequest request) {
        List<MessageStatusEvent> events = messageService.markDelivered(request.currentUserEmail(), request.otherUserEmail());
        events.forEach(event -> messagingTemplate.convertAndSend("/topic/message-status", event));
        return ResponseEntity.ok(events);
    }

    @PostMapping("/status/seen")
    public ResponseEntity<List<MessageStatusEvent>> markSeen(@RequestBody ConversationStatusRequest request) {
        List<MessageStatusEvent> events = messageService.markSeen(request.currentUserEmail(), request.otherUserEmail());
        events.forEach(event -> messagingTemplate.convertAndSend("/topic/message-status", event));
        return ResponseEntity.ok(events);
    }

    @PostMapping("/upload")
    public Map<String, String> uploadImage(@RequestParam MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "File is empty");
            return error;
        }

        String imageId = UUID.randomUUID().toString();
        IMAGE_STORE.put(imageId, file.getBytes());
        IMAGE_MIME_TYPES.put(imageId, file.getContentType());

        Map<String, String> response = new HashMap<>();
        response.put("imageUrl", "http://localhost:8080/messages/image/" + imageId);
        response.put("imageId", imageId);
        response.put("fileName", file.getOriginalFilename());
        return response;
    }

    @GetMapping("/image/{imageId}")
    @CrossOrigin(origins = "http://localhost:5173")
    public ResponseEntity<Resource> getImage(@PathVariable String imageId) {
        byte[] imageBytes = IMAGE_STORE.get(imageId);
        if (imageBytes == null) {
            return ResponseEntity.notFound().build();
        }

        String mimeType = IMAGE_MIME_TYPES.getOrDefault(imageId, "application/octet-stream");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .body(new ByteArrayResource(imageBytes));
    }
}
