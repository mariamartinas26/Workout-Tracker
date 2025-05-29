// components/WorkoutScheduler.js
import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8082/api';

// Service pentru WORKOUT PLANS (nu scheduled workouts!)
const WorkoutPlanService = {
    /**
     * Obține DOAR planurile de workout ale utilizatorului (templates)
     * NU scheduled workouts!
     */
    getUserWorkoutPlans: async (userId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/workout-plans/user/${userId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    // Adaugă authorization header dacă e necesar
                    // 'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch workout plans: ${response.status}`);
            }

            const workoutPlans = await response.json();
            console.log('Fetched workout plans:', workoutPlans);
            return workoutPlans;

        } catch (error) {
            console.error('Error fetching workout plans:', error);
            throw error;
        }
    },

    /**
     * Obține un plan specific cu detaliile exercițiilor
     */
    getWorkoutPlanById: async (workoutPlanId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/workout-plans/${workoutPlanId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch workout plan: ${response.status}`);
            }

            return await response.json();

        } catch (error) {
            console.error('Error fetching workout plan details:', error);
            throw error;
        }
    },

    /**
     * Creează un plan de workout nou
     */
    createWorkoutPlan: async (planData) => {
        try {
            const response = await fetch(`${API_BASE_URL}/workout-plans`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(planData)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to create workout plan');
            }

            return await response.json();

        } catch (error) {
            console.error('Error creating workout plan:', error);
            throw error;
        }
    },

    /**
     * Șterge un plan de workout
     */
    deleteWorkoutPlan: async (workoutPlanId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/workout-plans/${workoutPlanId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to delete workout plan: ${response.status}`);
            }

            return true;

        } catch (error) {
            console.error('Error deleting workout plan:', error);
            throw error;
        }
    },

    /**
     * Actualizează un plan de workout
     */
    updateWorkoutPlan: async (workoutPlanId, updatedData) => {
        try {
            const response = await fetch(`${API_BASE_URL}/workout-plans/${workoutPlanId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(updatedData)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to update workout plan');
            }

            return await response.json();

        } catch (error) {
            console.error('Error updating workout plan:', error);
            throw error;
        }
    },

    /**
     * Numără planurile utilizatorului
     */
    countUserWorkoutPlans: async (userId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/workout-plans/user/${userId}/count`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to count workout plans: ${response.status}`);
            }

            const result = await response.json();
            return result.count || 0;

        } catch (error) {
            console.error('Error counting workout plans:', error);
            return 0;
        }
    }
};

// Service pentru SCHEDULED WORKOUTS (dacă încă ai nevoie de ele)
const ScheduledWorkoutService = {
    /**
     * Obține workout-urile PROGRAMATE ale utilizatorului
     */
    getUserScheduledWorkouts: async (userId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/scheduled-workouts/user/${userId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch scheduled workouts: ${response.status}`);
            }

            return await response.json();

        } catch (error) {
            console.error('Error fetching scheduled workouts:', error);
            throw error;
        }
    },

    rescheduleWorkout: async (scheduledWorkoutId, newDate, newTime) => {
        try {
            const response = await fetch(`${API_BASE_URL}/scheduled-workouts/${scheduledWorkoutId}/reschedule`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    scheduledDate: newDate,
                    scheduledTime: newTime
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to reschedule workout');
            }

            return await response.json();

        } catch (error) {
            console.error('Error rescheduling workout:', error);
            throw error;
        }
    }
};

