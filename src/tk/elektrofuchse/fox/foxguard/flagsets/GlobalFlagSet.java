/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard.flagsets;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flagsets.util.ActiveFlags;
import tk.elektrofuchse.fox.foxguard.flagsets.util.PassiveFlags;

import javax.sql.DataSource;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public class GlobalFlagSet extends FlagSetBase {

    public static final String NAME = "_global";

    public GlobalFlagSet() {
        super(NAME, Integer.MIN_VALUE);
    }

    @Override
    public void setPriority(int priority) {
        this.priority = Integer.MIN_VALUE;
    }

    @Override
    public void setName(String name) {
        this.name = NAME;
    }

    @Override
    public String getType() {
        return "Global";
    }

    @Override
    public String getUniqueType() {
        return "global";
    }

    @Override
    public Text getDetails(String arguments) {
        return Texts.of("It's global. Nothing to see here. Now move along.");
    }

    @Override
    public void writeToDatabase(DataSource dataSource) {

    }

    @Override
    public boolean modify(String arguments, InternalCommandState state, CommandSource source) {
        return false;
    }

    @Override
    public Tristate hasPermission(Player player, ActiveFlags flag) {
        return Tristate.TRUE;
    }

    @Override
    public Tristate isFlagAllowed(PassiveFlags flag) {
        return Tristate.TRUE;
    }
}
