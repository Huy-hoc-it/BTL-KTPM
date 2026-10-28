# Đối chiếu Pha 1 và chạy kiểm thử trên Kaggle CPU

Đối chiếu mã nguồn nhánh `dev`, commit `36c8922`, ngày 29/10/2026. Hướng dẫn này chưa được thực thi trong phiên Kaggle; không phải báo cáo kết quả đo.

## 1. Dự án đạt yêu cầu nào?

| Yêu cầu đề bài | Đánh giá từ mã nguồn | Bằng chứng |
|---|---|---|
| Một nghiệp vụ backend | Đạt | Rạp phim: Identity, Movie, Theater, Booking |
| REST, JSON | Có REST và JSON cho kết quả thành công; cần chuẩn hóa lỗi Booking | `BookingController`, `BookingExceptionHandler` |
| POST, GET, DELETE | Đạt về triển khai | POST/GET `/api/v1/bookings`, DELETE `/api/v1/bookings/{bookingId}` |
| OpenAPI/Swagger | Đạt về cấu hình và tài liệu | `OpenApiConfig`, springdoc trong `pom.xml`, `docs/openapi.json` |
| API → nghiệp vụ → dữ liệu | Đạt về cấu trúc | Các package `api`, `business`, `data`; có `ArchitectureTest` |
| Business không import web/DB | Chưa thấy import web/JPA/JDBC/Spring Data trong business | Repository interface ở business, adapter JPA ở data. Spring DI và `@Transactional` vẫn có; đề bài không cấm toàn bộ Spring |
| Repository/DAL, ORM/data mapper | Đạt về triển khai | Spring Data JPA, entity và repository adapter |
| Đăng nhập | Đạt về triển khai | POST `/api/v1/auth/login`, BCrypt, JWT |
| GET và POST cần xác thực | Đạt về cấu hình | GET `/api/v1/users/me`, GET/POST `/api/v1/bookings` |
| Xác thực qua filter | Đạt về cấu hình | `SecurityConfig` đăng ký `JwtAuthenticationFilter` |
| Đóng gói Docker | Có hỗ trợ, cần xác nhận build/run | Dockerfile hai giai đoạn, Compose API + PostgreSQL, CI có docker build |
| GitHub công khai, commit cẩn thận | Có remote và lịch sử commit; chưa xác nhận public | Remote `https://github.com/Huy-hoc-it/BTL-KTPM.git`; mở bằng cửa sổ chưa đăng nhập để xác nhận |
| README kiến trúc và đặc tả | Đạt | `README.md` mô tả chức năng, tầng, cách chạy và liên kết SPEC/OpenAPI |
| Kiểm thử tải trên Kaggle CPU | Chưa có bằng chứng trong bản đang kiểm tra | Báo cáo `bao-cao-hieu-nang-mau.md` ghi Windows/Docker; Git chỉ theo dõi `.gitkeep` trong results |

**Kết luận:** đã có phần lớn yêu cầu triển khai Pha 1, chưa đủ cơ sở kết luận hoàn tất toàn bộ. Cần chạy trên Kaggle CPU và lưu bằng chứng; xác nhận GitHub public; chuẩn hóa phản hồi lỗi Booking để tuân thủ JSON nhất quán. Việc có file cấu hình Docker không tự chứng minh image build/chạy thành công.

Kiểm chứng tại máy local trong lần đối chiếu: **70 test chạy đạt, 0 lỗi, 0 bỏ qua**, gồm `ArchitectureTest`, `JwtTokenServiceTest`, `IdentityServiceTest`, `MovieServiceTest`, `BookingServiceTest`, `AuditoriumServiceTest`, `ShowtimeServiceTest`. Mockito được nạp bằng Java agent để tránh lỗi tự attach của môi trường Windows hiện tại. Đây là tập test được chọn, chưa phải toàn bộ `mvn verify`, chưa bao gồm kiểm chứng HTTP/PostgreSQL bằng Testcontainers hoặc Docker image.

### Những điểm cần biết trước khi đo

