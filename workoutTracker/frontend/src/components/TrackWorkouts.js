// ============= SERVICIU API PENTRU COMUNICARE CU BACKEND =============

// Configurarea API-ului
const API_BASE_URL = 'http://localhost:8080/api'; // Schimbă cu URL-ul tău de backend

// Serviciul pentru workout sessions
const WorkoutSessionService = {
    // 1. Programează un workout
    scheduleWorkout: async (workoutData) => {
        const response = await fetch(`${API_BASE_URL}/workout-sessions/schedule`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(workoutData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to schedule workout');
        }

        return await response.json();
    },

    // 2. Începe un workout programat
    startWorkout: async (workoutId) => {
        const response = await fetch(`${API_BASE_URL}/workout-sessions/${workoutId}/start`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to start workout');
        }

        return await response.json();
    },

    // 3. Începe un workout liber (fără plan)
    startFreeWorkout: async (userId, notes = '') => {
        const response = await fetch(`${API_BASE_URL}/workout-sessions/start-free`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                userId: userId,
                notes: notes
            })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to start free workout');
        }

        return await response.json();
    },

    // 4. Înregistrează un exercițiu
    logExercise: async (workoutId, exerciseData) => {
        const response = await fetch(`${API_BASE_URL}/workout-sessions/${workoutId}/exercises`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(exerciseData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to log exercise');
        }

        return await response.json();
    },

    // 5. Finalizează workout-ul
    completeWorkout: async (workoutId, completionData) => {
        const response = await fetch(`${API_BASE_URL}/workout-sessions/${workoutId}/complete`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(completionData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to complete workout');
        }

        return await response.json();
    },

    // 6. Anulează un workout
    cancelWorkout: async (workoutId) => {
        const response = await fetch(`${API_BASE_URL}/workout-sessions/${workoutId}/cancel`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to cancel workout');
        }

        return await response.json();
    },

    // 7. Obține statusul workout-ului
    getWorkoutStatus: async (workoutId) => {
        const response = await fetch(`${API_BASE_URL}/workout-sessions/${workoutId}/status`);

        if (!response.ok) {
            throw new Error('Failed to get workout status');
        }

        return await response.json();
    },

    // 8. Obține exercițiile dintr-un workout
    getWorkoutExercises: async (workoutId) => {
        const response = await fetch(`${API_BASE_URL}/workout-sessions/${workoutId}/exercises`);

        if (!response.ok) {
            throw new Error('Failed to get workout exercises');
        }

        return await response.json();
    },

    // 9. Obține sumarul workout-ului
    getWorkoutSummary: async (workoutId) => {
        const response = await fetch(`${API_BASE_URL}/workout-sessions/${workoutId}/summary`);

        if (!response.ok) {
            throw new Error('Failed to get workout summary');
        }

        return await response.json();
    },

    // 10. Șterge un exercițiu din sesiune
    removeExerciseFromSession: async (workoutId, logId) => {
        const response = await fetch(`${API_BASE_URL}/workout-sessions/${workoutId}/exercises/${logId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to remove exercise');
        }

        return await response.json();
    }
};

// ============= COMPONENTA TRACK WORKOUTS CU INTEGRARE API =============

const TrackWorkouts = () => {
    // State-uri pentru gestionarea aplicației
    const [currentWorkout, setCurrentWorkout] = useState(null);
    const [workoutStatus, setWorkoutStatus] = useState('idle'); // idle, in_progress, completed
    const [exercises, setExercises] = useState([]);
    const [showExerciseForm, setShowExerciseForm] = useState(false);
    const [showCompletionForm, setShowCompletionForm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    // Constante pentru utilizator (în realitate ar veni din context/auth)
    const currentUserId = 1; // Înlocuiește cu ID-ul real al utilizatorului

    // Exerciții disponibile (ar putea veni din API)
    const availableExercises = [
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

    // Funcții helper pentru notificări
    const showError = (message) => {
        setError(message);
        setTimeout(() => setError(''), 5000);
    };

    const showSuccess = (message) => {
        setSuccess(message);
        setTimeout(() => setSuccess(''), 3000);
    };

    // ============= FUNCȚII PRINCIPALE =============

    // 1. Începe un workout liber
    const handleStartFreeWorkout = async () => {
        setLoading(true);
        try {
            const response = await WorkoutSessionService.startFreeWorkout(currentUserId, 'Free workout session');

            setCurrentWorkout(response);
            setWorkoutStatus('in_progress');
            showSuccess('Workout-ul a început cu succes!');

            // Începe să încarce exercițiile
            await loadWorkoutExercises(response.workoutId);

        } catch (error) {
            showError(error.message);
        } finally {
            setLoading(false);
        }
    };

    // 2. Încarcă exercițiile pentru workout-ul curent
    const loadWorkoutExercises = async (workoutId) => {
        try {
            const exercisesList = await WorkoutSessionService.getWorkoutExercises(workoutId);
            setExercises(exercisesList);
        } catch (error) {
            showError('Nu s-au putut încărca exercițiile');
        }
    };

    // 3. Adaugă un exercițiu nou
    const handleAddExercise = async (exerciseData) => {
        if (!currentWorkout) return;

        setLoading(true);
        try {
            const response = await WorkoutSessionService.logExercise(currentWorkout.workoutId, {
                exerciseId: exerciseData.exerciseId,
                exerciseOrder: exercises.length + 1,
                setsCompleted: parseInt(exerciseData.sets) || 0,
                repsCompleted: parseInt(exerciseData.reps) || 0,
                weightUsedKg: parseFloat(exerciseData.weight) || null,
                durationSeconds: parseInt(exerciseData.duration) || null,
                caloriesBurned: parseInt(exerciseData.calories) || 0,
                difficultyRating: parseInt(exerciseData.difficulty) || 3,
                notes: exerciseData.notes || ''
            });

            showSuccess('Exercițiu adăugat cu succes!');
            await loadWorkoutExercises(currentWorkout.workoutId);
            setShowExerciseForm(false);

        } catch (error) {
            showError(error.message);
        } finally {
            setLoading(false);
        }
    };

    // 4. Șterge un exercițiu
    const handleRemoveExercise = async (logId) => {
        if (!currentWorkout) return;

        try {
            await WorkoutSessionService.removeExerciseFromSession(currentWorkout.workoutId, logId);
            showSuccess('Exercițiu șters!');
            await loadWorkoutExercises(currentWorkout.workoutId);
        } catch (error) {
            showError(error.message);
        }
    };

    // 5. Finalizează workout-ul
    const handleCompleteWorkout = async (completionData) => {
        if (!currentWorkout) return;

        setLoading(true);
        try {
            const response = await WorkoutSessionService.completeWorkout(currentWorkout.workoutId, {
                totalCaloriesBurned: parseInt(completionData.calories) || 0,
                overallRating: parseInt(completionData.rating) || 3,
                energyLevelAfter: parseInt(completionData.energy) || 3,
                notes: completionData.notes || ''
            });

            setWorkoutStatus('completed');
            showSuccess('Workout finalizat cu succes! Felicitări!');
            setShowCompletionForm(false);

        } catch (error) {
            showError(error.message);
        } finally {
            setLoading(false);
        }
    };

    // 6. Anulează workout-ul
    const handleCancelWorkout = async () => {
        if (!currentWorkout) return;

        try {
            await WorkoutSessionService.cancelWorkout(currentWorkout.workoutId);
            setCurrentWorkout(null);
            setWorkoutStatus('idle');
            setExercises([]);
            showSuccess('Workout anulat');
        } catch (error) {
            showError(error.message);
        }
    };

    // 7. Resetează pentru un workout nou
    const handleStartNewWorkout = () => {
        setCurrentWorkout(null);
        setWorkoutStatus('idle');
        setExercises([]);
        setShowExerciseForm(false);
        setShowCompletionForm(false);
    };

    // ============= COMPONENTE UI =============

    // Formular pentru adăugarea exercițiilor
    const ExerciseForm = ({ onSubmit, onCancel }) => {
        const [formData, setFormData] = useState({
            exerciseId: '',
            sets: '',
            reps: '',
            weight: '',
            duration: '',
            calories: '',
            difficulty: '3',
            notes: ''
        });

        const handleSubmit = (e) => {
            e.preventDefault();
            if (!formData.exerciseId) {
                showError('Selectează un exercițiu');
                return;
            }
            onSubmit(formData);
        };

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
                    overflowY: 'auto'
                }}>
                    <h2 style={{ marginBottom: '24px', color: '#2d3748' }}>Adaugă Exercițiu</h2>

                    <form onSubmit={handleSubmit} style={{ display: 'grid', gap: '16px' }}>
                        <div>
                            <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                Exercițiu *
                            </label>
                            <select
                                value={formData.exerciseId}
                                onChange={(e) => setFormData(prev => ({ ...prev, exerciseId: e.target.value }))}
                                style={{
                                    width: '100%',
                                    padding: '12px',
                                    border: '2px solid #e2e8f0',
                                    borderRadius: '8px',
                                    fontSize: '14px'
                                }}
                                required
                            >
                                <option value="">Selectează exercițiul...</option>
                                {availableExercises.map(exercise => (
                                    <option key={exercise.exercise_id} value={exercise.exercise_id}>
                                        {exercise.exercise_name} ({exercise.category})
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                    Seturi
                                </label>
                                <input
                                    type="number"
                                    value={formData.sets}
                                    onChange={(e) => setFormData(prev => ({ ...prev, sets: e.target.value }))}
                                    placeholder="3"
                                    style={{
                                        width: '100%',
                                        padding: '12px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px'
                                    }}
                                />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                    Repetări
                                </label>
                                <input
                                    type="number"
                                    value={formData.reps}
                                    onChange={(e) => setFormData(prev => ({ ...prev, reps: e.target.value }))}
                                    placeholder="10"
                                    style={{
                                        width: '100%',
                                        padding: '12px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px'
                                    }}
                                />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                    Greutate (kg)
                                </label>
                                <input
                                    type="number"
                                    step="0.5"
                                    value={formData.weight}
                                    onChange={(e) => setFormData(prev => ({ ...prev, weight: e.target.value }))}
                                    placeholder="20"
                                    style={{
                                        width: '100%',
                                        padding: '12px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px'
                                    }}
                                />
                            </div>
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                    Durată (secunde)
                                </label>
                                <input
                                    type="number"
                                    value={formData.duration}
                                    onChange={(e) => setFormData(prev => ({ ...prev, duration: e.target.value }))}
                                    placeholder="60"
                                    style={{
                                        width: '100%',
                                        padding: '12px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px'
                                    }}
                                />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                    Calorii arse
                                </label>
                                <input
                                    type="number"
                                    value={formData.calories}
                                    onChange={(e) => setFormData(prev => ({ ...prev, calories: e.target.value }))}
                                    placeholder="50"
                                    style={{
                                        width: '100%',
                                        padding: '12px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px'
                                    }}
                                />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                    Dificultate (1-5)
                                </label>
                                <select
                                    value={formData.difficulty}
                                    onChange={(e) => setFormData(prev => ({ ...prev, difficulty: e.target.value }))}
                                    style={{
                                        width: '100%',
                                        padding: '12px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px'
                                    }}
                                >
                                    <option value="1">1 - Foarte ușor</option>
                                    <option value="2">2 - Ușor</option>
                                    <option value="3">3 - Moderat</option>
                                    <option value="4">4 - Greu</option>
                                    <option value="5">5 - Foarte greu</option>
                                </select>
                            </div>
                        </div>

                        <div>
                            <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                Notițe
                            </label>
                            <textarea
                                value={formData.notes}
                                onChange={(e) => setFormData(prev => ({ ...prev, notes: e.target.value }))}
                                placeholder="Observații despre exercițiu..."
                                rows={3}
                                style={{
                                    width: '100%',
                                    padding: '12px',
                                    border: '2px solid #e2e8f0',
                                    borderRadius: '8px',
                                    fontSize: '14px',
                                    resize: 'vertical'
                                }}
                            />
                        </div>

                        <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
                            <button
                                type="button"
                                onClick={onCancel}
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
                                Anulează
                            </button>
                            <button
                                type="submit"
                                disabled={loading}
                                style={{
                                    background: loading ? '#cbd5e0' : 'linear-gradient(135deg, #48bb78, #38a169)',
                                    color: 'white',
                                    border: 'none',
                                    padding: '12px 24px',
                                    borderRadius: '10px',
                                    cursor: loading ? 'not-allowed' : 'pointer',
                                    fontSize: '14px',
                                    fontWeight: '600'
                                }}
                            >
                                {loading ? 'Se salvează...' : 'Adaugă Exercițiu'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        );
    };

    // Formular pentru finalizarea workout-ului
    const CompletionForm = ({ onSubmit, onCancel }) => {
        const [formData, setFormData] = useState({
            calories: '',
            rating: '3',
            energy: '3',
            notes: ''
        });

        const handleSubmit = (e) => {
            e.preventDefault();
            onSubmit(formData);
        };

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
                    maxWidth: '500px',
                    width: '100%'
                }}>
                    <h2 style={{ marginBottom: '24px', color: '#2d3748' }}>Finalizează Workout-ul</h2>

                    <form onSubmit={handleSubmit} style={{ display: 'grid', gap: '16px' }}>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                    Total calorii arse
                                </label>
                                <input
                                    type="number"
                                    value={formData.calories}
                                    onChange={(e) => setFormData(prev => ({ ...prev, calories: e.target.value }))}
                                    placeholder="200"
                                    style={{
                                        width: '100%',
                                        padding: '12px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px'
                                    }}
                                />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                    Rating general (1-5)
                                </label>
                                <select
                                    value={formData.rating}
                                    onChange={(e) => setFormData(prev => ({ ...prev, rating: e.target.value }))}
                                    style={{
                                        width: '100%',
                                        padding: '12px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px'
                                    }}
                                >
                                    <option value="1">1 - Foarte slab</option>
                                    <option value="2">2 - Slab</option>
                                    <option value="3">3 - OK</option>
                                    <option value="4">4 - Bun</option>
                                    <option value="5">5 - Excelent</option>
                                </select>
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                    Energie după (1-5)
                                </label>
                                <select
                                    value={formData.energy}
                                    onChange={(e) => setFormData(prev => ({ ...prev, energy: e.target.value }))}
                                    style={{
                                        width: '100%',
                                        padding: '12px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '8px',
                                        fontSize: '14px'
                                    }}
                                >
                                    <option value="1">1 - Foarte obosit</option>
                                    <option value="2">2 - Obosit</option>
                                    <option value="3">3 - Normal</option>
                                    <option value="4">4 - Energic</option>
                                    <option value="5">5 - Foarte energic</option>
                                </select>
                            </div>
                        </div>

                        <div>
                            <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600' }}>
                                Notițe despre workout
                            </label>
                            <textarea
                                value={formData.notes}
                                onChange={(e) => setFormData(prev => ({ ...prev, notes: e.target.value }))}
                                placeholder="Cum a fost workout-ul? Ce ai observat?"
                                rows={4}
                                style={{
                                    width: '100%',
                                    padding: '12px',
                                    border: '2px solid #e2e8f0',
                                    borderRadius: '8px',
                                    fontSize: '14px',
                                    resize: 'vertical'
                                }}
                            />
                        </div>

                        <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
                            <button
                                type="button"
                                onClick={onCancel}
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
                                Anulează
                            </button>
                            <button
                                type="submit"
                                disabled={loading}
                                style={{
                                    background: loading ? '#cbd5e0' : 'linear-gradient(135deg, #667eea, #764ba2)',
                                    color: 'white',
                                    border: 'none',
                                    padding: '12px 24px',
                                    borderRadius: '10px',
                                    cursor: loading ? 'not-allowed' : 'pointer',
                                    fontSize: '14px',
                                    fontWeight: '600'
                                }}
                            >
                                {loading ? 'Se finalizează...' : 'Finalizează Workout-ul'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        );
    };

    // ============= INTERFAȚA PRINCIPALĂ =============

    return (
        <div style={{
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            padding: '20px'
        }}>
            <div style={{
                maxWidth: '1200px',
                margin: '0 auto'
            }}>
                {/* Header */}
                <div style={{
                    textAlign: 'center',
                    marginBottom: '32px'
                }}>
                    <h1 style={{
                        color: 'white',
                        fontSize: '48px',
                        fontWeight: '800',
                        marginBottom: '8px',
                        textShadow: '0 4px 8px rgba(0,0,0,0.3)'
                    }}>
                        Track Your Workouts
                    </h1>
                    <p style={{
                        color: 'rgba(255,255,255,0.9)',
                        fontSize: '18px',
                        fontWeight: '400'
                    }}>
                        Monitorizează-ți progresul și atingi-ți obiectivele
                    </p>
                </div>

                {/* Notificări */}
                {error && (
                    <div style={{
                        backgroundColor: '#fed7d7',
                        color: '#c53030',
                        padding: '16px',
                        borderRadius: '12px',
                        marginBottom: '24px',
                        border: '1px solid #feb2b2',
                        textAlign: 'center',
                        fontWeight: '600'
                    }}>
                        ⚠️ {error}
                    </div>
                )}

                {success && (
                    <div style={{
                        backgroundColor: '#c6f6d5',
                        color: '#276749',
                        padding: '16px',
                        borderRadius: '12px',
                        marginBottom: '24px',
                        border: '1px solid #9ae6b4',
                        textAlign: 'center',
                        fontWeight: '600'
                    }}>
                        ✅ {success}
                    </div>
                )}

                {/* Interfața principală bazată pe statusul workout-ului */}
                {workoutStatus === 'idle' && (
                    <div style={{
                        backgroundColor: 'white',
                        borderRadius: '24px',
                        padding: '48px',
                        textAlign: 'center',
                        boxShadow: '0 20px 60px rgba(0, 0, 0, 0.1)',
                        marginBottom: '32px'
                    }}>
                        <div style={{
                            fontSize: '64px',
                            marginBottom: '24px'
                        }}>💪</div>

                        <h2 style={{
                            color: '#2d3748',
                            fontSize: '32px',
                            fontWeight: '700',
                            marginBottom: '16px'
                        }}>
                            Gata să începi un workout?
                        </h2>

                        <p style={{
                            color: '#718096',
                            fontSize: '18px',
                            marginBottom: '32px',
                            maxWidth: '600px',
                            margin: '0 auto 32px auto'
                        }}>
                            Începe un workout liber și adaugă exercițiile pe măsură ce le faci.
                            Monitorizează progresul în timp real!
                        </p>

                        <button
                            onClick={handleStartFreeWorkout}
                            disabled={loading}
                            style={{
                                background: loading ? '#cbd5e0' : 'linear-gradient(135deg, #48bb78, #38a169)',
                                color: 'white',
                                border: 'none',
                                padding: '20px 40px',
                                borderRadius: '16px',
                                cursor: loading ? 'not-allowed' : 'pointer',
                                fontSize: '18px',
                                fontWeight: '700',
                                boxShadow: '0 10px 30px rgba(72, 187, 120, 0.3)',
                                transition: 'all 0.3s ease',
                                transform: loading ? 'none' : 'translateY(0)',
                            }}
                            onMouseEnter={(e) => {
                                if (!loading) {
                                    e.target.style.transform = 'translateY(-2px)';
                                    e.target.style.boxShadow = '0 15px 40px rgba(72, 187, 120, 0.4)';
                                }
                            }}
                            onMouseLeave={(e) => {
                                if (!loading) {
                                    e.target.style.transform = 'translateY(0)';
                                    e.target.style.boxShadow = '0 10px 30px rgba(72, 187, 120, 0.3)';
                                }
                            }}
                        >
                            {loading ? '🔄 Se inițializează...' : '🚀 Începe Workout-ul'}
                        </button>
                    </div>
                )}

                {/* Workout în progres */}
                {workoutStatus === 'in_progress' && currentWorkout && (
                    <div style={{ display: 'grid', gap: '24px' }}>
                        {/* Header workout activ */}
                        <div style={{
                            backgroundColor: 'white',
                            borderRadius: '20px',
                            padding: '32px',
                            boxShadow: '0 10px 40px rgba(0, 0, 0, 0.1)'
                        }}>
                            <div style={{
                                display: 'flex',
                                justifyContent: 'space-between',
                                alignItems: 'center',
                                marginBottom: '24px'
                            }}>
                                <div>
                                    <h2 style={{
                                        color: '#2d3748',
                                        fontSize: '28px',
                                        fontWeight: '700',
                                        marginBottom: '8px'
                                    }}>
                                        🔥 Workout în progres
                                    </h2>
                                    <p style={{
                                        color: '#718096',
                                        fontSize: '16px'
                                    }}>
                                        Început la: {new Date(currentWorkout.startTime).toLocaleTimeString('ro-RO')}
                                    </p>
                                </div>

                                <div style={{ display: 'flex', gap: '12px' }}>
                                    <button
                                        onClick={() => setShowCompletionForm(true)}
                                        style={{
                                            background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                            color: 'white',
                                            border: 'none',
                                            padding: '12px 24px',
                                            borderRadius: '12px',
                                            cursor: 'pointer',
                                            fontSize: '14px',
                                            fontWeight: '600'
                                        }}
                                    >
                                        ✅ Finalizează
                                    </button>

                                    <button
                                        onClick={handleCancelWorkout}
                                        style={{
                                            background: '#fed7d7',
                                            color: '#c53030',
                                            border: 'none',
                                            padding: '12px 24px',
                                            borderRadius: '12px',
                                            cursor: 'pointer',
                                            fontSize: '14px',
                                            fontWeight: '600'
                                        }}
                                    >
                                        ❌ Anulează
                                    </button>
                                </div>
                            </div>

                            {/* Statistici rapide */}
                            <div style={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
                                gap: '16px',
                                padding: '20px',
                                backgroundColor: '#f7fafc',
                                borderRadius: '12px'
                            }}>
                                <div style={{ textAlign: 'center' }}>
                                    <div style={{
                                        fontSize: '24px',
                                        fontWeight: '700',
                                        color: '#2d3748',
                                        marginBottom: '4px'
                                    }}>
                                        {exercises.length}
                                    </div>
                                    <div style={{
                                        fontSize: '12px',
                                        color: '#718096',
                                        fontWeight: '600'
                                    }}>
                                        Exerciții
                                    </div>
                                </div>

                                <div style={{ textAlign: 'center' }}>
                                    <div style={{
                                        fontSize: '24px',
                                        fontWeight: '700',
                                        color: '#2d3748',
                                        marginBottom: '4px'
                                    }}>
                                        {exercises.reduce((total, ex) => total + (ex.setsCompleted || 0), 0)}
                                    </div>
                                    <div style={{
                                        fontSize: '12px',
                                        color: '#718096',
                                        fontWeight: '600'
                                    }}>
                                        Seturi totale
                                    </div>
                                </div>

                                <div style={{ textAlign: 'center' }}>
                                    <div style={{
                                        fontSize: '24px',
                                        fontWeight: '700',
                                        color: '#2d3748',
                                        marginBottom: '4px'
                                    }}>
                                        {exercises.reduce((total, ex) => total + (ex.caloriesBurned || 0), 0)}
                                    </div>
                                    <div style={{
                                        fontSize: '12px',
                                        color: '#718096',
                                        fontWeight: '600'
                                    }}>
                                        Calorii estimate
                                    </div>
                                </div>

                                <div style={{ textAlign: 'center' }}>
                                    <div style={{
                                        fontSize: '24px',
                                        fontWeight: '700',
                                        color: '#2d3748',
                                        marginBottom: '4px'
                                    }}>
                                        {Math.floor((new Date() - new Date(currentWorkout.startTime)) / (1000 * 60))}
                                    </div>
                                    <div style={{
                                        fontSize: '12px',
                                        color: '#718096',
                                        fontWeight: '600'
                                    }}>
                                        Minute
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Lista exercițiilor */}
                        <div style={{
                            backgroundColor: 'white',
                            borderRadius: '20px',
                            padding: '32px',
                            boxShadow: '0 10px 40px rgba(0, 0, 0, 0.1)'
                        }}>
                            <div style={{
                                display: 'flex',
                                justifyContent: 'space-between',
                                alignItems: 'center',
                                marginBottom: '24px'
                            }}>
                                <h3 style={{
                                    color: '#2d3748',
                                    fontSize: '24px',
                                    fontWeight: '700',
                                    margin: 0
                                }}>
                                    Exercițiile tale ({exercises.length})
                                </h3>

                                <button
                                    onClick={() => setShowExerciseForm(true)}
                                    style={{
                                        background: 'linear-gradient(135deg, #48bb78, #38a169)',
                                        color: 'white',
                                        border: 'none',
                                        padding: '12px 24px',
                                        borderRadius: '12px',
                                        cursor: 'pointer',
                                        fontSize: '14px',
                                        fontWeight: '600',
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '8px'
                                    }}
                                >
                                    ➕ Adaugă Exercițiu
                                </button>
                            </div>

                            {exercises.length === 0 ? (
                                <div style={{
                                    textAlign: 'center',
                                    padding: '48px 20px',
                                    color: '#718096'
                                }}>
                                    <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎯</div>
                                    <p style={{ fontSize: '18px', fontWeight: '600' }}>
                                        Încă nu ai adăugat niciun exercițiu
                                    </p>
                                    <p style={{ fontSize: '14px' }}>
                                        Apasă pe "Adaugă Exercițiu" pentru a începe să îți înregistrezi antrenamentul
                                    </p>
                                </div>
                            ) : (
                                <div style={{ display: 'grid', gap: '16px' }}>
                                    {exercises.map((exercise, index) => {
                                        const exerciseInfo = availableExercises.find(ex => ex.exercise_id === exercise.exercise?.exerciseId);
                                        return (
                                            <div key={exercise.logId || index} style={{
                                                backgroundColor: '#f7fafc',
                                                padding: '20px',
                                                borderRadius: '12px',
                                                border: '1px solid #e2e8f0'
                                            }}>
                                                <div style={{
                                                    display: 'flex',
                                                    justifyContent: 'space-between',
                                                    alignItems: 'flex-start',
                                                    marginBottom: '12px'
                                                }}>
                                                    <div style={{ flex: 1 }}>
                                                        <h4 style={{
                                                            color: '#2d3748',
                                                            fontSize: '18px',
                                                            fontWeight: '700',
                                                            marginBottom: '8px',
                                                            margin: 0
                                                        }}>
                                                            {exerciseInfo?.exercise_name || 'Exercițiu necunoscut'}
                                                        </h4>

                                                        <div style={{
                                                            display: 'flex',
                                                            flexWrap: 'wrap',
                                                            gap: '16px',
                                                            color: '#4a5568',
                                                            fontSize: '14px',
                                                            fontWeight: '600'
                                                        }}>
                                                            {exercise.setsCompleted && (
                                                                <span>📊 {exercise.setsCompleted} seturi</span>
                                                            )}
                                                            {exercise.repsCompleted && (
                                                                <span>🔄 {exercise.repsCompleted} repetări</span>
                                                            )}
                                                            {exercise.weightUsedKg && (
                                                                <span>⚖️ {exercise.weightUsedKg}kg</span>
                                                            )}
                                                            {exercise.durationSeconds && (
                                                                <span>⏱️ {Math.floor(exercise.durationSeconds / 60)}:{(exercise.durationSeconds % 60).toString().padStart(2, '0')}</span>
                                                            )}
                                                            {exercise.caloriesBurned && (
                                                                <span>🔥 {exercise.caloriesBurned} cal</span>
                                                            )}
                                                            {exercise.difficultyRating && (
                                                                <span>⭐ {exercise.difficultyRating}/5</span>
                                                            )}
                                                        </div>

                                                        {exercise.notes && (
                                                            <p style={{
                                                                color: '#718096',
                                                                fontSize: '14px',
                                                                marginTop: '8px',
                                                                fontStyle: 'italic',
                                                                margin: '8px 0 0 0'
                                                            }}>
                                                                💭 {exercise.notes}
                                                            </p>
                                                        )}
                                                    </div>

                                                    <button
                                                        onClick={() => handleRemoveExercise(exercise.logId)}
                                                        style={{
                                                            background: 'none',
                                                            border: 'none',
                                                            color: '#e53e3e',
                                                            cursor: 'pointer',
                                                            fontSize: '16px',
                                                            padding: '8px',
                                                            borderRadius: '6px',
                                                            marginLeft: '12px'
                                                        }}
                                                        title="Șterge exercițiul"
                                                    >
                                                        🗑️
                                                    </button>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {/* Workout finalizat */}
                {workoutStatus === 'completed' && (
                    <div style={{
                        backgroundColor: 'white',
                        borderRadius: '24px',
                        padding: '48px',
                        textAlign: 'center',
                        boxShadow: '0 20px 60px rgba(0, 0, 0, 0.1)'
                    }}>
                        <div style={{
                            fontSize: '64px',
                            marginBottom: '24px'
                        }}>🎉</div>

                        <h2 style={{
                            color: '#2d3748',
                            fontSize: '32px',
                            fontWeight: '700',
                            marginBottom: '16px'
                        }}>
                            Felicitări! Workout finalizat!
                        </h2>

                        <p style={{
                            color: '#718096',
                            fontSize: '18px',
                            marginBottom: '32px'
                        }}>
                            Ai completat cu succes workout-ul tău. Continui să îți atingi obiectivele!
                        </p>

                        <div style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
                            gap: '20px',
                            padding: '24px',
                            backgroundColor: '#f7fafc',
                            borderRadius: '16px',
                            marginBottom: '32px'
                        }}>
                            <div style={{ textAlign: 'center' }}>
                                <div style={{
                                    fontSize: '32px',
                                    fontWeight: '700',
                                    color: '#48bb78',
                                    marginBottom: '8px'
                                }}>
                                    {exercises.length}
                                </div>
                                <div style={{
                                    fontSize: '14px',
                                    color: '#718096',
                                    fontWeight: '600'
                                }}>
                                    Exerciții completate
                                </div>
                            </div>

                            <div style={{ textAlign: 'center' }}>
                                <div style={{
                                    fontSize: '32px',
                                    fontWeight: '700',
                                    color: '#667eea',
                                    marginBottom: '8px'
                                }}>
                                    {exercises.reduce((total, ex) => total + (ex.setsCompleted || 0), 0)}
                                </div>
                                <div style={{
                                    fontSize: '14px',
                                    color: '#718096',
                                    fontWeight: '600'
                                }}>
                                    Seturi totale
                                </div>
                            </div>

                            <div style={{ textAlign: 'center' }}>
                                <div style={{
                                    fontSize: '32px',
                                    fontWeight: '700',
                                    color: '#ed8936',
                                    marginBottom: '8px'
                                }}>
                                    {exercises.reduce((total, ex) => total + (ex.caloriesBurned || 0), 0)}
                                </div>
                                <div style={{
                                    fontSize: '14px',
                                    color: '#718096',
                                    fontWeight: '600'
                                }}>
                                    Calorii arse
                                </div>
                            </div>
                        </div>

                        <button
                            onClick={handleStartNewWorkout}
                            style={{
                                background: 'linear-gradient(135deg, #48bb78, #38a169)',
                                color: 'white',
                                border: 'none',
                                padding: '20px 40px',
                                borderRadius: '16px',
                                cursor: 'pointer',
                                fontSize: '18px',
                                fontWeight: '700',
                                boxShadow: '0 10px 30px rgba(72, 187, 120, 0.3)',
                                transition: 'all 0.3s ease'
                            }}
                        >
                            🔄 Începe un nou workout
                        </button>
                    </div>
                )}

                {/* Formularele (modals) */}
                {showExerciseForm && (
                    <ExerciseForm
                        onSubmit={handleAddExercise}
                        onCancel={() => setShowExerciseForm(false)}
                    />
                )}

                {showCompletionForm && (
                    <CompletionForm
                        onSubmit={handleCompleteWorkout}
                        onCancel={() => setShowCompletionForm(false)}
                    />
                )}
            </div>
        </div>
    );
};

// ============= EXPORT =============
export default TrackWorkouts;