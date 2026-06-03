package cn.edu.cn.javadiet.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeituanCaptureStatus {

    private boolean connected;
    private boolean listening;
    private boolean templateReady;
    private String debuggerAddress;
    private int capturedCount;
    private int capturedShopCount;
    private int menuResponseCount;
    private int searchResponseCount;
    private Integer latestItemCount;
    private String latestEndpoint;
    private String latestRestaurantName;
    private String latestPoiId;
    private LocalDateTime startedAt;
    private LocalDateTime latestCapturedAt;
    private String message;
}
