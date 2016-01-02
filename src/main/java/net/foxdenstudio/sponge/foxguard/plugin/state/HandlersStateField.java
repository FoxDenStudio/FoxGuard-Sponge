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

package net.foxdenstudio.sponge.foxguard.plugin.state;

import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.state.ListStateFieldBase;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGHelper;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.text.Text;

import java.util.Iterator;

public class HandlersStateField extends ListStateFieldBase<IHandler> {

    public static final String ID = "handler";

    public HandlersStateField() {
        super("Handlers");
    }

    @Override
    public Text state() {
        Text.Builder builder = Text.builder();
        int index = 1;
        for (Iterator<IHandler> it = this.list.iterator(); it.hasNext(); ) {
            IHandler handler = it.next();
            builder.append(Text.of(FGHelper.getColorForHandler(handler), "  " + (index++) + ": " + handler.getShortTypeName() + " : " + handler.getName()));
            if (it.hasNext()) builder.append(Text.of("\n"));
        }
        return builder.build();
    }

    @Override
    public ProcessResult add(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).parse2();

        if (parse.args.length < 1) throw new CommandException(Text.of("Must specify a name!"));
        IHandler handler = FGManager.getInstance().gethandler(parse.args[0]);
        if (handler == null)
            throw new ArgumentParseException(Text.of("No Handlers with this name!"), parse.args[0], 1);
        if (this.list.contains(handler))
            throw new ArgumentParseException(Text.of("Handler is already in your state buffer!"), parse.args[0], 1);
        this.list.add(handler);

        return ProcessResult.of(true, Text.of("Successfully added Handler to your state buffer!"));
    }

    @Override
    public ProcessResult subtract(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).parse2();

        if (parse.args.length < 1) throw new CommandException(Text.of("Must specify a name or a number!"));
        IHandler handler;
        try {
            int index = Integer.parseInt(parse.args[0]);
            handler = this.list.get(index - 1);
        } catch (NumberFormatException e) {
            handler = FGManager.getInstance().gethandler(parse.args[1]);
        } catch (IndexOutOfBoundsException e) {
            throw new ArgumentParseException(Text.of("Index out of bounds! (1 - " + this.list.size()), parse.args[0], 1);
        }
        if (handler == null)
            throw new ArgumentParseException(Text.of("No Handlers with this name!"), parse.args[0], 1);
        if (!this.list.contains(handler))
            throw new ArgumentParseException(Text.of("Handler is not in your state buffer!"), parse.args[0], 1);
        this.list.remove(handler);

        return ProcessResult.of(true, Text.of("Successfully removed Handler from your state buffer!"));
    }
}
