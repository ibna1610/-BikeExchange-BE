# Upgrade to Seller Feature

## Summary
Feature allows BUYER users to upgrade their account to SELLER status.

## Endpoint
`POST /users/{userId}/upgrade-to-seller`

**Request:**
```json
{
  "shopName": "string",
  "shopDescription": "string",
  "agreeToTerms": boolean
}
```

## Files Modified
1. **User.java** - Added: `shopName`, `shopDescription`, `upgradedToSellerAt`
2. **UserService.java** - Added: `upgradeToSeller()` method
3. **UserController.java** - Added: upgrade endpoint with validation
4. **UpgradeToSellerRequest.java** - New DTO with JSR-303 validation

## Key Features
- ✅ Only BUYER can upgrade to SELLER
- ✅ User can only upgrade own account (authenticated)
- ✅ Terms must be accepted
- ✅ Stores shop information
- ✅ Records upgrade timestamp

## Error Responses
- `403 Forbidden` - Trying to upgrade another user
- `400 Bad Request` - Missing fields or validation errors
- `404 Not Found` - User doesn't exist

## Database Changes
```sql
ALTER TABLE users ADD COLUMN shop_name VARCHAR(255);
ALTER TABLE users ADD COLUMN shop_description TEXT;
ALTER TABLE users ADD COLUMN upgraded_to_seller_at TIMESTAMP;
```

