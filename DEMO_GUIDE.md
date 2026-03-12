# 🎬 Demo Guide - Buyer → Seller Flow (Chi tiết từng bước)

**Date:** March 5, 2026  
**Purpose:** Step-by-step instructions để demo luồng wallet/points/seller upgrade trong Swagger UI

---

## 📋 Chuẩn Bị Trước Demo

### 1️⃣ **Khởi động Backend**
```bash
# Mở terminal ở project folder
cd C:\Users\GMT\Desktop\SWP\-BikeExchange-BE

# Chạy backend
mvn spring-boot:run
```
✅ Chờ tới khi thấy: `Tomcat started on port(s): 8080`

### 2️⃣ **Mở Swagger UI**
- Mở browser: **http://localhost:8080/swagger-ui.html**
- Bạn sẽ thấy tất cả API endpoints

### 3️⃣ **Chuẩn Bị 2-3 Tab Browser**
- Tab 1: Swagger UI (chính)
- Tab 2: REST Client hoặc Postman (tùy chọn)
- Tab 3: Để kiểm tra kết quả (tùy chọn)

---

## 🎯 Demo Flow (28 Steps)

### **PHASE 1: BUYER - LOGIN & DEPOSIT POINTS (5 steps)**

#### **Step 1: Login Buyer Account**
**Endpoint:** `POST /auth/login`

1. Click vào endpoint `POST /auth/login` (Auth Management section)
2. Click "Try it out"
3. Nhập body:
```json
{
  "email": "buyer@example.com",
  "password": "password123"
}
```
4. Click **Execute**
5. **Scroll down xem Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "role": "BUYER"
}
```
6. **📌 COPY token từ response** (dùng cho tất cả request sau)

#### **Step 2: Authorize Swagger**
1. Tìm nút **"Authorize"** ở góc phải trên của Swagger UI (🔒 icon)
2. Click nút đó
3. Paste token:
```
Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```
4. Click **Authorize**
5. Click **Close**

✅ Giờ mọi request sẽ tự động thêm token!

#### **Step 3: Check Initial Wallet**
**Endpoint:** `GET /wallet` (Wallet & Points section)

1. Click "Try it out"
2. Click **Execute**
3. **Xem Response:**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "availablePoints": 0,
    "frozenPoints": 0,
    "updatedAt": "2026-03-05T....."
  }
}
```
✅ Ví mới, chưa có điểm

#### **Step 4: Deposit 500,000 Points**
**Endpoint:** `POST /wallet/deposit` (Wallet & Points section)

1. Click "Try it out"
2. Nhập body:
```json
{
  "amount": 500000,
  "referenceId": "vnpay_demo_001"
}
```
3. Click **Execute**
4. **Xem Response:**
```json
{
  "success": true,
  "message": "Deposit successful",
  "data": {
    "userId": 1,
    "availablePoints": 500000,  // ← Increased!
    "frozenPoints": 0,
    "updatedAt": "2026-03-05T10:15:00"
  }
}
```
✅ Nạp tiền thành công! Có 500k điểm.

#### **Step 5: Check Transaction History**
**Endpoint:** `GET /wallet/transactions` (Wallet & Points section)

1. Click "Try it out"
2. Add params:
   - `userId`: 1
   - `type`: DEPOSIT
