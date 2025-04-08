package javaloginmodule;

import javaloginmodule.repository.UserRepositoryTest;
import javaloginmodule.security.PasswordHasherTest;
import javaloginmodule.service.AuthServiceTest;
import javaloginmodule.service.TokenServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        UserRepositoryTest.class,
        PasswordHasherTest.class,
        AuthServiceTest.class,
        TokenServiceTest.class
})
public class AllTestsSuite {
}
