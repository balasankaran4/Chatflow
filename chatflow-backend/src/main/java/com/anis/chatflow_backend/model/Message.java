package com.anis.chatflow_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "messages")
public class Message {

    @Id
    private String id;

    @JsonIgnore
    private String conversationId;

    @JsonIgnore
    private String senderHash;

    @JsonIgnore
    private String receiverHash;

    @JsonIgnore
    private String senderEncrypted;

    @JsonIgnore
    private String receiverEncrypted;

    @JsonIgnore
    private String textEncrypted;

    @JsonIgnore
    private String imageEncrypted;

    @JsonIgnore
    private String timeEncrypted;

    @JsonIgnore
    @Field("sender")
    private String legacySender;

    @JsonIgnore
    @Field("receiver")
    private String legacyReceiver;

    @JsonIgnore
    @Field("text")
    private String legacyText;

    @JsonIgnore
    @Field("image")
    private String legacyImage;

    @JsonIgnore
    @Field("time")
    private String legacyTime;

    private boolean delivered;

    private boolean seen;

    private long createdAt;

    @Transient
    private String sender;

    @Transient
    private String receiver;

    @Transient
    private String text;

    @Transient
    private String image;

    @Transient
    private String time;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderHash() {
        return senderHash;
    }

    public void setSenderHash(String senderHash) {
        this.senderHash = senderHash;
    }

    public String getReceiverHash() {
        return receiverHash;
    }

    public void setReceiverHash(String receiverHash) {
        this.receiverHash = receiverHash;
    }

    public String getSenderEncrypted() {
        return senderEncrypted;
    }

    public void setSenderEncrypted(String senderEncrypted) {
        this.senderEncrypted = senderEncrypted;
    }

    public String getReceiverEncrypted() {
        return receiverEncrypted;
    }

    public void setReceiverEncrypted(String receiverEncrypted) {
        this.receiverEncrypted = receiverEncrypted;
    }

    public String getTextEncrypted() {
        return textEncrypted;
    }

    public void setTextEncrypted(String textEncrypted) {
        this.textEncrypted = textEncrypted;
    }

    public String getImageEncrypted() {
        return imageEncrypted;
    }

    public void setImageEncrypted(String imageEncrypted) {
        this.imageEncrypted = imageEncrypted;
    }

    public String getTimeEncrypted() {
        return timeEncrypted;
    }

    public void setTimeEncrypted(String timeEncrypted) {
        this.timeEncrypted = timeEncrypted;
    }

    public String getLegacySender() {
        return legacySender;
    }

    public void setLegacySender(String legacySender) {
        this.legacySender = legacySender;
    }

    public String getLegacyReceiver() {
        return legacyReceiver;
    }

    public void setLegacyReceiver(String legacyReceiver) {
        this.legacyReceiver = legacyReceiver;
    }

    public String getLegacyText() {
        return legacyText;
    }

    public void setLegacyText(String legacyText) {
        this.legacyText = legacyText;
    }

    public String getLegacyImage() {
        return legacyImage;
    }

    public void setLegacyImage(String legacyImage) {
        this.legacyImage = legacyImage;
    }

    public String getLegacyTime() {
        return legacyTime;
    }

    public void setLegacyTime(String legacyTime) {
        this.legacyTime = legacyTime;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}