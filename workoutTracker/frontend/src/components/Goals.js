import React, { useState, useEffect } from 'react';
import WorkoutRecommendations from './WorkoutRecommendation';

const GoalsApi = {
    createGoal: async (goalData) => {
        try {
            const response = await fetch('http://localhost:8082/api/goals', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(goalData)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to create goal');
            }

            return await response.json();
        } catch (error) {
            console.error('Error creating goal:', error);
            throw error;
        }
    },

    getUserGoals: async (userId) => {
        try {
            const response = await fetch(`http://localhost:8082/api/goals/user/${userId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to fetch goals');
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching goals:', error);
            throw error;
        }
    },

    updateGoalStatus: async (goalId, status) => {
        try {
            const response = await fetch(`http://localhost:8082/api/goals/${goalId}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ status })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to update goal status');
            }

            return await response.json();
        } catch (error) {
            console.error('Error updating goal status:', error);
            throw error;
        }
    },

    deleteGoal: async (goalId) => {
        try {
            const response = await fetch(`http://localhost:8082/api/goals/${goalId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to delete goal');
            }

            return await response.json();
        } catch (error) {
            console.error('Error deleting goal:', error);
            throw error;
        }
    }
};

const Goals = ({ user, onBack, onGoalSet }) => {
    const [currentStep, setCurrentStep] = useState(1); // 1: select goal, 2: goal details, 3: goals list
    const [selectedGoal, setSelectedGoal] = useState('');
    const [goalDetails, setGoalDetails] = useState({
        targetWeightLoss: '',
        targetWeightGain: '',
        timeframe: 3, // months
        currentWeight: user?.weightKg || ''
    });
    const [userGoals, setUserGoals] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [showGoalsList, setShowGoalsList] = useState(false);
    const [success, setSuccess] = useState('');

    const goals = [
        {
            id: 'lose_weight',
            title: 'Lose Weight',
            description: 'Burn fat and achieve your ideal weight',
            icon: '⚖️',
            color: 'linear-gradient(135deg, #f093fb, #f5576c)'
        },
        {
            id: 'gain_muscle',
            title: 'Gain Muscle',
            description: 'Build strength and muscle mass',
            icon: '💪',
            color: 'linear-gradient(135deg, #4facfe, #00f2fe)'
        },
        {
            id: 'maintain_health',
            title: 'Maintain Healthy Life',
            description: 'Stay fit and maintain overall wellness',
            icon: '🌱',
            color: 'linear-gradient(135deg, #43e97b, #38f9d7)'
        }
    ];

    // Load user goals when component mounts
    useEffect(() => {
        if (user?.id) {
            fetchUserGoals();
        }
    }, [user?.id]);
    useEffect(() => {
        return () => {
            // Cleanup function - reset state when component unmounts
            setCurrentStep(1);
            setShowGoalsList(false);
            setSelectedGoal('');
            setError('');
            setSuccess('');
        };
    }, []);
    const fetchUserGoals = async () => {
        try {
            setLoading(true);
            setError('');
            setSuccess('');

            const response = await GoalsApi.getUserGoals(user.id);
            setUserGoals(response.goals || []);

            if (response.goals && response.goals.length > 0) {
                setSuccess(`Loaded ${response.goals.length} goals successfully!`);
                setTimeout(() => setSuccess(''), 3000);
            }
        } catch (err) {
            setError('Failed to load your goals. Please make sure the backend is running.');
            console.error('Error fetching goals:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleGoalSelect = (goalId) => {
        setSelectedGoal(goalId);
        setCurrentStep(2);
        setError('');
        setSuccess('');
    };

    const handleBackToGoals = () => {
        setCurrentStep(1);
        setSelectedGoal('');
        setError('');
        setSuccess('');
    };
    const handleBackToDashboard = () => {
        // Reset toate state-urile la valorile inițiale
        setCurrentStep(1);
        setShowGoalsList(false);
        setSelectedGoal('');
        setError('');
        setSuccess('');
        // Apoi cheamă callback-ul pentru a merge înapoi la dashboard
        onBack();
    };

    const calculateCalorieDeficit = (currentWeight, targetLoss, timeframeMonths) => {
        const targetLossKg = parseFloat(targetLoss);
        const timeframeWeeks = timeframeMonths * 4.33;
        const totalCaloriesNeeded = targetLossKg * 7700;
        const dailyCalorieDeficit = Math.round(totalCaloriesNeeded / (timeframeWeeks * 7));
        return {
            totalCalories: totalCaloriesNeeded,
            dailyDeficit: dailyCalorieDeficit,
            weeklyWeightLoss: Math.round((targetLossKg / timeframeWeeks) * 100) / 100
        };
    };

    const calculateCalorieSurplus = (targetGain, timeframeMonths) => {
        const targetGainKg = parseFloat(targetGain);
        const timeframeWeeks = timeframeMonths * 4.33;
        const totalCaloriesNeeded = targetGainKg * 5500;
        const dailyCalorieSurplus = Math.round(totalCaloriesNeeded / (timeframeWeeks * 7));
        return {
            totalCalories: totalCaloriesNeeded,
            dailySurplus: dailyCalorieSurplus,
            weeklyWeightGain: Math.round((targetGainKg / timeframeWeeks) * 100) / 100
        };
    };

    const handleSaveGoal = async () => {
        try {
            setLoading(true);
            setError('');
            setSuccess('');

            // Validate required fields
            if (!goalDetails.currentWeight) {
                setError('Current weight is required');
                return;
            }

            if (selectedGoal === 'lose_weight' && !goalDetails.targetWeightLoss) {
                setError('Target weight loss is required');
                return;
            }

            if (selectedGoal === 'gain_muscle' && !goalDetails.targetWeightGain) {
                setError('Target weight gain is required');
                return;
            }

            // Prepare goal data for API (matching backend DTO)
            const goalData = {
                userId: user.id,
                goalType: selectedGoal,
                targetWeightLoss: selectedGoal === 'lose_weight' ? parseFloat(goalDetails.targetWeightLoss) : null,
                targetWeightGain: selectedGoal === 'gain_muscle' ? parseFloat(goalDetails.targetWeightGain) : null,
                currentWeight: parseFloat(goalDetails.currentWeight),
                timeframe: parseInt(goalDetails.timeframe), // Backend expects 'timeframe' not 'timeframeMonths'
                notes: null
            };

            console.log('Sending goal data:', goalData);

            const response = await GoalsApi.createGoal(goalData);
            console.log('Goal created successfully:', response);
            console.log('Backend calculated values:', {
                dailyCalorieDeficit: response.dailyCalorieDeficit,
                dailyCalorieSurplus: response.dailyCalorieSurplus,
                weeklyWeightChange: response.weeklyWeightChange,
                targetWeight: response.targetWeight
            });

            // Refresh goals list
            await fetchUserGoals();

            // Call parent callback if provided
            if (onGoalSet) {
                onGoalSet(response);
            }

            // Show success message with backend calculations
            let calculationsMessage = '';
            if (response.dailyCalorieDeficit) {
                calculationsMessage = ` You need a ${response.dailyCalorieDeficit} calorie deficit daily.`;
            } else if (response.dailyCalorieSurplus) {
                calculationsMessage = ` You need a ${response.dailyCalorieSurplus} calorie surplus daily.`;
            }

            setSuccess(`Goal set successfully! Your personalized plan is ready.${calculationsMessage}`);
            setShowGoalsList(true);
            setCurrentStep(3);

            // Reset form
            setGoalDetails({
                targetWeightLoss: '',
                targetWeightGain: '',
                timeframe: 3,
                currentWeight: user?.weightKg || ''
            });
            setSelectedGoal('');

        } catch (err) {
            setError(err.message || 'Failed to save goal. Please check if the backend is running.');
            console.error('Error saving goal:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleUpdateGoalStatus = async (goalId, newStatus) => {
        try {
            setLoading(true);
            setError('');

            await GoalsApi.updateGoalStatus(goalId, newStatus);

            // Refresh goals list
            await fetchUserGoals();

            setSuccess(`Goal status updated to ${newStatus.toLowerCase()}`);
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setError(err.message || 'Failed to update goal status');
            console.error('Error updating goal status:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteGoal = async (goalId) => {
        if (!window.confirm('Are you sure you want to delete this goal?')) {
            return;
        }

        try {
            setLoading(true);
            setError('');

            await GoalsApi.deleteGoal(goalId);

            // Refresh goals list
            await fetchUserGoals();

            setSuccess('Goal deleted successfully');
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setError(err.message || 'Failed to delete goal');
            console.error('Error deleting goal:', err);
        } finally {
            setLoading(false);
        }
    };

    const formatGoalType = (goalType) => {
        const goalMap = {
            'lose_weight': 'Lose Weight',
            'gain_muscle': 'Gain Muscle',
            'maintain_health': 'Maintain Health'
        };
        return goalMap[goalType] || goalType;
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    };
    const [showWorkoutRecommendations, setShowWorkoutRecommendations] = useState(false);
    const [selectedGoalForWorkout, setSelectedGoalForWorkout] = useState(null);
    // Funcție pentru a deschide recomandările de workout
    const handleRecommendWorkout = (goal) => {
        setSelectedGoalForWorkout(goal);
        setShowWorkoutRecommendations(true);
    };

    // Funcție pentru a reveni din recomandări la lista de goals
    const handleBackFromRecommendations = () => {
        setShowWorkoutRecommendations(false);
        setSelectedGoalForWorkout(null);
    };

    // Funcție pentru salvarea planului de workout
    const handleSaveWorkoutPlan = (savedPlan) => {
        console.log('Workout plan saved:', savedPlan);
    };

    const renderGoalDetails = () => {
        const selectedGoalData = goals.find(g => g.id === selectedGoal);

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
                    maxWidth: '600px',
                    border: '1px solid rgba(255,255,255,0.2)',
                    position: 'relative'
                }}>
                    {/* Back button */}
                    <button
                        onClick={handleBackToGoals}
                        style={{
                            position: 'absolute',
                            top: '24px',
                            left: '24px',
                            background: 'rgba(102, 126, 234, 0.1)',
                            border: '1px solid rgba(102, 126, 234, 0.2)',
                            borderRadius: '12px',
                            padding: '12px 16px',
                            cursor: 'pointer',
                            fontSize: '14px',
                            fontWeight: '600',
                            color: '#667eea',
                            transition: 'all 0.2s'
                        }}
                    >
                        ← Back
                    </button>

                    {/* Header */}
                    <div style={{ textAlign: 'center', marginBottom: '40px', marginTop: '40px' }}>
                        <div style={{
                            width: '80px',
                            height: '80px',
                            background: selectedGoalData?.color,
                            borderRadius: '20px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            margin: '0 auto 20px auto',
                            boxShadow: '0 8px 32px rgba(0,0,0,0.15)'
                        }}>
                            <span style={{ fontSize: '32px' }}>{selectedGoalData?.icon}</span>
                        </div>
                        <h1 style={{
                            color: '#1a202c',
                            fontSize: '28px',
                            fontWeight: '800',
                            marginBottom: '8px',
                            letterSpacing: '-0.5px'
                        }}>
                            {selectedGoalData?.title}
                        </h1>
                        <p style={{
                            color: '#718096',
                            fontSize: '16px',
                            margin: '0'
                        }}>
                            Let's personalize your goal
                        </p>
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
                            fontWeight: '500'
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
                            fontWeight: '500'
                        }}>
                            {error}
                        </div>
                    )}

                    {/* Form */}
                    <div style={{ marginBottom: '32px' }}>
                        {/* Current Weight */}
                        <div style={{ marginBottom: '24px' }}>
                            <label style={{
                                display: 'block',
                                color: '#1a202c',
                                fontSize: '16px',
                                fontWeight: '600',
                                marginBottom: '8px'
                            }}>
                                Current Weight (kg) *
                            </label>
                            <input
                                type="number"
                                step="0.1"
                                min="0"
                                value={goalDetails.currentWeight}
                                onChange={(e) => setGoalDetails({...goalDetails, currentWeight: e.target.value})}
                                style={{
                                    width: '100%',
                                    padding: '16px',
                                    border: '2px solid rgba(102, 126, 234, 0.1)',
                                    borderRadius: '12px',
                                    fontSize: '16px',
                                    fontWeight: '500',
                                    background: 'rgba(255,255,255,0.8)',
                                    transition: 'all 0.2s',
                                    outline: 'none',
                                    boxSizing: 'border-box'
                                }}
                                placeholder="Enter your current weight"
                            />
                        </div>

                        {/* Target Weight Loss */}
                        {selectedGoal === 'lose_weight' && (
                            <div style={{ marginBottom: '24px' }}>
                                <label style={{
                                    display: 'block',
                                    color: '#1a202c',
                                    fontSize: '16px',
                                    fontWeight: '600',
                                    marginBottom: '8px'
                                }}>
                                    Target Weight Loss (kg) *
                                </label>
                                <input
                                    type="number"
                                    step="0.1"
                                    min="0"
                                    value={goalDetails.targetWeightLoss}
                                    onChange={(e) => setGoalDetails({...goalDetails, targetWeightLoss: e.target.value})}
                                    style={{
                                        width: '100%',
                                        padding: '16px',
                                        border: '2px solid rgba(102, 126, 234, 0.1)',
                                        borderRadius: '12px',
                                        fontSize: '16px',
                                        fontWeight: '500',
                                        background: 'rgba(255,255,255,0.8)',
                                        transition: 'all 0.2s',
                                        outline: 'none',
                                        boxSizing: 'border-box'
                                    }}
                                    placeholder="How much weight do you want to lose?"
                                />
                            </div>
                        )}

                        {/* Target Weight Gain */}
                        {selectedGoal === 'gain_muscle' && (
                            <div style={{ marginBottom: '24px' }}>
                                <label style={{
                                    display: 'block',
                                    color: '#1a202c',
                                    fontSize: '16px',
                                    fontWeight: '600',
                                    marginBottom: '8px'
                                }}>
                                    Target Muscle Gain (kg) *
                                </label>
                                <input
                                    type="number"
                                    step="0.1"
                                    min="0"
                                    value={goalDetails.targetWeightGain}
                                    onChange={(e) => setGoalDetails({...goalDetails, targetWeightGain: e.target.value})}
                                    style={{
                                        width: '100%',
                                        padding: '16px',
                                        border: '2px solid rgba(102, 126, 234, 0.1)',
                                        borderRadius: '12px',
                                        fontSize: '16px',
                                        fontWeight: '500',
                                        background: 'rgba(255,255,255,0.8)',
                                        transition: 'all 0.2s',
                                        outline: 'none',
                                        boxSizing: 'border-box'
                                    }}
                                    placeholder="How much muscle do you want to gain?"
                                />
                            </div>
                        )}

                        {/* Timeframe */}
                        <div style={{ marginBottom: '24px' }}>
                            <label style={{
                                display: 'block',
                                color: '#1a202c',
                                fontSize: '16px',
                                fontWeight: '600',
                                marginBottom: '8px'
                            }}>
                                Timeframe (months)
                            </label>
                            <select
                                value={goalDetails.timeframe}
                                onChange={(e) => setGoalDetails({...goalDetails, timeframe: parseInt(e.target.value)})}
                                style={{
                                    width: '100%',
                                    padding: '16px',
                                    border: '2px solid rgba(102, 126, 234, 0.1)',
                                    borderRadius: '12px',
                                    fontSize: '16px',
                                    fontWeight: '500',
                                    background: 'rgba(255,255,255,0.8)',
                                    transition: 'all 0.2s',
                                    outline: 'none',
                                    boxSizing: 'border-box'
                                }}
                            >
                                <option value={1}>1 month</option>
                                <option value={2}>2 months</option>
                                <option value={3}>3 months</option>
                                <option value={6}>6 months</option>
                                <option value={12}>1 year</option>
                            </select>
                        </div>

                        {/* Goal Summary - Will be populated after saving */}
                        {((selectedGoal === 'lose_weight' && goalDetails.targetWeightLoss && goalDetails.currentWeight) ||
                            (selectedGoal === 'gain_muscle' && goalDetails.targetWeightGain)) && (
                            <div style={{
                                background: 'rgba(102, 126, 234, 0.1)',
                                border: '1px solid rgba(102, 126, 234, 0.2)',
                                borderRadius: '16px',
                                padding: '20px',
                                marginBottom: '24px'
                            }}>
                                <h4 style={{
                                    color: '#1a202c',
                                    fontSize: '16px',
                                    fontWeight: '700',
                                    marginBottom: '12px'
                                }}>
                                    📋 Goal Preview
                                </h4>
                                <div>
                                    <p style={{ color: '#718096', fontSize: '14px', margin: '4px 0' }}>
                                        <strong>Goal Type:</strong> {goals.find(g => g.id === selectedGoal)?.title}
                                    </p>
                                    <p style={{ color: '#718096', fontSize: '14px', margin: '4px 0' }}>
                                        <strong>Current Weight:</strong> {goalDetails.currentWeight} kg
                                    </p>
                                    {selectedGoal === 'lose_weight' && (
                                        <p style={{ color: '#718096', fontSize: '14px', margin: '4px 0' }}>
                                            <strong>Target Loss:</strong> {goalDetails.targetWeightLoss} kg
                                        </p>
                                    )}
                                    {selectedGoal === 'gain_muscle' && (
                                        <p style={{ color: '#718096', fontSize: '14px', margin: '4px 0' }}>
                                            <strong>Target Gain:</strong> {goalDetails.targetWeightGain} kg
                                        </p>
                                    )}
                                    <p style={{ color: '#718096', fontSize: '14px', margin: '4px 0' }}>
                                        <strong>Timeframe:</strong> {goalDetails.timeframe} months
                                    </p>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Save Button */}
                    <button
                        onClick={handleSaveGoal}
                        disabled={loading || !goalDetails.currentWeight ||
                            (selectedGoal === 'lose_weight' && !goalDetails.targetWeightLoss) ||
                            (selectedGoal === 'gain_muscle' && !goalDetails.targetWeightGain)}
                        style={{
                            width: '100%',
                            background: loading ? '#cbd5e0' : 'linear-gradient(135deg, #667eea, #764ba2)',
                            color: 'white',
                            border: 'none',
                            padding: '18px',
                            borderRadius: '12px',
                            fontSize: '16px',
                            fontWeight: '700',
                            cursor: loading ? 'not-allowed' : 'pointer',
                            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                            boxShadow: loading ? 'none' : '0 8px 32px rgba(102, 126, 234, 0.3)'
                        }}
                    >
                        {loading ? 'Saving Goal...' : 'Save Goal'}
                    </button>
                </div>
            </div>
        );
    };

    const renderGoalsList = () => (
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
                maxWidth: '800px',
                border: '1px solid rgba(255,255,255,0.2)',
                position: 'relative'
            }}>
                <button
                    onClick={handleBackToDashboard}
                    style={{
                        position: 'absolute',
                        top: '24px',
                        left: '24px',
                        background: 'rgba(255, 255, 255, 0.1)',
                        backdropFilter:'blur(10px)',
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
                    ← Back to Dashboard
                </button>

                {/* Add Goal button */}
                <button
                    onClick={() => {
                        setCurrentStep(1);
                        setShowGoalsList(false);
                        setError('');
                        setSuccess('');
                    }}
                    style={{
                        position: 'absolute',
                        top: '24px',
                        right: '24px',
                        background: 'rgba(255, 255, 255, 0.1)',
                        backdropFilter:'blur(10px)',
                        boxShadow: 'rgba(30, 58, 138, 0.2)',
                        border: '2px solid rgba(30, 58, 138, 0.5)',
                        borderRadius: '12px',
                        padding: '12px 20px',
                        cursor: 'pointer',
                        fontSize: '14px',
                        fontWeight: '600',
                        color: 'rgba(30, 58, 138, 0.5)',
                        transition: 'all 0.2s'
                    }}
                >
                    + Add New Goal
                </button>

                {/* Header */}
                <div style={{textAlign: 'center', marginBottom: '48px', marginTop: '40px'}}>
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
                                // Fallback în caz că imaginea nu se poate încărca
                                e.target.style.display = 'none';
                                e.target.nextSibling.style.display = 'flex';
                            }}
                        />
                    </div>
                    <h1 style={{
                        color: '#000000',
                        fontSize: '36px',
                        fontWeight: '800',
                        marginBottom: '16px',
                        letterSpacing: '-0.5px',
                        backgroundClip: 'text',
                    }}>
                        Your Goals
                    </h1>
                    <p style={{
                        color: '#718096',
                        fontSize: '18px',
                        lineHeight: '1.6',
                        fontWeight: '500'
                    }}>
                        Track your fitness journey and achievements
                    </p>
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
                        fontWeight: '500'
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
                        fontWeight: '500'
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
                        <p style={{color: '#718096', fontSize: '16px'}}>Loading your goals...</p>
                    </div>
                )}

                {/* Goals list */}
                {!loading && userGoals.length > 0 && (
                    <div style={{display: 'grid', gap: '24px'}}>
                        {userGoals.map((goal) => (
                            <div
                                key={goal.goalId}
                                style={{
                                    background: 'rgba(255,255,255,0.9)',
                                    border: '2px solid rgba(102, 126, 234, 0.1)',
                                    borderRadius: '20px',
                                    padding: '32px',
                                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
                                }}
                            >
                                <div style={{
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'flex-start',
                                    marginBottom: '20px'
                                }}>
                                    <div>
                                        <h3 style={{
                                            color: '#1a202c',
                                            fontSize: '24px',
                                            fontWeight: '700',
                                            marginBottom: '8px'
                                        }}>
                                            {formatGoalType(goal.goalType)}
                                        </h3>
                                        <p style={{
                                            color: '#718096',
                                            fontSize: '14px',
                                            margin: '0 0 8px 0'
                                        }}>
                                            Created: {formatDate(goal.createdAt)}
                                        </p>
                                    </div>
                                    <div style={{display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap'}}>
                                        <span style={{
                                            background: goal.status === 'ACTIVE' ? '#22c55e' :
                                                goal.status === 'COMPLETED' ? 'linear-gradient(135deg, #667eea, #764ba2)' : '#cbd5e0',
                                            color: 'white',
                                            padding: '6px 12px',
                                            borderRadius: '20px',
                                            fontSize: '12px',
                                            fontWeight: '600',
                                            textTransform: 'capitalize'
                                        }}>
                                            {goal.status?.toLowerCase()}
                                        </span>

                                        {goal.status === 'ACTIVE' && (
                                            <button
                                                onClick={() => handleUpdateGoalStatus(goal.goalId, 'COMPLETED')}
                                                style={{
                                                    background: '#6366f1',
                                                    border: 'none',
                                                    borderRadius: '8px',
                                                    padding: '6px 12px',
                                                    cursor: 'pointer',
                                                    fontSize: '12px',
                                                    fontWeight: '600',
                                                    color: 'white',
                                                    transition: 'all 0.2s'
                                                }}
                                            >
                                                Complete
                                            </button>

                                        )}

                                        <button
                                            onClick={() => handleDeleteGoal(goal.goalId)}
                                            style={{
                                                background: '#ef4444',
                                                border: 'none',
                                                borderRadius: '8px',
                                                padding: '6px 12px',
                                                cursor: 'pointer',
                                                fontSize: '12px',
                                                fontWeight: '600',
                                                color: 'white',
                                                transition: 'all 0.2s'
                                            }}
                                        >
                                            Delete
                                        </button>
                                        {/*  Recommend Workout */}
                                        <button
                                            onClick={() => handleRecommendWorkout(goal)}
                                            style={{
                                                background: '#374151',
                                                border: 'none',
                                                borderRadius: '8px',
                                                padding: '6px 12px',
                                                cursor: 'pointer',
                                                fontSize: '12px',
                                                fontWeight: '600',
                                                color: 'white',
                                                transition: 'all 0.2s'
                                            }}
                                        >
                                            Recommend Workout
                                        </button>
                                    </div>
                                </div>

                                {/* Goal details */}
                                <div style={{
                                    display: 'grid',
                                    gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
                                    gap: '16px',
                                    marginBottom: '20px'
                                }}>
                                    {goal.targetWeightLoss && (
                                        <div style={{
                                            textAlign: 'center',
                                            padding: '16px',
                                            background: 'rgba(240, 147, 251, 0.1)',
                                            borderRadius: '12px'
                                        }}>
                                            <div style={{fontSize: '24px', fontWeight: '700', color: '#1a202c'}}>
                                                {goal.targetWeightLoss} kg
                                            </div>
                                            <div style={{fontSize: '12px', color: '#718096', fontWeight: '500'}}>
                                                Target Loss
                                            </div>
                                        </div>
                                    )}
                                    {goal.targetWeightGain && (
                                        <div style={{
                                            textAlign: 'center',
                                            padding: '16px',
                                            background: 'rgba(79, 172, 254, 0.1)',
                                            borderRadius: '12px'
                                        }}>
                                            <div style={{fontSize: '24px', fontWeight: '700', color: '#1a202c'}}>
                                                {goal.targetWeightGain} kg
                                            </div>
                                            <div style={{fontSize: '12px', color: '#718096', fontWeight: '500'}}>
                                                Target Gain
                                            </div>
                                        </div>
                                    )}
                                    {goal.currentWeight && (
                                        <div style={{
                                            textAlign: 'center',
                                            padding: '16px',
                                            background: 'rgba(102, 126, 234, 0.1)',
                                            borderRadius: '12px'
                                        }}>
                                            <div style={{fontSize: '24px', fontWeight: '700', color: '#1a202c'}}>
                                                {goal.currentWeight} kg
                                            </div>
                                            <div style={{fontSize: '12px', color: '#718096', fontWeight: '500'}}>
                                                Current Weight
                                            </div>
                                        </div>
                                    )}
                                    {goal.timeframeMonths && (
                                        <div style={{
                                            textAlign: 'center',
                                            padding: '16px',
                                            background: 'rgba(67, 233, 123, 0.1)',
                                            borderRadius: '12px'
                                        }}>
                                            <div style={{fontSize: '24px', fontWeight: '700', color: '#1a202c'}}>
                                                {goal.timeframeMonths} months
                                            </div>
                                            <div style={{fontSize: '12px', color: '#718096', fontWeight: '500'}}>
                                                Timeframe
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* Calculated metrics from backend */}
                                {(goal.dailyCalorieDeficit || goal.dailyCalorieSurplus || goal.weeklyWeightChange || goal.targetWeight) && (
                                    <div style={{
                                        padding: '20px',
                                        background: 'rgba(102, 126, 234, 0.05)',
                                        borderRadius: '16px',
                                        border: '1px solid rgba(102, 126, 234, 0.1)'
                                    }}>
                                        <h4 style={{
                                            color: '#1a202c',
                                            fontSize: '16px',
                                            fontWeight: '700',
                                            marginBottom: '16px'
                                        }}>
                                            📊 Calculated Plan
                                        </h4>
                                        <div style={{
                                            display: 'grid',
                                            gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
                                            gap: '16px'
                                        }}>
                                            {goal.dailyCalorieDeficit && (
                                                <div style={{
                                                    textAlign: 'center',
                                                    padding: '12px',
                                                    background: 'rgba(245, 87, 108, 0.1)',
                                                    borderRadius: '12px'
                                                }}>
                                                    <div
                                                        style={{fontSize: '20px', fontWeight: '700', color: '#e53e3e'}}>
                                                        -{goal.dailyCalorieDeficit}
                                                    </div>
                                                    <div
                                                        style={{fontSize: '12px', color: '#718096', fontWeight: '500'}}>
                                                        Daily Deficit (cal)
                                                    </div>
                                                </div>
                                            )}
                                            {goal.dailyCalorieSurplus && (
                                                <div style={{
                                                    textAlign: 'center',
                                                    padding: '12px',
                                                    background: 'rgba(67, 233, 123, 0.1)',
                                                    borderRadius: '12px'
                                                }}>
                                                    <div
                                                        style={{fontSize: '20px', fontWeight: '700', color: '#38a169'}}>
                                                        +{goal.dailyCalorieSurplus}
                                                    </div>
                                                    <div
                                                        style={{fontSize: '12px', color: '#718096', fontWeight: '500'}}>
                                                        Daily Surplus (cal)
                                                    </div>
                                                </div>
                                            )}
                                            {goal.weeklyWeightChange && (
                                                <div style={{
                                                    textAlign: 'center',
                                                    padding: '12px',
                                                    background: 'rgba(102, 126, 234, 0.1)',
                                                    borderRadius: '12px'
                                                }}>
                                                    <div
                                                        style={{fontSize: '20px', fontWeight: '700', color: '#667eea'}}>
                                                        {goal.weeklyWeightChange > 0 ? '+' : ''}{goal.weeklyWeightChange} kg
                                                    </div>
                                                    <div
                                                        style={{fontSize: '12px', color: '#718096', fontWeight: '500'}}>
                                                        Weekly Change
                                                    </div>
                                                </div>
                                            )}
                                            {goal.targetWeight && (
                                                <div style={{
                                                    textAlign: 'center',
                                                    padding: '12px',
                                                    background: 'rgba(240, 147, 251, 0.1)',
                                                    borderRadius: '12px'
                                                }}>
                                                    <div
                                                        style={{fontSize: '20px', fontWeight: '700', color: '#1a202c'}}>
                                                        {goal.targetWeight} kg
                                                    </div>
                                                    <div
                                                        style={{fontSize: '12px', color: '#718096', fontWeight: '500'}}>
                                                        Target Weight
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}

                {/* Empty state */}
                {!loading && userGoals.length === 0 && (
                    <div style={{textAlign: 'center', padding: '60px 20px'}}>
                        <div style={{
                            fontSize: '64px',
                            marginBottom: '24px',
                            opacity: '0.5'
                        }}>
                            🎯
                        </div>
                        <h3 style={{
                            color: '#1a202c',
                            fontSize: '24px',
                            fontWeight: '700',
                            marginBottom: '12px'
                        }}>
                            No Goals Yet
                        </h3>
                        <p style={{
                            color: '#718096',
                            fontSize: '16px',
                            marginBottom: '32px'
                        }}>
                            Start your fitness journey by setting your first goal
                        </p>
                        <button
                            onClick={() => {
                                setCurrentStep(1);
                                setShowGoalsList(false);
                                setError('');
                                setSuccess('');
                            }}
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
                            Set Your First Goal
                        </button>
                    </div>
                )}
            </div>
        </div>
    );

    const renderGoalSelection = () => (
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
                maxWidth: '800px',
                border: '1px solid rgba(255,255,255,0.2)',
                position: 'relative'
            }}>
                {/* Back button */}
                <button
                    onClick={handleBackToDashboard}
                    style={{
                        position: 'absolute',
                        top: '24px',
                        left: '24px',
                        background: 'rgba(255, 255, 255, 0.1)',
                        backdropFilter:'blur(10px)',
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
                    ← Back to Dashboard
                </button>

                {/* View Goals button */}
                {userGoals.length > 0 && (
                    <button
                        onClick={() => {
                            setShowGoalsList(true);
                            setCurrentStep(3);
                        }}
                        style={{
                            position: 'absolute',
                            top: '24px',
                            right: '24px',
                            background: 'rgba(255, 255, 255, 0.1)',
                            backdropFilter:'blur(10px)',
                            boxShadow: 'rgba(30, 58, 138, 0.2)',
                            border: '2px solid rgba(30, 58, 138, 0.5)',
                            borderRadius: '12px',
                            padding: '12px 20px',
                            cursor: 'pointer',
                            fontSize: '14px',
                            fontWeight: '600',
                            color: 'rgba(30, 58, 138, 0.5)',
                            transition: 'all 0.2s'
                        }}
                    >
                        View My Goals ({userGoals.length})
                    </button>
                )}

                {/* Header */}
                <div style={{textAlign: 'center', marginBottom: '48px', marginTop: '40px'}}>
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
                                // Fallback în caz că imaginea nu se poate încărca
                                e.target.style.display = 'none';
                                e.target.nextSibling.style.display = 'flex';
                            }}
                        />
                    </div>
                    <h1 style={{
                        color: '#000000',
                        fontSize: '36px',
                        fontWeight: '800',
                        marginBottom: '16px',
                        letterSpacing: '-0.5px',
                        backgroundClip: 'text',
                    }}>
                        What's Your Main Goal?
                    </h1>
                    <p style={{
                        color: '#718096',
                        fontSize: '18px',
                        lineHeight: '1.6',
                        fontWeight: '500',
                        maxWidth: '600px',
                        margin: '0 auto'
                    }}>
                        Choose your primary fitness objective and we'll create a personalized plan to help you achieve
                        it.
                    </p>
                </div>

                {/* Goal Options */}
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                    gap: '24px',
                    marginBottom: '32px'
                }}>
                    {goals.map((goal) => (
                        <div
                            key={goal.id}
                            onClick={() => handleGoalSelect(goal.id)}
                            style={{
                                background: 'rgba(255,255,255,0.9)',
                                border: '2px solid rgba(102, 126, 234, 0.1)',
                                borderRadius: '20px',
                                padding: '32px 24px',
                                textAlign: 'center',
                                cursor: 'pointer',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                position: 'relative',
                                overflow: 'hidden'
                            }}
                            onMouseEnter={(e) => {
                                e.target.style.transform = 'translateY(-8px)';
                                e.target.style.boxShadow = '0 20px 60px rgba(102, 126, 234, 0.15)';
                                e.target.style.borderColor = 'rgba(102, 126, 234, 0.3)';
                            }}
                            onMouseLeave={(e) => {
                                e.target.style.transform = 'translateY(0)';
                                e.target.style.boxShadow = 'none';
                                e.target.style.borderColor = 'rgba(102, 126, 234, 0.1)';
                            }}
                        >
                            <div style={{
                                width: '80px',
                                height: '80px',
                                background: goal.color,
                                borderRadius: '20px',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                margin: '0 auto 20px auto',
                                boxShadow: '0 8px 32px rgba(0,0,0,0.15)'
                            }}>
                                <span style={{fontSize: '32px'}}>{goal.icon}</span>
                            </div>
                            <h3 style={{
                                color: '#1a202c',
                                fontSize: '22px',
                                fontWeight: '700',
                                marginBottom: '12px',
                                letterSpacing: '-0.25px'
                            }}>
                                {goal.title}
                            </h3>
                            <p style={{
                                color: '#718096',
                                fontSize: '15px',
                                lineHeight: '1.5',
                                margin: '0',
                                fontWeight: '500'
                            }}>
                                {goal.description}
                            </p>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );

    if (showWorkoutRecommendations && selectedGoalForWorkout) {
        return (
            <WorkoutRecommendations
                user={user}
                goal={selectedGoalForWorkout}
                onBack={handleBackFromRecommendations}
                onSavePlan={handleSaveWorkoutPlan}
            />
        );
    }

    if (showGoalsList || currentStep === 3) {
        return renderGoalsList();
    }

    if (currentStep === 1) {
        return renderGoalSelection();
    }

    if (currentStep === 2) {
        return renderGoalDetails();
    }

    return null;
};

export default Goals;