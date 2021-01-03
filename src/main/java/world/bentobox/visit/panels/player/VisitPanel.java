//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit.panels.player;


import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.configs.Settings;
import world.bentobox.visit.database.object.IslandVisitSettings;
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

        this.filtersEnabled = this.addon.getSettings().isFiltersEnabled();
        this.filtersAtTheTop = this.addon.getSettings().isFiltersTopLine();
        this.maxElements = this.filtersEnabled ? 45 : 54;
        this.maxSlotIndex = !this.filtersEnabled || this.filtersAtTheTop ? 53 : 44;
        
        this.borderBlock = this.createBorderBlock();
        
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
                    Objects.requireNonNull(User.getInstance(o1.getOwner())).getName();
                String o2Name = o2.getName() != null ? o2.getName() :
                    Objects.requireNonNull(User.getInstance(o2.getOwner())).getName();

                return o1Name.compareToIgnoreCase(o2Name);
            }).
            collect(Collectors.toList());

        this.updateFilter();
    }


    // ---------------------------------------------------------------------
    // Section: Methods
    // ---------------------------------------------------------------------


    /**
     * Create border block panel item.
     *
     * @return the panel item
     */
    private PanelItem createBorderBlock()
    {
        if (!this.filtersEnabled)
        {
            return new PanelItemBuilder().build();
        }
        
        ItemStack itemStack = new ItemStack(this.addon.getSettings().getBorderBlock());
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(this.addon.getSettings().getBorderBlockName());
        itemStack.setItemMeta(meta);
        
        return new PanelItemBuilder().
            icon(itemStack).
            name(this.addon.getSettings().getBorderBlockName()).
            build();
    }
    

    /**
     * Build method manages current panel opening. It uses BentoBox PanelAPI that is easy to use and users can get nice
     * panels.
     */
    private void build()
    {
        // PanelBuilder is a BentoBox API that provides ability to easy create Panels.
        PanelBuilder panelBuilder = new PanelBuilder().
            user(this.user).
            name(this.user.getTranslation(Constants.TITLES + "choose"));

        // Create header
        if (this.filtersEnabled && this.filtersAtTheTop)
        {
            this.createFilters(panelBuilder, 0);
        }

        // Process island population.
        int index;

        if (this.pageIndex > 0)
        {
            panelBuilder.item(this.createPreviousPageButton());
            // Multiple pages. Adjust index.

            index = this.pageIndex * this.maxElements;
        }
        else
        {
            index = 0;
        }

        // Create island iterator for sublist that contains elements from index till list size.
        Iterator<Island> islandIterator = this.elementList.subList(index, this.elementList.size()).iterator();

        // Fill every free spot till last block is not used.
        while (islandIterator.hasNext() && panelBuilder.nextSlot() < this.maxSlotIndex)
        {
            panelBuilder.item(this.createIslandButton(islandIterator.next()));
        }

        // Check if there is spot for last island
        if (islandIterator.hasNext())
        {
            Island lastIsland = islandIterator.next();

            if (islandIterator.hasNext())
            {
                // There is more then one island left in the list.
                panelBuilder.item(this.createNextPageButton());
            }
            else
            {
                // This is the last island.. no need to create next page button.
                panelBuilder.item(this.createIslandButton(lastIsland));
            }
        }

        // Create footer.
        if (this.filtersEnabled && !this.filtersAtTheTop)
        {
            this.createFilters(panelBuilder, 45);
        }

        // At the end we just call build method that creates and opens panel.
        panelBuilder.build();
    }


    /**
     * Create filter row for the gui.
     *
     * @param panelBuilder the panel builder
     * @param startIndex the start index
     */
    private void createFilters(PanelBuilder panelBuilder, int startIndex)
    {
        // Populate panel with border material.
        for (int i = startIndex; i < startIndex + 9; i++)
        {
            panelBuilder.item(i, this.borderBlock);
        }
        
        panelBuilder.item(startIndex + 4, this.createSearchButton());
        panelBuilder.item(startIndex + 8, this.createFilterButton());
    }


    /**
     * This method creates and returns button that switch to previous page in view mode.
     *
     * @return PanelItem that allows to select previous island view page.
     */
    private PanelItem createPreviousPageButton()
    {
        int page = this.pageIndex;

        List<String> description = new ArrayList<>(3);
        description.add(this.user.getTranslationOrNothing(Constants.BUTTONS + "previous.description",
            Constants.PARAMETER_NUMBER, String.valueOf(page)));

        // add empty line
        description.add("");
        description.add(this.user.getTranslation(Constants.TIPS + "click-to-previous"));

        return new PanelItemBuilder().
            icon(Material.TIPPED_ARROW).
            name(this.user.getTranslation(Constants.BUTTONS + "previous.name")).
            description(description).
            amount(page).
            clickHandler((panel, user, clickType, index) -> {
                this.pageIndex = Math.max(0, this.pageIndex - 1);
                this.build();
                return true;
            }).
            build();
    }


    /**
     * This method creates and returns button that switch to next page in view mode.
     *
     * @return PanelItem that allows to select next island view page.
     */
    private PanelItem createNextPageButton()
    {
        int page = this.pageIndex + 2;

        List<String> description = new ArrayList<>(3);
        description.add(this.user.getTranslationOrNothing(Constants.BUTTONS + "next.description",
            Constants.PARAMETER_NUMBER, String.valueOf(page)));

        // add empty line
        description.add("");
        description.add(this.user.getTranslation(Constants.TIPS + "click-to-next"));

        return new PanelItemBuilder().
            icon(Material.TIPPED_ARROW).
            name(this.user.getTranslation(Constants.BUTTONS + "next.name")).
            description(description).
            amount(page).
            clickHandler((panel, user, clickType, index) -> {
                this.pageIndex++;
                this.build();
                return true;
            }).
            build();
    }


    /**
     * This method creates and returns button that allows searching island.
     *
     * @return PanelItem that allows to change search value
     */
    private PanelItem createSearchButton()
    {
        String name = this.user.getTranslation(Constants.BUTTONS + "search.name");
        Material icon = Material.PAPER;
        List<String> description = new ArrayList<>();
        description.add(this.user.getTranslationOrNothing(Constants.BUTTONS + "search.description"));

        if (this.searchString != null && !this.searchString.isEmpty())
        {
            description.add(this.user.getTranslation(Constants.BUTTONS + "search.search",
                Constants.PARAMETER_VALUE, this.searchString));
        }

        description.add("");
        description.add(this.user.getTranslation(Constants.TIPS + "left-click-to-edit"));

        // If there is search string, then add remove tooltip.
        if (this.searchString != null && !this.searchString.isEmpty())
        {
            description.add(this.user.getTranslation(Constants.TIPS + "right-click-to-clear"));
        }

        PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) -> {
            if (clickType.isRightClick())
            {
                // Clear string.
                this.searchString = "";
                this.updateFilter();
                // Rebuild gui.
                this.build();
            }
            else
            {
                // Create consumer that process description change
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
            }

            return true;
        };

        return new PanelItemBuilder().
            name(name).
            description(description).
            icon(icon).
            clickHandler(clickHandler).
            build();
    }


    /**
     * Create filter button panel item.
     *
     * @return the panel item
     */
    private PanelItem createFilterButton()
    {
        
        String name = this.user.getTranslation(Constants.BUTTONS + "filter.name");
        Material icon;

        switch (this.activeFilter)
        {
            case ONLINE_ISLANDS:
                icon = Material.SANDSTONE_STAIRS;
                break;
            case CAN_VISIT:
                icon = Material.SANDSTONE_STAIRS;
                break;
            default:
                icon = Material.SMOOTH_SANDSTONE;
                break;
        }

        List<String> description = new ArrayList<>();
        description.add(this.user.getTranslationOrNothing(Constants.BUTTONS + "filter.description"));

        description.add(this.user.getTranslation(Constants.BUTTONS + "filter.value",
            Constants.PARAMETER_VALUE,
            this.user.getTranslation(Constants.BUTTONS + "filter." + this.activeFilter.name().toLowerCase())));

        description.add("");
        description.add(this.user.getTranslation(Constants.TIPS + "click-to-switch"));

        PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) -> {
            if (clickType.isRightClick())
            {
                // Clear string.
                this.activeFilter = Utils.getPreviousValue(Settings.Filter.values(), this.activeFilter);
            }
            else
            {
                this.activeFilter = Utils.getNextValue(Settings.Filter.values(), this.activeFilter);
            }

            // Update filters
            this.updateFilter();
            // Rebuild gui.
            this.build();

            return true;
        };

        return new PanelItemBuilder().
            name(name).
            description(description).
            icon(icon).
            clickHandler(clickHandler).
            build();
    }
    
    
    /**
     * This method creates and returns button that switch to next page in view mode.
     *
     * @return PanelItem that allows to select next island view page.
     */
    private PanelItem createIslandButton(Island island)
    {
        // Get settings for island.
        IslandVisitSettings settings = this.manager.getIslandVisitSettings(island);

        PanelItemBuilder builder = new PanelItemBuilder();
        User owner = User.getInstance(island.getOwner());

        if (owner == null)
        {
            // return as island has no owner. Empty button will be created.
            return builder.build();
        }

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

        builder.name(this.user.getTranslation(Constants.BUTTONS + "island.name",
            Constants.PARAMETER_NAME, island.getName() != null ?
                island.getName() : owner.getName()));

        // Process Description of the button.

        // Generate [owner] text.
        String ownerText = this.user.getTranslationOrNothing(Constants.BUTTONS + "island.owner",
            Constants.PARAMETER_PLAYER, owner.getName());

        // Generate [members] text
        String memberText;

        if (island.getMemberSet().size() > 1)
        {
            StringBuilder memberBuilder = new StringBuilder(
                this.user.getTranslationOrNothing(Constants.BUTTONS + "island.members-title"));

            for (UUID uuid : island.getMemberSet())
            {
                User user = User.getInstance(uuid);

                if (memberBuilder.length() > 0)
                {
                    memberBuilder.append("\n");
                }

                if (user != null)
                {
                    memberBuilder.append(
                        this.user.getTranslationOrNothing(Constants.BUTTONS + "island.member",
                            Constants.PARAMETER_PLAYER, user.getName()));
                }
            }

            memberText = memberBuilder.toString();
        }
        else
        {
            memberText = "";
        }

        // Boolean that indicate if visiting is allowed.
        boolean canVisit = true;

        // Generate [noone-online] text
        String nooneOnlineText;

        if (!this.manager.canVisitOffline(island, settings))
        {
            nooneOnlineText = this.user.getTranslationOrNothing(Constants.BUTTONS + "island.noone-online");
            canVisit = false;
        }
        else
        {
            nooneOnlineText = "";
        }

        // Payment for visiting island.
        double payment = settings.getPayment() + this.addon.getSettings().getTaxAmount();
        // Generate [payment] text
        String paymentText;

        if (payment > 0)
        {
            paymentText = this.user.getTranslationOrNothing(Constants.BUTTONS + "island.payment",
                Constants.PARAMETER_NUMBER, String.valueOf(payment));

            if (!this.manager.hasCredits(this.user, payment))
            {
                canVisit = false;
            }
        }
        else
        {
            paymentText = "";
        }

        // Generate [no-visit] text;
        String noVisitText = canVisit ? "" :
            this.user.getTranslationOrNothing(Constants.BUTTONS + "island.no-visit");

        // Generate full description text.
        String descriptionText = this.user.getTranslationOrNothing(Constants.BUTTONS + "island.description",
            Constants.PARAMETER_OWNER, ownerText,
            Constants.PARAMETER_MEMBERS, memberText,
            Constants.PARAMETER_NOONE_ONLINE, nooneOnlineText,
            Constants.PARAMETER_PAYMENT, paymentText,
            Constants.PARAMETER_NO_VISIT, noVisitText);

        // Clean up description text and split it into parts.
        List<String> description = Arrays.stream(descriptionText.replaceAll("(?m)^[ \\t]*\\r?\\n", "").
            split("\n")).
            collect(Collectors.toList());

        // Add tooltips.
        if (canVisit)
        {
            this.user.getTranslationOrNothing(Constants.TIPS + "click-to-visit");
        }

        builder.description(description);

        // Glow icon if user can visit the island.
        builder.glow(canVisit);

        // Final variable for clicking in clickHandler.
        final boolean canClick = canVisit;

        return builder.clickHandler((panel, user, clickType, index) ->
        {
            if (canClick)
            {
                user.performCommand(this.label + " visit " + island.getOwner());
                // Close inventory
                user.closeInventory();
            }

            return true;
        }).build();
    }


    /**
     * This method update filtered element list
     */
    private void updateFilter()
    {
        switch (this.activeFilter)
        {
            case ONLINE_ISLANDS:
                this.elementList = this.islandList.stream().
                    filter(island -> island.getMemberSet().stream().
                        map(User::getInstance).
                        filter(Objects::nonNull).
                        anyMatch(User::isOnline)).
                    collect(Collectors.toList());
                break;
            case CAN_VISIT:
                // TODO: add balance and online check.
                this.elementList = this.islandList.stream().
                    filter(island -> island.isAllowed(this.user, Flags.LOCK)).
                    filter(island -> island.isAllowed(VisitAddon.ALLOW_VISITS_FLAG)).
                    filter(island -> !island.isBanned(this.user.getUniqueId())).
                    collect(Collectors.toList());

                break;
            default:
                this.elementList = this.islandList;
        }

        // Apply searchedString filter.
        if (this.searchString != null && !this.searchString.isEmpty())
        {
            this.elementList = this.elementList.stream().
                filter(island -> island.getName() == null ||
                    island.getName().toLowerCase().contains(this.searchString)).
                filter(island -> island.getMemberSet().stream().
                    map(User::getInstance).
                    filter(Objects::nonNull).
                    anyMatch(user -> user.getName().toLowerCase().contains(this.searchString))).
                collect(Collectors.toList());
        }

        this.pageIndex = 0;
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
     * This variable stores all islands in the given world.
     */
    private final List<Island> islandList;

    /**
     * This boolean indicate if filters line should be enabled.
     */
    private final boolean filtersEnabled;

    /**
     * Stores maximal allowed elements per page.
     */
    private final int maxElements;

    /**
     * Stores max island slot index.
     */
    private final int maxSlotIndex;

    /**
     * Indicates that filters should be located at the top or bottom line.
     */
    private final boolean filtersAtTheTop;

    /**
     * Border Block Item.
     */
    private final PanelItem borderBlock;

    /**
     * This variable stores filtered elements.
     */
    private List<Island> elementList;

    /**
     * This variable holds top command label which opened current panel.
     */
    private final String label;

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
