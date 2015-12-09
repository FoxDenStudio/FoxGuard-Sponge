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

import net.foxdenstudio.foxcore.commands.util.AdvCmdParse;
import net.foxdenstudio.foxcore.commands.util.ProcessResult;
import net.foxdenstudio.foxcore.state.ListStateFieldBase;
import net.foxdenstudio.foxguard.FGManager;
import net.foxdenstudio.foxguard.handlers.IHandler;
import net.foxdenstudio.foxguard.util.FGHelper;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;

import java.util.Iterator;

public class HandlersStateField extends ListStateFieldBase<IHandler> {

    public static final String ID = "handler";

    public HandlersStateField(String name) {
        super(name);
    }

    @Override
    public Text state() {
        TextBuilder builder = Texts.builder();
        builder.append(Texts.of(TextColors.GREEN, "Handlers: "));
        int index = 1;
        for (Iterator<IHandler> it = this.list.iterator(); it.hasNext(); ) {
            IHandler handler = it.next();
            builder.append(Texts.of(FGHelper.getColorForHandler(handler), "  " + (index++) + ": " + handler.getShortTypeName() + " : " + handler.getName()));
            if (it.hasNext()) builder.append(Texts.of("\n"));
        }
        return builder.build();
    }

    @Override
    public ProcessResult add(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse parse = AdvCmdParse.builder().arguments(arguments).build();
        String[] args = parse.getArgs();

        if (args.length < 1) throw new CommandException(Texts.of("Must specify a name!"));
        IHandler handler = FGManager.getInstance().gethandler(args[0]);
        if (handler == null)
            throw new ArgumentParseException(Texts.of("No Handlers with this name!"), args[0], 1);
        if (this.list.contains(handler))
            throw new ArgumentParseException(Texts.of("Handler is already in your state buffer!"), args[0], 1);
        this.list.add(handler);

        return ProcessResult.of(true, Texts.of("Successfully added Handler to your state buffer!"));
    }

    @Override
    public ProcessResult subtract(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse parse = AdvCmdParse.builder().arguments(arguments).build();
        String[] args = parse.getArgs();

        if (args.length < 1) throw new CommandException(Texts.of("Must specify a name or a number!"));
        IHandler handler;
        try {
            int index = Integer.parseInt(args[0]);
            handler = this.list.get(index - 1);
        } catch (NumberFormatException e) {
            handler = FGManager.getInstance().gethandler(args[1]);
        } catch (IndexOutOfBoundsException e) {
            throw new ArgumentParseException(Texts.of("Index out of bounds! (1 - " + this.list.size()), args[0], 1);
        }
        if (handler == null)
            throw new ArgumentParseException(Texts.of("No Handlers with this name!"), args[0], 1);
        if (!this.list.contains(handler))
            throw new ArgumentParseException(Texts.of("Handler is not in your state buffer!"), args[0], 1);
        this.list.remove(handler);

        return ProcessResult.of(true, Texts.of("Successfully removed Handler from your state buffer!"));
    }
}
