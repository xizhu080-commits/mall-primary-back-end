/*
package com.mall.demo.common.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitBaseConfig {


   */
/* @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }*//*



    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();

        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator
                .builder()
                .allowIfBaseType(Object.class)
                .build();

        objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);

        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
*/



package com.mall.demo.common.config;


import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitBaseConfig {

    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        converter.setClassMapper(typeMapper());

        return converter;
    }

    @Bean
    public org.springframework.amqp.support.converter.DefaultClassMapper typeMapper() {
        org.springframework.amqp.support.converter.DefaultClassMapper mapper =
            new org.springframework.amqp.support.converter.DefaultClassMapper();

        mapper.setIdClassMapping(java.util.Map.of(
            "com.mall.demo.module.coupon.entity.CouponUser",
            com.mall.demo.module.coupon.entity.CouponUser.class,
            "com.mall.demo.module.coupon.entity.CouponTemplate",
            com.mall.demo.module.coupon.entity.CouponTemplate.class,
            "com.mall.demo.module.product.entity.SPU",
            com.mall.demo.module.product.entity.SPU.class,
            "com.mall.demo.module.product.entity.SKU",
            com.mall.demo.module.product.entity.SKU.class,
            "com.mall.demo.module.user.entity.User",
            com.mall.demo.module.user.entity.User.class,
            "com.mall.demo.module.order.entity.MyOrder",
            com.mall.demo.module.order.entity.MyOrder.class,
            "com.mall.demo.module.logistic.entity.Logistic",
            com.mall.demo.module.logistic.entity.Logistic.class
        ));

        return mapper;
    }

}
