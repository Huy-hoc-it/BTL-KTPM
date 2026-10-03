# Yêu cầu chi tiết module Booking

## 1. Mục tiêu và phạm vi

Module Booking chịu trách nhiệm biến yêu cầu chọn ghế của khách thành một booking hợp lệ. Booking sở hữu người đặt, suất được đặt, các ghế đã chọn, giá chốt của từng ghế và tổng tiền.

Trong bước hiện tại, cần hoàn thiện business logic đặt ghế và unit test với fake Theater. API HTTP, JPA/PostgreSQL thật, migration, xem booking, hủy booking và chống double-booking ở database được hoàn thiện sau.

## 2. Ranh giới với Theater

Theater sở hữu phòng chiếu, ghế vật lý, loại ghế, suất chiếu, trạng thái suất, thời điểm bắt đầu và giá cơ bản. Booking không đọc controller, repository, entity JPA hoặc bảng của Theater.

Booking chỉ gọi public business contract của Theater là `ShowtimeModuleApi`.

### 2.1. `ShowtimeModuleApi.getShowtimeSnapshot` và `getSeatSnapshots`

**Mục đích:** tách dữ liệu suất chiếu và dữ liệu ghế thành hai truy vấn độc lập để Booking tự đối chiếu chúng.

**Đầu vào:**

- `getShowtimeSnapshot` nhận `showtimeId`.
- `getSeatSnapshots` nhận danh sách `seatIds` đã qua kiểm tra sơ bộ ở Booking.

**Đầu ra:**

- Showtime snapshot: ID suất, ID phòng, thời điểm bắt đầu, trạng thái và giá cơ bản.
- Danh sách Seat snapshot: ID ghế, ID phòng của ghế và loại ghế `STANDARD` hoặc `VIP`.

**Quy tắc:** Theater phải trả đúng dữ liệu tại thời điểm gọi. Nếu không tìm thấy suất, implementation Theater phải báo lỗi không tìm thấy hoặc trả kết quả mà Booking nhận diện được là không có suất. Snapshot không được là entity JPA.

## 3. `Booking`

**Mục đích:** mô tả kết quả nghiệp vụ của một lần đặt ghế.

**Dữ liệu phải giữ:**

- ID booking; có thể sinh ở business hoặc database ở bước sau.
- `userId`: người đặt.
- `showtimeId`: suất được đặt.
- Danh sách `BookingSeat`.
- `totalAmount`: tổng tiền chốt tại lúc tạo booking.
- Khi hoàn thiện persistence: trạng thái booking, mã booking, thời gian tạo và thời gian hủy.

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
- Khi triển khai database, `BookingSeat` sẽ có thêm `bookingId`, `showtimeId` và `releasedAt`.

## 5. `BookingRepository`

**Mục đích:** abstraction để BookingService lưu booking mà không biết JPA/PostgreSQL.

### 5.1. `save(booking)`

**Đầu vào:** một Booking đã vượt toàn bộ validation nghiệp vụ.

**Kết quả:** booking đã được lưu. Khi có database, kết quả phải chứa ID do persistence xác nhận nếu ID được sinh ở database.

**Quy tắc:**

- Business service chỉ gọi interface này.
- Unit test dùng fake repository; fake có thể giữ booking trong biến hoặc map trong phạm vi test.
- Tuần sau tầng `booking/data` triển khai interface bằng JPA/PostgreSQL.
- Khi unique index báo trùng ghế, implementation data phải chuyển lỗi đó thành lỗi nghiệp vụ `SEAT_ALREADY_BOOKED`.

## 6. `BookingService`

**Mục đích:** điều phối toàn bộ use case tạo booking. Service không chứa HTTP controller, JSON, SQL, JPA entity hoặc truy cập repository của Theater.

**Dependency bắt buộc:**

- `BookingRepository` để lưu booking.
- `ShowtimeModuleApi` để đọc dữ liệu Theater.
- `Clock` để so sánh thời điểm bắt đầu một cách ổn định trong test.

### 6.1. `createBooking(userId, showtimeId, seatIds)`

**Đầu vào:**

- `userId`: lấy từ security context khi có API; trong unit test truyền trực tiếp.
- `showtimeId`: do khách chọn.
- `seatIds`: do khách chọn.

**Trình tự bắt buộc:**

1. Gọi `validateBookingRequest`.
2. Gọi `getShowtimeSnapshot` một lần cho suất và `getSeatSnapshots` một lần cho danh sách ghế.
3. Gọi `validateShowtime`.
4. Đối chiếu ID phòng của từng Seat snapshot với ID phòng của Showtime snapshot; tạo BookingSeat cho từng ghế hợp lệ.
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

**Kết quả khi sai:** ném business exception phù hợp. Trong bản tạm thời có thể dùng exception validation đơn giản; khi có API phải được map về lỗi validation HTTP.

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

### 6.4. `validateSeats(seatSnapshots, requestedSeatIds, auditoriumId)`

**Mục đích:** kiểm tra danh sách ghế Theater trả về khớp request và tất cả ghế thuộc phòng của suất.

**Phải kiểm tra:**

- Danh sách trả về có đủ các ID ghế được yêu cầu, không thừa, không thiếu, không trùng.
- ID phòng của ghế bằng ID phòng của suất.
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

`createBooking` sẽ được đánh dấu transaction khi persistence hoàn chỉnh. Transaction giúp việc lưu Booking và toàn bộ BookingSeat thành công hoặc thất bại cùng nhau.

Transaction một mình không đủ để chống hai request cùng đặt một ghế. Tuần sau database phải có partial unique index cho cặp `showtime_id` và `seat_id` khi ghế chưa được giải phóng. Nếu index từ chối insert, Booking phải trả lỗi nghiệp vụ `SEAT_ALREADY_BOOKED`.

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

## 9. Chưa thuộc phạm vi hiện tại

- Controller và endpoint HTTP POST booking.
- Lấy `userId` thật từ Spring Security.
- JPA entity, database schema và Flyway migration.
- Repository PostgreSQL thật.
- Xem booking, phân quyền owner/admin, hủy booking.
- Ghế đã đặt/chưa đặt và partial unique index.
- Integration test PostgreSQL và test hai request cạnh tranh cùng ghế.