Phân công

Kiến trúc chung: Modular Monolith; mỗi module `identity`, `movie`, `theater`, `booking` tự chứa ba tầng `api`, `business`, `data`. Mỗi thành viên chịu trách nhiệm trọn luồng ba tầng của module mình.

Thành viên	Module chính	Việc bổ sung
Người 1	identity	Khởi tạo codebase, Spring Security/JWT, Docker, CI, quản lý pom.xml và cấu hình chung
Người 2	movie	Swagger/OpenAPI tổng thể, README, k6 và báo cáo baseline
Người 3	theater	Auditorium, Seat, Showtime, kiểm tra trùng lịch
Người 4	booking	Đặt/hủy vé, trạng thái ghế, transaction, chống double-booking


Người 1 chỉ tạo skeleton tối thiểu, không viết sẵn abstraction cho mọi module:
- Maven/Spring Boot.
- Cấu trúc package.
- PostgreSQL/Flyway.
- Docker Compose.
- Security skeleton.
- Exception response chung.
- Swagger, healthcheck.
- Một ArchUnit test cơ bản.
Sau khi skeleton được merge, cả bốn người bắt đầu module riêng.
Kế hoạch 6 tuần
Tuần 1 — Khởi tạo và chốt interface
Ngày 1–2, Người 1 tạo codebase. Những người còn lại rà API, schema và test case của module mình.
Ngày 3–5:
- Người 1: đăng ký, đăng nhập và JWT.
- Người 2: Create/Get/List Movie làm vertical slice mẫu.
- Người 3: business và data cho phòng, ghế, suất chiếu.
- Người 4: business Booking và unit test với fake Theater interface.
Cuối tuần phải đạt:
- docker compose up chạy được.
- Swagger truy cập được.
- Flyway chạy được.
- Các package/module đã tồn tại.
- Chốt hai interface liên module tối thiểu:
theater -> MovieModuleApi
booking -> ShowtimeModuleApi
Tuần 2 — Hoàn thiện chức năng độc lập
- Người 1: register, login, /users/me, phân quyền Admin.
- Người 2: Movie CRUD, validation và test.
- Người 3: Auditorium, Seat Layout, tạo và truy vấn Showtime.
- Người 4: Create Booking, tính giá, repository và migration.
Cuối tuần: Identity và Movie nên hoàn thành; Theater và Booking chạy được happy path.
Tuần 3 — Hoàn thiện nghiệp vụ chính
- Theater: kiểm tra overlap, xóa mềm phòng, hủy suất chiếu.
- Booking: xem booking, hủy booking, ownership.
- Thêm partial unique index chống đặt trùng.
- Hoàn thiện error mapping và OpenAPI của từng module.
- Mỗi người viết unit/integration test cho module mình.
Endpoint trạng thái ghế /showtimes/{id}/seats nên do Booking module xử lý: lấy sơ đồ từ Theater rồi kết hợp booking đang hoạt động. Như vậy tránh dependency vòng theater -> booking -> theater.
Tuần 4 — Tích hợp
- Ghép Movie → Theater → Booking.
- Test toàn bộ luồng Admin tạo dữ liệu.
- Test Customer đăng nhập và đặt/hủy vé.
- Kiểm tra 401, 403, 404, 409.
- Test hai request cùng đặt một ghế.
- Hoàn thiện seed data.
Cuối tuần phải đạt feature-complete; không thêm nghiệp vụ mới.
Tuần 5 — Đóng gói và chuẩn bị đánh giá
- Hoàn thiện Docker.
- Rà Swagger với API thực tế.
- Hoàn thiện README và sơ đồ kiến trúc.
- Viết k6: smoke, read-heavy, booking, contention.
- Chạy thử tải và sửa lỗi rõ ràng.
- Tag bản chuẩn bị kiểm thử.
Tuần 6 — Chỉ kiểm thử và sửa lỗi
- Không thêm tính năng.
- Chạy unit, integration, E2E và concurrency test.
- Chạy tải ít nhất ba lần trên cùng cấu hình Kaggle.
- Ghi p50, p95, p99, throughput, error rate, CPU/RAM.
- Rà Docker từ máy sạch.
- Chốt tag phase-1-baseline.
Quy tắc làm song song
- Mỗi người chỉ sửa package và migration thuộc module mình.
- pom.xml, shared, security và config chung do Người 1 quản lý.
- Interface liên module phải merge sớm; module phụ thuộc dùng fake/mock, không chờ module kia hoàn thành.
- Không truy cập repository của module khác.
- Feature branch ngắn, merge hằng ngày hoặc tối đa 2 ngày; tránh giữ branch cả tuần.
- Mỗi người tự viết test cho module mình, không dồn test cho một người ở tuần cuối.
- Review chéo: Người 1 ↔ Người 4, Người 2 ↔ Người 3.
