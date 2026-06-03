package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.MeituanBrowserStatus;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MeituanBrowserSessionService {

    private static final String HOME_URL = "https://h5.waimai.meituan.com/waimai/mindex/home";

    private final String chromeBinaryPath;
    private final String chromeUserDataDir;
    private final String chromeProfileDirectory;
    private final String configuredDebuggerAddress;
    private final int remoteDebuggingPort;
    private final HttpClient httpClient;
    private final AtomicReference<Process> launchedProcess = new AtomicReference<>();

    public MeituanBrowserSessionService(
            @Value("${meituan.chrome.binary-path:${MEITUAN_CHROME_BINARY_PATH:}}") String chromeBinaryPath,
            @Value("${meituan.chrome.user-data-dir:${MEITUAN_CHROME_USER_DATA_DIR:}}") String chromeUserDataDir,
            @Value("${meituan.chrome.profile-directory:${MEITUAN_CHROME_PROFILE_DIRECTORY:Default}}")
                    String chromeProfileDirectory,
            @Value("${meituan.chrome.debugger-address:${MEITUAN_CHROME_DEBUGGER_ADDRESS:}}")
                    String configuredDebuggerAddress,
            @Value("${meituan.chrome.remote-debugging-port:${MEITUAN_CHROME_REMOTE_DEBUGGING_PORT:9222}}")
                    Integer remoteDebuggingPort) {
        this.chromeBinaryPath = chromeBinaryPath == null ? "" : chromeBinaryPath;
        this.chromeUserDataDir = chromeUserDataDir == null ? "" : chromeUserDataDir;
        this.chromeProfileDirectory = chromeProfileDirectory == null || chromeProfileDirectory.isBlank()
                ? "Default"
                : chromeProfileDirectory;
        this.configuredDebuggerAddress = configuredDebuggerAddress == null ? "" : configuredDebuggerAddress;
        this.remoteDebuggingPort = remoteDebuggingPort == null ? 9222 : remoteDebuggingPort;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public synchronized MeituanBrowserStatus openWarmBrowser() {
        String debuggerAddress = resolveDebuggerAddress();
        if (isDevToolsAvailable(debuggerAddress)) {
            return buildStatus(true, "养号浏览器已连接");
        }

        try {
            String chromePath = resolveChromePath();
            if (!chromeUserDataDir.isBlank()) {
                Files.createDirectories(Path.of(chromeUserDataDir));
            }

            List<String> command = new ArrayList<>();
            command.add(chromePath);
            command.add("--remote-debugging-port=" + remoteDebuggingPort);
            command.add("--remote-allow-origins=*");
            if (!chromeUserDataDir.isBlank()) {
                command.add("--user-data-dir=" + chromeUserDataDir);
            }
            command.add("--profile-directory=" + chromeProfileDirectory);
            command.add("--new-window");
            command.add(HOME_URL);

            Process process = new ProcessBuilder(command).start();
            launchedProcess.set(process);
            waitForDevTools(debuggerAddress, Duration.ofSeconds(8));
            boolean connected = isDevToolsAvailable(debuggerAddress);
            return buildStatus(
                    connected,
                    connected
                            ? "养号浏览器已打开，请在窗口里登录、设置地址并手动搜索"
                            : "浏览器已启动，但还没连上调试端口；如果已有同资料目录 Chrome，请先关闭后重试");
        } catch (IOException exception) {
            return buildStatus(false, "无法打开 Chrome：" + exception.getMessage());
        }
    }

    public MeituanBrowserStatus getStatus() {
        boolean connected = isDevToolsAvailable(resolveDebuggerAddress());
        return buildStatus(connected, connected ? "养号浏览器已连接" : "养号浏览器未连接");
    }

    public Optional<String> getActiveDebuggerAddress() {
        String debuggerAddress = resolveDebuggerAddress();
        return isDevToolsAvailable(debuggerAddress) ? Optional.of(debuggerAddress) : Optional.empty();
    }

    private void waitForDevTools(String debuggerAddress, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isDevToolsAvailable(debuggerAddress)) {
                return;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean isDevToolsAvailable(String debuggerAddress) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + debuggerAddress + "/json/version"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException | IllegalArgumentException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private MeituanBrowserStatus buildStatus(boolean connected, String message) {
        Process process = launchedProcess.get();
        boolean launched = process != null && process.isAlive();
        return MeituanBrowserStatus.builder()
                .connected(connected)
                .launchedByBackend(launched)
                .debuggerAddress(resolveDebuggerAddress())
                .userDataDir(chromeUserDataDir)
                .profileDirectory(chromeProfileDirectory)
                .message(message)
                .build();
    }

    private String resolveDebuggerAddress() {
        if (!configuredDebuggerAddress.isBlank()) {
            return configuredDebuggerAddress;
        }
        return "127.0.0.1:" + remoteDebuggingPort;
    }

    private String resolveChromePath() throws IOException {
        if (!chromeBinaryPath.isBlank()) {
            return chromeBinaryPath;
        }
        List<String> candidates = List.of(
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe");
        for (String candidate : candidates) {
            if (Files.exists(Path.of(candidate))) {
                return candidate;
            }
        }
        return "chrome";
    }
}
