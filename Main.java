// Main.java
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * Main application class for downloading Chrome offline installers on Windows or macOS.
 */
public class Main extends Application {

    private ComboBox<String> versionChoiceBox;
    private Button fetchButton;
    private ListView<String> linkListView;
    private Button downloadButton;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Label resultLabel;
    private List<String> currentDownloadLinks = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        // Detect platform for title
        String osName = PlatformUtils.isWindows() ? "Windows" : (PlatformUtils.isMac() ? "macOS" : "Unknown");
        String arch = PlatformUtils.getArchForUpdateService();

        // UI Components
        versionChoiceBox = new ComboBox<>();
        versionChoiceBox.getItems().addAll("Stable", "Beta", "Dev", "Canary");
        versionChoiceBox.setValue("Stable");

        fetchButton = new Button("Fetch Download Links");
        fetchButton.setOnAction(e -> fetchDownloadLinks());

        linkListView = new ListView<>();
        linkListView.setPrefHeight(120);
        linkListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        downloadButton = new Button("Download Selected Link");
        downloadButton.setOnAction(e -> startDownload());
        downloadButton.setDisable(true);

        linkListView.getSelectionModel().selectedItemProperty()
            .addListener(this::onListViewSelectionChanged);

        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setProgress(0.0);

        progressLabel = new Label("Ready");
        progressLabel.setVisible(false);
        progressLabel.setStyle("-fx-text-fill: black;");

        resultLabel = new Label();
        resultLabel.setVisible(false);
        resultLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(
            new HBox(10, new Label("Select Version:"), versionChoiceBox),
            fetchButton,
            new Label("Available Download Links:"),
            linkListView,
            downloadButton,
            progressBar,
            progressLabel,
            resultLabel
        );

        // Set scene size to 935x435 pixels
        Scene scene = new Scene(root, 935, 435);

        primaryStage.setTitle("Chrome Offline Installer Downloader (" + osName + " " + arch + ")");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        // ✅ Load window icons from JAR classpath (MUST start with '/')
        addIconIfExists(primaryStage, "/Resources/512.png");
        addIconIfExists(primaryStage, "/Resources/256.png");
        addIconIfExists(primaryStage, "/Resources/128.png");
        addIconIfExists(primaryStage, "/Resources/96.png");
        addIconIfExists(primaryStage, "/Resources/72.png");
        addIconIfExists(primaryStage, "/Resources/64.png");

        primaryStage.show();
    }

    /**
     * ✅ Loads an icon from the classpath (e.g., inside the JAR).
     * Resource path must start with '/' to indicate root of classpath.
     */
    private void addIconIfExists(Stage stage, String resourcePath) {
        try (java.io.InputStream is = Main.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                Image icon = new Image(is);
                if (!icon.isError()) {
                    stage.getIcons().add(icon);
                }
            }
        } catch (Exception ignored) {
            // Ignore failures for individual icons
        }
    }

    private void onListViewSelectionChanged(ObservableValue<? extends String> obs, String oldVal, String newVal) {
        downloadButton.setDisable(newVal == null);
    }

    private void fetchDownloadLinks() {
        resultLabel.setVisible(false);
        progressLabel.setVisible(false);
        progressBar.setVisible(false);

        String selectedVersion = versionChoiceBox.getValue();
        FetchLinksTask task = new FetchLinksTask(selectedVersion);

        task.setOnSucceeded(e -> {
            currentDownloadLinks = task.getValue();
            linkListView.getItems().clear();
            if (currentDownloadLinks.isEmpty()) {
                linkListView.getItems().add("No download links found.");
                progressLabel.setVisible(true);
                progressLabel.setText("No download links found. Please try again later.");
                progressLabel.setStyle("-fx-text-fill: red;");
            } else {
                for (int i = 0; i < currentDownloadLinks.size(); i++) {
                    linkListView.getItems().add((i + 1) + ". " + currentDownloadLinks.get(i));
                }
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            progressLabel.setVisible(true);
            progressLabel.setText("Failed to fetch links: " + (ex != null ? ex.getMessage() : "Unknown error"));
            progressLabel.setStyle("-fx-text-fill: red;");
            linkListView.getItems().clear();
            linkListView.getItems().add("Error occurred while fetching links.");
        });

        new Thread(task).start();
    }

    private void startDownload() {
        int selectedIndex = linkListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= currentDownloadLinks.size()) return;

        String url = currentDownloadLinks.get(selectedIndex);
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        java.io.File outputFile = new java.io.File(fileName).getAbsoluteFile();

        resultLabel.setVisible(false);
        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressBar.setProgress(0);
        progressLabel.setText("Starting download...");
        progressLabel.setStyle("-fx-text-fill: black;");

        DownloadFileTask downloadTask = new DownloadFileTask(url, outputFile);

        downloadTask.progressProperty().addListener((obs, old, progress) ->
            progressBar.setProgress(progress.doubleValue())
        );
        downloadTask.messageProperty().addListener((obs, old, msg) ->
            progressLabel.setText(msg)
        );

        downloadTask.setOnSucceeded(e -> {
            progressLabel.setText("Download completed!");
            progressLabel.setStyle("-fx-text-fill: green;");
            resultLabel.setText("File saved as: " + outputFile.getAbsolutePath());
            resultLabel.setVisible(true);
        });

        downloadTask.setOnFailed(e -> {
            progressLabel.setText("Download failed");
            progressLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setVisible(false);
        });

        new Thread(downloadTask).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}