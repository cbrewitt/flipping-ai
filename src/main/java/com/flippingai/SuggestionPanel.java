package com.flippingai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;


@Slf4j
public class SuggestionPanel extends PluginPanel {

    private final JLabel suggestionInfo = new JLabel();
    private final JLabel spinnerWrapper = new JLabel();
    private final JPanel spinnerPlaceholder = new JPanel();


    void init(FlippingAiConfig config) {
        suggestLogin();
        suggestionInfo.setHorizontalAlignment(SwingConstants.CENTER);
        setIcon(IconTextField.Icon.LOADING);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Configure the placeholder panel
        spinnerPlaceholder.setPreferredSize(new Dimension(30, 30)); // Same size as spinner
        spinnerPlaceholder.setOpaque(false); // Make it invisible
        add(spinnerPlaceholder, gbc);

        // Configure constraints for spinnerWrapper
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 1;
        spinnerWrapper.setPreferredSize(new Dimension(30, 30)); // Adjust as needed
        add(spinnerWrapper, gbc);

        // Configure constraints for suggestionInfo
        gbc.gridy = 1; // Place it below the spinner
        suggestionInfo.setHorizontalAlignment(SwingConstants.CENTER);
        add(suggestionInfo, gbc);

        setIcon(IconTextField.Icon.LOADING);
        hideSpinner();

    }

    void showSpinner() {
        spinnerWrapper.setVisible(true);
        spinnerPlaceholder.setVisible(false);
    }

    void hideSpinner() {
        spinnerWrapper.setVisible(false);
        spinnerPlaceholder.setVisible(true);
    }

    void setIcon(IconTextField.Icon icon) {
        final ImageIcon imageIcon = new ImageIcon(new IconTextField().getClass().getResource(icon.getFile()));
        spinnerWrapper.setIcon(imageIcon);
    }

    void setText(String text) {
        suggestionInfo.setText(text);
    }

    void updateSuggestion(Suggestion suggestion) {
        NumberFormat formatter = NumberFormat.getNumberInstance();
        String suggestionString = "<html><center> <FONT COLOR=white><b>AI Suggestion" +
                "</b></FONT><br><br>";

        switch (suggestion.getType()) {
            case "wait":
                suggestionString += "Wait for offers to complete <br>";
                break;
            case "abort":
                suggestionString += "Abort offer for<br><FONT COLOR=white>" + suggestion.getName() + "</FONT>";
                break;
            case "buy":
            case "sell":
                String capitalisedAction = suggestion.getType().equals("buy") ? "Buy" : "Sell";
                suggestionString += capitalisedAction +
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

    void suggestCollect() {
        setText("<html><center> <FONT COLOR=white><b>AI Suggestion" +
                "</b></FONT><br><br>Collect items<br>");
    }

    void suggestLogin() {
        setText("<html><center> <FONT COLOR=white><b>AI Suggestion" +
                "</b></FONT><br><br> Log in to get a flip suggestion <br></center><html>");
    }
}
