package world.bentobox.visit.events;


import world.bentobox.bentobox.api.events.BentoBoxEvent;
import world.bentobox.visit.VisitAddon;


/**
 * This class shows simple BentoBoxEvent object that will automatically populate AddonEvent
 * map. It will allow to access this event outside BentoBox environment by catching
 * BentoBoxEvent and checking if its name equals VisitEvent.
 */
public class VisitEvent extends BentoBoxEvent
{
	// ---------------------------------------------------------------------
	// Section: Variables
	// ---------------------------------------------------------------------

	/**
	 * Variable that shows if addon is enabled
	 */
	private boolean enabled;

	/**
	 * String that contains return message of current event.
	 */
	private String returnMessage;


	// ---------------------------------------------------------------------
	// Section: Constructor
	// ---------------------------------------------------------------------


	/**
	 * Constructor ExampleAddonEvent creates a new ExampleAddonEvent instance.
	 *
	 * @param addon of type ExampleAddon
	 * @param returnMessage of type String
	 */
	public VisitEvent(VisitAddon addon, String returnMessage)
	{
		this.enabled = addon.isEnabled();
		this.returnMessage = returnMessage;
	}


	// ---------------------------------------------------------------------
	// Section: Getters and Setters
	// ---------------------------------------------------------------------


	/**
	 * This method returns the returnMessage value.
	 * @return the value of returnMessage.
	 */
	public String getReturnMessage()
	{
		return this.returnMessage;
	}


	/**
	 * This method sets the returnMessage value.
	 * @param returnMessage the returnMessage new value.
	 *
	 */
	public void setReturnMessage(String returnMessage)
	{
		this.returnMessage = returnMessage;
	}


	/**
	 * This method returns if addon is enabled or not.
	 * @return true if addon is enabled, otherwise false.
	 */
	public boolean isEnabled()
	{
		return this.enabled;
	}
}
