package io.github.prospector.orderly.config;

import io.github.prospector.orderly.Orderly;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;

public class OrderlyConfigManager {
    private static File file;
    private static OrderlyConfig config;

    private static void prepareBiomeConfigFile() {
        if (file != null) {
            return;
        }
        file = new File(FabricLoader.getInstance().getConfigDirectory(), Orderly.MOD_ID + ".json");
    }

    public static OrderlyConfig initializeConfig() {
        if (config != null) {
            return config;
        }

        config = new OrderlyConfig();
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

                config = Orderly.GSON.fromJson(br, OrderlyConfig.class);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Couldn't load Orderly configuration file; reverting to defaults");
            e.printStackTrace();
        }
    }

    public static void save() {
        prepareBiomeConfigFile();

        String jsonString = Orderly.GSON.toJson(config);

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(jsonString);
        } catch (IOException e) {
            System.err.println("Couldn't save Orderly configuration file");
            e.printStackTrace();
        }
    }

    public static OrderlyConfig getConfig() {
        return config;
    }
}
