//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit.panels.player;


import com.earth2me.essentials.Essentials;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.configs.Settings;
import world.bentobox.visit.managers.VisitAddonManager;
import world.bentobox.visit.panels.ConversationUtils;
import world.bentobox.visit.utils.Constants;
import world.bentobox.visit.utils.Utils;


/**
 * This class shows how to set up easy panel by using BentoBox PanelBuilder API
 */
public class VisitPanel
{
// ---------------------------------------------------------------------
// Section: Constructor
// ---------------------------------------------------------------------


    /**
     * This is internal constructor. It is used internally in current class to avoid creating objects everywhere.
     *
     * @param addon VisitAddon object
     * @param world World where user will be teleported
     * @param user User who opens panel
     * @param label String that represents top command label.
     */
    private VisitPanel(VisitAddon addon,
        World world,
        User user,
        String label)
    {
        this.addon = addon;
        this.manager = this.addon.getAddonManager();
        this.user = user;
        this.label = label;
        this.searchString = null;

        this.world = world;

        this.activeFilter = this.addon.getSettings().getDefaultFilter();

        // Unfortunately, it is necessary to store islands in local list, as there is no
        // other ways how to get the same island order from hash list :(.

        // Filter out islands without owner.
        // Filter out locked islands.
        // Filter out islands with disabled visits flag.
        // Filter out islands where player is banned.

        // Sort by island and owner name

        this.islandList = this.addon.getIslands().getIslands(world).stream().
            filter(Island::isOwned).
            sorted((o1, o2) ->
            {
                String o1Name = o1.getName() != null ? o1.getName() :
                    User.getInstance(o1.getOwner()).getName();
                String o2Name = o2.getName() != null ? o2.getName() :
                    User.getInstance(o2.getOwner()).getName();

                return o1Name.compareToIgnoreCase(o2Name);
            }).
            collect(Collectors.toList());

        this.updateFilter();
    }


// ---------------------------------------------------------------------
// Section: Methods
// ---------------------------------------------------------------------


    /**
     * Build method manages current panel opening. It uses BentoBox PanelAPI that is easy to use and users can get nice
     * panels.
     */
    private void build()
    {
        // Do not open gui if there is no magic sticks.
        if (this.islandList.isEmpty())
        {
            this.addon.logError("There are no available islands for visiting!");
            Utils.sendMessage(this.user, this.user.getTranslation(Constants.ERRORS + "no-islands",
                Constants.PARAMETER_GAMEMODE, Utils.getGameMode(this.world)));
            return;
        }

        // Start building panel.
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();

        // Set main template.
        panelBuilder.template("main_panel", new File(this.addon.getDataFolder(), "panels"));
        panelBuilder.user(this.user);
        panelBuilder.world(this.user.getWorld());

        // Register button builders
        panelBuilder.registerTypeBuilder("ISLAND", this::createIslandButton);

        // Register next and previous builders
        panelBuilder.registerTypeBuilder("NEXT", this::createNextButton);
        panelBuilder.registerTypeBuilder("PREVIOUS", this::createPreviousButton);
        panelBuilder.registerTypeBuilder("SEARCH", this::createSearchButton);
        panelBuilder.registerTypeBuilder("FILTER", this::createFilterButton);

        // Register unknown type builder.
        panelBuilder.build();
    }


// ---------------------------------------------------------------------
// Section: Buttons
// ---------------------------------------------------------------------


