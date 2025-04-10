package javaloginmodule.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import javaloginmodule.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Component
public class JWTTokenService implements TokenService {

    private final String jwtSecret;

    public JWTTokenService(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Override
    public String generateToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);

        return JWT.create()
                .withSubject(String.valueOf(user.id()))
                .withClaim("username", user.username())
                .withIssuer("login-app")
                .withExpiresAt(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .sign(algorithm);
    }

    @Override
    public Optional<String> verifyToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);

            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("login-app")
                    .acceptExpiresAt(1)
                    .build();

            DecodedJWT decoded = verifier.verify(token);
            String subject = decoded.getSubject();

            return Optional.of(subject);
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
