package com.mall.demo.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.demo.common.config.RedisConfig;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.common.result.PageResp;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.coupon.dto.req.CouponUpdateReqDto;
import com.mall.demo.module.coupon.dto.resp.AmountPayableRespDto;
import com.mall.demo.module.coupon.dto.resp.CouponRespDto;
import com.mall.demo.module.coupon.dto.resp.TwoCouponListsRespDto;
import com.mall.demo.module.coupon.entity.CouponUsedRecord;
import com.mall.demo.module.coupon.mapper.CouponUsedRecordMapper;
import com.mall.demo.module.coupon.mapper.CouponUserMapper;
import com.mall.demo.module.coupon.service.CouponService;
import com.mall.demo.module.logistic.entity.Logistic;
import com.mall.demo.module.logistic.mapper.LogisticMapper;
import com.mall.demo.module.order.dto.req.*;
import com.mall.demo.module.order.dto.resp.*;
import com.mall.demo.module.order.entity.MyOrder;
import com.mall.demo.module.order.entity.OrderItem;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.OrderItemMapper;
import com.mall.demo.module.order.mapper.OrderMapper;
import com.mall.demo.module.order.mapper.SuborderMapper;
import com.mall.demo.module.payment.entity.Payment;
import com.mall.demo.module.payment.mapper.PaymentMapper;
import com.mall.demo.module.product.entity.SKU;
import com.mall.demo.module.product.mapper.SKUMapper;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.shop.mapper.ShopMapper;
import com.mall.demo.module.user.entity.User;
import com.mall.demo.module.user.mapper.UserMapper;
import com.mall.demo.mq.producer.OrderProducer;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {


    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ShopMapper    shopMapper;
    private final SKUMapper skuMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderProducer orderProducer;
    private final CouponUserMapper couponUserMapper;
    private final StockService stockService;
    private final LogisticMapper logisticMapper;
    private final CouponService couponService;
    private final SuborderMapper suborderMapper;
    private final PaymentMapper paymentMapper;
    private final CouponUsedRecordMapper couponUsedRecordMapper;

    private final RedisService redisService;





        @Override
    public OrderPreviewRespDto previewOrder(OrderPreviewReqDto dto) {

            log.info("前端传入的优惠卷列表:{}",dto.getUsedCouponIds());
            log.info("是否为第一次请求:{}", dto.getIsFirstRequest());


        //获取用户id
        String userId = SecurityUtils.getId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), "用户未登录或token无效");
        }

        log.info("用户id:{}", userId);

        //检验商品
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException(ErrorCodeEnum.PRO_NOT_FOUND.getCode(), ErrorCodeEnum.PRO_NOT_FOUND.getMessage());
        }

        //初始化总金额为0
        BigDecimal totalAmount = BigDecimal.ZERO;

        //临时订单项
        List<OrderItem> orderItems = new ArrayList<>();


        //遍历订单获取订单项
        for (OrderItemReqDto item : dto.getItems()) {
            String skuId = item.getSkuId();
            Integer quantity = item.getQuantity();
            //查数据库
            log.info("查询商品:{}", skuId);
            SKU sku = skuMapper.selectById(skuId);
            if (sku == null) {
                throw new BizException(ErrorCodeEnum.PRO_NOT_FOUND.getCode(), ErrorCodeEnum.PRO_NOT_FOUND.getMessage());
            }
            if (sku.getStatus() != 1) {
                throw new BizException(ErrorCodeEnum.PRO_NOT_SELL.getCode(), ErrorCodeEnum.PRO_NOT_SELL.getMessage());
            }
            if (sku.getStock() < quantity) {
                throw new BizException(ErrorCodeEnum.STOCK_NOT_ENOUGH.getCode(), ErrorCodeEnum.STOCK_NOT_ENOUGH.getMessage());
            }
            // Deleted://扣库存
            // Deleted:if (!stockService.deductStock(skuId, quantity)) {
            // Deleted:    throw new BizException(ErrorCodeEnum.STOCK_NOT_ENOUGH.getCode(), ErrorCodeEnum.STOCK_NOT_ENOUGH.getMessage());
            // Deleted:}
            //计算金额
            BigDecimal itemPrice = sku.getPrice().multiply(new BigDecimal(quantity));
            totalAmount = totalAmount.add(itemPrice);
            log.info("创建订单项:{}", item.getSkuId());
            //创建订单项
            OrderItem orderItem = new OrderItem();
            orderItem.setSkuId(skuId);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(sku.getPrice());
            orderItem.setProductName(sku.getProductName());
            orderItem.setProductUrl(sku.getProductUrl());
            orderItem.setPrice(itemPrice);
            orderItems.add(orderItem);
        }

        //判断是否应该使用系统推荐优惠券
        boolean shouldUseRecommendedCoupons = true;
        if (!dto.getIsFirstRequest() && (dto.getUsedCouponIds() == null || dto.getUsedCouponIds().isEmpty())) {
            // 不是第一次请求且传入的优惠券为空，不推荐优惠券
            shouldUseRecommendedCoupons = false;
        }

        //应付金额和优惠金额计算（只调用一次，避免重复计算）
        AmountPayableRespDto amountPayableResp;
        BigDecimal payableAmount;
        BigDecimal discountAmount;

        if (!shouldUseRecommendedCoupons && (dto.getUsedCouponIds() == null || dto.getUsedCouponIds().isEmpty())) {
            // 不使用优惠券，直接返回原价
            payableAmount = totalAmount;
            discountAmount = BigDecimal.ZERO;
            amountPayableResp = new AmountPayableRespDto();
            amountPayableResp.setTotalAmount(totalAmount);
            amountPayableResp.setPayableAmount(payableAmount);
            amountPayableResp.setDiscountAmount(discountAmount);
            amountPayableResp.setCouponIds(new ArrayList<>());
        } else {
            // 计算优惠后的价格
            amountPayableResp = couponService.calculateAmountPayable(dto.getUsedCouponIds(), userId, totalAmount);
            payableAmount = amountPayableResp.getPayableAmount();
            discountAmount = amountPayableResp.getDiscountAmount();
        }

      //可用/不可用优惠卷列表（只调用一次，避免重复查询）
        TwoCouponListsRespDto twoCouponLists = couponService.getTwoCouponLists(userId, totalAmount);
        List<CouponRespDto> availableCoupons ;
        List<CouponRespDto> unavailableCoupons ;

        //空值赋值
        if (twoCouponLists == null) {
            availableCoupons = new ArrayList<>();
            unavailableCoupons = new ArrayList<>();
        } else {
            availableCoupons = twoCouponLists.getAvailableCoupons() != null ? twoCouponLists.getAvailableCoupons() : new ArrayList<>();
            unavailableCoupons = twoCouponLists.getUnavailableCoupons() != null ? twoCouponLists.getUnavailableCoupons() : new ArrayList<>();
        }



        //使用的优惠卷（根据条件决定是否使用系统推荐的优惠券）
        List<String> usedCouponIds = dto.getUsedCouponIds();
        if ((usedCouponIds == null || usedCouponIds.isEmpty()) && shouldUseRecommendedCoupons) {
            //使用系统推荐的优惠券
            usedCouponIds = amountPayableResp.getCouponIds();

            log.info("使用的优惠卷列表:{}", usedCouponIds);
        } else if (usedCouponIds == null) {
            // 如果为null但不使用推荐，设置为空列表
            usedCouponIds = new ArrayList<>();
        }





        List<OrderItemRespDto> itemRespDtos = new ArrayList<>();
        for (OrderItem item : orderItems) {
            itemRespDtos.add(OrderItemRespDto.builder()
                    .skuId(item.getSkuId())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .productName(item.getProductName())
                    .productUrl(item.getProductUrl())
                    .build());
        }


        log.info("使用的优惠卷列表:{}", usedCouponIds);
        log.info("返回给前端的价格:{}", payableAmount);
        log.info("前端传入的优惠卷列表:{}",dto.getUsedCouponIds());
        log.info("是否为第一次请求:{}", dto.getIsFirstRequest());
        log.info("是否第一次请求且优惠卷为空:{}",shouldUseRecommendedCoupons);
        return OrderPreviewRespDto.builder()
                //使用的优惠卷couponId列表
                .usedCouponIds(usedCouponIds)



                .totalAmount(totalAmount)
                .payableAmount(payableAmount)
                .discountAmount(discountAmount)
                .availableCoupons(availableCoupons)
                .unavailableCoupons(unavailableCoupons)
                .items(itemRespDtos)
                .build();

    }






    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateRespDto createOrder(OrderCreateReqDto dto) {
        //获取用户id
        String userId = SecurityUtils.getId();
        User user = userMapper.selectById(userId);
        if (userId == null || user == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), "用户未登录或token无效");
        }

        log.info("用户id:{}", userId);
        //检验商品
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException(ErrorCodeEnum.PRO_NOT_FOUND.getCode(), ErrorCodeEnum.PRO_NOT_FOUND.getMessage());
        }

        String userName = user.getUserName();


        //初始化总金额为0
        BigDecimal totalAmount = BigDecimal.ZERO;





        //记录已扣库存(用于回滚)
        List<OrderItemReqDto> deducterStockItems = new ArrayList<>();

        //临时订单项
        List<OrderItem> orderItems = new ArrayList<>();

        try {

            //遍历订单获取订单项
            for (OrderItemReqDto item : dto.getItems()) {
                String skuId = item.getSkuId();
                Integer quantity = item.getQuantity();
                //查数据库
                log.info("查询商品:{}", skuId);
                SKU sku = skuMapper.selectById(skuId);
                if (sku == null) {
                    throw new BizException(ErrorCodeEnum.PRO_NOT_FOUND.getCode(), ErrorCodeEnum.PRO_NOT_FOUND.getMessage());
                }
                if (sku.getStatus() != 1) {
                    throw new BizException(ErrorCodeEnum.PRO_NOT_SELL.getCode(), ErrorCodeEnum.PRO_NOT_SELL.getMessage());
                }
                if (sku.getStock() < quantity) {
                    throw new BizException(ErrorCodeEnum.STOCK_NOT_ENOUGH.getCode(), ErrorCodeEnum.STOCK_NOT_ENOUGH.getMessage());
                }
                //扣库存
                if (!stockService.deductStock(skuId, quantity)) {
                    throw new BizException(ErrorCodeEnum.STOCK_NOT_ENOUGH.getCode(), ErrorCodeEnum.STOCK_NOT_ENOUGH.getMessage());
                }
                //记录已扣订单项
                deducterStockItems.add(item);

                //计算金额
                BigDecimal itemPrice = sku.getPrice().multiply(new BigDecimal(quantity));
                totalAmount = totalAmount.add(itemPrice);
                log.info("创建订单项:{}", item.getSkuId());
                //创建订单项
                OrderItem orderItem = new OrderItem();
                orderItem.setSkuId(skuId);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(sku.getPrice());
                orderItem.setProductName(sku.getProductName());
                orderItem.setProductUrl(sku.getProductUrl());
                orderItem.setPrice(itemPrice);
                orderItems.add(orderItem);

            }


            //应付金额
            BigDecimal payableAmount = couponService.calculateAmountPayable(dto.getUsedCouponIds(), userId, totalAmount).getPayableAmount();
            //优惠金额
            BigDecimal discountAmount = totalAmount.subtract(payableAmount);


            MyOrder myOrder = new MyOrder();
            myOrder.setTotalAmount(totalAmount);
            myOrder.setPayAmount(payableAmount);
            myOrder.setUserId(userId);
            //已关闭: 0 , 待支付:1  代发货:2  待收货:3  待评价:4  完成:5
            myOrder.setStatus(1);
            myOrder.setPayType(1);
            myOrder.setAddress(dto.getAddress());
            //收货人
            myOrder.setReceiverName(dto.getReceiverName());
            myOrder.setRemark(dto.getRemark());
            myOrder.setCreateTime(LocalDateTime.now());
            myOrder.setExpireTime(LocalDateTime.now().plusMinutes(30));
            log.info("创建订单,插入数据库:{}", myOrder.getOrderId());
            orderMapper.insert(myOrder);


            List<Suborder> suborders = new ArrayList<>();
            for (OrderItemReqDto item : dto.getItems()) {
                String skuId = item.getSkuId();
                Integer quantity = item.getQuantity();
                SKU sku = skuMapper.selectById(skuId);
                Shop shop = shopMapper.selectById(sku.getShopId());
                // 对于每个商品数量，创建独立的子订单
                for (int i = 0; i < quantity; i++) {
                    //商品单价
                    BigDecimal itemPrice = sku.getPrice();

                    log.info("创建子订单项: {}, 第{}个", item.getSkuId(), i + 1);

                    //计算单个商品应付金额 = 单价 / 总金额 * 优惠金额
                    BigDecimal itemPayAmount = itemPrice.divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(payableAmount);




                    //创建子订单
                    Suborder suborder = new Suborder();
                    // 生成唯一的子订单ID，这里可以使用UUID或雪花算法
                    suborder.setUserId(userId);
                    suborder.setUserName(userName);
                    // 设置用户ID
                    suborder.setSkuId(skuId);
                    suborder.setMerchantId(shop.getMerchantId());
                    suborder.setOrderId(myOrder.getOrderId());
                    // 设置主订单ID
                    suborder.setPrice(sku.getPrice());
                    // 单价
                    suborder.setStatus(1);
                    //优惠金额
                    suborder.setDiscountAmount(itemPrice.subtract(itemPayAmount));

                    // 应付金额 = 单价 / 总金额 * 优惠金额
                    suborder.setPayAmount(itemPayAmount);

                    //店铺
                    suborder.setShopName(shop.getShopName());
                    suborder.setShopId(sku.getShopId());
                    // 商品名称
                    suborder.setProductName(sku.getProductName());
                    suborder.setSpecData(sku.getSpecData());
                    suborder.setProductUrl(sku.getProductUrl());
                    //收货地址
                    suborder.setAddress(dto.getAddress());
                    //收货人
                    suborder.setReceiverName(dto.getReceiverName());
                    suborder.setReceiverPhone(dto.getReceiverPhone());
                    //备注
                    suborder.setRemark(dto.getRemark());
                    suborder.setCreateTime(LocalDateTime.now());
                    suborder.setUpdateTime(LocalDateTime.now());
                    suborder.setAddress(dto.getAddress());
                    suborder.setRemark(dto.getRemark());
                    suborder.setPayType(1);
                  //  suborder.setLogisticCompanyName();

                    // 设置初始状态
                    suborders.add(suborder);
                }

            }
            //检验优惠卷
            AmountPayableRespDto amountPayableResp = couponService.calculateAmountPayable(dto.getUsedCouponIds(), userId, totalAmount);

            //使用的优惠卷（当用户未选择优惠券时，使用系统推荐的优惠券）
            List<String> usedCouponIds = dto.getUsedCouponIds();
            if (usedCouponIds == null || usedCouponIds.isEmpty()) {
                //使用系统推荐的优惠券
                usedCouponIds = amountPayableResp.getCouponIds();
            }
            log.info("使用的优惠卷couponId列表:{}", usedCouponIds);







            log.info("这是我的订单号:{}", myOrder.getOrderId());


            log.info("准备插入OrderItem数据库");
            for (OrderItem item : orderItems) {
                item.setOrderId(myOrder.getOrderId());
                orderItemMapper.insert(item);
            }
            log.info("准备插入Suborder数据库");
            for (Suborder item : suborders) {
                suborderMapper.insert(item);
            }


            //发送信息队列
            log.info("[缓存中心]发送订单支付信息:{}", myOrder);
            orderProducer.sendPayment(myOrder);


            boolean success = couponService.useCouponsToRecord(userId, usedCouponIds, myOrder.getOrderId());
            if (!success) {
                // 只要这里抛出异常，之前的逻辑如果涉及数据库操作都会回滚
                throw new BizException("优惠券核销失败，可能已被使用或已过期");
            }



            //返回一个订单模板信息: 主订单号,子订单号,商品信息,优惠券信息,应付金额,订单状态,过期时间

            return OrderCreateRespDto.builder()
                    .orderId(myOrder.getOrderId())
                    .userId(userId)
                    .usedCouponIds(usedCouponIds)
                    .subOrderList(suborders)
                    .build();






        } catch (Exception e) {

            //插入失败,则回滚库存
            for (OrderItemReqDto item : deducterStockItems) {
                stockService.addStock(item.getSkuId(), item.getQuantity());
            }
            throw new RuntimeException("创建订单失败", e);
        }
    }





