package org.newgo.configuration;


import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Setter(AccessLevel.PRIVATE)
public class GoogleCalendar {

    private static final String APPLICATION_NAME = "Qualisan Google Calendar";

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static String CREDENTIALS_FILE_PATH;

    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    private static Calendar calendarService;

    @Getter(AccessLevel.PRIVATE)
    private static com.google.api.services.calendar.model.Calendar calendarModel;

    private static final String ZONE_ID = "America/Sao_Paulo";

    private static String EMAIL_SENDER;

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendar.class);


    static {
        loadProperties();
    }

    public GoogleCalendar() {
        try {
                GoogleCredentials credentials = getCredentials();

                calendarService = new Calendar.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        JSON_FACTORY,
                        new HttpCredentialsAdapter(credentials))
                        .setApplicationName(APPLICATION_NAME)
                        .build();

                initializeCalendarModel();
        } catch (IOException | GeneralSecurityException | NullPointerException e) {
            logger.error("Error trying to create Google Calendar Service...", e);
        }
    }

    public String getCurrentCalendarId() {
        return calendarModel.getId();
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

    /**
     * Returns the environment variable value if the property is in the format ${ENV_VAR_NAME},
     * otherwise returns the property value as-is.
     *
     * @param propertyValue the property value that may contain an environment variable reference
     * @return the resolved value (from environment variable or the original value)
     */
    private static String getEnvOrDefault(String propertyValue) {
        if (propertyValue != null && propertyValue.startsWith("${") && propertyValue.endsWith("}")) {
            String envVarName = propertyValue.substring(2, propertyValue.length() - 1);
            String envValue = System.getenv(envVarName);
            return envValue != null ? envValue : propertyValue;
        }
        return propertyValue;
    }

    public Calendar getCalendar() throws IOException, GeneralSecurityException {
        return calendarService;
    }

    /**
     * Creates a new Calendar model with default settings.
     *
     * @return a new Calendar model configured with application name, timezone, and description
     */
    private com.google.api.services.calendar.model.Calendar createDefaultCalendar() {
        com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
        calendar.setSummary(APPLICATION_NAME);
        calendar.setTimeZone(ZONE_ID);
        calendar.setDescription("Qualisan Google Calendar");
        return calendar;
    }

    /**
     * Initializes the calendar model by retrieving an existing calendar or creating a new one.
     *
     * @throws IOException if there is an error communicating with the Google Calendar API
     */
    private void initializeCalendarModel() throws IOException {
        List<com.google.api.services.calendar.model.CalendarListEntry> availableCalendars =
                calendarService.calendarList().list().execute().getItems();

        if (availableCalendars != null && !availableCalendars.isEmpty()) {
            calendarModel = calendarService.calendars()
                    .get(availableCalendars.get(0).getId()).execute();
        } else {
            calendarModel = calendarService.calendars().insert(createDefaultCalendar()).execute();
        }
    }

    private GoogleCredentials getCredentials() throws IOException {
        return GoogleCredentials
                .fromStream(Files.newInputStream(Paths.get(CREDENTIALS_FILE_PATH)))
                .createScoped(SCOPES)
                .createDelegated(EMAIL_SENDER);
    }


}
