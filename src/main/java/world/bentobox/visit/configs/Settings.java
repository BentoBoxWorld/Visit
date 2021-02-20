//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.visit.configs;


import org.bukkit.Material;
import java.util.HashSet;
import java.util.Set;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;


/**
 * Settings that implements ConfigObject is powerful and dynamic Config Objects that does not need custom parsing. If it
 * is correctly loaded, all its values will be available.
 * <p>
 * Without Getter and Setter this class will not work.
 * <p>
 * To specify location for config object to be stored, you should use @StoreAt(filename="{config file name}",
 * path="{Path to your addon}") To save comments in config file you should use @ConfigComment("{message}") that adds any
 * message you want to be in file.
 */
@StoreAt(filename = "config.yml", path = "addons/Visit")
@ConfigComment("VisitAddon Configuration [version]")
@ConfigComment("This config file is dynamic and saved when the server is shutdown.")
@ConfigComment("")
public class Settings implements ConfigObject
{
// ---------------------------------------------------------------------
// Section: Constructor
// ---------------------------------------------------------------------


    /**
     * Default constructor init.
     */
    public Settings()
    {
    }


// ---------------------------------------------------------------------
// Section: Getters and Setters
// ---------------------------------------------------------------------


    /**
     * Gets tax amount.
     *
     * @return the tax amount
     */
    public double getTaxAmount()
    {
        return taxAmount;
    }


    /**
     * Sets tax amount.
     *
     * @param taxAmount the tax amount
     */
    public void setTaxAmount(double taxAmount)
    {
        this.taxAmount = taxAmount;
    }


    /**
     * Gets default visiting payment.
     *
     * @return the default visiting payment
     */
    public double getDefaultVisitingPayment()
    {
        return defaultVisitingPayment;
    }


    /**
     * Sets default visiting payment.
     *
     * @param defaultVisitingPayment the default visiting payment
     */
    public void setDefaultVisitingPayment(double defaultVisitingPayment)
    {
        this.defaultVisitingPayment = defaultVisitingPayment;
    }


    /**
     * Is default visiting offline boolean.
     *
     * @return the boolean
     */
    public boolean isDefaultVisitingOffline()
    {
        return defaultVisitingOffline;
    }


    /**
     * Sets default visiting offline.
     *
     * @param defaultVisitingOffline the default visiting offline
     */
    public void setDefaultVisitingOffline(boolean defaultVisitingOffline)
    {
        this.defaultVisitingOffline = defaultVisitingOffline;
    }


    /**
     * Is default visiting enabled boolean.
     *
     * @return the boolean
     */
    public boolean isDefaultVisitingEnabled()
    {
        return defaultVisitingEnabled;
    }


    /**
     * Sets default visiting enabled.
     *
     * @param defaultVisitingEnabled the default visiting enabled
     */
    public void setDefaultVisitingEnabled(boolean defaultVisitingEnabled)
    {
        this.defaultVisitingEnabled = defaultVisitingEnabled;
    }


    /**
     * Is filters enabled boolean.
     *
     * @return the boolean
     */
    public boolean isFiltersEnabled()
    {
        return filtersEnabled;
    }


    /**
     * Sets filters enabled.
     *
     * @param filtersEnabled the filters enabled
     */
    public void setFiltersEnabled(boolean filtersEnabled)
    {
        this.filtersEnabled = filtersEnabled;
    }


    /**
     * Gets default filter.
     *
     * @return the default filter
     */
    public Filter getDefaultFilter()
    {
        return defaultFilter;
    }


    /**
     * Sets default filter.
     *
     * @param defaultFilter the default filter
     */
    public void setDefaultFilter(Filter defaultFilter)
    {
        this.defaultFilter = defaultFilter;
    }


    /**
     * Is search enabled boolean.
     *
     * @return the boolean
     */
    public boolean isSearchEnabled()
    {
        return searchEnabled;
    }


    /**
     * Sets search enabled.
     *
     * @param searchEnabled the search enabled
     */
    public void setSearchEnabled(boolean searchEnabled)
    {
        this.searchEnabled = searchEnabled;
    }


    /**
     * Is top line boolean.
     *
     * @return the boolean
     */
    public boolean isFiltersTopLine()
    {
        return filtersTopLine;
    }


    /**
     * Sets top line.
     *
     * @param filtersTopLine the top line
     */
    public void setFiltersTopLine(boolean filtersTopLine)
    {
        this.filtersTopLine = filtersTopLine;
    }


    /**
     * Gets island icon.
     *
     * @return the island icon
     */
    public Material getIslandIcon()
    {
        return islandIcon;
    }


    /**
     * Sets island icon.
     *
     * @param islandIcon the island icon
     */
    public void setIslandIcon(Material islandIcon)
    {
        this.islandIcon = islandIcon;
    }


    /**
     * Gets border block.
     *
     * @return the border block
     */
    public Material getBorderBlock()
    {
        return borderBlock;
    }


