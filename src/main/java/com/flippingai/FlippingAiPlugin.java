package com.flippingai;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.HashMap;
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

	private HashMap<Integer, Integer> inventoryItems = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		log.info("Flipping AI started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Flipping AI stopped!");
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		log.info("Offer changed");
		printSlots();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
			log.info("Inventory changed");
			printInventory();
			// update inventory items

			ItemContainer inventory = event.getItemContainer();
			Item[] items = inventory.getItems();
			HashMap<Integer, Integer> inventoryItems = new HashMap<>();
			for (Item item : items) {
				int itemId = item.getId();
				ItemComposition itemComposition = client.getItemDefinition(itemId);

				// If the item is noted
				if (itemComposition.getNote() != -1) {
					itemId = itemComposition.getLinkedNoteId();
				}
				if(inventoryItems.containsKey(itemId)) {

				}

			}
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
}

