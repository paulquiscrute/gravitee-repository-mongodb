/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.mongodb.management.internal.event.EventMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.EventMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Titouan COMPIEGNE
 */
@Component
public class MongoEventRepository implements EventRepository {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private EventMongoRepository internalEventRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Event> findById(String id) throws TechnicalException {
        logger.debug("Find event by ID [{}]", id);

        EventMongo event = internalEventRepo.findOne(id);
        Event res = mapEvent(event);

        logger.debug("Find event by ID [{}] - Done", id);
        return Optional.ofNullable(res);
    }

    @Override
    public Event create(Event event) throws TechnicalException {
        logger.debug("Create event [{}]", event.getId());

        EventMongo eventMongo = mapEvent(event);
        EventMongo createdEventMongo = internalEventRepo.insert(eventMongo);

        Event res = mapEvent(createdEventMongo);

        logger.debug("Create event [{}] - Done", event.getId());

        return res;
    }

    @Override
    public Event update(Event event) throws TechnicalException {
        if (event == null || event.getId() == null) {
            throw new IllegalStateException("Event to update must have an id");
        }

        EventMongo eventMongo = internalEventRepo.findOne(event.getId());
        if (eventMongo == null) {
            throw new IllegalStateException(String.format("No event found with id [%s]", event.getId()));
        }

        try {
            eventMongo.setProperties(event.getProperties());
            eventMongo.setType(event.getType().toString());
            eventMongo.setPayload(event.getPayload());
            eventMongo.setParentId(event.getParentId());
            eventMongo.setUpdatedAt(event.getUpdatedAt());
            EventMongo eventMongoUpdated = internalEventRepo.save(eventMongo);
            return mapEvent(eventMongoUpdated);
        } catch (Exception e) {
            logger.error("An error occured when updating event", e);
            throw new TechnicalException("An error occured when updating event");
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalEventRepo.delete(id);
        } catch (Exception e) {
            logger.error("An error occured when deleting event [{}]", id, e);
            throw new TechnicalException("An error occured when deleting event");
        }
    }

    @Override
    public Set<Event> findByType(List<EventType> eventTypes) {
        List<String> types = new ArrayList<String>();
        for (EventType eventType : eventTypes) {
            types.add(eventType.toString());
        }
        Collection<EventMongo> eventsMongo = internalEventRepo.findByType(types);
        return mapEvents(eventsMongo);
    }

    @Override
    public Set<Event> findByProperty(String key, String value) {
        Collection<EventMongo> eventsMongo = internalEventRepo.findByProperty(key, value);

        return mapEvents(eventsMongo);
    }

    public Page<Event> search(Map<String, Object> values, long from, long to, int page, int size) {
        Page<EventMongo> eventsMongo = internalEventRepo.search(values, from, to, page, size);

        List<Event> content = mapper.collection2list(eventsMongo.getContent(), EventMongo.class, Event.class);
        Page<Event> eventsPage = new Page(content, page, size, eventsMongo.getTotalElements());

        return eventsPage;
    }

    private Set<Event> mapEvents(Collection<EventMongo> events) {
        return events.stream().map(this::mapEvent).collect(Collectors.toSet());
    }

    private EventMongo mapEvent(Event event) {
        if (event == null) {
            return null;
        }

        EventMongo eventMongo = new EventMongo();
        eventMongo.setId(event.getId());
        eventMongo.setType(event.getType().toString());
        eventMongo.setPayload(event.getPayload());
        eventMongo.setParentId(event.getParentId());
        eventMongo.setProperties(event.getProperties());
        eventMongo.setCreatedAt(event.getCreatedAt());
        eventMongo.setUpdatedAt(event.getUpdatedAt());

        return eventMongo;
    }

    private Event mapEvent(EventMongo eventMongo) {
        if (eventMongo == null) {
            return null;
        }

        Event event = new Event();
        event.setId(eventMongo.getId());
        event.setType(EventType.valueOf(eventMongo.getType()));
        event.setPayload(eventMongo.getPayload());
        event.setParentId(eventMongo.getParentId());
        event.setProperties(eventMongo.getProperties());
        event.setCreatedAt(eventMongo.getCreatedAt());
        event.setUpdatedAt(eventMongo.getUpdatedAt());

        return event;
    }

}
