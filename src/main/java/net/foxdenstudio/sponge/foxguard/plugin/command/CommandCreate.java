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
import net.foxdenstudio.sponge.foxcore.plugin.state.FCStateManager;
import net.foxdenstudio.sponge.foxcore.plugin.state.PositionStateField;
import net.foxdenstudio.sponge.foxguard.plugin.FGConfigManager;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.FGFactoryManager;
import net.foxdenstudio.sponge.foxguard.plugin.object.owner.provider.IOwnerProvider;
import net.foxdenstudio.sponge.foxguard.plugin.object.owner.OwnerManager;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandCreate extends FCCommandBase {

    private static final String[] PRIORITY_ALIASES = {"priority", "prio", "p", "order", "level", "rank"};
    //    private static final String[] STATE_ALIASES = {"state", "s", "buffer"};
    private static final String[] OWNER_ALIASES = {"owner", "own", "o"};

    private static final String CHAR_REGEX = "^.*[^0-9a-zA-Z_$\\-].*$";
    private static final String START_REGEX = "^[0-9\\-].*$";

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        } else if (isIn(PRIORITY_ALIASES, key) && !map.containsKey("priority")) {
            map.put("priority", value);
        } /*else if (isIn(STATE_ALIASES, key) && !map.containsKey("state")) {
            map.put("state", value);
        }*/ else if (isIn(OWNER_ALIASES, key) && !map.containsKey("owner")) {
            map.put("owner", value);
        }
        return true;
    };

    @Nonnull
    @Override
    public CommandResult process(@Nonnull CommandSource source, @Nonnull String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).limit(3).flagMapper(MAPPER).parse();

        int num = parse.args.length;

        if (num < 1) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        }

        String category = parse.args[0];
        FGCat fgCat = FGCat.from(category);
        if (fgCat == null) throw new CommandException(Text.of("\"" + category + "\" is not a valid category!"));

        if (num < 2) throw new CommandException(Text.of("Must specify a name!"));
        String name = parse.args[1];

        if (name.isEmpty()) throw new CommandException(Text.of("Name must not be blank!"));

        if (name.matches(CHAR_REGEX))
            throw new CommandException(Text.of("Name must be alphanumeric!"));
        if (name.matches(START_REGEX))
            throw new CommandException(Text.of("Name can't start with a number or hyphen!"));
        if (!FGManager.isNameValid(name))
            throw new CommandException(Text.of("You may not use \"" + name + "\" as a name!"));
        int lengthLimit = FGConfigManager.getInstance().getNameLengthLimit();
        if (lengthLimit > 0 && name.length() > lengthLimit)
            throw new CommandException(Text.of("Name is too long! Max " + lengthLimit + " characters."));

        UUID owner = FGManager.SERVER_OWNER;
        if (parse.flags.containsKey("owner")) {
            String ownerString = parse.flags.get("owner");
            if (ownerString.isEmpty()) {
                if (source instanceof Identifiable) {
                    owner = ((Identifiable) source).getUniqueId();
                }
            } else {
                String[] parts = ownerString.split(":", 2);
                OwnerManager registry = OwnerManager.getInstance();
                Optional<UUID> ownerOpt;
                if (parts.length == 1) {
                    ownerOpt = registry.getUUIDForOwner(null, parts[0]);
                } else if (parts.length == 2) {
                    ownerOpt = registry.getUUIDForOwner(parts[0], parts[1]);
                } else ownerOpt = Optional.empty();
                if (ownerOpt.isPresent()) {
                    owner = ownerOpt.get();
                } else {
                    throw new CommandException(Text.of("\"" + ownerString + "\" is not a valid owner!"));
                }
            }
        }

        World world = null;
        if (fgCat == FGCat.WORLDREGION) {
            if (source instanceof Locatable) world = ((Locatable) source).getWorld();
            String worldName = parse.flags.get("world");
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
        }

        if (!fgCat.isNameAvailable(name, owner, world)) {
            throw new CommandException(Text.of("That name is already in use!"));
        }

        if (num < 3) throw new CommandException(Text.of("Must specify a type!"));
        String type = parse.args[2];

        List<String> typeAliases = fgCat.typeAliases.get();
        if (!isIn(typeAliases.toArray(new String[typeAliases.size()]), type)) {
            throw new CommandException(Text.of("The type \"" + type + "\" is invalid!"));
        }

        String finalBlock = num < 4 ? "" : parse.args[3];
        IGuardObject object;
        try {
            object = fgCat.create(name, type, finalBlock, source);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandException(Text.of("There was an exception creating the " + fgCat.lName + "!", e));
        }
        if (object == null)
            throw new CommandException(Text.of("Failed to construct " + fgCat.lName + " for an unknown reason!"));

        if (object instanceof IHandler) {
            int priority = 0;
            try {
                priority = Integer.parseInt(parse.flags.get("priority"));
                if (priority < Integer.MIN_VALUE / 2 + 1) priority = Integer.MIN_VALUE / 2 + 1;
                else if (priority > Integer.MAX_VALUE / 2) priority = Integer.MAX_VALUE / 2;
            } catch (NumberFormatException ignored) {
            }
            ((IHandler) object).setPriority(priority);
        }

        boolean success = fgCat.add(object, owner, world);
        if (!success)
            throw new CommandException(Text.of("Successfully constructed but failed to add " + fgCat.lName + " for an unknown reason!"));

        if (object instanceof IRegion) {
            FCStateManager.instance().getStateMap().get(source).flush(PositionStateField.ID);
        }

        source.sendMessage(Text.of(TextColors.GREEN, fgCat.uName + " created successfully"));

        StringBuilder logMessage = new StringBuilder();
        logMessage.append(source.getName())
                .append(" created a ")
                .append(fgCat.lName)
                .append(":   Name: ")
                .append(object.getName());

        if (owner != null && !owner.equals(FGManager.SERVER_OWNER)) {
            logMessage.append("   Owner: ").append(OwnerManager.getInstance().getKeyword(owner, null))
                    .append(" (").append(owner).append(")");
        }

        if (object instanceof IWorldRegion) {
            logMessage.append("   World: ").append(((IWorldRegion) object).getWorld().getName());
        }

        FoxGuardMain.instance().getLogger().info(logMessage.toString());

        return CommandResult.empty();
    }


    @Nonnull
    @Override
    public List<String> getSuggestions(@Nonnull CommandSource source,
                                       @Nonnull String arguments,
                                       @Nullable Location<World> targetPosition) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(3)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .leaveFinalAsIs(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0)
                return Stream.of("region", "worldregion", "handler", "controller")
                        .filter(new StartsWithPredicate(parse.current.token))
                        .collect(GuavaCollectors.toImmutableList());
            else {
                FGCat fgCat = FGCat.from(parse.args[0]);
                if (parse.current.index == 1) {
                    if (parse.current.token == null || parse.current.token.isEmpty()) return ImmutableList.of();
                    if (parse.current.token.matches(CHAR_REGEX)) {
                        source.sendMessage(Text.of(TextColors.RED, "Name must be alphanumeric!"));
                        return ImmutableList.of();
                    }
                    if (parse.current.token.matches(START_REGEX)) {
                        source.sendMessage(Text.of(TextColors.RED, "Name can't start with a number!"));
                        return ImmutableList.of();
                    }
                    if (!FGManager.isNameValid(parse.current.token)) {
                        source.sendMessage(Text.of(TextColors.RED, "You may not use \"" + parse.current.token + "\" as a name!"));
                        return ImmutableList.of();
                    }
                    int lengthLimit = FGConfigManager.getInstance().getNameLengthLimit();
                    if (lengthLimit > 0 && parse.current.token.length() > lengthLimit) {
                        source.sendMessage(Text.of(TextColors.RED, "Name is too long!"));
                        return ImmutableList.of();
                    }

                    Tristate available;
                    if (fgCat == FGCat.REGION) {
                        available = Tristate.fromBoolean(FGManager.getInstance().isRegionNameAvailable(parse.current.token));
                    } else if (fgCat == FGCat.WORLDREGION) {
                        String worldName = parse.flags.get("world");
                        World world = null;
                        if (source instanceof Locatable) world = ((Locatable) source).getWorld();
                        if (!worldName.isEmpty()) {
                            Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                            if (optWorld.isPresent()) {
                                world = optWorld.get();
                            }
                        }
                        if (world == null) {
                            available = FGManager.getInstance().isWorldRegionNameAvailable(parse.current.token);
                        } else {
                            available = Tristate.fromBoolean(FGManager.getInstance().isWorldRegionNameAvailable(parse.current.token, world));
                        }
                    } else if (fgCat == FGCat.HANDLER || fgCat == FGCat.CONTROLLER) {
                        available = Tristate.fromBoolean(!FGManager.getInstance().getHandler(parse.current.token).isPresent());
                    } else {
                        return ImmutableList.of();
                    }
                    switch (available) {
                        case TRUE:
                            source.sendMessage(Text.of(TextColors.GREEN, "Name is available!"));
                            break;
                        case FALSE:
                            source.sendMessage(Text.of(TextColors.RED, "Name is already taken!"));
                            break;
                        case UNDEFINED:
                            source.sendMessage(Text.of(TextColors.YELLOW, "Name might be available. Must specify a world to confirm."));

                    }
                } else if (parse.current.index == 2) {
                    if (fgCat == null) return ImmutableList.of();
                    switch (fgCat) {

                        case REGION:
                            return FGFactoryManager.getInstance().getPrimaryRegionTypeAliases().stream()
                                    .filter(new StartsWithPredicate(parse.current.token))
                                    .map(args -> parse.current.prefix + args)
                                    .collect(GuavaCollectors.toImmutableList());

                        case WORLDREGION:
                            return FGFactoryManager.getInstance().getPrimaryWorldRegionTypeAliases().stream()
                                    .filter(new StartsWithPredicate(parse.current.token))
                                    .map(args -> parse.current.prefix + args)
                                    .collect(GuavaCollectors.toImmutableList());

                        case HANDLER:
                            return FGFactoryManager.getInstance().getPrimaryHandlerTypeAliases().stream()
                                    .filter(new StartsWithPredicate(parse.current.token))
                                    .map(args -> parse.current.prefix + args)
                                    .collect(GuavaCollectors.toImmutableList());
                        case CONTROLLER:
                            return FGFactoryManager.getInstance().getPrimaryControllerTypeAliases().stream()
                                    .filter(new StartsWithPredicate(parse.current.token))
                                    .map(args -> parse.current.prefix + args)
                                    .collect(GuavaCollectors.toImmutableList());
                    }
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY))
            return Stream.of("world", "priority", "owner")
                    .filter(new StartsWithPredicate(parse.current.token))
                    .map(args -> parse.current.prefix + args)
                    .collect(GuavaCollectors.toImmutableList());
        else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGVALUE)) {
            if (isIn(WORLD_ALIASES, parse.current.key)) {
                return Sponge.getGame().getServer().getWorlds().stream()
                        .map(World::getName)
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (isIn(OWNER_ALIASES, parse.current.key)) {
                String[] parts = parse.current.token.split(":", 2);
                System.out.println(parts.length );
                if (parts.length == 1) {
                    ImmutableList<String> collect = OwnerManager.getInstance().getProviders().stream()
                            .map(IOwnerProvider::getPrimaryAlias)
                            .filter(string -> string != null && !string.isEmpty())
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                    System.out.println(collect);
                    return collect;

                } else if (parts.length == 2) {
                    Optional<IOwnerProvider> providerOpt = OwnerManager.getInstance().getProvider(parts[0]);
                    if (providerOpt.isPresent()) {
                        IOwnerProvider provider = providerOpt.get();
                        System.out.println(provider.getOwnerKeywords());
                        return provider.getOwnerKeywords().stream()
                                .filter(new StartsWithPredicate(parts[1]))
                                .map(args -> parse.current.prefix + parts[0] + ":" + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.FINAL)) {
            if (isIn(REGIONS_ALIASES, parse.args[0])) {
                return FGFactoryManager.getInstance().regionSuggestions(source, parse.current.token, parse.args[2])
                        .stream()
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (isIn(WORLDREGIONS_ALIASES, parse.args[0])) {
                return FGFactoryManager.getInstance().worldRegionSuggestions(source, parse.current.token, parse.args[2])
                        .stream()
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                return FGFactoryManager.getInstance().handlerSuggestions(source, parse.current.token, parse.args[2])
                        .stream()
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (isIn(CONTROLLERS_ALIASES, parse.args[0])) {
                return FGFactoryManager.getInstance().controllerSuggestions(source, parse.current.token, parse.args[2])
                        .stream()
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(@Nonnull CommandSource source) {
        return source.hasPermission("foxguard.command.modify.objects.create");
    }

    @Nonnull
    @Override
    public Optional<Text> getShortDescription(@Nonnull CommandSource source) {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<Text> getHelp(@Nonnull CommandSource source) {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Text getUsage(@Nonnull CommandSource source) {
        return Text.of("create <region [--w:<world>] | handler> <name> [--priority:<num>] <type> [args...]");
    }

    private enum FGCat {
        REGION(REGIONS_ALIASES, FGFactoryManager.getInstance()::getRegionTypeAliases) {
            @Override
            public boolean isNameAvailable(String name, UUID owner, @Nullable World world) {
                return FGManager.getInstance().isRegionNameAvailable(name, owner);
            }

            @Override
            public IGuardObject create(String name, String type, String arguments, CommandSource source) throws CommandException {
                return FGFactoryManager.getInstance().createRegion(name, type, arguments, source);
            }

            @Override
            public boolean add(IGuardObject object, UUID owner, @Nullable World world) {
                return object instanceof IRegion
                        && FGManager.getInstance().addRegion(((IRegion) object), owner, world);
            }
        },
        WORLDREGION(WORLDREGIONS_ALIASES, FGFactoryManager.getInstance()::getWorldRegionTypeAliases) {
            @Override
            public boolean isNameAvailable(String name, UUID owner, @Nullable World world) {
                if (world == null)
                    return FGManager.getInstance().isWorldRegionNameAvailable(name, owner) == Tristate.TRUE;
                else return FGManager.getInstance().isWorldRegionNameAvailable(name, owner, world);
            }

            @Override
            public IGuardObject create(String name, String type, String arguments, CommandSource source) throws CommandException {
                return FGFactoryManager.getInstance().createWorldRegion(name, type, arguments, source);
            }

            @Override
            public boolean add(IGuardObject object, UUID owner, @Nullable World world) {
                return world != null
                        && object instanceof IWorldRegion
                        && FGManager.getInstance().addWorldRegion(((IWorldRegion) object), owner, world);
            }
        },
        HANDLER(HANDLERS_ALIASES, FGFactoryManager.getInstance()::getHandlerTypeAliases) {
            @Override
            public boolean isNameAvailable(String name, UUID owner, @Nullable World world) {
                return FGManager.getInstance().isHandlerNameAvailable(name, owner);
            }

            @Override
            public IGuardObject create(String name, String type, String arguments, CommandSource source) throws CommandException {
                return FGFactoryManager.getInstance().createHandler(name, type, arguments, source);
            }

            @Override
            public boolean add(IGuardObject object, UUID owner, @Nullable World world) {
                return object instanceof IHandler
                        && FGManager.getInstance().addHandler(((IHandler) object), owner);
            }
        },
        CONTROLLER(CONTROLLERS_ALIASES, FGFactoryManager.getInstance()::getControllerTypeAliases) {
            @Override
            public boolean isNameAvailable(String name, UUID owner, @Nullable World world) {
                return HANDLER.isNameAvailable(name, owner, world);
            }

            @Override
            public IGuardObject create(String name, String type, String arguments, CommandSource source) throws CommandException {
                return FGFactoryManager.getInstance().createController(name, type, arguments, source);
            }

            @Override
            public boolean add(IGuardObject object, UUID owner, @Nullable World world) {
                return HANDLER.add(object, owner, world);
            }
        };

        String[] catAliases;
        Supplier<List<String>> typeAliases;
        String lName = name().toLowerCase();
        String uName = FCCUtil.toCapitalCase(name());

        FGCat(String[] catAliases, Supplier<List<String>> typeAliases) {
            this.catAliases = catAliases;
            this.typeAliases = typeAliases;
        }

        public static FGCat from(String category) {
            for (FGCat cat : values()) {
                if (isIn(cat.catAliases, category)) return cat;
            }
            return null;
        }

        public abstract boolean isNameAvailable(String name, UUID owner, @Nullable World world);

        public abstract IGuardObject create(String name, String type, String arguments, CommandSource source) throws CommandException;

        public abstract boolean add(IGuardObject object, UUID owner, @Nullable World world);
    }
}
