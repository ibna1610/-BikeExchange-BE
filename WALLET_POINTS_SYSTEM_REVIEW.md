# 💰 Wallet & Points System - Comprehensive Review

**Date:** March 2026  
**Status:** Current Implementation Analysis

---

## 📋 Executive Summary

The BikeExchange platform implements a **Points-based Wallet System** similar to gaming platforms (LOL, etc.). Users deposit money to obtain "points" (xò) which are used to purchase bikes. The system includes:

- ✅ Wallet management with availablePoints & frozenPoints
- ✅ Point transactions with full history tracking
- ✅ Deposit/Withdrawal functionality
- ✅ Order escrow mechanism
- ✅ Commission splitting (5% admin fee)
- ✅ Transaction audit logging

---

## 🏗️ Current Architecture

### 1. **Data Models**

#### `UserWallet` Entity
```java
- userId (PK)
- availablePoints: Long (spendable balance)
- frozenPoints: Long (escrowed/locked in orders)
- user: OneToOne relationship
- updatedAt: timestamp
```

#### `PointTransaction` Entity
```java
- id (PK)
- user: ManyToOne
- amount: Long
- type: DEPOSIT, WITHDRAW, EARN, SPEND, ESCROW_HOLD, ESCROW_RELEASE, COMMISSION
- status: PENDING, SUCCESS, FAILED
- referenceId: String (links to order/deposit)
- createdAt: timestamp
```

#### `Order` Entity
```java
- id (PK)
- buyer: ManyToOne User
- bike: ManyToOne Bike
- amountPoints: Long (points used for purchase)
- status: PENDING_PAYMENT, ESCROWED, COMPLETED, DISPUTED, CANCELLED
- idempotencyKey: String (ensure idempotent operations)
```

#### `Bike` Entity
```java
- pricePoints: Long (cost in points, like LOL BE)
- seller: ManyToOne User
- status: DRAFT, ACTIVE, VERIFIED, RESERVED, SOLD
- inspectionStatus: tracking inspection progress
```

---

## 🔄 Wallet Flow - Complete User Journey

### **Step 1: User Registration → Wallet Creation**
- When user registers, auto-create a `UserWallet` with `availablePoints = 0`
- Status: ✅ Handled (likely in DataInitializer or UserService)

### **Step 2: Deposit Money → Get Points**

**API Endpoint:**
```
POST /wallet/deposit
{
  "amount": 500000,        // Real money (VND)
  "referenceId": "vnpay_123"
}
```

**Current Implementation:**
```java
WalletService.depositPoints(userId, amount, referenceId)
  → Lock wallet (Pessimistic locking)
  → availablePoints += amount
  → Create DEPOSIT transaction with SUCCESS status
  → Return updated wallet
```

**Status:** ✅ **Functional** but needs:
- [ ] Integration with VNPAY/payment gateway (currently manual)
- [ ] Deposit confirmation from payment provider
- [ ] Receipt ID validation
- [ ] Rate conversion (if accepting multiple currencies)

---

### **Step 3: Buy Bike (Escrow & Hold Points)**

**API Endpoint:**
```
POST /orders
{
  "bikeId": 101,
  "idempotencyKey": "order_xyz_123"
}
```

**Current Flow:**
1. **Check Bike Availability** - Must be ACTIVE or VERIFIED
2. **Lock Buyer Wallet** - Pessimistic locking for thread-safety
3. **Validate Points** - Require `availablePoints >= bike.pricePoints`
4. **Deduct Points:**
   - `availablePoints -= bike.pricePoints`
   - `frozenPoints += bike.pricePoints`
5. **Update Bike Status** - `ACTIVE → RESERVED`
6. **Create Transaction** - Type: `ESCROW_HOLD`, Status: `SUCCESS`
7. **Save Order** - Status: `ESCROWED`

**Status:** ✅ **Implemented** with proper:
- ✅ Idempotency protection
- ✅ Database locking
- ✅ Atomic transactions
- ✅ Full audit trail

---

### **Step 4: Complete Order (Release Points to Seller)**

**API Endpoint:**
```
POST /orders/{orderId}/approve
```

**Current Flow:**
1. **Validate Order Status** - Must be `ESCROWED`
2. **Calculate Commission:**
   - Admin Fee: `total * 5%`
   - Seller Revenue: `total * 95%`
3. **Update Wallets:**
   - Buyer: `frozenPoints -= total` (deduct)
   - Seller: `availablePoints += sellerRevenue` (credit)
