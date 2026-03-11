# 多阶段构建：先构建，再运行
# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# 复制 Maven Wrapper 文件并赋予执行权限
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# 复制 pom.xml 先下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建非 root 用户运行应用
RUN addgroup -S spring && adduser -S spring -G spring

# 从构建阶段复制 JAR 文件
COPY --from=builder /build/target/*.jar app.jar

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apk del tzdata

# 切换到非 root 用户
USER spring:spring

# 暴露端口
EXPOSE 8080

# JVM 优化参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]