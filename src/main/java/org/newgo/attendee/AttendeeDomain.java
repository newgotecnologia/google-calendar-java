package org.newgo.attendee;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PRIVATE)
public class AttendeeDomain {

    private final String name;
    private final String email;
    private final boolean optional;
    private final String responseStatus;


    private AttendeeDomain(String name, String email, boolean optional) {
        this.name = name;
        this.email = email;
        this.optional = optional;
        /*
         * https://developers.google.com/calendar/api/v3/reference/events?hl=pt-br#attendees.responseStatus
         * */
        this.responseStatus = "accepted";
    }

    public static AttendeeDomain of(String name, String email, boolean optional) {
        return new AttendeeDomain(name, email, optional);
    }
}
