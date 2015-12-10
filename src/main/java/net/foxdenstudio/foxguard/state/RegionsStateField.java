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

package net.foxdenstudio.foxguard.state;

import net.foxdenstudio.foxcore.command.util.AdvCmdParse;
import net.foxdenstudio.foxcore.command.util.ProcessResult;
import net.foxdenstudio.foxcore.state.ListStateFieldBase;
import net.foxdenstudio.foxguard.FGManager;
import net.foxdenstudio.foxguard.FoxGuardMain;
import net.foxdenstudio.foxguard.region.IRegion;
import net.foxdenstudio.foxguard.util.FGHelper;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.foxdenstudio.foxcore.util.Aliases.WORLD_ALIASES;
import static net.foxdenstudio.foxcore.util.Aliases.isAlias;

public class RegionsStateField extends ListStateFieldBase<IRegion> {

    public static final String ID = "region";
    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        if (isAlias(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
    };

    public RegionsStateField(String name) {
        super(name);
    }

    @Override
    public Text state() {
        TextBuilder builder = Texts.builder();
        builder.append(Texts.of(TextColors.GREEN, "Regions: "));
        Iterator<IRegion> regionIterator = this.list.iterator();
        int index = 1;
        while (regionIterator.hasNext()) {
            IRegion region = regionIterator.next();
            builder.append(Texts.of(FGHelper.getColorForRegion(region),
                    "\n  " + (index++) + ": " + region.getShortTypeName() + " : " + region.getWorld().getName() + " : " + region.getName()));
        }
        return builder.build();
    }

    @Override
    public ProcessResult add(CommandSource source, String arguments) throws CommandException {
        System.out.println(arguments);
        AdvCmdParse parse = AdvCmdParse.builder().arguments(arguments).flagMapper(MAPPER).build();
        String[] args = parse.getArgs();

        if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
        String worldName = parse.getFlagmap().get("world");
        World world = null;
        if (source instanceof Player) world = ((Player) source).getWorld();
        if (!worldName.isEmpty()) {
            Optional<World> optWorld = FoxGuardMain.instance().game().getServer().getWorld(worldName);
            if (optWorld.isPresent()) {
                world = optWorld.get();
            }
        }
        if (world == null) throw new CommandException(Texts.of("Must specify a world!"));
        IRegion region = FGManager.getInstance().getRegion(world, args[0]);
        if (region == null)
            throw new CommandException(Texts.of("No Regions with the name\"" + args[0] + "\"!"));
        if (this.list.contains(region))
            throw new CommandException(Texts.of("Region is already in your state buffer!"));
        this.list.add(region);

        return ProcessResult.of(true, Texts.of("Successfully added Region to your state buffer!"));
    }

    @Override
    public ProcessResult subtract(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse parse = AdvCmdParse.builder().arguments(arguments).flagMapper(MAPPER).build();
        String[] args = parse.getArgs();

        if (args.length < 1) throw new CommandException(Texts.of("Must specify a name or a number!"));
        String worldName = parse.getFlagmap().get("world");
        World world = null;
        if (source instanceof Player) world = ((Player) source).getWorld();
        if (!worldName.isEmpty()) {
            Optional<World> optWorld = FoxGuardMain.instance().game().getServer().getWorld(worldName);
            if (optWorld.isPresent()) {
                world = optWorld.get();
            }
        }
        IRegion region;
        try {
            int index = Integer.parseInt(args[0]);
            region = this.list.get(index - 1);
        } catch (NumberFormatException e) {
            if (world == null) throw new CommandException(Texts.of("Must specify a world!"));
            region = FGManager.getInstance().getRegion(world, args[0]);
        } catch (IndexOutOfBoundsException e) {
            throw new CommandException(Texts.of("Index " + args[0] + " out of bounds! (1 - "
                    + this.list.size()));
        }
        if (region == null)
            throw new CommandException(Texts.of("No Regions with the name\"" + args[0] + "\"!"));
        if (!this.list.contains(region))
            throw new CommandException(Texts.of("Region is not in your state buffer!"));
        this.list.remove(region);

        return ProcessResult.of(true, Texts.of("Successfully removed Region from your state buffer!"));
    }
}
