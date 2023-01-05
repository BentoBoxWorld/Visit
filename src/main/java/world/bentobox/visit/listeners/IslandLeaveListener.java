//
// Created by BONNe
// Copyright - 2023
//


package world.bentobox.visit.listeners;


import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.utils.Constants;
import world.bentobox.visit.utils.Utils;


/**
 * This listener sends chat message to island owners when visitor leaves the island.
 */
public class IslandLeaveListener implements Listener
{
    /**
     * Instantiates a new Island leave listener.
     *
     * @param addon the addon
     */
    public IslandLeaveListener(VisitAddon addon)
    {
        this.addon = addon;
    }


    /**
     * On move event listener
     *
     * @param event the player move event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event)
    {
        this.handleExitMessage(User.getInstance(event.getPlayer()), event.getFrom(), event.getTo());
    }


    /**
     * On teleport event listener.
     *
     * @param event the player teleport event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event)
    {
        this.handleExitMessage(User.getInstance(event.getPlayer()), event.getFrom(), event.getTo());
    }


    /**
     * This method handles player exit island messaging.
     * @param user User who need to be checked.
     * @param from From location.
     * @param to To location.
     * @param event Player Move event.
     */
    private void handleExitMessage(@NotNull User user,
        @NotNull Location from,
        @NotNull Location to)
    {
        if (!IslandLeaveListener.trackedPlayerSet.contains(user.getUniqueId()))
        {
            // This player is not visiting any island. Ignore it.
            return;
        }

        // Only process if there is a change in X or Z coords
        if (from.getWorld() != null &&
            from.getWorld().equals(to.getWorld()) &&
            from.toVector().multiply(XZ).equals(to.toVector().multiply(XZ)))
        {
            return;
        }

        Optional<Island> islandFrom = this.addon.getIslands().getProtectedIslandAt(from);
        Optional<Island> islandTo = this.addon.getIslands().getProtectedIslandAt(to);

        /*
         * Options:
         *
         * from = empty, to = island - entering
         * from = island1, to = island2 - leaving 1, entering 2
         * from = island, to = empty - leaving
         * from = empty, to = empty
         * from = island, to = island
         */
        if (islandFrom.equals(islandTo))
        {
            return;
        }

        // Now handle the messaging.
        // Remove player from tracking set.
        IslandLeaveListener.trackedPlayerSet.remove(user.getUniqueId());

        // Send message to island members.
        islandFrom.ifPresent(island -> {
            if (!island.getMemberSet().contains(user.getUniqueId()) &&
                island.isAllowed(VisitAddon.RECEIVE_VISIT_MESSAGE_FLAG))
            {
                // Send message that player is visiting the island.
                island.getMemberSet().forEach(uuid ->
                {
                    User member = User.getInstance(uuid);

                    if (member.isOnline())
                    {
                        Utils.sendMessage(member,
                            member.getTranslation(ISLAND_MESSAGE, Constants.PARAMETER_PLAYER, user.getName()));
                    }
                });
            }
        });
    }


    /**
     * Instance of visit addon.
     */
    private final VisitAddon addon;

    /**
     * Set of tracked players.
     */
    public static Set<UUID> trackedPlayerSet = new HashSet<>();

    /**
     * Coordinate normalization vector.
     */
    private static final Vector XZ = new Vector(1,0,1);

    /**
     * The island send message constant.
     */
    private static final String ISLAND_MESSAGE = Constants.CONVERSATIONS + "player-leaves-island";
}