    /**
     * Sets border block.
     *
     * @param borderBlock the border block
     */
    public void setBorderBlock(Material borderBlock)
    {
        this.borderBlock = borderBlock;
    }


    /**
     * Gets border block name.
     *
     * @return the border block name
     */
    public String getBorderBlockName()
    {
        return borderBlockName;
    }


    /**
     * Sets border block name.
     *
     * @param borderBlockName the border block name
     */
    public void setBorderBlockName(String borderBlockName)
    {
        this.borderBlockName = borderBlockName;
    }


    /**
     * Gets player main command.
     *
     * @return the player main command
     */
    public String getPlayerMainCommand()
    {
        return playerMainCommand;
    }


    /**
     * Sets player main command.
     *
     * @param playerMainCommand the player main command
     */
    public void setPlayerMainCommand(String playerMainCommand)
    {
        this.playerMainCommand = playerMainCommand;
    }


    /**
     * Gets player configure command.
     *
     * @return the player configure command
     */
    public String getPlayerConfigureCommand()
    {
        return playerConfigureCommand;
    }


    /**
     * Sets player configure command.
     *
     * @param playerConfigureCommand the player configure command
     */
    public void setPlayerConfigureCommand(String playerConfigureCommand)
    {
        this.playerConfigureCommand = playerConfigureCommand;
    }


    /**
     * Gets admin main command.
     *
     * @return the admin main command
     */
    public String getAdminMainCommand()
    {
        return adminMainCommand;
    }


    /**
     * Sets admin main command.
     *
     * @param adminMainCommand the admin main command
     */
    public void setAdminMainCommand(String adminMainCommand)
    {
        this.adminMainCommand = adminMainCommand;
    }


    /**
     * Gets disabled game modes.
     *
     * @return the disabled game modes
     */
    public Set<String> getDisabledGameModes()
    {
        return disabledGameModes;
    }


    /**
     * Sets disabled game modes.
     *
     * @param disabledGameModes the disabled game modes
     */
    public void setDisabledGameModes(Set<String> disabledGameModes)
    {
        this.disabledGameModes = disabledGameModes;
    }


    /**
     * Gets default config permission.
     *
     * @return the default config permission
     */
    public int getDefaultConfigPermission()
    {
        return defaultConfigPermission;
    }


    /**
     * Sets default config permission.
     *
     * @param defaultConfigPermission the default config permission
     */
    public void setDefaultConfigPermission(int defaultConfigPermission)
    {
        this.defaultConfigPermission = defaultConfigPermission;
    }


    /**
     * Gets player set location command.
     *
     * @return the player set location command
     */
    public String getPlayerSetLocationCommand()
    {
        return playerSetLocationCommand;
    }


    /**
     * Sets player set location command.
     *
     * @param playerSetLocationCommand the player set location command
     */
    public void setPlayerSetLocationCommand(String playerSetLocationCommand)
    {
        this.playerSetLocationCommand = playerSetLocationCommand;
    }


// ---------------------------------------------------------------------
// Section: Enums
// ---------------------------------------------------------------------


    /**
     * This enum stores filter values for Player GUI.
     */
    public enum Filter
    {
        /**
         * Indicate that filter should be showing all islands.
         */
        ALL_ISLANDS,
        /**
         * Indicate that filter should be showing only online player islands.
         */
        ONLINE_ISLANDS,
        /**
         * Indicate that filter should be showing only islands that can be visited.
         */
        CAN_VISIT
    }


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


    /**
     * The Tax amount.
     */
    @ConfigComment("")
    @ConfigComment("Allows to define tax amount (not %) that must be payed extra for visiting.")
    @ConfigComment("Example:")
    @ConfigComment("   Island payment is set to 10, and tax is set to 5, then visitor will need to pay 15.")
    @ConfigComment("   10 will go to island owner, while 5 will be erased from existence.")
    @ConfigEntry(path = "traveling-tax")
    private double taxAmount = 0.0;

    /**
     * The Default visiting payment.
     */
    @ConfigComment("")
    @ConfigComment("Allows to define default payment for visiting the island.")
    @ConfigComment("Setting 0 or less will mean that default value is free.")
    @ConfigComment("Payment goes to island owner.")
    @ConfigEntry(path = "default-settings.payment")
    private double defaultVisitingPayment = 0.0;

    /**
     * The Default visiting offline.
     */
    @ConfigComment("")
    @ConfigComment("Allows to set if players can visit other islands while")
    @ConfigComment("all members are offline.")
    @ConfigEntry(path = "default-settings.offline")
    private boolean defaultVisitingOffline = true;

    /**
     * The Default visiting enabled.
     */
    @ConfigComment("")
    @ConfigComment("Allows to set if default visit flag value is set as")
    @ConfigComment("enabled (true) or disabled (false).")
    @ConfigEntry(path = "default-settings.enabled")
    private boolean defaultVisitingEnabled = false;

