# 🚀 Quick Start Guide - Upgrade to Seller Feature

## What Was Implemented?

A complete feature that allows BUYER users to upgrade their account to SELLER status on the BikeExchange platform.

---

## 📌 Quick Facts

| Property | Value |
|----------|-------|
| **Endpoint** | `POST /users/{userId}/upgrade-to-seller` |
| **Authentication** | ✅ Required (Bearer Token) |
| **Role Required** | BUYER (to upgrade) |
| **New Role After** | SELLER |
| **Database Tables Modified** | `users` |
| **New Columns** | 3 (shop_name, shop_description, upgraded_to_seller_at) |

---

## 🎬 Getting Started - 5 Minute Setup

### Step 1: Apply Database Migration
```sql
ALTER TABLE users ADD COLUMN shop_name VARCHAR(255);
ALTER TABLE users ADD COLUMN shop_description TEXT;
ALTER TABLE users ADD COLUMN upgraded_to_seller_at TIMESTAMP;
```

### Step 2: Restart Application
```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

### Step 3: Test the Endpoint

**Get Auth Token First:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"buyer@test.com","password":"password"}'
```

**Upgrade to Seller:**
```bash
curl -X POST http://localhost:8080/users/1/upgrade-to-seller \
  -H "Authorization: Bearer {YOUR_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "shopName": "My Awesome Bike Shop",
    "shopDescription": "Selling quality bikes since 2024",
    "agreeToTerms": true
  }'
```

---

## 📋 Request/Response Specification

### Request
```json
{
  "shopName": "Bike Haven",           // Required, non-empty
  "shopDescription": "Premium bikes", // Required, non-empty
  "agreeToTerms": true                // Required, must be true
}
```

### Success Response (200 OK)
```json
{
  "success": true,
  "message": "User successfully upgraded to seller",
  "data": {
    "id": 1,
    "email": "buyer@example.com",
    "fullName": "John Doe",
    "role": "SELLER",
    "shopName": "Bike Haven",
    "shopDescription": "Premium bikes",
    "upgradedToSellerAt": "2026-03-02T10:30:00",
    "status": "ACTIVE",
    "rating": 0.0,
    "isVerified": false
  }
}
```

### Error Response Examples

**Missing Field (400):**
```json
{
  "success": false,
  "message": "Shop name is required"
}
```

**Not a Buyer (400):**
```json
{
  "success": false,
  "message": "Only buyers can upgrade to seller status"
}
```

**Already a Seller (400):**
```json
{
  "success": false,
  "message": "User is already a seller"
}
```

**User Not Found (404):**
```json
{
  "message": "Not Found"
}
```

---

## 📂 File Structure

```
BikeExchange-BE/
├── src/main/java/com/bikeexchange/
│   ├── model/
│   │   └── User.java                     ✏️ MODIFIED
│   ├── service/
│   │   └── UserService.java              ✏️ MODIFIED
│   ├── controller/
│   │   └── UserController.java           ✏️ MODIFIED
│   └── dto/request/
│       └── UpgradeToSellerRequest.java   ✨ NEW
├── src/test/java/com/bikeexchange/controller/
│   └── UserUpgradeToSellerTest.java      ✨ NEW
├── UPGRADE_TO_SELLER_FEATURE.md          ✨ NEW
├── UPGRADE_FEATURE_IMPLEMENTATION.md     ✨ NEW
└── QUICK_START_GUIDE.md                  ✨ NEW (this file)
```

---

## 🧪 Testing Checklist

- [ ] Test successful upgrade: BUYER → SELLER
- [ ] Test missing shopName: Get 400 error
- [ ] Test missing shopDescription: Get 400 error  
- [ ] Test agreeToTerms=false: Get 400 error
- [ ] Test already a SELLER: Get 400 error
- [ ] Test non-BUYER role: Get 400 error
- [ ] Test non-existent userId: Get 404 error
- [ ] Test upgrading other user's account: Get 403 error
- [ ] Verify seller can create posts after upgrade
- [ ] Verify user role changed in database

---

## 🔍 Debugging Tips

### Issue: Getting 403 Forbidden
**Cause:** Trying to upgrade another user's account  
**Solution:** Ensure `userId` in path matches authenticated user ID

### Issue: Getting "User not found"  
**Cause:** Invalid userId  
**Solution:** Verify the userId exists in the database

### Issue: Getting "Only buyers can upgrade to seller status"
**Cause:** User already has SELLER role or other role  
**Solution:** Check user's current role in database

### Issue: Getting "User is already a seller"
**Cause:** User already upgraded  
**Solution:** Check the `upgraded_to_seller_at` timestamp

### Issue: Missing TOKEN in Authorization
**Cause:** Not providing Bearer token  
**Solution:** Add `Authorization: Bearer {token}` to request headers

---

## 🎬 Next Steps for Sellers

After successful upgrade, a SELLER can:

1. **Create Listings**
   ```bash
   POST /seller/posts
   ```

2. **View Their Listings**
   ```bash
   GET /seller/posts
   ```

3. **Update Listings**
   ```bash
   PUT /seller/posts/{postId}
   ```

4. **Delete Listings**
   ```bash
   DELETE /seller/posts/{postId}
   ```

5. **Receive Reviews**
   ```bash
   GET /reviews?sellerId={sellerId}
   ```

6. **Check Sales Statistics**
   ```bash
   GET /users/{userId}/stats
   ```

---

## 💡 Key Features

✅ **Self-Service Upgrade** - Users can upgrade themselves  
✅ **Protected by Auth** - JWT token required  
✅ **Validation** - All required fields validated  
✅ **Role Check** - Only BUYER can upgrade  
✅ **Duplicate Prevention** - Can't upgrade if already seller  
✅ **Audit Trail** - Upgrade timestamp recorded  
✅ **Error Handling** - Clear error messages  

---

## 🤝 Integration Points

This feature integrates with:
- **AuthController** - For user authentication
- **SellerPostController** - Upgraded sellers can use this
- **ReviewService** - Sellers get reviews
- **UserWallet** - Sellers have points system
- **OrderService** - Sellers fulfill orders

---

## 📞 Common Questions

**Q: Can an ADMIN upgrade a user?**  
A: No, this is a self-service feature. Admins can use existing role change endpoint if needed.

**Q: Can a SELLER downgrade back to BUYER?**
A: Not with this feature. Would require separate downgrade feature.

**Q: Is shop name unique?**
A: Not enforced currently. Could be added in future.

**Q: What happens to buyer wishlist after upgrade?**
A: Wishlist data is preserved. User can still use buyer features.

**Q: Can I change shop name later?**
A: Not with current feature. Would require separate endpoint.

---

## 📚 Documentation Links

- Full Feature Docs: [UPGRADE_TO_SELLER_FEATURE.md](UPGRADE_TO_SELLER_FEATURE.md)
- Implementation Summary: [UPGRADE_FEATURE_IMPLEMENTATION.md](UPGRADE_FEATURE_IMPLEMENTATION.md)
- Unit Tests: [UserUpgradeToSellerTest.java](src/test/java/com/bikeexchange/controller/UserUpgradeToSellerTest.java)

---

**Version:** 1.0  
**Last Updated:** March 2, 2026  
**Status:** ✅ Ready for Production
