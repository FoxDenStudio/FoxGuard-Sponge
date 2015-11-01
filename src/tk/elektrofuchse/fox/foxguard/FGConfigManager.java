package tk.elektrofuchse.fox.foxguard;

import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.File;
import java.io.IOException;

/**
 * Created by Fox on 11/1/2015.
 */
public class FGConfigManager {

    private static FGConfigManager instance;


    public boolean forceLoading;


    public FGConfigManager() {
        if (instance == null) instance = this;
        load();
    }

    public void load() {
        File configFile = new File(
                FoxGuardMain.getInstance().getConfigDirectory().getPath() + "/foxguard.cfg");
        CommentedConfigurationNode root;
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setFile(configFile).build();
        if (configFile.exists()) {
            try {
                root = loader.load();
            } catch (IOException e) {
                root = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
        } else {
            root = loader.createEmptyNode(ConfigurationOptions.defaults());
        }
        //--------------------------------------------------------------------------------------------------------------

        forceLoading = root.getNode("storage", "forceLoad").getBoolean(false);

        //--------------------------------------------------------------------------------------------------------------
    }

    public void save() {
        File configFile = new File(
                FoxGuardMain.getInstance().getConfigDirectory().getPath() + "/foxguard.cfg");
        CommentedConfigurationNode root;
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setFile(configFile).build();
        if (configFile.exists()) {
            try {
                root = loader.load();
            } catch (IOException e) {
                root = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
        } else {
            root = loader.createEmptyNode(ConfigurationOptions.defaults());
        }
        //--------------------------------------------------------------------------------------------------------------

        root.getNode("storage", "forceLoad").setValue(forceLoading);

        //--------------------------------------------------------------------------------------------------------------
        try {
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static FGConfigManager getInstance() {
        return instance;
    }
}
