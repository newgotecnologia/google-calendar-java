package org.newgo.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.newgo.attendee.AttendeeDomain;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter(AccessLevel.PRIVATE)
public class EventDomain {
    @Setter(AccessLevel.PROTECTED)
    private String id;
    private final String title;
    private final String description;
    private final ZonedDateTime startDate;
    private final ZonedDateTime endDate;
    private final List<AttendeeDomain> attendees;
    //private final AttendeeDomain creator;
    @Setter(AccessLevel.PUBLIC)
    private String calendarId;
    private boolean isDeleted;

    private EventDomain(String id, String calendarId, String title, String description, ZonedDateTime startDate, ZonedDateTime endDate,
                        List<AttendeeDomain> attendees, boolean isDeleted) {
        Objects.requireNonNull(attendees, "Attendees must be not null");

        this.id = id;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.attendees = attendees;
        //this.creator = creator;
        this.calendarId = calendarId;
        this.isDeleted = isDeleted;
    }


    public static EventDomain of(String id, String calendarId, String title, String description, ZonedDateTime startDate, ZonedDateTime endDate,
                                 List<AttendeeDomain> attendees, boolean isDeleted) {
        return new EventDomain(id, calendarId, title, description, startDate, endDate, attendees, isDeleted);
    }


    @Override
    public String toString() {
        return "EventDomain{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", attendees=" + attendees +
                ", calendarId='" + calendarId + '\'' +
                ", isDeleted='" + isDeleted + '\'' +
                '}';
    }
}
