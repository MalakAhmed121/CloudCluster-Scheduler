package cs241;

import cs241.model.GanttEntry;
import cs241.model.Process;
import cs241.model.SimulationResult;
import cs241.sjf.PreemptiveSJF;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.control.ComboBox;
import cs241.acs.ACSScheduler;
import java.util.ArrayList;
import java.util.List;

public class App extends Application {
    private final TextArea processInput = new TextArea("P1,0,8\nP2,1,4\nP3,2,2\nP4,3,1");
    private final TextField contextSwitchInput = new TextField("1");

    private final ComboBox<String> algorithmInput = new ComboBox<>();

    // ACS Input Fields
    private final TextField acsIdInput = new TextField();
    private final TextField acsArrivalInput = new TextField();
    private final TextField acsBurstInput = new TextField();
    private final TextField acsPriorityInput = new TextField();

    // ACS Processes Table
    private final TableView<ProcessRow> acsProcessesTable = new TableView<>();

    // Table to display ACS demotion events
    private final TableView<String> demotionTable = new TableView<>();

    // Store ACS processes separately
    private final List<Process> acsProcesses = new ArrayList<>();

    // Input panels
    private VBox sjfInputCard;
    private VBox acsInputCard;

    // ACS Demotion Matrix card
    private VBox demotionCard;

    private final Label message = new Label("Enter one process per line: ID, arrival time, burst time.");
    private final TableView<ProcessRow> resultsTable = new TableView<>();
    // ACS Time Quantum
    private final TextField timeQuantumInput = new TextField("4");

    private final HBox timeline = new HBox(5);
    private final Label chartSummary = new Label("Run the simulation to view its execution timeline.");

    @Override
    public void start(Stage stage) {

        algorithmInput.getItems().addAll(
                "Preemptive SJF",
                "ACS");

        algorithmInput.setValue("Preemptive SJF");

        Label title = new Label("Cloud-Cluster CPU Scheduler");
        title.setStyle("-fx-font-size: 25px; -fx-font-weight: bold; -fx-text-fill: #12233f;");

        Label subtitle = new Label("Shortest Remaining Time First with arrival-aware preemption and context switching");
        subtitle.setStyle("-fx-text-fill: #607089; -fx-font-size: 13px;");

        processInput.setPrefRowCount(5);
        processInput.setPromptText("P1,0,8\nP2,1,4");
        processInput.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");

        contextSwitchInput.setPrefColumnCount(5);

        Button runButton = new Button("Run Simulation");
        runButton.setDefaultButton(true);
        runButton.setStyle(
                "-fx-background-color: #2463eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 9 16;");

        runButton.setOnAction(event -> runSimulation());

        HBox contextRow = new HBox(
                10,
                new Label("Algorithm:"),
                algorithmInput,
                new Label("Context switch time:"),
                contextSwitchInput,
                new Label("time units"),
                runButton);

        contextRow.setAlignment(Pos.CENTER_LEFT);

        // Prepare ACS table
        configureAcsProcessesTable();

        // Create ACS input panel
        acsInputCard = createAcsInputCard();

        // Area that will switch between SJF and ACS inputs
        VBox processInputArea = new VBox(10, processInput);

        // Main input card
        VBox inputCard = card("Processes", processInputArea, contextRow, message);

        // Configure results table
        configureResultsTable();

        timeline.setAlignment(Pos.CENTER_LEFT);
        timeline.setPadding(new Insets(14));

        ScrollPane chartScroll = new ScrollPane(timeline);
        chartScroll.setFitToHeight(true);
        chartScroll.setPrefViewportHeight(105);
        chartScroll.setStyle("-fx-background: white; -fx-background-color: white;");

        VBox ganttCard = card("Gantt Chart", chartSummary, chartScroll);
        VBox resultCard = card("SJF Results", resultsTable);

        // Configure the ACS Demotion Table
        configureDemotionTable();

        // Create the ACS Demotion Matrix card
        demotionCard = card("ACS Demotion Matrix", demotionTable);

        // Hide the card initially because SJF is selected
        demotionCard.setVisible(false);
        demotionCard.setManaged(false);

        // Switch input panel and demotion card when algorithm changes
        algorithmInput.valueProperty().addListener((observable, oldValue, newValue) -> {

            if ("ACS".equals(newValue)) {
                processInputArea.getChildren().setAll(acsInputCard);
            } else {
                processInputArea.getChildren().setAll(processInput);
            }

            // Show demotion matrix only for ACS
            boolean showDemotion = "ACS".equals(newValue);

            demotionCard.setVisible(showDemotion);
            demotionCard.setManaged(showDemotion);
        });

        // Add all cards to the main window
        VBox root = new VBox(
                16,
                title,
                subtitle,
                inputCard,
                ganttCard,
                resultCard,
                demotionCard);

        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f3f6fb;");

        ScrollPane page = new ScrollPane(root);
        page.setFitToWidth(true);

        stage.setScene(new Scene(page, 950, 760));
        stage.setTitle("Cloud-Cluster CPU Scheduling");
        stage.show();

        runSimulation();
    }

