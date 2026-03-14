# BikeExchange Backend - API Documentation

## Cap nhat doi chieu API (2026-03-11)

- Da bo sung file note doi chieu API muc tieu vs API hien co:
	- `API_GAP_NOTE_2026-03-11.md`
- File note danh dau ro 3 trang thai:
	- `OK`: da co endpoint
	- `EQ`: co endpoint tuong duong (khac path/method/ten)
	- `MISS`: chua co endpoint
- De xuat implementation theo pha da duoc ghi o cuoi file note.

## 🎯 Chức năng đã phát triển

### 1️⃣ BUYER API (`/api/buyer`)

#### 📌 Wishlist Management
- **GET** `/api/buyer/wishlist` - Xem danh sách xe yêu thích
- **POST** `/api/buyer/wishlist/{bikeId}` - Thêm xe vào wishlist
- **DELETE** `/api/buyer/wishlist/{bikeId}` - Xóa xe khỏi wishlist

Ghi chu tuong thich nguoc: backend van ho tro alias cu `/api/wishlist`.

#### 🔍 Search & Filter
- **GET** `/api/buyer/search-advanced?minPrice=X&maxPrice=Y&brand=Z&bikeType=TYPE...` - Tìm kiếm & lọc nâng cao
- **GET** `/api/buyer/bikes/{bikeId}` - Xem chi tiết xe (tăng view count)

#### 🔧 Inspection (Kiểm định)
- **POST** `/api/buyer/{buyerId}/inspection/request/{bikeId}?inspectionFee=X` - Yêu cầu kiểm định
- **GET** `/api/buyer/inspection/{bikeId}/approved` - Lấy báo cáo kiểm định đã phê duyệt
- **GET** `/api/buyer/bikes/{bikeId}/inspections` - Xem tất cả báo cáo kiểm định của xe

#### 💳 Purchase & Transaction
- **POST** `/api/orders` - Buyer tạo đơn hàng (escrow points)
- **GET** `/api/orders/my-purchases` - Buyer xem lịch sử mua
- **GET** `/api/orders/my-sales` - Seller xem lịch sử bán
- **GET** `/api/orders/{id}/history` - Xem timeline chi tiết đơn hàng
- **POST** `/api/orders/{id}/accept` - Seller accept đơn
- **POST** `/api/orders/{id}/deliver` - Seller đánh dấu đã giao (shipping carrier + tracking code)
- **POST** `/api/orders/{id}/confirm-receipt` - Buyer xác nhận nhận hàng (release points cho seller)
- **POST** `/api/orders/{id}/cancel` - Buyer hủy đơn ở trạng thái ESCROWED

#### ⭐ Rating & Review
- **POST** `/api/reviews?orderId=X&rating=Y&comment=TEXT` - Buyer đánh giá Seller sau khi order COMPLETED
- **GET** `/api/reviews/seller/{sellerId}` - Xem danh sách đánh giá của Seller

#### 📢 Report & Dispute
- **POST** `/api/buyer/{buyerId}/report?bikeId=X&reportedUserId=Y&reportType=FRAUD&description=TEXT` - Báo cáo vi phạm
- **GET** `/api/buyer/{buyerId}/reports` - Xem lịch sử báo cáo của tôi
- **PUT** `/api/buyer/transaction/{transactionId}/dispute?reason=TEXT` - Tranh chấp giao dịch
- **GET** `/api/orders/my-disputes` - Buyer xem danh sách tranh chấp đơn hàng của mình
- **POST** `/api/orders/{orderId}/return-dispute` - Buyer mở tranh chấp hoàn hàng khi seller không xác nhận hoàn

---

### 2️⃣ ADMIN API (`/api/admin`)

#### 📋 Listing Management (Quản lý tin đăng)
- **PUT** `/api/admin/listings/{bikeId}/approve` - Duyệt tin đăng
- **PUT** `/api/admin/listings/{bikeId}/reject?reason=TEXT` - Từ chối tin (kèm lý do)
- **GET** `/api/admin/listings/pending?page=0&size=20` - Xem danh sách tin chờ duyệt
- **PUT** `/api/admin/listings/{bikeId}/lock` - Khóa tin đăng
- **DELETE** `/api/admin/listings/{bikeId}` - Xóa tin đăng

#### 👥 User Management (Quản lý người dùng)
- **GET** `/api/admin/users?page=0&size=20&role=ROLE` - Danh sách người dùng (tùy chọn lọc theo role)
- **GET** `/api/admin/users/role/{role}?page=0&size=20` - Danh sách người dùng theo role (BUYER, SELLER, ADMIN)
- **GET** `/api/admin/users/search?email=TEXT&page=0&size=20` - Tìm người dùng theo email
- **POST** `/api/admin/users/{userId}/activate` - Kích hoạt tài khoản
- **POST** `/api/admin/users/{userId}/deactivate` - Vô hiệu hóa tài khoản
- **POST** `/api/admin/users/{userId}/suspend?reason=TEXT` - Khóa tài khoản (vì vi phạm)
- **POST** `/api/admin/users/{userId}/role?role=ROLE` - Thay đổi vai trò
- **POST** `/api/admin/users/{userId}/status?status=TEXT` - Cập nhật trường status

#### 📢 Report Management (Xử lý báo cáo)
- **GET** `/api/admin/reports/pending?page=0&size=20` - Xem báo cáo chờ xử lý
- **PUT** `/api/admin/reports/{reportId}/resolve?resolution=RESOLVED|REJECTED&adminNote=TEXT` - Xử lý báo cáo (đặt trạng thái)
- **GET** `/api/admin/reports/type/{type}?page=0&size=20` - Xem báo cáo theo loại (SPAM, FRAUD, INAPPROPRIATE...)

