# Upgrade to Seller Feature - Implementation Guide

## Overview
This feature allows authenticated BUYER users to upgrade their account to SELLER status. This is a self-service feature that enables buyers to become sellers on the BikeExchange platform.

## Components Implemented

### 1. **Model Changes** (`User.java`)
Added the following fields to support seller information:
- `shopName` (String): Name of the seller's shop
- `shopDescription` (String): Description of the seller's shop/business
- `upgradedToSellerAt` (LocalDateTime): Timestamp when user was upgraded to seller

### 2. **DTO - UpgradeToSellerRequest** (`UpgradeToSellerRequest.java`)
```java
{
    "shopName": "John's Bike Shop",
    "shopDescription": "Specializing in mountain bikes and road bikes",
    "agreeToTerms": true
}
```

**Validations:**
- `shopName`: Required field, non-blank
- `shopDescription`: Required field, non-blank
- `agreeToTerms`: Must be true (user must accept terms)

### 3. **Service Layer** (`UserService.java`)

#### New Method: `upgradeToSeller(Long userId, String shopName, String shopDescription)`

**Business Logic:**
- Validates that user exists
- Checks if user is already a seller (throws error if true)
- Checks if user role is BUYER (only BUYER can upgrade)
- Sets user role to SELLER
- Updates shop information
- Records the upgrade timestamp
- Saves and returns updated user

**Exceptions:**
- `RuntimeException`: "User not found" - if userId doesn't exist
- `IllegalArgumentException`: "User is already a seller" - if user already has SELLER role
- `IllegalArgumentException`: "Only buyers can upgrade to seller status" - if user is not a BUYER

### 4. **Controller Endpoint** (`UserController.java`)

#### Endpoint: `POST /users/{userId}/upgrade-to-seller`

**Authentication:** Required (Bearer Token)

**Path Parameters:**
- `userId` (Long): The ID of the user to upgrade

**Request Body:**
```json
{
    "shopName": "My Bike Shop",
    "shopDescription": "Quality bikes for everyone",
    "agreeToTerms": true
}
```

**Response (Success - 200 OK):**
```json
{
    "success": true,
    "message": "User successfully upgraded to seller",
    "data": {
        "id": 123,
        "email": "user@example.com",
        "fullName": "John Doe",
        "role": "SELLER",
        "shopName": "My Bike Shop",
        "shopDescription": "Quality bikes for everyone",
        "upgradedToSellerAt": "2026-03-02T10:30:00",
        "status": "ACTIVE",
        "rating": 0.0,
        "isVerified": false
    }
}
```

**Response (BadRequest - 400):**
```json
{
    "success": false,
    "message": "Please provide error details"
}
```

**Error Scenarios:**
1. **Forbidden (403)**: User trying to upgrade someone else's account
2. **BadRequest (400)**: Missing required fields (shopName, shopDescription, agreeToTerms)
3. **BadRequest (400)**: User is already a seller
4. **BadRequest (400)**: User is not a BUYER role
5. **NotFound (404)**: User ID doesn't exist

## API Usage Examples

### cURL Example:
```bash
curl -X POST http://localhost:8080/users/123/upgrade-to-seller \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shopName": "My Bike Shop",
    "shopDescription": "High-quality bikes and accessories",
    "agreeToTerms": true
  }'
```

### JavaScript/Fetch Example:
```javascript
const response = await fetch('http://localhost:8080/users/123/upgrade-to-seller', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    shopName: 'My Bike Shop',
    shopDescription: 'High-quality bikes and accessories',
    agreeToTerms: true
  })
});

const result = await response.json();
```

## Security Considerations

1. **Authentication Required**: Only authenticated users can upgrade
2. **User Ownership**: Users can only upgrade their own accounts (userId must match authenticated user)
3. **Role-Based Access**: Only BUYER role can be upgraded to SELLER
4. **Terms Acceptance**: User must explicitly accept terms to proceed

## Database Schema Changes

No new tables required. The following columns were added to the `users` table:
```sql
ALTER TABLE users ADD COLUMN shop_name VARCHAR(255);
ALTER TABLE users ADD COLUMN shop_description TEXT;
ALTER TABLE users ADD COLUMN upgraded_to_seller_at TIMESTAMP;
```

## Testing Guide

### Test Case 1: Successful Upgrade
```
Given: Authenticated BUYER user
When: POST /users/{userId}/upgrade-to-seller with valid data
Then: User role changes to SELLER, shop info saved, timestamp recorded
```

### Test Case 2: User Not Found
```
Given: Non-existent userId
When: POST /users/999/upgrade-to-seller
Then: Return 404 NotFound
```

### Test Case 3: Unauthorized Upgrade
```
Given: Authenticated user with different userId
When: POST /users/{otherId}/upgrade-to-seller
Then: Return 403 Forbidden
```

### Test Case 4: Already a Seller
```
Given: User already with SELLER role
When: POST /users/{userId}/upgrade-to-seller  
Then: Return 400 BadRequest with message "User is already a seller"
```

### Test Case 5: Missing Required Fields
```
Given: Valid buyer user
When: POST without shopName field
Then: Return 400 BadRequest with validation error
```

### Test Case 6: Terms Not Accepted
```
Given: Valid buyer user
When: POST with agreeToTerms = false
Then: Return 400 BadRequest with message "You must agree to the terms and conditions"
```

## Future Enhancements

1. **Admin Approval Required**: Add workflow requiring admin approval before upgrade becomes effective
2. **KYC Verification**: Require Know-Your-Customer identity verification
3. **Community Guidelines**: Acceptance of seller community guidelines
4. **Transaction History Check**: Verify buyer has completed at least X transactions
5. **Seller Badge**: Add visual badge/verification status  
6. **Onboarding Process**: Multi-step seller onboarding with tutorials
7. **Audit Trail**: Track all upgrades in history table
8. **Notification**: Send email/notification to user and admins on upgrade

## Related Features

- User Model: Stores role and seller information
- UserWallet: Manages points for sellers
- PostService: Sellers use this to create bike listings
- SellerPostController: Sellers manage their posts/listings
- ReviewService: Track seller ratings and reviews
