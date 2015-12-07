package com.iqu.sdk;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.json.JSONObject;

/**
 * IQUMessage encapsulates a single message that will be sent to the IQU server.
 * A message consists of an event and several ids. The event will not change,
 * the ids might change before the message is sent.
 */
@SuppressWarnings("unused")
class IQUMessage {
    //
    // PRIVATE VARIABLES
    //

    /**
     * See property definition.
     */
    private IQUMessage m_next;

    /**
     * The event (as JSON string)
     */
    private String m_event;

    /**
     * The event type.
     */
    private String m_eventType;

    /**
     * The ids.
     */
    private IQUIds m_ids;

    /**
     * Queue message is currently in.
     */
    private IQUMessageQueue m_queue;

    //
    // PROTECTED CONSTRUCTOR
    //

    /**
     * Initializes a new message instance and set the ids and event.
     * 
     * @param anIds
     *            Ids to use (a copy is stored)
     * @param anEvent
     *            Event the message encapsulates
     */
    protected IQUMessage(IQUIds anIds, JSONObject anEvent) {
        // store event as JSON string (no need to convert it every time)
        this.m_event = anEvent.toString();
        // get type
        this.m_eventType = anEvent.optString("type", "");
        // use copy of ids.
        this.m_ids = anIds.clone();
        // no queue
        this.m_queue = null;
    }

    /**
     * Initializes an empty message instance.
     */
    protected IQUMessage() {
        this.m_event = "";
        this.m_eventType = "";
        this.m_ids = new IQUIds();
        this.m_queue = null;
    }

    //
    // PROTECTED METHODS
    //

    /**
     * Removes references and resources.
     */
    protected void destroy() {
        this.m_next = null;
        this.m_queue = null;
        if (this.m_ids != null) {
            this.m_ids.destroy();
            this.m_ids = null;
        }
    }

    /**
     * Save message data. This will reset the dirty stored state.
     * 
     * @param anOutput
     *            Object instance implementing the DataOutput interface.
     * 
     * @throws IOException
     *             (if saving fails)
     */
    protected void save(DataOutput anOutput) throws IOException {
        anOutput.writeUTF(this.m_event);
        anOutput.writeUTF(this.m_eventType);
        this.m_ids.save(anOutput);
    }

    /**
     * Load message data.
     * 
     * @param anInput
     *            Object instance implementing the DataInput interface.
     * 
     * @throws IOException
     *             (if loading fails)
     */
    protected void load(DataInput anInput) throws IOException {
        this.m_event = anInput.readUTF();
        this.m_eventType = anInput.readUTF();
        this.m_ids.load(anInput);
    }

    /**
     * Update an id with a new value. For certain types the id only gets updated
     * if it is empty.
     * 
     * @param aType
     *            Type to update
     * @param aNewValue
     *            New value to use
     */
    protected void updateId(IQUIdType aType, String aNewValue) {
        // get current value and exit for certain types if the current value is
        // not empty.
        String currentValue = this.m_ids.get(aType);
        switch (aType) {
            case CUSTOM:
            case FACEBOOK:
            case TWITTER:
            case GOOGLE_PLUS:
            case SDK:
                if (currentValue.length() > 0) {
                    return;
                }
                break;
            default:
                // prevent lint warning
                break;
        }
        if (!currentValue.equals(aNewValue)) {
            this.m_ids.set(aType, aNewValue);
            // message changed
            if (this.m_queue != null) {
                this.m_queue.onMessageChanged(this);
            }
        }

    }

    /**
     * Returns the ids and event as JSON formatted string, using the following
     * format:
     * <p>
     * { "identifiers":{..}, "event":{..} }
     * </p>
     * <p>
     * The dirty JSON state will be reset.
     * </p>
     * 
     * @return JSON formatted object definition string
     */
    protected String toJSONString() {
        return "{" + "\"identifiers\":" + this.m_ids.toJSONString() + "," + "\"event\":"
                + this.m_event + "}";
    }

    //
    // PROTECTED PROPERTIES
    //

    /**
     * The next property contains the next message in the linked list chain.
     * 
     * @return next message or null if there is no next message
     */
    protected IQUMessage getNext() {
        return this.m_next;
    }

    /**
     * Sets the next property.
     * 
     * @param aValue
     *            Next message or null to clear next message.
     */
    protected void setNext(IQUMessage aValue) {
        this.m_next = aValue;
    }

    /**
     * The queue property contains the queue the message is currently part of.
     * 
     * @return current queue
     */
    protected IQUMessageQueue getQueue() {
        return this.m_queue;
    }

    /**
     * Sets the queue property.
     * 
     * @param aValue
     *            New queue value.
     */
    protected void setQueue(IQUMessageQueue aValue) {
        this.m_queue = aValue;
    }

    /**
     * The eventType property contains the type of event or an empty string if
     * the type could not be determined.
     * 
     * @return event type or "" if it is unknown.
     */
    protected String getEventType() {
        return this.m_eventType;
    }
}
