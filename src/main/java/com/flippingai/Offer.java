package com.flippingai;

import com.google.gson.JsonObject;
import lombok.Getter;

@Getter
public class Offer {
    String status = "empty";
    int itemId = 0;
    int price = 0;
    int amountTotal = 0;
    int amountSpent = 0;
    int amountTraded = 0;
    int itemsToCollect = 0;
    int gpToCollect = 0;
    int boxId = 0;
    boolean active = false;

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        jsonObject.addProperty("item_id", itemId);
        jsonObject.addProperty("price", price);
        jsonObject.addProperty("amount_total", amountTotal);
        jsonObject.addProperty("amount_spent", amountSpent);
        jsonObject.addProperty("amount_traded", amountTraded);
        jsonObject.addProperty("box_id", boxId);
        jsonObject.addProperty("active", active);
        return jsonObject;
    }

    public boolean equals(Object o) {
        if (!(o instanceof  Offer)) return false;
        Offer offer = (Offer) o;
        return offer.status.equals(this.status)
                && offer.itemId == this.itemId
                && offer.price == this.price
                && offer.amountTotal == this.amountTotal
                && offer.amountTraded == this.amountTraded
                && offer.boxId == this.boxId
                && offer.active == this.active;
    }
}
