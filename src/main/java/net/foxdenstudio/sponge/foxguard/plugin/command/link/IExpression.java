package net.foxdenstudio.sponge.foxguard.plugin.command.link;

import com.google.common.collect.ImmutableSet;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Fox on 4/20/2016.
 */
public interface IExpression {

    Set<IFGObject> getValue();

    Set<LinkEntry> getLinks() throws LinkException;

    class Expression implements IExpression {

        private static final String REGEX = "[>()]|([\"'])(?:\\\\.|[^\\\\>(),])*?\\1|(?:\\\\.|[^\"'\\s>(),])+";

        private List<Set<Expression>> contents;

        public Expression(String expression) throws LinkException {
            if(!checkParentheses(expression)) throw new LinkException();

        }

        @Override
        public Set<IFGObject> getValue() {
            if (contents.size() > 0) {
                Set<IFGObject> set = new HashSet<>();
                for (Expression expression : contents.get(0)) {
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
                    for (Set<Expression> eSet : contents) {
                        for (Expression ex : eSet) {
                            set.addAll(ex.getLinks());
                        }
                    }
                    Set<Expression> from, to = contents.get(0);
                    for (int i = 1; i < contents.size(); i++) {
                        from = to;
                        to = contents.get(i);
                        for (Expression fromEx : from) {
                            for (Expression toEx : to) {
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

        private boolean checkParentheses(String expression){
            Pattern leftPattern = Pattern.compile("\\(");
            Matcher leftMatcher = leftPattern.matcher(expression);
            int leftCount = 0;
            while (leftMatcher.find()){
                leftCount++;
            }
            Pattern rightPattern = Pattern.compile("\\)");
            Matcher rightMatcher = rightPattern.matcher(expression);
            int rightCount = 0;
            while (rightMatcher.find()){
                rightCount++;
            }
            return leftCount == rightCount;
        }

        private static class Stub implements IExpression {

            Set<IFGObject> set;

            public Stub(Set<IFGObject> set) {
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
}