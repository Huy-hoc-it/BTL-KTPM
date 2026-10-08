# Cinema Backend

Backend quản lý rạp phim, hiện đang ở giai đoạn dựng nền tảng; các API nghiệp vụ như phim, suất chiếu, đặt vé và tài khoản sẽ được bổ sung theo kế hoạch trong `docs/work_plan.md`.

## Kiến trúc

Ứng dụng dùng kiến trúc modular monolith. Mỗi module nghiệp vụ được chia thành ba tầng `api -> business -> data`: API nhận/trả JSON, business xử lý quy tắc nghiệp vụ và định nghĩa repository, data triển khai truy cập PostgreSQL bằng JPA. Các module chạy trong một Spring Boot application.

## Yêu cầu

- Java 21 để chạy ứng dụng và kiểm thử bằng Maven.
- Docker Desktop hoặc Docker Engine với Docker Compose để chạy PostgreSQL, ứng dụng và các kiểm thử Testcontainers.

## Chạy kiểm thử

```powershell
.\mvnw.cmd clean verify
```

`verify` chạy kiểm thử kiến trúc, kiểm thử HTTP cho health/security/error/OpenAPI và kiểm thử PostgreSQL thật bằng Testcontainers. Docker cần hoạt động cho kiểm thử PostgreSQL.

## Chạy local bằng Maven

Khởi động PostgreSQL trước, ví dụ bằng service `db` trong Compose:

```powershell
docker compose up -d --wait db
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Compose chỉ mở cổng PostgreSQL trên `127.0.0.1:5432`. Profile `local` dùng mặc định `localhost:5432`, database/user/password là `cinema`. Có thể ghi đè bằng các biến `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và `SERVER_PORT`.

## Chạy bằng Docker Compose

```powershell
Copy-Item .env.example .env
docker compose up --build -d
```

Các giá trị trong `.env.example` chỉ dành cho phát triển local; hãy đổi mật khẩu DB nếu dùng ở môi trường khác. Compose khởi động PostgreSQL trước, chờ database healthy rồi mới chạy API. Các địa chỉ kiểm tra:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Liveness: <http://localhost:8080/actuator/health/liveness>
- Readiness: <http://localhost:8080/actuator/health/readiness>

Dừng các container nhưng giữ dữ liệu PostgreSQL:

```powershell
docker compose down
```

## Tài liệu

- [Đặc tả hệ thống](docs/SPEC.md)
- [Kế hoạch phân công](docs/work_plan.md)
