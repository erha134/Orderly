package io.github.prospector.orderly.config;

import io.github.prospector.orderly.Orderly;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class OrderlyConfigManager {
    private static final Executor IO_HANDLER = Executors.newSingleThreadExecutor(r -> new Thread(r, "Orderly Config IO Handler"));

    private static File file;
    private static OrderlyConfig config;

    public static CompletableFuture<Void> initializeConfig() {
        return load().thenAccept(c -> config = c);
    }

    private static CompletableFuture<OrderlyConfig> load() {
        final File file = prepareBiomeConfigFile();
        if(!file.exists()) {
            save();
            return CompletableFuture.completedFuture(new OrderlyConfig());
        }
        return CompletableFuture.supplyAsync(() -> {
            OrderlyConfig config;
            try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                config = Orderly.GSON.fromJson(br, OrderlyConfig.class);
            }
            catch (IOException e) {
                config = new OrderlyConfig();
                System.err.println("Couldn't load Orderly configuration file; reverting to defaults");
                e.printStackTrace();
                try {
                    FileUtils.forceDelete(file);
                }
                catch (IOException ex) {
                    throw new RuntimeException("Unable to fall back to defaults", ex);
                }
                save();
            }
            return config;
        }, IO_HANDLER);
    }

    private static File prepareBiomeConfigFile() {
        if(file != null) {
            return file;
        }
        file = new File(FabricLoader.getInstance().getConfigDirectory(), Orderly.MOD_ID + ".json");
        return file;
    }

    public static CompletableFuture<Void> save() {
        final File file = prepareBiomeConfigFile();
        final OrderlyConfig configObj = config; //avoid thread desync
        return CompletableFuture.runAsync(() -> {
            String jsonString = Orderly.GSON.toJson(configObj);
            try(FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(jsonString);
            }
            catch (IOException e) {
                System.err.println("Couldn't save Orderly configuration file");
                e.printStackTrace();
            }
        }, IO_HANDLER);
    }

    public static OrderlyConfig getConfig() {
        if(config == null) {
            try {
                return config = load().get();
            }
            catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("unable to load orderly config", e);
            }
        }
        return config;
    }
}
