import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8082/api';

const getAuthToken = () => {
    const token = localStorage.getItem('workout_tracker_token') ||
        localStorage.getItem('token') ||
        localStorage.getItem('authToken');
    console.log('Getting token:', token ? 'Found' : 'Not found'); // Debug
    return token;
};

// Helper function to create authenticated headers
const getAuthHeaders = () => {
    const authToken = getAuthToken();
    if (!authToken) {
        throw new Error('No authentication token found. Please login again.');
    }

    console.log('Creating headers with token:', authToken.substring(0, 20) + '...'); // Debug

    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authToken}`
    };
};

const DashboardService = {
    getDashboardSummary: async () => {
        try {
            console.log('🔍 Fetching dashboard summary...');

            const response = await fetch(`${API_BASE_URL}/dashboard/summary`, {
                method: 'GET',
                headers: getAuthHeaders()
            });

            console.log('📡 Response status:', response.status);

            if (!response.ok) {
                if (response.status === 401) {
                    throw new Error('Authentication failed. Please login again.');
                }
                throw new Error(`Failed to fetch dashboard summary: ${response.status}`);
            }

            const data = await response.json();
            console.log('Dashboard data received:', data);
            return data;

        } catch (error) {
            console.error('Error fetching dashboard summary:', error);
            throw error;
        }
    },

    getWorkoutCalendar: async (startDate, endDate) => {
        try {
            let url = `${API_BASE_URL}/dashboard/calendar`;
            if (startDate && endDate) {
                url += `?startDate=${startDate}&endDate=${endDate}`;
            }

            const response = await fetch(url, {
                method: 'GET',
                headers: getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch workout calendar: ${response.status}`);
            }

            return await response.json();

        } catch (error) {
            console.error('Error fetching workout calendar:', error);
            throw error;
        }
    },


    getWorkoutTrends: async (period = 'weekly', startDate, endDate) => {
        try {
            let url = `${API_BASE_URL}/dashboard/trends?period=${period}`;
            if (startDate && endDate) {
                url += `&startDate=${startDate}&endDate=${endDate}`;
            }

            const response = await fetch(url, {
                method: 'GET',
                headers: getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch workout trends: ${response.status}`);
            }

            return await response.json();

        } catch (error) {
            console.error('Error fetching workout trends:', error);
            throw error;
        }
    },

    getWorkoutTypeBreakdown: async (startDate, endDate) => {
        try {
            let url = `${API_BASE_URL}/dashboard/workout-types`;
            if (startDate && endDate) {
                url += `?startDate=${startDate}&endDate=${endDate}`;
            }

            const response = await fetch(url, {
                method: 'GET',
                headers: getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch workout type breakdown: ${response.status}`);
            }

            return await response.json();

        } catch (error) {
            console.error('Error fetching workout type breakdown:', error);
            throw error;
        }
    },

    getRecentAchievements: async (daysBack = 30) => {
        try {
            let achievements = [];
            let completedGoals = [];

            try {
                const achievementsResponse = await fetch(`${API_BASE_URL}/dashboard/achievements?daysBack=${daysBack}`, {
                    method: 'GET',
                    headers: getAuthHeaders()
                });

                if (achievementsResponse.ok) {
                    const achievementsData = await achievementsResponse.json();
                    achievements = Array.isArray(achievementsData) ? achievementsData : [];
                }
            } catch (achievementsError) {
                console.warn('Could not fetch regular achievements, continuing with goals only:', achievementsError);
            }

            try {
                const goalsResponse = await fetch(`${API_BASE_URL}/goals/achievements/completed-goals?daysBack=${daysBack}`, {
                    method: 'GET',
                    headers: getAuthHeaders()
                });

                if (goalsResponse.ok) {
                    const goalsData = await goalsResponse.json();
                    completedGoals = goalsData.completedGoals || [];
                }
            } catch (goalsError) {
                console.warn('Could not fetch completed goals:', goalsError);

                try {
                    const allGoalsResponse = await fetch(`${API_BASE_URL}/goals/user`, {
                        method: 'GET',
                        headers: getAuthHeaders()
                    });

                    if (allGoalsResponse.ok) {
                        const allGoalsData = await allGoalsResponse.json();
                        const allGoals = allGoalsData.goals || [];

                        const cutoffDate = new Date();
                        cutoffDate.setDate(cutoffDate.getDate() - daysBack);

                        completedGoals = allGoals
                            .filter(goal => goal.status === 'COMPLETED')
                            .filter(goal => goal.completedAt && new Date(goal.completedAt) > cutoffDate)
                            .map(goal => ({
                                id: goal.goalId,
                                type: 'COMPLETED_GOAL',
                                title: this.getGoalAchievementTitle(goal),
                                description: this.getGoalAchievementDescription(goal),
                                achievedAt: goal.completedAt,
                                goalType: goal.goalType,
                                originalGoal: goal,
                                icon: this.getGoalAchievementIcon(goal.goalType),
                                points: this.calculateAchievementPoints(goal)
                            }));
                    }
                } catch (fallbackError) {
                    console.warn('Fallback goal fetch also failed:', fallbackError);
                }
            }

            const combinedAchievements = [
                ...achievements,
                ...completedGoals
            ];

            combinedAchievements.sort((a, b) => {
                const dateA = new Date(a.achievedAt || a.achievedDate || 0);
                const dateB = new Date(b.achievedAt || b.achievedDate || 0);
                return dateB - dateA;
            });

            return combinedAchievements;

        } catch (error) {
            console.error('Error fetching achievements:', error);
            return [];
        }
    },

    getGoalAchievementTitle: (goal) => {
        const goalType = goal.goalType?.toLowerCase() || '';

        if (goalType.includes('lose_weight')) {
            return "🎯 Weight Loss Goal Achieved!";
        } else if (goalType.includes('gain_muscle')) {
            return "💪 Muscle Gain Goal Achieved!";
        } else if (goalType.includes('maintain_health')) {
            return "⚖️ Health Maintenance Goal Achieved!";
        } else {
            return "🏆 Goal Completed!";
        }
    },

    getGoalAchievementDescription: (goal) => {
        const goalType = goal.goalType?.toLowerCase() || '';
        let description = "";

        if (goalType.includes('lose_weight') && goal.targetWeightLoss && goal.targetWeightLoss > 0) {
            description += `You lost ${goal.targetWeightLoss} kg`;
        } else if (goalType.includes('gain_muscle') && goal.targetWeightGain && goal.targetWeightGain > 0) {
            description += `You gained ${goal.targetWeightGain} kg of muscle`;
        } else if (goalType.includes('maintain_health')) {
            description += "You successfully maintained your health goals";
        } else {
            description += "You achieved your fitness goal";
        }

        if (goal.timeframeMonths) {
            const months = goal.timeframeMonths === 1 ? "month" : "months";
            description += ` in ${goal.timeframeMonths} ${months}`;
        }

        description += "!";
        return description;
    },

    getGoalAchievementIcon: (goalType) => {
        const type = goalType?.toLowerCase() || '';

        if (type.includes('lose_weight')) {
            return "🎯";
        } else if (type.includes('gain_muscle')) {
            return "💪";
        } else if (type.includes('maintain_health')) {
            return "⚖️";
        } else {
            return "🏆";
        }
    },

    calculateAchievementPoints: (goal) => {
        let basePoints = 100;

        if (goal.targetWeightLoss) {
            basePoints += goal.targetWeightLoss * 10;
        }
        if (goal.targetWeightGain) {
            basePoints += goal.targetWeightGain * 10;
        }

        if (goal.timeframeMonths) {
            basePoints += Math.max(0, (12 - goal.timeframeMonths) * 5);
        }

        return basePoints;
    },

    getQuickStats: async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/dashboard/quick-stats`, {
                method: 'GET',
                headers: getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch quick stats: ${response.status}`);
            }

            return await response.json();

        } catch (error) {
            console.error('Error fetching quick stats:', error);
            throw error;
        }
    }
};

