package com.tagease.view;

import com.tagease.controller.TagController;
import com.tagease.model.Tag;
import com.tagease.model.TaggedFile;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MainView {
    // UI Components
    private Stage stage;
    private TagController controller;
    private Set<String> selectedTags = new HashSet<>();
    private FlowPane selectedTagsPane;
    private VBox fileListContainer;
    private ListView<Object> tagFilterBox;
    private String searchTerm = "";
    private String searchOption = "File Name";

    // Data
    private static final List<String> PREDEFINED_TAGS = List.of(
        "New", "Reference", "15m Test", "Midterm Exam", "Final Exam", "Done", "In Progress"
    );
    private static final String DEFAULT_TAG = "New";
    private ObservableList<TaggedFile> filesList;

    // Add a field to track the current sort settings
    private String currentSortOption = "File Name";
    private boolean currentSortAscending = true;

    // Add a field to track if the application is initializing
    private boolean isInitializing = true;
    
    // Map to store tag colors
    private Map<String, Tag> tagColorMap = new HashMap<>();

    public MainView(Stage stage, TagController controller) {
        this.stage = stage;
        this.controller = controller;
        this.filesList = FXCollections.observableArrayList();
        
        initialize();
    }

    private void initialize() {
        isInitializing = true;
        
        // Load tag colors
        loadTagColors();
        
        // Check for missing files and update tags accordingly
        controller.checkForMissingFiles();
        
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Create the main horizontal layout
        HBox mainLayout = new HBox();
        mainLayout.getStyleClass().add("main-layout");
        
        // Create sidebar
        VBox sidebar = createSidebar();
        
        // Add components to the main layout
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
        
        // Make sure to update the selected tags display initially
        updateSelectedTagsDisplay();
        
        // Set initialization complete
        Platform.runLater(() -> {
            isInitializing = false;
        });
    }

    private void showTagManagementWindow() {
        Stage tagWindow = new Stage();
        tagWindow.setTitle("Manage Tags");
        tagWindow.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.getStyleClass().add("custom-dialog");

        // Title
        Label titleLabel = new Label("Manage Tags");
        titleLabel.getStyleClass().add("title-label");
        
        // Add Tag Section
        Label addTagLabel = new Label("Add New Tag");
        addTagLabel.getStyleClass().add("section-label");
        
        HBox addTagBox = new HBox(10);
        addTagBox.setAlignment(Pos.CENTER_LEFT);
        TextField newTagField = new TextField();
        newTagField.setPromptText("Enter new tag");
        newTagField.setPrefWidth(250);
        
        Button addTagButton = new Button("Add Tag");
        addTagButton.getStyleClass().add("ok-button");
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
        Label removeTagLabel = new Label("Remove Existing Tag");
        removeTagLabel.getStyleClass().add("section-label");
        
        HBox removeTagBox = new HBox(10);
        removeTagBox.setAlignment(Pos.CENTER_LEFT);
        ComboBox<String> tagSelector = new ComboBox<>();
        tagSelector.setPromptText("Select tag to remove");
        tagSelector.setItems(FXCollections.observableArrayList(controller.getAllTags()));
        tagSelector.setPrefWidth(250);
        
        Button removeTagButton = new Button("Remove Tag");
        removeTagButton.getStyleClass().add("cancel-button");
        removeTagButton.setOnAction(e -> {
            String selectedTag = tagSelector.getSelectionModel().getSelectedItem();
            if (selectedTag != null) {
                controller.removeTag(selectedTag);
                tagSelector.getItems().remove(selectedTag);
                updateTagFilterBox();
            }
        });
        removeTagBox.getChildren().addAll(tagSelector, removeTagButton);

        // Close button at the bottom
        Button closeButton = new Button("Close");
        closeButton.setPrefWidth(100);
        closeButton.setOnAction(e -> tagWindow.close());
        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));

        layout.getChildren().addAll(
            titleLabel,
            addTagLabel, addTagBox,
            removeTagLabel, removeTagBox,
            buttonBox
        );

        Scene scene = new Scene(layout, 450, 300);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        tagWindow.setScene(scene);
        tagWindow.showAndWait();
    }

    private void updateTagFilterBox() {
        try {
            // Get all tags with colors
            tagColorMap = controller.getAllTagsWithColors();
            
            // Update the filter box
            Set<String> allTags = tagColorMap.keySet();
            tagFilterBox.getItems().clear();
            
            for (String tagName : allTags) {
                CheckBox checkBox = new CheckBox(tagName);
                checkBox.setSelected(selectedTags.contains(tagName));
                
                // Get tag color
                Tag tag = tagColorMap.get(tagName);
                if (tag != null) {
                    // Create a colored rectangle for the tag
                    Rectangle colorRect = new Rectangle(12, 12);
                    colorRect.setFill(tag.getColor());
                    colorRect.setStroke(Color.GRAY);
                    colorRect.setStrokeWidth(1);
                    colorRect.setArcWidth(4);
                    colorRect.setArcHeight(4);
                    
                    // Create an HBox with the color and checkbox
                    HBox tagItem = new HBox(5, colorRect, checkBox);
                    tagItem.setAlignment(Pos.CENTER_LEFT);
                    
                    // Add to the filter box
                    tagFilterBox.getItems().add(tagItem);
                } else {
                    // Fallback if no color is found
                    tagFilterBox.getItems().add(checkBox);
                }
                
                checkBox.setOnAction(e -> {
                    if (checkBox.isSelected()) {
                        selectedTags.add(tagName);
                    } else {
                        selectedTags.remove(tagName);
                    }
                    updateSelectedTagsDisplay();
                    refreshTable();
                });
            }
        } catch (RuntimeException e) {
            showErrorDialog("Error", "Failed to load tags", e.getMessage());
        }
    }

    private void updateSelectedTagsDisplay() {
        selectedTagsPane.getChildren().clear();
        
        if (selectedTags.isEmpty()) {
            Label noTagsLabel = new Label("No tags selected");
            noTagsLabel.getStyleClass().add("no-tags-selected");
            selectedTagsPane.getChildren().add(noTagsLabel);
            return;
        }
        
        for (String tagName : selectedTags) {
            HBox tagBox = new HBox(5);
            tagBox.setAlignment(Pos.CENTER_LEFT);
            tagBox.getStyleClass().add("selected-tag");
            
            // Get tag color
            Tag tag = tagColorMap.get(tagName);
            String colorHex = (tag != null) ? tag.getColorHex() : "#2196F3"; // Default blue
            
            // Apply the tag's color to the tag pill
            tagBox.setStyle("-fx-background-color: " + colorHex + "33;"); // 33 = 20% opacity
            
            Label tagLabel = new Label(tagName);
            tagLabel.setStyle("-fx-text-fill: " + colorHex + ";");
            
            Button removeButton = new Button("×");
            removeButton.getStyleClass().add("remove-tag-button");
            removeButton.setOnAction(e -> {
                selectedTags.remove(tagName);
                updateSelectedTagsDisplay();
                refreshTable();
            });
            
            tagBox.getChildren().addAll(tagLabel, removeButton);
            selectedTagsPane.getChildren().add(tagBox);
        }
    }

    private VBox createSearchSection() {
        VBox searchContainer = new VBox(10);
        searchContainer.getStyleClass().add("search-container");
        searchContainer.setPadding(new Insets(15));
        
        // ---- SEARCH SECTION (TOP ROW) ----
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("control-section");
        
        // Search icon
        ImageView searchIcon = createIcon("src/main/resources/Image/magnifier_868231.png", 24);
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
        
        // ---- FILTER AND SORT ROW (BOTTOM ROW) ----
        HBox filterSortRow = new HBox(20);
        filterSortRow.setAlignment(Pos.CENTER_LEFT);
        
        // ---- FILTER SECTION ----
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.getStyleClass().add("control-section");
        filterBox.setPrefWidth(450);
        HBox.setHgrow(filterBox, Priority.SOMETIMES);
        
        // Filter icon
        ImageView filterIcon = createIcon("src/main/resources/Image/filter_679890.png", 24);
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
        
        filterBox.getChildren().addAll(filterLabel, tagLabel, tagComboBox, clearAllButton);
        
        // ---- SORT SECTION ----
        HBox sortBox = new HBox(10);
        sortBox.setAlignment(Pos.CENTER_LEFT);
        sortBox.getStyleClass().add("control-section");
        sortBox.setPrefWidth(450);
        HBox.setHgrow(sortBox, Priority.SOMETIMES);
        
        // Sort icon
        ImageView sortIcon = createIcon("src/main/resources/Image/swap_5296690.png", 24);
        Label sortLabel;
        if (sortIcon != null) {
            sortLabel = new Label("Sort by:", sortIcon);
            sortLabel.setGraphicTextGap(5);
        } else {
            sortLabel = new Label("Sort by:");
        }
        sortLabel.getStyleClass().add("section-label");
        
        // Sort options
        ComboBox<String> sortOptions = new ComboBox<>();
        sortOptions.getItems().addAll("File Name", "Created Date", "Last Accessed");
        sortOptions.setValue("File Name");
        sortOptions.getStyleClass().add("combo-box");
        
        // Sort direction toggle
        ToggleGroup sortDirectionGroup = new ToggleGroup();
        
        RadioButton ascendingSort = new RadioButton("Asc");
        ascendingSort.setToggleGroup(sortDirectionGroup);
        ascendingSort.setSelected(true);
        ascendingSort.getStyleClass().add("sort-direction-toggle");
        
        RadioButton descendingSort = new RadioButton("Desc");
        descendingSort.setToggleGroup(sortDirectionGroup);
        descendingSort.getStyleClass().add("sort-direction-toggle");
        
        sortBox.getChildren().addAll(sortLabel, sortOptions, ascendingSort, descendingSort);
        
        // Add filter and sort to the bottom row
        filterSortRow.getChildren().addAll(filterBox, sortBox);
        
        // Selected tags display in a separate row
        FlowPane selectedTagsPane = new FlowPane();
        selectedTagsPane.setHgap(5);
        selectedTagsPane.setVgap(5);
        selectedTagsPane.setPadding(new Insets(5));
        selectedTagsPane.getStyleClass().add("selected-tags-pane");
        
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
        
        // Sort option listener
        sortOptions.valueProperty().addListener((observable, oldValue, newValue) -> {
            sortFiles(newValue, ascendingSort.isSelected());
        });
        
        // Sort direction listeners
        ascendingSort.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                sortFiles(sortOptions.getValue(), true);
            }
        });
        
        descendingSort.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                sortFiles(sortOptions.getValue(), false);
            }
        });
        
        // Add all components to search container
        searchContainer.getChildren().addAll(searchBox, filterSortRow, selectedTagsPane);
        
        return searchContainer;
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
            
            // Apply dark theme to dialog
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            dialogPane.getStyleClass().add("dialog-pane");
            
            // Create the dialog content
            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.getStyleClass().add("custom-dialog");
            
            // Tags section
            Label tagsLabel = new Label("Tags (comma-separated):");
            tagsLabel.getStyleClass().add("section-label");
            
            TextField tagsField = new TextField();
            tagsField.setPrefHeight(35);
            
            // Add tag suggestions
            Label suggestionsLabel = new Label("Suggested Tags:");
            suggestionsLabel.getStyleClass().add("section-label");
            
            FlowPane tagSuggestions = new FlowPane();
            tagSuggestions.setHgap(8);
            tagSuggestions.setVgap(8);
            tagSuggestions.setPadding(new Insets(5));
            
            // Add predefined tags
            for (String tag : PREDEFINED_TAGS) {
                Button tagButton = new Button(tag);
                tagButton.getStyleClass().add("tag-button");
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
                    tagButton.getStyleClass().add("tag-button");
                    tagButton.setOnAction(e -> {
                        String currentTags = tagsField.getText();
                        tagsField.setText(currentTags.isEmpty() ? tag : currentTags + ", " + tag);
                    });
                    tagSuggestions.getChildren().add(tagButton);
                }
            }
            
            content.getChildren().addAll(tagsLabel, tagsField, suggestionsLabel, tagSuggestions);
            dialogPane.setContent(content);
            
            // Add buttons
            ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialogPane.getButtonTypes().addAll(addButton, cancelButton);
            
            // Style the buttons
            Button addButtonNode = (Button) dialogPane.lookupButton(addButton);
            addButtonNode.getStyleClass().add("ok-button");
            
            Button cancelButtonNode = (Button) dialogPane.lookupButton(cancelButton);
            cancelButtonNode.getStyleClass().add("cancel-button");
            
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

        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("custom-dialog");

        // Current tags section
        Label currentTagsLabel = new Label("Current Tags:");
        currentTagsLabel.getStyleClass().add("section-label");

        FlowPane currentTagsPane = new FlowPane();
        currentTagsPane.setHgap(8);
        currentTagsPane.setVgap(8);
        currentTagsPane.setPadding(new Insets(5));

        Set<String> fileTags = new HashSet<>(file.getTags());
        for (String tagName : fileTags) {
            // Get tag color
            Tag tag = tagColorMap.get(tagName);
            String colorHex = (tag != null) ? tag.getColorHex() : "#2196F3"; // Default blue
            
            Label tagLabel = new Label(tagName);
            tagLabel.getStyleClass().add("tag-pill");
            
            // Apply the tag's specific color
            tagLabel.setStyle("-fx-background-color: " + colorHex + ";");
            
            // For dark colors, use white text; for light colors, use dark text
            if (tag != null && isColorDark(tag.getColor())) {
                tagLabel.setTextFill(javafx.scene.paint.Color.WHITE);
            } else {
                tagLabel.setTextFill(javafx.scene.paint.Color.rgb(33, 33, 33));
            }
            
            currentTagsPane.getChildren().add(tagLabel);
        }

        // Edit tags section
        Label editTagsLabel = new Label("Edit Tags (comma-separated):");
        editTagsLabel.getStyleClass().add("section-label");

        TextField tagsField = new TextField();
        tagsField.setText(String.join(", ", fileTags));
        tagsField.setPrefHeight(35);

        // Available tags section
        Label availableTagsLabel = new Label("Available Tags:");
        availableTagsLabel.getStyleClass().add("section-label");

        FlowPane availableTagsPane = new FlowPane();
        availableTagsPane.setHgap(8);
        availableTagsPane.setVgap(8);
        availableTagsPane.setPadding(new Insets(5));

        // Add all available tags
        Set<String> allTagNames = tagColorMap.keySet();
        for (String tagName : allTagNames) {
            // Get tag color
            Tag tag = tagColorMap.get(tagName);
            String colorHex = (tag != null) ? tag.getColorHex() : "#2196F3"; // Default blue
            
            Button tagButton = new Button(tagName);
            tagButton.getStyleClass().add("tag-button");
            
            // Apply the tag's specific color
            tagButton.setStyle("-fx-text-fill: " + colorHex + "; -fx-border-color: " + colorHex + ";");
            
            tagButton.setOnAction(e -> {
                String currentTags = tagsField.getText();
                // Check if tag is already in the list
                if (!currentTags.contains(tagName)) {
                    tagsField.setText(currentTags.isEmpty() ? tagName : currentTags + ", " + tagName);
                }
            });
            availableTagsPane.getChildren().add(tagButton);
        }

        content.getChildren().addAll(
            currentTagsLabel, currentTagsPane,
            editTagsLabel, tagsField,
            availableTagsLabel, availableTagsPane
        );

        dialogPane.setContent(content);

        // Add buttons
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(saveButton, cancelButton);
        
        // Style the buttons
        Button saveButtonNode = (Button) dialogPane.lookupButton(saveButton);
        saveButtonNode.getStyleClass().add("ok-button");
        
        Button cancelButtonNode = (Button) dialogPane.lookupButton(cancelButton);
        cancelButtonNode.getStyleClass().add("cancel-button");

        // Set the result converter
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                // Parse the tags from the text field
                Set<String> newTags = new HashSet<>();
                String inputTags = tagsField.getText();
                if (!inputTags.trim().isEmpty()) {
                    for (String tag : inputTags.split(",")) {
                        String trimmedTag = tag.trim();
                        if (!trimmedTag.isEmpty()) {
                            newTags.add(trimmedTag);
                        }
                    }
                }

                // Update the file's tags
                file.setTags(newTags);
                controller.updateFileTags(file);
                refreshTable();
            }
            return buttonType;
        });

        // Show dialog
        dialog.showAndWait();
    }

    private void refreshTable() {
        // Skip if still initializing
        if (isInitializing && fileListContainer == null) {
            return;
        }
        
        // Get all files from the controller
        List<TaggedFile> allFiles = controller.getAllFiles();
        
        // Create a temporary list for filtering
        List<TaggedFile> filteredFiles = new ArrayList<>();
        
        // Filter files based on search term and selected tags
        for (TaggedFile file : allFiles) {
            if (matchesSearchCriteria(file) && isFileMatchingFilters(file)) {
                filteredFiles.add(file);
            }
        }
        
        // Apply current sort
        sortFileList(filteredFiles, currentSortOption, currentSortAscending);
        
        // Update the UI
        updateFileListDisplay(filteredFiles);
        
        // Make sure to update the selected tags display
        updateSelectedTagsDisplay();
    }
    
    private void updateFileListDisplay(List<TaggedFile> filesToDisplay) {
        // Run on JavaFX thread to avoid concurrency issues
        Platform.runLater(() -> {
            fileListContainer.getChildren().clear();
            
            if (filesToDisplay.isEmpty()) {
                Label noFilesLabel = new Label("No files found.");
                noFilesLabel.getStyleClass().add("no-files-label");
                fileListContainer.getChildren().add(noFilesLabel);
            } else {
                for (TaggedFile file : filesToDisplay) {
                    TitledPane filePanel = createFilePanel(file);
                    fileListContainer.getChildren().add(filePanel);
                }
            }
        });
    }
    
    private void sortFiles(String sortOption, boolean ascending) {
        // Update current sort settings
        this.currentSortOption = sortOption;
        this.currentSortAscending = ascending;
        
        // Get all files from the controller
        List<TaggedFile> allFiles = controller.getAllFiles();
        
        // Create a temporary list for filtering
        List<TaggedFile> filteredFiles = new ArrayList<>();
        
        // Filter files based on search term and selected tags
        for (TaggedFile file : allFiles) {
            if (matchesSearchCriteria(file) && isFileMatchingFilters(file)) {
                filteredFiles.add(file);
            }
        }
        
        // Apply sort
        sortFileList(filteredFiles, sortOption, ascending);
        
        // Update the UI
        updateFileListDisplay(filteredFiles);
    }
    
    private void sortFileList(List<TaggedFile> files, String sortOption, boolean ascending) {
        if (files == null || files.isEmpty()) {
            return;
        }
        
        Comparator<TaggedFile> comparator = null;
        
        switch (sortOption) {
            case "File Name":
                comparator = Comparator.comparing(TaggedFile::getFileName, String.CASE_INSENSITIVE_ORDER);
                break;
            case "Created Date":
                comparator = Comparator.comparing(TaggedFile::getCreatedAt);
                break;
            case "Last Accessed":
                comparator = Comparator.comparing(TaggedFile::getLastAccessedAt);
                break;
            default:
                comparator = Comparator.comparing(TaggedFile::getFileName, String.CASE_INSENSITIVE_ORDER);
        }
        
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        // Sort the list
        files.sort(comparator);
    }

    private TitledPane createFilePanel(TaggedFile file) {
        // Create the main content for the collapsed state
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 15, 10, 15)); // Increased padding for better spacing
        header.setPrefWidth(Control.USE_COMPUTED_SIZE); // Make panel fit window width
        header.setMaxWidth(Double.MAX_VALUE); // Allow expansion to full width
        header.setSpacing(15); // Add spacing between elements for better readability
        
        // Action buttons with icons - on the left side
        HBox actionButtons = new HBox(10);  // Spacing between buttons
        actionButtons.setAlignment(Pos.CENTER_LEFT);
        
        // Edit tags button with icon
        Button editTagsButton = new Button();
        editTagsButton.getStyleClass().addAll("icon-button", "edit-button");
        editTagsButton.setPrefSize(30, 30);
        editTagsButton.setMinSize(30, 30);
        editTagsButton.setMaxSize(30, 30);
        ImageView editIcon = createIcon("src/main/resources/Image/pencil_505210.png", 16);
        if (editIcon != null) {
            editTagsButton.setGraphic(editIcon);
        } else {
            editTagsButton.setText("✏️");
        }
        editTagsButton.setTooltip(new Tooltip("Edit Tags"));
        editTagsButton.setOnAction(e -> editFileTags(file));
        
        // Open file button with icon
        Button openFileButton = new Button();
        openFileButton.getStyleClass().addAll("icon-button", "open-button");
        openFileButton.setPrefSize(30, 30);
        openFileButton.setMinSize(30, 30);
        openFileButton.setMaxSize(30, 30);
        ImageView openIcon = createIcon("src/main/resources/Image/folder_3767084.png", 16);
        if (openIcon != null) {
            openFileButton.setGraphic(openIcon);
        } else {
            openFileButton.setText("📁");
        }
        openFileButton.setTooltip(new Tooltip("Open File"));
        openFileButton.setOnAction(e -> openFile(file));
        
        // Delete button with icon
        Button deleteFileButton = new Button();
        deleteFileButton.getStyleClass().addAll("icon-button", "delete-button");
        deleteFileButton.setPrefSize(30, 30);
        deleteFileButton.setMinSize(30, 30);
        deleteFileButton.setMaxSize(30, 30);
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
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                controller.deleteFile(file.getFilePath());
                refreshTable();
            }
        });
        
        actionButtons.getChildren().addAll(editTagsButton, openFileButton, deleteFileButton);
        
        // File name label with larger font
        Label fileNameLabel = new Label(file.getFileName());
        fileNameLabel.getStyleClass().add("file-name");
        fileNameLabel.setPrefWidth(300);
        fileNameLabel.setMinWidth(200);
        
        // Status indicator based on tags - moved to the right of file name
        Region statusIndicator = new Region();
        statusIndicator.getStyleClass().addAll("status-indicator");
        statusIndicator.setMinSize(8, 16);
        statusIndicator.setPrefSize(8, 16);
        statusIndicator.setMaxSize(8, 16);
        
        // Check if file exists
        File fileObj = new File(file.getFilePath());
        if (!fileObj.exists()) {
            // Missing file - red indicator
            statusIndicator.getStyleClass().add("status-missing");
            // We'll add the missing-file class to the file panel later
        } else if (file.getTags().contains("Done")) {
            statusIndicator.getStyleClass().add("status-done");
        } else if (file.getTags().contains("In Progress")) {
            statusIndicator.getStyleClass().add("status-in-progress");
        } else {
            statusIndicator.getStyleClass().add("status-new");
        }
        
        // Tags flow pane - horizontal layout
        FlowPane tagsPane = new FlowPane();
        tagsPane.setHgap(8); // Increased gap for better visuals
        tagsPane.setVgap(8);
        tagsPane.setPadding(new Insets(2)); // Small padding around tags
        HBox.setHgrow(tagsPane, Priority.ALWAYS); // Allow tags pane to take available horizontal space
        
        for (String tagName : file.getTags()) {
            // Get tag color
            Tag tag = tagColorMap.get(tagName);
            String colorHex = (tag != null) ? tag.getColorHex() : "#2196F3"; // Default blue
            
            Label tagLabel = new Label(tagName);
            tagLabel.getStyleClass().add("tag-pill");
            
            // Apply the tag's specific color
            tagLabel.setStyle("-fx-background-color: " + colorHex + ";");
            
            // For dark colors, use white text; for light colors, use dark text
            if (tag != null && isColorDark(tag.getColor())) {
                tagLabel.setTextFill(javafx.scene.paint.Color.WHITE);
            } else {
                tagLabel.setTextFill(javafx.scene.paint.Color.rgb(33, 33, 33));
            }
            
            tagsPane.getChildren().add(tagLabel);
        }
        
        // Add all components to header in the correct order
        header.getChildren().addAll(actionButtons, fileNameLabel, statusIndicator, tagsPane);
        
        // Create titled pane with the header
        TitledPane filePanel = new TitledPane();
        filePanel.setGraphic(header);
        filePanel.setText(null); // No text in the title bar, using custom header
        filePanel.setExpanded(false);
        filePanel.setAnimated(true);
        filePanel.getStyleClass().add("file-panel");
        
        // Add missing-file class to the file panel if the file doesn't exist
        if (!fileObj.exists()) {
            filePanel.getStyleClass().add("missing-file");
        }
        
        // Create content for expanded state
        VBox expandedContent = new VBox(10);
        expandedContent.setPadding(new Insets(10, 15, 15, 15));
        
        // File path
        Label pathLabel = new Label("Path: " + file.getFilePath());
        pathLabel.getStyleClass().add("file-metadata");
        
        // File size
        String fileSize = "Unknown";
        if (fileObj.exists()) {
            long size = fileObj.length();
            if (size < 1024) {
                fileSize = size + " B";
            } else if (size < 1024 * 1024) {
                fileSize = String.format("%.2f KB", size / 1024.0);
            } else {
                fileSize = String.format("%.2f MB", size / (1024.0 * 1024.0));
            }
        }
        Label sizeLabel = new Label("Size: " + fileSize);
        sizeLabel.getStyleClass().add("file-metadata");
        
        // Last modified
        String lastModified = "Unknown";
        if (fileObj.exists()) {
            lastModified = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(fileObj.lastModified()));
        }
        Label modifiedLabel = new Label("Last Modified: " + lastModified);
        modifiedLabel.getStyleClass().add("file-metadata");
        
        expandedContent.getChildren().addAll(pathLabel, sizeLabel, modifiedLabel);
        filePanel.setContent(expandedContent);
        
        return filePanel;
    }

    private void openFile(TaggedFile file) {
        try {
            File fileToOpen = new File(file.getFilePath());
            if (fileToOpen.exists()) {
                // Update last accessed time
                file.updateLastAccessed();
                controller.updateFileTags(file);
                
                // Open the file with the default system application
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    new ProcessBuilder("cmd", "/c", fileToOpen.getAbsolutePath()).start();
                } else {
                    new ProcessBuilder("xdg-open", fileToOpen.getAbsolutePath()).start();
                }
            } else {
                showErrorDialog("File Not Found", 
                        "Could not open file", 
                        "The file " + file.getFileName() + " does not exist.");
            }
        } catch (IOException e) {
            showErrorDialog("Error Opening File", 
                    "Could not open file", 
                    e.getMessage());
        }
    }

    private void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        // Apply dark theme to alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");
        
        alert.showAndWait();
    }

    private boolean isFileMatchingFilters(TaggedFile file) {
        if (selectedTags.isEmpty()) {
            return true;
        }
        
        for (String tag : selectedTags) {
            if (file.getTags().contains(tag)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean matchesSearchCriteria(TaggedFile file) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return true;
        }
        
        String term = searchTerm.toLowerCase();
        if (searchOption.equals("File Name")) {
            return file.getFileName().toLowerCase().contains(term);
        } else if (searchOption.equals("Tags")) {
            return file.getTags().stream()
                    .anyMatch(tag -> tag.toLowerCase().contains(term));
        } else if (searchOption.equals("Path")) {
            return file.getFilePath().toLowerCase().contains(term);
        }
        return false;
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

    /**
     * Loads all tags with their colors from the controller.
     */
    private void loadTagColors() {
        try {
            tagColorMap = controller.getAllTagsWithColors();
        } catch (Exception e) {
            showErrorDialog("Error", "Failed to load tag colors", e.getMessage());
        }
    }

    /**
     * Determines if a color is dark (to decide whether to use white or black text).
     * 
     * @param color The color to check
     * @return true if the color is dark, false otherwise
     */
    private boolean isColorDark(javafx.scene.paint.Color color) {
        // Calculate perceived brightness using the formula:
        // (0.299*R + 0.587*G + 0.114*B)
        double brightness = color.getRed() * 0.299 + 
                           color.getGreen() * 0.587 + 
                           color.getBlue() * 0.114;
        
        // If brightness is less than 0.5, consider it dark
        return brightness < 0.5;
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

    private VBox createSidebar() {
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
        ImageView addFileIcon = createIcon("src/main/resources/Image/file_338043.png", 24);
        if (addFileIcon != null) {
            addFileButton.setGraphic(addFileIcon);
            addFileButton.setGraphicTextGap(10);
        }
        addFileButton.setOnAction(e -> addFile());
        
        // Manage Tags button with icon
        Button manageTagsButton = new Button("Manage Tags");
        manageTagsButton.getStyleClass().add("sidebar-button");
        manageTagsButton.setMaxWidth(Double.MAX_VALUE);
        ImageView manageTagsIcon = createIcon("src/main/resources/Image/file_1346913.png", 24);
        if (manageTagsIcon != null) {
            manageTagsButton.setGraphic(manageTagsIcon);
            manageTagsButton.setGraphicTextGap(10);
        }
        manageTagsButton.setOnAction(e -> showTagManagementWindow());
        
        sidebar.getChildren().addAll(appTitle, addFileButton, manageTagsButton);
        return sidebar;
    }
}
