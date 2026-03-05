# 🚀 BikeExchange Backend - Swagger API Documentation

## 📌 Swagger Links

### **Main Swagger UI Endpoint:**
```
http://localhost:8080/swagger-ui.html
```

### **OpenAPI JSON Specification:**
```
http://localhost:8080/v3/api-docs
```

### **OpenAPI YAML Specification:**
```
http://localhost:8080/v3/api-docs.yaml
```

---

## 🔧 Cách chạy Backend

### **Bước 1: Chuẩn bị môi trường**
- Java 17 trở lên
- Maven 3.6+
- MySQL 5.7+ hoặc Database khác

### **Bước 2: Cấu hình Database**
Chỉnh sửa file `application.yml` (hoặc `application.properties`):

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bike_exchange_db
    username: root
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

### **Bước 3: Build & Run**

**Dùng Maven:**
```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

**Hoặc chạy JAR file:**
```bash
java -jar target/bike-exchange-be-1.0.0.jar
```

### **Bước 4: Truy cập Swagger UI**
Mở browser và go to:
```
http://localhost:8080/swagger-ui.html
```

---

## 📖 OpenAPI Configuration

**File cấu hình:** `SwaggerConfig.java`

**Thông tin API:**
- **Title:** BikeExchange API
- **Version:** 1.0.0
- **Description:** Backend API cho platform mua bán xe đạp thể thao cũ
- **Contact:** support@bikeexchange.com
- **License:** Apache 2.0

**Security Scheme:**
- **Type:** Bearer Token (JWT)
- **Format:** JWT

---

## 🔐 Authentication

### **Để sử dụng Buyer/Admin APIs:**

1. **Bước 1:** Lấy JWT Token
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "buyer@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "role": "BUYER"
}
```

2. **Bước 2:** Thêm Token vào Header
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

3. **Trên Swagger UI:**
   - Nhấn nút **"Authorize"** ở góc trên phải
   - Nhập: `Bearer <your_token>`
   - Nhấn **"Authorize"**

---

## 📚 API Endpoints Summary

### **Buyer API** (`/api/buyer`)
- ✅ Wishlist Management (4 endpoints)
- ✅ Search & Filter (2 endpoints)
- ✅ Inspection (3 endpoints)
- ✅ Purchase & Transaction (5 endpoints)
- ✅ Rating & Review (1 endpoint)
- ✅ Report & Dispute (3 endpoints)

**Tổng cộng: 18 endpoints**

### **Seller API** (`/api/users`, `/api/seller`)
- ✅ Upgrade to Seller (1 endpoint)
- ✅ Shop Management (shop info, stats)
- ✅ Listings Management (create, update, delete bike listings)
- ✅ Sales & Orders (track sales, manage orders)
- ✅ Wallet & Earnings (view earnings, withdrawal)
- ✅ Rating & Reviews (view customer feedback)

**Tổng cộng: 10+ endpoints**

### **Admin API** (`/api/admin`)
- ✅ Listing Management (5 endpoints)
- ✅ User Management (6 endpoints)
- ✅ Report Management (3 endpoints)
- ✅ Transaction Management (3 endpoints)
- ✅ Inspection Management (3 endpoints)
- ✅ Statistics & Dashboard (6 endpoints)

**Tổng cộng: 26 endpoints**

### **Existing APIs**
- User Management (`/api/users`)
- Health Check (`/health`)
- Transaction (`/api/transaction`)
- Bike (`/api/bikes`)

---

## 🧪 Sample API Calls

### **1. Search Bikes (Buyer)**
```bash
GET /api/buyer/search-advanced?minPrice=500000&maxPrice=10000000&bikeType=Road&page=0&size=20

Authorization: Bearer <token>
```

### **2. Add to Wishlist (Buyer)**
```bash
POST /api/buyer/1/wishlist/add/101

Authorization: Bearer <token>
```

### **3. Make Purchase (Buyer)**
```bash
POST /api/buyer/1/purchase/101?depositAmount=1000000

Authorization: Bearer <token>
```

