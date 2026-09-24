# Kế hoạch tạo base codebase

> Phạm vi: CB-01 đến CB-09\
> Người thực hiện chính: Người 1\
> Thời gian dự kiến: 1,5–2 ngày\
> Kết quả: một Spring Boot application chạy được, chưa có đầy đủ nghiệp vụ

## 1. Mục tiêu hoàn thành

Base codebase được coi là hoàn thành khi:

- `./mvnw verify` hoặc `.\mvnw.cmd verify` chạy thành công.
- `docker compose up --build` khởi động được API và PostgreSQL.
- Flyway kết nối và kiểm tra migration thành công.
- Liveness và readiness trả HTTP 200.
- Swagger UI và OpenAPI JSON truy cập được.
- Public endpoint được phép truy cập; endpoint còn lại bị chặn khi chưa đăng nhập.
- Error response có cùng một định dạng.
- Architecture test và integration smoke test chạy được.
- CI thực hiện build và test thành công.

Base codebase chưa cần Register, Login, Movie CRUD, Showtime hoặc Booking. Các chức năng đó thuộc module do từng thành viên triển khai sau.

Quy ước kiến trúc: Modular Monolith; mỗi module nghiệp vụ chia ba tầng `api -> business -> data`. `business` định nghĩa repository interface, còn `data` cung cấp implementation bằng JPA. Không tạo thêm `port`, `adapter`, `command` hoặc `query` khi chưa có nhu cầu thực tế.

## 2. Thứ tự thực hiện

```text
CB-01 -> CB-02 -> CB-03 -> CB-04
                            |
                            v
CB-05 -> CB-06 -> CB-07 -> CB-08 -> CB-09
```

Không bắt đầu CB-05 trước khi ứng dụng kết nối được PostgreSQL ở CB-04.

## 3. CB-01 — Khởi tạo Spring Boot

### Công việc

Khởi tạo Maven project với:

```text
groupId:    com.example
artifactId: cinema-backend
package:    com.example.cinema
Java:       21
build:      Maven
```

Đặt main class tại package gốc để Spring tự quét tất cả module ngang hàng:

```text
src/main/java/com/example/cinema/CinemaApplication.java
```

Không đặt main class trong `config`, vì mặc định Spring chỉ component-scan các package con của package chứa main class.

### Đầu ra

```text
pom.xml
mvnw
mvnw.cmd
.mvn/wrapper/
src/main/java/com/example/cinema/CinemaApplication.java
src/main/resources/application.yml
.gitignore
```

### Kiểm tra

```powershell
.\mvnw.cmd clean package
```

### Hoàn thành khi

- Maven Wrapper hoạt động.
- Project compile bằng Java 21.
- Có thể chạy main class mà không lỗi compile.

## 4. CB-02 — Thêm dependency tối thiểu

### Dependency runtime

- `spring-boot-starter-web` và `spring-boot-starter-actuator` đã được thêm ở CB-01 để có functional test khởi động ứng dụng.
- `spring-boot-starter-validation`
- `springdoc-openapi-starter-webmvc-ui`

Thêm ở bước phụ thuộc tương ứng, để ứng dụng không cần cấu hình giả chỉ để khởi động:

- `spring-boot-starter-data-jpa`, `flyway-core`, Flyway PostgreSQL support và PostgreSQL JDBC driver: CB-04, cùng PostgreSQL và cấu hình datasource.
- `spring-boot-starter-security`: CB-06, cùng security rule tập trung.

### Dependency test

- `spring-boot-starter-test`

Testcontainers PostgreSQL và Spring Boot Testcontainers support được thêm ở CB-04 để kiểm thử Flyway trên database thật. ArchUnit được thêm ở CB-08; các rule bắt đầu kiểm tra module khi có class thật.

### Không thêm

- Redis, Kafka hoặc RabbitMQ.
- MapStruct/Lombok nếu chưa có code cần chúng.
- Thư viện JWT riêng.
- Cache, tracing hoặc metrics backend ngoài Actuator.

### Kiểm tra

```powershell
.\mvnw.cmd dependency:tree
.\mvnw.cmd clean compile
```

### Hoàn thành khi

- Dependency resolve thành công.
- Không có dependency chưa phục vụ CB-01 đến CB-09.

## 5. CB-03 — Cấu hình ứng dụng

### Đầu ra

```text
src/main/resources/application.yml
src/main/resources/application-local.yml
src/main/resources/application-test.yml
.env.example
```

### Cấu hình bắt buộc

- Server port.
- Datasource URL, username, password từ environment variable.
- Flyway enabled.
- Hibernate `ddl-auto=validate`.
- UTC cho JDBC/Hibernate.
- Actuator liveness; readiness gồm trạng thái ứng dụng và kết nối PostgreSQL.
- Swagger/OpenAPI path.
- Không trả stack trace trong response production.

Biến môi trường mẫu:

