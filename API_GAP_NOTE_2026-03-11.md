# API Gap Note (2026-03-11)

## Scope
Doi chieu bang API muc tieu (Buyer/Seller/Inspector/Admin) voi endpoint hien co trong code backend.

## Update sau implement (Admin)
Da bo sung them cac API Admin con thieu trong `AdminController`:
- Users: `GET /api/admin/users/{id}`, `PUT /api/admin/users/{id}/lock`, `PUT /api/admin/users/{id}/unlock`, `DELETE /api/admin/users/{id}`
- Bikes moderation path: `GET /api/admin/bikes/pending`, `GET /api/admin/bikes/{id}`, `PUT /api/admin/bikes/{id}/approve|reject|hide`
- Orders admin: `GET /api/admin/orders`, `GET /api/admin/orders/{id}`, `PUT /api/admin/orders/{id}/update-status`
- Payments/Fees: `GET /api/admin/payments`, `GET /api/admin/fees`
- Reports: `GET /api/admin/reports/{id}`
- Inspection admin: `GET /api/admin/inspection-requests`, `GET /api/admin/inspection-reports`, `GET /api/admin/inspectors`, `PUT /api/admin/inspectors/{id}/approve|suspend`
- Dashboard/Statistics: `GET /api/admin/dashboard`, `GET /api/admin/statistics/users|bikes|orders|revenue|inspections`
- Admin Brands CRUD: `GET|POST|PUT|DELETE /api/admin/brands...`
- Admin Categories CRUD alias: `GET|POST|PUT|DELETE /api/admin/categories...`
- Admin Components CRUD: `GET|POST|PUT|DELETE /api/admin/components...` (da implement that su)
- Report paths theo spec: da bo sung `PUT /api/admin/reports/{id}/process` va `PUT /api/admin/reports/{id}/resolve`

Trang thai hien tai: Admin API da khop day du theo danh sach yeu cau.

Them cap nhat:
- Da chuan hoa response format cho Admin APIs theo mau thong nhat:
  - success: boolean
  - message: string
  - data: object/array/page (neu co)
  - summary: object (chi dung cho endpoint can tong hop)

## Legend
- `OK`: da co endpoint trung hoac gan trung.
- `EQ`: da co endpoint tuong duong, khac path/method/ten.
- `MISS`: chua co endpoint trong code.

## 1) Authentication
- `POST /api/auth/register` -> `OK` (`POST /api/auth/register`)
- `POST /api/auth/login` -> `OK` (`POST /api/auth/login`)
- `POST /api/auth/logout` -> `MISS`
- `PUT /api/auth/change-password` -> `MISS`
- `POST /api/auth/forgot-password` -> `OK` (`POST /api/auth/forgot-password`)
- `POST /api/auth/reset-password` -> `OK` (`POST /api/auth/reset-password`)

## 2) User Profile (dang nhap)
- `GET /api/users/profile` -> `MISS`
- `PUT /api/users/profile` -> `MISS`
- `GET /api/users/:id` -> `OK` (`GET /api/users/{userId}`)
- `GET /api/users/:id/reviews` -> `MISS` (co `GET /api/reviews/seller/{sellerId}`)

## 3) Guest APIs
### 3.1 Bike Listing
- `GET /api/bikes` -> `OK`
- `GET /api/bikes/:id` -> `OK`
- `GET /api/bikes/search` -> `EQ` (dang dung `GET /api/bikes` voi query params)
- `GET /api/bikes/filters` -> `MISS`

### 3.2 Public Data
- `GET /api/brands` -> `MISS`
- `GET /api/categories` -> `OK`
- `GET /api/featured-bikes` -> `MISS`

## 4) Buyer APIs
### 4.1 Quan ly tai khoan Buyer
- Khong co namespace rieng `/api/buyer/*` -> `MISS` (dang dung API chung)

### 4.2 Tim kiem va xem xe
- `GET /api/buyer/bikes` -> `EQ` (`GET /api/bikes`)
- `GET /api/buyer/bikes/:id` -> `EQ` (`GET /api/bikes/{id}`)
- `GET /api/buyer/bikes/:id/inspection-report` -> `MISS` (chi co nhom `/api/inspections`)

### 4.3 Wishlist
- `GET /api/buyer/wishlist` -> `OK`
- `POST /api/buyer/wishlist/:bikeId` -> `OK`
- `DELETE /api/buyer/wishlist/:bikeId` -> `OK`

Ghi chu: backend dang support them alias cu `/api/wishlist` de giu backward compatibility.

