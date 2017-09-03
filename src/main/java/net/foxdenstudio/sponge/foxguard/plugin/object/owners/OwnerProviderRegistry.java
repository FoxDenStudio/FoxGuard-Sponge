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

package net.foxdenstudio.sponge.foxguard.plugin.object.owners;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

/**
 * Created by Fox on 12/22/2016.
 */
public class OwnerProviderRegistry {

    private static final SecureRandom sRandom = new SecureRandom();

    private static OwnerProviderRegistry instance;
    private Set<UUID> inUse = new HashSet<>();
    private List<IOwnerProvider> ownerProviders;

    public OwnerProviderRegistry() {
        ownerProviders = new ArrayList<>();
        ownerProviders.add(UUIDProvider.getInstance());
        ownerProviders.add(new OnlinePlayerProvider());
        ownerProviders.add(new OfflineUserProvider());
    }

    public static OwnerProviderRegistry getInstance() {
        if (instance == null) instance = new OwnerProviderRegistry();
        return instance;
    }

    /**
     * This method checks if a {@link UUID} could potentially be a player {@link UUID}.
     * It does this by checking if it's a v3 or v4 uuid.
     * For our purposes, it really doesn't matter that we're breaking RFC spec,
     * because all we care about is avoiding name conflicts.
     * Beyond that, UUID is just a convenient method of keeping track of owners.
     *
     * @param uuid the {@link UUID} to check
     * @return Whether this is a v3 or v4 {@link UUID}, meaning whether it could be a player {@link UUID}.
     */
    public static boolean isPlayerUUID(UUID uuid) {
        byte a = (byte) (uuid.getMostSignificantBits() >>> 12 & 0xf);
        byte b = (byte) (uuid.getLeastSignificantBits() >>> 60 & 0xf);
        return (a == 0x3 || a == 0x4) && b >= 0x8 && b <= 0xb;
    }

    private static UUID uuidFromBytes(byte[] data) {
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++)
            msb = (msb << 8) | (data[i] & 0xff);
        for (int i = 8; i < 16; i++)
            lsb = (lsb << 8) | (data[i] & 0xff);
        return new UUID(msb, lsb);
    }

    /**
     * Registers a {@link UUID} for use as a virtual player owner.
     * This ensures that this UUID will not be used elsewhere due to collisions.
     *
     * @param uuid the {@link UUID} to register.
     * @return Whether the {@link UUID} was successfully registered. Returns false if it has already been registered.
     */

    public boolean registerUUID(UUID uuid) {
        return !isPlayerUUID(uuid) && inUse.add(uuid);
    }

    public boolean isRegistered(UUID uuid) {
        return inUse.contains(uuid);
    }

    public boolean unregisterUUID(UUID uuid) {
        return inUse.remove(uuid);
    }

    /**
     * Generates a random {@link UUID} that is NOT to RFC spec.
     * The version for this {@link UUID} is zero, and the variant is random.
     * By breaking RFC spec, we guarantee that there will not be a collision.
     *
     * @return
     */

    public UUID generateUUID() {
        byte[] randomBytes = new byte[16];
        UUID uuid;
        do {
            sRandom.nextBytes(randomBytes);
            randomBytes[0] = 0x00;   /* leading zeros */
            randomBytes[6] &= 0x0f;  /* clear version */
            uuid = uuidFromBytes(randomBytes);
        } while (inUse.contains(uuid));

        return uuid;
    }

    /**
     * Generates a {@link UUID} with {@link OwnerProviderRegistry#generateUUID()} and adds it.
     * This is a convenience method.
     *
     * @return the generated {@link UUID}
     */

    public UUID generateAndRegisterUUID() {
        UUID uuid = generateUUID();
        inUse.add(uuid);
        return uuid;
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
                return Optional.of(FGManager.SERVER_UUID);
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
