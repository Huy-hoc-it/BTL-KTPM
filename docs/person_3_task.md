# Checklist công việc Người 3 — Theater

Phạm vi được tổng hợp từ [`work_plan.md`](work_plan.md) và [`SPEC.md`](SPEC.md), đối chiếu với mã nguồn hiện tại trong module `theater`.

**Quy ước:** `[x]` nghĩa là đã thấy phần triển khai tương ứng trong repository; `[ ]` nghĩa là còn thiếu hoặc cần hoàn tất/xác nhận. Contract hoặc lớp service tồn tại chưa đồng nghĩa với việc đã được Spring đăng ký và tích hợp runtime. Việc đánh dấu không thay thế cho việc chạy kiểm thử.

## 1. Ranh giới và trách nhiệm

- [x] Chịu trách nhiệm module `theater`, gồm `Auditorium`, `Seat`, `Showtime` và kiểm tra trùng lịch.
- [x] Entity, repository và migration của `auditoriums`, `seats`, `showtimes` thuộc module Theater.
- [x] Định nghĩa contract `ShowtimeModuleApi` với snapshot suất chiếu và ghế cho module Booking sử dụng.
- [ ] Xác nhận các module khác gọi Theater qua public business API; repository/JPA entity Theater không được truy cập xuyên module. Hiện chưa có module Booking trong codebase để kiểm tra tích hợp này.
- [x] Đăng ký `AuditoriumService` và `ShowtimeService` làm Spring beans; expose `ShowtimeModuleApi` từ bean `ShowtimeService`. `MovieRepositoryAdapter` triển khai `MovieModuleApi` bằng repository JPA; API integration test lưu Movie thật vào PostgreSQL và dùng Movie đó để tạo suất.
- [x] Để endpoint trạng thái ghế tổng hợp với booking do module Booking xử lý, tránh phụ thuộc vòng Theater → Booking → Theater.

## 2. Auditorium và sơ đồ ghế

- [x] Tạo phòng với trạng thái ban đầu `ACTIVE`, tên duy nhất và timestamps.
- [x] Tạo ghế từ sơ đồ theo hàng, khoảng số ghế và loại `STANDARD`/`VIP`.
- [x] Chuẩn hóa nhãn hàng và phát hiện vị trí ghế trùng trong cùng sơ đồ.
- [x] Lưu ràng buộc duy nhất `(auditorium_id, row_label, seat_number)` và kiểm tra seat number dương ở PostgreSQL.
- [x] Tính sức chứa từ số ghế; không lưu thêm cột `capacity`.
- [x] Có luồng liệt kê phòng đang hoạt động.
- [x] Đảm bảo truy vấn phòng hoạt động lọc theo `status = ACTIVE` và `deleted_at IS NULL`.
- [x] Xóa mềm phòng bằng cách chuyển trạng thái `INACTIVE` và ghi `deleted_at`.
- [x] Từ chối xóa mềm phòng khi còn suất chiếu `SCHEDULED` trong tương lai.
- [x] Hoàn thiện REST endpoint Admin liệt kê phòng `GET /api/v1/admin/auditoriums`.
- [x] Hoàn thiện REST endpoint Admin tạo phòng và sơ đồ ghế `POST /api/v1/admin/auditoriums`.
- [ ] Hoàn thiện REST endpoint Admin xóa mềm phòng `DELETE /api/v1/admin/auditoriums/{id}`.
- [x] Bổ sung DTO, validation đầu vào và response/error mapping cho các endpoint Auditorium; bảo đảm endpoint quản trị yêu cầu quyền Admin.
- [ ] Xác nhận hoặc bổ sung kiểm thử cho các trường hợp sơ đồ rỗng/không hợp lệ, khoảng ghế sai, tên trùng và xóa phòng có lịch tương lai.

## 3. Showtime và lịch chiếu

- [x] Logic tạo suất kiểm tra Movie qua `MovieModuleApi`, chỉ nhận Movie chưa bị xóa mềm; tích hợp runtime qua repository PostgreSQL được API integration test xác nhận.
- [x] Chỉ tạo suất trong phòng `ACTIVE` và có giờ bắt đầu ở tương lai.
- [x] Tính `endsAt = startsAt + thời lượng phim + 15 phút dọn phòng`.
- [x] Phát hiện khoảng thời gian chồng lấn với suất không bị hủy trong cùng phòng.
- [x] Lưu trạng thái ban đầu `SCHEDULED`, giá cơ bản và timestamps.
- [x] Truy vấn suất theo phim và khoảng thời gian; sắp xếp tăng dần theo giờ bắt đầu ở tầng data.
- [x] Truy vấn suất theo ID và chuyển trường hợp không tìm thấy thành lỗi nghiệp vụ.
- [x] Hủy mềm suất bằng trạng thái `CANCELLED`, không xóa vật lý; từ chối hủy suất không còn ở trạng thái `SCHEDULED` hoặc đã bắt đầu.
- [x] Hoàn thiện REST endpoint công khai truy vấn lịch chiếu `GET /api/v1/showtimes?movieId=...&date=...`.
- [x] Hoàn thiện REST endpoint công khai xem chi tiết `GET /api/v1/showtimes/{id}`.
- [x] Hoàn thiện REST endpoint Admin tạo suất `POST /api/v1/admin/showtimes`.
- [ ] Hoàn thiện REST endpoint Admin hủy suất `DELETE /api/v1/admin/showtimes/{id}`.
- [x] Bổ sung DTO, validation, quy đổi ngày UTC thành khoảng thời gian phù hợp và response/error mapping; bảo đảm thao tác tạo yêu cầu quyền Admin.
- [x] Ánh xạ lỗi nghiệp vụ Theater sang HTTP status/error code theo SPEC trong `TheaterExceptionHandler`.
- [ ] Phối hợp với Người 4 để từ chối hủy suất đã có booking `CONFIRMED`, theo quy tắc trong SPEC; thống nhất interface liên module sao cho không tạo dependency vòng.
- [ ] Xem xét bảo đảm không thể tạo hai lịch chồng nhau khi có request đồng thời; hiện thấy kiểm tra overlap ở service/repository nhưng chưa thấy ràng buộc cơ sở dữ liệu chống race condition.
- [x] Lịch công khai chỉ trả suất `SCHEDULED`; chi tiết suất vẫn trả trạng thái để client thấy `CANCELLED`/`FINISHED`.

