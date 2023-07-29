package com.flippingai;

import net.runelite.api.Item;
import net.runelite.client.game.ItemManager;

import java.util.ArrayList;

public class RSItem {
    int id;
    int amount;

    RSItem(int id, int amount) {
        this.id = id;
        this.amount = amount;
    }

    int getId() {
        return id;
    }

    int getAmount() {
        return amount;
    }
}