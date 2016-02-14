package net.foxdenstudio.sponge.foxguard.plugin.controller;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxguard.plugin.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.controller.util.HandlerWrapper;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageController extends ControllerBase {

    private HandlerWrapper slot;
    private Map<String, Text> messages;
    private Map<Flag, Config> configs;


    public MessageController(String name, int priority) {
        super(name, priority);
        slot = HandlerWrapper.PASSTHROUGH;
        messages = new HashMap<>();
        configs = new EnumMap<>(Flag.class);
    }

    @Override
    public EventResult handle(@Nullable User user, Flag flag, Event event) {
        if (configs.containsKey(flag)) {
            Config c = configs.get(flag);
            if (messages.containsKey(c.messageID)) {
                EventResult result = slot.handle(user, flag, event);
                if (c.enabled.get(result.getState())) {
                    return EventResult.of(result.getState(), messages.get(c.messageID));
                } else {
                    return result;
                }
            } else {
                return slot.handle(user, flag, event);
            }
        } else {
            return slot.handle(user, flag, event);
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
    public void configureLinks(DataSource dataSource) {

    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {

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
        if(slot != null && slot.handler == handler) slot = HandlerWrapper.PASSTHROUGH;
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
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
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

    public Map<Flag, Config> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<Flag, Config> configs) {
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
}
