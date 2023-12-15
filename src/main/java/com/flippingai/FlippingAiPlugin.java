package com.flippingai;

import com.google.gson.JsonObject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static net.runelite.api.VarPlayer.CURRENT_GE_ITEM;


@Slf4j
@PluginDescriptor(name = "Flipping AI")
public class FlippingAiPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private FlippingAiConfig config;

	@Inject
	private ScheduledExecutorService executorService;

	private RSItem[] inventoryItems;
	private Offer[] offers;

	private TradeController tradeController;

	private boolean geOpen = false;

	private SuggestionPanel suggestionPanel;
	private NavigationButton navButton;

	@Inject
	@Getter
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	private final Lock suggestionLock = new ReentrantLock();
	private Timer suggestionTimer;

	private boolean suggestionNeeded = false;

	// Method to reset and start the timer
	private void resetSuggestionTimer() {
		if (suggestionTimer != null) {
			suggestionTimer.cancel();
		}
		suggestionTimer = new Timer();
		suggestionTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				suggestionNeeded = true;
			}
		}, 30000); // Schedule to run 30 seconds after reset
	}

	@Override
	protected void startUp() throws Exception {
		log.info("Flipping AI started!");
		tradeController = new TradeController("http://142.4.217.224:5000/trader", 2);

		suggestionPanel = injector.getInstance(SuggestionPanel.class);
		suggestionPanel.init(config);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "coflip-transparent-small.png");

		navButton = NavigationButton.builder()
				.tooltip("Flipping AI")
				.icon(icon)
				.priority(3)
				.panel(suggestionPanel)
				.build();

		clientToolbar.addNavigation(navButton);
		resetSuggestionTimer();
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Flipping AI stopped!");
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event) {
		updateOffers();
		suggestionNeeded = true;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
			log.info("Inventory changed");
			updateInventory();
		}
	}
	@Subscribe
	public void onGameTick(GameTick event) {
		if (suggestionNeeded) {
			getSuggestionAsync();
		}
	}

	@Provides
	FlippingAiConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(FlippingAiConfig.class);
	}

	private Widget getGeWidget() {
		return this.client.getWidget(WidgetInfo.GRAND_EXCHANGE_WINDOW_CONTAINER);
	}

	private Widget getGeSlotContainer() {
		return this.client.getWidget(465, 5);
	}

	public Boolean geOpen() {
		Widget geWidget = getGeWidget();
		return geWidget != null && !geWidget.isHidden();
	}

	void getSuggestion() {
		suggestionLock.lock();
		suggestionNeeded = false;
		try {
			if (offers != null && inventoryItems != null) {
				log.info("Getting suggestion");
				try {
					JsonObject suggestionJson = tradeController.getSuggestion(offers, inventoryItems, false, false);
					log.info("Received suggestion: " + suggestionJson.toString());
					Suggestion suggestion = Suggestion.fromJson(suggestionJson);
					suggestionPanel.updateSuggestion(suggestion);
				} catch (IOException e) {
					log.error("Error occurred while getting suggestion: " + e);
					suggestionPanel.setText("Failed to connect to server");
				}
			}
		} finally {
			suggestionLock.unlock();
			resetSuggestionTimer();
		}
	}

	private void getSuggestionAsync() {
		executorService.execute(this::getSuggestion);
	}

	private static String extractOfferStatus(GrandExchangeOfferState state) {
		String status;
		switch (state) {
			case SELLING:
			case CANCELLED_SELL:
			case SOLD:
				status = "sell";
				break;
			case BUYING:
			case CANCELLED_BUY:
			case BOUGHT:
				status = "buy";
				break;
			default:
				status = "empty";
		}
		return status;
	}

	void updateOffers() {
		GrandExchangeOffer[] runeliteOffers = this.client.getGrandExchangeOffers();
		offers = new Offer[runeliteOffers.length];
		for (int i = 0; i < runeliteOffers.length; i++) {
			GrandExchangeOffer runeliteOffer = runeliteOffers[i];
			if (runeliteOffer != null) {
				Offer offer = new Offer();
				offer.status = extractOfferStatus(runeliteOffer.getState());
				offer.itemId = runeliteOffer.getItemId();
				offer.price = runeliteOffer.getPrice();
				offer.amountTotal = runeliteOffer.getTotalQuantity();
				offer.amountSpent = runeliteOffer.getSpent();
				offer.amountTraded = runeliteOffer.getQuantitySold();
				offer.boxId = i;
				offer.active = runeliteOffer.getState().equals(GrandExchangeOfferState.BUYING)
						|| runeliteOffer.getState().equals(GrandExchangeOfferState.SELLING);

				offers[i] = offer;
			}
		}
	}

	void updateInventory() {
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory != null) {
			Item[] items = inventory.getItems();
			List<RSItem> unnotedItems = new ArrayList<>();
			for (Item item : items) {
				int itemId = item.getId();
				if (itemId == -1) continue;

				ItemComposition itemComposition = client.getItemDefinition(itemId);

				// If the item is noted
				if (itemComposition.getNote() != -1) {
					itemId = itemComposition.getLinkedNoteId();
				}
				unnotedItems.add(new RSItem(itemId, item.getQuantity()));
			}
			inventoryItems = unnotedItems.toArray(new RSItem[0]);
		}
	}
}
