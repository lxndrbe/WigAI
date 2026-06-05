package io.github.fabb.wigai.mcp.tool;

import io.github.fabb.wigai.bitwig.BitwigApiFacade;
import io.github.fabb.wigai.bitwig.DeviceRegistry;
import io.github.fabb.wigai.common.error.BitwigApiException;
import io.github.fabb.wigai.common.error.ErrorCode;
import io.github.fabb.wigai.common.logging.StructuredLogger;
import io.github.fabb.wigai.common.validation.ParameterValidator;
import io.github.fabb.wigai.mcp.McpErrorHandler;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP tool that inserts a Bitwig-native device into a track's device chain by UUID.
 *
 * Only Bitwig-native devices (bitwig_audio_fx, bitwig_instruments) can be inserted
 * via the Controller API. Use list_bitwig_devices to discover valid UUIDs.
 */
public class InsertBitwigDeviceTool {

    private record ValidatedParams(int trackIndex, String deviceUuid, String position) {}

    public static McpServerFeatures.SyncToolSpecification specification(
            BitwigApiFacade bitwigApiFacade, DeviceRegistry deviceRegistry, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {
                "track_index": {
                  "type": "integer",
                  "description": "0-based index of the track to insert the device into."
                },
                "device_uuid": {
                  "type": "string",
                  "description": "UUID of the Bitwig-native device to insert. Use list_bitwig_devices to find valid UUIDs."
                },
                "position": {
                  "type": "string",
                  "description": "Where to insert in the device chain: 'end' (default) appends after all existing devices, 'start' inserts before all existing devices.",
                  "enum": ["end", "start"]
                }
              },
              "required": ["track_index", "device_uuid"],
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("insert_bitwig_device")
                .description("""
                        Insert a Bitwig-native device into a track's device chain by UUID. \
                        Only Bitwig-native devices (instruments and audio effects built into Bitwig) \
                        can be inserted this way — not VST/CLAP plugins. \
                        Use list_bitwig_devices to discover available device UUIDs. \
                        The device is appended at the end of the chain by default; \
                        use position='start' to prepend instead.""")
                .inputSchema(schema)
                .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "insert_bitwig_device",
                        req.arguments(),
                        logger,
                        (args, operation) -> validateParameters(args, operation, deviceRegistry),
                        (params) -> {
                            bitwigApiFacade.insertBitwigDevice(
                                    params.trackIndex(), params.deviceUuid(), params.position());
                            DeviceRegistry.DeviceInfo info =
                                    deviceRegistry.findById(params.deviceUuid()).orElse(null);
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "insert_bitwig_device");
                            result.put("track_index", params.trackIndex());
                            result.put("device_uuid", params.deviceUuid());
                            result.put("device_name", info != null ? info.name() : "unknown");
                            result.put("position", params.position());
                            result.put("message", "Device '" + (info != null ? info.name() : params.deviceUuid())
                                    + "' inserted at " + params.position() + " of track " + params.trackIndex() + ".");
                            return result;
                        }
                );

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    private static ValidatedParams validateParameters(
            Map<String, Object> args, String operation, DeviceRegistry deviceRegistry)
            throws BitwigApiException {

        int trackIndex = ParameterValidator.validateRequiredInteger(args, "track_index", operation);
        if (trackIndex < 0) {
            throw new BitwigApiException(ErrorCode.INVALID_RANGE, operation,
                    "track_index must be >= 0, got: " + trackIndex);
        }

        String deviceUuid = ParameterValidator.validateRequiredString(args, "device_uuid", operation);
        ParameterValidator.validateNotEmpty(deviceUuid, "device_uuid", operation);

        if (!deviceRegistry.isInsertable(deviceUuid)) {
            throw new BitwigApiException(ErrorCode.INVALID_PARAMETER, operation,
                    "Unknown or non-insertable device UUID: '" + deviceUuid
                    + "'. Only Bitwig-native device UUIDs are supported. "
                    + "Use list_bitwig_devices to find valid UUIDs.");
        }

        String position = "end";
        if (args.containsKey("position") && args.get("position") instanceof String s) {
            if (!s.equals("end") && !s.equals("start")) {
                throw new BitwigApiException(ErrorCode.INVALID_PARAMETER, operation,
                        "position must be 'end' or 'start', got: '" + s + "'");
            }
            position = s;
        }

        return new ValidatedParams(trackIndex, deviceUuid, position);
    }
}
