package co.elastic.apm.otel.profiler.util;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.AnnotatedElement;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisabledOnAppleSiliconCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    AnnotatedElement element = context.getElement().orElse(null);
    return findAnnotation(element, DisabledOnAppleSilicon.class)
        .map(annotation -> isOnAppleSilicon()
            ? disabled(element + " is @DisabledOnAppleSilicon", annotation.value())
            : enabled("Not running on Apple silicon"))
        .orElse(enabled("@DisabledOnAppleSilicon is not present"));
  }

  public boolean isOnAppleSilicon() {
    String os = System.getProperty("os.name").toLowerCase();
    String arch = System.getProperty("os.arch").toLowerCase();
    return os.contains("mac") && arch.contains("aarch");
  }
}
