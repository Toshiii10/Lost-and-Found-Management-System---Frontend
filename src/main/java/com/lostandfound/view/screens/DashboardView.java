package com.lostandfound.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.lostandfound.api.ApiClient;
import com.lostandfound.api.Lookups;
import com.lostandfound.api.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * Replicates the CEBIMS modern dashboard: 4 KPI cards, Recent claims/requests table,
 * Live Donut chart + Category progress bars, Recent found items, and Alerts panel.
 */
public class DashboardView extends ScrollPane implements Reloadable {
    private final Consumer<String> navigateTo;

    // KPI Card labels
    private final Label kpiOpenVal = new Label("0");
    private final Label kpiOpenSub = new Label("Loading campus inventory...");
    private final Label kpiClaimsVal = new Label("0");
    private final Label kpiClaimsSub = new Label("0 awaiting review");
    private final Label kpiLostVal = new Label("0");
    private final Label kpiLostSub = new Label("Active reports");
    private final Label kpiAlertVal = new Label("0");
    private final Label kpiAlertSub = new Label("Unread updates");

    // Tables & Charts
    private final TableView<JsonNode> recentClaimsTable = new TableView<>();
    private final TableView<JsonNode> recentItemsTable = new TableView<>();
    private final Label reviewCountHint = new Label("0 need review");
    private final Canvas donutCanvas = new Canvas(130, 130);
    private final VBox legendBox = new VBox(6);
    private final VBox categoryBarsBox = new VBox(10);
    private final Label alertBannerTitle = new Label("All caught up!");
    private final Label alertBannerSub = new Label("No urgent actions required right now");
    private final VBox expectedList = new VBox(10);

