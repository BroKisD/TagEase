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
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainView {
    // UI Components
    private Stage stage;
    private TagController controller;
    private Set<String> selectedTags = new HashSet<>();
    private FlowPane selectedTagsPane;
    private VBox fileListContainer;
    private String searchTerm = "";
    private String searchOption = "File Name";

    // Data
    private static final List<String> PREDEFINED_TAGS = List.of(
        "New", "Reference", "15m Test", "Midterm Exam", "Final Exam", "Done", "In Progress"
    );
    private static final String DEFAULT_TAG = "New";
    private ObservableList<TaggedFile> filesList;

    public MainView(Stage stage, TagController controller) {
        this.stage = stage;
        this.controller = controller;
        this.filesList = FXCollections.observableArrayList();
        
        initializeUI();
    }

    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Create the main layout with a sidebar-content structure (Postman-like)
        HBox mainLayout = new HBox(0);
        mainLayout.setPrefHeight(Region.USE_COMPUTED_SIZE);
        mainLayout.setFillHeight(true);
        
        // Create left sidebar with toolbar buttons
        VBox sidebar = new VBox(5);
        sidebar.getStyleClass().add("sidebar");
        
        Label appTitle = new Label("TagEase");
        appTitle.getStyleClass().add("app-title");
        appTitle.setPadding(new Insets(10, 0, 20, 10));
        
        // Add File button with icon
        Button addFileButton = new Button("Add File");
        addFileButton.getStyleClass().add("sidebar-button");
        addFileButton.setMaxWidth(Double.MAX_VALUE);
        ImageView addFileIcon = createIcon("src/main/resources/Image/file_338043.png", 16);
        if (addFileIcon != null) {
            addFileButton.setGraphic(addFileIcon);
            addFileButton.setGraphicTextGap(10);
        }
        addFileButton.setOnAction(e -> addFile());
        
        // Manage Tags button with icon
        Button manageTagsButton = new Button("Manage Tags");
        manageTagsButton.getStyleClass().add("sidebar-button");
        manageTagsButton.setMaxWidth(Double.MAX_VALUE);
        ImageView manageTagsIcon = createIcon("src/main/resources/Image/file_1346913.png", 16);
        if (manageTagsIcon != null) {
            manageTagsButton.setGraphic(manageTagsIcon);
            manageTagsButton.setGraphicTextGap(10);
        }
        manageTagsButton.setOnAction(e -> showTagManagementWindow());
        
        sidebar.getChildren().addAll(appTitle, addFileButton, manageTagsButton);
        mainLayout.getChildren().add(sidebar);
        
        // Create the content area
        BorderPane contentArea = new BorderPane();
        contentArea.setPrefHeight(Region.USE_COMPUTED_SIZE);
        
        // Create search and filter section at the top of content area
        VBox searchAndFilterContainer = createSearchSection();
        contentArea.setTop(searchAndFilterContainer);

        // Create file list in the center
        VBox centerContent = createCenterContent();
        contentArea.setCenter(centerContent);
        
        // Add content area to main layout
        mainLayout.getChildren().add(contentArea);
        HBox.setHgrow(contentArea, Priority.ALWAYS);
        
        root.setCenter(mainLayout);

        // Create scene
        Scene scene = new Scene(root, 900, 700);
        
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
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        
        // Set the application to open in full screen by default
        stage.setMaximized(true);

        refreshTable();
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
        searchContainer.getStyleClass().add("search-container");
        searchContainer.setPadding(new Insets(15));
        
        // Search box with icon
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        
        // Search icon
        ImageView searchIcon = createIcon("src/main/resources/Image/magnifier_868231.png", 16);
        Label searchLabel;
        if (searchIcon != null) {
            searchLabel = new Label("Search:", searchIcon);
            searchLabel.setGraphicTextGap(5);
        } else {
            searchLabel = new Label("Search:");
        }
        searchLabel.getStyleClass().add("section-label");
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search by file name...");
        searchField.getStyleClass().add("search-box");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        ComboBox<String> searchOptions = new ComboBox<>();
        searchOptions.getItems().addAll("File Name", "Tags", "Path");
        searchOptions.setValue("File Name");
        searchOptions.getStyleClass().add("combo-box");
        
        searchBox.getChildren().addAll(searchLabel, searchField, searchOptions);
        
        // Filter section with enhanced styling
        VBox filterContainer = new VBox(10);
        filterContainer.getStyleClass().add("filter-container");
        filterContainer.setPadding(new Insets(12));
        
        // Tag selection - now in a single row with Clear All button on the right
        HBox tagSelectionBox = new HBox(10);
        tagSelectionBox.setAlignment(Pos.CENTER_LEFT);
        tagSelectionBox.getStyleClass().add("filter-header-item");
        
        // Filter icon
        ImageView filterIcon = createIcon("src/main/resources/Image/filter_679890.png", 16);
        Label filterLabel;
        if (filterIcon != null) {
            filterLabel = new Label("Filters:", filterIcon);
            filterLabel.setGraphicTextGap(5);
        } else {
            filterLabel = new Label("Filters:");
        }
        filterLabel.getStyleClass().add("section-label");
        
        Label tagLabel = new Label("Tag:");
        tagLabel.getStyleClass().add("field-label");
        
        // Enhanced tag dropdown
        ComboBox<String> tagComboBox = new ComboBox<>();
        tagComboBox.setPromptText("Select a tag");
        tagComboBox.getStyleClass().add("combo-box");
        HBox.setHgrow(tagComboBox, Priority.ALWAYS);
        
        // Populate tag dropdown with existing tags
        Set<String> allTags = controller.getAllTags();
        tagComboBox.getItems().addAll(allTags);
        
        Button clearAllButton = new Button("Clear All");
        clearAllButton.getStyleClass().add("clear-button");
        clearAllButton.setOnAction(e -> {
            selectedTags.clear();
            refreshTable();
            updateSelectedTagsDisplay();
        });
        
        // Add tag when selected from dropdown (no add button needed)
        tagComboBox.setOnAction(e -> {
            String selectedTag = tagComboBox.getValue();
            if (selectedTag != null && !selectedTag.isEmpty() && !selectedTags.contains(selectedTag)) {
                selectedTags.add(selectedTag);
                refreshTable();
                updateSelectedTagsDisplay();
                tagComboBox.setValue(null); // Clear selection after adding
            }
        });
        
        // Add all components to the tag selection box in a single row
        tagSelectionBox.getChildren().addAll(filterLabel, tagLabel, tagComboBox, clearAllButton);
        
        // Selected tags display
        FlowPane selectedTagsPane = new FlowPane();
        selectedTagsPane.setHgap(5);
        selectedTagsPane.setVgap(5);
        selectedTagsPane.setPadding(new Insets(5));
        selectedTagsPane.getStyleClass().add("selected-tags-pane");
        
        // Add filter components to filter container
        filterContainer.getChildren().addAll(tagSelectionBox, selectedTagsPane);
        
        // Store reference to update later
        this.selectedTagsPane = selectedTagsPane;
        
        // Search field listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchTerm = newValue;
            refreshTable();
        });
        
        // Search option listener
        searchOptions.valueProperty().addListener((observable, oldValue, newValue) -> {
            searchOption = newValue;
            refreshTable();
        });
        
        // Add all components to search container
        searchContainer.getChildren().addAll(searchBox, filterContainer);
        
        return searchContainer;
    }

    private void updateSelectedTagsDisplay() {
        selectedTagsPane.getChildren().clear();
        
        if (selectedTags.isEmpty()) {
            Label noFiltersLabel = new Label("No active filters");
            noFiltersLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #909090;");
            selectedTagsPane.getChildren().add(noFiltersLabel);
            return;
        }
        
        for (String tag : selectedTags) {
            HBox tagChip = new HBox(5);
            tagChip.getStyleClass().add("tag-chip");
            tagChip.setAlignment(Pos.CENTER_LEFT);
            
            Label tagLabel = new Label(tag);
            
            Button removeButton = new Button("×");
            removeButton.getStyleClass().add("close-button");
            removeButton.setOnAction(e -> {
                selectedTags.remove(tag);
                refreshTable();
                updateSelectedTagsDisplay();
            });
            
            tagChip.getChildren().addAll(tagLabel, removeButton);
            selectedTagsPane.getChildren().add(tagChip);
        }
    }

    private void updateTagFilterBox() {
        try {
            Set<String> allTags = controller.getAllTags();
        } catch (RuntimeException e) {
            showErrorDialog("Error", "Failed to load tags", e.getMessage());
        }
    }

    private VBox createCenterContent() {
        VBox centerContent = new VBox(10);
        centerContent.setPadding(new Insets(10));
        centerContent.setFillWidth(true);
        
        // Create file list container
        fileListContainer = new VBox(5);
        fileListContainer.setPadding(new Insets(10));
        fileListContainer.setFillWidth(true);
        
        // Create scroll pane to contain the file list
        ScrollPane fileListScrollPane = new ScrollPane(fileListContainer);
        fileListScrollPane.setFitToWidth(true);
        fileListScrollPane.setFitToHeight(true); // Make it fill available height
        fileListScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fileListScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fileListScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE); // Dynamic height
        fileListScrollPane.setMinHeight(400); // Minimum height
        
        // Make the scroll pane expand to fill available space
        VBox.setVgrow(fileListScrollPane, Priority.ALWAYS);
        
        centerContent.getChildren().add(fileListScrollPane);
        
        // Make the center content expand to fill available space
        VBox.setVgrow(centerContent, Priority.ALWAYS);
        
        refreshTable();
        
        return centerContent;
    }

    private void performSearch() {
        refreshTable();
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
        
        if (!selectedTags.isEmpty()) {
            files = controller.getFilesByTags(selectedTags);
        } else {
            files = controller.getAllFiles();
        }
        
        // Apply search filter if search term is not empty
        if (searchTerm != null && !searchTerm.isEmpty()) {
            String term = searchTerm.toLowerCase();
            files = files.stream().filter(file -> {
                if (searchOption.equals("File Name")) {
                    return file.getFileName().toLowerCase().contains(term);
                } else if (searchOption.equals("Tags")) {
                    return file.getTags().stream()
                            .anyMatch(tag -> tag.toLowerCase().contains(term));
                } else if (searchOption.equals("Path")) {
                    return file.getFilePath().toLowerCase().contains(term);
                }
                return false;
            }).collect(Collectors.toList());
        }
        
        // Clear and rebuild file list
        fileListContainer.getChildren().clear();
        
        if (files.isEmpty()) {
            Label noFilesLabel = new Label("No files found matching your criteria");
            noFilesLabel.getStyleClass().add("no-files-label");
            noFilesLabel.setPadding(new Insets(20));
            fileListContainer.getChildren().add(noFilesLabel);
        } else {
            for (TaggedFile file : files) {
                TitledPane filePanel = createFilePanel(file);
                fileListContainer.getChildren().add(filePanel);
            }
        }
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
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5, 15, 5, 10)); // Consistent padding
        header.setPrefWidth(Control.USE_COMPUTED_SIZE); // Make panel fit window width
        header.setMaxWidth(Double.MAX_VALUE); // Allow expansion to full width
        header.setSpacing(0); // No spacing between elements - we'll control it manually
        
        // Status indicator based on tags
        Region statusIndicator = new Region();
        statusIndicator.getStyleClass().addAll("status-indicator");
        statusIndicator.setMinWidth(8);
        statusIndicator.setPrefWidth(8);
        statusIndicator.setMaxWidth(8);
        
        // Check if file exists
        File fileObj = new File(file.getFilePath());
        if (!fileObj.exists()) {
            // Missing file - red indicator
            statusIndicator.getStyleClass().add("status-missing");
            // Also add missing-file class to the entire panel
            header.getStyleClass().add("missing-file");
        } else if (file.getTags().contains("Done")) {
            statusIndicator.getStyleClass().add("status-done");
        } else if (file.getTags().contains("In Progress")) {
            statusIndicator.getStyleClass().add("status-in-progress");
        } else {
            statusIndicator.getStyleClass().add("status-new");
        }
        
        // File name and tags container
        HBox contentContainer = new HBox(10);
        contentContainer.setAlignment(Pos.CENTER_LEFT);
        contentContainer.setPadding(new Insets(0, 0, 0, 10)); // Add padding after status indicator
        
        // File name label with larger font
        Label fileNameLabel = new Label(file.getFileName());
        fileNameLabel.getStyleClass().add("file-name");
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
        
        contentContainer.getChildren().addAll(fileNameLabel, tagsPane);
        
        // Add spacer to push action buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Action buttons with icons - all on the right with consistent placement
        HBox actionButtons = new HBox(20);  // Increased spacing between buttons
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.setPrefWidth(150);  // Fixed width for action buttons area
        actionButtons.setMinWidth(150);   // Ensure minimum width
        actionButtons.setMaxWidth(150);   // Ensure maximum width
        
        // Edit tags button with icon - fixed width and alignment
        Button editTagsButton = new Button();
        editTagsButton.getStyleClass().addAll("icon-button", "edit-button");
        editTagsButton.setPrefWidth(30);
        editTagsButton.setMinWidth(30);
        editTagsButton.setMaxWidth(30);
        ImageView editIcon = createIcon("src/main/resources/Image/pencil_505210.png", 16);
        if (editIcon != null) {
            editTagsButton.setGraphic(editIcon);
        } else {
            editTagsButton.setText("✏️");
        }
        editTagsButton.setTooltip(new Tooltip("Edit Tags"));
        editTagsButton.setOnAction(e -> editFileTags(file));
        
        // Open file button with icon - fixed width and alignment
        Button openFileButton = new Button();
        openFileButton.getStyleClass().addAll("icon-button", "open-button");
        openFileButton.setPrefWidth(30);
        openFileButton.setMinWidth(30);
        openFileButton.setMaxWidth(30);
        ImageView openIcon = createIcon("src/main/resources/Image/folder_3767084.png", 16);
        if (openIcon != null) {
            openFileButton.setGraphic(openIcon);
        } else {
            openFileButton.setText("📁");
        }
        openFileButton.setTooltip(new Tooltip("Open File"));
        openFileButton.setOnAction(e -> openFile(file));
        
        // Delete button with icon - fixed width and alignment
        Button deleteFileButton = new Button();
        deleteFileButton.getStyleClass().addAll("icon-button", "delete-button");
        deleteFileButton.setPrefWidth(30);
        deleteFileButton.setMinWidth(30);
        deleteFileButton.setMaxWidth(30);
        ImageView deleteIcon = createIcon("src/main/resources/Image/trash_1161747.png", 16);
        if (deleteIcon != null) {
            deleteFileButton.setGraphic(deleteIcon);
        } else {
            deleteFileButton.setText("🗑️");
        }
        deleteFileButton.setTooltip(new Tooltip("Delete"));
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
        
        // Add all components to the header with precise control over layout
        header.getChildren().addAll(statusIndicator, contentContainer, spacer, actionButtons);
        
        // Create the content for the expanded state
        VBox expandedContent = new VBox(10);
        expandedContent.setPadding(new Insets(15, 10, 10, 10));
        
        // File details
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(15);
        detailsGrid.setVgap(8);
        detailsGrid.setPadding(new Insets(5));
        
        // Created date
        Label createdLabel = new Label("Created:");
        createdLabel.getStyleClass().add("file-metadata");
        createdLabel.setStyle("-fx-font-weight: bold;");
        Label createdValueLabel = new Label(
            file.getCreatedAt() != null 
                ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(file.getCreatedAt())
                : "N/A"
        );
        createdValueLabel.getStyleClass().add("file-metadata");
        
        // Last accessed date
        Label accessedLabel = new Label("Last Accessed:");
        accessedLabel.getStyleClass().add("file-metadata");
        accessedLabel.setStyle("-fx-font-weight: bold;");
        Label accessedValueLabel = new Label(
            file.getLastAccessedAt() != null 
                ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(file.getLastAccessedAt())
                : "N/A"
        );
        accessedValueLabel.getStyleClass().add("file-metadata");
        
        // File path
        Label pathLabel = new Label("Path:");
        pathLabel.getStyleClass().add("file-metadata");
        pathLabel.setStyle("-fx-font-weight: bold;");
        Label pathValueLabel = new Label(file.getFilePath());
        pathValueLabel.getStyleClass().add("file-metadata");
        
        // Add to grid
        detailsGrid.add(createdLabel, 0, 0);
        detailsGrid.add(createdValueLabel, 1, 0);
        detailsGrid.add(accessedLabel, 0, 1);
        detailsGrid.add(accessedValueLabel, 1, 1);
        detailsGrid.add(pathLabel, 0, 2);
        detailsGrid.add(pathValueLabel, 1, 2);
        
        // Add all to expanded content
        expandedContent.getChildren().add(detailsGrid);
        
        // Create titled pane
        TitledPane filePanel = new TitledPane();
        filePanel.setGraphic(header);
        filePanel.setContent(expandedContent);
        filePanel.setExpanded(false);
        filePanel.setText(null); // Remove default text
        filePanel.setUserData(file); // Store the file object for reference
        filePanel.getStyleClass().add("file-panel");
        filePanel.setMaxWidth(Double.MAX_VALUE); // Make panel expand to full width
        
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
                        // File is missing - apply missing file CSS class
                        filePanel.getStyleClass().add("missing-file-panel");
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

    // Helper method to create an ImageView from an image file
    private ImageView createIcon(String imagePath, int size) {
        try {
            File iconFile = new File(imagePath);
            if (iconFile.exists()) {
                Image image = new Image(iconFile.toURI().toString());
                ImageView imageView = new ImageView(image);
                imageView.setFitHeight(size);
                imageView.setFitWidth(size);
                imageView.setPreserveRatio(true);
                return imageView;
            } else {
                System.err.println("Icon not found: " + imagePath);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + e.getMessage());
            return null;
        }
    }
}
