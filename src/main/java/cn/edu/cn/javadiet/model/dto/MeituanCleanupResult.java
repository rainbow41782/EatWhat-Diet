package cn.edu.cn.javadiet.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeituanCleanupResult {

    private boolean cleaned;
    private int restaurantCount;
    private int foodItemCount;
    private int menuItemCount;
    private String message;
}
