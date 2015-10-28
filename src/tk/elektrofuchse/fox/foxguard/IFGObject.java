package tk.elektrofuchse.fox.foxguard;

import org.spongepowered.api.text.Text;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by Fox on 10/28/2015.
 */
public interface IFGObject {
    String getName();

    void setName(String name);

    String getType();

    String getUniqueType();

    Text getDetails(String arguments);

    void writeToDatabase(DataSource dataSource) throws SQLException;
}
