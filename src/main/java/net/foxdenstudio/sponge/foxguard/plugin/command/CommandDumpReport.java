package net.foxdenstudio.sponge.foxguard.plugin.command;

import net.foxdenstudio.sponge.foxcore.plugin.command.FCCommandBase;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.misc.IDumpable;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class CommandDumpReport extends FCCommandBase {

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        Logger logger = FoxGuardMain.instance().getLogger();
        Path filePath = Paths.get("fox_report.txt");
        try (PrintStream stream = new PrintStream(Files.newOutputStream(filePath))) {
            FGManager manager = FGManager.getInstance();
            int maxLength = 0;
            stream.println("================================= REGIONS =================================");
            for (IRegion object : manager.getRegions()) {
                maxLength = Math.max(maxLength, object.getName().length());
                stream.print(">>> " + object.getName() + ": ");
                stream.println(object.toString());
                if (object instanceof IDumpable) {
                    ((IDumpable) object).dumpDebug(stream);
                }
            }
            stream.println();

            for (World world : Sponge.getServer().getWorlds()) {
                int worldNameLength = world.getName().length() + 1;
                stream.println("================================= WORLDREGIONS IN \"" + world.getName() + "\" =================================");
                for (IWorldRegion object : manager.getWorldRegions(world)) {
                    maxLength = Math.max(maxLength, object.getName().length() + worldNameLength);
                    stream.print(">>> " + object.getName() + ": ");
                    stream.println(object.toString());
                    if (object instanceof IDumpable) {
                        ((IDumpable) object).dumpDebug(stream);
                    }
                }
                stream.println();
            }

            stream.println("================================= HANDLERS =================================");
            for (IHandler object : manager.getHandlers()) {
                stream.print(">>> " + object.getName() + ": ");
                stream.println(object.toString());
                if (object instanceof IDumpable) {
                    ((IDumpable) object).dumpDebug(stream);
                }
            }
            stream.println();

            stream.println("================================= LINKS =================================");
            String linksFormat = "%" + Integer.toString(maxLength) + "s -> %s";
            for (IRegion region : manager.getRegions()) {
                stream.format(linksFormat, region.getName(), region.getHandlers());
            }

            for (World world : Sponge.getServer().getWorlds()) {
                for (IRegion region : manager.getWorldRegions(world)) {
                    stream.format(linksFormat, world.getName() + ":" + region.getName(), region.getHandlers());
                }
            }


        } catch (IOException e) {
            logger.error("Could not open file \"" + filePath + "\" for writing!", e);
            throw new CommandException(Text.of("Could not dump data to file!"), e);
        }

        return CommandResult.empty();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.dump");
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.of(Text.of("Dumps debug data into fox-report.txt for bug reporting purposes."));
    }
}
