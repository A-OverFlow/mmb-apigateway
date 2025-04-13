# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Gradle 설정 복사 및 실행 권한 부여
COPY gradlew ./
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .
RUN chmod +x gradlew

# 종속성 캐시 생성
RUN ./gradlew dependencies

# 소스 코드 복사
COPY src src

# 프로젝트 빌드 (테스트 제외)
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar /app/apigateway.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/apigateway.jar"]