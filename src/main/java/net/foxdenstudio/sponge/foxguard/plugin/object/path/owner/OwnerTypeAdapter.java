package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.Owner;

public abstract class OwnerTypeAdapter<T extends Owner> extends TypeAdapter<T> {

    public final String group;
    protected final Gson gson;

    protected OwnerTypeAdapter(String group, Gson gson) {
        this.group = group;
        this.gson = gson;
    }
}