```dotenv
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080
DB_URL=jdbc:postgresql://localhost:5432/cinema
DB_USERNAME=cinema
DB_PASSWORD=cinema
JWT_SECRET=replace-with-at-least-32-random-bytes
JWT_EXPIRATION_SECONDS=3600
```

`JWT_SECRET` chỉ được khai báo để thống nhất cấu hình; chưa viết JWT implementation ở bước này.

`application.yml` yêu cầu biến môi trường DB thay vì dùng credential mặc định. Profile `local` và `test` mới cung cấp giá trị mặc định dành riêng cho phát triển/kiểm thử. Property Flyway và JPA chưa có hiệu lực vì dependency database được thêm ở CB-04.

`ApplicationSmokeTest` loại trừ `DataSourceAutoConfiguration` vì chỉ kiểm tra web/health/OpenAPI. `PostgreSqlIntegrationTest` tại CB-04 dùng PostgreSQL Testcontainers để kiểm tra Flyway, nên `mvn verify` không phụ thuộc PostgreSQL cài ngoài nhưng cần Docker chạy.

### Hoàn thành khi

- Không có secret thật trong Git.
- Profile `local` và `test` tách biệt.
- Tên environment variable thống nhất với Docker Compose.

## 6. CB-04 — PostgreSQL, Flyway và Docker

### Đầu ra

```text
Dockerfile
compose.yaml
.dockerignore
src/main/resources/db/migration/
src/test/java/com/example/cinema/integration/PostgreSqlIntegrationTest.java
```

### Docker Compose

Chỉ gồm hai container:

```text
api
db
```

Yêu cầu:

- PostgreSQL có healthcheck.
- API chỉ khởi động sau khi DB healthy.
- API nhận cấu hình DB qua environment variable.
- Dockerfile multi-stage và runtime chạy bằng non-root user.

Chưa tạo migration nghiệp vụ giả. Migration đầu tiên sẽ do module `identity` tạo khi triển khai bảng `users`. Flyway vẫn phải khởi động thành công với zero migration.

### Kiểm tra

```powershell
.\mvnw.cmd clean verify
docker compose config
docker compose up --build -d
Invoke-RestMethod http://localhost:8080/actuator/health/liveness
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
docker compose down
```

### Hoàn thành khi

- PostgreSQL healthy.
- API kết nối được PostgreSQL.
- Log cho thấy Flyway khởi động thành công.
- `PostgreSqlIntegrationTest` xác nhận Flyway tạo `flyway_schema_history` trên PostgreSQL Testcontainers.
- `docker compose down` dừng hệ thống sạch sẽ.

## 7. CB-05 — Web baseline và tài liệu API

### Công việc

Cấu hình:

- Swagger UI.
- OpenAPI JSON.
- Actuator liveness; readiness gồm trạng thái ứng dụng và kết nối PostgreSQL.
- Tên và phiên bản API cơ bản.
- Base URL nghiệp vụ `/api/v1`.

Không cần tự viết health controller; dùng Spring Boot Actuator.

### Đầu ra

```text
src/main/java/com/example/cinema/config/OpenApiConfig.java
```

OpenAPI có title `Cinema API`, version `v1`. Base URL `/api/v1` là quy ước cho các controller nghiệp vụ; chưa tạo endpoint giả khi chưa có module.

### Endpoint kiểm tra

```text
GET /swagger-ui/index.html
GET /v3/api-docs
GET /actuator/health/liveness
GET /actuator/health/readiness
```

