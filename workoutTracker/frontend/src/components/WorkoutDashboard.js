// components/WorkoutDashboard.js
import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8082/api';

// Dashboard API Service
const DashboardService = {
    /**
     * Get complete dashboard summary
     */
    getDashboardSummary: async (userId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/dashboard/summary/${userId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch dashboard summary: ${response.status}`);
            }

            return await response.json();

        } catch (error) {
            console.error('Error fetching dashboard summary:', error);
            throw error;
        }
    },

    /**
     * Get workout calendar data for heatmap
     */
    getWorkoutCalendar: async (userId, startDate, endDate) => {
        try {
            let url = `${API_BASE_URL}/dashboard/calendar/${userId}`;
            if (startDate && endDate) {
                url += `?startDate=${startDate}&endDate=${endDate}`;
            }

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
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

    /**
     * Get workout trends for charts
     */
    getWorkoutTrends: async (userId, period = 'weekly', startDate, endDate) => {
        try {
            let url = `${API_BASE_URL}/dashboard/trends/${userId}?period=${period}`;
            if (startDate && endDate) {
                url += `&startDate=${startDate}&endDate=${endDate}`;
            }

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
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

    /**
     * Get workout type breakdown
     */
    getWorkoutTypeBreakdown: async (userId, startDate, endDate) => {
        try {
            let url = `${API_BASE_URL}/dashboard/workout-types/${userId}`;
            if (startDate && endDate) {
                url += `?startDate=${startDate}&endDate=${endDate}`;
            }

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
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

    /**
     * Get recent achievements
     */
    getRecentAchievements: async (userId, daysBack = 30) => {
        try {
            const response = await fetch(`${API_BASE_URL}/dashboard/achievements/${userId}?daysBack=${daysBack}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch achievements: ${response.status}`);
            }

            return await response.json();

        } catch (error) {
            console.error('Error fetching achievements:', error);
            throw error;
        }
    },

    /**
     * Get quick stats for summary cards
     */
    getQuickStats: async (userId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/dashboard/quick-stats/${userId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
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

const WorkoutDashboard = ({ currentUserId = 1, isOpen, onClose }) => {
    // State for dashboard data
    const [dashboardData, setDashboardData] = useState(null);
    const [workoutCalendar, setWorkoutCalendar] = useState([]);
    const [workoutTrends, setWorkoutTrends] = useState([]);
    const [workoutTypes, setWorkoutTypes] = useState([]);
    const [achievements, setAchievements] = useState([]);

    // Loading and error states
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // View settings
    const [selectedPeriod, setSelectedPeriod] = useState('weekly');
    const [selectedTab, setSelectedTab] = useState('overview');

    // Load dashboard data when component opens
    useEffect(() => {
        if (isOpen) {
            loadDashboardData();
        }
    }, [isOpen, currentUserId]);

    const loadDashboardData = async () => {
        setLoading(true);
        setError('');

        try {
            // Load main dashboard summary
            const summary = await DashboardService.getDashboardSummary(currentUserId);
            setDashboardData(summary);

            // Load workout calendar (last 365 days)
            const endDate = new Date().toISOString().split('T')[0];
            const startDate = new Date(Date.now() - 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
            const calendar = await DashboardService.getWorkoutCalendar(currentUserId, startDate, endDate);
            setWorkoutCalendar(calendar);

            // Load workout trends
            const trends = await DashboardService.getWorkoutTrends(currentUserId, selectedPeriod);
            setWorkoutTrends(trends);

            // Load workout type breakdown
            const types = await DashboardService.getWorkoutTypeBreakdown(currentUserId);
            setWorkoutTypes(types);

            // Load achievements
            const recentAchievements = await DashboardService.getRecentAchievements(currentUserId, 30);
            setAchievements(recentAchievements);

            console.log('Dashboard data loaded successfully');

        } catch (error) {
            console.error('Error loading dashboard data:', error);
            setError('Failed to load dashboard data. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    // Handle period change for trends
    const handlePeriodChange = async (newPeriod) => {
        setSelectedPeriod(newPeriod);
        try {
            const trends = await DashboardService.getWorkoutTrends(currentUserId, newPeriod);
            setWorkoutTrends(trends);
        } catch (error) {
            console.error('Error updating trends:', error);
        }
    };

    // Format numbers for display
    const formatNumber = (num) => {
        if (num === null || num === undefined) return '0';
        return num.toLocaleString();
    };

    // Get intensity color for calendar heatmap
    const getIntensityColor = (level) => {
        const colors = {
            0: '#f0f0f0',  // No workout
            1: '#c6e48b',  // Light
            2: '#7bc96f',  // Medium
            3: '#239a3b',  // High
            4: '#196127'   // Very high
        };
        return colors[level] || colors[0];
    };

    // Get category emoji
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
                {/* Close button */}
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

                {/* Header */}
                <div style={{ marginBottom: '32px' }}>
                    <h1 style={{
                        color: '#1a202c',
                        fontSize: '32px',
                        fontWeight: '800',
                        marginBottom: '8px',
                        background: 'linear-gradient(135deg, #667eea, #764ba2)',
                        backgroundClip: 'text',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent'
                    }}>
                        📊 Fitness Dashboard
                    </h1>
                    <p style={{
                        color: '#718096',
                        fontSize: '16px',
                        margin: 0
                    }}>
                        Monitor your progress and visualize your fitness journey
                    </p>
                </div>

                {/* Tab Navigation */}
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

                {/* Loading State */}
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

                {/* Error State */}
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

                {/* Dashboard Content */}
                {!loading && !error && dashboardData && (
                    <>
                        {/* Overview Tab */}
                        {selectedTab === 'overview' && (
                            <div>
                                {/* Summary Cards */}
                                <div style={{
                                    display: 'grid',
                                    gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
                                    gap: '20px',
                                    marginBottom: '32px'
                                }}>
                                    {/* Weekly Workouts Card */}
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

                                    {/* Monthly Progress Card */}
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

                                    {/* Current Streak Card */}
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

                                    {/* Total Progress Card */}
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

                                {/* Workout Types Breakdown */}
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
                                                    <div style={{
                                                        color: '#718096',
                                                        fontSize: '12px'
                                                    }}>
                                                        {type.percentage}% of total
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Trends Tab */}
                        {selectedTab === 'trends' && (
                            <div>
                                {/* Period Selector */}
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

                                {/* Trends Chart (Simple visualization) */}
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

                        {/* Calendar Tab */}
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

                                    {/* Simple Calendar Heatmap */}
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

                                    {/* Legend */}
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

                        {/* Achievements Tab */}
                        {selectedTab === 'achievements' && (
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
                                        Recent Achievements 🏆
                                    </h3>

                                    {achievements.length > 0 ? (
                                        <div style={{
                                            display: 'grid',
                                            gap: '16px'
                                        }}>
                                            {achievements.map((achievement, index) => (
                                                <div key={index} style={{
                                                    backgroundColor: 'white',
                                                    padding: '20px',
                                                    borderRadius: '12px',
                                                    border: '2px solid #e2e8f0',
                                                    borderLeft: '4px solid #667eea'
                                                }}>
                                                    <div style={{
                                                        color: '#2d3748',
                                                        fontSize: '18px',
                                                        fontWeight: '700',
                                                        marginBottom: '8px'
                                                    }}>
                                                        {achievement.achievementTitle}
                                                    </div>
                                                    <div style={{
                                                        color: '#4a5568',
                                                        fontSize: '14px',
                                                        marginBottom: '8px'
                                                    }}>
                                                        {achievement.achievementDescription}
                                                    </div>
                                                    <div style={{
                                                        color: '#718096',
                                                        fontSize: '12px'
                                                    }}>
                                                        Achieved on {achievement.achievedDate}
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
                                            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🏆</div>
                                            <div style={{ fontSize: '18px', fontWeight: '600', marginBottom: '8px' }}>
                                                No recent achievements
                                            </div>
                                            <div style={{ fontSize: '14px' }}>
                                                Keep working out to unlock achievements!
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}
                    </>
                )}

                {/* Footer */}
                <div style={{
                    marginTop: '32px',
                    padding: '20px',
                    borderTop: '2px solid #e2e8f0',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                }}>
                    <div style={{
                        color: '#718096',
                        fontSize: '14px'
                    }}>
                        Last updated: {new Date().toLocaleString()}
                    </div>
                    <button
                        onClick={loadDashboardData}
                        disabled={loading}
                        style={{
                            padding: '10px 20px',
                            backgroundColor: loading ? '#cbd5e0' : '#667eea',
                            color: 'white',
                            border: 'none',
                            borderRadius: '8px',
                            cursor: loading ? 'not-allowed' : 'pointer',
                            fontSize: '14px',
                            fontWeight: '600',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px'
                        }}
                    >
                        {loading ? '⏳' : '🔄'} Refresh Data
                    </button>
                </div>

                {/* CSS Animations */}
                <style jsx>{`
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

export default WorkoutDashboard;