const Analytics = ({ isOpen, onClose }) => {
    const [dashboardData, setDashboardData] = useState(null);
    const [workoutCalendar, setWorkoutCalendar] = useState([]);
    const [workoutTrends, setWorkoutTrends] = useState([]);
    const [workoutTypes, setWorkoutTypes] = useState([]);
    const [achievements, setAchievements] = useState([]);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const [selectedPeriod, setSelectedPeriod] = useState('weekly');
    const [selectedTab, setSelectedTab] = useState('overview');

    useEffect(() => {
        if (isOpen) {
            loadDashboardData();
        }
    }, [isOpen]);

    const loadDashboardData = async () => {
        setLoading(true);
        setError('');

        try {
            const summary = await DashboardService.getDashboardSummary();
            setDashboardData(summary);

            const endDate = new Date().toISOString().split('T')[0];
            const startDate = new Date(Date.now() - 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
            const calendar = await DashboardService.getWorkoutCalendar(startDate, endDate);
            setWorkoutCalendar(calendar);

            const trends = await DashboardService.getWorkoutTrends(selectedPeriod);
            setWorkoutTrends(trends);

            const types = await DashboardService.getWorkoutTypeBreakdown();
            setWorkoutTypes(types);

            const recentAchievements = await DashboardService.getRecentAchievements(30);
            setAchievements(recentAchievements);

            console.log('Dashboard data loaded successfully');

        } catch (error) {
            console.error('Error loading dashboard data:', error);
            setError('Failed to load dashboard data. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handlePeriodChange = async (newPeriod) => {
        setSelectedPeriod(newPeriod);
        try {
            const trends = await DashboardService.getWorkoutTrends(newPeriod);
            setWorkoutTrends(trends);
        } catch (error) {
            console.error('Error updating trends:', error);
        }
    };

    const formatNumber = (num) => {
        if (num === null || num === undefined) return '0';
        return num.toLocaleString();
    };

    const getIntensityColor = (level) => {
        const colors = {
            0: '#f0f0f0',
            1: '#c6e48b',
            2: '#7bc96f',
            3: '#239a3b',
            4: '#196127'
        };
        return colors[level] || colors[0];
    };

    const getCategoryEmoji = (category) => {
        const emojis = {
            'CARDIO': '🏃',
            'STRENGTH': '💪',
            'FLEXIBILITY': '🧘',
            'BALANCE': '⚖️',
            'SPORTS': '⚽',
            'OTHER': '🏋️'
        };
        return emojis[category] || '🏋️';
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
                maxWidth: '1200px',
                width: '100%',
                maxHeight: '90vh',
                overflowY: 'auto',
                boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
                position: 'relative'
            }}>
                <button
                    onClick={onClose}
                    style={{
                        position: 'absolute',
                        top: '20px',
                        right: '20px',
                        background: 'none',
                        border: 'none',
                        fontSize: '24px',
                        cursor: 'pointer',
                        color: '#718096',
                        width: '40px',
                        height: '40px',
                        borderRadius: '50%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                    }}
                >
                    ×
                </button>

                <div style={{ marginBottom: '32px' }}>
                    <h1 style={{
                        color: '#000000',
                        fontSize: '32px',
                        fontWeight: '800',
                        marginBottom: '8px',
                        backgroundClip: 'text',
                        WebkitBackgroundClip: 'text',
                    }}>
                        Fitness Dashboard
                    </h1>
                    <p style={{
                        color: '#718096',
                        fontSize: '16px',
                        margin: 0
                    }}>
                        Monitor your progress and visualize your fitness journey
                    </p>
                </div>

                <div style={{
                    display: 'flex',
                    marginBottom: '32px',
                    borderBottom: '2px solid #e2e8f0',
                    gap: '8px'
                }}>
                    {['overview', 'trends', 'calendar', 'achievements'].map(tab => (
                        <button
                            key={tab}
                            onClick={() => setSelectedTab(tab)}
                            style={{
                                padding: '12px 24px',
                                background: 'none',
                                border: 'none',
                                borderBottom: selectedTab === tab ? '3px solid #667eea' : '3px solid transparent',
                                color: selectedTab === tab ? '#667eea' : '#718096',
                                fontWeight: '600',
                                cursor: 'pointer',
                                fontSize: '16px',
                                textTransform: 'capitalize',
                                transition: 'all 0.2s'
                            }}
                        >
                            {tab === 'overview' && '📈 '}{tab === 'trends' && '📊 '}
                            {tab === 'calendar' && '📅 '}{tab === 'achievements' && '🏆 '}
                            {tab}
                        </button>
                    ))}
                </div>

                {loading && (
                    <div style={{
                        textAlign: 'center',
                        padding: '60px',
                        color: '#718096'
                    }}>
                        <div style={{
                            fontSize: '32px',
                            marginBottom: '16px',
                            animation: 'spin 1s linear infinite'
                        }}>⚡</div>
                        <div style={{ fontSize: '18px', fontWeight: '600' }}>Loading your fitness data...</div>
                    </div>
                )}

                {error && (
                    <div style={{
                        backgroundColor: '#fed7d7',
                        color: '#c53030',
                        padding: '16px',
                        borderRadius: '12px',
                        marginBottom: '24px',
                        border: '1px solid #feb2b2',
                        textAlign: 'center'
                    }}>
                        <div style={{ fontSize: '18px', marginBottom: '8px' }}>⚠️</div>
                        <div style={{ fontWeight: '600' }}>{error}</div>
                        <button
                            onClick={loadDashboardData}
                            style={{
                                marginTop: '12px',
                                padding: '8px 16px',
                                backgroundColor: '#e53e3e',
                                color: 'white',
                                border: 'none',
                                borderRadius: '6px',
                                cursor: 'pointer',
                                fontWeight: '600'
                            }}
                        >
                            Retry
                        </button>
                    </div>
                )}

                {!loading && !error && dashboardData && (
                    <>
                        {selectedTab === 'overview' && (
                            <div>
                                <div style={{
                                    display: 'grid',
                                    gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
                                    gap: '20px',
                                    marginBottom: '32px'
                                }}>
                                    <div style={{
                                        background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                        padding: '24px',
                                        borderRadius: '16px',
                                        color: 'white',
                                        boxShadow: '0 8px 32px rgba(102, 126, 234, 0.3)'
                                    }}>
                                        <div style={{ fontSize: '14px', opacity: 0.9, marginBottom: '8px' }}>This Week</div>
                                        <div style={{ fontSize: '32px', fontWeight: '800', marginBottom: '4px' }}>
                                            {formatNumber(dashboardData.weeklyWorkouts)}
                                        </div>
                                        <div style={{ fontSize: '16px', opacity: 0.9 }}>Workouts Completed</div>
                                        <div style={{ fontSize: '12px', opacity: 0.8, marginTop: '8px' }}>
                                            🔥 {formatNumber(dashboardData.weeklyCalories)} calories burned
                                        </div>
                                    </div>

                                    <div style={{
                                        background: 'linear-gradient(135deg, #f093fb, #f5576c)',
                                        padding: '24px',
                                        borderRadius: '16px',
                                        color: 'white',
                                        boxShadow: '0 8px 32px rgba(240, 147, 251, 0.3)'
                                    }}>
                                        <div style={{ fontSize: '14px', opacity: 0.9, marginBottom: '8px' }}>This Month</div>
                                        <div style={{ fontSize: '32px', fontWeight: '800', marginBottom: '4px' }}>
                                            {formatNumber(dashboardData.monthlyWorkouts)}
                                        </div>
                                        <div style={{ fontSize: '16px', opacity: 0.9 }}>Total Workouts</div>
                                        <div style={{ fontSize: '12px', opacity: 0.8, marginTop: '8px' }}>
                                            ⭐ {dashboardData.monthlyAvgRating || 0}/5 avg rating
                                        </div>
                                    </div>

                                    <div style={{
                                        background: 'linear-gradient(135deg, #4facfe, #00f2fe)',
                                        padding: '24px',
                                        borderRadius: '16px',
                                        color: 'white',
                                        boxShadow: '0 8px 32px rgba(79, 172, 254, 0.3)'
                                    }}>
                                        <div style={{ fontSize: '14px', opacity: 0.9, marginBottom: '8px' }}>Current Streak</div>
                                        <div style={{ fontSize: '32px', fontWeight: '800', marginBottom: '4px' }}>
                                            {formatNumber(dashboardData.currentStreak)} 🔥
                                        </div>
                                        <div style={{ fontSize: '16px', opacity: 0.9 }}>Days in a row</div>
                                        <div style={{ fontSize: '12px', opacity: 0.8, marginTop: '8px' }}>
                                            🏆 Best: {formatNumber(dashboardData.longestStreak)} days
                                        </div>
                                    </div>

                                    <div style={{
                                        background: 'linear-gradient(135deg, #fa709a, #fee140)',
                                        padding: '24px',
                                        borderRadius: '16px',
                                        color: 'white',
                                        boxShadow: '0 8px 32px rgba(250, 112, 154, 0.3)'
                                    }}>
                                        <div style={{ fontSize: '14px', opacity: 0.9, marginBottom: '8px' }}>All Time</div>
                                        <div style={{ fontSize: '32px', fontWeight: '800', marginBottom: '4px' }}>
                                            {formatNumber(dashboardData.totalWorkouts)}
                                        </div>
                                        <div style={{ fontSize: '16px', opacity: 0.9 }}>Total Workouts</div>
                                        <div style={{ fontSize: '12px', opacity: 0.8, marginTop: '8px' }}>
                                            💪 {formatNumber(dashboardData.totalCalories)} total calories
                                        </div>
                                    </div>
                                </div>

                                {workoutTypes.length > 0 && (
                                    <div style={{
                                        backgroundColor: '#f7fafc',
                                        padding: '24px',
                                        borderRadius: '16px',
                                        marginBottom: '32px'
                                    }}>
                                        <h3 style={{
                                            color: '#2d3748',
                                            fontSize: '20px',
                                            fontWeight: '700',
                                            marginBottom: '20px'
                                        }}>
                                            Workout Type Breakdown (Last 90 Days)
                                        </h3>
                                        <div style={{
                                            display: 'grid',
                                            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                                            gap: '16px'
                                        }}>
                                            {workoutTypes.map((type, index) => (
                                                <div key={index} style={{
                                                    backgroundColor: 'white',
                                                    padding: '20px',
                                                    borderRadius: '12px',
                                                    border: '2px solid #e2e8f0',
                                                    textAlign: 'center'
                                                }}>
                                                    <div style={{ fontSize: '24px', marginBottom: '8px' }}>
                                                        {getCategoryEmoji(type.category)}
                                                    </div>
                                                    <div style={{
                                                        color: '#2d3748',
                                                        fontSize: '18px',
                                                        fontWeight: '700',
                                                        marginBottom: '4px'
                                                    }}>
                                                        {type.category}
                                                    </div>
                                                    <div style={{
                                                        color: '#4a5568',
                                                        fontSize: '14px',
                                                        marginBottom: '8px'
                                                    }}>
                                                        {formatNumber(type.workoutCount)} workouts
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                        {selectedTab === 'achievements' && (
                            <div>
                                <div style={{
                                    backgroundColor: '#f7fafc',
                                    padding: '24px',
                                    borderRadius: '16px'
                                }}>
                                    <div style={{
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                        marginBottom: '20px'
                                    }}>
                                        <h3 style={{
                                            color: '#2d3748',
                                            fontSize: '20px',
                                            fontWeight: '700',
                                            margin: 0
                                        }}>
                                            Recent Achievements 🏆
                                        </h3>
                                        <div style={{
                                            backgroundColor: '#667eea',
                                            color: 'white',
                                            padding: '6px 12px',
                                            borderRadius: '20px',
                                            fontSize: '14px',
                                            fontWeight: '600'
                                        }}>
                                            {achievements.length} achievements
                                        </div>
                                    </div>

                                    {achievements.length > 0 ? (
                                        <div style={{
                                            display: 'grid',
                                            gap: '16px'
                                        }}>
                                            {achievements.map((achievement, index) => {
                                                const isGoalAchievement = achievement.type === 'COMPLETED_GOAL';

                                                return (
                                                    <div key={index} style={{
                                                        backgroundColor: 'white',
                                                        padding: '20px',
                                                        borderRadius: '12px',
                                                        border: '2px solid #e2e8f0',
                                                        borderLeft: `4px solid ${isGoalAchievement ? '#48bb78' : '#667eea'}`,
                                                        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                                                        transition: 'transform 0.2s, box-shadow 0.2s',
                                                        cursor: 'pointer'
                                                    }}
                                                         onMouseEnter={(e) => {
                                                             e.currentTarget.style.transform = 'translateY(-2px)';
                                                             e.currentTarget.style.boxShadow = '0 4px 16px rgba(0,0,0,0.15)';
                                                         }}
                                                         onMouseLeave={(e) => {
                                                             e.currentTarget.style.transform = 'translateY(0)';
                                                             e.currentTarget.style.boxShadow = '0 2px 8px rgba(0,0,0,0.1)';
                                                         }}>
                                                        <div style={{
                                                            display: 'flex',
                                                            alignItems: 'flex-start',
                                                            gap: '16px'
                                                        }}>
                                                            <div style={{
                                                                fontSize: '32px',
                                                                minWidth: '40px',
                                                                textAlign: 'center'
                                                            }}>
                                                                {achievement.icon || (isGoalAchievement ? '🎯' : '🏆')}
                                                            </div>

                                                            <div style={{ flex: 1 }}>
                                                                <div style={{
                                                                    color: '#2d3748',
                                                                    fontSize: '18px',
                                                                    fontWeight: '700',
                                                                    marginBottom: '8px',
                                                                    lineHeight: '1.3'
                                                                }}>
                                                                    {achievement.title || achievement.achievementTitle}
                                                                </div>

                                                                <div style={{
                                                                    color: '#4a5568',
                                                                    fontSize: '14px',
                                                                    marginBottom: '12px',
                                                                    lineHeight: '1.4'
                                                                }}>
                                                                    {achievement.description || achievement.achievementDescription}
                                                                </div>

                                                                <div style={{
                                                                    display: 'flex',
                                                                    justifyContent: 'space-between',
                                                                    alignItems: 'center'
                                                                }}>
                                                                    <div style={{
                                                                        color: '#718096',
                                                                        fontSize: '12px',
                                                                        display: 'flex',
                                                                        alignItems: 'center',
                                                                        gap: '4px'
                                                                    }}>
                                                                        📅 {new Date(achievement.achievedAt || achievement.achievedDate).toLocaleDateString('ro-RO', {
                                                                        day: 'numeric',
                                                                        month: 'long',
                                                                        year: 'numeric'
                                                                    })}
                                                                    </div>

                                                                    <div style={{
                                                                        display: 'flex',
                                                                        gap: '8px',
                                                                        alignItems: 'center'
                                                                    }}>
                                                                        {isGoalAchievement && (
                                                                            <span style={{
                                                                                backgroundColor: '#48bb78',
                                                                                color: 'white',
                                                                                padding: '4px 8px',
                                                                                borderRadius: '12px',
                                                                                fontSize: '11px',
                                                                                fontWeight: '600'
                                                                            }}>
                                                        GOAL
                                                    </span>
                                                                        )}

                                                                        {achievement.points && (
                                                                            <span style={{
                                                                                backgroundColor: '#fbb040',
                                                                                color: 'white',
                                                                                padding: '4px 8px',
                                                                                borderRadius: '12px',
                                                                                fontSize: '11px',
                                                                                fontWeight: '600'
                                                                            }}>
                                                        +{achievement.points} PTS
                                                    </span>
                                                                        )}
                                                                    </div>
                                                                </div>

                                                                {isGoalAchievement && achievement.originalGoal && (
                                                                    <div style={{
                                                                        marginTop: '12px',
                                                                        padding: '12px',
                                                                        backgroundColor: '#f7fafc',
                                                                        borderRadius: '8px',
                                                                        fontSize: '12px'
                                                                    }}>
                                                                        <div style={{
                                                                            color: '#4a5568',
                                                                            fontWeight: '600',
                                                                            marginBottom: '4px'
                                                                        }}>
                                                                            Goal Details:
                                                                        </div>
                                                                        <div style={{ color: '#718096' }}>
                                                                            {achievement.originalGoal.targetWeightLoss &&
                                                                                `Target: -${achievement.originalGoal.targetWeightLoss} kg`}
                                                                            {achievement.originalGoal.targetWeightGain &&
                                                                                `Target: +${achievement.originalGoal.targetWeightGain} kg`}
                                                                            {achievement.originalGoal.timeframeMonths &&
                                                                                ` in ${achievement.originalGoal.timeframeMonths} months`}
                                                                        </div>
                                                                    </div>
                                                                )}
                                                            </div>
                                                        </div>
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    ) : (
                                        <div style={{
                                            textAlign: 'center',
                                            padding: '60px 20px',
                                            color: '#718096'
                                        }}>
                                            <div style={{
                                                fontSize: '64px',
                                                marginBottom: '20px',
                                                opacity: 0.7
                                            }}>
                                                🏆
                                            </div>
                                            <div style={{
                                                fontSize: '20px',
                                                fontWeight: '600',
                                                marginBottom: '12px',
                                                color: '#2d3748'
                                            }}>
                                                No recent achievements
                                            </div>
                                            <div style={{
                                                fontSize: '16px',
                                                lineHeight: '1.5',
                                                maxWidth: '400px',
                                                margin: '0 auto'
                                            }}>
                                                Keep working out and completing your goals to unlock achievements!
                                            </div>
                                            <div style={{
                                                marginTop: '20px',
                                                fontSize: '14px',
                                                color: '#a0aec0'
                                            }}>
                                                💪 Complete workouts • 🎯 Achieve goals • 🏆 Earn achievements
                                            </div>
                                        </div>
                                    )}

                                    {achievements.length > 0 && (
                                        <div style={{
                                            marginTop: '24px',
                                            padding: '20px',
                                            backgroundColor: 'white',
                                            borderRadius: '12px',
                                            border: '2px solid #e2e8f0'
                                        }}>
                                            <h4 style={{
                                                color: '#2d3748',
                                                fontSize: '16px',
                                                fontWeight: '600',
                                                marginBottom: '16px'
                                            }}>
                                                Achievement Summary
                                            </h4>

                                            <div style={{
                                                display: 'grid',
                                                gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
                                                gap: '16px'
                                            }}>
                                                <div style={{ textAlign: 'center' }}>
                                                    <div style={{
                                                        color: '#667eea',
                                                        fontSize: '24px',
                                                        fontWeight: '800'
                                                    }}>
                                                        {achievements.length}
                                                    </div>
                                                    <div style={{
                                                        color: '#718096',
                                                        fontSize: '12px'
                                                    }}>
                                                        Total Achievements
                                                    </div>
                                                </div>

                                                <div style={{ textAlign: 'center' }}>
                                                    <div style={{
                                                        color: '#48bb78',
                                                        fontSize: '24px',
                                                        fontWeight: '800'
                                                    }}>
                                                        {achievements.filter(a => a.type === 'COMPLETED_GOAL').length}
                                                    </div>
                                                    <div style={{
                                                        color: '#718096',
                                                        fontSize: '12px'
                                                    }}>
                                                        Completed Goals
                                                    </div>
                                                </div>

                                                <div style={{ textAlign: 'center' }}>
                                                    <div style={{
                                                        color: '#fbb040',
                                                        fontSize: '24px',
                                                        fontWeight: '800'
                                                    }}>
                                                        {achievements.reduce((total, a) => total + (a.points || 0), 0)}
                                                    </div>
                                                    <div style={{
                                                        color: '#718096',
                                                        fontSize: '12px'
                                                    }}>
                                                        Total Points
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}

                        {selectedTab === 'trends' && (
                            <div>
                                <div style={{ marginBottom: '24px' }}>
                                    <div style={{
                                        display: 'flex',
                                        gap: '8px',
                                        marginBottom: '16px'
                                    }}>
                                        {['daily', 'weekly', 'monthly'].map(period => (
                                            <button
                                                key={period}
                                                onClick={() => handlePeriodChange(period)}
                                                style={{
                                                    padding: '8px 16px',
                                                    backgroundColor: selectedPeriod === period ? '#667eea' : '#f7fafc',
                                                    color: selectedPeriod === period ? 'white' : '#4a5568',
                                                    border: '2px solid #e2e8f0',
                                                    borderRadius: '8px',
                                                    cursor: 'pointer',
                                                    fontWeight: '600',
                                                    fontSize: '14px',
                                                    textTransform: 'capitalize'
                                                }}
                                            >
                                                {period}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                <div style={{
                                    backgroundColor: '#f7fafc',
                                    padding: '24px',
                                    borderRadius: '16px'
                                }}>
                                    <h3 style={{
                                        color: '#2d3748',
                                        fontSize: '20px',
                                        fontWeight: '700',
                                        marginBottom: '20px'
                                    }}>
                                        Workout Trends ({selectedPeriod})
                                    </h3>

                                    {workoutTrends.length > 0 ? (
                                        <div style={{
                                            display: 'grid',
                                            gap: '8px',
                                            maxHeight: '300px',
                                            overflowY: 'auto'
                                        }}>
                                            {workoutTrends.map((trend, index) => (
                                                <div key={index} style={{
                                                    backgroundColor: 'white',
                                                    padding: '16px',
                                                    borderRadius: '8px',
                                                    display: 'flex',
                                                    justifyContent: 'space-between',
                                                    alignItems: 'center',
                                                    border: '1px solid #e2e8f0'
                                                }}>
                                                    <div>
                                                        <div style={{
                                                            color: '#2d3748',
                                                            fontWeight: '600'
                                                        }}>
                                                            {trend.periodLabel}
                                                        </div>
                                                        <div style={{
                                                            color: '#718096',
                                                            fontSize: '14px'
                                                        }}>
                                                            {formatNumber(trend.workoutCount)} workouts
                                                        </div>
                                                    </div>
                                                    <div style={{ textAlign: 'right' }}>
                                                        <div style={{
                                                            color: '#4a5568',
                                                            fontSize: '14px'
                                                        }}>
                                                            🔥 {formatNumber(trend.totalCalories)} cal
                                                        </div>
                                                        <div style={{
                                                            color: '#718096',
                                                            fontSize: '12px'
                                                        }}>
                                                            ⭐ {trend.avgRating || 0}/5
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    ) : (
                                        <div style={{
                                            textAlign: 'center',
                                            padding: '40px',
                                            color: '#718096'
                                        }}>
                                            <div style={{ fontSize: '48px', marginBottom: '16px' }}>📊</div>
                                            <div>No trend data available for this period</div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}

                        {selectedTab === 'calendar' && (
                            <div>
                                <div style={{
                                    backgroundColor: '#f7fafc',
                                    padding: '24px',
                                    borderRadius: '16px'
                                }}>
                                    <h3 style={{
                                        color: '#2d3748',
                                        fontSize: '20px',
                                        fontWeight: '700',
                                        marginBottom: '20px'
                                    }}>
                                        Workout Calendar (Last 365 Days)
                                    </h3>

                                    <div style={{
                                        display: 'grid',
                                        gridTemplateColumns: 'repeat(auto-fill, minmax(12px, 1fr))',
                                        gap: '2px',
                                        maxWidth: '100%'
                                    }}>
                                        {workoutCalendar.map((day, index) => (
                                            <div
                                                key={index}
                                                title={`${day.workoutDate}: ${day.workoutCount} workouts, ${day.totalCalories} calories`}
                                                style={{
                                                    width: '12px',
                                                    height: '12px',
                                                    backgroundColor: getIntensityColor(day.intensityLevel),
                                                    borderRadius: '2px',
                                                    cursor: 'pointer'
                                                }}
                                            />
                                        ))}
                                    </div>

                                    <div style={{
                                        marginTop: '20px',
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '8px',
                                        fontSize: '12px',
                                        color: '#718096'
                                    }}>
                                        <span>Less</span>
                                        {[0, 1, 2, 3, 4].map(level => (
                                            <div
                                                key={level}
                                                style={{
                                                    width: '12px',
                                                    height: '12px',
                                                    backgroundColor: getIntensityColor(level),
                                                    borderRadius: '2px'
                                                }}
                                            />
                                        ))}
                                        <span>More</span>
                                    </div>
                                </div>
                            </div>
                        )}
                    </>
                )}

                <div style={{
                    marginTop: '32px',
                    padding: '20px',
                    borderTop: '2px solid #e2e8f0',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                }}>
                </div>

                <style>{`
                    @keyframes spin {
                        0% { transform: rotate(0deg); }
                        100% { transform: rotate(360deg); }
                    }

                    @keyframes fadeIn {
                        from { opacity: 0; transform: translateY(20px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                `}</style>
            </div>
        </div>
    );
};

export default Analytics;