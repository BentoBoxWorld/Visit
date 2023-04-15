//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.visit;



import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Pladdon;


/**
 * This is a dummy class that allows to load addon via Bukkit Plugin loader.
 * @author BONNe
 */
public class VisitPladdon extends Pladdon
{
    @Override
    public Addon getAddon()
    {
        return new VisitAddon();
    }
}
