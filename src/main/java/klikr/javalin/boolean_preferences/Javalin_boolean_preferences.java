package klikr.javalin.boolean_preferences;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import javafx.application.Application;
import klikr.javalin.Javalin_common;
import klikr.look.my_i18n.My_I18n;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class Javalin_boolean_preferences {
    private static final boolean dbg = true;
    private static Javalin_boolean_preferences instance = null;
    private final Application application;
    private final Logger logger;
    private Javalin javalin;
    private final int port_number;
    private final Set<WsContext> connected_clients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static class PreferenceItem {
        final String id;
        final String label;
        final String explanation;
        final boolean value;
        final String category;

        PreferenceItem(String id, String label, String explanation, boolean value, String category) {
            this.id = id;
            this.label = label;
            this.explanation = explanation;
            this.value = value;
            this.category = category;
        }

        public String toJson() {
            return String.format("{\"id\":\"%s\",\"label\":\"%s\",\"explanation\":\"%s\",\"value\":%b,\"category\":\"%s\"}",
                    escapeJson(id),
                    escapeJson(label),
                    escapeJson(explanation),
                    value,
                    escapeJson(category));
        }
    }

    public static void show(Application application, Logger logger) {
        init(application, logger);
        Javalin_common.open_browser(application, false, "Boolean Preferences", instance.port_number, logger);
    }

    private static void init(Application application, Logger logger) {
        synchronized (Javalin_boolean_preferences.class) {
            if (instance == null) {
                instance = new Javalin_boolean_preferences(application, logger);
                instance.start_javalin_server();
            }
        }
    }

    private Javalin_boolean_preferences(Application application, Logger logger) {
        this.application = application;
        this.logger = logger;
        this.port_number = Javalin_common.find_free_port(logger);
    }

    private void start_javalin_server() {
        CountDownLatch started = new CountDownLatch(1);
        Runnable r = () -> {
            logger.log("Creating Javalin server on port " + port_number);
            javalin = Javalin.create(config -> {
                config.staticFiles.add("/javalin_boolean_preferences",
                        io.javalin.http.staticfiles.Location.CLASSPATH);
            }).start(port_number);
            logger.log("Javalin server created and started");

            logger.log("Registering WebSocket handler at /preferences-ws");
            javalin.ws("/preferences-ws", ws -> {
                ws.onConnect(ctx -> {
                    connected_clients.add(ctx);
                    ctx.session.setIdleTimeout(java.time.Duration.ofMillis(3600000L));
                    logger.log("Boolean preferences WebSocket connected, clients: " + connected_clients.size());
                });

                ws.onMessage(ctx -> {
                    String msg = ctx.message();
                    logger.log("Received WebSocket message: " + msg);

                    if ("REQUEST_INIT".equals(msg)) {
                        try {
                            logger.log("Starting get_all_preferences()");
                            List<PreferenceItem> items = get_all_preferences();
                            logger.log("Got " + items.size() + " items from get_all_preferences()");

                            if (items.isEmpty()) {
                                logger.log("WARNING: items list is EMPTY!");
                                ctx.send("[]");
                                return;
                            }

                            List<String> jsonItems = new ArrayList<>();
                            for (PreferenceItem item : items) {
                                String json = item.toJson();
                                jsonItems.add(json);
                            }
                            String response = String.format("[%s]", String.join(",", jsonItems));
                            logger.log("Sending " + jsonItems.size() + " preference items, response length: " + response.length());
                            logger.log("First 200 chars: " + response.substring(0, Math.min(200, response.length())));
                            ctx.send(response);
                        } catch (Exception e) {
                            logger.log("ERROR in REQUEST_INIT: " + e.getMessage());
                            e.printStackTrace();
                            ctx.send("[]");
                        }
                        return;
                    }

                    if (msg.startsWith("TOGGLE:")) {
                        String featureName = msg.substring("TOGGLE:".length());
                        try {
                            Feature feature = Feature.valueOf(featureName);
                            boolean currentValue = Feature_cache.get(feature);
                            boolean newValue = !currentValue;
                            Feature_cache.update_cached_boolean(feature, newValue, null);
                            logger.log("Toggled " + featureName + " from " + currentValue + " to " + newValue);

                            // Broadcast update to all connected clients
                            broadcast_update(featureName, newValue);
                        } catch (IllegalArgumentException e) {
                            logger.log("Invalid feature name: " + featureName);
                        }
                    }
                });

                ws.onClose(ctx -> {
                    connected_clients.remove(ctx);
                    if (dbg) logger.log("Boolean preferences WebSocket disconnected");
                });
            });

            started.countDown();
        };

        Actor_engine.execute(r, "Javalin_boolean_preferences server", logger);
        try {
            started.await();
        } catch (InterruptedException e) {
            logger.log("Boolean preferences server interrupted: " + e.getMessage());
            return;
        }
        logger.log("Boolean preferences server started on port " + port_number);
    }

    private List<PreferenceItem> get_all_preferences() {
        List<PreferenceItem> items = new ArrayList<>();

        try {
            logger.log("get_all_preferences: starting, Feature.values() has " + Feature.values().length + " items");
            for (Feature feature : Feature.values()) {
                try {
                    String key = feature.name();
                    logger.log("Processing feature: " + key);
                    String label = My_I18n.get_I18n_string(key, null, logger);
                    String explanation = My_I18n.get_I18n_string(key + "_Explanation", null, logger);
                    boolean value = Feature_cache.get(feature);
                    String category = categorize_feature(feature);

                    items.add(new PreferenceItem(key, label, explanation, value, category));
                    logger.log("Loaded preference: " + key + " (category: " + category + ", value: " + value + ")");
                } catch (Exception e) {
                    logger.log("ERROR processing feature " + feature.name() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.log("ERROR in get_all_preferences: " + e.getMessage());
            e.printStackTrace();
        }

        logger.log("get_all_preferences: returning " + items.size() + " items");
        return items;
    }

    private String categorize_feature(Feature feature) {
        String name = feature.name();

        // UI Display Features
        if (name.startsWith("Show_") || name.startsWith("Hide_") || name.startsWith("Display_")) {
            return "UI Display";
        }

        // File Operations
        if (name.contains("_files") || name.contains("_folders") || name.contains("Monitor_folders")
            || name.equals("Remember_sorting_method_per_folder") || name.equals("Reload_last_folder_on_startup")) {
            return "File & Folder Display";
        }

        // Image Features
        if (name.contains("image") || name.contains("Image") || name.equals("Dont_zoom_small_images")) {
            return "Image Features";
        }

        // Processing & Advanced Features
        if (name.startsWith("Enable_") && (name.contains("mmap") || name.contains("face")
            || name.contains("similarity") || name.contains("deduplication")
            || name.contains("3D") || name.contains("backup") || name.contains("fusk")
            || name.contains("cleaning") || name.contains("corrupted") || name.contains("recursive"))) {
            return "Processing & Advanced";
        }

        // Text Editing
        if (name.contains("monaco") || name.contains("browser") || name.contains("text")) {
            return "Text Editing";
        }

        // Audio/Video
        if (name.equals("Play_music") || name.equals("Play_ding_after_long_processes")) {
            return "Audio & Video";
        }

        // Install Warnings
        if (name.contains("install_warning") || name.contains("install")) {
            return "Installation & Warnings";
        }

        // Logging & Debug
        if (name.startsWith("Log_") || name.contains("debug") || name.contains("Debug")) {
            return "Logging & Debug";
        }

        // Fusk specific
        if (name.contains("Fusk") || name.contains("fusk")) {
            return "Security & Obfuscation";
        }

        return "Other";
    }

    private void broadcast_update(String featureName, boolean newValue) {
        String update = String.format("{\"type\":\"UPDATE\",\"id\":\"%s\",\"value\":%b}",
                escapeJson(featureName), newValue);

        for (WsContext ctx : new ArrayList<>(connected_clients)) {
            if (ctx.session.isOpen()) {
                try {
                    ctx.send(update);
                } catch (Exception e) {
                    logger.log("Failed to send update to client: " + e.getMessage());
                    connected_clients.remove(ctx);
                }
            }
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
