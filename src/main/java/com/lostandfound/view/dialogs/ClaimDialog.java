package com.lostandfound.view;

import com.lostandfound.api.ApiClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Claim a found item: POST /v1/items/{id}/claims (multipart). Proof text and proof photo are both required. */
public final class ClaimDialog {
    private ClaimDialog() {}

    public static void open(Window owner, String itemId, String itemName, Runnable onDone) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Claim: " + itemName);
        ButtonType send = new ButtonType("Submit claim", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(send, ButtonType.CANCEL);

        Label intro = new Label("Prove it is yours. Describe something only the owner would know "
                + "(marks, contents, how you lost it) and attach a photo as proof. "
                + "You cannot claim an item you reported, and you can only have one pending claim per item.");
        intro.setWrapText(true);
        intro.setMaxWidth(460);

        TextArea proof = new TextArea();
        proof.setPrefRowCount(5);
        proof.setPromptText("Proof description");
        proof.setWrapText(true);

        Path[] chosen = new Path[1];
        Label fileName = new Label("No photo chosen");
        Button pick = new Button("Choose proof photo...");
        pick.setOnAction(e -> {
            Path p = Ui.pickImage(Ui.windowOf(dialog));
            if (p != null) { chosen[0] = p; fileName.setText(p.getFileName().toString()); }
        });

        Label error = new Label();
        error.setWrapText(true);
        error.setMaxWidth(460);
        error.setStyle("-fx-text-fill: #c0392b;");

        VBox content = new VBox(10, intro, proof, new HBox(10, pick, fileName), error);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);

        Button ok = (Button) dialog.getDialogPane().lookupButton(send);
        ok.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            error.setText("");
            if (proof.getText() == null || proof.getText().isBlank()) { error.setText("Write the proof description."); return; }
            if (chosen[0] == null) { error.setText("Choose a proof photo."); return; }
            try {
                if (Files.size(chosen[0]) > 5L * 1024 * 1024) { error.setText("The photo is larger than 5 MB."); return; }
                ApiClient.FilePart part = ApiClient.FilePart.fromFile("proofImage", chosen[0]);
                ok.setDisable(true);
                ApiClient.postMultipart("/v1/items/" + itemId + "/claims",
                        Map.of("proofDescription", proof.getText().trim()), part)
                        .whenComplete((resp, ex) -> Platform.runLater(() -> {
                            ok.setDisable(false);
                            if (ex != null) { error.setText("Cannot reach the server."); return; }
                            if (resp.ok()) {
                                dialog.close();
                                Ui.info("Claim submitted. Staff will review it, and you will get a notification "
                                        + "and an email when it is approved or rejected.");
                                if (onDone != null) onDone.run();
                            } else {
                                error.setText(resp.errorMessage());
                            }
                        }));
            } catch (IOException e) {
                error.setText("Could not read the photo.");
            }
        });
        dialog.show();
    }
}
