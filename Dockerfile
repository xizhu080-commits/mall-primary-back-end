# ============================================
# 多构建阶段（用 Maven 打包）
# ============================================
# ============================================
# 第1部分：构建阶段（用 Maven 打包）
# ============================================

# 1. 基础镜像：包含 Maven 和 JDK 21
FROM  maven:3.9.11-eclipse-temurin-21 AS builder


# 2. 设置容器内的工作目录
WORKDIR /build

# 3. 先复制 pom.xml（依赖配置文件）
COPY pom.xml .

# 4. 下载所有依赖包（利用缓存，源码没变就不用重下）
RUN mvn dependency:go-offline -B

# 5. 复制源码到容器
COPY src ./src

# 6. 执行打包（跳过测试）
RUN mvn package -DskipTests

# ============================================
# 第2部分：运行阶段（只复制 JAR 包）
# ============================================

# 7. 新的基础镜像：只有 JDK 21（更小）
FROM  eclipse-temurin:21-jdk-alpine


# 8. 设置容器内的工作目录
WORKDIR /app

# 9. 从构建阶段复制打包好的 JAR 文件
COPY --from=builder /build/target/*.jar app.jar

# 10. 暴露端口 8080
EXPOSE 8080

# 11. 容器默认使用 docker profile（加载 application-docker.yml）
#     运行时可用 -e SPRING_PROFILES_ACTIVE=prod 覆盖
ENV SPRING_PROFILES_ACTIVE=docker

# 12. 容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]













#
## ============================================
## 第1部分：構建階段（Maven打包）
## ============================================
#FROM maven:3.8-eclipse-temurin-21 AS builder
#WORKDIR /build
#
## 複製maven阿里源（加速容器內下載依賴，有就打開，沒註解掉）
## COPY settings.xml /root/.m2/settings.xml
#
#COPY pom.xml .
#RUN mvn dependency:go-offline -B
#COPY src ./src
#RUN mvn package -DskipTests
#
## ============================================
## 第2部分：執行階段【最小鏡像】 eclipse-temurin:21-jre-alpine
## ============================================
#FROM eclipse-temurin:21-jre-alpine
#WORKDIR /app
#
#COPY --from=builder /build/target/*.jar app.jar
#
#EXPOSE 8080
#ENTRYPOINT ["java","-jar","app.jar"]



## ============================================
## 執行階段【最小鏡像】 eclipse-temurin:21-jre-alpine
## ============================================
#FROM eclipse-temurin:21-jre-alpine
#WORKDIR /app
#
## 直接複製本地已構建的 jar 文件
#COPY target/mall-primary-back-end-0.0.1-SNAPSHOT.jar app.jar
#
#EXPOSE 8080
#ENTRYPOINT ["java","-jar","app.jar"]