package com.mysqlmanager.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TotpServiceTest {

    @Mock
    private SecretGenerator secretGenerator;

    @Mock
    private QrGenerator qrGenerator;

    @Mock
    private CodeVerifier codeVerifier;

    @InjectMocks
    private TotpService totpService;

    @Test
    void generateSecretDelegatesToSecretGenerator() {
        when(secretGenerator.generate()).thenReturn("MYSECRET");
        assertThat(totpService.generateSecret()).isEqualTo("MYSECRET");
    }

    @Test
    void generateQrCodeDataUri() throws QrGenerationException {
        byte[] fakeImage = new byte[]{1, 2, 3};
        when(qrGenerator.generate(any(QrData.class))).thenReturn(fakeImage);
        when(qrGenerator.getImageMimeType()).thenReturn("image/png");

        String uri = totpService.generateQrCodeDataUri("mario", "SECRET");

        assertThat(uri).startsWith("data:image/png;base64,");
    }

    @Test
    void generateQrCodeDataUriWrapsQrException() throws QrGenerationException {
        when(qrGenerator.generate(any(QrData.class)))
                .thenThrow(new QrGenerationException("fail", new RuntimeException()));

        assertThatThrownBy(() -> totpService.generateQrCodeDataUri("mario", "SECRET"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("QR code");
    }

    @Test
    void verifyCodeReturnsTrueWhenValid() {
        when(codeVerifier.isValidCode("SECRET", "123456")).thenReturn(true);
        assertThat(totpService.verifyCode("SECRET", "123456")).isTrue();
    }

    @Test
    void verifyCodeReturnsFalseWhenInvalid() {
        when(codeVerifier.isValidCode("SECRET", "000000")).thenReturn(false);
        assertThat(totpService.verifyCode("SECRET", "000000")).isFalse();
    }

    @Test
    void verifyCodeReturnsFalseWhenSecretIsNull() {
        assertThat(totpService.verifyCode(null, "123456")).isFalse();
    }

    @Test
    void verifyCodeReturnsFalseWhenCodeIsNull() {
        assertThat(totpService.verifyCode("SECRET", null)).isFalse();
    }
}
