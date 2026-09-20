USE master;
GO

IF DB_ID(N'ProductDB') IS NULL
BEGIN
    CREATE DATABASE ProductDB
        COLLATE Vietnamese_100_CI_AS;
    PRINT N'Đã tạo database ProductDB.';
END
ELSE
    PRINT N'Database ProductDB đã tồn tại, giữ nguyên.';
GO

USE ProductDB;
GO

IF OBJECT_ID(N'dbo.Category_User', N'U') IS NOT NULL DROP TABLE dbo.Category_User;
IF OBJECT_ID(N'dbo.Product',       N'U') IS NOT NULL DROP TABLE dbo.Product;
IF OBJECT_ID(N'dbo.[User]',        N'U') IS NOT NULL DROP TABLE dbo.[User];
IF OBJECT_ID(N'dbo.Category',      N'U') IS NOT NULL DROP TABLE dbo.Category;
GO

CREATE TABLE dbo.Category (
    id      BIGINT        IDENTITY(1,1) NOT NULL,
    name    NVARCHAR(100) NOT NULL,
    images  NVARCHAR(500) NULL,
    CONSTRAINT PK_Category      PRIMARY KEY (id),
    CONSTRAINT UQ_Category_name UNIQUE (name)
);
GO

CREATE TABLE dbo.[User] (
    id       BIGINT        IDENTITY(1,1) NOT NULL,
    fullname NVARCHAR(100) NOT NULL,
    email    VARCHAR(150)  NOT NULL,
    password NVARCHAR(255) NOT NULL,
    phone    VARCHAR(20)   NULL,
    CONSTRAINT PK_User       PRIMARY KEY (id),
    CONSTRAINT UQ_User_email UNIQUE (email)
);
GO

CREATE TABLE dbo.Product (
    id         BIGINT         IDENTITY(1,1) NOT NULL,
    title      NVARCHAR(200)  NOT NULL,
    quantity   INT            NOT NULL CONSTRAINT DF_Product_quantity DEFAULT 0,
    [desc]     NVARCHAR(1000) NULL,
    price      DECIMAL(18,2)  NOT NULL,
    userid     BIGINT         NULL,
    categoryid BIGINT         NOT NULL,
    CONSTRAINT PK_Product          PRIMARY KEY (id),
    CONSTRAINT FK_Product_User     FOREIGN KEY (userid)
        REFERENCES dbo.[User] (id),
    CONSTRAINT FK_Product_Category FOREIGN KEY (categoryid)
        REFERENCES dbo.Category (id),
    CONSTRAINT CK_Product_price    CHECK (price > 0),
    CONSTRAINT CK_Product_quantity CHECK (quantity >= 0)
);
GO

CREATE TABLE dbo.Category_User (
    categoryid BIGINT NOT NULL,
    userid     BIGINT NOT NULL,
    CONSTRAINT PK_Category_User PRIMARY KEY (categoryid, userid),
    CONSTRAINT FK_CategoryUser_Category FOREIGN KEY (categoryid)
        REFERENCES dbo.Category (id) ON DELETE CASCADE,
    CONSTRAINT FK_CategoryUser_User FOREIGN KEY (userid)
        REFERENCES dbo.[User] (id) ON DELETE CASCADE
);
GO

CREATE INDEX IX_Product_categoryid     ON dbo.Product (categoryid);
CREATE INDEX IX_Product_userid         ON dbo.Product (userid);
CREATE INDEX IX_Product_price          ON dbo.Product (price);
CREATE INDEX IX_Category_User_userid   ON dbo.Category_User (userid);
GO

INSERT INTO dbo.Category (name, images) VALUES
    (N'Điện thoại',    N'images/categories/dien-thoai.jpg'),
    (N'Laptop',        N'images/categories/laptop.jpg'),
    (N'Phụ kiện',      N'images/categories/phu-kien.jpg'),
    (N'Máy tính bảng', N'images/categories/may-tinh-bang.jpg');
GO

INSERT INTO dbo.[User] (fullname, email, password, phone)
SELECT v.fullname, v.email, CONVERT(NVARCHAR(255), HASHBYTES('SHA2_256', N'123456'), 2), v.phone
FROM (VALUES
    (N'Nguyễn Văn An', 'an.nguyen@example.com',  '0901000001'),
    (N'Trần Thị Bình', 'binh.tran@example.com',  '0901000002'),
    (N'Lê Minh Cường', 'cuong.le@example.com',   NULL)
) AS v(fullname, email, phone);
GO

