package com.flippingai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class TradeController {

    String url;
    int accountId = -1;

    private final Lock suggestionLock = new ReentrantLock();

    public TradeController(String url) {
        this.url = url;
    }

    public void resetAccountId() {
        this.accountId = -1;
    }

    public void postRestricted(int itemId, int accountId) throws IOException {
        JsonObject restrictedJson = new JsonObject();
        restrictedJson.addProperty("item_id", itemId);
        restrictedJson.addProperty("account_id", accountId);

        postJson(restrictedJson, "/restricted", false);
    }

    public JsonObject statusJson(Offer[] offers, RSItem[] items, boolean previousResult, boolean sendPreviousResult) {
        // convert offers to array of JsonObjects
        JsonArray offersJsonArray = new JsonArray();
        for(Offer offer : offers) {
            offersJsonArray.add(offer.toJson());
        }

        // get total amount of each item
        Map<Integer, Integer> itemsAmount = Arrays.stream(items).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(RSItem::getId,
                        Collectors.summingInt(RSItem::getAmount)));

        // uncollected items
        Map<Integer, Integer> uncollectedItemsAmount = Arrays.stream(offers).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Offer::getItemId,
                        Collectors.summingInt(Offer::getItemsToCollect)));

        // uncollected GP
        int totalGpToCollect = Arrays.stream(offers)
                .filter(Objects::nonNull)
                .mapToInt(Offer::getGpToCollect)
                .sum();

        itemsAmount.merge(995, totalGpToCollect, Integer::sum);
        uncollectedItemsAmount.forEach((key, value) -> itemsAmount.merge(key, value, Integer::sum));

        // remove items with zero quantity
        itemsAmount.entrySet().removeIf(entry -> entry.getValue() == 0);

        // convert items to list of JsonObjects

        JsonArray itemsJsonArray = new JsonArray();
        for(Map.Entry<Integer, Integer> entry : itemsAmount.entrySet()) {
            JsonObject itemJson = new JsonObject();
            itemJson.addProperty("item_id", entry.getKey());
            itemJson.addProperty("amount", entry.getValue());
            itemsJsonArray.add(itemJson);
        }

        JsonObject statusJson = new JsonObject();
        statusJson.addProperty("account_id", accountId);
        statusJson.add("offers", offersJsonArray);
        statusJson.add("items", itemsJsonArray);

        if (sendPreviousResult) {
            statusJson.addProperty("pcr", previousResult);
        }

        return statusJson;
    }

    public JsonObject getSuggestion(Offer[] offers, RSItem[] items, boolean previousSuccessful, boolean sendPreviousSuccessful) throws IOException {
        JsonObject status = statusJson(offers, items, previousSuccessful, sendPreviousSuccessful);
        suggestionLock.lock();
        JsonObject command = postJson(status, "/command", true);
        suggestionLock.unlock();
        return command;
    }

    public void getAccountId(String displayName) throws IOException {
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("display_name", displayName);

        JsonObject responseJson = postJson(requestJson, "/account-id", true);

        if (responseJson.has("account_id")) {
            accountId = responseJson.get("account_id").getAsInt();
        } else {
            throw new IOException("Account ID not found in the response");
        }
    }

    private JsonObject postJson(JsonElement json, String route, boolean jsonResponse) throws IOException {
        URL uri = new URL(url + route);
        HttpURLConnection con = (HttpURLConnection)uri.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        if(jsonResponse) {
            con.setRequestProperty("Accept", "application/json");
        }

        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(json.toString());
        wr.flush();

        int HttpResult = con.getResponseCode();

        if (HttpResult == HttpURLConnection.HTTP_OK) {
            if(jsonResponse) {
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                return JsonParser.parseString(sb.toString()).getAsJsonObject();
            } else {
                return new JsonObject(); // return an empty JSON object instead of null
            }
        } else {
            throw new IOException("Server returned HTTP response code: " + HttpResult + " for URL: " + uri);
        }
    }
}
