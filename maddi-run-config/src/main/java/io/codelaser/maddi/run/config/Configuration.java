/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.run.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.codelaser.maddi.cst.api.runtime.LanguageConfiguration;
import io.codelaser.maddi.cst.impl.runtime.LanguageConfigurationImpl;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;

public class Configuration {
    @JsonProperty
    private final GeneralConfiguration generalConfiguration;
    @JsonProperty
    private final InputConfiguration inputConfiguration;
    @JsonProperty
    private final AnalysisHintsConfiguration analysisHintsConfiguration;
    @JsonProperty
    private final LanguageConfiguration languageConfiguration;

    // deserialization creator: the interface-typed fields are resolved to their impls by the SimpleAbstractType
    // resolver in JsonStreaming (InputConfiguration, AnalysisHintsConfiguration, LanguageConfiguration)
    @JsonCreator
    Configuration(@JsonProperty("generalConfiguration") GeneralConfiguration generalConfiguration,
                  @JsonProperty("inputConfiguration") InputConfiguration inputConfiguration,
                  @JsonProperty("analysisHintsConfiguration") AnalysisHintsConfiguration analysisHintsConfiguration,
                  @JsonProperty("languageConfiguration") LanguageConfiguration languageConfiguration) {
        this.generalConfiguration = generalConfiguration;
        this.inputConfiguration = inputConfiguration;
        this.analysisHintsConfiguration = analysisHintsConfiguration;
        this.languageConfiguration = languageConfiguration;
    }


    @Override
    public String toString() {
        return generalConfiguration + "\n" + inputConfiguration + "\n" + analysisHintsConfiguration + "\n"
               + languageConfiguration;
    }

    public static class Builder {
        private GeneralConfiguration generalConfiguration;
        private InputConfiguration inputConfiguration;
        private AnalysisHintsConfiguration analysisHintsConfiguration;
        private LanguageConfiguration languageConfiguration;

        public Builder setAnalysisHintsConfiguration(AnalysisHintsConfiguration analysisHintsConfiguration) {
            this.analysisHintsConfiguration = analysisHintsConfiguration;
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
                    analysisHintsConfiguration == null
                            ? new AnalysisHintsConfigurationImpl.Builder().build() : analysisHintsConfiguration,
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

    public AnalysisHintsConfiguration analysisHintsConfiguration() {
        return analysisHintsConfiguration;
    }

    public LanguageConfiguration languageConfiguration() {
        return languageConfiguration;
    }
}