    public DashboardView(Consumer<String> navigateTo) {
        this.navigateTo = navigateTo;
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background: #f8fafc; -fx-background-color: #f8fafc; -fx-border-color: transparent;");

        VBox root = new VBox(22);
        root.setPadding(new Insets(24, 28, 32, 28));

        // 1. Greeting Header
        String firstName = Session.displayName() != null ? Session.displayName().split(" ")[0] : "User";
        Label welcome = new Label("Welcome back, " + firstName + "!");
        welcome.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label subtitle = new Label("Here's what's happening with campus lost & found items today.");
        subtitle.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #64748b;");
        VBox greetBox = new VBox(2, welcome, subtitle);

        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));
        Label dateLabel = new Label(todayStr);
        dateLabel.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #64748b; -fx-font-weight: 600;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(greetBox, headerSpacer, dateLabel);
        header.setAlignment(Pos.TOP_LEFT);

        // 2. Four KPI Cards Row
        HBox kpiRow = new HBox(18,
                buildKpiCard("📦", "#dbeafe", "#2563eb", "↗ On track", "#eff6ff", "#2563eb",
                        kpiOpenVal, "Open Found Items", kpiOpenSub, "#2563eb", () -> navigateTo.accept("found")),
                buildKpiCard("📋", "#fef3c7", "#d97706", "⏳ In queue", "#fffbeb", "#d97706",
                        kpiClaimsVal, Session.isStaff() ? "Pending Claims" : "My Claims", kpiClaimsSub, "#d97706",
                        () -> navigateTo.accept(Session.isStaff() ? "moderation" : "my-claims")),
                buildKpiCard("🔍", "#dcfce7", "#16a34a", "↗ Active", "#f0fdf4", "#16a34a",
                        kpiLostVal, "My Lost Reports", kpiLostSub, "#16a34a", () -> navigateTo.accept("my-lost")),
                buildKpiCard("!", "#fee2e2", "#dc2626", "▲ Needs action", "#fef2f2", "#dc2626",
                        kpiAlertVal, "Unread Alerts", kpiAlertSub, "#dc2626", () -> navigateTo.accept("notifications"))
        );

        // 3. Middle Row: Recent Claims Table (Left) + Item Availability Donut & Bars (Right)
        VBox recentClaimsCard = buildRecentClaimsCard();
        VBox availabilityCard = buildAvailabilityCard();
        HBox.setHgrow(recentClaimsCard, Priority.ALWAYS);
        availabilityCard.setPrefWidth(370);
        availabilityCard.setMinWidth(350);
        HBox midRow = new HBox(18, recentClaimsCard, availabilityCard);

        // 4. Bottom Row: Recent Found Items (Left) + Alerts & Quick Follow-ups (Right)
        VBox recentItemsCard = buildRecentItemsCard();
        VBox alertsCard = buildAlertsCard();
        HBox.setHgrow(recentItemsCard, Priority.ALWAYS);
        alertsCard.setPrefWidth(370);
        alertsCard.setMinWidth(350);
        HBox botRow = new HBox(18, recentItemsCard, alertsCard);

        root.getChildren().addAll(header, kpiRow, midRow, botRow);
        setContent(root);

        load();
    }

    private VBox buildKpiCard(String icon, String iconBg, String iconFg,
                              String pillText, String pillBg, String pillFg,
                              Label bigVal, String titleText, Label footerLabel, String footerColor,
                              Runnable onClick) {
        Label ic = new Label(icon);
        ic.setStyle("-fx-background-color: " + iconBg + "; -fx-text-fill: " + iconFg
                + "; -fx-font-size: 16px; -fx-font-weight: bold; -fx-min-width: 38; -fx-min-height: 38;"
                + "-fx-alignment: center; -fx-background-radius: 8;");

        Label pill = new Label(pillText);
        pill.setStyle("-fx-background-color: " + pillBg + "; -fx-text-fill: " + pillFg
                + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 999;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox top = new HBox(ic, sp, pill);
        top.setAlignment(Pos.CENTER_LEFT);

        bigVal.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-padding: 0 0 3 6;");
        HBox metricRow = new HBox(4, bigVal, title);
        metricRow.setAlignment(Pos.BASELINE_LEFT);

        footerLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: " + footerColor + ";");

        VBox card = new VBox(12, top, metricRow, footerLabel);
        card.getStyleClass().add("card");
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> onClick.run());
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox buildRecentClaimsCard() {
        Label title = new Label(Session.isStaff() ? "Recent claim requests" : "My recent claim requests");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label sub = new Label("Review and track incoming ownership claims");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        VBox titles = new VBox(2, title, sub);

        Button viewAll = new Button("View all ↗");
        viewAll.setOnAction(e -> navigateTo.accept(Session.isStaff() ? "moderation" : "my-claims"));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox head = new HBox(titles, sp, viewAll);
        head.setAlignment(Pos.CENTER_LEFT);

        reviewCountHint.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold; -fx-font-size: 12px;");

        recentClaimsTable.setPlaceholder(new Label("No claim requests yet."));
        recentClaimsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recentClaimsTable.setPrefHeight(240);

        if (Session.isStaff()) {
            recentClaimsTable.getColumns().add(avatarCol("CLAIMANT",
                    n -> n.path("claimant").path("name").asText("Student"),
                    n -> "ID: " + n.path("claimant").path("studentId").asText("-"), 170));
            recentClaimsTable.getColumns().add(twoLineCol("ITEM",
                    n -> n.path("item").path("name").asText("-"),
                    n -> n.path("item").path("brand").asText(""), 160));
        } else {
            recentClaimsTable.getColumns().add(Ui.col("PROOF SUMMARY", n -> Ui.t(n, "proofDescription"), 220));
            recentClaimsTable.getColumns().add(Ui.col("STAFF NOTE", n -> {
                String note = Ui.t(n, "reviewNote");
                return note.isEmpty() ? "Awaiting review" : note;
            }, 160));
        }
        recentClaimsTable.getColumns().add(Ui.col("REQUESTED", n -> Ui.when(Ui.t(n, "createdAt")), 110));
        recentClaimsTable.getColumns().add(Ui.statusCol("STATUS", n -> Ui.t(n, "status"), 110));

        VBox card = new VBox(12, head, recentClaimsTable);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildAvailabilityCard() {
        Label title = new Label("Campus item breakdown");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label sub = new Label("Live found inventory distribution");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        VBox titles = new VBox(2, title, sub);

        Button invBtn = new Button("Browse ↗");
        invBtn.setOnAction(e -> navigateTo.accept("found"));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox head = new HBox(titles, sp, invBtn);
        head.setAlignment(Pos.CENTER_LEFT);

        HBox chartArea = new HBox(18, donutCanvas, legendBox);
        chartArea.setAlignment(Pos.CENTER_LEFT);

        Label catHeader = new Label("ITEMS BY CATEGORY");
        catHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-padding: 6 0 0 0;");

        VBox card = new VBox(12, head, chartArea, catHeader, categoryBarsBox);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildRecentItemsCard() {
        Label title = new Label("Recent found reports");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label sub = new Label("Latest items turned in across campus");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        VBox titles = new VBox(2, title, sub);

        Button allBtn = new Button("Found items ↗");
        allBtn.setOnAction(e -> navigateTo.accept("found"));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox head = new HBox(titles, sp, allBtn);
        head.setAlignment(Pos.CENTER_LEFT);

        recentItemsTable.setPlaceholder(new Label("No found items reported yet."));
        recentItemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recentItemsTable.setPrefHeight(230);
        recentItemsTable.getColumns().add(twoLineCol("ITEM",
                n -> Ui.t(n, "name"),
                n -> Ui.t(n, "brand") + " • " + Ui.t(n, "color"), 180));
        recentItemsTable.getColumns().add(Ui.col("CATEGORY", n -> Lookups.categoryName(Ui.t(n, "categoryId")), 120));
        recentItemsTable.getColumns().add(Ui.col("FOUND AT", n -> Ui.t(n, "locationName"), 140));
        recentItemsTable.getColumns().add(Ui.statusCol("STATUS", n -> Ui.t(n, "status"), 100));
        recentItemsTable.getColumns().add(Ui.col("DATE", n -> Ui.t(n, "dateOccurred"), 95));

        recentItemsTable.setRowFactory(tv -> {
            TableRow<JsonNode> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    ItemDetailDialog.open(getScene().getWindow(), row.getItem());
                }
            });
            return row;
        });

        VBox card = new VBox(12, head, recentItemsTable);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildAlertsCard() {
        Label title = new Label("Action items & alerts");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label sub = new Label("Time-sensitive matches and claim updates");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        VBox titles = new VBox(2, title, sub);

        Button revBtn = new Button("Review ↗");
        revBtn.setOnAction(e -> navigateTo.accept("notifications"));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox head = new HBox(titles, sp, revBtn);
        head.setAlignment(Pos.CENTER_LEFT);

        Label warnIcon = new Label("⚠");
        warnIcon.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 18px;");
        alertBannerTitle.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-font-size: 13px;");
        alertBannerSub.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11.5px;");
        VBox bannerText = new VBox(2, alertBannerTitle, alertBannerSub);
        HBox banner = new HBox(12, warnIcon, bannerText);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setStyle("-fx-background-color: #fef2f2; -fx-border-color: #fecaca; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 12;");
        banner.setCursor(javafx.scene.Cursor.HAND);
        banner.setOnMouseClicked(e -> navigateTo.accept("notifications"));

        Label recentHeader = new Label("LATEST NOTIFICATIONS");
        recentHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-padding: 4 0 0 0;");

        VBox card = new VBox(12, head, banner, recentHeader, expectedList);
        card.getStyleClass().add("card");
        return card;
    }

    @Override
    public void reload() { load(); }

    private void load() {
        // 1. Load Open Found Items
        Ui.call(ApiClient.get("/v1/items?page=0&size=50"), r -> {
            if (!r.ok()) return;
            JsonNode root = r.json();
            long total = root.path("totalElements").asLong(0);
            kpiOpenVal.setText(String.format("%,d", total));
            kpiOpenSub.setText("+" + root.path("content").size() + " listed recently");

            List<JsonNode> items = new ArrayList<>();
            root.path("content").forEach(items::add);
            recentItemsTable.getItems().setAll(items.subList(0, Math.min(5, items.size())));
            updateCharts(items, total);
        });

        // 2. Load Claims (Staff moderation queue OR Student's own claims)
        String claimsPath = Session.isStaff() ? "/v1/moderation/claims?status=PENDING" : "/v1/claims/mine";
        Ui.call(ApiClient.get(claimsPath), r -> {
            if (!r.ok()) return;
            List<JsonNode> list = new ArrayList<>();
            r.json().forEach(list::add);
            long pendingCount = list.stream().filter(n -> "PENDING".equals(Ui.t(n, "status"))).count();
            kpiClaimsVal.setText(String.valueOf(list.size()));
            kpiClaimsSub.setText(pendingCount + " awaiting review");
            reviewCountHint.setText(pendingCount + " need review");
            recentClaimsTable.getItems().setAll(list.subList(0, Math.min(5, list.size())));
        });

        // 3. Load User's Lost Reports
        Ui.call(ApiClient.get("/v1/lost-items/mine"), r -> {
            if (!r.ok()) return;
            List<JsonNode> list = new ArrayList<>();
            r.json().forEach(list::add);
            long matched = list.stream().filter(n -> "MATCHED".equals(Ui.t(n, "status"))).count();
            kpiLostVal.setText(String.valueOf(list.size()));
            kpiLostSub.setText(matched > 0 ? matched + " possible match(es)!" : "No matches yet");
        });

        // 4. Load Notifications & Unread Count
        Ui.call(ApiClient.get("/v1/notifications?page=0&size=4"), r -> {
            if (!r.ok()) return;
            List<JsonNode> list = new ArrayList<>();
            r.json().path("content").forEach(list::add);
            long unread = list.stream().filter(n -> !n.path("read").asBoolean(true)).count();
            kpiAlertVal.setText(String.valueOf(unread));
            kpiAlertSub.setText(unread > 0 ? "Requires attention" : "All caught up");

            if (unread > 0) {
                alertBannerTitle.setText(unread + " unread notification(s)");
                alertBannerSub.setText("Click to review match suggestions and claim updates");
            } else {
                alertBannerTitle.setText("Zero pending alerts");
                alertBannerSub.setText("All notifications have been reviewed");
            }

            expectedList.getChildren().clear();
            if (list.isEmpty()) {
                Label none = new Label("No notifications yet.");
                none.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
                expectedList.getChildren().add(none);
            } else {
                for (JsonNode n : list) {
                    String msg = n.path("payload").path("message").asText("Notification");
                    String when = Ui.when(Ui.t(n, "createdAt"));
                    Label mLbl = new Label(msg);
                    mLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #1e293b; -fx-font-size: 12px;");
                    mLbl.setWrapText(true);
                    Label wLbl = new Label(when);
                    wLbl.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold; -fx-font-size: 11px;");
                    VBox itemBox = new VBox(2, mLbl, wLbl);
                    itemBox.setStyle("-fx-padding: 6 0 6 0; -fx-border-color: transparent transparent #f1f5f9 transparent;");
                    expectedList.getChildren().add(itemBox);
                }
            }
        });
    }

    private void updateCharts(List<JsonNode> items, long totalOpen) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (JsonNode item : items) {
            String cat = Lookups.categoryName(Ui.t(item, "categoryId"));
            counts.put(cat, counts.getOrDefault(cat, 0) + 1);
        }

        // Draw Donut Chart
        GraphicsContext gc = donutCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, 130, 130);
        gc.setLineWidth(16);
        gc.setLineCap(StrokeLineCap.BUTT);

        Color[] palette = {
                Color.web("#16a34a"), Color.web("#2563eb"),
                Color.web("#f59e0b"), Color.web("#dc2626"), Color.web("#8b5cf6")
        };

        legendBox.getChildren().clear();
        categoryBarsBox.getChildren().clear();

        int totalSample = Math.max(1, items.size());
        if (items.isEmpty()) {
            gc.setStroke(Color.web("#e2e8f0"));
            gc.strokeArc(15, 15, 100, 100, 0, 360, ArcType.OPEN);
        } else {
            double startAngle = 90;
            int idx = 0;
            for (var entry : counts.entrySet()) {
                double sweep = (entry.getValue() * 360.0) / totalSample;
                gc.setStroke(palette[idx % palette.length]);
                gc.strokeArc(15, 15, 100, 100, startAngle, -sweep, ArcType.OPEN);
                startAngle -= sweep;
                idx++;
            }
        }

        // Center text inside donut
        gc.setFill(Color.web("#0f172a"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        String centerNum = items.isEmpty() ? "0%" : "100%";
        gc.fillText(centerNum, items.isEmpty() ? 50 : 38, 64);
        gc.setFill(Color.web("#64748b"));
        gc.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 9));
        gc.fillText("OPEN ITEMS", 38, 80);

        // Populate Legend and Category Bars
        int idx = 0;
        String[] hexPalette = {"#16a34a", "#2563eb", "#f59e0b", "#dc2626", "#8b5cf6"};
        for (var entry : counts.entrySet()) {
            if (idx >= 4) break;
            String colorHex = hexPalette[idx % hexPalette.length];

            // Legend row
            Label dot = new Label("● " + entry.getKey());
            dot.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 12px; -fx-font-weight: 600;");
            Label val = new Label(String.valueOf(entry.getValue()));
            val.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            HBox legRow = new HBox(8, dot, sp, val);
            legRow.setPrefWidth(170);
            legendBox.getChildren().add(legRow);

            // Progress bar row
            Label cName = new Label(entry.getKey());
            cName.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
            Label cCount = new Label(entry.getValue() + " / " + items.size());
            cCount.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            Region sp2 = new Region();
            HBox.setHgrow(sp2, Priority.ALWAYS);
            HBox barLabelRow = new HBox(cName, sp2, cCount);

            ProgressBar pb = new ProgressBar((double) entry.getValue() / totalSample);
            pb.setMaxWidth(Double.MAX_VALUE);
            pb.setPrefHeight(7);
            pb.setStyle("-fx-accent: " + colorHex + ";");

            categoryBarsBox.getChildren().add(new VBox(4, barLabelRow, pb));
            idx++;
        }

        if (counts.isEmpty()) {
            Label empty = new Label("Report a found item to see category metrics.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            categoryBarsBox.getChildren().add(empty);
        }
    }

    // ---- Custom Table Cell Helpers for CEBIMS Look ----

    private static TableColumn<JsonNode, String> avatarCol(String title,
            java.util.function.Function<JsonNode, String> primary,
            java.util.function.Function<JsonNode, String> secondary, double width) {
        TableColumn<JsonNode, String> c = Ui.col(title, primary, width);
        c.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); setText(null); return;
                }
                JsonNode row = getTableRow().getItem();
                String name = primary.apply(row);
                String sub = secondary.apply(row);
                String initials = initials(name);

                Label av = new Label(initials);
                av.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-weight: bold;"
                        + "-fx-font-size: 11px; -fx-min-width: 30; -fx-min-height: 30; -fx-alignment: center; -fx-background-radius: 999;");
                Label l1 = new Label(name);
                l1.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a; -fx-font-size: 12.5px;");
                Label l2 = new Label(sub);
                l2.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                HBox box = new HBox(10, av, new VBox(0, l1, l2));
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
        return c;
    }

    private static TableColumn<JsonNode, String> twoLineCol(String title,
            java.util.function.Function<JsonNode, String> primary,
            java.util.function.Function<JsonNode, String> secondary, double width) {
        TableColumn<JsonNode, String> c = Ui.col(title, primary, width);
        c.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); setText(null); return;
                }
                JsonNode row = getTableRow().getItem();
                Label l1 = new Label(primary.apply(row));
                l1.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a; -fx-font-size: 12.5px;");
                Label l2 = new Label(secondary.apply(row));
                l2.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                VBox box = new VBox(0, l1, l2);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
        return c;
    }

    private static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}