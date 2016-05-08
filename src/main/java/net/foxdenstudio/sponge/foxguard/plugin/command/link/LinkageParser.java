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

package net.foxdenstudio.sponge.foxguard.plugin.command.link;

import com.google.common.collect.ImmutableSet;
import net.foxdenstudio.sponge.foxguard.plugin.command.link.exception.LinkException;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Fox on 4/19/2016.
 */
public class LinkageParser {

    private World currentWorld;

    public static Set<LinkEntry> parseLinkageExpression(String expression, CommandSource source) throws LinkException {

        return null;
    }

    public LinkageParser(String expression, CommandSource source) {

    }

    /**
     * Created by Fox on 4/30/2016.
     */
    public class Expression implements IExpression {

        private static final String REGEX = "[>()]|([\"'])(?:\\\\.|[^\\\\>(),])*?\\1|(?:\\\\.|[^\"'\\s>(),])+";

        private List<Set<IExpression>> contents;

        public Expression(String expression) throws LinkException {
            this(expression, null);
        }

        public Expression(String expression, World world) throws LinkException {
            if (!checkParentheses(expression)) throw new LinkException();
            Pattern pattern = Pattern.compile(REGEX);
            Matcher matcher = pattern.matcher(expression);
            Set<IExpression> set = new HashSet<>();
            while (true){

            }
        }

        @Override
        public Set<IFGObject> getValue() {
            if (contents.size() > 0) {
                Set<IFGObject> set = new HashSet<>();
                for (IExpression expression : contents.get(0)) {
                    set.addAll(expression.getValue());
                }
                return set;
            } else return ImmutableSet.of();
        }

        @Override
        public Set<LinkEntry> getLinks() throws LinkException {
            if (contents.size() > 0) {
                Set<LinkEntry> set = new HashSet<>();
                if (contents.size() > 1) {
                    for (Set<IExpression> eSet : contents) {
                        for (IExpression ex : eSet) {
                            set.addAll(ex.getLinks());
                        }
                    }
                    Set<IExpression> from, to = contents.get(0);
                    for (int i = 1; i < contents.size(); i++) {
                        from = to;
                        to = contents.get(i);
                        for (IExpression fromEx : from) {
                            for (IExpression toEx : to) {
                                for (IFGObject fromObj : fromEx.getValue()) {
                                    if (fromObj instanceof ILinkable) {
                                        for (IFGObject toObj : toEx.getValue()) {
                                            if (toObj instanceof IHandler) {
                                                set.add(new LinkEntry((ILinkable) fromObj, (IHandler) toObj));
                                            } else throw new LinkException();
                                        }
                                    } else throw new LinkException();
                                }
                            }
                        }
                    }
                    return set;
                } else {
                    for (IExpression e : contents.get(0)) {
                        set.addAll(e.getLinks());
                    }
                    return set;
                }
            } else return ImmutableSet.of();
        }

        private boolean checkParentheses(String expression) {
            Pattern leftPattern = Pattern.compile("\\(");
            Matcher leftMatcher = leftPattern.matcher(expression);
            int leftCount = 0;
            while (leftMatcher.find()) {
                leftCount++;
            }
            Pattern rightPattern = Pattern.compile("\\)");
            Matcher rightMatcher = rightPattern.matcher(expression);
            int rightCount = 0;
            while (rightMatcher.find()) {
                rightCount++;
            }
            return leftCount == rightCount;
        }

    }

    /**
     * Created by Fox on 4/30/2016.
     */
    public class ExpressionStub implements IExpression {

        Set<IFGObject> set;

        public ExpressionStub(Set<IFGObject> set) {
            this.set = set;
        }

        public Set<IFGObject> getValue() {
            return set;
        }

        @Override
        public Set<LinkEntry> getLinks() {
            return ImmutableSet.of();
        }

    }
}
