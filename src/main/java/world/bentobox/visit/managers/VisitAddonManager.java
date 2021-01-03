//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit.managers;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.events.VisitEvent;
import world.bentobox.visit.utils.Constants;
import world.bentobox.visit.utils.Utils;


/**
 * This class manages data handling and option processing for Visit Addon.
 */
public class VisitAddonManager
{
    /**
     * Constructor VisitAddonManager creates a new VisitAddonManager instance.
     *
     * @param addon of type VisitAddon
     */
    public VisitAddonManager(VisitAddon addon)
    {
        this.addon = addon;
        this.enabledAddonList = new ArrayList<>(5);
    }


// ---------------------------------------------------------------------
// Section: Methods
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
     * This method checks if any island member is online or offline visiting option is enabled.
     *
     * @param island Island that must be checked.
     * @return {@code true} if offline visiting is enabled or any member is online, {@code false} otherwise.
     */
    public boolean canVisitOffline(Island island)
    {
        // Check if settings allow offline visiting or any island member is online.

        return this.hasOfflineEnabled(island) ||
            island.getMemberSet().stream().
                map(User::getInstance).
                filter(Objects::nonNull).
                anyMatch(User::isOnline);
    }


    /**
     * Has offline enabled for given island. It checks if island has Offline Metadata and return its value. If metadata
     * is not present, then it returns default value for the flag.
     *
     * @param island the island
     * @return the boolean
     */
    public boolean hasOfflineEnabled(Island island)
    {
        if (island.getMetaData() != null && island.getMetaData().containsKey(Constants.METADATA_OFFLINE))
        {
            return island.getMetaData(Constants.METADATA_OFFLINE).asBoolean();
        }
        else
        {
            return this.addon.getSettings().isDefaultVisitingOffline();
        }
    }


    /**
     * Gets island earnings that are set in Island Payment Metadata. If metadata is not set up, return default value.
     *
     * @param island the island
     * @return the island earnings
     */
    public double getIslandEarnings(Island island)
    {
        if (island.getMetaData() != null && island.getMetaData().containsKey(Constants.METADATA_PAYMENT))
        {
            return island.getMetaData(Constants.METADATA_PAYMENT).asDouble();
        }
        else
        {
            return this.addon.getSettings().getDefaultVisitingPayment();
        }
    }


    /**
     * This method changes payment value to the island.
     *
     * @param island the island
     * @param payment new payment value
     */
    public void setIslandEarnings(Island island, double payment)
    {
        if (island.getMetaData() == null)
        {
            // Init new hashmap with 2 elements.
            island.setMetaData(new HashMap<>(2));
        }

        // Add new value to the metadata.
        island.putMetaData(Constants.METADATA_PAYMENT, new MetaDataValue(payment));
    }


    /**
     * Add offline data value to the island metadata.
     *
     * @param island the island
     * @param newValue the new value
     */
    public void setOfflineData(Island island, boolean newValue)
    {
        if (island.getMetaData() == null)
        {
            // Init new hashmap with 2 elements.
            island.setMetaData(new HashMap<>(2));
        }

        // Add new value to the metadata.
        island.putMetaData(Constants.METADATA_OFFLINE, new MetaDataValue(newValue));
    }


// ---------------------------------------------------------------------
// Section: VaultHook methods
// ---------------------------------------------------------------------


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


// ---------------------------------------------------------------------
// Section: Teleportation methods
// ---------------------------------------------------------------------


    /**
     * This method checks if teleportation can be performed.
     *
     * @param user Targeted user who need to be teleported.
     * @param island Island where user need to be teleported.
     * @return {@code true} if teleportation can be performed, {@code false} otherwise.
     */
    public boolean preprocessTeleportation(User user, Island island)
    {
        double payment = this.addon.getSettings().getTaxAmount() + this.getIslandEarnings(island);

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
        else if (!this.canVisitOffline(island))
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
     * This method process user teleportation to the given island.
     *
     * @param user Targeted user who need to be teleported.
     * @param island Island where user need to be teleported.
     */
    public void processTeleportation(User user, Island island)
    {
        double earnedMoney = this.getIslandEarnings(island);
        double payment = earnedMoney + this.addon.getSettings().getTaxAmount();

        if (payment > 0 && !this.withdrawCredits(user, payment))
        {
            // error on withdrawing credits. Cancelling
            Utils.sendMessage(user,
                user.getTranslation(Constants.ERRORS + "cannot-withdraw-credits",
                    Constants.PARAMETER_NUMBER, String.valueOf(payment)));
            return;
        }
        else if (earnedMoney > 0 &&
            !this.depositCredits(User.getInstance(island.getOwner()), earnedMoney))
        {
            // error on depositing credits. Cancelling
            this.depositCredits(user, earnedMoney + this.addon.getSettings().getTaxAmount());

            Utils.sendMessage(user,
                user.getTranslation(Constants.ERRORS + "cannot-deposit-credits",
                    Constants.PARAMETER_NUMBER, String.valueOf(earnedMoney)));
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


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


    /**
     * Local variable that stores all GameMode addons where current addon is enabled.
     */
    private final List<GameModeAddon> enabledAddonList;

    /**
     * Reference to main addon class.
     */
    private final VisitAddon addon;
}
