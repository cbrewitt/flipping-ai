package com.flippingai;

import org.json.JSONObject;

public class Offer {
    String status = "empty";
    int itemId = 0;
    int price = 0;
    int amountTotal = 0;
    int amountSpent = 0;
    int amountTraded = 0;
    int amountCollected = 0;
    int boxId = 0;
    boolean active = false;

    public JSONObject toJson() {
        return new JSONObject()
                .put("status", status)
                .put("item_id", itemId)
                .put("price", price)
                .put("amount_total", amountTotal)
                .put("amount_spent", amountSpent)
                .put("amount_traded", amountTraded)
                .put("box_id", boxId)
                .put("active", active);
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