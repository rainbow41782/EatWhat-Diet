package cn.edu.cn.javadiet.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeituanCrawlTaskCreateRequest {

    private List<MeituanCrawlTaskInput> tasks;
}
