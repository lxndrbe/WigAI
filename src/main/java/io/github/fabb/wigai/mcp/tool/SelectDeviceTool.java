package io.github.fabb.wigai.mcp.tool;

import io.github.fabb.wigai.bitwig.BitwigApiFacade;
import io.github.fabb.wigai.common.logging.StructuredLogger;
import io.github.fabb.wigai.mcp.McpErrorHandler;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP tools for navigating the device cursor in Bitwig Studio.
 */
public class SelectDeviceTool {

    public static McpServerFeatures.SyncToolSpecification selectNextDeviceSpecification(
            BitwigApiFacade bitwigApiFacade, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {},
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("select_next_device")
                .description("Select/focus the next device in the chain of the active track.")
                .inputSchema(schema)
                .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "select_next_device",
                        req.arguments(),
                        logger,
                        (args, operation) -> null,
                        (params) -> {
                            bitwigApiFacade.selectNextDevice();
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "select_next_device");
                            result.put("message", "Focused next device in chain.");
                            return result;
                        }
                );

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification selectPreviousDeviceSpecification(
            BitwigApiFacade bitwigApiFacade, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {},
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("select_previous_device")
                .description("Select/focus the previous device in the chain of the active track.")
                .inputSchema(schema)
                .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "select_previous_device",
                        req.arguments(),
                        logger,
                        (args, operation) -> null,
                        (params) -> {
                            bitwigApiFacade.selectPreviousDevice();
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "select_previous_device");
                            result.put("message", "Focused previous device in chain.");
                            return result;
                        }
                );

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification selectFirstDeviceSpecification(
            BitwigApiFacade bitwigApiFacade, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {},
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("select_first_device")
                .description("Select/focus the first device in the chain of the active track.")
                .inputSchema(schema)
                .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "select_first_device",
                        req.arguments(),
                        logger,
                        (args, operation) -> null,
                        (params) -> {
                            bitwigApiFacade.selectFirstDevice();
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "select_first_device");
                            result.put("message", "Focused first device in chain.");
                            return result;
                        }
                );

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }
}
