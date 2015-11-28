/*
 *
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

package net.gravityfox.foxguard.util;

import org.spongepowered.api.util.Tristate;

/**
 * Created by Fox on 11/26/2015.
 * Project: SpongeForge
 */
public class Aliases {

    public static final String[] REGIONS_ALIASES = {"regions", "region", "reg", "r"};
    public static final String[] FLAG_SETS_ALIASES = {"flagsets", "flagset", "flags", "flag", "f"};
    public static final String[] POSITIONS_ALIASES = {"positions", "position", "points", "point", "locations", "location", "pos", "loc", "locs", "p"};
    public static final String[] OWNER_GROUP_ALIASES = {"owners", "owner", "master", "masters", "creator", "creators",
            "admin", "admins", "administrator", "administrators", "mod", "mods"};
    public static final String[] SET_ALIASES = {"set", "perm", "flag", "rule", "perms", "flags", "rules", "permission", "permissions"};
    public static final String[] PASSIVE_ALIASES = {"passive", "causeless", "userless", "environment"};
    public static final String[] MEMBER_GROUP_ALIASES = {"member", "members", "user", "users", "player", "players"};
    public static final String[] DEFAULT_GROUP_ALIASES = {"default", "nonmember", "nonmembers", "everyone", "other"};
    public static final String[] GROUPS_ALIASES = {"group", "groups"};
    public static final String[] TRUE_ALIASES = {"true", "t", "allow", "a"};
    public static final String[] FALSE_ALIASES = {"false", "f", "deny", "d"};
    public static final String[] PASSTHROUGH_ALIASES = {"passthrough", "pass", "p", "undefined", "undef", "un", "u"};

    public static boolean isAlias(String[] aliases, String input) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) return true;
        }
        return false;
    }

    public static Tristate tristateFrom(String name) {
        if (isAlias(TRUE_ALIASES, name)) {
            return Tristate.TRUE;
        } else if (isAlias(FALSE_ALIASES, name)) {
            return Tristate.FALSE;
        } else if (isAlias(PASSTHROUGH_ALIASES, name)) {
            return Tristate.UNDEFINED;
        } else {
            return null;
        }
    }
}
