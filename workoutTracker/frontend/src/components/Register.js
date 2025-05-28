import React, { useState } from 'react';

const Register = ({ onSwitchToLogin }) => {
    const [formData, setFormData] = useState({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        confirmPassword: ''
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

        if (!formData.firstName.trim()) {
            newErrors.firstName = 'First name is required';
        }

        if (!formData.lastName.trim()) {
            newErrors.lastName = 'Last name is required';
        }

        if (!formData.email.trim()) {
            newErrors.email = 'Email is required';
        } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
            newErrors.email = 'Please enter a valid email';
        }

        if (!formData.password) {
            newErrors.password = 'Password is required';
        } else if (formData.password.length < 6) {
            newErrors.password = 'Password must be at least 6 characters';
        }

        if (!formData.confirmPassword) {
            newErrors.confirmPassword = 'Please confirm your password';
        } else if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = 'Passwords do not match';
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
            const response = await fetch('http://localhost:8080/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    firstName: formData.firstName,
                    lastName: formData.lastName,
                    email: formData.email,
                    password: formData.password
                }),
            });

            if (response.ok) {
                const data = await response.json();
                console.log('Registration successful:', data);
                alert('Account created successfully! You can now sign in.');
                onSwitchToLogin();
            } else {
                const errorData = await response.json();
                setErrors({ general: errorData.message || 'Registration failed' });
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
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, sans-serif",
            position: 'relative',
            overflow: 'hidden'
        }}>
            {/* Enhanced Background Animation */}
            <div style={{
                position: 'absolute',
                top: '-50%',
                left: '-50%',
                width: '200%',
                height: '200%',
                background: `
          radial-gradient(circle at 20% 50%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
          radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.15) 0%, transparent 50%),
          radial-gradient(circle at 40% 80%, rgba(120, 119, 198, 0.2) 0%, transparent 50%)
        `,
                animation: 'drift 20s ease-in-out infinite'
            }}></div>

            {/* Floating Elements */}
            <div style={{
                position: 'absolute',
                top: '15%',
                right: '10%',
                width: '200px',
                height: '200px',
                background: 'rgba(255,255,255,0.1)',
                borderRadius: '50%',
                filter: 'blur(60px)',
                animation: 'float 8s ease-in-out infinite'
            }}></div>
            <div style={{
                position: 'absolute',
                bottom: '20%',
                left: '8%',
                width: '150px',
                height: '150px',
                background: 'rgba(255,255,255,0.05)',
                borderRadius: '50%',
                filter: 'blur(40px)',
                animation: 'float 10s ease-in-out infinite reverse'
            }}></div>

            <div style={{
                backgroundColor: 'rgba(255,255,255,0.95)',
                backdropFilter: 'blur(25px)',
                borderRadius: '28px',
                boxShadow: `
          0 40px 80px rgba(0,0,0,0.12),
          0 0 0 1px rgba(255,255,255,0.2),
          inset 0 1px 0 rgba(255,255,255,0.3)
        `,
                padding: '50px',
                width: '100%',
                maxWidth: '540px',
                position: 'relative',
                overflow: 'hidden',
                border: '1px solid rgba(255,255,255,0.18)'
            }}>
                {/* Gradient Top Border */}
                <div style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    right: 0,
                    height: '4px',
                    background: 'linear-gradient(90deg, #667eea, #764ba2, #f093fb, #667eea)',
                    borderRadius: '28px 28px 0 0',
                    backgroundSize: '200% 100%',
                    animation: 'gradientShift 3s ease-in-out infinite'
                }}></div>

                {/* Header Section */}
                <div style={{
                    textAlign: 'center',
                    marginBottom: '40px'
                }}>
                    <div style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        width: '90px',
                        height: '90px',
                        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                        borderRadius: '22px',
                        marginBottom: '24px',
                        boxShadow: '0 12px 40px rgba(102, 126, 234, 0.35)',
                        position: 'relative'
                    }}>
                        <div style={{
                            position: 'absolute',
                            inset: '2px',
                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                            borderRadius: '20px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                        }}>
              <span style={{
                  fontSize: '36px',
                  color: 'white',
                  filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.2))'
              }}>🚀</span>
                        </div>
                    </div>

                    <h1 style={{
                        color: '#1a202c',
                        fontSize: '36px',
                        fontWeight: '800',
                        marginBottom: '12px',
                        letterSpacing: '-0.8px',
                        background: 'linear-gradient(135deg, #1a202c 0%, #4a5568 100%)',
                        backgroundClip: 'text',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent'
                    }}>Create Account</h1>

                    <p style={{
                        color: '#718096',
                        fontSize: '17px',
                        margin: '0',
                        fontWeight: '500',
                        lineHeight: '1.4'
                    }}>Join thousands transforming their fitness journey</p>
                </div>

                {/* Form Section */}
                <div>
                    {errors.general && (
                        <div style={{
                            background: 'linear-gradient(135deg, rgba(254, 215, 215, 0.9), rgba(254, 178, 178, 0.8))',
                            color: '#c53030',
                            padding: '18px 20px',
                            borderRadius: '14px',
                            marginBottom: '28px',
                            fontSize: '14px',
                            textAlign: 'center',
                            fontWeight: '600',
                            border: '1px solid rgba(197, 48, 48, 0.15)',
                            boxShadow: '0 4px 12px rgba(197, 48, 48, 0.1)'
                        }}>
                            {errors.general}
                        </div>
                    )}

                    {/* Name Fields Row */}
                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: '1fr 1fr',
                        gap: '20px',
                        marginBottom: '28px'
                    }}>
                        {/* First Name */}
                        <div>
                            <label style={{
                                display: 'block',
                                marginBottom: '10px',
                                fontWeight: '700',
                                color: '#2d3748',
                                fontSize: '14px',
                                letterSpacing: '0.025em',
                                textTransform: 'uppercase'
                            }}>First Name</label>
                            <div style={{ position: 'relative' }}>
                                <input
                                    type="text"
                                    name="firstName"
                                    value={formData.firstName}
                                    onChange={handleChange}
                                    placeholder="Enter first name"
                                    style={{
                                        width: '100%',
                                        padding: '18px 22px',
                                        border: errors.firstName ? '2px solid #e53e3e' : '2px solid #e2e8f0',
                                        borderRadius: '14px',
                                        fontSize: '16px',
                                        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                        outline: 'none',
                                        boxSizing: 'border-box',
                                        backgroundColor: '#fafafa',
                                        fontWeight: '500',
                                        fontFamily: 'inherit'
                                    }}
                                    onFocus={(e) => {
                                        e.target.style.borderColor = '#667eea';
                                        e.target.style.backgroundColor = '#ffffff';
                                        e.target.style.boxShadow = '0 0 0 4px rgba(102, 126, 234, 0.1)';
                                        e.target.style.transform = 'translateY(-1px)';
                                    }}
                                    onBlur={(e) => {
                                        e.target.style.borderColor = errors.firstName ? '#e53e3e' : '#e2e8f0';
                                        e.target.style.backgroundColor = '#fafafa';
                                        e.target.style.boxShadow = 'none';
                                        e.target.style.transform = 'translateY(0)';
                                    }}
                                />
                            </div>
                            {errors.firstName && (
                                <span style={{
                                    color: '#e53e3e',
                                    fontSize: '13px',
                                    marginTop: '8px',
                                    display: 'block',
                                    fontWeight: '600'
                                }}>{errors.firstName}</span>
                            )}
                        </div>

                        {/* Last Name */}
                        <div>
                            <label style={{
                                display: 'block',
                                marginBottom: '10px',
                                fontWeight: '700',
                                color: '#2d3748',
                                fontSize: '14px',
                                letterSpacing: '0.025em',
                                textTransform: 'uppercase'
                            }}>Last Name</label>
                            <div style={{ position: 'relative' }}>
                                <input
                                    type="text"
                                    name="lastName"
                                    value={formData.lastName}
                                    onChange={handleChange}
                                    placeholder="Enter last name"
                                    style={{
                                        width: '100%',
                                        padding: '18px 22px',
                                        border: errors.lastName ? '2px solid #e53e3e' : '2px solid #e2e8f0',
                                        borderRadius: '14px',
                                        fontSize: '16px',
                                        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                        outline: 'none',
                                        boxSizing: 'border-box',
                                        backgroundColor: '#fafafa',
                                        fontWeight: '500',
                                        fontFamily: 'inherit'
                                    }}
                                    onFocus={(e) => {
                                        e.target.style.borderColor = '#667eea';
                                        e.target.style.backgroundColor = '#ffffff';
                                        e.target.style.boxShadow = '0 0 0 4px rgba(102, 126, 234, 0.1)';
                                        e.target.style.transform = 'translateY(-1px)';
                                    }}
                                    onBlur={(e) => {
                                        e.target.style.borderColor = errors.lastName ? '#e53e3e' : '#e2e8f0';
                                        e.target.style.backgroundColor = '#fafafa';
                                        e.target.style.boxShadow = 'none';
                                        e.target.style.transform = 'translateY(0)';
                                    }}
                                />
                            </div>
                            {errors.lastName && (
                                <span style={{
                                    color: '#e53e3e',
                                    fontSize: '13px',
                                    marginTop: '8px',
                                    display: 'block',
                                    fontWeight: '600'
                                }}>{errors.lastName}</span>
                            )}
                        </div>
                    </div>

                    {/* Email Field */}
                    <div style={{ marginBottom: '28px' }}>
                        <label style={{
                            display: 'block',
                            marginBottom: '10px',
                            fontWeight: '700',
                            color: '#2d3748',
                            fontSize: '14px',
                            letterSpacing: '0.025em',
                            textTransform: 'uppercase'
                        }}>Email Address</label>
                        <div style={{ position: 'relative' }}>
                            <input
                                type="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                placeholder="Enter your email address"
                                style={{
                                    width: '100%',
                                    padding: '18px 22px',
                                    border: errors.email ? '2px solid #e53e3e' : '2px solid #e2e8f0',
                                    borderRadius: '14px',
                                    fontSize: '16px',
                                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                    outline: 'none',
                                    boxSizing: 'border-box',
                                    backgroundColor: '#fafafa',
                                    fontWeight: '500',
                                    fontFamily: 'inherit'
                                }}
                                onFocus={(e) => {
                                    e.target.style.borderColor = '#667eea';
                                    e.target.style.backgroundColor = '#ffffff';
                                    e.target.style.boxShadow = '0 0 0 4px rgba(102, 126, 234, 0.1)';
                                    e.target.style.transform = 'translateY(-1px)';
                                }}
                                onBlur={(e) => {
                                    e.target.style.borderColor = errors.email ? '#e53e3e' : '#e2e8f0';
                                    e.target.style.backgroundColor = '#fafafa';
                                    e.target.style.boxShadow = 'none';
                                    e.target.style.transform = 'translateY(0)';
                                }}
                            />
                        </div>
                        {errors.email && (
                            <span style={{
                                color: '#e53e3e',
                                fontSize: '13px',
                                marginTop: '8px',
                                display: 'block',
                                fontWeight: '600'
                            }}>{errors.email}</span>
                        )}
                    </div>

                    {/* Password Field */}
                    <div style={{ marginBottom: '28px' }}>
                        <label style={{
                            display: 'block',
                            marginBottom: '10px',
                            fontWeight: '700',
                            color: '#2d3748',
                            fontSize: '14px',
                            letterSpacing: '0.025em',
                            textTransform: 'uppercase'
                        }}>Password</label>
                        <div style={{ position: 'relative' }}>
                            <input
                                type="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                placeholder="Create a strong password"
                                style={{
                                    width: '100%',
                                    padding: '18px 22px',
                                    border: errors.password ? '2px solid #e53e3e' : '2px solid #e2e8f0',
                                    borderRadius: '14px',
                                    fontSize: '16px',
                                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                    outline: 'none',
                                    boxSizing: 'border-box',
                                    backgroundColor: '#fafafa',
                                    fontWeight: '500',
                                    fontFamily: 'inherit'
                                }}
                                onFocus={(e) => {
                                    e.target.style.borderColor = '#667eea';
                                    e.target.style.backgroundColor = '#ffffff';
                                    e.target.style.boxShadow = '0 0 0 4px rgba(102, 126, 234, 0.1)';
                                    e.target.style.transform = 'translateY(-1px)';
                                }}
                                onBlur={(e) => {
                                    e.target.style.borderColor = errors.password ? '#e53e3e' : '#e2e8f0';
                                    e.target.style.backgroundColor = '#fafafa';
                                    e.target.style.boxShadow = 'none';
                                    e.target.style.transform = 'translateY(0)';
                                }}
                            />
                        </div>
                        {errors.password && (
                            <span style={{
                                color: '#e53e3e',
                                fontSize: '13px',
                                marginTop: '8px',
                                display: 'block',
                                fontWeight: '600'
                            }}>{errors.password}</span>
                        )}
                    </div>

                    {/* Confirm Password Field */}
                    <div style={{ marginBottom: '36px' }}>
                        <label style={{
                            display: 'block',
                            marginBottom: '10px',
                            fontWeight: '700',
                            color: '#2d3748',
                            fontSize: '14px',
                            letterSpacing: '0.025em',
                            textTransform: 'uppercase'
                        }}>Confirm Password</label>
                        <div style={{ position: 'relative' }}>
                            <input
                                type="password"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                placeholder="Confirm your password"
                                style={{
                                    width: '100%',
                                    padding: '18px 22px',
                                    border: errors.confirmPassword ? '2px solid #e53e3e' : '2px solid #e2e8f0',
                                    borderRadius: '14px',
                                    fontSize: '16px',
                                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                    outline: 'none',
                                    boxSizing: 'border-box',
                                    backgroundColor: '#fafafa',
                                    fontWeight: '500',
                                    fontFamily: 'inherit'
                                }}
                                onFocus={(e) => {
                                    e.target.style.borderColor = '#667eea';
                                    e.target.style.backgroundColor = '#ffffff';
                                    e.target.style.boxShadow = '0 0 0 4px rgba(102, 126, 234, 0.1)';
                                    e.target.style.transform = 'translateY(-1px)';
                                }}
                                onBlur={(e) => {
                                    e.target.style.borderColor = errors.confirmPassword ? '#e53e3e' : '#e2e8f0';
                                    e.target.style.backgroundColor = '#fafafa';
                                    e.target.style.boxShadow = 'none';
                                    e.target.style.transform = 'translateY(0)';
                                }}
                            />
                        </div>
                        {errors.confirmPassword && (
                            <span style={{
                                color: '#e53e3e',
                                fontSize: '13px',
                                marginTop: '8px',
                                display: 'block',
                                fontWeight: '600'
                            }}>{errors.confirmPassword}</span>
                        )}
                    </div>

                    {/* Submit Button */}
                    <button
                        onClick={handleSubmit}
                        disabled={isLoading}
                        style={{
                            width: '100%',
                            padding: '20px',
                            background: isLoading ?
                                'linear-gradient(135deg, #a0aec0, #cbd5e0)' :
                                'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                            color: 'white',
                            border: 'none',
                            borderRadius: '14px',
                            fontSize: '17px',
                            fontWeight: '700',
                            cursor: isLoading ? 'not-allowed' : 'pointer',
                            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                            marginBottom: '28px',
                            boxShadow: isLoading ? 'none' : '0 12px 40px rgba(102, 126, 234, 0.35)',
                            letterSpacing: '0.025em',
                            position: 'relative',
                            overflow: 'hidden',
                            fontFamily: 'inherit'
                        }}
                        onMouseEnter={(e) => {
                            if (!isLoading) {
                                e.target.style.transform = 'translateY(-3px)';
                                e.target.style.boxShadow = '0 16px 50px rgba(102, 126, 234, 0.45)';
                            }
                        }}
                        onMouseLeave={(e) => {
                            if (!isLoading) {
                                e.target.style.transform = 'translateY(0)';
                                e.target.style.boxShadow = '0 12px 40px rgba(102, 126, 234, 0.35)';
                            }
                        }}
                    >
                        {isLoading ? (
                            <div style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: '12px'
                            }}>
                                <div style={{
                                    width: '20px',
                                    height: '20px',
                                    border: '2px solid rgba(255,255,255,0.25)',
                                    borderTop: '2px solid white',
                                    borderRadius: '50%',
                                    animation: 'spin 1s linear infinite'
                                }}></div>
                                Creating Account...
                            </div>
                        ) : 'Create Account'}
                    </button>
                </div>

                {/* Footer */}
                <div style={{
                    textAlign: 'center',
                    color: '#718096',
                    fontSize: '16px',
                    borderTop: '1px solid #e2e8f0',
                    paddingTop: '28px'
                }}>
                    <p style={{ margin: '0', fontWeight: '500' }}>
                        Already have an account?{' '}
                        <button
                            type="button"
                            onClick={onSwitchToLogin}
                            style={{
                                background: 'none',
                                border: 'none',
                                color: '#667eea',
                                cursor: 'pointer',
                                fontSize: '16px',
                                fontWeight: '700',
                                textDecoration: 'none',
                                transition: 'all 0.2s ease',
                                fontFamily: 'inherit'
                            }}
                            onMouseEnter={(e) => {
                                e.target.style.color = '#5a67d8';
                                e.target.style.textDecoration = 'underline';
                                e.target.style.transform = 'scale(1.05)';
                            }}
                            onMouseLeave={(e) => {
                                e.target.style.color = '#667eea';
                                e.target.style.textDecoration = 'none';
                                e.target.style.transform = 'scale(1)';
                            }}
                        >
                            Sign in here
                        </button>
                    </p>
                </div>
            </div>

            {/* Enhanced CSS Animations */}
            <style>
                {`
          @keyframes float {
            0%, 100% { 
              transform: translateY(0px) rotate(0deg); 
              opacity: 0.7;
            }
            50% { 
              transform: translateY(-25px) rotate(5deg); 
              opacity: 1;
            }
          }
          
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }

          @keyframes drift {
            0%, 100% { transform: translateX(0px) translateY(0px); }
            33% { transform: translateX(30px) translateY(-30px); }
            66% { transform: translateX(-20px) translateY(20px); }
          }

          @keyframes gradientShift {
            0%, 100% { background-position: 0% 50%; }
            50% { background-position: 100% 50%; }
          }
        `}
            </style>
        </div>
    );
};

export default Register;