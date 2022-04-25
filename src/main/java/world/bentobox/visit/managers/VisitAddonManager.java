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
import java.util.concurrent.CompletableFuture;

import world.bentobox.bank.BankResponse;
import world.bentobox.bank.data.Money;
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
        return island.getMetaData(Constants.METADATA_OFFLINE).map(MetaDataValue::asBoolean).orElse(this.addon.getSettings().isDefaultVisitingOffline());
    }


    /**
     * Gets island earnings that are set in Island Payment Metadata. If metadata is not set up, return default value.
     *
     * @param island the island
     * @return the island earnings
     */
    public double getIslandEarnings(Island island)
    {
        if (this.addon.getSettings().isDisableEconomy() ||
            this.addon.getVaultHook() == null ||
            !this.addon.getVaultHook().hook())
        {
            // Return 0 if economy is disabled, vault hook does not exist or vault is not hooked int economy.
            return 0;
        }
        else
        {
            return island.getMetaData(Constants.METADATA_PAYMENT).
                map(MetaDataValue::asDouble).
                orElse(this.addon.getSettings().getDefaultVisitingPayment());
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
        if (island.getMetaData().isEmpty())
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
        if (island.getMetaData().isEmpty())
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
     * Returns tax amount value.
     *
     * @return the tax amount
     */
    public double getTaxAmount()
    {
        if (this.addon.getSettings().isDisableEconomy() ||
            this.addon.getVaultHook() == null ||
            !this.addon.getVaultHook().hook())
        {
            // Return 0 if economy is disabled, vault hook does not exist or vault is not hooked int economy.
            return 0;
        }
        else
        {
            return this.addon.getSettings().getTaxAmount();
        }
    }


    /**
     * This method checks if given user has enough credits.
     *
     * @param user Targeted user.
     * @param credits Amount that must be checked.
     * @return {@code true} if vaultHook is enabled and player has enough credits, {@code false} otherwise
     */
    public boolean hasCredits(User user, double credits, World world)
    {
        if (this.addon.getSettings().isDisableEconomy() || credits <= 0)
        {
            return true;
        }
        else if (this.addon.getSettings().isUseIslandBank())
        {
            return this.addon.getBankHook() == null ||
                this.addon.getBankHook().getBankManager().getBalance(user, world).getValue() >= credits;
        }
        else
        {
            return this.addon.getVaultHook() == null ||
                !this.addon.getVaultHook().hook() ||
                this.addon.getVaultHook().has(user, credits);
        }
    }


    /**
     * This method deposits given amount to player account.
     *
     * @param user Targeted user.
     * @param credits Amount that must be deposited.
     */
    public void depositCredits(User user,
        double credits,
        World world,
        CompletableFuture<Boolean> deposit,
        String message)
    {
        if (this.addon.getSettings().isDisableEconomy() || credits <= 0)
        {
            // Economy is disabled.
            deposit.complete(true);
        }
        else if (this.addon.getSettings().isUseIslandBank())
        {
            // Process bank deposit.
            if (this.addon.getBankHook() != null)
            {
                this.addon.getBankHook().getBankManager().
                    deposit(user, new Money(credits), world).
                    whenComplete(((bankResponse, throwable) -> {
                        if (throwable != null)
                        {
                            deposit.completeExceptionally(new Throwable("FAILED_DEPOSIT"));
                        }
                        else if (bankResponse == BankResponse.SUCCESS)
                        {
                            Utils.sendMessage(user, message);
                            deposit.complete(true);
                        }
                        else
                        {
                            deposit.completeExceptionally(new Throwable(bankResponse.name()));
                        }
                    }));
            }
            else
            {
                deposit.completeExceptionally(new Throwable("MISSING_BANK_ADDON"));
                this.addon.logError("Missing Bank Addon. Cannot proceed with payments.");
            }
        }
        else if (this.addon.getVaultHook() != null && this.addon.getVaultHook().hook())
        {
            // Process Vault deposit.
            if (this.addon.getVaultHook().deposit(user, credits).transactionSuccess())
            {
                Utils.sendMessage(user, message);
                deposit.complete(true);
            }
            else
            {
                deposit.complete(false);
            }
        }
        else
        {
            deposit.completeExceptionally(new Throwable("MISSING_VAULT"));
            this.addon.logError("Missing Vault Plugin. Cannot proceed with payments.");
        }
    }


    /**
     * This method withdraws given amount from player account.
     *
     * @param user Targeted user.
     * @param credits Amount that must be removed.
     * @param world the world
     * @param withdraw the withdraw feature
     */
    public void withdrawCredits(User user,
        double credits,
        World world,
        CompletableFuture<Boolean> withdraw,
        String message)
    {
        if (this.addon.getSettings().isDisableEconomy() || credits <= 0)
        {
            // Economy is disabled.
            withdraw.complete(true);
        }
        else if (this.addon.getSettings().isUseIslandBank())
        {
            // Process bank withdraw.
            if (this.addon.getBankHook() != null)
            {
                this.addon.getBankHook().getBankManager().
                    withdraw(user, new Money(credits), world).
                    whenComplete(((bankResponse, throwable) -> {
                        if (throwable != null)
                        {
                            withdraw.completeExceptionally(new Throwable("FAILED_WITHDRAW"));
                        }
                        else if (bankResponse == BankResponse.SUCCESS)
                        {
                            Utils.sendMessage(user, message);
                            withdraw.complete(true);
                        }
                        else
                        {
                            withdraw.completeExceptionally(new Throwable(bankResponse.name()));
                        }
                    }));
            }
            else
            {
                withdraw.completeExceptionally(new Throwable("MISSING_BANK_ADDON"));
                this.addon.logError("Missing Bank Addon. Cannot proceed with payments.");
            }
        }
        else if (this.addon.getVaultHook() != null && this.addon.getVaultHook().hook())
        {
            if (this.addon.getVaultHook().withdraw(user, credits).transactionSuccess())
            {
                Utils.sendMessage(user, message);
                // Process Vault withdraw.
                withdraw.complete(true);
            }
            else
            {
                withdraw.complete(false);
            }
        }
        else
        {
            withdraw.completeExceptionally(new Throwable("MISSING_VAULT"));
            this.addon.logError("Missing Vault Plugin. Cannot proceed with payments.");
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
        return this.preprocessTeleportation(user, island, false);
    }


    /**
     * This method checks if teleportation can be performed.
     *
     * @param user Targeted user who need to be teleported.
     * @param island Island where user need to be teleported.
     * @param silent Should the message be sent to the player why teleportation is not possible.
     * @return {@code true} if teleportation can be performed, {@code false} otherwise.
     */
    public boolean preprocessTeleportation(User user, Island island, boolean silent)
    {
        double payment = this.getTaxAmount() + this.getIslandEarnings(island);

        if (Flags.PREVENT_TELEPORT_WHEN_FALLING.isSetForWorld(user.getWorld()) &&
            user.getPlayer().getFallDistance() > 0)
        {
            if (!silent)
            {
                // We're sending the "hint" to the player to tell them they cannot teleport while falling.
                Utils.sendMessage(user,
                    user.getTranslation(Flags.PREVENT_TELEPORT_WHEN_FALLING.getHintReference()));
            }
        }
        else if (island.isBanned(user.getUniqueId()))
        {
            if (!silent)
            {
                // Banned players are not allowed.
                Utils.sendMessage(user,
                    user.getTranslation("commands.island.ban.you-are-banned"));
            }
        }
        else if (!island.isAllowed(user, Flags.LOCK))
        {
            if (!silent)
            {
                // Island is locked.
                Utils.sendMessage(user,
                    user.getTranslation("protection.locked"));
            }
        }
        else if (!island.isAllowed(VisitAddon.ALLOW_VISITS_FLAG))
        {
            if (!silent)
            {
                // Visits are disabled in settings.
                Utils.sendMessage(user,
                    user.getTranslation(VisitAddon.ALLOW_VISITS_FLAG.getHintReference()));
            }
        }
        else if (!this.canVisitOffline(island))
        {
            if (!silent)
            {
                // Send a message that noone is online from this island
                Utils.sendMessage(user,
                    user.getTranslation(Constants.ERRORS + "noone-is-online"));
            }
        }
        else if (payment > 0 && !this.hasCredits(user, payment, island.getWorld()))
        {
            if (!silent)
            {
                if (this.addon.getSettings().isUseIslandBank())
                {
                    // Send a message that player has not enough credits.
                    Utils.sendMessage(user,
                        user.getTranslation(Constants.ERRORS + "not-enough-credits-bank",
                            Constants.PARAMETER_NUMBER, String.valueOf(payment)));
                }
                else
                {
                    // Send a message that player has not enough credits.
                    Utils.sendMessage(user,
                        user.getTranslation(Constants.ERRORS + "not-enough-credits",
                            Constants.PARAMETER_NUMBER, String.valueOf(payment)));
                }
            }
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
    public void processTeleportation(User user, Island island, World world)
    {
        double earnedMoney = this.getIslandEarnings(island);
        double payment = earnedMoney + this.getTaxAmount();

        final String paymentMessage;
        final String receiveMessage;

        if (this.addon.getSettings().isUseIslandBank())
        {
            paymentMessage = user.getTranslation(Constants.CONVERSATIONS + "visit-paid-bank",
                Constants.PARAMETER_PAYMENT, String.valueOf(payment),
                Constants.PARAMETER_TAX, String.valueOf(this.getTaxAmount()),
                Constants.PARAMETER_ISLAND, String.valueOf(island.getName()),
                Constants.PARAMETER_OWNER, this.addon.getPlayers().getName(island.getOwner()),
                Constants.PARAMETER_RECEIVER, String.valueOf(earnedMoney));
            receiveMessage = user.getTranslation(Constants.CONVERSATIONS + "visit-earn-bank",
                Constants.PARAMETER_PAYMENT, String.valueOf(earnedMoney),
                Constants.PARAMETER_PLAYER, user.getName());
        }
        else
        {
            paymentMessage = user.getTranslation(Constants.CONVERSATIONS + "visit-paid",
                Constants.PARAMETER_PAYMENT, String.valueOf(payment),
                Constants.PARAMETER_TAX, String.valueOf(this.getTaxAmount()),
                Constants.PARAMETER_ISLAND, String.valueOf(island.getName()),
                Constants.PARAMETER_OWNER, this.addon.getPlayers().getName(island.getOwner()),
                Constants.PARAMETER_RECEIVER, String.valueOf(earnedMoney));
            receiveMessage = user.getTranslation(Constants.CONVERSATIONS + "visit-earn",
                Constants.PARAMETER_PAYMENT, String.valueOf(earnedMoney),
                Constants.PARAMETER_PLAYER, user.getName());
        }

        // Depositing credits into visited island bank account.
        CompletableFuture<Boolean> deposit = new CompletableFuture<>();
        deposit.whenComplete((value, throwable) -> {
            if (throwable != null)
            {
                this.depositCredits(user, payment, world, new CompletableFuture<>(),
                    user.getTranslation(Constants.ERRORS + "cannot-deposit-credits",
                        Constants.PARAMETER_NUMBER, String.valueOf(payment)));
            }
            else if (value)
            {
                this.startTeleportation(user, island);
            }
        });

        // Withdrawing credits from user island bank account.
        CompletableFuture<Boolean> withdraw = new CompletableFuture<>();
        withdraw.whenComplete((value, throwable) -> {
            if (throwable != null || !value)
            {
                Utils.sendMessage(user,
                    user.getTranslation(Constants.ERRORS + "cannot-withdraw-credits",
                        Constants.PARAMETER_NUMBER, String.valueOf(payment)));
            }
            else
            {
                if (earnedMoney > 0 && island.getOwner() != null)
                {
                    this.depositCredits(
                        User.getInstance(island.getOwner()),
                        earnedMoney,
                        world,
                        deposit,
                        receiveMessage);
                }
                else
                {
                    deposit.complete(true);
                }
            }
        });

        // Process withdraw.
        this.withdrawCredits(user, payment, world, withdraw, paymentMessage);
    }


    /**
     * Start teleportation sequence by firing bukkit event and building safe spot.
     *
     * @param user the user
     * @param island the island
     */
    private void startTeleportation(User user, Island island)
    {
        // Call visit event.
        VisitEvent event = new VisitEvent(user, island);
        Bukkit.getPluginManager().callEvent(event);

        // If event is not cancelled, then teleport player.
        if (!event.isCancelled())
        {
            Location location = island.getSpawnPoint(World.Environment.NORMAL);

            // There is a possibility that location may be out of protected area. These locations should
            // not be valid for teleporting.
            if (location != null &&
                island.getProtectionBoundingBox().contains(location.toVector()) &&
                this.addon.getIslands().isSafeLocation(location))
            {
                // Teleport player async to island spawn point.
                Util.teleportAsync(user.getPlayer(), location);
            }
            else
            {
                // Use SafeSpotTeleport builder to avoid issues with players spawning in
                // bad spot.
                new SafeSpotTeleport.Builder(this.addon.getPlugin()).
                    entity(user.getPlayer()).
                    location(location == null ? island.getProtectionCenter() : location).
                    failureMessage(user.getTranslation("general.errors.no-safe-location-found")).
                    build();
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
