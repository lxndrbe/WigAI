package io.github.fabb.wigai.mcp.tool;

import io.github.fabb.wigai.bitwig.BitwigApiFacade;
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
 * MCP tool for toggling the bypass state of a device on a track.
 */
public class BypassDeviceTool {

    private record ValidatedParams(int trackIndex, int deviceIndex, boolean bypassed) {}

    public static McpServerFeatures.SyncToolSpecification specification(
            BitwigApiFacade bitwigApiFacade, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {
                "track_index": {
                  "type": "integer",
                  "description": "0-based index of the track containing the device."
                },
                "device_index": {
                  "type": "integer",
                  "description": "0-based index of the device in the track's chain."
                },
                "bypassed": {
                  "type": "boolean",
                  "description": "true to bypass (disable) the device, false to active (enable) it."
                }
              },
              "required": ["track_index", "device_index", "bypassed"],
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("set_device_bypass")
                .description("Bypass (deactivate) or activate a specific device on a track by track index and device index.")
                .inputSchema(schema)
                .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "set_device_bypass",
                        req.arguments(),
                        logger,
                        BypassDeviceTool::validateParameters,
                        (params) -> {
                            bitwigApiFacade.setDeviceBypass(params.trackIndex(), params.deviceIndex(), params.bypassed());
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "set_device_bypass");
                            result.put("track_index", params.trackIndex());
                            result.put("device_index", params.deviceIndex());
                            result.put("bypassed", params.bypassed());
                            result.put("message", "Device " + params.deviceIndex() + " on track " + params.trackIndex()
                                    + " bypass set to: " + params.bypassed() + ".");
                            return result;
                        }
                );

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    private static ValidatedParams validateParameters(Map<String, Object> args, String operation) {
        int trackIndex = ParameterValidator.validateRequiredInteger(args, "track_index", operation);
        if (trackIndex < 0) {
            throw new IllegalArgumentException("track_index must be >= 0");
        }

        int deviceIndex = ParameterValidator.validateRequiredInteger(args, "device_index", operation);
        if (deviceIndex < 0) {
            throw new IllegalArgumentException("device_index must be >= 0");
        }

        if (!args.containsKey("bypassed") || !(args.get("bypassed") instanceof Boolean)) {
            throw new IllegalArgumentException("Parameter 'bypassed' is required and must be a boolean");
        }
        boolean bypassed = (Boolean) args.get("bypassed");

        return new ValidatedParams(trackIndex, deviceIndex, bypassed);
    }
}
