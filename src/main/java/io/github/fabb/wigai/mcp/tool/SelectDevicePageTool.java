package io.github.fabb.wigai.mcp.tool;

import io.github.fabb.wigai.features.DeviceController;
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
 * MCP tool for selecting/switching a device remote control parameter page in Bitwig Studio.
 */
public class SelectDevicePageTool {

    private record ValidatedParams(Integer pageIndex, String pageName) {}

    public static McpServerFeatures.SyncToolSpecification specification(
            DeviceController deviceController, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {
                "page_index": {
                  "type": "integer",
                  "description": "0-based index of the remote control page to select. If specified, page_name must not be set."
                },
                "page_name": {
                  "type": "string",
                  "description": "Name of the remote control page to select (case-insensitive). If specified, page_index must not be set."
                }
              },
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
                .name("select_device_page")
                .description("Select/switch a remote control parameter page on the currently selected device.")
                .inputSchema(schema)
                .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, req) -> McpErrorHandler.executeWithValidation(
                        "select_device_page",
                        req.arguments(),
                        logger,
                        SelectDevicePageTool::validateParameters,
                        (params) -> {
                            if (params.pageIndex() != null) {
                                deviceController.selectDevicePage(params.pageIndex());
                            } else {
                                deviceController.selectDevicePageByName(params.pageName());
                            }
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("action", "select_device_page");
                            if (params.pageIndex() != null) {
                                result.put("page_index", params.pageIndex());
                            }
                            if (params.pageName() != null) {
                                result.put("page_name", params.pageName());
                            }
                            result.put("message", "Selected device page " + 
                                (params.pageIndex() != null ? "index " + params.pageIndex() : "'" + params.pageName() + "'") + ".");
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
        Integer pageIndex = null;
        if (args.containsKey("page_index")) {
            pageIndex = ParameterValidator.validateRequiredInteger(args, "page_index", operation);
            if (pageIndex < 0) {
                throw new BitwigApiException(ErrorCode.INVALID_PARAMETER, operation,
                        "page_index must be >= 0");
            }
        }

        String pageName = null;
        if (args.containsKey("page_name")) {
            pageName = ParameterValidator.validateRequiredString(args, "page_name", operation);
            ParameterValidator.validateNotEmpty(pageName, "page_name", operation);
        }

        if (pageIndex == null && pageName == null) {
            throw new BitwigApiException(ErrorCode.INVALID_PARAMETER, operation,
                    "Either page_index or page_name must be provided");
        }

        if (pageIndex != null && pageName != null) {
            throw new BitwigApiException(ErrorCode.INVALID_PARAMETER, operation,
                    "Cannot specify both page_index and page_name");
        }

        return new ValidatedParams(pageIndex, pageName);
    }
}
