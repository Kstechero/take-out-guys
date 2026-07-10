package com.sky.service.ai.mcp;

import com.sky.properties.AiProperties;
import com.sky.service.ai.knowledge.OperationsKnowledgeService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OperationsResourceCatalogService {

    private final AiProperties aiProperties;
    private final OperationsKnowledgeService knowledgeService;

    public OperationsResourceCatalogService(AiProperties aiProperties, OperationsKnowledgeService knowledgeService) {
        this.aiProperties = aiProperties;
        this.knowledgeService = knowledgeService;
    }

    public Map<String, Object> listCatalog() {
        Map<String, Object> sources = knowledgeService.listSources();
        return mapOf(
                "enabled", aiProperties.getMcp().isEnabled(),
                "catalogName", aiProperties.getMcp().getServerName(),
                "catalogVersion", aiProperties.getMcp().getServerVersion(),
                "transport", "in-process bridge",
                "protocol", "mcp-inspired",
                "methods", Arrays.asList("initialize", "tools/list", "tools/call", "resources/list", "resources/read"),
                "tools", toolCatalog(),
                "resources", sources.get("sources"),
                "notes", Arrays.asList(
                        "Current implementation exposes a read-only operations resource catalog through AI tools.",
                        "It is intended for internal document lookup, process explanation, and capability discovery."
                )
        );
    }

    public Map<String, Object> readResource(String uri) {
        return knowledgeService.readResource(uri);
    }

    public Map<String, Object> health() {
        return mapOf(
                "enabled", aiProperties.getMcp().isEnabled(),
                "catalogName", aiProperties.getMcp().getServerName(),
                "catalogVersion", aiProperties.getMcp().getServerVersion(),
                "mode", "read-only in-process bridge"
        );
    }

    private List<Map<String, Object>> toolCatalog() {
        return Arrays.asList(
                mapOf("name", "list_operational_documents", "type", "resource-discovery", "mode", "read-only"),
                mapOf("name", "search_operational_knowledge", "type", "knowledge-search", "mode", "read-only"),
                mapOf("name", "list_resource_catalog", "type", "capability-discovery", "mode", "read-only"),
                mapOf("name", "read_resource_detail", "type", "resource-read", "mode", "read-only")
        );
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
