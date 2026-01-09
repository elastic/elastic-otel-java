package baggage.example.extension;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class CustomBaggageInstrumentation implements TypeInstrumentation {

  CustomBaggageInstrumentation() {
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    // instrument known class in the application
    return named("baggage.example.SimpleServer");
  }

  @Override
  public void transform(TypeTransformer typeTransformer) {
    // instrument known method in the application where baggage needs to be added
    // method is identified by its name parts of its signature
    typeTransformer.applyAdviceToMethod(named("callWithoutBaggageApi")
            .and(takesArguments(2))
            .and(takesArgument(0, named("java.lang.String"))),
        this.getClass().getName() + "$CustomBaggageAdvice");
  }

  @SuppressWarnings("unused")
  public static class CustomBaggageAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Scope onEnter(@Advice.Argument(0) String customerId) {
      if (customerId == null) {
        // defensively handle null as with instrumentation there is no guarantee that application
        // code invariants and assumptions will be preserved over time.
        return Scope.noop();
      }

      // Add customer information into baggage
      // this information is provided directly from the instrumented method argument, so we just
      // have to copy it into baggage.
      BaggageBuilder baggage = Baggage.current().toBuilder()
          .put("example.customer.id", customerId)
          .put("example.customer.name", String.format("my-awesome-customer-%s", customerId));

      // make it current for the scope of the instrumented method
      return baggage.build().makeCurrent();
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Enter Scope scope) {
      if(scope != null){
        // close the scope with custom baggage
        // we have to defend against null in case an exception is thrown in the enter advice
        scope.close();
      }

    }
  }
}
