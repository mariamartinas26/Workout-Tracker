import React, { useState, useEffect } from 'react';
import Homepage from './components/Homepage';
import Login from './components/Login';
import Register from './components/Register';
import CompleteProfile from './components/CompleteProfile';
import Goals from './components/Goals';
import Dashboard from './components/Dashboard';

const App = () => {
    const [currentView, setCurrentView] = useState('homepage'); // 'homepage', 'login', 'register', 'complete-profile', 'goals'
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null);
    const [needsProfileCompletion, setNeedsProfileCompletion] = useState(false);

    // Sample exercises data
    const sampleExercises = [
        { exercise_id: 1, exercise_name: 'Push-ups', category: 'STRENGTH', primary_muscle_group: 'CHEST' },
        { exercise_id: 2, exercise_name: 'Squats', category: 'STRENGTH', primary_muscle_group: 'QUADRICEPS' },
        { exercise_id: 3, exercise_name: 'Pull-ups', category: 'STRENGTH', primary_muscle_group: 'BACK' },
        { exercise_id: 4, exercise_name: 'Plank', category: 'STRENGTH', primary_muscle_group: 'ABS' },
        { exercise_id: 5, exercise_name: 'Running', category: 'CARDIO', primary_muscle_group: 'CARDIO' },
        { exercise_id: 6, exercise_name: 'Deadlifts', category: 'STRENGTH', primary_muscle_group: 'BACK' },
        { exercise_id: 7, exercise_name: 'Bench Press', category: 'STRENGTH', primary_muscle_group: 'CHEST' },
        { exercise_id: 8, exercise_name: 'Lunges', category: 'STRENGTH', primary_muscle_group: 'QUADRICEPS' },
        { exercise_id: 9, exercise_name: 'Bicep Curls', category: 'STRENGTH', primary_muscle_group: 'BICEPS' },
        { exercise_id: 10, exercise_name: 'Tricep Dips', category: 'STRENGTH', primary_muscle_group: 'TRICEPS' }
    ];

    // Check if user is already authenticated on app load
    useEffect(() => {
        const isAuth = localStorage.getItem('isAuthenticated');
        const userData = localStorage.getItem('userData');

        if (isAuth === 'true' && userData) {
            const parsedUser = JSON.parse(userData);
            setIsAuthenticated(true);
            setUser(parsedUser);

            // Check if profile needs completion
            checkProfileCompletion(parsedUser);
        }
    }, []);

    // Check if user profile needs completion
    const checkProfileCompletion = (userData) => {
        // If any of these fields are missing, show profile completion
        const hasIncompleteProfile = !userData.dateOfBirth ||
            !userData.heightCm ||
            !userData.weightKg ||
            !userData.fitnessLevel;

        if (hasIncompleteProfile) {
            setNeedsProfileCompletion(true);
            setCurrentView('complete-profile');
        }
    };

    const handleSwitchToLogin = () => {
        setCurrentView('login');
    };

    const handleSwitchToRegister = () => {
        setCurrentView('register');
    };

    const handleSwitchToHomepage = () => {
        setCurrentView('homepage');
    };

    const handleLoginSuccess = (data) => {
        setIsAuthenticated(true);
        setUser(data);

        // Store user data in localStorage
        localStorage.setItem('isAuthenticated', 'true');
        localStorage.setItem('userData', JSON.stringify(data));

        // Check if profile needs completion
        checkProfileCompletion(data);
    };

    const handleRegistrationSuccess = (data) => {
        setIsAuthenticated(true);
        setUser(data);

        // Store user data in localStorage
        localStorage.setItem('isAuthenticated', 'true');
        localStorage.setItem('userData', JSON.stringify(data));

        // After registration, always show profile completion
        setNeedsProfileCompletion(true);
        setCurrentView('complete-profile');
    };

    const handleProfileComplete = (updatedUser) => {
        // Update user data with profile information
        setUser(updatedUser);
        localStorage.setItem('userData', JSON.stringify(updatedUser));

        // Profile is now complete
        setNeedsProfileCompletion(false);
        setCurrentView('dashboard');
    };

    const handleSkipProfile = () => {
        // User chose to skip profile completion
        setNeedsProfileCompletion(false);
        setCurrentView('dashboard');
    };

    const handleLogout = () => {
        localStorage.removeItem('isAuthenticated');
        localStorage.removeItem('userData');
        setIsAuthenticated(false);
        setUser(null);
        setNeedsProfileCompletion(false);
        setCurrentView('homepage');
    };

    const handleGoalSet = (goalData) => {
        console.log('Goal set:', goalData);
        // Here you would save the goal to backend
        setCurrentView('dashboard');
    };

    const handleGoToGoals = () => {
        setCurrentView('goals');
    };

    const handleBackToDashboard = () => {
        setCurrentView('dashboard');
    };

    const handleEditProfile = () => {
        setNeedsProfileCompletion(true);
        setCurrentView('complete-profile');
    };

    // Show Goals page if selected
    if (isAuthenticated && currentView === 'goals') {
        return (
            <Goals
                user={user}
                onBack={handleBackToDashboard}
                onGoalSet={handleGoalSet}
            />
        );
    }

    // Show profile completion if needed
    if (isAuthenticated && needsProfileCompletion && currentView === 'complete-profile') {
        return (
            <CompleteProfile
                user={user}
                onProfileComplete={handleProfileComplete}
                onSkip={handleSkipProfile}
            />
        );
    }

    // If user is authenticated and profile is complete, show dashboard
    if (isAuthenticated && !needsProfileCompletion) {
        return (
            <Dashboard
                user={user}
                sampleExercises={sampleExercises}
                onLogout={handleLogout}
                onEditProfile={handleEditProfile}
                onGoToGoals={handleGoToGoals}
            />
        );
    }

    // If not authenticated, show homepage/login/register forms
    return (
        <div>
            {currentView === 'homepage' ? (
                <Homepage
                    onLogin={handleSwitchToLogin}
                    onRegister={handleSwitchToRegister}
                />
            ) : currentView === 'login' ? (
                <Login
                    onSwitchToRegister={handleSwitchToRegister}
                    onLoginSuccess={handleLoginSuccess}
                />
            ) : (
                <Register
                    onSwitchToLogin={handleSwitchToLogin}
                    onRegistrationSuccess={handleRegistrationSuccess}
                />
            )}
        </div>
    );
};

export default App;