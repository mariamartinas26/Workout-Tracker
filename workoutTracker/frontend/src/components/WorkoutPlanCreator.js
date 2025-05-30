// components/WorkoutPlanCreator.js
import React, { useState } from 'react';

const API_BASE_URL = 'http://localhost:8082/api';

// Service pentru workout plans - folosește endpoint-ul corect
const WorkoutPlanService = {
    createWorkoutPlan: async (planData) => {
        console.log('Creez plan de workout cu datele:', planData);

        const response = await fetch(`${API_BASE_URL}/workout-plans`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(planData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to create workout plan');
        }

        const result = await response.json();
        return result;
    }
};

const WorkoutPlanCreator = ({ isOpen, onClose, sampleExercises = [], currentUserId = 1 }) => {
    const [workoutData, setWorkoutData] = useState({
        plan_name: '',
        description: '',
        estimated_duration_minutes: '',
        difficulty_level: 1,
        goals: '',
        notes: '',
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

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

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

    const handleSaveWorkout = async () => {
        // Validări
        if (!workoutData.plan_name.trim()) {
            setError('Numele planului este obligatoriu');
            return;
        }

        if (workoutData.exercises.length === 0) {
            setError('Adaugă cel puțin un exercițiu');
            return;
        }

        setLoading(true);
        setError('');

        try {
            // Formatează datele pentru backend conform cu CreateWorkoutPlanRequest
            const backendData = {
                userId: currentUserId,                    // ✅ User ID
                planName: workoutData.plan_name,          // ✅ Numele planului
                description: workoutData.description || null,
                estimatedDurationMinutes: workoutData.estimated_duration_minutes ? parseInt(workoutData.estimated_duration_minutes) : null,
                difficultyLevel: workoutData.difficulty_level,
                goals: workoutData.goals || null,
                notes: workoutData.notes || null,
                exercises: workoutData.exercises.map((exercise, index) => ({
                    exerciseId: exercise.exercise_id,
                    exerciseOrder: index + 1,
                    targetSets: exercise.target_sets ? parseInt(exercise.target_sets) : null,
                    targetRepsMin: exercise.target_reps_min ? parseInt(exercise.target_reps_min) : null,
                    targetRepsMax: exercise.target_reps_max ? parseInt(exercise.target_reps_max) : null,
                    targetWeightKg: exercise.target_weight_kg ? parseFloat(exercise.target_weight_kg) : null,
                    targetDurationSeconds: exercise.target_duration_seconds ? parseInt(exercise.target_duration_seconds) : null,
                    restTimeSeconds: exercise.rest_time_seconds ? parseInt(exercise.rest_time_seconds) : 60,
                    notes: exercise.notes || null
                }))

            };

            console.log('Trimit către backend:', backendData);

            // Trimite către backend folosind endpoint-ul corect
            const response = await WorkoutPlanService.createWorkoutPlan(backendData);

            console.log('Răspuns de la backend:', response);

            // Success message în română conform cu răspunsul backend-ului
            alert(`${response.message}\nPlan: "${response.planName}"\nID: ${response.workoutPlanId}\nExercitii: ${response.totalExercises}`);

            console.log('Plan de workout creat cu ID:', response.workoutPlanId);

            // Închide popup-ul și resetează formularul
            handleClosePopup();

        } catch (error) {
            console.error('Eroare la salvarea planului:', error);
            setError(error.message || 'A apărut o eroare la salvarea planului de workout');
        } finally {
            setLoading(false);
        }
    };

    const handleClosePopup = () => {
        // Resetează toate datele
        setWorkoutData({
            plan_name: '',
            description: '',
            estimated_duration_minutes: '',
            difficulty_level: 1,
            goals: '',
            notes: '',
            exercises: []
        });
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
        setError('');
        setLoading(false);
        onClose();
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
                    onClick={handleClosePopup}
                    disabled={loading}
                    style={{
                        position: 'absolute',
                        top: '20px',
                        right: '20px',
                        background: 'none',
                        border: 'none',
                        fontSize: '24px',
                        cursor: loading ? 'not-allowed' : 'pointer',
                        color: '#718096',
                        width: '40px',
                        height: '40px',
                        borderRadius: '50%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        transition: 'all 0.2s',
                        opacity: loading ? 0.5 : 1
                    }}
                >
                    ×
                </button>

                <h2 style={{
                    color: '#000000',
                    fontSize: '28px',
                    fontWeight: '800',
                    marginBottom: '24px',
                    textAlign: 'center',
                    backgroundClip: 'text',
                    WebkitBackgroundClip: 'text',
                }}>
                    Create New Workout Plan
                </h2>

                {/* Error message */}
                {error && (
                    <div style={{
                        backgroundColor: '#fed7d7',
                        color: '#c53030',
                        padding: '12px 16px',
                        borderRadius: '8px',
                        marginBottom: '24px',
                        border: '1px solid #feb2b2',
                        fontSize: '14px',
                        fontWeight: '600'
                    }}>
                        ⚠️ {error}
                    </div>
                )}

                {/* Loading overlay */}
                {loading && (
                    <div style={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        backgroundColor: 'rgba(255, 255, 255, 0.9)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        borderRadius: '20px',
                        zIndex: 10
                    }}>
                        <div style={{
                            textAlign: 'center',
                            color: '#4a5568'
                        }}>
                            <div style={{
                                fontSize: '24px',
                                marginBottom: '16px',
                                animation: 'spin 1s linear infinite'
                            }}>⚡</div>
                            <div style={{ fontWeight: '600' }}>Se creează planul de workout...</div>
                        </div>
                    </div>
                )}

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
                                disabled={loading}
                                style={{
                                    width: '100%',
                                    padding: '12px 16px',
                                    border: '2px solid #e2e8f0',
                                    borderRadius: '8px',
                                    fontSize: '14px',
                                    boxSizing: 'border-box',
                                    opacity: loading ? 0.6 : 1
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
                                    disabled={loading}
                                    style={{
                                        width: '100%',
                                        padding: '12px 16px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px',
                                        boxSizing: 'border-box',
                                        opacity: loading ? 0.6 : 1
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
                                    disabled={loading}
                                    style={{
                                        width: '100%',
                                        padding: '12px 16px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px',
                                        boxSizing: 'border-box',
                                        opacity: loading ? 0.6 : 1
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
                                Description
                            </label>
                            <textarea
                                value={workoutData.description}
                                onChange={(e) => setWorkoutData(prev => ({ ...prev, description: e.target.value }))}
                                placeholder="Describe your workout plan..."
                                rows={2}
                                disabled={loading}
                                style={{
                                    width: '100%',
                                    padding: '12px 16px',
                                    border: '2px solid #e2e8f0',
                                    borderRadius: '8px',
                                    fontSize: '14px',
                                    resize: 'vertical',
                                    boxSizing: 'border-box',
                                    opacity: loading ? 0.6 : 1
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
                                Goals
                            </label>
                            <textarea
                                value={workoutData.goals}
                                onChange={(e) => setWorkoutData(prev => ({ ...prev, goals: e.target.value }))}
                                placeholder="What do you want to achieve with this workout?"
                                rows={2}
                                disabled={loading}
                                style={{
                                    width: '100%',
                                    padding: '12px 16px',
                                    border: '2px solid #e2e8f0',
                                    borderRadius: '8px',
                                    fontSize: '14px',
                                    resize: 'vertical',
                                    boxSizing: 'border-box',
                                    opacity: loading ? 0.6 : 1
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
                                Notes
                            </label>
                            <textarea
                                value={workoutData.notes}
                                onChange={(e) => setWorkoutData(prev => ({ ...prev, notes: e.target.value }))}
                                placeholder="Additional notes about this workout plan..."
                                rows={2}
                                disabled={loading}
                                style={{
                                    width: '100%',
                                    padding: '12px 16px',
                                    border: '2px solid #e2e8f0',
                                    borderRadius: '8px',
                                    fontSize: '14px',
                                    resize: 'vertical',
                                    boxSizing: 'border-box',
                                    opacity: loading ? 0.6 : 1
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
                    border: '1px solid #c6f6d5',
                    opacity: loading ? 0.6 : 1
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
                                disabled={loading}
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
                                        disabled={loading}
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
                                        disabled={loading}
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
                                        disabled={loading}
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
                                        disabled={loading}
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
                                        disabled={loading}
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
                                        disabled={loading}
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
                            <div>
                                <label style={{
                                    display: 'block',
                                    marginBottom: '8px',
                                    color: '#4a5568',
                                    fontWeight: '600',
                                    fontSize: '14px'
                                }}>
                                    Notes
                                </label>
                                <textarea
                                    value={exerciseDetails.notes}
                                    onChange={(e) => setExerciseDetails(prev => ({ ...prev, notes: e.target.value }))}
                                    placeholder="Any specific notes for this exercise..."
                                    rows={2}
                                    disabled={loading}
                                    style={{
                                        width: '100%',
                                        padding: '12px 16px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px',
                                        resize: 'vertical',
                                        boxSizing: 'border-box',
                                        marginBottom: '16px'
                                    }}
                                />
                                <button
                                    onClick={handleAddExercise}
                                    disabled={loading}
                                    style={{
                                        background: loading ? '#cbd5e0' : 'linear-gradient(135deg, #48bb78, #38a169)',
                                        color: 'white',
                                        border: 'none',
                                        padding: '12px 24px',
                                        borderRadius: '8px',
                                        cursor: loading ? 'not-allowed' : 'pointer',
                                        fontSize: '14px',
                                        fontWeight: '600'
                                    }}
                                >
                                    Add Exercise
                                </button>
                            </div>
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
                        border: '1px solid #fed7aa',
                        opacity: loading ? 0.6 : 1
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
                                        disabled={loading}
                                        style={{
                                            background: loading ? '#e2e8f0' : '#fed7d7',
                                            color: loading ? '#a0aec0' : '#c53030',
                                            border: 'none',
                                            padding: '8px 12px',
                                            borderRadius: '6px',
                                            cursor: loading ? 'not-allowed' : 'pointer',
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
                        onClick={handleClosePopup}
                        disabled={loading}
                        style={{
                            background: '#f7fafc',
                            color: '#4a5568',
                            border: '2px solid #e2e8f0',
                            padding: '12px 24px',
                            borderRadius: '10px',
                            cursor: loading ? 'not-allowed' : 'pointer',
                            fontSize: '14px',
                            fontWeight: '600',
                            opacity: loading ? 0.6 : 1
                        }}
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSaveWorkout}
                        disabled={loading || !workoutData.plan_name || workoutData.exercises.length === 0}
                        style={{
                            background: (loading || !workoutData.plan_name || workoutData.exercises.length === 0)
                                ? '#cbd5e0'
                                : 'linear-gradient(135deg, #667eea, #764ba2)',
                            color: 'white',
                            border: 'none',
                            padding: '12px 24px',
                            borderRadius: '10px',
                            cursor: (loading || !workoutData.plan_name || workoutData.exercises.length === 0)
                                ? 'not-allowed'
                                : 'pointer',
                            fontSize: '14px',
                            fontWeight: '600'
                        }}
                    >
                        {loading ? 'Se creează...' : 'Create Workout Plan'}
                    </button>
                </div>

                {/* CSS for animations */}
                <style jsx>{`
                    @keyframes spin {
                        0% { transform: rotate(0deg); }
                        100% { transform: rotate(360deg); }
                    }
                `}</style>
            </div>
        </div>
    );
};

export default WorkoutPlanCreator;