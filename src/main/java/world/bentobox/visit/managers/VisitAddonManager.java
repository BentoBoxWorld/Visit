package world.bentobox.visit.managers;


import org.eclipse.jdt.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.database.object.IslandVisitSettings;


/**
 * This class manages data handling and option processing for Visit Addon.
 */
public class VisitAddonManager
{
	// ---------------------------------------------------------------------
	// Section: Variables
	// ---------------------------------------------------------------------

	/**
	 * Local variable that stores all GameMode addons where current addon is enabled.
	 */
	private final List<GameModeAddon> enabledAddonList;

	/**
	 * This config object stores structures for island visit settings objects.
	 */
	private Database<IslandVisitSettings> visitSettingsDatabase;

	/**
	 * This is local cache that links island unique id with island visit settings object.
	 */
	private Map<String, IslandVisitSettings> visitSettingsCacheData;

	/**
	 * Reference to main addon class.
	 */
	private final VisitAddon addon;

	// ---------------------------------------------------------------------
	// Section: Constructors
	// ---------------------------------------------------------------------


	/**
	 * Constructor VisitAddonManager creates a new VisitAddonManager instance.
	 *
	 * @param addon of type VisitAddon
	 */
	public VisitAddonManager(VisitAddon addon)
	{
		this.addon = addon;
		this.enabledAddonList = new ArrayList<>(5);

		// Init database and cache
		this.visitSettingsDatabase = new Database<>(addon, IslandVisitSettings.class);
		this.visitSettingsCacheData = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		this.load();
	}


	// ---------------------------------------------------------------------
	// Section: Database related methods
	// ---------------------------------------------------------------------


	/**
	 * Clear and reload all challenges
	 */
	public void load()
	{
		this.addon.log("Loading stored island visit settings...");
		this.visitSettingsCacheData.clear();
		this.visitSettingsDatabase.loadObjects().forEach(this::loadSettings);
	}


	/**
	 * This method tries to load island visit settings from database silently with
	 * overwrite enabled.
	 * @param islandSettings object that must be loaded in local cache
	 */
	private void loadSettings(@NonNull IslandVisitSettings islandSettings)
	{
		this.loadSettings(islandSettings, true, null, true);
	}


	/**
	 * This method loads given island visit settings into local cache. It provides
	 * functionality to overwrite local value with new one, and send message to given user.
	 *
	 * @param islandSettings object that must be loaded in local cache
	 * @param overwrite of type boolean that indicate if local element must be overwritten.
	 * @param user of type User who will receive messages.
	 * @param silent of type boolean that indicate if message to user must be sent.
	 * @return boolean that indicate about load status.
	 */
	public boolean loadSettings(@NonNull IslandVisitSettings islandSettings,
		boolean overwrite,
		User user,
		boolean silent)
	{
		// This may happen if database somehow failed to load challenge and return
		// null as input.
		if (islandSettings == null)
		{
			if (!silent)
			{
				user.sendMessage("load-error", "[value]", "NULL");
			}

			return false;
		}

		if (this.visitSettingsCacheData.containsKey(islandSettings.getUniqueId()))
		{
			if (!overwrite)
			{
				if (!silent)
				{
					user.sendMessage("visit.messages.load-skipping",
						"[value]", islandSettings.getUniqueId());
				}

				return false;
			}
			else
			{
				if (!silent)
				{
					user.sendMessage("visit.messages.load-overwriting",
						"[value]", islandSettings.getUniqueId());
				}
			}
		}
		else
		{
			if (!silent)
			{
				user.sendMessage("visit.messages.load-add",
					"[value]", islandSettings.getUniqueId());
			}
		}

		this.visitSettingsCacheData.put(islandSettings.getUniqueId(), islandSettings);
		return true;
	}


