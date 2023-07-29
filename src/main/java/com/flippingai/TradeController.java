package com.flippingai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class TradeController {


    String url;
    int accountId;

    public TradeController(String url, int accountId) {
        this.url = url;
        this.accountId = accountId;
    }

    public void postRestricted(int itemId, int accountId) throws IOException {
        JSONObject restrictedJson = new JSONObject()
                .put("item_id", itemId)
                .put("account_id", accountId);

        postJson(restrictedJson, "/restricted", false);
    }

    public JSONObject statusJson(Offer[] offers, RSItem[] items, int world, boolean previousResult,
                                 boolean sendPreviousResult) {
        /* create JSONObject with current status of offers and items */

        // convert offers to array of JSONObjects
        JSONObject[] offersJson = new JSONObject[offers.length];
        for(int i = 0; i < offers.length; i++) {
            offersJson[i] = offers[i].toJson();
        }

        // get total amount of each item
        Map<Integer, Integer> itemsAmount = Arrays.stream(items).filter(i -> i!=null)
                .collect(Collectors.groupingBy(RSItem::getId,
                        Collectors.summingInt(RSItem::getAmount)));



        // convert items to list of JSONObjects
        List<JSONObject> itemsJson = new ArrayList<JSONObject>(itemsAmount.size());
        for(Map.Entry<Integer, Integer> entry : itemsAmount.entrySet()) {
            JSONObject itemJson = new JSONObject()
                    .put("item_id", entry.getKey())
                    .put("amount", entry.getValue());
            itemsJson.add(itemJson);
        }

        JSONObject statusJson = new JSONObject()
                .put("account_id", accountId)
                .put("offers", offersJson)
                .put("items", itemsJson)
                .put("world", world);

        if (sendPreviousResult) {
            statusJson.put("pcr", previousResult);
        }

        return statusJson;
    }

    public JSONObject getCommand(Offer[] offers, RSItem[] items, int world, boolean previousSuccessful,
                                 boolean sendPreviousSuccessful) throws IOException {
        /* send status to trade controller and receive command*/

        JSONObject status = statusJson(offers, items, world, previousSuccessful, sendPreviousSuccessful);
        return postJson(status, "/command", true);
    }

    private JSONObject postJson(JSONObject json, String route, boolean jsonResponse)
            throws IOException {
        /* send http POST request with json body, and optionally accept json response */

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
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                return new JSONObject(sb.toString());
            } else {
                return null;
            }
        } else {

            throw new IOException();
        }

    }
}