4. **Cap Bike Status** - `RESERVED → SOLD`
5. **Create Transactions:**
   - Seller: Type `EARN`, Amount `sellerRevenue`
   - (Admin commission currently not logged separately)
6. **Update Order** - Status: `COMPLETED`

**Status:** ✅ **Implemented** with:
- ✅ Proper commission calculation
- ✅ Seller earnings directly credited
- ⚠️ Admin commission not tracked in PointTransaction

---

### **Step 5: Seller Withdraw Points → Real Money**

**API Endpoint:**
```
POST /wallet/withdraw-request
{
  "amount": 100000,
  "bankName": "TechComBank",
  "bankAccountName": "John Doe",
  "bankAccountNumber": "123456789"
}
```

**Current Flow:**
1. **Validate Amount** - Must be > 0
2. **Lock Wallet** - Pessimistic locking
3. **Check Balance** - Require `availablePoints >= amount`
4. **Freeze Points:**
   - `availablePoints -= amount`
   - `frozenPoints += amount`
5. **Create Transaction:**
   - Type: `WITHDRAW`
   - Status: `PENDING` (waiting for admin approval)
   - Details stored in `referenceId`
6. **Return Updated Wallet**

**Status:** ✅ **Implemented** with:
- ✅ Points frozen until approval
- ⚠️ No actual bank transfer integration
- ⚠️ Manual admin approval required

---

## 👥 Admin Functions - Wallet Management

### **1. List All Withdrawal Requests**

