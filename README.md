# Cinema Backend

REST API quản lý rạp phim gồm đăng ký/đăng nhập, quản lý phim, phòng chiếu, suất chiếu, ghế và đặt vé. Hệ thống được xây dựng bằng kiến trúc **modular monolith**, chạy bằng Spring Boot và PostgreSQL.

## 1. Chức năng chính

| Module | Chức năng |
|---|---|
| Identity | Đăng ký Customer, đăng nhập JWT, xem hồ sơ hiện tại |
| Movie | Xem danh sách/chi tiết phim; Admin tạo, cập nhật và xóa mềm phim |
| Theater | Quản lý phòng và sơ đồ ghế; tạo, xem và hủy suất chiếu |
| Booking | Xem trạng thái ghế; Customer tạo, xem và hủy booking của mình |

Vai trò trong hệ thống:

- **Guest:** đăng ký, đăng nhập, xem phim, xem suất chiếu và trạng thái ghế.
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

Triển khai theo kiến trúc modular monolith, chia module theo miền nghiệp vụ. Mỗi module triển khai theo kiến trúc 3-layer:

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

Các module không truy cập trực tiếp repository của nhau. Giữa các module giao tiếp thông qua Java interface nội bộ:

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



## 5. Đặc tả API

- Có bản đặc tả api [OpenAPI 3.1 dạng JSON](docs/openapi.json)
- Swagger UI và `/v3/api-docs` là tài liệu sinh trực tiếp từ phiên bản ứng dụng đang chạy.
- Swagger/OpenAPI chỉ được truy cập công khai ở profile `local` và `test`.

Khi contract API thay đổi, chạy ứng dụng rồi cập nhật bản tĩnh:

```powershell
Invoke-WebRequest http://localhost:8080/v3/api-docs -OutFile docs/openapi.json
```

### Danh sách API

| Module | Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|---|
| Identity | `POST` | `/api/v1/auth/register` | Public | Đăng ký tài khoản mới với role `CUSTOMER` |
| Identity | `POST` | `/api/v1/auth/login` | Public | Xác thực username/password và cấp JWT access token |
| Identity | `GET` | `/api/v1/users/me` | Đã đăng nhập | Lấy hồ sơ và role của người dùng hiện tại |
| Movie | `GET` | `/api/v1/movies?page=1&pageSize=20&status=NOW_SHOWING` | Public | Xem danh sách phim chưa xóa, hỗ trợ phân trang và lọc trạng thái |
| Movie | `GET` | `/api/v1/movies/{movieId}` | Public | Xem chi tiết một phim chưa bị xóa mềm |
| Movie | `POST` | `/api/v1/admin/movies` | Admin | Tạo phim mới |
| Movie | `PATCH` | `/api/v1/admin/movies/{movieId}` | Admin | Cập nhật các field được gửi; field không gửi được giữ nguyên |
| Movie | `DELETE` | `/api/v1/admin/movies/{movieId}` | Admin | Xóa mềm phim bằng cách ghi `deletedAt` |
| Theater | `GET` | `/api/v1/admin/auditoriums` | Admin | Xem các phòng đang hoạt động cùng toàn bộ sơ đồ ghế |
| Theater | `POST` | `/api/v1/admin/auditoriums` | Admin | Tạo phòng chiếu và sinh các ghế từ seat layout |
| Theater | `DELETE` | `/api/v1/admin/auditoriums/{id}` | Admin | Xóa mềm phòng nếu phòng không còn suất chiếu tương lai |
| Theater | `POST` | `/api/v1/admin/showtimes` | Admin | Tạo suất chiếu, tính giờ kết thúc và kiểm tra trùng lịch |
| Theater | `DELETE` | `/api/v1/admin/showtimes/{id}` | Admin | Hủy suất chiếu tương lai nếu chưa có booking |
| Theater | `GET` | `/api/v1/showtimes?movieId={uuid}&date=YYYY-MM-DD` | Public | Xem các suất `SCHEDULED` của một phim trong ngày UTC |
| Theater | `GET` | `/api/v1/showtimes/{id}` | Public | Xem chi tiết suất chiếu, kể cả suất đã hủy hoặc kết thúc |
| Booking | `GET` | `/api/v1/showtimes/{id}/seats` | Public | Xem sơ đồ ghế và trạng thái `AVAILABLE`/`BOOKED` của một suất |
| Booking | `POST` | `/api/v1/bookings` | Đã đăng nhập | Đặt từ 1–8 ghế, kiểm tra ghế trống và tính tổng tiền |
| Booking | `GET` | `/api/v1/bookings` | Đã đăng nhập | Xem danh sách booking của người dùng hiện tại |
| Booking | `GET` | `/api/v1/bookings/{bookingId}` | Chủ booking | Xem chi tiết booking thuộc người dùng hiện tại |
| Booking | `DELETE` | `/api/v1/bookings/{bookingId}` | Chủ booking | Hủy booking và giải phóng các ghế đã đặt |

