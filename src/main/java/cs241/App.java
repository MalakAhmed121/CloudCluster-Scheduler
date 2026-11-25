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

import java.util.ArrayList;
import java.util.List;

public class App extends Application {
    private final TextArea processInput = new TextArea("P1,0,8\nP2,1,4\nP3,2,2\nP4,3,1");
    private final TextField contextSwitchInput = new TextField("1");
    private final Label message = new Label("Enter one process per line: ID, arrival time, burst time.");
    private final TableView<ProcessRow> resultsTable = new TableView<>();
    private final HBox timeline = new HBox(5);
    private final Label chartSummary = new Label("Run the simulation to view its execution timeline.");

    @Override
    public void start(Stage stage) {
        Label title = new Label("Preemptive SJF Scheduler");
        title.setStyle("-fx-font-size: 25px; -fx-font-weight: bold; -fx-text-fill: #12233f;");
        Label subtitle = new Label("Shortest Remaining Time First with arrival-aware preemption and context switching");
        subtitle.setStyle("-fx-text-fill: #607089; -fx-font-size: 13px;");

        processInput.setPrefRowCount(5);
        processInput.setPromptText("P1,0,8\nP2,1,4");
        processInput.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");
        contextSwitchInput.setPrefColumnCount(5);
        Button runButton = new Button("Run Preemptive SJF");
        runButton.setDefaultButton(true);
        runButton.setStyle("-fx-background-color: #2463eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 9 16;");
        runButton.setOnAction(event -> runSimulation());

        HBox contextRow = new HBox(10, new Label("Context switch time:"), contextSwitchInput, new Label("time units"), runButton);
        contextRow.setAlignment(Pos.CENTER_LEFT);
        VBox inputCard = card("Processes", processInput, contextRow, message);

        configureResultsTable();
        timeline.setAlignment(Pos.CENTER_LEFT);
        timeline.setPadding(new Insets(14));
        ScrollPane chartScroll = new ScrollPane(timeline);
        chartScroll.setFitToHeight(true);
        chartScroll.setPrefViewportHeight(105);
        chartScroll.setStyle("-fx-background: white; -fx-background-color: white;");
        VBox ganttCard = card("Gantt Chart", chartSummary, chartScroll);
        VBox resultCard = card("SJF Results", resultsTable);

        VBox root = new VBox(16, title, subtitle, inputCard, ganttCard, resultCard);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f3f6fb;");
        ScrollPane page = new ScrollPane(root);
        page.setFitToWidth(true);
        stage.setScene(new Scene(page, 950, 760));
        stage.setTitle("Cloud-Cluster CPU Scheduling");
        stage.show();
        runSimulation();
    }

    private VBox card(String heading, javafx.scene.Node... content) {
        Label label = new Label(heading);
        label.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1d3557;");
        VBox box = new VBox(10);
        box.getChildren().add(label);
        box.getChildren().addAll(content);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e1e8f1; -fx-border-radius: 10;");
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

    private void runSimulation() {
        try {
            int contextSwitch = Integer.parseInt(contextSwitchInput.getText().trim());
            List<Process> input = parseProcesses(processInput.getText());
            SimulationResult result = new PreemptiveSJF().run(input, 0, contextSwitch);
            resultsTable.getItems().setAll(result.processes.stream().map(ProcessRow::new).toList());
            renderTimeline(result.gantt);
            double avgWait = result.processes.stream().mapToInt(Process::waiting).average().orElse(0);
            double avgTurnaround = result.processes.stream().mapToInt(Process::turnaround).average().orElse(0);
            double avgResponse = result.processes.stream().mapToInt(Process::response).average().orElse(0);
            chartSummary.setText(String.format("Average waiting: %.2f    Average turnaround: %.2f    Average response: %.2f time units",
                    avgWait, avgTurnaround, avgResponse));
            message.setText("Simulation completed for " + input.size() + " process(es).");
            message.setStyle("-fx-text-fill: #18794e;");
        } catch (RuntimeException ex) {
            message.setText(ex.getMessage() == null ? "Please check the input values." : ex.getMessage());
            message.setStyle("-fx-text-fill: #c0392b;");
        }
    }

    private List<Process> parseProcesses(String text) {
        List<Process> processes = new ArrayList<>();
        int lineNumber = 0;
        for (String line : text.split("\\R")) {
            lineNumber++;
            if (line.isBlank()) continue;
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
        if (processes.isEmpty()) throw new IllegalArgumentException("Enter at least one process.");
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
                    (entry.label.equals(PreemptiveSJF.CONTEXT_SWITCH) || entry.label.equals(PreemptiveSJF.IDLE) ? "#475569" : "white") + ";");
            Label interval = new Label(entry.start + " - " + entry.end);
            interval.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                    (entry.label.equals(PreemptiveSJF.CONTEXT_SWITCH) || entry.label.equals(PreemptiveSJF.IDLE) ? "#475569" : "#eaf0ff") + ";");
            block.getChildren().addAll(process, interval);
            timeline.getChildren().add(block);
        }
    }

    private String colorFor(String label) {
        if (label.equals(PreemptiveSJF.CONTEXT_SWITCH)) return "#e9eef5";
        if (label.equals(PreemptiveSJF.IDLE)) return "#f3f4f6";
        int hue = Math.floorMod(label.hashCode(), 360);
        return Color.hsb(hue, 0.62, 0.72).toString().replace("0x", "#").substring(0, 7);
    }

    public static class ProcessRow {
        private final String id;
        private final int arrival, burst, firstStart, finish, waiting, turnaround, response;
        ProcessRow(Process p) {
            id = p.id; arrival = p.arrival; burst = p.burst; firstStart = p.firstStart;
            finish = p.finish; waiting = p.waiting(); turnaround = p.turnaround(); response = p.response();
        }
        public String getId() { return id; }
        public int getArrival() { return arrival; }
        public int getBurst() { return burst; }
        public int getFirstStart() { return firstStart; }
        public int getFinish() { return finish; }
        public int getWaiting() { return waiting; }
        public int getTurnaround() { return turnaround; }
        public int getResponse() { return response; }
    }

    public static void main(String[] args) { launch(args); }
}
