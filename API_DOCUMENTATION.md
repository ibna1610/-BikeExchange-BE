# BikeExchange Backend - API Documentation

## 🎯 Chức năng đã phát triển

### 1️⃣ BUYER API (`/api/buyer`)

#### 📌 Wishlist Management
- **POST** `/api/buyer/{buyerId}/wishlist/add/{bikeId}` - Thêm xe vào yêu thích
- **DELETE** `/api/buyer/{buyerId}/wishlist/remove/{bikeId}` - Xóa xe khỏi yêu thích
- **GET** `/api/buyer/{buyerId}/wishlist` - Xem danh sách yêu thích
- **GET** `/api/buyer/{buyerId}/wishlist/{bikeId}` - Kiểm tra xe có trong yêu thích không

#### 🔍 Search & Filter
- **GET** `/api/buyer/search-advanced?minPrice=X&maxPrice=Y&brand=Z&bikeType=TYPE...` - Tìm kiếm & lọc nâng cao
- **GET** `/api/buyer/bikes/{bikeId}` - Xem chi tiết xe (tăng view count)

#### 🔧 Inspection (Kiểm định)
- **POST** `/api/buyer/{buyerId}/inspection/request/{bikeId}?inspectionFee=X` - Yêu cầu kiểm định
- **GET** `/api/buyer/inspection/{bikeId}/approved` - Lấy báo cáo kiểm định đã phê duyệt
- **GET** `/api/buyer/bikes/{bikeId}/inspections` - Xem tất cả báo cáo kiểm định của xe

#### 💳 Purchase & Transaction
- **POST** `/api/buyer/{buyerId}/purchase/{bikeId}?depositAmount=X` - Đặt mua / cọc xe
- **GET** `/api/buyer/transaction/{transactionId}` - Theo dõi giao dịch
- **GET** `/api/buyer/{buyerId}/transactions` - Xem tất cả giao dịch của Buyer
- **PUT** `/api/buyer/transaction/{transactionId}/complete` - Hoàn tất giao dịch
- **PUT** `/api/buyer/transaction/{transactionId}/cancel` - Hủy đặt cọc

#### ⭐ Rating & Review
- **POST** `/api/buyer/transaction/{transactionId}/rate?rating=X&review=TEXT` - Đánh giá Seller sau giao dịch

#### 📢 Report & Dispute
- **POST** `/api/buyer/{buyerId}/report?bikeId=X&reportedUserId=Y&reportType=FRAUD&description=TEXT` - Báo cáo vi phạm
- **GET** `/api/buyer/{buyerId}/reports` - Xem lịch sử báo cáo của tôi
- **PUT** `/api/buyer/transaction/{transactionId}/dispute?reason=TEXT` - Tranh chấp giao dịch

---

### 2️⃣ ADMIN API (`/api/admin`)

#### 📋 Listing Management (Quản lý tin đăng)
- **GET** `/api/admin/listings?status=ACTIVE&status=VERIFIED&page=0&size=20` - Lấy danh sách tin đăng (lọc theo status)
- **PUT** `/api/admin/listings/{listingId}` - Cập nhật trạng thái (param `status`, optional `reason`)
- **POST** `/api/admin/posts/{postId}/approve` - Phê duyệt tin đăng (một alias cho listings)
- **PUT** `/api/admin/listings/{listingId}/lock` - Khóa tin đăng (chuyển sang CANCELLED)
- **DELETE** `/api/admin/listings/{listingId}` - Xóa tin đăng (chuyển sang CANCELLED)

#### 👥 User Management (Quản lý người dùng)
- **GET** `/api/admin/users?page=0&size=20` - Danh sách tất cả người dùng
- **GET** `/api/admin/users/role/{role}?page=0&size=20` - Danh sách người dùng theo role (BUYER, SELLER, ADMIN)
- **POST** `/api/admin/users/{userId}/activate` - Kích hoạt tài khoản
- **POST** `/api/admin/users/{userId}/deactivate` - Vô hiệu hóa tài khoản
- **POST** `/api/admin/users/{userId}/suspend?reason=TEXT` - Khóa tài khoản (vì vi phạm)
- **GET** `/api/admin/users/search?email=TEXT` - Tìm người dùng theo email

#### 📢 Report Management (Xử lý báo cáo)
- **GET** `/api/admin/reports/pending?page=0&size=20` - Xem báo cáo chờ xử lý
- **PUT** `/api/admin/reports/{reportId}/resolve?adminNote=TEXT&resolution=RESOLVED` - Xử lý báo cáo
- **GET** `/api/admin/reports/type/{type}` - Xem báo cáo theo loại (SPAM, FRAUD, INAPPROPRIATE...)

#### 💼 Transaction Management (Quản lý giao dịch)
- **GET** `/api/admin/transactions?page=0&size=20` - Xem tất cả giao dịch
- **GET** `/api/admin/transactions/status/{status}?page=0&size=20` - Xem giao dịch theo status (PENDING, COMPLETED, CANCELLED...)
- **PUT** `/api/admin/transactions/{transactionId}/cancel?reason=TEXT` - Hủy giao dịch (hoàn tiền)

#### 🔧 Inspection Approval (Phê duyệt kiểm định)
- **PUT** `/api/admin/inspections/{inspectionId}/approve` - Duyệt báo cáo kiểm định
- **PUT** `/api/admin/inspections/{inspectionId}/reject?reason=TEXT` - Từ chối báo cáo
- **GET** `/api/admin/inspections/pending?page=0&size=20` - Xem báo cáo kiểm định chờ duyệt

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
3. Seller xác nhận/từ chối
4. Nếu chấp nhận → giao dịch COMPLETED
5. Buyer đánh giá → Seller rating update

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