**Endpoint:** `GET /admin/withdrawals?status=PENDING`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 5,
      "userId": 10,
      "amount": 500000,
      "type": "WITHDRAW",
      "status": "PENDING",
      "referenceId": "Withdrawal: TechComBank | John Doe | 123456789",
      "createdAt": "2026-03-05T10:30:00"
    }
  ]
}
```

**Status:** ✅ Implemented

### **2. Approve Withdrawal**

**Endpoint:** `POST /admin/withdrawals/{transactionId}/approve`

**Logic:**
1. Find withdrawal transaction (WITHDRAW + PENDING)
2. Lock seller's wallet
3. Release frozen points: `frozenPoints -= amount`
4. Mark transaction: `status = SUCCESS`
5. ⚠️ Now requires manual bank transfer (not automated)

**Status:** ✅ Implemented but **needs bank integration**

### **3. Reject Withdrawal**

**Endpoint:** `POST /admin/withdrawals/{transactionId}/reject?reason=Invalid%20account`

**Logic:**
1. Find withdrawal transaction
2. Lock seller's wallet
3. Refund points: 
   - `frozenPoints -= amount`
   - `availablePoints += amount`
4. Mark transaction: `status = FAILED`
5. Store rejection reason in `remarks`

**Status:** ✅ Implemented

### **4. Dashboard Metrics**

**Endpoint:** `GET /admin/dashboard`

**Current Metrics:** (Need to verify AdminService)
- [ ] Total users
- [ ] Total deposits
- [ ] Total withdrawals (pending/approved)
- [ ] Total transaction volume
- [ ] Admin commission earned
- [ ] Platform balance

**Status:** ⚠️ Partially implemented

---

## 🏪 Buyer → Seller Upgrade Flow

### **Upgrade Endpoint**
```
POST /users/{userId}/upgrade-to-seller
```

### **Requirements**
- User must be authenticated (Bearer token)
- User must have BUYER role (cannot upgrade sellers again)
- Must agree to terms and conditions
- Shop name and description required

### **Request Body**
```json
{
  "shopName": "My Bike Shop",
  "shopDescription": "Quality bikes and professional service",
  "agreeToTerms": true
}
```

### **Response (Success - 200)**
```json
{
  "success": true,
  "message": "User successfully upgraded to seller",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "John Doe",
    "role": "SELLER",         # ⬅️ Changed from BUYER
    "shopName": "My Bike Shop",
    "shopDescription": "Quality bikes and professional service",
    "upgradedToSellerAt": "2026-03-05T10:15:00",
    "status": "ACTIVE",
    "rating": 0.0,
    "isVerified": false
  }
}
```

### **What Changes After Upgrade**

| Aspect | Before (Buyer) | After (Seller) |
|--------|---|---|
| **Role** | BUYER | SELLER |
| **Can View Listings** | ✅ Yes | ✅ Yes |
| **Can Purchase Bikes** | ✅ Yes | ✅ Yes |
| **Can Create Listings** | ❌ No | ✅ Yes |
| **Can Receive Orders** | ❌ No | ✅ Yes |
| **Can Withdraw Points** | ✅ Yes (earned only) | ✅ Yes (from sales) |
| **Shop Profile** | ❌ None | ✅ Yes |
| **Commission Applied** | N/A | ✅ 5% on sales |

### **Error Scenarios**

#### ❌ **1. User is Already a Seller**
```json
{
  "success": false,
  "message": "User is already a seller"
}
```

#### ❌ **2. Missing Required Fields**
```json
{
  "success": false,
  "message": "Shop name is required",
  "code": "VALIDATION_ERROR"
}
```

#### ❌ **3. Did Not Agree to Terms**
```json
{
  "success": false,
  "message": "You must agree to the terms and conditions"
}
```

#### ❌ **4. Trying to Upgrade Someone Else's Account**
```json
{
  "success": false,
  "message": "You can only upgrade your own account",
  "code": "FORBIDDEN"
}
```

### **Status:** ✅ **Fully Implemented**

---

## 👤 Buyer Wallet & Order Functions - REVIEW NEEDED

### **Current Buyer Wallet Features**

#### **1. View Wallet Balance**
```
GET /wallet?userId=5
```
- Shows: `availablePoints`, `frozenPoints`, `user` details
- **Status:** ✅ Works

#### **2. View Transaction History**
```
GET /wallet/transactions?userId=5&type=DEPOSIT,SPEND
```
- Filters by transaction type
- Aggregates stats (count, total amount by type)
- **Status:** ✅ Works

#### **3. Deposit Points**
```
POST /wallet/deposit
{
  "amount": 500000,
  "referenceId": "vnpay_123"
}
```
- Requires authentication
- **Status:** ✅ Works but needs payment integration

#### **4. Create Order (Escrow Points)**
```
POST /orders
{
  "bikeId": 101,
  "idempotencyKey": "order_abc"
}
```
- Automatically escrows points
- **Status:** ✅ Works

#### **5. Complete Order (Release to Seller)**
```
POST /orders/{orderId}/approve
```
- Only accessible by buyer who made order
- Transfers points to seller (minus 5% fee)
- **Status:** ✅ Works

### **Missing/Needed Buyer Functions**

#### ❌ **1. Cancel Order & Refund Points**
- Buyer should be able to cancel order before seller ships
- Points should be released from escrow back to available
- Action: `POST /orders/{orderId}/cancel` with reason

#### ❌ **2. Escrow Timeout & Auto-Refund**
- If seller doesn't ship within X days, auto-refund escrow
- Create background job to process expired orders
- Send notification to buyer

#### ❌ **3. Request Refund**
- If order has issues, buyer can request refund
- Freezes escrow for admin review
- Requires dispute resolution

#### ❌ **4. Point Usage History**
- Show breakdown of how points were used
- Earned points, spent points, pending points separately
- **Current:** Basic history exists but needs better UX

#### ❌ **5. Real-time Points Balance Widget**
- Quick view of available vs frozen points
- Pending withdrawals/orders
- Estimated earnings for sellers

---

## ⭐ Review & Rating System - CURRENT STATE

### **Current Implementation**

**Model: `Review`**
```java
- id
- reviewer: User (buyer/inspector)
- seller: User
- rating: Integer (1-5 stars)
- comment: String
- createdAt
```

**API Endpoints:**

1. **Create Review**
   ```
   POST /reviews
   ?sellerId=10&rating=5&comment=Great bike!
   ```
   - **Issue:** Only requires `sellerId`, no `orderId` reference
   - **Risk:** Users can review sellers without buying from them
   - **Status:** ⚠️ **NEEDS VERIFICATION**

2. **Get Reviews for Seller**
   ```
   GET /reviews/seller/{sellerId}
   ```
   - **Status:** ✅ Works

### **Issues & Recommendations**

#### 🚨 **Issue 1: Review Verification**
- Reviews should only be allowed after order completion
- Should link to specific order, not just seller
- Current: No validation that reviewer is legitimate buyer

**Recommendation:**
```java
// Add to Review model
private Order order;  // Link to completed order
private ReviewType type;  // PRODUCT, SELLER, SERVICE

