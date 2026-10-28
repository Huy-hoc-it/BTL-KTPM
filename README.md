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

## 8. Kết quả kiểm thử tải trên Kaggle CPU

Ngày **29/10/2026** (Asia/Saigon), dự án đã hoàn tất **18/18 lượt đo đạt toàn bộ ngưỡng k6**, gồm Movie Read và Booking, mỗi kịch bản chạy 10/25/50 người dùng ảo (VU), ba lượt cho mỗi mức. CSV tổng hợp đã được đối chiếu với 18 summary JSON trong gói `cinema-kaggle-results.zip`.

### Môi trường và cách đo

| Mục | Cấu hình |
|---|---|
| Commit được đo | `36c892288a89110b203dfa1d2cf4470a14617d88` |
| Nền tảng | Kaggle Notebook, Accelerator = None, Linux x86_64 |
| CPU | Intel Xeon 2.20 GHz, 4 CPU logic, quota 4 vCPU |
| Giới hạn RAM | 30 GiB |
| Công cụ | OpenJDK 21.0.12.1, Python 3.13.15, k6 2.3.0 |
| PostgreSQL | 16.15, xác nhận bằng `SELECT version()` trong Notebook; runner ghi phiên bản DB là `not available` |
| JVM / connection pool | `-Xms256m -Xmx768m`; HikariCP mặc định 10 kết nối |
| Vị trí tiến trình | API, PostgreSQL và k6 cùng chạy trong phiên Kaggle, gọi API qua `127.0.0.1:8080` |
| Thời lượng mỗi lượt | Warm-up 30 giây, đo 120 giây, think time 100 ms |
| Dataset seed | 1.000 phim (50 xóa mềm), 1 phòng với 109 ghế, 2 suất chiếu, 100 user tải và 50 user dành cho contention |

Seed được tạo một lần cho cả baseline; user/ghế warm-up tách khỏi user/ghế đo. Smoke test tạo và xóa mềm thêm một phim trước khi đo. Các request setup, warm-up và cleanup không được tính vào throughput nghiệp vụ trong bảng dưới.

- **Movie Read:** 50% GET danh sách phim, 50% GET chi tiết phim.
- **Booking:** 35% danh sách phim, 25% lịch chiếu, 25% trạng thái ghế, 10% POST booking và 5% GET booking của user đã đăng nhập. Booking mẫu được hủy trước lần đặt tiếp theo và khi teardown.

### Kết quả tổng hợp

Mỗi giá trị là **median của ba lượt** ở cùng mức tải; median của ba p95 không phải p95 gộp toàn bộ request. Throughput được tính bằng `measured_requests / 120`; các percentile lấy từ metric riêng của pha đo. Độ trễ tính bằng mili giây (ms).

| Kịch bản | VU | Request/giây | GET p50 | GET p95 | GET p99 | POST booking p95 |
|---|---:|---:|---:|---:|---:|---:|
| Movie Read | 10 | 95,88 | 3,19 | 5,40 | 7,19 | — |
| Movie Read | 25 | 239,81 | 3,13 | 5,47 | 8,15 | — |
| Movie Read | 50 | 477,65 | 3,33 | 7,12 | 11,20 | — |
| Booking | 10 | 93,88 | 3,51 | 6,22 | 8,36 | 18,65 |
| Booking | 25 | 236,10 | 3,31 | 4,84 | 7,21 | 15,71 |
| Booking | 50 | 465,12 | 3,86 | 9,36 | 16,52 | 22,83 |

Trong cả 18 lượt, tỷ lệ lỗi kỹ thuật và lỗi nghiệp vụ đều **0%**, không có HTTP status ngoài kỳ vọng và checks của pha đo đạt **100%**. Các ngưỡng của script đều đạt: p95 GET < 500 ms, p95 POST booking < 1.000 ms, lỗi kỹ thuật/nghiệp vụ < 1%. Đây là ngưỡng dự án chọn để đánh giá baseline.

Kết quả cho thấy hệ thống đáp ứng các mức tải đã đo trên phiên Kaggle này. Bài đo dùng mô hình closed loop có think time 100 ms, chưa xác định tải bão hòa hay công suất production. Kịch bản tranh ghế (contention) chưa được chạy trong baseline này.

### Số liệu và cách chạy lại

- [CSV kết quả đầy đủ 18 lượt](docs/performance/kaggle-cpu-2026-10-29-summary.csv).
- [Thông tin môi trường, ngưỡng từng lượt và SHA-256 của gói kết quả](docs/performance/kaggle-cpu-2026-10-29-evidence.json).
- [Notebook Kaggle](https://www.kaggle.com/code/thaidao2k6/cinema-phase-1-kaggle-cpu-baseline) và [bản Notebook trong repository](load-test/cinema-kaggle-cpu.ipynb).
- [Hướng dẫn các kịch bản k6 và runner](load-test/README.md).

Gói kết quả gốc chứa 18 raw JSON, 18 CSV tài nguyên, summary và log. Hai thư mục lượt đo là `20261028-174115` (Movie Read) và `20261028-180352` (Booking); tên thư mục dùng UTC, tương ứng ngày 29/10/2026 tại Việt Nam. Baseline Kaggle được ghi riêng với [báo cáo Windows/Docker trước đó](docs/bao-cao-hieu-nang-mau.md).

## 9. Tài liệu liên quan

- [Đặc tả hệ thống](docs/SPEC.md)
- [Kế hoạch và phân công](docs/work_plan.md)
- [Quyết định kiến trúc modular monolith](docs/adr/001-modular-monolith-3-tier.md)
- [Contract tích hợp Theater–Booking](docs/theater_booking_contract.md)
- [Yêu cầu Booking](docs/booking_requirements.md)
