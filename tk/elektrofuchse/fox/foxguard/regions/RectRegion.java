package tk.elektrofuchse.fox.foxguard.regions;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import tk.elektrofuchse.fox.foxguard.commands.util.CommandParseHelper;
import tk.elektrofuchse.fox.foxguard.regions.util.BoundingBox2;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 8/18/2015.
 */
public class RectRegion extends RegionBase implements IOwnable {

    private final BoundingBox2 bb;
    private List<Player> ownerList = new LinkedList<>();

    public RectRegion(String name, BoundingBox2 bb) {
        super(name);
        this.bb = bb;
    }

    public RectRegion(String name, List<Vector3i> positions, String[] args, CommandSource source)
            throws CommandException {
        super(name);
        List<Vector3i> allPositions = new LinkedList<>(positions);
        for (int i = 0; i < args.length - 2; i += 3) {
            int x, y, z;
            try {
                x = CommandParseHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockX() : 0, args[i]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Texts.of("Unable to parse \"" + args[i] + "\"!"), e, args[i], i);
            }
            try {
                y = CommandParseHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockY() : 0, args[i + 1]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Texts.of("Unable to parse \"" + args[i + 1] + "\"!"), e, args[i + 1], i + 1);
            }
            try {
                z = CommandParseHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockZ() : 0, args[i + 2]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Texts.of("Unable to parse \"" + args[i + 2] + "\"!"), e, args[i + 2], i + 2);
            }
            allPositions.add(new Vector3i(x, y, z));
        }
        if (allPositions.isEmpty()) throw new CommandException(Texts.of("No parameters specified!"));
        Vector3i a = new Vector3i(), b = new Vector3i();
        for (Vector3i pos : positions) {
            a = a.min(pos);
            b = b.max(pos);
        }
        this.bb = new BoundingBox2(a, b);
    }

    public RectRegion(String name, List<Vector3i> positions, String[] args, CommandSource source, Player... owners) throws CommandException {
        this(name, positions, args, source);
        Collections.addAll(ownerList, owners);
    }

    public RectRegion(String name, List<Vector3i> positions, String[] args, CommandSource source, List<Player> owners) throws CommandException {
        this(name, positions, args, source);
        this.ownerList = owners;
    }

    @Override
    public boolean isInRegion(int x, int y, int z) {
        return bb.contains(x, z);
    }


    @Override
    public boolean isInRegion(double x, double y, double z) {
        return bb.contains(x, z);
    }

    @Override
    public String getType() {
        return "Rect";
    }

    @Override
    public List<Player> getOwners() {
        return this.ownerList;
    }

    @Override
    public void setOwners(List<Player> owners) {
        this.ownerList = owners;
    }

    @Override
    public boolean addOwner(Player player) {
        if (this.ownerList.contains(player)) return false;
        this.ownerList.add(player);
        return true;
    }

    @Override
    public boolean removeOwner(Player player) {
        if (!this.ownerList.contains(player)) return false;
        this.ownerList.remove(player);
        return true;
    }


}
