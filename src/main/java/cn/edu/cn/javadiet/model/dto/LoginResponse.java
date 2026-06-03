package cn.edu.cn.javadiet.model.dto;

import cn.edu.cn.javadiet.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private User user;
    private String token;
}