INSERT INTO dbo.Category_User (categoryid, userid)
SELECT c.id, u.id
FROM (VALUES
    (N'Điện thoại',    'an.nguyen@example.com'),
    (N'Điện thoại',    'binh.tran@example.com'),
    (N'Laptop',        'an.nguyen@example.com'),
    (N'Laptop',        'cuong.le@example.com'),
    (N'Phụ kiện',      'binh.tran@example.com'),
    (N'Phụ kiện',      'cuong.le@example.com'),
    (N'Máy tính bảng', 'an.nguyen@example.com'),
    (N'Máy tính bảng', 'binh.tran@example.com'),
    (N'Máy tính bảng', 'cuong.le@example.com')
) AS v(category_name, email)
JOIN dbo.Category c ON c.name  = v.category_name
JOIN dbo.[User]   u ON u.email = v.email;
GO

INSERT INTO dbo.Product (title, quantity, [desc], price, userid, categoryid)
SELECT v.title, v.quantity, v.[desc], v.price, u.id, c.id
FROM (VALUES
    (N'iPhone 15',                     25, N'Chip A16 Bionic, camera 48MP',         22990000, 'an.nguyen@example.com',  N'Điện thoại'),
    (N'Samsung Galaxy S24',            30, N'Snapdragon 8 Gen 3, màn 6.2 inch',     19990000, 'an.nguyen@example.com',  N'Điện thoại'),
    (N'Xiaomi Redmi Note 13',          80, N'Màn AMOLED 120Hz, pin 5000mAh',         5490000, 'binh.tran@example.com',  N'Điện thoại'),
    (N'MacBook Air M2',                15, N'Chip M2, RAM 8GB, SSD 256GB',          26990000, 'an.nguyen@example.com',  N'Laptop'),
    (N'Dell XPS 13',                   10, N'Intel Core i7, RAM 16GB, SSD 512GB',   32990000, 'cuong.le@example.com',   N'Laptop'),
    (N'Asus Vivobook 15',              20, N'Intel Core i5, RAM 8GB, SSD 512GB',    12990000, 'cuong.le@example.com',   N'Laptop'),
    (N'Tai nghe AirPods Pro 2',        40, N'Chống ồn chủ động, cổng USB-C',         5990000, 'binh.tran@example.com',  N'Phụ kiện'),
    (N'Chuột Logitech MX Master 3S',   50, N'Chuột không dây công thái học',         2490000, 'cuong.le@example.com',   N'Phụ kiện'),
    (N'Sạc nhanh Anker 65W',          100, N'GaN, 3 cổng, sạc được laptop',           890000, 'binh.tran@example.com',  N'Phụ kiện'),
    (N'Bàn phím cơ Keychron K2',       35, N'Bluetooth, hot-swap, layout 75%',       1890000, 'cuong.le@example.com',   N'Phụ kiện'),
    (N'iPad Air 5',                    18, N'Chip M1, màn 10.9 inch',               14990000, 'an.nguyen@example.com',  N'Máy tính bảng'),
    (N'Samsung Galaxy Tab S9',         12, N'Màn Dynamic AMOLED 2X, kèm S Pen',     17990000, 'binh.tran@example.com',  N'Máy tính bảng')
) AS v(title, quantity, [desc], price, email, category_name)
JOIN dbo.Category c ON c.name  = v.category_name
JOIN dbo.[User]   u ON u.email = v.email;
GO

SELECT c.id, c.name, COUNT(p.id) AS product_count
FROM dbo.Category c
LEFT JOIN dbo.Product p ON p.categoryid = c.id
GROUP BY c.id, c.name
ORDER BY c.id;

SELECT u.id, u.fullname, u.email, COUNT(DISTINCT cu.categoryid) AS category_count, COUNT(DISTINCT p.id) AS product_count
FROM dbo.[User] u
LEFT JOIN dbo.Category_User cu ON cu.userid = u.id
LEFT JOIN dbo.Product p        ON p.userid  = u.id
GROUP BY u.id, u.fullname, u.email
ORDER BY u.id;

SELECT p.id, p.title, p.price, p.quantity, c.name AS category, u.fullname AS owner
FROM dbo.Product p
JOIN dbo.Category c ON c.id = p.categoryid
JOIN dbo.[User]   u ON u.id = p.userid
ORDER BY p.price ASC;
GO
