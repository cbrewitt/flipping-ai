package com.flippingai;

import com.google.gson.JsonObject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
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
import java.util.concurrent.ScheduledExecutorService;


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
	private Offer[] offers = new Offer[8];
	private TradeController tradeController;
	private boolean geOpen = false;
	private SuggestionPanel suggestionPanel;
	private NavigationButton navButton;
	@Inject
	@Getter
	private ClientThread clientThread;
	@Inject
	private ClientToolbar clientToolbar;
	private Timer suggestionTimer;
	private boolean suggestionNeeded = false;
	private Suggestion currentSuggestion;

	//this flag is to know that when we see the login screen an account has actually logged out and its not just that the
	//client has started.
	private boolean previouslyLoggedIn;
	//the display name of the currently logged in user. This is the only account that can actually receive offers
	//as this is the only account currently logged in.
	@Getter
	@Setter
	private String currentlyLoggedInAccount;


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
		tradeController = new TradeController("http://142.4.217.224:5000/trader");

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
		updateOffer(event);
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
		if (suggestionNeeded && getOpenSlot() == -1) {
			suggestionNeeded = false;
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
		suggestionPanel.showSpinner();
		try {
			if (offers != null && inventoryItems != null && currentlyLoggedInAccount != null) {
				log.info("Getting suggestion");
				try {
					if(tradeController.accountId == -1) {
						tradeController.getAccountId(currentlyLoggedInAccount);
					}

					JsonObject suggestionJson = tradeController.getSuggestion(offers, inventoryItems, false, false);
					log.info("Received suggestion: " + suggestionJson.toString());
					currentSuggestion = Suggestion.fromJson(suggestionJson);
					if (collectNeeded(currentSuggestion)) {
						suggestionPanel.suggestCollect();
					} else {
						suggestionPanel.updateSuggestion(currentSuggestion);
					}
				} catch (IOException e) {
					log.error("Error occurred while getting suggestion: " + e);
					suggestionPanel.setText("Failed to connect to server");
				}
			}
		} finally {
			resetSuggestionTimer();
			if (!suggestionNeeded) {
				suggestionPanel.hideSpinner();
			}
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

	boolean collectNeeded(Suggestion suggestion) {
		if ((suggestion.getType().equals("buy") || suggestion.getType().equals("sell"))
				&& !offers[suggestion.getBoxId()].status.equals("empty")) {
			return true;
		}
		if (suggestion.getType().equals("buy")) {
			int gp = 0;
			for (RSItem item : inventoryItems) {
				if (item.getId() == 995) {
					gp += item.getAmount();
				}
			}
			return gp < suggestion.getPrice() * suggestion.getQuantity();
		} else if (suggestion.getType().equals("sell")) {
			int amount = 0;
			for (RSItem item : inventoryItems) {
				if (item.getId() == suggestion.getItemId()) {
					amount += item.getAmount();
				}
			}
			return amount < suggestion.getQuantity();
		}
		return false;
	}



	void updateAllOffers() {
		GrandExchangeOffer[] runeliteOffers = this.client.getGrandExchangeOffers();
		offers = new Offer[runeliteOffers.length];
		for (int i = 0; i < runeliteOffers.length; i++) {
			GrandExchangeOffer runeliteOffer = runeliteOffers[i];
			if (runeliteOffer != null) {
				Offer offer = extractOffer(runeliteOffer, i);
				offers[i] = offer;
			}
		}
	}

	void initialiseOffers() {
		offers = new Offer[8];
		for (int i = 0; i < 8; i++) {
				Offer offer = new Offer();
				offer.boxId = i;
				offers[i] = offer;
		}
	}

	Offer extractOffer(GrandExchangeOffer runeliteOffer, int boxId) {
		Offer offer = new Offer();
		offer.status = extractOfferStatus(runeliteOffer.getState());
		offer.itemId = runeliteOffer.getItemId();
		offer.price = runeliteOffer.getPrice();
		offer.amountTotal = runeliteOffer.getTotalQuantity();
		offer.amountSpent = runeliteOffer.getSpent();
		offer.amountTraded = runeliteOffer.getQuantitySold();
		offer.boxId = boxId;
		offer.active = runeliteOffer.getState().equals(GrandExchangeOfferState.BUYING)
				|| runeliteOffer.getState().equals(GrandExchangeOfferState.SELLING);
		return offer;
	}

	void updateOffer(GrandExchangeOfferChanged event) {
		Offer oldOffer = offers[event.getSlot()];
		GrandExchangeOffer runeliteOffer = event.getOffer();
		Offer newOffer = extractOffer(runeliteOffer, event.getSlot());

		// add uncollected items and gp
		if (oldOffer != null) {
			if (newOffer.status.equals("buy")) {
				newOffer.itemsToCollect = oldOffer.itemsToCollect +
						newOffer.amountTraded - oldOffer.amountTraded;
			} else if (newOffer.status.equals("sell")) {
				newOffer.gpToCollect = oldOffer.gpToCollect +
						newOffer.amountSpent - oldOffer.amountSpent;
			}
		}

		// add uncollected items and gp from aborted offers
		if (oldOffer == null || oldOffer.active) {
			if (runeliteOffer.getState().equals(GrandExchangeOfferState.CANCELLED_BUY)) {
				newOffer.gpToCollect += (newOffer.amountTotal - newOffer.amountTraded) * newOffer.price;
			} else if (runeliteOffer.getState().equals(GrandExchangeOfferState.CANCELLED_SELL)) {
				newOffer.itemsToCollect += newOffer.amountTotal - newOffer.amountTraded;
			}
		}

		offers[newOffer.boxId] = newOffer;
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

	void handleCollectAll(MenuOptionClicked event) {
		String menuOption = event.getMenuOption();
		Widget widget = event.getWidget();

		if (widget != null && widget.getId() == 30474246) {
			if (menuOption.contains("Collect")) {
				for (Offer offer: offers) {
					offer.itemsToCollect = 0;
					offer.gpToCollect = 0;
				}
				suggestionPanel.updateSuggestion(currentSuggestion);
			}
		}
	}

	void handleCollectWithSlotOpen(MenuOptionClicked event) {
		String menuOption = event.getMenuOption();
		Widget widget = event.getWidget();

		if (widget != null && widget.getId() == 30474264 ) {
			if (menuOption.contains("Collect") || menuOption.contains("Bank")) {
				int slot = getOpenSlot();
				if (widget.getItemId() == 995) {
					offers[slot].gpToCollect = 0;
				} else {
					offers[slot].itemsToCollect = 0;
				}
				if (!collectNeeded(currentSuggestion)) {
					suggestionPanel.updateSuggestion(currentSuggestion);
				}
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		handleCollectAll(event);
		handleCollectWithSlotOpen(event);
	}

	int getOpenSlot() {
		return client.getVarbitValue(4439) - 1;
	}

	// Borrowed from FlippingUtilities
	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN) {
			onLoggedInGameState();
		} else if (event.getGameState() == GameState.LOGIN_SCREEN && previouslyLoggedIn) {
			//this randomly fired at night hours after i had logged off...so i'm adding this guard here.
			if (currentlyLoggedInAccount != null && client.getGameState() != GameState.LOGGED_IN) {
				handleLogout();
			}
		}
	}

	// Borrowed from FlippingUtilities
	private void onLoggedInGameState() {
		//keep scheduling this task until it returns true (when we have access to a display name)
		clientThread.invokeLater(() ->
		{
			//we return true in this case as something went wrong and somehow the state isn't logged in, so we don't
			//want to keep scheduling this task.
			if (client.getGameState() != GameState.LOGGED_IN) {
				return true;
			}

			final Player player = client.getLocalPlayer();

			//player is null, so we can't get the display name so, return false, which will schedule
			//the task on the client thread again.
			if (player == null) {
				return false;
			}

			final String name = player.getName();

			if (name == null) {
				return false;
			}

			if (name.equals("")) {
				return false;
			}
			previouslyLoggedIn = true;

			if (currentlyLoggedInAccount == null) {
				handleLogin(name);
			}
			//stops scheduling this task
			return true;
		});
	}

	// Borrowed from FlippingUtilities
	public void handleLogin(String displayName) {
		if (client.getAccountType().isIronman()) {
			log.info("account is an ironman");
			return;
		}
		currentlyLoggedInAccount = displayName;
	}


	// Borrowed from FlippingUtilities
	public void handleLogout() {
		log.info("{} is logging out", currentlyLoggedInAccount);
		currentlyLoggedInAccount = null;
		tradeController.resetAccountId();
		suggestionPanel.suggestLogin();
	}

}


