#ifndef CURVE25519_H
#define CURVE25519_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Curve25519 scalar multiplication (RFC 7748).
 * Computes shared_secret = scalar_multiplication(private_key, basepoint_or_public_key).
 *
 * @param out Output 32-byte shared secret or public key
 * @param scalar 32-byte private key scalar
 * @param point 32-byte public key point (or base point 9)
 * @return 0 on success, -1 on failure
 */
int curve25519_eval(uint8_t out[32], const uint8_t scalar[32], const uint8_t point[32]);

/**
 * Generates a public key from a 32-byte private key.
 */
int curve25519_generate_public_key(uint8_t public_key[32], const uint8_t private_key[32]);

#ifdef __cplusplus
}
#endif

#endif // CURVE25519_H
