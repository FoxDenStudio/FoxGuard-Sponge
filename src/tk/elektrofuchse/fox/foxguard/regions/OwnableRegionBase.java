package tk.elektrofuchse.fox.foxguard.regions;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import tk.elektrofuchse.fox.foxguard.pieces.IOwnable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 10/26/2015.
 * Project: foxguard
 */
abstract public class OwnableRegionBase extends RegionBase implements IOwnable {

    protected List<User> ownerList = new LinkedList<>();

    public OwnableRegionBase(String name) {
        super(name);
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

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS OWNERS(NAMES VARCHAR(256), USERUUID UUID);" +
                    "DELETE FROM OWNERS");
            PreparedStatement insert = conn.prepareStatement("INSERT INTO OWNERS(NAMES, USERUUID) VALUES (?, ?)");
            for(User owner : ownerList){
                insert.setString(1, owner.getName());
                insert.setObject(2, owner.getUniqueId());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }
}
