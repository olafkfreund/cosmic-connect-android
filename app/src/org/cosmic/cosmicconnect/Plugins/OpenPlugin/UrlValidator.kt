/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Android Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.OpenPlugin

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.URISyntaxException

/**
 * UrlValidator - Secure URL validation for App Continuity feature
 *
 * This validator implements defense-in-depth security measures to prevent:
 * - SSRF (Server-Side Request Forgery) attacks via internal IP addresses
 * - Credential leakage via URL userinfo
 * - Scheme-based attacks (javascript:, file:, data:)
 * - Null byte injection attacks
 * - Denial of service via extremely long URLs
 *
 * ## OWASP References
 * - OWASP ASVS V5: Validation, Sanitization and Encoding
 * - OWASP SSRF Prevention Cheat Sheet
 * - CWE-918: Server-Side Request Forgery (SSRF)
 * - CWE-601: URL Redirection to Untrusted Site ('Open Redirect')
 *
 * ## Security Model
 *
 * This validator uses an allowlist approach for URL schemes, blocking all
 * schemes by default except those explicitly permitted. This follows the
 * principle of "deny by default, allow by exception."
 *
 * @see <a href="https://owasp.org/www-project-web-security-testing-guide/">OWASP Testing Guide</a>
 */
object UrlValidator {

    /**
     * Allowed URL schemes (allowlist approach)
     *
     * - http/https: Standard web URLs
     * - mailto: Email composition
     * - tel: Phone calls
     * - geo: Geographic coordinates (maps)
     * - sms: SMS composition
     * - smsto: SMS composition (alternative)
     *
     * Explicitly blocked schemes (handled by not being in allowlist):
     * - javascript: XSS attacks
     * - file: Local file access
     * - data: Data URL injection
     * - blob: Binary data injection
     * - about: Browser internal pages
     * - chrome/chrome-extension: Browser internals
     * - intent: Android intent hijacking
     */
    private val ALLOWED_SCHEMES = setOf(
        "http",
        "https",
        "mailto",
        "tel",
        "geo",
        "sms",
        "smsto"
    )

    /**
     * Blocked hostnames (localhost and loopback addresses)
     *
     * These are blocked to prevent SSRF attacks that could:
     * - Access internal services running on localhost
     * - Bypass firewall rules
     * - Access cloud metadata services
     */
    private val BLOCKED_HOSTS = setOf(
        "localhost",
        "localhost.localdomain",
        "127.0.0.1",
        "::1",
        "0.0.0.0",
        "[::1]",
        "[0:0:0:0:0:0:0:1]"
    )

    /**
     * Cloud metadata endpoints to block (SSRF prevention)
     *
     * These are commonly targeted in SSRF attacks to steal cloud credentials
     */
    private val BLOCKED_METADATA_HOSTS = setOf(
        "169.254.169.254",      // AWS/Azure/GCP metadata service
        "metadata.google.internal",
        "metadata.google.com",
        "169.254.170.2",        // AWS ECS task metadata
        "fd00:ec2::254"         // AWS IPv6 metadata
    )

    /**
     * Maximum allowed URL length (prevents DoS and buffer overflow attacks)
     *
     * RFC 2616 suggests servers should handle URLs up to 8000 bytes.
     * We use a conservative limit of 2048 bytes which is sufficient
     * for all legitimate use cases while preventing abuse.
     */
    private const val MAX_URL_LENGTH = 2048

    /**
     * Maximum allowed hostname length (DNS specification limit)
     */
    private const val MAX_HOSTNAME_LENGTH = 253

    /**
     * Result of URL validation
     */
    sealed class ValidationResult {
        /**
         * URL passed all security checks
         */
        object Valid : ValidationResult()

        /**
         * URL failed one or more security checks
         *
         * @property reason Human-readable description of the failure
         * @property securityCode OWASP/CWE reference for the vulnerability
         */
        data class Invalid(
            val reason: String,
            val securityCode: String = ""
        ) : ValidationResult()

        /**
         * Check if the result is valid
         */
        val isValid: Boolean
            get() = this is Valid

        /**
         * Get the error message if invalid, or null if valid
         */
        val errorMessage: String?
            get() = (this as? Invalid)?.reason
    }

