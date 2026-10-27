# Báo cáo hiệu năng — mẫu theo SPEC

Trạng thái: **chưa có baseline của bản hiện tại**. Các ô dưới đây phải được điền từ raw k6 và CSV tài nguyên; không dùng số liệu lượt chạy của commit khác.

## Phiên bản và môi trường

| Mục | Giá trị |
|---|---|
| Ngày, người đo | Chưa ghi |
| Git commit đo; working tree sạch | Chưa ghi |
| Kaggle notebook/URL và phiên chạy | Chưa ghi |
| OS, CPU model, vCPU/quota | Chưa ghi |
| RAM khả dụng/giới hạn | Chưa ghi |
| Java, PostgreSQL, k6, Python | Chưa ghi |
| JVM flags, heap/GC, Hikari pool | Chưa ghi |
| PostgreSQL shared_buffers/max_connections | Chưa ghi |
| Vị trí API/DB/k6 và container limit | Chưa ghi |
| Tag `phase-1-baseline` | Chưa tạo |

## Dataset và cách chạy

- Tổng phim, phim xóa mềm, hot set: chưa ghi.
- Showtime, số ghế, số user tải: chưa ghi.
- Lệnh/biến môi trường (đã bỏ secret): chưa ghi.
- Warm-up 30 giây; 10/25/50 VU; 120 giây đo; ba lượt mỗi mức: chưa chạy.
- Think time, workload ratio: chưa ghi.
- Threshold và lý do chọn: chưa ghi.
- CPU/RAM lấy trong phase đo hay cả lượt: chưa ghi.

## Movie read — từng lượt

| VU | Lượt | Requests | req/s | GET p50 ms | GET p95 ms | GET p99 ms | Lỗi kỹ thuật % | Lỗi nghiệp vụ % |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 1 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 10 | 2 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 10 | 3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 25 | 1 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 25 | 2 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 25 | 3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 50 | 1 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 50 | 2 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 50 | 3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |

## Booking workload — từng lượt

Workload: 35% Movie list, 25% showtime list, 25% seat availability, 10% POST booking, 5% GET own bookings. Ghi cleanup/cancel phát sinh riêng.

| VU | Lượt | Requests | req/s | GET p50/p95/p99 ms | POST p50/p95/p99 ms | Lỗi kỹ thuật % | Lỗi nghiệp vụ % |
|---:|---:|---:|---:|---|---|---:|---:|
| 10 | 1 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 10 | 2 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 10 | 3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 25 | 1 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 25 | 2 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 25 | 3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 50 | 1 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 50 | 2 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 50 | 3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |

## Contention — đúng đắn dữ liệu

| VU | Lượt | Winner 201 | `SEAT_ALREADY_BOOKED` 409 | Kết quả khác | Active claim sau test | Xác nhận không double-booking |
|---:|---:|---:|---:|---:|---:|---|
| 10 | 1–3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa xác nhận |
| 25 | 1–3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa xác nhận |
| 50 | 1–3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa xác nhận |

Mỗi lượt contention phải có đúng một winner, N−1 conflict đúng mã và không có kết quả khác. Giữ winner đến khi lưu truy vấn DB; dùng ghế fixture khác nhau cho từng lượt.

## Tài nguyên

| VU/kịch bản | API CPU TB/đỉnh % | API RSS đỉnh MiB | DB CPU TB/đỉnh % | DB RSS đỉnh MiB | k6 CPU/RSS đỉnh |
|---|---|---|---|---|---|
| 10 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 25 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 50 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |

Nêu cách lấy RSS và giới hạn/quota. Tổng RSS tiến trình có thể đếm trùng bộ nhớ chia sẻ. Báo median và min–max qua ba lượt; không lấy trung bình các p95 để gọi là p95 chung.

## Nhận xét và bằng chứng

Nhận xét latency/throughput/lỗi và giới hạn của máy chạy k6; chỉ nêu nguyên nhân sau khi có profiling. Raw k6, summary, resource CSV, `environment.json`, truy vấn xác minh DB và notebook: **chưa có**.
