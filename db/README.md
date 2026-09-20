# Database script (SQL Server)

Chỉ có **một** file: [`setup_database.sql`](setup_database.sql). Nó tạo database `ProductDB`, các bảng `Category`, `User`, `Category_User`, `Product` và chèn dữ liệu mẫu (4 category, 3 user, 9 liên kết category–user, 12 product). Spring Boot không tự tạo bảng (`ddl-auto=validate`), nên phải chạy script này trước khi chạy app.

> Chạy lại được nhiều lần, nhưng mỗi lần sẽ **xoá và tạo lại** toàn bộ các bảng trên (mất dữ liệu cũ). Database cũ có bảng `Product`/`Category` theo schema trước đây cũng sẽ bị thay thế.

## Cách 1: SSMS

1. Mở SSMS, kết nối tới server (ví dụ `localhost`).
2. `File > Open > File...`, chọn `db/setup_database.sql`.
3. Bấm **Execute** (F5). Cuối script có 3 câu SELECT hiển thị kết quả kiểm tra.

## Cách 2: sqlcmd

Chạy từ thư mục gốc project. `-f 65001` để đọc file UTF-8 (tiếng Việt), `-C` để tin cậy chứng chỉ tự ký (bắt buộc với ODBC Driver 18).

```bat
:: Windows Authentication
sqlcmd -S localhost -E -C -f 65001 -i db\setup_database.sql

:: SQL Authentication
sqlcmd -S localhost -U sa -P <password> -C -f 65001 -i db\setup_database.sql
```

## Sơ đồ bảng

```
Category                                   [User]
  id BIGINT PK IDENTITY                      id BIGINT PK IDENTITY
  name NVARCHAR(100) UNIQUE                  fullname NVARCHAR(100)
  images NVARCHAR(500) NULL                  email VARCHAR(150) UNIQUE
     |  1                    N  |            password NVARCHAR(255)   (lưu hash)
     |                          |            phone VARCHAR(20) NULL
     |     Category_User        |               |  1
     +---- categoryid FK ------ + ---- userid FK+
           PK (categoryid, userid)              |
           (quan hệ N-N)                        |
     |  1                                       |  1
     N                                          N
Product
  id BIGINT PK IDENTITY
  title NVARCHAR(200)
  quantity INT (>= 0, mặc định 0)
  [desc] NVARCHAR(1000) NULL
  price DECIMAL(18,2) (> 0)
  userid     BIGINT NULL FK -> [User].id
  categoryid BIGINT FK -> Category.id
```

| Quan hệ | Cài đặt |
|---|---|
| Category 1 – N Product | `Product.categoryid` → `Category.id` (NO ACTION: không xoá được Category còn Product) |
| User 1 – N Product | `Product.userid` → `[User].id`, **cho phép NULL** vì User chưa được dùng (NO ACTION: không xoá được User còn Product) |
| Category N – N User | bảng `Category_User`, khoá chính kép, xoá Category/User thì tự xoá dòng liên kết (`ON DELETE CASCADE`) |

Lưu ý:
- `User` và `desc` là từ khoá của T-SQL nên khi viết SQL phải dùng `[User]` và `[desc]`.
- Mật khẩu mẫu của 3 user là `123456`, lưu bằng SHA-256 chỉ để demo. Khi làm đăng nhập thật nên dùng BCrypt.
