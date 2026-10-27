# Kịch bản k6 cho Movie

Các script gửi HTTP tới ứng dụng đang chạy; chúng không tự khởi động Spring Boot hay PostgreSQL. Cần k6 và Node.js 18 trở lên để seed dữ liệu.

| Script | Phạm vi |
|---|---|
| `smoke.js` | Tạo phim, đọc, sửa, kiểm tra validation và xóa mềm bằng API Movie |
| `movie-read.js` | Đọc danh sách và chi tiết phim theo tỉ lệ 50/50 |

## Chuẩn bị dữ liệu

Seed thêm phim vào DB và không xóa dữ liệu cũ. Dùng môi trường đo riêng nếu không muốn làm bẩn DB phát triển. Fixture cục bộ có JWT Admin để tạo/xóa phim nên không commit file `fixtures.local.json`; GET Movie trong cả hai kịch bản không gửi token.

~~~powershell
$env:BASE_URL = 'http://127.0.0.1:8080'
$env:ADMIN_USERNAME = 'admin'
$env:ADMIN_PASSWORD = '...'
$env:MOVIE_COUNT = '1000'
$env:DELETED_COUNT = '50'
node load-test/seed-movie.mjs
$env:FIXTURES = (Resolve-Path 'load-test/fixtures.local.json').Path
k6 run --include-system-env-vars load-test/smoke.js
k6 run --include-system-env-vars load-test/movie-read.js
~~~

Có thể dùng `ADMIN_TOKEN` thay cho username/password. Mặc định seed 1.000 phim, xóa mềm 50 phim đầu và lưu token Admin trong fixture. `movie-read.js` dùng tối đa 100 ID phim còn lại. Không seed lại giữa các lượt so sánh vì dữ liệu cũ vẫn ảnh hưởng DB.

Để thử nhanh script, giảm `MOVIE_COUNT`, đặt `DELETED_COUNT=0`, `WARMUP_SECONDS=0`, `DURATION_SECONDS=5`. Lượt ngắn chỉ kiểm tra luồng chạy, không phải baseline.

## Đo tải đọc

Mặc định kịch bản dùng 10 VU, warm-up 30 giây, đo 120 giây và nghỉ 100 ms giữa các request. Đây là tải closed-loop: khi API chậm, mỗi VU gửi ít request hơn. Warm-up và đo được tách thành hai phase.

~~~powershell
$env:GIT_COMMIT = git rev-parse HEAD
$env:VUS = '25'
$env:WARMUP_SECONDS = '30'
$env:DURATION_SECONDS = '120'
k6 run --include-system-env-vars --out json=load-test/results/movie-read-25vu-run1-raw.json load-test/movie-read.js
~~~

Chạy riêng 10, 25 và 50 VU, ít nhất ba lượt mỗi mức; giữ nguyên dataset, cấu hình JVM/pool và phần cứng. Tính throughput bằng số request trong phase đo chia thời lượng phase đó, không dùng `http_reqs.rate` của toàn lượt vì metric này còn tính setup và warm-up.

Threshold mặc định là lỗi kỹ thuật dưới 1%, không có status bất ngờ và p95 GET dưới 500 ms. Đây là ngưỡng tham khảo, không phải số liệu kết quả. Ghi lại mọi thay đổi ngưỡng trong báo cáo.

## Ghi nhận tài nguyên và báo cáo

Ghi CPU/RAM API, PostgreSQL và máy chạy k6 trong cùng khoảng thời gian. Với Docker, dùng số liệu container; với process Windows, ghi rõ CPU chuẩn hóa, private bytes hoặc working set. Working set cộng qua nhiều tiến trình PostgreSQL có thể đếm trùng bộ nhớ chia sẻ.

Chọn khoảng đo theo timestamp của metric `measured_requests` trong raw JSON, rồi lọc cùng khoảng đó trong CSV tài nguyên; không lấy mốc mở process cộng warm-up vì seed/setup cũng mất thời gian. Ghi commit, dataset, VU, warm-up, thời lượng, Java/PostgreSQL/k6, JVM flags và giới hạn CPU/RAM.

Điền [mẫu báo cáo Movie](../docs/bao-cao-hieu-nang-mau.md) sau khi chạy đủ lượt. Nếu chưa đo, để trạng thái là chưa đo; không tự điền số giả định.