    private VBox createAcsInputCard() {

        Label heading = new Label("Add Process");

        HBox firstRow = new HBox(
                10,
                new VBox(5, new Label("Process ID"), acsIdInput),
                new VBox(5, new Label("Arrival Time"), acsArrivalInput));

        HBox secondRow = new HBox(
                10,
                new VBox(5, new Label("Burst Time"), acsBurstInput),
                new VBox(5, new Label("Priority"), acsPriorityInput));
        // ACS Time Quantum input
        HBox quantumRow = new HBox(
                10,
                new Label("Time Quantum:"),
                timeQuantumInput);

        Button addButton = new Button("Add Process");
        Button removeButton = new Button("Remove Selected Process");

        // Handle Add Process button click
        addButton.setOnAction(event -> {
            try {
                // Read process data from input fields
                String id = acsIdInput.getText().trim();

                // Check that ID is not empty
                if (id.isEmpty()) {
                    throw new IllegalArgumentException("Enter Process ID.");
                }
                // Check if the process ID already exists
                boolean idExists = acsProcesses.stream()
                        .anyMatch(p -> p.id.equalsIgnoreCase(id));

                if (idExists) {
                    throw new IllegalArgumentException("Process ID already exists.");
                }
                // Convert input values to integers
                int arrival = Integer.parseInt(acsArrivalInput.getText().trim());
                int burst = Integer.parseInt(acsBurstInput.getText().trim());
                int priority = Integer.parseInt(acsPriorityInput.getText().trim());

                // Validate process values
                if (arrival < 0 || burst <= 0 || priority <= 0) {
                    throw new IllegalArgumentException(
                            "Arrival must be >= 0, Burst and Priority must be > 0.");
                }

                // Create a new process object
                Process process = new Process(id, arrival, burst, priority);

                // Store process in ACS list
                acsProcesses.add(process);

                // Display process in ACS table
                acsProcessesTable.getItems().add(new ProcessRow(process));

                // Clear input fields after adding
                acsIdInput.clear();
                acsArrivalInput.clear();
                acsBurstInput.clear();
                acsPriorityInput.clear();

                // Show success message
                message.setText("Process " + id + " added successfully.");
                message.setStyle("-fx-text-fill: #18794e;");

            } catch (NumberFormatException ex) {
                // Handle invalid number input
                message.setText("Arrival, Burst and Priority must be whole numbers.");
                message.setStyle("-fx-text-fill: #c0392b;");

            } catch (IllegalArgumentException ex) {
                // Handle validation errors
                message.setText(ex.getMessage());
                message.setStyle("-fx-text-fill: #c0392b;");
            }
        });
        // Handle Remove Process button click
        removeButton.setOnAction(event -> {

            // Get the selected process from the table
            ProcessRow selectedRow = acsProcessesTable.getSelectionModel().getSelectedItem();

            // Check if a process is selected
            if (selectedRow == null) {
                message.setText("Please select a process to remove.");
                message.setStyle("-fx-text-fill: #c0392b;");
                return;
            }

            // Remove the process from the ACS list
            acsProcesses.removeIf(p -> p.id.equalsIgnoreCase(selectedRow.getId()));

            // Remove the process from the table
            acsProcessesTable.getItems().remove(selectedRow);

            // Show success message
            message.setText("Process " + selectedRow.getId() + " removed successfully.");
            message.setStyle("-fx-text-fill: #18794e;");
        });
        VBox inputFields = new VBox(
                12,
                heading,
                firstRow,
                secondRow,
                quantumRow,
                addButton,
                removeButton,
                acsProcessesTable);

        inputFields.setPadding(new Insets(16));

        return inputFields;
    }

