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

package net.foxdenstudio.sponge.foxguard.plugin.object.ownerold;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxguard.plugin.object.ownerold.provider.*;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

/**
 * Created by Fox on 12/22/2016.
 */
public class OwnerManager {

    private static OwnerManager instance;
    private List<IOwnerProvider> ownerProviders;

    public OwnerManager() {
        ownerProviders = new ArrayList<>();
        ownerProviders.add(UUIDProvider.getInstance());
        ownerProviders.add(new OnlinePlayerProvider());
        ownerProviders.add(new OfflineUserProvider());
    }

    public static OwnerManager getInstance() {
        if (instance == null) instance = new OwnerManager();
        return instance;
    }

    public boolean registerProvider(IOwnerProvider provider) {
        return !ownerProviders.contains(provider) && ownerProviders.add(provider);
    }

    public Optional<IOwnerProvider> getProvider(String type) {
        for (IOwnerProvider provider : ownerProviders) {
            if (Aliases.isIn(provider.getAliases(), type)) {
                return Optional.of(provider);
            }
        }
        return Optional.empty();
    }

    public List<IOwnerProvider> getProviders() {
        return ImmutableList.copyOf(this.ownerProviders);
    }

    public Optional<UUID> getUUIDForOwner(@Nullable String providerType, @Nullable String qualifier) {
        if (providerType == null || providerType.isEmpty()) {
            if (qualifier == null || qualifier.isEmpty()) {
                return Optional.empty();//return Optional.of(FGManager.SERVER_OWNER);
            } else {
                for (IOwnerProvider provider : ownerProviders) {
                    Optional<UUID> ownerOpt = provider.getOwnerUUID(qualifier);
                    if (ownerOpt.isPresent()) return ownerOpt;
                }
                return Optional.empty();
            }
        } else {
            Optional<IOwnerProvider> providerOpt = getProvider(providerType);
            return providerOpt.flatMap(provider -> provider.getOwnerUUID(qualifier));
        }
    }

    public String getDisplayName(UUID owner, @Nullable String providerType, @Nullable CommandSource viewer) {
        return getDisplayable(provider -> provider::getDisplayName,
                owner.toString(),
                owner, providerType, viewer);
    }

    public Text getDisplayText(UUID owner, @Nullable String providerType, @Nullable CommandSource viewer) {
        return getDisplayable(provider -> provider::getDisplayText,
                Text.of(owner.toString()),
                owner, providerType, viewer);
    }

    public Text getHoverText(UUID owner, @Nullable String providerType, @Nullable CommandSource viewer) {
        return getDisplayable(provider -> provider::getHoverText,
                Text.of(owner.toString()),
                owner, providerType, viewer);
    }

    public String getKeyword(UUID owner, @Nullable String providerType) {
        Optional<String> keywordOpt = Optional.empty();
        if (providerType == null || providerType.isEmpty()) {
            for (IOwnerProvider provider : ownerProviders) {
                if (provider == UUIDProvider.getInstance()) continue;
                keywordOpt = provider.getKeyword(owner);
                if (keywordOpt.isPresent()) break;
            }
        } else {
            keywordOpt = getProvider(providerType)
                    .flatMap(provider -> provider.getKeyword(owner));
        }
        return keywordOpt.orElse(owner.toString());
    }

    private <R> R getDisplayable(Function<IDisplayableOwnerProvider, BiFunction<UUID, CommandSource, Optional<R>>> function,
                                 R def,
                                 UUID owner,
                                 @Nullable String providerType,
                                 @Nullable CommandSource viewer) {
        Optional<R> returnOpt = Optional.empty();
        if (providerType == null || providerType.isEmpty()) {
            for (IOwnerProvider provider : ownerProviders) {
                if (provider instanceof IDisplayableOwnerProvider) {
                    IDisplayableOwnerProvider displayableProvider = (IDisplayableOwnerProvider) provider;
                    returnOpt = function.apply(displayableProvider).apply(owner, viewer);
                    if (returnOpt.isPresent()) break;
                }
            }
        } else {
            returnOpt = getProvider(providerType)
                    .filter(provider -> provider instanceof IDisplayableOwnerProvider)
                    .map(provider -> (IDisplayableOwnerProvider) provider)
                    .flatMap(provider -> function.apply(provider).apply(owner, viewer));
        }
        return returnOpt.orElse(def);
    }

}
