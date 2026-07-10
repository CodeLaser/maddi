/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.run.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.e2immu.analyzer.aapi.parser.AnalysisHintsConfiguration;
import org.e2immu.analyzer.aapi.parser.AnalysisHintsConfigurationImpl;
import org.e2immu.language.cst.api.runtime.LanguageConfiguration;
import org.e2immu.language.cst.impl.runtime.LanguageConfigurationImpl;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;

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
