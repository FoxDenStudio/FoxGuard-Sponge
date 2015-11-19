# FoxGuard [![Build Status](https://travis-ci.org/gravityfox/FoxGuard.svg?branch=master)](https://travis-ci.org/gravityfox/FoxGuard)
A Minecraft world protection plugin for [SpongeAPI](https://github.com/SpongePowered/SpongeAPI)

## Why not WorldGuard?
While I am aware that WorldGuard is being ported to Sponge, the way this plugin handles the protection is different.
Also, at the time of it's creation, there really wasn't a flexible and powerful solution in protecting places like spawn areas.

## How it works
This plugin separates regions from flags, in that you create them separately and then link them as needed.
Regions define areas of effect, and each dimension has its own unique regions. (Dynamic Regions are under consideration, but not high priority.)
FlagSets are groups of rules about how events should be handled. They are global for the server.

When events are fired, they are checked to see if they fall within one or more regions. The event is then passed on to all FlagSets that have been linked to the corrisponding regions. The flagsets are then evaluated from highest to lowest priority, and the first non-ambivalent response is used to cancel/allow the action.

This allows some very complex rules for those who need the power to control everything down to the finest detail.
However, this also allows for very simple rules that are much friendlier to lower performing servers.

Essentially, Regions can be a simple as rectangular plots of land, or as complex as mandelbrot fractals.
FlagSets can be as simple as a "block everything for everyone" or as complicated as the United States government.
This offers both performance and power, and allows users to use only what they need.
It also allows other plugins to add their own Regions and FlagSets for plugin interoperability or simply more options.
