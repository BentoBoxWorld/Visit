//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit.managers;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.database.object.IslandVisitSettings;
import world.bentobox.visit.events.VisitEvent;
import world.bentobox.visit.utils.Constants;
import world.bentobox.visit.utils.Utils;


/**
 * This class manages data handling and option processing for Visit Addon.
 */
public class VisitAddonManager
{
    // ---------------------------------------------------------------------
    // Section: Variables
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


    /**
     * Clear and reload all challenges
     */
    public void load()
    {
        this.addon.log("Loading stored island visit settings...");
        this.visitSettingsCacheData.clear();
        this.visitSettingsDatabase.loadObjects().forEach(this::loadSettings);
        this.addon.log("Visits data loaded.");
    }


    /**
     * This method tries to load island visit settings from database silently with overwrite enabled.
     *
     * @param islandSettings object that must be loaded in local cache
     */
    private void loadSettings(@NotNull IslandVisitSettings islandSettings)
    {
        this.visitSettingsCacheData.put(islandSettings.getUniqueId(), islandSettings);
    }


    // ---------------------------------------------------------------------
    // Section: Constructors
    // ---------------------------------------------------------------------


    /**
     * This method saves given settings object to database.
     *
     * @param settings object that must be saved
     */
    public void saveSettings(IslandVisitSettings settings)
    {
        // Save only if default values are not the same as current object.
        // Limitation is that if owner changes default, it will affect this
        // island too, but that is how it is intended to work.

        if (!this.hasDefaultValues(settings))
        {
            this.visitSettingsDatabase.saveObjectAsync(settings);
        }
        else if (this.visitSettingsCacheData.containsKey(settings.getUniqueId()))
        {
            this.visitSettingsDatabase.deleteObject(settings);
            this.visitSettingsCacheData.remove(settings.getUniqueId());
        }
    }


    // ---------------------------------------------------------------------
    // Section: Database related methods
    // ---------------------------------------------------------------------


    /**
     * This method removes everything from database and cache.
     */
    public void wipeDatabase()
    {
        List<IslandVisitSettings> visitSettings = this.visitSettingsDatabase.loadObjects();
        visitSettings.forEach(settings -> this.visitSettingsDatabase.deleteID(settings.getUniqueId()));
        this.visitSettingsCacheData.clear();
    }


    /**
     * This method removes data from database for given island.
     * @param island Island which data must be removed.
     */
    public void removeData(@NotNull Island island)
    {
        this.visitSettingsCacheData.remove(island.getUniqueId());
        this.visitSettingsDatabase.deleteID(island.getUniqueId());
    }


    /**
     * This method returns island visit settings object from cache for given island.
     *
     * @param island Island.
     * @return IslandVisitSettings object.
     */
    public IslandVisitSettings getIslandVisitSettings(Island island)
    {
        return this.getIslandVisitSettings(island.getUniqueId());
    }


    /**
     * This method returns island visit settings object from cache for given island uuid.
     *
     * @param uuid Island UUID.
     * @return IslandVisitSettings object.
     */
    public IslandVisitSettings getIslandVisitSettings(String uuid)
    {
        return this.visitSettingsCacheData.computeIfAbsent(uuid, id -> {
            IslandVisitSettings settings = this.defaultIslandVisitSettings();
            settings.setUniqueId(id);
            return settings;
        });
    }


    /**
     * This method creates default IslandVisitSettings object.
     *
     * @return IslandVisitSettings object that has default values.
     */
    private IslandVisitSettings defaultIslandVisitSettings()
    {
        IslandVisitSettings settings = new IslandVisitSettings();

        settings.setOfflineVisit(this.addon.getSettings().isDefaultVisitingOffline());
        settings.setPayment(this.addon.getSettings().getDefaultVisitingPayment());

        return settings;
    }


    /**
     * This method returns if given settings object has all default values.
     *
     * @param settings Settings object that must be checked.
     * @return {@code true} if object uses default settings, {@code false} otherwise.
     */
    public boolean hasDefaultValues(IslandVisitSettings settings)
    {
        return this.addon.getSettings().getDefaultVisitingPayment() == settings.getPayment() &&
            this.addon.getSettings().isDefaultVisitingOffline() == settings.isOfflineVisit();
    }