const WorkoutScheduler = ({ isOpen, onClose, currentUserId = 1 }) => {
    // State pentru workout plans (templates)
    const [workoutPlans, setWorkoutPlans] = useState([]);
    const [loadingPlans, setLoadingPlans] = useState(false);

    // State pentru scheduled workouts
    const [scheduledWorkouts, setScheduledWorkouts] = useState([]);
    const [loadingWorkouts, setLoadingWorkouts] = useState(false);

    const [selectedWorkout, setSelectedWorkout] = useState(null);
    const [selectedPlan, setSelectedPlan] = useState(null);
    const [scheduleData, setScheduleData] = useState({
        scheduledDate: '',
        scheduledTime: '10:00'
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [activeTab, setActiveTab] = useState('plans'); // 'plans' sau 'scheduled'

    // Încarcă datele când se deschide popup-ul
    useEffect(() => {
        if (isOpen) {
            loadWorkoutPlans();
            loadScheduledWorkouts();
            // Setează data de azi ca default
            const today = new Date().toISOString().split('T')[0];
            setScheduleData(prev => ({ ...prev, scheduledDate: today }));
        }
    }, [isOpen, currentUserId]);

    // Încarcă planurile de workout (templates)
    const loadWorkoutPlans = async () => {
        setLoadingPlans(true);
        setError('');
        try {
            const plans = await WorkoutPlanService.getUserWorkoutPlans(currentUserId);
            setWorkoutPlans(plans);
            console.log('Loaded workout plans:', plans);
        } catch (error) {
            console.error('Error loading workout plans:', error);
            setError('Nu s-au putut încărca planurile de workout');
        } finally {
            setLoadingPlans(false);
        }
    };

    // Încarcă workout-urile programate
    const loadScheduledWorkouts = async () => {
        setLoadingWorkouts(true);
        setError('');
        try {
            const workouts = await ScheduledWorkoutService.getUserScheduledWorkouts(currentUserId);
            setScheduledWorkouts(workouts);
            console.log('Loaded scheduled workouts:', workouts);
        } catch (error) {
            console.error('Error loading scheduled workouts:', error);
            setError('Nu s-au putut încărca workout-urile programate');
        } finally {
            setLoadingWorkouts(false);
        }
    };

    const handleRescheduleWorkout = async () => {
        if (!selectedWorkout) {
            setError('Selectează un workout pentru a-l reprograma');
            return;
        }

        if (!scheduleData.scheduledDate) {
            setError('Selectează o dată');
            return;
        }

        if (!scheduleData.scheduledTime) {
            setError('Selectează o oră');
            return;
        }

        setLoading(true);
        setError('');

        try {
            const response = await ScheduledWorkoutService.rescheduleWorkout(
                selectedWorkout.scheduledWorkoutId,
                scheduleData.scheduledDate,
                scheduleData.scheduledTime
            );

            console.log('Workout reprogramat cu succes:', response);

            // Success message
            alert(`Workout-ul "${selectedWorkout.workoutPlan?.planName || 'Workout'}" a fost reprogramat cu succes!\nData nouă: ${scheduleData.scheduledDate}\nOra nouă: ${scheduleData.scheduledTime}`);

            // Reîncarcă workout-urile și închide popup-ul
            await loadScheduledWorkouts();
            handleClosePopup();

        } catch (error) {
            console.error('Eroare la reprogramarea workout-ului:', error);
            setError(error.message || 'A apărut o eroare la reprogramarea workout-ului');
        } finally {
            setLoading(false);
        }
    };

    const handleClosePopup = () => {
        setSelectedWorkout(null);
        setSelectedPlan(null);
        setScheduleData({
            scheduledDate: '',
            scheduledTime: '10:00'
        });
        setError('');
        setLoading(false);
        onClose();
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ro-RO', {
            weekday: 'short',
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    };

    const formatTime = (timeString) => {
        if (!timeString) return '';
        return timeString.substring(0, 5); // Extract HH:MM from HH:MM:SS
    };

    const getStatusColor = (status) => {
        switch (status?.toLowerCase()) {
            case 'scheduled':
            case 'programat':
                return { bg: '#e6f3ff', border: '#3182ce', text: '#2c5282' };
            case 'in_progress':
            case 'în progres':
                return { bg: '#fff2e6', border: '#ed8936', text: '#c05621' };
            case 'completed':
            case 'finalizat':
                return { bg: '#e6ffe6', border: '#38a169', text: '#2f855a' };
            case 'cancelled':
            case 'anulat':
                return { bg: '#ffe6e6', border: '#e53e3e', text: '#c53030' };
            default:
                return { bg: '#f7fafc', border: '#cbd5e0', text: '#4a5568' };
        }
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
                maxWidth: '900px',
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
                    Workout Manager
                </h2>

                {/* Tab Navigation */}
                <div style={{
                    display: 'flex',
                    marginBottom: '24px',
                    borderBottom: '2px solid #e2e8f0'
                }}>
                    <button
                        onClick={() => setActiveTab('plans')}
                        style={{
                            padding: '12px 24px',
                            background: 'none',
                            border: 'none',
                            borderBottom: activeTab === 'plans' ? '3px solid #667eea' : '3px solid transparent',
                            color: activeTab === 'plans' ? '#667eea' : '#718096',
                            fontWeight: '600',
                            cursor: 'pointer',
                            fontSize: '16px'
                        }}
                    >
                        📋 Workout Plans ({workoutPlans.length})
                    </button>
                    <button
                        onClick={() => setActiveTab('scheduled')}
                        style={{
                            padding: '12px 24px',
                            background: 'none',
                            border: 'none',
                            borderBottom: activeTab === 'scheduled' ? '3px solid #667eea' : '3px solid transparent',
                            color: activeTab === 'scheduled' ? '#667eea' : '#718096',
                            fontWeight: '600',
                            cursor: 'pointer',
                            fontSize: '16px'
                        }}
                    >
                        📅 Scheduled Workouts ({scheduledWorkouts.length})
                    </button>
                </div>

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

                {/* Workout Plans Tab */}
                {activeTab === 'plans' && (
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
                            Your Workout Plans (Templates)
                        </h3>

                        {loadingPlans ? (
                            <div style={{
                                textAlign: 'center',
                                padding: '40px',
                                color: '#718096'
                            }}>
                                <div style={{ fontSize: '20px', marginBottom: '8px' }}>⏳</div>
                                <div>Loading your workout plans...</div>
                            </div>
                        ) : workoutPlans.length === 0 ? (
                            <div style={{
                                textAlign: 'center',
                                padding: '40px',
                                color: '#718096',
                                backgroundColor: '#f9fafb',
                                borderRadius: '8px',
                                border: '2px dashed #e2e8f0'
                            }}>
                                <div style={{ fontSize: '24px', marginBottom: '8px' }}>📝</div>
                                <div style={{ fontWeight: '600', marginBottom: '4px' }}>No Workout Plans Found</div>
                                <div style={{ fontSize: '14px' }}>Create your first workout plan template</div>
                            </div>
                        ) : (
                            <div style={{
                                display: 'grid',
                                gap: '12px'
                            }}>
                                {workoutPlans.map((plan) => (
                                    <div
                                        key={plan.workoutPlanId}
                                        onClick={() => setSelectedPlan(plan)}
                                        style={{
                                            backgroundColor: selectedPlan?.workoutPlanId === plan.workoutPlanId ? '#e6fffa' : 'white',
                                            border: selectedPlan?.workoutPlanId === plan.workoutPlanId ? '2px solid #38b2ac' : '2px solid #e2e8f0',
                                            borderRadius: '12px',
                                            padding: '20px',
                                            cursor: loading ? 'not-allowed' : 'pointer',
                                            transition: 'all 0.2s',
                                            opacity: loading ? 0.6 : 1
                                        }}
                                    >
                                        <div style={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'flex-start',
                                            marginBottom: '12px'
                                        }}>
                                            <div style={{ flex: 1 }}>
                                                <h4 style={{
                                                    color: '#2d3748',
                                                    fontSize: '16px',
                                                    fontWeight: '700',
                                                    marginBottom: '4px'
                                                }}>
                                                    {plan.planName}
                                                </h4>

                                                <div style={{
                                                    color: '#4a5568',
                                                    fontSize: '14px',
                                                    marginBottom: '8px'
                                                }}>
                                                    📅 Created: {formatDate(plan.createdAt)}
                                                    {plan.updatedAt && plan.updatedAt !== plan.createdAt &&
                                                        ` • Updated: ${formatDate(plan.updatedAt)}`
                                                    }
                                                </div>

                                                {plan.description && (
                                                    <p style={{
                                                        color: '#718096',
                                                        fontSize: '13px',
                                                        lineHeight: '1.4',
                                                        marginBottom: '8px'
                                                    }}>
                                                        {plan.description.length > 60
                                                            ? plan.description.substring(0, 60) + '...'
                                                            : plan.description}
                                                    </p>
                                                )}
                                            </div>
                                            {selectedPlan?.workoutPlanId === plan.workoutPlanId && (
                                                <div style={{
                                                    backgroundColor: '#38b2ac',
                                                    color: 'white',
                                                    borderRadius: '50%',
                                                    width: '24px',
                                                    height: '24px',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    fontSize: '14px',
                                                    fontWeight: 'bold'
                                                }}>
                                                    ✓
                                                </div>
                                            )}
                                        </div>

                                        <div style={{
                                            display: 'flex',
                                            gap: '16px',
                                            flexWrap: 'wrap',
                                            color: '#718096',
                                            fontSize: '13px'
                                        }}>
                                            {plan.estimatedDurationMinutes && (
                                                <span>⏱️ {plan.estimatedDurationMinutes} min</span>
                                            )}
                                            {plan.difficultyLevel && (
                                                <span>💪 Level {plan.difficultyLevel}/5</span>
                                            )}
                                            {plan.goals && (
                                                <span>🎯 {plan.goals}</span>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {/* Scheduled Workouts Tab - codul existent rămâne la fel */}
                {activeTab === 'scheduled' && (
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
                            Your Scheduled Workouts
                        </h3>

                        {loadingWorkouts ? (
                            <div style={{
                                textAlign: 'center',
                                padding: '40px',
                                color: '#718096'
                            }}>
                                <div style={{ fontSize: '20px', marginBottom: '8px' }}>⏳</div>
                                <div>Loading your scheduled workouts...</div>
                            </div>
                        ) : scheduledWorkouts.length === 0 ? (
                            <div style={{
                                textAlign: 'center',
                                padding: '40px',
                                color: '#718096',
                                backgroundColor: '#f9fafb',
                                borderRadius: '8px',
                                border: '2px dashed #e2e8f0'
                            }}>
                                <div style={{ fontSize: '24px', marginBottom: '8px' }}>📅</div>
                                <div style={{ fontWeight: '600', marginBottom: '4px' }}>No Scheduled Workouts Found</div>
                                <div style={{ fontSize: '14px' }}>You don't have any scheduled workouts yet</div>
                            </div>
                        ) : (
                            <div style={{
                                display: 'grid',
                                gap: '12px'
                            }}>
                                {scheduledWorkouts.map((workout) => {
                                    const statusStyle = getStatusColor(workout.status);
                                    return (
                                        <div
                                            key={workout.scheduledWorkoutId}
                                            onClick={() => setSelectedWorkout(workout)}
                                            style={{
                                                backgroundColor: selectedWorkout?.scheduledWorkoutId === workout.scheduledWorkoutId ? '#e6fffa' : 'white',
                                                border: selectedWorkout?.scheduledWorkoutId === workout.scheduledWorkoutId ? '2px solid #38b2ac' : '2px solid #e2e8f0',
                                                borderRadius: '12px',
                                                padding: '20px',
                                                cursor: loading ? 'not-allowed' : 'pointer',
                                                transition: 'all 0.2s',
                                                opacity: loading ? 0.6 : 1
                                            }}
                                        >
                                            {/* Conținutul pentru scheduled workouts rămâne la fel */}
                                            <div style={{
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                alignItems: 'flex-start',
                                                marginBottom: '12px'
                                            }}>
                                                <div style={{ flex: 1 }}>
                                                    <h4 style={{
                                                        color: '#2d3748',
                                                        fontSize: '16px',
                                                        fontWeight: '700',
                                                        marginBottom: '4px'
                                                    }}>
                                                        {workout.workoutPlan?.planName || `Workout #${workout.scheduledWorkoutId}`}
                                                    </h4>

                                                    <div style={{
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        gap: '8px',
                                                        marginBottom: '8px'
                                                    }}>
                                                        <span style={{
                                                            backgroundColor: statusStyle.bg,
                                                            color: statusStyle.text,
                                                            border: `1px solid ${statusStyle.border}`,
                                                            padding: '2px 8px',
                                                            borderRadius: '6px',
                                                            fontSize: '12px',
                                                            fontWeight: '600'
                                                        }}>
                                                            {workout.status || 'Programat'}
                                                        </span>
                                                    </div>

                                                    <div style={{
                                                        color: '#4a5568',
                                                        fontSize: '14px',
                                                        marginBottom: '8px'
                                                    }}>
                                                        📅 {formatDate(workout.scheduledDate)}
                                                        {workout.scheduledTime && ` • 🕒 ${formatTime(workout.scheduledTime)}`}
                                                    </div>

                                                    {workout.workoutPlan?.description && (
                                                        <p style={{
                                                            color: '#718096',
                                                            fontSize: '13px',
                                                            lineHeight: '1.4',
                                                            marginBottom: '8px'
                                                        }}>
                                                            {workout.workoutPlan.description.length > 60
                                                                ? workout.workoutPlan.description.substring(0, 60) + '...'
                                                                : workout.workoutPlan.description}
                                                        </p>
                                                    )}
                                                </div>
                                                {selectedWorkout?.scheduledWorkoutId === workout.scheduledWorkoutId && (
                                                    <div style={{
                                                        backgroundColor: '#38b2ac',
                                                        color: 'white',
                                                        borderRadius: '50%',
                                                        width: '24px',
                                                        height: '24px',
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'center',
                                                        fontSize: '14px',
                                                        fontWeight: 'bold'
                                                    }}>
                                                        ✓
                                                    </div>
                                                )}
                                            </div>

                                            <div style={{
                                                display: 'flex',
                                                gap: '16px',
                                                flexWrap: 'wrap',
                                                color: '#718096',
                                                fontSize: '13px'
                                            }}>
                                                {workout.workoutPlan?.estimatedDurationMinutes && (
                                                    <span>⏱️ {workout.workoutPlan.estimatedDurationMinutes} min</span>
                                                )}
                                                {workout.workoutPlan?.difficultyLevel && (
                                                    <span>💪 Level {workout.workoutPlan.difficultyLevel}</span>
                                                )}
                                                {workout.caloriesBurned && (
                                                    <span>🔥 {workout.caloriesBurned} cal</span>
                                                )}
                                                {workout.rating && (
                                                    <span>⭐ {workout.rating}/5</span>
                                                )}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                )}

                {/* Reschedule section - doar pentru scheduled workouts */}
                {activeTab === 'scheduled' && selectedWorkout && (
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
                            Reschedule Details
                        </h3>

                        <div style={{
                            backgroundColor: '#e6fffa',
                            padding: '16px',
                            borderRadius: '8px',
                            marginBottom: '20px',
                            border: '1px solid #b2f5ea'
                        }}>
                            <div style={{
                                color: '#2d3748',
                                fontWeight: '600',
                                marginBottom: '8px'
                            }}>
                                Selected Workout: {selectedWorkout.workoutPlan?.planName || `Workout #${selectedWorkout.scheduledWorkoutId}`}
                            </div>
                            <div style={{
                                color: '#718096',
                                fontSize: '14px',
                                marginBottom: '4px'
                            }}>
                                Current Schedule: {formatDate(selectedWorkout.scheduledDate)}
                                {selectedWorkout.scheduledTime && ` at ${formatTime(selectedWorkout.scheduledTime)}`}
                            </div>
                            <div style={{
                                color: '#718096',
                                fontSize: '14px'
                            }}>
                                {selectedWorkout.workoutPlan?.estimatedDurationMinutes && `Duration: ${selectedWorkout.workoutPlan.estimatedDurationMinutes} minutes`}
                                {selectedWorkout.workoutPlan?.difficultyLevel && ` • Difficulty: Level ${selectedWorkout.workoutPlan.difficultyLevel}`}
                            </div>
                        </div>

                        <div style={{
                            display: 'grid',
                            gridTemplateColumns: '1fr 1fr',
                            gap: '16px',
                            marginBottom: '16px'
                        }}>
                            <div>
                                <label style={{
                                    display: 'block',
                                    marginBottom: '8px',
                                    color: '#4a5568',
                                    fontWeight: '600',
                                    fontSize: '14px'
                                }}>
                                    New Date *
                                </label>
                                <input
                                    type="date"
                                    value={scheduleData.scheduledDate}
                                    onChange={(e) => setScheduleData(prev => ({ ...prev, scheduledDate: e.target.value }))}
                                    min={new Date().toISOString().split('T')[0]}
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
                                    New Time *
                                </label>
                                <input
                                    type="time"
                                    value={scheduleData.scheduledTime}
                                    onChange={(e) => setScheduleData(prev => ({ ...prev, scheduledTime: e.target.value }))}
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

                        {/* Quick Date Options */}
                        <div style={{
                            marginTop: '16px',
                            display: 'flex',
                            gap: '8px',
                            flexWrap: 'wrap'
                        }}>
                            <button
                                onClick={() => setScheduleData(prev => ({
                                    ...prev,
                                    scheduledDate: new Date().toISOString().split('T')[0]
                                }))}
                                disabled={loading}
                                style={{
                                    background: '#e2e8f0',
                                    color: '#4a5568',
                                    border: 'none',
                                    padding: '8px 12px',
                                    borderRadius: '6px',
                                    cursor: loading ? 'not-allowed' : 'pointer',
                                    fontSize: '12px',
                                    fontWeight: '600'
                                }}
                            >
                                Today
                            </button>
                            <button
                                onClick={() => {
                                    const tomorrow = new Date();
                                    tomorrow.setDate(tomorrow.getDate() + 1);
                                    setScheduleData(prev => ({
                                        ...prev,
                                        scheduledDate: tomorrow.toISOString().split('T')[0]
                                    }));
                                }}
                                disabled={loading}
                                style={{
                                    background: '#e2e8f0',
                                    color: '#4a5568',
                                    border: 'none',
                                    padding: '8px 12px',
                                    borderRadius: '6px',
                                    cursor: loading ? 'not-allowed' : 'pointer',
                                    fontSize: '12px',
                                    fontWeight: '600'
                                }}
                            >
                                Tomorrow
                            </button>
                        </div>
                    </div>
                )}

                {/* Plan Details section - pentru workout plans */}
                {activeTab === 'plans' && selectedPlan && (
                    <div style={{
                        backgroundColor: '#f0f9ff',
                        padding: '24px',
                        borderRadius: '16px',
                        marginBottom: '24px',
                        border: '1px solid #bfdbfe'
                    }}>
                        <h3 style={{
                            color: '#2d3748',
                            fontSize: '18px',
                            fontWeight: '700',
                            marginBottom: '16px'
                        }}>
                            Plan Details
                        </h3>

                        <div style={{
                            backgroundColor: '#e0f2fe',
                            padding: '16px',
                            borderRadius: '8px',
                            marginBottom: '20px',
                            border: '1px solid #7dd3fc'
                        }}>
                            <div style={{
                                color: '#2d3748',
                                fontWeight: '700',
                                fontSize: '18px',
                                marginBottom: '8px'
                            }}>
                                {selectedPlan.planName}
                            </div>

                            {selectedPlan.description && (
                                <div style={{
                                    color: '#4a5568',
                                    fontSize: '14px',
                                    marginBottom: '12px',
                                    lineHeight: '1.5'
                                }}>
                                    {selectedPlan.description}
                                </div>
                            )}

                            <div style={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                                gap: '12px',
                                color: '#718096',
                                fontSize: '14px'
                            }}>
                                {selectedPlan.estimatedDurationMinutes && (
                                    <div>
                                        <strong>Duration:</strong> {selectedPlan.estimatedDurationMinutes} minutes
                                    </div>
                                )}
                                {selectedPlan.difficultyLevel && (
                                    <div>
                                        <strong>Difficulty:</strong> Level {selectedPlan.difficultyLevel}/5
                                    </div>
                                )}
                                {selectedPlan.goals && (
                                    <div>
                                        <strong>Goals:</strong> {selectedPlan.goals}
                                    </div>
                                )}
                                <div>
                                    <strong>Created:</strong> {formatDate(selectedPlan.createdAt)}
                                </div>
                                {selectedPlan.updatedAt && selectedPlan.updatedAt !== selectedPlan.createdAt && (
                                    <div>
                                        <strong>Updated:</strong> {formatDate(selectedPlan.updatedAt)}
                                    </div>
                                )}
                            </div>

                            {selectedPlan.notes && (
                                <div style={{
                                    marginTop: '12px',
                                    padding: '12px',
                                    backgroundColor: '#f8fafc',
                                    borderRadius: '6px',
                                    border: '1px solid #e2e8f0'
                                }}>
                                    <strong style={{ color: '#4a5568' }}>Notes:</strong>
                                    <div style={{ color: '#718096', marginTop: '4px' }}>
                                        {selectedPlan.notes}
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Action buttons pentru workout plans */}
                        <div style={{
                            display: 'flex',
                            gap: '12px',
                            flexWrap: 'wrap'
                        }}>
                            <button
                                onClick={async () => {
                                    try {
                                        const planDetails = await WorkoutPlanService.getWorkoutPlanById(selectedPlan.workoutPlanId);
                                        console.log('Plan details with exercises:', planDetails);
                                        // Aici poți afișa exercițiile într-un modal sau expandat
                                        alert(`Plan "${planDetails.planName}" loaded! Check console for details.`);
                                    } catch (error) {
                                        console.error('Error loading plan details:', error);
                                        alert('Error loading plan details: ' + error.message);
                                    }
                                }}
                                style={{
                                    background: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
                                    color: 'white',
                                    border: 'none',
                                    padding: '10px 16px',
                                    borderRadius: '8px',
                                    cursor: 'pointer',
                                    fontSize: '14px',
                                    fontWeight: '600'
                                }}
                            >
                                👁️ View Exercises
                            </button>

                            <button
                                onClick={() => {
                                    // Redirect to edit page or open edit modal
                                    console.log('Edit plan:', selectedPlan.workoutPlanId);
                                    alert('Edit functionality - implement redirect to edit page');
                                }}
                                style={{
                                    background: 'linear-gradient(135deg, #f59e0b, #d97706)',
                                    color: 'white',
                                    border: 'none',
                                    padding: '10px 16px',
                                    borderRadius: '8px',
                                    cursor: 'pointer',
                                    fontSize: '14px',
                                    fontWeight: '600'
                                }}
                            >
                                ✏️ Edit Plan
                            </button>

                            <button
                                onClick={async () => {
                                    if (confirm(`Are you sure you want to delete "${selectedPlan.planName}"?`)) {
                                        try {
                                            await WorkoutPlanService.deleteWorkoutPlan(selectedPlan.workoutPlanId);
                                            alert('Plan deleted successfully!');
                                            await loadWorkoutPlans(); // Reload the list
                                            setSelectedPlan(null); // Clear selection
                                        } catch (error) {
                                            console.error('Error deleting plan:', error);
                                            alert('Error deleting plan: ' + error.message);
                                        }
                                    }
                                }}
                                style={{
                                    background: 'linear-gradient(135deg, #ef4444, #dc2626)',
                                    color: 'white',
                                    border: 'none',
                                    padding: '10px 16px',
                                    borderRadius: '8px',
                                    cursor: 'pointer',
                                    fontSize: '14px',
                                    fontWeight: '600'
                                }}
                            >
                                🗑️ Delete Plan
                            </button>

                            <button
                                onClick={() => {
                                    // Implement schedule workout functionality
                                    console.log('Schedule workout from plan:', selectedPlan.workoutPlanId);
                                    alert('Schedule workout functionality - implement schedule creation');
                                }}
                                style={{
                                    background: 'linear-gradient(135deg, #10b981, #059669)',
                                    color: 'white',
                                    border: 'none',
                                    padding: '10px 16px',
                                    borderRadius: '8px',
                                    cursor: 'pointer',
                                    fontSize: '14px',
                                    fontWeight: '600'
                                }}
                            >
                                📅 Schedule Workout
                            </button>
                        </div>
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
                            <div style={{ fontWeight: '600' }}>
                                {activeTab === 'scheduled' ? 'Se reprogramează workout-ul...' : 'Processing...'}
                            </div>
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
                        Close
                    </button>

                    {/* Show reschedule button only for scheduled workouts */}
                    {activeTab === 'scheduled' && (
                        <button
                            onClick={handleRescheduleWorkout}
                            disabled={loading || !selectedWorkout || !scheduleData.scheduledDate || !scheduleData.scheduledTime}
                            style={{
                                background: (loading || !selectedWorkout || !scheduleData.scheduledDate || !scheduleData.scheduledTime)
                                    ? '#cbd5e0'
                                    : 'linear-gradient(135deg, #48bb78, #38a169)',
                                color: 'white',
                                border: 'none',
                                padding: '12px 24px',
                                borderRadius: '10px',
                                cursor: (loading || !selectedWorkout || !scheduleData.scheduledDate || !scheduleData.scheduledTime)
                                    ? 'not-allowed'
                                    : 'pointer',
                                fontSize: '14px',
                                fontWeight: '600'
                            }}
                        >
                            {loading ? 'Se reprogramează...' : 'Reschedule Workout'}
                        </button>
                    )}
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

export default WorkoutScheduler;