package cn.edu.cn.javadiet.model.dto;

import cn.edu.cn.javadiet.model.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserBasicRequest {

    private String nickname;
    private String email;
    private Gender gender;
    private Integer age;
}
