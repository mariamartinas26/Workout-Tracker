// components/WorkoutSchedule.js
import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8082/api';

// Service pentru programarea workout-urilor
const ScheduleWorkoutService = {
    /**
     * Programează un workout nou
     */
    scheduleWorkout: async (userId, workoutPlanId, scheduledDate, scheduledTime) => {
        try {
            // Ensure proper data types and format
            const requestData = {
                userId: parseInt(userId),
                workoutPlanId: parseInt(workoutPlanId),
                scheduledDate: scheduledDate, // Should be in YYYY-MM-DD format
                scheduledTime: scheduledTime   // Should be in HH:MM format
            };

            console.log('Sending schedule request:', requestData);

            const response = await fetch(`${API_BASE_URL}/scheduled-workouts/schedule`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    // Adaugă authorization header dacă e necesar
                    // 'Authorization': `Bearer ${token}`
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
                    // If response is not JSON, get text
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

    /**
     * Verifică dacă utilizatorul are deja un workout programat la data și ora respectivă
     */
    checkConflictingSchedule: async (userId, scheduledDate, scheduledTime) => {
        try {
            const response = await fetch(`${API_BASE_URL}/scheduled-workouts/user/${userId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                return false; // În caz de eroare, presupunem că nu există conflict
            }

            const scheduledWorkouts = await response.json();

            // Verifică dacă există un workout la aceeași dată și oră
            const conflict = scheduledWorkouts.some(workout => {
                const workoutDate = new Date(workout.scheduledDate).toISOString().split('T')[0];
                const workoutTime = workout.scheduledTime ? workout.scheduledTime.substring(0, 5) : '';

                return workoutDate === scheduledDate &&
                    workoutTime === scheduledTime &&
                    workout.status !== 'cancelled' &&
                    workout.status !== 'anulat';
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
                                  onWorkoutScheduled // Callback pentru când workout-ul a fost programat cu succes
                              }) => {
    const [scheduleData, setScheduleData] = useState({
        scheduledDate: '',
        scheduledTime: '10:00'
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [hasConflict, setHasConflict] = useState(false);

    // Resetează starea când se deschide modal-ul
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

    // Verifică conflictele când se schimbă data sau ora
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
        // Validări
        if (!workoutPlan) {
            setError('Nu a fost selectat niciun plan de workout');
            return;
        }

        if (!scheduleData.scheduledDate) {
            setError('Selectează o dată pentru workout');
            return;
        }

        if (!scheduleData.scheduledTime) {
            setError('Selectează o oră pentru workout');
            return;
        }

        // Validează formatul datei (YYYY-MM-DD)
        const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
        if (!dateRegex.test(scheduleData.scheduledDate)) {
            setError('Formatul datei nu este valid (YYYY-MM-DD)');
            return;
        }

        // Validează formatul orei (HH:MM)
        const timeRegex = /^\d{2}:\d{2}$/;
        if (!timeRegex.test(scheduleData.scheduledTime)) {
            setError('Formatul orei nu este valid (HH:MM)');
            return;
        }

        // Verifică dacă data nu este în trecut
        const selectedDateTime = new Date(`${scheduleData.scheduledDate}T${scheduleData.scheduledTime}`);
        const now = new Date();

        if (selectedDateTime < now) {
            setError('Nu poți programa un workout în trecut');
            return;
        }

        // Validează că IDs sunt numere valide
        if (!workoutPlan.workoutPlanId || isNaN(workoutPlan.workoutPlanId)) {
            setError('ID-ul planului de workout nu este valid');
            return;
        }

        if (!currentUserId || isNaN(currentUserId)) {
            setError('ID-ul utilizatorului nu este valid');
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

            setSuccess(`Workout-ul "${workoutPlan.planName}" a fost programat cu succes!`);

            // Notifică componenta părinte despre succesul operației
            if (onWorkoutScheduled) {
                onWorkoutScheduled(result);
            }

            // Închide modal-ul după 2 secunde
            setTimeout(() => {
                handleCloseModal();
            }, 2000);

        } catch (error) {
            console.error('Eroare la programarea workout-ului:', error);

            // Afișează un mesaj de eroare mai detaliat
            let errorMessage = 'A apărut o eroare la programarea workout-ului';
            if (error.message) {
                if (error.message.includes('Type definition error')) {
                    errorMessage = 'Eroare de validare a datelor. Verifică că toate câmpurile sunt completate corect.';
                } else if (error.message.includes('400')) {
                    errorMessage = 'Datele introduse nu sunt valide. Verifică data și ora selectate.';
                } else if (error.message.includes('401')) {
                    errorMessage = 'Nu ești autorizat să programezi workout-uri.';
                } else if (error.message.includes('404')) {
                    errorMessage = 'Planul de workout nu a fost găsit.';
                } else if (error.message.includes('500')) {
                    errorMessage = 'Eroare de server. Încearcă din nou mai târziu.';
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
            { label: 'Dimineața', time: '07:00' },
            { label: 'Prânz', time: '12:00' },
            { label: 'După-amiază', time: '17:00' },
            { label: 'Seara', time: '19:00' }
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
                label: 'Astăzi',
                date: today.toISOString().split('T')[0]
            },
            {
                label: 'Mâine',
                date: tomorrow.toISOString().split('T')[0]
            },
            {
                label: 'Săptămâna viitoare',
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
                {/* Close button */}
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

                {/* Workout Plan Info */}
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

                {/* Success message */}
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

                {/* Conflict warning */}
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
                        ⚠️ Ai deja un workout programat la această dată și oră. Poți continua dacă dorești să ai multiple workout-uri în aceeași perioadă.
                    </div>
                )}

                {/* Schedule Form */}
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
                        Alege data și ora
                    </h3>

                    {/* Date and Time inputs */}
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
                                Data *
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
                                Ora *
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

                    {/* Selected date/time preview */}
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
                                Workout programat pentru:
                            </div>
                            <div style={{
                                color: '#4a5568',
                                fontSize: '16px'
                            }}>
                                📅 {formatDate(scheduleData.scheduledDate)} la 🕐 {scheduleData.scheduledTime}
                            </div>
                        </div>
                    )}

                    {/* Quick Date Options */}
                    <div style={{ marginBottom: '16px' }}>
                        <label style={{
                            display: 'block',
                            marginBottom: '8px',
                            color: '#4a5568',
                            fontWeight: '600',
                            fontSize: '14px'
                        }}>
                            Date rapide:
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

                    {/* Quick Time Options */}
                    <div>
                        <label style={{
                            display: 'block',
                            marginBottom: '8px',
                            color: '#4a5568',
                            fontWeight: '600',
                            fontSize: '14px'
                        }}>
                            Ore populare:
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
                                Se programează workout-ul...
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
                        Anulează
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
                        {loading ? 'Se programează...' : '📅 Programează Workout'}
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

export default WorkoutSchedule;