package com.pixelmarks.core.security.model

/**
 * Enumeration of device biometric & credential authentication capability states.
 */
enum class BiometricAuthCapability(val userMessage: String, val canLock: Boolean) {
    AVAILABLE("Biometric or device credentials available", true),
    NONE_ENROLLED("No biometric or screen lock enrolled on device", false),
    NO_HARDWARE("Biometric hardware is not available on this device", false),
    HARDWARE_UNAVAILABLE("Biometric sensor is temporarily unavailable", false),
    SECURITY_UPDATE_REQUIRED("A security update is required to use biometrics", false),
    UNAVAILABLE("Authentication is currently unavailable", false)
}
