/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard.flagsets;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.IFGObject;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flagsets.util.ActiveFlags;
import tk.elektrofuchse.fox.foxguard.flagsets.util.PassiveFlags;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public interface IFlagSet extends IFGObject, Comparable<IFlagSet> {

    Tristate hasPermission(Player player, ActiveFlags flag);

    Tristate isFlagAllowed(PassiveFlags flag);

    boolean isEnabled();

    void setIsEnabled(boolean state);

    int getPriority();

    void setPriority(int priority);

}