    /**
     * Create next button panel item.
     *
     * @param template the template
     * @param slot the slot
     * @return the panel item
     */
    @Nullable
    private PanelItem createNextButton(@NotNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        int size = this.elementList.size();

        if (size <= slot.amountMap().getOrDefault("ISLAND", 1) ||
            1.0 * size / slot.amountMap().getOrDefault("ISLAND", 1) <= this.pageIndex + 1)
        {
            // There are no next elements
            return null;
        }

        int nextPageIndex = this.pageIndex + 2;

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            ItemStack clone = template.icon().clone();

            if ((Boolean) template.dataMap().getOrDefault("indexing", false))
            {
                clone.setAmount(nextPageIndex);
            }

            builder.icon(clone);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title()));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.world, template.description(),
                Constants.PARAMETER_NUMBER, String.valueOf(nextPageIndex)));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            template.actions().forEach(action -> {
                if (clickType == action.clickType()  || action.clickType() == ClickType.UNKNOWN)
                {
                    if ("NEXT".equalsIgnoreCase(action.actionType()))
                    {
                        // Next button ignores click type currently.
                        this.pageIndex++;
                        this.build();
                    }
                }
            });

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }


    /**
     * Create previous button panel item.
     *
     * @param template the template
     * @param slot the slot
     * @return the panel item
     */
    @Nullable
    private PanelItem createPreviousButton(@NotNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        if (this.pageIndex == 0)
        {
            // There are no next elements
            return null;
        }

        int previousPageIndex = this.pageIndex;

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            ItemStack clone = template.icon().clone();

            if ((Boolean) template.dataMap().getOrDefault("indexing", false))
            {
                clone.setAmount(previousPageIndex);
            }

            builder.icon(clone);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title()));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.world, template.description(),
                Constants.PARAMETER_NUMBER, String.valueOf(previousPageIndex)));
        }

        // Add ClickHandler
        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            template.actions().forEach(action -> {
                if (clickType == action.clickType()  || action.clickType() == ClickType.UNKNOWN)
                {
                    if ("PREVIOUS".equalsIgnoreCase(action.actionType()))
                    {
                        // Next button ignores click type currently.
                        this.pageIndex--;
                        this.build();
                    }
                }
            });

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }


    /**
     * This method creates and returns button that allows searching island.
     *
     * @return PanelItem that allows to change search value
     */
    @Nullable
    private PanelItem createSearchButton(@NotNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        final String reference = Constants.BUTTONS + "search.";

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            builder.icon(template.icon().clone());
        }
        else
        {
            builder.icon(Material.PAPER);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title()));
        }
        else
        {
            builder.name(this.user.getTranslation(reference + "name"));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslationOrNothing(template.description(),
                Constants.PARAMETER_VALUE, this.searchString));
        }
        else
        {
            builder.description(this.user.getTranslationOrNothing(reference + "description"));

            if (this.searchString != null && !this.searchString.isEmpty())
            {
                builder.description(this.user.getTranslationOrNothing(reference + "search",
                    Constants.PARAMETER_VALUE, this.searchString));
            }
        }

        // Filter valid actions
        List<ItemTemplateRecord.ActionRecords> actions = template.actions().stream().
            filter(action -> !"CLEAR".equalsIgnoreCase(action.actionType()) ||
                (this.searchString != null && !this.searchString.isEmpty())).
            collect(Collectors.toList());

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            actions.forEach(action -> {
                if (clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN)
                {
                    if ("INPUT".equalsIgnoreCase(action.actionType()))
                    {
                        Consumer<String> consumer = value ->
                        {
                            if (value != null)
                            {
                                this.searchString = value;
                                this.updateFilter();
                            }

                            this.build();
                        };

                        // start conversation
                        ConversationUtils.createStringInput(consumer,
                            this.user,
                            this.user.getTranslation(Constants.CONVERSATIONS + "write-search"),
                            this.user.getTranslation(Constants.CONVERSATIONS + "search-updated"));

                        this.clickedIsland = null;
                    }
                    else if ("CLEAR".equalsIgnoreCase(action.actionType()))
                    {
                        // Clear string.
                        this.searchString = "";
                        this.updateFilter();
                        // Rebuild gui.
                        this.build();

                        this.clickedIsland = null;
                    }
                }
            });

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = actions.stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(actions.size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }


    /**
     * This method creates and returns button that allows filter island.
     *
     * @return PanelItem that allows to change filter value
     */
    @Nullable
    private PanelItem createFilterButton(@NotNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        final String reference = Constants.BUTTONS + "filter.";

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            builder.icon(template.icon().clone());
        }
        else
        {
            builder.icon(switch (this.activeFilter) {
                case ONLINE_ISLANDS -> Material.SANDSTONE_STAIRS;
                case CAN_VISIT -> Material.SANDSTONE_SLAB;
                default -> Material.SMOOTH_SANDSTONE;
            });
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title()));
        }
        else
        {
            builder.name(this.user.getTranslation(reference + "name"));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslationOrNothing(template.description(),
                Constants.PARAMETER_VALUE,
                this.user.getTranslation(reference + this.activeFilter.name().toLowerCase())));
        }
        else
        {
            builder.description(this.user.getTranslationOrNothing(reference + "description"));

            builder.description(this.user.getTranslationOrNothing(reference + "value",
                Constants.PARAMETER_VALUE,
                this.user.getTranslation(reference + this.activeFilter.name().toLowerCase())));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            template.actions().forEach(action -> {
                if (clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN)
                {
                    if ("UP".equalsIgnoreCase(action.actionType()))
                    {
                        this.activeFilter = Utils.getPreviousValue(Settings.Filter.values(), this.activeFilter);
                        this.clickedIsland = null;

                        // Update filters
                        this.updateFilter();
                        // Rebuild gui.
                        this.build();
                    }
                    else if ("DOWN".equalsIgnoreCase(action.actionType()))
                    {
                        this.activeFilter = Utils.getNextValue(Settings.Filter.values(), this.activeFilter);
                        this.clickedIsland = null;

                        // Update filters
                        this.updateFilter();
                        // Rebuild gui.
                        this.build();
                    }
                }
            });

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }


    /**
     * This method creates and returns island button.
     *
     * @return PanelItem that represents island button.
     */
    @Nullable
    private PanelItem createIslandButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        if (this.elementList.isEmpty())
        {
            // Does not contain any sticks.
            return null;
        }

        Island island;

        // Check if that is a specific sticks
        if (template.dataMap().containsKey("id"))
        {
            String id = (String) template.dataMap().get("id");

            // Find a challenge with given id;
            island = this.islandList.stream().
                filter(biomeId -> biomeId.getUniqueId().equals(id)).
                findFirst().
                orElse(null);

            if (island == null)
            {
                // There is no stick in the list with specific id.
                return null;
            }
        }
        else
        {
            int index = this.pageIndex * slot.amountMap().getOrDefault("ISLAND", 1) + slot.slot();

            if (index >= this.elementList.size())
            {
                // Out of index.
                return null;
            }

            island = this.elementList.get(index);
        }

        return this.createIslandButton(template, island);
    }


