package world.bentobox.visit.commands.admin;


import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.panels.admin.AdminPanel;
import world.bentobox.visit.panels.player.ConfigurePanel;


/**
 * This class manages {@code /{admin_command} visit} command call.
 */
public class VisitAdminCommand extends CompositeCommand
{
    /**
     * This is simple constructor for initializing /{admin_command} visit command.
     *
     * @param addon Our Visit addon.
     * @param parentCommand Parent Command where we hook our command into.
     */
    public VisitAdminCommand(VisitAddon addon, CompositeCommand parentCommand)
    {
        super(addon, parentCommand, "visit");
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
        this.setPermission("admin.visit");
        this.setParametersHelp("visit.commands.admin.main.parameters");
        this.setDescription("visit.commands.admin.main.description");

        this.setOnlyPlayer(true);
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
            AdminPanel.openPanel(this.getAddon(), this.getWorld(), user);
        }
        else if (args.size() == 1)
        {
            UUID targetUUID = Util.getUUID(args.get(0));

            if (targetUUID == null)
            {
                user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            }
            else
            {
                // Use getIsland as it returns island even if player is in team.
                Island island = this.getIslands().getIsland(this.getWorld(), targetUUID);

                if (island == null)
                {
                    // There is no place to teleport.
                    user.sendMessage("general.errors.player-has-no-island");
                }
                else
                {
                    // Process config opening
                    ConfigurePanel.openPanel(this.getAddon(), island, user);
                }
            }
        }
        else
        {
            this.showHelp(this, user);
        }

        return true;
    }
}
