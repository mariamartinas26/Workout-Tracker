// components/WorkoutScheduler.js
import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8082/api';

// Service pentru scheduled workouts
const WorkoutService = {
    // Obține workout-urile programate ale utilizatorului
    getUserScheduledWorkouts: async (userId) => {
        const response = await fetch(`${API_BASE_URL}/scheduled-workouts/user/${userId}`);
        if (!response.ok) {
            throw new Error('Failed to fetch scheduled workouts');
        }
        return await response.json();
    },

    // Reprogramează un workout existent
    rescheduleWorkout: async (workoutId, newDate, newTime) => {
        // Dacă nu există endpoint specific pentru reprogramare, folosim endpoint-ul de programare
        // cu ID-ul workout-ului existent
        const response = await fetch(`${API_BASE_URL}/scheduled-workouts/schedule`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                scheduledWorkoutId: workoutId,
                scheduledDate: newDate,
                scheduledTime: newTime
            })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to reschedule workout');
        }

        return await response.json();
    },

    // Verifică disponibilitatea pentru programare
    checkAvailability: async (userId, date, time) => {
        const params = new URLSearchParams({
            date: date,
            ...(time && { time: time })
        });

        const response = await fetch(`${API_BASE_URL}/scheduled-workouts/user/${userId}/availability?${params}`);
        if (!response.ok) {
            throw new Error('Failed to check availability');
        }
        return await response.json();
    }
};

const WorkoutScheduler = ({ isOpen, onClose, currentUserId = 1 }) => {
    const [scheduledWorkouts, setScheduledWorkouts] = useState([]);
    const [selectedWorkout, setSelectedWorkout] = useState(null);
    const [scheduleData, setScheduleData] = useState({
        scheduledDate: '',
        scheduledTime: '10:00'
    });

    const [loading, setLoading] = useState(false);
    const [loadingWorkouts, setLoadingWorkouts] = useState(false);
    const [error, setError] = useState('');
    const [availability, setAvailability] = useState(null);
    const [checkingAvailability, setCheckingAvailability] = useState(false);

    // Încarcă workout-urile programate când se deschide popup-ul
    useEffect(() => {
        if (isOpen) {
            loadScheduledWorkouts();
            // Setează data de azi ca default
            const today = new Date().toISOString().split('T')[0];
            setScheduleData(prev => ({ ...prev, scheduledDate: today }));
        }
    }, [isOpen, currentUserId]);

    // Verifică disponibilitatea când se schimbă data sau ora
    useEffect(() => {
        if (scheduleData.scheduledDate && scheduleData.scheduledTime) {
            checkTimeAvailability();
        }
    }, [scheduleData.scheduledDate, scheduleData.scheduledTime]);

    const loadScheduledWorkouts = async () => {
        setLoadingWorkouts(true);
        setError('');
        try {
            const workouts = await WorkoutService.getUserScheduledWorkouts(currentUserId);
            setScheduledWorkouts(workouts);
            console.log('Loaded scheduled workouts:', workouts);
        } catch (error) {
            console.error('Error loading scheduled workouts:', error);
            setError('Nu s-au putut încărca workout-urile programate');
        } finally {
            setLoadingWorkouts(false);
        }
    };

    const checkTimeAvailability = async () => {
        if (!scheduleData.scheduledDate || !scheduleData.scheduledTime) return;

        setCheckingAvailability(true);
        try {
            const result = await WorkoutService.checkAvailability(
                currentUserId,
                scheduleData.scheduledDate,
                scheduleData.scheduledTime
            );
            setAvailability(result);
        } catch (error) {
            console.error('Error checking availability:', error);
            setAvailability(null);
        } finally {
            setCheckingAvailability(false);
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

        if (availability && !availability.available) {
            setError('Slotul selectat nu este disponibil');
            return;
        }

        setLoading(true);
        setError('');

        try {
            const response = await WorkoutService.rescheduleWorkout(
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
        setScheduleData({
            scheduledDate: '',
            scheduledTime: '10:00'
        });
        setError('');
        setAvailability(null);
        setLoading(false);
        onClose();
    };

    const getTomorrowDate = () => {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        return tomorrow.toISOString().split('T')[0];
    };

    const getMinDate = () => {
        return new Date().toISOString().split('T')[0];
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
                    Reschedule Your Workouts
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
                            <div style={{ fontWeight: '600' }}>Se reprogramează workout-ul...</div>
                        </div>
                    </div>
                )}

                {/* Scheduled Workouts Section */}
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

                {/* Reschedule Details Section */}
                {selectedWorkout && (
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
                                    min={getMinDate()}
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

                        {/* Availability Check */}
                        {scheduleData.scheduledDate && scheduleData.scheduledTime && (
                            <div style={{
                                padding: '12px 16px',
                                borderRadius: '8px',
                                backgroundColor: checkingAvailability ? '#f7fafc' :
                                    availability?.available ? '#f0fff4' : '#fed7d7',
                                border: `1px solid ${checkingAvailability ? '#e2e8f0' :
                                    availability?.available ? '#c6f6d5' : '#feb2b2'}`,
                                color: checkingAvailability ? '#718096' :
                                    availability?.available ? '#2f855a' : '#c53030',
                                fontSize: '14px',
                                fontWeight: '600'
                            }}>
                                {checkingAvailability ? (
                                    '🔄 Checking availability...'
                                ) : availability?.available ? (
                                    `✅ ${availability.message || 'Slot disponibil'}`
                                ) : (
                                    `❌ ${availability?.message || 'Slot ocupat'}`
                                )}
                            </div>
                        )}

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
                                onClick={() => setScheduleData(prev => ({
                                    ...prev,
                                    scheduledDate: getTomorrowDate()
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
                                Tomorrow
                            </button>
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
                        onClick={handleRescheduleWorkout}
                        disabled={loading || !selectedWorkout || !scheduleData.scheduledDate || !scheduleData.scheduledTime || (availability && !availability.available)}
                        style={{
                            background: (loading || !selectedWorkout || !scheduleData.scheduledDate || !scheduleData.scheduledTime || (availability && !availability.available))
                                ? '#cbd5e0'
                                : 'linear-gradient(135deg, #48bb78, #38a169)',
                            color: 'white',
                            border: 'none',
                            padding: '12px 24px',
                            borderRadius: '10px',
                            cursor: (loading || !selectedWorkout || !scheduleData.scheduledDate || !scheduleData.scheduledTime || (availability && !availability.available))
                                ? 'not-allowed'
                                : 'pointer',
                            fontSize: '14px',
                            fontWeight: '600'
                        }}
                    >
                        {loading ? 'Se reprogramează...' : 'Reschedule Workout'}
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

export default WorkoutScheduler;