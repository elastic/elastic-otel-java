package co.elastic.otel.openai;

import java.util.List;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.StubMappingTransformer;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

public class PrettyPrintEqualToJsonStubMappingTransformer extends StubMappingTransformer {
    @Override
    public String getName() {
        return "pretty-print-equal-to-json";
    }

    @Override
    public StubMapping transform(StubMapping stubMapping, FileSource files, Parameters parameters) {
        List<ContentPattern<?>> patterns = stubMapping.getRequest().getBodyPatterns();
        if (patterns != null) {
            for (int i = 0; i < patterns.size(); i++) {
                ContentPattern<?> pattern = patterns.get(i);
                if (!(pattern instanceof EqualToJsonPattern)) {
                    continue;
                }
                EqualToJsonPattern equalToJsonPattern = (EqualToJsonPattern) pattern;
                patterns.set(i, new EqualToJsonPattern(
                        equalToJsonPattern.getExpected(), // pretty printed,
                        // We exact match the request unlike the default.
                        false,
                        false
                ));
            }
        }
        return stubMapping;
    }
}
