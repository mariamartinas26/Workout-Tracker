import React, { useState } from 'react';

const CompleteProfile = ({ user, onProfileComplete, onSkip }) => {
    const [formData, setFormData] = useState({
        dateOfBirth: '',
        heightCm: '',
        weightKg: '',
        fitnessLevel: 'BEGINNER'
    });
    const [errors, setErrors] = useState({});
    const [isLoading, setIsLoading] = useState(false);

    const handleInputChange = (e) => {
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

        if (formData.dateOfBirth) {
            const birthDate = new Date(formData.dateOfBirth);
            const today = new Date();
            const age = today.getFullYear() - birthDate.getFullYear();

            if (age < 13 || age > 120) {
                newErrors.dateOfBirth = 'Age must be between 13 and 120 years';
            }
        }

        if (formData.heightCm && (formData.heightCm < 50 || formData.heightCm > 300)) {
            newErrors.heightCm = 'Height must be between 50 and 300 cm';
        }

        if (formData.weightKg && (formData.weightKg < 20 || formData.weightKg > 1000)) {
            newErrors.weightKg = 'Weight must be between 20 and 1000 kg';
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
            // Simulate API call to update user profile
            console.log("Complete User object:", JSON.stringify(user, null, 2));
            const response =  await fetch('http://localhost:8082/api/users/complete-profile', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },

                body: JSON.stringify({
                    userId: user.userId, // folosește user.id în loc de user.userId
                    dateOfBirth: formData.dateOfBirth || null,
                    heightCm: formData.heightCm ? parseInt(formData.heightCm) : null,
                    weightKg: formData.weightKg ? parseFloat(formData.weightKg) : null,
                    fitnessLevel: formData.fitnessLevel || "BEGINNER"
                })
            });

            if (response.ok) {
                const updatedUser = await response.json();
                onProfileComplete(updatedUser);
            } else {
                const errorData = await response.json();
                setErrors({ submit: errorData.message || 'Failed to update profile' });
            }
        } catch (error) {
            setErrors({ submit: 'Network error. Please try again.' });
        } finally {
            setIsLoading(false);
        }
    };

    const handleSkip = () => {
        onSkip();
    };

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
                backgroundColor: 'rgba(255,255,255,0.95)',
                backdropFilter: 'blur(20px)',
                borderRadius: '24px',
                boxShadow: '0 20px 60px rgba(0,0,0,0.15)',
                padding: '48px',
                width: '100%',
                maxWidth: '500px',
                border: '1px solid rgba(255,255,255,0.2)',
                position: 'relative',
                zIndex: 1
            }}>
                {/* Header */}
                <div style={{ textAlign: 'center', marginBottom: '40px' }}>
                    <div style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        width: '80px',
                        height: '80px',
                        background: 'linear-gradient(135deg, #667eea, #764ba2)',
                        borderRadius: '20px',
                        marginBottom: '24px',
                        boxShadow: '0 12px 40px rgba(102, 126, 234, 0.3)'
                    }}>
                        <span style={{ fontSize: '32px', color: 'white' }}>👤</span>
                    </div>
                    <h1 style={{
                        color: '#1a202c',
                        fontSize: '32px',
                        fontWeight: '800',
                        marginBottom: '12px',
                        letterSpacing: '-0.5px'
                    }}>
                        Complete Your Profile
                    </h1>
                    <p style={{
                        color: '#718096',
                        fontSize: '16px',
                        margin: '0',
                        lineHeight: '1.5',
                        fontWeight: '500'
                    }}>
                        Help us personalize your fitness experience
                    </p>
                </div>

                <form onSubmit={handleSubmit}>
                    {/* Date of Birth */}
                    <div style={{ marginBottom: '24px' }}>
                        <label style={{
                            display: 'block',
                            color: '#2d3748',
                            fontSize: '14px',
                            fontWeight: '600',
                            marginBottom: '8px',
                            letterSpacing: '0.025em'
                        }}>
                            Date of Birth
                        </label>
                        <input
                            type="date"
                            name="dateOfBirth"
                            value={formData.dateOfBirth}
                            onChange={handleInputChange}
                            style={{
                                width: '100%',
                                padding: '16px',
                                border: errors.dateOfBirth ? '2px solid #e53e3e' : '2px solid #e2e8f0',
                                borderRadius: '12px',
                                fontSize: '16px',
                                fontWeight: '500',
                                backgroundColor: '#f8fafc',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                outline: 'none',
                                fontFamily: 'inherit'
                            }}
                            onFocus={(e) => {
                                e.target.style.borderColor = '#667eea';
                                e.target.style.backgroundColor = '#ffffff';
                                e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
                            }}
                            onBlur={(e) => {
                                e.target.style.borderColor = errors.dateOfBirth ? '#e53e3e' : '#e2e8f0';
                                e.target.style.backgroundColor = '#f8fafc';
                                e.target.style.boxShadow = 'none';
                            }}
                        />
                        {errors.dateOfBirth && (
                            <p style={{
                                color: '#e53e3e',
                                fontSize: '12px',
                                marginTop: '6px',
                                fontWeight: '500'
                            }}>
                                {errors.dateOfBirth}
                            </p>
                        )}
                    </div>

                    {/* Height */}
                    <div style={{ marginBottom: '24px' }}>
                        <label style={{
                            display: 'block',
                            color: '#2d3748',
                            fontSize: '14px',
                            fontWeight: '600',
                            marginBottom: '8px',
                            letterSpacing: '0.025em'
                        }}>
                            Height (cm)
                        </label>
                        <input
                            type="number"
                            name="heightCm"
                            value={formData.heightCm}
                            onChange={handleInputChange}
                            placeholder="e.g. 175"
                            min="50"
                            max="300"
                            style={{
                                width: '100%',
                                padding: '16px',
                                border: errors.heightCm ? '2px solid #e53e3e' : '2px solid #e2e8f0',
                                borderRadius: '12px',
                                fontSize: '16px',
                                fontWeight: '500',
                                backgroundColor: '#f8fafc',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                outline: 'none',
                                fontFamily: 'inherit'
                            }}
                            onFocus={(e) => {
                                e.target.style.borderColor = '#667eea';
                                e.target.style.backgroundColor = '#ffffff';
                                e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
                            }}
                            onBlur={(e) => {
                                e.target.style.borderColor = errors.heightCm ? '#e53e3e' : '#e2e8f0';
                                e.target.style.backgroundColor = '#f8fafc';
                                e.target.style.boxShadow = 'none';
                            }}
                        />
                        {errors.heightCm && (
                            <p style={{
                                color: '#e53e3e',
                                fontSize: '12px',
                                marginTop: '6px',
                                fontWeight: '500'
                            }}>
                                {errors.heightCm}
                            </p>
                        )}
                    </div>

                    {/* Weight */}
                    <div style={{ marginBottom: '24px' }}>
                        <label style={{
                            display: 'block',
                            color: '#2d3748',
                            fontSize: '14px',
                            fontWeight: '600',
                            marginBottom: '8px',
                            letterSpacing: '0.025em'
                        }}>
                            Weight (kg)
                        </label>
                        <input
                            type="number"
                            name="weightKg"
                            value={formData.weightKg}
                            onChange={handleInputChange}
                            placeholder="e.g. 70.5"
                            min="20"
                            max="1000"
                            step="0.1"
                            style={{
                                width: '100%',
                                padding: '16px',
                                border: errors.weightKg ? '2px solid #e53e3e' : '2px solid #e2e8f0',
                                borderRadius: '12px',
                                fontSize: '16px',
                                fontWeight: '500',
                                backgroundColor: '#f8fafc',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                outline: 'none',
                                fontFamily: 'inherit'
                            }}
                            onFocus={(e) => {
                                e.target.style.borderColor = '#667eea';
                                e.target.style.backgroundColor = '#ffffff';
                                e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
                            }}
                            onBlur={(e) => {
                                e.target.style.borderColor = errors.weightKg ? '#e53e3e' : '#e2e8f0';
                                e.target.style.backgroundColor = '#f8fafc';
                                e.target.style.boxShadow = 'none';
                            }}
                        />
                        {errors.weightKg && (
                            <p style={{
                                color: '#e53e3e',
                                fontSize: '12px',
                                marginTop: '6px',
                                fontWeight: '500'
                            }}>
                                {errors.weightKg}
                            </p>
                        )}
                    </div>

                    {/* Fitness Level */}
                    <div style={{ marginBottom: '32px' }}>
                        <label style={{
                            display: 'block',
                            color: '#2d3748',
                            fontSize: '14px',
                            fontWeight: '600',
                            marginBottom: '8px',
                            letterSpacing: '0.025em'
                        }}>
                            Fitness Level
                        </label>
                        <select
                            name="fitnessLevel"
                            value={formData.fitnessLevel}
                            onChange={handleInputChange}
                            style={{
                                width: '100%',
                                padding: '16px',
                                border: '2px solid #e2e8f0',
                                borderRadius: '12px',
                                fontSize: '16px',
                                fontWeight: '500',
                                backgroundColor: '#f8fafc',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                outline: 'none',
                                fontFamily: 'inherit',
                                cursor: 'pointer'
                            }}
                            onFocus={(e) => {
                                e.target.style.borderColor = '#667eea';
                                e.target.style.backgroundColor = '#ffffff';
                                e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
                            }}
                            onBlur={(e) => {
                                e.target.style.borderColor = '#e2e8f0';
                                e.target.style.backgroundColor = '#f8fafc';
                                e.target.style.boxShadow = 'none';
                            }}
                        >
                            <option value="BEGINNER">Beginner - Just starting out</option>
                            <option value="INTERMEDIATE">Intermediate - Some experience</option>
                            <option value="ADVANCED">Advanced - Experienced athlete</option>
                        </select>
                    </div>

                    {errors.submit && (
                        <div style={{
                            backgroundColor: '#fed7d7',
                            color: '#c53030',
                            padding: '12px 16px',
                            borderRadius: '8px',
                            fontSize: '14px',
                            fontWeight: '500',
                            marginBottom: '24px',
                            border: '1px solid rgba(197, 48, 48, 0.2)'
                        }}>
                            {errors.submit}
                        </div>
                    )}

                    {/* Buttons */}
                    <div style={{ display: 'flex', gap: '12px', flexDirection: 'column' }}>
                        <button
                            type="submit"
                            disabled={isLoading}
                            style={{
                                width: '100%',
                                background: isLoading
                                    ? 'linear-gradient(135deg, #a0aec0, #718096)'
                                    : 'linear-gradient(135deg, #667eea, #764ba2)',
                                color: 'white',
                                border: 'none',
                                padding: '16px',
                                borderRadius: '12px',
                                fontSize: '16px',
                                fontWeight: '700',
                                cursor: isLoading ? 'not-allowed' : 'pointer',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                boxShadow: isLoading
                                    ? 'none'
                                    : '0 8px 32px rgba(102, 126, 234, 0.3)',
                                letterSpacing: '0.025em',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: '8px'
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
                                <>
                                    <div style={{
                                        width: '20px',
                                        height: '20px',
                                        border: '2px solid rgba(255,255,255,0.3)',
                                        borderTop: '2px solid white',
                                        borderRadius: '50%',
                                        animation: 'spin 1s linear infinite'
                                    }}></div>
                                    Updating Profile...
                                </>
                            ) : (
                                'Complete Profile'
                            )}
                        </button>

                        <button
                            type="button"
                            onClick={handleSkip}
                            style={{
                                width: '100%',
                                background: 'transparent',
                                color: '#718096',
                                border: '2px solid #e2e8f0',
                                padding: '16px',
                                borderRadius: '12px',
                                fontSize: '16px',
                                fontWeight: '600',
                                cursor: 'pointer',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                letterSpacing: '0.025em'
                            }}
                            onMouseEnter={(e) => {
                                e.target.style.borderColor = '#cbd5e0';
                                e.target.style.color = '#4a5568';
                            }}
                            onMouseLeave={(e) => {
                                e.target.style.borderColor = '#e2e8f0';
                                e.target.style.color = '#718096';
                            }}
                        >
                            Skip for Now
                        </button>
                    </div>
                </form>
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

export default CompleteProfile;