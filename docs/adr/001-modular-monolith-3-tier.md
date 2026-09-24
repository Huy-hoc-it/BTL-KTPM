# ADR-001: Modular Monolith với kiến trúc 3-tier

- Trạng thái: Accepted
- Ngày: 2026-09-22

## Bối cảnh

Dự án Pha 1 do bốn thành viên thực hiện trong 5–6 tuần và đề bài yêu cầu luồng rõ ràng `API → Nghiệp vụ → Truy cập dữ liệu`. Cấu trúc Clean Architecture ban đầu tạo nhiều package `port`, `adapter`, `command`, `query` hơn mức cần thiết cho phạm vi này.

## Quyết định

Giữ Modular Monolith theo các module `identity`, `movie`, `theater`, `booking`. Bên trong mỗi module dùng ba tầng:

```text
api -> business -> data
```

- `api`: controller, request/response DTO và validation.
- `business`: service, business model/rule và repository interface.
- `data`: JPA entity, Spring Data repository và repository implementation.

Tầng `business` không phụ thuộc Spring Web, JPA, Spring Data hoặc driver database. Module khác chỉ gọi public business interface/service, không truy cập `api` hoặc `data` của nhau.

## Hệ quả

- Ít package và lớp trung gian hơn, dễ chia mỗi module cho một thành viên.
- Vẫn đáp ứng yêu cầu tách nghiệp vụ khỏi web và database.
- JPA entity phải tách khỏi business model và cần mapping ở tầng `data`.
- Chỉ bổ sung seam hoặc abstraction mới khi có nhu cầu thực tế.
