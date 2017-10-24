/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) gravityfox - https://gravityfox.net/
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.foxdenstudio.sponge.foxguard.plugin.command;


import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.util.FCCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.FCCommandBase;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.FlagMapper;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGlobal;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandDelete extends FCCommandBase {

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
        return true;
    };


    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .flagMapper(MAPPER)
                .parse();
        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else {
            String category = parse.args[0];
            FGCat fgCat = FGCat.from(category);
            if (fgCat == null) throw new CommandException(Text.of("\"" + category + "\" is not a valid category!"));

            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));

            FGUtil.OwnerResult ownerResult = FGUtil.processUserInput(parse.args[1]);

            IFGObject object;
            FGManager fgManager = FGManager.getInstance();
            switch (fgCat) {
                case REGION:
                    object = FGUtil.getRegionFromCommand(source, ownerResult, parse.flags.containsKey("world"), parse.flags.get("world"));
                    if (object instanceof IWorldRegion) fgCat = FGCat.WORLDREGION;
                    break;
                case HANDLER:
                    object = FGUtil.getHandlerFromCommand(ownerResult);
                    if (object instanceof IController) fgCat = FGCat.CONTROLLER;
                    break;
                default:
                    throw new CommandException(Text.of("Something went horribly wrong."));
            }

            if (object instanceof IGlobal) {
                throw new CommandException(Text.of("You may not delete the global " + fgCat.lName + "!"));
            }

            boolean success = fgManager.removeObject(object);
            if (!success)
                throw new CommandException(Text.of("There was an error trying to delete the " + fgCat.lName + "!"));
            source.sendMessage(Text.of(TextColors.GREEN, fgCat.uName + " deleted successfully!"));
            return CommandResult.success();
        }
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0)
                return Stream.of("region", "handler")
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            else if (parse.current.index == 1) {
                FGUtil.OwnerTabResult result = FGUtil.getOwnerSuggestions(parse.current.token);
                if (result.isComplete()) {
                    return result.getSuggestions().stream()
                            .map(str -> parse.current.prefix)
                            .collect(GuavaCollectors.toImmutableList());
                }

                if (isIn(REGIONS_ALIASES, parse.args[0])) {
                    String worldName = parse.flags.get("world");
                    boolean key = parse.flags.containsKey("world");
                    World world = null;
                    if (source instanceof Locatable) world = ((Locatable) source).getWorld();
                    if (!worldName.isEmpty()) {
                        Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                        if (optWorld.isPresent()) {
                            world = optWorld.get();
                        }
                    }
                    if (key && world != null) {
                        return FGManager.getInstance().getAllRegions(world, result.getOwner()).stream()
                                .filter(region -> !(region instanceof IGlobal))
                                .map(IFGObject::getName)
                                .filter(new StartsWithPredicate(result.getToken()))
                                .map(args -> parse.current.prefix + result.getPrefix() + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else {
                        return FGManager.getInstance().getAllRegionsWithUniqueNames(result.getOwner(), world).stream()
                                .filter(region -> !(region instanceof IGlobal))
                                .map(IFGObject::getName)
                                .filter(new StartsWithPredicate(result.getToken()))
                                .map(args -> parse.current.prefix + result.getPrefix() + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                    return FGManager.getInstance().getHandlers().stream()
                            .filter(handler -> !(handler instanceof IGlobal))
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(result.getToken()))
                            .map(args -> parse.current.prefix + result.getPrefix() + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY))
            return Stream.of("world")
                    .filter(new StartsWithPredicate(parse.current.token))
                    .map(args -> parse.current.prefix + args)
                    .collect(GuavaCollectors.toImmutableList());
        else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGVALUE)) {
            if (isIn(WORLD_ALIASES, parse.current.key))
                return Sponge.getGame().getServer().getWorlds().stream()
                        .map(World::getName)
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.objects.delete");
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("delete <region [--w:<world>] | handler> <name>");
    }

    private enum FGCat {
        REGION(REGIONS_ALIASES),
        WORLDREGION(null),
        HANDLER(HANDLERS_ALIASES),
        CONTROLLER(null);

        public final String[] catAliases;
        public final String lName = name().toLowerCase();
        public final String uName = FCCUtil.toCapitalCase(name());

        FGCat(String[] catAliases) {
            this.catAliases = catAliases;
        }

        public static FGCat from(String category) {
            for (FGCat cat : values()) {
                if (isIn(cat.catAliases, category)) return cat;
            }
            return null;
        }
    }
}
