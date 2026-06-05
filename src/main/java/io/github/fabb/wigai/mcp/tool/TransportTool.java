package io.github.fabb.wigai.mcp.tool;

import io.github.fabb.wigai.common.logging.StructuredLogger;
import io.github.fabb.wigai.features.TransportController;
import io.github.fabb.wigai.mcp.McpErrorHandler;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP tools for transport control in Bitwig using unified error handling architecture.
 */
public class TransportTool {

    /**
     * Creates a "transport_start" tool specification using the unified error handling system.
     *
     * @param transportController The controller for transport operations
     * @param logger The structured logger for logging operations
     * @return A SyncToolSpecification for the "transport_start" tool
     */
    public static McpServerFeatures.SyncToolSpecification transportStartSpecification(
            TransportController transportController, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {}
            }""";
        var tool = McpSchema.Tool.builder()
            .name("transport_start")
            .description("Start Bitwig's transport playbook.")
            .inputSchema(schema)
            .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
            (exchange, req) -> McpErrorHandler.executeWithErrorHandling(
                "transport_start",
                logger,
                () -> {
                    String resultMessage = transportController.startTransport();
                    return Map.of(
                        "action", "transport_started",
                        "message", resultMessage
                    );
                }
            );

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(tool)
            .callHandler(handler)
            .build();
    }

    /**
     * Creates a "transport_stop" tool specification using the unified error handling system.
     *
     * @param transportController The controller for transport operations
     * @param logger The structured logger for logging operations
     * @return A SyncToolSpecification for the "transport_stop" tool
     */
    public static McpServerFeatures.SyncToolSpecification transportStopSpecification(
            TransportController transportController, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {}
            }""";
        var tool = McpSchema.Tool.builder()
            .name("transport_stop")
            .description("Stop Bitwig's transport playback.")
            .inputSchema(schema)
            .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
            (exchange, req) -> McpErrorHandler.executeWithErrorHandling(
                "transport_stop",
                logger,
                () -> {
                    String resultMessage = transportController.stopTransport();
                    return Map.of(
                        "action", "transport_stopped",
                        "message", resultMessage
                    );
                }
            );

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(tool)
            .callHandler(handler)
            .build();
    }

    /**
     * Creates a "configure_transport" tool specification using the unified error handling system.
     *
     * @param transportController The controller for transport operations
     * @param logger The structured logger for logging operations
     * @return A SyncToolSpecification for the "configure_transport" tool
     */
    public static McpServerFeatures.SyncToolSpecification configureTransportSpecification(
            TransportController transportController, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {
                "tempo": {
                  "type": "number",
                  "description": "Tempo in BPM (e.g. 120.0)."
                },
                "metronome": {
                  "type": "boolean",
                  "description": "Enable (true) or disable (false) the metronome."
                },
                "loop": {
                  "type": "boolean",
                  "description": "Enable (true) or disable (false) the arranger loop."
                },
                "record_arm": {
                  "type": "boolean",
                  "description": "Enable (true) or disable (false) the arranger record-arm."
                }
              },
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
            .name("configure_transport")
            .description("Configure transport properties: tempo (BPM), metronome, arranger loop, and arranger record-arm.")
            .inputSchema(schema)
            .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
            (exchange, req) -> McpErrorHandler.executeWithErrorHandling(
                "configure_transport",
                logger,
                () -> {
                    Map<String, Object> args = req.arguments();
                    Double tempo = null;
                    if (args.containsKey("tempo") && args.get("tempo") instanceof Number n) {
                        tempo = n.doubleValue();
                        if (tempo <= 0.0) {
                            throw new IllegalArgumentException("tempo must be positive");
                        }
                    }
                    Boolean metronome = null;
                    if (args.containsKey("metronome") && args.get("metronome") instanceof Boolean b) {
                        metronome = b;
                    }
                    Boolean loop = null;
                    if (args.containsKey("loop") && args.get("loop") instanceof Boolean b) {
                        loop = b;
                    }
                    Boolean recordArm = null;
                    if (args.containsKey("record_arm") && args.get("record_arm") instanceof Boolean b) {
                        recordArm = b;
                    }

                    String resultMessage = transportController.configureTransport(tempo, metronome, loop, recordArm);
                    return Map.of(
                        "action", "configure_transport",
                        "message", resultMessage
                    );
                }
            );

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(tool)
            .callHandler(handler)
            .build();
    }
}
