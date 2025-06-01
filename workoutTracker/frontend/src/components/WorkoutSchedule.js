import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8082/api';

const ScheduleWorkoutService = {
    scheduleWorkout: async (userId, workoutPlanId, scheduledDate, scheduledTime) => {
        try {
            const requestData = {
                userId: parseInt(userId),
                workoutPlanId: parseInt(workoutPlanId),
                scheduledDate: scheduledDate,
                scheduledTime: scheduledTime
            };

            console.log('Sending schedule request:', requestData);

            const response = await fetch(`${API_BASE_URL}/scheduled-workouts/schedule`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestData)
            });

            console.log('Response status:', response.status);
            console.log('Response headers:', response.headers);

            if (!response.ok) {
                let errorMessage = `HTTP ${response.status}`;
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.message || errorData.error || errorMessage;
                    console.error('Error response data:', errorData);
                } catch (parseError) {
                    const errorText = await response.text();
                    console.error('Error response text:', errorText);
                    errorMessage = errorText || errorMessage;
                }
                throw new Error(errorMessage);
            }

            const result = await response.json();
            console.log('Workout scheduled successfully:', result);
            return result;

        } catch (error) {
            console.error('Error scheduling workout:', error);
            throw error;
        }
    },

    checkConflictingSchedule: async (userId, scheduledDate, scheduledTime) => {
        try {
            const response = await fetch(`${API_BASE_URL}/scheduled-workouts/user/${userId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                return false;
            }

            const scheduledWorkouts = await response.json();

            const conflict = scheduledWorkouts.some(workout => {
                const workoutDate = new Date(workout.scheduledDate).toISOString().split('T')[0];
                const workoutTime = workout.scheduledTime ? workout.scheduledTime.substring(0, 5) : '';

                return workoutDate === scheduledDate &&
                    workoutTime === scheduledTime &&
                    workout.status !== 'cancelled' &&
                    workout.status !== 'canceled';
            });

            return conflict;

        } catch (error) {
            console.error('Error checking schedule conflicts:', error);
            return false;
        }
    }
};

const WorkoutSchedule = ({
                             isOpen,
                             onClose,
                             workoutPlan,
                             currentUserId = 1,
                             onWorkoutScheduled
                         }) => {
    const [scheduleData, setScheduleData] = useState({
        scheduledDate: '',
        scheduledTime: '10:00'
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [hasConflict, setHasConflict] = useState(false);

    useEffect(() => {
        if (isOpen) {
            const today = new Date().toISOString().split('T')[0];
            setScheduleData({
                scheduledDate: today,
                scheduledTime: '10:00'
            });
            setError('');
            setSuccess('');
            setHasConflict(false);
        }
    }, [isOpen]);

    useEffect(() => {
        if (isOpen && scheduleData.scheduledDate && scheduleData.scheduledTime) {
            checkForConflicts();
        }
    }, [scheduleData.scheduledDate, scheduleData.scheduledTime, isOpen]);

    const checkForConflicts = async () => {
        try {
            const conflict = await ScheduleWorkoutService.checkConflictingSchedule(
                currentUserId,
                scheduleData.scheduledDate,
                scheduleData.scheduledTime
            );
            setHasConflict(conflict);
        } catch (error) {
            console.error('Error checking conflicts:', error);
            setHasConflict(false);
        }
    };

    const handleScheduleWorkout = async () => {
        if (!workoutPlan) {
            setError('No workout plan selected');
            return;
        }

        if (!scheduleData.scheduledDate) {
            setError('Please select a date for the workout');
            return;
        }

        if (!scheduleData.scheduledTime) {
            setError('Please select a time for the workout');
            return;
        }

        const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
        if (!dateRegex.test(scheduleData.scheduledDate)) {
            setError('Invalid date format (YYYY-MM-DD)');
            return;
        }

        const timeRegex = /^\d{2}:\d{2}$/;
        if (!timeRegex.test(scheduleData.scheduledTime)) {
            setError('Invalid time format (HH:MM)');
            return;
        }

        const selectedDateTime = new Date(`${scheduleData.scheduledDate}T${scheduleData.scheduledTime}`);
        const now = new Date();

        if (selectedDateTime < now) {
            setError('Cannot schedule a workout in the past');
            return;
        }

        if (!workoutPlan.workoutPlanId || isNaN(workoutPlan.workoutPlanId)) {
            setError('Invalid workout plan ID');
            return;
        }

        if (!currentUserId || isNaN(currentUserId)) {
            setError('Invalid user ID');
            return;
        }

        setLoading(true);
        setError('');
        setSuccess('');

        try {
            console.log('Scheduling workout with params:', {
                userId: currentUserId,
                workoutPlanId: workoutPlan.workoutPlanId,
                scheduledDate: scheduleData.scheduledDate,
                scheduledTime: scheduleData.scheduledTime
            });

            const result = await ScheduleWorkoutService.scheduleWorkout(
                currentUserId,
                workoutPlan.workoutPlanId,
                scheduleData.scheduledDate,
                scheduleData.scheduledTime
            );

            setSuccess(`Workout "${workoutPlan.planName}" has been scheduled successfully!`);

            if (onWorkoutScheduled) {
                onWorkoutScheduled(result);
            }

            setTimeout(() => {
                handleCloseModal();
            }, 2000);

        } catch (error) {
            console.error('Error scheduling workout:', error);

            let errorMessage = 'An error occurred while scheduling the workout';
            if (error.message) {
                if (error.message.includes('Type definition error')) {
                    errorMessage = 'Data validation error. Please check that all fields are completed correctly.';
                } else if (error.message.includes('400')) {
                    errorMessage = 'Invalid data entered. Please check the selected date and time.';
                } else if (error.message.includes('401')) {
                    errorMessage = 'You are not authorized to schedule workouts.';
                } else if (error.message.includes('404')) {
                    errorMessage = 'Workout plan not found.';
                } else if (error.message.includes('500')) {
                    errorMessage = 'Server error. Please try again later.';
                } else {
                    errorMessage = error.message;
                }
            }

            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const handleCloseModal = () => {
        setScheduleData({
            scheduledDate: '',
            scheduledTime: '10:00'
        });
        setError('');
        setSuccess('');
        setHasConflict(false);
        setLoading(false);
        onClose();
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ro-RO', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    };

    const getQuickTimeOptions = () => {
        return [
            { label: 'Morning', time: '07:00' },
            { label: 'Lunch', time: '12:00' },
            { label: 'Afternoon', time: '17:00' },
            { label: 'Evening', time: '19:00' }
        ];
    };

    const getQuickDateOptions = () => {
        const today = new Date();
        const tomorrow = new Date(today);
        tomorrow.setDate(tomorrow.getDate() + 1);

        const nextWeek = new Date(today);
        nextWeek.setDate(nextWeek.getDate() + 7);

        return [
            {
                label: 'Today',
                date: today.toISOString().split('T')[0]
            },
            {
                label: 'Tomorrow',
                date: tomorrow.toISOString().split('T')[0]
            },
            {
                label: 'Next Week',
                date: nextWeek.toISOString().split('T')[0]
            }
        ];
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
                maxWidth: '600px',
                width: '100%',
                maxHeight: '90vh',
                overflowY: 'auto',
                boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
                position: 'relative'
            }}>
                <button
                    onClick={handleCloseModal}
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
                    background: 'linear-gradient(135deg, #10b981, #059669)',
                    backgroundClip: 'text',
                    WebkitBackgroundClip: 'text',
                    WebkitTextFillColor: 'transparent'
                }}>
                    📅 Schedule Workout
                </h2>

                {workoutPlan && (
                    <div style={{
                        backgroundColor: '#f0fff4',
                        padding: '20px',
                        borderRadius: '12px',
                        marginBottom: '24px',
                        border: '1px solid #c6f6d5'
                    }}>
                        <h3 style={{
                            color: '#2d3748',
                            fontSize: '18px',
                            fontWeight: '700',
                            marginBottom: '8px'
                        }}>
                            {workoutPlan.planName}
                        </h3>

                        {workoutPlan.description && (
                            <p style={{
                                color: '#4a5568',
                                fontSize: '14px',
                                marginBottom: '12px',
                                lineHeight: '1.5'
                            }}>
                                {workoutPlan.description}
                            </p>
                        )}

                        <div style={{
                            display: 'flex',
                            gap: '16px',
                            flexWrap: 'wrap',
                            color: '#718096',
                            fontSize: '13px'
                        }}>
                            {workoutPlan.estimatedDurationMinutes && (
                                <span>⏱️ {workoutPlan.estimatedDurationMinutes} min</span>
                            )}
                            {workoutPlan.difficultyLevel && (
                                <span>💪 Level {workoutPlan.difficultyLevel}/5</span>
                            )}
                            {workoutPlan.goals && (
                                <span>🎯 {workoutPlan.goals}</span>
                            )}
                        </div>
                    </div>
                )}

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

                {success && (
                    <div style={{
                        backgroundColor: '#c6f6d5',
                        color: '#2f855a',
                        padding: '12px 16px',
                        borderRadius: '8px',
                        marginBottom: '24px',
                        border: '1px solid #9ae6b4',
                        fontSize: '14px',
                        fontWeight: '600'
                    }}>
                        ✅ {success}
                    </div>
                )}

                {hasConflict && !error && !success && (
                    <div style={{
                        backgroundColor: '#fef2e2',
                        color: '#d97706',
                        padding: '12px 16px',
                        borderRadius: '8px',
                        marginBottom: '24px',
                        border: '1px solid #fed7aa',
                        fontSize: '14px',
                        fontWeight: '600'
                    }}>
                        ⚠️ You already have a workout scheduled at this date and time. You can continue if you want to have multiple workouts in the same period.
                    </div>
                )}

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
                        marginBottom: '20px'
                    }}>
                        Choose date and time
                    </h3>

                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: '1fr 1fr',
                        gap: '16px',
                        marginBottom: '20px'
                    }}>
                        <div>
                            <label style={{
                                display: 'block',
                                marginBottom: '8px',
                                color: '#4a5568',
                                fontWeight: '600',
                                fontSize: '14px'
                            }}>
                                Date *
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
                                Time *
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
                                    opacity: loading ? 0.6 : 1
                                }}
                            />
                        </div>
                    </div>

                    {scheduleData.scheduledDate && scheduleData.scheduledTime && (
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
                                marginBottom: '4px'
                            }}>
                                Workout scheduled for:
                            </div>
                            <div style={{
                                color: '#4a5568',
                                fontSize: '16px'
                            }}>
                                📅 {formatDate(scheduleData.scheduledDate)} at 🕐 {scheduleData.scheduledTime}
                            </div>
                        </div>
                    )}

                    <div style={{ marginBottom: '16px' }}>
                        <label style={{
                            display: 'block',
                            marginBottom: '8px',
                            color: '#4a5568',
                            fontWeight: '600',
                            fontSize: '14px'
                        }}>
                            Quick dates:
                        </label>
                        <div style={{
                            display: 'flex',
                            gap: '8px',
                            flexWrap: 'wrap'
                        }}>
                            {getQuickDateOptions().map((option, index) => (
                                <button
                                    key={index}
                                    onClick={() => setScheduleData(prev => ({
                                        ...prev,
                                        scheduledDate: option.date
                                    }))}
                                    disabled={loading}
                                    style={{
                                        background: scheduleData.scheduledDate === option.date
                                            ? '#10b981' : '#e2e8f0',
                                        color: scheduleData.scheduledDate === option.date
                                            ? 'white' : '#4a5568',
                                        border: 'none',
                                        padding: '8px 12px',
                                        borderRadius: '6px',
                                        cursor: loading ? 'not-allowed' : 'pointer',
                                        fontSize: '12px',
                                        fontWeight: '600',
                                        opacity: loading ? 0.6 : 1
                                    }}
                                >
                                    {option.label}
                                </button>
                            ))}
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
                            Popular times:
                        </label>
                        <div style={{
                            display: 'flex',
                            gap: '8px',
                            flexWrap: 'wrap'
                        }}>
                            {getQuickTimeOptions().map((option, index) => (
                                <button
                                    key={index}
                                    onClick={() => setScheduleData(prev => ({
                                        ...prev,
                                        scheduledTime: option.time
                                    }))}
                                    disabled={loading}
                                    style={{
                                        background: scheduleData.scheduledTime === option.time
                                            ? '#10b981' : '#e2e8f0',
                                        color: scheduleData.scheduledTime === option.time
                                            ? 'white' : '#4a5568',
                                        border: 'none',
                                        padding: '8px 12px',
                                        borderRadius: '6px',
                                        cursor: loading ? 'not-allowed' : 'pointer',
                                        fontSize: '12px',
                                        fontWeight: '600',
                                        opacity: loading ? 0.6 : 1
                                    }}
                                >
                                    {option.label}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>

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
                                Scheduling workout...
                            </div>
                        </div>
                    </div>
                )}

                <div style={{
                    display: 'flex',
                    gap: '12px',
                    justifyContent: 'flex-end'
                }}>
                    <button
                        onClick={handleCloseModal}
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
                        onClick={handleScheduleWorkout}
                        disabled={loading || !scheduleData.scheduledDate || !scheduleData.scheduledTime}
                        style={{
                            background: (loading || !scheduleData.scheduledDate || !scheduleData.scheduledTime)
                                ? '#cbd5e0'
                                : 'linear-gradient(135deg, #10b981, #059669)',
                            color: 'white',
                            border: 'none',
                            padding: '12px 24px',
                            borderRadius: '10px',
                            cursor: (loading || !scheduleData.scheduledDate || !scheduleData.scheduledTime)
                                ? 'not-allowed'
                                : 'pointer',
                            fontSize: '14px',
                            fontWeight: '600'
                        }}
                    >
                        {loading ? 'Scheduling...' : '📅 Schedule Workout'}
                    </button>
                </div>

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

export default WorkoutSchedule;