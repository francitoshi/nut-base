This is a technical specification of the **Shared Key Identity Protocol (SKIP)** based on the implementation provided.

---

# Shared Key Identity Protocol (SKIP) Specification - v1

The **Shared Key Identity Protocol (SKIP)** is a symmetric-key protocol designed for secure message exchange and identity verification. It provides confidentiality, integrity, and replay protection by utilizing authenticated encryption (AES-GCM), a secondary identity MAC (HMAC-SHA256), and a timestamp-based validity window.

## 1. Cryptographic Primitives
*   **Encryption:** AES-256 in Galois/Counter Mode (GCM) with a 128-bit authentication tag.
*   **Integrity/Identity:** HMAC-SHA256.
*   **Key Derivation:** HKDF-style derivation (or optionally Argon2id).
*   **Encoding:** Base64 (Standard).

---

## 2. Key Derivation Logic
SKIP uses key separation to ensure that the encryption process and the identity authentication process use distinct keys derived from the same `Shared Secret`.

### 2.1. Initial Keying Material (IKM)
By default, the IKM is generated using a fixed salt:
*   **Salt:** `SKIP-v1` (UTF-8 encoded bytes).
*   **IKM Calculation:** `HMAC-SHA256(Salt, SharedSecret)`

### 2.2. Sub-key Expansion
From the IKM, two 32-byte keys are derived:
1.  **Encryption Key (`kEnc`):** `HMAC-SHA256(Salt, IKM || 0x01)`
2.  **MAC Key (`kMac`):** `HMAC-SHA256(Salt, IKM || 0x02)`

---

## 3. Message Wire Format
A SKIP message is a binary blob, which is then Base64 encoded for transport.

### 3.1. Binary Structure
The total length of the binary message is `52 bytes + Ciphertext Length`.

| Offset | Length | Field | Description |
| :--- | :--- | :--- | :--- |
| 0 | 8 bytes | **Timestamp** | Unix Epoch (seconds) in Big-Endian format. |
| 8 | 12 bytes | **IV** | Random Initialization Vector for AES-GCM. |
| 20 | Variable | **Ciphertext** | AES-GCM encrypted payload (includes 16-byte GCM tag). |
| 20 + N | 32 bytes | **HMAC** | HMAC-SHA256 signature of the preceding fields. |

### 3.2. HMAC Construction
The HMAC is calculated over the raw bytes of the message components before Base64 encoding:
`HMAC_Result = HMAC-SHA256(kMac, Timestamp || IV || Ciphertext)`

---

## 4. Protocol Operations

### 4.1. Sending a Message (Encryption)
1.  **Input:** `Payload` (String), `SharedSecret` (char array).
2.  **Keys:** Derive `kEnc` and `kMac`.
3.  **Time:** Obtain current Unix `EpochSeconds`.
4.  **IV:** Generate 12 random bytes.
5.  **Encrypt:** Encrypt `Payload` using AES-GCM-256 with `kEnc`, `IV`, and a 128-bit tag.
6.  **Sign:** Compute `HMAC` over `(Timestamp || IV || Ciphertext)` using `kMac`.
7.  **Assemble:** Concatenate `Timestamp + IV + Ciphertext + HMAC`.
8.  **Output:** Base64 encode the result.

### 4.2. Receiving a Message (Verification)
1.  **Input:** Base64 String.
2.  **Decode:** Base64 decode to retrieve raw bytes.
3.  **Parsing:** Extract `Timestamp`, `IV`, `Ciphertext`, and `HMAC`.
4.  **Time Window Check:**
    *   If `Timestamp > CurrentTime`, reject (Future timestamp).
    *   If `CurrentTime - Timestamp > 604,800 seconds` (7 days), reject (Expired).
5.  **Integrity Check:**
    *   Recalculate `ExpectedHMAC` using `kMac`.
    *   Compare `ExpectedHMAC` and `ReceivedHMAC` using **constant-time comparison** to prevent timing attacks.
6.  **Decrypt:**
    *   Decrypt `Ciphertext` using AES-GCM-256 with `kEnc` and `IV`.
    *   If GCM tag verification fails, the operation throws an error.
7.  **Output:** Plaintext String.

---

## 5. Security Considerations

### 5.1. Replay Protection
Messages are protected against replay attacks by the mandatory `Timestamp` field. The receiver enforces a Time-To-Live (TTL) of 7 days. Note: For high-security environments, the receiver should also track previously seen `IVs` or `HMACs` within the 7-day window to prevent exact-copy replays.

### 5.2. Memory Safety
Sensitive data management is critical:
*   The `SharedSecret` is held as a `char[]` rather than a `String` to allow for manual clearing.
*   Derived keys (`kEnc`, `kMac`) and the `IKM` must be overwritten with zeros (`0x00`) immediately after use.
*   The `destroy()` method should be called to wipe the shared secret from memory when the protocol session ends.

### 5.3. Key Separation
By using distinct keys for encryption and authentication, an issue in one algorithm (e.g., a hypothetical weakness in AES-GCM tag processing) does not directly compromise the identity verification provided by the HMAC-SHA256 layer.

