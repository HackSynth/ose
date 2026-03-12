package com.ose.ai;

import com.ose.common.config.AppProperties;
import com.ose.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AiSecretCryptoService {

    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public boolean canPersistSecrets() {
        String secret = appProperties.getAi().getSecretEncryptionKey();
        return secret != null && !secret.isBlank();
    }

    public String encrypt(String plainText) {
        if (!canPersistSecrets()) {
            throw new BusinessException("未配置 AI_SECRET_ENCRYPTION_KEY，当前实例不能托管数据库密钥");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, payload, IV_LENGTH, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("AI 密钥加密失败，请检查服务端配置");
        }
    }

    public String decrypt(String encryptedText) {
        if (!canPersistSecrets()) {
            throw new BusinessException("未配置 AI_SECRET_ENCRYPTION_KEY，无法解密数据库中的 AI 密钥");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText);
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("AI 密钥解密失败，请检查 AI_SECRET_ENCRYPTION_KEY 是否正确");
        }
    }

    public String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        String normalized = apiKey.trim();
        int prefixLength = Math.min(3, normalized.length());
        int suffixLength = Math.min(4, Math.max(0, normalized.length() - prefixLength));
        String prefix = normalized.substring(0, prefixLength);
        String suffix = suffixLength == 0 ? "" : normalized.substring(normalized.length() - suffixLength);
        return prefix + "***" + suffix;
    }

    private SecretKeySpec secretKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(appProperties.getAi().getSecretEncryptionKey().getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, "AES");
        } catch (Exception ex) {
            throw new BusinessException("AI 加密主密钥初始化失败");
        }
    }
}
