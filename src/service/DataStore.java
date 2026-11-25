package service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import model.Listing;
import model.Message;
import model.User;

/**
 * Keeps every user, listing and message, saved to a file in the user's home folder
 * (~/.roommatefinder) after each change. Set -Droommatefinder.dataDir=... to use another folder.
 */
public final class DataStore {
    private static DataStore instance;

    private final Path dataDir;
    private final Path dataFile;
    private final Path photoDir;
    private final Path samplePhotoDir;

    private final Map<String, User> usersByEmail = new LinkedHashMap<>();
    private final List<Listing> listings = new ArrayList<>();
    private final Map<String, List<Message>> inboxes = new HashMap<>();

    /** Everything that gets written to disk. */
    private static final class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        private Map<String, User> users;
        private List<Listing> listings;
        private Map<String, List<Message>> inboxes;
        /** Which version of the demo data this file holds (0 for files saved before versioning). */
        private int sampleVersion;
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    private DataStore() {
        String override = System.getProperty("roommatefinder.dataDir");
        dataDir = override != null ? Paths.get(override) : Paths.get(System.getProperty("user.home"), ".roommatefinder");
        dataFile = dataDir.resolve("data.ser");
        photoDir = dataDir.resolve("photos");
        samplePhotoDir = dataDir.resolve("sample-photos"); // shared by demo listings, never auto-deleted
        load();
    }

    // ---------------------------------------------------------------- persistence

    private void load() {
        try {
            Files.createDirectories(photoDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create the data folder " + dataDir, e);
        }
        if (Files.exists(dataFile)) {
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(dataFile)))) {
                Snapshot snapshot = (Snapshot) in.readObject();
                usersByEmail.putAll(snapshot.users);
                listings.addAll(snapshot.listings);
                inboxes.putAll(snapshot.inboxes);
                if (snapshot.sampleVersion < SampleData.VERSION) {
                    SampleData.upgrade(this);
                    save();
                }
                return;
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                System.err.println("Could not read " + dataFile + " (" + e + "). Starting with fresh sample data;"
                        + " the old file was kept as data.ser.bak");
                try {
                    Files.move(dataFile, dataFile.resolveSibling("data.ser.bak"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                    // nothing more we can do; the file will be overwritten below
                }
                usersByEmail.clear();
                listings.clear();
                inboxes.clear();
            }
        }
        SampleData.populate(this);
        save();
    }

