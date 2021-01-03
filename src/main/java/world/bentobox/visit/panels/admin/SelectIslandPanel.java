//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit.panels.admin;


import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.panels.ConversationUtils;
import world.bentobox.visit.utils.Constants;
import world.bentobox.visit.utils.Utils;


/**
 * This class contains all necessary things that allows to select single island.
 */
public class SelectIslandPanel
{
    /**
     * Defautl constructor object.
     *
     * @param user User who opens Panel
     * @param inputList Input Island List
     * @param consumer Consumer that returns selected island object.
     */
    private SelectIslandPanel(User user,
        List<Island> inputList,
        Consumer<Island> consumer)
    {
        this.consumer = consumer;
        this.user = user;

        this.islandList = inputList;

        this.borderBlock = this.createBorderBlock();
        this.updateFilter();
    }


    /**
     * This method builds all necessary elements in GUI panel.
     */
    private void build()
    {
        PanelBuilder panelBuilder = new PanelBuilder().
            user(this.user).
            name(this.user.getTranslation(Constants.TITLES + "select-island"));

        // Populate panel with border material.
        for (int i = 0; i < 9; i++)
        {
            panelBuilder.item(i, this.borderBlock);
        }

        panelBuilder.item(4, this.createSearchButton());

        final int MAX_ELEMENTS = 45;

        int index;

        if (this.pageIndex > 0)
        {
            panelBuilder.item(this.createPreviousPageButton());
            // Multiple pages. Adjust index.

            index = this.pageIndex * MAX_ELEMENTS;
        }
        else
        {
            index = 0;
        }

        // Create island iterator for sublist that contains elements from index till list size.
        Iterator<Island> islandIterator = this.elementList.subList(index, this.elementList.size()).iterator();

        // Fill every free spot till last block is not used.
        while (islandIterator.hasNext() && panelBuilder.nextSlot() < 53)
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


        panelBuilder.build();
    }


    /**
     * Create border block panel item.
     *
     * @return the panel item
     */
    private PanelItem createBorderBlock()
    {
        ItemStack itemStack = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(" ");
        itemStack.setItemMeta(meta);

        return new PanelItemBuilder().
            icon(itemStack).
            name(" ").
            build();
    }


    /**
     * This method creates and returns button that switch to previous page in view mode.
     *
     * @return PanelItem that allows to select previous island view page.
     */
    private PanelItem createPreviousPageButton()
    {
        List<String> description = new ArrayList<>(3);
        description.add(this.user.getTranslationOrNothing(Constants.BUTTONS + "previous.description",
            Constants.PARAMETER_NUMBER, String.valueOf(this.pageIndex)));

        // add empty line
        description.add("");
        description.add(this.user.getTranslation(Constants.TIPS + "click-to-previous"));

        return new PanelItemBuilder().
            icon(Material.TIPPED_ARROW).
            name(this.user.getTranslation(Constants.BUTTONS + "previous.name")).
            description(description).
            amount(this.pageIndex).
            clickHandler((panel, user, clickType, index) ->
            {
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
            clickHandler((panel, user, clickType, index) ->
            {
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
     * This method creates and returns button that switch to next page in view mode.
     *
     * @return PanelItem that allows to select next island view page.
     */
    private PanelItem createIslandButton(Island island)
    {
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
                VisitAddon.getInstance().getSettings().getIslandIcon().name()));

        if (material == null)
        {
            // Set material to a default icon from settings.
            material = VisitAddon.getInstance().getSettings().getIslandIcon();
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

        // Generate [noone-online] text
        String nooneOnlineText = "";

        // Payment for visiting island.
        double payment = VisitAddon.getInstance().getAddonManager().getIslandEarnings(island) +
            VisitAddon.getInstance().getSettings().getTaxAmount();

        // Generate [payment] text
        String paymentText;

        if (payment > 0)
        {
            paymentText = this.user.getTranslationOrNothing(Constants.BUTTONS + "island.payment",
                Constants.PARAMETER_NUMBER, String.valueOf(payment));
        }
        else
        {
            paymentText = "";
        }

        // Generate [no-visit] text;
        String noVisitText = "";

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
        builder.description(description);

        return builder.clickHandler((panel, user, clickType, index) ->
        {
            this.consumer.accept(island);
            return true;
        }).build();
    }


    /**
     * This method updates filter value.
     */
    private void updateFilter()
    {
        // Apply searchedString filter.
        if (this.searchString != null && !this.searchString.isEmpty())
        {
            this.elementList = this.islandList.stream().
                filter(island -> island.getName() == null ||
                    island.getName().toLowerCase().contains(this.searchString)).
                filter(island -> island.getMemberSet().stream().
                    map(User::getInstance).
                    filter(Objects::nonNull).
                    anyMatch(user -> user.getName().toLowerCase().contains(this.searchString))).
                collect(Collectors.toList());
        }
        else
        {
            this.elementList = this.islandList;
        }

        this.pageIndex = 0;
    }


    /**
     * Simple warpper around panel.
     *
     * @param user User who opens Panel
     * @param inputList Input Island List
     * @param consumer Consumer that returns selected island object.
     */
    public static void open(User user, List<Island> inputList, Consumer<Island> consumer)
    {
        new SelectIslandPanel(user, inputList, consumer).build();
    }


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


    /**
     * List with elements that will be displayed in current GUI.
     */
    private final List<Island> islandList;

    /**
     * This variable stores consumer.
     */
    private final Consumer<Island> consumer;

    /**
     * User who runs GUI.
     */
    private final User user;

    /**
     * Border Block Item.
     */
    private final PanelItem borderBlock;

    /**
     * List of filtered elements.
     */
    private List<Island> elementList;

    /**
     * Search string.
     */
    private String searchString;

    /**
     * Stores current pageIndex.
     */
    private int pageIndex;
}
