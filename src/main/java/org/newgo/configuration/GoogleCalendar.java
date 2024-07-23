package org.newgo.configuration;


import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;


import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

@Setter(AccessLevel.PRIVATE)
public class GoogleCalendar {

    private static final String APPLICATION_NAME = "Qualisan Google Calendar";

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static String CREDENTIALS_FILE_PATH;

    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    private static Calendar calendarService;

    @Getter(AccessLevel.PRIVATE)
    private com.google.api.services.calendar.model.Calendar calendarModel;

    private static final String ZONE_ID = "America/Sao_Paulo";

    private static String EMAIL_SENDER;

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendar.class);


    static {
        loadProperties();
    }

    public GoogleCalendar() throws GeneralSecurityException, IOException {
        if (calendarService == null) {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            calendarService = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(getCredentials()))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            setCalendarModel();
        }
    }

    public String getCurrentCalendarId() {
        return this.calendarModel.getId();
    }

    private static void loadProperties() {
        logger.info("Loading properties from application.properties...");
        Properties properties = new Properties();

        try (InputStream input = GoogleCalendar.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.error("Error trying to find application.properties file...");
                return;
            }

            // Load the properties file
            properties.load(input);
            CREDENTIALS_FILE_PATH = properties.getProperty("google.calendar.serviceAccount");
            EMAIL_SENDER = properties.getProperty("google.calendar.emailSender");

            // Override with environment variables if they exist
            CREDENTIALS_FILE_PATH = getEnvOrDefault(CREDENTIALS_FILE_PATH);
            EMAIL_SENDER = getEnvOrDefault(EMAIL_SENDER);

        } catch (IOException ex) {
            logger.error("Error trying to load properties...", ex);
        }
    }

    private static String getEnvOrDefault(String propertyValue) {
        if (propertyValue != null) {
            return System.getenv(propertyValue);
        }
        return null;
    }

    public Calendar getCalendar() throws IOException, GeneralSecurityException {
        return calendarService;
    }

    private com.google.api.services.calendar.model.Calendar calendar() {
        if (calendarModel == null) {
            logger.info("Creating Google Calendar Model...");
            com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
            calendar.setSummary(APPLICATION_NAME);
            calendar.setTimeZone(ZONE_ID);
            calendar.setDescription("Qualisan Google Calendar");
            setCalendarModel(calendar);
        }

        return getCalendarModel();
    }

    private void setCalendarModel() throws IOException {
        List<com.google.api.services.calendar.model.CalendarListEntry> availableCalendars =
                calendarService.calendarList().list().execute().getItems();


        if (!availableCalendars.isEmpty()) {
            this.calendarModel = calendarService.calendars()
                    .get(availableCalendars.get(0).getId()).execute();


        } else {
            this.calendarModel = calendarService.calendars().insert(calendar()).execute();
<<<<<<< HEAD
=======
            System.out.println(this.calendarModel);
>>>>>>> be8536386b8e1080bded9036b2a2dd46ed21c1ff
        }
    }

    private GoogleCredentials getCredentials() throws IOException {
        InputStream in = GoogleCalendar.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }

        return GoogleCredentials.fromStream(in).createScoped(SCOPES)
                .createDelegated(EMAIL_SENDER);
    }


}
