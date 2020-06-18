package world.bentobox.visit;


import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.Optional;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.clicklisteners.CycleClick;
import world.bentobox.bentobox.hooks.VaultHook;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.visit.commands.admin.VisitAdminCommand;
import world.bentobox.visit.commands.player.VisitPlayerCommand;
import world.bentobox.visit.configs.Settings;
import world.bentobox.visit.managers.VisitAddonManager;


/**
 * This is main Addon class. It allows to load it into BentoBox hierarchy.
 */
public class VisitAddon extends Addon
{
	// ---------------------------------------------------------------------
	// Section: Variables
	// ---------------------------------------------------------------------

	/**
	 * Settings object contains
	 */
	private Settings settings;

	/**
	 * Stores addon manager.
	 */
	private VisitAddonManager addonManager;

	/**
	 * Local variable that stores if vaultHook is present.
	 */
	private Optional<VaultHook> vaultHook;


	// ---------------------------------------------------------------------
	// Section: Flags
	// ---------------------------------------------------------------------


	/**
	 * Settings flags allows to modifying parameters of the island.
	 *
	 * It can be modified by the players (island owner).
	 * This is usually an on/off setting.
	 *
	 * ALLOW_VISITS_FLAG should also be defined in language file under
	 * protection.flags section.
	 *
	 * By default setting is set to false.
	 */
	public final static Flag ALLOW_VISITS_FLAG =
		new Flag.Builder("ALLOW_VISITS_FLAG", Material.PUMPKIN_PIE).
			type(Flag.Type.SETTING).
			build();

	/**
	 * This flag allows to change who have access to modify island visitor config option.
	 * Owner can change it from member rank till owner rank.
	 * Default value is set to subowner.
	 */
	public final static Flag EDIT_CONFIG_FLAG =
		new Flag.Builder("EDIT_CONFIG_FLAG", Material.PUMPKIN_PIE).
			type(Flag.Type.PROTECTION).
			defaultRank(RanksManager.SUB_OWNER_RANK).
			clickHandler(new CycleClick("VISITOR_CONFIG", RanksManager.MEMBER_RANK, RanksManager.OWNER_RANK)).
			build();

	/**
	 * Stores instance of the addon.
	 */
	private static VisitAddon INSTANCE;


	// ---------------------------------------------------------------------
	// Section: Methods
	// ---------------------------------------------------------------------


	/**
	 * Executes code when loading the addon. This is called before {@link #onEnable()}.
	 * This <b>must</b> be used to setup configuration, worlds and commands.
	 */
	@Override
	public void onLoad()
	{
		super.onLoad();

		// in most of addons, onLoad we want to store default configuration if it does not
		// exist and load it.

		// Storing default configuration is simple. But be aware, you need
		// @StoreAt(filename="config.yml", path="addons/Visits") in header of your Config file.
		this.saveDefaultConfig();

		this.settings = new Config<>(this, Settings.class).loadConfigObject();

		if (this.settings == null)
		{
			// If we failed to load Settings then we should not enable addon.
			// We can log error and set state to DISABLED.

			this.logError("Visit settings could not load! Addon disabled.");
			this.setState(State.DISABLED);
		}
	}