// ---------------------------------------------------------------------
// Section: Other methods
// ---------------------------------------------------------------------


    /**
     * This method creates and returns button that switch to next page in view mode.
     *
     * @return PanelItem that allows to select next island view page.
     */
    private PanelItem createIslandButton(ItemTemplateRecord template, Island island)
    {
        if (island.getOwner() == null || this.clickedIsland != null && this.clickedIsland != island)
        {
            // return as island has no owner. Empty button will be created.
            return null;
        }

        final String reference = Constants.BUTTONS + "island.";
        User owner = User.getInstance(island.getOwner());

        // Get settings for island.
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            if (template.icon().getType().equals(Material.PLAYER_HEAD))
            {
                builder.icon(owner.getName());
            }
            else
            {
                builder.icon(template.icon().clone());
            }
        }
        else
        {
            // Check owner for a specific icon
            Material material = Material.matchMaterial(
                Utils.getPermissionValue(owner, "visit.icon",
                    this.addon.getSettings().getIslandIcon().name()));

            if (material == null)
            {
                // Set material to a default icon from settings.
                material = this.addon.getSettings().getIslandIcon();
            }

            if (material == Material.PLAYER_HEAD)
            {
                builder.icon(owner.getName());
            }
            else
            {
                builder.icon(material);
            }
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title(),
                Constants.PARAMETER_NAME, island.getName() != null ?
                    island.getName() : owner.getName()));
        }
        else
        {
            builder.name(this.user.getTranslation(reference + "name",
                Constants.PARAMETER_NAME, island.getName() != null ?
                    island.getName() : owner.getName()));
        }

        // Process Description of the button.

        // Generate [owner] text.
        String ownerText = this.user.getTranslationOrNothing(reference + "owner",
            Constants.PARAMETER_PLAYER, owner.getName());

        // Generate [members] text
        String memberText;

        if (island.getMemberSet().size() > 1)
        {
            StringBuilder memberBuilder = new StringBuilder(
                this.user.getTranslationOrNothing(reference + "members-title"));

            island.getMemberSet().stream().
                map(this.addon.getPlayers()::getName).
                forEach(user -> {
                    if (memberBuilder.length() > 0)
                    {
                        memberBuilder.append("\n");
                    }

                    memberBuilder.append(
                        this.user.getTranslationOrNothing(reference + "member",
                            Constants.PARAMETER_PLAYER, user));
                });

            memberText = memberBuilder.toString();
        }
        else
        {
            memberText = "";
        }

        // Boolean that indicate if visiting is allowed.
        final boolean canVisit;

        // Generate [noone-online] text
        String nooneOnlineText;

        if (!this.manager.canVisitOffline(island))
        {
            nooneOnlineText = this.user.getTranslationOrNothing(reference + "noone-online");
            canVisit = false;
        }
        else
        {
            nooneOnlineText = "";

            canVisit = this.manager.preprocessTeleportation(this.user, island, true);
        }

        // Payment for visiting island.
        final double payment = this.manager.getIslandEarnings(island) + this.manager.getTaxAmount();

        // Generate [payment] text
        String paymentText;

        if (payment > 0)
        {
            paymentText = this.user.getTranslationOrNothing(reference + "payment",
                Constants.PARAMETER_NUMBER, String.valueOf(payment));
        }
        else
        {
            paymentText = "";
        }

        // Generate [no-visit] text;
        String noVisitText = canVisit ? "" :
            this.user.getTranslationOrNothing(reference + "no-visit");

        String descriptionText;

        if (template.description() != null)
        {
            descriptionText = this.user.getTranslationOrNothing(template.description(),
                    Constants.PARAMETER_OWNER, ownerText,
                    Constants.PARAMETER_MEMBERS, memberText,
                    Constants.PARAMETER_NOONE_ONLINE, nooneOnlineText,
                    Constants.PARAMETER_PAYMENT, paymentText,
                    Constants.PARAMETER_NO_VISIT, noVisitText).
                replaceAll("(?m)^[ \\t]*\\r?\\n", "").
                replaceAll("(?<!\\\\)\\|", "\n").
                replaceAll("\\\\\\|", "|");
        }
        else
        {
            descriptionText = this.user.getTranslationOrNothing(reference + "description",
                Constants.PARAMETER_OWNER, ownerText,
                Constants.PARAMETER_MEMBERS, memberText,
                Constants.PARAMETER_NOONE_ONLINE, nooneOnlineText,
                Constants.PARAMETER_PAYMENT, paymentText,
                Constants.PARAMETER_NO_VISIT, noVisitText);

            // Clean up description text and split it into parts.
            descriptionText = descriptionText.replaceAll("(?m)^[ \\t]*\\r?\\n", "").
                replaceAll("(?<!\\\\)\\|", "\n").
                replaceAll("\\\\\\|", "|");
        }

        builder.description(descriptionText);

        List<ItemTemplateRecord.ActionRecords> actions = template.actions().stream().
            filter(action -> switch (action.actionType().toUpperCase()) {
                case "VISIT" -> this.clickedIsland == null && canVisit;
                case "CONFIRM", "CANCEL" -> this.clickedIsland != null;
                default -> false;
            }).
            collect(Collectors.toList());

        // Glow icon if user can visit the island.
        builder.glow(canVisit);

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            actions.forEach(action -> {
                if (clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN)
                {
                    if ("CONFIRM".equalsIgnoreCase(action.actionType()))
                    {
                        this.runCommandCall();
                    }
                    else if ("CANCEL".equalsIgnoreCase(action.actionType()))
                    {
                        this.clickedIsland = null;
                        this.build();
                    }
                    else if ("VISIT".equalsIgnoreCase(action.actionType()))
                    {
                        this.clickedIsland = island;

                        if (this.addon.getSettings().isPaymentConfirmation() &&
                            payment > 0)
                        {
                            // rebuild gui with the only this icon.
                            this.build();
                        }
                        else
                        {
                            this.runCommandCall();
                        }
                    }
                }
            });

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = actions.stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(actions.size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }


    /**
     * This method update filtered element list
     */
    private void updateFilter()
    {
        switch (this.activeFilter)
        {
            case ONLINE_ISLANDS -> this.elementList = this.islandList.stream().
                filter(island -> island.getMemberSet().stream().
                    map(User::getInstance).
                    anyMatch(User::isOnline)).
                    filter(island -> {
                        /* If essentials is not hooked, this filter doesn't apply */
                        Essentials essentials = VisitAddon.getInstance().getEssentials();
                        if (essentials == null) return true;

                        /* If user can see vanished players, this filter does not apply */
                        if (this.user.hasPermission("essentials.vanish.see")) return true;

                        /* If the owner is online, and is vanished filter them out */
                        User owner = User.getInstance(Objects.requireNonNull(island.getOwner()));
                        /* If the owner is not online for some reason, bail */
                        if (!owner.isOnline()) return false;

                        /* If the target owner is vanished, do not include them as "online" */
                        return !essentials.getVanishedPlayersNew().contains(owner.getName());
                    }).
                collect(Collectors.toList());
            case CAN_VISIT ->
                // TODO: add balance and online check.
                this.elementList = this.islandList.stream().
                    filter(island -> island.isAllowed(this.user, Flags.LOCK)).
                    filter(island -> island.isAllowed(VisitAddon.ALLOW_VISITS_FLAG)).
                    filter(island -> !island.isBanned(this.user.getUniqueId())).
                    collect(Collectors.toList());
            default -> this.elementList = this.islandList;
        }

        // Apply searchedString filter.
        if (this.searchString != null && !this.searchString.isEmpty())
        {
            this.elementList = this.elementList.stream().
                filter(island -> island.getName() == null ||
                    island.getName().toLowerCase().contains(this.searchString)).
                filter(island -> island.getMemberSet().stream().
                    map(User::getInstance).
                    anyMatch(user -> user.getName().toLowerCase().contains(this.searchString))).
                collect(Collectors.toList());
        }

        this.pageIndex = 0;
    }


    /**
     * This method runs command call that allows player to visit clicked island.
     */
    private void runCommandCall()
    {
        // Get first player command label.
        String command = this.addon.getSettings().getPlayerMainCommand().split(" ")[0];

        this.addon.log(this.user.getName() + " called: `" + this.label + " " + command + " " + this.clickedIsland.getOwner() + " bypass`");
        // Confirmation is done via GUI. Bypass.
        this.user.performCommand(this.label + " " + command + " " + this.clickedIsland.getOwner() + " bypass");

        // Close inventory
        this.user.closeInventory();
    }


