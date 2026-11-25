package model;

import java.io.Serializable;
import java.time.LocalDate;

/** A registered member. Passwords are never stored, only a salted PBKDF2 hash. */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String email;
    private final LocalDate memberSince;
    private String passwordHash;
    private String name;
    private int age;
    private Gender gender = Gender.PREFER_NOT_TO_SAY;
    private String phone = "";
    private String bio = "";
    private boolean smoker;
    private boolean hasPets;
    private SleepSchedule sleepSchedule = SleepSchedule.FLEXIBLE;

    public User(String email, String name, String passwordHash) {
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.memberSince = LocalDate.now();
    }

    public String getFirstName() {
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getMemberSince() {
        return memberSince;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? "" : phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio == null ? "" : bio;
    }

    public boolean isSmoker() {
        return smoker;
    }

    public void setSmoker(boolean smoker) {
        this.smoker = smoker;
    }

    public boolean hasPets() {
        return hasPets;
    }

    public void setHasPets(boolean hasPets) {
        this.hasPets = hasPets;
    }

    public SleepSchedule getSleepSchedule() {
        return sleepSchedule;
    }

    public void setSleepSchedule(SleepSchedule sleepSchedule) {
        this.sleepSchedule = sleepSchedule;
    }
}
