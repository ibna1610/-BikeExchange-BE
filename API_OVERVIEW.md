# Tài liệu tóm tắt API

Tập hợp các API quan trọng cho tính năng nạp sò (wallet) và mua hàng (order) bằng sò.

**Mục tiêu:**
- Mô tả endpoints để nạp, kiểm tra ví, xem lịch sử giao dịch và rút tiền
- Mô tả endpoints tạo đơn, xác nhận đơn (escrow → release)
- Mô tả luồng nạp tiền qua VNPAY (ví dụ đã implement)

**File tham chiếu:**
- Controller xử lý ví: [src/main/java/com/bikeexchange/controller/WalletController.java](src/main/java/com/bikeexchange/controller/WalletController.java#L1-L200)
- Controller xử lý order: [src/main/java/com/bikeexchange/controller/OrderController.java](src/main/java/com/bikeexchange/controller/OrderController.java#L1-L200)
- Service VNPAY: [src/main/java/com/bikeexchange/service/VnPayService.java](src/main/java/com/bikeexchange/service/VnPayService.java#L1-L200)

---

**1. Wallet (Ví / Sò)**

- GET /wallet
  - Mô tả: Lấy thông tin ví của user (availablePoints, frozenPoints,...)
  - Auth: optional (nếu không có token cần truyền `userId` query)
  - Params: `userId` (optional nếu đã login)
  - Response: `success`, `data` = `UserWallet` object

- GET /wallet/transactions
  - Mô tả: Lấy lịch sử giao dịch điểm
  - Query params: `userId` (optional), `type` (list of types: DEPOSIT, WITHDRAW, SPEND, EARN, ESCROW_HOLD, ESCROW_RELEASE, COMMISSION)
  - Response: danh sách `PointTransaction` và `summary`

- POST /wallet/deposit
  - Mô tả: Ghi nhận nạp sò (sử dụng sau khi thanh toán thực tế thành công)
  - Auth: Bearer required
  - Body (DepositRequest):
    ```json
    {
      "amount": 1000,           // số sò (points)
      "referenceId": "txn-abc-123" // id giao dịch bên cổng thanh toán
    }
    ```
  - Hành vi: tăng `availablePoints`, tạo `PointTransaction` type=DEPOSIT status=SUCCESS

- POST /wallet/withdraw-request
  - Mô tả: Tạo yêu cầu rút tiền (admin duyệt sau)
  - Auth: Bearer
  - Body (WithdrawRequest): thông tin ngân hàng và `amount`
  - Hành vi: trừ `availablePoints`, tăng `frozenPoints`, tạo `PointTransaction` type=WITHDRAW status=PENDING

**Ngoại lệ thường gặp:** `ResourceNotFoundException`, `InsufficientBalanceException`.

---

**2. Orders (Mua hàng bằng sò)**

- POST /orders
  - Mô tả: Buyer tạo order để mua bike; hệ thống sẽ escrow (khóa) sò từ `availablePoints` → `frozenPoints`.
  - Auth: Bearer
  - Body (`OrderCreateRequest`):
    ```json
    {
      "bikeId": 10,
      "idempotencyKey": "uuid-generated-by-client"
    }
    ```
  - Hành vi chính (OrderService.createOrder):
    1. Kiểm tra idempotency: nếu `idempotencyKey` đã tồn tại, reject.
    2. Khóa record `Bike` (FOR UPDATE), kiểm tra trạng thái `ACTIVE` hoặc `VERIFIED`.
    3. Khóa `UserWallet` buyer, kiểm tra `availablePoints` >= `pricePoints`.
    4. Di chuyển points: `availablePoints -= price`, `frozenPoints += price`.
    5. Thay đổi `Bike` → `RESERVED`, tạo `Order` với `status = ESCROWED`.
    6. Ghi `PointTransaction` loại `ESCROW_HOLD`.
  - Response: order data, status `ESCROWED`.

- POST /orders/{id}/approve
  - Mô tả: Buyer xác nhận đã nhận hàng → giải phóng escrow, trả tiền cho seller (trừ hoa hồng 5%)
  - Auth: Bearer (chỉ buyer mới được phép)
  - Hành vi chính (OrderService.approveOrder):
    1. Khóa order FOR UPDATE, kiểm tra buyer match và order status == ESCROWED.
    2. Buyer `frozenPoints -= total`.
    3. Seller `availablePoints += total - adminCommission`(adminCommission = 5%).
    4. Tạo `PointTransaction` cho seller (type=EARN)
    5. Update `Order` → `COMPLETED`, `Bike` → `SOLD`.
  - Response: order data, status `COMPLETED`.

**Lưu ý quan trọng:** Sử dụng `idempotencyKey` khi tạo order để tránh duplicate khi người dùng bấm nhiều lần.

---

**3. Nạp tiền qua VNPAY (ví dụ đã cài sẵn)**

- Luồng tóm tắt (xem `VnPayService`):
  1. Frontend gọi backend để tạo `vnpUrl` (VnPayService.generatePaymentUrl) với `amountVnd` và `userId`.
  2. User thanh toán trên VNPAY, VNPAY redirect về `vnp_ReturnUrl` kèm tham số.
  3. Backend verify chữ ký (VnPayService.verifySignature) và nếu hợp lệ gọi `depositIfNotProcessed(userId, amountVnd, referenceId)`.
  4. `depositIfNotProcessed` kiểm tra idempotency bằng `referenceId` (PointTransaction.referenceId) rồi gọi `WalletService.depositPoints(userId, points, referenceId)`.
  5. Mapping VND → points: hiện tại mã nguồn chia `amountVnd / 1000` để ra `points`.

**Chú ý cấu hình:** xem `VNPAYConfig` để biết `vnp_ReturnUrl`, `tmnCode`, `hashSecret`.

---

**4. Các DTO & Model liên quan (tham khảo)**
- `DepositRequest` (amount, referenceId)
- `WithdrawRequest` (amount, bankName, bankAccountName, bankAccountNumber)
- `OrderCreateRequest` (bikeId, idempotencyKey)
- `PointTransaction` model (type, status, amount, referenceId)
- `UserWallet` (availablePoints, frozenPoints)

**Đường dẫn code:**
- [src/main/java/com/bikeexchange/dto/request/DepositRequest.java](src/main/java/com/bikeexchange/dto/request/DepositRequest.java#L1-L50)
- [src/main/java/com/bikeexchange/dto/request/OrderCreateRequest.java](src/main/java/com/bikeexchange/dto/request/OrderCreateRequest.java#L1-L50)

---

**5. Ví dụ request / response ngắn**

- Tạo order (POST /orders):

Request:
```json
{
  "bikeId": 10,
  "idempotencyKey": "333e4444-e89b-12d3-a456-426614174000"
}
```

Response (200):
```json
{
  "success": true,
  "message": "Order created and points escrowed successfully",
  "data": { /* order info with status=ESCROWED */ }
}
```

- Deposit (POST /wallet/deposit):

Request:
```json
{
  "amount": 1000,
  "referenceId": "VNPAY-XYZ-123"
}
```

Response (200):
```json
{
  "success": true,
  "message": "Deposit successful",
  "data": { /* updated wallet */ }
}
```

---

**6. Gợi ý kiểm thử nhanh**
- Tạo user test, gọi `POST /wallet/deposit` để nạp 2000 sò.
- Gọi `POST /orders` với bike có giá 1500 sò → xác nhận ví giảm available → frozen tăng.
- Gọi `POST /orders/{id}/approve` → kiểm tra seller nhận 1500 - 5%.

---

Nếu bạn muốn tôi thêm các endpoint khác (Admin APIs để duyệt rút, chi tiết schema DTO, hoặc generate OpenAPI/Swagger partial), hãy cho biết tôi sẽ mở rộng file này.
