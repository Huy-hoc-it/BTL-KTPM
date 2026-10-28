# Cinema Backend

REST API quản lý rạp phim gồm đăng ký/đăng nhập, phim, phòng chiếu, suất chiếu, ghế và đặt vé. Hệ thống được xây dựng dưới dạng **modular monolith**, chạy bằng Spring Boot và PostgreSQL.

## 1. Chức năng chính

| Module | Chức năng |
|---|---|
| Identity | Đăng ký Customer, đăng nhập JWT, xem hồ sơ hiện tại |
| Movie | Xem danh sách/chi tiết phim; Admin tạo, cập nhật và xóa mềm phim |
| Theater | Quản lý phòng và sơ đồ ghế; tạo, xem và hủy suất chiếu |
| Booking | Xem trạng thái ghế; Customer tạo, xem và hủy booking của mình |

Vai trò trong hệ thống:

- **Guest:** đăng ký, đăng nhập, xem phim, suất chiếu và trạng thái ghế.
- **Customer:** có quyền của Guest và quản lý booking của chính mình.
- **Admin:** quản lý phim, phòng chiếu và suất chiếu.

## 2. Công nghệ

- Java 21, Spring Boot 3.5, Maven Wrapper.
- Spring Web MVC, Spring Security, JWT Bearer Token.
- Spring Data JPA, PostgreSQL 17, Flyway.
- Springdoc OpenAPI và Swagger UI.
- JUnit 5, Mockito, ArchUnit, Testcontainers.
- Docker, Docker Compose và GitHub Actions.

## 3. Kiến trúc

Mã nguồn được chia theo module nghiệp vụ. Mỗi module tiếp tục chia thành ba tầng:

```text
HTTP/JSON
   |
api       Controller, request/response DTO, HTTP validation
   |
business  Service, domain model, business rule, repository interface
   |
data      JPA entity, Spring Data repository, repository adapter
   |
PostgreSQL
```

Các module không truy cập trực tiếp repository của nhau. Quan hệ chính:

```text
Booking -> Theater -> Movie
Identity -> JWT principal dùng chung cho các API được bảo vệ
```

- Theater lấy thông tin phim qua `MovieModuleApi`.
- Booking lấy snapshot suất chiếu và ghế qua `ShowtimeModuleApi`.
- Booking cung cấp guard để Theater không hủy suất chiếu đã có booking.
- Luật phụ thuộc giữa các package được kiểm tra bằng ArchUnit.

Cấu trúc chính:

```text
src/main/java/com/example/cinema/
  config/                 Security, JWT, OpenAPI, seed local
  shared/api/             Error response và request ID
  modules/
    identity/{api,business,data}/
    movie/{api,business,data}/
    theater/{api,business,config,data}/
    booking/{api,business,data}/
src/main/resources/
  db/migration/           Flyway migrations
```

## 4. Chạy ứng dụng

### Yêu cầu

- JDK 21.
- Docker Desktop hoặc Docker Engine có Docker Compose.

### Chạy toàn bộ bằng Docker Compose

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up --build -d
docker compose ps
```

Ứng dụng chỉ mở cổng local tại `http://localhost:8080`; PostgreSQL tại `localhost:5432`.

Các địa chỉ kiểm tra:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Liveness: <http://localhost:8080/actuator/health/liveness>
- Readiness: <http://localhost:8080/actuator/health/readiness>

Dừng ứng dụng nhưng giữ dữ liệu:

```powershell
docker compose down
```

### Chạy API bằng Maven

Khởi động PostgreSQL trước, sau đó chạy profile `local`:

```powershell
docker compose up -d --wait db
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

### Tài khoản Admin local

`.env.example` cấu hình tài khoản demo `admin` / `demo-pass-1234`. Seeder chỉ chạy ở môi trường local/test và chỉ tạo tài khoản khi cả `DEMO_ADMIN_USERNAME` và `DEMO_ADMIN_PASSWORD` có giá trị.

Đăng nhập bằng `POST /api/v1/auth/login`, lấy `data.accessToken`, sau đó gửi header:

```http
Authorization: Bearer <access-token>
```

Không sử dụng thông tin demo hoặc `JWT_SECRET` mẫu ở môi trường thật.

## 5. Cấu hình

| Biến | Ý nghĩa | Giá trị local mặc định |
|---|---|---|
| `DB_URL` | JDBC URL của PostgreSQL | `jdbc:postgresql://localhost:5432/cinema` |
| `DB_USERNAME` | Tài khoản database | `cinema` |
| `DB_PASSWORD` | Mật khẩu database | `cinema` |
| `JWT_SECRET` | Khóa ký JWT | Bắt buộc thay ngoài local |
| `JWT_EXPIRATION_SECONDS` | Thời hạn token | `3600` |
| `SERVER_PORT` | Cổng API | `8080` |
| `DEMO_ADMIN_USERNAME` | Username Admin demo | `admin` |
| `DEMO_ADMIN_PASSWORD` | Password Admin demo | `demo-pass-1234` |

## 6. Đặc tả API

- [OpenAPI 3.1 dạng JSON](docs/openapi.json) là bản đặc tả tĩnh có thể preview trên GitHub/IDE hoặc import vào Swagger Editor, Postman và Insomnia.
- Swagger UI và `/v3/api-docs` là tài liệu sinh trực tiếp từ phiên bản ứng dụng đang chạy.
- Swagger/OpenAPI chỉ được truy cập công khai ở profile `local` và `test`.

Khi contract API thay đổi, chạy ứng dụng rồi cập nhật bản tĩnh:

```powershell
Invoke-WebRequest http://localhost:8080/v3/api-docs -OutFile docs/openapi.json
```

Nhóm endpoint chính:

| Nhóm | Prefix | Truy cập |
|---|---|---|
| Authentication | `/api/v1/auth` | Public |
| User profile | `/api/v1/users` | Đã đăng nhập |
| Movie reads | `/api/v1/movies` | Public |
| Showtime/seat reads | `/api/v1/showtimes` | Public |
| Booking | `/api/v1/bookings` | Customer sở hữu dữ liệu |
| Administration | `/api/v1/admin` | Admin |

## 7. Kiểm thử và CI

```powershell
.\mvnw.cmd clean verify
```

Lệnh `verify` chạy unit test, API test, architecture test và integration test PostgreSQL bằng Testcontainers. Docker phải hoạt động khi chạy toàn bộ bộ test. GitHub Actions cũng chạy `mvn verify` và kiểm tra Docker image có build được hay không.

## 8. Tài liệu liên quan

- [Đặc tả hệ thống](docs/SPEC.md)
- [Kế hoạch và phân công](docs/work_plan.md)
- [Quyết định kiến trúc modular monolith](docs/adr/001-modular-monolith-3-tier.md)
- [Contract tích hợp Theater–Booking](docs/theater_booking_contract.md)
- [Yêu cầu Booking](docs/booking_requirements.md)
