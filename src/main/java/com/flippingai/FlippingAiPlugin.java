package com.flippingai;

import com.google.gson.JsonObject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.io.IOException;
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


	@Override
	protected void startUp() throws Exception
	{
		log.info("Flipping AI started!");
		tradeController = new TradeController("http://142.4.217.224:5000/trader", 2);
		scheduledTask = executorService.scheduleAtFixedRate(this::getCommand, 0, 4, TimeUnit.SECONDS);
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


	void printSlots() {
		GrandExchangeOffer[] offers = this.client.getGrandExchangeOffers();
		for (int i = 0; i < 8; i++) {
			GrandExchangeOffer offer = offers[i];
			if( offer != null) {
				log.info("----------------------");
				log.info("slot: " + i);
				log.info("status:" + offer.getState());
				log.info("item id: " + offer.getItemId());
				log.info("price: " + offer.getPrice());
				log.info("Quantity sold: " + offer.getQuantitySold());
				log.info("total quantity: " + offer.getTotalQuantity());
				log.info("amount spent: " + offer.getSpent());
			}
			else {
				log.info("offer is null");
			}
		}
	}

	void printInventory() {
		// Log inventory items
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory != null) {
			Item[] items = inventory.getItems();
			for (Item item : items) {
				int itemId = item.getId();
				log.info("Inventory item ID: " + itemId);
				ItemComposition itemComposition = client.getItemDefinition(itemId);

				// If the item is noted
				if (itemComposition.getNote() != -1) {
					itemId = itemComposition.getLinkedNoteId();
					log.info("Unnoted item ID: " + itemId);
				}

				log.info("----------------------------");
				log.info("Inventory item ID: " + itemId);
				log.info("Quantity: " + item.getQuantity());
			}
		}
	}

	private Widget getGeWidget() {
		return this.client.getWidget(WidgetInfo.GRAND_EXCHANGE_WINDOW_CONTAINER);
	}

	public Boolean geOpen() {
		Widget geWidget = getGeWidget();
		return geWidget != null && !geWidget.isHidden();
	}

	void getCommand() {
		if (offers != null && inventoryItems != null) {
			log.info("Getting command");
			try {
				JsonObject command = tradeController.getCommand(offers, inventoryItems, false, false);
				log.info("Received command: " + command.toString());
			} catch (IOException e) {
				log.error("Error occurred while getting command: " + e);
			}
		}
	}

	void updateOffers() {
		GrandExchangeOffer[] runeliteOffers = this.client.getGrandExchangeOffers();
		offers = new Offer[runeliteOffers.length];
		for (int i = 0; i < runeliteOffers.length; i++) {
			GrandExchangeOffer runeliteOffer = runeliteOffers[i];
			if (runeliteOffer != null) {

				Offer offer = new Offer();
				// Assigning values from GrandExchangeOffer to our custom Offer class
				offer.status = runeliteOffer.getState().name();
				offer.itemId = runeliteOffer.getItemId();
				offer.price = runeliteOffer.getPrice();
				offer.amountTotal = runeliteOffer.getTotalQuantity();
				offer.amountSpent = runeliteOffer.getSpent();
				offer.amountTraded = runeliteOffer.getQuantitySold();
				offer.boxId = i;
				offer.active = runeliteOffer.getState().equals(GrandExchangeOfferState.BUYING)
						|| runeliteOffer.getState().equals(GrandExchangeOfferState.SELLING); // Assuming 'active' is true if status is not 'empty'

				offers[i] = offer;
			}
		}

	}

	void updateInventory() {
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory != null) {
			Item[] items = inventory.getItems();
			RSItem[] unnotedItems = new RSItem[items.length];
			for  (int i = 0; i < items.length; i++) {
				Item item = items[i];
				int itemId = item.getId();
				ItemComposition itemComposition = client.getItemDefinition(itemId);

				// If the item is noted
				if (itemComposition.getNote() != -1) {
					itemId = itemComposition.getLinkedNoteId();
				}
				unnotedItems[i] = new RSItem(itemId, item.getQuantity());
			}
			inventoryItems = unnotedItems;
		}
	}
}

