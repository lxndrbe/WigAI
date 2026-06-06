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

    private record ValidatedParams(String type, Integer parentGroupIndex, String parentGroupName) {}

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
                },
                "parent_group_index": {
                  "type": "integer",
                  "description": "Optional 0-based index of the parent group track in which to create the track. If specified, parent_group_name must not be set."
                },
                "parent_group_name": {
                  "type": "string",
                  "description": "Optional name of the parent group track in which to create the track. If specified, parent_group_index must not be set."
                }
              },
              "required": ["type"],
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("create_track")
                .description("Create a new track in Bitwig Studio (audio, instrument, effect, or group). Optionally inside a parent group track.")
                .inputSchema(schema)
                .build();
        
        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "create_track",
                        req.arguments(),
                        logger,
                        CreateTrackTool::validateParameters,
                        (params) -> {
                            bitwigApiFacade.createTrack(params.type(), params.parentGroupIndex(), params.parentGroupName());
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "create_track");
                            result.put("type", params.type());
                            if (params.parentGroupIndex() != null) {
                                result.put("parent_group_index", params.parentGroupIndex());
                            }
                            if (params.parentGroupName() != null) {
                                result.put("parent_group_name", params.parentGroupName());
                            }
                            result.put("message", "Track of type '" + params.type() + "' created successfully" + 
                                (params.parentGroupIndex() != null ? " inside parent group index " + params.parentGroupIndex() : "") +
                                (params.parentGroupName() != null ? " inside parent group '" + params.parentGroupName() + "'" : "") + ".");
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

        Integer parentGroupIndex = null;
        if (args.containsKey("parent_group_index")) {
            parentGroupIndex = ParameterValidator.validateRequiredInteger(args, "parent_group_index", operation);
            if (parentGroupIndex < 0) {
                throw new BitwigApiException(ErrorCode.INVALID_PARAMETER, operation,
                        "parent_group_index must be >= 0");
            }
        }

        String parentGroupName = null;
        if (args.containsKey("parent_group_name")) {
            parentGroupName = ParameterValidator.validateRequiredString(args, "parent_group_name", operation);
            ParameterValidator.validateNotEmpty(parentGroupName, "parent_group_name", operation);
        }

        if (parentGroupIndex != null && parentGroupName != null) {
            throw new BitwigApiException(ErrorCode.INVALID_PARAMETER, operation,
                    "Cannot specify both parent_group_index and parent_group_name");
        }

        return new ValidatedParams(lowerType, parentGroupIndex, parentGroupName);
    }
}
