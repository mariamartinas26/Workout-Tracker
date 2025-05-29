import React, { useState } from 'react';

const Goals = ({ user, onBack, onGoalSet }) => {
    const [currentStep, setCurrentStep] = useState(1); // 1: select goal, 2: goal details
    const [selectedGoal, setSelectedGoal] = useState('');
    const [goalDetails, setGoalDetails] = useState({
        targetWeightLoss: '',
        targetWeightGain: '',
        timeframe: '3', // months
        currentWeight: user?.weightKg || ''
    });

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

    const handleGoalSelect = (goalId) => {
        setSelectedGoal(goalId);
        setCurrentStep(2);
    };

    const handleBackToGoals = () => {
        setCurrentStep(1);
        setSelectedGoal('');
    };

    const calculateCalorieDeficit = (currentWeight, targetLoss, timeframeMonths) => {
        const targetLossKg = parseFloat(targetLoss);
        const timeframeWeeks = timeframeMonths * 4.33; // approximate weeks in months

        // 1 kg fat = approximately 7700 calories
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

        // 1 kg muscle gain = approximately 5500-6000 calories (more efficient than fat)
        const totalCaloriesNeeded = targetGainKg * 5500;
        const dailyCalorieSurplus = Math.round(totalCaloriesNeeded / (timeframeWeeks * 7));

        return {
            totalCalories: totalCaloriesNeeded,
            dailySurplus: dailyCalorieSurplus,
            weeklyWeightGain: Math.round((targetGainKg / timeframeWeeks) * 100) / 100
        };
    };

    const handleSaveGoal = () => {
        const goalData = {
            goalType: selectedGoal,
            ...goalDetails,
            createdAt: new Date().toISOString()
        };

        console.log('Saving goal:', goalData);

        // Here you would typically save to backend
        onGoalSet && onGoalSet(goalData);

        alert('Goal set successfully! Your personalized plan is ready.');
    };

    const renderGoalSelection = () => (
        <div style={{
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
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
                    onClick={onBack}
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
                    onMouseEnter={(e) => {
                        e.target.style.backgroundColor = 'rgba(102, 126, 234, 0.15)';
                    }}
                    onMouseLeave={(e) => {
                        e.target.style.backgroundColor = 'rgba(102, 126, 234, 0.1)';
                    }}
                >
                    ← Back to Dashboard
                </button>

                {/* Header */}
                <div style={{ textAlign: 'center', marginBottom: '48px', marginTop: '40px' }}>
                    <div style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        width: '100px',
                        height: '100px',
                        background: 'linear-gradient(135deg, #667eea, #764ba2)',
                        borderRadius: '25px',
                        marginBottom: '24px',
                        boxShadow: '0 12px 40px rgba(102, 126, 234, 0.3)'
                    }}>
                        <span style={{ fontSize: '40px', color: 'white' }}>🎯</span>
                    </div>
                    <h1 style={{
                        color: '#1a202c',
                        fontSize: '36px',
                        fontWeight: '800',
                        marginBottom: '16px',
                        letterSpacing: '-0.5px',
                        background: 'linear-gradient(135deg, #667eea, #764ba2)',
                        backgroundClip: 'text',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent'
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
                        Choose your primary fitness objective and we'll create a personalized plan to help you achieve it.
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
                                <span style={{ fontSize: '32px' }}>{goal.icon}</span>
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

    const renderGoalDetails = () => {
        const selectedGoalData = goals.find(g => g.id === selectedGoal);

        return (
            <div style={{
                minHeight: '100vh',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
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
                            margin: '0',
                            fontWeight: '500'
                        }}>
                            Let's customize your plan
                        </p>
                    </div>

                    {/* Goal-specific form */}
                    <div style={{ marginBottom: '32px' }}>
                        {selectedGoal === 'lose_weight' && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                                <div>
                                    <label style={{
                                        display: 'block',
                                        color: '#2d3748',
                                        fontSize: '14px',
                                        fontWeight: '600',
                                        marginBottom: '8px'
                                    }}>
                                        Current Weight: {user?.weightKg || 'Not set'} kg
                                    </label>
                                    <input
                                        type="number"
                                        value={goalDetails.currentWeight}
                                        onChange={(e) => setGoalDetails(prev => ({ ...prev, currentWeight: e.target.value }))}
                                        placeholder="Enter your current weight"
                                        style={{
                                            width: '100%',
                                            padding: '16px',
                                            border: '2px solid #e2e8f0',
                                            borderRadius: '12px',
                                            fontSize: '16px',
                                            fontWeight: '500',
                                            backgroundColor: '#f8fafc',
                                            boxSizing: 'border-box'
                                        }}
                                    />
                                </div>
                                <div>
                                    <label style={{
                                        display: 'block',
                                        color: '#2d3748',
                                        fontSize: '14px',
                                        fontWeight: '600',
                                        marginBottom: '8px'
                                    }}>
                                        How many kg do you want to lose?
                                    </label>
                                    <input
                                        type="number"
                                        value={goalDetails.targetWeightLoss}
                                        onChange={(e) => setGoalDetails(prev => ({ ...prev, targetWeightLoss: e.target.value }))}
                                        placeholder="e.g. 5"
                                        min="0.5"
                                        max="50"
                                        step="0.5"
                                        style={{
                                            width: '100%',
                                            padding: '16px',
                                            border: '2px solid #e2e8f0',
                                            borderRadius: '12px',
                                            fontSize: '16px',
                                            fontWeight: '500',
                                            backgroundColor: '#f8fafc',
                                            boxSizing: 'border-box'
                                        }}
                                    />
                                </div>
                            </div>
                        )}

                        {selectedGoal === 'gain_muscle' && (
                            <div>
                                <label style={{
                                    display: 'block',
                                    color: '#2d3748',
                                    fontSize: '14px',
                                    fontWeight: '600',
                                    marginBottom: '8px'
                                }}>
                                    How many kg of muscle do you want to gain?
                                </label>
                                <input
                                    type="number"
                                    value={goalDetails.targetWeightGain}
                                    onChange={(e) => setGoalDetails(prev => ({ ...prev, targetWeightGain: e.target.value }))}
                                    placeholder="e.g. 3"
                                    min="0.5"
                                    max="20"
                                    step="0.5"
                                    style={{
                                        width: '100%',
                                        padding: '16px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '12px',
                                        fontSize: '16px',
                                        fontWeight: '500',
                                        backgroundColor: '#f8fafc',
                                        boxSizing: 'border-box'
                                    }}
                                />
                            </div>
                        )}

                        {selectedGoal === 'maintain_health' && (
                            <div style={{
                                background: 'linear-gradient(135deg, rgba(67, 233, 123, 0.1), rgba(56, 249, 215, 0.05))',
                                padding: '24px',
                                borderRadius: '16px',
                                border: '1px solid rgba(67, 233, 123, 0.2)',
                                textAlign: 'center'
                            }}>
                                <h3 style={{
                                    color: '#1a202c',
                                    fontSize: '18px',
                                    fontWeight: '700',
                                    marginBottom: '12px'
                                }}>
                                    Perfect Choice!
                                </h3>
                                <p style={{
                                    color: '#718096',
                                    fontSize: '15px',
                                    lineHeight: '1.5',
                                    margin: '0'
                                }}>
                                    We'll create a balanced routine to help you maintain your current fitness level and overall health.
                                </p>
                            </div>
                        )}

                        {/* Timeframe selector for weight goals */}
                        {(selectedGoal === 'lose_weight' || selectedGoal === 'gain_muscle') && (
                            <div style={{ marginTop: '24px' }}>
                                <label style={{
                                    display: 'block',
                                    color: '#2d3748',
                                    fontSize: '14px',
                                    fontWeight: '600',
                                    marginBottom: '8px'
                                }}>
                                    Timeframe to achieve this goal
                                </label>
                                <select
                                    value={goalDetails.timeframe}
                                    onChange={(e) => setGoalDetails(prev => ({ ...prev, timeframe: e.target.value }))}
                                    style={{
                                        width: '100%',
                                        padding: '16px',
                                        border: '2px solid #e2e8f0',
                                        borderRadius: '12px',
                                        fontSize: '16px',
                                        fontWeight: '500',
                                        backgroundColor: '#f8fafc',
                                        boxSizing: 'border-box'
                                    }}
                                >
                                    <option value="1">1 month</option>
                                    <option value="3">3 months (Recommended)</option>
                                    <option value="6">6 months</option>
                                    <option value="12">12 months</option>
                                </select>
                            </div>
                        )}

                        {/* Calculations display */}
                        {selectedGoal === 'lose_weight' && goalDetails.targetWeightLoss && goalDetails.currentWeight && (
                            <div style={{
                                marginTop: '32px',
                                background: 'linear-gradient(135deg, rgba(240, 147, 251, 0.1), rgba(245, 87, 108, 0.05))',
                                padding: '24px',
                                borderRadius: '16px',
                                border: '1px solid rgba(240, 147, 251, 0.2)'
                            }}>
                                <h3 style={{
                                    color: '#1a202c',
                                    fontSize: '18px',
                                    fontWeight: '700',
                                    marginBottom: '16px'
                                }}>
                                    Your Personalized Plan
                                </h3>
                                {(() => {
                                    const calc = calculateCalorieDeficit(goalDetails.currentWeight, goalDetails.targetWeightLoss, parseInt(goalDetails.timeframe));
                                    const targetWeight = parseFloat(goalDetails.currentWeight) - parseFloat(goalDetails.targetWeightLoss);
                                    return (
                                        <div style={{ display: 'grid', gap: '12px' }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                                <span style={{ color: '#718096', fontWeight: '500' }}>Target Weight:</span>
                                                <span style={{ color: '#1a202c', fontWeight: '700' }}>{targetWeight.toFixed(1)} kg</span>
                                            </div>
                                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                                <span style={{ color: '#718096', fontWeight: '500' }}>Weekly Weight Loss:</span>
                                                <span style={{ color: '#1a202c', fontWeight: '700' }}>{calc.weeklyWeightLoss} kg/week</span>
                                            </div>
                                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                                <span style={{ color: '#718096', fontWeight: '500' }}>Daily Calorie Deficit:</span>
                                                <span style={{ color: '#e53e3e', fontWeight: '700' }}>-{calc.dailyDeficit} calories</span>
                                            </div>
                                        </div>
                                    );
                                })()}
                            </div>
                        )}

                        {selectedGoal === 'gain_muscle' && goalDetails.targetWeightGain && (
                            <div style={{
                                marginTop: '32px',
                                background: 'linear-gradient(135deg, rgba(79, 172, 254, 0.1), rgba(0, 242, 254, 0.05))',
                                padding: '24px',
                                borderRadius: '16px',
                                border: '1px solid rgba(79, 172, 254, 0.2)'
                            }}>
                                <h3 style={{
                                    color: '#1a202c',
                                    fontSize: '18px',
                                    fontWeight: '700',
                                    marginBottom: '16px'
                                }}>
                                    Your Muscle Building Plan
                                </h3>
                                {(() => {
                                    const calc = calculateCalorieSurplus(goalDetails.targetWeightGain, parseInt(goalDetails.timeframe));
                                    return (
                                        <div style={{ display: 'grid', gap: '12px' }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                                <span style={{ color: '#718096', fontWeight: '500' }}>Target Muscle Gain:</span>
                                                <span style={{ color: '#1a202c', fontWeight: '700' }}>{goalDetails.targetWeightGain} kg</span>
                                            </div>
                                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                                <span style={{ color: '#718096', fontWeight: '500' }}>Weekly Gain:</span>
                                                <span style={{ color: '#1a202c', fontWeight: '700' }}>{calc.weeklyWeightGain} kg/week</span>
                                            </div>
                                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                                <span style={{ color: '#718096', fontWeight: '500' }}>Daily Calorie Surplus:</span>
                                                <span style={{ color: '#38a169', fontWeight: '700' }}>+{calc.dailySurplus} calories</span>
                                            </div>
                                        </div>
                                    );
                                })()}
                            </div>
                        )}
                    </div>

                    {/* Save button */}
                    <button
                        onClick={handleSaveGoal}
                        disabled={
                            (selectedGoal === 'lose_weight' && (!goalDetails.targetWeightLoss || !goalDetails.currentWeight)) ||
                            (selectedGoal === 'gain_muscle' && !goalDetails.targetWeightGain)
                        }
                        style={{
                            width: '100%',
                            background:
                                (selectedGoal === 'lose_weight' && (!goalDetails.targetWeightLoss || !goalDetails.currentWeight)) ||
                                (selectedGoal === 'gain_muscle' && !goalDetails.targetWeightGain)
                                    ? '#cbd5e0'
                                    : 'linear-gradient(135deg, #667eea, #764ba2)',
                            color: 'white',
                            border: 'none',
                            padding: '16px',
                            borderRadius: '12px',
                            fontSize: '16px',
                            fontWeight: '700',
                            cursor:
                                (selectedGoal === 'lose_weight' && (!goalDetails.targetWeightLoss || !goalDetails.currentWeight)) ||
                                (selectedGoal === 'gain_muscle' && !goalDetails.targetWeightGain)
                                    ? 'not-allowed'
                                    : 'pointer',
                            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                            boxShadow: '0 8px 32px rgba(102, 126, 234, 0.3)'
                        }}
                    >
                        Set My Goal
                    </button>
                </div>
            </div>
        );
    };

    return currentStep === 1 ? renderGoalSelection() : renderGoalDetails();
};

export default Goals;