3. Click **Execute**
4. **Xem Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 1,
      "amount": 500000,
      "type": "DEPOSIT",
      "status": "SUCCESS",
      "referenceId": "vnpay_demo_001",
      "createdAt": "2026-03-05T10:15:00"
    }
  ],
  "summary": {
    "totalCount": 1,
    "totalAmount": 500000
  }
}
```
✅ Lịch sử giao dịch: Đã nạp 500k

---

### **PHASE 2: BUYER - PURCHASE BIKE (3 steps)**

#### **Step 6: Create Order (Lock Points in Escrow)**
**Endpoint:** `POST /orders` (mục Quản lý đơn hàng)

1. Click "Try it out"
2. Nhập body:
```json
{
  "bikeId": 1,
  "idempotencyKey": "order_demo_123"
}
```
3. Click **Execute**
4. **Xem Response:**
```json
{
  "success": true,
  "message": "Order created and points escrowed successfully",
  "data": {
    "id": 1,
    "buyerId": 1,
    "bikeId": 1,
    "amountPoints": 100000,
    "status": "ESCROWED",
    "createdAt": "2026-03-05T10:20:00",
    "bike": {
      "id": 1,
      "title": "Giant TCR Advanced",
      "pricePoints": 100000,
      "status": "RESERVED"
    }
  }
}
```
✅ Order tạo thành công
✅ 📌 **Note:** Status = ESCROWED (đang khóa điểm)

#### **Step 7: Verify Wallet After Order**
**Endpoint:** `GET /wallet` (Wallet & Points section)

1. Click "Try it out"
2. Click **Execute**
3. **Xem Response:**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "availablePoints": 400000,   // ← 500k - 100k = 400k
    "frozenPoints": 100000,      // ← 100k bị khóa
    "updatedAt": "2026-03-05T10:20:00"
  }
}
```
✅ **QUAN TRỌNG:** 100k điểm bây giờ bị khóa, chỉ còn 400k có thể dùng

#### **Step 8: Approve Order (Complete Purchase)**
**Endpoint:** `POST /orders/{id}/approve` (mục Quản lý đơn hàng)

1. Click "Try it out"
2. Thay `{id}` bằng `1`
3. Click **Execute**
4. **Xem Response:**
```json
{
  "success": true,
  "message": "Order completed and points released to seller",
  "data": {
    "id": 1,
    "buyerId": 1,
    "status": "COMPLETED",
    "amountPoints": 100000,
    "bike": {
      "status": "SOLD"
    }
  }
}
```
✅ Order hoàn thành
✅ Seller nhận tiền (95k điểm)
✅ Admin lấy fee (5k điểm)

---

### **PHASE 3: LEAVE REVIEW & UPGRADE TO SELLER (2 steps)**

#### **Step 9: Create Review**
**Endpoint:** `POST /reviews` (Review & Rating section)

1. Click "Try it out"
2. Add params:
   - `sellerId`: 2
   - `rating`: 5
   - `comment`: Xe%20đứng%20đẻ%20tuyệt%20vời (URL encoded: "Xe đứng đẻ tuyệt vời")
3. Click **Execute**
4. **Xem Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "reviewerId": 1,
    "sellerId": 2,
    "rating": 5,
    "comment": "Xe đứng đẻ tuyệt vời",
    "createdAt": "2026-03-05T10:25:00"
  }
}
```
✅ Review tạo thành công

#### **Step 10: UPGRADE TO SELLER 🎉**
**Endpoint:** `POST /users/{userId}/upgrade-to-seller` (User Management section)

1. Click "Try it out"
2. Thay `{userId}` bằng `1`
3. Nhập body:
```json
{
  "shopName": "My Bike Shop",
  "shopDescription": "Quality bikes for everyone - Chuyên bán xe đạp thể thao cũ",
  "agreeToTerms": true
}
```
4. Click **Execute**
5. **🎉 Xem Response:**
```json
{
  "success": true,
  "message": "User successfully upgraded to seller",
  "data": {
    "id": 1,
    "email": "buyer@example.com",
    "fullName": "John Doe",
    "role": "SELLER",                         // ← SELLER! (was BUYER)
    "shopName": "My Bike Shop",
    "shopDescription": "Quality bikes for everyone - Chuyên bán xe đạp thể thao cũ",
    "upgradedToSellerAt": "2026-03-05T10:30:00",
    "status": "ACTIVE",
    "rating": 0.0,
    "isVerified": false
  }
}
```
✅ **THÀNH CÔNG - Bây giờ là SELLER rồi!** 🎉
✅ Tiếp tục dùng token này cho phase tiếp theo

---

### **PHASE 4: SELLER - CREATE BIKE LISTING (1 step)**

#### **Step 11: Create Bike Listing**
**Endpoint:** `POST /api/seller/listings/create` (Seller API section)

1. Click "Try it out"
2. Nhập body:
```json
{
  "bikeName": "Trek Domane SLR",
  "bikeType": "Road",
  "brand": "Trek",
  "pricePoints": 120000,
  "condition": "LIKE_NEW",
  "description": "Professional road bike, barely used, perfect condition",
  "features": "Carbon frame, electronic shifting, 22 speeds, warranty included"
}
```
3. Click **Execute**
4. **Xem Response:**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "sellerId": 1,
    "bikeName": "Trek Domane SLR",
    "bikeType": "Road",
    "pricePoints": 120000,
    "status": "DRAFT",                    // ← DRAFT (chưa được admin duyệt)
    "createdAt": "2026-03-05T10:35:00"
  }
}
```
✅ Xe tạo thành công với status = DRAFT
✅ **📌 Save bikeId = 2** (dùng cho bước sau)

