package io.github.fabb.wigai.mcp.tool;

import io.github.fabb.wigai.bitwig.DeviceRegistry;
import io.github.fabb.wigai.common.logging.StructuredLogger;
import io.github.fabb.wigai.mcp.McpErrorHandler;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * MCP tool that lists known Bitwig devices with their UUIDs.
 *
 * Without a category filter: returns all Bitwig-native devices (insertable via Controller API)
 * grouped by category (bitwig_audio_fx, bitwig_instruments).
 *
 * With category="all": additionally includes CLAP/VST2/VST3 reference entries (not insertable
 * via this API).
 */
public class ListBitwigDevicesTool {

    private static final Set<String> VALID_CATEGORIES =
            Set.of("bitwig_audio_fx", "bitwig_instruments", "clap_audio_fx", "clap_instruments",
                    "vst2_audio_fx", "vst2_instruments", "vst3_audio_fx", "vst3_instruments", "all");

    public static McpServerFeatures.SyncToolSpecification specification(
            DeviceRegistry deviceRegistry, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {
                "category": {
                  "type": "string",
                  "description": "Filter by category. Omit to list only insertable Bitwig-native devices. Use 'all' for every category including CLAP/VST2/VST3 reference entries.",
                  "enum": ["bitwig_audio_fx", "bitwig_instruments", "clap_audio_fx", "clap_instruments", "vst2_audio_fx", "vst2_instruments", "vst3_audio_fx", "vst3_instruments", "all"]
                }
              },
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("list_bitwig_devices")
                .description("""
                        List known Bitwig devices with their UUIDs. \
                        Without a category filter, returns all insertable Bitwig-native devices \
                        (bitwig_audio_fx and bitwig_instruments) grouped by category. \
                        Use category='all' to also see CLAP/VST2/VST3 reference entries \
                        (those cannot be inserted via insert_bitwig_device). \
                        Use the returned uuid values with the insert_bitwig_device tool.""")
                .inputSchema(schema)
                .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithErrorHandling(
                        "list_bitwig_devices",
                        logger,
                        () -> buildResponse(deviceRegistry, req.arguments())
                );

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    private static Object buildResponse(DeviceRegistry registry, Map<String, Object> args) {
        String categoryFilter = args != null && args.get("category") instanceof String s ? s : null;

        if (categoryFilter == null) {
            // Default: only Bitwig-native (insertable) devices, grouped by category
            Map<String, List<Map<String, String>>> grouped = new LinkedHashMap<>();
            registry.getAllByCategory().forEach((cat, devices) -> {
                if (cat.equals("bitwig_audio_fx") || cat.equals("bitwig_instruments")) {
                    grouped.put(cat, devices.stream()
                            .map(d -> Map.of("name", d.name(), "uuid", d.id()))
                            .collect(Collectors.toList()));
                }
            });
            int total = grouped.values().stream().mapToInt(List::size).sum();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("insertable_device_count", total);
            result.put("note", "Only Bitwig-native devices are insertable via insert_bitwig_device. Use category='all' to see CLAP/VST2/VST3 reference entries.");
            result.put("categories", grouped);
            return result;
        }

        if (categoryFilter.equals("all")) {
            Map<String, Object> result = new LinkedHashMap<>();
            Map<String, List<Map<String, String>>> grouped = new LinkedHashMap<>();
            registry.getAllByCategory().forEach((cat, devices) -> {
                boolean insertable = cat.equals("bitwig_audio_fx") || cat.equals("bitwig_instruments");
                grouped.put(cat, devices.stream()
                        .map(d -> {
                            Map<String, String> entry = new LinkedHashMap<>();
                            entry.put("name", d.name());
                            entry.put("id", d.id());
                            entry.put("insertable", insertable ? "true" : "false");
                            return entry;
                        })
                        .collect(Collectors.toList()));
            });
            int total = grouped.values().stream().mapToInt(List::size).sum();
            result.put("total_device_count", total);
            result.put("categories", grouped);
            return result;
        }

        // Specific category
        List<DeviceRegistry.DeviceInfo> devices = registry.getAllByCategory().getOrDefault(categoryFilter, List.of());
        boolean insertable = categoryFilter.equals("bitwig_audio_fx") || categoryFilter.equals("bitwig_instruments");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("category", categoryFilter);
        result.put("insertable", insertable);
        result.put("device_count", devices.size());
        result.put("devices", devices.stream()
                .map(d -> Map.of("name", d.name(), "id", d.id()))
                .collect(Collectors.toList()));
        return result;
    }
}
