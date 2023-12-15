package com.flippingai;


import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import lombok.Getter;

@Getter
public class Suggestion {
    private final String type;
    private final int boxId;
    private final int itemId;
    private final int price;
    private final int quantity;
    private final String name;
    private final int waitTime;
    private final int id;

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
        JsonElement nullableText = jsonSuggestion.get("name");
        String name = (nullableText instanceof JsonNull) ? "" : nullableText.getAsString();
        int waitTime = jsonSuggestion.get("wait_time").getAsInt();
        int id = jsonSuggestion.get("command_id").getAsInt();
        return new Suggestion(type, boxId, itemId, price, quantity, name, waitTime, id);
    }

    @Override
    public String toString() {
        return "<html><b>Flipping AI Suggestion:</b>" +
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


