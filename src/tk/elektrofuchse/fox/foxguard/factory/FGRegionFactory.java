package tk.elektrofuchse.fox.foxguard.factory;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.regions.CuboidRegion;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;
import tk.elektrofuchse.fox.foxguard.regions.RectangularRegion;
import tk.elektrofuchse.fox.foxguard.regions.util.BoundingBox2;
import tk.elektrofuchse.fox.foxguard.regions.util.BoundingBox3;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class FGRegionFactory implements IRegionFactory {

    static final String[] rectAliases = {"square", "rectangular", "rectangle", "rect"};
    static final String[] cuboidAliases = {"box", "cube", "cuboid", "cuboidal", "rectangularprism", "rectangleprism", "rectprism"};
    static final String[] types = {"rectangular", "cuboid"};

    @Override
    public IRegion createRegion(String name, String type, String arguments, InternalCommandState state, World world, CommandSource source) throws CommandException {
        if (FGHelper.contains(rectAliases, type)) {
            if (source instanceof Player)
                return new RectangularRegion(name, state.positions, arguments.split(" "), source, (Player) source);
            else return new RectangularRegion(name, state.positions, arguments.split(" "), source);
        } else if (FGHelper.contains(cuboidAliases, type)) {
            if (source instanceof Player)
                return new CuboidRegion(name, state.positions, arguments.split(" "), source, (Player) source);
            else return new CuboidRegion(name, state.positions, arguments.split(" "), source);
        } else return null;
    }

    @Override
    public IRegion createRegion(DataSource source, String name, String type) throws SQLException {
        if (type.equalsIgnoreCase("rectangular")) {
            Vector2i a;
            Vector2i b;
            List<User> userList = new LinkedList<>();
            try (Connection conn = source.getConnection()) {
                ResultSet boundSet = conn.createStatement().executeQuery("SELECT * FROM BOUNDS");
                ResultSet ownerSet = conn.createStatement().executeQuery("SELECT * FROM OWNERS");
                boundSet.next();
                a = new Vector2i(boundSet.getInt("X"), boundSet.getInt("Z"));
                boundSet.next();
                b = new Vector2i(boundSet.getInt("X"), boundSet.getInt("Z"));
                while (ownerSet.next()) {
                    Optional<User> user = FoxGuardMain.getInstance().getUserStorage().get((UUID) ownerSet.getObject("USERUUID"));
                    if (user.isPresent()) userList.add(user.get());
                }
            }
            RectangularRegion region = new RectangularRegion(name, new BoundingBox2(a, b));
            region.setOwners(userList);
            return region;
        } else if (type.equalsIgnoreCase("cuboid")) {
            Vector3i a;
            Vector3i b;
            List<User> userList = new LinkedList<>();
            try (Connection conn = source.getConnection()) {
                ResultSet boundSet = conn.createStatement().executeQuery("SELECT * FROM BOUNDS");
                ResultSet ownerSet = conn.createStatement().executeQuery("SELECT * FROM OWNERS");
                boundSet.next();
                a = new Vector3i(boundSet.getInt("X"), boundSet.getInt("Y"), boundSet.getInt("Z"));
                boundSet.next();
                b = new Vector3i(boundSet.getInt("X"),boundSet.getInt("Y"), boundSet.getInt("Z"));
                while (ownerSet.next()) {
                    Optional<User> user = FoxGuardMain.getInstance().getUserStorage().get((UUID) ownerSet.getObject("USERUUID"));
                    if (user.isPresent()) userList.add(user.get());
                }
            }
            CuboidRegion region = new CuboidRegion(name, new BoundingBox3(a, b));
            region.setOwners(userList);
            return region;
        } else return null;
    }

    @Override
    public String[] getAliases() {
        return FGHelper.concatAll(rectAliases, cuboidAliases);
    }

    @Override
    public String[] getTypes() {
        return types;
    }
}