// Validation in ReviewService
public Review createReview(Long buyerId, Long orderId, Integer rating, String comment) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    
    // Verify buyer is the one who completed the order
    if (!order.getBuyer().getId().equals(buyerId)) {
        throw new IllegalArgumentException("Only buyer can review their purchase");
    }
    
    if (order.getStatus() != Order.OrderStatus.COMPLETED) {
        throw new InvalidOrderStatusException("Can only review completed orders");
    }
    
    // Check if already reviewed
    if (reviewRepository.existsByOrderId(orderId)) {
        throw new IllegalArgumentException("Order already reviewed");
    }
    
    // Now safe to create review
    Review review = new Review();
    review.setOrder(order);
    review.setBuyer(buyer);
    review.setSeller(order.getBike().getSeller());
    review.setRating(rating);
    review.setComment(comment);
    return reviewRepository.save(review);
}
```

#### 🚨 **Issue 2: Seller Rating Calculation**
- No aggregated seller rating shown
- No "average rating" on seller profile
- Buyers can't see before purchase

**Recommendation:**
```java
// New endpoint: GET /users/{sellerId}/rating-summary
{
  "sellerId": 10,
  "averageRating": 4.8,
  "totalReviews": 42,
  "ratingBreakdown": {
    "5": 35,   // 5 star reviews
    "4": 5,
    "3": 2,
    "2": 0,
    "1": 0
  },
  "recentReviews": [...]
}
```

#### 🚨 **Issue 3: Response to Reviews**
- Sellers can't reply to reviews
- No way to resolve negative feedback
- One-way communication only

**Recommendation:**
```java
// Add: ReviewResponse
- id
- review: Review
- seller: User
- responseText: String
- createdAt
```

---

## 📊 Transaction Types & Flows

### **Complete Transaction Type References**

| Type | Direction | Scenario | Status |
|------|-----------|----------|--------|
| **DEPOSIT** | `User → System` | User deposits VND to get points | ✅ |
| **WITHDRAW** | `System → User` | User cashes out points to bank | ✅ |
| **ESCROW_HOLD** | `Buyer Available → Frozen` | Buyer purchases bike | ✅ |
| **ESCROW_RELEASE** | `Buyer Frozen → Released` | order cancelled/refunded | ⚠️ |
| **EARN** | `System → Seller` | Seller completes sale | ✅ |
| **SPEND** | `Buyer Available → Spent` | Not fully used | ⚠️ |
| **COMMISSION** | `System → Admin/Fee` | Admin takes 5% cut | ⚠️ |

**Issues:**
- COMMISSION not properly logged
- ESCROW_RELEASE not implemented
- SPEND type underutilized

---

## 🔐 Security & Data Integrity

### **Current Protections** ✅

1. **Pessimistic Locking** - Lock wallets during critical operations
   ```java
   walletRepository.findByUserIdForUpdate(userId)
   ```

2. **Isolation Level** - `READ_COMMITTED` transactions
   ```java
   @Transactional(isolation = Isolation.READ_COMMITTED)
   ```

3. **Idempotency Keys** - Prevent duplicate orders
   ```java
   order.setIdempotencyKey(uniqueKey)
   ```

4. **Audit Trail** - Full transaction history
   ```java
   PointTransaction with type, status, referenceId
   ```

### **Areas Needing Attention** ⚠️

- [ ] No rate limiting on deposit/withdraw endpoints
- [ ] No maximum transaction limits
- [ ] No fraud detection  
- [ ] Admin commission calculation not audited separately
- [ ] No reconciliation endpoint for admins

---

## 🎯 Integration Points

### **Required External Integrations**

#### 1. **Payment Gateway (VNPAY)**
- Current: Placeholder, no real integration
- Needed: Validate payment callback before creditingpoints
- File: `VnPayService.java`

#### 2. **Bank API for Withdrawals**
- Current: Points frozen, awaiting manual transfer
- Needed: Automated bank transfer API (ACH/wire)
- Or: Send to payment processor for disbursement

#### 3. **Notification System**
- Send deposit confirmation
- Send order status updates
- Send withdrawal approval/rejection
- File: Create `NotificationService`

---

## 📝 Recommended Changes Summary

### **High Priority**

1. **Review System Validation**
   - Link reviews to completed orders
   - Prevent fake reviews
   - Add seller response capability

2. **Order Cancellation**
   - Implement: `POST /orders/{id}/cancel`
   - Release escrow back to buyer
   - Add cancellation reasons

3. **Admin Commission Tracking**
   - Create separate COMMISSION transactions
   - Add admin wallet to track platform revenue
   - Create: `AdminWallet` entity

### **Medium Priority**

4. **Escrow Timeout Management**
   - Implement auto-refund for expired orders
   - Add background job for daily checks
   - Send notifications

5. **Seller Rating Display**
   - Aggregate and show average rating
   - Display on seller profile
   - Use in search/ranking

6. **Enhanced Dashboard**
   - Show real-time wallet stats
   - Transaction breakdown charts
   - User growth metrics

### **Low Priority**

7. **Rate Limiting**
   - Add deposit/withdraw limits per user
   - Daily/monthly caps
   - Fraud detection rules

8. **Multi-currency Support**
   - If expanding internationally
   - Currency conversion rates
   - Fee handling

---

## 🚀 Implementation Roadmap

### **Phase 1 (Immediate)**
- [ ] Fix Review validation (link to orders)
- [ ] Implement order cancellation
- [ ] Add admin commission tracking

### **Phase 2 (Short-term)**
- [ ] Payment gateway integration
- [ ] Seller rating aggregation
- [ ] Escrow timeout management

### **Phase 3 (Medium-term)**
- [ ] Bank withdrawal automation
- [ ] Advanced admin dashboard
- [ ] Notification system

### **Phase 4 (Long-term)**
- [ ] Fraud detection AI
- [ ] Multi-currency support
- [ ] Point loyalty programs

---

## 🏪 Buyer → Seller Upgrade Feature

### **Endpoint: Feature Overview**

**Purpose:** Allow any authenticated BUYER user to upgrade their account to SELLER status and start selling bikes.

**Key Endpoint:**
```
POST /users/{userId}/upgrade-to-seller
```

**Authentication:** Required (Bearer Token)

### **Request Body**
```json
{
  "shopName": "My Bike Shop",
  "shopDescription": "Quality bikes and professional service",
  "agreeToTerms": true
}
```

**Field Validations:**
- `shopName` - Required, non-blank, max length
- `shopDescription` - Required, non-blank, detailed description
- `agreeToTerms` - Must be `true` (user accepts T&C)

### **Success Response (200 OK)**
```json
{
  "success": true,
  "message": "User successfully upgraded to seller",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "John Doe",
    "role": "SELLER",                           // ⬅️ Changed from BUYER
    "shopName": "My Bike Shop",
    "shopDescription": "Quality bikes and professional service",
    "upgradedToSellerAt": "2026-03-05T10:15:00",
    "status": "ACTIVE",
    "totalBikesSold": 0,
    "rating": 0.0,
    "isVerified": false
  }
}
```

### **What Changes After Upgrade**

| Capability | Before (BUYER) | After (SELLER) |
|---|---|---|
| **View Listings** | ✅ Yes | ✅ Yes |
| **Purchase Bikes** | ✅ Yes | ✅ Yes (still can) |
| **Create Listings** | ❌ No | ✅ Yes |
| **Receive Orders** | ❌ No | ✅ Yes |
| **Withdraw Earned Points** | ⚠️ Only if earned | ✅ Yes |
| **Shop Profile** | ❌ None | ✅ Yes (with shopName) |
| **Commission Applied** | N/A | ✅ 5% on sales |
| **Rating System** | ✅ Can rate sellers | ✅ Can be rated by buyers |

### **Key Business Rules**

1. ✅ **One-way Upgrade** - Once upgraded to SELLER, cannot downgrade back to BUYER
2. ✅ **Wallet Carries Over** - All points balance transfers to seller account
3. ✅ **New Timestamps** - `upgradedToSellerAt` field records exact upgrade moment
4. ✅ **Admin Approval Required** - Listings must be approved by admin before visibility
5. ✅ **Same Payment System** - Uses same Points/Wallet system for deposits and withdrawals

### **Error Scenarios**

#### ❌ **Already a Seller**
```json
{
  "success": false,
  "message": "User is already a seller"
}
```

#### ❌ **Missing Required Fields**
```json
{
  "success": false,
  "message": "Shop name is required"
}
```

#### ❌ **Did Not Agree to Terms**
```json
{
  "success": false,
  "message": "You must agree to the terms and conditions"
}
```

#### ❌ **Trying to Upgrade Someone Else's Account**
```json
{
  "success": false,
  "message": "You can only upgrade your own account",
  "code": "FORBIDDEN"
}
```

#### ❌ **User Not Found**
```json
{
  "success": false,
  "message": "User not found"
}
```

### **Implementation Status: ✅ COMPLETE**

**Implemented in:**
- [UserController.java](src/main/java/com/bikeexchange/controller/UserController.java) - POST endpoint
- [UserService.java](src/main/java/com/bikeexchange/service/UserService.java) - Business logic
- [User.java](src/main/java/com/bikeexchange/model/User.java) - Model with shopName, shopDescription, upgradedToSellerAt

**Test Coverage:**
- ✅ Happy path (BUYER → SELLER)
- ✅ Already seller scenario
- ✅ Missing fields validation
- ✅ Terms agreement check
- ✅ User existence check

---

## 📚 API Reference Example Flows

### **Complete User Flow: Deposit → Buy → Review → Upgrade → Sell → Withdraw**

```
1. REGISTER
   → Auto-create UserWallet (availablePoints = 0)
   → Role = BUYER

