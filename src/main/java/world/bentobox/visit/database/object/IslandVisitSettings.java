package world.bentobox.visit.database.object;


import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;


/**
 * This class stores information about island visiting data.
 */
@Table(name = "IslandVisitSettings")
public class IslandVisitSettings implements DataObject
{
	// ---------------------------------------------------------------------
	// Section: Variables
	// ---------------------------------------------------------------------

	/**
	 * Current object uniqueId object.
	 */
	@Expose
	private String uniqueId = "";

	/**
	 * Stores payment value for visitor.
	 */
	@Expose
	private double payment = 0.0;

	/**
	 * Stores if visits are allowed while offline.
	 */
	@Expose
	private boolean offlineVisit = true;


	// ---------------------------------------------------------------------
	// Section: Constructor
	// ---------------------------------------------------------------------


	/**
	 * Empty constructor.
	 */
	public IslandVisitSettings()
	{
	}


	// ---------------------------------------------------------------------
	// Section: Methods
	// ---------------------------------------------------------------------


	/**
	 * @return uniqueId value.
	 */
	@Override
	public String getUniqueId()
	{
		return this.uniqueId;
	}


	/**
	 * @param uniqueId new uniqueId value.
	 */
	@Override
	public void setUniqueId(String uniqueId)
	{
		this.uniqueId = uniqueId;
	}


	/**
	 * This method returns the payment value.
	 * @return the value of payment.
	 */
	public double getPayment()
	{
		return this.payment;
	}


	/**
	 * This method sets the payment value.
	 * @param payment the payment new value.
	 *
	 */
	public void setPayment(double payment)
	{
		this.payment = payment;
	}


	/**
	 * This method returns the offlineVisit value.
	 * @return the value of offlineVisit.
	 */
	public boolean isOfflineVisit()
	{
		return this.offlineVisit;
	}


	/**
	 * This method sets the offlineVisit value.
	 * @param offlineVisit the offlineVisit new value.
	 *
	 */
	public void setOfflineVisit(boolean offlineVisit)
	{
		this.offlineVisit = offlineVisit;
	}
}
