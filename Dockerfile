# Stage 1: Build
FROM gradle:8.6-jdk21 AS builder
WORKDIR /home/gradle/app

# Gradle 설정 복사 및 실행 권한 부여
COPY ./src/main/resources/*.yml ./
COPY build.gradle settings.gradle ./
#COPY gradle gradle

# 종속성 캐시 생성
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY . .

# 프로젝트 빌드 (테스트 제외)
RUN gradle clean bootJar --no-daemon

# Stage 2: Run
FROM amazoncorretto:21-alpine

# 빌드된 JAR 복사
COPY --from=builder /home/gradle/app/build/libs/*.jar /app/apigateway.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/apigateway.jar"]