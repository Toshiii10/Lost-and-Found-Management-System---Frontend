package com.lostandfound.view;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.lostandfound.api.ApiClient;
import com.lostandfound.api.Lookups;
import com.lostandfound.api.Session;
import com.lostandfound.view.dialogs.AuthDialogs;
import com.lostandfound.view.util.Ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 *CEBIMS-theme screen:
 */
public class LoginView extends BorderPane {
    private final TextField email = new TextField();
    private final PasswordField password = new PasswordField();
    private final Button loginBtn = new Button("Sign in");
    private final Label errorBanner = new Label();

    public LoginView() {
        setStyle("-fx-background-color: #f8fafc;");
        setLeft(buildBrandSidebar());
        setCenter(buildLoginWorkspace());
    }

    // ---- Left Dark Branding Panel 

    private VBox buildBrandSidebar() {
        VBox side = new VBox(18);
        side.setPrefWidth(440);
        side.setPadding(new Insets(42, 38, 36, 38));
        side.setStyle("-fx-background-color: #0f172a;");

        // Top Brand Logo
        Label logoIcon = new Label("🎓");
        logoIcon.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 22px;"
                + "-fx-min-width: 46; -fx-min-height: 46; -fx-alignment: center; -fx-background-radius: 12;");
        Label brandTitle = new Label("CEBIMS L&F");
        brandTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 21px; -fx-font-weight: bold;");
        Label brandSub = new Label("Campus Lost & Found Management System");
        brandSub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        HBox brandHeader = new HBox(14, logoIcon, new VBox(2, brandTitle, brandSub));
        brandHeader.setAlignment(Pos.CENTER_LEFT);

