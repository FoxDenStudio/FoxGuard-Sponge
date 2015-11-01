package tk.elektrofuchse.fox.foxguard.regions;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.regions.util.BoundingBox3;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by Fox on 8/18/2015.
 * Project: foxguard
 */
public class CuboidRegion extends OwnableRegionBase {

    private final BoundingBox3 boundingBox;


    public CuboidRegion(String name, BoundingBox3 boundingBox) {
        super(name);
        this.boundingBox = boundingBox;
    }

    public CuboidRegion(String name, List<Vector3i> positions, String[] args, CommandSource source)
            throws CommandException {
        super(name);
        List<Vector3i> allPositions = new LinkedList<>(positions);
        for (int i = 0; i < args.length - 2; i += 3) {
            int x, y, z;
            try {
                x = FGHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockX() : 0, args[i]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Texts.of("Unable to parse \"" + args[i] + "\"!"), e, args[i], i);
            }
            try {
                y = FGHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockY() : 0, args[i + 1]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Texts.of("Unable to parse \"" + args[i + 1] + "\"!"), e, args[i + 1], i + 1);
            }
            try {
                z = FGHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockZ() : 0, args[i + 2]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Texts.of("Unable to parse \"" + args[i + 2] + "\"!"), e, args[i + 2], i + 2);
            }
            allPositions.add(new Vector3i(x, y, z));
        }
        if (allPositions.isEmpty()) throw new CommandException(Texts.of("No parameters specified!"));
        Vector3i a = allPositions.get(0), b = allPositions.get(0);
        for (Vector3i pos : allPositions) {
            a = a.min(pos);
            b = b.max(pos);
        }
        this.boundingBox = new BoundingBox3(a, b);
    }

    public CuboidRegion(String name, List<Vector3i> positions, String[] args, CommandSource source, User... owners) throws CommandException {
        this(name, positions, args, source);
        Collections.addAll(ownerList, owners);
    }

    public CuboidRegion(String name, List<Vector3i> positions, String[] args, CommandSource source, List<User> owners) throws CommandException {
        this(name, positions, args, source);
        this.ownerList = owners;
    }

    @Override
    public boolean modify(String arguments, InternalCommandState state, CommandSource source) {
        return false;
    }

    @Override
    public boolean isInRegion(int x, int y, int z) {
        return boundingBox.contains(x, y, z);
    }


    @Override
    public boolean isInRegion(double x, double y, double z) {
        return boundingBox.contains(x, y, z);
    }

    @Override
    public String getType() {
        return "Cuboid";
    }

    @Override
    public String getUniqueType() {
        return "cuboid";
    }


    @Override
    public Text getDetails(String arguments) {
        TextBuilder builder = Texts.builder();
        builder.append(super.getDetails(arguments));
        builder.append(Texts.of(TextColors.GREEN, "\nBounds: "));
        builder.append(Texts.of(TextColors.RESET, boundingBox.toString()));
        return builder.build();
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        super.writeToDatabase(dataSource);
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS BOUNDS(X INTEGER, Y INTEGER, Z INTEGER);" +
                    "DELETE FROM BOUNDS;" +
                    "INSERT INTO BOUNDS(X, Y, Z) VALUES (" + boundingBox.a.getX() + ", " + boundingBox.a.getY() + ", " + boundingBox.a.getZ() + ");" +
                    "INSERT INTO BOUNDS(X, Y, Z) VALUES (" + boundingBox.b.getX() + ", " + boundingBox.b.getY() + ", " + boundingBox.b.getZ() + ");");
        }
    }

    @Override
    public String toString() {
        return this.boundingBox.toString();
    }
}