⚠️ **LƯU Ý:** Xe chưa hiển thị cho buyers vì status = DRAFT. Cần admin duyệt

---

### **PHASE 5: ADMIN - APPROVE LISTING (4 steps)**

#### **Step 12: Login as ADMIN**
**Endpoint:** `POST /auth/login` (Auth Management section)

1. Click "Try it out" (nếu chưa reset Authorize)
2. Nhập body:
```json
{
  "email": "admin@example.com",
  "password": "admin123"
}
```
3. Click **Execute**
4. **Copy admin token** từ response

#### **Step 13: Update Authorize with Admin Token**
1. Click nút **"Authorize"** 🔒
2. Xóa token cũ, paste token admin mới
3. Click **Authorize**
4. Click **Close**

✅ Giờ là logged in as ADMIN

#### **Step 14: Get All Pending Listings (Optional)**
**Endpoint:** `GET /api/admin/listings` (Admin API section)

1. Click "Try it out"
2. Add param: `status=DRAFT`
3. Click **Execute**
4. **Xem Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 2,
      "bikeName": "Trek Domane SLR",
      "status": "DRAFT",
      "sellerId": 1
    }
  ]
}
```
✅ Thấy xe chờ duyệt

#### **Step 15: Approve Seller's Listing**
**Endpoint:** `PUT /api/admin/listings/{id}/approve` (Admin API section)

1. Click "Try it out"
2. Thay `{id}` bằng `2` (bikeId từ Step 11)
3. Nhập body:
```json
{
}
```
(empty body is fine)
4. Click **Execute**
5. **Xem Response:**
```json
{
  "success": true,
  "message": "Listing approved",
  "data": {
    "id": 2,
    "bikeName": "Trek Domane SLR",
    "status": "ACTIVE",                  // ← Changed from DRAFT
    "pricePoints": 120000,
    "sellerId": 1
  }
}
```
✅ **Xe được duyệt!** Bây giờ visible cho tất cả buyers

---

### **PHASE 6: DIFFERENT BUYER - PURCHASE FROM SELLER (5 steps)**

#### **Step 16: Login as DIFFERENT BUYER**
**Endpoint:** `POST /auth/login` (Auth Management section)

1. Click "Try it out"
2. Nhập body:
```json
{
  "email": "buyer2@example.com",
  "password": "password123"
}
```
3. Click **Execute**
4. **Copy buyer2 token**

#### **Step 17: Update Authorize with Buyer2 Token**
1. Click **"Authorize"** 🔒
2. Paste buyer2 token
3. Click **Authorize** → **Close**

✅ Giờ là buyer2

#### **Step 18: Deposit Money for Buyer2**
**Endpoint:** `POST /wallet/deposit` (Wallet & Points section)

1. Click "Try it out"
2. Nhập body:
```json
{
  "amount": 500000,
  "referenceId": "vnpay_demo_002"
}
```
3. Click **Execute**
4. **Xem Response:**
```json
{
  "success": true,
  "data": {
    "availablePoints": 500000,
    "frozenPoints": 0
  }
}
```
✅ Buyer2 có 500k điểm

#### **Step 19: Purchase Seller's Bike**
**Endpoint:** `POST /orders` (mục Quản lý đơn hàng)

1. Click "Try it out"
2. Nhập body:
```json
{
  "bikeId": 2,
  "idempotencyKey": "order_demo_456"
}
```
3. Click **Execute**
4. **Xem Response:**
```json
{
  "success": true,
  "message": "Order created and points escrowed successfully",
  "data": {
    "id": 2,
    "buyerId": 2,
    "bikeId": 2,
    "amountPoints": 120000,
    "status": "ESCROWED",
    "bike": {
      "title": "Trek Domane SLR",
      "pricePoints": 120000,
      "status": "RESERVED"
    }
  }
}
```
✅ Order tạo thành công
✅ **📌 Note bikeId = 2 bị RESERVED**

#### **Step 20: Approve Order (Seller Gets Paid!)**
**Endpoint:** `POST /orders/{id}/approve` (mục Quản lý đơn hàng)

1. Click "Try it out"
2. Thay `{id}` bằng `2`
3. Click **Execute**
4. **🎉 Xem Response:**
```json
{
  "success": true,
  "message": "Order completed and points released to seller",
  "data": {
    "id": 2,
    "status": "COMPLETED",
    "amountPoints": 120000
  }
}
```
✅ **QUAN TRỌNG:**
   - Buyer2: Mất 120k điểm (bị trừ vĩnh viễn)
   - Seller (user_1): Nhận **114,000 điểm** (120k - 6k fee = 114k) ✨
   - Admin: Lấy commission 6k điểm

---

### **PHASE 7: SELLER - CHECK EARNINGS & WITHDRAW (4 steps)**

#### **Step 21: Login Back as SELLER**
**Endpoint:** `POST /auth/login` (Auth Management section)

1. Click "Try it out"
2. Nhập body:
```json
{
  "email": "buyer@example.com",
  "password": "password123"
}
```
3. Click **Execute**
4. **Copy seller token**

#### **Step 22: Update Authorize with Seller Token**
1. Click **"Authorize"** 🔒
2. Paste seller token
3. Click **Authorize** → **Close**

✅ Giờ là seller (user_1) lại

#### **Step 23: Check Wallet - See Earnings**
**Endpoint:** `GET /wallet` (Wallet & Points section)

1. Click "Try it out"
2. Click **Execute**
3. **🎉 Xem Response:**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "availablePoints": 514000,        // ← 400k từ trước + 114k từ sale!
    "frozenPoints": 0,
    "updatedAt": "2026-03-05T10:45:00"
  }
}
```
✅ **CHỨNG MINH:** Seller kiếm được 114k từ việc bán xe!

