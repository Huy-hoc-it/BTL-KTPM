# Contract tích hợp Theater và Booking

Contract này là public business API nội bộ. Nguồn chuẩn là `ShowtimeModuleApi` và các record lồng trong interface; hai lớp snapshot độc lập cũ đã được loại bỏ.

## 1. Snapshot và truy vấn

```java
ShowtimeSnapshot getShowtimeSnapshot(UUID showtimeId);
Optional<ShowtimeSnapshot> findShowtime(UUID showtimeId);
ShowtimeSnapshot lockShowtimeForBooking(UUID showtimeId);
List<SeatSnapshot> getSeatSnapshots(List<UUID> seatIds);
```

- `ShowtimeSnapshot(showtimeId, auditoriumId, startsAt, basePrice, status, seats)` chứa toàn bộ sơ đồ ghế vật lý. Trạng thái: `ShowtimeState.SCHEDULED`, `CANCELLED`, `FINISHED`.
- `SeatSnapshot(seatId, auditoriumId, rowLabel, seatNumber, type)` có loại `SeatKind.STANDARD` hoặc `VIP` và ID phòng để Booking đối chiếu.
- Các record bất biến; danh sách ghế được sao chép bằng `List.copyOf`. Không trả entity JPA hay repository.
- Suất không tồn tại: `getShowtimeSnapshot`/`lockShowtimeForBooking` trả `null`; `findShowtime` trả `Optional.empty()`. Các endpoint `GET /api/v1/showtimes/{id}` và `GET /api/v1/showtimes/{id}/seats` trả `404 SHOWTIME_NOT_FOUND`.
- `getSeatSnapshots` chỉ trả ghế tồn tại trong tập ID yêu cầu, mỗi ghế một lần, không cam kết thứ tự. ID không tồn tại bị bỏ qua. Booking kiểm tra đủ ID, không thừa/trùng và cùng phòng; thiếu ghế phải từ chối toàn bộ booking.
- Snapshot không chứa trạng thái đặt ghế: Theater cung cấp sơ đồ, Booking sở hữu ghế đã đặt.

## 2. Tạo booking và khóa suất

`BookingService.createBooking` chạy trong một transaction:

1. Kiểm tra người đặt, ID suất, 1–8 ghế, ID null/trùng.
2. Gọi `lockShowtimeForBooking`: khóa ghi hàng suất đến hết transaction. Method yêu cầu transaction đang hoạt động (`MANDATORY`).
3. Gọi `getSeatSnapshots`; kiểm tra suất `SCHEDULED`, chưa bắt đầu và giá dương.
4. Đối chiếu tập ID, `SeatSnapshot.auditoriumId` và sơ đồ suất.
5. Kiểm tra ghế đã đặt; tính giá STANDARD = basePrice, VIP = basePrice × 1.20, scale 2/HALF_UP; lưu booking cùng ghế.
6. Commit và giải phóng khóa. Unique constraint ghế tại database tiếp tục bảo vệ đặt trùng.

`getShowtimeSnapshot` và `findShowtime` dành cho truy vấn chỉ đọc, không thay thế method khóa khi tạo booking.

## 3. Hủy suất, không có phụ thuộc vòng

Theater định nghĩa port `ShowtimeCancellationGuard.checkCancellationAllowed(UUID)`. Booking triển khai bằng `BookingShowtimeCancellationGuard`, chỉ dùng repository Booking. Theater không import package Booking.

`ShowtimeService.cancel` khóa cùng hàng suất mà Booking sử dụng, kiểm tra `SCHEDULED` và chưa bắt đầu, gọi guard, rồi lưu `CANCELLED`. Có booking xác nhận trả `409 SHOWTIME_HAS_CONFIRMED_BOOKINGS`.

Persistence Booking hiện chưa có cột trạng thái với các giá trị `CONFIRMED`/`CANCELLED`: mỗi booking đã lưu được xem là xác nhận và giữ ghế đến khi bị xóa theo luồng hiện có. `existsConfirmedByShowtimeId` kiểm tra sự tồn tại booking, độc lập với `payment_status` (`PENDING`/`PAID`/`FAILED`). Khi Booking bổ sung hủy mềm theo SPEC, module Booking phải đổi implementation sang lọc `CONFIRMED` và ghế chưa giải phóng; chữ ký Theater không đổi.

