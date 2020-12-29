package world.bentobox.visit.panels.admin;


import org.bukkit.Material;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.database.object.IslandVisitSettings;
import world.bentobox.visit.panels.GuiUtils;


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

        this.elements = inputList;
    }


    /**
     * This method builds all necessary elements in GUI panel.
     */
    private void build()
    {
        PanelBuilder panelBuilder = new PanelBuilder().user(this.user).
            name(this.user.getTranslation("visit.gui.admin.title.select-island"));

        final int MAX_ELEMENTS = 27;

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
        Iterator<Island> islandIterator = this.elements.subList(index, this.elements.size()).iterator();

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


// ---------------------------------------------------------------------
// Section: Methods
// ---------------------------------------------------------------------


    /**
     * This method creates and returns button that switch to previous page in view mode.
     *
     * @return PanelItem that allows to select previous island view page.
     */
    private PanelItem createPreviousPageButton()
    {
        return new PanelItemBuilder().
            icon(Material.TIPPED_ARROW).
            name(this.user.getTranslation("visit.gui.player.button.previous.name")).
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
        return new PanelItemBuilder().
            icon(Material.TIPPED_ARROW).
            name(this.user.getTranslation("visit.gui.player.button.next.name")).
            clickHandler((panel, user, clickType, index) -> {
                this.pageIndex++;
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
    private PanelItem createIslandButton(Island island)
    {
        PanelItemBuilder builder = new PanelItemBuilder();

        if (VisitAddon.getInstance().getSettings().getIslandIcon() == Material.PLAYER_HEAD)
        {
            builder.icon(Objects.requireNonNull(User.getInstance(island.getOwner())).getName());
        }
        else
        {
            builder.icon(VisitAddon.getInstance().getSettings().getIslandIcon());
        }

        builder.name(this.user.getTranslation("visit.gui.player.button.island.name",
            "[name]",
            island.getName() != null ? island.getName() :
                Objects.requireNonNull(User.getInstance(island.getOwner())).getName()));

        List<String> description = new ArrayList<>();

        IslandVisitSettings settings = VisitAddon.getInstance().getAddonManager().getIslandVisitSettings(island);

        description.add(this.user.getTranslation("visit.gui.player.button.island.cost",
            "[cost]", String.valueOf(settings.getPayment())));

        builder.description(GuiUtils.stringSplit(description));

        // Glow icon if user can visit the island.
        builder.glow(VisitAddon.getInstance().getAddonManager().hasDefaultValues(settings));

        return builder.clickHandler((panel, user, clickType, index) ->
        {
            this.consumer.accept(island);
            return true;
        }).build();
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
    private List<Island> elements;

    /**
     * This variable stores consumer.
     */
    private Consumer<Island> consumer;

    /**
     * User who runs GUI.
     */
    private User user;

    /**
     * Stores current pageIndex.
     */
    private int pageIndex;
}
