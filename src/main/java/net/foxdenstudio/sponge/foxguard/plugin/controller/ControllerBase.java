package net.foxdenstudio.sponge.foxguard.plugin.controller;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.HandlerBase;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;

import java.util.ArrayList;
import java.util.List;

public abstract class ControllerBase extends HandlerBase implements IController {

    protected final List<IHandler> handlers;

    ControllerBase(String name, int priority) {
        super(name, priority);
        this.handlers = new ArrayList<>();
    }

    @Override
    public List<IHandler> getHandlers() {
        return ImmutableList.copyOf(this.handlers);
    }

    @Override
    public boolean addHandler(IHandler handler) {
        if (!FGManager.getInstance().isRegistered(handler)) {
            return false;
        }
        return this.handlers.add(handler);
    }

    @Override
    public boolean removeHandler(IHandler handler) {
        return this.handlers.remove(handler);
    }

    @Override
    public void clearHandlers() {
        this.handlers.clear();
    }

}
