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
 * MCP tool for programmatically creating new tracks in Bitwig Studio.
 */
public class CreateTrackTool {

    private record ValidatedParams(String type) {}

    public static McpServerFeatures.SyncToolSpecification specification(
            BitwigApiFacade bitwigApiFacade, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {
                "type": {
                  "type": "string",
                  "description": "Type of track to create: 'audio', 'instrument', 'effect' ('fx'), or 'group'.",
                  "enum": ["audio", "instrument", "effect", "fx", "group"]
                }
              },
              "required": ["type"],
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("create_track")
                .description("Create a new track in Bitwig Studio (audio, instrument, effect, or group).")
                .inputSchema(schema)
                .build();
        
        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "create_track",
                        req.arguments(),
                        logger,
                        CreateTrackTool::validateParameters,
                        (params) -> {
                            bitwigApiFacade.createTrack(params.type());
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "create_track");
                            result.put("type", params.type());
                            result.put("message", "Track of type '" + params.type() + "' created successfully.");
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
        String type = ParameterValidator.validateRequiredString(args, "type", operation);
        ParameterValidator.validateNotEmpty(type, "type", operation);

        String lowerType = type.toLowerCase();
        if (!lowerType.equals("audio") && !lowerType.equals("instrument") && !lowerType.equals("effect") && !lowerType.equals("fx") && !lowerType.equals("group")) {
            throw new BitwigApiException(ErrorCode.INVALID_PARAMETER, operation,
                    "type must be 'audio', 'instrument', 'effect', 'fx', or 'group', got: '" + type + "'");
        }

        return new ValidatedParams(lowerType);
    }
}
