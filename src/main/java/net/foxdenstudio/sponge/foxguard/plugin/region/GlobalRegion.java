package net.foxdenstudio.sponge.foxguard.plugin.region;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by Fox on 4/2/2016.
 */
public class GlobalRegion extends RegionBase {
    public static final String NAME = "_sglobal";

    public GlobalRegion() {
        super(NAME);
    }

    @Override
    public void setName(String name) {
        this.name = NAME;
    }

    @Override
    public boolean contains(int x, int y, int z, World world) {
        return true;
    }

    @Override
    public boolean contains(double x, double y, double z, World world) {
        return true;
    }

    @Override
    public boolean contains(Vector3i vec, World world) {
        return true;
    }

    @Override
    public boolean contains(Vector3d vec, World world) {
        return true;
    }

    @Override
    public boolean isInChunk(Vector3i chunk, World world) {
        return true;
    }

    @Override
    public String getShortTypeName() {
        return "SGlobal";
    }

    @Override
    public String getLongTypeName() {
        return "SuperGlobal";
    }

    @Override
    public String getUniqueTypeString() {
        return "superglobal";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setIsEnabled(boolean state) {
        this.isEnabled = true;
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        return Text.of("It's global. Nothing to see here. Now move along.");
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

    @Override
    public void save(Path directory) {

    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        return ProcessResult.failure();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }
}
