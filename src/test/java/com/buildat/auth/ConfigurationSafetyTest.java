package com.buildat.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class ConfigurationSafetyTest {
    @Test void missingTrustedConfigurationPreventsStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtConfiguration.class).run(context -> assertThat(context).hasFailed());
    }
}
