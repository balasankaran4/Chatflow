package com.anis.chatflow_backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.anis.chatflow_backend.dto.ProfileResponse;
import com.anis.chatflow_backend.dto.ProfileUpdateRequest;
import com.anis.chatflow_backend.dto.RegisterRequest;
import com.anis.chatflow_backend.dto.UserActionRequest;
import com.anis.chatflow_backend.dto.UserSummaryResponse;
import com.anis.chatflow_backend.model.User;
import com.anis.chatflow_backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CryptoService cryptoService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, CryptoService cryptoService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String name = safeTrim(request.getName());
        String password = request.getPassword() == null ? "" : request.getPassword().trim();

        if (email.isBlank() || name.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Name, email, and password are required.");
        }

        String emailHash = hashEmail(email);
        if (userRepository.findByEmailHash(emailHash).isPresent() || findLegacyUser(email).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = new User();
        user.setEmailHash(emailHash);
        user.setEmailEncrypted(cryptoService.encrypt(email));
        user.setNameEncrypted(cryptoService.encrypt(name));
        user.setBioEncrypted(cryptoService.encrypt(""));
        user.setProfileImageEncrypted(cryptoService.encrypt(""));
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public User authenticate(String email, String password) {
        User user = findUserByEmail(email);
        String suppliedPassword = password == null ? "" : password;

        if (hasText(user.getPasswordHash())) {
            if (!passwordEncoder.matches(suppliedPassword, user.getPasswordHash())) {
                throw new IllegalArgumentException("Wrong password.");
            }
            return ensureEncryptedUser(user);
        }

        if (!Objects.equals(suppliedPassword, user.getPassword())) {
            throw new IllegalArgumentException("Wrong password.");
        }

        user.setPasswordHash(passwordEncoder.encode(suppliedPassword));
        return ensureEncryptedUser(user);
    }

    public User findUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        Optional<User> secureUser = userRepository.findByEmailHash(hashEmail(normalizedEmail));
        if (secureUser.isPresent()) {
            return ensureEncryptedUser(secureUser.get());
        }

        return findLegacyUser(normalizedEmail)
                .map(this::ensureEncryptedUser)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public List<UserSummaryResponse> getUsersFor(String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        String currentHash = currentUser.getEmailHash();

        Comparator<UserSummaryResponse> comparator = Comparator
                .comparing(UserSummaryResponse::canChat).reversed()
                .thenComparing(UserSummaryResponse::incomingRequest).reversed()
                .thenComparing(UserSummaryResponse::outgoingRequest).reversed()
                .thenComparing(UserSummaryResponse::name, String.CASE_INSENSITIVE_ORDER);

        return userRepository.findAll().stream()
                .map(this::ensureEncryptedUser)
                .filter(user -> !Objects.equals(user.getEmailHash(), currentHash))
                .map(user -> toSummary(user, currentUser))
                .sorted(comparator)
                .toList();
    }

    public ProfileResponse getProfile(String email) {
        User currentUser = findUserByEmail(email);
        return buildProfileResponse(currentUser);
    }

    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        User currentUser = findUserByEmail(request.email());

        String nextName = safeTrim(request.name());
        if (!nextName.isBlank()) {
            currentUser.setNameEncrypted(cryptoService.encrypt(nextName));
        }

        currentUser.setBioEncrypted(cryptoService.encrypt(safeTrim(request.bio())));
        currentUser.setProfileImageEncrypted(cryptoService.encrypt(safeTrim(request.profileImage())));

        userRepository.save(currentUser);
        return buildProfileResponse(currentUser);
    }

    public ProfileResponse sendRequest(UserActionRequest request) {
        User currentUser = findUserByEmail(request.currentUserEmail());
        User targetUser = findUserByEmail(request.targetEmail());
        validateDistinctUsers(currentUser, targetUser);

        String targetHash = targetUser.getEmailHash();
        String currentHash = currentUser.getEmailHash();

        if (isBlockedEitherWay(currentUser, targetUser)) {
            throw new IllegalArgumentException("Request cannot be sent while one of the users is blocked.");
        }

        if (currentUser.getContactHashes().contains(targetHash)) {
            throw new IllegalArgumentException("You can already chat with this user.");
        }

        if (currentUser.getSentRequestHashes().contains(targetHash)) {
            throw new IllegalArgumentException("Friend request already sent.");
        }

        if (currentUser.getReceivedRequestHashes().contains(targetHash)) {
            throw new IllegalArgumentException("This user already sent you a request. Accept it from your requests list.");
        }

        currentUser.getSentRequestHashes().add(targetHash);
        targetUser.getReceivedRequestHashes().add(currentHash);

        userRepository.saveAll(List.of(currentUser, targetUser));
        return buildProfileResponse(currentUser);
    }

    public ProfileResponse acceptRequest(UserActionRequest request) {
        User currentUser = findUserByEmail(request.currentUserEmail());
        User targetUser = findUserByEmail(request.targetEmail());
        validateDistinctUsers(currentUser, targetUser);

        String targetHash = targetUser.getEmailHash();
        String currentHash = currentUser.getEmailHash();

        if (!currentUser.getReceivedRequestHashes().contains(targetHash)) {
            throw new IllegalArgumentException("No incoming request found for this user.");
        }

        currentUser.getReceivedRequestHashes().remove(targetHash);
        targetUser.getSentRequestHashes().remove(currentHash);
        currentUser.getContactHashes().add(targetHash);
        targetUser.getContactHashes().add(currentHash);

        userRepository.saveAll(List.of(currentUser, targetUser));
        return buildProfileResponse(currentUser);
    }

    public ProfileResponse rejectRequest(UserActionRequest request) {
        User currentUser = findUserByEmail(request.currentUserEmail());
        User targetUser = findUserByEmail(request.targetEmail());
        validateDistinctUsers(currentUser, targetUser);

        String targetHash = targetUser.getEmailHash();
        String currentHash = currentUser.getEmailHash();

        currentUser.getReceivedRequestHashes().remove(targetHash);
        currentUser.getSentRequestHashes().remove(targetHash);
        targetUser.getReceivedRequestHashes().remove(currentHash);
        targetUser.getSentRequestHashes().remove(currentHash);

        userRepository.saveAll(List.of(currentUser, targetUser));
        return buildProfileResponse(currentUser);
    }

    public ProfileResponse blockUser(UserActionRequest request) {
        User currentUser = findUserByEmail(request.currentUserEmail());
        User targetUser = findUserByEmail(request.targetEmail());
        validateDistinctUsers(currentUser, targetUser);

        String targetHash = targetUser.getEmailHash();
        String currentHash = currentUser.getEmailHash();

        currentUser.getBlockedHashes().add(targetHash);
        currentUser.getReceivedRequestHashes().remove(targetHash);
        currentUser.getSentRequestHashes().remove(targetHash);
        targetUser.getReceivedRequestHashes().remove(currentHash);
        targetUser.getSentRequestHashes().remove(currentHash);

        userRepository.saveAll(List.of(currentUser, targetUser));
        return buildProfileResponse(currentUser);
    }

    public ProfileResponse unblockUser(UserActionRequest request) {
        User currentUser = findUserByEmail(request.currentUserEmail());
        User targetUser = findUserByEmail(request.targetEmail());
        validateDistinctUsers(currentUser, targetUser);

        currentUser.getBlockedHashes().remove(targetUser.getEmailHash());
        userRepository.save(currentUser);
        return buildProfileResponse(currentUser);
    }

    public boolean canSendMessages(String currentUserEmail, String otherUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        User otherUser = findUserByEmail(otherUserEmail);
        return canSendMessages(currentUser, otherUser);
    }

    public boolean canSendMessages(User currentUser, User otherUser) {
        return currentUser.getContactHashes().contains(otherUser.getEmailHash())
                && !currentUser.getBlockedHashes().contains(otherUser.getEmailHash())
                && !otherUser.getBlockedHashes().contains(currentUser.getEmailHash());
    }

    public String getDecryptedEmail(User user) {
        return cryptoService.decrypt(user.getEmailEncrypted());
    }

    public String hashEmail(String email) {
        return sha256(normalizeEmail(email));
    }

    public String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public String buildConversationId(String firstEmail, String secondEmail) {
        String firstHash = hashEmail(firstEmail);
        String secondHash = hashEmail(secondEmail);
        return firstHash.compareTo(secondHash) <= 0
                ? firstHash + ":" + secondHash
                : secondHash + ":" + firstHash;
    }

    public UserSummaryResponse toSummary(User targetUser, User viewer) {
        User encryptedTargetUser = ensureEncryptedUser(targetUser);
        User encryptedViewer = ensureEncryptedUser(viewer);
        String targetHash = encryptedTargetUser.getEmailHash();
        String viewerHash = encryptedViewer.getEmailHash();

        boolean contact = encryptedViewer.getContactHashes().contains(targetHash);
        boolean incomingRequest = encryptedViewer.getReceivedRequestHashes().contains(targetHash);
        boolean outgoingRequest = encryptedViewer.getSentRequestHashes().contains(targetHash);
        boolean blocked = encryptedViewer.getBlockedHashes().contains(targetHash);
        boolean blockedBy = encryptedTargetUser.getBlockedHashes().contains(viewerHash);
        boolean canChat = contact && !blocked && !blockedBy;

        return new UserSummaryResponse(
                safeTrim(cryptoService.decrypt(encryptedTargetUser.getNameEncrypted())),
                safeTrim(cryptoService.decrypt(encryptedTargetUser.getEmailEncrypted())),
                safeTrim(cryptoService.decrypt(encryptedTargetUser.getBioEncrypted())),
                safeTrim(cryptoService.decrypt(encryptedTargetUser.getProfileImageEncrypted())),
                contact,
                incomingRequest,
                outgoingRequest,
                blocked,
                blockedBy,
                canChat);
    }

    private ProfileResponse buildProfileResponse(User currentUser) {
        User encryptedCurrentUser = ensureEncryptedUser(currentUser);
        List<User> allUsers = userRepository.findAll().stream()
                .map(this::ensureEncryptedUser)
                .toList();
        Map<String, User> usersByHash = allUsers.stream()
                .collect(Collectors.toMap(User::getEmailHash, Function.identity(), (left, right) -> left));

        return new ProfileResponse(
                safeTrim(cryptoService.decrypt(encryptedCurrentUser.getNameEncrypted())),
                safeTrim(cryptoService.decrypt(encryptedCurrentUser.getEmailEncrypted())),
                safeTrim(cryptoService.decrypt(encryptedCurrentUser.getBioEncrypted())),
                safeTrim(cryptoService.decrypt(encryptedCurrentUser.getProfileImageEncrypted())),
                mapUsers(encryptedCurrentUser.getContactHashes(), encryptedCurrentUser, usersByHash),
                mapUsers(encryptedCurrentUser.getReceivedRequestHashes(), encryptedCurrentUser, usersByHash),
                mapUsers(encryptedCurrentUser.getSentRequestHashes(), encryptedCurrentUser, usersByHash),
                mapUsers(encryptedCurrentUser.getBlockedHashes(), encryptedCurrentUser, usersByHash));
    }

    private List<UserSummaryResponse> mapUsers(Set<String> hashes, User viewer, Map<String, User> usersByHash) {
        if (hashes == null || hashes.isEmpty()) {
            return List.of();
        }

        List<UserSummaryResponse> users = new ArrayList<>();
        for (String hash : hashes) {
            User target = usersByHash.get(hash);
            if (target != null) {
                users.add(toSummary(target, viewer));
            }
        }

        users.sort(Comparator.comparing(UserSummaryResponse::name, String.CASE_INSENSITIVE_ORDER));
        return users;
    }

    private Optional<User> findLegacyUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        List<User> exactMatches = userRepository.findByEmail(normalizedEmail);
        if (!exactMatches.isEmpty()) {
            return Optional.of(exactMatches.get(0));
        }

        return userRepository.findAll().stream()
                .filter(user -> normalizedEmail.equals(normalizeEmail(user.getEmail())))
                .findFirst();
    }

    private User ensureEncryptedUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        boolean changed = false;
        String email = hasText(user.getEmailEncrypted())
                ? normalizeEmail(cryptoService.decrypt(user.getEmailEncrypted()))
                : normalizeEmail(user.getEmail());
        String name = hasText(user.getNameEncrypted())
                ? safeTrim(cryptoService.decrypt(user.getNameEncrypted()))
                : safeTrim(user.getName());

        if (!hasText(user.getEmailHash()) && hasText(email)) {
            user.setEmailHash(hashEmail(email));
            changed = true;
        }

        if (!hasText(user.getEmailEncrypted()) && hasText(email)) {
            user.setEmailEncrypted(cryptoService.encrypt(email));
            changed = true;
        }

        if (!hasText(user.getNameEncrypted())) {
            user.setNameEncrypted(cryptoService.encrypt(hasText(name) ? name : email));
            changed = true;
        }

        if (!hasText(user.getBioEncrypted())) {
            user.setBioEncrypted(cryptoService.encrypt(""));
            changed = true;
        }

        if (!hasText(user.getProfileImageEncrypted())) {
            user.setProfileImageEncrypted(cryptoService.encrypt(""));
            changed = true;
        }

        if (user.getContactHashes() == null) {
            user.setContactHashes(new LinkedHashSet<>());
            changed = true;
        }

        if (user.getSentRequestHashes() == null) {
            user.setSentRequestHashes(new LinkedHashSet<>());
            changed = true;
        }

        if (user.getReceivedRequestHashes() == null) {
            user.setReceivedRequestHashes(new LinkedHashSet<>());
            changed = true;
        }

        if (user.getBlockedHashes() == null) {
            user.setBlockedHashes(new LinkedHashSet<>());
            changed = true;
        }

        if (!hasText(user.getPasswordHash()) && hasText(user.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(user.getPassword()));
            changed = true;
        }

        if (hasText(user.getName()) || hasText(user.getEmail()) || hasText(user.getPassword())) {
            user.setName(null);
            user.setEmail(null);
            user.setPassword(null);
            changed = true;
        }

        return changed ? userRepository.save(user) : user;
    }

    private void validateDistinctUsers(User currentUser, User targetUser) {
        if (Objects.equals(currentUser.getEmailHash(), targetUser.getEmailHash())) {
            throw new IllegalArgumentException("You cannot perform this action on your own account.");
        }
    }

    private boolean isBlockedEitherWay(User currentUser, User targetUser) {
        return currentUser.getBlockedHashes().contains(targetUser.getEmailHash())
                || targetUser.getBlockedHashes().contains(currentUser.getEmailHash());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash value", exception);
        }
    }
}