// ---------------------------------------------------------------------
// Section: Static methods
// ---------------------------------------------------------------------


    /**
     * This method is used to open UserPanel outside this class. It will be much easier to open panel with single method
     * call then initializing new object.
     *
     * @param addon VisitAddon object
     * @param world World where user will be teleported
     * @param user User who opens panel
     * @param label String that represents top command label.
     */
    public static void openPanel(VisitAddon addon,
        World world,
        User user,
        String label)
    {
        new VisitPanel(addon, world, user, label).build();
    }


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


    /**
     * This variable allows to access addon object.
     */
    private final VisitAddon addon;

    /**
     * This variable allows to access addon manager object.
     */
    private final VisitAddonManager manager;

    /**
     * This variable holds user who opens panel. Without it panel cannot be opened.
     */
    private final User user;

    /**
     * This variable holds world where panel is opened. Without it panel cannot be opened.
     */
    private final World world;

    /**
     * This variable stores all islands in the given world.
     */
    private final List<Island> islandList;

    /**
     * This variable holds top command label which opened current panel.
     */
    private final String label;

    /**
     * This variable stores filtered elements.
     */
    private List<Island> elementList;

    /**
     * This variable stores clicked island for the confirmation.
     */
    private Island clickedIsland = null;

    /**
     * This variable holds current pageIndex for multi-page island choosing.
     */
    private int pageIndex;

    /**
     * This variable stores search string for player / island names.
     */
    private String searchString;

    /**
     * Stores active filter.
     */
    private Settings.Filter activeFilter;
}
