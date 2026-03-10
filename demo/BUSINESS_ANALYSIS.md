# 📊 Phân tích Nghiệp vụ: User Management + Cart System

## 📋 Mục lục

1. [Hệ thống User (Tài khoản)](#1-hệ-thống-user-tài-khoản)
2. [Giỏ hàng (Cart)](#2-giỏ-hàng-cart)
3. [Workflow Đăng ký / Đăng nhập](#3-workflow-đăng-ký--đăng-nhập)
4. [Quy trình Luồng Chính](#4-quy-trình-luồng-chính)
5. [Mối quan hệ dữ liệu](#5-mối-quan-hệ-dữ-liệu)
6. [Controller & Routes cần](#6-controller--routes-cần)
7. [Công nghệ & Tools cần dùng](#7-công-nghệ--tools-cần-dùng)
8. [Các Validation cần](#8-các-validation-cần)
9. [Data Flow - Add to Cart Example](#9-data-flow---add-to-cart-example)
10. [Session vs JWT](#10-khác-biệt-session-vs-jwt)

---

## 1️⃣ Hệ thống User (Tài khoản)

### Entities cần tạo

#### **User.java**

```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password; // hashed với BCrypt

    @Column(length = 100)
    private String fullName;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String defaultAddress;

    @Enumerated(EnumType.STRING)
    private UserRole role; // CUSTOMER, ADMIN

    @Enumerated(EnumType.STRING)
    private UserStatus status; // ACTIVE, INACTIVE, BANNED

    @CreationTimestamp
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    private LocalDateTime lastLogin;

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cart> carts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders;
}

// Enums
public enum UserRole {
    CUSTOMER, ADMIN
}

public enum UserStatus {
    ACTIVE, INACTIVE, BANNED
}
```

### Chức năng User

- ✅ **Đăng ký**: username + email + password + fullname + phone
- ✅ **Đăng nhập**: username/email + password → Session
- ✅ **Đăng xuất**: Clear Session
- ✅ **Cập nhật profile**: Fullname, phone, address...
- ✅ **Quên mật khẩu**: Reset link (qua email) - Optional
- ✅ **Xem lịch sử đơn hàng**: Dashboard/Account

### Security cần

- 🔐 Password hashing (BCrypt)
- 🔐 Session management (@EnableSpringHttpSession hoặc Spring Security)
- 🔐 CSRF protection (Thymeleaf)
- 🔐 Authentication filter

---

## 2️⃣ Giỏ hàng (Cart)

### Entities cần tạo

#### **Cart.java**

```java
@Entity
@Table(name = "carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private CartStatus status; // ACTIVE, CHECKED_OUT, ABANDONED

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount; // calculated từ items

    @CreationTimestamp
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items;

    // Helper method
    public void calculateTotal() {
        this.totalAmount = items.stream()
            .map(item -> item.getSubtotal())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

public enum CartStatus {
    ACTIVE, CHECKED_OUT, ABANDONED
}
```

#### **CartItem.java**

```java
@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @Min(1)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice; // lưu giá tại thời điểm thêm

    @CreationTimestamp
    private LocalDateTime addedDate;

    // Helper method
    public BigDecimal getSubtotal() {
        return this.unitPrice.multiply(new BigDecimal(this.quantity));
    }
}
```

### Chức năng Cart

- ✅ **Thêm sản phẩm**: Add to cart từ product detail
- ✅ **Xem giỏ**: View cart page với danh sách items
- ✅ **Cập nhật số lượng**: Tăng/giảm quantity
- ✅ **Xóa sản phẩm**: Remove item from cart
- ✅ **Xóa tất cả**: Clear cart
- ✅ **Tính tổng tiền**: Subtotal, tax (nếu có), total
- ✅ **Kiểm tra tồn kho**: Alert nếu out of stock

---

## 3️⃣ Workflow Đăng ký / Đăng nhập

```
┌─────────────────────────────────────────┐
│ Khách truy cập website                  │
├─────────────────────────────────────────┤
│ [Browse Products] (không cần log in)    │
│        │                                 │
│        ↓                                 │
│ [View Product] → [Add to Cart]          │
│        │                                 │
│        ↓                                 │
│ ⚠️ Kiểm tra: Đã đăng nhập chưa?        │
│        │                                 │
│        ├─→ ✅ YES → Thêm vào Cart       │
│        │                                 │
│        └─→ ❌ NO → Redirect /login      │
│              │                           │
│              ├─ [Sign In] (có account)  │
│              │  - Username/Email        │
│              │  - Password              │
│              │  → SUCCESS → /cart       │
│              │                           │
│              └─ [Sign Up] (tạo mới)     │
│                 - Username              │
│                 - Email                 │
│                 - Password              │
│                 - Fullname              │
│                 - Phone                 │
│                 → Auto login → /cart    │
└─────────────────────────────────────────┘
```

---

## 4️⃣ Quy trình Luồng Chính

### Scenario 1: Guest chuyển thành Customer

```
1. Guest duyệt sản phẩm (không cần login)
   ↓
2. Guest click "Add to Cart"
   ↓
3. System check: User authenticated?
   - NO → Redirect /login page
   ↓
4. User chọn Sign Up (hoặc Sign In nếu có account)
   ↓
5. After successful login:
   - Tạo mới User record
   - Tạo Cart mới for this User
   - Add item to Cart
   ↓
6. User vào /cart → xem items
   ↓
7. Cập nhật quantity, remove items...
   ↓
8. Click "Checkout" → /checkout (yêu cầu auth lại)
```

### Scenario 2: Returning Customer

```
1. User login với email/username
   ↓
2. Check if Cart.status = ACTIVE for this user
   - YES → Load it (khôi phục giỏ cũ)
   - NO → Create new empty cart
   ↓
3. User thêm sản phẩm
   ↓
4. Continue shopping hoặc checkout
```

---

## 5️⃣ Mối quan hệ dữ liệu

```
┌─────────────┐
│   User      │
├─────────────┤
│ id (PK)     │
│ username    │
│ email       │
│ password    │ ← Hash với BCrypt
│ fullname    │
│ phone       │
│ address     │
└────┬────────┘
     │ 1:N
     ↓
┌──────────────┐         ┌─────────────┐
│    Cart      │ 1:N     │ CartItem    │
├──────────────┤────────→├─────────────┤
│ id (PK)      │         │ id (PK)     │
│ user_id (FK) │         │ cart_id(FK) │
│ status       │         │ product_id  │
│ total_amount │         │ quantity    │
│ created_date │         │ unit_price  │
└──────────────┘         └─────────────┘
     ↑                          ↑
     │                          │
     └──────────────┬───────────┘
             1:N (Qua CartItem)
        ┌───────────────┐
        │   Product     │
        ├───────────────┤
        │ id (PK)       │
        │ name          │
        │ price         │
        │ description   │
        │ category_id   │
        └───────────────┘
```

---

## 6️⃣ Controller & Routes cần

### Authentication Routes

```
GET   /auth/login              → Form login
POST  /auth/login              → Process login
GET   /auth/register           → Form register
POST  /auth/register           → Process register
GET   /auth/logout             → Logout & redirect home
```

### Cart Routes (Require Authentication)

```
GET   /cart                    → View cart page
POST  /cart/add/{productId}    → Add product to cart
POST  /cart/update             → Update quantity (AJAX or form)
POST  /cart/remove/{itemId}    → Remove item from cart
POST  /cart/clear              → Clear all items
```

### User Account Routes (Require Authentication)

```
GET   /account                 → Profile page / dashboard
GET   /account/orders          → Order history
POST  /account/update          → Update profile
```

---

## 7️⃣ Công nghệ & Tools cần dùng

| Tính năng             | Công nghệ                             | Ghi chú                      |
| --------------------- | ------------------------------------- | ---------------------------- |
| **Password Security** | Spring Security BCryptPasswordEncoder | Mã hóa mật khẩu              |
| **Session**           | Spring Session / HttpSession          | Lưu trữ login state          |
| **Authentication**    | Spring Security Filter                | Kiểm tra auth trước endpoint |
| **CSRF**              | Spring Security CSRF Filter           | Bảo vệ form                  |
| **Email** (Optional)  | JavaMailSender                        | Cho reset password           |
| **Validation**        | Jakarta Validation (@Valid)           | Server-side validation       |
| **Template**          | Thymeleaf SecSecurityDialect          | Security tags trong HTML     |

---

## 8️⃣ Các Validation cần

### Sign Up Validation

- **Username**:
  - Length: 5-20 ký tự
  - Pattern: Chỉ chứa chữ, số, underscore
  - Unique: Không trùng username cũ

- **Email**:
  - Format: Valid email syntax
  - Unique: Không trùng email cũ

- **Password**:
  - Length: Min 6, Max 50 ký tự
  - Pattern: Có ít nhất 1 chữ cái + 1 số (nếu muốn strict)

- **Phone**:
  - Format: Valid Vietnam phone (10 digits, start with 0)

- **Fullname**:
  - Not blank
  - Length: 2-100 ký tự

### Sign In Validation

- **Username/Email**: Not blank
- **Password**: Not blank
- **Rate Limiting**: Max 5 failed login attempts → lock 15 minutes (nếu implement)

### Cart Validation

- **Quantity**:
  - Must be > 0
  - Must be ≤ stock available
  - Must be ≤ 999 (max limit)

- **Product**:
  - Phải tồn tại trong DB
  - Phải có status = ACTIVE
  - Stock > 0

- **CartItem uniqueness**:
  - 1 product chỉ có 1 CartItem trong 1 Cart
  - Nếu thêm lại → cập nhật quantity

---

## 9️⃣ Data Flow - Add to Cart Example

### Request Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    User (Frontend UI)                        │
├─────────────────────────────────────────────────────────────┤
│ Click [Add to Cart] button on product-detail.html           │
│ Request: POST /cart/add/123                                 │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ↓
┌─────────────────────────────────────────────────────────────┐
│              CartController.addToCart(productId=123)        │
├─────────────────────────────────────────────────────────────┤
│ 1. @GetUser (Spring Security)                              │
│    ├─ Check HttpSession → Lấy User ID                      │
│    └─ If not authenticated → Redirect /login               │
│                                                              │
│ 2. Load User từ Database                                   │
│    └─ If không tìm thấy → Error                            │
│                                                              │
│ 3. Get or Create Cart for this User                        │
│    ├─ Query: Cart where user_id=? AND status='ACTIVE'    │
│    ├─ If exists → use it                                   │
│    ├─ If not exists → Create new Cart                      │
│    └─ If status='ABANDONED' → Clear old items              │
│                                                              │
│ 4. Load Product từ Database                                │
│    ├─ Query: Product where id=?                            │
│    ├─ If not exist → Error «Product not found»             │
│    └─ If stock ≤ 0 → Error «Out of stock»                 │
│                                                              │
│ 5. Check if CartItem already exists                        │
│    ├─ Query: CartItem where cart_id=? AND product_id=?   │
│    ├─ If YES → quantity++, save                            │
│    └─ If NO → Create new CartItem, save                    │
│           └─ Set: unitPrice = product.price               │
│              quantity = 1                                   │
│              addedDate = now()                              │
│                                                              │
│ 6. Recalculate Cart.totalAmount                            │
│    └─ totalAmount = SUM(cartItem.subtotal)                │
│                                                              │
│ 7. Save Cart to Database                                   │
│                                                              │
│ 8. Return Response                                         │
│    └─ Redirect /cart or JSON response with OK status      │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ↓
┌─────────────────────────────────────────────────────────────┐
│                   Frontend (Browser)                         │
├─────────────────────────────────────────────────────────────┤
│ Response OK → Navigate to /cart (View cart updated)        │
└─────────────────────────────────────────────────────────────┘
```

### Database State Changes

```
BEFORE:
┌────────────────────────────────────────┐
│ User (id=1)                            │
│ - username: john_doe                   │
│ - email: john@example.com              │
└────────────────────────────────────────┘

┌────────────────────────────────────────┐
│ Cart (id=5, user_id=1, status=ACTIVE)  │
│ - totalAmount: 0                       │
├────────────────────────────────────────┤
│ CartItem(s):                           │
│ (none)                                 │
└────────────────────────────────────────┘

AFTER:
┌────────────────────────────────────────┐
│ Cart (id=5, user_id=1, status=ACTIVE)  │
│ - totalAmount: 150.00                  │
├────────────────────────────────────────┤
│ CartItem(s):                           │
│ ├─ id: 10                              │
│ ├─ product_id: 123                     │
│ ├─ quantity: 1                         │
│ ├─ unitPrice: 150.00                   │
│ └─ addedDate: 2026-03-05 10:30:00     │
└────────────────────────────────────────┘
```

---

## 🔟 Khác biệt: Session vs JWT

| Cách                      | Ưu điểm                                        | Nhược điểm                                      | Phù hợp khi                                               |
| ------------------------- | ---------------------------------------------- | ----------------------------------------------- | --------------------------------------------------------- |
| **Session (HttpSession)** | Đơn giản, built-in Spring, secure, easy logout | Server-side memory, không scale được nhiều      | Web truyền thống, 1 server hoặc load-balanced server farm |
| **JWT**                   | Stateless, scalable, tốt cho microservices     | Phức tạp hơn, logout khó hơn, cần refresh token | Mobile app, Single Page App (SPA), microservices          |

**Đề xuất cho dự án này:** Dùng **Spring Security + HttpSession**

- Đơn giản, phù hợp với Thymeleaf + Server-side rendering
- Không cần microservices
- Logout ngay lập tức

---

## 🎯 Tóm tắt hành động cần làm

### Phase 1: User Management

1. ✨ Tạo `User` entity + Enums (UserRole, UserStatus)
2. ✨ Tạo `UserRepository` (JpaRepository)
3. ✨ Tạo `UserService`:
   - `register(username, email, password, fullname, phone)`
   - `login(username, password)`
   - `findByUsername(username)`
   - `findByEmail(email)`
   - `updateProfile(user, fullname, phone, address)`
4. ✨ Cấu hình **Spring Security**:
   - BCryptPasswordEncoder bean
   - UserDetailsService implementation
   - SecurityConfig class (filters, URLs)
5. ✨ Tạo `AuthController`:
   - `GET /auth/login` → login.html
   - `POST /auth/login` → authenticate & create session
   - `GET /auth/register` → register.html
   - `POST /auth/register` → create user & auto login
   - `GET /auth/logout` → invalidate session
6. ✨ Tạo forms (Thymeleaf):
   - `login.html`
   - `register.html`

### Phase 2: Cart

1. ✨ Tạo `Cart` entity + `CartStatus` enum
2. ✨ Tạo `CartItem` entity
3. ✨ Tạo repositories:
   - `CartRepository`
   - `CartItemRepository`
4. ✨ Tạo `CartService`:
   - `getActiveCart(userId)` or create
   - `addToCart(userId, productId, quantity)`
   - `removeFromCart(userId, cartItemId)`
   - `updateItemQuantity(cartItemId, newQuantity)`
   - `clearCart(userId)`
   - `calculateTotal(cart)`
5. ✨ Tạo `CartController`:
   - `GET /cart` → cart.html
   - `POST /cart/add/{productId}` → add item
   - `POST /cart/remove/{itemId}` → remove item
   - `POST /cart/clear` → clear all
6. ✨ Tạo forms (Thymeleaf):
   - `cart.html` (view, update quantity, remove buttons)

### Phase 3: Integration

1. ✨ Thêm "Add to Cart" button trên `product-detail.html` (require login)
2. ✨ Update navbar:
   - Show [Cart] link when authenticated
   - Show [Login/Register] when not authenticated
   - Show [Account | Logout] when authenticated

---

## 📌 File cần tạo

- `User.java` (model)
- `UserRole.java`, `UserStatus.java` (enums)
- `Cart.java` (model)
- `CartItem.java` (model)
- `CartStatus.java` (enum)
- `UserRepository.java`
- `CartRepository.java`
- `CartItemRepository.java`
- `UserService.java`
- `CartService.java`
- `AuthController.java`
- `CartController.java`
- `SecurityConfig.java` (Spring Security config)
- Thymeleaf templates: `login.html`, `register.html`, `cart.html`, etc.

---

## 🔌 Dependencies cần thêm (pom.xml)

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Spring Session (nếu muốn distributed session) -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-core</artifactId>
</dependency>

<!-- Jakarta Validation -->
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>

<!-- Thymeleaf Security Dialect -->
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```

---

**Status**: 📝 Tài liệu phân tích chi tiết hoàn thành ✅

**Next Step**: Chọn Phase 1, 2 hay 3 để bắt đầu implementation
