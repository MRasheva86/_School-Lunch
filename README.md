_School-Lunch is the main application of the School Lunch Management System.  
It provides a user-facing interface for parents, students, school administrators, and restaurant staff to manage school lunch registrations, orders, and balances.

This project communicates with the separate microservice `lunch-svc`, which handles all lunch-related backend operations.

## 🚀 Features
- Parent and student registration
- Authentication and authorization
- Student profiles and lunch information
- Ordering and managing school lunches
- Viewing balances and wallet information
- Integration with the `lunch-svc` REST microservice
- Clean, modular design suitable for production

## 🧱 Architecture Overview
The `_School-Lunch` project acts as the **main client** and orchestrator, forwarding business operations to the `lunch-svc` backend via REST calls.

## 🛠️ Technologies Used
- **Java 17+**
- **Spring Boot**
- **Spring Web / MVC**
- **Spring Security**
- **REST API integration**
- **Maven**

## 🔧 Installation & Setup
- Clone the repository
git clone https://github.com/MRasheva86/_School-Lunch.git
cd _School-Lunch
- Update your application.properties or application.yml with the URL of your lunch-svc instance.
- Run the application
