package controller;

import java.util.Optional;

import application.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.User;
import service.DataStore;
import service.Session;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {
        Platform.runLater(emailField::requestFocus);
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            return;
        }
        Optional<User> user = DataStore.getInstance().authenticate(email, password);
        if (user.isEmpty()) {
            showError("Incorrect email or password.");
            passwordField.clear();
            passwordField.requestFocus();
            return;
        }
        Session.login(user.get());
        Navigator.showMainApp();
    }

    @FXML
    private void goToRegister() {
        Navigator.showRegister();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @FXML
    private void goToWelcome() {
        Navigator.showWelcome();
    }
}
