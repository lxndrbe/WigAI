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
 * MCP tool for selecting/focusing a specific track in Bitwig Studio.
 */
public class SelectTrackTool {

    public static McpServerFeatures.SyncToolSpecification specification(
            BitwigApiFacade bitwigApiFacade, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {
                "track_index": {
                  "type": "integer",
                  "description": "0-based index of the track to select/focus."
                }
              },
              "required": ["track_index"],
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("select_track")
                .description("Select/focus a specific track in Bitwig Studio by its 0-based index.")
                .inputSchema(schema)
                .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "select_track",
                        req.arguments(),
                        logger,
                        SelectTrackTool::validateParameters,
                        (trackIndex) -> {
                            bitwigApiFacade.selectTrack(trackIndex);
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "select_track");
                            result.put("track_index", trackIndex);
                            result.put("message", "Selected track index " + trackIndex + ".");
                            return result;
                        }
                );

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    private static Integer validateParameters(Map<String, Object> args, String operation) {
        int trackIndex = ParameterValidator.validateRequiredInteger(args, "track_index", operation);
        if (trackIndex < 0) {
            throw new IllegalArgumentException("track_index must be >= 0");
        }
        return trackIndex;
    }
}
