package com.iqu.sdk;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.json.JSONObject;

import android.os.Build;
import android.provider.Settings.Secure;

/**
 * IQUIds is a collection that can store an id for every id type.
 */
class IQUIds {
    //
    // PRIVATE VARS
    //

    /**
     * Use array to store ids
     */
    private volatile String[] m_ids;

    /**
     * Size to use for m_ids
     */
    private static volatile int m_count = -1;

    /**
     * Secure.ANDROID_ID, will be created only one time.
     */
    private static volatile String m_androidId = null;

    //
    // CONSTRUCTORS
    //

    /**
     * Creates empty ids container.
     */
    protected IQUIds() {
        // determine size of ids array?
        if (m_count < 0) {
            // yes, just iterate over all items and determine maximum value
            for (IQUIdType type : IQUIdType.values()) {
                // use +1 since the value must be a valid index into the array
                m_count = Math.max(m_count, type.getValue() + 1);
            }
        }
        this.m_ids = new String[m_count];
        this.clearIds();
    }

    /**
     * Creates ids container using another ids container as source.
     * 
     * @param aSource
     *            Source to make copy from.
     */
    private IQUIds(IQUIds aSource) {
        this.m_ids = aSource.m_ids.clone();
    }

    //
    // PROTECTED METHODS
    //

    /**
     * Cleans up references and used resources.
     */
    protected void destroy() {
    }

    /**
     * Returns a id value for a certain type. If the id is not known, an empty
     * string is returned.
     * 
     * @param aType
     *            Id type to get value for.
     * 
     * @return id value or "" if no value was stored for that type.
     */
    protected String get(IQUIdType aType) {
        // handle types that have a fixed value
        switch (aType) {
            case ANDROID_ID:
                if (m_androidId == null) {
                    if (IQUSDK.instance().application() != null) {
                        m_androidId = Secure.getString(IQUSDK.instance().application()
                                .getContentResolver(), Secure.ANDROID_ID);
                        if (m_androidId == null) {
                            m_androidId = "";
                        }
                    }
                }
                return m_androidId == null ? "" : m_androidId;
            case ANDROID_SERIAL:
                return Build.SERIAL;
            default:
                return this.m_ids[aType.getValue()];
        }
    }

    /**
     * Store a value for a certain type. Any previous value is overwritten.
     * 
     * @param aType
     *            Type to store value for.
     * @param aValue
     *            Value to store for the type.
     */
    protected void set(IQUIdType aType, String aValue) {
        switch (aType) {
            case ANDROID_ID:
            case ANDROID_SERIAL:
                // don't store ids with fixed values
                break;
            default:
                this.m_ids[aType.getValue()] = aValue == null ? "" : aValue;
                break;
        }
    }

    /**
     * Save the ids.
     * 
     * @param anOutput
     *            Output to write values to.
     * 
     * @throws IOException
     *             (if saving fails)
     */
    protected void save(DataOutput anOutput) throws IOException {
        // process each type and write those with non empty values
        for (IQUIdType type : IQUIdType.values()) {
            String value = this.get(type);
            if (value.length() > 0) {
                anOutput.writeByte(type.getValue());
                anOutput.writeUTF(value);
            }
        }
        // store -1 to indicate there are no more keys
        anOutput.writeByte(-1);
    }

    /**
     * Load the ids.
     * 
     * @param anInput
     *            Input to read value from.
     * 
     * @throws IOException
     *             (if loading fails)
     */
    protected void load(DataInput anInput) throws IOException {
        this.clearIds();
        for (int key = anInput.readByte(); key >= 0; key = anInput.readByte()) {
            this.m_ids[key] = anInput.readUTF();
        }
    }

    /**
     * Returns a copy of this instance.
     * 
     * @return IQUIds instance containing the same ids.
     */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException",
      "CloneDoesntCallSuperClone"})
    protected IQUIds clone() {
        return new IQUIds(this);
    }

    /**
     * Returns ids as JSON formatted string; only non empty ids are returned.
     * 
     * @return JSON formatted string
     */
    protected String toJSONString() {
        // use JSON object to create JSON string
        JSONObject json = new JSONObject();
        try {
            for (IQUIdType type : IQUIdType.values()) {
                String value = this.get(type);
                if (value.length() != 0) {
                    json.put(this.getJSONName(type), value);
                }
            }
        } catch (Exception ignore) {
        }
        return json.toString();
    }

    //
    // PRIVATE METHODS
    //

    /**
     * Returns property name for use with JSON formatted definitions.
     * 
     * @param aType
     *            Type to get JSON name for
     * 
     * @return name for use with JSON formatted definitions.
     */
    private String getJSONName(IQUIdType aType) {
        switch (aType) {
            case ANDROID_AD_TRACKING:
                return "android_ad_tracking";
            case ANDROID_ADVERTISING:
                return "android_advertiser_id";
            case ANDROID_ID:
                return "android_id";
            case ANDROID_SERIAL:
                return "android_serial";
            case CUSTOM:
                return "custom_user_id";
            case FACEBOOK:
                return "facebook_user_id";
            case GOOGLE_PLUS:
                return "google_plus_user_id";
            case SDK:
                return "iqu_sdk_id";
            case TWITTER:
                return "twitter_user_id";
            default:
                return aType.toString();
        }
    }

    /**
     * Clear all stored ids.
     */
    private void clearIds() {
        for (int index = 0; index < this.m_ids.length; index++) {
            this.m_ids[index] = "";
        }
    }
}
