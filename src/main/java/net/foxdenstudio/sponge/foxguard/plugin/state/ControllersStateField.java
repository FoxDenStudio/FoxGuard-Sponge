package net.foxdenstudio.sponge.foxguard.plugin.state;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.state.ListStateFieldBase;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGHelper;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;

import java.util.Iterator;
import java.util.List;

public class ControllersStateField extends ListStateFieldBase<IController> {

    public static final String ID = "controller";

    public ControllersStateField() {
        super("Controllers");
    }

    @Override
    public Text currentState() {
        Text.Builder builder = Text.builder();
        int index = 1;
        for (Iterator<IController> it = this.list.iterator(); it.hasNext(); ) {
            IController controller = it.next();
            builder.append(Text.of(FGHelper.getColorForObject(controller), "  " + (index++) + ": " + controller.getShortTypeName() + " : " + controller.getName()));
            if (it.hasNext()) builder.append(Text.of("\n"));
        }
        return builder.build();
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).limit(1).parseLastFlags(false).parse();
        String newArgs = parse.args.length > 1 ? parse.args[1] : "";
        if (parse.args.length == 0 || parse.args[0].equalsIgnoreCase("add")) {
            return add(source, newArgs);
        } else if (parse.args[0].equalsIgnoreCase("remove")) {
            return remove(source, newArgs);
        }
        return ProcessResult.of(false, Text.of("Not a valid controller state command!"));
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                .arguments(arguments)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0) {
                return ImmutableList.of("add", "remove").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                if (parse.args[0].equals("add")) {
                    return FGManager.getInstance().getHandlerList().stream()
                            .filter(handler -> !this.list.contains(handler))
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (parse.args[0].equals("remove")) {
                    return this.list.stream()
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            }
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    public ProcessResult add(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).parse();

        if (parse.args.length < 1) throw new CommandException(Text.of("Must specify a name!"));
        IController controller = FGManager.getInstance().getController(parse.args[0]);
        if (controller == null)
            throw new ArgumentParseException(Text.of("No controllers with this name!"), parse.args[0], 1);
        if (this.list.contains(controller))
            throw new ArgumentParseException(Text.of("Controller is already in your state buffer!"), parse.args[0], 1);
        this.list.add(controller);

        return ProcessResult.of(true, Text.of("Successfully added controller to your state buffer!"));
    }

    public ProcessResult remove(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).parse();

        if (parse.args.length < 1) throw new CommandException(Text.of("Must specify a name or a number!"));
        if (parse.args.length == 1) {
            IController controller;
            try {
                int index = Integer.parseInt(parse.args[0]);
                controller = this.list.get(index - 1);
            } catch (NumberFormatException e) {
                controller = FGManager.getInstance().getController(parse.args[0]);
            } catch (IndexOutOfBoundsException e) {
                throw new ArgumentParseException(Text.of("Index out of bounds! (1 - " + this.list.size()), parse.args[0], 1);
            }
            if (controller == null)
                throw new ArgumentParseException(Text.of("No controllers with this name!"), parse.args[0], 1);
            if (!this.list.contains(controller))
                throw new ArgumentParseException(Text.of("Controller is not in your state buffer!"), parse.args[0], 1);
            this.list.remove(controller);
            return ProcessResult.of(true, Text.of("Successfully removed controller from your state buffer!"));
        } else {
            int successes = 0, failures = 0;
            for (String arg : parse.args) {
                IController controller;
                try {
                    int index = Integer.parseInt(arg);
                    controller = this.list.get(index - 1);
                } catch (NumberFormatException e) {
                    controller = FGManager.getInstance().getController(arg);
                } catch (IndexOutOfBoundsException e) {
                    failures++;
                    continue;
                }
                if (controller == null) {
                    failures++;
                    continue;
                }
                if (!this.list.contains(controller)) {
                    failures++;
                    continue;
                }
                this.list.remove(controller);
                successes++;
            }
            if (successes > 0) {
                return ProcessResult.of(true, Text.of(TextColors.GREEN, "Successfully removed controllers from your state buffer with "
                        + successes + " successes" + (failures > 0 ? " and " + failures + " failures!" : "!")));
            } else {
                return ProcessResult.of(false, Text.of(failures + " failures while trying to remove controllers from your state buffer. " +
                        "Check that their names or indices are valid."));
            }
        }
    }

    public List<String> removeSuggestions(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                .arguments(arguments)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0)
                return this.list.stream()
                        .map(IFGObject::getName)
                        .filter(new StartsWithPredicate(parse.current.token))
                        .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }
}
