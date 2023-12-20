package com.flippingai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;


@Slf4j
public class SuggestionPanel extends PluginPanel {

    private final JLabel suggestionInfo = new JLabel();

    void init(FlippingAiConfig config) {
            setText("<html> Log in to get a flip suggestion <html>");

            suggestionInfo.setHorizontalAlignment(SwingConstants.CENTER);
            add(suggestionInfo, BorderLayout.CENTER);

    }

    void setText(String text) {
        suggestionInfo.setText(text);
    }

    void updateSuggestion(Suggestion suggestion) {
        NumberFormat formatter = NumberFormat.getNumberInstance();
        String suggestionString = "<html><center> <FONT COLOR=white><b>AI Suggestion:" +
                "</b></FONT><br><br>";

        switch (suggestion.getType()) {
            case "wait":
                suggestionString += "Wait for offers to complete... <br>";
                break;
            case "abort":
                suggestionString += "Abort offer for<br><FONT COLOR=white>" + suggestion.getName() + "</FONT>";
                break;
            case "buy":
            case "sell":
                suggestionString += suggestion.getType() +
                        " <FONT COLOR=yellow>" + formatter.format(suggestion.getQuantity()) + "</FONT><br>" +
                        "<FONT COLOR=white>" + suggestion.getName() + "</FONT><br>" +
                        "for <FONT COLOR=yellow>" + formatter.format(suggestion.getPrice()) + "</FONT> coins each<br>";
                break;
            default:
                suggestionString += "Error processing suggestion.<br>";
        }
        suggestionString += "</center><html>";
        setText(suggestionString);
    }

}