// ... existing code ...
    /**
     * 获取订单列表
     */
    @Override
    public GetSuborderListRespDto getSuborderList() {
        String userId = SecurityUtils.getId();

        // 缓存Key：订单列表:用户ID
        String cacheKey = "order:suborder:list:" + userId;

        // 使用RedisService的通用缓存方法（防穿透+防击穿+防雪崩）
        GetSuborderListRespDto respDto = redisService.get(
                cacheKey,
                GetSuborderListRespDto.class,
                (key) -> {
                    log.info("缓存未命中，从数据库查询订单列表，userId: {}", userId);

                    // 直接查询完整的订单列表（包含物流、支付信息）
                    List<GetSuborderDetailRespDto> detailList = suborderMapper.getSuborderDetailListByUserId(userId);

                    // 创建响应对象
                    GetSuborderListRespDto dto = new GetSuborderListRespDto();

                    // 设置订单列表（查不到时设置为空列表）
                    if (detailList == null || detailList.isEmpty()) {
                        dto.setSuborderList(new ArrayList<>());
                    } else {
                        dto.setSuborderList(detailList);
                    }

                    return dto;
                },
                30
                // 缓存30分钟
        );

        log.info("获取子订单列表成功，userId: {}, 来源: 缓存", userId);
        return respDto;
    }






    /**
     * 获取订单详情
     */
    @Override
    public GetSuborderDetailRespDto getSuborderDetail(String suborderId) {

        String userId = SecurityUtils.getId();

        // 缓存Key：订单:子订单ID:用户ID
        String cacheKey = "order:suborder:" + suborderId + ":" + userId;

        // 使用RedisService的通用缓存方法（防穿透+防击穿+防雪崩）
        GetSuborderDetailRespDto respDto = redisService.get(
                cacheKey,
                GetSuborderDetailRespDto.class,
                (key) -> {
                    log.info("缓存未命中，从数据库查询子订单详情，suborderId: {}", suborderId);

                    //获取子订单数据
                    Suborder suborder = suborderMapper.selectById(suborderId);
                    if (suborder == null) {
                        throw new BizException(ErrorCodeEnum.ORDER_NOT_EXIST.getCode(), ErrorCodeEnum.ORDER_NOT_EXIST.getMessage());
                    }

                    String orderId = suborder.getOrderId();
                    //获取物流数据
                    LambdaQueryWrapper<Logistic> logisticWrapper = new LambdaQueryWrapper<>();
                    logisticWrapper.eq(Logistic::getBuyerId, userId)
                            .eq(Logistic::getOrderId, orderId)
                            .eq(Logistic::getSuborderId, suborderId);
                    Logistic logistic = logisticMapper.selectOne(logisticWrapper);


                    String spuId = skuMapper.selectById(suborder.getSkuId()).getSpuId();
                    String shopLogo = shopMapper.selectById(suborder.getShopId()).getShopLogo();

                    //获取支付单数据
                    Payment payment = paymentMapper.getPaymentByIdForUpdate(orderId, userId);
                    if (payment == null) {
                        throw new BizException(ErrorCodeEnum.PAYMENT_NOT_EXIST.getCode(), ErrorCodeEnum.PAYMENT_NOT_EXIST.getMessage());
                    }

                    GetSuborderDetailRespDto dto = new GetSuborderDetailRespDto();
                    //子订单数据
                    dto.setSuborderId(suborderId);
                    dto.setOrderId(orderId);
                    dto.setMerchantId(suborder.getMerchantId());
                    dto.setShopId(suborder.getShopId());
                    dto.setShopName(suborder.getShopName());
                    dto.setShopLogo(shopLogo);
                    dto.setSpuId(spuId);
                    dto.setSkuId(suborder.getSkuId());
                    dto.setSpecData(suborder.getSpecData());
                    dto.setProductName(suborder.getProductName());
                    dto.setProductUrl(suborder.getProductUrl());
                    dto.setPrice(suborder.getPrice());
                    dto.setDiscountAmount(suborder.getDiscountAmount());
                    dto.setPayAmount(suborder.getPayAmount());
                    dto.setStatus(suborder.getStatus());
                    dto.setRemark(suborder.getRemark());

                    //物流数据（可选，未发货时为空）
                    if (logistic != null) {
                        dto.setLogisticId(logistic.getLogisticId());
                        dto.setLogisticCompanyName(logistic.getLogisticCompanyName());
                        dto.setShipperTime(logistic.getShipperTime());
                        dto.setDeliveryTime(logistic.getDeliveryTime());
                        dto.setSignTime(logistic.getSignTime());
                    }
                        // 未发货时，使用子订单中的收货信息
                        dto.setReceiverName(suborder.getReceiverName());
                        dto.setReceiverPhone(suborder.getReceiverPhone());
                        dto.setAddress(suborder.getAddress());


                    //支付数据
                    dto.setPayType(payment.getPayType());
                    dto.setPaymentId(payment.getPaymentId());
                    dto.setPayTime(payment.getPayTime());
                    dto.setCreateTime(payment.getCreateTime());

                    return dto;
                },
                30  // 缓存30分钟
        );

        log.info("获取子订单详情成功，suborderId: {}, 来源: 缓存", suborderId);
        log.info("子订单详情: {}", respDto);
        return respDto;

    }


}