    private void configureAcsProcessesTable() {
        acsProcessesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addAcsColumn("Process ID", "id");
        addAcsColumn("Arrival", "arrival");
        addAcsColumn("Burst", "burst");
        addAcsColumn("Priority", "priority");

        acsProcessesTable.setPrefHeight(180);
    }

    private VBox card(String heading, javafx.scene.Node... content) {
        Label label = new Label(heading);
        label.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1d3557;");
        VBox box = new VBox(10);
        box.getChildren().add(label);
        box.getChildren().addAll(content);
        box.setPadding(new Insets(16));
        box.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e1e8f1; -fx-border-radius: 10;");
        return box;
    }

    private void configureResultsTable() {
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        addColumn("Process", "id", 100);
        addColumn("Arrival", "arrival", 80);
        addColumn("Burst", "burst", 80);
        addColumn("First start", "firstStart", 100);
        addColumn("Finish", "finish", 80);
        addColumn("Waiting", "waiting", 80);
        addColumn("Turnaround", "turnaround", 100);
        addColumn("Response", "response", 90);
        resultsTable.setPrefHeight(220);
    }

    private void addColumn(String heading, String property, double width) {
        TableColumn<ProcessRow, Object> column = new TableColumn<>(heading);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        resultsTable.getColumns().add(column);
    }

    // to add process to ACS table
    private void addAcsColumn(String heading, String property) {
        TableColumn<ProcessRow, Object> column = new TableColumn<>(heading);

        column.setCellValueFactory(new PropertyValueFactory<>(property));

        acsProcessesTable.getColumns().add(column);
    }

    private void runSimulation() {
        try {
            // Read context switch time
            int contextSwitch = Integer.parseInt(contextSwitchInput.getText().trim());

            SimulationResult result;
            List<Process> input;

            // Check selected algorithm
            if ("ACS".equals(algorithmInput.getValue())) {

                // Check if ACS processes exist
                if (acsProcesses.isEmpty()) {
                    throw new IllegalArgumentException("Add at least one process.");
                }

                // Read Time Quantum
                int quantum = Integer.parseInt(timeQuantumInput.getText().trim());

                // Use ACS processes
                input = acsProcesses;

                // Run ACS Scheduler
                result = new ACSScheduler().run(input, quantum, contextSwitch);

            } else {

                // Read SJF processes from TextArea
                input = parseProcesses(processInput.getText());

                // Run Preemptive SJF
                result = new PreemptiveSJF().run(input, 0, contextSwitch);
            }

            // Display simulation results
            resultsTable.getItems().setAll(
                    result.processes.stream().map(ProcessRow::new).toList());

            // Display ACS demotion events
            if ("ACS".equals(algorithmInput.getValue())) {
                demotionTable.getItems().setAll(result.demotionMatrix);
            } else {
                demotionTable.getItems().clear();
            }

            // Display Gantt Chart
            renderTimeline(result.gantt);

            // Calculate average metrics
            double avgWait = result.processes.stream()
                    .mapToInt(Process::waiting).average().orElse(0);

            double avgTurnaround = result.processes.stream()
                    .mapToInt(Process::turnaround).average().orElse(0);

            double avgResponse = result.processes.stream()
                    .mapToInt(Process::response).average().orElse(0);

            // Display average metrics
            chartSummary.setText(String.format(
                    "Average waiting: %.2f    Average turnaround: %.2f    Average response: %.2f time units",
                    avgWait, avgTurnaround, avgResponse));

            message.setText("Simulation completed for " + input.size() + " process(es).");
            message.setStyle("-fx-text-fill: #18794e;");

        } catch (RuntimeException ex) {
            message.setText(
                    ex.getMessage() == null ? "Please check the input values." : ex.getMessage());
            message.setStyle("-fx-text-fill: #c0392b;");
        }
    }