	/**
	 * Executes code when enabling the addon. This is called after {@link #onLoad()}.
	 * <br/> Note that commands and worlds registration <b>must</b> be done in {@link
	 * #onLoad()}, if need be. Failure to do so <b>will</b> result in issues such as
	 * tab-completion not working for commands.
	 */
	@Override
	public void onEnable()
	{
		// Check if it is enabled - it might be loaded, but not enabled.

		if (this.getPlugin() == null || !this.getPlugin().isEnabled())
		{
			Bukkit.getLogger().severe("BentoBox is not available or disabled!");
			this.setState(State.DISABLED);
			return;
		}

		// Check if addon is not disabled before.

		if (this.getState().equals(State.DISABLED))
		{
			Bukkit.getLogger().severe("Visit Addon is not available or disabled!");
			return;
		}

		this.addonManager = new VisitAddonManager(this);

		// If your addon wants to hook into other GameModes, f.e. use flags, then you should
		// hook these flags into each GameMode.

		// Fortunately BentoBox provides ability to a list of all loaded GameModes.

		this.getPlugin().getAddonsManager().getGameModeAddons().forEach(gameModeAddon -> {
			// In Settings (and config) we define DisabledGameModes, list of GameModes where
			// current Addon should not work.
			// This is where we do not hook current addon into GameMode addon.

			if (!this.settings.getDisabledGameModes().contains(gameModeAddon.getDescription().getName()))
			{
				// Now we add GameModes to our Flags
				ALLOW_VISITS_FLAG.addGameModeAddon(gameModeAddon);
				EDIT_CONFIG_FLAG.addGameModeAddon(gameModeAddon);

				// Each GameMode could have Player Command and Admin Command and we could
				// want to integrate our Visit Command into these commands.
				// It provides ability to call command with GameMode command f.e. "/island visit"

				gameModeAddon.getAdminCommand().ifPresent(
					adminCommand -> new VisitAdminCommand(this, adminCommand));

				// Of course we should check if these commands exists, as it is possible to
				// create GameMode without them.

				gameModeAddon.getPlayerCommand().ifPresent(
					playerCommand -> new VisitPlayerCommand(this, playerCommand));

				// Add gamemode to enabled addon list. Used in GUIs.
				this.addonManager.addGameMode(gameModeAddon);
			}
		});

		if (!this.addonManager.getEnabledAddonList().isEmpty())
		{
			// After we added all GameModes into flags, we need to register these flags
			// into BentoBox.

			ALLOW_VISITS_FLAG.setDefaultSetting(this.settings.isDefaultVisitingEnabled());
			this.registerFlag(ALLOW_VISITS_FLAG);
			this.registerFlag(EDIT_CONFIG_FLAG);

			INSTANCE = this;
		}

		// BentoBox does not manage money, but it provides VaultHook that does it.
		this.vaultHook = this.getPlugin().getVault();

		// Even if Vault is installed, it does not mean that economy can be used. It is
		// necessary to check it via VaultHook#hook() method.

		if (!this.vaultHook.isPresent() || !this.vaultHook.get().hook())
		{
			this.logWarning("Economy plugin not found by Visit Addon!");
		}
	}


	/**
	 * Executes code when reloading the addon.
	 */
	@Override
	public void onReload()
	{
		super.onReload();

		// onReload most of addons just need to reload configuration.
		// If flags, listeners and handlers were set up correctly via Addon.class then
		// they will be reloaded automatically.

		this.settings = new Config<>(this, Settings.class).loadConfigObject();

		// TODO: check and readd disabled addon list in addon manager.

		if (this.settings == null)
		{
			// If we failed to load Settings then we should not enable addon.
			// We can log error and set state to DISABLED.

			this.logError("Visits settings could not load! Addon disabled.");
			this.setState(State.DISABLED);
		}
	}


	/**
	 * Executes code when disabling the addon.
	 */
	@Override
	public void onDisable()
	{
	}


	/**
	 * This method saves settings file from memory.
	 */
	public void saveSettings()
	{
		if (this.settings != null)
		{
			new Config<>(this, Settings.class).saveConfigObject(this.settings);
		}
	}


	// ---------------------------------------------------------------------
	// Section: Getters
	// ---------------------------------------------------------------------


	/**
	 * This getter will allow to access to VaultHook. It is written so that it could
	 * return null, if Vault is not present.
	 * @return {@code VaultHook} if it is present, {@code null} otherwise.
	 */
	public VaultHook getVaultHook()
	{
		return this.vaultHook.orElse(null);
	}


	/**
	 * This method returns the settings value.
	 * @return the value of settings.
	 */
	public Settings getSettings()
	{
		return this.settings;
	}


	/**
	 * This method returns the addonManager value.
	 * @return the value of addonManager.
	 */
	public VisitAddonManager getAddonManager()
	{
		return this.addonManager;
	}


	/**
	 * This method returns instance of current addon.
	 * @return Addon instance.
	 */
	public static VisitAddon getInstance()
	{
		return VisitAddon.INSTANCE;
	}
}