    /**
     * Validate a URL for safe opening on a remote device
     *
     * This method performs comprehensive security checks including:
     * 1. Length validation (DoS prevention)
     * 2. Null byte detection (injection prevention)
     * 3. URI parsing validation (malformed URL detection)
     * 4. Scheme allowlist validation (attack vector reduction)
     * 5. Credential detection (information leakage prevention)
     * 6. Host validation (SSRF prevention)
     * 7. Private IP detection (SSRF prevention)
     * 8. Cloud metadata detection (credential theft prevention)
     *
     * @param url The URL string to validate
     * @return ValidationResult indicating whether the URL is safe
     */
    fun validate(url: String): ValidationResult {
        // 1. Check for null or empty URL
        if (url.isBlank()) {
            return ValidationResult.Invalid(
                reason = "URL cannot be empty",
                securityCode = "INPUT-001"
            )
        }

        // 2. Check URL length (prevents DoS via extremely long URLs)
        if (url.length > MAX_URL_LENGTH) {
            return ValidationResult.Invalid(
                reason = "URL exceeds maximum length of $MAX_URL_LENGTH characters",
                securityCode = "CWE-400"
            )
        }

        // 3. Check for null bytes (prevents null byte injection attacks)
        if (url.contains('\u0000')) {
            return ValidationResult.Invalid(
                reason = "URL contains null bytes which are not allowed",
                securityCode = "CWE-158"
            )
        }

        // 4. Check for control characters (prevents encoding attacks)
        if (url.any { it.isISOControl() && it != '\t' }) {
            return ValidationResult.Invalid(
                reason = "URL contains invalid control characters",
                securityCode = "CWE-116"
            )
        }

        // 5. Parse URL using java.net.URI (more strict than URL class)
        val parsed: URI
        try {
            parsed = URI(url)
        } catch (e: URISyntaxException) {
            return ValidationResult.Invalid(
                reason = "Malformed URL: ${e.reason ?: "Invalid syntax"}",
                securityCode = "CWE-20"
            )
        }

        // 6. Check for scheme presence
        val scheme = parsed.scheme?.lowercase()
        if (scheme == null) {
            return ValidationResult.Invalid(
                reason = "URL must have a scheme (e.g., https://)",
                securityCode = "INPUT-002"
            )
        }

        // 7. Validate scheme against allowlist
        if (scheme !in ALLOWED_SCHEMES) {
            return ValidationResult.Invalid(
                reason = "URL scheme '$scheme' is not allowed. Allowed schemes: ${ALLOWED_SCHEMES.joinToString(", ")}",
                securityCode = "CWE-601"
            )
        }

        // 8. Check for credentials in URL (information leakage)
        if (parsed.userInfo != null) {
            return ValidationResult.Invalid(
                reason = "URLs containing credentials (username:password) are not allowed for security reasons",
                securityCode = "CWE-522"
            )
        }

        // For schemes that require host validation
        if (scheme in setOf("http", "https")) {
            val host = parsed.host?.lowercase()

            // 9. Check for host presence in http/https URLs
            if (host.isNullOrBlank()) {
                return ValidationResult.Invalid(
                    reason = "HTTP/HTTPS URLs must have a valid host",
                    securityCode = "INPUT-003"
                )
            }

            // 10. Check hostname length
            if (host.length > MAX_HOSTNAME_LENGTH) {
                return ValidationResult.Invalid(
                    reason = "Hostname exceeds maximum length",
                    securityCode = "CWE-400"
                )
            }

            // 11. Check against blocked hosts (localhost, loopback)
            if (host in BLOCKED_HOSTS) {
                return ValidationResult.Invalid(
                    reason = "Host '$host' is blocked for security reasons",
                    securityCode = "CWE-918"
                )
            }

            // 12. Check against cloud metadata endpoints
            if (host in BLOCKED_METADATA_HOSTS) {
                return ValidationResult.Invalid(
                    reason = "Cloud metadata endpoints are blocked for security reasons",
                    securityCode = "CWE-918"
                )
            }

            // 13. Check for private/internal IP addresses (SSRF prevention)
            val privateIpResult = checkPrivateIp(host)
            if (privateIpResult is ValidationResult.Invalid) {
                return privateIpResult
            }

            // 14. Check for IPv6 localhost variants
            if (isIPv6Localhost(host)) {
                return ValidationResult.Invalid(
                    reason = "IPv6 localhost addresses are blocked",
                    securityCode = "CWE-918"
                )
            }

            // 15. Check port (block common internal service ports)
            val port = parsed.port
            if (port != -1 && isBlockedPort(port)) {
                return ValidationResult.Invalid(
                    reason = "Port $port is blocked for security reasons",
                    securityCode = "CWE-918"
                )
            }
        }

        // All checks passed
        return ValidationResult.Valid
    }

