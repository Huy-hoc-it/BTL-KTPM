# Đặc tả backend quản lý rạp phim

> Phiên bản: 1.1\
> Phạm vi: Pha 1 và nền tảng đo lường cho Pha 2\
> Kiến trúc: Modular Monolith kết hợp 3-tier\
> Giao diện: Swagger UI/OpenAPI, chưa xây dựng frontend

## 1. Mục tiêu

Xây dựng một REST API đơn giản cho một rạp phim, cho phép:

- Khách xem phim, lịch chiếu và trạng thái ghế.
- Người dùng đăng ký, đăng nhập, đặt ghế, xem và hủy vé của mình.
- Quản trị viên quản lý phim, phòng chiếu và suất chiếu.
- API trao đổi JSON và có tài liệu tương tác bằng Swagger UI.
- Ứng dụng chạy bằng Docker và có kịch bản kiểm thử tải trên Kaggle CPU.

Pha 1 ưu tiên tính đúng đắn, ranh giới module rõ, dễ kiểm thử và dễ giải thích. Cấu trúc phải cho phép nhiều thành viên làm song song mà ít đụng cùng package. Ranh giới module cũng cần đủ rõ để có thể tách sau này nếu có nhu cầu thực tế, nhưng khả năng tách microservice không phải mục tiêu triển khai của Pha 1.

Không triển khai thanh toán thật, email, QR check-in, đồ ăn, voucher hay microservices.

## 2. Công nghệ đề xuất

- Java 21 LTS, Spring Boot 3.x, Maven.
- Spring Web MVC, Bean Validation.
- Spring Security và JWT Bearer Token.
- Spring Data JPA/Hibernate chỉ ở tầng `data`.
- PostgreSQL và Flyway.
- springdoc-openapi (OpenAPI 3/Swagger UI).
- JUnit 5, Mockito, Spring Boot Test, Testcontainers.
- k6, Docker và Docker Compose.

Khi khởi tạo codebase phải khóa phiên bản cụ thể trong `pom.xml`; không dùng version động. Lombok là tùy chọn.

## 3. Phạm vi nghiệp vụ

### 3.1 Vai trò

| Vai trò | Quyền |
|---|---|
| Guest | Đăng ký, đăng nhập, xem phim, suất chiếu và ghế |
| Customer | Quyền của Guest; đặt ghế, xem và hủy booking của mình |
| Admin | Quản lý phim, phòng và suất chiếu; xem mọi booking |

### 3.2 Chức năng Pha 1

- **Identity:** đăng ký Customer, đăng nhập, xem hồ sơ hiện tại.
- **Movie:** xem danh sách/chi tiết phim; Admin tạo, cập nhật và xóa mềm phim.
- **Theater:** Admin tạo phòng cùng sơ đồ ghế; xem lịch chiếu và ghế; Admin tạo/hủy suất chiếu.
- **Booking:** đặt 1–8 ghế; xem/hủy booking của mình; Admin xem mọi booking.

### 3.3 Ngoài phạm vi

- Thanh toán/hoàn tiền thật, giữ ghế tạm thời, Redis hoặc hàng đợi.
- Nhiều cụm rạp, giá động, khuyến mại, đồ ăn.
- Refresh token, social login, email/SMS, QR check-in.
- Microservices, message broker, CQRS hoặc event sourcing.

### 3.4 Các giản lược có chủ đích

- Hệ thống quản lý **một địa điểm rạp**, vì vậy chưa có entity `Cinema`; `Auditorium` là phòng thuộc rạp duy nhất.
- Chỉ có `CUSTOMER` và `ADMIN`; chưa cần role `STAFF`.
- Booking được xác nhận ngay khi giữ ghế thành công; chưa có `PENDING`, `EXPIRED`, thời hạn giữ ghế hay Payment module.
- Chưa cần Genre, loại phòng IMAX, ghế đôi hoặc ticket riêng. Có thể bổ sung sau mà không đổi luồng cốt lõi.
- Chưa dùng Spring internal event. Pha 1 ưu tiên interface nghiệp vụ đồng bộ và transaction rõ ràng.

Các mục trên là lựa chọn phạm vi, không phải thiếu sót triển khai. Nếu Pha 2 chọn bài toán giữ ghế/thanh toán làm thuộc tính chất lượng cần nghiên cứu thì phải bổ sung spec và ADR trước khi code.

## 4. Kiến trúc

Hệ thống là một ứng dụng triển khai duy nhất và một PostgreSQL database. Mã nguồn được chia theo nghiệp vụ trước, sau đó mỗi module áp dụng ba tầng `api`, `business`, `data`.

```text
HTTP/JSON
   |
API: Controller, request/response DTO, validation, HTTP mapping
   |
Business: service, model, business rule, repository interface
   |
Data: JPA entity, Spring Data repository, repository implementation
   |
PostgreSQL
```

