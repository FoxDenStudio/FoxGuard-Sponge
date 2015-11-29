/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.gravityfox.foxguard.handlers;

import net.gravityfox.foxguard.objects.IOwnable;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;

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
abstract public class OwnableHandlerBase extends HandlerBase implements IOwnable {

    protected List<User> ownerList = new LinkedList<>();

    public OwnableHandlerBase(String name, int priority) {
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
    public List<User> getOwners() {
        return ownerList;
    }

    @Override
    public void setOwners(List<User> owners) {
        this.ownerList = owners;
    }

    @Override
    public Text getDetails(String arguments) {
        TextBuilder builder = Texts.builder();
        builder.append(Texts.of(TextColors.GOLD, "Owners: "));
        for (User p : ownerList) {
            builder.append(Texts.of(TextColors.RESET, p.getName() + " "));
        }
        return builder.build();
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS OWNERS(NAMES VARCHAR(256), USERUUID UUID);" +
                        "DELETE FROM OWNERS");
            }
            try (PreparedStatement insert = conn.prepareStatement("INSERT INTO OWNERS(NAMES, USERUUID) VALUES (?, ?)")) {
                for (User owner : ownerList) {
                    insert.setString(1, owner.getName());
                    insert.setObject(2, owner.getUniqueId());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        }
    }

    public enum UserOperations {
        ADD,
        REMOVE,
        SET
    }
}
