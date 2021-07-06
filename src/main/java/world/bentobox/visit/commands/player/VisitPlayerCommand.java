//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit.commands.player;


import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.DelayedTeleportCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.panels.player.VisitPanel;
import world.bentobox.visit.utils.Constants;


/**
 * This class process /{player_command} visit command call.
 */
public class VisitPlayerCommand extends DelayedTeleportCommand
{
    /**
     * This is simple constructor for initializing /{player_command} visit command.
     *
     * @param addon Our Visit addon.
     * @param parentCommand Parent Command where we hook our command into.
     */
    public VisitPlayerCommand(VisitAddon addon, CompositeCommand parentCommand)
    {
        super(addon,
            parentCommand,
            addon.getSettings().getPlayerMainCommand().split(" ")[0],
            addon.getSettings().getPlayerMainCommand().split(" "));
    }


    /**
     * Setups anything that is needed for this command. <br/><br/> It is recommended you do the following in this
     * method:
     * <ul>
     * <li>Register any of the sub-commands of this command;</li>
     * <li>Define the permission required to use this command using {@link
     * CompositeCommand#setPermission(String)};</li>
     * <li>Define whether this command can only be run by players or not using {@link
     * CompositeCommand#setOnlyPlayer(boolean)};</li>
     * </ul>
     */
    @Override
    public void setup()
    {
        this.setPermission("visit");
        this.setParametersHelp(Constants.PLAYER_COMMANDS + "main.parameters");
        this.setDescription(Constants.PLAYER_COMMANDS + "main.description");

        new VisitConfigureCommand(this.getAddon(), this);
        new VisitSetLocationCommand(this.getAddon(), this);

        this.setOnlyPlayer(true);
    }


    /**
     * Ask confirmation for teleportation.
     * @param user User who need to confirm.
     * @param message Message that will be set to user.
     * @param confirmed Confirm task.
     */
    public void askConfirmation(User user, String message, Runnable confirmed)
    {
        // Check for pending confirmations
        if (toBeConfirmed.containsKey(user))
        {
            if (toBeConfirmed.get(user).getTopLabel().equals(getTopLabel()) &&
                toBeConfirmed.get(user).getLabel().equalsIgnoreCase(getLabel()))
            {
                toBeConfirmed.get(user).getTask().cancel();
                Bukkit.getScheduler().runTask(getPlugin(), toBeConfirmed.get(user).getRunnable());
                toBeConfirmed.remove(user);
                return;
            }
            else
            {
                // Player has another outstanding confirmation request that will now be cancelled
                user.sendMessage("commands.confirmation.previous-request-cancelled");
            }
        }
        // Send user the context message if it is not empty
        if (!message.trim().isEmpty())
        {
            user.sendRawMessage(message);
        }
        // Tell user that they need to confirm
        user.sendMessage("commands.confirmation.confirm",
            "[seconds]",
            String.valueOf(getSettings().getConfirmationTime()));
        // Set up a cancellation task
        BukkitTask task = Bukkit.getScheduler().runTaskLater(getPlugin(), () ->
        {
            user.sendMessage("commands.confirmation.request-cancelled");
            toBeConfirmed.remove(user);
        }, getPlugin().getSettings().getConfirmationTime() * 20L);

        // Add to the global confirmation map
        toBeConfirmed.put(user, new Confirmer(getTopLabel(), getLabel(), confirmed, task));
    }


