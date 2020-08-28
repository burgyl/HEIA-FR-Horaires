package ch.lburgy.heiafrschedule.preferences;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

@RequiresApi(api = Build.VERSION_CODES.M)
class Cryptography {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private byte[] iv;
    private KeyStore keyStore;

    Cryptography() throws Exception {
        initKeyStore();
    }

    private void initKeyStore() throws Exception {
        keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
    }

    byte[] encryptText(final String alias, final String textToEncrypt) throws Exception {

        final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKeyEncrypted(alias));

        iv = cipher.getIV();

        return cipher.doFinal(textToEncrypt.getBytes(StandardCharsets.UTF_8));
    }

    String decryptData(final String alias, final byte[] encryptedData, final byte[] encryptionIv) throws Exception {
        final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        final GCMParameterSpec spec = new GCMParameterSpec(128, encryptionIv);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKeyDecrypted(alias), spec);

        return new String(cipher.doFinal(encryptedData), StandardCharsets.UTF_8);
    }

    private SecretKey getSecretKeyDecrypted(final String alias) throws NoSuchAlgorithmException,
            UnrecoverableEntryException, KeyStoreException {
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(alias, null)).getSecretKey();
    }

    @NonNull
    private SecretKey getSecretKeyEncrypted(final String alias) throws Exception {

        final KeyGenerator keyGenerator = KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

        keyGenerator.init(new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());

        return keyGenerator.generateKey();
    }

    byte[] getIv() {
        return iv;
    }
}
