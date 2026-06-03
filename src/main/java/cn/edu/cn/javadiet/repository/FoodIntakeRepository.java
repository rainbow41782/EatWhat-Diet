package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.FoodIntake;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodIntakeRepository extends JpaRepository<FoodIntake, Long> {

    List<FoodIntake> findByMealRecordId(Long mealRecordId);

    List<FoodIntake> findByMealRecordIdIn(Collection<Long> mealRecordIds);
}
