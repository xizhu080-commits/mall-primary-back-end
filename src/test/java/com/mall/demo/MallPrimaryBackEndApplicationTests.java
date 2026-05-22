package com.mall.demo; // 确保包名和你的项目一致

import com.mall.demo.module.product.entity.SPU;
import com.mall.demo.module.user.entity.User;
import com.mall.demo.mq.producer.ProductProducer;
import com.mall.demo.mq.producer.UserProducer; // 引入生产者
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

// 1. 这个注解会启动整个 Spring 容器，加载 Redis、MQ 等所有配置
@Component
@SpringBootTest
class MallPrimaryBackEndApplicationTests {

    // 2. 把生产者注入进来
    @Autowired
    private UserProducer userProducer;

    @Test
    void testMqAndRedis() throws InterruptedException {
        System.out.println("🚀 测试开始：准备发送用户注册消息...");

        // 3. 调用发送方法，模拟用户ID为 8888
        User user = new User();
        user.setUserId("88888");
        userProducer.sendRegister(user);

        System.out.println("✅ 消息已发送。等待消费者处理...");

        // 4. 重要：让消费者有时间跑完（因为MQ是异步的）
        // 如果主线程立刻结束，可能看不到消费结果
        Thread.sleep(3000);
        System.out.println("✅ 测试结束。");
    }

    // 原来的空测试可以留着，也可以删掉
    @Test
    void contextLoads() {
    }
}

/*



@SpringBootTest
class MallPrimaryBackEndApplicationTests2 {

    // 2. 把生产者注入进来
    @Autowired
    private ProductProducer productProducer;

    @Test
    void testMqAndRedis() throws InterruptedException {
        System.out.println("🚀 测试开始：准备发送用户注册消息...");

        // 3. 调用发送方法，模拟商品ID为 8888
        SPU spu = new SPU();
        spu.setProductId(8888L);
        productProducer.sendPublishSpu(spu);

        System.out.println("✅ 消息已发送。等待消费者处理...");

        // 4. 重要：让消费者有时间跑完（因为MQ是异步的）
        // 如果主线程立刻结束，可能看不到消费结果
        Thread.sleep(3000);
    }

    // 原来的空测试可以留着，也可以删掉
    @Test
    void contextLoads() {
    }
}*/
