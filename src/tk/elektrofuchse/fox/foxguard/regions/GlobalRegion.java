/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard.regions;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;

import javax.sql.DataSource;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public class GlobalRegion extends RegionBase {

    public static final String NAME = "_global";

    public GlobalRegion() {
        super(NAME);
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

    public boolean isInRegion(int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isInRegion(Vector3i vec) {
        return true;
    }

    @Override
    public boolean isInRegion(double x, double y, double z) {
        return true;
    }

    @Override
    public boolean isInRegion(Vector3d vec) {
        return true;
    }

}