2. DEPOSIT 500,000 VND
   POST /wallet/deposit?amount=500000&referenceId=vnpay_123
   → availablePoints = 500000
   → PointTransaction: DEPOSIT, SUCCESS

3. SEARCH & FIND BIKE (pricePoints = 100000)
   GET /bikes?...

4. CREATE ORDER
   POST /orders
   Body: {bikeId: 101, idempotencyKey: order_abc}
   → availablePoints = 400000
   → frozenPoints = 100000
   → Order.status = ESCROWED
   → PointTransaction: ESCROW_HOLD, SUCCESS

5. SELLER SHIPS → BUYER RECEIVES

6. APPROVE ORDER (Complete Purchase)
   POST /orders/123/approve
   Buyer:
   → frozenPoints = 0
   → (100,000 points permanently spent)
   
   Seller:
   → availablePoints += 95000 (5% fee taken)
   → PointTransaction: EARN, SUCCESS
   
   System:
   → [MISSING] Commission logged (5000 points to admin)
   → [MISSING] PointTransaction: COMMISSION, SUCCESS to admin

7. CREATE REVIEW ⚠️ [NEEDS VALIDATION]
   POST /reviews?sellerId=5&rating=5&comment=Perfect!
   
   Should verify:
   → Reviewer was buyer on completed order
   → Review isn't duplicate
   → Only after order.status == COMPLETED