### 4.4 Chat/Message voi Seller
- `GET /api/chats` -> `EQ` (`GET /api/chat/conversations`)
- `GET /api/chats/:chatId/messages` -> `EQ` (`GET /api/chat/conversations/{conversationId}/messages`)
- `POST /api/chats` -> `EQ` (`POST /api/chat/conversations`)
- `POST /api/chats/:chatId/messages` -> `MISS` (hien la `POST /api/chat/messages`)
- `PUT /api/chats/:chatId/read` -> `MISS` (mark read dang o service khi GET messages)

### 4.5 Dat mua/giao dich
- `POST /api/orders` -> `OK`
- `GET /api/orders/my-orders` -> `EQ` (`GET /api/orders/my-purchases`)
- `GET /api/orders/:orderId` -> `MISS` (co `GET /api/orders/{id}/history`)
- `PUT /api/orders/:orderId/cancel` -> `EQ` (`POST /api/orders/{id}/cancel`)

### 4.6 Thanh toan
- `POST /api/payments/create` -> `EQ` (`GET /api/vnpay/create-payment`)
- `GET /api/payments/:paymentId` -> `MISS`
- `POST /api/payments/callback` -> `EQ` (`GET /api/vnpay/vnpay-payment-return`, `GET /api/vnpay/ipn`)
- `GET /api/payments/history` -> `EQ` (`GET /api/wallet/transactions`)

### 4.7 Danh gia Seller
- `POST /api/reviews` -> `OK`
- `GET /api/reviews/my-reviews` -> `MISS`
- `PUT /api/reviews/:reviewId` -> `MISS`

## 5) Seller APIs
### 5.1 Nang cap Seller
- `POST /api/seller/register` -> `EQ` (`POST /api/users/{userId}/upgrade-to-seller`)

### 5.2 Quan ly tin dang xe
- `POST /api/seller/bikes` -> `EQ` (`POST /api/bikes` role SELLER)
- `GET /api/seller/bikes` -> `MISS` (co the loc `GET /api/bikes` theo seller neu bo sung query)
- `GET /api/seller/bikes/:id` -> `EQ` (`GET /api/bikes/{id}`)
- `PUT /api/seller/bikes/:id` -> `EQ` (`PUT /api/bikes/{id}`)
- `DELETE /api/seller/bikes/:id` -> `EQ` (`DELETE /api/bikes/{id}`)
- `PUT /api/seller/bikes/:id/hide` -> `MISS`
- `PUT /api/seller/bikes/:id/show` -> `MISS`
- `PUT /api/seller/bikes/:id/status` -> `MISS`

### 5.3 Upload media
- `POST /api/uploads/images` -> `MISS`

### 5.4 Quan ly giao dich
- `GET /api/seller/orders` -> `EQ` (`GET /api/orders/my-sales`)
- `GET /api/seller/orders/:orderId` -> `EQ` (`GET /api/orders/{id}/history`)
- `PUT /api/seller/orders/:orderId/confirm` -> `EQ` (`POST /api/orders/{id}/deliver` hoac `confirm-return` tuy nghiep vu)
- `PUT /api/seller/orders/:orderId/reject` -> `MISS`
- `PUT /api/seller/orders/:orderId/update-status` -> `MISS`

### 5.5 Danh gia va uy tin
- `GET /api/seller/reviews` -> `EQ` (`GET /api/reviews/seller/{sellerId}`)
- `GET /api/seller/rating` -> `MISS`

### 5.6 Goi dich vu
- `GET /api/packages` -> `MISS`
- `POST /api/seller/packages/buy` -> `MISS`
- `GET /api/seller/packages/history` -> `MISS`
- `GET /api/seller/packages/current` -> `MISS`

### 5.7 Yeu cau kiem dinh
- `POST /api/seller/bikes/:bikeId/request-inspection` -> `EQ` (`POST /api/inspections`)
- `GET /api/seller/inspection-requests` -> `EQ` (`GET /api/inspections?sellerId=...`)
- `GET /api/seller/bikes/:bikeId/inspection-status` -> `EQ` (`GET /api/inspections?bike_id=...`)

## 6) Inspector APIs
- Nhom `/api/inspector/*` -> phan lon `MISS` o level path.
- Nghiep vu tuong duong hien co trong `/api/inspections`:
  - list requests -> `EQ` (`GET /api/inspections` + filter)
  - detail request -> `EQ` (`GET /api/inspections/{inspectionId}`)
  - accept/reject -> `EQ` (`PUT /api/inspections/{inspectionId}?status=ASSIGNED/REJECTED`)
  - tao/cap nhat report -> `EQ` (`POST /api/inspections/{inspectionId}/report` + `PUT /api/inspections/{inspectionId}`)
