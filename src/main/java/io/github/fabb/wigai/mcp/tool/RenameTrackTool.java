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
 * MCP tool for programmatically renaming tracks in Bitwig Studio.
 */
public class RenameTrackTool {

    private record ValidatedParams(int trackIndex, String name) {}

    public static McpServerFeatures.SyncToolSpecification specification(
            BitwigApiFacade bitwigApiFacade, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {
                "track_index": {
                  "type": "integer",
                  "description": "0-based index of the track to rename.",
                  "minimum": 0
                },
                "name": {
                  "type": "string",
                  "description": "The new name for the track."
                }
              },
              "required": ["track_index", "name"],
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("rename_track")
                .description("Rename a track in Bitwig Studio by providing its zero-based index and a new name.")
                .inputSchema(schema)
                .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "rename_track",
                        req.arguments(),
                        logger,
                        RenameTrackTool::validateParameters,
                        (params) -> {
                            bitwigApiFacade.renameTrack(params.trackIndex(), params.name());
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "rename_track");
                            result.put("track_index", params.trackIndex());
                            result.put("name", params.name());
                            result.put("message", "Track " + params.trackIndex() + " renamed to '" + params.name() + "' successfully.");
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
        int trackIndex = ParameterValidator.validateRequiredInteger(args, "track_index", operation);
        if (trackIndex < 0) {
            throw new BitwigApiException(ErrorCode.INVALID_RANGE, operation,
                    "track_index must be >= 0, got: " + trackIndex);
        }

        String name = ParameterValidator.validateRequiredString(args, "name", operation);
        ParameterValidator.validateNotEmpty(name, "name", operation);

        return new ValidatedParams(trackIndex, name);
    }
}
