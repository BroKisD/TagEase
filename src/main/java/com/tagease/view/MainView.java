package com.tagease.view;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.tagease.controller.TagController;
import com.tagease.model.TaggedFile;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainView {
    // UI Components
    private final TagController controller;
    private final Stage stage;
    private VBox fileListContainer;
    private ScrollPane fileListScrollPane;
    private TextField searchField;
    private ComboBox<String> searchCriteriaBox;
    private ComboBox<String> tagFilterBox;
    private FlowPane selectedTagsPane;

    // Data
    private final ObservableList<TaggedFile> filesList;
    private static final List<String> PREDEFINED_TAGS = List.of(
        "New", "Reference", "15m Test", "Midterm Exam", "Final Exam", "Done", "In Progress"
    );
    private static final String DEFAULT_TAG = "New";
    private Set<String> selectedTags = new HashSet<>();

    public MainView(Stage stage, TagController controller) {
        this.stage = stage;
        this.controller = controller;
        this.filesList = FXCollections.observableArrayList();
        
        initializeUI();
    }

    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.getStyleClass().add("root");

        // Create the main layout
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        // Create sections
        VBox searchContainer = createSearchSection();
        root.setTop(searchContainer);

        VBox centerContent = createCenterContent();
        root.setCenter(centerContent);

        // Create scene
        Scene scene = new Scene(root, 800, 600);
        
        // Load custom styles with specific exception handling
        try {
            File cssFile = new File("src/main/resources/styles.css");
            if (cssFile.exists()) {
                String cssPath = cssFile.toURI().toURL().toExternalForm();
                scene.getStylesheets().add(cssPath);
            } else {
                System.err.println("CSS file not found at: " + cssFile.getAbsolutePath());
            }
        } catch (MalformedURLException | SecurityException e) {
            System.err.println("Failed to load CSS - " + e.getMessage());
            e.printStackTrace();
        }
        
        stage.setTitle("TagEase - File Tagging System");
        stage.setScene(scene);

        // Create toolbar
        createToolbar();

        refreshTable();
    }

    private void createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));

        Button addFileButton = new Button("Add File");
        addFileButton.setOnAction(e -> addFile());

        Button manageTagsButton = new Button("Manage Tags");
        manageTagsButton.setOnAction(e -> showTagManagementWindow());

        toolbar.getChildren().addAll(addFileButton, manageTagsButton);
        BorderPane root = (BorderPane) stage.getScene().getRoot();
        root.setBottom(toolbar);
    }

    private void showTagManagementWindow() {
        Stage tagWindow = new Stage();
        tagWindow.setTitle("Manage Tags");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        // Add Tag Section
        HBox addTagBox = new HBox(10);
        TextField newTagField = new TextField();
        newTagField.setPromptText("Enter new tag");
        Button addTagButton = new Button("Add Tag");
        addTagButton.setOnAction(e -> {
            String newTag = newTagField.getText().trim();
            if (!newTag.isEmpty()) {
                controller.addTag(newTag);
                newTagField.clear();
                updateTagFilterBox();
            }
        });
        addTagBox.getChildren().addAll(newTagField, addTagButton);

        // Remove Tag Section
        HBox removeTagBox = new HBox(10);
        ComboBox<String> tagSelector = new ComboBox<>();
        tagSelector.setPromptText("Select tag to remove");
        tagSelector.setItems(FXCollections.observableArrayList(controller.getAllTags()));
        Button removeTagButton = new Button("Remove Tag");
        removeTagButton.setOnAction(e -> {
            String selectedTag = tagSelector.getSelectionModel().getSelectedItem();
            if (selectedTag != null) {
                controller.removeTag(selectedTag);
                tagSelector.getItems().remove(selectedTag);
                updateTagFilterBox();
            }
        });
        removeTagBox.getChildren().addAll(tagSelector, removeTagButton);

        layout.getChildren().addAll(
            new Label("Add New Tag:"), addTagBox,
            new Label("Remove Existing Tag:"), removeTagBox
        );

        Scene scene = new Scene(layout, 400, 200);
        tagWindow.setScene(scene);
        tagWindow.show();
    }

    private VBox createSearchSection() {
        VBox searchContainer = new VBox(10);
        searchContainer.setPadding(new Insets(10));

        // Search box on top
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = new Label("Search:");
        searchField = new TextField();
        searchField.setPromptText("Enter search terms...");
        searchField.setPrefWidth(300);

        // Add search by option
        Label searchByLabel = new Label("Search by:");
        searchCriteriaBox = new ComboBox<>();
        searchCriteriaBox.getItems().addAll("File Name", "Tags");
        searchCriteriaBox.setValue("File Name");

        searchBox.getChildren().addAll(searchLabel, searchField, searchByLabel, searchCriteriaBox);

        // Filter controls
        HBox filterControls = new HBox(10);
        filterControls.setAlignment(Pos.CENTER_LEFT);

        // Filter by tags
        Label filterTagLabel = new Label("Filter by tags:");
        filterTagLabel.setMinWidth(80);
        tagFilterBox = new ComboBox<>();
        tagFilterBox.setPromptText("Select tags");
        tagFilterBox.setPrefWidth(200);
        tagFilterBox.setMinWidth(100);

        // Selected tags visualization
        selectedTagsPane = new FlowPane();
        selectedTagsPane.setHgap(5);
        selectedTagsPane.setVgap(5);
        selectedTagsPane.setPadding(new Insets(5));

        Button clearTagsButton = new Button("Clear Filters");
        clearTagsButton.setMinWidth(100);

        Label selectedTagsLabel = new Label("Selected tags:");
        selectedTagsLabel.setMinWidth(100);

        filterControls.getChildren().addAll(
            filterTagLabel, tagFilterBox, clearTagsButton,
            selectedTagsLabel, selectedTagsPane
        );

        // Update selected tags visualization
        updateSelectedTagsPane();

        // Set up event handlers
        tagFilterBox.setOnAction(e -> {
            String selectedTag = tagFilterBox.getSelectionModel().getSelectedItem();
            if (selectedTag != null && !selectedTags.contains(selectedTag)) {
                selectedTags.add(selectedTag);
                refreshTable();
                updateSelectedTagsPane();
                updateTagFilterBox();
            }
        });

        clearTagsButton.setOnAction(e -> {
            selectedTags.clear();
            refreshTable();
            updateSelectedTagsPane();
            updateTagFilterBox();
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> performSearch());
        searchCriteriaBox.setOnAction(e -> performSearch());

        searchContainer.getChildren().addAll(searchBox, filterControls);

        updateTagFilterBox();
        return searchContainer;
    }

    private void updateSelectedTagsPane() {
        selectedTagsPane.getChildren().clear();
        for (String tag : selectedTags) {
            HBox tagBox = new HBox(5);
            tagBox.setAlignment(Pos.CENTER_LEFT);
            tagBox.setPadding(new Insets(3, 8, 3, 8));
            tagBox.setStyle("-fx-background-color: #e0e9e9; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #c0c9c9;");
            
            Label tagLabel = new Label(tag);
            tagLabel.setStyle("-fx-text-fill: #333333;");
            
            Button removeButton = new Button("×");
            removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #666666; -fx-padding: 0 0 0 5; -fx-font-weight: bold;");
            removeButton.setCursor(Cursor.HAND);
            removeButton.setOnAction(e -> {
                selectedTags.remove(tag);
                refreshTable();
                updateSelectedTagsPane();
                updateTagFilterBox();
            });
            
            tagBox.getChildren().addAll(tagLabel, removeButton);
            selectedTagsPane.getChildren().add(tagBox);
        }
    }

    private void updateTagFilterBox() {
        try {
            Set<String> allTags = controller.getAllTags();
            tagFilterBox.getItems().setAll(allTags);
            tagFilterBox.getSelectionModel().clearSelection();
        } catch (RuntimeException e) {
            showErrorDialog("Error", "Failed to load tags", e.getMessage());
        }
    }

    private VBox createCenterContent() {
        VBox tableBox = new VBox(5);
        tableBox.getStyleClass().add("section-box");
        
        // Create file list container
        fileListContainer = new VBox(5);
        fileListContainer.setPadding(new Insets(10));
        
        // Create scroll pane to contain the file list
        fileListScrollPane = new ScrollPane(fileListContainer);
        fileListScrollPane.setFitToWidth(true);
        fileListScrollPane.setPrefHeight(400);
        
        Label noFilesLabel = new Label("No files found");
        noFilesLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
        
        tableBox.getChildren().addAll(fileListScrollPane);
        return tableBox;
    }

    private void performSearch() {
        String searchText = searchField.getText().toLowerCase();
        String searchCriteria = searchCriteriaBox.getValue();
        
        List<TaggedFile> allFiles = controller.getAllFiles();
        List<TaggedFile> filteredFiles = new ArrayList<>();
        
        for (TaggedFile file : allFiles) {
            boolean matches = false;
            switch (searchCriteria) {
                case "File Name":
                    matches = file.getFileName().toLowerCase().contains(searchText);
                    break;
                case "Tags":
                    matches = file.getTags().stream()
                        .anyMatch(tag -> tag.toLowerCase().contains(searchText));
                    break;
            }
            if (matches) {
                filteredFiles.add(file);
            }
        }
        
        if (filteredFiles.isEmpty()) {
            Label noResultsLabel = new Label("No Results Found");
            noResultsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
            fileListContainer.getChildren().clear();
            fileListContainer.getChildren().add(noResultsLabel);
        } else {
            fileListContainer.getChildren().clear();
            filesList.setAll(filteredFiles);
            updateFileListView();
        }
    }

    private void addFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            // Get existing tags before showing dialog
            Set<String> existingTags = controller.getAllTags();
            
            Dialog<TaggedFile> dialog = new Dialog<>();
            dialog.setTitle("Add File");
            dialog.setHeaderText("Add tags for " + file.getName());
            
            // Create the dialog content
            VBox content = new VBox(10);
            content.setPadding(new Insets(10));
            
            // Tags section
            Label tagsLabel = new Label("Tags (comma-separated):");
            TextField tagsField = new TextField();
            
            // Add tag suggestions
            FlowPane tagSuggestions = new FlowPane();
            tagSuggestions.setHgap(5);
            tagSuggestions.setVgap(5);
            
            // Add predefined tags
            for (String tag : PREDEFINED_TAGS) {
                Button tagButton = new Button(tag);
                tagButton.setOnAction(e -> {
                    String currentTags = tagsField.getText();
                    tagsField.setText(currentTags.isEmpty() ? tag : currentTags + ", " + tag);
                });
                tagSuggestions.getChildren().add(tagButton);
            }
            
            // Add existing tags
            for (String tag : existingTags) {
                if (!PREDEFINED_TAGS.contains(tag)) {
                    Button tagButton = new Button(tag);
                    tagButton.setOnAction(e -> {
                        String currentTags = tagsField.getText();
                        tagsField.setText(currentTags.isEmpty() ? tag : currentTags + ", " + tag);
                    });
                    tagSuggestions.getChildren().add(tagButton);
                }
            }
            
            content.getChildren().addAll(tagsLabel, tagsField, new Label("Suggested Tags:"), tagSuggestions);
            dialog.getDialogPane().setContent(content);
            
            // Add buttons
            ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);
            
            // Set the result converter
            dialog.setResultConverter(buttonType -> {
                if (buttonType == addButton) {
                    Set<String> tags = new HashSet<>();
                    String inputTags = tagsField.getText();
                    if (inputTags.trim().isEmpty()) {
                        tags.add(DEFAULT_TAG);
                    } else {
                        for (String tag : inputTags.split(",")) {
                            String trimmedTag = tag.trim();
                            if (!trimmedTag.isEmpty()) {
                                tags.add(trimmedTag);
                            }
                        }
                    }
                    return new TaggedFile(file.getName(), file.getAbsolutePath(), tags);
                }
                return null;
            });
            
            Optional<TaggedFile> result = dialog.showAndWait();
            result.ifPresent(taggedFile -> {
                try {
                    controller.addFile(taggedFile, existingTags);
                    refreshTable();
                } catch (RuntimeException e) {
                    showErrorDialog("Error", "Failed to add file", e.getMessage());
                }
            });
        }
    }

    private void editFileTags(TaggedFile file) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Tags");
        dialog.setHeaderText("Edit tags for " + file.getFileName());

        // Apply dark theme to dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");

        // Create the content
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Current tags
        Label currentTagsLabel = new Label("Current Tags:");
        FlowPane currentTags = new FlowPane(5, 5);
        
        for (String tag : file.getTags()) {
            HBox tagBox = new HBox(5);
            tagBox.getStyleClass().add("tag-box");
            
            Label tagLabel = new Label(tag);
            Button removeButton = new Button("×");
            removeButton.getStyleClass().add("remove-tag-button");
            removeButton.setOnAction(e -> {
                Set<String> updatedTags = new HashSet<>(file.getTags());
                updatedTags.remove(tag);
                file.setTags(updatedTags);
                controller.updateFileTags(file);
                currentTags.getChildren().remove(tagBox);
            });
            
            tagBox.getChildren().addAll(tagLabel, removeButton);
            currentTags.getChildren().add(tagBox);
        }

        // Add new tag
        Label addTagLabel = new Label("Add Tag:");
        ComboBox<String> tagInput = new ComboBox<>();
        tagInput.setEditable(true);
        tagInput.setPromptText("Enter or select a tag");
        tagInput.getItems().addAll(controller.getAllTags());

        Button addButton = new Button("Add Tag");
        addButton.getStyleClass().addAll("button", "action-button");
        addButton.setOnAction(e -> {
            String newTag = tagInput.getValue();
            if (newTag != null && !newTag.trim().isEmpty()) {
                Set<String> updatedTags = new HashSet<>(file.getTags());
                updatedTags.add(newTag.trim());
                file.setTags(updatedTags);
                controller.updateFileTags(file);
                
                HBox tagBox = new HBox(5);
                tagBox.getStyleClass().add("tag-box");
                
                Label tagLabel = new Label(newTag.trim());
                Button removeButton = new Button("×");
                removeButton.getStyleClass().add("remove-tag-button");
                removeButton.setOnAction(ev -> {
                    Set<String> tags = new HashSet<>(file.getTags());
                    tags.remove(newTag.trim());
                    file.setTags(tags);
                    controller.updateFileTags(file);
                    currentTags.getChildren().remove(tagBox);
                });
                
                tagBox.getChildren().addAll(tagLabel, removeButton);
                currentTags.getChildren().add(tagBox);
                
                tagInput.setValue("");
            }
        });

        content.getChildren().addAll(
            currentTagsLabel, currentTags,
            addTagLabel, tagInput, addButton
        );

        dialogPane.setContent(content);

        // Add buttons
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

        // Show dialog
        dialog.showAndWait();
        
        // Refresh the table to show updated tags
        refreshTable();
    }

    private void refreshTable() {
        List<TaggedFile> files;
        if (selectedTags.isEmpty()) {
            files = controller.getAllFiles();
        } else {
            files = controller.getFilesByTags(selectedTags);
        }

        // Apply search filter
        String searchText = searchField.getText().toLowerCase();
        String searchCriteria = searchCriteriaBox.getValue();
        List<TaggedFile> filteredFiles = files.stream()
            .filter(file -> {
                if (searchCriteria.equals("File Name")) {
                    return file.getFileName().toLowerCase().contains(searchText);
                } else {
                    return file.getTags().stream()
                        .anyMatch(tag -> tag.toLowerCase().contains(searchText));
                }
            })
            .collect(Collectors.toList());

        filesList.setAll(filteredFiles);
        updateFileListView();
    }
    
    private void updateFileListView() {
        fileListContainer.getChildren().clear();
        
        if (filesList.isEmpty()) {
            Label noFilesLabel = new Label("No files found");
            noFilesLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
            fileListContainer.getChildren().add(noFilesLabel);
            return;
        }
        
        for (TaggedFile file : filesList) {
            fileListContainer.getChildren().add(createFilePanel(file));
        }
    }
    
    private TitledPane createFilePanel(TaggedFile file) {
        // Create the main content for the collapsed state
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0));
        header.setMaxWidth(700); // Limit width to prevent arrow overlap
        
        // File name label
        Label fileNameLabel = new Label(file.getFileName());
        fileNameLabel.setStyle("-fx-font-weight: bold;");
        fileNameLabel.setMaxWidth(300);
        
        // Tags flow pane
        FlowPane tagsPane = new FlowPane();
        tagsPane.setHgap(5);
        tagsPane.setVgap(5);
        tagsPane.setPadding(new Insets(0, 5, 0, 5));
        tagsPane.setPrefWrapLength(350); // Set preferred wrap length
        
        for (String tag : file.getTags()) {
            Label tagLabel = new Label(tag);
            tagLabel.getStyleClass().add("tag-box");
            tagsPane.getChildren().add(tagLabel);
        }
        
        // Add all components to the header
        header.getChildren().addAll(fileNameLabel, tagsPane);
        
        // Create the content for the expanded state
        VBox expandedContent = new VBox(10);
        expandedContent.setPadding(new Insets(10));
        
        // File details
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(10);
        detailsGrid.setVgap(5);
        detailsGrid.setPadding(new Insets(5));
        
        // Created date
        Label createdLabel = new Label("Created:");
        createdLabel.setStyle("-fx-font-weight: bold;");
        Label createdValueLabel = new Label(
            file.getCreatedAt() != null 
                ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(file.getCreatedAt())
                : "N/A"
        );
        
        // Last accessed date
        Label accessedLabel = new Label("Last Accessed:");
        accessedLabel.setStyle("-fx-font-weight: bold;");
        Label accessedValueLabel = new Label(
            file.getLastAccessedAt() != null 
                ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(file.getLastAccessedAt())
                : "N/A"
        );
        
        // File path
        Label pathLabel = new Label("Path:");
        pathLabel.setStyle("-fx-font-weight: bold;");
        Label pathValueLabel = new Label(file.getFilePath());
        
        // Add to grid
        detailsGrid.add(createdLabel, 0, 0);
        detailsGrid.add(createdValueLabel, 1, 0);
        detailsGrid.add(accessedLabel, 0, 1);
        detailsGrid.add(accessedValueLabel, 1, 1);
        detailsGrid.add(pathLabel, 0, 2);
        detailsGrid.add(pathValueLabel, 1, 2);
        
        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));
        
        Button editTagsButton = new Button("Edit Tags");
        editTagsButton.getStyleClass().addAll("button", "action-button");
        editTagsButton.setOnAction(e -> editFileTags(file));
        
        Button openFileButton = new Button("Open File");
        openFileButton.getStyleClass().addAll("button", "action-button");
        openFileButton.setOnAction(e -> openFile(file));
        
        Button deleteFileButton = new Button("Delete");
        deleteFileButton.getStyleClass().addAll("button", "delete-button");
        deleteFileButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete File");
            alert.setHeaderText("Delete " + file.getFileName());
            alert.setContentText("Are you sure you want to delete this file from the system?");
            
            // Apply dark theme to alert dialog
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            dialogPane.getStyleClass().add("dialog-pane");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                controller.deleteFile(file.getFilePath());
                refreshTable();
            }
        });
        
        actionButtons.getChildren().addAll(editTagsButton, openFileButton, deleteFileButton);
        
        // Add all to expanded content
        expandedContent.getChildren().addAll(detailsGrid, actionButtons);
        
        // Create titled pane
        TitledPane filePanel = new TitledPane();
        filePanel.setGraphic(header);
        filePanel.setContent(expandedContent);
        filePanel.setExpanded(false);
        filePanel.setText(null); // Remove default text
        filePanel.setUserData(file); // Store the file object for reference
        filePanel.getStyleClass().add("file-panel");
        
        return filePanel;
    }

    private void openFile(TaggedFile file) {
        try {
            File f = new File(file.getFilePath());
            if (f.exists()) {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    new ProcessBuilder("cmd", "/c", f.getAbsolutePath()).start();
                } else {
                    new ProcessBuilder("xdg-open", f.getAbsolutePath()).start();
                }
            } else {
                showErrorDialog("File Not Found", "Could not open file", "The file " + file.getFileName() + " does not exist.");
            }
        } catch (IOException e) {
            showErrorDialog("Error Opening File", "Could not open file", e.getMessage());
        }
    }

    private void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Highlights files in the table that are missing from the file system.
     * Missing files will be highlighted in yellow.
     * 
     * @param missingFilePaths List of file paths that are missing
     */
    public void highlightMissingFiles(List<String> missingFilePaths) {
        if (missingFilePaths == null || missingFilePaths.isEmpty()) {
            return;
        }
        
        // Refresh the file list to apply the highlighting
        refreshTable();
        
        // Apply highlighting to missing files
        for (Node node : fileListContainer.getChildren()) {
            if (node instanceof TitledPane) {
                TitledPane filePanel = (TitledPane) node;
                if (filePanel.getUserData() instanceof TaggedFile) {
                    TaggedFile file = (TaggedFile) filePanel.getUserData();
                    if (missingFilePaths.contains(file.getFilePath())) {
                        // File is missing - highlight in red
                        filePanel.setStyle("-fx-border-color: #ff0000; -fx-border-radius: 5; -fx-background-color: #ffeeee;");
                    }
                }
            }
        }
        
        // Show a notification about missing files that includes the filenames
        if (!missingFilePaths.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Missing Files");
            alert.setHeaderText("Some files are missing");
            
            // Build a list of missing filenames to display
            StringBuilder missingFilesText = new StringBuilder("The following files are missing:\n\n");
            for (String filePath : missingFilePaths) {
                File file = new File(filePath);
                missingFilesText.append("• ").append(file.getName()).append("\n");
            }
            missingFilesText.append("\nThey may have been moved, renamed, or deleted.");
            
            alert.setContentText(missingFilesText.toString());
            alert.show();
        }
    }

    public void show() {
        stage.show();
    }
}
