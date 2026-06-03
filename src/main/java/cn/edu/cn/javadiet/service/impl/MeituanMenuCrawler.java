package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.MeituanCrawlResult;
import cn.edu.cn.javadiet.model.entity.MeituanCrawlTask;
import org.springframework.stereotype.Component;

@Component
public class MeituanMenuCrawler {

    private final MeituanSignedRequestCaptureService captureService;
    private final MeituanMenuParser parser;

    public MeituanMenuCrawler(
            MeituanSignedRequestCaptureService captureService,
            MeituanMenuParser parser) {
        this.captureService = captureService;
        this.parser = parser;
    }

    public MeituanCrawlResult crawl(MeituanCrawlTask task) {
        MeituanCrawlResult result = parser.parse(captureService.crawlWithLatestTemplate(task));
        if (result.getItems().isEmpty()) {
            throw new IllegalStateException("签名模板没有解析到菜单菜品，请重新人工采集模板");
        }
        if (result.getMeituanPoiId() == null) {
            result.setMeituanPoiId(task.getMeituanPoiId());
        }
        if (result.getRestaurantName() == null) {
            result.setRestaurantName(task.getShopName());
        }
        if (result.getAddress() == null) {
            result.setAddress(task.getAddress());
        }
        result.setLatitude(task.getLatitude());
        result.setLongitude(task.getLongitude());
        return result;
    }
}
