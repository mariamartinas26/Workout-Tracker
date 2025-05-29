import React, { useState, useEffect } from 'react';
import Homepage from './components/Homepage';
import Login from './components/Login';
import Register from './components/Register';
import CompleteProfile from './components/CompleteProfile';

const App = () => {
    const [currentView, setCurrentView] = useState('homepage'); // 'homepage', 'login', 'register', 'complete-profile'
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null);
    const [needsProfileCompletion, setNeedsProfileCompletion] = useState(false);

    const [showWorkoutPopup, setShowWorkoutPopup] = useState(false);

    // 3. ADAUGĂ ACESTE DATE PENTRU EXERCIȚII (după state-uri)
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

    // 4. ADAUGĂ ACEASTĂ COMPONENTĂ COMPLETĂ (înainte de useEffect-ul tău existent)
    const AddWorkoutPopup = ({ isOpen, onClose }) => {
        const [workoutData, setWorkoutData] = useState({
            plan_name: '',
            description: '',
            estimated_duration_minutes: '',
            difficulty_level: 1,
            goals: '',
            exercises: []
        });

        const [selectedExercise, setSelectedExercise] = useState('');
        const [exerciseDetails, setExerciseDetails] = useState({
            target_sets: '',
            target_reps_min: '',
            target_reps_max: '',
            target_weight_kg: '',
            target_duration_seconds: '',
            rest_time_seconds: 60,
            notes: ''
        });

        const handleAddExercise = () => {
            if (!selectedExercise) return;

            const exercise = sampleExercises.find(ex => ex.exercise_id === parseInt(selectedExercise));
            const newExercise = {
                ...exercise,
                ...exerciseDetails,
                exercise_order: workoutData.exercises.length + 1
            };

            setWorkoutData(prev => ({
                ...prev,
                exercises: [...prev.exercises, newExercise]
            }));

            // Reset exercise details
            setSelectedExercise('');
            setExerciseDetails({
                target_sets: '',
                target_reps_min: '',
                target_reps_max: '',
                target_weight_kg: '',
                target_duration_seconds: '',
                rest_time_seconds: 60,
                notes: ''
            });
        };

        const handleRemoveExercise = (index) => {
            setWorkoutData(prev => ({
                ...prev,
                exercises: prev.exercises.filter((_, i) => i !== index)
            }));
        };

        const handleSaveWorkout = () => {
            // Aici ai putea trimite datele către backend
            console.log('Saving workout:', workoutData);
            alert('Workout plan saved successfully!');
            onClose();

            // Reset form
            setWorkoutData({
                plan_name: '',
                description: '',
                estimated_duration_minutes: '',
                difficulty_level: 1,
                goals: '',
                exercises: []
            });
        };

        if (!isOpen) return null;

        return (
            <div style={{
                position: 'fixed',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundColor: 'rgba(0, 0, 0, 0.7)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 1000,
                padding: '20px'
            }}>
                <div style={{
                    backgroundColor: 'white',
                    borderRadius: '20px',
                    padding: '32px',
                    maxWidth: '800px',
                    width: '100%',
                    maxHeight: '90vh',
                    overflowY: 'auto',
                    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
                    position: 'relative'
                }}>
                    {/* Close button */}
                    <button
                        onClick={onClose}
                        style={{
                            position: 'absolute',
                            top: '20px',
                            right: '20px',
                            background: 'none',
                            border: 'none',
                            fontSize: '24px',
                            cursor: 'pointer',
                            color: '#718096',
                            width: '40px',
                            height: '40px',
                            borderRadius: '50%',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            transition: 'all 0.2s'
                        }}
                    >
                        ×
                    </button>

                    <h2 style={{
                        color: '#1a202c',
                        fontSize: '28px',
                        fontWeight: '800',
                        marginBottom: '24px',
                        textAlign: 'center',
                        background: 'linear-gradient(135deg, #667eea, #764ba2)',
                        backgroundClip: 'text',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent'
                    }}>
                        Create New Workout Plan
                    </h2>

                    {/* Workout Plan Details */}
                    <div style={{
                        backgroundColor: '#f7fafc',
                        padding: '24px',
                        borderRadius: '16px',
                        marginBottom: '24px'
                    }}>
                        <h3 style={{
                            color: '#2d3748',
                            fontSize: '18px',
                            fontWeight: '700',
                            marginBottom: '16px'
                        }}>
                            Workout Plan Details
                        </h3>

                        <div style={{ display: 'grid', gap: '16px' }}>
                            <div>
                                <label style={{
                                    display: 'block',
                                    marginBottom: '8px',
                                    color: '#4a5568',
                                    fontWeight: '600',
                                    fontSize: '14px'
                                }}>
                                    Plan Name *
                                </label>
                                <input
                                    type="text"
                                    value={workoutData.plan_name}
                                    onChange={(e) => setWorkoutData(prev => ({ ...prev, plan_name: e.target.value }))}
                                    placeholder="e.g., Upper Body Strength"
                                    style={{
                                        width: '100%',
                                        padding: '12px 16px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px',
                                        boxSizing: 'border-box'
                                    }}
                                />
                            </div>

                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                <div>
                                    <label style={{
                                        display: 'block',
                                        marginBottom: '8px',
                                        color: '#4a5568',
                                        fontWeight: '600',
                                        fontSize: '14px'
                                    }}>
                                        Duration (minutes)
                                    </label>
                                    <input
                                        type="number"
                                        value={workoutData.estimated_duration_minutes}
                                        onChange={(e) => setWorkoutData(prev => ({ ...prev, estimated_duration_minutes: e.target.value }))}
                                        placeholder="45"
                                        style={{
                                            width: '100%',
                                            padding: '12px 16px',
                                            border: '2px solid #e2e8f0',
                                            borderRadius: '8px',
                                            fontSize: '14px',
                                            boxSizing: 'border-box'
                                        }}
                                    />
                                </div>
                                <div>
                                    <label style={{
                                        display: 'block',
                                        marginBottom: '8px',
                                        color: '#4a5568',
                                        fontWeight: '600',
                                        fontSize: '14px'
                                    }}>
                                        Difficulty Level
                                    </label>
                                    <select
                                        value={workoutData.difficulty_level}
                                        onChange={(e) => setWorkoutData(prev => ({ ...prev, difficulty_level: parseInt(e.target.value) }))}
                                        style={{
                                            width: '100%',
                                            padding: '12px 16px',
                                            border: '2px solid #e2e8f0',
                                            borderRadius: '8px',
                                            fontSize: '14px',
                                            boxSizing: 'border-box'
                                        }}
                                    >
                                        <option value={1}>1 - Beginner</option>
                                        <option value={2}>2 - Easy</option>
                                        <option value={3}>3 - Moderate</option>
                                        <option value={4}>4 - Hard</option>
                                        <option value={5}>5 - Expert</option>
                                    </select>
                                </div>
                            </div>

                            <div>
                                <label style={{
                                    display: 'block',
                                    marginBottom: '8px',
                                    color: '#4a5568',
                                    fontWeight: '600',
                                    fontSize: '14px'
                                }}>
                                    Goals
                                </label>
                                <textarea
                                    value={workoutData.goals}
                                    onChange={(e) => setWorkoutData(prev => ({ ...prev, goals: e.target.value }))}
                                    placeholder="What do you want to achieve with this workout?"
                                    rows={3}
                                    style={{
                                        width: '100%',
                                        padding: '12px 16px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px',
                                        resize: 'vertical',
                                        boxSizing: 'border-box'
                                    }}
                                />
                            </div>
                        </div>
                    </div>

                    {/* Add Exercise Section */}
                    <div style={{
                        backgroundColor: '#f0fff4',
                        padding: '24px',
                        borderRadius: '16px',
                        marginBottom: '24px',
                        border: '1px solid #c6f6d5'
                    }}>
                        <h3 style={{
                            color: '#2d3748',
                            fontSize: '18px',
                            fontWeight: '700',
                            marginBottom: '16px'
                        }}>
                            Add Exercise
                        </h3>

                        <div style={{ display: 'grid', gap: '16px' }}>
                            <div>
                                <label style={{
                                    display: 'block',
                                    marginBottom: '8px',
                                    color: '#4a5568',
                                    fontWeight: '600',
                                    fontSize: '14px'
                                }}>
                                    Select Exercise *
                                </label>
                                <select
                                    value={selectedExercise}
                                    onChange={(e) => setSelectedExercise(e.target.value)}
                                    style={{
                                        width: '100%',
                                        padding: '12px 16px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px',
                                        boxSizing: 'border-box'
                                    }}
                                >
                                    <option value="">Choose an exercise...</option>
                                    {sampleExercises.map(exercise => (
                                        <option key={exercise.exercise_id} value={exercise.exercise_id}>
                                            {exercise.exercise_name} ({exercise.category})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {selectedExercise && (
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: '16px' }}>
                                    <div>
                                        <label style={{
                                            display: 'block',
                                            marginBottom: '8px',
                                            color: '#4a5568',
                                            fontWeight: '600',
                                            fontSize: '14px'
                                        }}>
                                            Sets
                                        </label>
                                        <input
                                            type="number"
                                            value={exerciseDetails.target_sets}
                                            onChange={(e) => setExerciseDetails(prev => ({ ...prev, target_sets: e.target.value }))}
                                            placeholder="3"
                                            style={{
                                                width: '100%',
                                                padding: '12px 16px',
                                                border: '2px solid #e2e8f0',
                                                borderRadius: '8px',
                                                fontSize: '14px',
                                                boxSizing: 'border-box'
                                            }}
                                        />
                                    </div>
                                    <div>
                                        <label style={{
                                            display: 'block',
                                            marginBottom: '8px',
                                            color: '#4a5568',
                                            fontWeight: '600',
                                            fontSize: '14px'
                                        }}>
                                            Reps (Min)
                                        </label>
                                        <input
                                            type="number"
                                            value={exerciseDetails.target_reps_min}
                                            onChange={(e) => setExerciseDetails(prev => ({ ...prev, target_reps_min: e.target.value }))}
                                            placeholder="8"
                                            style={{
                                                width: '100%',
                                                padding: '12px 16px',
                                                border: '2px solid #e2e8f0',
                                                borderRadius: '8px',
                                                fontSize: '14px',
                                                boxSizing: 'border-box'
                                            }}
                                        />
                                    </div>
                                    <div>
                                        <label style={{
                                            display: 'block',
                                            marginBottom: '8px',
                                            color: '#4a5568',
                                            fontWeight: '600',
                                            fontSize: '14px'
                                        }}>
                                            Reps (Max)
                                        </label>
                                        <input
                                            type="number"
                                            value={exerciseDetails.target_reps_max}
                                            onChange={(e) => setExerciseDetails(prev => ({ ...prev, target_reps_max: e.target.value }))}
                                            placeholder="12"
                                            style={{
                                                width: '100%',
                                                padding: '12px 16px',
                                                border: '2px solid #e2e8f0',
                                                borderRadius: '8px',
                                                fontSize: '14px',
                                                boxSizing: 'border-box'
                                            }}
                                        />
                                    </div>
                                    <div>
                                        <label style={{
                                            display: 'block',
                                            marginBottom: '8px',
                                            color: '#4a5568',
                                            fontWeight: '600',
                                            fontSize: '14px'
                                        }}>
                                            Weight (kg)
                                        </label>
                                        <input
                                            type="number"
                                            step="0.1"
                                            value={exerciseDetails.target_weight_kg}
                                            onChange={(e) => setExerciseDetails(prev => ({ ...prev, target_weight_kg: e.target.value }))}
                                            placeholder="20"
                                            style={{
                                                width: '100%',
                                                padding: '12px 16px',
                                                border: '2px solid #e2e8f0',
                                                borderRadius: '8px',
                                                fontSize: '14px',
                                                boxSizing: 'border-box'
                                            }}
                                        />
                                    </div>
                                    <div>
                                        <label style={{
                                            display: 'block',
                                            marginBottom: '8px',
                                            color: '#4a5568',
                                            fontWeight: '600',
                                            fontSize: '14px'
                                        }}>
                                            Duration (sec)
                                        </label>
                                        <input
                                            type="number"
                                            value={exerciseDetails.target_duration_seconds}
                                            onChange={(e) => setExerciseDetails(prev => ({ ...prev, target_duration_seconds: e.target.value }))}
                                            placeholder="30"
                                            style={{
                                                width: '100%',
                                                padding: '12px 16px',
                                                border: '2px solid #e2e8f0',
                                                borderRadius: '8px',
                                                fontSize: '14px',
                                                boxSizing: 'border-box'
                                            }}
                                        />
                                    </div>
                                    <div>
                                        <label style={{
                                            display: 'block',
                                            marginBottom: '8px',
                                            color: '#4a5568',
                                            fontWeight: '600',
                                            fontSize: '14px'
                                        }}>
                                            Rest (sec)
                                        </label>
                                        <input
                                            type="number"
                                            value={exerciseDetails.rest_time_seconds}
                                            onChange={(e) => setExerciseDetails(prev => ({ ...prev, rest_time_seconds: e.target.value }))}
                                            placeholder="60"
                                            style={{
                                                width: '100%',
                                                padding: '12px 16px',
                                                border: '2px solid #e2e8f0',
                                                borderRadius: '8px',
                                                fontSize: '14px',
                                                boxSizing: 'border-box'
                                            }}
                                        />
                                    </div>
                                </div>
                            )}

                            {selectedExercise && (
                                <button
                                    onClick={handleAddExercise}
                                    style={{
                                        background: 'linear-gradient(135deg, #48bb78, #38a169)',
                                        color: 'white',
                                        border: 'none',
                                        padding: '12px 24px',
                                        borderRadius: '8px',
                                        cursor: 'pointer',
                                        fontSize: '14px',
                                        fontWeight: '600',
                                        alignSelf: 'start'
                                    }}
                                >
                                    Add Exercise
                                </button>
                            )}
                        </div>
                    </div>

                    {/* Added Exercises List */}
                    {workoutData.exercises.length > 0 && (
                        <div style={{
                            backgroundColor: '#fffbf0',
                            padding: '24px',
                            borderRadius: '16px',
                            marginBottom: '24px',
                            border: '1px solid #fed7aa'
                        }}>
                            <h3 style={{
                                color: '#2d3748',
                                fontSize: '18px',
                                fontWeight: '700',
                                marginBottom: '16px'
                            }}>
                                Added Exercises ({workoutData.exercises.length})
                            </h3>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                {workoutData.exercises.map((exercise, index) => (
                                    <div key={index} style={{
                                        backgroundColor: 'white',
                                        padding: '16px',
                                        borderRadius: '8px',
                                        border: '1px solid #e2e8f0',
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center'
                                    }}>
                                        <div style={{ flex: 1 }}>
                                            <div style={{
                                                color: '#2d3748',
                                                fontWeight: '600',
                                                marginBottom: '4px'
                                            }}>
                                                {exercise.exercise_name}
                                            </div>
                                            <div style={{
                                                color: '#718096',
                                                fontSize: '14px'
                                            }}>
                                                {exercise.target_sets && `${exercise.target_sets} sets`}
                                                {exercise.target_reps_min && ` • ${exercise.target_reps_min}-${exercise.target_reps_max || exercise.target_reps_min} reps`}
                                                {exercise.target_weight_kg && ` • ${exercise.target_weight_kg}kg`}
                                                {exercise.target_duration_seconds && ` • ${exercise.target_duration_seconds}s`}
                                            </div>
                                        </div>
                                        <button
                                            onClick={() => handleRemoveExercise(index)}
                                            style={{
                                                background: '#fed7d7',
                                                color: '#c53030',
                                                border: 'none',
                                                padding: '8px 12px',
                                                borderRadius: '6px',
                                                cursor: 'pointer',
                                                fontSize: '12px',
                                                fontWeight: '600'
                                            }}
                                        >
                                            Remove
                                        </button>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Action Buttons */}
                    <div style={{
                        display: 'flex',
                        gap: '12px',
                        justifyContent: 'flex-end'
                    }}>
                        <button
                            onClick={onClose}
                            style={{
                                background: '#f7fafc',
                                color: '#4a5568',
                                border: '2px solid #e2e8f0',
                                padding: '12px 24px',
                                borderRadius: '10px',
                                cursor: 'pointer',
                                fontSize: '14px',
                                fontWeight: '600'
                            }}
                        >
                            Cancel
                        </button>
                        <button
                            onClick={handleSaveWorkout}
                            disabled={!workoutData.plan_name || workoutData.exercises.length === 0}
                            style={{
                                background: workoutData.plan_name && workoutData.exercises.length > 0
                                    ? 'linear-gradient(135deg, #667eea, #764ba2)'
                                    : '#cbd5e0',
                                color: 'white',
                                border: 'none',
                                padding: '12px 24px',
                                borderRadius: '10px',
                                cursor: workoutData.plan_name && workoutData.exercises.length > 0 ? 'pointer' : 'not-allowed',
                                fontSize: '14px',
                                fontWeight: '600'
                            }}
                        >
                            Save Workout Plan
                        </button>
                    </div>
                </div>
            </div>
        );
    };
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
            <div style={{
                minHeight: '100vh',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                padding: '20px',
                fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, sans-serif"
            }}>
                {/* Background decorative elements */}
                <div style={{
                    position: 'fixed',
                    top: '10%',
                    left: '5%',
                    width: '300px',
                    height: '300px',
                    background: 'rgba(255,255,255,0.05)',
                    borderRadius: '50%',
                    filter: 'blur(80px)',
                    animation: 'float 8s ease-in-out infinite',
                    zIndex: 0
                }}></div>
                <div style={{
                    position: 'fixed',
                    bottom: '15%',
                    right: '10%',
                    width: '200px',
                    height: '200px',
                    background: 'rgba(255,255,255,0.03)',
                    borderRadius: '50%',
                    filter: 'blur(60px)',
                    animation: 'float 10s ease-in-out infinite reverse',
                    zIndex: 0
                }}></div>

                <div style={{
                    maxWidth: '1200px',
                    margin: '0 auto',
                    position: 'relative',
                    zIndex: 1
                }}>
                    {/* Header */}
                    <div style={{
                        backgroundColor: 'rgba(255,255,255,0.95)',
                        backdropFilter: 'blur(20px)',
                        borderRadius: '20px',
                        boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
                        padding: '24px 32px',
                        marginBottom: '24px',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        border: '1px solid rgba(255,255,255,0.2)'
                    }}>
                        <div style={{ display: 'flex', alignItems: 'center' }}>
                            <div style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                width: '48px',
                                height: '48px',
                                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                borderRadius: '12px',
                                marginRight: '16px',
                                boxShadow: '0 4px 16px rgba(102, 126, 234, 0.3)'
                            }}>
                                <span style={{ fontSize: '20px', color: 'white' }}>💪</span>
                            </div>
                            <div>
                                <h1 style={{
                                    margin: '0',
                                    color: '#1a202c',
                                    fontSize: '24px',
                                    fontWeight: '800',
                                    letterSpacing: '-0.5px'
                                }}>
                                    WorkoutTracker
                                </h1>
                                {user && (
                                    <p style={{
                                        margin: '2px 0 0 0',
                                        color: '#718096',
                                        fontSize: '14px',
                                        fontWeight: '500'
                                    }}>
                                        Welcome back, {user.firstName} {user.lastName}!
                                    </p>
                                )}
                            </div>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                            <button
                                onClick={() => {
                                    setNeedsProfileCompletion(true);
                                    setCurrentView('complete-profile');
                                }}
                                style={{
                                    background: 'linear-gradient(135deg, #38b2ac, #319795)',
                                    color: 'white',
                                    border: 'none',
                                    padding: '12px 20px',
                                    borderRadius: '10px',
                                    cursor: 'pointer',
                                    fontSize: '14px',
                                    fontWeight: '600',
                                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                    boxShadow: '0 4px 16px rgba(56, 178, 172, 0.3)',
                                    letterSpacing: '0.025em'
                                }}
                                onMouseEnter={(e) => {
                                    e.target.style.transform = 'translateY(-2px)';
                                    e.target.style.boxShadow = '0 6px 20px rgba(56, 178, 172, 0.4)';
                                }}
                                onMouseLeave={(e) => {
                                    e.target.style.transform = 'translateY(0)';
                                    e.target.style.boxShadow = '0 4px 16px rgba(56, 178, 172, 0.3)';
                                }}
                            >
                                Edit Profile
                            </button>
                            <button
                                onClick={handleLogout}
                                style={{
                                    background: 'linear-gradient(135deg, #e53e3e, #c53030)',
                                    color: 'white',
                                    border: 'none',
                                    padding: '12px 24px',
                                    borderRadius: '10px',
                                    cursor: 'pointer',
                                    fontSize: '14px',
                                    fontWeight: '600',
                                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                    boxShadow: '0 4px 16px rgba(229, 62, 62, 0.3)',
                                    letterSpacing: '0.025em'
                                }}
                                onMouseEnter={(e) => {
                                    e.target.style.transform = 'translateY(-2px)';
                                    e.target.style.boxShadow = '0 6px 20px rgba(229, 62, 62, 0.4)';
                                }}
                                onMouseLeave={(e) => {
                                    e.target.style.transform = 'translateY(0)';
                                    e.target.style.boxShadow = '0 4px 16px rgba(229, 62, 62, 0.3)';
                                }}
                            >
                                Sign Out
                            </button>
                        </div>
                    </div>

                    {/* Dashboard Content */}
                    <div style={{
                        backgroundColor: 'rgba(255,255,255,0.95)',
                        backdropFilter: 'blur(20px)',
                        borderRadius: '20px',
                        boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
                        padding: '48px',
                        textAlign: 'center',
                        border: '1px solid rgba(255,255,255,0.2)',
                        position: 'relative',
                        overflow: 'hidden'
                    }}>
                        {/* Subtle gradient overlay */}
                        <div style={{
                            position: 'absolute',
                            top: 0,
                            left: 0,
                            right: 0,
                            height: '4px',
                            background: 'linear-gradient(90deg, #667eea, #764ba2, #f093fb)',
                            borderRadius: '20px 20px 0 0'
                        }}></div>

                        <div style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: '100px',
                            height: '100px',
                            background: 'linear-gradient(135deg, #667eea, #764ba2)',
                            borderRadius: '25px',
                            marginBottom: '32px',
                            boxShadow: '0 12px 40px rgba(102, 126, 234, 0.3)'
                        }}>
                            <span style={{ fontSize: '40px', color: 'white' }}>🎯</span>
                        </div>

                        <h2 style={{
                            color: '#1a202c',
                            fontSize: '40px',
                            fontWeight: '800',
                            marginBottom: '16px',
                            letterSpacing: '-1px',
                            background: 'linear-gradient(135deg, #667eea, #764ba2)',
                            backgroundClip: 'text',
                            WebkitBackgroundClip: 'text',
                            WebkitTextFillColor: 'transparent'
                        }}>
                            Welcome to WorkoutTracker!
                        </h2>
                        <p style={{
                            color: '#718096',
                            fontSize: '20px',
                            marginBottom: '48px',
                            lineHeight: '1.6',
                            fontWeight: '500',
                            maxWidth: '600px',
                            margin: '0 auto 48px auto'
                        }}>
                            Your personal fitness companion to track workouts, monitor progress, and achieve your goals.
                        </p>

                        {/* User Profile Summary */}
                        {user && (user.dateOfBirth || user.heightCm || user.weightKg || user.fitnessLevel) && (
                            <div style={{
                                background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.08), rgba(118, 75, 162, 0.05))',
                                borderRadius: '16px',
                                padding: '24px',
                                marginBottom: '40px',
                                border: '1px solid rgba(102, 126, 234, 0.1)'
                            }}>
                                <h3 style={{
                                    color: '#1a202c',
                                    fontSize: '18px',
                                    fontWeight: '700',
                                    marginBottom: '16px',
                                    letterSpacing: '-0.25px'
                                }}>
                                    Your Profile
                                </h3>
                                <div style={{
                                    display: 'grid',
                                    gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
                                    gap: '16px',
                                    textAlign: 'center'
                                }}>
                                    {user.dateOfBirth && (
                                        <div>
                                            <div style={{
                                                color: '#667eea',
                                                fontSize: '20px',
                                                fontWeight: '800',
                                                marginBottom: '4px'
                                            }}>
                                                {Math.floor((new Date() - new Date(user.dateOfBirth)) / (365.25 * 24 * 60 * 60 * 1000))}
                                            </div>
                                            <div style={{
                                                color: '#718096',
                                                fontSize: '12px',
                                                fontWeight: '600',
                                                textTransform: 'uppercase',
                                                letterSpacing: '0.5px'
                                            }}>
                                                Years Old
                                            </div>
                                        </div>
                                    )}
                                    {user.heightCm && (
                                        <div>
                                            <div style={{
                                                color: '#667eea',
                                                fontSize: '20px',
                                                fontWeight: '800',
                                                marginBottom: '4px'
                                            }}>
                                                {user.heightCm}cm
                                            </div>
                                            <div style={{
                                                color: '#718096',
                                                fontSize: '12px',
                                                fontWeight: '600',
                                                textTransform: 'uppercase',
                                                letterSpacing: '0.5px'
                                            }}>
                                                Height
                                            </div>
                                        </div>
                                    )}
                                    {user.weightKg && (
                                        <div>
                                            <div style={{
                                                color: '#667eea',
                                                fontSize: '20px',
                                                fontWeight: '800',
                                                marginBottom: '4px'
                                            }}>
                                                {user.weightKg}kg
                                            </div>
                                            <div style={{
                                                color: '#718096',
                                                fontSize: '12px',
                                                fontWeight: '600',
                                                textTransform: 'uppercase',
                                                letterSpacing: '0.5px'
                                            }}>
                                                Weight
                                            </div>
                                        </div>
                                    )}
                                    {user.fitnessLevel && (
                                        <div>
                                            <div style={{
                                                color: '#667eea',
                                                fontSize: '16px',
                                                fontWeight: '800',
                                                marginBottom: '4px',
                                                textTransform: 'capitalize'
                                            }}>
                                                {user.fitnessLevel.toLowerCase()}
                                            </div>
                                            <div style={{
                                                color: '#718096',
                                                fontSize: '12px',
                                                fontWeight: '600',
                                                textTransform: 'uppercase',
                                                letterSpacing: '0.5px'
                                            }}>
                                                Fitness Level
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}

                        <div style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                            gap: '24px',
                            marginTop: '48px'
                        }}>
                            <div style={{
                                background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.05))',
                                padding: '32px',
                                borderRadius: '16px',
                                border: '1px solid rgba(102, 126, 234, 0.1)',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                cursor: 'pointer',
                                position: 'relative',
                                overflow: 'hidden'
                            }}
                                 onClick={() => setShowWorkoutPopup(true)}
                                 onMouseEnter={(e) => {
                                     e.target.style.transform = 'translateY(-4px)';
                                     e.target.style.boxShadow = '0 16px 40px rgba(102, 126, 234, 0.15)';
                                 }}
                                 onMouseLeave={(e) => {
                                     e.target.style.transform = 'translateY(0)';
                                     e.target.style.boxShadow = 'none';
                                 }}>
                                <div style={{
                                    width: '56px',
                                    height: '56px',
                                    background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                    borderRadius: '14px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    marginBottom: '20px',
                                    boxShadow: '0 8px 24px rgba(102, 126, 234, 0.3)'
                                }}>
                                    <span style={{ fontSize: '24px', color: 'white' }}>🏋️</span>
                                </div>
                                <h3 style={{
                                    color: '#1a202c',
                                    fontSize: '20px',
                                    fontWeight: '700',
                                    marginBottom: '12px',
                                    letterSpacing: '-0.25px'
                                }}>
                                    Track Workouts
                                </h3>
                                <p style={{
                                    color: '#718096',
                                    fontSize: '15px',
                                    margin: '0',
                                    lineHeight: '1.5',
                                    fontWeight: '500'
                                }}>
                                    Log and monitor your daily training sessions with detailed exercise tracking
                                </p>
                            </div>

                            <div style={{
                                background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.05))',
                                padding: '32px',
                                borderRadius: '16px',
                                border: '1px solid rgba(102, 126, 234, 0.1)',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                cursor: 'pointer',
                                position: 'relative',
                                overflow: 'hidden'
                            }}
                                 onMouseEnter={(e) => {
                                     e.target.style.transform = 'translateY(-4px)';
                                     e.target.style.boxShadow = '0 16px 40px rgba(102, 126, 234, 0.15)';
                                 }}
                                 onMouseLeave={(e) => {
                                     e.target.style.transform = 'translateY(0)';
                                     e.target.style.boxShadow = 'none';
                                 }}>
                                <div style={{
                                    width: '56px',
                                    height: '56px',
                                    background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                    borderRadius: '14px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    marginBottom: '20px',
                                    boxShadow: '0 8px 24px rgba(102, 126, 234, 0.3)'
                                }}>
                                    <span style={{ fontSize: '24px', color: 'white' }}>📈</span>
                                </div>
                                <h3 style={{
                                    color: '#1a202c',
                                    fontSize: '20px',
                                    fontWeight: '700',
                                    marginBottom: '12px',
                                    letterSpacing: '-0.25px'
                                }}>
                                    Monitor Progress
                                </h3>
                                <p style={{
                                    color: '#718096',
                                    fontSize: '15px',
                                    margin: '0',
                                    lineHeight: '1.5',
                                    fontWeight: '500'
                                }}>
                                    Visualize your fitness journey with comprehensive analytics and insights
                                </p>
                            </div>

                            <div style={{
                                background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.05))',
                                padding: '32px',
                                borderRadius: '16px',
                                border: '1px solid rgba(102, 126, 234, 0.1)',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                cursor: 'pointer',
                                position: 'relative',
                                overflow: 'hidden'
                            }}
                                 onMouseEnter={(e) => {
                                     e.target.style.transform = 'translateY(-4px)';
                                     e.target.style.boxShadow = '0 16px 40px rgba(102, 126, 234, 0.15)';
                                 }}
                                 onMouseLeave={(e) => {
                                     e.target.style.transform = 'translateY(0)';
                                     e.target.style.boxShadow = 'none';
                                 }}>
                                <div style={{
                                    width: '56px',
                                    height: '56px',
                                    background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                    borderRadius: '14px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    marginBottom: '20px',
                                    boxShadow: '0 8px 24px rgba(102, 126, 234, 0.3)'
                                }}>
                                    <span style={{ fontSize: '24px', color: 'white' }}>🎯</span>
                                </div>
                                <h3 style={{
                                    color: '#1a202c',
                                    fontSize: '20px',
                                    fontWeight: '700',
                                    marginBottom: '12px',
                                    letterSpacing: '-0.25px'
                                }}>
                                    Achieve Goals
                                </h3>
                                <p style={{
                                    color: '#718096',
                                    fontSize: '15px',
                                    margin: '0',
                                    lineHeight: '1.5',
                                    fontWeight: '500'
                                }}>
                                    Set personalized fitness targets and celebrate your achievements
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
                <AddWorkoutPopup
                    isOpen={showWorkoutPopup}
                    onClose={() => setShowWorkoutPopup(false)}
                />
                <style>
                    {`
                        @keyframes float {
                            0%, 100% { transform: translateY(0px); }
                            50% { transform: translateY(-20px); }
                        }
                    `}
                </style>
            </div>
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