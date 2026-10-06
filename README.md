# Cinema Backend

Backend quản lý rạp phim. Hiện đã có đăng ký, đăng nhập, JWT Bearer và API xem hồ sơ người dùng; các API phim, suất chiếu và đặt vé đang được triển khai theo kế hoạch trong `docs/work_plan.md`.

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

Compose chỉ mở cổng PostgreSQL trên `127.0.0.1:5432`. Khi chạy bằng Maven, profile `local` chỉ mở API trên `127.0.0.1`; database/user/password mặc định là `cinema`. Có thể ghi đè bằng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và `SERVER_PORT`. Nếu chủ động đặt `SERVER_ADDRESS` để mở API ra mạng, hãy đặt `JWT_SECRET` riêng đủ mạnh.

## Chạy bằng Docker Compose

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up --build -d
```

Các giá trị trong `.env.example` chỉ dành cho phát triển local; hãy đổi mật khẩu DB nếu dùng ở môi trường khác. Compose khởi động PostgreSQL trước, chờ database healthy rồi mới chạy API. API lắng nghe trên mọi địa chỉ **bên trong container** để Compose chuyển tiếp cổng, nhưng cổng trên máy chỉ mở ở `127.0.0.1:8080`. Các địa chỉ kiểm tra:

`DEMO_ADMIN_USERNAME` và `DEMO_ADMIN_PASSWORD` trong `.env` tạo tài khoản Admin demo ở profile `local`. Tài khoản được tạo một lần, mật khẩu phải dài 8–16 ký tự và được lưu bằng BCrypt. Đặt cả hai biến thành rỗng để bỏ qua seeding. Seeder không chạy ở profile `prod`.

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Liveness: <http://localhost:8080/actuator/health/liveness>
- Readiness: <http://localhost:8080/actuator/health/readiness>

### Thử đăng nhập bằng Swagger

Trong `.env.example`, tài khoản Admin demo mặc định là `admin` / `demo-pass-1234`. Sau khi Compose khởi động:

1. Mở Swagger UI, gọi `POST /api/v1/auth/login` với JSON:

   ```json
   {"username":"admin","password":"demo-pass-1234"}
   ```

2. Sao chép `data.accessToken` trong response, bấm **Authorize** và dán riêng token (không thêm tiền tố `Bearer`).
3. Gọi `GET /api/v1/users/me`; response cần có `data.role: "ADMIN"`.

Nếu đã đổi `DEMO_ADMIN_USERNAME` hoặc `DEMO_ADMIN_PASSWORD` trong `.env`, hãy dùng giá trị đó. Để tắt tài khoản demo, đặt cả hai biến thành rỗng. Tài khoản này chỉ dành cho local; API quản trị nghiệp vụ sẽ được thêm cùng các module tương ứng.

Dừng các container nhưng giữ dữ liệu PostgreSQL:

```powershell
docker compose down
```

## Tài liệu

- [Đặc tả hệ thống](docs/SPEC.md)
- [Kế hoạch phân công](docs/work_plan.md)
- [Các bước dựng codebase](docs/codebase_setup.md)
