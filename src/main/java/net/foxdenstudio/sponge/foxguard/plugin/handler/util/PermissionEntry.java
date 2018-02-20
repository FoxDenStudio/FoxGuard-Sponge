/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) gravityfox - https://gravityfox.net/
 * Copyright (c) contributors
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

package net.foxdenstudio.sponge.foxguard.plugin.handler.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.flag.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagRegistry;
import org.slf4j.Logger;
import org.spongepowered.api.util.Tristate;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Fox on 11/15/2016.
 */
public class PermissionEntry extends Entry {

    public static final TypeAdapter<PermissionEntry> ADAPTER = new PermissionEntryAdapter().nullSafe();
    public String permission;
    public boolean relative;

    public PermissionEntry(Set<Flag> set, String permission) {
        super(set);
        this.permission = permission.toLowerCase();
    }

    public PermissionEntry(String permission, Flag... flags) {
        super(flags);
        this.permission = permission.toLowerCase();
    }

    public static PermissionEntry deserialize(String string) {
        FlagRegistry registry = FlagRegistry.getInstance();
        String[] parts = string.split(":");
        String[] flags = parts[0].split(",", 2);
        Set<Flag> flagSet = new HashSet<>();
        for (String flagName : flags) {
            Optional<Flag> flagOptional = registry.getFlag(flagName);
            if (flagOptional.isPresent()) {
                flagSet.add(flagOptional.get());
            }
        }
        return new PermissionEntry(flagSet, parts[1]);
    }

    @Override
    public String serializeValue() {
        return permission;
    }

    @SuppressWarnings("Duplicates")
    public static class PermissionEntryAdapter extends TypeAdapter<PermissionEntry> {

        private PermissionEntryAdapter() {
        }

        @Override
        public void write(JsonWriter out, PermissionEntry value) throws IOException {
            out.beginObject();
            out.name("set");
            out.beginArray();
            for (Flag flag : value.set) {
                out.value(flag.getName());
            }
            out.endArray();
            out.name("value");
            out.value(value.serializeValue());
            out.endObject();
        }

        @Override
        public PermissionEntry read(JsonReader in) throws IOException {
            FlagRegistry registry = FlagRegistry.getInstance();
            Logger logger = FoxGuardMain.instance().getLogger();

            in.beginObject();
            Set<Flag> set = new HashSet<>();
            String permission = null;
            while (in.peek() != JsonToken.END_OBJECT) {
                String name = in.nextName();
                switch (name) {
                    case "set": {
                        if (in.peek() == JsonToken.NULL) {
                            in.nextNull();
                            break;
                        }
                        in.beginArray();
                        while (in.peek() != JsonToken.END_ARRAY) {
                            if(in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                continue;
                            }
                            Optional<Flag> flagOptional = registry.getFlag(in.nextString());
                            flagOptional.ifPresent(set::add);
                        }
                        in.endArray();
                    }
                    break;
                    case "value": {
                        if (in.peek() == JsonToken.NULL) {
                            in.nextNull();
                            break;
                        }
                        permission = in.nextString();
                    }
                    break;
                }
            }
            if (set.isEmpty()) {
                // TODO add custom exception for stacktrace
                logger.error("Tried to deserialize a TristateEntry with an empty flag set!", new RuntimeException("Error deserializing TristateEntry"));
                return null;
            }
            if (permission == null) {
                logger.warn("Deserialized a TristateEntry with a null tristate! Replacing with UNDEFINED.");
                permission = "undefined";
            }

            return new PermissionEntry(set, permission);
        }
    }
}
