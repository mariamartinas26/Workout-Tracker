# Workout Tracker

A comprehensive web application for managing and tracking your fitness workouts, built with Spring Boot, React, and PostgreSQL.

## 🏋️ Features

### Workout Scheduling
- **Flexible planning**: Create and schedule personalized workout routines
- **Exercise management**: Add, modify, and organize your favorite exercises

### Fitness Goals
Set and track progress for different fitness objectives:
- 🔥 **Weight Loss** - Intensive cardio workouts and calorie-burning programs
- 💪 **Muscle Gain** - Strength and hypertrophy training programs
- 🌱 **Health Maintenance** - Balanced workouts for overall wellness

### Intelligent Recommendations
The app's algorithm provides personalized workout recommendations based on:
- Your fitness goals
- Workout history from the last 90 days
- Your experience level

### Statistics and Analytics
- **Interactive dashboard** with detailed metrics about your progress
- **Progress charts** to track performance over time

### Calendar and Streak Tracking
- **Visual calendar** with color-coded workout intensity
- **Streak counter** to track your consistency

## 🛠️ Tech Stack

### Backend
- **Spring Boot** - Main framework for REST API
- **PostgreSQL** - Relational database
- **Spring Security** - Authentication and authorization with JWT tokens
- **Spring Data JPA** - Data persistence
- **Maven** - Dependency management

### Frontend
- **React** - User interface library
- **JavaScript** - Application logic

### Database
- **PostgreSQL** 

## 🚀 Installation and Setup

### Backend Setup

1. **Clone the repository**
```bash
git clone https://github.com/mariamartinas26/Workout-Tracker.git
cd Workout-Tracker
```

2. **Configure the database**
```sql
CREATE DATABASE workout_tracker;
CREATE USER workout_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE workout_tracker TO workout_user;
```

3. **Configure application.properties**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/workout_tracker
spring.datasource.username=workout_user
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

4. **Run the backend application**
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend Setup

1. **Install dependencies**
```bash
cd frontend
npm install
```

2. **Run the frontend application**
```bash
npm start
```
