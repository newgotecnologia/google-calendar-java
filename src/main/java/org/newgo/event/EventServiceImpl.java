package org.newgo.event;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import org.newgo.configuration.GoogleCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);
    private final GoogleCalendar googleCalendar;

    public EventServiceImpl() throws GeneralSecurityException, IOException {
        this.googleCalendar = new GoogleCalendar();
    }


    public Optional<EventDomain> processEvent(EventDomain eventDomain) {
        if (isEventCreatable(eventDomain)) {
            return Optional.of(createEvent(eventDomain));
        }
        processEventUpdateOrDeletion(eventDomain);
        return Optional.empty();
    }

    private void processEventUpdateOrDeletion(EventDomain eventDomain) {
        boolean isEventDeleted = eventDomain.isDeleted();
        boolean isEventIdEmpty = eventDomain.getId() == null || eventDomain.getId().isEmpty();
        boolean isEventCalendarIdEmpty = eventDomain.getCalendarId() == null || eventDomain.getCalendarId().isEmpty();

        if (!isEventIdEmpty && !isEventCalendarIdEmpty) {
            if (isEventDeleted) {
                cancelEvent(eventDomain);
            } else {
                updateEvent(eventDomain);
            }
        }
    }


    private EventDomain createEvent(EventDomain eventDomain) {
        Event event = instantiateEvent(eventDomain);

        try {
            Event eventCreated =
                    googleCalendar.getCalendar().events()
                            .insert(googleCalendar.getCurrentCalendarId(), event)
                            .execute();
            eventDomain.setId(eventCreated.getId());
            eventDomain.setCalendarId(this.googleCalendar.getCurrentCalendarId());

        } catch (GeneralSecurityException | IOException e) {
            log.error("Erro ao criar evento para as agendas {}.\nCliente: {}\nMensagem da API: {}",
                    eventDomain.getAttendees(), eventDomain.getTitle(), e.getMessage());
        }


        return eventDomain;
    }

    private void updateEvent(EventDomain eventDomain) {
        Event event = instantiateEvent(eventDomain);

        try {
            if (findNonCancelledEvent(eventDomain.getId(), eventDomain.getCalendarId()).isPresent()) {
                googleCalendar.getCalendar().events()
                        .update(eventDomain.getCalendarId(), eventDomain.getId(), event)
                        .execute();
            }
        } catch (GeneralSecurityException | IOException e) {
            log.error("Erro ao atualizar evento.\nMensagem da API: {}", e.getMessage());
        }

    }

    /**
     * Cancels an event in the Google Calendar if it is not already cancelled.
     *
     * <p>This method checks if an event with the given ID and calendar ID is not already cancelled.
     * If the event is found and is not cancelled, it deletes the event from the calendar using
     * the Google Calendar API.
     *
     * <p>When an event is cancelled using the Google Calendar API, the following happens:
     * <ul>
     *     <li>The event is removed from the calendar but it is still visible for 30 days in agenda's bin.</li>
     *     <li>The event's <strong>status</strong> is set to "cancelled".</li>
     * </ul>
     *
     * @param eventDomain the event domain object containing the event ID and calendar ID
     */
    private void cancelEvent(EventDomain eventDomain) {

        try {
            if (findNonCancelledEvent(eventDomain.getId(), eventDomain.getCalendarId()).isPresent()) {
                googleCalendar.getCalendar().events()
                        .delete(eventDomain.getCalendarId(), eventDomain.getId())
                        .execute();
            }
        } catch (GeneralSecurityException | IOException e) {
            log.error("Erro ao cancelar evento da agenda. \nMensagem da API: {}", e.getMessage());
        }

    }

    private boolean isEventCancelled(Event event) {
        return event.getStatus().equalsIgnoreCase("cancelled");
    }

    private Optional<Event> findNonCancelledEvent(String eventId, String calendarId) {
        try {
            Event event = googleCalendar.getCalendar().events().get(calendarId, eventId).execute();

            if (event != null && !isEventCancelled(event)) {
                return Optional.of(event);
            }
        } catch (GeneralSecurityException | IOException e) {
            log.error("Erro ao resgatar evento da agenda.\nMensagem da API: {}", e.getMessage());
        }

        return Optional.empty();
    }


    private boolean isEventCreatable(EventDomain eventDomain) {
        boolean isEventDeleted = eventDomain.isDeleted();
        boolean isEventIdEmpty = eventDomain.getId() == null || eventDomain.getId().isEmpty();
        boolean isEventCalendarIdEmpty = eventDomain.getCalendarId() == null || eventDomain.getCalendarId().isEmpty();
        return !isEventDeleted && (isEventIdEmpty || isEventCalendarIdEmpty);
    }

    private void setEventStartAndEndDateTime(Event event, EventDomain eventDomain) {
        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(eventDomain.getStartDate().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .setTimeZone(eventDomain.getStartDate().getZone().getId());

        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(eventDomain.getEndDate().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .setTimeZone(eventDomain.getEndDate().getZone().getId());

        event.setStart(start);
        event.setEnd(end);
    }


    private void setAttendees(Event event, EventDomain eventDomain) {

        if (eventDomain.getAttendees() != null) {
            List<EventAttendee> attendeeList = eventDomain.getAttendees().stream()
                    .map(attendeeDomain -> {
                        EventAttendee attendee = new EventAttendee();
                        attendee.setEmail(attendeeDomain.getEmail());
                        attendee.setDisplayName(attendeeDomain.getName());
                        attendee.setOptional(attendeeDomain.isOptional());
                        attendee.setResponseStatus(attendeeDomain.getResponseStatus());
                        return attendee;
                    })
                    .collect(Collectors.toList());

            event.setAttendees(attendeeList);
        } else {
            event.setAttendees(new ArrayList<EventAttendee>());
        }


    }

    private Event instantiateEvent(EventDomain eventDomain) {
        Event event = new Event();
        event.setSummary(eventDomain.getTitle());
        event.setDescription(eventDomain.getDescription());
        setEventStartAndEndDateTime(event, eventDomain);
        setAttendees(event, eventDomain);
        return event;
    }
}
