package com.mall.demo.module.user.service;


import cn.hutool.core.util.DesensitizedUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.common.util.JwtUtils;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.Refund.entity.RefundNotifyMessage;
import com.mall.demo.module.Refund.mapper.RefundNotifyMessageMapper;
import com.mall.demo.module.logistic.entity.Address;
import com.mall.demo.module.logistic.entity.LogisticNotifyMessage;
import com.mall.demo.module.logistic.mapper.AddressMapper;
import com.mall.demo.module.logistic.mapper.LogisticNotifyMessageMapper;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.SuborderMapper;
import com.mall.demo.module.payment.entity.PaymentNotifyMessage;
import com.mall.demo.module.payment.mapper.PaymentNotifyMessageMapper;
import com.mall.demo.module.user.dto.req.UserResetPasswordReqDto;
import com.mall.demo.module.user.dto.req.UserLoginReqDto;
import com.mall.demo.module.user.dto.req.UserRegisterReqDto;
import com.mall.demo.module.user.dto.req.UserUpdateReqDto;
import com.mall.demo.module.user.dto.resp.GetUserInfoRespDto;
import com.mall.demo.module.user.dto.resp.UserLoginRespDto;
import com.mall.demo.module.user.entity.User;
import com.mall.demo.module.user.mapper.UserMapper;
import com.mall.demo.mq.producer.UserProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private final StringRedisTemplate stringRedisTemplate;


    private final UserMapper userMapper;
    private final PaymentNotifyMessageMapper paymentNotifyMessageMapper;
    private final RefundNotifyMessageMapper refundNotifyMessageMapper;
    private final LogisticNotifyMessageMapper logisticNotifyMessageMapper;
    private final AddressMapper addressMapper;

    private final SuborderMapper suborderMapper;
    private final UserProducer userProducer;
    private final RedisService redisService;

    private final JwtUtils jwtUtils;

    private final PasswordEncoder passwordEncoder;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String register(UserRegisterReqDto dto) {

        String phone = dto.getPhone();
        String password = dto.getPassword();
        String confirmPassword = dto.getConfirmPassword();

//       1. 校验手机号
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCodeEnum.PHONE_ERROR.getCode(), ErrorCodeEnum.PHONE_ERROR.getMessage());
        }

//       2.校验密码
        //格式
        if (password == null || password.length() < 6) {
            throw new BizException(ErrorCodeEnum.PASSWORD_NOT_EXIST.getCode(), ErrorCodeEnum.PASSWORD_NOT_EXIST.getMessage());
        }
        //确认密码是否一致
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            throw new BizException(ErrorCodeEnum.PASSWORD_NOT_MATCH.getCode(), ErrorCodeEnum.PASSWORD_NOT_MATCH.getMessage());
        }


//        3.校验验证码
        checkCode(phone, dto.getCode());


        Long nextId = stringRedisTemplate.opsForValue().increment("user:register:count");

        String defaultUserName = "拼夕夕用户" + nextId;


//        密码加密Bcrypt
        String encodedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setPhone(phone);
        user.setPassword(encodedPassword);
        user.setUserName(defaultUserName);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

//        插入数据库
        try {
            log.info("尝试插入数据库");
            userMapper.insert(user);
            log.info("插入数据库成功!!!");
        } catch (DuplicateKeyException e) {
            log.error("数据库插入冲突详细:", e);
            log.error("手机号:{} 已注册,插入数据库失败", phone);
            throw new BizException(ErrorCodeEnum.PHONE_EXIST.getCode(), ErrorCodeEnum.PHONE_EXIST.getMessage());
        }
        // 5. 发送注册消息
        log.info("开始发送注册消息--->生产者");
        userProducer.sendRegister(user);
        return"注册成功";
    }



    @Override
    public String resetPassword(UserResetPasswordReqDto dto){

        String userId = dto.getUserId() ;
        String phone = dto.getPhone();
        String password = dto.getPassword();
        String confirmPassword = dto.getConfirmPassword();



        User user = userMapper.selectById(userId);
        if (user == null){
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
        }







//       1. 校验手机号
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCodeEnum.PHONE_ERROR.getCode(), ErrorCodeEnum.PHONE_ERROR.getMessage());
        }

//       2.校验密码
        //格式
        if (password == null || password.length() < 6) {
            throw new BizException(ErrorCodeEnum.PASSWORD_NOT_EXIST.getCode(), ErrorCodeEnum.PASSWORD_NOT_EXIST.getMessage());
        }
        //确认密码是否一致
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            throw new BizException(ErrorCodeEnum.PASSWORD_NOT_MATCH.getCode(), ErrorCodeEnum.PASSWORD_NOT_MATCH.getMessage());
        }


//        3.校验验证码
        checkCode(phone, dto.getCode());





