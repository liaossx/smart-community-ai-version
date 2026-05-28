FROM eclipse-temurin:17

# 工作目录
WORKDIR /app

# 拷贝jar（target目录下）
COPY target/*.jar app.jar

# 启动
ENTRYPOINT ["java","-jar","/app/app.jar"]
