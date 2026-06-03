package cn.edu.cn.javadiet.model.dto;

import cn.edu.cn.javadiet.model.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String email;
    private Gender gender;
    private Integer age;
    private Double heightCm;
    private Double weightKg;
    private String address;
    private Double latitude;
    private Double longitude;
}