Nếu đặt vé thắng khóa, hủy suất chờ commit rồi bị guard từ chối. Nếu hủy thắng khóa, đặt vé đọc lại `CANCELLED` rồi bị từ chối. Khi chạy Theater độc lập với `cinema.booking.enabled=false`, Booking guard không được đăng ký.

## 4. Trạng thái ghế và lịch công khai

`GET /api/v1/showtimes/{id}/seats` thuộc `booking/api/ShowtimeSeatController`, cho phép Guest. Booking kết hợp snapshot Theater với `findBookedSeatIds`:

```json
[{"id":"<seat-uuid>","row":"A","number":1,"type":"STANDARD","status":"AVAILABLE"}]
```

Trạng thái ghế là `AVAILABLE` hoặc `BOOKED`; trả đầy đủ ghế theo sơ đồ. Suất `CANCELLED`/`FINISHED` vẫn truy vấn được sơ đồ. Ghế AVAILABLE không thay thế quy tắc chỉ đặt suất SCHEDULED chưa bắt đầu.

- `GET /api/v1/showtimes?movieId=...&date=YYYY-MM-DD`: ngày theo UTC, khoảng `[00:00 UTC, 00:00 UTC ngày kế tiếp)`, tăng dần theo startsAt, chỉ trả SCHEDULED.
- `GET /api/v1/showtimes/{id}`: trả mọi trạng thái, kể cả CANCELLED/FINISHED để tra cứu lịch sử.

## 5. Cạnh tranh ghi và migration

Tạo suất và xóa mềm phòng khóa chung hàng auditorium để không thể tạo suất vào phòng vừa bị vô hiệu hóa.

Migration `V8__protect_theater_schedule_and_names.sql` thêm exclusion constraint trên `(auditorium_id, tstzrange(starts_at, ends_at, '[)'))` với suất khác CANCELLED. endsAt đã gồm 15 phút dọn phòng; hai suất tiếp giáp được phép. V8 dùng extension PostgreSQL `btree_gist` và unique index `lower(name)` để bảo vệ tên phòng khi tạo đồng thời. Adapter chuyển lỗi constraint thành `SCHEDULE_OVERLAP`/`AUDITORIUM_ALREADY_EXISTS`.

V8 sẽ từ chối migration nếu dữ liệu cũ có lịch chồng hoặc tên phòng trùng không phân biệt hoa/thường; cần xử lý dữ liệu đó trước khi nâng cấp.

## 6. REST Admin và mã lỗi

| Endpoint | Thành công | Lỗi nghiệp vụ chính |
|---|---|---|
| GET /api/v1/admin/auditoriums | 200, phòng ACTIVE và ghế | 401/403 |
| POST /api/v1/admin/auditoriums | 201 | 409 AUDITORIUM_ALREADY_EXISTS / DUPLICATE_SEAT_POSITION |
| DELETE /api/v1/admin/auditoriums/{id} | 204, xóa mềm | 404 AUDITORIUM_NOT_FOUND, 409 AUDITORIUM_HAS_FUTURE_SHOWTIMES |
| POST /api/v1/admin/showtimes | 201, có Location | 404 MOVIE_NOT_FOUND / AUDITORIUM_NOT_FOUND, 409 SCHEDULE_OVERLAP |
| DELETE /api/v1/admin/showtimes/{id} | 204, chuyển CANCELLED | 404 SHOWTIME_NOT_FOUND, 409 SHOWTIME_HAS_CONFIRMED_BOOKINGS, 422 THEATER_RULE_VIOLATION |

Admin cần JWT và role ADMIN: chưa xác thực trả 401, sai quyền trả 403. JSON/query sai kiểu trả 400 MALFORMED_REQUEST; DTO vi phạm constraint trả 422 VALIDATION_ERROR. Giá suất mới phải dương, tối đa 10 chữ số nguyên và 2 chữ số thập phân. Response thành công Theater/ghế hiện dùng object/list trực tiếp như code hiện hữu; lỗi dùng envelope error chung. OpenAPI ghi rõ route và mã HTTP.
