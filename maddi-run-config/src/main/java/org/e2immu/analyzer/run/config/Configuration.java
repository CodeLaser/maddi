package org.e2immu.analyzer.run.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfiguration;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfigurationImpl;
import org.e2immu.language.cst.api.runtime.LanguageConfiguration;
import org.e2immu.language.cst.impl.runtime.LanguageConfigurationImpl;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;

public class Configuration {
    @JsonProperty
    private final GeneralConfiguration generalConfiguration;
    @JsonProperty
    private final InputConfiguration inputConfiguration;
    //@JsonProperty
    private final AnnotatedAPIConfiguration annotatedAPIConfiguration;
    @JsonProperty
    private final LanguageConfiguration languageConfiguration;

    @JsonCreator
    private Configuration(@JsonProperty("generalConfiguration") GeneralConfiguration generalConfiguration,
                          @JsonProperty("inputConfiguration") InputConfiguration inputConfiguration,
                          @JsonProperty("languageConfiguration") LanguageConfiguration languageConfiguration) {
        this(generalConfiguration, inputConfiguration, null, languageConfiguration);
    }

    private Configuration(GeneralConfiguration generalConfiguration,
                          InputConfiguration inputConfiguration,
                          AnnotatedAPIConfiguration annotatedAPIConfiguration,
                          LanguageConfiguration languageConfiguration) {
        this.annotatedAPIConfiguration = annotatedAPIConfiguration;
        this.generalConfiguration = generalConfiguration;
        this.inputConfiguration = inputConfiguration;
        this.languageConfiguration = languageConfiguration;
    }


    @Override
    public String toString() {
        return generalConfiguration + "\n" + inputConfiguration + "\n" + annotatedAPIConfiguration + "\n"
               + languageConfiguration;
    }

    public static class Builder {
        private GeneralConfiguration generalConfiguration;
        private InputConfiguration inputConfiguration;
        private AnnotatedAPIConfiguration annotatedAPIConfiguration;
        private LanguageConfiguration languageConfiguration;

        public Builder setAnnotatedAPIConfiguration(AnnotatedAPIConfiguration annotatedAPIConfiguration) {
            this.annotatedAPIConfiguration = annotatedAPIConfiguration;
            return this;
        }

        public Builder setGeneralConfiguration(GeneralConfiguration generalConfiguration) {
            this.generalConfiguration = generalConfiguration;
            return this;
        }

        public Builder setInputConfiguration(InputConfiguration inputConfiguration) {
            this.inputConfiguration = inputConfiguration;
            return this;
        }

        public Builder setLanguageConfiguration(LanguageConfiguration languageConfiguration) {
            this.languageConfiguration = languageConfiguration;
            return this;
        }

        public Configuration build() {
            return new Configuration(generalConfiguration == null
                    ? new GeneralConfiguration.Builder().build() : generalConfiguration,
                    inputConfiguration == null
                            ? new InputConfigurationImpl.Builder().build() : inputConfiguration,
                    annotatedAPIConfiguration == null
                            ? new AnnotatedAPIConfigurationImpl.Builder().build() : annotatedAPIConfiguration,
                    languageConfiguration == null ? new LanguageConfigurationImpl(true)
                            : languageConfiguration);
        }
    }

    public GeneralConfiguration generalConfiguration() {
        return generalConfiguration;
    }

    public InputConfiguration inputConfiguration() {
        return inputConfiguration;
    }

    public AnnotatedAPIConfiguration annotatedAPIConfiguration() {
        return annotatedAPIConfiguration;
    }

    public LanguageConfiguration languageConfiguration() {
        return languageConfiguration;
    }
}
