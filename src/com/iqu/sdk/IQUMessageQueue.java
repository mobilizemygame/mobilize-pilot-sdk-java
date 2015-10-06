package com.iqu.sdk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.content.Context;

/**
 * IQUMessageQueue contains a list of IQUMessage instances. It can store the
 * messages to a local storage and return the whole list as a JSON string.
 */
class IQUMessageQueue {
    //
    // PRIVATE CONST
    //

    /**
     * Name of file to store the messages in.
     */
    private static final String FILE_NAME = "IQUSDK_messages.bin";

    /**
     * Version of stored data. This value should be increased whenever the
     * format of the stored messages changes.
     */
    private static final int FILE_VERSION = 1;

    //
    // PRIVATE VARS
    //

    /**
     * Points to first message in chain.
     */
    private IQUMessage m_first;

    /**
     * Points to last message in chain.
     */
    private IQUMessage m_last;

    /**
     * Cached JSON string.
     */
    private String m_cachedJSONString;

    /**
     * When true the JSON string should be recreated.
     */
    private boolean m_dirtyJSON;

    /**
     * When true the queue should save itself to persistent storage.
     */
    private boolean m_dirtyStored;

    //
    // CONSTRUCTOR
    //

    /**
     * Initializes the instance to an empty queue.
     */
    protected IQUMessageQueue() {
        this.reset();
    }

    //
    // PROTECTED METHODS
    //

    /**
     * Cleans up references and used resources.
     */
    protected void destroy() {
        this.clear(false);
    }

    /**
     * Checks if the queue is empty.
     * 
     * @return <code>true</code> when there are no messages in the queue.
     */
    protected boolean isEmpty() {
        return this.m_last == null;
    }

    /**
     * Counts the number of messages in the queue.
     * 
     * @return number of messages
     */
    protected int getCount() {
        int result = 0;
        for (IQUMessage message = this.m_first; message != null; message = message.getNext()) {
            result++;
        }
        return result;
    }

    /**
     * Add a message to end of queue.
     * 
     * @param aMessage
     *            The message to add to the end.
     */
    protected void add(IQUMessage aMessage) {
        if (this.m_first == null) {
            this.m_first = aMessage;
        }
        if (this.m_last != null) {
            this.m_last.setNext(aMessage);
        }
        this.m_last = aMessage;
        // update queue property so the IQUMessage will call onMessageChanged
        aMessage.setQueue(this);
        // queue has changed.
        this.m_dirtyJSON = true;
        this.m_dirtyStored = true;
    }

    /**
     * Prepend a queue before the current queue. This will move the items from
     * aQueue to this queue.
     * <p>
     * After this call, aQueue will be empty.
     * </p>
     * 
     * @param aQueue
     *            The queue to prepend before this queue.
     * @param aChangeQueue
     *            When <code>true</code> change the queue property in every
     *            message to this queue.
     */
    protected void prepend(IQUMessageQueue aQueue, boolean aChangeQueue) {
        if (!aQueue.isEmpty()) {
            // if this queue is empty, copy cached JSON string and dirty state;
            // else reset it.
            if (this.m_first == null) {
                this.m_cachedJSONString = aQueue.m_cachedJSONString;
                this.m_dirtyJSON = aQueue.m_dirtyJSON;
                this.m_dirtyStored = aQueue.m_dirtyStored;
            } else {
                this.m_cachedJSONString = null;
                this.m_dirtyJSON = true;
                this.m_dirtyStored = true;
            }
            // get first and last
            IQUMessage first = aQueue.m_first;
            IQUMessage last = aQueue.m_last;
            // this queue is empty?
            if (this.m_last == null) {
                // yes, just copy last
                this.m_last = last;
            } else {
                // add the first message in the chain to the chain in aQueue
                last.setNext(this.m_first);
            }
            // chain starts now with the first message in the chain of aQueue
            this.m_first = first;
            // update queue property?
            if (aChangeQueue) {
                for (IQUMessage message = first; message != null; message = message.getNext()) {
                    message.setQueue(this);
                }
            }
            // aQueue is now empty
            aQueue.reset();
        }
    }

    /**
     * Destroy the queue. It will call destroy on every message and remove any
     * reference to each message instance.
     * <p>
     * After this method, the queue will be empty and can be filled again.
     * </p>
     * 
     * @param aClearStorage
     *            When <code>true</code> clear the persistently stored messages.
     */
    protected void clear(boolean aClearStorage) {
        IQUMessage message = this.m_first;
        while (message != null) {
            IQUMessage next = message.getNext();
            message.destroy();
            message = next;
        }
        this.reset();
        // delete the local file
        if (aClearStorage) {
            IQUSDK.instance().application().deleteFile(FILE_NAME);
        }
    }

    /**
     * Clears the references to the messages, but don't destroy the messages
     * themselves.
     * <p>
     * After this method the queue will be empty.
     * </p>
     */
    protected void reset() {
        this.m_first = null;
        this.m_last = null;
        this.m_cachedJSONString = null;
        this.m_dirtyJSON = false;
        this.m_dirtyStored = false;
    }

