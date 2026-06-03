package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.FoodNutritionReference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodNutritionReferenceRepository extends JpaRepository<FoodNutritionReference, Long> {

    Optional<FoodNutritionReference> findBySourceAndFdcId(String source, Long fdcId);
}
