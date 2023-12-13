package com.flippingai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;


@Slf4j
public class SuggestionPanel extends PluginPanel {

    private final JLabel suggestionInfo = new JLabel();

    void init(FlippingAiConfig config) {
            // Set the text for the JLabel
            setText("<html> Open the Grand Exchange to get a<br>" +
                    "flipping suggestion <html>");

            // Add the JLabel to the PluginPanel
            add(suggestionInfo, BorderLayout.CENTER);

    }

    void setText(String text) {
        suggestionInfo.setText(text);
    }

}
