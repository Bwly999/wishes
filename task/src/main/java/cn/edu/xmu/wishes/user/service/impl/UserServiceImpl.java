package cn.edu.xmu.wishes.user.service.impl;

import cn.edu.xmu.wishes.core.util.JwtHelper;
import cn.edu.xmu.wishes.core.util.RedisUtil;
import cn.edu.xmu.wishes.core.util.ReturnNo;
import cn.edu.xmu.wishes.core.util.ReturnObject;
import cn.edu.xmu.wishes.user.config.MailSenderProperty;
import cn.edu.xmu.wishes.user.mapper.UserMapper;
import cn.edu.xmu.wishes.user.model.po.User;
import cn.edu.xmu.wishes.user.model.vo.*;
import cn.edu.xmu.wishes.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;

import static cn.edu.xmu.wishes.core.util.Common.cloneVo;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author bwly
 * @since 2022-02-07
 */
@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final static String USER_KEY="user_%d";
    private static final String CAPTCHA_KEY = "cap_%s";

    @Value("${user.login.jwt.expire}")
    private Integer USER_EXPIRE_TIME = 3600;

    @Value("${user.login.captcha.expire}")
    private Integer CAPTCHA_EXPIRE_TIME = 600;

    private static JwtHelper jwtHelper = new JwtHelper();

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MailSenderProperty mailProperty;

    @Transactional(rollbackFor = Exception.class)
    public ReturnObject registerUser(UserVo userVo) {
        try {
            //判断是否可以注册
            List<User> userList = lambdaQuery().eq(User::getMobile, userVo.getMobile())
                    .or().eq(User::getEmail, userVo.getEmail())
                    .or().eq(User::getUserName, userVo.getUserName()).list();

            if (userList.size() > 0) {
                for(User user1 : userList) {
                    if (user1.getUserName() != null && user1.getUserName().equals(userVo.getUserName())) {
                        return new ReturnObject(ReturnNo.CUSTOMER_NAMEEXIST);
                    }
                    else if (user1.getMobile() != null && user1.getMobile().equals(userVo.getMobile())) {
                        return new ReturnObject(ReturnNo.CUSTOMER_MOBILEEXIST);
                    }
                    else if (user1.getEmail() != null && user1.getEmail().equals(userVo.getEmail())) {
                        return new ReturnObject(ReturnNo.CUSTOMER_EMAILEXIST);
                    }
                }
            }

            String userEmail = userVo.getEmail();
            String captcha = createCaptcha(userVo);
            sendEmail(captcha, userEmail);
            return new ReturnObject();
        }
        catch (Exception e) {
            log.error("注册失败" + e.getMessage());
            return new ReturnObject(ReturnNo.INTERNAL_SERVER_ERR);
        }
    }

    public String createCaptcha(Serializable value)
    {
        try {
            //随机生成验证码
            String captcha = getRandomString(6);
            while (redisUtil.hasKey(captcha)) {
                captcha = getRandomString(6);
            }

            String key =  String.format(CAPTCHA_KEY, captcha);
            redisUtil.set(key, value, CAPTCHA_EXPIRE_TIME);
            return captcha;
        } catch (Exception e) {
            log.error("captcha" + e.getMessage());
            return null;
        }
    }
    private ReturnObject sendEmail(String message,String to)
    {
        try
        {
            //发送邮件(请在配置文件application.properties填写密钥)
            mailProperty.setText(String.format(mailProperty.getFormat(),message));
            SimpleMailMessage msg =new SimpleMailMessage();
            msg.setText(mailProperty.getText());
            msg.setFrom(mailProperty.getFrom());
            msg.setSentDate(new Date());
            msg.setSubject(mailProperty.getSubject());
            msg.setTo(to);
            mailSender.send(msg);
            return new ReturnObject(ReturnNo.OK);
        }catch (MailException e)
        {
            return new ReturnObject(ReturnNo.INTERNAL_SERVER_ERR);
        }
    }

    public ReturnObject verifyLoginUpCaptcha(CaptchaVo captchaVo) {
        try {
            String key = String.format(CAPTCHA_KEY, captchaVo.getCaptcha());
            //通过验证码取出id
            if (!redisUtil.hasKey(key)) {
                return new ReturnObject(ReturnNo.CUSTOMER_CAPTCHA_ERROR);
            }

            User user = cloneVo(redisUtil.get(key), User.class);
            if (user != null && user.getEmail().equals(captchaVo.getEmail())) {
                //删除redis中的验证码
                redisUtil.del(key);
                // 保存到数据库
                save(user);
                return new ReturnObject();
            }
            return new ReturnObject(ReturnNo.CUSTOMER_CAPTCHA_ERROR);
        } catch (Exception e) {
            log.error("verify captcha" + e.getMessage());
            return new ReturnObject(ReturnNo.INTERNAL_SERVER_ERR);
        }

    }

    private String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }



    private void banJwt(String jwt) {
        String[] banSetName = {"BanJwt_0", "BanJwt_1"};
        String banIndexKey = "banIndex";
        String scriptPath = "scripts/ban-jwt.lua";

        DefaultRedisScript<Void> script = new DefaultRedisScript<>();

        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(scriptPath)));
        script.setResultType(Void.class);

        List<String> keyList = new ArrayList<>(List.of(banSetName));
        keyList.add(banIndexKey);
        redisUtil.executeScript(script, keyList, banSetName.length, jwt, USER_EXPIRE_TIME);
    }

    private void banToken(Long id)
    {
        String key = String.format(USER_KEY, id);
        Set<Serializable> set = redisUtil.getSet(key);
        String jwt = null;
        for (Serializable str : set) {
            /* 找出JWT */
            if ((str.toString()).length() > 8) {
                jwt = str.toString();
                banJwt(jwt);
            }
        }
        redisUtil.del(key);
    }

    public ReturnObject logout(Long userId)
    {
        try {
            banToken(userId);
            return new ReturnObject(ReturnNo.OK);
        } catch (Exception e) {
            log.error("logout " + e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public ReturnObject getUserInfo(Long userId) {
        try {
            User user = getById(userId);
            return new ReturnObject(cloneVo(user, UserRetVo.class));
        } catch (Exception e) {
            log.error("logout " + e.getMessage());
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ReturnObject changeUserInfo(Long userId, SimpleUserVo vo) {
        try {
            User user = new User();
            BeanUtils.copyProperties(vo, user);
            user.setId(userId);
            this.updateById(user);
            return new ReturnObject();
        } catch (Exception e) {
            log.error("changeUserInfo " + e.getMessage());
            throw e;
        }
    }

    @Override
    public ReturnObject changeUserPassword(String userEmail, NewPasswordVo vo) {
        try {
            LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(User::getEmail, userEmail);
            User user = this.getOne(lambdaQueryWrapper);
            if (user == null) {
                return new ReturnObject(ReturnNo.RESOURCE_ID_NOTEXIST, "该邮箱未注册");
            }
            if (!user.getPassword().equals(vo.getOldPassword())) {
                return new ReturnObject(ReturnNo.CUSTOMER_PASSWORDWRONG);
            }

            user.setPassword(vo.getNewPassword());
            this.updateById(user);
            return new ReturnObject();
        } catch (Exception e) {
            log.error("changeUserPassword " + e.getMessage());
            throw e;
        }
    }
}
