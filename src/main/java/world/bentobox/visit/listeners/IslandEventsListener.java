//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.visit.listeners;


import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.visit.VisitAddon;


/**
 * This listener deletes island data after it is removed.
 */
public class IslandEventsListener implements Listener
{
    /**
     * @param addon - addon
     */
    public IslandEventsListener(VisitAddon addon)
    {
        this.addon = addon;
    }


    /**
     * This method handles island deletion. On island deletion it should remove generator data too.
     *
     * @param event IslandDeletedEvent instance.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDelete(IslandDeleteEvent event)
    {
        this.addon.getAddonManager().removeData(event.getIsland());
    }


    /**
     * stores addon instance
     */
    private final VisitAddon addon;
}