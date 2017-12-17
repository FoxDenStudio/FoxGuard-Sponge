# FoxGuard [![Build Status](https://travis-ci.org/FoxDenStudio/FoxGuard-Sponge.svg?branch=api5)](https://travis-ci.org/FoxDenStudio/FoxGuard-Sponge) [![Join the chat at https://gitter.im/FoxDenStudio/FoxGuard-Sponge](https://badges.gitter.im/FoxDenStudio/FoxGuard-Sponge.svg)](https://gitter.im/FoxDenStudio/FoxGuard-Sponge?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
A Minecraft world protection plugin for [SpongeAPI](https://github.com/SpongePowered/SpongeAPI).

Requires [FoxCore](https://github.com/FoxDenStudio/FoxCore-Sponge).

## Why not WorldGuard?
While I am aware that WorldGuard is being ported to Sponge, the way this plugin handles the protection is different.
Also, at the time of it's creation, there really wasn't a flexible and powerful solution in protecting places like spawn areas.
rom handlers, in that you create them separately and then link them as needed.
Regions define areas of effec
## How it works
This plugin separates regions ft, with some being contained within a world, and others spanning multiple worlds.
Handlers are instructions for how events should be handled. They are global for the server.

When events are fired, they are checked to see if they fall within one or more regions. 
The event is then passed on to all Handlers that have been linked to the corresponding regions. 
The handlers are then evaluated from highest to lowest priority, and the first non-ambivalent response is used to cancel/allow the action.

This allows some very complex rules for those who need the power to control everything down to the finest detail.
However, this also allows for very simple rules that are much friendlier to lower performing servers.

Essentially, regions can be a simple as rectangular plots of land, or as complex as mandelbrot fractals.
Handlers can be as simple as a "block everything for everyone" or as complicated as the United States government.
This offers both performance and power, and allows users to use only what they need.
It also allows other plugins to add their own regions and handlers for plugin interoperability or simply more options.

## Building from source
### Downloading
Make sure you have git installed. If you don't have it installed, you can find downloads for it [here](https://git-scm.com/downloads).
Recommended settings in the wizard should be fine. I would personally add git to the command line though (Second option out of three on one of the wizard screens).

Then run:

`git clone --recursive https://github.com/foxdenstudio/FoxGuard.git`

If there's a specific branch or commit that you want to build run these commands as well:

`git checkout <yourbranchorcommit>`
`git submodule update`

### Building
Open a command line inside the directory.

If you have gradle installed:

`gradle build`

If you don't have gradle installed (Or if you just want to use the wrapper for sanity's sake):

`./gradlew build`

### Build Location
The built jarfile can be found under `./build/libs`
It should be named something like FoxGuard-SNAPSHOT.jar with some version info and whatnot.

The required version of FoxCore is also (conveniently) built in the process, and can be found under `./FoxCore/build/libs`

## Note from the author about the plugin 
When I started writing this plugin it was meant as a one-off compile and forget plugin to protect a spawn area for a friend.
The dimension was supposed to be hard coded and it was only supposed to take a day or two. I named it as such because I couldn't actually think of a good name.
Well, it was supposed to be a temporary replacement of WorldGuard and I'm a fox soo... that's the name.
Somehow, this one-off project evolved into something bigger, and I started getting more involved with Sponge in general.
One day I needed to ask a quick question so I decided to hop on the #spongedev irc channel. There I met some the most awesome people ever.
So shoutout to all the Sponge devs. You guys are the best.
Note over.
