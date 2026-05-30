package com.crpg.ebb.data;

import com.crpg.ebb.EbbMod;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class JsonDataRegistry {
    private final String displayName;
    private final String directory;
    private final Identifier reloadListenerId;
    private final List<Consumer<JsonDataRegistry>> reloadObservers = new CopyOnWriteArrayList<>();
    private volatile Map<Identifier, JsonObject> entries = Map.of();
    private volatile List<String> validationMessages = List.of();

    public JsonDataRegistry(String displayName, String directory) {
        this.displayName = displayName;
        this.directory = directory;
        this.reloadListenerId = EbbMod.id("reload/" + directory.replace('/', '_'));
    }

    public String displayName() {
        return displayName;
    }

    public String directory() {
        return directory;
    }

    public Identifier reloadListenerId() {
        return reloadListenerId;
    }

    public int size() {
        return entries.size();
    }

    public Set<Identifier> ids() {
        return entries.keySet();
    }

    public Map<Identifier, JsonObject> entries() {
        return entries;
    }

    public List<String> validationMessages() {
        return validationMessages;
    }

    public void addReloadObserver(Consumer<JsonDataRegistry> observer) {
        reloadObservers.add(observer);
    }

    public Listener createReloadListener() {
        return new Listener(this);
    }

    private void apply(LoadResult result) {
        this.entries = Collections.unmodifiableMap(result.entries());
        this.validationMessages = List.copyOf(result.messages());
        EbbMod.LOGGER.info("Loaded {} {} JSON definition(s) from data/*/{} with {} message(s).",
                entries.size(), displayName, directory, validationMessages.size());
        for (String message : validationMessages) {
            EbbMod.LOGGER.warn("{} reload: {}", displayName, message);
        }
        for (Consumer<JsonDataRegistry> observer : reloadObservers) {
            observer.accept(this);
        }
    }

    public static final class Listener extends SimplePreparableReloadListener<LoadResult> {
        private final JsonDataRegistry registry;
        private final FileToIdConverter converter;

        private Listener(JsonDataRegistry registry) {
            this.registry = registry;
            this.converter = FileToIdConverter.json(registry.directory);
        }

        @Override
        protected LoadResult prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<Identifier, JsonObject> preparedEntries = new LinkedHashMap<>();
            List<String> messages = new ArrayList<>();
            Map<Identifier, Resource> resources = converter.listMatchingResources(resourceManager);

            for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                Identifier fileId = entry.getKey();
                Identifier dataId = converter.fileToId(fileId);
                try (BufferedReader reader = entry.getValue().openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);
                    JsonObject previous = preparedEntries.put(dataId, json);
                    if (previous != null) {
                        messages.add("Duplicate definition id " + dataId + " while reading " + fileId);
                    }
                } catch (JsonParseException | IllegalArgumentException ex) {
                    messages.add("Invalid JSON in " + fileId + ": " + ex.getMessage());
                } catch (IOException ex) {
                    messages.add("Could not read " + fileId + ": " + ex.getMessage());
                }
            }

            return new LoadResult(new LinkedHashMap<>(preparedEntries), List.copyOf(messages));
        }

        @Override
        protected void apply(LoadResult result, ResourceManager resourceManager, ProfilerFiller profiler) {
            registry.apply(result);
        }

        @Override
        public String getName() {
            return EbbMod.MOD_ID + "/" + registry.directory;
        }
    }

    private record LoadResult(Map<Identifier, JsonObject> entries, List<String> messages) {
    }
}
