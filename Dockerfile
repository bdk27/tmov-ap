# 1. 建置階段 (Build Stage)
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# 跳過測試進行打包，避免雲端環境連不到資料庫導致失敗
RUN mvn clean package -DskipTests

# 2. 執行階段 (Run Stage)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# 從建置階段複製 jar 檔
COPY --from=build /app/target/*.jar app.jar

# 建立 uploads 資料夾 (避免報錯)
RUN mkdir -p uploads

# 設定環境變數 (預設 prod)
ENV SPRING_PROFILES_ACTIVE=prod

# 開放 8080 port
EXPOSE 8080

# 啟動指令
ENTRYPOINT ["java", "--enable-preview", "-Xmx380m", "-Xms380m", "-jar", "app.jar"]