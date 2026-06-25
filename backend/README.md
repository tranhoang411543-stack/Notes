# FINI Todo Backend

Backend local cho app Todo trên Android phone va Android Auto/car emulator.

## Chay local

```bash
mvn spring-boot:run
```

Mặc định backend kết nối tới cơ sở dữ liệu PostgreSQL cục bộ tại `jdbc:postgresql://localhost:5432/todo_app` (cấu hình trong `src/main/resources/application.properties`).

- Localhost trên máy tính: `http://localhost:8080`
- Android emulator gọi backend trên máy tính: `http://10.0.2.2:8080`
- Health check: `GET /api/health`

Nếu cần ghi đè cấu hình kết nối database, bạn có thể thiết lập biến môi trường hoặc chỉnh sửa file `config/application-local.properties` khi chạy với profile `local`.

## Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/verify-otp`
- `POST /api/auth/reset-password`

Sau login/register, gui token cho cac API con lai:

```http
Authorization: Bearer <accessToken>
```

## Categories

- `GET /api/categories`
- `GET /api/categories/{id}`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`

Payload:

```json
{
  "name": "Work",
  "color": "#4285F4"
}
```

## Tasks

- `GET /api/tasks?dateFilter=ALL|TODAY|THIS_WEEK&categoryId=<uuid>&keyword=<text>`
- `GET /api/tasks/{id}`
- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `DELETE /api/tasks/{id}`
- `PATCH /api/tasks/{id}/complete?completed=true|false`

Payload task:

```json
{
  "categoryId": "uuid-or-null",
  "title": "Team meeting",
  "note": "Discuss sync",
  "startAt": "2026-06-06T09:00:00",
  "dueAt": "2026-06-06T10:00:00",
  "reminderEnabled": true,
  "reminderBeforeMinutes": 15,
  "repeatType": "WEEKLY",
  "repeatDays": "MON,WED",
  "hasLocation": true,
  "latitude": 10.762622,
  "longitude": 106.660172,
  "locationName": "Office",
  "address": "Ho Chi Minh City"
}
```

Rules:

- `reminderBeforeMinutes` chi nhan `5, 10, 15, 20, 25, 30`.
- Neu bat reminder thi phai co `dueAt`.
- `repeatType`: `NONE`, `DAILY`, `WEEKLY`.
- `repeatDays` chi dung khi `repeatType=WEEKLY`; chap nhan `1-7`, `MON-SUN`, hoac `MONDAY-SUNDAY`.
- Neu `hasLocation=false`, backend se xoa toa do/location trong task.

## Sync Phone/Car

`POST /api/sync`

```json
{
  "deviceId": "uuid-or-null",
  "deviceType": "PHONE",
  "deviceName": "Pixel Emulator",
  "fcmToken": "firebase-token",
  "lastSyncAt": "2026-06-06T10:00:00",
  "changedCategories": [],
  "changedTasks": []
}
```

`deviceType` chi nhan `PHONE` hoac `CAR`. Response tra ve `deviceId`, `serverTime`, `categories`, va `tasks` da thay doi sau `lastSyncAt`.

## Test API

```bash
mvn test
```

Test integration hien cover auth/reset password, category CRUD, task CRUD/filter/search/reminder/repeat/location, va sync phone/car.