1. `BookingExceptionHandler` trả `ResponseEntity<String>` cho lỗi 400/404, không dùng JSON error envelope chung. Cần sửa nếu yêu cầu JSON áp dụng cả phản hồi lỗi.
2. `contention.js` tìm `body.data.bookingId`, nhưng `BookingController` trả `BookingDetailResponse` trực tiếp với `bookingId` ở ngoài cùng. Winner hợp lệ sẽ bị đánh giá sai. Khi sửa, có thể dùng helper `data(response)` đang có sẵn.
3. Ghế đã được đặt đang ném `IllegalArgumentException` và trả 400 dạng chuỗi; contention đòi 409 JSON với `error.code=SEAT_ALREADY_BOOKED`. Không hạ kỳ vọng test để che lỗi contract này.
4. `verify-contention.sql` dùng `released_at` và `bookings.status`, trong khi persistence hiện tại xóa booking/ghế khi hủy. Đối chiếu migration thực tế trước khi dùng SQL này.
5. Stub trong `load-test/tests/client-check.mjs` trả POST booking dưới dạng `{data: ...}`; vì vậy kiểm tra stub hiện tại có thể bỏ lọt lỗi khác biệt body của API thật.

Movie Read và workload Booking thông thường có thể đo trước. Contention là bài kiểm tra bổ sung về tính đúng đắn; cần sửa các điểm trên trước khi công bố nó đạt.

## 2. Tạo Notebook Kaggle

Tạo Notebook Python tại Kaggle, đặt **Accelerator = None** để dùng CPU và **Internet = On** để tải mã nguồn/công cụ. Nếu tài khoản chưa cho bật Internet, hoàn tất điều kiện mà Kaggle hiển thị. Không cần GPU.

Chạy API, PostgreSQL và k6 trong cùng Notebook, gọi `127.0.0.1`. Ghi rõ ba tiến trình chia sẻ tài nguyên CPU; kết quả phù hợp baseline học phần nhưng không suy ra công suất production.

Các ô dưới đây chạy lần lượt. `%%bash` là ô Bash; những ô còn lại là Python. Biến `export` trong một ô Bash không tồn tại ở ô sau, nên cấu hình dùng `os.environ` trong Python.

### Ô 1 — Lấy đúng phiên bản

```python
import os, pathlib, subprocess, json, time, urllib.request

repo = pathlib.Path('/kaggle/working/BTL-KTPM')
if not repo.exists():
    subprocess.run(['git', 'clone', '--branch', 'dev',
                    'https://github.com/Huy-hoc-it/BTL-KTPM.git', str(repo)], check=True)
os.chdir(repo)
# Để tái lập đúng bản đã đối chiếu; đổi SHA nếu nhóm đã sửa và push phiên bản mới.
subprocess.run(['git', 'checkout', '36c8922'], check=True)
subprocess.run(['git', 'rev-parse', 'HEAD'], check=True)
subprocess.run(['git', 'status', '--short'], check=True)
```

Nếu nhóm sửa code/script, commit và push trước, rồi dùng SHA mới. Runner không chấp nhận thay đổi chưa commit trong `src/main`, `pom.xml`, `load-test`. Không tạo commit giả chỉ để bỏ qua lỗi này.

### Ô 2 — Cài công cụ

```bash
%%bash
set -euo pipefail
sudo apt-get update -qq
sudo apt-get install -y openjdk-21-jdk-headless postgresql postgresql-contrib nodejs curl ca-certificates gnupg
curl -fsSL https://dl.k6.io/key.gpg | sudo gpg --batch --yes --dearmor -o /usr/share/keyrings/k6-archive-keyring.gpg
echo 'deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main' | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update -qq
sudo apt-get install -y k6
```

Ô này dùng gói PostgreSQL từ Ubuntu của phiên Kaggle; không mặc định nó là PostgreSQL 17. Ghi phiên bản thực tế và giữ cùng phiên bản khi so sánh Pha 1/Pha 2. Nếu image không có gói Java 21, cài JDK 21 từ Adoptium rồi đặt `JAVA_HOME`/`PATH` đúng bản trước ô tiếp theo.

