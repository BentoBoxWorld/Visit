//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit.panels.admin;


import org.bukkit.Material;
import org.bukkit.World;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.configs.Settings;
import world.bentobox.visit.panels.ConversationUtils;
import world.bentobox.visit.panels.GuiUtils;
import world.bentobox.visit.panels.player.ConfigurePanel;
import world.bentobox.visit.utils.Constants;
import world.bentobox.visit.utils.Utils;


/**
 * This class allows to edit default values as well as manage other player island values from single GUI.
 */
public class AdminPanel
{
    /**
     * This is internal constructor. It is used internally in current class to avoid creating objects everywhere.
     *
     * @param addon VisitAddon object.
     */
    private AdminPanel(VisitAddon addon,
        World world,
        User user)
    {
        this.addon = addon;
        this.world = world;
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
            name(this.user.getTranslation(Constants.TITLES + "main"));

        // Fill border
        GuiUtils.fillBorder(panelBuilder, 5, Material.MAGENTA_STAINED_GLASS_PANE);

        panelBuilder.item(10, this.createButton(Button.MANAGE));
        panelBuilder.item(28, this.createButton(Button.RESET));
        panelBuilder.item(11, this.createButton(Button.TAX));

        panelBuilder.item(13, this.createButton(Button.AT_TOP));
        panelBuilder.item(22, this.createButton(Button.TOGGLE_FILTERS));
        panelBuilder.item(31, this.createButton(Button.TOGGLE_SEARCH));
        panelBuilder.item(14, this.createButton(Button.FILTER));

        panelBuilder.item(16, this.createButton(Button.DEFAULT_ENABLED));
        panelBuilder.item(25, this.createButton(Button.DEFAULT_OFFLINE));
        panelBuilder.item(34, this.createButton(Button.DEFAULT_PAYMENT));

        // At the end we just call build method that creates and opens panel.
        panelBuilder.build();
    }