- Con thieu ro rang:
  - `POST /api/inspector/reports/:reportId/upload-images` -> `MISS`
  - `PUT /api/inspector/bikes/:bikeId/verified|unverified` -> `MISS` (hien verify qua admin approve inspection)
  - Toan bo nhom `inspector/disputes` -> `MISS`

## 7) Admin APIs
### 7.1 Users
- `GET /api/admin/users` -> `OK`
- `GET /api/admin/users/:id` -> `MISS`
- `PUT /api/admin/users/:id` -> `OK`
- `PUT /api/admin/users/:id/lock` -> `EQ` (dung `PUT /api/admin/users/{id}?status=...`)
- `PUT /api/admin/users/:id/unlock` -> `EQ` (dung `PUT /api/admin/users/{id}?status=...`)
- `DELETE /api/admin/users/:id` -> `MISS`

### 7.2 Kiem duyet tin
- `GET /api/admin/bikes/pending` -> `EQ` (`GET /api/admin/listings?status=...`)
- `GET /api/admin/bikes/:id` -> `MISS`
- `PUT /api/admin/bikes/:id/approve` -> `EQ` (`PUT /api/admin/listings/{postId}?action=APPROVE`)
- `PUT /api/admin/bikes/:id/reject` -> `EQ` (`PUT /api/admin/listings/{postId}?action=REJECT`)
- `PUT /api/admin/bikes/:id/hide` -> `EQ` (`PUT /api/admin/listings/{postId}?action=...`)

### 7.3 Danh muc
- Categories CRUD -> `EQ` (`/api/categories`, role ADMIN cho CUD)
- Brands CRUD -> `MISS`
- Components CRUD -> `MISS`

### 7.4 Giao dich
- `GET /api/admin/orders` -> `MISS`
- `GET /api/admin/orders/:id` -> `MISS`
- `PUT /api/admin/orders/:id/update-status` -> `MISS`
- `GET /api/admin/payments` -> `EQ` (`GET /api/admin/transactions`)
- `GET /api/admin/fees` -> `MISS`

### 7.5 Reports/Khieu nai
- `GET /api/admin/reports` -> `OK`
- `GET /api/admin/reports/:id` -> `MISS`
- `PUT /api/admin/reports/:id/process` -> `EQ` (`PUT /api/admin/reports/{id}`)
- `PUT /api/admin/reports/:id/resolve` -> `EQ` (`PUT /api/admin/reports/{id}`)

### 7.6 Kiem dinh
- `GET /api/admin/inspection-requests` -> `EQ` (`GET /api/admin/inspections/pending`)
- `GET /api/admin/inspection-reports` -> `MISS`
- `GET /api/admin/inspectors` -> `MISS`
- `PUT /api/admin/inspectors/:id/approve` -> `MISS`
- `PUT /api/admin/inspectors/:id/suspend` -> `MISS`

### 7.7 Dashboard/Thong ke
- `GET /api/admin/dashboard` -> `EQ` (`GET /api/admin/metrics?type=system`)
- `GET /api/admin/statistics/users` -> `MISS`
- `GET /api/admin/statistics/bikes` -> `MISS`
- `GET /api/admin/statistics/orders` -> `MISS`
- `GET /api/admin/statistics/revenue` -> `MISS`
- `GET /api/admin/statistics/inspections` -> `MISS`

---

## Quick Summary
- Da co mot phan lon nghiep vu core (auth, bikes, orders, wishlist, chat, inspections, admin basics).
- Khoang cach lon nhat la:
  1) Chua chuan hoa namespace theo role (`/buyer`, `/seller`, `/inspector`).
  2) Con thieu nhieu endpoint chi tiet cho admin va seller packages.
  3) Con thieu module brands/components/payments abstraction.

## De xuat implement theo pha
1. Pha 1 (compatibility path): them alias endpoint de map sang service hien co (khong doi nghiep vu).
2. Pha 2 (missing core): bo sung endpoint thieu quan trong: `auth/logout`, `auth/change-password`, `users/profile`, `admin/users/{id}`, `admin/reports/{id}`, `orders/{id}`.
3. Pha 3 (new module): brands/components/packages/uploads/payments domain.
4. Pha 4 (path standardization): refactor ve dung namespace `/buyer`, `/seller`, `/inspector`, giu backward compatibility trong 1-2 release.
