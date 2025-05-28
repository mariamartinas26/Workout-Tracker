import React, { useState, useEffect } from 'react';
import Homepage from './components/Homepage';
import Login from './components/Login';
import Register from './components/Register';

const App = () => {
    const [currentView, setCurrentView] = useState('homepage'); // 'homepage', 'login' or 'register'
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null);

    // Check if user is already authenticated on app load
    useEffect(() => {
        const isAuth = localStorage.getItem('isAuthenticated');
        const userData = localStorage.getItem('userData');

        if (isAuth === 'true' && userData) {
            setIsAuthenticated(true);
            setUser(JSON.parse(userData));
        }
    }, []);

    const handleSwitchToLogin = () => {
        setCurrentView('login');
    };

    const handleSwitchToRegister = () => {
        setCurrentView('register');
    };

    const handleSwitchToHomepage = () => {
        setCurrentView('homepage');
    };

    const handleLoginSuccess = (data) => {
        setIsAuthenticated(true);
        setUser(data);
    };

    const handleLogout = () => {
        localStorage.removeItem('isAuthenticated');
        localStorage.removeItem('userData');
        setIsAuthenticated(false);
        setUser(null);
        setCurrentView('homepage');
    };

    // If user is authenticated, show dashboard
    if (isAuthenticated) {
        return (
            <div style={{
                minHeight: '100vh',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
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
                                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                borderRadius: '12px',
                                marginRight: '16px',
                                boxShadow: '0 4px 16px rgba(102, 126, 234, 0.3)'
                            }}>
                                <span style={{ fontSize: '20px', color: 'white' }}>💪</span>
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
                        <button
                            onClick={handleLogout}
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

                        <div style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: '100px',
                            height: '100px',
                            background: 'linear-gradient(135deg, #667eea, #764ba2)',
                            borderRadius: '25px',
                            marginBottom: '32px',
                            boxShadow: '0 12px 40px rgba(102, 126, 234, 0.3)'
                        }}>
                            <span style={{ fontSize: '40px', color: 'white' }}>🎯</span>
                        </div>

                        <h2 style={{
                            color: '#1a202c',
                            fontSize: '40px',
                            fontWeight: '800',
                            marginBottom: '16px',
                            letterSpacing: '-1px',
                            background: 'linear-gradient(135deg, #667eea, #764ba2)',
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

                        <div style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                            gap: '24px',
                            marginTop: '48px'
                        }}>
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
                                 onMouseEnter={(e) => {
                                     e.target.style.transform = 'translateY(-4px)';
                                     e.target.style.boxShadow = '0 16px 40px rgba(102, 126, 234, 0.15)';
                                 }}
                                 onMouseLeave={(e) => {
                                     e.target.style.transform = 'translateY(0)';
                                     e.target.style.boxShadow = 'none';
                                 }}>
                                <div style={{
                                    width: '56px',
                                    height: '56px',
                                    background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                    borderRadius: '14px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    marginBottom: '20px',
                                    boxShadow: '0 8px 24px rgba(102, 126, 234, 0.3)'
                                }}>
                                    <span style={{ fontSize: '24px', color: 'white' }}>🏋️</span>
                                </div>
                                <h3 style={{
                                    color: '#1a202c',
                                    fontSize: '20px',
                                    fontWeight: '700',
                                    marginBottom: '12px',
                                    letterSpacing: '-0.25px'
                                }}>
                                    Track Workouts
                                </h3>
                                <p style={{
                                    color: '#718096',
                                    fontSize: '15px',
                                    margin: '0',
                                    lineHeight: '1.5',
                                    fontWeight: '500'
                                }}>
                                    Log and monitor your daily training sessions with detailed exercise tracking
                                </p>
                            </div>

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
                                 onMouseEnter={(e) => {
                                     e.target.style.transform = 'translateY(-4px)';
                                     e.target.style.boxShadow = '0 16px 40px rgba(102, 126, 234, 0.15)';
                                 }}
                                 onMouseLeave={(e) => {
                                     e.target.style.transform = 'translateY(0)';
                                     e.target.style.boxShadow = 'none';
                                 }}>
                                <div style={{
                                    width: '56px',
                                    height: '56px',
                                    background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                    borderRadius: '14px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    marginBottom: '20px',
                                    boxShadow: '0 8px 24px rgba(102, 126, 234, 0.3)'
                                }}>
                                    <span style={{ fontSize: '24px', color: 'white' }}>📈</span>
                                </div>
                                <h3 style={{
                                    color: '#1a202c',
                                    fontSize: '20px',
                                    fontWeight: '700',
                                    marginBottom: '12px',
                                    letterSpacing: '-0.25px'
                                }}>
                                    Monitor Progress
                                </h3>
                                <p style={{
                                    color: '#718096',
                                    fontSize: '15px',
                                    margin: '0',
                                    lineHeight: '1.5',
                                    fontWeight: '500'
                                }}>
                                    Visualize your fitness journey with comprehensive analytics and insights
                                </p>
                            </div>

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
                                 onMouseEnter={(e) => {
                                     e.target.style.transform = 'translateY(-4px)';
                                     e.target.style.boxShadow = '0 16px 40px rgba(102, 126, 234, 0.15)';
                                 }}
                                 onMouseLeave={(e) => {
                                     e.target.style.transform = 'translateY(0)';
                                     e.target.style.boxShadow = 'none';
                                 }}>
                                <div style={{
                                    width: '56px',
                                    height: '56px',
                                    background: 'linear-gradient(135deg, #667eea, #764ba2)',
                                    borderRadius: '14px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    marginBottom: '20px',
                                    boxShadow: '0 8px 24px rgba(102, 126, 234, 0.3)'
                                }}>
                                    <span style={{ fontSize: '24px', color: 'white' }}>🎯</span>
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
    }

    // If not authenticated, show homepage/login/register forms
    return (
        <div>
            {currentView === 'homepage' ? (
                <Homepage
                    onLogin={handleSwitchToLogin}
                    onRegister={handleSwitchToRegister}
                />
            ) : currentView === 'login' ? (
                <Login
                    onSwitchToRegister={handleSwitchToRegister}
                    onLoginSuccess={handleLoginSuccess}
                />
            ) : (
                <Register
                    onSwitchToLogin={handleSwitchToLogin}
                />
            )}
        </div>
    );
};

export default App;