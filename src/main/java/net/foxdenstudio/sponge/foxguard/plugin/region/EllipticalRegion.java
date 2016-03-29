package net.foxdenstudio.sponge.foxguard.plugin.region;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.BoundingBox2;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Created by Fox on 3/28/2016.
 */
public class EllipticalRegion extends RegionBase {

    private final BoundingBox2 boundingBox;
    private final double centerX, centerY, width, height;
    private final double widthSq, heightSq;

    public EllipticalRegion(String name, double centerX, double centerY, double height, double width) {
        super(name);
        this.centerX = centerX;
        this.centerY = centerY;
        this.width = width;
        this.height = height;
        this.widthSq = width * width;
        this.heightSq = height * height;
        this.boundingBox = new BoundingBox2(
                new Vector2i(centerX - width / 2, centerY - height / 2),
                new Vector2i(centerX + width / 2, centerY + height / 2));
    }

    @Override
    public boolean contains(int x, int y, int z) {
        double xo = x + 0.5 - centerX, yo = y + 0.5 - centerY;
        return (xo * xo / widthSq) + (yo * yo / heightSq) <= 1;
    }

    @Override
    public boolean contains(double x, double y, double z) {
        double xo = x - centerX, yo = y - centerY;
        return (xo * xo / widthSq) + (yo * yo / heightSq) <= 1;
    }

    @Override
    public String getShortTypeName() {
        return "Elli2D";
    }

    @Override
    public String getLongTypeName() {
        return "Elliptical";
    }

    @Override
    public String getUniqueTypeString() {
        return "elliptical";
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GREEN, "Center: "));
        builder.append(Text.of(TextColors.RESET, centerX, ", ", centerY, "\n"));
        builder.append(Text.of(TextColors.GREEN, "Width: "));
        builder.append(Text.of(TextColors.RESET, width, "\n"));
        builder.append(Text.of(TextColors.GREEN, "Height: "));
        builder.append(Text.of(TextColors.RESET, height));
        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS BOUNDS(X DOUBLE, Z DOUBLE);" +
                        "DELETE FROM BOUNDS;" +
                        "INSERT INTO BOUNDS(X, Z) VALUES (" + boundingBox.a.getX() + ", " + boundingBox.a.getY() + ");" +
                        "INSERT INTO BOUNDS(X, Z) VALUES (" + boundingBox.b.getX() + ", " + boundingBox.b.getY() + ");");
            }
        }
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        return ProcessResult.failure();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean isInChunk(Vector3i chunk) {
        final Vector2i a = chunk.mul(16).toVector2(true), b = a.add(16, 16), c = this.boundingBox.a, d = this.boundingBox.b;
        return !(a.getX() > d.getX() || b.getX() < c.getX() || a.getY() > d.getY() || b.getY() < c.getY());
    }
}
