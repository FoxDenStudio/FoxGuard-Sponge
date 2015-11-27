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

/**
 * Created by Fox on 11/26/2015.
 * Project: SpongeForge
 */
public class Aliases {

    public static final String[] regionsAliases = {"regions", "region", "reg", "r"};
    public static final String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};
    public static final String[] positionsAliases = {"positions", "position", "points", "point", "locations", "location", "pos", "loc", "locs", "p"};
    public static final String[] ownerAliases = {"owners", "owner", "master", "masters", "creator", "creators",
            "admin", "admins", "administrator", "administrators", "mod", "mods"};
    public static final String[] permissionAliases = {"permissions", "permission", "perms", "perm", "flags", "flag"};
    public static final String[] passiveAliases = {"passive", "causeless", "userless", "environment"};
    public static final String[] memberAliases = {"member", "members", "user", "users", "player", "players"};
    public static final String[] defaultAliases = {"default", "nonmember", "nonmembers", "everyone", "other"};
    public static final String[] groupsAliases = {"group", "groups"};
    public static final String[] activeflagsAliases = {"activeflags", "active"};
    public static final String[] trueAliases = {"true", "t", "allow", "a"};
    public static final String[] falseAliases = {"false", "f", "deny", "d"};
    public static final String[] passthroughAliases = {"passthrough", "pass", "p", "undefined", "undef", "un", "u"};
    public static final String[] setAliases = {"set"};

    public static boolean isAlias(String[] aliases, String input) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) return true;
        }
        return false;
    }
}
