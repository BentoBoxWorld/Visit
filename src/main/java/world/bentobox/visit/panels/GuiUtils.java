//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.visit.panels;


import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Collections;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;


public class GuiUtils
{
// ---------------------------------------------------------------------
// Section: Border around GUIs
// ---------------------------------------------------------------------


    /**
     * This method creates border of black panes around given panel with 5 rows.
     *
     * @param panelBuilder PanelBuilder which must be filled with border blocks.
     */
    public static void fillBorder(PanelBuilder panelBuilder)
    {
        GuiUtils.fillBorder(panelBuilder, 5, Material.BLACK_STAINED_GLASS_PANE);
    }


    /**
     * This method sets black stained glass pane around Panel with given row count.
     *
     * @param panelBuilder object that builds Panel.
     * @param rowCount in Panel.
     */
    public static void fillBorder(PanelBuilder panelBuilder, int rowCount)
    {
        GuiUtils.fillBorder(panelBuilder, rowCount, Material.BLACK_STAINED_GLASS_PANE);
    }


    /**
     * This method sets blocks with given Material around Panel with 5 rows.
     *
     * @param panelBuilder object that builds Panel.
     * @param material that will be around Panel.
     */
    public static void fillBorder(PanelBuilder panelBuilder, Material material)
    {
        GuiUtils.fillBorder(panelBuilder, 5, material);
    }


    /**
     * This method sets blocks with given Material around Panel with given row count.
     *
     * @param panelBuilder object that builds Panel.
     * @param rowCount in Panel.
     * @param material that will be around Panel.
     */
    public static void fillBorder(PanelBuilder panelBuilder, int rowCount, Material material)
    {
        // Only for useful filling.
        if (rowCount < 3)
        {
            return;
        }

        for (int i = 0; i < 9 * rowCount; i++)
        {
            // First (i < 9) and last (i > 35) rows must be filled
            // First column (i % 9 == 0) and last column (i % 9 == 8) also must be filled.

            if (i < 9 || i > 9 * (rowCount - 1) || i % 9 == 0 || i % 9 == 8)
            {
                panelBuilder.item(i, BorderBlock.getPanelBorder(material, " "));
            }
        }
    }


    /**
     * This BorderBlock is simple PanelItem but without item meta data.
     */
    private static class BorderBlock extends PanelItem
    {
        private BorderBlock(ItemStack icon)
        {
            super(new PanelItemBuilder().
                icon(icon.clone()).
                name(icon.getItemMeta().getDisplayName()).
                description(Collections.emptyList()).
                glow(false).
                clickHandler(null));
        }


        /**
         * This method retunrs BorderBlock with requested item stack.
         *
         * @param material of which broder must be created.
         * @return PanelItem that acts like border.
         */
        private static BorderBlock getPanelBorder(Material material, String name)
        {
            ItemStack itemStack = new ItemStack(material);
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(name);
            itemStack.setItemMeta(meta);

            return new BorderBlock(itemStack);
        }
    }
}
