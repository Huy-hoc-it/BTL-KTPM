# Báo cáo hiệu năng baseline (Phase 1)

Trạng thái: **Đã hoàn thành đo đạc baseline cho Movie Read và Booking Workload**. Kết quả được trích xuất từ raw JSON của k6 và CSV tài nguyên Docker trong thư mục `load-test/results/20261027-042141` và `load-test/results/20261027-044926`. Đối với kịch bản tranh chấp ghế (Contention), đã xác minh tính toàn vẹn DB và ghi nhận tình trạng lệch mã lỗi hợp đồng của API hiện tại.

---

## Phiên bản và môi trường

| Mục | Giá trị |
|---|---|
| Ngày, người đo | 27/10/2026 — Người 2 (tuhitomi) |
| Git commit đo; working tree sạch | `95cc990e51eced354c5098338c087ccc8729b5bb` (Movie), `48bfac88f10055d71cac6bc3f469d89862898f54` (Booking); working tree sạch (`gitDirty: false`) |
| Nền tảng chạy đo | Máy Windows Local Host (phục vụ baseline nội bộ; sẵn sàng chạy lại trên Kaggle Linux khi bàn giao chung) |
| OS, CPU model, vCPU/quota | Microsoft Windows 11 (NT 10.0.26300.0), Intel(R) Core(TM) i7-14700HX (28 logical processors) |
| RAM khả dụng/giới hạn | 15.71 GiB Physical RAM (Docker Engine memory limit: 7.61 GiB) |
| Java, PostgreSQL, k6, Python | Eclipse Temurin 21 JRE (`eclipse-temurin:21-jre`), PostgreSQL 17.0 (`postgres:17-alpine`), k6 v1.3.0, Node.js v24.20.0 |
| JVM flags, heap/GC, Hikari pool | Container default (G1GC, MaxRAMPercentage=25.0, initial heap 195 MiB, max heap 1.90 GiB), HikariCP pool default: 10 connections |
| PostgreSQL shared_buffers/max_connections | shared_buffers = 128MB, max_connections = 100 |
| Vị trí API/DB/k6 và container limit | `btl-ktpm-api-baseline` (127.0.0.1:8080), `btl-ktpm-db-1` (127.0.0.1:5432) trong Docker; k6 chạy host Windows loopback |
| Tag `phase-1-baseline` | Sẵn sàng tạo sau khi hoàn tất kiểm thử tuần 6 |

---

## Dataset và cách chạy

- **Tổng phim**: 1,000 phim (`MOVIE_COUNT=1000`), gồm 50 phim xóa mềm (`DELETED_COUNT=50`), hot set là 100 phim đầu (ID ngẫu nhiên theo seed).
- **Phòng chiếu & Suất chiếu**: 1 phòng chiếu (100 ghế), 2 suất chiếu độc lập (1 suất thường cho workload Booking, 1 suất dành riêng cho Contention).
- **User tải**: 100 user fixture chuyên dụng (50 user warm-up, 50 user đo) + 50 contender users phục vụ tranh ghế; có JWT Bearer được tạo sẵn trong `fixtures.local.json`.
- **Cấu hình thời gian**:
  - Warm-up: 30 giây (loại bỏ khỏi tính toán throughput và percentile).
  - Pha đo chính (measure): 120 giây liên tục.
  - Ba lượt chạy lặp lại (`run1`, `run2`, `run3`) cho mỗi mức 10 VU, 25 VU, 50 VU.
- **Mô hình tải (Workload & Think time)**:
  - Think time: 100ms giữa các iteration của mỗi VU (closed-loop model).
  - Tỷ lệ workload Booking: 35% Movie list, 25% Showtime list, 25% Seat availability, 10% POST booking, 5% GET my bookings.
- **Thresholds**:
  - SLA p95 latency < 500ms đối với mọi request đọc và đặt vé.
  - Tỷ lệ lỗi kỹ thuật (`measured_technical_error`) = 0%.
  - Tỷ lệ lỗi nghiệp vụ (`measured_business_error`) = 0%.
- **Cách thu thập tài nguyên**: Lấy mẫu Docker stats định kỳ 1 giây/lần. Bộ lọc chỉ tính trong 120 giây của pha đo `measure`.

---

## Movie read — từng lượt

