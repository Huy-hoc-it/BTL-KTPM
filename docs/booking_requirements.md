# Yêu cầu chi tiết module Booking

## 1. Mục tiêu và phạm vi

Module Booking chịu trách nhiệm biến yêu cầu chọn ghế của khách thành một booking hợp lệ. Booking sở hữu người đặt, suất được đặt, các ghế đã chọn, giá chốt của từng ghế và tổng tiền.

Tài liệu giữ các yêu cầu nghiệp vụ của Booking và cập nhật contract đã tích hợp với Theater. Hiện đã có business logic, unit test với fake Theater, API tạo/xem booking, JPA/PostgreSQL, migration và ràng buộc duy nhất chống trùng ghế. Các yêu cầu còn lại được đối chiếu ở mục 9.

## 2. Ranh giới với Theater

Theater sở hữu phòng chiếu, ghế vật lý, loại ghế, suất chiếu, trạng thái suất, thời điểm bắt đầu và giá cơ bản. Booking không đọc controller, repository, entity JPA hoặc bảng của Theater.

Booking chỉ gọi public business contract của Theater là `ShowtimeModuleApi`.

### 2.1. Contract đã tích hợp

Nguồn chuẩn: [theater_booking_contract.md](theater_booking_contract.md). Các record `ShowtimeSnapshot` và `SeatSnapshot` nằm trong `ShowtimeModuleApi`.

- Showtime snapshot có `showtimeId`, `auditoriumId`, `startsAt`, `basePrice`, `status`, `seats`.
- Seat snapshot có `seatId`, `auditoriumId`, `rowLabel`, `seatNumber`, `type`.
- Tạo booking gọi `lockShowtimeForBooking` trong transaction để giữ khóa hàng suất đến commit. Hai truy vấn chỉ đọc là `getShowtimeSnapshot`/`findShowtime`.
- `getSeatSnapshots` trả ghế tồn tại trong tập yêu cầu; Booking từ chối nếu thiếu/thừa/trùng ID hoặc sai phòng.
- Booking triển khai port Theater `ShowtimeCancellationGuard` và endpoint công khai `GET /api/v1/showtimes/{id}/seats`.
- Trạng thái xác nhận hiện suy ra từ booking còn tồn tại, độc lập với `payment_status`. Khi triển khai hủy mềm, repository Booking phải lọc `CONFIRMED` và ghế chưa giải phóng như SPEC.

Trình tự tạo booking ở mục 6.1 đã được cập nhật theo implementation hiện tại.

## 3. `Booking`

**Mục đích:** mô tả kết quả nghiệp vụ của một lần đặt ghế.

**Dữ liệu phải giữ:**

- ID booking; có thể sinh ở business hoặc database ở bước sau.
- `userId`: người đặt.
- `showtimeId`: suất được đặt.
- Danh sách `BookingSeat`.
- `totalAmount`: tổng tiền chốt tại lúc tạo booking.
- Theo SPEC cần bổ sung trạng thái booking, mã booking và thời gian hủy; persistence hiện đã có thời gian tạo và `payment_status`.

**Quy tắc:**

- Không cho phép sửa trực tiếp danh sách ghế từ bên ngoài sau khi tạo booking.
- Giá trong Booking là snapshot giao dịch; không đổi theo `basePrice` mới của Theater.
- `totalAmount` bằng tổng giá các `BookingSeat`.

## 4. `BookingSeat`

**Mục đích:** biểu diễn một ghế thuộc một booking.

**Dữ liệu phải giữ:**

- `seatId`.
- `price`: giá cuối cùng của ghế lúc đặt.

**Quy tắc:**

- Giá phải dùng `BigDecimal`.
- Giá có scale 2 và làm tròn `HALF_UP`.
- Không lưu loại ghế hoặc giá gốc nếu chúng không cần cho lịch sử giao dịch; giá chốt là bắt buộc.
- Persistence hiện lưu `bookingId`, `showtimeId` và ID ghế. `releasedAt` thuộc yêu cầu hủy mềm, chưa được triển khai.

## 5. `BookingRepository`

**Mục đích:** abstraction để BookingService lưu booking mà không biết JPA/PostgreSQL.

### 5.1. `save(booking)`

