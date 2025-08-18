package org.e2immu.language.cst.impl.analysis;

import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.Info;

public record MessageImpl(Source source, Info info, Level level, String message) implements Message {

    public static Message warn(Info info, String message) {
        return new MessageImpl(null, info, LevelEnum.WARN, message);
    }

    public enum LevelEnum implements Level {
        WARN, ERROR;

        @Override
        public boolean isWarning() {
            return WARN == this;
        }

        @Override
        public boolean isError() {
            return ERROR == this;
        }
    }

}
