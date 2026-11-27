package cs241.ui;

import cs241.model.GanttEntry;
import cs241.model.SimulationResult;
import cs241.model.StepSnapshot;
import cs241.model.StepSnapshot.Tier1Item;
import cs241.model.StepSnapshot.Tier2Item;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Collections;
import java.util.List;

public class VisualizationPanel extends VBox {
    private static final String CS_TOKEN = "CS";
    private static final String IDLE_TOKEN = "IDLE";

    // part 1: Gantt Chart
    private final HBox timelineBox = new HBox(5);
    private final Label ganttSummaryLabel = new Label("Run a simulation to view the Gantt chart.");

    // part 2: Step Controls
    private final VBox stepControlsCard;
    private final Button prevStepButton = new Button("Previous Step");
    private final Button nextStepButton = new Button("Next Step");
    private final Slider stepSlider = new Slider(0, 0, 0);
    private final Label stepCounterLabel = new Label("Step 0 of 0");
    private final Label runningProcessLabel = new Label("Active: -");

    // part 3: Tier 1, Tier 2, & Aging Panels
    private final VBox acsPanelsContainer = new VBox(12);
    private final TableView<Tier1Item> tier1Table = new TableView<>();
    private final TableView<Tier2Item> tier2Table = new TableView<>();
    private final ListView<String> agingListView = new ListView<>();

    private List<StepSnapshot> steps = Collections.emptyList();
    private boolean syncingSlider = false;

    public VisualizationPanel() {
        setSpacing(14);

        // 1)  Gantt Chart
        VBox ganttCard = createCard("Gantt Chart", ganttSummaryLabel, buildTimelineScrollPane());

        // 2)  Step Controls
        stepControlsCard = buildStepControlsCard();

        // 3)  Tier 1, Tier 2, & Aging Info
        configureTier1Table();
        configureTier2Table();
        agingListView.setPrefHeight(135);
        agingListView.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 6;");

        VBox tier1Card = createCard("Tier 1: Round Robin Queue", tier1Table);
        VBox tier2Card = createCard("Tier 2: Priority Queue", tier2Table);
        HBox queuesRow = new HBox(12, tier1Card, tier2Card);
        HBox.setHgrow(tier1Card, Priority.ALWAYS);
        HBox.setHgrow(tier2Card, Priority.ALWAYS);

        VBox agingCard = createCard("Aging Info", agingListView);
        acsPanelsContainer.getChildren().addAll(stepControlsCard, queuesRow, agingCard);

        setAcsMode(false);

        getChildren().addAll(ganttCard, acsPanelsContainer);
    }


    public void show(SimulationResult result, boolean isAcs) {
        if (result == null) {
            renderGantt(Collections.emptyList());
            setAcsMode(false);
            return;
        }

        renderGantt(result.gantt == null ? Collections.emptyList() : result.gantt);
        boolean hasSteps = isAcs && result.steps != null && !result.steps.isEmpty();
        if (hasSteps) {
            loadSteps(result.steps);
        } else {
            setAcsMode(false);
        }
    }


    public void loadSteps(List<StepSnapshot> stepSnapshots) {
        if (stepSnapshots == null || stepSnapshots.isEmpty()) {
            this.steps = Collections.emptyList();
            setAcsMode(false);
            return;
        }

        this.steps = stepSnapshots;
        setAcsMode(true);

        syncingSlider = true;
        stepSlider.setMin(0);
        stepSlider.setMax(this.steps.size() - 1);
        stepSlider.setValue(0);
        stepSlider.setMajorTickUnit(1);
        stepSlider.setMinorTickCount(0);
        stepSlider.setSnapToTicks(true);
        syncingSlider = false;

        renderStepAt(0);
    }

    /**
     *part 4  : show or disapper ACS
     */
    public void setAcsMode(boolean enabled) {
        acsPanelsContainer.setVisible(enabled);
        acsPanelsContainer.setManaged(enabled);
        stepControlsCard.setVisible(enabled);
        stepControlsCard.setManaged(enabled);
    }

    // ==================== part 1: Gantt Chart ====================

    private ScrollPane buildTimelineScrollPane() {
        timelineBox.setAlignment(Pos.CENTER_LEFT);
        timelineBox.setPadding(new Insets(14));

        ScrollPane scrollPane = new ScrollPane(timelineBox);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefViewportHeight(110);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white; -fx-border-color: transparent;");
        return scrollPane;
    }

