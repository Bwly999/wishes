package cn.edu.xmu.wishes.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRetVo {
    private String userName;

    private String sign;

    private String address;

    private String mobile;

    private String email;

//    @NotBlank(message = "真实姓名不能为空")
//    private String name;
}