### 4.1 Module

| Module | Trách nhiệm | Dữ liệu sở hữu |
|---|---|---|
| `identity` | Tài khoản, đăng nhập, vai trò | User |
| `movie` | Thông tin phim | Movie |
| `theater` | Phòng, ghế, suất chiếu | Auditorium, Seat, Showtime |
| `booking` | Đặt ghế và hủy booking | Booking, BookingSeat |

`shared` chỉ chứa thành phần thực sự dùng chung như kiểu phân trang, `Clock`, mã lỗi và request ID. Không đưa logic riêng của một module vào `shared`.

Hướng phụ thuộc liên module:

```text
identity   (độc lập)
movie      (độc lập)
theater  ──────────> movie public business interface
booking  ──────────> theater public business interface
```

`booking` nhận `userId` đã xác thực từ security context nên không cần phụ thuộc vào repository của `identity`. Không cho phép dependency vòng.

### 4.2 Quy tắc ba tầng

```text
api      -> business + Spring Web/Validation
business -> Java + Spring service/transaction + repository interface
data     -> business + JPA/Spring Data/PostgreSQL
config   -> cấu hình chung và security
```

- `api` chỉ xử lý HTTP, validate/chuyển đổi request, gọi business service và dựng response.
- `business` không import Spring Web, Jakarta Persistence, Hibernate, Spring Data hoặc driver database. Có thể dùng `@Service` và `@Transactional` để giữ triển khai 3-tier đơn giản.
- Mỗi method nghiệp vụ phục vụ một mục tiêu rõ ràng, ví dụ `createBooking` hoặc `cancelBooking`.
- Interface repository nằm trong `business`; implementation JPA nằm trong `data`.
- JPA entity tách khỏi business model và không được trả trực tiếp qua API.
- Không đặt business rule trong controller, filter hoặc repository.
- Module không truy cập repository/JPA entity của module khác.
- Giao tiếp liên module chỉ qua public business interface và immutable contract nhỏ.
- Pha 1 gọi đồng bộ trong cùng tiến trình; chưa cần event bus.

Ví dụ: `booking` gọi method truy vấn do `TheaterService` công khai để lấy snapshot suất chiếu và ghế, không đọc trực tiếp bảng của `theater`.

Mỗi module công khai một số interface hoặc service cần thiết trong `business`; `api` và `data` là chi tiết nội bộ của module. Không tạo thêm package `port`, `command`, `query` hoặc lớp use-case một phương thức nếu business service đã diễn đạt đủ rõ.

Luồng kiểu dữ liệu qua boundary:

```text
HTTP Request
  -> Request DTO
  -> Business Service
  -> Business Model/Result
  -> Response DTO
  -> HTTP Response
```

Business không nhận/trả `ResponseEntity`, `HttpServletRequest` hoặc JPA entity.

### 4.3 Cấu trúc thư mục

```text
src/main/java/com/example/cinema/
|-- CinemaApplication.java
|-- config/
|-- shared/
`-- modules/
    |-- identity/
    |   |-- api/              # controller và DTO
    |   |-- business/         # service, model, rule, repository interface
    |   `-- data/             # JPA entity và repository implementation
    |-- movie/               # cùng cấu trúc
    |-- theater/             # cùng cấu trúc
    `-- booking/             # cùng cấu trúc

src/main/resources/
|-- application.yml
`-- db/migration/

src/test/java/
|-- unit/
|-- integration/
`-- e2e/

load-test/
|-- cinema.js
`-- README.md
```

`CinemaApplication.java` phải nằm tại package gốc `com.example.cinema` để component scan mặc định bao phủ `config`, `shared` và toàn bộ `modules`.

Đây là một Maven project duy nhất. Pha 1 không cần multi-module Maven; dùng ArchUnit để kiểm tra quy tắc phụ thuộc.

### 4.4 Ownership dữ liệu

Một PostgreSQL database vật lý được dùng chung, nhưng ownership được quy định rõ:

| Module | Bảng |
|---|---|
| `identity` | `users` |
| `movie` | `movies` |
| `theater` | `auditoriums`, `seats`, `showtimes` |
| `booking` | `bookings`, `booking_seats` |

Module chỉ đọc/ghi bảng mình sở hữu. ID của aggregate thuộc module khác được lưu như tham chiếu logic; việc kiểm tra tồn tại đi qua public API của module sở hữu. Không dùng cascade ORM xuyên boundary module.

## 5. Mô hình dữ liệu

Mọi ID dùng UUID. Tên cột dùng `snake_case`. Thời gian lưu UTC và API trả ISO 8601 có timezone.

### 5.1 User

