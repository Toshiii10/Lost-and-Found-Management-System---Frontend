package com.lostandfound.view.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AdminScreen extends BorderPane {

    public AdminScreen() {
        this.getStyleClass().add("admin-main-container");

        // ==========================================
        // 1. TOP HEADER BAR
        // ==========================================
        HBox headerContainer = new HBox();
        headerContainer.setAlignment(Pos.CENTER_LEFT);
        headerContainer.setPadding(new Insets(15, 25, 15, 25));
        headerContainer.getStyleClass().add("admin-header");

        Label titleLabel = new Label("Admin Master Control Center");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.getStyleClass().add("admin-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Log Out");
        logoutBtn.getStyleClass().add("button-logout");

        headerContainer.getChildren().addAll(titleLabel, spacer, logoutBtn);
        this.setTop(headerContainer);

        // ==========================================
        // 2. CENTRAL TAB PANELS (The Core Requirements)
        // ==========================================
        TabPane adminTabPane = new TabPane();
        adminTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // TAB A: Claims & Moderation (Shared with Moderator)
        Tab claimsTab = new Tab("📋 Claims & Moderation", buildClaimsPanel());

        // TAB B: User Directory Management
        Tab usersTab = new Tab("👥 User Management", buildUserManagementPanel());

        // TAB C: Categories Config
        Tab categoriesTab = new Tab("🗂 Categories Settings", buildCategoriesPanel());

        // TAB D: Drop-off Locations
        Tab locationsTab = new Tab("📍 Drop-off Locations", buildLocationsPanel());

        adminTabPane.getTabs().addAll(claimsTab, usersTab, categoriesTab, locationsTab);
        this.setCenter(adminTabPane);
    }

    // --- REQUIREMENT: Everything a Moderator can do (Review & Filter Claims) ---
    private VBox buildClaimsPanel() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label sectionLabel = new Label("Pending Item Claims (Oldest First)");
        sectionLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        TableView<Object> claimsTable = new TableView<>();
        TableColumn<Object, String> claimIdCol = new TableColumn<>("Claim ID");
        TableColumn<Object, String> itemCol = new TableColumn<>("Item Requested");
        TableColumn<Object, String> claimantCol = new TableColumn<>("Claimant");
        TableColumn<Object, String> descriptionCol = new TableColumn<>("Proof Description");
        claimsTable.getColumns().addAll(claimIdCol, itemCol, claimantCol, descriptionCol);
        VBox.setVgrow(claimsTable, Priority.ALWAYS);

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button approveBtn = new Button("✓ Approve with Note");
        Button rejectBtn = new Button("✕ Reject with Reason");
        approveBtn.getStyleClass().add("button-approve");
        rejectBtn.getStyleClass().add("button-reject");
        actions.getChildren().addAll(approveBtn, rejectBtn);

        layout.getChildren().addAll(sectionLabel, claimsTable, actions);
        return layout;
    }

    // --- REQUIREMENT: List all users filtered by role, Create Staff, Promote/Demote ---
    private VBox buildUserManagementPanel() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        HBox filtersAndCreation = new HBox(15);
        filtersAndCreation.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> roleFilter = new ComboBox<>();
        roleFilter.getItems().addAll("All Roles", "Student", "Moderator", "Admin");
        roleFilter.setValue("All Roles");

        Button createStaffBtn = new Button("+ Create Mod/Admin Account");
        createStaffBtn.getStyleClass().add("button-approve");

        filtersAndCreation.getChildren().addAll(new Label("Filter by Role:"), roleFilter, createStaffBtn);

        TableView<Object> usersTable = new TableView<>();
        TableColumn<Object, String> userIdCol = new TableColumn<>("User ID");
        TableColumn<Object, String> nameCol = new TableColumn<>("Name");
        TableColumn<Object, String> roleCol = new TableColumn<>("Assigned Role");
        TableColumn<Object, String> emailCol = new TableColumn<>("Email Verified Status");
        usersTable.getColumns().addAll(userIdCol, nameCol, roleCol, emailCol);
        VBox.setVgrow(usersTable, Priority.ALWAYS);

        HBox operationalButtons = new HBox(15);
        operationalButtons.setAlignment(Pos.CENTER_RIGHT);
        Button promoteBtn = new Button("▲ Promote Account");
        Button demoteBtn = new Button("▼ Demote Account");
        operationalButtons.getChildren().addAll(promoteBtn, demoteBtn);

        layout.getChildren().addAll(filtersAndCreation, usersTable, operationalButtons);
        return layout;
    }

    // --- REQUIREMENT: Add, edit, and delete categories ---
    private VBox buildCategoriesPanel() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        HBox entryBar = new HBox(15);
        entryBar.setAlignment(Pos.CENTER_LEFT);
        TextField categoryField = new TextField();
        categoryField.setPromptText("Enter new category name...");
        Button addCatBtn = new Button("Add Category");
        entryBar.getChildren().addAll(categoryField, addCatBtn);

        TableView<Object> categoryTable = new TableView<>();
        TableColumn<Object, String> catIdCol = new TableColumn<>("Category ID");
        TableColumn<Object, String> catNameCol = new TableColumn<>("Category Name");
        categoryTable.getColumns().addAll(catIdCol, catNameCol);
        VBox.setVgrow(categoryTable, Priority.ALWAYS);

        HBox modifications = new HBox(15);
        modifications.setAlignment(Pos.CENTER_RIGHT);
        Button editBtn = new Button("Edit Category Name");
        Button deleteBtn = new Button("🗑 Delete Category");
        deleteBtn.getStyleClass().add("button-delete");
        modifications.getChildren().addAll(editBtn, deleteBtn);

        layout.getChildren().addAll(entryBar, categoryTable, modifications);
        return layout;
    }

    // --- REQUIREMENT: Add, edit, delete locations & View sensitive details (Contact/Phone) ---
    private VBox buildLocationsPanel() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label restrictionNotice = new Label("🔒 Secure View: Admin clearance active. Displaying sensitive contact phone numbers.");
        restrictionNotice.setStyle("-fx-text-fill: #0d6efd; -fx-font-weight: bold;");

        TableView<Object> locationsTable = new TableView<>();
        TableColumn<Object, String> locNameCol = new TableColumn<>("Location Name");
        TableColumn<Object, String> contactCol = new TableColumn<>("Contract Person (Admin Only)");
        TableColumn<Object, String> phoneCol = new TableColumn<>("Phone Number (Admin Only)");
        locationsTable.getColumns().addAll(locNameCol, contactCol, phoneCol);
        VBox.setVgrow(locationsTable, Priority.ALWAYS);

        HBox modifications = new HBox(15);
        modifications.setAlignment(Pos.CENTER_RIGHT);
        Button addLocBtn = new Button("+ Add New Location");
        Button editLocBtn = new Button("Edit Location Details");
        Button deleteLocBtn = new Button("🗑 Delete Location");
        deleteLocBtn.getStyleClass().add("button-delete");
        modifications.getChildren().addAll(addLocBtn, editLocBtn, deleteLocBtn);

        layout.getChildren().addAll(restrictionNotice, locationsTable, modifications);
        return layout;
    }
}
