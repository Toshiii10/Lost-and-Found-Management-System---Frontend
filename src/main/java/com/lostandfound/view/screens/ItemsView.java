package com.lostandfound.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.lostandfound.api.ApiClient;
import com.lostandfound.api.Lookups;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Found items.
 *  mine == false: GET /v1/items (open items, full-text + fuzzy search, category filter, paging).
 *                 Double-click or "View / Claim" opens the public details.
 *  mine == true:  GET /v1/items/mine, with New / Edit / Photos / Delete (owner only, enforced by the backend).
 */
public class ItemsView extends BorderPane implements Reloadable {
    private final boolean mine;
    private final TableView<JsonNode> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<Lookups.Option> category = new ComboBox<>();
    private final Label pageLabel = new Label();
    private final Button prev = new Button("< Prev");
    private final Button next = new Button("Next >");
    private int page = 0;
    private int totalPages = 1;

    public ItemsView(boolean mine) {
        this.mine = mine;
        setPadding(new Insets(12));

        table.setPlaceholder(new Label("No items found."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().add(Ui.col("Name", n -> Ui.t(n, "name"), 180));
        table.getColumns().add(Ui.col("Category", n -> Lookups.categoryName(Ui.t(n, "categoryId")), 120));
        table.getColumns().add(Ui.col("Color", n -> Ui.t(n, "color"), 90));
        table.getColumns().add(Ui.col("Brand", n -> Ui.t(n, "brand"), 110));
        table.getColumns().add(Ui.col("Where found", n -> Ui.t(n, "locationName"), 160));
        table.getColumns().add(Ui.col("Drop-off", n -> Lookups.dropName(Ui.t(n, "dropLocationId")), 140));
        table.getColumns().add(Ui.col("Date", n -> Ui.t(n, "dateOccurred"), 100));
        table.getColumns().add(Ui.col("Photos", n -> String.valueOf(n.path("images").size()), 60));
        table.getColumns().add(Ui.col("Status", n -> Ui.t(n, "status"), 90));
        setCenter(table);

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> load());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 10, 0));

        if (mine) {
            Button add = new Button("New report");
            Button edit = new Button("Edit");
            Button photos = new Button("Photos");
            Button del = new Button("Delete");
            for (Button b : List.of(edit, photos, del)) {
                b.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
            }
            add.setOnAction(e -> ItemFormDialog.open(getScene().getWindow(), null, false, this::load));
            edit.setOnAction(e -> ItemFormDialog.open(getScene().getWindow(),
                    table.getSelectionModel().getSelectedItem(), false, this::load));
            photos.setOnAction(e -> {
                JsonNode sel = table.getSelectionModel().getSelectedItem();
                PhotosDialog.open(getScene().getWindow(), Ui.t(sel, "id"), false, sel.path("images"), this::load);
            });
            del.setOnAction(e -> delete(table.getSelectionModel().getSelectedItem()));
            bar.getChildren().addAll(add, edit, photos, del, spacer, refresh);
        } else {
            search.setPromptText("Search by name, color, brand...");
            search.setPrefWidth(280);
            category.getItems().add(new Lookups.Option(null, "All categories"));
            category.getItems().addAll(Lookups.categories());
            category.setValue(category.getItems().get(0));
            category.setOnAction(e -> { page = 0; load(); });
            Button go = new Button("Search");
            go.setOnAction(e -> { page = 0; load(); });
            search.setOnAction(e -> { page = 0; load(); });
            Button view = new Button("View / Claim");
            view.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
            view.setOnAction(e -> openDetail(table.getSelectionModel().getSelectedItem()));
            bar.getChildren().addAll(search, category, go, view, spacer, refresh);

            table.setRowFactory(tv -> {
                TableRow<JsonNode> row = new TableRow<>();
                row.setOnMouseClicked(ev -> {
                    if (ev.getClickCount() == 2 && !row.isEmpty()) openDetail(row.getItem());
                });
                return row;
            });

            prev.setOnAction(e -> { if (page > 0) { page--; load(); } });
            next.setOnAction(e -> { if (page + 1 < totalPages) { page++; load(); } });
            HBox paging = new HBox(10, prev, pageLabel, next);
            paging.setAlignment(Pos.CENTER);
            paging.setPadding(new Insets(10, 0, 0, 0));
            setBottom(paging);
        }
        setTop(bar);
        load();
    }

    @Override public void reload() { load(); }

    private void load() {
        String path;
        if (mine) {
            path = "/v1/items/mine";
        } else {
            String q = search.getText() == null ? "" : search.getText().trim();
            path = "/v1/items?page=" + page + "&size=20"
                    + (q.isEmpty() ? "" : "&q=" + URLEncoder.encode(q, StandardCharsets.UTF_8));
            Lookups.Option c = category.getValue();
            if (c != null && c.id() != null) path += "&categoryId=" + URLEncoder.encode(c.id(), StandardCharsets.UTF_8);
        }
        Ui.call(ApiClient.get(path), r -> {
            if (!r.ok()) { Ui.error(r.errorMessage()); return; }
            JsonNode root = r.json();
            JsonNode rows = mine ? root : root.path("content");
            List<JsonNode> list = new ArrayList<>();
            rows.forEach(list::add);
            table.getItems().setAll(list);
            if (!mine) {
                totalPages = Math.max(1, root.path("totalPages").asInt(1));
                pageLabel.setText("Page " + (page + 1) + " of " + totalPages
                        + "   (" + root.path("totalElements").asLong(list.size()) + " items)");
                prev.setDisable(page == 0);
                next.setDisable(page + 1 >= totalPages);
            }
        });
    }

    private void openDetail(JsonNode item) {
        if (item != null) ItemDetailDialog.open(getScene().getWindow(), item);
    }

    private void delete(JsonNode item) {
        if (item == null) return;
        if (!Ui.confirm("Delete \"" + Ui.t(item, "name") + "\"?\nThis only works while it is OPEN with no pending claim.")) return;
        Ui.call(ApiClient.delete("/v1/items/" + Ui.t(item, "id")), r -> {
            if (r.ok()) load();
            else Ui.error(r.errorMessage());
        });
    }
}
