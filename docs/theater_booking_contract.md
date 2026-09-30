# Contract giao tiếp: Theater → Booking

Tài liệu này chỉ mô tả giao tiếp nội bộ giữa hai module. Đây không phải REST API.

## Mục đích

Booking cần biết suất chiếu có còn được đặt không, ghế khách chọn thuộc phòng nào và ghế đó là STANDARD hay VIP. Booking không được đọc database hoặc repository của Theater.

Booking gọi interface `ShowtimeModuleApi` của Theater.

## 1. Lấy thông tin suất chiếu

**Method:** `getShowtimeSnapshot(showtimeId)`

**Booking gửi:** ID của suất chiếu.

**Theater trả:** một `ShowtimeSnapshot` gồm:

- `showtimeId`: ID suất chiếu.
- `auditoriumId`: ID phòng chiếu của suất.
- `startsAt`: thời điểm bắt đầu.
- `status`: `SCHEDULED`, `CANCELLED` hoặc `FINISHED`.
- `basePrice`: giá cơ bản của suất.

**Booking dùng để:**

- Kiểm tra đúng suất được yêu cầu.
- Chỉ cho đặt suất `SCHEDULED`.
- Không cho đặt sau khi suất bắt đầu.
- Tính giá ghế.

## 2. Lấy thông tin các ghế

**Method:** `getSeatSnapshots(seatIds)`

**Booking gửi:** danh sách ID ghế khách đã chọn.

**Theater trả:** danh sách `SeatSnapshot`. Mỗi ghế gồm:

- `seatId`: ID ghế.
- `auditoriumId`: ID phòng của ghế.
- `type`: `STANDARD` hoặc `VIP`.

**Quy tắc Theater phải bảo đảm:**

- Chỉ trả ghế tồn tại.
- Trả đúng các ID Booking yêu cầu; không thiếu, không thừa, không trùng.
- Không trả trạng thái ghế đã đặt hoặc còn trống. Đây là dữ liệu Booking quản lý.

**Booking dùng để:**

- So sánh `SeatSnapshot.auditoriumId` với `ShowtimeSnapshot.auditoriumId`.
- Từ chối nếu ghế thuộc phòng khác.
- Tính giá theo `type` và `basePrice`.

## 3. Phân chia trách nhiệm

| Theater | Booking |
|---|---|
| Quản lý suất, phòng, ghế vật lý và loại ghế | Quản lý booking và ghế đã được đặt trong từng suất |
| Trả dữ liệu suất theo `showtimeId` | Kiểm tra trạng thái/thời điểm của suất theo luật đặt vé |
| Trả dữ liệu ghế theo `seatIds` | So sánh phòng của suất và phòng của ghế |
| Không biết ghế đã được khách đặt hay chưa | Kiểm tra ghế đã có booking active hay chưa |
| Không tính tổng tiền giao dịch | Tính giá từng ghế và tổng tiền |

## 4. Luồng gọi

1. Khách gửi `showtimeId` và `seatIds` cho Booking.
2. Booking kiểm tra số ghế và ID trùng.
3. Booking gọi Theater lấy `ShowtimeSnapshot`.
4. Booking gọi Theater lấy danh sách `SeatSnapshot`.
5. Booking đối chiếu phòng, tính giá và kiểm tra ghế đã đặt trong dữ liệu của Booking.
6. Booking lưu booking.