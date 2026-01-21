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

/**
 * Implementation of the event service for Google Calendar integration.
 *
 * <p>This class provides functionalities to create, update, and cancel events
 * in Google Calendar through the official Google API.
 *
 * @author Marcelo
 * @version 1.0
 * @see EventService
 * @see GoogleCalendar
 */
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);
    private final GoogleCalendar googleCalendar;

    /**
     * Default constructor that initializes the Google Calendar connection.
     *
     * <p>Creates a new instance of {@link GoogleCalendar} to manage
     * communication with the Google Calendar API.
     */
    public EventServiceImpl() {
        this.googleCalendar = new GoogleCalendar();
    }

    /**
     * Processes an event, determining whether it should be created, updated, or deleted.
     *
     * <p>This method is the main entry point for event processing.
     * It analyzes the state of the {@link EventDomain} and decides the appropriate action:
     * <ul>
     *     <li>If the event is creatable (new), creates the event in Google Calendar</li>
     *     <li>If the event already exists, processes update or deletion as needed</li>
     * </ul>
     *
     * @param eventDomain the domain object containing the event data to be processed
     * @return an {@link Optional} containing the created {@link EventDomain} with ID and calendarId
     *         populated, or {@link Optional#empty()} if the event was updated or deleted
     */
    public Optional<EventDomain> processEvent(EventDomain eventDomain) {
        if (isEventCreatable(eventDomain)) {
            return Optional.of(createEvent(eventDomain));
        }
        processEventUpdateOrDeletion(eventDomain);
        return Optional.empty();
    }

    /**
     * Processes the update or deletion of an existing event.
     *
     * <p>Checks if the event has valid ID and calendarId before processing.
     * If the event is marked as deleted, cancels the event; otherwise,
     * updates the event with the new information.
     *
     * @param eventDomain the domain object containing the event data
     */
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

    /**
     * Creates a new event in Google Calendar.
     *
     * <p>Instantiates a Google API {@link Event} object from the {@link EventDomain},
     * inserts the event into the current calendar, and updates the eventDomain with
     * the generated ID and the used calendarId.
     *
     * @param eventDomain the domain object containing the event data to be created
     * @return the {@link EventDomain} updated with the created event ID and calendarId
     */
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
            log.error("Error creating event for calendars {}.\nClient: {}\nAPI message: {}",
                    eventDomain.getAttendees(), eventDomain.getTitle(), e.getMessage());
        }


        return eventDomain;
    }

    /**
     * Updates an existing event in Google Calendar.
     *
     * <p>Checks if the event exists and is not cancelled before performing the update.
     * If the event is found, updates all its properties with the new values
     * from the {@link EventDomain}.
     *
     * @param eventDomain the domain object containing the updated event data
     */
    private void updateEvent(EventDomain eventDomain) {
        Event event = instantiateEvent(eventDomain);

        try {
            if (findNonCancelledEvent(eventDomain.getId(), eventDomain.getCalendarId()).isPresent()) {
                googleCalendar.getCalendar().events()
                        .update(eventDomain.getCalendarId(), eventDomain.getId(), event)
                        .execute();
            }
        } catch (GeneralSecurityException | IOException e) {
            log.error("Error updating event.\nAPI message: {}", e.getMessage());
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
            log.error("Error cancelling calendar event.\nAPI message: {}", e.getMessage());
        }

    }

    /**
     * Checks if an event has a cancelled status.
     *
     * @param event the Google API {@link Event} object to be checked
     * @return {@code true} if the event status is "cancelled", {@code false} otherwise
     */
    private boolean isEventCancelled(Event event) {
        return event.getStatus().equalsIgnoreCase("cancelled");
    }

    /**
     * Finds a non-cancelled event in Google Calendar.
     *
     * <p>Retrieves the event by ID and calendarId and verifies that it is not cancelled.
     * This method is used to validate the existence of an event before
     * performing update or cancellation operations.
     *
     * @param eventId the unique identifier of the event in Google Calendar
     * @param calendarId the identifier of the calendar where the event is stored
     * @return an {@link Optional} containing the {@link Event} if found and not cancelled,
     *         or {@link Optional#empty()} if the event does not exist or is cancelled
     */
    private Optional<Event> findNonCancelledEvent(String eventId, String calendarId) {
        try {
            Event event = googleCalendar.getCalendar().events().get(calendarId, eventId).execute();

            if (event != null && !isEventCancelled(event)) {
                return Optional.of(event);
            }
        } catch (GeneralSecurityException | IOException e) {
            log.error("Error retrieving calendar event.\nAPI message: {}", e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Checks if an event can be created in Google Calendar.
     *
     * <p>An event is considered creatable when:
     * <ul>
     *     <li>It is not marked as deleted</li>
     *     <li>It does not have an ID defined (it is a new event)</li>
     *     <li>Or it does not have a calendarId defined</li>
     * </ul>
     *
     * @param eventDomain the event domain object to be checked
     * @return {@code true} if the event can be created, {@code false} otherwise
     */
    private boolean isEventCreatable(EventDomain eventDomain) {
        boolean isEventDeleted = eventDomain.isDeleted();
        boolean isEventIdEmpty = eventDomain.getId() == null || eventDomain.getId().isEmpty();
        boolean isEventCalendarIdEmpty = eventDomain.getCalendarId() == null || eventDomain.getCalendarId().isEmpty();
        return !isEventDeleted && (isEventIdEmpty || isEventCalendarIdEmpty);
    }

    /**
     * Sets the start and end dates and times of the event.
     *
     * <p>Converts the dates from the {@link EventDomain} to the format expected by the
     * Google Calendar API, including timezone information.
     *
     * @param event the Google API {@link Event} object to be configured
     * @param eventDomain the domain object containing the start and end dates
     */
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

    /**
     * Configures the list of event attendees.
     *
     * <p>Converts the list of {@link org.newgo.attendee.AttendeeDomain} from the domain event
     * to a list of {@link EventAttendee} from the Google Calendar API.
     * Each attendee is configured with email, display name, optional status,
     * and response status.
     *
     * <p>If the attendee list is null, an empty list is assigned to the event.
     *
     * @param event the Google API {@link Event} object to be configured
     * @param eventDomain the domain object containing the list of attendees
     */
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
            event.setAttendees(new ArrayList<>());
        }


    }

    /**
     * Instantiates and configures a Google Calendar API {@link Event} object.
     *
     * <p>Creates a new event and configures its basic properties:
     * <ul>
     *     <li>Title (summary)</li>
     *     <li>Description</li>
     *     <li>Start and end date/time</li>
     *     <li>List of attendees</li>
     * </ul>
     *
     * @param eventDomain the domain object containing the event data
     * @return a configured {@link Event} object ready to be sent to the API
     */
    private Event instantiateEvent(EventDomain eventDomain) {
        Event event = new Event();
        event.setSummary(eventDomain.getTitle());
        event.setDescription(eventDomain.getDescription());
        setEventStartAndEndDateTime(event, eventDomain);
        setAttendees(event, eventDomain);
        return event;
    }
}