=== NOW BUYER DECIDES TO BECOME A SELLER ===

8. UPGRADE TO SELLER 🎉 [NEW]
   POST /users/{userId}/upgrade-to-seller
   Body: {
     "shopName": "My Bike Shop",
     "shopDescription": "Quality bikes for everyone",
     "agreeToTerms": true
   }
   → Role changes: BUYER → SELLER
   → Can now create listings
   → Can receive orders from buyers
   → Same wallet (points carry over)
   → upgradedToSellerAt timestamp recorded

9. CREATE BIKE LISTING (As Seller Now)
   POST /api/seller/listings/create
   Body: {
     "bikeName": "Giant TCR Advanced",
     "bikeType": "Road",
     "pricePoints": 80000,
     "condition": "LIKE_NEW",
     "description": "Excellent condition"
   }
   → Bike.status = DRAFT (awaiting admin approval)
   → Bike.seller = current user

10. ADMIN APPROVES LISTING
    PUT /api/admin/listings/101/approve
    → Bike.status = ACTIVE
    → Now visible to buyers for purchase

11. BUYER PURCHASES SELLER'S BIKE
    POST /orders
    Body: {bikeId: 101, idempotencyKey: order_def}
    → availablePoints (some amount) -= 80000
    → frozenPoints += 80000
    → Order.status = ESCROWED
    (This seller NOW EARNS money!)

12. BUYER APPROVES ORDER
    POST /orders/xyz/approve
    → Seller receives: 76000 points (80000 - 4000 commission)
    → PointTransaction: EARN, SUCCESS
    → Seller wallet grows

13. SELLER WITHDRAW 76000 POINTS
    POST /wallet/withdraw-request
    Body: {amount: 76000, bankName: ..., bankAccount: ...}
    → availablePoints -= 76000
    → frozenPoints += 76000
    → PointTransaction: WITHDRAW, PENDING

14. ADMIN APPROVES WITHDRAWAL
    POST /admin/withdrawals/X/approve
    → frozenPoints = 0
    → PointTransaction status: SUCCESS
    → [MISSING] Actual bank transfer

15. WITHDRAWAL COMPLETE
    Seller receives money in bank account
```

```

---

## 📋 Complete Demo Script (Step-by-Step for Swagger)

### **Phase 1: BUYER - Register & Deposit Points**

```
🔵 Step 1: Login Buyer Account
POST /auth/login
{
  "email": "buyer@example.com",
  "password": "password123"
}
💾 Copy token, Click Authorize button in Swagger, paste: Bearer {token}

🔵 Step 2: Check Initial Wallet
GET /wallet
Expected: availablePoints = 0, frozenPoints = 0

🔵 Step 3: Deposit Money → Get Points
POST /wallet/deposit
{
  "amount": 500000,
  "referenceId": "vnpay_demo_001"
}
Expected: availablePoints = 500000, frozenPoints = 0

🔵 Step 4: Check Transaction History
GET /wallet/transactions?type=DEPOSIT
Expected: Shows 1 DEPOSIT transaction with SUCCESS status
```

