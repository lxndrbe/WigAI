package io.github.fabb.wigai.mcp.tool;

import io.github.fabb.wigai.bitwig.BitwigApiFacade;
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
 * MCP tool for selecting/focusing a specific track in Bitwig Studio.
 */
public class SelectTrackTool {

    private record ValidatedParams(Integer trackIndex, String trackName) {}

    public static McpServerFeatures.SyncToolSpecification specification(
            BitwigApiFacade bitwigApiFacade, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {
                "track_index": {
                  "type": "integer",
                  "description": "0-based index of the track to select/focus. If specified, track_name must not be set."
                },
                "track_name": {
                  "type": "string",
                  "description": "Name of the track to select/focus. If specified, track_index must not be set."
                }
              },
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("select_track")
                .description("Select/focus a specific track in Bitwig Studio by its 0-based index or its name.")
                .inputSchema(schema)
                .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "select_track",
                        req.arguments(),
                        logger,
                        SelectTrackTool::validateParameters,
                        (params) -> {
                            if (params.trackIndex() != null) {
                                bitwigApiFacade.selectTrack(params.trackIndex());
                            } else {
                                bitwigApiFacade.selectTrackByName(params.trackName());
                            }
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "select_track");
                            if (params.trackIndex() != null) {
                                result.put("track_index", params.trackIndex());
                            }
                            if (params.trackName() != null) {
                                result.put("track_name", params.trackName());
                            }
                            result.put("message", "Selected track " + 
                                (params.trackIndex() != null ? "index " + params.trackIndex() : "'" + params.trackName() + "'") + ".");
                            return result;
                        }
                );

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    private static ValidatedParams validateParameters(Map<String, Object> args, String operation)
            throws BitwigApiException {
        Integer trackIndex = null;
        if (args.containsKey("track_index")) {
            trackIndex = ParameterValidator.validateRequiredInteger(args, "track_index", operation);
            if (trackIndex < 0) {
                throw new BitwigApiException(ErrorCode.INVALID_PARAMETER, operation,
                        "track_index must be >= 0");
            }
        }

        String trackName = null;
        if (args.containsKey("track_name")) {
            trackName = ParameterValidator.validateRequiredString(args, "track_name", operation);
            ParameterValidator.validateNotEmpty(trackName, "track_name", operation);
        }

        if (trackIndex == null && trackName == null) {
            throw new BitwigApiException(ErrorCode.INVALID_PARAMETER, operation,
                    "Either track_index or track_name must be provided");
        }

        if (trackIndex != null && trackName != null) {
            throw new BitwigApiException(ErrorCode.INVALID_PARAMETER, operation,
                    "Cannot specify both track_index and track_name");
        }

        return new ValidatedParams(trackIndex, trackName);
    }
}
