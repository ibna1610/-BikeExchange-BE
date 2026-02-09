# 🎯 BikeExchange Swagger - Setup & How to Deploy

## 🚀 Quick Start - Chạy Backend & Truy cập Swagger

### **Step 1: Build Project & Download Dependencies**
```bash
cd c:\Users\GMT\Desktop\SWP\-BikeExchange-BE
mvn clean install -DskipTests
```

### **Step 2: Run Application**
```bash
mvn spring-boot:run
```

**Output nếu thành công:**
```
...
2026-02-09 10:30:45.123 INFO [...] Started BikeExchangeApplication in 5.234 seconds
```

### **Step 3: Truy cập Swagger UI**
Mở browser và go to:

```
http://localhost:8080/swagger-ui.html
```

---

## 📊 Swagger UI Giao diện

### **Main Sections:**
1. **Buyer Management** - 18 API endpoints
   - Wishlist (4 endpoints)
   - Search & Filter (2 endpoints)
   - Inspection (3 endpoints)
   - Purchase & Transaction (5 endpoints)
   - Rating & Review (1 endpoint)
   - Report & Dispute (3 endpoints)

2. **Admin Management** - 26 API endpoints
   - Listing Management (5 endpoints)
   - User Management (6 endpoints)
   - Report Management (3 endpoints)
   - Transaction Management (3 endpoints)
   - Inspection Management (3 endpoints)
   - Statistics & Dashboard (6 endpoints)

3. **Other APIs** (existing)
   - User Management
   - Health Check
   - Transaction
   - Bike

---

## 🔗 Swagger Endpoints

| URL | Mô tả |
|-----|------|
| `http://localhost:8080/swagger-ui.html` | 🎯 **Main Swagger UI** |
| `http://localhost:8080/v3/api-docs` | JSON OpenAPI Spec |
| `http://localhost:8080/v3/api-docs.yaml` | YAML OpenAPI Spec |

---

## 📝 Cấu hình Database (application.yaml)

**Location:** `src/main/resources/application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bike_exchange_db?useSSL=false&serverTimezone=UTC
    username: root
    password: your_mysql_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect
  
  application:
    name: BikeExchange API

server:
  port: 8080
  servlet:
    context-path: /

logging:
  level:
    root: INFO
    com.bikeexchange: DEBUG
```

---

## ✅ Verify Installation

### **1. Check if Swagger is loaded:**
```bash
# Should return OpenAPI spec
curl http://localhost:8080/v3/api-docs | head -20
```

### **2. Test a sample endpoint:**
```bash
# Get all Buyer endpoints (check annotation)
curl -X GET http://localhost:8080/swagger-ui.html
```

### **3. View logs:**
```bash
# Check Spring Boot logs
tail -f target/application.log
```

---

## 🔐 Authentication Setup (Next Step)

### **To implement JWT Auth:**

1. **Create JWT Utility Class**
```java
@Component
public class JwtUtil {
    @Value("${jwt.secret:your-secret-key}")
    private String secretKey;
    
    public String generateToken(String email) {
        // Implementation
    }
    
    public String extractEmail(String token) {
        // Implementation
    }
}
```

2. **Create Auth Controller**
```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // Validate credentials & return JWT token
}
```

3. **Create JWT Filter**
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // Validate token in each request
}
```

---

## 📦 Project Dependencies Added

```xml
<!-- Springdoc OpenAPI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.0.2</version>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

---

## 🐛 Troubleshooting

### **Problem: "Cannot resolve io.swagger"**
**Solution:** Run `mvn clean install` to download dependencies

### **Problem: "Unable to connect to MySQL"**
**Solution:** Check MySQL is running
```bash
# Windows
mysql -u root -p -e "SELECT 1"

# Linux/Mac
mysql -u root -p -e "SELECT 1"
```

### **Problem: Port 8080 already in use**
**Solution:** Change port in `application.yaml`
```yaml
server:
  port: 8081
```

### **Problem: Swagger UI not loading**
**Solution:** Check logs
```bash
curl -v http://localhost:8080/swagger-ui.html
```

---

## 📋 API Testing Workflow

1. **Open Swagger UI**
   - Link: `http://localhost:8080/swagger-ui.html`

2. **Click Expand Section** (e.g., "Buyer Management")

3. **Try It Out**
   - Click "Try it out" button on any endpoint
   - Fill in parameters
   - Click "Execute"

4. **View Response**
   - See HTTP status code
   - View response body (JSON)
   - Check response headers

---

## 📚 Sample API Calls Using Swagger

### **Search Bikes**
```
GET /api/buyer/search-advanced?minPrice=500000&maxPrice=10000000&page=0&size=20
```

### **Add to Wishlist**
```
POST /api/buyer/1/wishlist/add/101
```

###  **Make Purchase**
```
POST /api/buyer/1/purchase/101?depositAmount=1000000
```

### **Approve Listing (Admin)**
```
PUT /api/admin/listings/101/approve
```

### **Get Dashboard Stats (Admin)**
```
GET /api/admin/metrics/dashboard
```

---

## 🎓 Generated Documentation

Files tạo:
- ✅ `pom.xml` - Dependencies updated
- ✅ `SwaggerConfig.java` - OpenAPI configuration
- ✅ `BuyerController.java` - With @Operation & @Parameter annotations
- ✅ `AdminController.java` - With @Operation & @Parameter annotations
- ✅ `SWAGGER_GUIDE.md` - This documentation

---

## 🚀 Next Steps

1. **Implement JWT Authentication**
   - Create auth endpoints
   - Add JWT filter
   - Secure API endpoints

2. **Test All Endpoints**
   - Use Swagger UI for testing
   - Use Postman for advanced testing

3. **Deploy to Server**
   - Docker containerization
   - Cloud deployment (AWS, Azure, etc.)

4. **Add More Documents**
   - Create Postman collection
   - Document error codes
   - Add usage examples

---

**Version:** 1.0.0  
**Last Updated:** February 9, 2026  
**Status:** Ready for Testing ✅
