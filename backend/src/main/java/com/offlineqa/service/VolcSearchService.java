package com.offlineqa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class VolcSearchService {

    private final WebClient webClient;
    private final boolean enabled;
    private final String apiKey;
    private final String path;
    private final int topK;
    private final int timeoutMs;
    private final String searchType;
    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final String service;
    private final String action;
    private final String version;
    private final String host;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VolcSearchService(@Value("${app.volc.search-enabled:false}") boolean enabled,
                             @Value("${app.volc.base-url:https://open.feedcoopapi.com}") String baseUrl,
                             @Value("${app.volc.api-key:}") String apiKey,
                             @Value("${app.volc.path:/search}") String path,
                             @Value("${app.volc.top-k:3}") int topK,
                             @Value("${app.volc.timeout-ms:8000}") int timeoutMs,
                             @Value("${app.volc.search-type:web_summary}") String searchType,
                             @Value("${app.volc.access-key:}") String accessKey,
                             @Value("${app.volc.secret-key:}") String secretKey,
                             @Value("${app.volc.region:cn-beijing}") String region,
                             @Value("${app.volc.service:volc_torchlight_api}") String service,
                             @Value("${app.volc.action:WebSearch}") String action,
                             @Value("${app.volc.version:2025-01-01}") String version) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.path = path;
        this.topK = topK;
        this.timeoutMs = timeoutMs;
        this.searchType = (searchType == null || searchType.isBlank()) ? "web_summary" : searchType;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
        this.service = service;
        this.action = action;
        this.version = version;
        this.host = URI.create(baseUrl).getHost();
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public String factCheck(String question) {
        if (!enabled || question == null || question.isBlank()) {
            return "";
        }
        try {
            Map<?, ?> resp = rawSearch(question);
            if (resp == null) {
                return "";
            }
            return simplifyResponse(resp);
        } catch (Exception ex) {
            return "";
        }
    }

    public Map<String, Object> debugSearch(String question) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", enabled);
        out.put("path", path);
        out.put("topK", topK);
        out.put("timeoutMs", timeoutMs);
        out.put("searchType", searchType);
        out.put("authMode", hasAkSk() ? "AKSK_SIGN" : "API_KEY_BEARER");
        out.put("action", action);
        out.put("version", version);
        out.put("region", region);
        out.put("service", service);
        if (!enabled) {
            out.put("ok", false);
            out.put("error", "联网搜索未启用");
            return out;
        }
        if (!hasAkSk() && (apiKey == null || apiKey.isBlank())) {
            out.put("ok", false);
            out.put("error", "未配置可用鉴权（AK/SK 或 API Key）");
            return out;
        }
        try {
            Map<?, ?> resp = rawSearch(question);
            out.put("ok", true);
            out.put("summary", simplifyResponse(resp));
            out.put("raw", resp);
            return out;
        } catch (Exception e) {
            out.put("ok", false);
            out.put("error", e.getMessage());
            return out;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConfigured() {
        return hasAkSk() || (apiKey != null && !apiKey.isBlank());
    }

    private Map<?, ?> rawSearch(String question) {
        if (hasAkSk()) {
            return rawSearchWithAkSk(question);
        }
        return rawSearchWithBearer(question);
    }

    private Map<?, ?> rawSearchWithBearer(String question) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("VOLC_SEARCH_API_KEY 未配置，无法使用 Bearer 鉴权");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("Query", question);
        payload.put("SearchType", searchType);
        payload.put("Count", topK);
        payload.put("NeedSummary", true);
        String body = webClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + apiKey)
            .header("X-Traffic-Tag", "offlineqa_backend")
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorResume(ex -> Mono.error(new RuntimeException("VolcSearch调用失败(Bearer): " + ex.getMessage(), ex)))
            .block();
        return parseResponseBody(body);
    }

    private Map<?, ?> rawSearchWithAkSk(String question) {
        String payload = "{\"Query\":\"" + escapeJson(question) + "\",\"SearchType\":\"" + escapeJson(searchType) + "\",\"Count\":" + topK + ",\"NeedSummary\":true}";
        SignHeader signHeader = buildSignHeaders(payload);

        String body = webClient.post()
            .uri(uriBuilder -> uriBuilder.path(path).queryParam("Action", action).queryParam("Version", version).build())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Host", host)
            .header("X-Date", signHeader.xDate())
            .header("X-Content-Sha256", signHeader.payloadHash())
            .header("Authorization", signHeader.authorization())
            .header("X-Traffic-Tag", "offlineqa_backend")
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorResume(ex -> Mono.error(new RuntimeException("VolcSearch调用失败(AKSK): " + ex.getMessage(), ex)))
            .block();
        return parseResponseBody(body);
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> parseResponseBody(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        String candidate = body.trim();
        if (candidate.contains("data:")) {
            String[] lines = candidate.split("\\r?\\n");
            for (int i = lines.length - 1; i >= 0; i--) {
                String line = lines[i].trim();
                if (!line.startsWith("data:")) continue;
                String payload = line.substring(5).trim();
                if (payload.isBlank() || "[DONE]".equals(payload)) continue;
                candidate = payload;
                break;
            }
        }
        try {
            return objectMapper.readValue(candidate, Map.class);
        } catch (Exception ex) {
            return Map.of("raw", candidate);
        }
    }

    private SignHeader buildSignHeaders(String payload) {
        String xDate = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(java.time.ZoneOffset.UTC)
                .format(java.time.Instant.now());
        String shortDate = xDate.substring(0, 8);
        String payloadHash = sha256Hex(payload);
        String canonicalUri = path == null || path.isBlank() ? "/" : path;

        Map<String, String> queryMap = new TreeMap<>();
        queryMap.put("Action", action);
        queryMap.put("Version", version);
        String canonicalQuery = queryMap.entrySet().stream()
                .map(e -> uriEncode(e.getKey()) + "=" + uriEncode(e.getValue()))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        String canonicalHeaders = "content-type:application/json\n" +
                "host:" + host + "\n" +
                "x-content-sha256:" + payloadHash + "\n" +
                "x-date:" + xDate + "\n";
        String signedHeaders = "content-type;host;x-content-sha256;x-date";
        String canonicalRequest = "POST\n" + canonicalUri + "\n" + canonicalQuery + "\n" +
                canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;
        String credentialScope = shortDate + "/" + region + "/" + service + "/request";
        String stringToSign = "HMAC-SHA256\n" + xDate + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);

        byte[] signingKey = hmacSha256(("VOLC" + secretKey).getBytes(StandardCharsets.UTF_8), shortDate);
        signingKey = hmacSha256(signingKey, region);
        signingKey = hmacSha256(signingKey, service);
        signingKey = hmacSha256(signingKey, "request");
        String signature = bytesToHex(hmacSha256(signingKey, stringToSign));
        String authorization = "HMAC-SHA256 Credential=" + accessKey + "/" + credentialScope +
                ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
        return new SignHeader(xDate, payloadHash, authorization);
    }

    @SuppressWarnings("unchecked")
    private String simplifyResponse(Map<?, ?> resp) {
        if (resp == null || resp.isEmpty()) {
            return "";
        }
        Object results = resp.get("result");
        if (!(results instanceof List<?>)) {
            results = resp.get("Result");
        }
        if (results instanceof Map<?, ?> resultMap) {
            Object webResults = resultMap.get("WebResults");
            if (webResults instanceof List<?> list && !list.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        Object title = m.get("Title");
                        Object snippet = m.get("Snippet");
                        if (title != null || snippet != null) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append("- ");
                            if (title != null) sb.append(title);
                            if (snippet != null) sb.append("：").append(snippet);
                        }
                    }
                }
                if (sb.length() > 0) {
                    return sb.toString();
                }
            }
        }
        if (results instanceof List<?> list && !list.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Object title = m.get("title");
                    Object snippet = m.get("snippet");
                    if (title != null || snippet != null) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append("- ");
                        if (title != null) sb.append(title);
                        if (snippet != null) sb.append("：").append(snippet);
                    }
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return resp.toString();
    }

    private boolean hasAkSk() {
        return accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String uriEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    private static String sha256Hex(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return bytesToHex(md.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private record SignHeader(String xDate, String payloadHash, String authorization) {
    }

}