    /**
     * Create button panel item.
     *
     * @param button the button
     * @return the panel item
     */
    private PanelItem createButton(Button button)
    {
        final String reference = Constants.BUTTONS + button.name().toLowerCase();
        String name = this.user.getTranslation(reference + ".name");
        List<String> description = new ArrayList<>();
        description.add(this.user.getTranslationOrNothing(reference + ".description"));

        Material material;
        PanelItem.ClickHandler clickHandler;
        boolean glow = false;

        switch (button)
        {
            case MANAGE:
            {
                material = Material.PLAYER_HEAD;
                description.add("");
                description.add(this.user.getTranslation(Constants.TIPS + "click-to-choose"));

                clickHandler = (panel, user, clickType, slot) ->
                {
                    // Filter out islands without owner.
                    // Sort by island and owner name

                    List<Island> islandList = this.addon.getIslands().getIslands(this.world).stream().
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

                    // Open Edit panel after user selected island.
                    SelectIslandPanel.open(user,
                        islandList,
                        island -> ConfigurePanel.openPanel(this.addon, island, this.user));

                    return true;
                };

                break;
            }
            case RESET:
            {
                material = Material.TNT;
                description.add("");
                description.add(this.user.getTranslation(Constants.TIPS + "click-to-reset"));

                clickHandler = (panel, user, clickType, slot) ->
                {
                    // Create consumer that accepts value from conversation.
                    Consumer<Boolean> consumer = value ->
                    {
                        if (value)
                        {
                            this.addon.getAddonManager().wipeDatabase();
                        }

                        this.build();
                    };

                    // Create conversation that gets user acceptance to delete island data.
                    ConversationUtils.createConfirmation(
                        consumer,
                        this.user,
                        this.user.getTranslation(Constants.CONVERSATIONS + "confirm-island-data-deletion",
                            Constants.PARAMETER_GAMEMODE, Utils.getGameMode(this.world)),
                        this.user.getTranslation(Constants.CONVERSATIONS + "user-data-removed",
                            Constants.PARAMETER_GAMEMODE, Utils.getGameMode(this.world)));

                    return true;
                };

                break;
            }
            case TAX:
            {
                material = Material.GOLD_INGOT;
                description.add(this.user.getTranslation(reference + ".value",
                    Constants.PARAMETER_NUMBER, String.valueOf(this.addon.getSettings().getTaxAmount())));
                description.add("");
                description.add(this.user.getTranslation(Constants.TIPS + "click-to-edit"));

                clickHandler = (panel, user, clickType, slot) ->
                {
                    Consumer<Number> numberConsumer = number ->
                    {
                        if (number != null)
                        {
                            this.addon.getSettings().setTaxAmount(number.doubleValue());
                            this.addon.saveSettings();
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

                break;
            }
            case DEFAULT_PAYMENT:
            {
                material = Material.ANVIL;
                description.add(this.user.getTranslation(reference + ".value",
                    Constants.PARAMETER_NUMBER, String.valueOf(this.addon.getSettings().getDefaultVisitingPayment())));
                description.add("");
                description.add(this.user.getTranslation(Constants.TIPS + "click-to-edit"));

                clickHandler = (panel, user, clickType, slot) ->
                {
                    Consumer<Number> numberConsumer = number ->
                    {
                        if (number != null)
                        {
                            this.addon.getSettings().setDefaultVisitingPayment(number.doubleValue());
                            this.addon.saveSettings();
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

                break;
            }
            case DEFAULT_OFFLINE:
            {
                material = Material.MUSIC_DISC_11;

                glow = this.addon.getSettings().isDefaultVisitingOffline();

                if (glow)
                {
                    description.add(this.user.getTranslation(reference + ".enabled"));
                }
                else
                {
                    description.add(this.user.getTranslation(reference + ".disabled"));
                }
                description.add("");
                description.add(this.user.getTranslation(Constants.TIPS + "click-to-toggle"));

                clickHandler = (panel, user, clickType, slot) ->
                {
                    this.addon.getSettings().setDefaultVisitingOffline(
                        !this.addon.getSettings().isDefaultVisitingOffline());
                    this.addon.saveSettings();
                    this.build();

                    return true;
                };

                break;
            }
            case DEFAULT_ENABLED:
            {
                material = Material.MINECART;

                glow = this.addon.getSettings().isDefaultVisitingEnabled();

                if (glow)
                {
                    description.add(this.user.getTranslation(reference + ".enabled"));
                }
                else
                {
                    description.add(this.user.getTranslation(reference + ".disabled"));
                }
                description.add("");
                description.add(this.user.getTranslation(Constants.TIPS + "click-to-toggle"));

                clickHandler = (panel, user, clickType, slot) ->
                {
                    this.addon.getSettings().setDefaultVisitingEnabled(
                        !this.addon.getSettings().isDefaultVisitingEnabled());
                    VisitAddon.ALLOW_VISITS_FLAG.setDefaultSetting(
                        this.addon.getSettings().isDefaultVisitingEnabled());
                    this.addon.saveSettings();

                    this.build();

                    return true;
                };

                break;
            }
            case AT_TOP:
            {
                material = Material.GLASS;

                glow = this.addon.getSettings().isFiltersTopLine();

                if (glow)
                {
                    description.add(this.user.getTranslation(reference + ".enabled"));
                }
                else
                {
                    description.add(this.user.getTranslation(reference + ".disabled"));
                }
                description.add("");
                description.add(this.user.getTranslation(Constants.TIPS + "click-to-toggle"));

                clickHandler = (panel, user, clickType, slot) ->
                {
                    this.addon.getSettings().setFiltersTopLine(
                        !this.addon.getSettings().isFiltersTopLine());
                    this.addon.saveSettings();
                    this.build();

                    return true;
                };

                break;
            }
            case TOGGLE_FILTERS:
            {
                material = Material.HOPPER;

                glow = this.addon.getSettings().isFiltersEnabled();

                if (glow)
                {
                    description.add(this.user.getTranslation(reference + ".enabled"));
                }
                else
                {
                    description.add(this.user.getTranslation(reference + ".disabled"));
                }
                description.add("");
                description.add(this.user.getTranslation(Constants.TIPS + "click-to-toggle"));

                clickHandler = (panel, user, clickType, slot) ->
                {
                    this.addon.getSettings().setFiltersEnabled(
                        !this.addon.getSettings().isFiltersEnabled());
                    this.addon.saveSettings();
                    this.build();

                    return true;
                };

                break;
            }
            case FILTER:
            {
                switch (this.addon.getSettings().getDefaultFilter())
                {
                    case ONLINE_ISLANDS:
                        material = Material.SANDSTONE_STAIRS;
                        break;
                    case CAN_VISIT:
                        material = Material.SANDSTONE_STAIRS;
                        break;
                    default:
                        material = Material.SMOOTH_SANDSTONE;
                        break;
                }

                description.add(this.user.getTranslation(reference + "." +
                    this.addon.getSettings().getDefaultFilter().name().toLowerCase()));
                description.add("");
                description.add(this.user.getTranslation(Constants.TIPS + "click-to-switch"));

                clickHandler = (panel, user, clickType, slot) -> {
                    if (clickType.isRightClick())
                    {
                        // Clear string.
                        this.addon.getSettings().setDefaultFilter(Utils.getPreviousValue(
                            Settings.Filter.values(),
                            this.addon.getSettings().getDefaultFilter()));
                    }
                    else
                    {
                        this.addon.getSettings().setDefaultFilter(Utils.getNextValue(
                            Settings.Filter.values(),
                            this.addon.getSettings().getDefaultFilter()));
                    }
                    // Save settings
                    this.addon.saveSettings();
                    // Rebuild gui.
                    this.build();

                    return true;
                };

                break;
            }
            case TOGGLE_SEARCH:
            {
                material = Material.PAPER;

                glow = this.addon.getSettings().isSearchEnabled();

                if (glow)
                {
                    description.add(this.user.getTranslation(reference + ".enabled"));
                }
                else
                {
                    description.add(this.user.getTranslation(reference + ".disabled"));
                }
                description.add("");
                description.add(this.user.getTranslation(Constants.TIPS + "click-to-toggle"));

                clickHandler = (panel, user, clickType, slot) ->
                {
                    this.addon.getSettings().setSearchEnabled(
                        !this.addon.getSettings().isSearchEnabled());
                    this.addon.saveSettings();
                    this.build();

                    return true;
                };

                break;
            }
            case DEFAULT_RANK:
                // TODO: need Implementing
                return PanelItem.empty();
            case ICON:
                // TODO: need Implementing
                return PanelItem.empty();
            case BORDER_BLOCK:
                // TODO: need Implementing
                return PanelItem.empty();
            case BORDER_BLOCK_NAME:
                // TODO: need Implementing
                return PanelItem.empty();
            default:
                return PanelItem.empty();
        }

        return new PanelItemBuilder().
            name(name).
            description(description).
            icon(material).
            clickHandler(clickHandler).
            glow(glow).
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
        World world,
        User user)
    {
        new AdminPanel(addon, world, user).build();
    }


    /**
     * This enum holds all buttons from the GUI.
     */
    private enum Button
    {
        /**
         * Allows to manage island settings.
         */
        MANAGE,
        /**
         * Allows to reset all island config to the default values.
         */
        RESET,
        /**
         * Allows to change tax amount.
         */
        TAX,
        /**
         * Allows to change default payment amount.
         */
        DEFAULT_PAYMENT,
        /**
         * Allows to change default offline visit mode.
         */
        DEFAULT_OFFLINE,
        /**
         * Allows to change default visiting enable mode.
         */
        DEFAULT_ENABLED,
        /**
         * Allows to switch filters from top to bottom.
         */
        AT_TOP,
        /**
         * Allows to toggle filters
         */
        TOGGLE_FILTERS,
        /**
         * Allows to change default active filter
         */
        FILTER,
        /**
         * Allows to toggle search button
         */
        TOGGLE_SEARCH,
        /**
         * Allows to change default rank for changing config.
         */
        DEFAULT_RANK,
        /**
         * Allows to switch default island icon.
         */
        ICON,
        /**
         * Allows to change border block material.
         */
        BORDER_BLOCK,
        /**
         * Allows to change border block name.
         */
        BORDER_BLOCK_NAME,
    }


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


    /**
     * This variable allows to access addon object.
     */
    private final VisitAddon addon;

    /**
     * This variable stores main world where GUI is targeted.
     */
    private final World world;

    /**
     * This variable holds user who opens panel. Without it panel cannot be opened.
     */
    private final User user;
}