    public synchronized void save() {
        Snapshot snapshot = new Snapshot();
        snapshot.users = new LinkedHashMap<>(usersByEmail);
        snapshot.listings = new ArrayList<>(listings);
        snapshot.sampleVersion = SampleData.VERSION;
        snapshot.inboxes = new HashMap<>();
        inboxes.forEach((email, messages) -> snapshot.inboxes.put(email, new ArrayList<>(messages)));

        Path temp = dataFile.resolveSibling("data.ser.tmp");
        try {
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
                out.writeObject(snapshot);
            }
            try {
                Files.move(temp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save data to " + dataFile, e);
        }
    }

    // ---------------------------------------------------------------- users

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public synchronized boolean emailExists(String email) {
        return usersByEmail.containsKey(normalizeEmail(email));
    }

    /** Creates and saves a new account. Throws if the email is already registered. */
    public synchronized User registerUser(String email, String name, String password) {
        String key = normalizeEmail(email);
        if (usersByEmail.containsKey(key)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User user = new User(key, name.trim(), PasswordHasher.hash(password));
        usersByEmail.put(key, user);
        save();
        return user;
    }

    public synchronized Optional<User> authenticate(String email, String password) {
        User user = usersByEmail.get(normalizeEmail(email));
        if (user != null && PasswordHasher.verify(password, user.getPasswordHash())) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public synchronized Optional<User> findUser(String email) {
        return Optional.ofNullable(usersByEmail.get(normalizeEmail(email)));
    }

    public synchronized boolean changePassword(User user, String currentPassword, String newPassword) {
        if (!PasswordHasher.verify(currentPassword, user.getPasswordHash())) {
            return false;
        }
        user.setPasswordHash(PasswordHasher.hash(newPassword));
        save();
        return true;
    }

    // ---------------------------------------------------------------- listings

    public synchronized List<Listing> getAllListings() {
        return new ArrayList<>(listings);
    }

    public synchronized List<Listing> getListingsByOwner(String email) {
        List<Listing> result = new ArrayList<>();
        for (Listing listing : listings) {
            if (listing.getOwnerEmail().equalsIgnoreCase(email)) {
                result.add(listing);
            }
        }
        result.sort(Comparator.comparing(Listing::getCreatedAt).reversed());
        return result;
    }

    public synchronized Optional<Listing> findListing(String id) {
        return listings.stream().filter(l -> l.getId().equals(id)).findFirst();
    }

    /** Adds a new listing, or replaces the existing listing with the same id. */
    public synchronized void saveListing(Listing listing) {
        for (int i = 0; i < listings.size(); i++) {
            Listing existing = listings.get(i);
            if (existing.getId().equals(listing.getId())) {
                Set<String> stillUsed = new HashSet<>(listing.getPhotoPaths());
                deleteManagedPhotos(existing.getPhotoPaths().stream().filter(p -> !stillUsed.contains(p)).toList());
                listings.set(i, listing);
                save();
                return;
            }
        }
        listings.add(listing);
        save();
    }

    public synchronized void deleteListing(Listing listing) {
        if (listings.removeIf(l -> l.getId().equals(listing.getId()))) {
            deleteManagedPhotos(listing.getPhotoPaths());
            save();
        }
    }

    // ---------------------------------------------------------------- demo data (used by SampleData)

    /** Adds a user without hashing a password or saving; the caller saves once at the end. */
    synchronized void addUserWithoutSaving(User user) {
        usersByEmail.put(normalizeEmail(user.getEmail()), user);
    }

    synchronized void addListingWithoutSaving(Listing listing) {
        listings.add(listing);
    }

    /** Removes listings (and messages about them) without touching any photo files. */
    synchronized void removeListingsWithoutSaving(java.util.function.Predicate<Listing> which) {
        Set<String> removed = new HashSet<>();
        listings.removeIf(listing -> which.test(listing) && removed.add(listing.getId()));
        inboxes.values().forEach(messages -> messages.removeIf(m -> removed.contains(m.listingId())));
    }

    Path getSamplePhotoDir() {
        return samplePhotoDir;
    }

    // ---------------------------------------------------------------- photos

    /** Copies a photo the user picked into the app's photo folder and returns the stored path. */
    public String importPhoto(File source) throws IOException {
        String name = source.getName();
        int dot = name.lastIndexOf('.');
        String extension = dot >= 0 ? name.substring(dot).toLowerCase(Locale.ROOT) : ".jpg";
        Path target = photoDir.resolve(UUID.randomUUID() + extension);
        Files.copy(source.toPath(), target);
        return target.toAbsolutePath().toString();
    }

    public boolean isStoredPhoto(String path) {
        return Paths.get(path).toAbsolutePath().startsWith(photoDir.toAbsolutePath());
    }

    private void deleteManagedPhotos(List<String> paths) {
        for (String path : paths) {
            if (isStoredPhoto(path)) {
                try {
                    Files.deleteIfExists(Paths.get(path));
                } catch (IOException e) {
                    System.err.println("Could not delete photo " + path + ": " + e.getMessage());
                }
            }
        }
    }

    // ---------------------------------------------------------------- messages

    public synchronized void sendMessage(Message message) {
        inboxes.computeIfAbsent(normalizeEmail(message.toEmail()), key -> new ArrayList<>()).add(message);
        save();
    }

    /** Messages received by this user, newest first. */
    public synchronized List<Message> getInbox(String email) {
        List<Message> messages = new ArrayList<>(inboxes.getOrDefault(normalizeEmail(email), List.of()));
        messages.sort(Comparator.comparing(Message::sentAt).reversed());
        return messages;
    }

    /** Every message this user sent or received, oldest first. Used to show conversations. */
    public synchronized List<Message> getMessagesInvolving(String email) {
        String me = normalizeEmail(email);
        List<Message> messages = new ArrayList<>();
        for (List<Message> inbox : inboxes.values()) {
            for (Message message : inbox) {
                if (normalizeEmail(message.fromEmail()).equals(me) || normalizeEmail(message.toEmail()).equals(me)) {
                    messages.add(message);
                }
            }
        }
        messages.sort(Comparator.comparing(Message::sentAt));
        return messages;
    }

    /** The most recent message this user sent about the listing, if any. */
    public synchronized Optional<Message> lastMessageAbout(String fromEmail, Listing listing) {
        return inboxes.getOrDefault(normalizeEmail(listing.getOwnerEmail()), List.of()).stream()
                .filter(m -> m.listingId().equals(listing.getId()) && m.fromEmail().equalsIgnoreCase(fromEmail))
                .max(Comparator.comparing(Message::sentAt));
    }

    public synchronized long countInquiries(Listing listing) {
        return inboxes.getOrDefault(normalizeEmail(listing.getOwnerEmail()), List.of()).stream()
                .filter(m -> m.listingId().equals(listing.getId()))
                .count();
    }
}
