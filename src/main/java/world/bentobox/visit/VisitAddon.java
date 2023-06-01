//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit;


import com.earth2me.essentials.Essentials;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import org.bukkit.plugin.Plugin;
import world.bentobox.bank.Bank;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.hooks.VaultHook;
import world.bentobox.visit.commands.admin.VisitAdminCommand;
import world.bentobox.visit.commands.player.VisitPlayerCommand;
import world.bentobox.visit.configs.Settings;
import world.bentobox.visit.listeners.IslandLeaveListener;
import world.bentobox.visit.managers.VisitAddonManager;

import java.util.Optional;


/**
 * This is main Addon class. It allows to load it into BentoBox hierarchy.
 */
public class VisitAddon extends Addon
{
    /**
     * Executes code when loading the addon. This is called before {@link #onEnable()}. This <b>must</b> be used to
     * setup configuration, worlds and commands.
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

        ALLOW_VISITS_FLAG = new Flag.Builder("ALLOW_VISITS_FLAG", Material.PUMPKIN_PIE).
            type(Flag.Type.SETTING).
            defaultRank(this.settings.isDefaultVisitingEnabled() ? 0 : -1).
            build();

        RECEIVE_VISIT_MESSAGE_FLAG = new Flag.Builder("RECEIVE_VISIT_MESSAGE_FLAG", Material.PAPER).
            type(Flag.Type.SETTING).
            defaultRank(0).
            build();

        // Save existing panels.
        this.saveResource("panels/main_panel.yml", false);
        this.saveResource("panels/manage_panel.yml", false);
    }


    /**
     * Executes code when enabling the addon. This is called after {@link #onLoad()}. <br/> Note that commands and
     * worlds registration <b>must</b> be done in {@link #onLoad()}, if need be. Failure to do so <b>will</b> result in
     * issues such as tab-completion not working for commands.
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
                RECEIVE_VISIT_MESSAGE_FLAG.addGameModeAddon(gameModeAddon);

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
            this.registerFlag(ALLOW_VISITS_FLAG);
            this.registerFlag(RECEIVE_VISIT_MESSAGE_FLAG);

            this.registerListener(new IslandLeaveListener(this));

            INSTANCE = this;
        }
    }


    @Override
    public void allLoaded()
    {
        super.allLoaded();
        this.hookExtensions();
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


// ---------------------------------------------------------------------
// Section: Methods
// ---------------------------------------------------------------------


    /**
     * This method finds and hooks into visit addon extensions.
     */
    private void hookExtensions()
    {
        // Try to find Level addon and if it does not exist, display a warning
        this.getAddonByName("Bank").ifPresentOrElse(addon ->
        {
            this.bankHook = (Bank) addon;
            this.log("Visit Addon hooked into Bank addon.");
        }, () ->
        {
            this.bankHook = null;
        });

        // Try to find Vault Plugin and if it does not exist, display a warning
        this.getPlugin().getVault().ifPresentOrElse(hook ->
        {
            this.vaultHook = hook;

            if (this.vaultHook.hook())
            {
                this.log("Visit Addon hooked into Economy.");
            }
            else
            {
                this.logWarning("Visit Addon could not hook into valid Economy.");
            }
        }, () ->
        {
            this.vaultHook = null;
            this.logWarning("Vault plugin not found. Economy will not work!");
        });

        Optional<Plugin> essentials = Optional.ofNullable(Bukkit.getPluginManager().getPlugin("Essentials"));
        essentials.ifPresentOrElse(hook -> {
            this.essentials = (Essentials) hook;
            if (this.essentials.isEnabled()) {
                this.log("Visit Addon hooked into Essentials.");
            }
            else {
                this.logWarning("Visit Addon could not hook into Essentials.");
            }
        }, () -> {
            this.essentials = null;
            this.logWarning("Essentials plugin not found!");
        });
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


    /**
     * This getter will allow to access to VaultHook. It is written so that it could return null, if Vault is not
     * present.
     *
     * @return {@code VaultHook} if it is present, {@code null} otherwise.
     */
    public VaultHook getVaultHook()
    {
        return this.vaultHook;
    }

    /**
     * This getter will allow to access to Essentials. It is written so that it could return null, if Essentials is not
     * present.
     *
     * @return {@code Essentials} if it is present, {@code null} otherwise.
     */
    public Essentials getEssentials()
    {
        return this.essentials;
    }

    /**
     * Gets bank hook.
     *
     * @return the bank hook
     */
    public Bank getBankHook()
    {
        return this.bankHook;
    }


    /**
     * This method returns the settings value.
     *
     * @return the value of settings.
     */
    public Settings getSettings()
    {
        return this.settings;
    }


    /**
     * This method returns the addonManager value.
     *
     * @return the value of addonManager.
     */
    public VisitAddonManager getAddonManager()
    {
        return this.addonManager;
    }


    /**
     * This method returns instance of current addon.
     *
     * @return Addon instance.
     */
    public static VisitAddon getInstance()
    {
        return VisitAddon.INSTANCE;
    }


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
     * Local variable that stores vault hook.
     */
    private VaultHook vaultHook;

    /**
     * Local variable that stores essentials hook.
     */
    private Essentials essentials;

    /**
     * Local variable that stores bank addon hook.
     */
    private Bank bankHook;

    /**
     * Stores instance of the addon.
     */
    private static VisitAddon INSTANCE;

    /**
     * Settings flags allows to modifying parameters of the island.
     * <p>
     * It can be modified by the players (island owner). This is usually an on/off setting.
     * <p>
     * ALLOW_VISITS_FLAG should also be defined in language file under protection.flags section.
     * <p>
     * By default setting is set to false.
     */
    public static Flag ALLOW_VISITS_FLAG;

    /**
     * This flag allows to toggle if player should receive message that someone visits his island.
     */
    public static Flag RECEIVE_VISIT_MESSAGE_FLAG;
}