    /**
     * Returns whether the command can be executed by this user or not. It is recommended to send messages to let this
     * user know why they could not execute the command. Note that this is run previous to {@link #execute(User, String,
     * List)}.
     *
     * @param user the {@link User} who is executing this command.
     * @param label the label which has been used to execute this command. It can be {@link CompositeCommand#getLabel()}
     * or an alias.
     * @param args the command arguments.
     * @return {@code true} if this command can be executed, {@code false} otherwise.
     * @since 1.3.0
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args)
    {
        if (args.isEmpty())
        {
            // Open panel. No checks required.
            return true;
        }
        else if (args.size() == 1)
        {
            UUID targetUUID = Util.getUUID(args.get(0));

            if (targetUUID == null)
            {
                user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
                return false;
            }
            else
            {
                // Use getIsland as it returns island even if player is in team.
                this.island = this.getIslands().getIsland(this.getWorld(), targetUUID);

                if (this.island == null)
                {
                    // There is no place to teleport.
                    user.sendMessage("general.errors.player-has-no-island");
                    return false;
                }
                else
                {
                    // Return preprocess result from teleportation.
                    return this.<VisitAddon>getAddon().getAddonManager().
                        preprocessTeleportation(user, this.island);
                }
            }
        }

        return true;
    }


    /**
     * Defines what will be executed when this command is run.
     *
     * @param user the {@link User} who is executing this command.
     * @param label the label which has been used to execute this command. It can be {@link CompositeCommand#getLabel()}
     * or an alias.
     * @param args the command arguments.
     * @return {@code true} if the command executed successfully, {@code false} otherwise.
     */
    @Override
    public boolean execute(User user, String label, List<String> args)
    {
        if (args.isEmpty())
        {
            VisitPanel.openPanel(this.getAddon(), this.getWorld(), user, this.getTopLabel());
        }
        else if (args.size() == 1)
        {
            double tax;
            double earnings;

            if (this.<VisitAddon>getAddon().getSettings().isDisableEconomy())
            {
                tax = 0;
                earnings = 0;
            }
            else
            {
                // check tax and island earnings that if economy is enabled.
                tax = this.<VisitAddon>getAddon().getSettings().getTaxAmount();
                earnings = this.<VisitAddon>getAddon().getAddonManager().getIslandEarnings(this.island);
            }

            String prefix = user.getTranslation(Constants.CONVERSATIONS + "prefix");
            String message = prefix +
                user.getTranslation(Constants.CONVERSATIONS + "visit-payment",
                    Constants.PARAMETER_PAYMENT, String.valueOf(tax + earnings),
                    Constants.PARAMETER_TAX, String.valueOf(tax),
                    Constants.PARAMETER_ISLAND, String.valueOf(this.island.getName()),
                    Constants.PARAMETER_OWNER, this.getPlayers().getName(this.island.getOwner()),
                    Constants.PARAMETER_RECEIVER, String.valueOf(earnings));

            if (this.<VisitAddon>getAddon().getSettings().isPaymentConfirmation() && (tax + earnings) > 0)
            {
                // If there is associated cost, then ask confirmation from user.
                this.askConfirmation(user,
                    message,
                    () -> this.delayCommand(user, () ->
                        this.<VisitAddon>getAddon().getAddonManager().processTeleportation(user, this.island)));
            }
            else
            {
                // Execute teleportation without confirmation.
                this.delayCommand(user,
                    (tax + earnings > 0) ? message : "",
                    () -> this.<VisitAddon>getAddon().getAddonManager().processTeleportation(user, this.island));
            }
        }
        else
        {
            this.showHelp(this, user);
        }

        return true;
    }


    /**
     * Tab Completer for CompositeCommands. Note that any registered sub-commands will be automatically added to the
     * list. Use this to add tab-complete for things like names.
     *
     * @param user the {@link User} who is executing this command.
     * @param alias alias for command
     * @param args command arguments
     * @return List of strings that could be used to complete this command.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args)
    {
        // TODO: nice addition would be to autocomplete user names.
        return super.tabComplete(user, alias, args);
    }


    // ---------------------------------------------------------------------
    // Section: Classes
    // ---------------------------------------------------------------------


    /**
     * This is clone from BentoBox ConfirmableCommand class.
     */
    private static class Confirmer {
        private final String topLabel;
        private final String label;
        private final Runnable runnable;
        private final BukkitTask task;

        /**
         * @param label - command label
         * @param runnable - runnable to run when confirmed
         * @param task - task ID to cancel when confirmed
         */
        Confirmer(String topLabel, String label, Runnable runnable, BukkitTask task) {
            this.topLabel = topLabel;
            this.label = label;
            this.runnable = runnable;
            this.task = task;
        }
        /**
         * @return the topLabel
         */
        public String getTopLabel() {
            return topLabel;
        }
        /**
         * @return the label
         */
        public String getLabel() {
            return label;
        }
        /**
         * @return the runnable
         */
        public Runnable getRunnable() {
            return runnable;
        }
        /**
         * @return the task
         */
        public BukkitTask getTask() {
            return task;
        }
    }


    // ---------------------------------------------------------------------
    // Section: Variables
    // ---------------------------------------------------------------------


    /**
     * Island instance to which player will be teleported.
     */
    private Island island;

    /**
     * Map that contains which users are in confirmation process.
     */
    private static final Map<User, Confirmer> toBeConfirmed = new HashMap<>();
}
