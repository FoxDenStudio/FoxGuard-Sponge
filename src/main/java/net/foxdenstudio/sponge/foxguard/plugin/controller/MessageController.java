package net.foxdenstudio.sponge.foxguard.plugin.controller;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxguard.plugin.IFlag;
import net.foxdenstudio.sponge.foxguard.plugin.controller.util.HandlerWrapper;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IHandlerFactory;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;

public class MessageController extends ControllerBase {

    private HandlerWrapper slot;
    private Map<String, Text> messages;
    private Map<IFlag, Config> configs;


    public MessageController(String name, int priority) {
        super(name, priority);
        slot = HandlerWrapper.PASSTHROUGH;
        messages = new HashMap<>();
        configs = new HashMap<>();
    }

    @Override
    public EventResult handle(@Nullable User user, IFlag flag, Optional<Event> event, Object... extra) {
        if (configs.containsKey(flag)) {
            Config c = configs.get(flag);
            if (messages.containsKey(c.messageID)) {
                EventResult result = slot.handle(user, flag, event, extra);
                if (c.enabled.get(result.getState())) {
                    return EventResult.of(result.getState(), messages.get(c.messageID));
                } else {
                    return result;
                }
            } else {
                return slot.handle(user, flag, event, extra);
            }
        } else {
            return slot.handle(user, flag, event, extra);
        }
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();


        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

    @Override
    public void save(Path directory) {

    }

    @Override
    public void loadLinks(Path directory) {

    }

    @Override
    public int maxLinks() {
        return -1;
    }


    @Override
    public boolean addHandler(IHandler handler) {
        if (this.handlers.size() < 1) {
            slot = new HandlerWrapper(handler);
            return super.addHandler(handler);
        } else return false;
    }

    @Override
    public boolean removeHandler(IHandler handler) {
        if (slot != null && slot.handler == handler) slot = HandlerWrapper.PASSTHROUGH;
        return super.removeHandler(handler);
    }

    @Override
    public void clearHandlers() {
        slot = HandlerWrapper.PASSTHROUGH;
        super.clearHandlers();
    }

    public IHandler getHandler() {
        if (this.handlers.size() == 0) return null;
        else return this.handlers.get(0);
    }

    @Override
    public String getShortTypeName() {
        return "Msg";
    }

    @Override
    public String getLongTypeName() {
        return "Message";
    }

    @Override
    public String getUniqueTypeString() {
        return "message";
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments).parse();

        return ProcessResult.failure();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    public HandlerWrapper getSlot() {
        return slot;
    }

    public void setSlot(HandlerWrapper slot) {
        this.slot = slot;
    }

    public Map<String, Text> getMessages() {
        return messages;
    }

    public void setMessages(Map<String, Text> messages) {
        this.messages = messages;
    }

    public Map<IFlag, Config> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<IFlag, Config> configs) {
        this.configs = configs;
    }

    private class Config {

        public final Map<Tristate, Boolean> enabled;
        public String messageID;


        private Config() {
            enabled = new EnumMap<>(Tristate.class);
        }

        public Config(Tristate state, String messageID) {
            this();
            this.messageID = messageID;
            for (Tristate t : Tristate.values()) {
                if (t == state) {
                    enabled.put(t, true);
                } else {
                    enabled.put(t, false);
                }
            }
        }

        public Config(boolean deny, boolean passthrough, boolean allow, String messageID) {
            this();
            this.messageID = messageID;
            enabled.put(Tristate.FALSE, deny);
            enabled.put(Tristate.UNDEFINED, passthrough);
            enabled.put(Tristate.TRUE, allow);
        }
    }

    public static class Factory implements IHandlerFactory {

        private static final String[] messageAliases = {"message", "mess", "msg"};

        @Override
        public IHandler create(String name, int priority, String arguments, CommandSource source) {
            return null;
        }

        @Override
        public IHandler create(Path directory, String name, int priority, boolean isEnabled) {
            return null;
        }

        @Override
        public String[] getAliases() {
            return messageAliases;
        }

        @Override
        public String getType() {
            return "message";
        }

        @Override
        public String getPrimaryAlias() {
            return "message";
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type) throws CommandException {
            return ImmutableList.of();
        }
    }
}
