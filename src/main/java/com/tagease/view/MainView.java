package com.tagease.view;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
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
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainView {
    // UI Components
    private final TagController controller;
    private final Stage stage;
    private TableView<TaggedFile> fileTable;
    private TextField searchField;
    private ComboBox<String> searchCriteriaBox;
    private ComboBox<String> tagFilterBox;
    private FlowPane selectedTagsPane;

    // Data
    private ObservableList<TaggedFile> filesList;
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
        // Initialize UI components
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Create sections
        VBox searchContainer = createSearchSection();
        root.setTop(searchContainer);

        VBox tableBox = createTableSection();
        root.setCenter(tableBox);

        // Create scene
        Scene scene = new Scene(root, 700, 500);
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
            Label tagLabel = new Label(tag);
            tagLabel.setStyle("-fx-background-color: #e0e9e9; -fx-padding: 2 5; -fx-border-radius: 5;");
            selectedTagsPane.getChildren().add(tagLabel);
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

    private VBox createTableSection() {
        VBox tableBox = new VBox(5);
        tableBox.getStyleClass().add("section-box");
        
        // Create table
        fileTable = new TableView<>();
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Add double-click handler
        fileTable.setRowFactory(tv -> {
            TableRow<TaggedFile> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    TaggedFile file = row.getItem();
                    openFile(file);
                }
            });
            return row;
        });
        
        // Create columns
        TableColumn<TaggedFile, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        
        TableColumn<TaggedFile, Set<String>> tagsCol = new TableColumn<>("Tags");
        tagsCol.setCellValueFactory(new PropertyValueFactory<>("tags"));
        tagsCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Set<String> tags, boolean empty) {
                super.updateItem(tags, empty);
                if (empty || tags == null) {
                    setText(null);
                } else {
                    setText(String.join(", ", tags));
                }
            }
        });
        
        TableColumn<TaggedFile, LocalDateTime> createdCol = new TableColumn<>("Created At");
        createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdCol.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            @Override
            protected void updateItem(LocalDateTime datetime, boolean empty) {
                super.updateItem(datetime, empty);
                if (empty || datetime == null) {
                    setText(null);
                } else {
                    setText(formatter.format(datetime));
                }
            }
        });
        
        TableColumn<TaggedFile, LocalDateTime> accessedCol = new TableColumn<>("Last Accessed");
        accessedCol.setCellValueFactory(new PropertyValueFactory<>("lastAccessedAt"));
        accessedCol.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            @Override
            protected void updateItem(LocalDateTime datetime, boolean empty) {
                super.updateItem(datetime, empty);
                if (empty || datetime == null) {
                    setText(null);
                } else {
                    setText(formatter.format(datetime));
                }
            }
        });

        fileTable.getColumns().addAll(nameCol, tagsCol, createdCol, accessedCol);
        
        // Add context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editTagsItem = new MenuItem("Edit Tags");
        editTagsItem.setOnAction(e -> {
            TaggedFile selectedFile = fileTable.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                editFileTags(selectedFile);
            }
        });
        
        MenuItem deleteFileItem = new MenuItem("Delete File");
        deleteFileItem.setOnAction(e -> {
            TaggedFile selectedFile = fileTable.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete File");
                alert.setHeaderText("Delete " + selectedFile.getFileName());
                alert.setContentText("Are you sure you want to delete this file from the system?");
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    controller.deleteFile(selectedFile.getFilePath());
                    refreshTable();
                }
            }
        });
        
        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(event -> {
            TaggedFile selectedFile = fileTable.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                openFile(selectedFile);
            }
        });
        
        contextMenu.getItems().addAll(editTagsItem, deleteFileItem, openItem);
        fileTable.setContextMenu(contextMenu);
        
        tableBox.getChildren().addAll(fileTable);
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
            fileTable.setPlaceholder(noResultsLabel);
        } else {
            fileTable.setPlaceholder(new Label(""));
        }
        
        filesList.setAll(filteredFiles);
        fileTable.setItems(filesList);
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
        Dialog<Set<String>> dialog = new Dialog<>();
        dialog.setTitle("Edit Tags");
        dialog.setHeaderText("Edit tags for " + file.getFileName());
        
        // Create the dialog content
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Current tags
        Label currentTagsLabel = new Label("Current Tags (comma-separated):");
        TextField tagsField = new TextField(String.join(", ", file.getTags()));
        
        content.getChildren().addAll(currentTagsLabel, tagsField);
        dialog.getDialogPane().setContent(content);
        
        // Add buttons
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        
        // Set the result converter
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                Set<String> tags = new HashSet<>();
                for (String tag : tagsField.getText().split(",")) {
                    String trimmedTag = tag.trim();
                    if (!trimmedTag.isEmpty()) {
                        tags.add(trimmedTag);
                    }
                }
                return tags;
            }
            return null;
        });
        
        Optional<Set<String>> result = dialog.showAndWait();
        result.ifPresent(tags -> {
            file.setTags(tags);
            controller.updateFileTags(file);
            refreshTable();
        });
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
        fileTable.setItems(filesList);
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

    public void show() {
        stage.show();
    }
}
