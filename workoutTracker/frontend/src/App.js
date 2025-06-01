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


            checkProfileCompletion(parsedUser);
        }
    }, []);


    const checkProfileCompletion = (userData) => {

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

        localStorage.setItem('isAuthenticated', 'true');
        localStorage.setItem('userData', JSON.stringify(data));

        checkProfileCompletion(data);
    };

    const handleRegistrationSuccess = (data) => {
        setIsAuthenticated(true);
        setUser(data);

        localStorage.setItem('isAuthenticated', 'true');
        localStorage.setItem('userData', JSON.stringify(data));

        setNeedsProfileCompletion(true);
        setCurrentView('complete-profile');
    };

    const handleProfileComplete = (updatedUser) => {

        setUser(updatedUser);
        localStorage.setItem('userData', JSON.stringify(updatedUser));

        setNeedsProfileCompletion(false);
        setCurrentView('dashboard');
    };

    const handleSkipProfile = () => {
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

    if (isAuthenticated && currentView === 'goals') {
        return (
            <Goals
                user={user}
                onBack={handleBackToDashboard}
                onGoalSet={handleGoalSet}
            />
        );
    }

    if (isAuthenticated && needsProfileCompletion && currentView === 'complete-profile') {
        return (
            <CompleteProfile
                user={user}
                onProfileComplete={handleProfileComplete}
                onSkip={handleSkipProfile}
            />
        );
    }

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