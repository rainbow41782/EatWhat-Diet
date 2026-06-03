package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.dto.MeituanBrowserStatus;
import cn.edu.cn.javadiet.model.dto.MeituanCaptureImportResponse;
import cn.edu.cn.javadiet.model.dto.MeituanCaptureStatus;
import cn.edu.cn.javadiet.model.dto.MeituanCapturedShop;
import cn.edu.cn.javadiet.model.dto.MeituanCleanupResult;
import cn.edu.cn.javadiet.model.dto.MeituanCrawlResult;
import cn.edu.cn.javadiet.model.dto.MeituanCrawlRunResponse;
import cn.edu.cn.javadiet.model.dto.MeituanCrawlTaskCreateRequest;
import cn.edu.cn.javadiet.model.entity.MeituanCrawlTask;
import cn.edu.cn.javadiet.model.enums.MeituanCrawlStatus;
import cn.edu.cn.javadiet.service.MeituanCrawlService;
import cn.edu.cn.javadiet.service.impl.MeituanBrowserSessionService;
import cn.edu.cn.javadiet.service.impl.MeituanSignedRequestCaptureService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/meituan-crawl")
public class MeituanCrawlController {

    private final MeituanCrawlService meituanCrawlService;
    private final MeituanBrowserSessionService browserSessionService;
    private final MeituanSignedRequestCaptureService captureService;

    public MeituanCrawlController(
            MeituanCrawlService meituanCrawlService,
            MeituanBrowserSessionService browserSessionService,
            MeituanSignedRequestCaptureService captureService) {
        this.meituanCrawlService = meituanCrawlService;
        this.browserSessionService = browserSessionService;
        this.captureService = captureService;
    }

    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<List<MeituanCrawlTask>>> createTasks(
            @RequestBody MeituanCrawlTaskCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(meituanCrawlService.createTasks(request.getTasks())));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<MeituanCrawlTask>>> listTasks(
            @RequestParam(required = false) MeituanCrawlStatus status,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(meituanCrawlService.listTasks(status, keyword)));
    }

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<MeituanCrawlRunResponse>> runPendingTasks() {
        return ResponseEntity.ok(ApiResponse.ok(meituanCrawlService.runPendingTasks()));
    }

    @PostMapping("/browser/open")
    public ResponseEntity<ApiResponse<MeituanBrowserStatus>> openWarmBrowser() {
        return ResponseEntity.ok(ApiResponse.ok(browserSessionService.openWarmBrowser()));
    }

    @GetMapping("/browser/status")
    public ResponseEntity<ApiResponse<MeituanBrowserStatus>> getWarmBrowserStatus() {
        return ResponseEntity.ok(ApiResponse.ok(browserSessionService.getStatus()));
    }

    @PostMapping("/capture/start")
    public ResponseEntity<ApiResponse<MeituanCaptureStatus>> startCapture() {
        return ResponseEntity.ok(ApiResponse.ok(captureService.startCapture()));
    }

    @PostMapping("/capture/stop")
    public ResponseEntity<ApiResponse<MeituanCaptureStatus>> stopCapture() {
        return ResponseEntity.ok(ApiResponse.ok(captureService.stopCapture()));
    }

    @GetMapping("/capture/status")
    public ResponseEntity<ApiResponse<MeituanCaptureStatus>> getCaptureStatus() {
        return ResponseEntity.ok(ApiResponse.ok(captureService.getStatus()));
    }

    @GetMapping("/capture/preview")
    public ResponseEntity<ApiResponse<MeituanCrawlResult>> previewLatestCapturedMenu() {
        return ResponseEntity.ok(ApiResponse.ok(meituanCrawlService.previewCapturedMenu(null)));
    }

    @PostMapping("/capture/import-latest")
    public ResponseEntity<ApiResponse<MeituanCaptureImportResponse>> importLatestCapturedMenu() {
        return ResponseEntity.ok(ApiResponse.ok(meituanCrawlService.importLatestCapturedMenu()));
    }

    @GetMapping("/captures")
    public ResponseEntity<ApiResponse<List<MeituanCapturedShop>>> listCapturedShops() {
        return ResponseEntity.ok(ApiResponse.ok(meituanCrawlService.listCapturedShops()));
    }

    @GetMapping("/captures/{captureKey}/preview")
    public ResponseEntity<ApiResponse<MeituanCrawlResult>> previewCapturedMenu(@PathVariable String captureKey) {
        return ResponseEntity.ok(ApiResponse.ok(meituanCrawlService.previewCapturedMenu(captureKey)));
    }

    @PostMapping("/captures/{captureKey}/import")
    public ResponseEntity<ApiResponse<MeituanCaptureImportResponse>> importCapturedMenu(
            @PathVariable String captureKey) {
        return ResponseEntity.ok(ApiResponse.ok(meituanCrawlService.importCapturedMenu(captureKey)));
    }

    @DeleteMapping("/captures/{captureKey}")
    public ResponseEntity<ApiResponse<Void>> deleteCapturedShop(@PathVariable String captureKey) {
        meituanCrawlService.deleteCapturedShop(captureKey);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/cleanup-preview")
    public ResponseEntity<ApiResponse<MeituanCleanupResult>> previewBadDataCleanup() {
        return ResponseEntity.ok(ApiResponse.ok(meituanCrawlService.previewBadDataCleanup()));
    }

    @PostMapping("/cleanup-bad-data")
    public ResponseEntity<ApiResponse<MeituanCleanupResult>> cleanupBadData() {
        return ResponseEntity.ok(ApiResponse.ok(meituanCrawlService.cleanupBadData()));
    }

    @PostMapping("/tasks/{taskId}/retry")
    public ResponseEntity<ApiResponse<MeituanCrawlTask>> retryTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.ok(meituanCrawlService.retryTask(taskId)));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long taskId) {
        meituanCrawlService.deleteTask(taskId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