**Đầu vào:** một Booking đã vượt toàn bộ validation nghiệp vụ.

**Kết quả:** booking đã được lưu. Khi có database, kết quả phải chứa ID do persistence xác nhận nếu ID được sinh ở database.

**Quy tắc:**

- Business service chỉ gọi interface này.
- Unit test dùng fake repository; fake có thể giữ booking trong biến hoặc map trong phạm vi test.
- Tầng `booking/data` hiện triển khai interface bằng JPA/PostgreSQL.
- Yêu cầu còn lại: chuyển vi phạm ràng buộc duy nhất ghế thành lỗi `409 SEAT_ALREADY_BOOKED` theo SPEC.

## 6. `BookingService`

**Mục đích:** điều phối toàn bộ use case tạo booking. Service không chứa HTTP controller, JSON, SQL, JPA entity hoặc truy cập repository của Theater.

**Dependency bắt buộc:**

- `BookingRepository` để lưu booking.
- `ShowtimeModuleApi` để đọc dữ liệu Theater.
- `Clock` để so sánh thời điểm bắt đầu một cách ổn định trong test.

### 6.1. `createBooking(userId, showtimeId, seatIds)`

**Đầu vào:**

- `userId`: theo SPEC phải lấy từ security context; controller hiện vẫn nhận từ request. Unit test truyền trực tiếp.
- `showtimeId`: do khách chọn.
- `seatIds`: do khách chọn.

**Trình tự bắt buộc:**

1. Gọi `validateBookingRequest`.
2. Gọi `lockShowtimeForBooking` một lần cho suất và `getSeatSnapshots` một lần cho danh sách ghế trong cùng transaction.
3. Gọi `validateShowtime`.
4. Kiểm tra tập ID ghế, phòng và sơ đồ của suất; kiểm tra ghế chưa được đặt trong repository Booking, rồi tạo BookingSeat cho từng ghế hợp lệ.
5. Cộng giá từng ghế để tính `totalAmount`.
6. Tạo đối tượng Booking với user, suất, các BookingSeat và tổng tiền.
7. Gọi `BookingRepository.save` đúng một lần.
8. Trả booking đã lưu.

**Không được làm:**

- Không tin giá hoặc loại ghế từ request khách hàng.
- Không tự gọi repository/JPA của Theater.
- Không lưu bằng `List` trong service để dùng khi ứng dụng chạy thật.
- Không gọi save nếu validation thất bại.

### 6.2. `validateBookingRequest(userId, showtimeId, seatIds)`

**Mục đích:** chặn input sai trước khi gọi Theater.

**Phải kiểm tra:**

- `userId` không null.
- `showtimeId` không null.
- `seatIds` không null.
- Số ghế từ 1 đến 8, tính cả hai đầu.
- Không có phần tử null.
- Không có seat ID trùng trong cùng request.

**Kết quả khi sai:** không lưu booking. Implementation hiện ném `IllegalArgumentException`, được `BookingExceptionHandler` ánh xạ về HTTP 400; chuẩn hóa lỗi validation theo SPEC là phần còn cần hoàn thiện.

### 6.3. `validateShowtime(showtimeSnapshot, requestedShowtimeId)`

**Mục đích:** xác nhận suất chiếu vẫn cho phép đặt.

**Phải kiểm tra:**

- Snapshot tồn tại.
- ID suất trong snapshot đúng bằng `requestedShowtimeId`.
- Trạng thái đúng là `SCHEDULED`.
- `startsAt` tồn tại và phải sau thời điểm hiện tại lấy từ `Clock`.
- `auditoriumId` tồn tại.
- `basePrice` tồn tại và lớn hơn 0.

**Kết quả khi sai:** không tiếp tục tạo Booking và không gọi repository save.

### 6.4. `validateSeats(showtime, seatSnapshots, requestedSeatIds)`

**Mục đích:** kiểm tra danh sách ghế Theater trả về khớp request và tất cả ghế thuộc phòng của suất.

**Phải kiểm tra:**

- Danh sách trả về có đủ các ID ghế được yêu cầu, không thừa, không thiếu, không trùng.
- ID phòng của ghế bằng ID phòng của suất; ID ghế phải có trong sơ đồ `ShowtimeSnapshot.seats`.
- Loại ghế tồn tại và thuộc một trong hai loại hỗ trợ: `STANDARD` hoặc `VIP`.

