package co.elastic.otel.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ProfilingControllerTest {

    @Autowired
    private ProfilingController controller;

    @BeforeEach
    void contextLoads() {
        assertThat(controller).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void testScenarios(int id) {
        assertThat(controller.scenario(id))
                .isEqualTo("scenario %d OK", id);
    }
}
