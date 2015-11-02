/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */
package tk.elektrofuchse.fox.foxguard.commands;

import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.util.command.*;
import org.spongepowered.api.util.command.dispatcher.Disambiguator;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.commands.util.CallbackHashMap;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public final class FGCommandMainDispatcher extends FGCommandDispatcher {

    private final Map<CommandSource, InternalCommandState> stateMap = new CallbackHashMap<>((o, m) -> {
        if (o instanceof CommandSource) {
            m.put((CommandSource) o, new InternalCommandState());
        }
    });

    private static FGCommandMainDispatcher instance;
    private final PaginationService pageService;

    public FGCommandMainDispatcher() {
        this(FIRST_DISAMBIGUATOR);
        instance = this;
    }

    public FGCommandMainDispatcher(Disambiguator disambiguatorFunc) {
        super(disambiguatorFunc);
        pageService = FoxGuardMain.getInstance().getGame().getServiceManager().provide(PaginationService.class).get();
    }

    public Map<CommandSource, InternalCommandState> getStateMap() {
        return stateMap;
    }

    public static FGCommandMainDispatcher getInstance() {
        return instance;
    }

    public PaginationService getPageService() {
        return pageService;
    }
}
