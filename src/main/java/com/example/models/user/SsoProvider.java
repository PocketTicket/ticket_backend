package com.example.models.user;

import java.util.Locale;

/** Where a customer logs in. */
public enum SsoProvider {
    ISERV,
    MOODLE;

    /** Lets URLs use lowercase names, e.g. /auth/iserv/login. Unknown names answer 404. */
    public static SsoProvider fromString(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    public String pathName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