| Trường | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | PK |
| `username` | varchar(50) | unique, lowercase |
| `password_hash` | varchar(255) | không trả qua API |
| `role` | varchar(20) | `CUSTOMER`, `ADMIN` |
| `created_at`, `updated_at` | timestamptz | not null |

### 5.2 Movie

| Trường | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | PK |
| `title` | varchar(200) | not null |
| `description` | text | not null |
| `duration_minutes` | integer | > 0 |
| `age_rating` | varchar(10) | `P`, `T13`, `T16`, `T18` |
| `release_date` | date | not null |
| `poster_url` | varchar(500) | nullable |
| `status` | varchar(20) | `COMING_SOON`, `NOW_SHOWING`, `ENDED` |
| `deleted_at` | timestamptz | nullable |
| `created_at`, `updated_at` | timestamptz | not null |

### 5.3 Auditorium và Seat

`auditoriums`: `id`, `name` (unique), `status` (`ACTIVE`, `INACTIVE`), `deleted_at`, timestamps.

`seats`: `id`, `auditorium_id`, `row_label`, `seat_number`, `type` (`STANDARD`, `VIP`).

Unique `(auditorium_id, row_label, seat_number)`. Không sửa sơ đồ ghế sau khi phòng đã có suất chiếu.

Sức chứa phòng được tính từ số Seat đang tồn tại, không lưu thêm một cột `capacity` có thể lệch dữ liệu.

### 5.4 Showtime

| Trường | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | PK |
| `movie_id` | UUID | tham chiếu logic đến Movie |
| `auditorium_id` | UUID | FK Auditorium |
| `starts_at`, `ends_at` | timestamptz | not null |
| `base_price` | numeric(12,2) | >= 0 |
| `status` | varchar(20) | `SCHEDULED`, `CANCELLED`, `FINISHED` |
| `created_at`, `updated_at` | timestamptz | not null |

`ends_at` bằng thời lượng phim cộng 15 phút dọn phòng. Không cho hai suất trong cùng phòng chồng lấn.

### 5.5 Booking và BookingSeat

`bookings`:

| Trường | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | PK |
| `booking_code` | varchar(20) | unique, khó đoán |
| `user_id`, `showtime_id` | UUID | not null |
| `status` | varchar(20) | `CONFIRMED`, `CANCELLED` |
| `total_amount` | numeric(12,2) | snapshot khi đặt |
| `created_at` | timestamptz | not null |
| `cancelled_at` | timestamptz | nullable |

`booking_seats`: `id`, `booking_id`, `showtime_id`, `seat_id`, `price`, `released_at` (nullable).

Để vừa giữ lịch sử vừa chống đặt trùng ghế, Flyway tạo partial unique index PostgreSQL:

```sql
CREATE UNIQUE INDEX uq_active_showtime_seat
ON booking_seats (showtime_id, seat_id)
WHERE released_at IS NULL;
```

Flyway là nguồn sự thật của schema; Hibernate dùng để map dữ liệu.

### 5.6 Index tối thiểu

- `users(username)` unique.
- `movies(status, release_date)`.
- `showtimes(movie_id, starts_at)`.
- `showtimes(auditorium_id, starts_at)`.
- `bookings(user_id, created_at desc)`.
- Partial unique index cho ghế đang được giữ như trên.

## 6. Quy tắc nghiệp vụ

1. Username được trim, chuyển lowercase và phải duy nhất; chỉ gồm chữ cái không dấu, chữ số, dấu chấm, gạch dưới hoặc gạch ngang.
2. Chỉ tạo suất chiếu cho phim chưa xóa, phòng `ACTIVE`, thời điểm trong tương lai.
3. `endsAt = startsAt + durationMinutes + 15 phút`.
4. Không tạo lịch chồng lấn trong cùng phòng.
5. Một booking có 1–8 ghế và không nhận ID trùng.
6. Tất cả ghế phải thuộc phòng của suất chiếu.
7. Chỉ đặt vé cho suất `SCHEDULED` chưa bắt đầu.
8. Giá `STANDARD = basePrice`; `VIP = basePrice × 1.2`.
9. Tiền dùng `BigDecimal`, scale 2, `RoundingMode.HALF_UP`; không dùng `double`.
10. Transaction và unique index bảo đảm hai request đồng thời không đặt thành công cùng ghế.
11. Customer chỉ xem/hủy booking của mình; Admin xem được mọi booking.
12. Chỉ hủy booking `CONFIRMED` trước giờ chiếu ít nhất 30 phút.
13. Khi hủy, đổi booking thành `CANCELLED` và đặt `released_at` cho các ghế trong cùng transaction.
14. Xóa Movie/Auditorium là xóa mềm. Không xóa phòng đang có suất chiếu tương lai.
15. DELETE Showtime đổi trạng thái sang `CANCELLED`, không xóa vật lý; từ chối nếu đã có booking `CONFIRMED`.