### Ô 3 — Chọn Java, kiểm tra phiên bản

```python
import glob
jdk = sorted(glob.glob('/usr/lib/jvm/java-21-openjdk-*'))
assert jdk, 'Chưa cài được JDK 21'
os.environ['JAVA_HOME'] = jdk[0]
os.environ['PATH'] = jdk[0] + '/bin:' + os.environ['PATH']
subprocess.run(['java', '-version'], check=True)
subprocess.run(['k6', 'version'], check=True)
node_major = int(subprocess.check_output(['node', '-p', 'process.versions.node.split(".")[0]'], text=True))
assert node_major >= 18, 'Seed script cần Node.js >= 18 (fetch API)'
```

### Ô 4 — Khởi động PostgreSQL và tạo database đo riêng

```python
clusters = subprocess.check_output(['pg_lsclusters', '--no-header'], text=True).splitlines()
assert clusters, 'Gói PostgreSQL chưa tạo cluster; kiểm tra quá trình cài đặt'
cluster = clusters[-1].split()
pg_version, pg_name, pg_port = cluster[:3]
subprocess.run(['sudo', 'pg_ctlcluster', '--skip-systemctl-redirect',
                pg_version, pg_name, 'start'], check=False)

def pg(sql):
    return subprocess.check_output(['sudo', '-u', 'postgres', 'psql', '-p', pg_port,
                                    '-v', 'ON_ERROR_STOP=1', '-tAc', sql], text=True).strip()

pg("""DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='cinema_load') THEN
        CREATE ROLE cinema_load LOGIN PASSWORD 'cinema_load_pw';
    END IF;
END $$;""")
if pg("SELECT 1 FROM pg_database WHERE datname='cinema_load'") != '1':
    subprocess.run(['sudo', '-u', 'postgres', 'createdb', '-p', pg_port,
                    '-O', 'cinema_load', 'cinema_load'], check=True)
data_dir = pg('SHOW data_directory')
database_pid = int(subprocess.check_output(['sudo', 'head', '-n', '1', data_dir + '/postmaster.pid'], text=True))
print('PostgreSQL:', pg('SELECT version()'))
print('Database PID:', database_pid, 'port:', pg_port)
```

Không tạo lại database hay seed lại giữa các lượt đo cùng baseline. Một phiên Kaggle mới với database mới sẽ dễ tái lập hơn phiên đã chạy nhiều lần.

### Ô 5 — Build và chạy API nền

```python
os.environ.update({
    'SPRING_PROFILES_ACTIVE': 'local',
    'DB_URL': f'jdbc:postgresql://127.0.0.1:{pg_port}/cinema_load',
    'DB_USERNAME': 'cinema_load', 'DB_PASSWORD': 'cinema_load_pw',
    'JWT_SECRET': 'kaggle-local-only-cinema-benchmark-secret-2026-please-change',
    'DEMO_ADMIN_USERNAME': 'admin', 'DEMO_ADMIN_PASSWORD': 'demo-pass-1234',
    'JWT_EXPIRATION_SECONDS': '3600',
    'SERVER_ADDRESS': '127.0.0.1', 'SERVER_PORT': '8080',
    'BASE_URL': 'http://127.0.0.1:8080',
    'ADMIN_USERNAME': 'admin', 'ADMIN_PASSWORD': 'demo-pass-1234',
})
subprocess.run(['bash', './mvnw', '-B', '-DskipTests', 'package'], check=True)
jars = list((repo / 'target').glob('*.jar'))
assert len(jars) == 1, jars
api_log = open('/kaggle/working/cinema-api.log', 'w')
api = subprocess.Popen(['java', '-Xms256m', '-Xmx768m', '-jar', str(jars[0])],
                       stdout=api_log, stderr=subprocess.STDOUT, env=os.environ.copy())
os.environ['API_PID'] = str(api.pid)
os.environ['DATABASE_PID'] = str(database_pid)
ready = False
for _ in range(120):
    if api.poll() is not None:
        break
    try:
        with urllib.request.urlopen(os.environ['BASE_URL'] + '/actuator/health/readiness', timeout=2) as r:
            ready = r.status == 200
        if ready:
            break
    except Exception:
        pass
    time.sleep(1)
if not ready:
    print(pathlib.Path('/kaggle/working/cinema-api.log').read_text()[-12000:])
    raise RuntimeError('API chưa sẵn sàng; xử lý lỗi trước khi đo')
print('API ready, PID:', api.pid)
```

