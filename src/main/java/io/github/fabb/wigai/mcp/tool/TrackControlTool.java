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
 * MCP tool for controlling track parameters like volume, pan, mute, solo, and arm.
 */
public class TrackControlTool {

    private record ValidatedParams(int trackIndex, String parameterName, Object value) {}

    public static McpServerFeatures.SyncToolSpecification specification(
            BitwigApiFacade bitwigApiFacade, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {
                "track_index": {
                  "type": "integer",
                  "description": "0-based index of the track."
                },
                "parameter_name": {
                  "type": "string",
                  "description": "The track parameter to modify.",
                  "enum": ["volume", "pan", "mute", "solo", "arm"]
                },
                "value": {
                  "description": "The value to set. For volume and pan, a float between 0.0 and 1.0. For mute, solo, and arm, a boolean."
                }
              },
              "required": ["track_index", "parameter_name", "value"],
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("set_track_parameter")
                .description("Modify parameters of a specific track: volume (0.0-1.0), pan (0.0-1.0), mute (boolean), solo (boolean), or arm (boolean).")
                .inputSchema(schema)
                .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "set_track_parameter",
                        req.arguments(),
                        logger,
                        TrackControlTool::validateParameters,
                        (params) -> {
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "set_track_parameter");
                            result.put("track_index", params.trackIndex());
                            result.put("parameter_name", params.parameterName());
                            result.put("value", params.value());

                            switch (params.parameterName().toLowerCase()) {
                                case "volume" -> {
                                    double val = ((Number) params.value()).doubleValue();
                                    bitwigApiFacade.setTrackVolume(params.trackIndex(), val);
                                    result.put("message", "Set track " + params.trackIndex() + " volume to " + val + ".");
                                }
                                case "pan" -> {
                                    double val = ((Number) params.value()).doubleValue();
                                    bitwigApiFacade.setTrackPan(params.trackIndex(), val);
                                    result.put("message", "Set track " + params.trackIndex() + " pan to " + val + ".");
                                }
                                case "mute" -> {
                                    boolean val = (Boolean) params.value();
                                    bitwigApiFacade.setTrackMute(params.trackIndex(), val);
                                    result.put("message", "Set track " + params.trackIndex() + " mute to " + val + ".");
                                }
                                case "solo" -> {
                                    boolean val = (Boolean) params.value();
                                    bitwigApiFacade.setTrackSolo(params.trackIndex(), val);
                                    result.put("message", "Set track " + params.trackIndex() + " solo to " + val + ".");
                                }
                                case "arm" -> {
                                    boolean val = (Boolean) params.value();
                                    bitwigApiFacade.setTrackArm(params.trackIndex(), val);
                                    result.put("message", "Set track " + params.trackIndex() + " arm to " + val + ".");
                                }
                            }
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

        String parameterName = ParameterValidator.validateRequiredString(args, "parameter_name", operation);
        if (!args.containsKey("value")) {
            throw new IllegalArgumentException("Parameter 'value' is required");
        }
        Object value = args.get("value");

        switch (parameterName.toLowerCase()) {
            case "volume", "pan" -> {
                if (!(value instanceof Number)) {
                    throw new IllegalArgumentException("value must be a number for volume/pan");
                }
                double val = ((Number) value).doubleValue();
                if (val < 0.0 || val > 1.0) {
                    throw new IllegalArgumentException("value must be between 0.0 and 1.0 for volume/pan");
                }
            }
            case "mute", "solo", "arm" -> {
                if (!(value instanceof Boolean)) {
                    throw new IllegalArgumentException("value must be a boolean for mute/solo/arm");
                }
            }
            default -> throw new IllegalArgumentException("Unknown parameter_name: '" + parameterName + "'. Supported: volume, pan, mute, solo, arm");
        }

        return new ValidatedParams(trackIndex, parameterName, value);
    }
}
