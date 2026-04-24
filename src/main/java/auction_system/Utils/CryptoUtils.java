package auction_system.Utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


public class CryptoUtils {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public static String hash(String password) {
        String encoded = encoder.encode(password);
        return encoded;
    }

    public static boolean verify(String rawpassword,String hashpassword) {
        return encoder.matches(rawpassword,hashpassword);
    }
}
