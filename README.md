# bt_08 – Quản lý Product & Category (Spring Boot + GraphQL + SQL Server)

Backend GraphQL (Spring Boot 4, Java 21) quản lý **Product** và **Category**, database SQL Server,
giao diện HTML thuần + `fetch()` gọi `/graphql`.

## Chạy dự án

### 1. Tạo database (chạy trước)

Chạy [`db/setup_database.sql`](db/setup_database.sql) một lần trong SSMS (hoặc `sqlcmd`, xem [db/README.md](db/README.md)).
Script tạo database `ProductDB`, các bảng `Category`, `User`, `Category_User`, `Product` và dữ liệu mẫu (4 category, 3 user, 12 product).
Bảng **không** do Hibernate sinh ra (`ddl-auto=validate`); script SQL là nguồn chân lý của schema.
`User` (và quan hệ N–N với Category) hiện chỉ có trong DB và entity, chưa có API/giao diện; vì vậy `Product.userid` cho phép NULL.

### 2. Cấu hình connection string

Sửa [`src/main/resources/application.properties`](src/main/resources/application.properties):

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=ProductDB;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=<mật khẩu của bạn>
```

Yêu cầu: SQL Server bật TCP/IP ở cổng 1433 và cho phép đăng nhập SQL Authentication.
Không nên commit mật khẩu thật; có thể dùng biến môi trường, ví dụ `spring.datasource.password=${DB_PASSWORD}`.

### 3. Chạy Spring Boot

```bat
mvnw spring-boot:run
```

(hoặc chạy `Bt08Application` trong IDE). Ứng dụng chạy ở <http://localhost:8080>.

### 4. Mở giao diện để test

| URL | Mô tả |
|---|---|
| <http://localhost:8080/> | Giao diện HTML (bảng product/category, lọc theo danh mục, thêm/sửa/xoá) |
| <http://localhost:8080/graphiql> | GraphiQL để thử query/mutation |
| `POST http://localhost:8080/graphql` | Endpoint GraphQL (dùng cho Postman) |

Query/mutation mẫu và bảng các lỗi: [docs/graphql-samples.md](docs/graphql-samples.md).

### Chạy test

```bat
mvnw test
```

Test tích hợp (`GraphQLApiTests`, `EntityMappingTests`) chạy trên SQL Server thật nên cần làm bước 1–2 trước; mỗi test tự rollback nên không thay đổi dữ liệu mẫu.

## Cấu trúc

```
db/                               script SQL Server + hướng dẫn
docs/graphql-samples.md           query/mutation mẫu
src/main/java/com/example/bt_08/
  entity/       Category, User, Product (map đúng bảng trong db/; Category N-N User qua bảng Category_User)
  repository/   Spring Data JPA
  service/      nghiệp vụ, trả DTO
  dto/          ProductInput, CategoryInput (kèm validation), ProductDto, CategoryDto
  controller/graphql/   @QueryMapping / @MutationMapping / @BatchMapping
  exception/    ResourceNotFoundException, BusinessException, GraphQLExceptionAdvice
src/main/resources/
  graphql/schema.graphqls
  static/       index.html, app.js, style.css
```

## Danh sách GraphQL API

Schema đầy đủ: [schema.graphqls](src/main/resources/graphql/schema.graphqls).

### Query

| Query | Mô tả |
|---|---|
| `products: [Product!]!` | Tất cả product, **giá tăng dần** |
| `productsByCategory(categoryId: ID!): [Product!]!` | Product của 1 category (theo id), giá tăng dần |
| `productsByCategoryName(categoryName: String!): [Product!]!` | Như trên, theo tên category (không phân biệt hoa/thường) |
| `product(id: ID!): Product` | 1 product, `null` nếu không có |
| `categories: [Category!]!` | Tất cả category |
| `category(id: ID!): Category` | 1 category, `null` nếu không có |

### Mutation

| Mutation | Mô tả |
|---|---|
| `createProduct(input: ProductInput!): Product!` | Thêm product |
| `updateProduct(id: ID!, input: ProductInput!): Product!` | Sửa product |
| `deleteProduct(id: ID!): Boolean!` | Xoá product |
| `createCategory(input: CategoryInput!): Category!` | Thêm category (tên không được trùng) |
| `updateCategory(id: ID!, input: CategoryInput!): Category!` | Sửa category |
| `deleteCategory(id: ID!): Boolean!` | Xoá category (lỗi nếu còn product) |

### Input

```graphql
input ProductInput  { title: String!, price: Float!, quantity: Int!, desc: String, categoryId: ID! }
input CategoryInput { name: String!, images: String }
```

Quy tắc validate: `title` không rỗng (≤ 200 ký tự), `name` category không rỗng (≤ 100), giá > 0 (tối đa 2 chữ số thập phân), số lượng ≥ 0, `categoryId` phải tồn tại.

### Lỗi

GraphQL luôn trả HTTP 200, lỗi nằm ở mảng `errors`:
`extensions.classification` là `BAD_REQUEST` (validate, trùng tên, xoá category còn product), `NOT_FOUND` (id không tồn tại)
hoặc `ValidationError` (query sai schema). Lỗi validate có thêm `extensions.fieldErrors` theo từng field.
