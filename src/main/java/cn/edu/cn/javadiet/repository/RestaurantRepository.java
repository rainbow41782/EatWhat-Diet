package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.Restaurant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByExternalSourceAndExternalId(String externalSource, String externalId);

    List<Restaurant> findByExternalSource(String externalSource);
}
