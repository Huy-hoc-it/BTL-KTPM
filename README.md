# Cinema Backend

Backend quản lý rạp phim. Hiện đã có đăng ký, đăng nhập, JWT Bearer và API xem hồ sơ; Movie có CRUD/xóa mềm; Theater quản lý phòng, sơ đồ ghế và suất chiếu; Booking hỗ trợ tạo/xem booking và truy vấn trạng thái ghế. Phạm vi nghiệp vụ và kế hoạch được mô tả trong `docs/SPEC.md` và `docs/work_plan.md`.

## Kiến trúc

Ứng dụng dùng kiến trúc modular monolith. Mỗi module nghiệp vụ được chia thành ba tầng `api -> business -> data`: API nhận/trả JSON, business xử lý quy tắc nghiệp vụ và định nghĩa repository, data triển khai truy cập PostgreSQL bằng JPA. Các module chạy trong một Spring Boot application.

### Contract Theater và Booking

Theater cung cấp snapshot suất và ghế qua public business API; Booking kiểm tra phòng của ghế, tổng hợp trạng thái ghế và triển khai guard ngăn hủy suất đã có booking. Cả đặt vé và hủy suất khóa cùng hàng suất trong transaction. Xem [contract tích hợp](docs/theater_booking_contract.md).

## Yêu cầu

- Java 21 để chạy ứng dụng và kiểm thử bằng Maven.
- Docker Desktop hoặc Docker Engine với Docker Compose để chạy PostgreSQL, ứng dụng và các kiểm thử Testcontainers.

## Chạy kiểm thử

```powershell
.\mvnw.cmd clean verify
```

`verify` chạy kiểm thử kiến trúc, kiểm thử HTTP cho health/security/error/OpenAPI và kiểm thử PostgreSQL thật bằng Testcontainers. Docker cần hoạt động cho kiểm thử PostgreSQL.

Bản code tích hợp Theater–Booking đã qua **117 test, không lỗi, không bỏ qua**, gồm các tình huống đồng thời trên PostgreSQL (xác minh ngày 25/10/2026). Báo cáo nằm trong `target/surefire-reports` của thư mục chạy Maven.

## Chạy local bằng Maven

Khởi động PostgreSQL trước, ví dụ bằng service `db` trong Compose:

```powershell
docker compose up -d --wait db
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Compose chỉ mở cổng PostgreSQL trên `127.0.0.1:5432`. Khi chạy bằng Maven, profile `local` chỉ mở API trên `127.0.0.1`; database/user/password mặc định là `cinema`. Có thể ghi đè bằng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và `SERVER_PORT`. Nếu chủ động đặt `SERVER_ADDRESS` để mở API ra mạng, hãy đặt `JWT_SECRET` riêng đủ mạnh.

Flyway tự chạy các migration khi API khởi động. Profile `local` có thể tạo Admin demo theo biến môi trường; ứng dụng hiện không seed sẵn Customer, Movie, phòng hay suất chiếu. Tạo dữ liệu tải bằng hướng dẫn trong `load-test/README.md`.

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

Nếu đã đổi `DEMO_ADMIN_USERNAME` hoặc `DEMO_ADMIN_PASSWORD` trong `.env`, hãy dùng giá trị đó. Để tắt tài khoản demo, đặt cả hai biến thành rỗng. Tài khoản này chỉ dành cho local; có thể dùng để thử các API quản trị Movie, phòng và suất chiếu trong Swagger.

## Movie

| Method | Endpoint | Quyền |
|---|---|---|
| GET | `/api/v1/movies?page=1&pageSize=20&status=NOW_SHOWING` | Public |
| GET | `/api/v1/movies/{movieId}` | Public |
| POST | `/api/v1/admin/movies` | Admin |
| PATCH | `/api/v1/admin/movies/{movieId}` | Admin |
| DELETE | `/api/v1/admin/movies/{movieId}` | Admin |

Ví dụ tạo phim:

```powershell
$movie = @{
  title = 'Interstellar'
  description = 'Một chuyến đi tìm nơi sống mới ngoài Trái Đất.'
  durationMinutes = 169
  ageRating = 'T13'
  releaseDate = '2026-10-01'
  posterUrl = $null
  status = 'NOW_SHOWING'
} | ConvertTo-Json

Invoke-RestMethod 'http://localhost:8080/api/v1/admin/movies' -Method Post -Headers $headers -ContentType 'application/json' -Body $movie
```

`PATCH` chỉ đổi field có trong JSON; `posterUrl: null` xóa ảnh. Body rỗng, field bắt buộc null, title/description rỗng, duration không dương, ngày sai định dạng và kiểu JSON sai đều bị từ chối. `DELETE` xóa mềm nên hàng vẫn còn trong database, nhưng không còn xuất hiện ở Movie API hay contract Theater.

Danh sách và chi tiết Movie là Public. Các thao tác ghi yêu cầu Admin; Movie trả `422` cho validation, `404` với mã `MOVIE_NOT_FOUND`, `401` khi thiếu token và `403` khi token không có quyền Admin. Response lỗi dùng envelope chung và trả `X-Request-Id`.

## Kiểm thử Movie

```powershell
.\mvnw.cmd '-Dtest=MovieServiceTest,MovieApiTest' test
```

Bộ này gồm 30 unit test service và 43 API test. Xem kết quả lần chạy gần nhất và phạm vi kiểm thử ở [docs/kiem-thu-movie.md](docs/kiem-thu-movie.md).

## K6 và báo cáo hiệu năng

[load-test/README.md](load-test/README.md) hướng dẫn seed, chạy smoke/read-heavy/booking/contention, thu CPU/RAM và ghi kết quả. `smoke.js` cần JWT Admin để ghi Movie; các GET Movie được gọi không cần token.

[Báo cáo hiệu năng baseline](docs/bao-cao-hieu-nang-mau.md) tổng hợp kết quả đo đạc thực tế của Movie Read và Booking Workload theo chuẩn SPEC.

Dừng các container nhưng giữ dữ liệu PostgreSQL:

```powershell
docker compose down
```

## Tài liệu

- [Đặc tả hệ thống](docs/SPEC.md)
- [Kế hoạch phân công](docs/work_plan.md)
- [Contract tích hợp Theater–Booking](docs/theater_booking_contract.md)
- [Yêu cầu và tiến độ Booking](docs/booking_requirements.md)
