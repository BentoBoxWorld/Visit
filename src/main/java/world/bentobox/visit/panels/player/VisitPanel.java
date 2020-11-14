package world.bentobox.visit.panels.player;


import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.database.object.IslandVisitSettings;
import world.bentobox.visit.managers.VisitAddonManager;


/**
 * This class shows how to set up easy panel by using BentoBox PanelBuilder API
 */
public class VisitPanel
{
	// ---------------------------------------------------------------------
	// Section: Variables
	// ---------------------------------------------------------------------

	/**
	 * This variable allows to access addon object.
	 */
	private final VisitAddon addon;

	/**
	 * This variable allows to access addon manager object.
	 */
	private final VisitAddonManager manager;

	/**
	 * This variable stores main world where GUI is targeted.
	 */
	private final World world;

	/**
	 * This variable holds user who opens panel. Without it panel cannot be opened.
	 */
	private final User user;

	/**
	 * This variable stores all islands in the given world.
	 */
	private final List<Island> islandList;

	/**
	 * This variable holds current pageIndex for multi-page island choosing.
	 */
	private int pageIndex;


	// ---------------------------------------------------------------------
	// Section: Static Variables
	// ---------------------------------------------------------------------


	/**
	 * Material for panel Header.
	 */
	private final static PanelItem HEADER_BLOCK = new PanelItemBuilder().
		icon(Material.MAGENTA_STAINED_GLASS_PANE).
		description(" ").
		build();


	// ---------------------------------------------------------------------
	// Section: Internal Constructor
	// ---------------------------------------------------------------------


	/**
	 * This is internal constructor. It is used internally in current class to avoid
	 * creating objects everywhere.
	 * @param addon VisitAddon object.
	 */
	private VisitPanel(VisitAddon addon,
		World world,
		User user)
	{
		this.addon = addon;
		this.manager = this.addon.getAddonManager();
		this.world = world;
		this.user = user;

		// Unfortunately, it is necessary to store islands in local list, as there is no
		// other ways how to get the same island order from hash list :(.
		this.islandList = this.addon.getIslands().getIslands(this.world).stream().
		// Filter out islands without owner.
			filter(Island::isOwned).
		// Filter out locked islands.
			filter(island -> island.isAllowed(this.user, Flags.LOCK)).
		// Filter out islands with disabled visits flag.
			filter(island -> island.isAllowed(VisitAddon.ALLOW_VISITS_FLAG)).
		// Filter out islands where player is banned.
			filter(island -> !island.isBanned(this.user.getUniqueId())).
		// Sort by island and owner name
			sorted((o1, o2) ->
				(o1.getName() != null ?
					o1.getName() : Objects.requireNonNull(User.getInstance(o1.getOwner())).getName()).
					compareToIgnoreCase(o2.getName() != null ?
						o2.getName() : Objects.requireNonNull(User.getInstance(o2.getOwner())).getName())).
			collect(Collectors.toList());
	}


	/**
	 * This method is used to open UserPanel outside this class. It will be much easier
	 * to open panel with single method call then initializing new object.
	 * @param addon VisitAddon object
	 * @param user User who opens panel
	 */
	public static void openPanel(VisitAddon addon,
		World world,
		User user)
	{
		new VisitPanel(addon, world, user).build();
	}


	// ---------------------------------------------------------------------
	// Section: Methods
	// ---------------------------------------------------------------------


	/**
	 * Build method manages current panel opening. It uses BentoBox PanelAPI that is easy
	 * to use and users can get nice panels.
	 */
	private void build()
	{
		// PanelBuilder is a BentoBox API that provides ability to easy create Panels.
		PanelBuilder panelBuilder = new PanelBuilder().
			user(this.user).
			name(this.user.getTranslation("visit.gui.player.title.choose"));

		// We can allow to assing next button automatically by PanelBuilder.
		boolean hasHeader = this.createHeader(panelBuilder);

		// Panels are limited to 54 elements. If there exist header, then only 45 islands
		// can be viewed.
		final int MAX_ELEMENTS = hasHeader ? 45 : 54;

		int index;

		if (this.pageIndex > 0)
		{
			panelBuilder.item(this.createPreviousPageButton());
			// Multiple pages. Adjust index.

			index = this.pageIndex * MAX_ELEMENTS;
		}
		else
		{
			index = 0;
		}

		// Create island iterator for sublist that contains elements from index till list size.
		Iterator<Island> islandIterator = this.islandList.subList(index, this.islandList.size()).iterator();

		// Fill every free spot till last block is not used.
		while (islandIterator.hasNext() && panelBuilder.nextSlot() < 53)
		{
			panelBuilder.item(this.createIslandButton(islandIterator.next()));
		}

		// Check if there is spot for last island
		if (islandIterator.hasNext())
		{
			Island lastIsland = islandIterator.next();

			if (islandIterator.hasNext())
			{
				// There is more then one island left in the list.
				panelBuilder.item(this.createNextPageButton());
			}
			else
			{
				// This is the last island.. no need to create next page button.
				panelBuilder.item(this.createIslandButton(lastIsland));
			}
		}

		// At the end we just call build method that creates and opens panel.
		panelBuilder.build();
	}


