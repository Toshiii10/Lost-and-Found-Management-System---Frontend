package com.lostandfound.view.screens;

import com.lostandfound.view.Reloadable;
import com.lostandfound.view.dialogs.ItemFormDialog;
import com.lostandfound.view.dialogs.MatchesDialog;
import com.lostandfound.view.dialogs.PhotosDialog;
import com.lostandfound.view.util.Page;
import com.lostandfound.view.util.Ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.lostandfound.api.ApiClient;
import com.lostandfound.api.Lookups;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * Your own lost reports (private to you): GET /v1/lost-items/mine.
 * New / Edit / Delete / Photos (up to 3, private), suggested Matches, and "I found it myself".
 */
public class LostReportsView extends BorderPane implements Reloadable {
    private final TableView<JsonNode> table = new TableView<>();

    public LostReportsView() {
        setPadding(new Insets(22, 28, 22, 28));

        table.setPlaceholder(new Label("You have not filed any lost reports."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().add(Ui.col("Name", n -> Ui.t(n, "name"), 180));
        table.getColumns().add(Ui.col("Category", n -> Lookups.categoryName(Ui.t(n, "categoryId")), 120));
        table.getColumns().add(Ui.col("Color", n -> Ui.t(n, "color"), 90));
        table.getColumns().add(Ui.col("Brand", n -> Ui.t(n, "brand"), 110));
        table.getColumns().add(Ui.col("Where lost", n -> Ui.t(n, "locationName"), 170));
        table.getColumns().add(Ui.col("Date", n -> Ui.t(n, "dateOccurred"), 100));
        table.getColumns().add(Ui.col("Photos", n -> String.valueOf(n.path("images").size()), 60));
        table.getColumns().add(Ui.statusCol("Status", n -> Ui.t(n, "status"), 90));
        setCenter(table);

        Button add = new Button("New lost report");
        add.getStyleClass().add("btn-primary");
        Button edit = new Button("Edit");
        Button photos = new Button("Photos");
        Button matches = new Button("Suggested matches");
        Button found = new Button("I found it myself");
        Button del = new Button("Delete");
        Button refresh = new Button("Refresh");
        for (Button b : List.of(edit, photos, matches, found, del)) {
            b.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        }
        add.setOnAction(e -> ItemFormDialog.open(getScene().getWindow(), null, true, this::load));
        edit.setOnAction(e -> ItemFormDialog.open(getScene().getWindow(), selected(), true, this::load));
        photos.setOnAction(e -> PhotosDialog.open(getScene().getWindow(), Ui.t(selected(), "id"), true,
                selected().path("images"), this::load));
        matches.setOnAction(e -> MatchesDialog.open(getScene().getWindow(), Ui.t(selected(), "id"), Ui.t(selected(), "name")));
        found.setOnAction(e -> closeReport());
        del.setOnAction(e -> delete());
        refresh.setOnAction(e -> load());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(8, add, edit, photos, matches, found, del, spacer, refresh);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 10, 0));
        setTop(Page.top("My lost reports", "Private reports of things you lost. We suggest matching found items.", bar));
        load();
    }

    @Override public void reload() { load(); }

    private JsonNode selected() { return table.getSelectionModel().getSelectedItem(); }

    private void load() {
        Ui.call(ApiClient.get("/v1/lost-items/mine"), r -> {
            if (!r.ok()) { Ui.error(r.errorMessage()); return; }
            List<JsonNode> list = new ArrayList<>();
            r.json().forEach(list::add);
            table.getItems().setAll(list);
        });
    }

    private void closeReport() {
        JsonNode item = selected();
        if (item == null) return;
        if (!Ui.confirm("Mark \"" + Ui.t(item, "name") + "\" as found by yourself?\n"
                + "The report will be closed and no more matches will be suggested.")) return;
        Ui.call(ApiClient.post("/v1/lost-items/" + Ui.t(item, "id") + "/close", null), r -> {
            if (r.ok()) load();
            else Ui.error(r.errorMessage());
        });
    }

    private void delete() {
        JsonNode item = selected();
        if (item == null) return;
        if (!Ui.confirm("Delete \"" + Ui.t(item, "name") + "\"?\nThis only works while the report is OPEN.")) return;
        Ui.call(ApiClient.delete("/v1/lost-items/" + Ui.t(item, "id")), r -> {
            if (r.ok()) load();
            else Ui.error(r.errorMessage());
        });
    }
}
