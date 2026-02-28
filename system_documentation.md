# Tài liệu Luồng Hệ thống & Nghiệp vụ BikeExchange

Tài liệu này tổng hợp toàn bộ các luồng nghiệp vụ chính, các API tương ứng và quy trình xử lý dữ liệu trong hệ thống BikeExchange.

---

## 1. Luồng Người dùng & Tài khoản (Authentication)

Hệ thống sử dụng JWT để xác thực. Khi đăng ký, một Ví (Wallet) sẽ tự động được tạo cho người dùng.

### 1.1 Đăng ký tài khoản
*   **API**: `POST /api/auth/register`
*   **Body**:
    ```json
    {
      "email": "user@example.com",
      "password": "password123",
      "fullName": "Nguyen Van A",
      "phone": "0901234567",
      "address": "TP.HCM"
    }
    ```
*   **Nghiệp vụ**: Tạo User mới với quyền `BUYER` mặc định + Tự động tạo `UserWallet` (0 điểm).

### 1.2 Đăng nhập
*   **API**: `POST /api/auth/login`
*   **Body**: `{"email": "...", "password": "..."}`
*   **Response**: Trả về `accessToken` dạng JWT. Cần đính kèm vào Header `Authorization: Bearer <token>` cho các API sau.

---

## 2. Luồng Ví & Điểm (Wallet & Points)

Điểm (Points) là đơn vị thanh toán chính trong hệ thống. 1 điểm tương đương với giá trị quy đổi (Ví dụ: 1.000 VNĐ).

### 2.1 Nạp tiền (Deposit)
*   **API**: `POST /api/wallet/deposit`
*   **Body**: `{"amount": 500, "referenceId": "MOMO_123"}`
*   **Nghiệp vụ**: Cộng trực tiếp vào `availablePoints` và lưu lịch sử giao dịch.

### 2.2 Rút tiền (Withdraw)
*   **API Yêu cầu**: `POST /api/wallet/withdraw-request`
*   **Body**:
    ```json
    {
      "amount": 500,
      "bankName": "Vietcombank",
      "bankAccountName": "NGUYEN VAN A",
      "bankAccountNumber": "0123456789"
    }
    ```
*   **Nghiệp vụ**: 
    1. Kiểm tra số dư `availablePoints`.
    2. Trừ `availablePoints` và cộng vào `frozenPoints` (phong tỏa).
    3. Tạo giao dịch trạng thái `PENDING`.
    4. Chờ Admin duyệt.

### 2.3 Lịch sử giao dịch
*   **API**: `GET /api/wallet/transactions?type=DEPOSIT&type=WITHDRAW`
*   **Nghiệp vụ**: Xem danh sách biến động số dư, hỗ trợ lọc theo loại giao dịch.

---

## 3. Luồng Quản lý Xe (Bike Management)

### 3.1 Đăng bán xe
*   **API**: `POST /api/bikes`
*   **Nghiệp vụ**: Người bán đăng tin. Xe mới đăng sẽ có trạng thái `ACTIVE`. Nếu được kiểm định thành công sẽ thành `VERIFIED`.

### 3.2 Tìm kiếm & Lọc
*   **Lọc theo danh mục**: `GET /api/categories/{id}/bikes?page=0&size=20`
*   **Tìm kiếm tổng hợp**: `GET /api/bikes?brandId=1&status=VERIFIED&minPrice=100`

---

## 4. Luồng Kiểm định Xe (Inspection Flow) - QUAN TRỌNG

Đây là quy trình đảm bảo chất lượng xe trước khi bán.

### Bước 1: Người bán yêu cầu kiểm định
*   **API**: `POST /api/inspections?sellerId=1`
*   **Body**: `{"bikeId": 5}`
*   **Phí**: Hệ thống tự động phong tỏa (Freeze) **100 điểm** từ ví người bán để làm phí kiểm định.
*   **Trạng thái đơn**: `REQUESTED`.

### Bước 2: Chỉ định Kiểm định viên (Admin/Inspector)
*   **API**: `PUT /api/inspections/{inspectionId}?status=ASSIGNED&inspectorId=2`
*   **Trạng thái đơn**: `ASSIGNED`.

