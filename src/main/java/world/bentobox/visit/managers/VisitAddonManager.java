package world.bentobox.visit.managers;


import java.util.ArrayList;
import java.util.List;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.visit.VisitAddon;


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
	// Section: Database methods
	// ---------------------------------------------------------------------



}
