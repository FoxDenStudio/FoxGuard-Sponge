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


import com.flowpowered.math.vector.Vector2i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.FCCommandBase;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.FlagMapper;
import net.foxdenstudio.sponge.foxcore.plugin.util.BoundingBox2;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.object.FGObjectData;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.FoxPath;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.IOwner;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.RectangularRegion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.WORLD_ALIASES;
import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.isIn;

public class CommandTest extends FCCommandBase {

    /*
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this
            command!"));
            return CommandResult.empty();
        }
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).limit(3)
        .parse2();
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD, "-----------------------------\n"));
        builder.append(Text.of(TextColors.GOLD, "Args: \"", TextColors.RESET, arguments,
        TextColors.GOLD, "\"\n"));
        builder.append(Text.of(TextColors.GOLD, "Type: ", TextColors.RESET, parse.currentElement
        .type, TextColors.GOLD, "\n"));
        builder.append(Text.of(TextColors.GOLD, "Token: \"", TextColors.RESET, parse
        .currentElement.token, TextColors.GOLD, "\"\n"));
        builder.append(Text.of(TextColors.GOLD, "Index: ", TextColors.RESET, parse.currentElement
        .index, TextColors.GOLD, "\n"));
        builder.append(Text.of(TextColors.GOLD, "Key: \"", TextColors.RESET, parse.currentElement
        .key, TextColors.GOLD, "\"\n"));
        source.sendMessage(builder.build());
        return CommandResult.empty();
    }
    */

    /*@Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws
    CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .autoCloseQuotes(true)
                .limit(2)
                .parse();
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD, "\n-----------------------------\n"));
        builder.append(Text.of(TextColors.GOLD, "Args: \"", TextColors.RESET, arguments,
        TextColors.GOLD, "\"\n"));
        builder.append(Text.of(TextColors.GOLD, "Type: ", TextColors.RESET, parse.current.type,
        TextColors.GOLD, "\n"));
        builder.append(Text.of(TextColors.GOLD, "Token: \"", TextColors.RESET, parse.current
        .token, TextColors.GOLD, "\"\n"));
        builder.append(Text.of(TextColors.GOLD, "Index: ", TextColors.RESET, parse.current.index,
         TextColors.GOLD, "\n"));
        builder.append(Text.of(TextColors.GOLD, "Key: \"", TextColors.RESET, parse.current.key,
        TextColors.GOLD, "\"\n"));
        builder.append(Text.of(TextColors.GOLD, "Prefix: \"", TextColors.RESET, parse.current
        .prefix, TextColors.GOLD, "\"\n"));
        source.sendMessage(builder.build());
        return ImmutableList.of();
    }*/

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxcore.command.debug.test");
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
        return Text.of("test [mystery args]...");
    }

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
        return true;
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {

//        Optional<? extends IOwner> ownerOpt = FoxPath.getOwner(arguments, source);
//
//        source.sendMessage(Text.of(ownerOpt));



        return CommandResult.empty();
    }

    /*@Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this " +
                    "command!"));
            return CommandResult.empty();
        }

        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .flagMapper(MAPPER)
                .parse();

        String worldName = parse.flags.get("world");
        World world = null;

        if (source instanceof Locatable) world = ((Locatable) source).getWorld();
        if (!worldName.isEmpty()) {
            Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
            if (optWorld.isPresent()) {
                world = optWorld.get();
            } else {
                if (world == null)
                    throw new CommandException(Text.of("No world exists with name \"" + worldName + "\"!"));
            }
        }
        if (world == null) throw new CommandException(Text.of("Must specify a world!"));

        if (parse.args.length == 0) return CommandResult.empty();
        int amount;
        try {
            amount = Integer.parseInt(parse.args[0]);
        } catch (NumberFormatException e) {
            return CommandResult.empty();
        }
        int counter = 1;
        String name;
        if (parse.args.length > 1) {
            name = parse.args[1];
        } else name = "region";

        FGManager manager = FGManager.getInstance();

        for (int i = 1; i <= amount; ) {
            String numberName = name + counter;
            if (manager.isWorldRegionNameAvailable(numberName, world)) {
                FGObjectData data = new FGObjectData();
                data.setName(numberName);
                IWorldRegion region = new RectangularRegion(data, new BoundingBox2(new Vector2i(-i, -i), new Vector2i(i, i)));
                manager.addWorldRegion(region, world);
                i++;
            }
            counter++;
        }

        source.sendMessage(Text.of(TextColors.GREEN, "Successfully created " + amount + " regions!"));
        return CommandResult.success();
    }*/


    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        Thread.dumpStack();
        return ImmutableList.of("b", "c", "a", "d");
    }

    /*@Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this
            command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).limit(2)
        .parse();
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD, "\n-----------------------------\n"));
        int count = 0;
        for (String str : parse.args) {
            count++;
            builder.append(Text.of(count + ": " + str + "\n"));
        }
        for (Map.Entry<String, String> entry : parse.flags.entrySet()) {
            builder.append(Text.of(entry.getKey() + " : " + entry.getValue() + "\n"));
        }
        source.sendMessage(builder.build());
        return CommandResult.empty();
    }*/
}
