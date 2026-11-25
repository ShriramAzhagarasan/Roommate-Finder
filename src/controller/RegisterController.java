package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import application.Navigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.Gender;
import model.SleepSchedule;
import model.User;
import service.DataStore;
import service.Session;
import ui.Inputs;

public class RegisterController {
    static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    static final int MIN_PASSWORD_LENGTH = 8;

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField ageField;
    @FXML private ComboBox<Gender> genderBox;
    @FXML private TextField phoneField;
    @FXML private ComboBox<SleepSchedule> sleepBox;
    @FXML private CheckBox smokerBox;
    @FXML private CheckBox petsBox;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        genderBox.setItems(FXCollections.observableArrayList(Gender.values()));
        sleepBox.setItems(FXCollections.observableArrayList(SleepSchedule.values()));
        sleepBox.setValue(SleepSchedule.FLEXIBLE);
        Inputs.allowWholeNumbers(ageField, 3);
    }

    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        List<String> errors = new ArrayList<>();
        if (name.isEmpty()) {
            errors.add("Enter your full name.");
        }
        if (!EMAIL.matcher(email).matches()) {
            errors.add("Enter a valid email address.");
        } else if (DataStore.getInstance().emailExists(email)) {
            errors.add("An account with this email already exists. Try logging in instead.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.add("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        } else if (!password.equals(confirmPasswordField.getText())) {
            errors.add("Passwords do not match.");
        }
        Integer age = Inputs.parseWholeNumber(ageField);
        if (age == null || age < 18 || age > 99) {
            errors.add("Enter an age between 18 and 99.");
        }
        if (genderBox.getValue() == null) {
            errors.add("Select a gender (\"Prefer not to say\" is fine).");
        }
        if (!errors.isEmpty()) {
            Inputs.showErrors(errorLabel, errors);
            return;
        }

        User user = DataStore.getInstance().registerUser(email, name, password);
        user.setAge(age);
        user.setGender(genderBox.getValue());
        user.setPhone(phoneField.getText().trim());
        user.setSleepSchedule(sleepBox.getValue());
        user.setSmoker(smokerBox.isSelected());
        user.setHasPets(petsBox.isSelected());
        DataStore.getInstance().save();

        Session.login(user);
        Navigator.showMainApp();
    }

    @FXML
    private void goToLogin() {
        Navigator.showLogin();
    }

    @FXML
    private void goToWelcome() {
        Navigator.showWelcome();
    }
}
