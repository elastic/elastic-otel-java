package co.elastic.apm.otel.profiler.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DisabledOnAppleSiliconCondition.class)
public @interface DisabledOnAppleSilicon {

  /**
   * The reason this annotated test class or test method is disabled.
   */
  String value() default "";
}
