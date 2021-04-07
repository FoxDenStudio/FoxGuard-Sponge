package net.foxdenstudio.foxsuite.foxguard.sponge.pluginold.listener.util;

import net.foxdenstudio.foxsuite.foxguard.sponge.pluginold.flag.Flag;
import net.foxdenstudio.foxsuite.foxguard.sponge.pluginold.flag.FlagRegistry;
import org.spongepowered.api.entity.*;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.explosive.FusedExplosive;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.*;
import org.spongepowered.api.entity.living.complex.ComplexLiving;
import org.spongepowered.api.entity.living.golem.Golem;
import org.spongepowered.api.entity.living.monster.Boss;
import org.spongepowered.api.entity.living.monster.Monster;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.DamagingProjectile;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.ProjectileLauncher;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.entity.vehicle.minecart.Minecart;

import java.util.*;

import static net.foxdenstudio.foxsuite.foxguard.sponge.pluginold.flag.Flags.*;

public class EntityFlagCalculator {

    private static EntityFlagCalculator instance = new EntityFlagCalculator();

    public static EntityFlagCalculator getInstance() {
        return instance;
    }

    private static final FlagRegistry FLAG_REGISTRY = FlagRegistry.getInstance();

    private Map<Set<Class<? extends Entity>>, MiniFlagSet> flagCache = new HashMap<>();
    private Map<Class<? extends Entity>, Set<Flag>> perEntityCache = new HashMap<>();

    public void applyEntityFlags(Collection<Entity> entities, boolean[] flags) {

        Map<Class<? extends Entity>, Entity> types = new HashMap<>();
        for (Entity entity : entities) types.putIfAbsent(entity.getClass(), entity);

        MiniFlagSet mini = flagCache.get(types.keySet());
        if (mini == null) {
            mini = getMiniFlag(types);
            flagCache.put(new HashSet<>(types.keySet()), mini);
        }

        mini.apply(flags);
    }

    private MiniFlagSet getMiniFlag(Map<Class<? extends Entity>, Entity> types) {
        MiniFlagSet mini = new MiniFlagSet();
        Set<Flag> totalFlags = new HashSet<>();
        for (Map.Entry<Class<? extends Entity>, Entity> entry : types.entrySet()) {
            Set<Flag> flagSet = perEntityCache.get(entry.getKey());
            if (flagSet == null) {
                flagSet = getEntityFlagSet(entry.getValue());
                perEntityCache.put(entry.getKey(), flagSet);
            }
            totalFlags.addAll(flagSet);
        }
        for (Flag flag : totalFlags) {
            mini.setFlag(flag.id);
        }
        mini.finish();
        return mini;
    }

    private Set<Flag> getEntityFlagSet(Entity entity) {
        Set<Flag> flags = new HashSet<>();

        if (entity instanceof Item) {
            flags.add(ITEM);
        }
        if (entity instanceof ExperienceOrb) {
            flags.add(XP_ORB);
        }
        if (entity instanceof FallingBlock) {
            flags.add(FALLING_BLOCK);
        }
        if (entity instanceof Hanging) {
            flags.add(HANGING);
        }
        if (entity instanceof Boat) {
            flags.add(BOAT);
        }
        if (entity instanceof Minecart) {
            flags.add(MINECART);
        }
        if (entity instanceof Projectile) {
            flags.add(PROJECTILE);
        }
        if (entity instanceof DamagingProjectile) {
            flags.add(DAMAGING_PROJ);
        }
        if (entity instanceof AreaEffectCloud) {
            flags.add(AOE_CLOUD);
        }

        //--------------------------------------------------------------------------------------------------------------

        if (entity instanceof Hostile) {
            flags.add(HOSTILE);
        }
        if (entity instanceof Explosive) {
            flags.add(EXPLOSIVE);
        }
        if (entity instanceof FusedExplosive) {
            flags.add(FUSED_EXPL);
        }
        if (entity instanceof ProjectileLauncher) {
            flags.add(PROJ_LAUNCHER);
        }

        //--------------------------------------------------------------------------------------------------------------

        if (entity instanceof Living) {
            flags.add(LIVING);
        }
        if (entity instanceof ComplexLiving) {
            flags.add(COMPLEX);
        }
        if (entity instanceof Player) {
            flags.add(PLAYER);
        }
        if (entity instanceof ArmorStand) {
            flags.add(ARMORSTAND);
        }
        if (entity instanceof Agent) {
            flags.add(AGENT);
        }
        if (entity instanceof Ranger) {
            flags.add(RANGER);
        }
        if (entity instanceof Ambient) {
            flags.add(AMBIENT);
        }
        if (entity instanceof Aerial) {
            flags.add(AERIAL);
        }
        if (entity instanceof Creature) {
            flags.add(CREATURE);
        }
        if (entity instanceof Ageable) {
            flags.add(AGEABLE);
        }
        if (entity instanceof Golem) {
            flags.add(GOLEM);
        }
        if (entity instanceof Aquatic) {
            flags.add(AQUATIC);
        }
        if (entity instanceof Monster) {
            flags.add(MONSTER);
        }
        if (entity instanceof Boss) {
            flags.add(BOSS);
        }
        if (entity instanceof Human) {
            flags.add(HUMAN);
        }

        if (flags.contains(AGENT) && !flags.contains(HOSTILE) && !flags.contains(HUMAN)) {
            flags.add(PASSIVE);
        }

        if (flags.isEmpty()) {
            flags.add(OTHER);
        }

        return flags;
    }

    private class MiniFlagSet {
        int[] flags = new int[FlagRegistry.getInstance().getNumFlags()];
        int size = 0;
        boolean finished = false;

        public void finish() {
            flags = Arrays.copyOf(flags, size);
            finished = true;
        }

        public void setFlag(int id) {
            if (finished) return;

            flags[size++] = id;
        }

        /*public void setFlagCheck(int id) {
            if (finished) return;

            for (int flag : flags) {
                if (flag == id) return;
            }

            flags[size++] = id;
        }*/

        public void apply(boolean[] flagset) {
            if (!finished) finish();

            for (int flag : flags) {
                flagset[flag] = true;
            }
        }
    }

    /*public static void main(String[] args) {
        EntityFlagCalculator calculator = EntityFlagCalculator.getInstance();
        FlagRegistry registry = FlagRegistry.getInstance();
        Entity entity = (Entity) Proxy.newProxyInstance(EntityFlagCalculator.class.getClassLoader(),
                new Class<?>[]{Wither.class}, (proxy, method, args1) -> null);
        List<Flag> flags = new ArrayList<>(calculator.getEntityFlagSet(entity));
        Collections.sort(flags);
        for (Flag flag : flags) {
            System.out.println(flag.name);
        }
    }*/
}
