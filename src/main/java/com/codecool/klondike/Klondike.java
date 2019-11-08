package com.codecool.klondike;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.awt.*;

public class Klondike extends Application {

    static void initStage(Stage primaryStage) {
        final double WINDOW_WIDTH = 1400;
        final double WINDOW_HEIGHT = 900;
        Card.loadCardImages();
        Game game = new Game(primaryStage);
        game.setTableBackground(new Image("/table/green.png"));
        primaryStage.setTitle("Klondike Solitaire");
        primaryStage.setScene(new Scene(game, WINDOW_WIDTH, WINDOW_HEIGHT));
        primaryStage.show();
        primaryStage.setMaximized(true);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Klondike.initStage(primaryStage);
    }
}