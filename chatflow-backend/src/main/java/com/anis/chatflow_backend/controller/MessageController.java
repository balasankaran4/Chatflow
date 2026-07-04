package com.anis.chatflow_backend.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.anis.chatflow_backend.model.Message;
import com.anis.chatflow_backend.repository.MessageRepository;

@RestController

@RequestMapping("/messages")

@CrossOrigin(origins = "http://localhost:5173")

public class MessageController {

    private static final Map<String, byte[]> imageStore = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, String> imageMimeTypes = new java.util.concurrent.ConcurrentHashMap<>();

        private final MessageRepository messageRepository;

        public MessageController(MessageRepository messageRepository) {
                this.messageRepository = messageRepository;
        }

    // SAVE MESSAGE

    @PostMapping

    public Message saveMessage(
            @RequestBody Message message
    ) {

                return messageRepository.save(Objects.requireNonNull(message));
    }

    // PRIVATE CHAT

    @GetMapping

    public List<Message> getMessages(

            @RequestParam String sender,

            @RequestParam String receiver
    ) {

        List<Message> messages = new ArrayList<>();

        messages.addAll(

                messageRepository.findBySenderAndReceiver(
                        sender,
                        receiver
                )
        );

        messages.addAll(

                messageRepository.findByReceiverAndSender(
                        sender,
                        receiver
                )
        );

        return messages;
    }

    // IMAGE UPLOAD

    @PostMapping("/upload")
    public Map<String, String> uploadImage(
            @RequestParam MultipartFile file
    ) throws IOException {
        
        if (file.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "File is empty");
            return error;
        }

        // Generate unique ID for image
        String imageId = UUID.randomUUID().toString();
        
        // Store image in memory
        imageStore.put(imageId, file.getBytes());
        imageMimeTypes.put(imageId, file.getContentType());

        // Return simple URL reference
        Map<String, String> response = new HashMap<>();
        response.put("imageUrl", "http://localhost:8080/messages/image/" + imageId);
        response.put("imageId", imageId);
        response.put("fileName", file.getOriginalFilename());

        return response;
    }

    // IMAGE RETRIEVAL

    @GetMapping("/image/{imageId}")
    @CrossOrigin(origins = "http://localhost:5173")
    public ResponseEntity<Resource> getImage(@PathVariable String imageId) {
        byte[] imageBytes = imageStore.get(imageId);
        
        if (imageBytes == null) {
            return ResponseEntity.notFound().build();
        }
        
                String mimeType = imageMimeTypes.get(imageId);
                if (mimeType == null) {
                        mimeType = "application/octet-stream";
                }
        
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(mimeType))
            .body(new ByteArrayResource(imageBytes));
    }
}