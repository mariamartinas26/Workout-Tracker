import React from 'react';

const Homepage = ({ onLogin, onRegister }) => {
    return (
        <div style={{
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #334155 100%)',
            color: 'white',
            fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, sans-serif",
            position: 'relative',
            overflow: 'hidden'
        }}>
            {/* Background Elements */}
            <div style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundImage: `
          radial-gradient(circle at 20% 30%, rgba(59, 130, 246, 0.15) 0%, transparent 50%),
          radial-gradient(circle at 80% 70%, rgba(147, 51, 234, 0.1) 0%, transparent 50%),
          radial-gradient(circle at 40% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%)
        `,
                animation: 'backgroundFloat 20s ease-in-out infinite'
            }}></div>

            {/* Geometric Background Pattern */}
            <div style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundImage: `
          linear-gradient(45deg, transparent 48%, rgba(255,255,255,0.02) 49%, rgba(255,255,255,0.02) 51%, transparent 52%),
          linear-gradient(-45deg, transparent 48%, rgba(255,255,255,0.02) 49%, rgba(255,255,255,0.02) 51%, transparent 52%)
        `,
                backgroundSize: '60px 60px',
                opacity: 0.3
            }}></div>

            {/* Navigation */}
            <nav style={{
                position: 'relative',
                zIndex: 10,
                padding: '24px 32px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                background: 'rgba(15, 23, 42, 0.8)',
                backdropFilter: 'blur(20px)',
                borderBottom: '1px solid rgba(255, 255, 255, 0.1)'
            }}>
                {/* Logo */}
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px'
                }}>
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        width: '40px',
                        height: '40px',
                        background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
                        borderRadius: '10px',
                        boxShadow: '0 8px 25px rgba(59, 130, 246, 0.3)'
                    }}>
            <span style={{
                fontSize: '20px',
                fontWeight: 'bold',
                color: 'white'
            }}>💪</span>
                    </div>
                    <h1 style={{
                        fontSize: '28px',
                        fontWeight: '800',
                        margin: 0,
                        background: 'linear-gradient(135deg, #ffffff, #e2e8f0)',
                        backgroundClip: 'text',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent',
                        letterSpacing: '-0.5px'
                    }}>STRONG</h1>
                </div>

                {/* Navigation Links */}
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '32px'
                }}>
                    <button style={{
                        background: 'none',
                        border: 'none',
                        color: '#e2e8f0',
                        fontSize: '16px',
                        fontWeight: '500',
                        cursor: 'pointer',
                        transition: 'color 0.3s ease',
                        fontFamily: 'inherit'
                    }}
                            onMouseEnter={(e) => e.target.style.color = '#ffffff'}
                            onMouseLeave={(e) => e.target.style.color = '#e2e8f0'}>
                        Learn More
                    </button>

                    <button style={{
                        background: 'none',
                        border: 'none',
                        color: '#e2e8f0',
                        fontSize: '16px',
                        fontWeight: '500',
                        cursor: 'pointer',
                        transition: 'color 0.3s ease',
                        fontFamily: 'inherit'
                    }}
                            onMouseEnter={(e) => e.target.style.color = '#ffffff'}
                            onMouseLeave={(e) => e.target.style.color = '#e2e8f0'}>
                        App Store
                    </button>

                    <button style={{
                        background: 'none',
                        border: 'none',
                        color: '#e2e8f0',
                        fontSize: '16px',
                        fontWeight: '500',
                        cursor: 'pointer',
                        transition: 'color 0.3s ease',
                        fontFamily: 'inherit'
                    }}
                            onMouseEnter={(e) => e.target.style.color = '#ffffff'}
                            onMouseLeave={(e) => e.target.style.color = '#e2e8f0'}>
                        Google Play
                    </button>
                </div>
            </nav>

            {/* Main Content */}
            <main style={{
                position: 'relative',
                zIndex: 5,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                minHeight: 'calc(100vh - 100px)',
                padding: '0 32px',
                textAlign: 'center'
            }}>
                {/* Hero Text */}
                <div style={{
                    maxWidth: '900px',
                    marginBottom: '60px'
                }}>
                    <h1 style={{
                        fontSize: 'clamp(48px, 8vw, 120px)',
                        fontWeight: '800',
                        lineHeight: '0.9',
                        marginBottom: '40px',
                        letterSpacing: '-2px',
                        background: 'linear-gradient(135deg, #ffffff 0%, #e2e8f0 100%)',
                        backgroundClip: 'text',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent'
                    }}>
                        Think less.
                        <br />
                        <span style={{
                            background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
                            backgroundClip: 'text',
                            WebkitBackgroundClip: 'text',
                            WebkitTextFillColor: 'transparent'
                        }}>Lift more.</span>
                    </h1>

                    <p style={{
                        fontSize: 'clamp(18px, 3vw, 24px)',
                        fontWeight: '500',
                        lineHeight: '1.6',
                        color: '#cbd5e1',
                        marginBottom: '20px',
                        maxWidth: '700px',
                        margin: '0 auto 20px auto'
                    }}>
                        Strong is the simplest, most intuitive workout tracking experience.
                    </p>

                    <p style={{
                        fontSize: 'clamp(16px, 2.5vw, 20px)',
                        fontWeight: '500',
                        color: '#94a3b8',
                        margin: 0
                    }}>
                        Trusted by over 3 million users worldwide.
                    </p>
                </div>

                {/* CTA Buttons */}
                <div style={{
                    display: 'flex',
                    gap: '20px',
                    flexWrap: 'wrap',
                    justifyContent: 'center',
                    alignItems: 'center'
                }}>
                    <button
                        onClick={onRegister}
                        style={{
                            padding: '18px 40px',
                            background: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
                            color: 'white',
                            border: 'none',
                            borderRadius: '16px',
                            fontSize: '18px',
                            fontWeight: '700',
                            cursor: 'pointer',
                            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                            boxShadow: '0 12px 40px rgba(59, 130, 246, 0.3)',
                            letterSpacing: '0.025em',
                            fontFamily: 'inherit',
                            position: 'relative',
                            overflow: 'hidden'
                        }}
                        onMouseEnter={(e) => {
                            e.target.style.transform = 'translateY(-3px)';
                            e.target.style.boxShadow = '0 16px 50px rgba(59, 130, 246, 0.4)';
                        }}
                        onMouseLeave={(e) => {
                            e.target.style.transform = 'translateY(0)';
                            e.target.style.boxShadow = '0 12px 40px rgba(59, 130, 246, 0.3)';
                        }}
                    >
                        Get Started Free
                    </button>

                    <button
                        onClick={onLogin}
                        style={{
                            padding: '18px 40px',
                            background: 'rgba(255, 255, 255, 0.1)',
                            color: 'white',
                            border: '2px solid rgba(255, 255, 255, 0.2)',
                            borderRadius: '16px',
                            fontSize: '18px',
                            fontWeight: '700',
                            cursor: 'pointer',
                            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                            backdropFilter: 'blur(10px)',
                            letterSpacing: '0.025em',
                            fontFamily: 'inherit'
                        }}
                        onMouseEnter={(e) => {
                            e.target.style.background = 'rgba(255, 255, 255, 0.2)';
                            e.target.style.borderColor = 'rgba(255, 255, 255, 0.4)';
                            e.target.style.transform = 'translateY(-2px)';
                        }}
                        onMouseLeave={(e) => {
                            e.target.style.background = 'rgba(255, 255, 255, 0.1)';
                            e.target.style.borderColor = 'rgba(255, 255, 255, 0.2)';
                            e.target.style.transform = 'translateY(0)';
                        }}
                    >
                        Sign In
                    </button>
                </div>

                {/* Features Preview */}
                <div style={{
                    marginTop: '100px',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                    gap: '30px',
                    maxWidth: '1000px',
                    width: '100%'
                }}>
                    {/* Feature 1 */}
                    <div style={{
                        background: 'rgba(255, 255, 255, 0.05)',
                        backdropFilter: 'blur(20px)',
                        border: '1px solid rgba(255, 255, 255, 0.1)',
                        borderRadius: '20px',
                        padding: '32px',
                        textAlign: 'center',
                        transition: 'all 0.3s ease',
                        cursor: 'pointer'
                    }}
                         onMouseEnter={(e) => {
                             e.target.style.transform = 'translateY(-8px)';
                             e.target.style.background = 'rgba(255, 255, 255, 0.08)';
                         }}
                         onMouseLeave={(e) => {
                             e.target.style.transform = 'translateY(0)';
                             e.target.style.background = 'rgba(255, 255, 255, 0.05)';
                         }}>
                        <div style={{
                            width: '60px',
                            height: '60px',
                            background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
                            borderRadius: '16px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            margin: '0 auto 20px auto',
                            boxShadow: '0 8px 25px rgba(59, 130, 246, 0.3)'
                        }}>
                            <span style={{ fontSize: '24px', color: 'white' }}>⚡</span>
                        </div>
                        <h3 style={{
                            fontSize: '20px',
                            fontWeight: '700',
                            marginBottom: '12px',
                            color: 'white'
                        }}>Lightning Fast</h3>
                        <p style={{
                            fontSize: '15px',
                            color: '#94a3b8',
                            lineHeight: '1.5',
                            margin: 0
                        }}>
                            Log your workouts in seconds with our intuitive interface
                        </p>
                    </div>

                    {/* Feature 2 */}
                    <div style={{
                        background: 'rgba(255, 255, 255, 0.05)',
                        backdropFilter: 'blur(20px)',
                        border: '1px solid rgba(255, 255, 255, 0.1)',
                        borderRadius: '20px',
                        padding: '32px',
                        textAlign: 'center',
                        transition: 'all 0.3s ease',
                        cursor: 'pointer'
                    }}
                         onMouseEnter={(e) => {
                             e.target.style.transform = 'translateY(-8px)';
                             e.target.style.background = 'rgba(255, 255, 255, 0.08)';
                         }}
                         onMouseLeave={(e) => {
                             e.target.style.transform = 'translateY(0)';
                             e.target.style.background = 'rgba(255, 255, 255, 0.05)';
                         }}>
                        <div style={{
                            width: '60px',
                            height: '60px',
                            background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
                            borderRadius: '16px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            margin: '0 auto 20px auto',
                            boxShadow: '0 8px 25px rgba(59, 130, 246, 0.3)'
                        }}>
                            <span style={{ fontSize: '24px', color: 'white' }}>📊</span>
                        </div>
                        <h3 style={{
                            fontSize: '20px',
                            fontWeight: '700',
                            marginBottom: '12px',
                            color: 'white'
                        }}>Smart Analytics</h3>
                        <p style={{
                            fontSize: '15px',
                            color: '#94a3b8',
                            lineHeight: '1.5',
                            margin: 0
                        }}>
                            Track your progress with detailed insights and analytics
                        </p>
                    </div>

                    {/* Feature 3 */}
                    <div style={{
                        background: 'rgba(255, 255, 255, 0.05)',
                        backdropFilter: 'blur(20px)',
                        border: '1px solid rgba(255, 255, 255, 0.1)',
                        borderRadius: '20px',
                        padding: '32px',
                        textAlign: 'center',
                        transition: 'all 0.3s ease',
                        cursor: 'pointer'
                    }}
                         onMouseEnter={(e) => {
                             e.target.style.transform = 'translateY(-8px)';
                             e.target.style.background = 'rgba(255, 255, 255, 0.08)';
                         }}
                         onMouseLeave={(e) => {
                             e.target.style.transform = 'translateY(0)';
                             e.target.style.background = 'rgba(255, 255, 255, 0.05)';
                         }}>
                        <div style={{
                            width: '60px',
                            height: '60px',
                            background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
                            borderRadius: '16px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            margin: '0 auto 20px auto',
                            boxShadow: '0 8px 25px rgba(59, 130, 246, 0.3)'
                        }}>
                            <span style={{ fontSize: '24px', color: 'white' }}>🎯</span>
                        </div>
                        <h3 style={{
                            fontSize: '20px',
                            fontWeight: '700',
                            marginBottom: '12px',
                            color: 'white'
                        }}>Goal Focused</h3>
                        <p style={{
                            fontSize: '15px',
                            color: '#94a3b8',
                            lineHeight: '1.5',
                            margin: 0
                        }}>
                            Set and achieve your fitness goals with personalized plans
                        </p>
                    </div>
                </div>
            </main>

            {/* CSS Animations */}
            <style>
                {`
          @keyframes backgroundFloat {
            0%, 100% {
              transform: translateX(0) translateY(0);
            }
            25% {
              transform: translateX(5px) translateY(-10px);
            }
            50% {
              transform: translateX(-5px) translateY(-5px);
            }
            75% {
              transform: translateX(-10px) translateY(-15px);
            }
          }

          @media (max-width: 768px) {
            nav {
              padding: 16px 20px !important;
            }
            
            main {
              padding: 0 20px !important;
            }
          }
        `}
            </style>
        </div>
    );
};

export default Homepage;