## 7. REST API

Base path: `/api/v1`. Request/response dùng `application/json`.

### 7.1 Quy ước response

```json
{
  "data": { "id": "7b1c6d73-8fb0-4b41-9892-1bb26860886c" }
}
```

```json
{
  "data": [],
  "meta": { "page": 1, "pageSize": 20, "totalItems": 0, "totalPages": 0 }
}
```

```json
{
  "error": {
    "code": "SEAT_ALREADY_BOOKED",
    "message": "Một hoặc nhiều ghế không còn trống",
    "details": [{ "field": "seatIds", "reason": "contains unavailable seats" }],
    "requestId": "0f43cdb5-7972-40a7-8932-b60cd50f031e"
  }
}
```

`page` bắt đầu từ 1, mặc định 1; `pageSize` mặc định 20, tối đa 100.

### 7.2 Identity

| Method | Path | Quyền | Mô tả | Status chính |
|---|---|---|---|---|
| POST | `/auth/register` | Public | Đăng ký Customer | 201, 409, 422 |
| POST | `/auth/login` | Public | Đăng nhập | 200, 401, 422 |
| GET | `/users/me` | Authenticated | Hồ sơ hiện tại | 200, 401 |

Đăng ký thành công không tự đăng nhập và không trả JWT; người dùng gọi `/auth/login` riêng để lấy token.

```json
{
  "username": "nguyenvana",
  "password": "StrongPass123!"
}
```

Request login:

```json
{
  "username": "nguyenvana",
  "password": "StrongPass123!"
}
```

Response login:

```json
{
  "data": { "accessToken": "<jwt>", "tokenType": "Bearer", "expiresIn": 3600 }
}
```

Response `GET /users/me` chỉ có `id`, `username`, `role` trong `data`; không trả `createdAt` hoặc `passwordHash`:

```json
{
  "data": {
    "id": "7b1c6d73-8fb0-4b41-9892-1bb26860886c",
    "username": "nguyenvana",
    "role": "CUSTOMER"
  }
}
```

### 7.3 Movie

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/movies?page=1&pageSize=20&status=NOW_SHOWING` | Public | Danh sách phim |
| GET | `/movies/{movieId}` | Public | Chi tiết phim |
| POST | `/admin/movies` | Admin | Tạo phim |
| PATCH | `/admin/movies/{movieId}` | Admin | Cập nhật phim |
| DELETE | `/admin/movies/{movieId}` | Admin | Xóa mềm; trả 204 |

### 7.4 Theater

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/admin/auditoriums` | Admin | Danh sách phòng |
| POST | `/admin/auditoriums` | Admin | Tạo phòng và ghế |
| DELETE | `/admin/auditoriums/{id}` | Admin | Xóa mềm phòng nếu hợp lệ |
| GET | `/showtimes?movieId={id}&date=2026-09-21` | Public | Lịch chiếu |
| GET | `/showtimes/{id}` | Public | Chi tiết suất chiếu |
| GET | `/showtimes/{id}/seats` | Public | Ghế và trạng thái |
| POST | `/admin/showtimes` | Admin | Tạo suất chiếu |
| DELETE | `/admin/showtimes/{id}` | Admin | Hủy suất chưa có booking; trả 204 |

Request tạo phòng:

```json
{
  "name": "Room 01",
  "seatLayout": [
    { "row": "A", "from": 1, "to": 10, "type": "STANDARD" },
    { "row": "B", "from": 1, "to": 8, "type": "VIP" }
  ]
}
```

Request tạo suất:

```json
{
  "movieId": "c172b99d-790d-4244-bc53-c84130c19a29",
  "auditoriumId": "20c2186e-9de9-45a6-ab48-b866e22bc6fe",
  "startsAt": "2026-10-02T12:30:00Z",
  "basePrice": 90000.00
}
```

### 7.5 Booking

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/bookings` | Customer/Admin | Đặt ghế |
| GET | `/bookings/me?page=1&pageSize=20` | Customer/Admin | Booking của tôi |
| GET | `/bookings/{id}` | Owner/Admin | Chi tiết booking |
| DELETE | `/bookings/{id}` | Owner/Admin | Hủy booking; trả 204 |
| GET | `/admin/bookings?page=1&pageSize=20` | Admin | Tất cả booking |

```json
{
  "showtimeId": "c03b631b-c958-46df-a531-8b9406c50f23",
  "seatIds": [
    "cc70bb5c-b28a-4d72-908c-c15cbebeea19",
    "80ba8220-34f0-49a4-a07b-fcb988dd08c0"
  ]
}
```

Đặt thành công trả `201`. Nếu ghế vừa được request khác đặt, trả `409` với code `SEAT_ALREADY_BOOKED`.

### 7.6 Vận hành và tài liệu

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/actuator/health/liveness` | Public | Process đang chạy |
| GET | `/actuator/health/readiness` | Public | Ứng dụng và DB sẵn sàng |
| GET | `/swagger-ui/index.html` | Public ở dev/test | Swagger UI |
| GET | `/v3/api-docs` | Public ở dev/test | OpenAPI JSON |