    /**
     * Check if the host is a private/internal IP address
     *
     * Blocks:
     * - 10.0.0.0/8 (Class A private)
     * - 172.16.0.0/12 (Class B private)
     * - 192.168.0.0/16 (Class C private)
     * - 100.64.0.0/10 (Carrier-grade NAT)
     * - 127.0.0.0/8 (Loopback)
     * - 169.254.0.0/16 (Link-local)
     * - fc00::/7 (IPv6 Unique Local)
     * - fe80::/10 (IPv6 Link-Local)
     */
    private fun checkPrivateIp(host: String): ValidationResult {
        try {
            // Try to parse as IP address
            val address = InetAddress.getByName(host)

            when (address) {
                is Inet4Address -> {
                    val bytes = address.address

                    // 127.0.0.0/8 - Loopback
                    if (bytes[0] == 127.toByte()) {
                        return ValidationResult.Invalid(
                            reason = "Loopback addresses (127.x.x.x) are blocked",
                            securityCode = "CWE-918"
                        )
                    }

                    // 10.0.0.0/8 - Class A private
                    if (bytes[0] == 10.toByte()) {
                        return ValidationResult.Invalid(
                            reason = "Private IP addresses (10.x.x.x) are blocked",
                            securityCode = "CWE-918"
                        )
                    }

                    // 172.16.0.0/12 - Class B private
                    if (bytes[0] == 172.toByte() &&
                        (bytes[1].toInt() and 0xFF) in 16..31
                    ) {
                        return ValidationResult.Invalid(
                            reason = "Private IP addresses (172.16.x.x - 172.31.x.x) are blocked",
                            securityCode = "CWE-918"
                        )
                    }

                    // 192.168.0.0/16 - Class C private
                    if (bytes[0] == 192.toByte() && bytes[1] == 168.toByte()) {
                        return ValidationResult.Invalid(
                            reason = "Private IP addresses (192.168.x.x) are blocked",
                            securityCode = "CWE-918"
                        )
                    }

                    // 169.254.0.0/16 - Link-local
                    if (bytes[0] == 169.toByte() && bytes[1] == 254.toByte()) {
                        return ValidationResult.Invalid(
                            reason = "Link-local addresses (169.254.x.x) are blocked",
                            securityCode = "CWE-918"
                        )
                    }

                    // 100.64.0.0/10 - Carrier-grade NAT
                    if (bytes[0] == 100.toByte() &&
                        (bytes[1].toInt() and 0xFF) in 64..127
                    ) {
                        return ValidationResult.Invalid(
                            reason = "Carrier-grade NAT addresses (100.64.x.x - 100.127.x.x) are blocked",
                            securityCode = "CWE-918"
                        )
                    }

                    // 0.0.0.0/8 - Current network
                    if (bytes[0] == 0.toByte()) {
                        return ValidationResult.Invalid(
                            reason = "Current network addresses (0.x.x.x) are blocked",
                            securityCode = "CWE-918"
                        )
                    }
                }

                is Inet6Address -> {
                    // Check for IPv6 loopback
                    if (address.isLoopbackAddress) {
                        return ValidationResult.Invalid(
                            reason = "IPv6 loopback addresses are blocked",
                            securityCode = "CWE-918"
                        )
                    }

                    // Check for link-local
                    if (address.isLinkLocalAddress) {
                        return ValidationResult.Invalid(
                            reason = "IPv6 link-local addresses are blocked",
                            securityCode = "CWE-918"
                        )
                    }

                    // Check for site-local (deprecated but still used)
                    @Suppress("DEPRECATION")
                    if (address.isSiteLocalAddress) {
                        return ValidationResult.Invalid(
                            reason = "IPv6 site-local addresses are blocked",
                            securityCode = "CWE-918"
                        )
                    }

                    // Check for Unique Local Addresses (fc00::/7)
                    val bytes = address.address
                    if ((bytes[0].toInt() and 0xFE) == 0xFC) {
                        return ValidationResult.Invalid(
                            reason = "IPv6 unique local addresses (fc00::/7) are blocked",
                            securityCode = "CWE-918"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // If we can't parse as IP, it's probably a hostname which is OK
            // DNS resolution will handle validity
        }

        return ValidationResult.Valid
    }

    /**
     * Check for IPv6 localhost variants that might bypass simple string matching
     */
    private fun isIPv6Localhost(host: String): Boolean {
        val normalized = host.lowercase().trim()

        // Common IPv6 localhost representations
        val localhostVariants = listOf(
            "::1",
            "[::1]",
            "0:0:0:0:0:0:0:1",
            "[0:0:0:0:0:0:0:1]",
            "0000:0000:0000:0000:0000:0000:0000:0001",
            "[0000:0000:0000:0000:0000:0000:0000:0001]",
            "::ffff:127.0.0.1",
            "[::ffff:127.0.0.1]"
        )

        return normalized in localhostVariants
    }

    /**
     * Check if a port is commonly used for internal services
     *
     * These ports are often used by internal services and could be
     * targets for SSRF attacks.
     */
    private fun isBlockedPort(port: Int): Boolean {
        val blockedPorts = setOf(
            22,     // SSH
            23,     // Telnet
            25,     // SMTP
            110,    // POP3
            143,    // IMAP
            445,    // SMB
            1433,   // MSSQL
            1521,   // Oracle
            3306,   // MySQL
            3389,   // RDP
            5432,   // PostgreSQL
            5900,   // VNC
            6379,   // Redis
            8080,   // Common proxy
            8443,   // HTTPS alt
            9200,   // Elasticsearch
            27017   // MongoDB
        )

        return port in blockedPorts
    }

    /**
     * Quick validation check that returns a boolean
     *
     * Convenience method for simple validation checks.
     *
     * @param url The URL to validate
     * @return true if valid, false otherwise
     */
    fun isValid(url: String): Boolean = validate(url).isValid

    /**
     * Sanitize a URL by extracting only the essential parts
     *
     * This method creates a normalized version of the URL, removing
     * any potentially dangerous components while preserving functionality.
     *
     * @param url The URL to sanitize
     * @return Sanitized URL string, or null if URL is invalid
     */
    fun sanitize(url: String): String? {
        val result = validate(url)
        if (result !is ValidationResult.Valid) {
            return null
        }

        return try {
            val parsed = URI(url)
            // Reconstruct URL without userInfo
            URI(
                parsed.scheme,
                null, // Remove userInfo
                parsed.host,
                parsed.port,
                parsed.path,
                parsed.query,
                parsed.fragment
            ).toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get a detailed security report for a URL
     *
     * Useful for debugging and logging security decisions.
     *
     * @param url The URL to analyze
     * @return Security report string
     */
    fun getSecurityReport(url: String): String {
        val result = validate(url)

        return buildString {
            appendLine("=== URL Security Report ===")
            appendLine("URL: ${url.take(100)}${if (url.length > 100) "..." else ""}")
            appendLine("Length: ${url.length}/$MAX_URL_LENGTH")
            appendLine()

            when (result) {
                is ValidationResult.Valid -> {
                    appendLine("Status: VALID")
                    try {
                        val parsed = URI(url)
                        appendLine("Scheme: ${parsed.scheme}")
                        appendLine("Host: ${parsed.host}")
                        appendLine("Port: ${if (parsed.port == -1) "default" else parsed.port.toString()}")
                        appendLine("Path: ${parsed.path ?: "/"}")
                    } catch (e: Exception) {
                        appendLine("Parse error: ${e.message}")
                    }
                }

                is ValidationResult.Invalid -> {
                    appendLine("Status: INVALID")
                    appendLine("Reason: ${result.reason}")
                    appendLine("Security Code: ${result.securityCode}")
                }
            }

            appendLine()
            appendLine("Allowed Schemes: ${ALLOWED_SCHEMES.joinToString(", ")}")
            appendLine("=== End Report ===")
        }
    }
}