## 4. Trạng thái ghế và giao tiếp với Booking

- [x] Contract `ShowtimeModuleApi` mô tả snapshot gồm ID phòng, giờ chiếu, giá, trạng thái và danh sách ghế.
- [x] Dùng kiểu dữ liệu immutable cho contract liên module; ánh xạ loại ghế `STANDARD`/`VIP`.
- [x] Có kiểm thử unit cho contract `ShowtimeModuleApi`.
- [x] Không nhận trách nhiệm tổng hợp trạng thái còn trống/đã đặt của ghế ở module Theater; Booking sở hữu trạng thái booking.
- [ ] Sau khi có module Booking, xác nhận module này dùng `ShowtimeModuleApi` để dựng endpoint `GET /api/v1/showtimes/{id}/seats`; endpoint này không thuộc controller Theater. Hiện chưa có code Booking để xác nhận.

## 5. Data, migration và kiểm thử

- [x] Có entity, mapper và repository adapter JPA cho Auditorium, Seat, Showtime.
- [x] Có migration `V3__create_theater_tables.sql` tạo ba bảng, foreign key phòng, unique vị trí ghế, check constraint trạng thái/loại ghế/giá/thời gian và index tra cứu lịch.
- [ ] Đối chiếu quyết định migration với quy ước trong SPEC: SPEC dự kiến V3 tạo auditorium/seats và V4 tạo showtimes, trong khi code hiện gộp cả ba bảng vào V3. Xác nhận đây là thay đổi có chủ ý hoặc cập nhật SPEC trước khi migration được dùng trên môi trường chung.
- [x] Có unit test cho `AuditoriumService`, `ShowtimeService` và `ShowtimeModuleApi`.
- [x] Có `TheaterPersistenceIntegrationTest` cho persistence của module Theater.
- [x] Có `TheaterBusinessFlowIntegrationTest`: tạo phòng/ghế và suất chiếu qua business service, sau đó truy vấn suất theo ID/khoảng thời gian và lấy snapshot ghế qua `ShowtimeModuleApi` trên PostgreSQL Testcontainers.
- [x] Chạy `./mvnw -B -ntp clean verify` bằng Java 21 và Docker; build thành công, 37 test passed, không có failure/error/skip.
- [ ] Mở rộng integration test để xác nhận check/unique constraint ở PostgreSQL và các hành vi service khác chưa được test trên DB.
- [x] Chạy lại và bổ sung kiểm thử sau khi hoàn thiện REST API, wiring service và Movie adapter; `./mvnw -B -ntp clean verify` thành công, 37 test passed.
- [x] Xác nhận integration test truy vấn suất theo phim và khoảng thời gian, đồng thời dựng snapshot suất/ghế qua contract Booking. API integration test dùng `MovieRepositoryAdapter` và một Movie row thật trong PostgreSQL, không mock provider.
- [ ] Bổ sung integration test hủy suất và từ chối hủy khi có booking `CONFIRMED` sau khi module Booking được tích hợp. Overlap đã được kiểm tra qua API integration test.
- [x] Kiểm tra API Theater qua OpenAPI và API integration test; xác nhận các status 201/200/403/404/409/422.

## 6. Đối chiếu tiến độ hiện tại

**Phạm vi task được giao đã hoàn thành:** Admin tạo phòng cùng ghế và tạo suất cho Movie thật chưa bị xóa mềm; giờ kết thúc lấy từ thời lượng Movie cộng 15 phút; suất chỉ tạo được với phòng hợp lệ; Guest xem lịch và chi tiết. API integration test xác nhận các luồng này trên PostgreSQL, bao gồm sai sơ đồ ghế, Movie không tồn tại/đã xóa, phòng không tồn tại và thời gian kết thúc.

Các checkbox còn mở phía trên là các mục rộng hơn trong SPEC/work plan (xóa phòng/hủy suất, phối hợp Booking, concurrency và hoàn thiện thứ tự migration); chúng không thuộc phạm vi endpoint được giao ở task này. CRUD Movie vẫn thuộc phần việc module Movie; Theater chỉ dùng business contract và adapter tra cứu Movie.

Các mục `[x]` được xác nhận từ mã nguồn và test. API integration test lưu Movie thật qua `MovieJpaRepository`; Theater đọc bản ghi qua bean `MovieRepositoryAdapter` trước khi tạo suất. `./mvnw -B -ntp clean verify` thành công với Java 21: 37 test passed, không có failure/error/skip.