    private List<Process> parseProcesses(String text) {
        List<Process> processes = new ArrayList<>();
        int lineNumber = 0;
        for (String line : text.split("\\R")) {
            lineNumber++;
            if (line.isBlank())
                continue;
            String[] fields = line.split(",", -1);
            if (fields.length != 3 || fields[0].trim().isEmpty()) {
                throw new IllegalArgumentException("Line " + lineNumber + ": use ID, arrival, burst.");
            }
            try {
                processes.add(new Process(fields[0].trim(), Integer.parseInt(fields[1].trim()),
                        Integer.parseInt(fields[2].trim()), 0));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Line " + lineNumber + ": arrival and burst must be whole numbers.");
            }
        }
        if (processes.isEmpty())
            throw new IllegalArgumentException("Enter at least one process.");
        return processes;
    }

    private void renderTimeline(List<GanttEntry> entries) {
        timeline.getChildren().clear();
        for (GanttEntry entry : entries) {
            VBox block = new VBox(5);
            block.setAlignment(Pos.CENTER);
            block.setPadding(new Insets(10, 8, 8, 8));
            block.setMinWidth(Math.max(70, Math.min(180, (entry.end - entry.start) * 28.0)));
            block.setStyle("-fx-background-color: " + colorFor(entry.label) + "; -fx-background-radius: 6;");
            Label process = new Label(entry.label);
            process.setStyle("-fx-font-weight: bold; -fx-text-fill: " +
                    (entry.label.equals(PreemptiveSJF.CONTEXT_SWITCH) || entry.label.equals(PreemptiveSJF.IDLE)
                            ? "#475569"
                            : "white")
                    + ";");
            Label interval = new Label(entry.start + " - " + entry.end);
            interval.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                    (entry.label.equals(PreemptiveSJF.CONTEXT_SWITCH) || entry.label.equals(PreemptiveSJF.IDLE)
                            ? "#475569"
                            : "#eaf0ff")
                    + ";");
            block.getChildren().addAll(process, interval);
            timeline.getChildren().add(block);
        }
    }

    private String colorFor(String label) {
        if (label.equals(PreemptiveSJF.CONTEXT_SWITCH))
            return "#e9eef5";
        if (label.equals(PreemptiveSJF.IDLE))
            return "#f3f4f6";
        int hue = Math.floorMod(label.hashCode(), 360);
        return Color.hsb(hue, 0.62, 0.72).toString().replace("0x", "#").substring(0, 7);
    }

    public static class ProcessRow {
        private final String id;
        private final int arrival, burst, firstStart, finish, waiting, turnaround, response, priority;

        ProcessRow(Process p) {
            id = p.id;
            arrival = p.arrival;
            burst = p.burst;
            firstStart = p.firstStart;
            finish = p.finish;
            waiting = p.waiting();
            turnaround = p.turnaround();
            response = p.response();
            priority = p.priority;
        }

        public String getId() {
            return id;
        }

        public int getArrival() {
            return arrival;
        }

        public int getBurst() {
            return burst;
        }

        public int getFirstStart() {
            return firstStart;
        }

        public int getFinish() {
            return finish;
        }

        public int getWaiting() {
            return waiting;
        }

        public int getTurnaround() {
            return turnaround;
        }

        public int getResponse() {
            return response;
        }

        public int getPriority() {
            return priority;
        }
    }

    // Configure the ACS Demotion Table
    private void configureDemotionTable() {

        // Create a column to display demotion details
        TableColumn<String, String> detailsColumn = new TableColumn<>("Demotion Details");

        // Get the event text for each row
        detailsColumn.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(data.getValue()));

        // Add the column to the table
        demotionTable.getColumns().setAll(detailsColumn);

        // Make the column fill the available table width
        demotionTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
