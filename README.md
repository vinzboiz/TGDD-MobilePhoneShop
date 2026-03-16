# 🛍️ Demo E-Commerce Management System

A Spring Boot 4.0.1 web application for managing e-commerce products and categories with Thymeleaf templates and MySQL database.

## 📋 Project Overview

**Name:** Demo E-Commerce Management System
**Framework:** Spring Boot 4.0.1
**Language:** Java 21
**Build Tool:** Maven
**Database:** MySQL
**Frontend:** Thymeleaf Templates + Bootstrap 5

## 🚀 Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- MySQL 8.0+
- Git (optional)

### Database Setup

1. Create MySQL database:

```sql
CREATE DATABASE shopdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. The application will automatically create tables using JPA (hibernate.ddl-auto=update)

### Build & Run

```bash
# Clone or navigate to project directory
cd demo

# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

## 📁 Project Structure

```
demo/
├── src/
│   ├── main/
│   │   ├── java/com/hutech/demo/
│   │   │   ├── DemoApplication.java         # Main entry point
│   │   │   ├── controller/                  # Request handlers
│   │   │   │   ├── HomeController.java
│   │   │   │   ├── CategoryController.java
│   │   │   │   └── ProductController.java
│   │   │   ├── model/                       # JPA entities
│   │   │   │   ├── Category.java
│   │   │   │   ├── Product.java
│   │   │   │   ├── Order.java
│   │   │   │   └── OrderDetail.java
│   │   │   ├── service/                     # Business logic
│   │   │   │   ├── CategoryService.java
│   │   │   │   └── ProductService.java
│   │   │   └── repository/                  # Data access
│   │   │       ├── CategoryRepository.java
│   │   │       ├── ProductRepository.java
│   │   │       ├── OrderRepository.java
│   │   └── resources/
│   │       ├── application.properties       # Configuration
│   │       ├── templates/                   # HTML templates
│   │       │   ├── layout.html              # Master layout
│   │       │   ├── layouts/
│   │       │   ├── home/
│   │       │   ├── categories/
│   │       │   ├── products/
│   │       └── static/
│   │           ├── css/                     # Stylesheets
│   │           ├── js/                      # JavaScript
│   │           └── images/                  # Images
│   └── test/                                # Unit tests
├── pom.xml                                  # Maven configuration
├── .gitignore
└── README.md
```

## 🔌 API Endpoints

### Categories

- `GET /categories` - List all categories
- `GET /categories/add` - Show add form
- `POST /categories/add` - Create category
- `GET /categories/edit/{id}` - Show edit form
- `POST /categories/edit/{id}` - Update category
- `POST /categories/delete/{id}` - Delete category

### Products

- `GET /products` - List products (with filter by category)
- `GET /products/add` - Show add form
- `POST /products/add` - Create product (with image upload)
- `GET /products/edit/{id}` - Show edit form
- `POST /products/edit/{id}` - Update product
- `POST /products/delete/{id}` - Delete product
- `GET /products` - List products (with filter by category)
- `GET /products/{id}` - Show detail page for a product (includes gallery of all images)
- `GET /products/add` - Show add form
- `POST /products/add` - Create product (with image upload)
- `GET /products/edit/{id}` - Show edit form
- `POST /products/edit/{id}` - Update product
- `POST /products/delete/{id}` - Delete product
- `GET /products/image/{id}` - Display primary product image
- `GET /products/image/{id}/{index}` - Display image by slot (1=primary, 2+ secondary)

### Home

- `GET /` - Home page

## 🎨 Features

✅ Complete CRUD operations for categories and products
✅ Product image upload and storage (BLOB) for up to four images per product (1 primary + 3 extras) using a separate table
✅ Product detail page with gallery of images
✅ Form validation with error messages
✅ Responsive UI with Bootstrap 5
✅ Thymeleaf template inheritance
✅ Cascade delete (deleting category removes products)
✅ Flash messages (success/error notifications)
✅ Search and filter functionality
✅ Lazy loading for images and relationships

## ⚙️ Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/shopdb
spring.datasource.username=root
spring.datasource.password=

# File Upload
spring.servlet.multipart.max-file-size=10MB
```

## 🗄️ Database Schema

**Categories Table**

- `id` (PK) - Long
- `name` - String (NOT NULL)

**Products Table**

- `id` (PK) - Long
- `name` - String (NOT NULL)
- `price` - Double (NOT NULL)
- `description` - Text
- `category_id` (FK) - Long
- `image_data` - LONGBLOB (primary image)
- `image_type` - String
- `image_name` - String

Secondary images are stored separately in the **product_images** table (see below).

**Product Images Table**

- `id` (PK) - Long
- `product_id` (FK) - Long
- `position` - Integer (ordering 1..3)
- `image_data` - LONGBLOB
- `image_type` - String
- `image_name` - String

**Orders Table**

- `id` (PK) - Long
- `order_date` - DateTime
- `total_amount` - Double

**Order Details Table**

- `id` (PK) - Long
- `order_id` (FK) - Long
- `product_id` (FK) - Long
- `quantity` - Integer
- `price` - Double

## 🔒 Security Notes

⚠️ **Current Version:**

- No authentication/authorization
- No CSRF protection
- Public endpoints

**For Production:**

- Add Spring Security
- Implement user authentication
- Enable CSRF token handling
- Sanitize user inputs
- Use HTTPS/SSL

## 📦 Dependencies

- Spring Boot 4.0.1 Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- Spring Boot Starter Thymeleaf
- Thymeleaf Layout Dialect 3.0.0
- MySQL Connector/J 8.0.33
- Lombok
- Bootstrap 5.3
- Jakarta Validation API 3.0.2

## 🧪 Testing

```bash
# Run tests
mvn test

# Run specific test
mvn test -Dtest=DemoApplicationTests
```

## 🐛 Troubleshooting

**MySQL connection error:**

- Ensure MySQL is running
- Check database credentials in application.properties
- Verify `shopdb` database exists

**Port already in use:**

```bash
# Change port in application.properties
server.port=8081
```

**Image upload fails:**

- Check file size (max 10MB)
- Verify BLOB column exists in products table

## 📚 Learning Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Thymeleaf Documentation](https://www.thymeleaf.org)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Bootstrap 5](https://getbootstrap.com)

## 🤝 Contributing

Feel free to fork and submit pull requests for any improvements.

## 📝 License

This project is open source and available for educational purposes.

## 👨‍💻 Author

**Created:** March 5, 2026
**Framework:** Spring Boot 4.0.1
**Java Version:** 21

---

**Happy coding! 🚀**
