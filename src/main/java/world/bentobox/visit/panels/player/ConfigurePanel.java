package world.bentobox.visit.panels.player;


import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.database.object.IslandVisitSettings;
import world.bentobox.visit.managers.VisitAddonManager;
import world.bentobox.visit.panels.ConversationUtils;
import world.bentobox.visit.panels.GuiUtils;
import world.bentobox.visit.utils.Constants;


/**
 * This class shows how to set up easy panel by using BentoBox PanelBuilder API
 */
public class ConfigurePanel
{
    /**
     * This is internal constructor. It is used internally in current class to avoid creating objects everywhere.
     *
     * @param addon VisitAddon object.
     */
    private ConfigurePanel(VisitAddon addon,
        @Nullable Island island,
        User user)
    {
        this.addon = addon;
        this.manager = this.addon.getAddonManager();
        this.island = island;
        this.user = user;
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
            name(this.user.getTranslation(Constants.TITLES + "configure")).
            type(Panel.Type.HOPPER);

        if (this.island == null)
        {
            // Nothing to open as user do not have island anymore.
            return;
        }

        IslandVisitSettings settings = this.manager.getIslandVisitSettings(this.island);

        if (this.addon.getVaultHook().hook())
        {
            // Add value button only if vault is enabled.
            panelBuilder.item(0, this.createValueButton(settings));
        }

        panelBuilder.item(2, this.createOfflineOnlyButton(settings));
        panelBuilder.item(4, this.createEnableButton(this.island));

        // At the end we just call build method that creates and opens panel.
        panelBuilder.build();
    }


    /**
     * This method creates input button that allows to write any number in chat.
     *
     * @param settings Settings where this number will be saved.
     * @return PanelItem button that allows to change payment value.
     */
    private PanelItem createValueButton(IslandVisitSettings settings)
    {
        String name = this.user.getTranslation(Constants.BUTTONS + "payment.name");
        List<String> description = new ArrayList<>(3);

        description.add(this.user.getTranslation(Constants.BUTTONS + "payment.description",
            Constants.PARAMETER_NUMBER, Double.toString(settings.getPayment())));

        description.add("");
        description.add(this.user.getTranslation(Constants.TIPS + "click-to-change"));

        ItemStack icon = new ItemStack(Material.ANVIL);
        PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) ->
        {
            Consumer<Number> numberConsumer = number -> {
                if (number != null)
                {
                    settings.setPayment(number.doubleValue());
                    this.manager.saveSettings(settings);
                }

                // reopen panel
                this.build();
            };

            ConversationUtils.createNumericInput(numberConsumer,
                this.user,
                this.user.getTranslation(Constants.CONVERSATIONS + "input-number"),
                0,
                Double.MAX_VALUE);

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
     * This method creates toggleable button that allows to switch between only online/ offline button.
     *
     * @param settings Settings where this number will be saved.
     * @return PanelItem button.
     */
    private PanelItem createOfflineOnlyButton(IslandVisitSettings settings)
    {
        String name = this.user.getTranslation(Constants.BUTTONS + "offline.name");
        List<String> description = new ArrayList<>(5);
        description.add(this.user.getTranslation(Constants.BUTTONS + "offline.description"));

        ItemStack icon;

        if (settings.isOfflineVisit())
        {
            description.add(this.user.getTranslation(Constants.BUTTONS + "offline.enabled"));
            icon = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        }
        else
        {
            description.add(this.user.getTranslation(Constants.BUTTONS + "offline.disabled"));
            icon = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        }

        description.add("");
        description.add(this.user.getTranslation(Constants.TIPS + "click-to-toggle"));

        PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) ->
        {
            settings.setOfflineVisit(!settings.isOfflineVisit());
            this.manager.saveSettings(settings);

            panel.getInventory().setItem(slot, this.createOfflineOnlyButton(settings).getItem());

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
     * This method creates toggleable button that allows to switch if visiting is enabled or not.
     *
     * @return PanelItem button.
     */
    private PanelItem createEnableButton(Island island)
    {
        boolean isAllowed = island.isAllowed(VisitAddon.ALLOW_VISITS_FLAG);

        String name = this.user.getTranslation(Constants.BUTTONS + "enabled.name");
        List<String> description = new ArrayList<>(5);
        description.add(this.user.getTranslation(Constants.BUTTONS + "enabled.description"));

        if (isAllowed)
        {
            description.add(this.user.getTranslation(Constants.BUTTONS + "enabled.enabled"));
        }
        else
        {
            description.add(this.user.getTranslation(Constants.BUTTONS + "enabled.disabled"));
        }

        description.add("");
        description.add(this.user.getTranslation(Constants.TIPS + "click-to-toggle"));


        ItemStack icon = new ItemStack(Material.PUMPKIN_PIE);
        PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) ->
        {
            island.setSettingsFlag(VisitAddon.ALLOW_VISITS_FLAG, !isAllowed);
            this.build();

            return true;
        };

        return new PanelItemBuilder().
            name(name).
            description(description).
            icon(icon).
            clickHandler(clickHandler).
            glow(isAllowed).
            build();
    }


    /**
     * This method is used to open UserPanel outside this class. It will be much easier to open panel with single method
     * call then initializing new object.
     *
     * @param addon VisitAddon object
     * @param user User who opens panel
     */
    public static void openPanel(VisitAddon addon,
        @Nullable Island island,
        User user)
    {
        new ConfigurePanel(addon, island, user).build();
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
     * This variable stores main island which GUI is targeted.
     */
    private final Island island;

    /**
     * This variable holds user who opens panel. Without it panel cannot be opened.
     */
    private final User user;
}
