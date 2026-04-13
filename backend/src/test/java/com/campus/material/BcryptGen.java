package com.campus.material;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptGen {
    @Test
    public void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Abc@123456");
        System.out.println("============================================");
        System.out.println("BCRYPT_HASH=" + hash);
        System.out.println("VERIFY=" + encoder.matches("Abc@123456", hash));
        System.out.println("============================================");
    }
}