## 8. Xác thực và bảo mật

- Mật khẩu dài 8–16 ký tự, không bắt buộc chữ hoa, số hay ký tự đặc biệt; hash bằng BCrypt. Không tự trim/chuẩn hóa mật khẩu.
- JWT chứa `sub`, `role`, `iat`, `exp`; `sub` là UUID của User, không phải username; token hết hạn mặc định sau 60 phút.
- Một `OncePerRequestFilter` xác thực JWT và thiết lập `SecurityContext`.
- Authorization dùng `SecurityFilterChain` và method security cho role/ownership; không đọc token lặp lại trong controller.
- Route mặc định yêu cầu xác thực; route public được khai báo rõ.
- Toàn bộ `/api/v1/admin/**` yêu cầu role `ADMIN`.
- API đăng ký luôn tạo `CUSTOMER`; Admin chỉ được tạo qua seed cho local/test, với username/password demo lấy từ biến môi trường và không dùng seed đó ở production.
- Secret lấy từ biến môi trường; không commit `.env`.
- Không log password, token, password hash hoặc chuỗi kết nối.
- Production không trả stack trace, SQL hoặc exception nội bộ.

Yêu cầu đề bài được đáp ứng tối thiểu bởi GET `/bookings/me` và POST `/bookings`, cả hai đều xác thực tập trung.

## 9. Luồng đặt ghế và transaction

`BookingService.createBooking`:

1. Nhận `userId` từ security context và input từ request.
2. Kiểm tra 1–8 ghế, không có ID trùng.
3. Gọi `ShowtimeQuery` để lấy snapshot suất chiếu và ghế.
4. Kiểm tra suất còn đặt được và ghế thuộc đúng phòng.
5. Tính giá bằng business service.
6. Trong một transaction, lưu Booking và BookingSeat.
7. Nếu partial unique index bị vi phạm, tầng data chuyển thành lỗi nghiệp vụ `SeatAlreadyBooked`.
8. Web exception handler ánh xạ thành HTTP 409.

Transaction đặt ở method business service bằng `@Transactional`. Database constraint là lớp bảo vệ cuối trước race condition.

## 10. Ánh xạ lỗi HTTP

| Trường hợp | HTTP | Code ví dụ |
|---|---:|---|
| JSON sai cú pháp, sai kiểu hoặc query parameter không parse được | 400 | `MALFORMED_REQUEST` |
| DTO đúng cú pháp nhưng vi phạm constraint validation | 422 | `VALIDATION_ERROR` |
| Sai username/password khi đăng nhập | 401 | `INVALID_CREDENTIALS` |
| Thiếu, sai hoặc hết hạn token ở endpoint bảo vệ | 401 | `UNAUTHORIZED` |
| Thiếu role/ownership | 403 | `FORBIDDEN` |
| Không tìm thấy | 404 | `MOVIE_NOT_FOUND` |
| Username trùng, ghế đã đặt, lịch chồng | 409 | `USERNAME_ALREADY_EXISTS`, `SEAT_ALREADY_BOOKED` |
| Lỗi không dự kiến | 500 | `INTERNAL_ERROR` |

Một `@RestControllerAdvice` thực hiện mapping. Tầng business không ném exception HTTP của Spring.

Các error code tối thiểu: `MALFORMED_REQUEST`, `VALIDATION_ERROR`, `USERNAME_ALREADY_EXISTS`, `INVALID_CREDENTIALS`, `UNAUTHORIZED`, `FORBIDDEN`, `MOVIE_NOT_FOUND`, `AUDITORIUM_NOT_FOUND`, `SHOWTIME_NOT_FOUND`, `BOOKING_NOT_FOUND`, `SEAT_NOT_FOUND`, `SEAT_ALREADY_BOOKED`, `SCHEDULE_OVERLAP`, `BOOKING_CANNOT_BE_CANCELLED`, `INTERNAL_ERROR`.

### 10.1 Validation đầu vào

| Đối tượng | Validation tối thiểu |
|---|---|
| Register/Login | username dài 3–50 ký tự, khớp `[a-zA-Z0-9._-]+`; password 8–16 ký tự |
| Movie | title/description không rỗng; duration > 0; status hợp lệ |
| Auditorium | name không rỗng; mỗi dải ghế hợp lệ; không trùng hàng/số |
| Showtime | UUID hợp lệ; startsAt trong tương lai; basePrice > 0 |
| Booking | showtimeId hợp lệ; `seatIds` có 1–8 UUID không trùng |