#### **Step 24: Request Withdrawal**
**Endpoint:** `POST /wallet/withdraw-request` (Wallet & Points section)

1. Click "Try it out"
2. Nhập body:
```json
{
  "amount": 114000,
  "bankName": "TechComBank",
  "bankAccountName": "Nguyen Van Seller",
  "bankAccountNumber": "1234567890123"
}
```
3. Click **Execute**
4. **Xem Response:**
```json
{
  "success": true,
  "message": "Withdraw request submitted. Pending admin approval.",
  "data": {
    "userId": 1,
    "availablePoints": 400000,        // ← 514k - 114k = 400k (trừ đi)
    "frozenPoints": 114000,           // ← 114k bị khóa chờ duyệt
    "updatedAt": "2026-03-05T10:50:00"
  }
}
```
✅ Withdrawal request tạo thành công
✅ **SỰ THẬT:** 114k điểm bây giờ bị khóa, chờ admin duyệt

---

### **PHASE 8: ADMIN - APPROVE WITHDRAWAL (4 steps)**

#### **Step 25: Login as ADMIN Again**
**Endpoint:** `POST /auth/login` (Auth Management section)

1. Click "Try it out"
2. Nhập body:
```json
{
  "email": "admin@example.com",
  "password": "admin123"
}
```
3. Click **Execute**
4. **Copy admin token**

#### **Step 26: Update Authorize**
1. Click **"Authorize"** 🔒
2. Paste admin token
3. Click **Authorize** → **Close**

✅ Admin logged in

#### **Step 27: List Pending Withdrawals**
**Endpoint:** `GET /admin/withdrawals` (Admin Management section)

