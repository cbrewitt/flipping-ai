package com.flippingai;


import com.google.gson.JsonObject;

public class Suggestion {
    private String type;
    private int boxId;
    private int itemId;
    private int price;
    private int quantity;
    private String name;
    private int waitTime;
    private int id;

    Suggestion(String type, int boxId, int itemId, int price, int quantity, String name, int waitTime, int id) {
        this.type = type;
        this.boxId = boxId;
        this.itemId = itemId;
        this.price = price;
        this.quantity = quantity;
        this.name = name;
        this.waitTime = waitTime;
        this.id = id;
    }

    static Suggestion fromJson(JsonObject jsonSuggestion) {
        String type = jsonSuggestion.get("type").getAsString();

        int boxId = jsonSuggestion.get("box_id").getAsInt();
        int itemId = jsonSuggestion.get("item_id").getAsInt();
        int price = jsonSuggestion.get("price").getAsInt();
        int quantity = jsonSuggestion.get("quantity").getAsInt();
        String name = jsonSuggestion.get("name").getAsString();
        int waitTime = jsonSuggestion.get("wait_time").getAsInt();
        int id = jsonSuggestion.get("command_id").getAsInt();
        return new Suggestion(type, boxId, itemId, price, quantity, name, waitTime, id);
    }

    @Override
    public String toString() {
        return "<html> Suggestion:" +
                "<br>Type: " + type +
                "<br>Box ID: " + boxId +
                "<br>Item ID: " + itemId +
                "<br>Price: " + price +
                "<br>Quantity: " + quantity +
                "<br>Name: " + name +
                "<br>Wait Time: " + waitTime +
                "<br>ID: " + id + "<html>";
    }
}