### **4. Upgrade to Seller**
```bash
POST /users/1/upgrade-to-seller
Content-Type: application/json

Authorization: Bearer <token>

{
  "shopName": "My Bike Shop",
  "shopDescription": "Quality bikes and accessories",
  "agreeToTerms": true
}
```

### **5. Create Bike Listing (Seller)**
```bash
POST /api/seller/listings/create
Content-Type: application/json

Authorization: Bearer <seller_token>

{
  "bikeName": "Giant TCR Advanced",
  "bikeType": "Road",
  "brand": "Giant",
  "condition": "LIKE_NEW",
  "pricePoints": 5000,
  "description": "Professional road bike in excellent condition"
}
```

### **6. Get Seller Dashboard Stats (Seller)**
```bash
GET /api/seller/dashboard/stats

Authorization: Bearer <seller_token>
```

### **7. Approve Listing (Admin)**
```bash
PUT /api/admin/listings/101/approve

Authorization: Bearer <admin_token>
```

### **8. Get Dashboard Stats (Admin)**
```bash
GET /api/admin/metrics/dashboard

Authorization: Bearer <admin_token>
```

---

## �️ Seller API Flow

Seller có 3 bước chính:

### **Bước 1: Upgrade từ Buyer → Seller**
```bash
POST /users/{userId}/upgrade-to-seller
```
- Body: `shopName`, `shopDescription`, `agreeToTerms`
- Response: User object với role=SELLER
- **Chỉ BUYER role mới có thể upgrade**
- Sau upgrade, user nhận được seller capabilities

### **Bước 2: Tạo & Quản lý Bike Listings**
- `POST /api/seller/listings/create` - Tạo bike listing mới
- `GET /api/seller/listings` - Xem tất cả listing của seller
- `PUT /api/seller/listings/{id}` - Update listing info
- `DELETE /api/seller/listings/{id}` - Delete listing
- Listing cần admin approve trước khi khách có thể mua

### **Bước 3: Quản lý Sales & Earnings**
- `GET /api/seller/orders` - Xem orders của seller
- `GET /api/seller/earnings/summary` - Xem tổng earnings
- `POST /api/seller/withdraw-request` - Request rút tiền
- `GET /api/seller/ratings` - Xem ratings từ buyers
- Earnings = Sale Price - 5% Commission (admin fee)

---

## �🔍 Swagger UI Features

### **Explore & Test:**
1. **Browse** tất cả endpoints được organize theo tags
2. **Xem** chi tiết parameters, request/response schemas
3. **Try it out** - Test trực tiếp từ UI
4. **Xem** HTTP request/response examples

### **Authorization:**
- Click "**Authorize**" button
- Nhập JWT token
- Tất cả requests sẽ tự động thêm token

### **Generate Code:**
- Nhấn "**Generate client**" (if supported)
- Copy OpenAPI spec cho tạo client libraries

---

## 📋 API Documentation Fields

### **Operation:**
- **Summary** - Tóm tắt chức năng
- **Description** - Mô tả chi tiết (nếu có)
- **Tags** - Phân loại API

### **Parameters:**
- **Path/Query/Body** parameters
- **Data types** - String, Number, Boolean, Object, Array
- **Required/Optional** - Hiển thị rõ ràng

### **Responses:**
- **HTTP Status Codes** (200, 201, 400, 404, 500...)
- **Response Body** - Ví dụ JSON
- **Error Messages** - Chi tiết lỗi

---

## 🛠️ Dependencies Added

```xml
<!-- Springdoc OpenAPI (Swagger UI) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.0.2</version>
</dependency>

<!-- Spring Security (JWT) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT Token -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
```

---

## ⚙️ Configuration Properties (optional)

Add to `application.yaml` để customize:

```yaml
# Swagger Configuration
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operations-sorter: method
    tags-sorter: alpha
```

---

## 📞 Support

**Documentation:**
- [SpringDoc OpenAPI Docs](https://springdoc.org/)
- [Swagger OpenAPI Spec](https://swagger.io/specification/)

**Issues:**
- Kiểm tra logs: `log/app.log`
- Verify JWT token validity
- Check database connection

---

**Last Updated:** February 2026  
**API Version:** 1.0.0