//        密码加密Bcrypt
        String encodedPassword = passwordEncoder.encode(password);


        user.setPassword(encodedPassword);
        user.setUpdateTime(LocalDateTime.now());

//        插入数据库
        try {

            userMapper.updateById(user);

        } catch (DuplicateKeyException e) {

            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
        }


        return "用户密码重置成功!";
    }



    @Override
    public UserLoginRespDto login(UserLoginReqDto dto) {

        String phone = dto.getPhone();
        String userId = dto.getUserId();
        // 防刷 - 使用正确的key前缀
        String rateLimitKey = "login:rate:" + phone;

        Long keyCount = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        log.info("【防刷】=访问次数:{}", keyCount);
        if (keyCount == 1) {
            stringRedisTemplate.expire(rateLimitKey, 60, TimeUnit.SECONDS);
        }
        if (keyCount > 10) {
            throw new BizException(ErrorCodeEnum.MANY_REQUEST.getCode(), ErrorCodeEnum.MANY_REQUEST.getMessage());
        }
        log.info("【防刷】客户端IP:{} 访问成功", rateLimitKey);


//查数据库

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(userWrapper);
        if (user == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
        }

        // 校验密码
        log.info("【用户中心】开始校验密码...");
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCodeEnum.PASSWORD_ERROR.getCode(), ErrorCodeEnum.PASSWORD_ERROR.getMessage());
        }
        log.info("【用户中心】密码校验成功...");
        log.info("前端传入的验证码:{}", dto.getCode());
        log.info("【用户中心】开始校验验证码...");
        checkCode(phone, dto.getCode());
        log.info("用户{}登陆成功", dto.getPhone());


//       生成token
        log.info("【用户中心】开始生成token...");
        String token = jwtUtils.generateToken(userId, "USER");
        // 6. 存 Redis（token）
        stringRedisTemplate.opsForValue().set(
                "token:" + token,
                userId,
                7,
                TimeUnit.DAYS
        );
        log.info("【缓存中心】存储用户信息...");
        redisService.set("user:" + userId, JSON.toJSONString(user), 10);

        UserLoginRespDto userLoginRespDto = new UserLoginRespDto();
        userLoginRespDto.setToken(token);
        userLoginRespDto.setUserId(userId);
        userLoginRespDto.setUserName(user.getUserName());
        userLoginRespDto.setPhone(user.getPhone());
        userLoginRespDto.setRole(user.getRole());
        return userLoginRespDto;
    }

    @Override
    public void update(UserUpdateReqDto dto) {

        String userId = SecurityUtils.getId();
        if (userId == null){
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
        }

        redisService.delete("user:" + userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
        }
        user.setUserId(userId);
        if(dto.getNewUserName() != null && !dto.getNewUserName().isEmpty()){
        user.setUserName(dto.getNewUserName());
        }
        if (dto.getNewUserAvatar() != null && !dto.getNewUserAvatar().isEmpty()) {
            user.setUserAvatar(dto.getNewUserAvatar());
        }
        if (dto.getNewPassword() != null && !dto.getNewPassword().isEmpty())  {
            user.setSex(dto.getNewSex());
        }

        if (dto.getNewPhone() != null && !dto.getNewPhone().isEmpty()){
            user.setPhone(dto.getNewPhone());
        }

        userMapper.updateById(user);





    }

    @Override
    public GetUserInfoRespDto getCurrentUserInfo() {
        // 1. 从 SecurityUtils 获取当前用户ID
        String userId = SecurityUtils.getId();



        // 2. 缓存Key
        String cacheKey = "user:" + userId;

        // 3. 使用通用缓存方法
        User user = redisService.get(
                cacheKey,
                User.class,
                (key) -> {
                    // 从数据库查询用户
                    return userMapper.selectById(userId);
                },
                30
                // 缓存30分钟
        );

        // 4. 判断用户是否存在
        if (user == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), "用户不存在");
        }

        //子订单数量
        Integer suborderCount = suborderMapper.getSuborderCountById(userId);

        Integer messageCount = suborderMapper.getMessageCountById(userId);

        Integer couponCount = suborderMapper.getCouponCountById(userId);



        //我的消息:退款消息
        LambdaQueryWrapper<RefundNotifyMessage> refundWrapper = new LambdaQueryWrapper<>();
        refundWrapper.eq(RefundNotifyMessage::getTargetUserId, userId);
        List<RefundNotifyMessage> myRefundMessageList = refundNotifyMessageMapper.selectList(refundWrapper);
        //我的消息:物流消息
        LambdaQueryWrapper<LogisticNotifyMessage> logisticWrapper = new LambdaQueryWrapper<>();
        logisticWrapper.eq(LogisticNotifyMessage::getTargetUserId, userId);
        List<LogisticNotifyMessage> myLogisticMessageList = logisticNotifyMessageMapper.selectList(logisticWrapper);
        //我的消息:支付消息
        LambdaQueryWrapper<PaymentNotifyMessage> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(PaymentNotifyMessage::getTargetUserId, userId);
        List<PaymentNotifyMessage> myPayMessageList = paymentNotifyMessageMapper.selectList(paymentWrapper);






        GetUserInfoRespDto respDto = new GetUserInfoRespDto();
        respDto.setUserId(user.getUserId());
        respDto .setUserName(user.getUserName());
        String maskedPhone = DesensitizedUtil.mobilePhone(user.getPhone());
        respDto.setPhone(maskedPhone);
        respDto.setUserAvatar(user.getUserAvatar());
        respDto.setEmail(user.getEmail());
        respDto.setSex(user.getSex());
        respDto.setRole(user.getRole());
        respDto.setStatus(user.getStatus());
        respDto.setCreateTime(user.getCreateTime());
        respDto.setSuborderCount(suborderCount);
        respDto.setMessageCount(messageCount);
        respDto.setCouponCount(couponCount);
        respDto.setMyRefundMessageList(myRefundMessageList);
        respDto.setMyLogisticMessageList(myLogisticMessageList);
        respDto.setMyPayMessageList(myPayMessageList);

        // 根据您的 GetUserInfoRespDto 字段继续设置
        return respDto;
    }




    @Override
