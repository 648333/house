import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePassword {
    public static void main(String[] args) {
        String password = "648666";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashed_password = encoder.encode(password);
        System.out.println("Hashed Password: " + hashed_password);
    }
}
