import React, { useState, useEffect } from 'react';

const WorkoutRecommendationsApi = {
    getRecommendations: async (userId, goalType, duration = 60, maxExercises = 8) => {
        try {
            const response = await fetch(`http://localhost:8082/api/workouts/recommend`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: userId,
                    goalType: goalType
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to get workout recommendations');
            }

            return await response.json();
        } catch (error) {
            console.error('Error getting recommendations:', error);
            throw error;
        }
    },

    saveWorkoutPlan: async (userId, recommendations, goalId) => {
        try {
            const response = await fetch(`http://localhost:8082/api/workouts/save-recommended-plan`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: userId,
                    goalId: goalId,
                    recommendations: recommendations,
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to save workout plan');
            }

            return await response.json();
        } catch (error) {
            console.error('Error saving workout plan:', error);
            throw error;
        }
    }
};


const WorkoutRecommendations = ({ user, goal, onBack, onSavePlan }) => {
    const [recommendations, setRecommendations] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [workoutDuration, setWorkoutDuration] = useState(60);
    const [maxExercises, setMaxExercises] = useState(8);
    const [saving, setSaving] = useState(false);

    // Mapare goal type pentru backend
    const mapGoalTypeForBackend = (goalType) => {
        const goalMapping = {
            'lose_weight': 'WEIGHT_LOSS',
            'gain_muscle': 'MUSCLE_GAIN',
            'maintain_health': 'MAINTENANCE'
        };
        return goalMapping[goalType] || 'MAINTENANCE';
    };

    // Mapare goal type pentru display
    const mapGoalTypeForDisplay = (goalType) => {
        const goalMapping = {
            'lose_weight': 'Weight Loss',
            'gain_muscle': 'Muscle Gain',
            'maintain_health': 'Healthy Lifestyle'
        };
        return goalMapping[goalType] || goalType;
    };

    // Încarcă recomandările când componenta se montează
    useEffect(() => {
        if (user?.id && goal?.goalType) {
            getRecommendations();
        }
    }, [user?.id, goal?.goalType]);

    const getRecommendations = async () => {
        try {
            setLoading(true);
            setError('');
            setSuccess('');

            const backendGoalType = mapGoalTypeForBackend(goal.goalType);
            console.log('Getting recommendations for:', {
                userId: user.id,
                goalType: backendGoalType,
                originalGoalType: goal.goalType
            });

            const response = await WorkoutRecommendationsApi.getRecommendations(
                user.id,
                backendGoalType,
                workoutDuration,
                maxExercises
            );

            console.log('Recommendations response:', response);
            setRecommendations(response.recommendations || []);

            if (response.recommendations && response.recommendations.length > 0) {
                setSuccess(`Generated ${response.recommendations.length} personalized exercises for your ${mapGoalTypeForDisplay(goal.goalType).toLowerCase()} goal!`);
                setTimeout(() => setSuccess(''), 5000);
            } else {
                setError('No recommendations found. Please make sure you have exercises in your database.');
            }
        } catch (err) {
            setError(err.message || 'Failed to get workout recommendations. Please check if the backend is running.');
            console.error('Error getting recommendations:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleSaveWorkoutPlan = async () => {
        try {
            setSaving(true);
            setError('');

            // Trimite un titlu simplu, curat
            const response = await fetch(`http://localhost:8082/api/workouts/save-recommended-plan`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: user.id,
                    goalId: goal.goalId,
                    recommendations: recommendations,
                    planName: `Recommended Workout for ${mapGoalTypeForDisplay(goal.goalType)}`
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to save workout plan');
            }

            const result = await response.json();
            console.log('Workout plan saved:', result);
            setSuccess('Workout plan saved successfully! You can now find it in your workout plans.');

            if (onSavePlan) {
                onSavePlan(result);
            }

            setTimeout(() => setSuccess(''), 5000);
        } catch (err) {
            setError(err.message || 'Failed to save workout plan');
            console.error('Error saving workout plan:', err);
        } finally {
            setSaving(false);
        }
    };

    const handleRefreshRecommendations = () => {
        getRecommendations();
    };

    const getExerciseIcon = (exerciseName) => {
        const name = exerciseName.toLowerCase();
        if (name.includes('run') || name.includes('cardio')) return '🏃‍♂️';
        if (name.includes('push') || name.includes('press')) return '💪';
        if (name.includes('pull') || name.includes('chin')) return '🤸‍♂️';
        if (name.includes('squat') || name.includes('leg')) return '🦵';
        if (name.includes('plank') || name.includes('abs')) return '🏋️‍♂️';
        if (name.includes('jump') || name.includes('rope')) return '🤾‍♂️';
        if (name.includes('cycle') || name.includes('bike')) return '🚴‍♂️';
        if (name.includes('yoga') || name.includes('stretch')) return '🧘‍♀️';
        if (name.includes('curl') || name.includes('bicep')) return '💪';
        if (name.includes('deadlift')) return '🏋️‍♀️';
        return '💪';
    };

    const getDifficultyColor = (score) => {
        if (score >= 4) return '#e53e3e'; // red for hard
        if (score >= 3) return '#f6ad55'; // orange for medium
        return '#38a169'; // green for easy
    };

    const getDifficultyText = (score) => {
        if (score >= 4) return 'High';
        if (score >= 3) return 'Medium';
        return 'Easy';
    };

    return (
        <div style={{
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, sans-serif"
        }}>
            <div style={{
                backgroundColor: 'rgba(255,255,255,0.95)',
                backdropFilter: 'blur(20px)',
                borderRadius: '24px',
                boxShadow: '0 20px 60px rgba(0,0,0,0.15)',
                padding: '48px',
                width: '100%',
                maxWidth: '1000px',
                border: '1px solid rgba(255,255,255,0.2)',
                position: 'relative',
                maxHeight: '90vh',
                overflowY: 'auto'
            }}>
                {/* Back button */}
                <button
                    onClick={onBack}
                    style={{
                        position: 'absolute',
                        top: '24px',
                        left: '24px',
                        background: 'rgba(255, 255, 255, 0.1)',
                        backdropFilter: 'blur(10px)',
                        boxShadow: 'rgba(30, 58, 138, 0.2)',
                        border: '2px solid rgba(30, 58, 138, 0.5)',
                        borderRadius: '12px',
                        padding: '12px 16px',
                        cursor: 'pointer',
                        fontSize: '14px',
                        fontWeight: '600',
                        color: 'rgba(30, 58, 138, 0.5)',
                        transition: 'all 0.2s'
                    }}
                >
                    ← Back to Goals
                </button>

                {/* Header */}
                <div style={{textAlign: 'center', marginBottom: '40px', marginTop: '40px'}}>
                    <div>
                        <img
                            src="/target.png"
                            alt="Target Logo"
                            style={{
                                width: '48px',
                                height: '48px',
                                objectFit: 'contain'
                            }}
                            onError={(e) => {
                                e.target.style.display = 'none';
                                e.target.nextSibling.style.display = 'flex';
                            }}
                        />
                    </div>
                    <h1 style={{
                        color: '#1a202c',
                        fontSize: '32px',
                        fontWeight: '800',
                        marginBottom: '8px',
                        letterSpacing: '-0.5px'
                    }}>
                        Recommended Workout
                    </h1>
                    <p style={{
                        color: '#718096',
                        fontSize: '18px',
                        margin: '0 0 8px 0'
                    }}>
                        Personalized for your <strong>{mapGoalTypeForDisplay(goal.goalType)}</strong> goal
                    </p>
                </div>

                {/* Workout Settings */}
                <div style={{
                    display: 'flex',
                    gap: '16px',
                    marginBottom: '32px',
                    justifyContent: 'center',
                    flexWrap: 'wrap'
                }}>


                </div>

                {/* Success message */}
                {success && (
                    <div style={{
                        background: 'linear-gradient(135deg, rgba(67, 233, 123, 0.1), rgba(67, 233, 123, 0.05))',
                        border: '1px solid rgba(67, 233, 123, 0.2)',
                        borderRadius: '12px',
                        padding: '16px',
                        marginBottom: '24px',
                        color: '#38a169',
                        fontSize: '14px',
                        fontWeight: '500',
                        textAlign: 'center'
                    }}>
                        {success}
                    </div>
                )}

                {/* Error message */}
                {error && (
                    <div style={{
                        background: 'linear-gradient(135deg, rgba(245, 87, 108, 0.1), rgba(245, 87, 108, 0.05))',
                        border: '1px solid rgba(245, 87, 108, 0.2)',
                        borderRadius: '12px',
                        padding: '16px',
                        marginBottom: '24px',
                        color: '#e53e3e',
                        fontSize: '14px',
                        fontWeight: '500',
                        textAlign: 'center'
                    }}>
                        {error}
                    </div>
                )}

                {/* Loading state */}
                {loading && (
                    <div style={{textAlign: 'center', padding: '40px'}}>
                        <div style={{
                            width: '40px',
                            height: '40px',
                            border: '3px solid rgba(102, 126, 234, 0.2)',
                            borderTop: '3px solid #667eea',
                            borderRadius: '50%',
                            animation: 'spin 1s linear infinite',
                            margin: '0 auto 16px'
                        }}></div>
                        <p style={{color: '#718096', fontSize: '16px'}}>
                            Generating personalized workout recommendations...
                        </p>
                    </div>
                )}

                {/* Recommendations list */}
                {!loading && recommendations.length > 0 && (
                    <>
                        <div style={{
                            display: 'grid',
                            gap: '20px',
                            marginBottom: '32px'
                        }}>
                            {recommendations.map((rec, index) => (
                                <div
                                    key={index}
                                    style={{
                                        background: 'rgba(255,255,255,0.9)',
                                        border: '2px solid rgba(102, 126, 234, 0.1)',
                                        borderRadius: '16px',
                                        padding: '24px',
                                        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '20px'
                                    }}
                                >
                                    {/* Exercise icon and order */}
                                    <div style={{
                                        display: 'flex',
                                        flexDirection: 'column',
                                        alignItems: 'center',
                                        minWidth: '80px'
                                    }}>
                                        <div>
                                            <span style={{fontSize: '20px'}}>
                                                {getExerciseIcon(rec.exerciseName)}
                                            </span>
                                        </div>

                                    </div>

                                    {/* Exercise details */}
                                    <div style={{flex: 1}}>
                                        <h3 style={{
                                            color: '#1a202c',
                                            fontSize: '18px',
                                            fontWeight: '700',
                                            marginBottom: '8px'
                                        }}>
                                            {rec.exerciseName}
                                        </h3>

                                        <div style={{
                                            display: 'grid',
                                            gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
                                            gap: '12px',
                                            marginBottom: '12px'
                                        }}>
                                            <div style={{
                                                background: 'rgba(79, 172, 254, 0.1)',
                                                padding: '8px 12px',
                                                borderRadius: '8px',
                                                textAlign: 'center'
                                            }}>
                                                <div style={{fontSize: '16px', fontWeight: '700', color: '#1a202c'}}>
                                                    {rec.recommendedSets}
                                                </div>
                                                <div style={{fontSize: '12px', color: '#718096'}}>Sets</div>
                                            </div>

                                            {rec.recommendedRepsMin && rec.recommendedRepsMax && (
                                                <div style={{
                                                    background: 'rgba(67, 233, 123, 0.1)',
                                                    padding: '8px 12px',
                                                    borderRadius: '8px',
                                                    textAlign: 'center'
                                                }}>
                                                    <div
                                                        style={{fontSize: '16px', fontWeight: '700', color: '#1a202c'}}>
                                                        {rec.recommendedRepsMin === rec.recommendedRepsMax
                                                            ? rec.recommendedRepsMin
                                                            : `${rec.recommendedRepsMin}-${rec.recommendedRepsMax}`}
                                                    </div>
                                                    <div style={{fontSize: '12px', color: '#718096'}}>Reps</div>
                                                </div>
                                            )}

                                            {rec.recommendedWeightPercentage && (
                                                <div style={{
                                                    background: 'rgba(240, 147, 251, 0.1)',
                                                    padding: '8px 12px',
                                                    borderRadius: '8px',
                                                    textAlign: 'center'
                                                }}>
                                                    <div
                                                        style={{fontSize: '16px', fontWeight: '700', color: '#1a202c'}}>
                                                        {Math.round(rec.recommendedWeightPercentage)}%
                                                    </div>
                                                    <div style={{fontSize: '12px', color: '#718096'}}>Weight</div>
                                                </div>
                                            )}

                                            <div style={{
                                                background: 'rgba(245, 87, 108, 0.1)',
                                                padding: '8px 12px',
                                                borderRadius: '8px',
                                                textAlign: 'center'
                                            }}>
                                                <div style={{fontSize: '16px', fontWeight: '700', color: '#1a202c'}}>
                                                    {rec.restTimeSeconds}s
                                                </div>
                                                <div style={{fontSize: '12px', color: '#718096'}}>Rest</div>
                                            </div>
                                        </div>

                                        {/* Priority score */}
                                        <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                                            <span style={{fontSize: '12px', color: '#718096'}}>
                                                Difficulty:
                                            </span>
                                            <span style={{
                                                background: getDifficultyColor(rec.priorityScore),
                                                color: 'white',
                                                padding: '2px 8px',
                                                borderRadius: '12px',
                                                fontSize: '11px',
                                                fontWeight: '600'
                                            }}>
                                                {getDifficultyText(rec.priorityScore)}
                                            </span>
                                            <span style={{fontSize: '12px', color: '#a0aec0'}}>
                                                (Score: {parseFloat(rec.priorityScore).toFixed(1)})
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>

                        {/* Save workout plan button */}
                        <div style={{textAlign: 'center'}}>
                            <button
                                onClick={handleSaveWorkoutPlan}
                                disabled={saving}
                                style={{
                                    background: saving ? '#cbd5e0' : 'linear-gradient(135deg, #667eea, #764ba2)',
                                    color: 'white',
                                    border: 'none',
                                    padding: '18px 36px',
                                    borderRadius: '12px',
                                    fontSize: '16px',
                                    fontWeight: '700',
                                    cursor: saving ? 'not-allowed' : 'pointer',
                                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                    boxShadow: saving ? 'none' : '0 8px 32px rgba(102, 126, 234, 0.3)'
                                }}
                            >
                                {saving ? 'Saving Plan...' : 'Save as Workout Plan'}
                            </button>
                        </div>
                    </>
                )}

                {/* Empty state */}
                {!loading && recommendations.length === 0 && !error && (
                    <div style={{textAlign: 'center', padding: '60px 20px'}}>
                        <div style={{fontSize: '64px', marginBottom: '24px', opacity: '0.5'}}>
                            🤔
                        </div>
                        <h3 style={{
                            color: '#1a202c',
                            fontSize: '24px',
                            fontWeight: '700',
                            marginBottom: '12px'
                        }}>
                            No Recommendations Available
                        </h3>
                        <p style={{
                            color: '#718096',
                            fontSize: '16px',
                            marginBottom: '32px'
                        }}>
                            We couldn't generate recommendations for this goal. Try adjusting the settings or check if
                            exercises are available in the database.
                        </p>
                        <button
                            onClick={handleRefreshRecommendations}
                            style={{
                                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                color: 'white',
                                border: 'none',
                                padding: '16px 32px',
                                borderRadius: '12px',
                                fontSize: '16px',
                                fontWeight: '700',
                                cursor: 'pointer',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                boxShadow: '0 8px 32px rgba(102, 126, 234, 0.3)'
                            }}
                        >
                            Try Again
                        </button>
                    </div>
                )}
            </div>

            <style>{`
                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
            `}</style>
        </div>
    );
};

export default WorkoutRecommendations;