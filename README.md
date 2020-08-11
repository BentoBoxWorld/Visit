# Visit Addon
[![Discord](https://img.shields.io/discord/272499714048524288.svg?logo=discord)](https://discord.bentobox.world)
[![Build Status](https://ci.codemc.io/buildStatus/icon?job=BentoBoxWorld/Visit)](https://ci.codemc.io/job/BentoBoxWorld/job/Visit/)

This is Visit Addon for BentoBox plugin.  

## How to use

1. Place the addon jar in the addons folder of the BentoBox plugin
2. Restart the server
3. In game you can change flags that allows to use current addon.

## Small information

The main idea of the addon is to allow faster ways how to visit any island. 
The main difference from warps addon is that unlike warps, signs are not necessary, and users can travel to any island.

Users can visit others by executing `/[player_command] visit <player_name>` or by choosing an island from the GUI which opens after executing command `/[player_command] visit` 

However, users can disable the ability to travel to their island via island settings flag "Visit protection".
There is also an option to disable visiting while island members are offline. It can be done by `/[player_command] visit configure` command.

This addon offers also an option to earn money from visitors. In the same GUI, island members can set payment price for visitors. All incoming money will go to the island owner.

There is also protection setting for island owner, which allows specifying minimum rank that can access to island visiting config editor: "Manage visitor config".  

Server admins have also an in-game GUI that allows to edit config options.
There is also an option to set tax amount for visitors, that will be taken from player account on each island visit.

## Compatibility

- [x] BentoBox 1.14+