    // ---------------------------------------------------------------------
    // Section: Local cache access data
    // ---------------------------------------------------------------------


    /**
     * This method adds given gamemode to the enabled addon list.
     *
     * @param addon Addon that must be added to enabled addon list.
     */
    public void addGameMode(GameModeAddon addon)
    {
        this.enabledAddonList.add(addon);
    }


    /**
     * This method returns the enabledAddonList value.
     *
     * @return the value of enabledAddonList.
     */
    public List<GameModeAddon> getEnabledAddonList()
    {
        return enabledAddonList;
    }


    /**
     * This method checks if given user has enough credits.
     *
     * @param user Targeted user.
     * @param credits Amount that must be checked.
     * @return {@code true} if vaultHook is enabled and player has enough credits, {@code false} otherwise
     */
    public boolean hasCredits(User user, double credits)
    {
        return this.addon.getVaultHook() == null ||
            !this.addon.getVaultHook().hook() ||
            this.addon.getVaultHook().has(user, credits);
    }


    /**
     * This method deposits given amount to player account.
     *
     * @param user Targeted user.
     * @param credits Amount that must be deposited.
     * @return {@code true} if vaultHook not enabled or transaction was successful, {@code false} otherwise
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


    // ---------------------------------------------------------------------
    // Section: Methods
    // ---------------------------------------------------------------------


    /**
     * This method withdraws given amount from player account.
     *
     * @param user Targeted user.
     * @param credits Amount that must be removed.
     * @return {@code true} if vaultHook is not enabled or transaction was successful, {@code false} otherwise
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


    /**
     * This method checks if teleportation can be performed.
     *
     * @param user Targeted user who need to be teleported.
     * @param island Island where user need to be teleported.
     * @return {@code true} if teleportation can be performed, {@code false} otherwise.
     */
    public boolean preprocessTeleportation(User user, Island island)
    {
        return this.preprocessTeleportation(user, island, this.getIslandVisitSettings(island));
    }


    // ---------------------------------------------------------------------
    // Section: VaultHook wrapper
    // ---------------------------------------------------------------------


    /**
     * This method checks if teleportation can be performed.
     *
     * @param user Targeted user who need to be teleported.
     * @param island Island where user need to be teleported.
     * @param settings Island Visit Settings object.
     * @return {@code true} if teleportation can be performed, {@code false} otherwise.
     */
    public boolean preprocessTeleportation(User user, Island island, IslandVisitSettings settings)
    {
        double payment = settings.getPayment() + this.addon.getSettings().getTaxAmount();

        if (Flags.PREVENT_TELEPORT_WHEN_FALLING.isSetForWorld(user.getWorld()) &&
            user.getPlayer().getFallDistance() > 0)
        {
            // We're sending the "hint" to the player to tell them they cannot teleport while falling.
            Utils.sendMessage(user,
                user.getTranslation(Flags.PREVENT_TELEPORT_WHEN_FALLING.getHintReference()));
        }
        else if (island.isBanned(user.getUniqueId()))
        {
            // Banned players are not allowed.
            Utils.sendMessage(user,
                user.getTranslation("commands.island.ban.you-are-banned"));
        }
        else if (!island.isAllowed(user, Flags.LOCK))
        {
            // Island is locked.
            Utils.sendMessage(user,
                user.getTranslation("protection.locked"));
        }
        else if (!island.isAllowed(VisitAddon.ALLOW_VISITS_FLAG))
        {
            // Visits are disabled in settings.
            Utils.sendMessage(user,
                user.getTranslation(VisitAddon.ALLOW_VISITS_FLAG.getHintReference()));
        }
        else if (!this.canVisitOffline(island, settings))
        {
            // Send a message that noone is online from this island
            Utils.sendMessage(user,
                user.getTranslation(Constants.ERRORS + "noone-is-online"));
        }
        else if (payment > 0 && !this.hasCredits(user, payment))
        {
            // Send a message that player has not enough credits.
            Utils.sendMessage(user,
                user.getTranslation(Constants.ERRORS + "not-enough-credits",
                    Constants.PARAMETER_NUMBER, String.valueOf(payment)));
        }
        else
        {
            // All other checks failed. Teleportation can be performed.
            return true;
        }

        // Return statement at the end is always false.
        return false;
    }