`-DskipTests` ở bước build không có nghĩa bộ test đã đạt. Bộ integration test của dự án dùng Testcontainers; chạy `mvn verify` trên môi trường có Docker riêng. Tránh khởi động API lần hai nếu lần trước vẫn chiếm cổng 8080.

### Ô 6 — Xác minh xác thực, seed và smoke

```python
import urllib.error
for method, route in [('GET', '/api/v1/users/me'), ('POST', '/api/v1/bookings')]:
    request = urllib.request.Request(os.environ['BASE_URL'] + route,
        data=b'{}' if method == 'POST' else None, method=method,
        headers={'Content-Type': 'application/json'})
    try:
        urllib.request.urlopen(request)
        raise AssertionError(f'{method} {route} cho truy cập khi thiếu token')
    except urllib.error.HTTPError as e:
        assert e.code == 401, (method, route, e.code)
        print(method, route, '→ 401 OK')

os.environ.update({'MOVIE_COUNT': '1000', 'DELETED_COUNT': '50',
                   'SEED_SCOPE': 'all', 'VUS': '50',
                   'FIXTURES': str(repo / 'load-test/fixtures.local.json')})
subprocess.run(['node', 'load-test/seed.mjs'], check=True)
subprocess.run(['k6', 'run', '--include-system-env-vars', 'load-test/smoke.js'], check=True)
```

Seed tạo 1.000 phim (50 xóa mềm), 1 phòng với **109 ghế**, 2 suất chiếu, 100 user tải và 50 contender user. Hai nhóm user/ghế tách warm-up và đo. Fixture chứa JWT; không công khai file này. Seed chỉ chạy một lần cho baseline, không phải mỗi mức VU.

### Ô 7 — Chạy baseline

```python
for scenario in ['movie-read', 'booking']:
    subprocess.run(['python', 'load-test/run-baseline.py', '--scenario', scenario,
                    '--api-pid', str(api.pid), '--database-pid', str(database_pid)], check=True)
```

Mỗi kịch bản chạy **10/25/50 người dùng ảo**, **3 lượt/mức**, **warm-up 30 giây + đo 120 giây/lượt**. Hai kịch bản cần tối thiểu khoảng **45 phút**, cộng setup/cleanup và build/seed. Token mặc định có hạn 1 giờ; nếu gần hết hạn, chạy `node load-test/refresh-tokens.mjs` trước một kịch bản, không chạy khi k6 đang đo.

Ngưỡng mặc định của script: lỗi kỹ thuật <1%, lỗi nghiệp vụ <1%, không có HTTP status ngoài kỳ vọng, p95 đọc <500ms, p95 POST booking <1000ms. Đây là ngưỡng dự án chọn, không phải ngưỡng bắt buộc trong đề bài đã cung cấp.

Khi k6 vượt ngưỡng, runner dừng và giữ kết quả/log đã tạo. Đó là kết quả test thất bại cần báo cáo, không phải lý do giảm mức tải hoặc nới ngưỡng để làm đẹp kết quả. Xem log để phân biệt chậm do tải, token hết hạn hay lỗi API.

Chỉ chạy contention sau khi sửa và commit các điểm ở mục 1, dùng fixture mới nếu ghế contention đã bị giữ. Khi đó:

```python
# Chưa chạy với commit 36c8922 vì các lỗi contract đã nêu.
# subprocess.run(['python', 'load-test/run-baseline.py', '--scenario', 'contention',
#                 '--api-pid', str(api.pid), '--database-pid', str(database_pid)], check=True)
```

