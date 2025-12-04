School-Lunch is the main Spring Boot web application of the system. It is responsible for:

User management (parents, children, admin)
Wallet balances & transactions
Lunch order creation and cancellation
Parent ↔ Child linking
Role-based access and administration
UI rendering using Thymeleaf

Triggering lunch operations through a REST microservice (lunch-svc)

Caching of frequently accessed resources

This project communicates with the separate microservice `lunch-svc` via Spring Cloud OpenFeign, which handles all lunch-related backend operations.

## 🧱 Architecture Overview
+--------------------------+
|      _School-Lunch       |
|  (Main Application)      |
|                          |
|  • UI & Frontend         |
|  • Authentication         |
|  • Parent & Child        |
|  • Wallet & Transactions |
|  • Admin Operations      |
|  • Feign Client          |
+-------------+------------+
              |
              | REST Calls
              v
+-------------+------------+
|          lunch-svc       |
|   (Lunch Microservice)   |
+--------------------------+

The `_School-Lunch` project acts as the **main client** and orchestrator, forwarding business operations to the `lunch-svc` backend via REST calls.

## 🛠️ Technologies Used
- Java 17
- Spring Boot 3.4
- Spring MVC (Thymeleaf UI)
- Spring Security
- Spring Data JPA (UUID identifiers)
- MySQL
- OpenFeign
- Spring Cache (Caffeine)
- JUnit + Integration + API tests
- 
📦 Domain Model
Entities (UUID primary keys)
1. Parent
id
name
email
password
role
children (relation)
2. Child
id
name
grade
parent (relation)
3. Wallet
id
parent
balance
transactions (relation)
4. Transaction
id
wallet (relation)
amount
timestamp
5. Lunch
id
child

✨ User Functionalities
The Main Application implements 6+ valid domain functionalities:
1. Create Lunch Order -> Sends request to lunch-svc via Feign Client → updates local Lunch entity → displays confirmation.
2. Cancel Lunch Order
3. Load Money Into Wallet -> Creates Transaction and updates Wallet balance.
4. Assign Child to Parent Account
5. View Last 5 Transactions
6. Admin: View All Users
7. Admin: Change User Status / Delete Users
8. Login, Registration, Profile Edit (supporting features)

🔐 Security
Spring Security with USER and ADMIN roles
Registration and login
Hashed passwords
Role-based access to admin pages
Profile edit available to authenticated users
CSRF enabled
Method-level and URL-level authorization

🔄 Caching
User list caching with Caffeine

🌐 Microservice Communication
Using Feign Client to call:
Create lunch order
Cancel lunch order
Get meals
Get order status

All POST/PUT/DELETE calls are state-changing and satisfy the project requirements.

⚠️ Validation and Error Handling
DTO and entity validation
Custom exception classes
Global exception handlers
Error views for UI
No white-label error pages

🧪 Testing
Main Application includes:
Unit tests
Integration tests
API tests using MockMvc

🗃 Database
JPA/Hibernate
UUID identifiers
One-to-many relations (Parent–Child, Wallet–Transactions)
Passwords hashed
Separate database from microservice

🚀 Run Instructions
Start the microservice first:
cd lunch-svc
mvn spring-boot:run

Then start the Main Application:
cd _School-Lunch
mvn spring-boot:run

📚 Commit Guidelines
Using Conventional Commits:
Examples:
feat: implement lunch order creation
fix: resolve wallet balance calculation
refactor: extract child service logic

🏁 Conclusion
School-Lunch is a complete main application that handles UI, users, roles, wallet management, and communication with the microservice.
