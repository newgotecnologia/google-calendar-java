package org.newgo.event;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

public interface EventService {
    Optional<EventDomain> processEvent(EventDomain eventDomain) throws GeneralSecurityException, IOException;

}
