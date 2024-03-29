//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit.events;


import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import java.util.UUID;

import world.bentobox.bentobox.api.events.BentoBoxEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;


/**
 * This class shows simple BentoBoxEvent object that will automatically populate AddonEvent map. It will allow to access
 * this event outside BentoBox environment by catching BentoBoxEvent and checking if its name equals VisitEvent.
 */
public class VisitEvent extends BentoBoxEvent implements Cancellable
{
    // ---------------------------------------------------------------------
    // Section: Constructor
    // ---------------------------------------------------------------------


    /**
     * Constructor VisitEvent creates a new VisitEvent instance.
     *
     * @param player of type UUID
     * @param island of type Island
     */
    public VisitEvent(User player, Island island)
    {
        this.player = player;
        this.island = island;
    }


    // ---------------------------------------------------------------------
    // Section: Getters and Setters
    // ---------------------------------------------------------------------


    /**
     * This method returns the cancelled value.
     *
     * @return the value of cancelled.
     */
    @Override
    public boolean isCancelled()
    {
        return this.cancelled;
    }


    /**
     * This method sets the cancelled value.
     *
     * @param cancelled the cancelled new value.
     */
    @Override
    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }


    /**
     * This method returns the island value.
     *
     * @return the value of island.
     */
    public Island getIsland()
    {
        return this.island;
    }


    /**
     * This method returns the player UUID.
     *
     * @return the UUID of player.
     */
    public UUID getPlayer()
    {
        return this.player.getUniqueId();
    }


    /**
     * This method returns the player.
     *
     * @return the user
     */
    public User getUser()
    {
        return this.player;
    }


// ---------------------------------------------------------------------
// Section: Handler methods
// ---------------------------------------------------------------------


    /**
     * Gets handlers.
     *
     * @return the handlers
     */
    @Override
    public HandlerList getHandlers()
    {
        return VisitEvent.handlers;
    }


    /**
     * Gets handlers.
     *
     * @return the handlers
     */
    public static HandlerList getHandlerList()
    {
        return VisitEvent.handlers;
    }


    // ---------------------------------------------------------------------
    // Section: Variables
    // ---------------------------------------------------------------------

    /**
     * Variable that shows if visiting is cancelled.
     */
    private boolean cancelled;

    /**
     * Variable that stores island that will be visited.
     */
    private Island island;

    /**
     * Variable that stores player who wants to visit an island.
     */
    private User player;

    /**
     * Event listener list for current
     */
    private static final HandlerList handlers = new HandlerList();
}
