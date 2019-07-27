package io.github.prospector.rocks.config;

import io.github.prospector.rocks.Rocks;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;

public class RocksConfigManager {
    private static File file;
    private static RocksConfig config;

    private static void prepareBiomeConfigFile() {
        if (file != null) {
            return;
        }
        file = new File(FabricLoader.getInstance().getConfigDirectory(), Rocks.MOD_ID + ".json");
    }

    public static RocksConfig initializeConfig() {
        if (config != null) {
            return config;
        }

        config = new RocksConfig();
        load();

        return config;
    }

    private static void load() {
        prepareBiomeConfigFile();

        try {
            if (!file.exists()) {
                save();
            }
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));

                config = Rocks.GSON.fromJson(br, RocksConfig.class);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Couldn't load Rocks configuration file; reverting to defaults");
            e.printStackTrace();
        }
    }

    public static void save() {
        prepareBiomeConfigFile();

        String jsonString = Rocks.GSON.toJson(config);

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(jsonString);
        } catch (IOException e) {
            System.err.println("Couldn't save Rocks configuration file");
            e.printStackTrace();
        }
    }

    public static RocksConfig getConfig() {
        return config;
    }
}