Kỳ vọng mỗi lượt tranh ghế: đúng 1 HTTP 201, N−1 HTTP 409 đúng error code và DB chỉ có 1 booking/ghế đó. Đây là batch một request/VU, không phải tải duy trì 120 giây.

### Ô 8 — Tổng hợp số liệu và tải bằng chứng

```python
import csv, shutil
rows = []
for folder in sorted((repo / 'load-test/results').iterdir()):
    if not folder.is_dir():
        continue
    for f in sorted(folder.glob('*.json')):
        if f.name.endswith('-raw.json') or f.name == 'environment.json':
            continue
        result = json.loads(f.read_text())
        if 'metadata' not in result or 'summary' not in result:
            continue
        meta = result['metadata']
        metrics = result['summary']['metrics']
        values = lambda key: metrics.get(key, {}).get('values', {})
        rows.append({
            'run': folder.name + '/' + f.stem,
            'scenario': meta['script'], 'vus': meta['vus'],
            'req_per_s': meta.get('measuredThroughput'),
            'read_p50_ms': values('measured_read_latency').get('med'),
            'read_p95_ms': values('measured_read_latency').get('p(95)'),
            'read_p99_ms': values('measured_read_latency').get('p(99)'),
            'booking_p95_ms': values('measured_booking_latency').get('p(95)'),
            'technical_error_rate': values('measured_technical_error').get('rate'),
            'business_error_rate': values('measured_business_error').get('rate'),
        })
if rows:
    with open('/kaggle/working/kaggle-summary.csv', 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)
    for row in rows:
        print(row)

# Chỉ đóng gói kết quả, không đóng gói fixtures.local.json hay cấu hình mật khẩu.
archive = shutil.make_archive('/kaggle/working/cinema-kaggle-results', 'zip',
                              root_dir=str(repo / 'load-test'), base_dir='results')
print('Tải tại Output:', archive)
```

Lưu phiên bản Notebook kèm output; tải ZIP và CSV trong `/kaggle/working`. Báo cáo cần chứa SHA commit, phiên bản Java/DB/k6, CPU/vCPU/quota/RAM, JVM heap `256m/768m`, Hikari mặc định, dataset, thời lượng, think time 100ms và vị trí ba tiến trình.

Mỗi thư mục kết quả có `environment.json`, summary JSON, raw k6 JSON, log và CSV tài nguyên. Tính throughput bằng `measured_requests / 120`; dùng `measured_read_latency` và `measured_booking_latency` để loại warm-up, setup và cleanup. Trong summary, `med` là p50. Không lấy `http_reqs` toàn bài chia 120 vì có cả request phụ.

CSV tài nguyên lấy mẫu toàn phiên test. Khi báo cáo CPU/RSS riêng pha đo, xác định khoảng thời gian từ raw JSON có tag `phase=measure`, rồi lọc CSV theo timestamp. `cpuPercentMachine` được chuẩn hóa theo `os.cpu_count()`, không theo quota cgroup; xem quota trong metadata. RSS cộng các tiến trình PostgreSQL có thể đếm trùng vùng nhớ dùng chung. Kiểm tra `available=True` cho API/DB để tránh báo nhầm tài nguyên 0 khi truyền sai PID.

Tổng hợp ba lượt bằng median của từng chỉ số và ghi khoảng min–max. Median của ba p95 là số tổng hợp giữa các lượt, không phải p95 gộp của tất cả request. Giữ raw data để người chấm có thể kiểm chứng. Báo cáo Windows cũ cần được giữ nhãn môi trường Windows, tạo báo cáo Kaggle riêng.

## Nguồn tham khảo

- Kaggle Notebook: https://www.kaggle.com/docs/notebooks
- Cài k6: https://grafana.com/docs/k6/latest/set-up/install-k6/
- Threshold và mã thoát test: https://grafana.com/docs/k6/latest/using-k6/thresholds/
