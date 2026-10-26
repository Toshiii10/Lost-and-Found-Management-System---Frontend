package com.lostandfound.view;
import java.util.LinkedHashMap;
import java.util.Map;

import com.lostandfound.api.ApiClient;
import com.lostandfound.api.Session;
import com.lostandfound.view.screens.DashboardView;
import com.lostandfound.view.screens.ItemsView;
import com.lostandfound.view.screens.LostReportsView;
import com.lostandfound.view.screens.ModerationView;
import com.lostandfound.view.screens.MyClaimsView;
import com.lostandfound.view.screens.NotificationsView;
import com.lostandfound.view.screens.ProfileView;
import com.lostandfound.view.screens.StaffLostView;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Modern CEBIMS-style shell: Left dark sidebar navigation + Top header bar + Center content workspace.
 */
public class MainView extends BorderPane {
    private final Map<String, Node> views = new LinkedHashMap<>();
    private final Map<String, HBox> navButtons = new LinkedHashMap<>();
    private final StackPane contentArea = new StackPane();
    private final Label notifBadge = new Label("0");
    private final Label bellBadge = new Label("0");
    private final Timeline poller;

    public MainView() {
        // Pre-build views so switching is instant and state is preserved
        views.put("dashboard", new DashboardView(this::switchTo));
        views.put("found", new ItemsView(false));
        views.put("my-found", new ItemsView(true));
        views.put("my-lost", new LostReportsView());
        views.put("my-claims", new MyClaimsView());
        views.put("notifications", new NotificationsView(this::refreshBadge));
        views.put("profile", new ProfileView());
        if (Session.isStaff()) {
            views.put("moderation", new ModerationView());
            views.put("staff-lost", new StaffLostView());
        }

        setLeft(buildSidebar());
        setTop(buildTopBar());
        setCenter(contentArea);

        switchTo("dashboard");

        refreshBadge();
        poller = new Timeline(new KeyFrame(Duration.seconds(60), e -> refreshBadge()));
        poller.setCycleCount(Animation.INDEFINITE);
        poller.play();
        sceneProperty().addListener((o, old, scene) -> { if (scene == null) poller.stop(); });
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(6);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(245);

        // Brand Logo Header
        Label logoIcon = new Label("🎓");
        logoIcon.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 18px;"
                + "-fx-min-width: 38; -fx-min-height: 38; -fx-alignment: center; -fx-background-radius: 10;");
        Label brandTitle = new Label("CEBIMS L&F");
        brandTitle.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");
        Label brandSub = new Label("Campus Lost & Found\nManagement System");
        brandSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10.5px;");
        HBox brandBox = new HBox(12, logoIcon, new VBox(1, brandTitle, brandSub));
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(0, 6, 14, 6));

        Label workSec = new Label("WORKSPACE");
        workSec.getStyleClass().add("sidebar-section");

        sidebar.getChildren().addAll(
                brandBox,
                workSec,
                navItem("dashboard", "🏠   Dashboard", null),
                navItem("found", "📦   Found Items", null),
                navItem("my-found", "📥   My Found Reports", null),
                navItem("my-lost", "🔍   My Lost Reports", null),
                navItem("my-claims", "📋   My Claims", null),
                navItem("notifications", "🔔   Notifications", notifBadge)
        );

        if (Session.isStaff()) {
            Label staffSec = new Label("MODERATION");
            staffSec.getStyleClass().add("sidebar-section");
            sidebar.getChildren().addAll(
                    staffSec,
                    navItem("moderation", "⚖   Claims Review", null),
                    navItem("staff-lost", "📂   All Lost Reports", null)
            );
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bottom Campus Status Card
        Label campIcon = new Label("🏢");
        campIcon.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-size: 14px;"
                + "-fx-min-width: 32; -fx-min-height: 32; -fx-alignment: center; -fx-background-radius: 8;");
        Label campName = new Label("Main Campus");
        campName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12.5px;");
        Label campSub = new Label("All systems operational");
        campSub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        HBox campusCard = new HBox(10, campIcon, new VBox(1, campName, campSub));
        campusCard.setAlignment(Pos.CENTER_LEFT);
        campusCard.getStyleClass().add("campus-card");

        sidebar.getChildren().addAll(
                spacer,
                campusCard,
                navItem("profile", "⚙   Settings", null)
        );
        return sidebar;
    }

    private HBox navItem(String key, String label, Label badge) {
        Label text = new Label(label);
        text.setStyle("-fx-text-fill: inherit; -fx-font-weight: 600; -fx-font-size: 13.5px;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox row = new HBox(8, text, sp);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("nav-btn");
        if (badge != null) {
            badge.setVisible(false);
            badge.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: #ffffff; -fx-font-size: 11px;"
                    + "-fx-font-weight: bold; -fx-padding: 1 7 1 7; -fx-background-radius: 999;");
            row.getChildren().add(badge);
        }
        row.setOnMouseClicked(e -> switchTo(key));
        navButtons.put(key, row);
        return row;
    }

    private HBox buildTopBar() {
        TextField quickSearch = new TextField();
        quickSearch.setPromptText("🔍  Search found items, brands, or locations...");
        quickSearch.getStyleClass().add("search-box");
        quickSearch.setPrefWidth(380);
        quickSearch.setOnAction(e -> switchTo("found"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Notification Bell Button with Red Badge
        Button bellBtn = new Button("🔔");
        bellBtn.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-background-radius: 8; -fx-border-radius: 8;");
        bellBtn.setOnAction(e -> switchTo("notifications"));

        bellBadge.setVisible(false);
        bellBadge.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 10px;"
                + "-fx-font-weight: bold; -fx-padding: 1 5 1 5; -fx-background-radius: 999;");
        StackPane bellStack = new StackPane(bellBtn, bellBadge);
        StackPane.setAlignment(bellBadge, Pos.TOP_RIGHT);

        // User Profile Chip
        Label userAvatar = new Label("👤");
        userAvatar.setStyle("-fx-background-color: #e2e8f0; -fx-min-width: 34; -fx-min-height: 34;"
                + "-fx-alignment: center; -fx-background-radius: 999;");
        Label uName = new Label(Session.displayName());
        uName.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a; -fx-font-size: 13px;");
        Label uRole = new Label(Session.role());
        uRole.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        VBox uInfo = new VBox(0, uName, uRole);
        HBox userChip = new HBox(10, userAvatar, uInfo);
        userChip.setAlignment(Pos.CENTER_LEFT);
        userChip.setCursor(javafx.scene.Cursor.HAND);
        userChip.setOnMouseClicked(e -> switchTo("profile"));

        Button logoutBtn = new Button("Log out");
        logoutBtn.setOnAction(e -> logout());

        HBox top = new HBox(18, quickSearch, spacer, bellStack, userChip, logoutBtn);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");
        return top;
    }

    public void switchTo(String key) {
        Node view = views.get(key);
        if (view == null) return;

        navButtons.forEach((k, btn) -> {
            btn.getStyleClass().remove("nav-btn-active");
            for (Node child : btn.getChildren()) {
                if (child instanceof Label lbl && lbl != notifBadge) {
                    lbl.setStyle("-fx-text-fill: " + (k.equals(key) ? "#ffffff" : "#94a3b8")
                            + "; -fx-font-weight: 600; -fx-font-size: 13.5px;");
                }
            }
        });
        HBox active = navButtons.get(key);
        if (active != null) active.getStyleClass().add("nav-btn-active");

        contentArea.getChildren().setAll(view);
        if (view instanceof Reloadable r) r.reload();
    }

    private void refreshBadge() {
        if (!Session.loggedIn()) return;
        ApiClient.get("/v1/notifications/unread-count").whenComplete((r, ex) -> Platform.runLater(() -> {
            if (ex != null || !r.ok()) return;
            long n = r.json().path("count").asLong(0);
            notifBadge.setText(String.valueOf(n));
            notifBadge.setVisible(n > 0);
            bellBadge.setText(String.valueOf(n));
            bellBadge.setVisible(n > 0);
        }));
    }

    private void logout() {
        poller.stop();
        String refresh = Session.refreshToken();
        if (refresh != null) {
            ApiClient.post("/v1/auth/logout", Map.of("refreshToken", refresh));
        }
        Session.clear();
        SceneManager.showLogin();
    }
}