| VU | Lượt | Requests | req/s | GET p50 ms | GET p95 ms | GET p99 ms | Lỗi kỹ thuật % | Lỗi nghiệp vụ % |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 1 | 11,171 | 93.09 | 4.43 | 10.74 | 16.18 | 0.00% | 0.00% |
| 10 | 2 | 11,180 | 93.17 | 4.08 | 9.84 | 18.25 | 0.00% | 0.00% |
| 10 | 3 | 11,146 | 92.88 | 4.59 | 10.02 | 15.81 | 0.00% | 0.00% |
| 25 | 1 | 27,795 | 231.63 | 4.71 | 10.93 | 16.70 | 0.00% | 0.00% |
| 25 | 2 | 27,770 | 231.42 | 4.92 | 10.73 | 15.52 | 0.00% | 0.00% |
| 25 | 3 | 27,826 | 231.88 | 4.62 | 10.31 | 14.33 | 0.00% | 0.00% |
| 50 | 1 | 56,881 | 474.01 | 3.47 | 8.96 | 12.92 | 0.00% | 0.00% |
| 50 | 2 | 56,645 | 472.04 | 3.82 | 9.34 | 13.54 | 0.00% | 0.00% |
| 50 | 3 | 56,528 | 471.07 | 3.88 | 9.69 | 13.61 | 0.00% | 0.00% |

---

## Booking workload — từng lượt

Workload: 35% Movie list, 25% showtime list, 25% seat availability, 10% POST booking, 5% GET own bookings. Các booking do user fixture tạo ra được hủy trước lần lặp tiếp theo và giải phóng toàn bộ khi teardown.

| VU | Lượt | Requests | req/s | GET p50/p95/p99 ms | POST p50/p95/p99 ms | Lỗi kỹ thuật % | Lỗi nghiệp vụ % |
|---:|---:|---:|---:|---|---|---:|---:|
| 10 | 1 | 11,152 | 92.93 | 3.80 / 10.01 / 19.02 | 5.23 / 12.85 / 42.05 | 0.00% | 0.00% |
| 10 | 2 | 11,113 | 92.61 | 4.30 / 10.29 / 19.26 | 4.83 / 11.19 / 28.74 | 0.00% | 0.00% |
| 10 | 3 | 11,127 | 92.72 | 4.15 / 11.67 / 24.86 | 4.75 / 13.38 / 40.44 | 0.00% | 0.00% |
| 25 | 1 | 28,047 | 233.72 | 3.86 / 9.60 / 13.90 | 5.08 / 10.34 / 19.05 | 0.00% | 0.00% |
| 25 | 2 | 27,882 | 232.35 | 4.14 / 10.08 / 14.10 | 5.14 / 10.25 / 15.78 | 0.00% | 0.00% |
| 25 | 3 | 27,752 | 231.27 | 4.81 / 10.44 / 14.72 | 5.19 / 10.73 / 17.42 | 0.00% | 0.00% |
| 50 | 1 | 56,035 | 466.96 | 3.86 / 10.74 / 14.50 | 6.39 / 13.98 / 19.42 | 0.00% | 0.00% |
| 50 | 2 | 55,729 | 464.41 | 4.46 / 11.25 / 16.16 | 6.78 / 14.95 / 20.05 | 0.00% | 0.00% |
| 50 | 3 | 56,607 | 471.73 | 3.28 / 9.58 / 14.67 | 6.14 / 13.89 / 20.13 | 0.00% | 0.00% |

---

## Contention — đúng đắn dữ liệu

| VU | Lượt | Winner 201 | `SEAT_ALREADY_BOOKED` 409 | Kết quả khác | Active claim sau test | Xác nhận không double-booking |
|---:|---:|---:|---:|---:|---:|---|
| 10 | Thử nghiệm | 1 | 0 | 9 (HTTP 400 Bad Request) | 1 booking duy nhất | **Đã xác nhận không double-booking** |
| 25 | Baseline SPEC | — | — | — | — | Chờ Người 4 chuẩn hóa mã lỗi HTTP 409 |
| 50 | Baseline SPEC | — | — | — | — | Chờ Người 4 chuẩn hóa mã lỗi HTTP 409 |

### Đánh giá tranh chấp ghế và tính toàn vẹn:
1. **Tính đúng đắn dữ liệu (Database Integrity)**:
   - Khi cho nhiều tài khoản người dùng đồng thời tranh chấp đúng 1 ghế duy nhất trong cùng 1 suất chiếu, cơ chế transaction và khóa của tầng Booking đã hoạt động hiệu quả: **Chỉ có đúng 1 request được ghi thành công (Winner)** và cơ sở dữ liệu PostgreSQL chỉ tồn tại duy nhất 1 bản ghi `booking_seats` cho ghế này. Hoàn toàn không xảy ra hiện tượng **double-booking**.
