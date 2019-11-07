package com.codecool.klondike;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.*;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();
    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 2;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;

    private Stage primaryStage;

    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
        makeTopCardVisible();
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        if (draggedCards == null) {
            draggedCards = FXCollections.observableArrayList();
        }
        draggedCards = FXCollections.observableArrayList();
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        if (activePile.getPileType() == Pile.PileType.STOCK) {
            return;
        }
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        draggedCards = FXCollections.observableArrayList();
        if (!card.isFaceDown()) {
            if (activePile.getPileType() != Pile.PileType.DISCARD) {
                for (int i = activePile.getCards().indexOf(card); i < activePile.getCards().size(); i++) {
                    draggedCards.add(activePile.getCards().get(i));
                }
            } else {
                if (activePile.getPileType() == Pile.PileType.DISCARD) {
                    //   System.out.println("Card " + card.getShortName() + " to be removed from discard.");
                }
                draggedCards.add(card);
//                draggedCards.clear();
            }
        }

        for (int i = 0; i < draggedCards.size(); i++) {
            draggedCards.get(i).getDropShadow().setRadius(20);
            draggedCards.get(i).getDropShadow().setOffsetX(10);
            draggedCards.get(i).getDropShadow().setOffsetY(10);

            draggedCards.get(i).toFront();
            draggedCards.get(i).setTranslateX(offsetX);
            draggedCards.get(i).setTranslateY(offsetY);
        }

//        card.getDropShadow().setRadius(20);
//        card.getDropShadow().setOffsetX(10);
//        card.getDropShadow().setOffsetY(10);

//        card.toFront();
//        card.setTranslateX(offsetX);
//        card.setTranslateY(offsetY);
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards == null) {
            draggedCards = FXCollections.observableArrayList();
        }
        if (draggedCards.isEmpty()) {
            return;
        }
        Card card = (Card) e.getSource();

        Pile pile = getValidIntersectingPile(card, foundationPiles);
        if (pile == null) {
            pile = getValidIntersectingPile(card, tableauPiles);
        }


        //TODO
        if (pile != null) {
            handleValidMove(card, pile);
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards = null;
        }
        if (isGameWon()) {
            System.out.println("WON");
        }
    };

    public boolean isGameWon() {
        //TODO
        boolean isWon = false;
        for (int i = 0; i < foundationPiles.size(); i++) {
            if (foundationPiles.get(i).isEmpty() || foundationPiles.get(i).checkIfTopCardValueNotEquals(13)) {
                isWon = false;
            }
        }
        return isWon;
    }

    public Game(Stage primaryStage) {
        this.primaryStage = primaryStage;
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        System.out.println("Stock refilled from discard pile.");
        if (stockPile.isEmpty()) {
            for (Card card : discardPile.getCards()) {
                stockPile.addCard(card);
                card.flip();
            }
            stockPile.reversePile();
            discardPile.clear();
        }

    }

    public boolean isMoveValid(Card card, Pile destPile) {
        if (destPile.getPileType() == Pile.PileType.FOUNDATION) {
            if (destPile.isEmpty()) {   // Empty pile and card is an ace
                return card.getRank() == Rank.ACE;
            }

            Card topCard = destPile.getTopCard();
            if (Card.isSameSuit(card, topCard)) {
                return Rank.isNextRank(card, topCard);
            }
        }

        //Move to tableaus
        if (destPile.getPileType() == Pile.PileType.TABLEAU) {
            if (destPile.isEmpty()) {
                return card.getRank() == Rank.KING;
            }

            Card topCard = destPile.getTopCard();
            if (Card.isOppositeColor(card, topCard)) {
                return Rank.isPreviousRank(card, topCard);
            }
        }
        return false;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) && isOverPile(card, pile) && isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty()) {
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
    }
        else

    {
        return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

}

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);

            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards = FXCollections.observableArrayList();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i,
                    FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }

        for(Pile fp : foundationPiles) {
            fp.getCards().addListener((ListChangeListener<Card>) c -> {
                if (isGameWon()) {
                    System.out.println("YIPEE");

                    Platform.runLater(() -> {
                        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                        ButtonType cancel = new ButtonType("CLOSE", ButtonBar.ButtonData.CANCEL_CLOSE);
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"Do you want to restart?", ok, cancel);
                        alert.setTitle("WINNER!");
                        alert.setHeaderText("You have WON this game!");

                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.get() == ok){
                            primaryStage.close();
                            Klondike.initStage(primaryStage);
                        } else {
                            System.exit(0);
                        }
                    });
                }
            });
        }


        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }

//        ButtonPane buttonPane = new ButtonPane();
//        buttonPane.setLayoutX(25);
//        buttonPane.setLayoutY(20);
//        getChildren().add(buttonPane);
//        buttonPane.getRestartButton().setOnAction(ActionEvent -> {
//            System.out.println("Restart button pressed.");
//            primaryStage.close();
//            Klondike.initStage(primaryStage);
//        });
    }

    public void makeTopCardVisible() {
        for (int i = 0; i < tableauPiles.size(); i++) {
//            if (tableauPiles.get(i).getTopCard().isFaceDown()) {
                if (!tableauPiles.get(i).isEmpty() && tableauPiles.get(i).getTopCard().isFaceDown()) {
                    tableauPiles.get(i).getTopCard().flip();
                }
            }
        }

    public void dealCards() {
        Collections.shuffle(deck);
        int cardNumber = 0;
        for (int i = 0; i < tableauPiles.size(); i++) {
            for (int j = 0; j <= i; j++) {
                tableauPiles.get(i).addCard(deck.get(cardNumber++));
            }
        }
        while (cardNumber < deck.size()) {
            stockPile.addCard(deck.get(cardNumber++));
        }
        Iterator<Card> deckIterator = deck.iterator();
        deckIterator.forEachRemaining(card -> {
            addMouseEventHandlers(card);
            getChildren().add(card);
        });

        for (Pile i: tableauPiles) {
            i.getCards().addListener((ListChangeListener<Card>) c -> {
                if (!i.isEmpty() && i.getTopCard().isFaceDown()) {
                    i.getTopCard().flip();
                }
            });
        }
        makeTopCardVisible();
    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }
}