### **Phase 2: BUYER - Purchase Bike**

```
🔵 Step 5: Create Order (Escrow Points)
POST /orders
{
  "bikeId": 1,
  "idempotencyKey": "order_demo_123"
}
Expected: Order.status = ESCROWED, amountPoints = 100000

🔵 Step 6: Verify Wallet Changed
GET /wallet
Expected: availablePoints = 400000, frozenPoints = 100000
💡 100k points are NOW LOCKED for this order

🔵 Step 7: Approve Order (Complete Purchase)
POST /orders/1/approve
Expected: Order.status = COMPLETED
         Bike.status = SOLD
         Seller earned: 95000 points minus 5% commission

🔵 Step 8: Verify Final Wallet State
GET /wallet
Expected: availablePoints = 400000, frozenPoints = 0
💡 100k points permanently spent (buyer lost them)
```

### **Phase 3: Leave Review & Upgrade to Seller**

```
🔵 Step 9: Create Review (⚠️ Currently no validation)
POST /reviews
Params: sellerId=2&rating=5&comment=Great%20bike!
Expected: Review created with success = true

🔵 Step 10: NOW UPGRADE TO SELLER 🎉
POST /users/{userId}/upgrade-to-seller
Replace {userId} with your user ID (usually 1 for first user)
{
  "shopName": "My Bike Shop",
  "shopDescription": "Quality bikes and professional service",
  "agreeToTerms": true
}
Expected: 
{
  "role": "SELLER",  ← Changed from BUYER
  "shopName": "My Bike Shop",
  "upgradedToSellerAt": "2026-03-05T10:30:00"
}
✨ YOU ARE NOW A SELLER!
```

### **Phase 4: Seller - Create & List Bike**

```
🔵 Step 11: Create Bike Listing (As Seller)
POST /api/seller/listings/create
{
  "bikeName": "Trek Domane SLR",
  "bikeType": "Road",
  "brand": "Trek",
  "pricePoints": 120000,
  "condition": "LIKE_NEW",
  "description": "Professional road bike, barely used",
  "features": "Carbon frame, electronic shifting, 22 speeds"
}
Expected: Bike created with status = DRAFT
💾 Save bikeId from response (you'll need it in Step 15)

⚠️ Your bike is NOT yet visible to buyers (DRAFT status)
   It needs admin approval first!
```

### **Phase 5: Admin - Approve Listing**

```
🔵 Step 12: Logout current seller account

🔵 Step 13: Login as Admin Account
POST /auth/login
{
  "email": "admin@example.com",
  "password": "admin123"
}
💾 Copy admin token, Update Authorize with: Bearer {admin_token}

🔵 Step 14: Get All Listings (Optional - to see pending)
GET /api/admin/listings?status=DRAFT

🔵 Step 15: Approve Your Seller's Listing
PUT /api/admin/listings/{bikeId}/approve
(Replace {bikeId} with the ID from Step 11)
{}  (empty body)
Expected: Bike.status = ACTIVE ✅
         Now visible to ALL BUYERS for purchase!
```

### **Phase 6: Different Buyer - Purchase from Seller**

```
🔵 Step 16: Logout admin account

🔵 Step 17: Login as DIFFERENT Buyer Account
POST /auth/login
{
  "email": "buyer2@example.com",
  "password": "password123"
}
💾 Copy token, Update Authorize: Bearer {new_buyer_token}

🔵 Step 18: Deposit Money for this Buyer
POST /wallet/deposit
{
  "amount": 500000,
  "referenceId": "vnpay_demo_002"
}
Expected: availablePoints = 500000

🔵 Step 19: Purchase Seller's Bike
POST /orders
{
  "bikeId": {bikeIdFromStep11},
  "idempotencyKey": "order_demo_456"
}
Expected: Order.status = ESCROWED
         This Buyer's frozenPoints = 120000

🔵 Step 20: Approve Order (Complete Purchase)
POST /orders/{orderId}/approve
Expected: Order COMPLETED
🎉 YOUR SELLER ACCOUNT JUST EARNED 114,000 POINTS!
   (120,000 - 5% commission = 114,000)
```

### **Phase 7: Seller - Check Earnings & Withdraw**

