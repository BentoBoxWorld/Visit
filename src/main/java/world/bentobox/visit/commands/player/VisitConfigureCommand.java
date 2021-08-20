//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit.commands.player;


import org.bukkit.Location;
import org.bukkit.event.entity.ItemSpawnEvent;
import java.util.List;
import java.util.Optional;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.panels.player.ConfigurePanel;
import world.bentobox.visit.utils.Constants;


/**
 * This class manages {@code /{player_command} visit configure} command call.
 */
public class VisitConfigureCommand extends CompositeCommand
{
    /**
     * This is simple constructor for initializing /{player_command} visit configure command.
     *
     * @param addon Our Visit addon.
     * @param parentCommand Parent Command where we hook our command into.
     */
    public VisitConfigureCommand(VisitAddon addon, CompositeCommand parentCommand)
    {
        super(addon,
            parentCommand,
            addon.getSettings().getPlayerConfigureCommand().split(" ")[0],
            addon.getSettings().getPlayerConfigureCommand().split(" "));
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
        this.setPermission("visit.configure");
        this.setParametersHelp(Constants.PLAYER_COMMANDS + "configure.parameters");
        this.setDescription(Constants.PLAYER_COMMANDS + "configure.description");

        this.setOnlyPlayer(true);
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
        Island island = this.getIslands().getIsland(this.getWorld(), user);

        if (island == null)
        {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        return island.isAllowed(user, VisitAddon.VISIT_CONFIG_PERMISSION);
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
        ConfigurePanel.openPanel(this.getAddon(),
            this.getIslands().getIsland(this.getWorld(), user),
            user);

        return true;
    }
}
