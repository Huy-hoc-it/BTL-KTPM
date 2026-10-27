# Báo cáo hiệu năng — mẫu điền sau khi đo

Trạng thái: **chưa đo**. Các ô dưới đây không phải số liệu thực nghiệm.

## Phiên bản và môi trường

| Mục | Giá trị |
|---|---|
| Ngày, người đo | Chưa ghi |
| Git commit; working tree có thay đổi không | Chưa ghi |
| Phạm vi Movie API đọc | Chưa ghi |
| Máy/Kaggle notebook, hệ điều hành | Chưa ghi |
| CPU model, vCPU, quota | Chưa ghi |
| RAM khả dụng/giới hạn | Chưa ghi |
| Java / PostgreSQL / k6 | Chưa ghi |
| JVM flags, heap, GC; Hikari pool | Chưa ghi |
| PG shared_buffers, max_connections | Chưa ghi |
| Vị trí API/DB/k6; container limit nếu có | Chưa ghi |
| Tag baseline nếu đã tạo | Chưa ghi |

## Dataset và cách chạy

- Tổng số phim, số phim xóa mềm và kích thước hot set: chưa ghi.
- Lệnh chạy và biến môi trường đã che secret: chưa ghi.
- 10/25/50 VU, warm-up 30 giây, đo ít nhất 120 giây/mức, ít nhất ba lần.
- Think time và tỷ trọng GET danh sách/chi tiết: chưa ghi.
- Threshold và lý do chọn: chưa ghi.
- Công cụ thu tài nguyên; khoảng đo có bao gồm warm-up không: chưa ghi.

## Kết quả từng lượt

Chỉ dùng phase measure. Throughput = request nghiệp vụ đo / thời gian đo. K6 trend med là p50.

| VU | Lượt | Request | req/s | p50 GET ms | p95 GET ms | p99 GET ms | Lỗi kỹ thuật % | Status bất ngờ |
|---|---|---|---|---|---|---|---|---|
| 10 | 1 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 10 | 2 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 10 | 3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 25 | 1 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 25 | 2 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 25 | 3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 50 | 1 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 50 | 2 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 50 | 3 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |

## Tài nguyên và tổng hợp

| VU | CPU API TB/đỉnh % | RAM API đỉnh MiB | CPU DB TB/đỉnh % | RAM DB đỉnh MiB | CPU/RAM k6 |
|---|---|---|---|---|---|
| 10 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 25 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |
| 50 | Chưa đo | Chưa đo | Chưa đo | Chưa đo | Chưa đo |

Ghi RAM là private bytes, RSS, working set hay container usage. Tổng working set nhiều process không phải RAM riêng chính xác.

Báo median và min–max qua ba lượt cho req/s, p95/p99 và tài nguyên. Không lấy trung bình ba p95 rồi gọi là p95 của toàn bộ request.

## Nhận xét và bước cải tiến

Nêu điều nhìn thấy: mức tải nào tăng latency, API hay DB dùng tài nguyên cao, client có bị giới hạn không. Chưa profiling thì ghi giả thuyết, không kết luận nguyên nhân.

Đổi một nhóm yếu tố, chạy lại cùng máy/dataset. Ghi cả tốc độ, lỗi, tài nguyên và đánh đổi về độ đúng/độ phức tạp.

## Bằng chứng

Summary JSON, raw k6, resource CSV, environment đã bỏ secret và Kaggle notebook: **chưa có**.