    private void renderGantt(List<GanttEntry> entries) {
        timelineBox.getChildren().clear();

        if (entries.isEmpty()) {
            ganttSummaryLabel.setText("No timeline intervals to display.");
            return;
        }

        long csCount = entries.stream().filter(e -> isContextSwitch(e.label)).count();
        ganttSummaryLabel.setText(csCount > 0
                ? "Timeline includes " + csCount + " Context Switch (CS) interval(s)."
                : "Execution timeline across time intervals.");

        for (GanttEntry entry : entries) {
            boolean cs = isContextSwitch(entry.label);
            boolean idle = isIdle(entry.label);
            int duration = Math.max(1, entry.end - entry.start);

            VBox block = new VBox(4);
            block.setAlignment(Pos.CENTER);
            block.setPadding(new Insets(10, 8, 8, 8));
            block.setMinWidth(Math.max(68, Math.min(190, duration * 28.0)));

            String bgColor = resolveBlockColor(entry.label, cs, idle);
            String borderStyle = cs
                    ? "-fx-border-color: #94a3b8; -fx-border-width: 1.5; -fx-border-style: dashed; -fx-border-radius: 6;"
                    : "";

            block.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 6;" + borderStyle);

            String primaryTextColor = (cs || idle) ? "#334155" : "white";
            String secondaryTextColor = (cs || idle) ? "#475569" : "#f1f5f9";

            Label title = new Label(cs ? CS_TOKEN : entry.label);
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + primaryTextColor + ";");

            Label interval = new Label(entry.start + " - " + entry.end);
            interval.setStyle("-fx-font-size: 11px; -fx-text-fill: " + secondaryTextColor + ";");

            block.getChildren().addAll(title, interval);
            timelineBox.getChildren().add(block);
        }
    }

    // ==================== part 2: Step Controls ====================

    private VBox buildStepControlsCard() {
        styleNavButton(prevStepButton);
        styleNavButton(nextStepButton);

        prevStepButton.setOnAction(e -> {
            int current = (int) Math.round(stepSlider.getValue());
            if (current > 0) {
                stepSlider.setValue(current - 1);
            }
        });

        nextStepButton.setOnAction(e -> {
            int current = (int) Math.round(stepSlider.getValue());
            if (current < steps.size() - 1) {
                stepSlider.setValue(current + 1);
            }
        });

        stepSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!syncingSlider && !steps.isEmpty()) {
                renderStepAt((int) Math.round(newVal.doubleValue()));
            }
        });

        stepCounterLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1d3557;");
        runningProcessLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2463eb;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controlsRow = new HBox(10, prevStepButton, nextStepButton, stepSlider, stepCounterLabel, spacer, runningProcessLabel);
        controlsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(stepSlider, Priority.ALWAYS);

        return createCard("Step Navigation", controlsRow);
    }

    private void renderStepAt(int index) {
        if (index < 0 || index >= steps.size()) return;

        StepSnapshot snapshot = steps.get(index);
        stepCounterLabel.setText("Step " + (index + 1) + " / " + steps.size() + " (t = " + snapshot.timeStep + ")");

        String active = (snapshot.runningProcessId == null || snapshot.runningProcessId.isBlank())
                ? "None"
                : snapshot.runningProcessId;
        runningProcessLabel.setText("Running: " + active);

        prevStepButton.setDisable(index == 0);
        nextStepButton.setDisable(index == steps.size() - 1);

        tier1Table.getItems().setAll(snapshot.tier1Queue == null ? List.of() : snapshot.tier1Queue);
        tier2Table.getItems().setAll(snapshot.tier2Queue == null ? List.of() : snapshot.tier2Queue);
        agingListView.getItems().setAll(snapshot.agingEvents == null ? List.of() : snapshot.agingEvents);
    }

    // ==================== part 3: Tier 1, Tier 2, Aging ====================

    private void configureTier1Table() {
        tier1Table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tier1Table.setPrefHeight(175);

        TableColumn<Tier1Item, String> idCol = new TableColumn<>("Process ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().processId()));

        TableColumn<Tier1Item, Number> burstCol = new TableColumn<>("Remaining Burst");
        burstCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().remainingBurst()));

        tier1Table.getColumns().addAll(idCol, burstCol);
    }

    private void configureTier2Table() {
        tier2Table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tier2Table.setPrefHeight(175);

        TableColumn<Tier2Item, String> idCol = new TableColumn<>("Process ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().processId()));

        TableColumn<Tier2Item, Number> priorityCol = new TableColumn<>("Effective Priority");
        priorityCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().effectivePriority()));

        TableColumn<Tier2Item, Number> remainingCol = new TableColumn<>("Remaining Time");
        remainingCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().remainingTime()));

        TableColumn<Tier2Item, String> stateCol = new TableColumn<>("Status");
        stateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().running() ? "RUNNING" : "Ready"));
        stateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("RUNNING".equals(item)
                            ? "-fx-font-weight: bold; -fx-text-fill: #15803d;"
                            : "-fx-text-fill: #475569;");
                }
            }
        });

        tier2Table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Tier2Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.running()) {
                    setStyle("-fx-background-color: #dcfce7;");
                } else {
                    setStyle("");
                }
            }
        });

        tier2Table.getColumns().addAll(idCol, priorityCol, remainingCol, stateCol);
    }

    // ====================helping tools ====================

    private VBox createCard(String heading, javafx.scene.Node... children) {
        Label headingLabel = new Label(heading);
        headingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1d3557;");

        VBox card = new VBox(10);
        card.getChildren().add(headingLabel);
        card.getChildren().addAll(children);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: #e1e8f1; -fx-border-radius: 10;");
        return card;
    }

    private void styleNavButton(Button button) {
        button.setStyle("-fx-background-color: #edf2f7; -fx-text-fill: #1d3557; "
                + "-fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
    }

    private boolean isContextSwitch(String label) {
        if (label == null) return false;
        String normalized = label.trim().toUpperCase();
        return normalized.equals(CS_TOKEN) || normalized.contains("CONTEXT SWITCH");
    }

    private boolean isIdle(String label) {
        if (label == null) return false;
        return label.trim().equalsIgnoreCase(IDLE_TOKEN);
    }

    private String resolveBlockColor(String label, boolean cs, boolean idle) {
        if (cs) return "#e2e8f0";
        if (idle) return "#f1f5f9";
        int hue = Math.floorMod(label.hashCode(), 360);
        return Color.hsb(hue, 0.62, 0.72).toString().replace("0x", "#").substring(0, 7);
    }
    public void setSummary(String summary) {
        if (summary != null && !summary.isBlank()) {
            ganttSummaryLabel.setText(summary);
        }
    }
}