**Kết quả:** chỉ sau khi danh sách ghế hợp lệ mới tạo BookingSeat và tính giá cho từng ghế.

### 6.5. `calculateSeatPrice(basePrice, seatType)`

**Mục đích:** tính giá cuối cùng cho đúng một ghế.

**Quy tắc giá:**

- `STANDARD`: giá bằng `basePrice`.
- `VIP`: giá bằng `basePrice` nhân 1.20.
- Kết quả dùng `BigDecimal`, scale 2, làm tròn `HALF_UP`.
- Không dùng `double`, `float` hoặc giá do client gửi.

### 6.6. Tính `totalAmount`

**Mục đích:** tổng kết giá booking.

**Quy tắc:**

- Cộng giá đã chốt của toàn bộ BookingSeat.
- Chuẩn hóa kết quả về scale 2, `HALF_UP`.
- Không tính lại từ Theater sau khi Booking đã được tạo.

## 7. Transaction và cạnh tranh ghế

`createBooking` đã được đánh dấu `@Transactional`: lưu Booking và toàn bộ BookingSeat thành công hoặc thất bại cùng nhau. Booking gọi `lockShowtimeForBooking` để giữ khóa hàng suất đến hết transaction; hủy suất dùng cùng khóa để không thể vừa hủy vừa đặt vé thành công.

Migration V6 hiện có unique constraint `uq_booking_seat_per_showtime` trên `(showtime_id, seat_id)`. Booking còn lưu thì ghế vẫn được giữ; luồng `deleteBooking` hiện xóa vật lý booking cùng các ghế. Chưa có `released_at` hoặc partial unique index cho hủy mềm.

Theo SPEC, khi triển khai hủy mềm phải bổ sung `released_at`, partial unique index cho ghế chưa giải phóng và chuyển lỗi trùng ghế thành `409 SEAT_ALREADY_BOOKED`.

## 8. Unit test bắt buộc

Unit test không khởi động Spring và không dùng PostgreSQL. Tạo fake `ShowtimeModuleApi`, fake `BookingRepository` và Clock cố định.

Phải có các test:

- 0 ghế bị từ chối.
- 9 ghế bị từ chối.
- Seat ID trùng bị từ chối.
- `userId`, `showtimeId` hoặc seat ID null bị từ chối.
- Không tìm thấy suất bị từ chối.
- Suất `CANCELLED` bị từ chối.
- Suất `FINISHED` bị từ chối.
- Suất đã bắt đầu bị từ chối.
- Ghế không có trong snapshot bị từ chối.
- Ghế thuộc phòng khác bị từ chối.
- Ghế STANDARD có giá bằng giá cơ bản.
- Ghế VIP có giá bằng 120% giá cơ bản.
- Nhiều ghế tính đúng `totalAmount`.
- Khi validation thất bại, repository không được gọi save.
- Khi hợp lệ, repository được gọi save đúng một lần.

## 9. Đối chiếu triển khai và phần còn lại

### Đã có

- Business logic đặt 1–8 ghế, đối chiếu phòng và tính giá bằng snapshot Theater.
- Controller tạo/xem booking, JPA entity, repository PostgreSQL và Flyway migration.
- Unique constraint ghế theo suất; transaction và khóa suất khi tạo booking.
- Endpoint Guest xem ghế `AVAILABLE`/`BOOKED` và guard ngăn hủy suất có booking xác nhận.
- Unit test và integration test Theater–Booking; test đặt vé đồng thời với hủy suất.

### Còn cần hoàn thiện theo SPEC

- Lấy `userId` từ principal đã xác thực và kiểm tra ownership; controller hiện dùng `userId` do client gửi.
- Hủy mềm booking qua HTTP, trạng thái `CONFIRMED`/`CANCELLED`, thời hạn hủy 30 phút, `released_at` và partial unique index.
- Mã booking và endpoint Admin xem mọi booking theo SPEC.
- Chuẩn hóa response/error mapping của Booking, gồm `409 SEAT_ALREADY_BOOKED` và lỗi validation.
- Integration test riêng cho hai khách đồng thời đặt cùng một ghế; test đặt vé–hủy suất hiện có không thay thế tình huống này.
