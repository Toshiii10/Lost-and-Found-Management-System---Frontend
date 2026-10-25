package com.lostandfound.view;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.lostandfound.api.ApiClient;
import com.lostandfound.api.Lookups;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Create or edit a report.
 *   lost == false: found item  -> POST /v1/items , PUT /v1/items/{id}        (has a drop-off place)
 *   lost == true:  lost report -> POST /v1/lost-items , PUT /v1/lost-items/{id}
 * Errors from the server are shown inside the dialog so nothing typed is lost.
 */
public final class ItemFormDialog {
    private ItemFormDialog() {}

    public static void open(Window owner, JsonNode existing, boolean lost, Runnable onSaved) {
        final String base = lost ? "/v1/lost-items" : "/v1/items";
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(existing == null
                ? (lost ? "File a lost report" : "Report a found item")
                : (lost ? "Edit lost report" : "Edit found item"));
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField name = new TextField();
        TextArea description = new TextArea();
        description.setPrefRowCount(3);
        ComboBox<Lookups.Option> category = new ComboBox<>();
        category.getItems().setAll(Lookups.categories());
        category.setMaxWidth(Double.MAX_VALUE);
        TextField color = new TextField();
        TextField brand = new TextField();
        TextArea marks = new TextArea();
        marks.setPrefRowCount(2);
        marks.setPromptText(lost ? "Private: only you and staff see this."
                : "Private: only used to check the real owner. Not shown publicly.");
        TextField where = new TextField();
        where.setPromptText(lost ? "Where you lost it" : "Where you found it");
        ComboBox<Lookups.Option> drop = new ComboBox<>();
        drop.getItems().setAll(Lookups.dropLocations());
        drop.setMaxWidth(Double.MAX_VALUE);
        DatePicker date = new DatePicker(LocalDate.now());
        date.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isAfter(LocalDate.now())); // backend rejects future dates
            }
        });

        if (existing != null) {
            name.setText(existing.path("name").asText());
            description.setText(existing.path("description").asText());
            select(category, existing.path("categoryId").asText());
            color.setText(existing.path("color").asText());
            brand.setText(existing.path("brand").asText());
            marks.setText(existing.path("distinguishingMarks").asText());
            where.setText(existing.path("locationName").asText());
            if (!lost) select(drop, existing.path("dropLocationId").asText());
            try { date.setValue(LocalDate.parse(existing.path("dateOccurred").asText())); }
            catch (Exception ignored) { /* keep today */ }
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        int r = 0;
        grid.addRow(r++, new Label("Name"), name);
        grid.addRow(r++, new Label("Description"), description);
        grid.addRow(r++, new Label("Category"), category);
        grid.addRow(r++, new Label("Color"), color);
        grid.addRow(r++, new Label("Brand"), brand);
        grid.addRow(r++, new Label("Distinguishing marks"), marks);
        grid.addRow(r++, new Label(lost ? "Where lost" : "Where found"), where);
        if (!lost) grid.addRow(r++, new Label("Left at (drop-off)"), drop);
        grid.addRow(r++, new Label(lost ? "Date lost" : "Date found"), date);
        grid.getColumnConstraints().add(new ColumnConstraints(140));
        grid.getColumnConstraints().add(new ColumnConstraints(320));

        Label error = new Label();
        error.setWrapText(true);
        error.setMaxWidth(470);
        error.setStyle("-fx-text-fill: #c0392b;");
        dialog.getDialogPane().setContent(new VBox(6, grid, error));

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        // Take over Save so the dialog stays open if the server rejects the data.
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            error.setText("");
            if (blank(name) || blank(description) || blank(color) || blank(brand) || blank(marks)
                    || blank(where) || category.getValue() == null || (!lost && drop.getValue() == null)
                    || date.getValue() == null) {
                error.setText("Please fill in every field.");
                return;
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", name.getText().trim());
            body.put("description", description.getText().trim());
            body.put("categoryId", category.getValue().id());
            body.put("color", color.getText().trim());
            body.put("brand", brand.getText().trim());
            body.put("distinguishingMarks", marks.getText().trim());
            body.put("locationName", where.getText().trim());
            if (!lost) body.put("dropLocationId", drop.getValue().id());
            body.put("dateOccurred", date.getValue().toString()); // yyyy-MM-dd

            saveBtn.setDisable(true);
            CompletableFuture<ApiClient.Response> req = existing == null
                    ? ApiClient.post(base, body)
                    : ApiClient.put(base + "/" + existing.path("id").asText(), body);
            req.whenComplete((resp, ex) -> Platform.runLater(() -> {
                saveBtn.setDisable(false);
                if (ex != null) { error.setText("Cannot reach the server."); return; }
                if (resp.ok()) { dialog.close(); onSaved.run(); }
                else error.setText(resp.errorMessage());
            }));
        });

        dialog.show();
    }

    private static boolean blank(TextField f) { return f.getText() == null || f.getText().isBlank(); }
    private static boolean blank(TextArea f)  { return f.getText() == null || f.getText().isBlank(); }

    private static void select(ComboBox<Lookups.Option> box, String id) {
        for (Lookups.Option o : box.getItems()) {
            if (o.id().equals(id)) { box.setValue(o); return; }
        }
    }
}