DTO validation chỉ kiểm tra hình dạng/ràng buộc đơn giản. Các quy tắc cần dữ liệu hoặc trạng thái hiện tại được kiểm tra trong business.

## 11. Cấu hình

```dotenv
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
DB_URL=jdbc:postgresql://db:5432/cinema
DB_USERNAME=cinema
DB_PASSWORD=cinema
JWT_SECRET=replace-with-at-least-32-random-bytes
JWT_EXPIRATION_SECONDS=3600
```

Sử dụng các profile:

- `application.yml`: cấu hình chung và placeholder.
- `application-local.yml`: chạy máy developer.
- `application-test.yml`: integration/E2E test.
- `application-prod.yml`: cấu hình production-safe.

Ứng dụng fail fast nếu thiếu cấu hình bắt buộc. Các file YAML không chứa secret.

## 12. Docker và chạy cục bộ

- `Dockerfile` multi-stage: build bằng Maven/JDK, chạy bằng JRE và non-root user.
- `compose.yaml` gồm `api` và `db`.
- Có `.dockerignore`, `.gitignore`, `.env.example` và healthcheck.
- Flyway chạy khi khởi động; Hibernate dùng `ddl-auto=validate`, không dùng `create`/`update`.

README cung cấp:

```bash
docker compose up --build
./mvnw test
./mvnw verify
```

Seed dev tạo một Admin, một Customer, một phim, một phòng và một suất chiếu. Admin demo chỉ được tạo ở local/test khi có username/password từ biến môi trường; credential demo không dùng ở production.

### 12.1 Quy ước Flyway migration

Migration phải được commit và chỉ tiến về phía trước. Bộ migration khởi đầu dự kiến:

```text
V1__create_users.sql
V2__create_movies.sql
V3__create_auditoriums_and_seats.sql
V4__create_showtimes.sql
V5__create_bookings.sql
V6__add_active_seat_unique_index.sql
```

Dev seed không đặt trong chuỗi migration production. Đặt nó ở location riêng như `db/devdata/` và chỉ bật trong profile `local`, hoặc chạy bằng một seed command riêng. Không sửa nội dung migration đã chạy trên môi trường dùng chung; tạo migration mới để thay đổi schema.

## 13. Kiểm thử

### Unit

- Giá ghế STANDARD/VIP.
- Giới hạn 1–8 ghế và ID trùng.
- Quy tắc thời điểm đặt/hủy.
- Tính `endsAt`, phát hiện overlap.
- Business service với fake/mock repository interface, không khởi động Spring.

### Integration (PostgreSQL Testcontainers)

- Flyway migration và JPA mapping.
- Transaction đặt/hủy booking.
- Hai transaction không thể giữ cùng ghế.
- Query ghế trả đúng trạng thái.

### E2E

- Đăng ký → đăng nhập → xem hồ sơ.
- Admin tạo phim → phòng → suất chiếu.
- Customer xem ghế → đặt → xem booking → hủy.
- Thiếu token nhận 401; Customer gọi API Admin nhận 403.
- Hai request đồng thời đặt cùng ghế: đúng một 201 và một 409.

Mục tiêu coverage cho tầng `business`: tối thiểu 80% line. Business rule quan trọng phải có test trực tiếp.

## 14. OpenAPI/Swagger

OpenAPI phải mô tả mọi endpoint, parameter, schema, status chính, Bearer JWT, role, enum, UUID, date-time, pagination và schema lỗi chung. Swagger UI cho nhập token bằng nút **Authorize**. `/v3/api-docs` phải phản ánh API thực tế và có smoke test.

## 15. Observability tối thiểu

- Mỗi request có `requestId`; nhận `X-Request-Id` là UUID dạng chuẩn hoặc tự sinh. Response trả lại cùng ID trong header `X-Request-Id`; error body dùng cùng ID.
- Access log có method, route template, status, duration; không log dữ liệu nhạy cảm.
- Dùng SLF4J qua logging facade của Spring Boot; không dùng `System.out.println()`.
- Log ở mức phù hợp cho login thất bại/thành công, booking được tạo/hủy và lỗi cạnh tranh ghế; không log JWT hoặc password.
- Tách liveness/readiness.
- Khi load test lưu latency, throughput và error rate.

Pha 1 chưa cần Prometheus, Grafana hoặc distributed tracing.

## 16. Kiểm thử tải trên Kaggle CPU

