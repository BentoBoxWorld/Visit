package world.bentobox.visit.commands.player;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.panels.player.VisitPanel;


/**
 * This class process /{player_command} visit command call.
 */
public class VisitPlayerCommand extends CompositeCommand
{
	/**
	 * This is simple constructor for initializing /{player_command} visit command.
	 * @param addon Our Visit addon.
	 * @param parentCommand Parent Command where we hook our command into.
	 */
	public VisitPlayerCommand(VisitAddon addon, CompositeCommand parentCommand)
	{
		super(addon, parentCommand, "visit");
	}


	/**
	 * Setups anything that is needed for this command. <br/><br/> It is recommended you
	 * do the following in this method:
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
		this.setParametersHelp("visit.commands.player.main.parameters");
		this.setDescription("visit.commands.player.main.description");

		new VisitConfigureCommand(this.getAddon(), this);

		this.setOnlyPlayer(true);
	}


	/**
	 * Returns whether the command can be executed by this user or not. It is recommended
	 * to send messages to let this user know why they could not execute the command. Note
	 * that this is run previous to {@link #execute(User, String, List)}.
	 *
	 * @param user the {@link User} who is executing this command.
	 * @param label the label which has been used to execute this command. It can be
	 * {@link CompositeCommand#getLabel()} or an alias.
	 * @param args the command arguments.
	 * @return {@code true} if this command can be executed, {@code false} otherwise.
	 * @since 1.3.0
	 */
	@Override
	public boolean canExecute(User user, String label, List<String> args)
	{
		return true;
	}


	/**
	 * Defines what will be executed when this command is run.
	 *
	 * @param user the {@link User} who is executing this command.
	 * @param label the label which has been used to execute this command. It can be
	 * {@link CompositeCommand#getLabel()} or an alias.
	 * @param args the command arguments.
	 * @return {@code true} if the command executed successfully, {@code false} otherwise.
	 */
	@Override
	public boolean execute(User user, String label, List<String> args)
	{
		if (args.isEmpty())
		{
			VisitPanel.openPanel(this.getAddon(), this.getWorld(), user);
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
					// Process teleporation
					this.<VisitAddon>getAddon().getAddonManager().processTeleportation(user, island);
				}
			}
		}
		else
		{
			this.showHelp(this, user);
		}

		return true;
	}


	/**
	 * Tab Completer for CompositeCommands. Note that any registered sub-commands will be
	 * automatically added to the list. Use this to add tab-complete for things like
	 * names.
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
}
