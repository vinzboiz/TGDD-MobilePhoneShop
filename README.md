# 🛒 TheGioiDiDong Clone – Spring Boot E‑Commerce

> E‑commerce website inspired by Thế Giới Di Động – home page with flash-sale section, category browsing, cart, checkout, reward points, quantity‑based promotions and an admin area for managing products/categories.

[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8+-4479A1?logo=mysql)](https://www.mysql.com/)

---

## 📋 Table of Contents

- [Quick start](#-quick-start)
- [Features](#-features)
- [Tech stack](#-tech-stack)
- [Detailed setup](#-detailed-setup)
- [Project structure](#-project-structure)
- [Business logic highlights](#-business-logic-highlights)
- [Maven scripts](#-maven-scripts)
- [Troubleshooting](#-troubleshooting)

---

## 🚀 Quick start

Requirements:

- **JDK 21+**
- **MySQL 8+**
- Maven 3.9+ (or Maven wrapper)

```bash
# 1. Go into the project
cd demo

# 2. Configure database in src/main/resources/application.properties
#    (see section below)

# 3. Build & run
mvn clean package
mvn spring-boot:run
# → http://localhost:8080
```

---

## ✨ Features

| Area | Description |
|------|-------------|
| **Home page** | TGDD‑like layout with sticky header, search bar, categories and flash sale section. Shows original price, discounted price, discount percent and a stock bar “X promo slots left”. When promo quota is exhausted or stock is out, it shows “Promotion ended” / “Sold out”. |
| **Products** | List with filtering by parent/child category; product detail with main image + gallery, description, base price, discount percent and promotion quota (`promoQuota`, `promoRemaining`). |
| **Cart** | Add/remove/update quantity; supports both guest users (session cart) and logged‑in users (cart stored in DB). Subtotal, shipping fee, grand total and reward points are updated via AJAX without full page reload. |
| **Checkout / Orders** | Create orders from cart, persist order details, shipping address and payment method. After checkout the cart is marked as `CHECKED_OUT`. |
| **Account & Profile** | Register, login, logout with Spring Security. Profile page has a custom layout showing personal info, order history, addresses, vouchers and **total accumulated reward points**. |
| **Reward points** | For every 10,000₫ of merchandise subtotal (excluding shipping) the user earns 1 point. Each order stores its own `rewardPoints`, and the User entity stores `totalRewardPoints`. Points are shown on the order detail page and in the profile page. |
| **Quantity‑based promotion (quota)** | Each product has `discountPercent` and `promoQuota` / `promoRemaining`. In the cart, if the quantity exceeds the remaining promo quota, only `promoRemaining` items use the discounted price, the rest use base price and a warning is shown. On successful checkout `promoRemaining` is reduced accordingly. |
| **Shipping fee** | Free shipping when **subtotal ≥ 1,000,000₫** and **total quantity ≥ 2**, otherwise shipping fee is **30,000₫**. Logic is enforced on the server (OrderService) and mirrored on the client (cart JS) for real‑time UX but the server remains the source of truth. |
| **Admin (Back‑office)** | TGDD‑style admin dashboard: product‑by‑category bar chart (Chart.js), manage parent/child categories, manage products (create/edit/delete, upload primary and secondary images). Only users with role `ADMIN` can access. |
| **UI/UX** | All main pages (home, cart, auth, account, admin) use custom CSS (`home.css`, `cart.css`, `auth.css`, `account.css`, `admin.css`, `header.css`, `footer.css`) to mimic TGDD experience: sticky header top, product cards, stock bar, “Buy now” buttons, etc. |

---

## 🛠 Tech stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 21, Spring Boot (web, thymeleaf, security, data‑jpa, validation) |
| **View** | Thymeleaf + Thymeleaf Layout Dialect, header/footer fragments, Spring Security dialect |
| **Database** | MySQL 8, Spring Data JPA / Hibernate |
| **Security** | Spring Security 6, form login, `CUSTOMER` / `ADMIN` roles |
| **Front‑end assets** | HTML5, CSS3, some Bootstrap 5, custom TGDD‑style CSS |
| **Charts** | Chart.js for product‑per‑category chart on admin dashboard |
| **Others** | Lombok, Jackson, Spring Boot DevTools |

---

## 📦 Detailed setup

### 1. Database configuration

In `demo/src/main/resources/application.properties` (example: MySQL user `root`, no password, database `tgdd_shop`):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tgdd_shop?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Create the database:

```sql
CREATE DATABASE tgdd_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> Note: `ddl-auto=update` is convenient for development. For production, prefer `validate` and manage schema via migrations/scripts.

### 2. Run the application

```bash
cd demo
mvn clean package
mvn spring-boot:run
```

Open `http://localhost:8080` in your browser.

Main routes:

- `/` – Home page
- `/auth/login`, `/auth/register` – Auth pages
- `/cart` – Cart page
- `/account/profile` – Account profile
- `/admin/dashboard` – Admin dashboard

---

## 📁 Project structure

```text
demo/
├── src/
│   ├── main/
│   │   ├── java/com/vinh/thegioididong/
│   │   │   ├── DemoApplication.java          # Spring Boot entry point
│   │   │   ├── config/                       # Security, layout advice, seeders
│   │   │   ├── controller/                   # Home, Auth, Account, Cart, Checkout, Admin, Product, Category, ...
│   │   │   ├── model/                        # Entities: User, Product, Category, Cart, CartItem, Order, OrderDetail, ...
│   │   │   ├── repository/                   # Spring Data JPA repositories
│   │   │   └── service/                      # Business services: CartService, OrderService, ProductService, ...
│   │   ├── resources/
│   │   │   ├── templates/                    # Thymeleaf templates (home, cart, auth, account, admin, ...)
│   │   │   ├── templates/fragments/          # Header/footer fragments
│   │   │   ├── static/css/                   # CSS: home.css, cart.css, header.css, footer.css, admin.css, ...
│   │   │   └── static/images/                # Logos, banners, icons, flashsale images, QR, ...
│   │   └── application.properties
│   └── test/                                 # Unit/integration tests (optional)
└── pom.xml
```

---

## 🧠 Business logic highlights

### 1. Shipping fee

Implemented in `OrderService.computeShippingFee(cart)` and mirrored in `cart.html`:

- Free shipping when:
  - `subtotal ≥ 1,000,000₫` **and**
  - `totalQuantity ≥ 2`.
- Otherwise: shipping fee = **30,000₫**.

### 2. Reward points

- When an order is created:
  - Order `rewardPoints` = `subtotal / 10,000` (floor, shipping excluded).
  - `User.totalRewardPoints += rewardPoints`.
- Display:
  - In `order-detail.html`: “You earned X points for this order (Total accumulated: Y points)”.
  - In `profile.html`: the user’s total accumulated points.

### 3. Quantity‑based promotion (promo quota)

- Each `Product` has:
  - `discountPercent` – percent discount.
  - `promoQuota` – total number of promo slots.
  - `promoRemaining` – number of promo slots still available.
- In the cart:
  - If `quantity > promoRemaining`, only `promoRemaining` items use the discounted price; the rest use the base price.
  - A warning is rendered under the item: “Only X units get the promo price, the rest are charged at base price.”
- On checkout:
  - `promoRemaining` is decreased by `discountedQty` (the actual number of discounted units sold).
  - The home page stock bar reflects the new `promoRemaining` value.

---

## 📜 Maven scripts

| Command | Description |
|---------|-------------|
| `mvn spring-boot:run` | Run the app in dev mode with hot reload (DevTools) |
| `mvn clean package` | Build an executable JAR |
| `mvn test` | Run tests (if any) |

---

## 🐞 Troubleshooting

| Symptom | How to fix |
|---------|------------|
| Cannot connect to MySQL | Check `spring.datasource.url`, username/password, and ensure the database exists and MySQL is running. |
| Build fails due to Lombok | Enable annotation processing in your IDE (Settings → Build → Annotation Processors) and install the Lombok plugin. |
| CORS errors when calling APIs from another host | Add a CORS configuration in `SecurityConfig` or on the relevant controller. |
| Thymeleaf `TemplateInputException` | Check `th:replace` paths, template names and that the referenced fragments actually exist. |

---

*This project is for learning purposes – a Spring Boot + Thymeleaf clone of Thế Giới Di Động’s shopping flow and UI/UX.* 
