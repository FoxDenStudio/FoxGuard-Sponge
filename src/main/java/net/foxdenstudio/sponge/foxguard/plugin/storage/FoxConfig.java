package net.foxdenstudio.sponge.foxguard.plugin.storage;

public class FoxConfig<T> extends FoxConfigStub{
    public T data;

    public FoxConfig() {
    }

    public FoxConfig(T data, Integer version) {
        super(version);
        this.data = data;
        this.hash = data.hashCode();
    }
}