    /**
     * This method checks if any island member is online or offline visiting option is enabled.
     *
     * @param island Island that must be checked.
     * @param settings Island Visit Settings object.
     * @return {@code true} if offline visiting is enabled or any member is online, {@code false} otherwise.
     */
    public boolean canVisitOffline(Island island, IslandVisitSettings settings)
    {
        // Check if settings allow offline visiting or any island member is online.
        return settings.isOfflineVisit() ||
            island.getMemberSet().stream().
                map(User::getInstance).
                filter(Objects::nonNull).
                anyMatch(User::isOnline);
    }


    /**
     * This method process user teleportation to the given island.
     *
     * @param user Targeted user who need to be teleported.
     * @param island Island where user need to be teleported.
     */
    public void processTeleportation(User user,
        Island island)
    {
        this.processTeleportation(user, island, this.getIslandVisitSettings(island));
    }


    // ---------------------------------------------------------------------
    // Section: Teleportation methods
    // ---------------------------------------------------------------------


    /**
     * This method process user teleportation to the given island.
     *
     * @param user Targeted user who need to be teleported.
     * @param island Island where user need to be teleported.
     * @param settings IslandVisitSettings object.
     */
    public void processTeleportation(User user,
        Island island,
        IslandVisitSettings settings)
    {
        double payment = settings.getPayment() + this.addon.getSettings().getTaxAmount();

        if (payment > 0 && !this.withdrawCredits(user, payment))
        {
            // error on withdrawing credits. Cancelling
            Utils.sendMessage(user,
                user.getTranslation(Constants.ERRORS + "cannot-withdraw-credits",
                    Constants.PARAMETER_NUMBER, String.valueOf(payment)));
            return;
        }
        else if (settings.getPayment() > 0 &&
            !this.depositCredits(User.getInstance(island.getOwner()), settings.getPayment()))
        {
            // error on depositing credits. Cancelling
            this.depositCredits(user, settings.getPayment() + this.addon.getSettings().getTaxAmount());

            Utils.sendMessage(user,
                user.getTranslation(Constants.ERRORS + "cannot-deposit-credits",
                    Constants.PARAMETER_NUMBER, String.valueOf(settings.getPayment())));
            return;
        }

        // Call visit event.
        VisitEvent event = new VisitEvent(user.getUniqueId(), island);
        Bukkit.getPluginManager().callEvent(event);

        // If event is not cancelled, then teleport player.
        if (!event.isCancelled())
        {
            Location location = island.getSpawnPoint(World.Environment.NORMAL);

            if (location == null || !this.addon.getIslands().isSafeLocation(location))
            {
                // Use SafeSpotTeleport builder to avoid issues with players spawning in
                // bad spot.
                new SafeSpotTeleport.Builder(this.addon.getPlugin()).
                    entity(user.getPlayer()).
                    location(location == null ? island.getCenter() : location).
                    failureMessage(user.getTranslation("general.errors.no-safe-location-found")).
                    build();
            }
            else
            {
                // Teleport player async to island spawn point.
                Util.teleportAsync(user.getPlayer(), location);
            }
        }
    }

    /**
     * Local variable that stores all GameMode addons where current addon is enabled.
     */
    private final List<GameModeAddon> enabledAddonList;

    /**
     * Reference to main addon class.
     */
    private final VisitAddon addon;

    /**
     * This config object stores structures for island visit settings objects.
     */
    private final Database<IslandVisitSettings> visitSettingsDatabase;

    /**
     * This is local cache that links island unique id with island visit settings object.
     */
    private final Map<String, IslandVisitSettings> visitSettingsCacheData;
}
