package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.MeituanCaptureStatus;
import cn.edu.cn.javadiet.model.dto.MeituanCapturedShop;
import cn.edu.cn.javadiet.model.dto.MeituanCrawlResult;
import cn.edu.cn.javadiet.model.entity.MeituanCrawlTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class MeituanSignedRequestCaptureService {

    private static final List<String> TARGET_ENDPOINTS = List.of(
            "/openh5/v2/poi/food",
            "/openh5/v2/poi/menuproducts",
            "/openh5/v2/poi/smooth/render",
            "/openh5/poi/info",
            "/openh5/search/globalpage");
    private static final String DOM_MENU_ENDPOINT = "__dom_menu_snapshot__";
    private static final Set<String> NETWORK_MENU_ENDPOINTS = Set.of(
            "/openh5/v2/poi/food",
            "/openh5/v2/poi/menuproducts",
            "/openh5/v2/poi/smooth/render",
            "/openh5/poi/info");
    private static final Set<String> MENU_ENDPOINTS = Set.of(
            "/openh5/v2/poi/food",
            "/openh5/v2/poi/menuproducts",
            "/openh5/v2/poi/smooth/render",
            "/openh5/poi/info",
            DOM_MENU_ENDPOINT);
    private static final String UNKNOWN_CAPTURE_KEY = "unknown";
    private static final String DOM_SNAPSHOT_SCRIPT = """
            new Promise((resolve) => {
              setTimeout(() => {
                const clean = (value) => (value || '').replace(/\\s+/g, ' ').trim();
                const textOf = (el) => clean(el && (el.innerText || el.textContent));
                const visible = (el) => {
                  if (!el) return false;
                  const style = window.getComputedStyle(el);
                  const rect = el.getBoundingClientRect();
                  return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0;
                };
                const texts = (selector, root = document) => Array.from(root.querySelectorAll(selector))
                  .filter(visible)
                  .map(textOf)
                  .filter(Boolean);
                const parseMoney = (value) => {
                  const match = clean(value).match(/(?:¥|￥)?\\s*(\\d+(?:\\.\\d+)?)/);
                  return match ? match[1] : null;
                };
                const badRestaurantName = (value) => !value
                  || value.length > 80
                  || /^(折扣|热销|菜单|评价|商家|新品|美团外卖)$/.test(value)
                  || /月售|¥|￥|\\d+(?:\\.\\d+)?折/.test(value);
                const restaurantName = (() => {
                  const selectors = [
                    'div[class*="title_"][class*="ellipsis"]',
                    '[class*="shopTitle"]',
                    '[class*="poiName"]',
                    '[class*="restaurantName"]',
                    'h1'
                  ];
                  const candidates = selectors.flatMap((selector) => Array.from(document.querySelectorAll(selector)))
                    .filter(visible)
                    .filter((el) => !el.closest('[data-tag="spu"], div[class*="spu_"]'))
                    .map((el) => ({
                      text: textOf(el),
                      top: el.getBoundingClientRect().top
                    }))
                    .filter((item) => !badRestaurantName(item.text))
                    .sort((left, right) => left.top - right.top);
                  const candidate = candidates.find((item) => /[\\u4e00-\\u9fa5]/.test(item.text)) || candidates[0];
                  return candidate ? candidate.text : null;
                })();
                const pickCategory = (card) => {
                  const section = card.closest('dl,[data-tag="scroll-anchor"],[data-tag="spu-category"]') || card.parentElement;
                  const value = section && texts('dt[data-tag="title"], [data-tag="title"], [class*="titleBar"]', section)[0];
                  return value && !badRestaurantName(value) ? value : null;
                };
                const pickName = (card) => {
                  const reject = (value) => !value
                    || value.length > 60
                    || /^(新品|招牌|热销|折扣|可售|不可售)$/.test(value)
                    || /月售|¥|￥|\\d+(?:\\.\\d+)?折|精选|口感|套餐包含|不含蘸酱/.test(value);
                  const candidates = Array.from(card.querySelectorAll('[data-tag="spu-name"],[data-tag="name"],[class*="name_"],[class*="title_"],div,span'))
                    .filter(visible)
                    .map((el) => {
                      const style = window.getComputedStyle(el);
                      return {
                        text: textOf(el),
                        fontSize: parseFloat(style.fontSize || '0') || 0,
                        fontWeight: parseInt(style.fontWeight || '400', 10) || 400
                      };
                    })
                    .filter((item) => !reject(item.text));
                  candidates.sort((left, right) => (right.fontSize - left.fontSize) || (right.fontWeight - left.fontWeight));
                  return candidates[0] ? candidates[0].text : null;
                };
                const pickPrice = (card) => {
                  const priceRoot = card.querySelector('[data-tag="price"]') || card.querySelector('[class*="price"]') || card;
                  const originalEls = Array.from(priceRoot.querySelectorAll('[class*="oprice"],[class*="origin"],[class*="original"]'));
                  const originalPrice = parseMoney(originalEls.map(textOf).join(' '));
                  let currentText = textOf(priceRoot);
                  originalEls.forEach((el) => {
                    currentText = currentText.replace(textOf(el), ' ');
                  });
                  return {
                    price: parseMoney(currentText) || parseMoney(textOf(priceRoot)),
                    originalPrice
                  };
                };
                const cards = Array.from(document.querySelectorAll('[data-tag="spu"], div[class*="spu_"]'))
                  .filter(visible);
                const seen = new Set();
                const items = cards.map((card) => {
                  const name = pickName(card);
                  if (!name || seen.has(name)) return null;
                  seen.add(name);
                  const price = pickPrice(card);
                  const description = texts('[class*="desc_"],[data-tag="desc"],[class*="spuDesc"]', card)[0] || null;
                  return {
                    source: 'dom',
                    name,
                    category: pickCategory(card),
                    description,
                    price: price.price,
                    originalPrice: price.originalPrice,
                    discountText: texts('[class*="discount"],[class*="tag"]', card).find((value) => /折/.test(value)) || null
                  };
                }).filter(Boolean);
                let poiId = null;
                try {
                  poiId = new URL(location.href).searchParams.get('poi_id_str');
                } catch (error) {
                  poiId = null;
                }
                resolve(JSON.stringify({
                  source: 'MEITUAN_DOM_SNAPSHOT',
                  poiId,
                  restaurantName,
                  items
                }));
              }, 350);
            })
            """;
    private static final Set<String> SKIPPED_REPLAY_HEADERS = Set.of(
            "accept-encoding",
            "connection",
            "content-length",
            "host",
            "origin-trial",
            "proxy-connection",
            "transfer-encoding",
            "upgrade");

    private final MeituanBrowserSessionService browserSessionService;
    private final MeituanMenuParser parser;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final AtomicBoolean listening = new AtomicBoolean(false);
    private final AtomicInteger commandId = new AtomicInteger(1);
    private final Map<Integer, CompletableFuture<JsonNode>> pendingCommands = new ConcurrentHashMap<>();
    private final Map<String, CapturedExchange> inflight = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<CapturedExchange> captured = new CopyOnWriteArrayList<>();

    private volatile WebSocket webSocket;
    private volatile String debuggerAddress;
    private volatile LocalDateTime startedAt;
    private volatile String message = "尚未开始监听";

    public MeituanSignedRequestCaptureService(
            MeituanBrowserSessionService browserSessionService,
            MeituanMenuParser parser) {
        this.browserSessionService = browserSessionService;
        this.parser = parser;
    }

    public synchronized MeituanCaptureStatus startCapture() {
        if (listening.get()) {
            return getStatus("正在监听美团接口请求");
        }
        Optional<String> activeAddress = browserSessionService.getActiveDebuggerAddress();
        if (activeAddress.isEmpty()) {
            message = "养号浏览器未连接，请先打开养号浏览器";
            return getStatus(message);
        }
        try {
            debuggerAddress = activeAddress.get();
            String pageWebSocketUrl = findInspectablePage(debuggerAddress);
            webSocket = httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(URI.create(pageWebSocketUrl), new CdpListener())
                    .join();
            listening.set(true);
            startedAt = LocalDateTime.now();
            captured.clear();
            inflight.clear();
            sendCommand("Network.enable", Map.of(
                    "maxTotalBufferSize", 100000000,
                    "maxResourceBufferSize", 10000000))
                    .orTimeout(5, TimeUnit.SECONDS)
                    .join();
            message = "正在监听，请在养号浏览器里手动搜索并打开店铺菜单";
            return getStatus(message);
        } catch (Exception exception) {
            listening.set(false);
            closeSocket();
            message = "无法开始监听：" + rootMessage(exception);
            return getStatus(message);
        }
    }

    public synchronized MeituanCaptureStatus stopCapture() {
        listening.set(false);
        closeSocket();
        message = "监听已停止";
        return getStatus(message);
    }

    public MeituanCaptureStatus getStatus() {
        return getStatus(message);
    }

    public List<String> getCapturedMenuResponseBodies() {
        return getCapturedMenuResponseBodies(null);
    }

    public List<String> getCapturedMenuResponseBodies(String captureKey) {
        String key = captureKey == null || captureKey.isBlank() ? latestCapturedShopKey() : captureKey;
        if (key == null) {
            return List.of();
        }
        return captured.stream()
                .filter(item -> MENU_ENDPOINTS.contains(item.endpoint()))
                .filter(item -> key.equals(captureKey(item)))
                .map(CapturedExchange::responseBody)
                .filter(body -> body != null && !body.isBlank())
                .toList();
    }

    public MeituanCrawlResult previewCapturedMenu() {
        return previewCapturedMenu(null);
    }

    public MeituanCrawlResult previewCapturedMenu(String captureKey) {
        String key = captureKey == null || captureKey.isBlank() ? latestCapturedShopKey() : captureKey;
        MeituanCrawlResult result = parser.parse(getCapturedMenuResponseBodies(key));
        if (result.getMeituanPoiId() == null && key != null && !UNKNOWN_CAPTURE_KEY.equals(key)) {
            result.setMeituanPoiId(key);
        }
        return result;
    }

    public List<MeituanCapturedShop> listCapturedShops() {
        return groupCapturedMenuExchanges().entrySet().stream()
                .map(entry -> toCapturedShop(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> nullSafeLatest(right).compareTo(nullSafeLatest(left)))
                .toList();
    }

    public void deleteCapturedShop(String captureKey) {
        if (captureKey == null || captureKey.isBlank()) {
            return;
        }
        captured.removeIf(exchange -> captureKey.equals(captureKey(exchange)));
    }

    public List<String> crawlWithLatestTemplate(MeituanCrawlTask task) {
        List<CapturedExchange> templates = latestMenuTemplates();
        if (templates.isEmpty()) {
            throw new IllegalStateException("没有可用的美团签名模板，请先开始监听并手动打开一次店铺菜单");
        }
        List<String> bodies = new ArrayList<>();
        for (CapturedExchange template : templates) {
            String body = replay(template, task);
            if (body != null && !body.isBlank()) {
                bodies.add(body);
            }
        }
        if (bodies.isEmpty()) {
            throw new IllegalStateException("签名模板请求没有返回菜单数据，请重新人工采集模板");
        }
        MeituanCrawlResult replayResult = parser.parse(bodies);
        if (replayResult.getItems().isEmpty()) {
            throw new IllegalStateException(
                    "签名模板重放后没有解析到菜品。通常是美团签名不允许跨店铺复用，"
                            + "请手动打开目标店铺菜单后使用“导入最近捕获菜单”。");
        }
        return bodies;
    }

    private MeituanCaptureStatus getStatus(String statusMessage) {
        String latestCaptureKey = latestCapturedShopKey();
        List<String> menuBodies = getCapturedMenuResponseBodies(latestCaptureKey);
        MeituanCrawlResult result = parser.parse(menuBodies);
        CapturedExchange latest = captured.isEmpty() ? null : captured.get(captured.size() - 1);
        boolean connected = browserSessionService.getActiveDebuggerAddress().isPresent();
        return MeituanCaptureStatus.builder()
                .connected(connected)
                .listening(listening.get())
                .templateReady(!latestMenuTemplates().isEmpty())
                .debuggerAddress(debuggerAddress)
                .capturedCount(captured.size())
                .capturedShopCount(groupCapturedMenuExchanges().size())
                .menuResponseCount((int) captured.stream()
                        .filter(item -> NETWORK_MENU_ENDPOINTS.contains(item.endpoint()))
                        .count())
                .searchResponseCount((int) captured.stream()
                        .filter(item -> "/openh5/search/globalpage".equals(item.endpoint()))
                        .count())
                .latestItemCount(result.getItems() == null ? 0 : result.getItems().size())
                .latestEndpoint(latest == null ? null : latest.endpoint())
                .latestRestaurantName(result.getRestaurantName())
                .latestPoiId(result.getMeituanPoiId())
                .startedAt(startedAt)
                .latestCapturedAt(latest == null ? null : latest.capturedAt())
                .message(statusMessage)
                .build();
    }

    private String findInspectablePage(String address) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + address + "/json/list"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Chrome 调试端口不可用：" + response.statusCode());
        }
        JsonNode pages = objectMapper.readTree(response.body());
        JsonNode fallback = null;
        for (JsonNode page : pages) {
            if (!"page".equals(page.path("type").asText())) {
                continue;
            }
            if (fallback == null) {
                fallback = page;
            }
            String url = page.path("url").asText("");
            if (url.contains("h5.waimai.meituan.com")) {
                return page.path("webSocketDebuggerUrl").asText();
            }
        }
        if (fallback != null) {
            return fallback.path("webSocketDebuggerUrl").asText();
        }
        throw new IllegalStateException("没有找到可监听的 Chrome 标签页");
    }

    private CompletableFuture<JsonNode> sendCommand(String method, Map<String, ?> params) {
        WebSocket socket = webSocket;
        if (socket == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("监听连接未建立"));
        }
        int id = commandId.getAndIncrement();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingCommands.put(id, future);
        ObjectNode command = objectMapper.createObjectNode();
        command.put("id", id);
        command.put("method", method);
        if (params != null && !params.isEmpty()) {
            command.set("params", objectMapper.valueToTree(params));
        }
        try {
            socket.sendText(objectMapper.writeValueAsString(command), true);
        } catch (Exception exception) {
            pendingCommands.remove(id);
            future.completeExceptionally(exception);
        }
        return future;
    }

    private void handleCdpMessage(String rawMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            if (root.has("id")) {
                CompletableFuture<JsonNode> future = pendingCommands.remove(root.path("id").asInt());
                if (future != null) {
                    if (root.has("error")) {
                        future.completeExceptionally(new IllegalStateException(root.path("error").toString()));
                    } else {
                        future.complete(root);
                    }
                }
                return;
            }
            String method = root.path("method").asText();
            JsonNode params = root.path("params");
            if ("Network.requestWillBeSent".equals(method)) {
                onRequestWillBeSent(params);
            } else if ("Network.responseReceived".equals(method)) {
                onResponseReceived(params);
            } else if ("Network.loadingFinished".equals(method)) {
                onLoadingFinished(params);
            }
        } catch (Exception exception) {
            message = "监听消息解析失败：" + rootMessage(exception);
        }
    }

    private void onRequestWillBeSent(JsonNode params) {
        String requestId = params.path("requestId").asText();
        JsonNode request = params.path("request");
        String url = request.path("url").asText();
        String endpoint = endpointFromUrl(url);
        if (endpoint == null) {
            return;
        }
        CapturedExchange exchange = inflight.computeIfAbsent(requestId, ignored -> new CapturedExchange(requestId));
        exchange.endpoint(endpoint);
        exchange.url(url);
        exchange.method(request.path("method").asText("GET"));
        exchange.postData(request.path("postData").asText(null));
        exchange.headers(readHeaders(request.path("headers")));
    }

    private void onResponseReceived(JsonNode params) {
        String requestId = params.path("requestId").asText();
        JsonNode response = params.path("response");
        String url = response.path("url").asText();
        String endpoint = endpointFromUrl(url);
        if (endpoint == null && !inflight.containsKey(requestId)) {
            return;
        }
        CapturedExchange exchange = inflight.computeIfAbsent(requestId, ignored -> new CapturedExchange(requestId));
        if (endpoint != null) {
            exchange.endpoint(endpoint);
            exchange.url(url);
        }
        exchange.status(response.path("status").asInt());
    }

    private void onLoadingFinished(JsonNode params) {
        String requestId = params.path("requestId").asText();
        CapturedExchange exchange = inflight.get(requestId);
        if (exchange == null || exchange.endpoint() == null) {
            return;
        }
        sendCommand("Network.getResponseBody", Map.of("requestId", requestId))
                .thenAccept(response -> storeResponseBody(exchange, response))
                .exceptionally(exception -> {
                    message = "读取美团接口响应失败：" + rootMessage(exception);
                    return null;
                });
    }

    private void storeResponseBody(CapturedExchange exchange, JsonNode response) {
        String body = response.path("result").path("body").asText("");
        if (response.path("result").path("base64Encoded").asBoolean(false)) {
            body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }
        if (body.isBlank()) {
            return;
        }
        exchange.responseBody(body);
        exchange.poiId(extractPoiId(exchange.url(), exchange.postData(), body));
        exchange.capturedAt(LocalDateTime.now());
        captured.add(exchange.copy());
        while (captured.size() > 80) {
            captured.remove(0);
        }
        captureDomSnapshot(exchange.copy());
        message = "已捕获 " + exchange.endpoint();
    }

    private void captureDomSnapshot(CapturedExchange sourceExchange) {
        if (!NETWORK_MENU_ENDPOINTS.contains(sourceExchange.endpoint())) {
            return;
        }
        CompletableFuture.supplyAsync(() -> null, CompletableFuture.delayedExecutor(350, TimeUnit.MILLISECONDS))
                .thenCompose(ignored -> sendCommand("Runtime.evaluate", Map.of(
                        "expression", DOM_SNAPSHOT_SCRIPT,
                        "awaitPromise", true,
                        "returnByValue", true)))
                .orTimeout(3, TimeUnit.SECONDS)
                .thenAccept(response -> storeDomSnapshot(sourceExchange, response))
                .exceptionally(exception -> null);
    }

    private void storeDomSnapshot(CapturedExchange sourceExchange, JsonNode response) {
        String snapshotJson = response.path("result").path("result").path("value").asText("");
        if (snapshotJson.isBlank()) {
            return;
        }
        try {
            JsonNode snapshot = objectMapper.readTree(snapshotJson);
            if (!"MEITUAN_DOM_SNAPSHOT".equals(snapshot.path("source").asText())) {
                return;
            }
            boolean hasRestaurantName = !snapshot.path("restaurantName").asText("").isBlank();
            boolean hasItems = snapshot.path("items").isArray() && snapshot.path("items").size() > 0;
            if (!hasRestaurantName && !hasItems) {
                return;
            }
            CapturedExchange domExchange = new CapturedExchange(sourceExchange.requestId() + ":dom");
            domExchange.endpoint(DOM_MENU_ENDPOINT);
            domExchange.url(sourceExchange.url());
            domExchange.method("GET");
            domExchange.poiId(coalesce(
                    normalizePoiId(snapshot.path("poiId").asText(null)),
                    sourceExchange.poiId()));
            domExchange.responseBody(snapshotJson);
            domExchange.capturedAt(LocalDateTime.now());
            captured.add(domExchange);
            while (captured.size() > 80) {
                captured.remove(0);
            }
        } catch (Exception ignored) {
            // DOM snapshots are best-effort; signed API payloads remain the source of stable IDs.
        }
    }

    private String replay(CapturedExchange template, MeituanCrawlTask task) {
        try {
            String method = template.method() == null ? "GET" : template.method().toUpperCase(Locale.ROOT);
            String url = rewriteUrl(template.url(), task);
            String postData = rewriteForm(template.postData(), task);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20));
            for (Map.Entry<String, String> header : template.headers().entrySet()) {
                String name = header.getKey();
                String value = header.getValue();
                if (name == null || value == null || value.isBlank()) {
                    continue;
                }
                String normalized = name.toLowerCase(Locale.ROOT);
                if (name.startsWith(":") || SKIPPED_REPLAY_HEADERS.contains(normalized)) {
                    continue;
                }
                builder.header(name, value);
            }
            if ("POST".equals(method)) {
                builder.POST(HttpRequest.BodyPublishers.ofString(postData == null ? "" : postData));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new IllegalStateException("美团签名模板已失效或触发验证：" + response.statusCode());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("美团接口返回异常状态：" + response.statusCode());
            }
            return response.body();
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("请求美团签名模板失败：" + rootMessage(exception), exception);
        }
    }

    private List<CapturedExchange> latestMenuTemplates() {
        Map<String, CapturedExchange> byEndpoint = new LinkedHashMap<>();
        List<CapturedExchange> snapshot = new ArrayList<>(captured);
        Collections.reverse(snapshot);
        for (CapturedExchange exchange : snapshot) {
            if (NETWORK_MENU_ENDPOINTS.contains(exchange.endpoint())
                    && exchange.url() != null
                    && exchange.responseBody() != null
                    && !exchange.responseBody().isBlank()) {
                byEndpoint.putIfAbsent(exchange.endpoint(), exchange);
            }
        }
        List<CapturedExchange> templates = new ArrayList<>(byEndpoint.values());
        Collections.reverse(templates);
        return templates;
    }

    private Map<String, List<CapturedExchange>> groupCapturedMenuExchanges() {
        Map<String, List<CapturedExchange>> groups = new LinkedHashMap<>();
        for (CapturedExchange exchange : captured) {
            if (!MENU_ENDPOINTS.contains(exchange.endpoint()) || exchange.responseBody() == null
                    || exchange.responseBody().isBlank()) {
                continue;
            }
            groups.computeIfAbsent(captureKey(exchange), ignored -> new ArrayList<>()).add(exchange);
        }
        return groups;
    }

    private MeituanCapturedShop toCapturedShop(String captureKey, List<CapturedExchange> exchanges) {
        List<String> bodies = exchanges.stream()
                .map(CapturedExchange::responseBody)
                .filter(body -> body != null && !body.isBlank())
                .toList();
        MeituanCrawlResult result = parser.parse(bodies);
        CapturedExchange latest = exchanges.isEmpty() ? null : exchanges.get(exchanges.size() - 1);
        String poiId = result.getMeituanPoiId();
        if (poiId == null && !UNKNOWN_CAPTURE_KEY.equals(captureKey)) {
            poiId = captureKey;
        }
        int itemCount = result.getItems() == null ? 0 : result.getItems().size();
        return MeituanCapturedShop.builder()
                .captureKey(captureKey)
                .meituanPoiId(poiId)
                .restaurantName(result.getRestaurantName())
                .address(result.getAddress())
                .endpointCount(exchanges.size())
                .itemCount(itemCount)
                .missingNutritionCount(itemCount)
                .latestEndpoint(latest == null ? null : latest.endpoint())
                .latestCapturedAt(latest == null ? null : latest.capturedAt())
                .build();
    }

    private LocalDateTime nullSafeLatest(MeituanCapturedShop shop) {
        return shop.getLatestCapturedAt() == null ? LocalDateTime.MIN : shop.getLatestCapturedAt();
    }

    private String latestCapturedShopKey() {
        List<CapturedExchange> snapshot = new ArrayList<>(captured);
        Collections.reverse(snapshot);
        return snapshot.stream()
                .filter(exchange -> MENU_ENDPOINTS.contains(exchange.endpoint()))
                .filter(exchange -> exchange.responseBody() != null && !exchange.responseBody().isBlank())
                .map(this::captureKey)
                .findFirst()
                .orElse(null);
    }

    private String captureKey(CapturedExchange exchange) {
        String poiId = trimToNull(exchange.poiId());
        if (poiId == null) {
            poiId = extractPoiId(exchange.url(), exchange.postData(), exchange.responseBody());
        }
        return poiId == null ? UNKNOWN_CAPTURE_KEY : poiId;
    }

    private String extractPoiId(String url, String postData, String body) {
        String poiId = extractPoiIdFromUrl(url);
        if (poiId != null) {
            return poiId;
        }
        poiId = extractPoiIdFromForm(postData);
        if (poiId != null) {
            return poiId;
        }
        return extractPoiIdFromBody(body);
    }

    private static String extractPoiIdFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        int queryStart = url.indexOf('?');
        return queryStart < 0 ? null : extractPoiIdFromForm(url.substring(queryStart + 1));
    }

    private static String extractPoiIdFromForm(String form) {
        if (form == null || form.isBlank()) {
            return null;
        }
        for (String part : form.split("&", -1)) {
            String[] pair = part.split("=", 2);
            if (pair.length < 2) {
                continue;
            }
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            if (!List.of("poi_id_str", "poiIdStr", "wm_poi_id", "poi_id").contains(key)) {
                continue;
            }
            String value = normalizePoiId(URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String extractPoiIdFromBody(String body) {
        if (body == null || body.isBlank() || !body.stripLeading().startsWith("{")) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            return normalizePoiId(findFirstText(root, "poi_id_str", "poiIdStr", "wm_poi_id"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String findFirstText(JsonNode node, String... keys) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            for (String key : keys) {
                JsonNode value = node.get(key);
                if (value != null && !value.isNull()) {
                    String text = value.asText();
                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
            Iterator<JsonNode> values = node.elements();
            while (values.hasNext()) {
                String nested = findFirstText(values.next(), keys);
                if (nested != null) {
                    return nested;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                String nested = findFirstText(child, keys);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static String normalizePoiId(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || "-100".equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> T coalesce(T first, T second) {
        return first != null ? first : second;
    }

    private static Map<String, String> readHeaders(JsonNode node) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return headers;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            headers.put(field.getKey(), field.getValue().asText());
        }
        return headers;
    }

    private static String endpointFromUrl(String url) {
        if (url == null) {
            return null;
        }
        return TARGET_ENDPOINTS.stream().filter(url::contains).findFirst().orElse(null);
    }

    private static String rewriteUrl(String url, MeituanCrawlTask task) {
        if (url == null || task == null || task.getMeituanPoiId() == null || task.getMeituanPoiId().isBlank()) {
            return url;
        }
        int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return url;
        }
        return url.substring(0, queryStart + 1) + rewriteForm(url.substring(queryStart + 1), task);
    }

    private static String rewriteForm(String form, MeituanCrawlTask task) {
        if (form == null || form.isBlank() || task == null) {
            return form;
        }
        Map<String, String> replacements = replacementValues(task);
        if (replacements.isEmpty()) {
            return form;
        }
        String[] parts = form.split("&", -1);
        List<String> rewritten = new ArrayList<>(parts.length);
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            String key = pair.length > 0 ? pair[0] : "";
            String decodedKey = java.net.URLDecoder.decode(key, StandardCharsets.UTF_8);
            if (replacements.containsKey(decodedKey)) {
                rewritten.add(key + "=" + encode(replacements.get(decodedKey)));
            } else {
                rewritten.add(part);
            }
        }
        return String.join("&", rewritten);
    }

    private static Map<String, String> replacementValues(MeituanCrawlTask task) {
        Map<String, String> values = new HashMap<>();
        if (task.getMeituanPoiId() != null && !task.getMeituanPoiId().isBlank()) {
            values.put("poi_id_str", task.getMeituanPoiId().trim());
        }
        if (task.getLatitude() != null) {
            String lat = task.getLatitude().toString();
            values.put("lat", lat);
            values.put("gpsLat", lat);
            values.put("actualLat", lat);
            values.put("initialLat", lat);
        }
        if (task.getLongitude() != null) {
            String lng = task.getLongitude().toString();
            values.put("lng", lng);
            values.put("gpsLng", lng);
            values.put("actualLng", lng);
            values.put("initialLng", lng);
        }
        return values;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private void closeSocket() {
        WebSocket socket = webSocket;
        webSocket = null;
        if (socket != null) {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "stop");
            } catch (RuntimeException ignored) {
            }
        }
        pendingCommands.values().forEach(future -> future.cancel(true));
        pendingCommands.clear();
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private final class CdpListener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String rawMessage = buffer.toString();
                buffer.setLength(0);
                handleCdpMessage(rawMessage);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            listening.set(false);
            message = "监听连接已关闭";
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            listening.set(false);
            message = "监听连接异常：" + rootMessage(error);
        }
    }

    private static final class CapturedExchange {
        private final String requestId;
        private String endpoint;
        private String url;
        private String method = "GET";
        private Map<String, String> headers = Map.of();
        private String postData;
        private int status;
        private String poiId;
        private String responseBody;
        private LocalDateTime capturedAt;

        private CapturedExchange(String requestId) {
            this.requestId = requestId;
        }

        private CapturedExchange copy() {
            CapturedExchange copy = new CapturedExchange(requestId);
            copy.endpoint = endpoint;
            copy.url = url;
            copy.method = method;
            copy.headers = new LinkedHashMap<>(headers);
            copy.postData = postData;
            copy.status = status;
            copy.poiId = poiId;
            copy.responseBody = responseBody;
            copy.capturedAt = capturedAt;
            return copy;
        }

        private String requestId() {
            return requestId;
        }

        private String endpoint() {
            return endpoint;
        }

        private void endpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        private String url() {
            return url;
        }

        private void url(String url) {
            this.url = url;
        }

        private String method() {
            return method;
        }

        private void method(String method) {
            this.method = method;
        }

        private Map<String, String> headers() {
            return headers == null ? Map.of() : headers;
        }

        private void headers(Map<String, String> headers) {
            this.headers = headers == null ? Map.of() : headers;
        }

        private String postData() {
            return postData;
        }

        private void postData(String postData) {
            this.postData = postData;
        }

        private void status(int status) {
            this.status = status;
        }

        private String poiId() {
            return poiId;
        }

        private void poiId(String poiId) {
            this.poiId = poiId;
        }

        private String responseBody() {
            return responseBody;
        }

        private void responseBody(String responseBody) {
            this.responseBody = responseBody;
        }

        private LocalDateTime capturedAt() {
            return capturedAt;
        }

        private void capturedAt(LocalDateTime capturedAt) {
            this.capturedAt = capturedAt;
        }
    }
}
