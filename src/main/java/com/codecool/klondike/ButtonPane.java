package com.codecool.klondike;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class ButtonPane extends Pane {

    Button restartButton;

    public ButtonPane() {

        Image image = new Image("/button/reset.png");
        ImageView imageView = new ImageView(image);
        restartButton = new Button("",imageView);

        this.getChildren().add(restartButton);
    }

    public Button getRestartButton() {
        return restartButton;
    }

    public void setRestartButton(Button restartButton){
        this.restartButton = restartButton;
    }
}
