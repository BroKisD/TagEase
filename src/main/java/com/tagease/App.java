package com.tagease;

import com.tagease.view.MainView;
import com.tagease.controller.TagController;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        TagController controller = new TagController();
        MainView mainView = new MainView(primaryStage, controller);
        mainView.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}