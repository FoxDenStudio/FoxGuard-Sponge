package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

public class RootGroupElement extends StructuredGroupElement {

    public RootGroupElement(){
        this(ImmutableMap.of());
    }

    public RootGroupElement(Map<String, IPathElement> fixedChildren) {
        super(fixedChildren);
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    public static RootGroupElement getServerRootGroup(){
        Map<String, IPathElement> fixed = new HashMap<>();



        return new RootGroupElement(fixed);
    }



}
