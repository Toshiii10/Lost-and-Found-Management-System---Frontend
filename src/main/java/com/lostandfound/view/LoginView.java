package com.lostandfound.view;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.lostandfound.api.ApiClient;
import com.lostandfound.api.Lookups;
import com.lostandfound.api.Session;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class LoginView extends StackPane {
    private final TextField email = new TextField();
    private final PasswordField password = new PasswordField();
    private final Button loginBtn = new Button("Log in");
    private final Label error = new Label();

    public LoginView() {
        Label title = new Label("Lost & Found");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        Label sub = new Label("Log in to continue");

        email.setPromptText("Email");
        password.setPromptText("Password");
        loginBtn.setDefaultButton(true);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        error.setWrapText(true);
        error.setStyle("-fx-text-fill: #c0392b;");

        FlowPane links = new FlowPane(10, 2,
                link("Create account", () -> AuthDialogs.register(owner())),
                link("Verify email", () -> AuthDialogs.verifyEmail(owner())),
                link("Resend verification", () -> AuthDialogs.resendVerification(owner())),
                link("Forgot password", () -> AuthDialogs.forgotPassword(owner())),
                link("Reset password", () -> AuthDialogs.resetPassword(owner())));
        links.setPrefWrapLength(340);

        VBox box = new VBox(10, title, sub, email, password, loginBtn, error, links);
        box.setMaxWidth(360);
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(box);

        loginBtn.setOnAction(e -> login());
        box.getStyleClass().add("card");
    }

    private Window owner() { return getScene() == null ? null : getScene().getWindow(); }

    private static Hyperlink link(String text, Runnable action) {
        Hyperlink h = new Hyperlink(text);
        h.setOnAction(e -> action.run());
        return h;
    }

    private void login() {
        error.setText("");
        String em = email.getText().trim();
        String pw = password.getText();
        if (em.isEmpty() || pw.isEmpty()) {
            error.setText("Enter your email and password.");
            return;
        }
        loginBtn.setDisable(true);
        Ui.call(ApiClient.post("/v1/auth/login", Map.of("email", em, "password", pw)), r -> {
            loginBtn.setDisable(false);
            if (!r.ok()) {
                // 403 = email not verified, 429 = account locked: the backend message says which
                error.setText(r.errorMessage());
                return;
            }
            JsonNode n = r.json();
            JsonNode u = n.path("user");
            Session.set(n.path("accessToken").asText(), n.path("refreshToken").asText(),
                    u.path("role").asText(), u.path("id").asText(),
                    u.path("firstName").asText() + " " + u.path("lastName").asText());
            Lookups.load().thenRun(() -> Platform.runLater(SceneManager::showMain));
        });
    }
}