### Bước 3: Nộp báo cáo kiểm định (Inspector)
*   **API**: `POST /api/inspections/{inspectionId}/report?inspectorId=2`
*   **Body**: Gửi các đánh giá về Frame, Groupset, Wheel, Score và tối đa 5 ảnh minh họa.
*   **Trạng thái đơn**: `INSPECTED`.

### Bước 4: Admin duyệt báo cáo
*   **API**: `POST /api/inspections/{inspectionId}/approve`
*   **Kết quả**:
    1.  **Xe (Bike)**: Chuyển trạng thái sang `VERIFIED`.
    2.  **Ví người bán**: Trừ hẳn 100 điểm đã phong tỏa.
    3.  **Ví kiểm định viên**: Nhận 80% phí (80 điểm), 20% còn lại là phí hệ thống.
    4.  **Lịch sử**: Lưu vào bảng `History`.

---

## 5. Luồng Mua hàng & Thanh toán (Order/Escrow Flow)

Hệ thống sử dụng cơ chế **Escrow (Giao dịch đảm bảo)**.

1.  **Người mua đặt hàng**: Hệ thống phong tỏa số tiền tương ứng với giá xe trong ví người mua.
    *   Trạng thái Order: `ESCROWED`.
    *   Trạng thái Xe: `RESERVED`.
2.  **Người mua xác nhận đã nhận hàng**:
    *   Giải ngân tiền từ ví người mua sang ví người bán (sau khi trừ 5% hoa hồng hệ thống).
    *   Trạng thái Order: `COMPLETED`.
    *   Trạng thái Xe: `SOLD`.

---

## 6. Luồng Chat & Nhắn tin (Chat & Messaging)

Hệ thống hỗ trợ cả REST và WebSocket (STOMP).

*   **Tạo/Lấy hội thoại**: `POST /chat/conversations` (Dùng khi người mua nhấn "Chat với người bán").
*   **Gửi tin nhắn**: `POST /chat/messages`.
*   **Danh sách hội thoại**: `GET /chat/conversations`.
*   **Chi tiết tin nhắn**: `GET /chat/conversations/{conversationId}/messages`.

---

## 7. Luồng Trị an & Lịch sử (Dispute & History)

*   **Khiếu nại (Dispute)**: Nếu có vấn đề sau khi nhận hàng, người mua có thể tạo khiếu nại trước khi nhấn xác nhận.
*   **Lịch sử (History)**: Mọi thay đổi trạng thái của Xe, Đơn hàng, Kiểm định đều được lưu lại để truy vết.
    *   Xem lịch sử chi tiết 1 đơn kiểm định: `GET /api/inspections/{inspectionId}` (Trả về bao gồm thông tin đơn, báo cáo và lịch sử thay đổi).

---

## 7. Luồng Quản trị (Admin Management)

Danh cho tài khoản có quyền `ADMIN`.

### 7.1 Quản lý Rút tiền (Withdrawal Management)
*   **Xem danh sách**: `GET /api/admin/withdrawals?status=PENDING`
    *   Hỗ trợ lọc theo trạng thái: `PENDING`, `SUCCESS`, `FAILED`.
*   **Duyệt lệnh (Approve)**: `POST /api/admin/withdrawals/{transactionId}/approve`
    *   **Nghiệp vụ**: Chuyển trạng thái giao dịch sang `SUCCESS`, trừ `frozenPoints`.
*   **Từ chối lệnh (Reject)**: `POST /api/admin/withdrawals/{transactionId}/reject?reason=...`
    *   **Nghiệp vụ**: Chuyển trạng thái giao dịch sang `FAILED`, lưu lý do vào `remarks`, giải tỏa `frozenPoints` và **hoàn tiền** về `availablePoints` cho người dùng.

### 7.2 Dashboard & Thống kê
*   **API**: `GET /api/admin/dashboard`
*   **Nghiệp vụ**: Xem tổng quan số lượng người dùng, tin đăng, đơn hàng và doanh thu hệ thống.

---

## 8. Các Mã lỗi Thường gặp

*   `400 Bad Request`: Thiếu dữ liệu hoặc vi phạm logic nghiệp vụ.
*   `401 Unauthorized`: Token hết hạn hoặc không hợp lệ.
*   `403 Forbidden`: ID người bán không khớp với chủ sở hữu xe.
*   `404 Not Found`: Không tìm thấy Ví, Xe hoặc Báo cáo.
*   `InsufficientBalanceException`: Không đủ tiền trong ví (Available points < phí yêu cầu).
