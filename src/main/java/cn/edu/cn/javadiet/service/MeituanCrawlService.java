package cn.edu.cn.javadiet.service;

import cn.edu.cn.javadiet.model.dto.MeituanCrawlRunResponse;
import cn.edu.cn.javadiet.model.dto.MeituanCrawlTaskInput;
import cn.edu.cn.javadiet.model.dto.MeituanCaptureImportResponse;
import cn.edu.cn.javadiet.model.dto.MeituanCapturedShop;
import cn.edu.cn.javadiet.model.dto.MeituanCleanupResult;
import cn.edu.cn.javadiet.model.dto.MeituanCrawlResult;
import cn.edu.cn.javadiet.model.entity.MeituanCrawlTask;
import cn.edu.cn.javadiet.model.enums.MeituanCrawlStatus;
import java.util.List;

public interface MeituanCrawlService {

    List<MeituanCrawlTask> createTasks(List<MeituanCrawlTaskInput> inputs);

    List<MeituanCrawlTask> listTasks(MeituanCrawlStatus status, String keyword);

    MeituanCrawlRunResponse runPendingTasks();

    MeituanCaptureImportResponse importLatestCapturedMenu();

    List<MeituanCapturedShop> listCapturedShops();

    MeituanCrawlResult previewCapturedMenu(String captureKey);

    MeituanCaptureImportResponse importCapturedMenu(String captureKey);

    void deleteCapturedShop(String captureKey);

    MeituanCleanupResult previewBadDataCleanup();

    MeituanCleanupResult cleanupBadData();

    MeituanCrawlTask retryTask(Long taskId);

    void deleteTask(Long taskId);
}
