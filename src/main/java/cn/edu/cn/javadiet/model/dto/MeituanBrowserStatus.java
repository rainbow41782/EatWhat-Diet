package cn.edu.cn.javadiet.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeituanBrowserStatus {

    private boolean connected;
    private boolean launchedByBackend;
    private String debuggerAddress;
    private String userDataDir;
    private String profileDirectory;
    private String message;
}
