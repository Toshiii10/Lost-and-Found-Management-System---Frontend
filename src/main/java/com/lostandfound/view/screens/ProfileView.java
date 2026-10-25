package com.lostandfound.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.lostandfound.api.ApiClient;
import com.lostandfound.api.Lookups;
import com.lostandfound.api.Session;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Your profile (GET /v1/auth/me), "log out of all devices", and the category / drop-off lists.
 * Only Moderators and Admins can edit their own name and phone (the backend refuses students).
 */
public class ProfileView extends BorderPane implements Reloadable {
    private final Label name = new Label();
    private final Label role = new Label();
    private final Label studentId = new Label();
    private final Label email = new Label();
    private final Label phone = new Label();
    private String first = "", last = "", phoneNum = "";

    public ProfileView() {
        setPadding(new Insets(16));

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(8);
        addRow(g, 0, "Name", name);
        addRow(g, 1, "Role", role);
        addRow(g, 2, "Student ID", studentId);
        addRow(g, 3, "Email", email);
        addRow(g, 4, "Phone", phone);

        Button edit = new Button("Edit my name and phone");
        edit.setVisible(Session.isStaff());
        edit.setManaged(Session.isStaff());
        edit.setOnAction(e -> AuthDialogs.editProfile(getScene().getWindow(), "Edit my profile",
                "You can change your name and phone number. Email and student ID cannot be changed here.",
                "/v1/auth/me", first, last, phoneNum, this::load));

        Button logoutAll = new Button("Log out of all devices");
        logoutAll.setOnAction(e -> logoutAll());

        Label hint = new Label(Session.isStaff() ? ""
                : "Your name, phone and email can only be changed by staff.");
        hint.setWrapText(true);

        VBox left = new VBox(14, new Label("My profile"), g, new HBox(8, edit, logoutAll), hint);
        ((Label) left.getChildren().get(0)).setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        ListView<String> cats = new ListView<>(FXCollections.observableArrayList(
                Lookups.categories().stream().map(Lookups.Option::name).toList()));
        ListView<String> drops = new ListView<>(FXCollections.observableArrayList(
                Lookups.dropLocations().stream().map(Lookups.Option::name).toList()));
        cats.setPrefHeight(200);
        drops.setPrefHeight(200);
        VBox right = new VBox(8, new Label("Categories"), cats, new Label("Drop-off locations"), drops);
        right.setPrefWidth(300);
        right.setPadding(new Insets(0, 0, 0, 30));

        setLeft(left);
        setCenter(right);
        load();
    }

    private static void addRow(GridPane g, int r, String label, Label value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold;");
        g.addRow(r, l, value);
    }

    @Override public void reload() { load(); }

    private void load() {
        Ui.call(ApiClient.get("/v1/auth/me"), r -> {
            if (!r.ok()) { Ui.error(r.errorMessage()); return; }
            JsonNode u = r.json();
            first = Ui.t(u, "firstName");
            last = Ui.t(u, "lastName");
            phoneNum = Ui.t(u, "phoneNum");
            name.setText(first + " " + last);
            role.setText(Ui.t(u, "role"));
            studentId.setText(Ui.t(u, "studentId").isEmpty() ? "-" : Ui.t(u, "studentId"));
            email.setText(Ui.t(u, "email"));
            phone.setText(phoneNum);
        });
    }

    private void logoutAll() {
        if (!Ui.confirm("Log out of every device, including this one?")) return;
        Ui.call(ApiClient.post("/v1/auth/logout-all", null), r -> {
            if (!r.ok()) { Ui.error(r.errorMessage()); return; }
            Session.clear();
            SceneManager.showLogin();
        });
    }
}