Mỗi lần đo trước/sau phải ghi số CPU/RAM, Java/PostgreSQL version, Git commit, dataset, JVM options, connection pool, warm-up, VU và duration. Không so sánh kết quả từ phần cứng khác nhau.

```text
load-test/
|-- smoke.js          # kiểm tra môi trường và luồng tối thiểu
|-- movie-read.js     # tải thiên về đọc
|-- booking.js        # tải hỗn hợp và booking
|-- contention.js     # nhiều request tranh cùng ghế
`-- README.md         # seed, biến môi trường, lệnh chạy, threshold
```

| Kịch bản | Tỷ trọng |
|---|---:|
| GET danh sách phim | 35% |
| GET lịch chiếu | 25% |
| GET trạng thái ghế | 25% |
| POST booking | 10% |
| GET booking của tôi | 5% |

Warm-up 30 giây; chạy riêng 10, 25, 50 VU trong ít nhất 2 phút/mức. Request booking thông thường dùng user/ghế khác nhau; thêm một kịch bản contention riêng.

Báo cáo throughput, p50/p95/p99, tỷ lệ lỗi kỹ thuật/nghiệp vụ, CPU/RAM và xác nhận không có double-booking. Threshold ban đầu chỉ là giả thuyết, ví dụ lỗi kỹ thuật < 1%, p95 GET < 500 ms, p95 POST booking < 1 giây ở 25 VU.

## 17. Hướng Pha 2

Chỉ cải tiến sau khi đo/profiling chỉ ra vấn đề; không mặc định thêm Redis hay tách microservice.

| Quan sát | Cải tiến có thể thử | Thuộc tính | Đánh đổi |
|---|---|---|---|
| Phim/lịch chiếu đọc chậm | Cache in-memory/Redis | Performance, scalability | Stale data, invalidation |
| Query ghế chậm | Index, projection, tối ưu query | Performance | Query/schema phức tạp |
| Tranh chấp ghế cao | So sánh isolation/locking | Consistency, reliability | Throughput, latency |
| Quá nhiều request | Rate limiting/backpressure | Availability, security | Từ chối request hợp lệ |
| Một instance không đủ | Stateless + nhiều replica | Scalability, availability | Vận hành phức tạp |
| Khó chẩn đoán | Metrics, tracing | Observability | Tài nguyên/lưu trữ |
| Boundary bị vi phạm | ArchUnit/contract test | Maintainability | Thêm test/quy ước |

Quy trình: tag `phase-1-baseline`; chạy ít nhất 3 lần; báo median và độ dao động; xác định nút thắt; nêu giả thuyết; thay một nhóm yếu tố; chạy lại cùng cấu hình; so sánh hiệu năng, tài nguyên và tính đúng; kết luận trade-off.

## 18. CI và GitHub

Pipeline pull request: compile/format, ArchUnit, unit test, integration/E2E với PostgreSQL, `mvn verify`, build Docker image.

- Không commit secret, `.env`, `target/` hoặc file IDE.
- Mỗi task dùng feature branch ngắn và merge bằng pull request; tránh commit trực tiếp vào nhánh bảo vệ.
- Commit nhỏ, có ý nghĩa; dùng Conventional Commits như `feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `chore:`.
- README mô tả bài toán, kiến trúc, module, cách chạy, migration/seed, tài khoản demo, Swagger, test tải và giới hạn.
- Gắn tag `phase-1-baseline` trước Pha 2.

## 19. Thứ tự triển khai

1. Maven/Spring Boot, PostgreSQL, Flyway, Docker, healthcheck.
2. Package skeleton, error model, security skeleton và ArchUnit rule.
3. Làm vertical slice `CreateMovie`, `GetMovie`, `ListMovies` đầy đủ từ REST đến PostgreSQL để làm mẫu.
4. Identity: register, login, JWT, role.
5. Hoàn thiện Movie và OpenAPI.
6. Theater: phòng/ghế, suất chiếu, overlap.
7. Booking: transaction, partial unique index, ownership, hủy.
8. Exception mapping, request ID, log.
9. Unit/integration/E2E, CI.
10. Seed, k6, hướng dẫn Kaggle và baseline.

## 20. Definition of Done

### 20.1 Base codebase

- [ ] Maven Wrapper build thành công và ứng dụng khởi động được.
- [ ] PostgreSQL kết nối được, toàn bộ Flyway migration chạy thành công.
- [ ] Package structure và module boundary tồn tại đúng spec.
- [ ] ArchUnit kiểm tra được dependency rule quan trọng.
- [ ] Liveness/readiness trả 200 khi hệ thống sẵn sàng.
- [ ] Swagger UI và OpenAPI JSON truy cập được ở profile `local`/`test`.
- [ ] Global exception handler trả đúng error schema.
- [ ] Testcontainers test infrastructure chạy được.
- [ ] Docker image build được và `docker compose up` chạy toàn hệ thống.

