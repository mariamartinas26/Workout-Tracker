// components/PlanWorkout.js
import React, { useState, useEffect } from 'react';
import WorkoutSchedule from './WorkoutSchedule';

const API_BASE_URL = 'http://localhost:8082/api';


const WorkoutPlanService = {

    getUserWorkoutPlans: async (userId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/workout-plans/user/${userId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
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


const ScheduledWorkoutService = {

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
    },

    startWorkout: async (workoutId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/scheduled-workouts/${workoutId}/start`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to start workout');
            }

            return await response.json();

        } catch (error) {
            console.error('Error starting workout:', error);
            throw error;
        }
    },

    completeWorkout: async (workoutId, caloriesBurned, rating) => {
        try {
            const response = await fetch(`${API_BASE_URL}/scheduled-workouts/${workoutId}/complete`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    caloriesBurned: caloriesBurned,
                    rating: rating
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to complete workout');
            }

            return await response.json();

        } catch (error) {
            console.error('Error completing workout:', error);
            throw error;
        }
    },

    cancelWorkout: async (workoutId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/scheduled-workouts/${workoutId}/cancel`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to cancel workout');
            }

            return await response.json();

        } catch (error) {
            console.error('Error cancelling workout:', error);
            throw error;
        }
    }
};

const PlanWorkout = ({ isOpen, onClose, currentUserId = 1 }) => {
    const [workoutPlans, setWorkoutPlans] = useState([]);
    const [loadingPlans, setLoadingPlans] = useState(false);

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

    // NEW STATE FOR SCHEDULE MODAL
    const [showScheduleModal, setShowScheduleModal] = useState(false);
    const [selectedPlanForScheduling, setSelectedPlanForScheduling] = useState(null);

    const [showCompleteModal, setShowCompleteModal] = useState(false);
    const [completeWorkoutData, setCompleteWorkoutData] = useState({
        caloriesBurned: '',
        rating: 5
    });

    useEffect(() => {
        if (isOpen) {
            loadWorkoutPlans();
            loadScheduledWorkouts();
            // Setează data de azi ca default
            const today = new Date().toISOString().split('T')[0];
            setScheduleData(prev => ({...prev, scheduledDate: today}));
        }
    }, [isOpen, currentUserId]);

    const isWorkoutToday = (workout) => {
        const today = new Date().toISOString().split('T')[0];
        return workout.scheduledDate === today;
    };

    const getWorkoutActionButton = (workout) => {
        if (!isWorkoutToday(workout)) return null;

        const status = workout.status?.toLowerCase();

        switch (status) {
            case 'planned':
                return {
                    text: '▶️ Start Workout',
                    action: 'start',
                    color: 'linear-gradient(135deg, #10b981, #059669)',
                    enabled: true
                };
            case 'in_progress':
                return {
                    text: '✅ Complete Workout',
                    action: 'complete',
                    color: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
                    enabled: true
                };
            case 'completed':
                return {
                    text: '✅ Completed',
                    action: 'none',
                    color: '#9ca3af',
                    enabled: false
                };
            case 'cancelled':
                return {
                    text: '❌ Cancelled',
                    action: 'none',
                    color: '#9ca3af',
                    enabled: false
                };
            case 'missed':
                return {
                    text: '⏰ Missed',
                    action: 'none',
                    color: '#ef4444',
                    enabled: false
                };
            case 'scheduled':
                return {
                    text: '▶️ Start Workout',
                    action: 'start',
                    color: 'linear-gradient(135deg, #10b981, #059669)',
                    enabled: true
                };
            default:
                return {
                    text: '▶️ Start Workout',
                    action: 'start',
                    color: 'linear-gradient(135deg, #10b981, #059669)',
                    enabled: true
                };
        }
    };


    const loadWorkoutPlans = async () => {
        setLoadingPlans(true);
        setError('');
        try {
            const plans = await WorkoutPlanService.getUserWorkoutPlans(currentUserId);
            setWorkoutPlans(plans);
            console.log('Loaded workout plans:', plans);
        } catch (error) {
            console.error('Error loading workout plans:', error);
            setError('Could not load workout plans');
        } finally {
            setLoadingPlans(false);
        }
    };


    const loadScheduledWorkouts = async () => {
        setLoadingWorkouts(true);
        setError('');
        try {
            const workouts = await ScheduledWorkoutService.getUserScheduledWorkouts(currentUserId);
            setScheduledWorkouts(workouts);
            console.log('Loaded scheduled workouts:', workouts);
        } catch (error) {
            console.error('Error loading scheduled workouts:', error);
            setError('Could not load scheduled workouts');
        } finally {
            setLoadingWorkouts(false);
        }
    };

    const handleScheduleWorkout = (plan) => {
        setSelectedPlanForScheduling(plan);
        setShowScheduleModal(true);
    };

    const handleWorkoutScheduled = (result) => {
        console.log('Workout scheduled successfully:', result);
        loadScheduledWorkouts();
        setError('');
    };

    const handleStartWorkout = async (workout) => {
        setLoading(true);
        setError('');

        try {
            await ScheduledWorkoutService.startWorkout(workout.scheduledWorkoutId);

            await loadScheduledWorkouts();

        } catch (error) {
            console.error('Error starting workout:', error);
            setError(error.message || 'Failed to start workout');
        } finally {
            setLoading(false);
        }
    };

    const handleCompleteWorkoutClick = (workout) => {
        setSelectedWorkout(workout);
        setCompleteWorkoutData({
            caloriesBurned: '',
            rating: 5
        });
        setShowCompleteModal(true);
    };

    const handleCompleteWorkoutSubmit = async () => {
        if (!selectedWorkout) return;

        setLoading(true);
        setError('');

        try {
            await ScheduledWorkoutService.completeWorkout(
                selectedWorkout.scheduledWorkoutId,
                completeWorkoutData.caloriesBurned ? parseInt(completeWorkoutData.caloriesBurned) : null,
                completeWorkoutData.rating
            );

            setShowCompleteModal(false);
            setSelectedWorkout(null);
            await loadScheduledWorkouts();

        } catch (error) {
            console.error('Error completing workout:', error);
            setError(error.message || 'Failed to complete workout');
        } finally {
            setLoading(false);
        }
    };

    const handleWorkoutAction = async (workout, action) => {
        switch (action) {
            case 'start':
                await handleStartWorkout(workout);
                break;
            case 'complete':
                handleCompleteWorkoutClick(workout);
                break;
            default:
                break;
        }
    };
    const cleanWorkoutTitle = (planName) => {
        if (!planName) return 'Workout';

        let cleanTitle = planName
            .replace(/\(Goal \d+\)/gi, '')
            .replace(/\s*-\s*\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}/g, '')
            .replace(/\s*-\s*\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/g, '')
            .replace(/\s*-\s*\d{2}:\d{2}/g, '')
            .trim()
            .replace(/\s+/g, ' ');

        return cleanTitle || 'Workout';
    };

    const handleRescheduleWorkout = async () => {
        if (!selectedWorkout) {
            setError('Select a workout to reschedule');
            return;
        }

        if (!scheduleData.scheduledDate) {
            setError('Select a date');
            return;
        }

        if (!scheduleData.scheduledTime) {
            setError('Select an hour');
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

            console.log('Workout successfully rescheduled:', response);
            await loadScheduledWorkouts();
            handleClosePopup();

        } catch (error) {
            console.error('Error at rescheduling the workout:', error);
            setError(error.message || 'Error reschedule');
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
            case 'planned':
                return {bg: '#e6f3ff', border: '#3182ce', text: '#2c5282'};
            case 'in_progress':
                return {bg: '#fff2e6', border: '#ed8936', text: '#c05621'};
            case 'completed':
                return {bg: '#e6ffe6', border: '#38a169', text: '#2f855a'};
            case 'cancelled':
                return {bg: '#ffe6e6', border: '#e53e3e', text: '#c53030'};
            case 'missed':
                return {bg: '#fef2f2', border: '#ef4444', text: '#dc2626'};
            case 'scheduled':
            case 'programat':
                return {bg: '#e6f3ff', border: '#3182ce', text: '#2c5282'};
            case 'în progres':
                return {bg: '#fff2e6', border: '#ed8936', text: '#c05621'};
            case 'finalizat':
                return {bg: '#e6ffe6', border: '#38a169', text: '#2f855a'};
            case 'anulat':
                return {bg: '#ffe6e6', border: '#e53e3e', text: '#c53030'};
            default:
                return {bg: '#f7fafc', border: '#cbd5e0', text: '#4a5568'};
        }
    };


    if (!isOpen) return null;

    return (
        <>
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
                        color: '#000000',
                        fontSize: '28px',
                        fontWeight: '800',
                        marginBottom: '24px',
                        textAlign: 'center',
                        backgroundClip: 'text',
                        WebkitBackgroundClip: 'text'
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
                                Your Workout Plans
                            </h3>

                            {loadingPlans ? (
                                <div style={{
                                    textAlign: 'center',
                                    padding: '40px',
                                    color: '#718096'
                                }}>
                                    <div style={{fontSize: '20px', marginBottom: '8px'}}>⏳</div>
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
                                    <div style={{fontSize: '24px', marginBottom: '8px'}}>📝</div>
                                    <div style={{fontWeight: '600', marginBottom: '4px'}}>No Workout Plans Found</div>
                                    <div style={{fontSize: '14px'}}>Create your first workout plan template</div>
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
                                                <div style={{flex: 1}}>
                                                    <h4 style={{
                                                        color: '#2d3748',
                                                        fontSize: '16px',
                                                        fontWeight: '700',
                                                        marginBottom: '4px'
                                                    }}>
                                                        {plan.planName}
                                                    </h4>

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

                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {/* Scheduled Workouts Tab*/}
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
                                    <div style={{fontSize: '20px', marginBottom: '8px'}}>⏳</div>
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
                                    <div style={{fontSize: '24px', marginBottom: '8px'}}>📅</div>
                                    <div style={{fontWeight: '600', marginBottom: '4px'}}>No Scheduled Workouts Found
                                    </div>
                                    <div style={{fontSize: '14px'}}>You don't have any scheduled workouts yet</div>
                                </div>
                            ) : (
                                <div style={{
                                    display: 'grid',
                                    gap: '12px'
                                }}>
                                    {scheduledWorkouts.map((workout) => {
                                        const statusStyle = getStatusColor(workout.status);
                                        const actionButton = getWorkoutActionButton(workout);

                                        return (
                                            <div
                                                key={workout.scheduledWorkoutId}
                                                style={{
                                                    backgroundColor: selectedWorkout?.scheduledWorkoutId === workout.scheduledWorkoutId ? '#e6fffa' : 'white',
                                                    border: selectedWorkout?.scheduledWorkoutId === workout.scheduledWorkoutId ? '2px solid #38b2ac' : '2px solid #e2e8f0',
                                                    borderRadius: '12px',
                                                    padding: '20px',
                                                    cursor: loading ? 'not-allowed' : 'pointer',
                                                    transition: 'all 0.2s',
                                                    opacity: loading ? 0.6 : 1,
                                                    position: 'relative'
                                                }}
                                            >
                                                {/* Action button for today's workouts */}
                                                {actionButton && (
                                                    <div style={{
                                                        position: 'absolute',
                                                        top: '16px',
                                                        right: '16px',
                                                        zIndex: 5
                                                    }}>
                                                        <button
                                                            onClick={(e) => {
                                                                e.stopPropagation(); // Prevent card selection
                                                                if (actionButton.enabled) {
                                                                    handleWorkoutAction(workout, actionButton.action);
                                                                }
                                                            }}
                                                            disabled={!actionButton.enabled || loading}
                                                            style={{
                                                                background: actionButton.enabled ? actionButton.color : '#9ca3af',
                                                                color: 'white',
                                                                border: 'none',
                                                                padding: '8px 12px',
                                                                borderRadius: '8px',
                                                                cursor: actionButton.enabled && !loading ? 'pointer' : 'not-allowed',
                                                                fontSize: '12px',
                                                                fontWeight: '600',
                                                                whiteSpace: 'nowrap',
                                                                boxShadow: actionButton.enabled ? '0 2px 4px rgba(0,0,0,0.1)' : 'none',
                                                                opacity: loading ? 0.6 : 1,
                                                                transition: 'all 0.2s'
                                                            }}
                                                        >
                                                            {actionButton.text}
                                                        </button>
                                                    </div>
                                                )}

                                                {/* Workout card content */}
                                                <div
                                                    onClick={() => setSelectedWorkout(workout)}
                                                    style={{
                                                        paddingRight: actionButton ? '120px' : '0px' // Make space for button
                                                    }}
                                                >
                                                    <div style={{
                                                        display: 'flex',
                                                        justifyContent: 'space-between',
                                                        alignItems: 'flex-start',
                                                        marginBottom: '12px'
                                                    }}>
                                                        <div style={{flex: 1}}>
                                                            <h4 style={{
                                                                color: '#2d3748',
                                                                fontSize: '16px',
                                                                fontWeight: '700',
                                                                marginBottom: '4px'
                                                            }}>
                                                                {cleanWorkoutTitle(workout.workoutPlan?.planName) || `Workout #${workout.scheduledWorkoutId}`}
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
                                                                {/* Today indicator */}
                                                                {isWorkoutToday(workout) && (
                                                                    <span style={{
                                                                        backgroundColor: '#fef3c7',
                                                                        color: '#92400e',
                                                                        border: '1px solid #f59e0b',
                                                                        padding: '2px 8px',
                                                                        borderRadius: '6px',
                                                                        fontSize: '12px',
                                                                        fontWeight: '600'
                                                                    }}>
                                                                    🌟 Today
                                                                </span>
                                                                )}
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
                                                        {selectedWorkout?.scheduledWorkoutId === workout.scheduledWorkoutId && !actionButton && (
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
                                                        {workout.actualDurationMinutes && (
                                                            <span>⏰ Actual: {workout.actualDurationMinutes} min</span>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    )}

                    {/* Complete Workout Modal */}
                    {showCompleteModal && (
                        <div style={{
                            position: 'fixed',
                            top: 0,
                            left: 0,
                            right: 0,
                            bottom: 0,
                            backgroundColor: 'rgba(0, 0, 0, 0.5)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            zIndex: 2000
                        }}>
                            <div style={{
                                backgroundColor: 'white',
                                borderRadius: '16px',
                                padding: '24px',
                                maxWidth: '400px',
                                width: '90%',
                                boxShadow: '0 10px 30px rgba(0, 0, 0, 0.3)'
                            }}>
                                <h3 style={{
                                    color: '#2d3748',
                                    fontSize: '20px',
                                    fontWeight: '700',
                                    marginBottom: '16px',
                                    textAlign: 'center'
                                }}>
                                    Complete Workout
                                </h3>

                                <div style={{
                                    backgroundColor: '#f0f9ff',
                                    padding: '12px',
                                    borderRadius: '8px',
                                    marginBottom: '20px',
                                    textAlign: 'center'
                                }}>
                                    <div style={{fontWeight: '600', color: '#2d3748'}}>
                                        {selectedWorkout?.workoutPlan?.planName || 'Workout'}
                                    </div>
                                    <div style={{fontSize: '14px', color: '#718096'}}>
                                        Great job finishing your workout! 🎉
                                    </div>
                                </div>

                                <div style={{marginBottom: '16px'}}>
                                    <label style={{
                                        display: 'block',
                                        marginBottom: '8px',
                                        color: '#4a5568',
                                        fontWeight: '600',
                                        fontSize: '14px'
                                    }}>
                                        Calories Burned (optional)
                                    </label>
                                    <input
                                        type="number"
                                        value={completeWorkoutData.caloriesBurned}
                                        onChange={(e) => setCompleteWorkoutData(prev => ({
                                            ...prev,
                                            caloriesBurned: e.target.value
                                        }))}
                                        placeholder="e.g., 350"
                                        min="0"
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

                                <div style={{marginBottom: '20px'}}>
                                    <label style={{
                                        display: 'block',
                                        marginBottom: '8px',
                                        color: '#4a5568',
                                        fontWeight: '600',
                                        fontSize: '14px'
                                    }}>
                                        How was your workout? ⭐
                                    </label>
                                    <div style={{
                                        display: 'flex',
                                        gap: '8px',
                                        justifyContent: 'center',
                                        marginBottom: '8px'
                                    }}>
                                        {[1, 2, 3, 4, 5].map(rating => (
                                            <button
                                                key={rating}
                                                onClick={() => setCompleteWorkoutData(prev => ({
                                                    ...prev,
                                                    rating: rating
                                                }))}
                                                style={{
                                                    background: 'none',
                                                    border: 'none',
                                                    fontSize: '24px',
                                                    cursor: 'pointer',
                                                    color: rating <= completeWorkoutData.rating ? '#f59e0b' : '#e2e8f0',
                                                    transition: 'color 0.2s'
                                                }}
                                            >
                                                ⭐
                                            </button>
                                        ))}
                                    </div>
                                    <div style={{
                                        textAlign: 'center',
                                        fontSize: '12px',
                                        color: '#718096'
                                    }}>
                                        Rating: {completeWorkoutData.rating}/5
                                    </div>
                                </div>

                                <div style={{
                                    display: 'flex',
                                    gap: '12px',
                                    justifyContent: 'flex-end'
                                }}>
                                    <button
                                        onClick={() => {
                                            setShowCompleteModal(false);
                                            setSelectedWorkout(null);
                                        }}
                                        disabled={loading}
                                        style={{
                                            background: '#f7fafc',
                                            color: '#4a5568',
                                            border: '2px solid #e2e8f0',
                                            padding: '10px 16px',
                                            borderRadius: '8px',
                                            cursor: loading ? 'not-allowed' : 'pointer',
                                            fontSize: '14px',
                                            fontWeight: '600'
                                        }}
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        onClick={handleCompleteWorkoutSubmit}
                                        disabled={loading}
                                        style={{
                                            background: loading ? '#cbd5e0' : 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
                                            color: 'white',
                                            border: 'none',
                                            padding: '10px 16px',
                                            borderRadius: '8px',
                                            cursor: loading ? 'not-allowed' : 'pointer',
                                            fontSize: '14px',
                                            fontWeight: '600'
                                        }}
                                    >
                                        {loading ? 'Completing...' : '✅ Complete Workout'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Enhanced Reschedule section*/}
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
                                marginBottom: '16px',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px'
                            }}>
                                📅 Reschedule Workout
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
                                    fontSize: '14px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '4px'
                                }}>
                <span style={{
                    backgroundColor: getStatusColor(selectedWorkout.status).bg,
                    color: getStatusColor(selectedWorkout.status).text,
                    border: `1px solid ${getStatusColor(selectedWorkout.status).border}`,
                    padding: '2px 8px',
                    borderRadius: '4px',
                    fontSize: '12px',
                    fontWeight: '600'
                }}>
                    {selectedWorkout.status || 'PLANNED'}
                </span>
                                    {selectedWorkout.workoutPlan?.estimatedDurationMinutes && (
                                        <>
                                            <span>•</span>
                                            <span>Duration: {selectedWorkout.workoutPlan.estimatedDurationMinutes} minutes</span>
                                        </>
                                    )}
                                    {selectedWorkout.workoutPlan?.difficultyLevel && (
                                        <>
                                            <span>•</span>
                                            <span>Difficulty: Level {selectedWorkout.workoutPlan.difficultyLevel}</span>
                                        </>
                                    )}
                                </div>
                            </div>

                            {/* Show warning for completed/in-progress workouts */}
                            {(selectedWorkout.status?.toLowerCase() === 'completed' ||
                                selectedWorkout.status?.toLowerCase() === 'in_progress') && (
                                <div style={{
                                    backgroundColor: '#fef3c7',
                                    color: '#92400e',
                                    padding: '12px 16px',
                                    borderRadius: '8px',
                                    marginBottom: '16px',
                                    border: '1px solid #f59e0b',
                                    fontSize: '14px',
                                    fontWeight: '600'
                                }}>
                                    ⚠️ {selectedWorkout.status?.toLowerCase() === 'completed'
                                    ? 'This workout is already completed and cannot be rescheduled.'
                                    : 'This workout is in progress and cannot be rescheduled.'}
                                </div>
                            )}

                            {/* Show info for missed workouts */}
                            {selectedWorkout.status?.toLowerCase() === 'missed' && (
                                <div style={{
                                    backgroundColor: '#fef2f2',
                                    color: '#dc2626',
                                    padding: '12px 16px',
                                    borderRadius: '8px',
                                    marginBottom: '16px',
                                    border: '1px solid #ef4444',
                                    fontSize: '14px',
                                    fontWeight: '600'
                                }}>
                                    ⏰ This workout was missed. You can reschedule it to try again.
                                </div>
                            )}

                            {/* Show info for cancelled workouts */}
                            {selectedWorkout.status?.toLowerCase() === 'cancelled' && (
                                <div style={{
                                    backgroundColor: '#fef2f2',
                                    color: '#dc2626',
                                    padding: '12px 16px',
                                    borderRadius: '8px',
                                    marginBottom: '16px',
                                    border: '1px solid #ef4444',
                                    fontSize: '14px',
                                    fontWeight: '600'
                                }}>
                                    ❌ This workout was cancelled. Rescheduling will reactivate it.
                                </div>
                            )}

                            {/* Only show reschedule inputs if workout can be rescheduled */}
                            {selectedWorkout.status?.toLowerCase() !== 'completed' &&
                                selectedWorkout.status?.toLowerCase() !== 'in_progress' && (
                                    <>
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
                                                    onChange={(e) => setScheduleData(prev => ({
                                                        ...prev,
                                                        scheduledDate: e.target.value
                                                    }))}
                                                    min={new Date().toISOString().split('T')[0]}
                                                    disabled={loading}
                                                    style={{
                                                        width: '100%',
                                                        padding: '12px 16px',
                                                        border: '2px solid #e2e8f0',
                                                        borderRadius: '8px',
                                                        fontSize: '14px',
                                                        boxSizing: 'border-box',
                                                        backgroundColor: loading ? '#f7fafc' : 'white'
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
                                                    onChange={(e) => setScheduleData(prev => ({
                                                        ...prev,
                                                        scheduledTime: e.target.value
                                                    }))}
                                                    disabled={loading}
                                                    style={{
                                                        width: '100%',
                                                        padding: '12px 16px',
                                                        border: '2px solid #e2e8f0',
                                                        borderRadius: '8px',
                                                        fontSize: '14px',
                                                        boxSizing: 'border-box',
                                                        backgroundColor: loading ? '#f7fafc' : 'white'
                                                    }}
                                                />
                                            </div>
                                        </div>

                                        {/* Quick Date Options */}
                                        <div style={{
                                            marginBottom: '16px'
                                        }}>
                                            <label style={{
                                                display: 'block',
                                                marginBottom: '8px',
                                                color: '#4a5568',
                                                fontWeight: '600',
                                                fontSize: '14px'
                                            }}>
                                                Quick Options:
                                            </label>
                                            <div style={{
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
                                                        fontWeight: '600',
                                                        transition: 'all 0.2s'
                                                    }}
                                                >
                                                    📅 Today
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
                                                        fontWeight: '600',
                                                        transition: 'all 0.2s'
                                                    }}
                                                >
                                                    📅 Tomorrow
                                                </button>
                                                <button
                                                    onClick={() => {
                                                        const nextWeek = new Date();
                                                        nextWeek.setDate(nextWeek.getDate() + 7);
                                                        setScheduleData(prev => ({
                                                            ...prev,
                                                            scheduledDate: nextWeek.toISOString().split('T')[0]
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
                                                        fontWeight: '600',
                                                        transition: 'all 0.2s'
                                                    }}
                                                >
                                                    📅 Next Week
                                                </button>
                                            </div>
                                        </div>

                                        {/* Show preview of new schedule */}
                                        {scheduleData.scheduledDate && scheduleData.scheduledTime && (
                                            <div style={{
                                                backgroundColor: '#f0f9ff',
                                                padding: '12px 16px',
                                                borderRadius: '8px',
                                                marginBottom: '16px',
                                                border: '1px solid #bfdbfe'
                                            }}>
                                                <div style={{
                                                    color: '#1e40af',
                                                    fontSize: '14px',
                                                    fontWeight: '600'
                                                }}>
                                                    📋 New Schedule Preview:
                                                </div>
                                                <div style={{
                                                    color: '#3730a3',
                                                    fontSize: '14px',
                                                    marginTop: '4px'
                                                }}>
                                                    {formatDate(scheduleData.scheduledDate)} at {scheduleData.scheduledTime}
                                                </div>
                                                {/* Show reactivation message for cancelled/missed workouts */}
                                                {(selectedWorkout.status?.toLowerCase() === 'cancelled' ||
                                                    selectedWorkout.status?.toLowerCase() === 'missed') && (
                                                    <div style={{
                                                        color: '#059669',
                                                        fontSize: '12px',
                                                        marginTop: '4px',
                                                        fontWeight: '600'
                                                    }}>
                                                        ✅ Status will change to PLANNED when rescheduled
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </>
                                )}
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
                                    display: 'flex',
                                    gap: '16px',
                                    flexWrap: 'wrap',
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
                                </div>

                                {selectedPlan.notes && (
                                    <div style={{
                                        marginTop: '12px',
                                        padding: '12px',
                                        backgroundColor: '#f8fafc',
                                        borderRadius: '6px',
                                        border: '1px solid #e2e8f0'
                                    }}>
                                        <strong style={{color: '#4a5568'}}>Notes:</strong>
                                        <div style={{color: '#718096', marginTop: '4px'}}>
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

                                        } catch (error) {
                                            console.error('Error loading plan details:', error);

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
                                     View Exercises
                                </button>

                                <button
                                    onClick={() => {
                                        console.log('Edit plan:', selectedPlan.workoutPlanId);

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
                                    Edit Plan
                                </button>

                                <button
                                    onClick={async () => {
                                        if (window.confirm(`Are you sure you want to delete "${selectedPlan.planName}"?`)) {
                                            try {
                                                await WorkoutPlanService.deleteWorkoutPlan(selectedPlan.workoutPlanId);

                                                await loadWorkoutPlans();
                                                setSelectedPlan(null);
                                            } catch (error) {
                                                console.error('Error deleting plan:', error);

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
                                    Delete Plan
                                </button>

                                <button
                                    onClick={() => handleScheduleWorkout(selectedPlan)}
                                    disabled={!selectedPlan}
                                    style={{
                                        background: !selectedPlan
                                            ? '#cbd5e0'
                                            : 'linear-gradient(135deg, #10b981, #059669)',
                                        color: 'white',
                                        border: 'none',
                                        padding: '10px 16px',
                                        borderRadius: '8px',
                                        cursor: !selectedPlan ? 'not-allowed' : 'pointer',
                                        fontSize: '14px',
                                        fontWeight: '600'
                                    }}
                                >
                                    Schedule Workout
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
                                }}>⚡
                                </div>
                                <div style={{fontWeight: '600'}}>
                                    {activeTab === 'scheduled' ? 'Processing workout...' : 'Loading...'}
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
                                {loading ? 'Rescheduling...' : 'Reschedule Workout'}
                            </button>
                        )}
                    </div>

                    {/* CSS for animations */}
                    <style jsx>{`
                        @keyframes spin {
                            0% {
                                transform: rotate(0deg);
                            }
                            100% {
                                transform: rotate(360deg);
                            }
                        }
                    `}</style>
                </div>
            </div>

            {/* Schedule Workout Modal */}
            <WorkoutSchedule
                isOpen={showScheduleModal}
                onClose={() => {
                    setShowScheduleModal(false);
                    setSelectedPlanForScheduling(null);
                }}
                workoutPlan={selectedPlanForScheduling}
                currentUserId={currentUserId}
                onWorkoutScheduled={handleWorkoutScheduled}
            />
        </>
    );
}
export default PlanWorkout;