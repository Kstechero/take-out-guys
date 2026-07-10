package com.sky.service.ai.knowledge;

import com.sky.properties.AiProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OperationsKnowledgeService {

    private static final int RESOURCE_READ_LIMIT = 12000;
    private static final Pattern LATIN_TOKEN_PATTERN = Pattern.compile("[a-z0-9_\\-]{2,}");
    private static final Pattern CJK_TOKEN_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");

    private final AiProperties aiProperties;

    private volatile boolean indexed;
    private volatile LocalDateTime indexedAt;
    private final List<KnowledgeDocument> documents = new ArrayList<>();
    private final List<KnowledgeChunk> chunks = new ArrayList<>();

    public OperationsKnowledgeService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    public synchronized Map<String, Object> listSources() {
        ensureIndexed();
        List<Map<String, Object>> items = new ArrayList<>();
        for (KnowledgeDocument document : documents) {
            items.add(document.toMap());
        }
        return mapOf(
                "enabled", aiProperties.getRag().isEnabled(),
                "indexed", indexed,
                "indexedAt", indexedAt == null ? null : indexedAt.toString(),
                "documentCount", items.size(),
                "chunkCount", chunks.size(),
                "retrievalMode", "multi-signal business knowledge retrieval",
                "sources", items
        );
    }

    public synchronized Map<String, Object> search(String query, Integer topK) {
        ensureIndexed();
        if (!aiProperties.getRag().isEnabled()) {
            return mapOf("enabled", false, "matches", Collections.emptyList());
        }
        if (!StringUtils.hasText(query)) {
            return mapOf("enabled", true, "matches", Collections.emptyList(), "message", "query is empty");
        }

        int limit = topK == null || topK <= 0 ? aiProperties.getRag().getTopK() : topK;
        List<String> queryTerms = extractTerms(query);
        List<Map<String, Object>> matches = new ArrayList<>();
        List<RankedChunk> rankedChunks = new ArrayList<>();

        for (KnowledgeChunk chunk : chunks) {
            int score = score(chunk, query, queryTerms);
            if (score <= 0) {
                continue;
            }
            rankedChunks.add(new RankedChunk(chunk, score));
        }

        rankedChunks.sort(Comparator
                .comparingInt(RankedChunk::getScore).reversed()
                .thenComparingInt(RankedChunk::getPriority));

        for (RankedChunk rankedChunk : rankedChunks) {
            KnowledgeChunk chunk = rankedChunk.chunk;
            matches.add(mapOf(
                    "uri", chunk.uri,
                    "title", chunk.title,
                    "path", chunk.path,
                    "domain", chunk.domain,
                    "tags", chunk.tags,
                    "score", rankedChunk.score,
                    "excerpt", excerpt(chunk.content, query),
                    "chunkIndex", chunk.chunkIndex
            ));
            if (matches.size() >= limit) {
                break;
            }
        }

        return mapOf(
                "enabled", true,
                "retrievalMode", "multi-signal business knowledge retrieval",
                "query", query,
                "topK", limit,
                "matches", matches
        );
    }

    public synchronized String buildGroundingContext(String query, Integer topK) {
        Map<String, Object> searchResult = search(query, topK);
        Object matchesObject = searchResult.get("matches");
        if (!(matchesObject instanceof List)) {
            return "";
        }

        List<?> matches = (List<?>) matchesObject;
        if (matches.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("Use the following internal business knowledge as grounded context. ");
        context.append("If tool results or live business data conflict with these documents, trust the tool results.\n");
        int index = 1;
        for (Object item : matches) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> match = (Map<?, ?>) item;
            context.append("[Source ").append(index++).append("] ")
                    .append(stringValue(match.get("title")))
                    .append(" | domain=")
                    .append(stringValue(match.get("domain")))
                    .append(" | uri=")
                    .append(stringValue(match.get("uri")))
                    .append('\n')
                    .append(stringValue(match.get("excerpt")))
                    .append("\n\n");
        }
        return context.toString().trim();
    }

    public synchronized Map<String, Object> readResource(String uri) {
        ensureIndexed();
        for (KnowledgeDocument document : documents) {
            if (document.uri.equals(uri)) {
                boolean truncated = document.content.length() > RESOURCE_READ_LIMIT;
                return mapOf(
                        "enabled", true,
                        "uri", document.uri,
                        "title", document.title,
                        "path", document.path,
                        "domain", document.domain,
                        "tags", document.tags,
                        "content", truncated ? document.content.substring(0, RESOURCE_READ_LIMIT) : document.content,
                        "truncated", truncated
                );
            }
        }
        return mapOf("enabled", aiProperties.getRag().isEnabled(), "uri", uri, "found", false);
    }

    public synchronized Map<String, Object> health() {
        ensureIndexed();
        return mapOf(
                "enabled", aiProperties.getRag().isEnabled(),
                "indexed", indexed,
                "indexedAt", indexedAt == null ? null : indexedAt.toString(),
                "documentCount", documents.size(),
                "chunkCount", chunks.size(),
                "retrievalMode", "multi-signal business knowledge retrieval"
        );
    }

    private void ensureIndexed() {
        if (indexed) {
            return;
        }

        documents.clear();
        chunks.clear();

        if (!aiProperties.getRag().isEnabled()) {
            indexed = true;
            indexedAt = LocalDateTime.now();
            return;
        }

        List<String> configuredPaths = aiProperties.getRag().getSourcePaths();
        if (configuredPaths == null || configuredPaths.isEmpty()) {
            configuredPaths = defaultSourcePaths();
        }

        int priority = 0;
        for (String configuredPath : configuredPaths) {
            Path resolved = resolvePath(configuredPath);
            if (resolved == null || !Files.exists(resolved)) {
                continue;
            }
            try {
                String content = new String(Files.readAllBytes(resolved), StandardCharsets.UTF_8);
                Document document = Document.from(content);
                String normalizedText = document.text();
                SourceProfile profile = buildProfile(resolved);
                KnowledgeDocument knowledgeDocument = new KnowledgeDocument(
                        profile.uri,
                        resolved.getFileName().toString(),
                        resolved.toString(),
                        normalizedText,
                        profile.domain,
                        profile.tags,
                        priority
                );
                documents.add(knowledgeDocument);

                List<TextSegment> splitSegments = split(normalizedText);
                int segmentLimit = Math.min(splitSegments.size(), aiProperties.getRag().getMaxSegmentsPerSource());
                for (int i = 0; i < segmentLimit; i++) {
                    String chunkText = splitSegments.get(i).text();
                    chunks.add(new KnowledgeChunk(
                            knowledgeDocument.uri,
                            knowledgeDocument.title,
                            knowledgeDocument.path,
                            knowledgeDocument.domain,
                            knowledgeDocument.tags,
                            chunkText,
                            i,
                            knowledgeDocument.priority,
                            normalize(chunkText),
                            extractTerms(chunkText)
                    ));
                }
                priority++;
            } catch (IOException ignored) {
                // Keep other documents available even if one source cannot be loaded.
            }
        }

        indexed = true;
        indexedAt = LocalDateTime.now();
    }

    private List<String> defaultSourcePaths() {
        List<String> defaults = new ArrayList<>();
        defaults.add("docs/agent.md");
        defaults.add("docs/SKY_TAKE_OUT_FULL_PROJECT_README.md");
        defaults.add("docs/PROJECT_DEVELOPMENT_LOG.md");
        defaults.add("backend/database/数据库设计文档.md");
        return defaults;
    }

    private Path resolvePath(String configuredPath) {
        if (!StringUtils.hasText(configuredPath)) {
            return null;
        }
        Path direct = Paths.get(configuredPath).normalize();
        if (Files.exists(direct)) {
            return direct.toAbsolutePath().normalize();
        }

        String[] prefixes = new String[]{"", ".", "..", "../..", "../../.."};
        for (String prefix : prefixes) {
            Path candidate = StringUtils.hasText(prefix)
                    ? Paths.get(prefix, configuredPath).normalize()
                    : Paths.get(configuredPath).normalize();
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private SourceProfile buildProfile(Path resolved) {
        String fileName = resolved.getFileName().toString().toLowerCase(Locale.ROOT);
        String path = resolved.toString().toLowerCase(Locale.ROOT);
        String domain = "operations";
        Set<String> tags = new LinkedHashSet<>();

        if (fileName.contains("log")) {
            domain = "delivery-history";
            tags.add("change-log");
            tags.add("project-progress");
        } else if (fileName.contains("readme")) {
            domain = "architecture";
            tags.add("architecture");
            tags.add("capability-boundary");
        } else if (fileName.contains("agent")) {
            domain = "ai-operations";
            tags.add("ai-workflow");
            tags.add("tool-boundary");
        } else if (path.contains("database")) {
            domain = "data-design";
            tags.add("database-design");
            tags.add("schema");
        }

        String uri = "ops-doc://" + slug(resolved.getFileName().toString());
        return new SourceProfile(uri, domain, new ArrayList<>(tags));
    }

    private List<TextSegment> split(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }

        List<TextSegment> result = new ArrayList<>();
        int maxLength = Math.max(200, aiProperties.getRag().getMaxSegmentLength());
        int overlap = Math.max(0, Math.min(aiProperties.getRag().getMaxOverlap(), maxLength / 2));

        String[] paragraphs = text.split("\\r?\\n\\s*\\r?\\n");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String normalizedParagraph = paragraph == null ? "" : paragraph.trim();
            if (!StringUtils.hasText(normalizedParagraph)) {
                continue;
            }
            if (current.length() > 0 && current.length() + normalizedParagraph.length() + 2 > maxLength) {
                appendChunk(result, current.toString());
                current = new StringBuilder(overlapTail(current.toString(), overlap));
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(normalizedParagraph);
            while (current.length() > maxLength) {
                String source = current.toString();
                appendChunk(result, source.substring(0, maxLength));
                current = new StringBuilder(source.substring(Math.max(0, maxLength - overlap)).trim());
            }
        }

        appendChunk(result, current.toString());
        return result;
    }

    private void appendChunk(List<TextSegment> result, String chunk) {
        String normalizedChunk = chunk == null ? "" : chunk.trim();
        if (StringUtils.hasText(normalizedChunk)) {
            result.add(TextSegment.from(normalizedChunk));
        }
    }

    private String overlapTail(String source, int overlap) {
        if (!StringUtils.hasText(source) || overlap <= 0) {
            return "";
        }
        int begin = Math.max(0, source.length() - overlap);
        return source.substring(begin).trim();
    }

    private int score(KnowledgeChunk chunk, String rawQuery, List<String> queryTerms) {
        if (!StringUtils.hasText(rawQuery)) {
            return 0;
        }
        int score = 0;
        String normalizedQuery = normalize(rawQuery);
        String normalizedTitle = normalize(chunk.title);
        String normalizedPath = normalize(chunk.path);

        if (chunk.normalizedContent.contains(normalizedQuery)) {
            score += 160;
        }
        if (normalizedTitle.contains(normalizedQuery)) {
            score += 120;
        }
        if (normalizedPath.contains(normalizedQuery)) {
            score += 60;
        }

        int overlapCount = 0;
        for (String term : queryTerms) {
            if (!StringUtils.hasText(term)) {
                continue;
            }
            if (chunk.normalizedContent.contains(term)) {
                score += 18;
                overlapCount++;
            }
            if (normalizedTitle.contains(term)) {
                score += 12;
            }
            if (containsAny(chunk.tags, term)) {
                score += 10;
            }
            if (normalize(chunk.domain).contains(term)) {
                score += 8;
            }
        }

        if (overlapCount >= 2) {
            score += 20;
        }
        score += Math.max(0, 12 - chunk.chunkIndex);
        score += Math.max(0, 6 - chunk.priority);
        return score;
    }

    private boolean containsAny(List<String> tags, String term) {
        if (tags == null || tags.isEmpty()) {
            return false;
        }
        for (String tag : tags) {
            if (normalize(tag).contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String excerpt(String content, String query) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalizedContent = normalize(content);
        String normalizedQuery = normalize(query);
        int index = normalizedContent.indexOf(normalizedQuery);
        if (index < 0) {
            return content.length() > 260 ? content.substring(0, 260) : content;
        }
        int start = Math.max(0, index - 100);
        int end = Math.min(content.length(), index + 220);
        return content.substring(start, end).trim();
    }

    private List<String> extractTerms(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }

        String normalized = normalize(text);
        Set<String> terms = new LinkedHashSet<>();

        Matcher latinMatcher = LATIN_TOKEN_PATTERN.matcher(normalized);
        while (latinMatcher.find()) {
            terms.add(latinMatcher.group());
        }

        Matcher cjkMatcher = CJK_TOKEN_PATTERN.matcher(normalized);
        while (cjkMatcher.find()) {
            String token = cjkMatcher.group();
            terms.add(token);
            for (int i = 0; i < token.length() - 1; i++) {
                terms.add(token.substring(i, i + 2));
            }
            if (token.length() > 2) {
                for (int i = 0; i < token.length() - 2; i++) {
                    terms.add(token.substring(i, i + 3));
                }
            }
        }

        return new ArrayList<>(terms);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('\n', ' ').replace('\r', ' ').trim();
    }

    private String slug(String value) {
        String base = value == null ? "resource" : value.toLowerCase(Locale.ROOT);
        base = base.replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-");
        base = base.replaceAll("(^-+|-+$)", "");
        return StringUtils.hasText(base) ? base : "resource";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    private static class SourceProfile {
        private final String uri;
        private final String domain;
        private final List<String> tags;

        private SourceProfile(String uri, String domain, List<String> tags) {
            this.uri = uri;
            this.domain = domain;
            this.tags = tags;
        }
    }

    private static class KnowledgeDocument {
        private final String uri;
        private final String title;
        private final String path;
        private final String content;
        private final String domain;
        private final List<String> tags;
        private final int priority;

        private KnowledgeDocument(String uri, String title, String path, String content, String domain, List<String> tags, int priority) {
            this.uri = uri;
            this.title = title;
            this.path = path;
            this.content = content;
            this.domain = domain;
            this.tags = tags;
            this.priority = priority;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("uri", uri);
            result.put("title", title);
            result.put("path", path);
            result.put("domain", domain);
            result.put("tags", tags);
            result.put("length", content == null ? 0 : content.length());
            return result;
        }
    }

    private static class KnowledgeChunk {
        private final String uri;
        private final String title;
        private final String path;
        private final String domain;
        private final List<String> tags;
        private final String content;
        private final int chunkIndex;
        private final int priority;
        private final String normalizedContent;
        @SuppressWarnings("unused")
        private final List<String> terms;

        private KnowledgeChunk(String uri, String title, String path, String domain, List<String> tags,
                               String content, int chunkIndex, int priority, String normalizedContent, List<String> terms) {
            this.uri = uri;
            this.title = title;
            this.path = path;
            this.domain = domain;
            this.tags = tags;
            this.content = content;
            this.chunkIndex = chunkIndex;
            this.priority = priority;
            this.normalizedContent = normalizedContent;
            this.terms = terms;
        }
    }

    private static class RankedChunk {
        private final KnowledgeChunk chunk;
        private final int score;

        private RankedChunk(KnowledgeChunk chunk, int score) {
            this.chunk = chunk;
            this.score = score;
        }

        private int getScore() {
            return score;
        }

        private int getPriority() {
            return chunk.priority;
        }
    }
}