#### 💼 Transaction Management (Quản lý giao dịch)
- **GET** `/api/admin/transactions?page=0&size=20&status=STATUS` - Xem tất cả giao dịch (lọc theo status)
- **GET** `/api/admin/transactions/status/{status}?page=0&size=20` - Xem giao dịch theo status (PENDING, COMPLETED, CANCELLED...)
- **PUT** `/api/admin/transactions/{transactionId}/cancel?reason=TEXT` - Hủy giao dịch (hoàn tiền)

#### 🔧 Inspection Approval (Phê duyệt kiểm định)
- **GET** `/api/admin/inspections/pending?page=0&size=20` - Xem biểu mẫu kiểm định chờ xử lý
- **PUT** `/api/admin/inspections/{inspectionId}/approve` - Duyệt báo cáo kiểm định
- **PUT** `/api/admin/inspections/{inspectionId}/reject?reason=TEXT` - Từ chối báo cáo

#### 📊 Statistics & Dashboard (Thống kê)
- **GET** `/api/admin/metrics/system` - Thống kê hệ thống (tổng users, bikes, transactions, reports...)
- **GET** `/api/admin/metrics/dashboard` - Dashboard (thống kê Buyers, Sellers, Inspectors, revenue...)
- **GET** `/api/admin/metrics/inspection` - Thống kê kiểm định (pending, approved, rejected)
- **GET** `/api/admin/metrics/reports` - Thống kê báo cáo (pending, reviewing, resolved...)
- **GET** `/api/admin/metrics/reports-count` - Tổng số báo cáo
- **GET** `/api/admin/metrics/pending-reports-count` - Số báo cáo chờ xử lý

---

## 📊 Database Entities Tạo Mới

### 1. **Wishlist** - Danh sách yêu thích
```
- id (PK)
- buyer_id (FK) → User
- bike_id (FK) → Bike
- added_at
```

### 2. **Inspection** - Báo cáo kiểm định
```
- id (PK)
- bike_id (FK) → Bike
- inspector_id (FK) → User (người kiểm định)
- requester_id (FK) → User (người yêu cầu)
- status (PENDING, IN_PROGRESS, APPROVED, REJECTED, EXPIRED)
- frameCondition, brakeCondition, drivingCondition
- reportImages, reportDescription
- inspectionFee, isPaid
- validUntil (báo cáo có hiệu lực 90 ngày)
- created_at, completed_at
```

### 3. **Report** - Báo cáo vi phạm
```
- id (PK)
- reporter_id (FK) → User
- bike_id (FK) → Bike (nullable)
- user_id (FK) → User (nullable - người bị báo cáo)
- reportType (SPAM, FRAUD, INAPPROPRIATE, OFFENSIVE_LANGUAGE, FAKE_ITEM, OTHER)
- description, adminNote
- status (PENDING, REVIEWING, RESOLVED, REJECTED)
- created_at, resolved_at
```

---

## 🔄 Main Business Flows Implemented

### ✅ Flow 1: Buyer Wishlist & Search
1. Buyer xem danh sách xe
2. Tìm kiếm & lọc nâng cao
3. Thêm xe vào yêu thích
4. Xem chi tiết xe + báo cáo kiểm định

### ✅ Flow 2: Inspection Workflow
1. Buyer/Seller yêu cầu kiểm định
2. Inspector kiểm tra & upload báo cáo
3. Admin phê duyệt báo cáo
4. System gắn nhãn "Xe đã kiểm định"

### ✅ Flow 3: Purchase & Transaction
1. Buyer đặt mua (cọc)
2. Xe chuyển sang RESERVED
3. Seller accept đơn (ESCROWED → ACCEPTED)
4. Seller đánh dấu đã giao (ACCEPTED → DELIVERED)
5. Buyer xác nhận nhận hàng hoặc auto-release sau 14 ngày (→ COMPLETED)
6. Buyer đánh giá seller (chỉ khi order COMPLETED) → cập nhật rating

### ✅ Flow 4: Admin Moderation
1. Duyệt tin đăng
2. Xử lý báo cáo vi phạm
3. Quản lý người dùng (activate/suspend)
4. Xem thống kê

### ✅ Flow 5: Dispute & Refund
1. Buyer/Seller tranh chấp giao dịch
2. Admin can thiệp & hủy
3. Hoàn tiền cọc

---

## ⚙️ Technology Stack Used

- **Framework**: Spring Boot 3.x
- **Database**: MySQL (via JPA/Hibernate)
- **ORM**: Spring Data JPA
- **API**: RESTful
- **Validation**: Jakarta Persistence & Validation
- **Dependency Injection**: Spring Beans
- **Transaction Management**: @Transactional

---

## 🚀 Next Steps (Còn cần làm)

1. **Authentication & Authorization** - Thêm Spring Security + JWT
2. **Chat/Messaging** - Tạo module Chat giữa Buyer-Seller
3. **Payment Gateway Integration** - VNPay, Momo
4. **Cloud Storage** - Upload ảnh/video (Cloudinary, Firebase)
5. **Email Service** - Gửi thông báo email
6. **Testing** - Unit tests, Integration tests
7. **API Documentation** - Swagger/OpenAPI
8. **Error Handling** - Exception handlers, validation messages
9. **Logging** - SLF4J + Logback
10. **Deployment** - Docker, CI/CD pipeline

---

## 📝 Notes

- Wishlist giới hạn **50 xe** mỗi Buyer
- Báo cáo kiểm định có hiệu lực **90 ngày**
- Seller chỉ được đặt cọc **1 xe tại 1 thời điểm**
- Soft delete được áp dụng (không xóa cứng dữ liệu)
- Lịch sử đầy đủ (audit trail) được lưu qua `createdAt`, `updatedAt`

---

**Created**: February 2026  
**By**: GitHub Copilot