        // Hero Headline
        Label pill = new Label("●  CAMPUS PORTAL");
        pill.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #60a5fa; -fx-font-size: 11px;"
                + "-fx-font-weight: bold; -fx-padding: 4 12 4 12; -fx-background-radius: 999;");

        Label headline = new Label("Smart recovery for campus belongings.");
        headline.setWrapText(true);
        headline.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label desc = new Label("Report found items, file private lost reports, and get automated match suggestions backed by verified staff review.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13.5px;");

        VBox heroText = new VBox(12, pill, headline, desc);
        heroText.setPadding(new Insets(24, 0, 10, 0));

        // Feature Highlights (CEBIMS Card Pill Style)
        VBox features = new VBox(12,
                featureRow("🔍", "#dbeafe", "#2563eb", "Automated Smart Matching",
                        "Instant matching across category, brand, color, and date"),
                featureRow("🛡", "#dcfce7", "#15803d", "Verified Claim Protection",
                        "Photo proof & secret distinguishing marks checked by staff"),
                featureRow("🔔", "#fef3c7", "#d97706", "Real-Time Alerts & Pickup",
                        "In-app & email updates with exact drop-off location details")
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bottom Campus Operational Card (Exact replica from screenshot)
        Label campIcon = new Label("🏢");
        campIcon.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-size: 15px;"
                + "-fx-min-width: 36; -fx-min-height: 36; -fx-alignment: center; -fx-background-radius: 9;");
        Label campName = new Label("Main Campus");
        campName.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label campSub = new Label("All systems operational");
        campSub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px;");

        Label statusDot = new Label("● Online");
        statusDot.setStyle("-fx-background-color: #064e3b; -fx-text-fill: #34d399; -fx-font-size: 11px;"
                + "-fx-font-weight: bold; -fx-padding: 3 9 3 9; -fx-background-radius: 999;");
        Region cardSpacer = new Region();
        HBox.setHgrow(cardSpacer, Priority.ALWAYS);

        HBox campusCard = new HBox(12, campIcon, new VBox(1, campName, campSub), cardSpacer, statusDot);
        campusCard.setAlignment(Pos.CENTER_LEFT);
        campusCard.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12; -fx-padding: 14;");

        side.getChildren().addAll(brandHeader, heroText, features, spacer, campusCard);
        return side;
    }

    private HBox featureRow(String icon, String iconBg, String iconFg, String title, String sub) {
        Label ic = new Label(icon);
        ic.setStyle("-fx-background-color: " + iconBg + "; -fx-text-fill: " + iconFg
                + "; -fx-font-size: 14px; -fx-min-width: 34; -fx-min-height: 34; -fx-alignment: center; -fx-background-radius: 8;");
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label s = new Label(sub);
        s.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px;");
        s.setWrapText(true);
        HBox row = new HBox(12, ic, new VBox(2, t, s));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: rgba(30, 41, 59, 0.65); -fx-background-radius: 10; -fx-padding: 11 14 11 14;");
        return row;
    }

    // Right Login Form Card 

    private StackPane buildLoginWorkspace() {
        StackPane wrapper = new StackPane();
        wrapper.setPadding(new Insets(32));

        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setMaxWidth(430);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(34, 36, 30, 36));

        // Card Header
        Label badge = new Label("AUTHENTICATION");
        badge.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-font-size: 10.5px;"
                + "-fx-font-weight: bold; -fx-padding: 3 10 3 10; -fx-background-radius: 999;");

        Label title = new Label("Welcome back!");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label sub = new Label("Enter your campus credentials to access your dashboard.");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        sub.setWrapText(true);

        VBox header = new VBox(4, badge, title, sub);

        // Email Input
        Label emailLbl = new Label("Email Address");
        emailLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 12.5px;");
        email.setPromptText("student@campus.edu.ph");
        email.setPrefHeight(40);
        VBox emailGroup = new VBox(6, emailLbl, email);

        // Password Input + Forgot Password Link
        Label pwLbl = new Label("Password");
        pwLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 12.5px;");
        Hyperlink forgotLink = link("Forgot password?", () -> AuthDialogs.forgotPassword(owner()));
        forgotLink.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: 600; -fx-font-size: 12px; -fx-border-color: transparent;");
        Region pwSpacer = new Region();
        HBox.setHgrow(pwSpacer, Priority.ALWAYS);
        HBox pwHeader = new HBox(pwLbl, pwSpacer, forgotLink);
        pwHeader.setAlignment(Pos.CENTER_LEFT);

        password.setPromptText("••••••••••••");
        password.setPrefHeight(40);
        VBox pwGroup = new VBox(6, pwHeader, password);

        // Error Alert Banner (CEBIMS Red Alert Box)
        errorBanner.setWrapText(true);
        errorBanner.setMaxWidth(Double.MAX_VALUE);
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
        errorBanner.setStyle("-fx-background-color: #fef2f2; -fx-border-color: #fecaca; -fx-text-fill: #b91c1c;"
                + "-fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 10 12 10 12;"
                + "-fx-background-radius: 8; -fx-border-radius: 8;");

        // Sign In Primary Button
        loginBtn.setDefaultButton(true);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(42);
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 14px;"
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        loginBtn.setOnAction(e -> login());

        // Create Account Box
        Button registerBtn = new Button("Create Student Account");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setPrefHeight(38);
        registerBtn.setOnAction(e -> AuthDialogs.register(owner()));

        // Verification & Token Tools Footer
        Label toolsTitle = new Label("EMAIL & TOKEN TOOLS");
        toolsTitle.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        Button verifyBtn = pillBtn(" Verify email", () -> AuthDialogs.verifyEmail(owner()));
        Button resendBtn = pillBtn(" Resend link", () -> AuthDialogs.resendVerification(owner()));
        Button resetBtn  = pillBtn(" Reset token", () -> AuthDialogs.resetPassword(owner()));
        HBox toolRow = new HBox(8, verifyBtn, resendBtn, resetBtn);
        toolRow.setAlignment(Pos.CENTER);

        VBox footer = new VBox(8, new Separator(), toolsTitle, toolRow);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(4, 0, 0, 0));

        card.getChildren().addAll(header, emailGroup, pwGroup, errorBanner, loginBtn, registerBtn, footer);
        wrapper.getChildren().add(card);
        return wrapper;
    }

    private Window owner() { return getScene() == null ? null : getScene().getWindow(); }

    private static Hyperlink link(String text, Runnable action) {
        Hyperlink h = new Hyperlink(text);
        h.setOnAction(e -> action.run());
        return h;
    }

    private static Button pillBtn(String text, Runnable action) {
        Button b = new Button(text);
        b.setCursor(Cursor.HAND);
        b.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #475569; -fx-border-color: #e2e8f0;"
                + "-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-padding: 5 10 5 10;"
                + "-fx-background-radius: 6; -fx-border-radius: 6;");
        b.setOnAction(e -> action.run());
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private void showError(String msg) {
        if (msg == null || msg.isBlank()) {
            errorBanner.setText("");
            errorBanner.setVisible(false);
            errorBanner.setManaged(false);
        } else {
            errorBanner.setText("⚠  " + msg);
            errorBanner.setVisible(true);
            errorBanner.setManaged(true);
        }
    }

    private void login() {
        showError(null);
        String em = email.getText() == null ? "" : email.getText().trim();
        String pw = password.getText() == null ? "" : password.getText();
        if (em.isEmpty() || pw.isEmpty()) {
            showError("Enter both your email address and password.");
            return;
        }
        loginBtn.setDisable(true);
        loginBtn.setText("Signing in...");
        Ui.call(ApiClient.post("/v1/auth/login", Map.of("email", em, "password", pw)), r -> {
            loginBtn.setDisable(false);
            loginBtn.setText("Signing in...");
            if (!r.ok()) {
                showError(r.errorMessage());
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
