package cn.edu.cn.javadiet.model.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeituanMenuItem {

    private String externalId;
    private String skuId;
    private String name;
    private String category;
    private String description;
    private BigDecimal price;
    private Boolean available;
    private String portionSize;
    private String imageUrl;
    private String rawSpec;
    private String rawJson;
}