    /**
     * Saves the messages to persistent storage. This method only performs the
     * save if new messages have been added or one of the messages changed.
     */
    protected void save() {
        // only store if at least one messages stored state is dirty
        if (this.m_dirtyStored) {
            try {
                // create stream and data stream
                FileOutputStream fileStream = IQUSDK.instance().application()
                        .openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
                DataOutputStream dataStream = new DataOutputStream(fileStream);
                // store version
                dataStream.writeInt(FILE_VERSION);
                // get count
                int count = this.getCount();
                dataStream.writeInt(count);
                // store the messages
                for (IQUMessage message = this.m_first; message != null; message = message
                        .getNext()) {
                    message.save(dataStream);
                }
                dataStream.close();
                fileStream.close();
                // no longer dirty
                this.m_dirtyStored = false;
                // debug info
                if (IQUSDK.DEBUG) {
                    IQUSDK.instance().addLog("[Queue] saved " + count + " message(s)");
                }
            } catch (Exception error) {
                if (IQUSDK.DEBUG) {
                    IQUSDK.instance().addLog(
                            "[Queue][Error] While saving: " + error.getClass().getName() + ": "
                                    + error.getMessage());
                }
            }
        }
    }

    /**
     * Loads the messages from persistent storage.
     */
    protected void load() {
        try {
            // get reference to file
            File file = IQUSDK.instance().application().getFileStreamPath(FILE_NAME);
            // only load from it if the file does actually exists
            if (file.exists()) {
                FileInputStream fileStream = new FileInputStream(file);
                DataInputStream dataStream = new DataInputStream(fileStream);
                // get version
                int version = dataStream.readInt();
                // only process if the file version is correct
                if (version == FILE_VERSION) {
                    // clear current list (but don't destroy the file)
                    this.clear(false);
                    // get number of stored message
                    int messageCount = dataStream.readInt();
                    // create and load messages
                    for (int count = messageCount; count > 0; count--) {
                        IQUMessage message = new IQUMessage();
                        message.load(dataStream);
                        this.add(message);
                    }
                    // debug info
                    if (IQUSDK.DEBUG) {
                        IQUSDK.instance().addLog("[Queue] loaded " + messageCount + " message(s)");
                    }
                } else {
                    // clear current list and destroy file (since it is no
                    // longer supported)
                    this.clear(true);
                    // debug info
                    if (IQUSDK.DEBUG) {
                        IQUSDK.instance().addLog(
                                "[Queue] no messages were loaded, file uses unsupported version ("
                                        + version + "), current version is " + FILE_VERSION);
                    }
                }
                dataStream.close();
                fileStream.close();
                // no need to save the just loaded messages
                this.m_dirtyStored = false;
            }
        } catch (Exception error) {
            if (IQUSDK.DEBUG) {
                IQUSDK.instance().addLog(
                        "[Queue][Error] While loading: " + error.getClass().getName() + ": "
                                + error.getMessage());
            }
        }
    }

    /**
     * Returns the queue as a JSON formatted string.
     * 
     * @return JSON formatted string.
     */
    protected String toJSONString() {
        // rebuild string if there is none or one or more messages became dirty.
        if ((this.m_cachedJSONString == null) || this.m_dirtyJSON) {
            this.m_cachedJSONString = this.buildJSONString();
            this.m_dirtyJSON = false;
        }
        return this.m_cachedJSONString;
    }

    /**
     * Update an id within all the stored messages.
     * 
     * @param aType
     *            Id type to update value for.
     * @param aNewValue
     *            New value to use.
     */
    protected void updateId(IQUIdType aType, String aNewValue) {
        for (IQUMessage message = this.m_first; message != null; message = message.getNext()) {
            message.updateId(aType, aNewValue);
        }
    }

    /**
     * Checks if queue contains at least one message for a certain event type.
     * 
     * @param aType
     *            Event type to check
     * 
     * @return <code>true</code> if there is at least one message,
     *         <code>false</code> if not.
     */
    protected boolean hasEventType(String aType) {
        for (IQUMessage message = this.m_first; message != null; message = message.getNext()) {
            if (message.getEventType().equals(aType))
                return true;
        }
        return false;
    }

    //
    // EVENT HANDLERS
    //

    /**
     * This handler is called by IQUMessage when the contents changes.
     * 
     * @param aMessage
     *            Message with changed content
     */
    protected void onMessageChanged(IQUMessage aMessage) {
        this.m_dirtyJSON = true;
        this.m_dirtyStored = true;
    }

    //
    // PRIVATE METHODS
    //

    /**
     * Builds JSON formatted definition string from all messages in the queue.
     * It creates the following format:
     * <p>
     * [ {...},{...},... ]
     * </p>
     * 
     * @return JSON formatted definition string.
     */
    private String buildJSONString() {
        StringBuilder result = new StringBuilder();
        result.append('[');
        boolean notEmpty = false;
        for (IQUMessage message = this.m_first; message != null; message = message.getNext()) {
            if (notEmpty) {
                result.append(',');
            }
            result.append(message.toJSONString());
            notEmpty = true;
        }
        result.append(']');
        return result.toString();
    }
}
