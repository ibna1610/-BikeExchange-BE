# 🧪 Testing Image Upload Flow - Complete Checklist

## Backend Setup ✅
- ✅ Backend compiles with `mvn clean compile`
- ✅ CloudinarySignController.java created
- ✅ BikeUploadService.java fixed (removed setPublicId call)
- ✅ CloudinaryService.java fixed (removed unsupported width() calls)

## Pre-Testing Requirements

### 1. Start Backend Server
```bash
cd c:\Users\GMT\Desktop\SWP\-BikeExchange-BE
mvn spring-boot:run
# Wait for: "Started BikeExchangeApplication in X seconds"
```
Expected: Server running on http://localhost:8080

### 2. Start Frontend Server  
```bash
cd "c:\Users\GMT\Desktop\SWP\BikeExChange-FE"
npm run dev
# Should see: "Local: http://localhost:5173/"
```
Expected: Frontend running on http://localhost:5173

### 3. Browser DevTools Setup
- Open Chrome DevTools (F12)
- Go to Network tab
- Go to Console tab for logs

## Test Scenario: Create Bike with Images

### Step 1: Login as Seller
- [ ] Navigate to http://localhost:5173
- [ ] Login with seller account
- [ ] Confirm you're on seller dashboard

### Step 2: Create New Bike
- [ ] Click "Đăng tin bán xe" or "Create Bike" button
- [ ] Should see form with image upload section

### Step 3: Upload 3 Images
- [ ] Click image upload area
- [ ] Select 3 bike images (from desktop/camera)
- [ ] **Verify:** 3 previews appear below
- [ ] **Check:** `mediaPreview` state has 3 items
- [ ] **Check:** `images` state has 3 items with `{ name, dataUrl, file }`

### Step 4: Fill Form
- [ ] Enter required fields: title, brand, price
- [ ] Fill optional fields: description, category
- [ ] Leave inspection request blank (optional)

### Step 5: Monitor Upload (Console)
- [ ] Open Browser Console (F12 → Console)
- [ ] Watch for messages:
  ```
  ✅ Image uploaded to Cloudinary via backend: https://res.cloudinary.com/...
  ✅ Image uploaded to Cloudinary via backend: https://res.cloudinary.com/...
  ✅ Image uploaded to Cloudinary via backend: https://res.cloudinary.com/...
  ```

### Step 6: Monitor Network Requests
- [ ] In Network tab, watch for requests
- [ ] Should see: `POST /api/cloudinary/upload` (3 times, one per image)
- [ ] Response should be `{ "url": "https://res.cloudinary.com/..." }`

### Step 7: Submit Form
- [ ] Click "Đăng tin" button
- [ ] Watch for success modal
- [ ] Success message shows: "Đăng bài thành công! Đã upload 3 ảnh từ Cloudinary."
- [ ] Form clears
- [ ] Wallet balance updated

## Post-Creation Verification

### Step 8: Verify Bike Created
- [ ] Go to bike listing page
- [ ] New bike should appear at top
- [ ] **CRITICAL:** 3 images should be visible as thumbnails
- [ ] Hover over images - should show Cloudinary URLs

### Step 9: Check Bike Details
- [ ] Click on created bike
- [ ] Go to detail page
- [ ] **CRITICAL:** All 3 images should display correctly
- [ ] Images should be from Cloudinary (fast loading)
- [ ] Can click image gallery to view full size

## Network Request Details (Expected)

### Upload Image Request
```
POST /api/cloudinary/upload
Content-Type: multipart/form-data

file: [binary image data]
```

### Upload Response
```json
{
  "url": "https://res.cloudinary.com/dwe8yl6xv/image/upload/bikes/uploads/1234567890/photo.jpg",
  "publicId": "bikes/uploads/1234567890/photo",
  "message": "Upload successful"
}
```

### Create Bike Request
```
POST /api/bikes (or /api/bikes/with-images)
Content-Type: application/json

{
  "title": "Xe đạp Road 2024",
  "brandId": 5,
  "price": 5000000,
  "media": [
    { "url": "https://res.cloudinary.com/...", "type": "IMAGE", "sortOrder": 1 },
    { "url": "https://res.cloudinary.com/...", "type": "IMAGE", "sortOrder": 2 },
    { "url": "https://res.cloudinary.com/...", "type": "IMAGE", "sortOrder": 3 }
  ]
}
```

## Troubleshooting

### Issue: Images not uploading
**Symptoms:** No "✅ Image uploaded" logs, images not visible after creation

**Check:**
1. [ ] Backend running? (Check http://localhost:8080/api/health)
2. [ ] Cloudinary config in application.yml?
3. [ ] CloudinarySignController endpoint exists?
4. [ ] Network request to `/api/cloudinary/upload` shown in Network tab?
5. [ ] Response has `"url"` field?

### Issue: Upload hangs / times out
**Symptoms:** "Đang upload ảnh..." button stuck

**Check:**
1. [ ] Large images? Try images < 5MB
2. [ ] Backend hanging? Check server logs
3. [ ] Network tab shows request stuck?
4. [ ] Try refreshing page and retrying

### Issue: Images upload but don't display
**Symptoms:** Success message shown, but no images on bike detail page

**Check:**
1. [ ] Media array in response has URLs?
2. [ ] Bike in database has media entries?
3. [ ] BikeMedia table populated?
4. [ ] URL valid? Try opening in browser

### Issue: 500 Error on upload
**Symptoms:** Network tab shows POST 500 error

**Check:**
1. [ ] Check backend server logs
2. [ ] Cloudinary credentials valid?
3. [ ] CloudinaryService autowired correctly?
4. [ ] File not empty?

## Success Criteria

✅ **Test PASSED if:**
1. Images upload without errors (console shows 3 ✅ messages)
2. Success modal displays with correct count
3. Bike appears in listing with all 3 images visible
4. Bike detail page shows all 3 images from Cloudinary
5. Images load fast (not placeholder URLs)
6. No errors in browser console or server logs

❌ **Test FAILED if:**
1. Any "placeholder URL" images appear (fallback mode)
2. Images missing after creation
3. Network errors (4xx, 5xx)
4. Success modal doesn't appear
5. Form doesn't clear after submit

## Advanced Testing

### Performance Test
- [ ] Upload 10 images one after another
- [ ] Measure time for 10-image upload
- [ ] Expected: < 30 seconds
- [ ] All images should have Cloudinary URLs

### Error Recovery Test
- [ ] Stop backend while uploading
- [ ] Should show error message
- [ ] Form should still be available for retry
- [ ] No data loss

### Image Quality Test
- [ ] Upload different image formats: JPG, PNG, WebP
- [ ] Verify all formats upload successfully
- [ ] Display correctly in listing and detail view

---
**Last Updated:** 2026-03-18
**Status:** Ready for testing