Base codebase là skeleton; JWT/Bearer Authorize và vertical slice Movie được triển khai cùng các module ở Pha 1.

### 20.2 Pha 1

- [ ] Có endpoint REST/JSON POST, GET, DELETE hoạt động.
- [ ] Swagger UI và OpenAPI JSON đúng API thực tế.
- [ ] Tầng `business` không import Spring Web, JPA, Spring Data hoặc thư viện DB.
- [ ] Truy cập DB đi qua repository interface trong `business` và implementation trong `data`.
- [ ] Login hoạt động; GET `/bookings/me` và POST `/bookings` được bảo vệ tập trung.
- [ ] Role và ownership được kiểm thử.
- [ ] Constraint và transaction ngăn double-booking.
- [ ] Unit, integration, E2E chính đều pass.
- [ ] `docker compose up --build` chạy được API và DB.
- [ ] Migration, seed, `.env.example`, README đầy đủ.
- [ ] Có kịch bản tải tái lập trên Kaggle CPU và báo cáo baseline.
- [ ] GitHub public, không có secret, commit rõ ràng.

Một feature chỉ được coi là hoàn thành khi có đủ phần phù hợp với feature đó: business rule/model, business service, repository nếu cần, REST endpoint, validation, error mapping, test và mô tả OpenAPI.

## 21. Quyết định kiến trúc chính

| Quyết định | Lý do |
|---|---|
| Modular Monolith | Một artifact/DB dễ triển khai nhưng vẫn có boundary nghiệp vụ |
| [3-tier trong từng module](adr/001-modular-monolith-3-tier.md) | Khớp trực tiếp yêu cầu API → Business → Data và vừa sức nhóm |
| Một Maven project | Đủ đơn giản; ArchUnit bảo vệ boundary |
| PostgreSQL + transaction + partial unique index | Ngăn double-booking và vẫn giữ lịch sử ghế hủy |
| JWT stateless | Phù hợp REST và vừa sức Pha 1 |
| JPA entity tách business model | Mô hình nghiệp vụ không phụ thuộc ORM |
| Gọi đồng bộ liên module | Dễ hiểu/debug; chỉ dùng async khi Pha 2 có bằng chứng |
| Không cache ở Pha 1 | Có baseline trung thực, tránh tối ưu trước khi đo |

Tài liệu này là nguồn yêu cầu chính của codebase. Thay đổi phạm vi, business rule, module boundary hoặc chiến lược nhất quán dữ liệu phải cập nhật spec và ghi ADR ngắn trong `docs/adr/`.

## 22. Quy tắc code

- Tuân thủ KISS và YAGNI; chỉ tạo abstraction khi có boundary hoặc khả năng thay thế rõ ràng.
- Không tạo `BaseRepository<T>` chung chỉ để giảm vài dòng code.
- Không tạo các package `controller/service/repository/entity` dùng chung toàn hệ thống; luôn package theo business module trước rồi mới chia `api/business/data`.
- Không để controller chứa business logic hoặc JPA entity chứa orchestration nghiệp vụ.
- Không để repository của module này gọi repository của module khác.
- Module khác chỉ gọi public business interface/service; không truy cập package `api` hoặc `data` của nhau.
- MapStruct và Lombok có thể dùng để giảm boilerplate nhưng không bắt buộc; mapper có business rule phải viết rõ và có test.
- Không tạo internal event chỉ để “chuẩn bị cho microservice”. Chỉ thêm event khi có ít nhất hai bên thực sự cần tách coupling.

## 23. Mục tiêu lần tạo codebase đầu tiên

Lần khởi tạo đầu chưa cần sinh toàn bộ chức năng. Cần tạo một skeleton chạy được gồm:

- Maven Wrapper và cấu hình project.
- Package/module boundary theo mục 4.
- PostgreSQL, Flyway và thư mục migration; migration nghiệp vụ đầu tiên được thêm cùng module `identity`.
- Global exception handler và error response chuẩn.
- Spring Security skeleton; JWT được thêm cùng module `identity`.
- Cấu trúc package 3-tier và ArchUnit rule; repository implementation được thêm cùng module nghiệp vụ đầu tiên.
- Test structure, ArchUnit và Testcontainers.
- Dockerfile, Compose, healthcheck và Swagger UI.

Sau khi hoàn tất skeleton, triển khai vertical slice Movie:

```text
MovieController
    -> MovieService
    -> Movie business model
    -> MovieRepository interface
    -> JpaMovieRepository implementation
    -> PostgreSQL
```

Vertical slice này phải có validation, error mapping, unit test, integration test và OpenAPI; nó là mẫu để nhóm triển khai các module còn lại nhất quán.
