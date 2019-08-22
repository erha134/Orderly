package io.github.prospector.orderly.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.prospector.orderly.Orderly;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class OrderlyConfigManager {

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(r -> new Thread(r, "Orderly Config Manager"));
    private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();
    private static OrderlyConfig config;
    private static Path configFile;

    public static OrderlyConfig getConfig() {
        return config != null ? config : init();
    }

    public static OrderlyConfig init() {
        configFile = FabricLoader.getInstance().getConfigDirectory().toPath().resolve(Orderly.MOD_ID + ".json");
        load().thenApplyAsync(c -> config = c, MinecraftClient.getInstance()).join();
        return Objects.requireNonNull(config, "failed to init config");
    }

    public static CompletableFuture<OrderlyConfig> load() {
        return save(true).thenApplyAsync(aVoid -> {
            try(BufferedReader reader = Files.newBufferedReader(configFile)) {
                return GSON.fromJson(reader, OrderlyConfig.class);
            }
            catch (IOException e) {
                Orderly.getLogger().error("unable to read config file", e);
                return new OrderlyConfig();
            }
        }, EXECUTOR);
    }

    public static CompletableFuture<Void> save() {
        return save(true);
    }

    public static CompletableFuture<Void> save(boolean overwrite) {
        final OrderlyConfig toSave = overwrite ? new OrderlyConfig() : config;
        return CompletableFuture.runAsync(() -> {
            try(BufferedWriter writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(toSave, writer);
            }
            catch (IOException e) {
                Orderly.getLogger().error("unable to write config file", e);
            }
        }, EXECUTOR);
    }
}
