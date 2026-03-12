package com.ose.ai;

import com.ose.common.config.AppProperties;
import com.ose.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiSecretCryptoServiceTest {

    @Test
    void shouldEncryptAndDecryptSecret() {
        AppProperties properties = new AppProperties();
        properties.getAi().setSecretEncryptionKey("unit-test-ai-secret");
        AiSecretCryptoService service = new AiSecretCryptoService(properties);

        String encrypted = service.encrypt("sk-test-1234");

        assertNotEquals("sk-test-1234", encrypted);
        assertEquals("sk-test-1234", service.decrypt(encrypted));
    }

    @Test
    void shouldRejectEncryptionWhenMasterKeyMissing() {
        AppProperties properties = new AppProperties();
        AiSecretCryptoService service = new AiSecretCryptoService(properties);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.encrypt("sk-test-1234"));

        assertTrue(ex.getMessage().contains("AI_SECRET_ENCRYPTION_KEY"));
    }

    @Test
    void shouldMaskKeyWithoutExposingPlaintext() {
        AppProperties properties = new AppProperties();
        AiSecretCryptoService service = new AiSecretCryptoService(properties);

        assertEquals("sk-***1234", service.mask("sk-test-1234"));
        assertNull(service.mask(null));
    }
}