	/**
	 * This method saves given settings object to database.
	 * @param settings object that must be saved
	 */
	public void saveSettings(IslandVisitSettings settings)
	{
		// Save only if default values are not the same as current object.
		// Limitation is that if owner changes default, it will affect this
		// island too, but that is how it is intended to work.

		if (this.addon.getSettings().getDefaultVisitingPayment() != settings.getPayment() ||
			this.addon.getSettings().isDefaultVisitingOffline() != settings.isOfflineVisit())
		{
			this.visitSettingsDatabase.saveObjectAsync(settings);
		}
	}

	// ---------------------------------------------------------------------
	// Section: Local cache access data
	// ---------------------------------------------------------------------


	/**
	 * This method returns island visit settings object from cache for given island.
	 * @param island Island.
	 * @return IslandVisitSettings object.
	 */
	public IslandVisitSettings getIslandVisitSettings(Island island)
	{
		return this.getIslandVisitSettings(island.getUniqueId());
	}


	/**
	 * This method returns island visit settings object from cache for given island uuid.
	 * @param uuid Island UUID.
	 * @return IslandVisitSettings object.
	 */
	public IslandVisitSettings getIslandVisitSettings(String uuid)
	{
		return this.visitSettingsCacheData.getOrDefault(uuid, this.defaultIslandVisitSettings());
	}


	/**
	 * This method creates default IslandVisitSettings object.
	 * @return IslandVisitSettings object that has default values.
	 */
	private IslandVisitSettings defaultIslandVisitSettings()
	{
		IslandVisitSettings settings = new IslandVisitSettings();

		settings.setOfflineVisit(this.addon.getSettings().isDefaultVisitingOffline());
		settings.setPayment(this.addon.getSettings().getDefaultVisitingPayment());

		return settings;
	}


	// ---------------------------------------------------------------------
	// Section: Methods
	// ---------------------------------------------------------------------


	/**
	 * This method adds given gamemode to the enabled addon list.
	 * @param addon Addon that must be added to enabled addon list.
	 */
	public void addGameMode(GameModeAddon addon)
	{
		this.enabledAddonList.add(addon);
	}


	/**
	 * This method returns the enabledAddonList value.
	 * @return the value of enabledAddonList.
	 */
	public List<GameModeAddon> getEnabledAddonList()
	{
		return enabledAddonList;
	}


	// ---------------------------------------------------------------------
	// Section: VaultHook wrapper
	// ---------------------------------------------------------------------


	/**
	 * This method checks if given user has enough credits.
 	 * @param user Targeted user.
	 * @param credits Amount that must be checked.
	 * @return {@code true} if vaultHook is enabled and player has enough credits,
	 * {@code false} otherwise
	 */
	public boolean hasCredits(User user, double credits)
	{
		return this.addon.getVaultHook() == null ||
			!this.addon.getVaultHook().hook() ||
			this.addon.getVaultHook().has(user, credits);
	}


	/**
	 * This method deposits given amount to player account.
	 * @param user Targeted user.
	 * @param credits Amount that must be deposited.
	 * @return {@code true} if vaultHook not enabled or transaction was successful,
	 * {@code false} otherwise
	 */
	public boolean depositCredits(User user, double credits)
	{
		if (this.addon.getVaultHook() != null &&
			this.addon.getVaultHook().hook())
		{
			return this.addon.getVaultHook().deposit(user, credits).transactionSuccess();
		}
		else
		{
			return true;
		}
	}


	/**
	 * This method withdraws given amount from player account.
	 * @param user Targeted user.
	 * @param credits Amount that must be removed.
	 * @return {@code true} if vaultHook is not enabled or transaction was successful,
	 * {@code false} otherwise
	 */
	public boolean withdrawCredits(User user, double credits)
	{
		if (this.addon.getVaultHook() != null &&
			this.addon.getVaultHook().hook())
		{
			return this.addon.getVaultHook().withdraw(user, credits).transactionSuccess();
		}
		else
		{
			return true;
		}
	}
}
