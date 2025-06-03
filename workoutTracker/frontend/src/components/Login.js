import React, { useState } from 'react';
import { toast } from 'react-toastify';

const Login = ({ onSwitchToRegister, onLoginSuccess }) => {
    const [formData, setFormData] = useState({
        email: '',
        password: ''
    });
    const [errors, setErrors] = useState({});
    const [isLoading, setIsLoading] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        // Clear error when user starts typing
        if (errors[name]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const validateForm = () => {
        const newErrors = {};

        if (!formData.email.trim()) {
            newErrors.email = 'Email is required';
        } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
            newErrors.email = 'Please enter a valid email';
        }

        if (!formData.password) {
            newErrors.password = 'Password is required';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        setIsLoading(true);

        try {
            const response = await fetch('http://localhost:8082/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    email: formData.email,
                    password: formData.password
                }),
            });

            if (response.ok) {
                const userData = await response.json();
                console.log('Login successful:', userData);

                // Store user data in localStorage
                localStorage.setItem('userData', JSON.stringify(userData));
                localStorage.setItem('workout_tracker_token', userData.token);
                localStorage.setItem('isAuthenticated', 'true');

                console.log('Token saved:', localStorage.getItem('workout_tracker_token'));

                toast.success("Login successful!", {
                    position: "top-right",
                    autoClose: 3000,
                    hideProgressBar: false,
                    closeOnClick: true,
                    pauseOnHover: true,
                    draggable: true
                });

                if (onLoginSuccess) {
                    onLoginSuccess(userData);
                }
            } else {
                const errorData = await response.json();
                setErrors({ general: errorData.message || 'Invalid email or password' });
            }
        } catch (error) {
            console.error('Error:', error);
            setErrors({ general: 'Connection error. Please try again.' });
        } finally {
            setIsLoading(false);
        }
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
            {/* Background decorative elements */}
            <div style={{
                position: 'absolute',
                top: '10%',
                left: '10%',
                width: '200px',
                height: '200px',
                background: 'rgba(255,255,255,0.1)',
                borderRadius: '50%',
                filter: 'blur(60px)',
                animation: 'float 6s ease-in-out infinite'
            }}></div>
            <div style={{
                position: 'absolute',
                bottom: '20%',
                right: '15%',
                width: '150px',
                height: '150px',
                background: 'rgba(255,255,255,0.05)',
                borderRadius: '50%',
                filter: 'blur(40px)',
                animation: 'float 8s ease-in-out infinite reverse'
            }}></div>

            <div style={{
                backgroundColor: 'rgba(255,255,255,0.95)',
                backdropFilter: 'blur(20px)',
                borderRadius: '24px',
                boxShadow: '0 32px 64px rgba(0,0,0,0.15), 0 0 0 1px rgba(255,255,255,0.1)',
                padding: '48px',
                width: '100%',
                maxWidth: '460px',
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
                    borderRadius: '24px 24px 0 0'
                }}></div>

                <div style={{
                    textAlign: 'center',
                    marginBottom: '40px'
                }}>
                    <div>
                        <img
                            src="/icon.png"
                            alt="WorkoutTracker Logo"
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
                        color: '#1a202c',
                        fontSize: '32px',
                        fontWeight: '800',
                        marginBottom: '8px',
                        letterSpacing: '-0.5px'
                    }}>Welcome Back</h1>
                    <p style={{
                        color: '#718096',
                        fontSize: '16px',
                        margin: '0',
                        fontWeight: '500'
                    }}>Sign in to continue your fitness journey</p>
                </div>

                <div>
                {errors.general && (
                        <div style={{
                            background: 'linear-gradient(135deg, #fed7d7, #feb2b2)',
                            color: '#c53030',
                            padding: '16px',
                            borderRadius: '12px',
                            marginBottom: '24px',
                            fontSize: '14px',
                            textAlign: 'center',
                            fontWeight: '500',
                            border: '1px solid rgba(197, 48, 48, 0.2)'
                        }}>
                            {errors.general}
                        </div>
                    )}

                    <div style={{ marginBottom: '24px' }}>
                        <label style={{
                            display: 'block',
                            marginBottom: '8px',
                            fontWeight: '600',
                            color: '#2d3748',
                            fontSize: '14px',
                            letterSpacing: '0.025em'
                        }}>Email Address</label>
                        <div style={{ position: 'relative' }}>
                            <input
                                type="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                placeholder="Enter your email"
                                style={{
                                    width: '100%',
                                    padding: '16px 20px',
                                    border: errors.email ? '2px solid #e53e3e' : '2px solid #e2e8f0',
                                    borderRadius: '12px',
                                    fontSize: '16px',
                                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                    outline: 'none',
                                    boxSizing: 'border-box',
                                    backgroundColor: '#fafafa',
                                    fontWeight: '500'
                                }}
                                onFocus={(e) => {
                                    e.target.style.borderColor = '#667eea';
                                    e.target.style.backgroundColor = '#ffffff';
                                    e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
                                }}
                                onBlur={(e) => {
                                    e.target.style.borderColor = errors.email ? '#e53e3e' : '#e2e8f0';
                                    e.target.style.backgroundColor = '#fafafa';
                                    e.target.style.boxShadow = 'none';
                                }}
                            />
                        </div>
                        {errors.email && (
                            <span style={{
                                color: '#e53e3e',
                                fontSize: '13px',
                                marginTop: '6px',
                                display: 'block',
                                fontWeight: '500'
                            }}>{errors.email}</span>
                        )}
                    </div>

                    <div style={{ marginBottom: '32px' }}>
                        <label style={{
                            display: 'block',
                            marginBottom: '8px',
                            fontWeight: '600',
                            color: '#2d3748',
                            fontSize: '14px',
                            letterSpacing: '0.025em'
                        }}>Password</label>
                        <div style={{ position: 'relative' }}>
                            <input
                                type="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                placeholder="Enter your password"
                                style={{
                                    width: '100%',
                                    padding: '16px 20px',
                                    border: errors.password ? '2px solid #e53e3e' : '2px solid #e2e8f0',
                                    borderRadius: '12px',
                                    fontSize: '16px',
                                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                    outline: 'none',
                                    boxSizing: 'border-box',
                                    backgroundColor: '#fafafa',
                                    fontWeight: '500'
                                }}
                                onFocus={(e) => {
                                    e.target.style.borderColor = '#667eea';
                                    e.target.style.backgroundColor = '#ffffff';
                                    e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
                                }}
                                onBlur={(e) => {
                                    e.target.style.borderColor = errors.password ? '#e53e3e' : '#e2e8f0';
                                    e.target.style.backgroundColor = '#fafafa';
                                    e.target.style.boxShadow = 'none';
                                }}
                            />
                        </div>
                        {errors.password && (
                            <span style={{
                                color: '#e53e3e',
                                fontSize: '13px',
                                marginTop: '6px',
                                display: 'block',
                                fontWeight: '500'
                            }}>{errors.password}</span>
                        )}
                    </div>

                    <button
                        onClick={handleSubmit}
                        disabled={isLoading}
                        style={{
                            width: '100%',
                            padding: '18px',
                            background: isLoading ?
                                'linear-gradient(135deg, #a0aec0, #cbd5e0)' :
                                'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                            color: 'white',
                            border: 'none',
                            borderRadius: '12px',
                            fontSize: '16px',
                            fontWeight: '700',
                            cursor: isLoading ? 'not-allowed' : 'pointer',
                            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                            marginBottom: '24px',
                            boxShadow: isLoading ? 'none' : '0 8px 32px rgba(102, 126, 234, 0.3)',
                            letterSpacing: '0.025em',
                            transform: isLoading ? 'none' : 'translateY(0)',
                            position: 'relative',
                            overflow: 'hidden'
                        }}
                        onMouseEnter={(e) => {
                            if (!isLoading) {
                                e.target.style.transform = 'translateY(-2px)';
                                e.target.style.boxShadow = '0 12px 40px rgba(102, 126, 234, 0.4)';
                            }
                        }}
                        onMouseLeave={(e) => {
                            if (!isLoading) {
                                e.target.style.transform = 'translateY(0)';
                                e.target.style.boxShadow = '0 8px 32px rgba(102, 126, 234, 0.3)';
                            }
                        }}
                    >
                        {isLoading ? (
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                <div style={{
                                    width: '20px',
                                    height: '20px',
                                    border: '2px solid rgba(255,255,255,0.3)',
                                    borderTop: '2px solid white',
                                    borderRadius: '50%',
                                    animation: 'spin 1s linear infinite',
                                    marginRight: '8px'
                                }}></div>
                                Signing In...
                            </div>
                        ) : 'Sign In'}
                    </button>

                    <div style={{
                        textAlign: 'center',
                        marginBottom: '24px'
                    }}>
                    </div>
                </div>

                <div style={{
                    textAlign: 'center',
                    color: '#718096',
                    fontSize: '15px',
                    borderTop: '1px solid #e2e8f0',
                    paddingTop: '24px'
                }}>
                    <p style={{ margin: '0', fontWeight: '500' }}>
                        Don't have an account?{' '}
                        <button
                            type="button"
                            onClick={onSwitchToRegister}
                            style={{
                                background: 'none',
                                border: 'none',
                                color: '#667eea',
                                cursor: 'pointer',
                                fontSize: '15px',
                                fontWeight: '700',
                                textDecoration: 'none',
                                transition: 'color 0.2s'
                            }}
                            onMouseEnter={(e) => {
                                e.target.style.color = '#5a67d8';
                                e.target.style.textDecoration = 'underline';
                            }}
                            onMouseLeave={(e) => {
                                e.target.style.color = '#667eea';
                                e.target.style.textDecoration = 'none';
                            }}
                        >
                            Sign up
                        </button>
                    </p>
                </div>
            </div>

            <style>
                {`
          @keyframes float {
            0%, 100% { transform: translateY(0px); }
            50% { transform: translateY(-20px); }
          }
          
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}
            </style>
        </div>
    );
};

export default Login;