2. **Lệch hợp đồng (Contract Deviation)**:
   - Theo đặc tả `docs/SPEC.md` và `docs/booking_requirements.md`, request thất bại khi ghế đã có người đặt phải nhận mã lỗi **HTTP 409 Conflict** với body JSON chứa error code `SEAT_ALREADY_BOOKED`.
   - Tuy nhiên, triển khai hiện tại của `BookingService.java` (thuộc phạm vi Người 4) đang ném `IllegalArgumentException("Seat has already been booked")`, dẫn đến ExceptionHandler trả về **HTTP 400 Bad Request**.
   - Theo quy tắc phân chia quyền hạn của Người 2 (không can thiệp sửa đổi mã nguồn nghiệp vụ của module Booking), Người 2 không sửa code này mà ghi nhận chính xác lỗi contract tại đây để Người 4 hoàn thiện.

---

## Tài nguyên

| VU / kịch bản | API CPU TB / đỉnh % | API RSS đỉnh MiB | DB CPU TB / đỉnh % | DB RSS đỉnh MiB | k6 CPU / RSS đỉnh |
|---|---|---|---|---|---|
| **10 VU (Movie)** | 8.2% / 12.3% | 558.9 MiB | 5.1% / 8.0% | 74.7 MiB | 0.5% / 51.3 MiB |
| **25 VU (Movie)** | 19.9% / 31.8% | 572.4 MiB | 12.4% / 17.6% | 75.3 MiB | 1.3% / 69.3 MiB |
| **50 VU (Movie)** | 43.5% / 67.0% | 578.1 MiB | 25.1% / 36.6% | 78.4 MiB | 2.1% / 93.7 MiB |
| **10 VU (Booking)** | 13.6% / 27.9% | 748.0 MiB | 7.6% / 11.2% | 86.5 MiB | 0.4% / 53.6 MiB |
| **25 VU (Booking)** | 29.0% / 41.7% | 755.5 MiB | 19.3% / 26.7% | 86.3 MiB | 1.0% / 76.5 MiB |
| **50 VU (Booking)** | 61.1% / 88.6% | 758.8 MiB | 41.9% / 63.7% | 85.9 MiB | 1.8% / 109.4 MiB |

*Ghi chú*:
- Tỷ lệ CPU container được trích xuất từ dữ liệu `docker stats` ghi nhận định kỳ mỗi giây và lấy giá trị trung vị (median) của các lượt chạy trong 120 giây đo.
- Mức RAM RSS phản ánh mức tiêu thụ bộ nhớ thực tế của tiến trình bên trong container. API duy trì bộ nhớ rất ổn định quanh ngưỡng ~578 MiB (Movie) và ~758 MiB (Booking), không có hiện tượng rò rỉ bộ nhớ (memory leak).
- PostgreSQL duy trì bộ nhớ rất gọn gàng (< 90 MiB) và giải phóng kết nối nhanh chóng nhờ HikariCP.

---

## Nhận xét và bằng chứng

1. **Khả năng mở rộng (Scalability & Throughput)**:
   - Cả hai kịch bản đều thể hiện độ tăng tuyến tính gần như lý tưởng:
     - 10 VU đạt ~93 req/s.
     - 25 VU đạt ~232 req/s.
     - 50 VU đạt ~470 – 474 req/s.
2. **Độ trễ (Latency SLA)**:
   - Toàn bộ các mức tải đều vượt xa kỳ vọng SLA đề ra (p95 < 500ms):
     - Movie Read: p95 duy trì từ **8.96 ms đến 10.93 ms**.
     - Booking Workload: p95 đọc từ **9.58 ms đến 11.67 ms**, p95 đặt vé từ **10.25 ms đến 14.95 ms**.
     - Ngay cả ở mức 50 VU với độ trễ cao nhất (p99), hệ thống cũng chỉ mất dưới 25 ms cho các yêu cầu đọc và đặt vé thông thường.
3. **Độ tin cậy (Error Rate)**:
   - Tỷ lệ lỗi kỹ thuật (5xx/Network) và lỗi nghiệp vụ không mong muốn: **0.00%** qua toàn bộ 18 lượt chạy baseline chính thức.
4. **Vị trí lưu trữ dữ liệu raw làm bằng chứng**:
   - Movie Baseline: `load-test/results/20261027-042141/`
   - Booking Baseline: `load-test/results/20261027-044926/`
   - Dữ liệu bao gồm đầy đủ file raw metrics k6 (`.json`), output console log (`.log`), thông số tài nguyên host (`-resources.csv`) và thông số container Docker (`-docker-resources.csv`).
