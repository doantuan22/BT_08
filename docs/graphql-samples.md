# Query / Mutation mẫu

Endpoint: `POST http://localhost:8080/graphql` (header `Content-Type: application/json`).
GraphiQL: <http://localhost:8080/graphiql>. Dán phần query vào ô bên trái, phần *Variables* vào ô Variables.

Postman: body raw JSON dạng `{"query": "...", "variables": {...}}`.

> `price` trả về dạng số thực, có thể hiển thị `1.299E7` (= 12.990.000), JavaScript đọc bình thường.

---

## 1. Tất cả product, giá tăng dần

```graphql
{
  products {
    id title price quantity desc
    category { id name }
  }
}
```

## 2. Product theo category

Theo id:
```graphql
{ productsByCategory(categoryId: 3) { id title price category { name } } }
```
Theo tên (không phân biệt hoa/thường):
```graphql
{ productsByCategoryName(categoryName: "laptop") { id title price } }
```

## 3. Category

```graphql
{ categories { id name images } }
```
```graphql
{ category(id: 1) { id name } product(id: 1) { id title category { name } } }
```
(`category(id)` và `product(id)` trả `null` nếu không tồn tại.)

## 4. Tạo / sửa / xoá Category

```graphql
mutation($input: CategoryInput!) { createCategory(input: $input) { id name images } }
```
Variables: `{ "input": { "name": "Đồng hồ", "images": "images/categories/dong-ho.jpg" } }`

```graphql
mutation($input: CategoryInput!) { updateCategory(id: 5, input: $input) { id name images } }
```
```graphql
mutation { deleteCategory(id: 5) }
```
(Không xoá được category còn product.)

## 5. Tạo / sửa / xoá Product

```graphql
mutation($input: ProductInput!) {
  createProduct(input: $input) { id title price quantity category { id name } }
}
```
Variables:
```json
{ "input": { "title": "Apple Watch S9", "price": 10990000, "quantity": 5,
             "desc": "GPS 45mm", "categoryId": 5 } }
```

```graphql
mutation($input: ProductInput!) {
  updateProduct(id: 13, input: $input) { id title price quantity category { name } }
}
```
```graphql
mutation { deleteProduct(id: 13) }
```

## 6. Các trường hợp lỗi đã kiểm tra

| Tình huống | `extensions.classification` | message |
|---|---|---|
| `price` ≤ 0 | `BAD_REQUEST` | Giá phải lớn hơn 0 (kèm `extensions.fieldErrors.price`) |
| `title` rỗng, `quantity` < 0 (nhiều lỗi cùng lúc) | `BAD_REQUEST` | các message nối bằng `;`, `fieldErrors` theo từng field |
| `price` > 2 chữ số thập phân | `BAD_REQUEST` | Giá không hợp lệ (tối đa 16 chữ số nguyên và 2 chữ số thập phân) |
| `categoryId` / `id` không tồn tại (mutation) | `NOT_FOUND` | `Product không tồn tại với id = 999` |
| Tạo/sửa category trùng tên | `BAD_REQUEST` | `Category 'Laptop' đã tồn tại` |
| Xoá category còn product | `BAD_REQUEST` | `Không thể xoá category '...' vì vẫn còn sản phẩm...` |
| Thiếu field bắt buộc / `input: null` | `ValidationError` | do GraphQL schema (`Float!`, `ProductInput!`) chặn |
| `id` sai kiểu (`"abc"`) | `BAD_REQUEST` | `Tham số không hợp lệ: sai kiểu dữ liệu ...` |
| Field không có trong schema | `ValidationError` | `Field 'foo' in type 'Product' is undefined` |

Lưu ý: GraphQL luôn trả **HTTP 200**; lỗi nằm trong mảng `errors` của body.
