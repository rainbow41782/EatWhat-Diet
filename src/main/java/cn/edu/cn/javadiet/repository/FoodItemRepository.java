package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.FoodItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    Optional<FoodItem> findByExternalSourceAndExternalId(String externalSource, String externalId);

    Optional<FoodItem> findFirstByNameIgnoreCase(String name);

    List<FoodItem> findByExternalSource(String externalSource);
}