### Endpoint hệ thống và tài liệu

| Method | Endpoint | Chức năng |
|---|---|---|
| `GET` | `/actuator/health/liveness` | Kiểm tra tiến trình ứng dụng còn hoạt động |
| `GET` | `/actuator/health/readiness` | Kiểm tra ứng dụng và database đã sẵn sàng phục vụ |
| `GET` | `/swagger-ui.html` | Giao diện xem và thử API ở profile `local`/`test` |
| `GET` | `/v3/api-docs` | Đặc tả OpenAPI được sinh từ ứng dụng đang chạy |

## 6. Kiểm thử và CI

```powershell
.\mvnw.cmd clean verify
```

Lệnh `verify` chạy unit test, API test, architecture test và integration test PostgreSQL bằng Testcontainers. Docker phải hoạt động khi chạy toàn bộ bộ test. GitHub Actions cũng chạy `mvn verify` và kiểm tra Docker image có build được hay không.

## 7. Kiểm thử tải trên Kaggle CPU

Kiểm thử trên Kaggle CPU (4 vCPU, 30 GiB RAM), Java 21 và PostgreSQL 16. Hai kịch bản **Movie Read** và **Booking** chạy ở mức 10/25/50 người dùng ảo (VU), mỗi mức 3 lượt; mỗi lượt warm-up 30 giây, đo 120 giây và think time 100 ms. API, database và k6 cùng chạy trong một phiên Kaggle.

Bảng dưới là **median của ba lượt**; độ trễ tính bằng mili giây (ms).

| Kịch bản | VU | Request/giây | GET p95 (ms) | POST booking p95 (ms) |
|---|---:|---:|---:|---:|
| Movie Read | 10 | 95,88 | 5,40 | — |
| Movie Read | 25 | 239,81 | 5,47 | — |
| Movie Read | 50 | 477,65 | 7,12 | — |
| Booking | 10 | 93,88 | 6,22 | 18,65 |
| Booking | 25 | 236,10 | 4,84 | 15,71 |
| Booking | 50 | 465,12 | 9,36 | 22,83 |

**18/18 lượt đạt ngưỡng k6**; lỗi kỹ thuật và nghiệp vụ thực tế đều 0%. Ngưỡng đánh giá là GET p95 < 500 ms, POST booking p95 < 1.000 ms và tỷ lệ mỗi loại lỗi < 1%.

Kết quả áp dụng cho điều kiện tải đã đo; chưa xác định tải bão hòa. Kịch bản tranh ghế (contention) chưa được chạy.

[CSV 18 lượt](docs/performance/kaggle-cpu-2026-10-29-summary.csv) · [Môi trường và ngưỡng từng lượt](docs/performance/kaggle-cpu-2026-10-29-evidence.json) · [Notebook](load-test/cinema-kaggle-cpu.ipynb) · [Hướng dẫn chạy k6](load-test/README.md)

## 9. Tài liệu liên quan

- [Đặc tả hệ thống](docs/SPEC.md)
- [Quyết định kiến trúc modular monolith](docs/adr/001-modular-monolith-3-tier.md)
