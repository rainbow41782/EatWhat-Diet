package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.MeituanCrawlTask;
import cn.edu.cn.javadiet.model.enums.MeituanCrawlStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeituanCrawlTaskRepository extends JpaRepository<MeituanCrawlTask, Long> {

    List<MeituanCrawlTask> findByStatusOrderByIdAsc(MeituanCrawlStatus status);

    List<MeituanCrawlTask> findByShopNameContainingIgnoreCaseOrAddressContainingIgnoreCaseOrderByIdAsc(
            String shopName,
            String address);

    Optional<MeituanCrawlTask> findFirstByMeituanPoiId(String meituanPoiId);

    Optional<MeituanCrawlTask> findFirstByShopNameIgnoreCaseAndAddressIgnoreCase(String shopName, String address);

    boolean existsByStatus(MeituanCrawlStatus status);
}
