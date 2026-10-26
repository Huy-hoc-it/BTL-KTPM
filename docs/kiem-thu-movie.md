# Kiểm thử Movie

Lần kiểm chứng: 26/10/2026, Windows, Java 24.0.2 (compile release 21).

Đã chạy 73 test Movie: 30 unit test `MovieServiceTest` và 43 API test `MovieApiTest`; không failure, error hay skipped.

Lệnh kiểm chứng:
~~~
.\mvnw.cmd '-Dtest=MovieServiceTest,MovieApiTest' test
~~~

Đây không phải kết quả của toàn bộ test trong repo và không phải số liệu hiệu năng.

## Phạm vi

MovieServiceTest chạy business với repository giả. MovieApiTest khởi động web/security thật, dùng JWT có chữ ký và mock repository; không chứng minh JPA/database.

Các trường hợp:
- CRUD, Location, envelope data/meta, phân trang/lọc và thứ tự khi timestamp bằng nhau.
- Title/description rỗng, field bắt buộc, độ dài, duration và page/pageSize.
- PATCH giữ field không gửi; poster null xóa ảnh; null ở field bắt buộc/body rỗng bị từ chối.
- Input sai không ghi DB; PATCH nhiều field lỗi không cập nhật một phần.
- JSON/UUID/query/date sai, enum dạng số, số dạng chuỗi, field lạ.
- Release date chỉ nhận ngày yyyy-MM-dd hợp lệ; không nhận array, datetime hay chuỗi rỗng.
- Đọc và ghi Movie cần JWT; Admin mới được ghi; JWT sai bị từ chối; Customer nhận 403.
- 404 MOVIE_NOT_FOUND, 422 có details/requestId.
- Xóa mềm giữ hàng; contract Theater phản ánh duration mới và ẩn phim đã xóa.
- OpenAPI mô tả năm operation Movie và security theo cấu hình hiện tại.

## Báo cáo và chạy lại

Xem README để cấu hình Docker hoặc DB test riêng. Report Maven nằm trong `target/surefire-reports`.

Test không thay cho load test. Baseline hiệu năng vẫn là chưa đo.

## Kiểm tra k6

`smoke.js` và `movie-read.js` phục vụ kiểm tra chức năng và tải đọc Movie. Lượt chạy ngắn chỉ xác nhận script, không phải baseline.
