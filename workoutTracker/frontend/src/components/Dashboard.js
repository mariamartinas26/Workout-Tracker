import React, { useState } from 'react';
import CreatePlan from './CreatePlan';
import PlanWorkout from './PlanWorkout';
import Analytics from './Analytics';

const Dashboard = ({ user, sampleExercises, onLogout, onEditProfile, onGoToGoals }) => {
    const [showWorkoutPopup, setShowWorkoutPopup] = useState(false);
    const [showSchedulerPopup, setShowSchedulerPopup] = useState(false);
    const [showAnalytics, setShowAnalytics] = useState(false);


    console.log('showSchedulerPopup state:', showSchedulerPopup);

    return (
        <div style={{
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
            padding: '20px',
            fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, sans-serif"
        }}>
            {/* Background decorative elements */}
            <div style={{
                position: 'fixed',
                top: '10%',
                left: '5%',
                width: '300px',
                height: '300px',
                background: 'rgba(255,255,255,0.05)',
                borderRadius: '50%',
                filter: 'blur(80px)',
                animation: 'float 8s ease-in-out infinite',
                zIndex: 0
            }}></div>
            <div style={{
                position: 'fixed',
                bottom: '15%',
                right: '10%',
                width: '200px',
                height: '200px',
                background: 'rgba(255,255,255,0.03)',
                borderRadius: '50%',
                filter: 'blur(60px)',
                animation: 'float 10s ease-in-out infinite reverse',
                zIndex: 0
            }}></div>

            <div style={{
                maxWidth: '1200px',
                margin: '0 auto',
                position: 'relative',
                zIndex: 1
            }}>
                {/* Header */}
                <div style={{
                    backgroundColor: 'rgba(255,255,255,0.95)',
                    backdropFilter: 'blur(20px)',
                    borderRadius: '20px',
                    boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
                    padding: '24px 32px',
                    marginBottom: '24px',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    border: '1px solid rgba(255,255,255,0.2)'
                }}>
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: '48px',
                            height: '48px',
                            marginRight: '16px'
                        }}>
                            <img
                                src="/icon.png"
                                alt="WorkoutTracker Logo"
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
                            <div style={{
                                display: 'none',
                                alignItems: 'center',
                                justifyContent: 'center',
                                width: '48px',
                                height: '48px',
                                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                borderRadius: '12px',
                                boxShadow: '0 4px 16px rgba(102, 126, 234, 0.3)'
                            }}>
                            </div>
                        </div>
                        <div>
                            <h1 style={{
                                margin: '0',
                                color: '#1a202c',
                                fontSize: '24px',
                                fontWeight: '800',
                                letterSpacing: '-0.5px'
                            }}>
                                WorkoutTracker
                            </h1>
                            {user && (
                                <p style={{
                                    margin: '2px 0 0 0',
                                    color: '#718096',
                                    fontSize: '14px',
                                    fontWeight: '500'
                                }}>
                                    Welcome back, {user.firstName} {user.lastName}!
                                </p>
                            )}
                        </div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        {/* Add Analytics Button */}
                        <button
                            onClick={() => setShowAnalytics(true)}
                            style={{
                                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                boxShadow: '0 4px 16px rgba(102, 126, 234, 0.3)',
                                color: 'white',
                                border: 'none',
                                padding: '12px 20px',
                                borderRadius: '10px',
                                cursor: 'pointer',
                                fontSize: '14px',
                                fontWeight: '600',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                letterSpacing: '0.025em',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px'
                            }}
                            onMouseEnter={(e) => {
                                e.target.style.transform = 'translateY(-2px)';
                                e.target.style.boxShadow = '0 6px 20px rgba(102, 126, 234, 0.4)';
                            }}
                            onMouseLeave={(e) => {
                                e.target.style.transform = 'translateY(0)';
                                e.target.style.boxShadow = '0 4px 16px rgba(102, 126, 234, 0.3)';
                            }}
                        >
                            📊 Analytics
                        </button>

                        <button
                            onClick={onEditProfile}
                            style={{
                                background: 'linear-gradient(135deg, #1e3a8a, #1d4ed8)',
                                boxShadow: '0 4px 16px rgba(30, 58, 138, 0.3)',
                                color: 'white',
                                border: 'none',
                                padding: '12px 20px',
                                borderRadius: '10px',
                                cursor: 'pointer',
                                fontSize: '14px',
                                fontWeight: '600',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                letterSpacing: '0.025em'
                            }}
                            onMouseEnter={(e) => {
                                e.target.style.transform = 'translateY(-2px)';
                                e.target.style.boxShadow = '0 6px 20px rgba(56, 178, 172, 0.4)';
                            }}
                            onMouseLeave={(e) => {
                                e.target.style.transform = 'translateY(0)';
                                e.target.style.boxShadow = '0 4px 16px rgba(56, 178, 172, 0.3)';
                            }}
                        >
                            Edit Profile
                        </button>
                        <button
                            onClick={onLogout}
                            style={{
                                background: 'linear-gradient(135deg, #e53e3e, #c53030)',
                                color: 'white',
                                border: 'none',
                                padding: '12px 24px',
                                borderRadius: '10px',
                                cursor: 'pointer',
                                fontSize: '14px',
                                fontWeight: '600',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                boxShadow: '0 4px 16px rgba(229, 62, 62, 0.3)',
                                letterSpacing: '0.025em'
                            }}
                            onMouseEnter={(e) => {
                                e.target.style.transform = 'translateY(-2px)';
                                e.target.style.boxShadow = '0 6px 20px rgba(229, 62, 62, 0.4)';
                            }}
                            onMouseLeave={(e) => {
                                e.target.style.transform = 'translateY(0)';
                                e.target.style.boxShadow = '0 4px 16px rgba(229, 62, 62, 0.3)';
                            }}
                        >
                            Sign Out
                        </button>
                    </div>
                </div>

                {/* Dashboard Content */}
                <div style={{
                    backgroundColor: 'rgba(255,255,255,0.95)',
                    backdropFilter: 'blur(20px)',
                    borderRadius: '20px',
                    boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
                    padding: '48px',
                    textAlign: 'center',
                    border: '1px solid rgba(255,255,255,0.2)',
                    position: 'relative',
                    overflow: 'hidden'
                }}>
                    {/* Subtle gradient overlay */}
                    <div style={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        right: 0,
                        height: '4px',
                        background: 'linear-gradient(90deg, #667eea, #764ba2, #f093fb)',
                        borderRadius: '20px 20px 0 0'
                    }}></div>

                    <h2 style={{
                        color: '#000000',
                        fontSize: '40px',
                        fontWeight: '800',
                        marginBottom: '16px',
                        letterSpacing: '-1px',
                        background: '#000000',
                        backgroundClip: 'text',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent'
                    }}>
                        Welcome to WorkoutTracker!
                    </h2>
                    <p style={{
                        color: '#718096',
                        fontSize: '20px',
                        marginBottom: '48px',
                        lineHeight: '1.6',
                        fontWeight: '500',
                        maxWidth: '600px',
                        margin: '0 auto 48px auto'
                    }}>
                        Your personal fitness companion to track workouts, monitor progress, and achieve your goals.
                    </p>

                    {/* User Profile Summary */}
                    {user && (user.dateOfBirth || user.heightCm || user.weightKg || user.fitnessLevel) && (
                        <div style={{
                            background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.08), rgba(118, 75, 162, 0.05))',
                            borderRadius: '16px',
                            padding: '24px',
                            marginBottom: '40px',
                            border: '1px solid rgba(102, 126, 234, 0.1)'
                        }}>
                            <h3 style={{
                                color: '#1a202c',
                                fontSize: '18px',
                                fontWeight: '700',
                                marginBottom: '16px',
                                letterSpacing: '-0.25px'
                            }}>
                                Your Profile
                            </h3>
                            <div style={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
                                gap: '16px',
                                textAlign: 'center'
                            }}>
                                {user.dateOfBirth && (
                                    <div>
                                        <div style={{
                                            color: '#000000',
                                            fontSize: '20px',
                                            fontWeight: '800',
                                            marginBottom: '4px'
                                        }}>
                                            {Math.floor((new Date() - new Date(user.dateOfBirth)) / (365.25 * 24 * 60 * 60 * 1000))}
                                        </div>
                                        <div style={{
                                            color: '#718096',
                                            fontSize: '12px',
                                            fontWeight: '600',
                                            textTransform: 'uppercase',
                                            letterSpacing: '0.5px'
                                        }}>
                                            Years Old
                                        </div>
                                    </div>
                                )}
                                {user.heightCm && (
                                    <div>
                                        <div style={{
                                            color: '#000000',
                                            fontSize: '20px',
                                            fontWeight: '800',
                                            marginBottom: '4px'
                                        }}>
                                            {user.heightCm}cm
                                        </div>
                                        <div style={{
                                            color: '#718096',
                                            fontSize: '12px',
                                            fontWeight: '600',
                                            textTransform: 'uppercase',
                                            letterSpacing: '0.5px'
                                        }}>
                                            Height
                                        </div>
                                    </div>
                                )}
                                {user.weightKg && (
                                    <div>
                                        <div style={{
                                            color: '#000000',
                                            fontSize: '20px',
                                            fontWeight: '800',
                                            marginBottom: '4px'
                                        }}>
                                            {user.weightKg}kg
                                        </div>
                                        <div style={{
                                            color: '#718096',
                                            fontSize: '12px',
                                            fontWeight: '600',
                                            textTransform: 'uppercase',
                                            letterSpacing: '0.5px'
                                        }}>
                                            Weight
                                        </div>
                                    </div>
                                )}
                                {user.fitnessLevel && (
                                    <div>
                                        <div style={{
                                            color: '#000000',
                                            fontSize: '16px',
                                            fontWeight: '800',
                                            marginBottom: '4px',
                                            textTransform: 'capitalize'
                                        }}>
                                            {user.fitnessLevel.toLowerCase()}
                                        </div>
                                        <div style={{
                                            color: '#718096',
                                            fontSize: '12px',
                                            fontWeight: '600',
                                            textTransform: 'uppercase',
                                            letterSpacing: '0.5px'
                                        }}>
                                            Fitness Level
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                        gap: '24px',
                        marginTop: '48px'
                    }}>
                        {/* Create Workout Plan Card */}
                        <div style={{
                            background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.05))',
                            padding: '32px',
                            borderRadius: '16px',
                            border: '1px solid rgba(102, 126, 234, 0.1)',
                            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                            cursor: 'pointer',
                            position: 'relative',
                            overflow: 'hidden'
                        }}
                             onClick={() => setShowWorkoutPopup(true)}
                             onMouseEnter={(e) => {
                                 e.target.style.transform = 'translateY(-4px)';
                                 e.target.style.boxShadow = '0 16px 40px rgba(102, 126, 234, 0.15)';
                             }}
                             onMouseLeave={(e) => {
                                 e.target.style.transform = 'translateY(0)';
                                 e.target.style.boxShadow = 'none';
                             }}>
                            <div>
                                <img
                                    src="/weight-lifting.png"
                                    alt="WeightLift Logo"
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
                            <h3 style={{
                                color: '#1a202c',
                                fontSize: '20px',
                                fontWeight: '700',
                                marginBottom: '12px',
                                letterSpacing: '-0.25px'
                            }}>
                                Create Workout Plan
                            </h3>
                            <p style={{
                                color: '#718096',
                                fontSize: '15px',
                                margin: '0',
                                lineHeight: '1.5',
                                fontWeight: '500'
                            }}>
                                Design custom workout plans with detailed exercise routines
                            </p>
                        </div>

                        {/* Schedule Workout Card */}
                        <div
                            onClick={() => {
                                console.log('Schedule Workout clicked!');
                                console.log('Current showSchedulerPopup state:', showSchedulerPopup);
                                setShowSchedulerPopup(true);
                                console.log('Setting showSchedulerPopup to true');
                            }}
                            style={{
                                background: 'linear-gradient(135deg, rgba(56, 178, 172, 0.1), rgba(49, 151, 149, 0.05))',
                                padding: '32px',
                                borderRadius: '16px',
                                border: '1px solid rgba(56, 178, 172, 0.1)',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                cursor: 'pointer',
                                position: 'relative',
                                overflow: 'hidden'
                            }}
                            onMouseEnter={(e) => {
                                e.target.style.transform = 'translateY(-4px)';
                                e.target.style.boxShadow = '0 16px 40px rgba(56, 178, 172, 0.15)';
                            }}
                            onMouseLeave={(e) => {
                                e.target.style.transform = 'translateY(0)';
                                e.target.style.boxShadow = 'none';
                            }}>
                            <div>
                                <img
                                    src="/calendar.png"
                                    alt="Calendar Logo"
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
                            <h3 style={{
                                color: '#1a202c',
                                fontSize: '20px',
                                fontWeight: '700',
                                marginBottom: '12px',
                                letterSpacing: '-0.25px'
                            }}>
                                Schedule Workout
                            </h3>
                            <p style={{
                                color: '#718096',
                                fontSize: '15px',
                                margin: '0',
                                lineHeight: '1.5',
                                fontWeight: '500'
                            }}>
                                Plan your workout sessions from your existing workout plans
                            </p>
                        </div>

                        {/* Achieve Goals Card */}
                        <div style={{
                            background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.05))',
                            padding: '32px',
                            borderRadius: '16px',
                            border: '1px solid rgba(102, 126, 234, 0.1)',
                            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                            cursor: 'pointer',
                            position: 'relative',
                            overflow: 'hidden'
                        }}
                             onClick={onGoToGoals}
                             onMouseEnter={(e) => {
                                 e.target.style.transform = 'translateY(-4px)';
                                 e.target.style.boxShadow = '0 16px 40px rgba(102, 126, 234, 0.15)';
                             }}
                             onMouseLeave={(e) => {
                                 e.target.style.transform = 'translateY(0)';
                                 e.target.style.boxShadow = 'none';
                             }}>
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
                            <h3 style={{
                                color: '#1a202c',
                                fontSize: '20px',
                                fontWeight: '700',
                                marginBottom: '12px',
                                letterSpacing: '-0.25px'
                            }}>
                                Achieve Goals
                            </h3>
                            <p style={{
                                color: '#718096',
                                fontSize: '15px',
                                margin: '0',
                                lineHeight: '1.5',
                                fontWeight: '500'
                            }}>
                                Set personalized fitness targets and celebrate your achievements
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* Popupuri  */}
            <CreatePlan
                isOpen={showWorkoutPopup}
                onClose={() => setShowWorkoutPopup(false)}
                sampleExercises={sampleExercises}
                currentUserId={user?.id || user?.userId || user?.user_id || 1}
            />
            <PlanWorkout
                isOpen={showSchedulerPopup}
                onClose={() => setShowSchedulerPopup(false)}
                currentUserId={user?.id || user?.userId || user?.user_id || 1}
            />
            {/* ADD WORKOUT DASHBOARD */}
            <Analytics
                isOpen={showAnalytics}
                onClose={() => setShowAnalytics(false)}
                currentUserId={user?.id || user?.userId || user?.user_id || 1}
            />

            <style>
                {`
                    @keyframes float {
                        0%, 100% { transform: translateY(0px); }
                        50% { transform: translateY(-20px); }
                    }
                `}
            </style>
        </div>
    );
};

export default Dashboard;