	/**
	 * This method creates header for current Panel.
	 * @param panelBuilder Builder that will create panel.
	 * @return {@code true} if header is created, {@code false} otherwise.
	 */
	private boolean createHeader(PanelBuilder panelBuilder)
	{
		if (!this.addon.getSettings().isShowGameModeHeader())
		{
			// GameMode header is disabled.
			return false;
		}

		List<GameModeAddon> enabledAddons = this.manager.getEnabledAddonList();

		// Sets if slot 4 should be empty.
		boolean skipMiddle = enabledAddons.size() % 2 == 0;
		// Sets starting index for icon creation
		int startingIndex = 4 - (skipMiddle ?  enabledAddons.size() : (enabledAddons.size() - 1)) / 2;

		if (enabledAddons.size() < 2)
		{
			// Do not build header if there is only 1 gamemode.
			return false;
		}

		if (enabledAddons.size() > 9)
		{
			// Too many gamemode addons.
			return false;
		}

		int addonIndex = 0;

		for (int slot = 0; slot < 9; slot++)
		{
			// Populate with GameMode icons.
			if (!skipMiddle || slot != 4)
			{
				panelBuilder.item(slot, this.createGameModeButton(enabledAddons.get(addonIndex++)));
			}
			else
			{
				panelBuilder.item(slot, VisitPanel.HEADER_BLOCK);
			}
		}


		return true;
	}


	/**
	 * This method creates Button for given GameMode gameModeAddon.
	 * @param gameModeAddon Addon which button must be created.
	 * @return PanelItem that represents given GameMode gameModeAddon.
	 */
	private PanelItem createGameModeButton(final GameModeAddon gameModeAddon)
	{
		return new PanelItemBuilder().
			icon(gameModeAddon.getDescription().getIcon()).
			name(this.user.getTranslation("visit.gui.player.button.gamemode.name",
				"[gamemode]", gameModeAddon.getDescription().getName())).
			description(this.user.getTranslation("visit.gui.player.button.gamemode.description",
				"[gamemode]", gameModeAddon.getDescription().getName())).
			clickHandler((panel, user, clickType, index) -> {
				gameModeAddon.getPlayerCommand().ifPresent(command ->
					VisitPanel.openPanel(this.addon,
						gameModeAddon.getOverWorld(),
						user));

				return true;
			}).
			glow(Util.getWorld(this.world) == gameModeAddon.getOverWorld()).
			build();
	}


	/**
	 * This method creates and returns button that switch to previous page in view mode.
	 * @return PanelItem that allows to select previous island view page.
	 */
	private PanelItem createPreviousPageButton()
	{
		return new PanelItemBuilder().
			icon(Material.TIPPED_ARROW).
			name(this.user.getTranslation("visit.gui.player.button.previous.name")).
			clickHandler((panel, user, clickType, index) -> {
				this.pageIndex = Math.max(0, this.pageIndex - 1);
				this.build();
				return true;
			}).
			build();
	}


	/**
	 * This method creates and returns button that switch to next page in view mode.
	 * @return PanelItem that allows to select next island view page.
	 */
	private PanelItem createNextPageButton()
	{
		return new PanelItemBuilder().
			icon(Material.TIPPED_ARROW).
			name(this.user.getTranslation("visit.gui.player.button.next.name")).
			clickHandler((panel, user, clickType, index) -> {
				this.pageIndex++;
				this.build();
				return true;
			}).
			build();
	}


	/**
	 * This method creates and returns button that switch to next page in view mode.
	 * @return PanelItem that allows to select next island view page.
	 */
	private PanelItem createIslandButton(Island island)
	{
		PanelItemBuilder builder = new PanelItemBuilder();

		if (this.addon.getSettings().getIslandIcon() == Material.PLAYER_HEAD)
		{
			builder.icon(Objects.requireNonNull(User.getInstance(island.getOwner())).getName());
		}
		else
		{
			builder.icon(this.addon.getSettings().getIslandIcon());
		}

		builder.name(this.user.getTranslation("visit.gui.player.button.island.name",
			"[name]",
			island.getName() != null ? island.getName() :
				Objects.requireNonNull(User.getInstance(island.getOwner())).getName()));

		List<String> description = new ArrayList<>();

		IslandVisitSettings settings = this.manager.getIslandVisitSettings(island);

		boolean canVisit = true;

		if (!this.manager.canVisitOffline(island, settings))
		{
			description.add(this.user.getTranslation("visit.gui.player.button.island.noone-online"));
			canVisit = false;
		}

		double payment = settings.getPayment() + this.addon.getSettings().getTaxAmount();

		if (payment > 0)
		{
			description.add(this.user.getTranslation("visit.gui.player.button.island.cost",
				"[cost]", String.valueOf(payment)));

			if (!this.manager.hasCredits(this.user, payment))
			{
				canVisit = false;
			}
		}

		builder.description(description);

		// Glow icon if user can visit the island.
		builder.glow(canVisit);

		// Final variable for clicking in clickHandler.
		final boolean canClick = canVisit;

		return builder.clickHandler((panel, user, clickType, index) ->
			{
				if (canClick)
				{
					this.manager.processTeleportation(user, island, settings);
					// Close inventory
					user.closeInventory();
				}

				return true;
			}).
			build();
	}
}