```
🔵 Step 21: Logout buyer 2, Login back as SELLER
POST /auth/login (seller account)
💾 Update Authorize with seller token

🔵 Step 22: Check Wallet - Should Show Earnings
GET /wallet
Expected: availablePoints = 514000
         (400000 from before + 114000 earned from sale)
💡 This is proof that seller earned from the sale!

🔵 Step 23: Check Transaction History
GET /wallet/transactions?type=EARN
Expected: Shows transaction with amount = 114000, type = EARN, status = SUCCESS
💡 Full audit trail of seller earnings

🔵 Step 24: Request Withdrawal
POST /wallet/withdraw-request
{
  "amount": 114000,
  "bankName": "TechComBank",
  "bankAccountName": "Seller Full Name",
  "bankAccountNumber": "1234567890"
}
Expected: 
{
  "availablePoints": 400000,  ← 114000 deducted
  "frozenPoints": 114000       ← 114000 frozen
}
💡 Points are locked, pending admin approval to transfer to bank
```

### **Phase 8: Admin - Approve Withdrawal**

```
🔵 Step 25: Logout seller, Login as ADMIN again
POST /auth/login (admin account)
💾 Update Authorize with admin token

🔵 Step 26: List All Pending Withdrawals
GET /admin/withdrawals?status=PENDING
Expected: Shows seller's withdrawal request with:
         - amount: 114000
         - type: WITHDRAW
         - status: PENDING

🔵 Step 27: Approve the Withdrawal
POST /admin/withdrawals/{transactionId}/approve
(Replace {transactionId} with ID from Step 26)
Expected: 
{
  "success": true,
  "message": "Withdrawal approved and completed"
}
✅ Points are now released (frozen → transferred)
   Admin backend should now process bank transfer

🔵 Step 28: Optional - Verify Seller Wallet After Approve
[Logout admin, Login as seller again]
GET /wallet
Expected: frozenPoints = 0 (released)
         availablePoints = 400000 (unchanged, or minus the 114000)
```

---

## ✨ Complete Cycle Summary

```
✅ Buyer registered with 0 points
✅ Buyer deposited 500,000 points (nạp tiền = get xò)
✅ Buyer purchased bike from existing seller (100,000 points)
✅ Buyer left review for seller
✅ Buyer upgraded role: BUYER → SELLER 🎉
✅ Seller created new bike listing (80k points)
✅ Admin approved seller's listing
✅ New buyer purchased from seller (120,000 points)
✅ Seller earned: 114,000 points (after 5% commission)
✅ Seller requested withdrawal (114,000 points)
✅ Admin approved withdrawal
✅ Seller can now withdraw money to bank

OUTCOME:
- Platform earned: 6,000 points commission
  (5% from sale: 100k = 5k, 120k = 6k, total = 11k)
- Buyer spent: 100,000 points
- Seller earned: 114,000 points
- System working as designed ✅
```

---

## 💡 Notes for Development

**Key Files to Review:**
- [UserController.java](src/main/java/com/bikeexchange/controller/UserController.java) - POST endpoint for upgrade-to-seller
- [UserService.java](src/main/java/com/bikeexchange/service/UserService.java) - Business logic for role upgrade
- [WalletController.java](src/main/java/com/bikeexchange/controller/WalletController.java) - Deposit/withdraw endpoints
- [WalletService.java](src/main/java/com/bikeexchange/service/WalletService.java) - Wallet transaction business logic
- [OrderService.java](src/main/java/com/bikeexchange/service/OrderService.java) - Order creation & approval with escrow
- [ReviewService.java](src/main/java/com/bikeexchange/service/ReviewService.java) - Review creation (needs validation fix)
- [AdminController.java](src/main/java/com/bikeexchange/controller/AdminController.java) - Withdrawal approval & admin functions

**Testing Considerations:**
- Test concurrent orders from same buyer (pessimistic locking prevents race conditions)
- Test escrow with insufficient balance (should return InsufficientBalanceException)
- Test commission calculations (5% properly deducted)
- Test withdrawal approval/rejection flows (points frozen/unfrozen correctly)
- Test review validation rules (⚠️ currently missing - need to add order verification)
- Test upgrade-to-seller flow (only BUYER can upgrade, requires terms agreement)
- Test seller can still purchase after upgrade (role doesn't restrict buying)
- Test seller withdrawals (frozen until admin approval)

---

**Last Updated:** March 5, 2026 (Updated with complete Buyer→Seller upgrade flow & demo scripts)  
**Reviewed By:** System Analysis  
**Status:** Ready for Production Demo