1. Click "Try it out"
2. Add param: `status=PENDING`
3. Click **Execute**
4. **Xem Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 2,
      "userId": 1,
      "amount": 114000,
      "type": "WITHDRAW",
      "status": "PENDING",
      "referenceId": "Withdrawal: TechComBank | Nguyen Van Seller | 1234567890123",
      "createdAt": "2026-03-05T10:50:00"
    }
  ]
}
```
✅ Thấy pending withdrawal từ seller

#### **Step 28: Approve Withdrawal**
**Endpoint:** `POST /admin/withdrawals/{transactionId}/approve` (Admin Management section)

1. Click "Try it out"
2. Thay `{transactionId}` bằng `2`
3. Click **Execute**
4. **✅ Xem Response:**
```json
{
  "success": true,
  "message": "Withdrawal approved and completed"
}
```
✅ **HOÀN THÀNH!** Withdrawal được duyệt
✅ Points được phát hành từ frozen → ready để chuyển khoản

---

## 🌟 Complete Flow Summary

```
✅ Step 1-5:   Buyer nạp 500k điểm
✅ Step 6-8:   Buyer mua xe (100k), xe được bán
✅ Step 9-10:  Buyer viết review & UPGRADE thành SELLER 🎉
✅ Step 11:    Seller tạo listing xe mới (120k)
✅ Step 12-15: Admin duyệt listing
✅ Step 16-20: Buyer khác mua từ seller (120k), seller KIẾM 114k ✨
✅ Step 21-24: Seller kiểm tra ví (514k), request rút 114k
✅ Step 25-28: Admin duyệt withdrawal, seller sẵn sàng nhận tiền

FINAL STATE:
- User_1 (Seller): 400k điểm (after withdrawal)
- User_2 (Buyer): 380k điểm (spent 120k on bike)
- Admin Commission: ~11k điểm (5% từ 2 sales)
- Platform: Working perfectly ✅
```

---

## 📊 Kết Quả Của Demo

| Entity | Điểm Ban Đầu | Điểm Cuối | Thay Đổi | Chi Tiết |
|--------|------|-------|---------|---------|
| **Buyer_1** | 0 | 400k | +400k | Nạp 500k, mua 100k |
| **Buyer_2** | 0 | 380k | +380k | Nạp 500k, mua 120k |
| **Seller_1** | 0 | 400k | +400k | Kiếm 114k từ sale, rút 114k chờ duyệt |
| **Admin** | 0 | ~6k | +6k | Commission từ sales |
| **Platform** | 0 | ~30k | +30k | Tất cả withdrawals pending |

---

## 🚀 Tips Khi Demo

1. **Screenshot mỗi bước** - Để chứng minh flow
2. **Highlight key moments:**
   - Step 10: Upgrade thành SELLER
   - Step 15: Admin approve listing
   - Step 20: Seller kiếm tiền
   - Step 28: Admin approve withdrawal

3. **Giải thích:**
   - Points như "xò" trong LOL
   - Buyer deposit tiền → nhận xò
   - Seller kiếm xò từ bán hàng
   - Admin fee là 5% mỗi sale
   - Withdrawal phải admin duyệt

4. **Khiên points trước khi bắt đầu:**
   - Xóa database record cũ (nếu có)
   - Hoặc dùng bikeId/userId khác nhau mỗi demo

---

## ❓ FAQ Khi Demo

**Q: Sao buyer không thể mua nếu bikeId không tồn tại?**
A: Phải tạo bike trước hoặc dùng bikeId từ database

**Q: Sao points bị trừ khi approve order?**
A: Đó là cơ chế escrow - points bị khóa khi tạo order, trừ vĩnh viễn khi approve

**Q: Seller có thể rút sao nếu chưa kiếm điểm?**
A: Lỗi "InsufficientBalanceException" sẽ được throw

**Q: Admin fee là bao nhiêu?**
A: 5% mỗi sale (hardcoded trong code hiện tại)

---

**Bắt đầu demo ngay! 🚀**

Good luck! 🎉
