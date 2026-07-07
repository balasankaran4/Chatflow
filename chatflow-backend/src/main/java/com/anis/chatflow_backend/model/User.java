package com.anis.chatflow_backend.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @JsonIgnore
    private String emailHash;

    @JsonIgnore
    private String emailEncrypted;

    @JsonIgnore
    private String nameEncrypted;

    @JsonIgnore
    private String bioEncrypted;

    @JsonIgnore
    private String profileImageEncrypted;

    @JsonIgnore
    private String passwordHash;

    @JsonIgnore
    private Set<String> contactHashes = new LinkedHashSet<>();

    @JsonIgnore
    private Set<String> sentRequestHashes = new LinkedHashSet<>();

    @JsonIgnore
    private Set<String> receivedRequestHashes = new LinkedHashSet<>();

    @JsonIgnore
    private Set<String> blockedHashes = new LinkedHashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmailHash() {
        return emailHash;
    }

    public void setEmailHash(String emailHash) {
        this.emailHash = emailHash;
    }

    public String getEmailEncrypted() {
        return emailEncrypted;
    }

    public void setEmailEncrypted(String emailEncrypted) {
        this.emailEncrypted = emailEncrypted;
    }

    public String getNameEncrypted() {
        return nameEncrypted;
    }

    public void setNameEncrypted(String nameEncrypted) {
        this.nameEncrypted = nameEncrypted;
    }

    public String getBioEncrypted() {
        return bioEncrypted;
    }

    public void setBioEncrypted(String bioEncrypted) {
        this.bioEncrypted = bioEncrypted;
    }

    public String getProfileImageEncrypted() {
        return profileImageEncrypted;
    }

    public void setProfileImageEncrypted(String profileImageEncrypted) {
        this.profileImageEncrypted = profileImageEncrypted;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Set<String> getContactHashes() {
        return contactHashes;
    }

    public void setContactHashes(Set<String> contactHashes) {
        this.contactHashes = contactHashes;
    }

    public Set<String> getSentRequestHashes() {
        return sentRequestHashes;
    }

    public void setSentRequestHashes(Set<String> sentRequestHashes) {
        this.sentRequestHashes = sentRequestHashes;
    }

    public Set<String> getReceivedRequestHashes() {
        return receivedRequestHashes;
    }

    public void setReceivedRequestHashes(Set<String> receivedRequestHashes) {
        this.receivedRequestHashes = receivedRequestHashes;
    }

    public Set<String> getBlockedHashes() {
        return blockedHashes;
    }

    public void setBlockedHashes(Set<String> blockedHashes) {
        this.blockedHashes = blockedHashes;
    }
}