    /**
     * The Default config permission.
     */
    @ConfigComment("")
    @ConfigComment("Allows to set default rank that can edit config settings.")
    @ConfigComment("OWNER_RANK = 1000")
    @ConfigComment("SUB_OWNER_RANK = 900")
    @ConfigComment("MEMBER_RANK = 500")
    @ConfigEntry(path = "default-settings.permission", needsRestart = true)
    private int defaultConfigPermission = 900;

    /**
     * The Filters enabled.
     */
    @ConfigComment("Allows to toggle if player GUI should show filter line.")
    @ConfigComment("enabled (true) or disabled (false).")
    @ConfigEntry(path = "gui.filters.enabled")
    private boolean filtersEnabled = true;

    /**
     * The Default filter.
     */
    @ConfigComment("Allows to switch default active filter for Player Visit GUI.")
    @ConfigComment("   ALL_ISLANDS - all islands will be available in the GUI.")
    @ConfigComment("   ONLINE_ISLANDS - all online islands will be available in the GUI.")
    @ConfigComment("   CAN_VISIT - all islands that can be visited will be available in the GUI.")
    @ConfigEntry(path = "gui.filters.show")
    private Filter defaultFilter = Filter.ALL_ISLANDS;

    /**
     * The Search enabled.
     */
    @ConfigComment("Allows to toggle if player GUI should show search button.")
    @ConfigComment("enabled (true) or disabled (false).")
    @ConfigEntry(path = "gui.filters.search")
    private boolean searchEnabled = true;

    /**
     * The Filters top line.
     */
    @ConfigComment("Allows to toggle if filters line should be at the top or bottom.")
    @ConfigComment("enabled (true) or disabled (false).")
    @ConfigEntry(path = "gui.filters.top")
    private boolean filtersTopLine = true;

    /**
     * The Island icon.
     */
    @ConfigComment("Allows to change icon in the GUI for island selection.")
    @ConfigComment("PLAYER_HEAD will be transformed to island owner head.")
    @ConfigComment("Island owner permission `visit.icon.[material]` can overwrite the icon.")
    @ConfigEntry(path = "gui.island-icon")
    private Material islandIcon = Material.PLAYER_HEAD;

    /**
     * The Border block.
     */
    @ConfigComment("Allows to change icon in the GUI for island selection.")
    @ConfigComment("PLAYER_HEAD will be transformed to island owner head.")
    @ConfigComment("Island owner permission `visit.icon.[material]` can overwrite the icon.")
    @ConfigEntry(path = "gui.border-block")
    private Material borderBlock = Material.MAGENTA_STAINED_GLASS_PANE;

    /**
     * The Border block name.
     */
    @ConfigComment("Allows to change icon in the GUI for island selection.")
    @ConfigComment("PLAYER_HEAD will be transformed to island owner head.")
    @ConfigComment("Island owner permission `visit.icon.[material]` can overwrite the icon.")
    @ConfigEntry(path = "gui.border-block-name")
    private String borderBlockName = " ";

    /**
     * The Player main command.
     */
    @ConfigComment("Player main sub-command to access the addon.")
    @ConfigComment("This command label will be required to write after gamemode player command label, f.e. /[label] visit")
    @ConfigEntry(path = "commands.player.main", needsRestart = true)
    private String playerMainCommand = "visit";

    /**
     * The Player configure command.
     */
    @ConfigComment("Player configure sub-command that allows to see configure GUI.")
    @ConfigComment("This command label will be required to write after gamemode player command label, f.e. /[label] visit configure")
    @ConfigEntry(path = "commands.player.configure", needsRestart = true)
    private String playerConfigureCommand = "configure";

    /**
     * The Player setLocation command.
     */
    @ConfigComment("Player setLocation sub-command that allows to change spawn location for visitors.")
    @ConfigComment("This command label will be required to write after gamemode player command label, f.e. /[label] visit setLocation")
    @ConfigEntry(path = "commands.player.set-location", needsRestart = true)
    private String playerSetLocationCommand = "setLocation";

    /**
     * The Admin main command.
     */
    @ConfigComment("Admin main sub-command to access the addon.")
    @ConfigComment("This command label will be required to write after gamemode admin command label, f.e. /[label] visit")
    @ConfigEntry(path = "commands.admin.main", needsRestart = true)
    private String adminMainCommand = "visit";

    /**
     * You can define any variable you want, as long as it can be serialized. For each variable you need corresponding
     * Getter and Setter method.
     * <p>
     * To specify which config entry this variable refer you can just add @ConfigEntry(path = "{Path to your entry}")
     * <p>
     * Good codding practise is to initialize variable with default value
     */
    @ConfigComment("")
    @ConfigComment("This list stores GameModes in which Example addon should not work.")
    @ConfigComment("To disable addon it is necessary to write its name in new line that starts with -. Example:")
    @ConfigComment("disabled-gamemodes:")
    @ConfigComment(" - BSkyBlock")
    @ConfigEntry(path = "disabled-gamemodes")
    private Set<String> disabledGameModes = new HashSet<>();
}
