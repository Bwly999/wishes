package cn.edu.xmu.wishes.user.model.po;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@TableName("wishes_auth_role")
public class AuthRole {
    private String name;
    private String descr;
    private List<AuthPrivilege> privilegeList;
    private Type state;

    public enum Type {
        NORMAL(0, "正常"),
        BANNED(1, "封禁");

        private static final Map<Integer, Type> TYPE_MAP;

        static {
            TYPE_MAP = new HashMap();
            for (Type enum1 : values()) {
                TYPE_MAP.put(enum1.code, enum1);
            }
        }

        @EnumValue
        private int code;

        private String description;

        Type(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public static Type getTypeByCode(Integer code) {
            return TYPE_MAP.get(code);
        }

        public Integer getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

    }
}
