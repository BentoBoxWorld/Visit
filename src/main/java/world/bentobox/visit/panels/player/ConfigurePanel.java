//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit.panels.player;


import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
import world.bentobox.visit.managers.VisitAddonManager;
import world.bentobox.visit.panels.ConversationUtils;
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
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();

        panelBuilder.user(this.user);
        panelBuilder.world(this.island.getWorld());

        panelBuilder.template("manage_panel", new File(this.addon.getDataFolder(), "panels"));

        panelBuilder.registerTypeBuilder("PAYMENT", this::createValueButton);
        panelBuilder.registerTypeBuilder("OFFLINE", this::createOfflineOnlyButton);
        panelBuilder.registerTypeBuilder("ALLOWED", this::createEnableButton);

        // Register unknown type builder.
        panelBuilder.build();
    }


    /**
     * Create value button panel item.
     *
     * @param template the template
     * @param itemSlot the item slot
     * @return the panel item
     */
    private PanelItem createValueButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot itemSlot)
    {
        final String reference = Constants.BUTTONS + "payment.";

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            builder.icon(template.icon().clone());
        }
        else
        {
            builder.icon(Material.ANVIL);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(template.title()));
        }
        else
        {
            builder.name(this.user.getTranslation(reference + "name"));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(template.description(),
                Constants.PARAMETER_NUMBER,
                Double.toString(this.addon.getAddonManager().getIslandEarnings(this.island))));
        }
        else
        {
            builder.description(this.user.getTranslationOrNothing(reference + "description",
                Constants.PARAMETER_NUMBER,
                Double.toString(this.addon.getAddonManager().getIslandEarnings(this.island))));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            template.actions().forEach(action -> {
                if (action.clickType() == clickType || action.clickType() == ClickType.UNKNOWN)
                {
                    if ("CHANGE".equalsIgnoreCase(action.actionType()))
                    {
                        Consumer<Number> numberConsumer = number -> {
                            if (number != null)
                            {
                                this.manager.setIslandEarnings(this.island, number.doubleValue());
                            }

                            // reopen panel
                            this.build();
                        };

                        final double maxAmount = this.addon.getSettings().getMaxAmount();

                        ConversationUtils.createNumericInput(numberConsumer,
                            this.user,
                            this.user.getTranslation(Constants.CONVERSATIONS + "input-number"),
                            0,
                            maxAmount > 0 ? maxAmount : Double.MAX_VALUE);
                    }
                }
            });

            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(action.tooltip())).
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
     * This method creates toggleable button that allows to switch between only online/ offline button.
     *
     * @param template the template
     * @param itemSlot the item slot
     * @return PanelItem button.
     */
    private PanelItem createOfflineOnlyButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot itemSlot)
    {
        final String reference = Constants.BUTTONS + "offline.";

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            builder.icon(template.icon().clone());
        }
        else
        {
            builder.icon(Material.REDSTONE_LAMP);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(template.title()));
        }
        else
        {
            builder.name(this.user.getTranslation(reference + "name"));
        }

        final boolean value = this.manager.hasOfflineEnabled(this.island);

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(template.description(),
                Constants.PARAMETER_VALUE,
                this.user.getTranslation(reference + (value ? "enabled" : "disabled"))));
        }
        else
        {
            builder.description(this.user.getTranslationOrNothing(reference + "description"));
            builder.description(this.user.getTranslationOrNothing(reference + (value ? "enabled" : "disabled")));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, slot) ->
        {
            template.actions().forEach(action -> {
                if (action.clickType() == clickType || action.clickType() == ClickType.UNKNOWN)
                {
                    if ("TOGGLE".equalsIgnoreCase(action.actionType()))
                    {
                        this.manager.setOfflineData(this.island, !value);
                        this.build();
                    }
                }
            });

            return true;
        });

        builder.glow(value);

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(action.tooltip())).
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
     * This method creates toggleable button that allows to switch if visiting is enabled or not.
     *
     * @param template the template
     * @param itemSlot the item slot
     * @return PanelItem button.
     */
    private PanelItem createEnableButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot itemSlot)
    {
        final String reference = Constants.BUTTONS + "enabled.";

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            builder.icon(template.icon().clone());
        }
        else
        {
            builder.icon(Material.PUMPKIN_PIE);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(template.title()));
        }
        else
        {
            builder.name(this.user.getTranslation(reference + "name"));
        }

        final boolean value = island.isAllowed(VisitAddon.ALLOW_VISITS_FLAG);

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(template.description(),
                Constants.PARAMETER_VALUE,
                this.user.getTranslation(reference + (value ? "enabled" : "disabled"))));
        }
        else
        {
            builder.description(this.user.getTranslationOrNothing(reference + "description"));
            builder.description(this.user.getTranslationOrNothing(reference + (value ? "enabled" : "disabled")));
        }

        List<ItemTemplateRecord.ActionRecords> actions = template.actions().stream().
            filter(action -> this.island.isAllowed(this.user, Flags.CHANGE_SETTINGS)).
            collect(Collectors.toList());

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, slot) ->
        {
            actions.forEach(action -> {
                if (action.clickType() == clickType || action.clickType() == ClickType.UNKNOWN)
                {
                    if ("TOGGLE".equalsIgnoreCase(action.actionType()))
                    {
                        this.island.setSettingsFlag(VisitAddon.ALLOW_VISITS_FLAG, !value);
                        this.build();
                    }
                }
            });

            return true;
        });

        // Collect tooltips.
        List<String> tooltips = actions.stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(actions.size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        builder.glow(value);

        return builder.build();
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