### Kiểm tra

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health/liveness
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
Invoke-RestMethod http://localhost:8080/v3/api-docs
curl.exe --fail --silent http://localhost:8080/swagger-ui/index.html
```

### Hoàn thành khi

- Hai health endpoint trả trạng thái `UP`; readiness chỉ `UP` khi cả ứng dụng và PostgreSQL sẵn sàng.
- `PostgreSqlIntegrationTest` gọi readiness qua HTTP và xác nhận component `db` là `UP`.
- OpenAPI JSON hợp lệ, có title `Cinema API` và version `v1`.
- Swagger UI mở được trên trình duyệt.

## 8. CB-06 — Security baseline

### Đầu ra

```text
src/main/java/com/example/cinema/config/SecurityConfig.java
```

### Quy tắc ban đầu

Cho phép không cần token:

```text
/api/v1/auth/**
/swagger-ui/**
/v3/api-docs/**
/actuator/health/liveness
/actuator/health/readiness
```

Swagger UI và OpenAPI JSON chỉ public trong profile `local`/`test`; ở các profile khác chúng yêu cầu xác thực.

Các endpoint nghiệp vụ public như GET Movie/Showtime sẽ được thêm khi module tương ứng tồn tại.

Yêu cầu:

- Stateless session.
- Tắt form login và HTTP Basic.
- Tắt CSRF cho JSON REST API stateless.
- `/api/v1/admin/**` dành cho `ADMIN` khi JWT được hoàn thiện.
- Các request còn lại yêu cầu authentication.

Chưa tạo `JwtAuthenticationFilter`, token encoder/decoder hoặc login giả. Module `identity` sẽ thêm JWT khi có User và method đăng nhập thật trong business service.

### Kiểm tra

- Swagger trong profile `local`/`test` và healthcheck không trả 401; Swagger trong profile khác yêu cầu xác thực.
- Một URL không được public trả 401/403, không redirect sang trang login.

### Hoàn thành khi

- Security rule nằm tập trung trong một cấu hình.
- Controller không tự đọc hoặc kiểm tra token.
- `ApplicationSmokeTest` xác nhận endpoint không public trả `401` và không redirect.

## 9. CB-07 — Error response chung

### Đầu ra tối thiểu

```text
src/main/java/com/example/cinema/shared/api/ErrorResponse.java
src/main/java/com/example/cinema/shared/api/GlobalExceptionHandler.java
src/main/java/com/example/cinema/shared/api/RequestIdFilter.java
```

Error response:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request is invalid",
    "details": [],
    "requestId": "uuid"
  }
}
```

`X-Request-Id` hợp lệ là UUID dạng chuẩn. Mỗi response trả ID này trong header; error body dùng cùng ID. Header không hợp lệ hoặc thiếu thì server tự sinh UUID.

Xử lý trước các nhóm:

| Trường hợp | HTTP |
|---|---:|
| JSON sai cú pháp/sai kiểu hoặc query parameter sai kiểu | 400 |
| Bean Validation thất bại | 422 |
| Chưa xác thực | 401 |
| Không đủ quyền | 403 |
| Lỗi không dự kiến | 500 |

401/403 xảy ra trong security filter chain nên dùng `AuthenticationEntryPoint` và `AccessDeniedHandler`, không trông chờ `@RestControllerAdvice` bắt chúng.

Chưa tạo cây exception nghiệp vụ chung. Từng module tự định nghĩa lỗi nghiệp vụ, sau đó bổ sung mapping cần thiết.

### Hoàn thành khi

- Mọi lỗi nền tảng trả JSON cùng schema.
- Response production không chứa stack trace hoặc SQL.

## 10. CB-08 — Architecture rule

### Dependency bổ sung

- ArchUnit JUnit 5.

Testcontainers PostgreSQL đã được cài ở CB-04 và dùng bởi `PostgreSqlIntegrationTest`.

### Đầu ra

```text
src/test/java/com/example/cinema/architecture/ArchitectureTest.java
```

Architecture rule tối thiểu:

- `business` không phụ thuộc Spring Web/HTTP, Jakarta Servlet/Persistence, Spring Data, Hibernate, driver database hoặc `data`.
- `api` không gọi trực tiếp `data`; mọi luồng đi qua `business`.
- Module không truy cập package `api` hoặc `data` của module khác.
- Không có dependency vòng giữa các module.

Không viết unit test giả chỉ để tăng coverage.

### Kiểm tra

```powershell
.\mvnw.cmd test
```

### Hoàn thành khi

- Architecture rule sẽ tự áp dụng khi các module bắt đầu có class thật.

## 11. CB-09 — CI, README và kiểm tra cuối

### Đầu ra

```text
.github/workflows/ci.yml
README.md
```

### CI tối thiểu

```text
checkout
setup Java 21
cache Maven
./mvnw verify
docker build .
```

Không thêm deploy, release automation hoặc nhiều môi trường ở Pha 1.

### README tối thiểu

- Yêu cầu Java 21 và Docker.
- Cách tạo `.env` từ `.env.example`.
- Cách chạy bằng Maven.
- Cách chạy bằng Docker Compose.
- Link Swagger và healthcheck.
- Cách chạy test.
- Link đến `docs/SPEC.md` và `docs/work_plan.md`.

### Kiểm tra cuối

```powershell
.\mvnw.cmd clean verify
docker compose up --build -d
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
Invoke-RestMethod http://localhost:8080/v3/api-docs
docker compose down
```

### Hoàn thành khi

- Tất cả lệnh trên chạy thành công từ một checkout sạch.
- Không cần thao tác tạo database thủ công.
- Không có secret hoặc file build được commit.
- Người khác clone repository và chạy được theo README.

## 12. Checklist bàn giao

- [x] CB-01 Maven/Spring Boot bootstrap.
- [x] CB-02 Dependency tối thiểu.
- [x] CB-03 Application configuration.
- [x] CB-04 PostgreSQL, Flyway và Docker.
- [x] CB-05 Swagger và healthcheck.
- [x] CB-06 Security baseline.
- [x] CB-07 Error response chung.
- [x] CB-08 Architecture/integration smoke test.
- [ ] CB-09 CI, README và kiểm tra cuối: đã kiểm tra local, còn xác nhận CI và checkout sạch sau khi push lên GitHub.

Sau khi checklist hoàn thành, merge base codebase vào nhánh chung. Bốn thành viên tạo feature branch từ cùng commit này và bắt đầu module được phân công.