//    发送验证码
    public String sendCode(String phone) {

//        校验手机号
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCodeEnum.PHONE_ERROR.getCode(), ErrorCodeEnum.PHONE_ERROR.getMessage());
        }

        // 1. 限流（60秒内只能发一次）
        String key = "code:send:" + phone;
        if (stringRedisTemplate.hasKey(key)) {
            throw new BizException(ErrorCodeEnum.MANY_REQUEST.getCode(), ErrorCodeEnum.MANY_REQUEST.getMessage());
        }

        // 2. 生成验证码（6位）
        String code = String.valueOf(
                ThreadLocalRandom.current().nextInt(100000, 999999)
        );

        // 3. 存 Redis（验证码）
        stringRedisTemplate.opsForValue().set(
                "code:" + phone,
                code,
                3,
                TimeUnit.MINUTES
        );

        // 4. 设置发送限制（60秒）
        stringRedisTemplate.opsForValue().set(
                key,
                "1",
                60,
                TimeUnit.SECONDS
        );

        log.info("发送验证码到手机: {}, 验证码: {}", phone, code);


        return "手机号码:" + phone + " 发送成功,验证码有效期3分钟";
    }

    @Override
//    校验验证码
    public String checkCode(String phone, String code) {

        /*
         * phone 前端传入的手机号 ,根据该手机号查询缓存获取验证码
         * code  前端传入的验证码,判断该验证码与缓存的验证码是否一致
         * cacheCode 缓存的验证码
         * */

        log.info("前端传入的验证码:{}",code);

        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCodeEnum.PHONE_ERROR.getCode(), ErrorCodeEnum.PHONE_ERROR.getMessage());
        }

        // 1. 从 Redis 获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get("code:" + phone);
        if (cacheCode == null) {
            throw new BizException(ErrorCodeEnum.CODE_EXPIRED.getCode(), ErrorCodeEnum.CODE_EXPIRED.getMessage());
        }

        if (!cacheCode.equals(code)) {
            throw new BizException(ErrorCodeEnum.CODE_ERROR.getCode(), ErrorCodeEnum.CODE_ERROR.getMessage());
        }
        // 2. 删除验证码缓存
        stringRedisTemplate.delete("code:" + phone);

        return "校验成功";
    }


    @Override
    public User getUserByPhone(String phone) {


        String key = "cache:user:" + phone;

        String json = stringRedisTemplate.opsForValue().get(key);

        if (json != null) {
            if ("NULL".equals(json)) {
                return null;
            }
            return JSON.parseObject(json, User.class);
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));

        if (user == null) {
            stringRedisTemplate.opsForValue().set(
                    key,
                    "NULL",
                    2,
                    TimeUnit.MINUTES
            );
            return null;
        }

        stringRedisTemplate.opsForValue().set(
                key,
                JSON.toJSONString(user),
                30,
                TimeUnit.MINUTES
        );

        return user;
    }

    @Override
    public User getUserByUserId(String userId) {
        String key = "cache:user:" + userId;

        String json = stringRedisTemplate.opsForValue().get(key);

        if (json != null) {
            if ("NULL".equals(json)) {
                return null;
            }
            return JSON.parseObject(json, User.class);
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserId, userId));

        if (user == null) {
            stringRedisTemplate.opsForValue().set(
                    key,
                    "NULL",
                    2,
                    TimeUnit.MINUTES
            );
            return null;
        }

        stringRedisTemplate.opsForValue().set(
                key,
                JSON.toJSONString(user),
                30,
                TimeUnit.MINUTES
        );

        return user;
    }
}



