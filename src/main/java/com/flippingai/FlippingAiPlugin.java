package com.flippingai;

import com.google.gson.JsonObject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.pushingpixels.substance.api.skin.SubstanceGeminiLookAndFeel;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
	name = "Flipping AI"
)
public class FlippingAiPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private FlippingAiConfig config;

	@Inject
	private ScheduledExecutorService executorService;

	private Future<?> scheduledTask;

	private RSItem[] inventoryItems;
	private Offer[] offers;

	private TradeController tradeController;

	private boolean geOpen = false;


	private SuggestionPanel suggestionPanel;
	private NavigationButton navButton;

	@Inject
	private ClientToolbar clientToolbar;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Flipping AI started!");
		tradeController = new TradeController("http://142.4.217.224:5000/trader", 2);
		scheduledTask = executorService.scheduleAtFixedRate(this::getSuggestion, 0, 10, TimeUnit.SECONDS);

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

	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Flipping AI stopped!");

		if (scheduledTask != null && !scheduledTask.isCancelled()) {
			scheduledTask.cancel(true);
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		updateOffers();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
			log.info("Inventory changed");
			updateInventory();
		}

	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event) {
		if (event.getGroupId() == WidgetID.GRAND_EXCHANGE_GROUP_ID) {
			geOpen = true;
		}
	}

	@Provides
	FlippingAiConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FlippingAiConfig.class);
	}


	private Widget getGeWidget() {
		return this.client.getWidget(WidgetInfo.GRAND_EXCHANGE_WINDOW_CONTAINER);
	}

	public Boolean geOpen() {
		Widget geWidget = getGeWidget();
		return geWidget != null && !geWidget.isHidden();
	}

	void getSuggestion() {
		if (offers != null && inventoryItems != null) {
			log.info("Getting suggestion");
			try {
				JsonObject suggestionJson = tradeController.getSuggestion(offers, inventoryItems, false, false);
				log.info("Received suggestion: " + suggestionJson.toString());
				Suggestion suggestion = Suggestion.fromJson(suggestionJson);
				suggestionPanel.setText(suggestion.toString());
			} catch (IOException e) {
				log.error("Error occurred while getting suggestion: " + e);
			}
		}
	}

	private static String extractOfferState(GrandExchangeOfferState state) {
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
				offer.status = extractOfferState(runeliteOffer.getState());
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

