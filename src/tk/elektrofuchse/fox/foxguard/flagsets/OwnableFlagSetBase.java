/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard.flagsets;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import tk.elektrofuchse.fox.foxguard.pieces.IOwnable;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 10/26/2015.
 * Project: foxguard
 */
abstract public class OwnableFlagSetBase extends FlagSetBase implements IOwnable {

    protected List<User> ownerList = new LinkedList<>();

    public OwnableFlagSetBase(String name, int priority) {
        super(name, priority);
    }

    @Override
    public boolean removeOwner(User user) {
        return ownerList.remove(user);
    }

    @Override
    public boolean addOwner(User user) {
        return ownerList.add(user);
    }

    @Override
    public void setOwners(List<User> owners) {
        this.ownerList = owners;
    }

    @Override
    public List<User> getOwners() {
        return ownerList;
    }

    @Override
    public Text getDetails(String arguments) {
        TextBuilder builder = Texts.builder();
        builder.append(Texts.of(TextColors.GREEN, "Owners: "));
        for (User p : ownerList) {
            builder.append(Texts.of(TextColors.RESET, p.getName() + " "));
        }
        return builder.build();
    }
}
