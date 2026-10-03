/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public interface X509CertificateBuilder
{
    public X509Certificate buildCertificate(PublicKey publicKey, PrivateKey privateKey, String dnAlias) throws Exception;
    public X509Certificate[] buildCertificateChain(PublicKey publicKey, PrivateKey privateKey, String dnAlias) throws Exception;
}
