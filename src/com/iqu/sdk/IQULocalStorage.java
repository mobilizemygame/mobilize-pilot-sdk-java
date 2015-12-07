package com.iqu.sdk;

import android.content.SharedPreferences;

/**
 * IQULocalStorage encapsulates a local storage specific to the SDK target. 
 */
@SuppressWarnings("unused")
class IQULocalStorage {
    //
    // PRIVATE VARS
    //

    /**
     * Local storage implementation for Android.
     */
    private SharedPreferences m_preferences = null;
    
    /**
     * Shortcut to editor, used to set values.
     */
    private SharedPreferences.Editor m_editor = null;
    
    //
    // PROTECTED METHODS
    //
    
    /**
     * Creates local storage instance.
     * 
     * @param aPreferences
     *            The local storage object for Android
     */
    protected IQULocalStorage(SharedPreferences aPreferences) {
        this.m_preferences = aPreferences;
        this.m_editor = null;
    }
    
    /**
     * Cleans up references and used resources.
     */
    protected void destroy() {
        this.m_preferences = null;
        this.m_editor = null;
    }

    /**
     * Gets a String.
     * 
     * @param aKey
     *            A key to get the String for.
     * @param aDefault
     *            A default value to use when there is no String stored for the key.
     * 
     * @return The stored String or aDefault.
     */
    protected String getString(String aKey, String aDefault) {
        return this.m_preferences.getString(aKey, aDefault);
    }

    /**
     * Gets a String.
     * 
     * @param aKey
     *            A key to get the String for.
     * 
     * @return The stored String or "" if none could be found.
     */
    protected String getString(String aKey) {
        return this.getString(aKey, "");
    }

    /**
     * Gets an integer.
     * 
     * @param aKey
     *            A key to get the integer for.
     * @param aDefault
     *            A default value to use when there is no integer stored for the key.
     * 
     * @return The stored integer or aDefault.
     */
    protected int getInt(String aKey, int aDefault) {
        return this.m_preferences.getInt(aKey, aDefault);
    }

    /**
     * Gets an integer.
     * 
     * @param aKey
     *            A key to get the integer for.
     * 
     * @return The stored integer or 0 if none could be found.
     */
    protected int getInt(String aKey) {
        return this.getInt(aKey, 0);
    }

    /**
     * Gets a long.
     * 
     * @param aKey
     *            A key to get the long for.
     * @param aDefault
     *            A default value to use when there is no long stored for the key.
     * 
     * @return The stored long or aDefault.
     */
    protected long getLong(String aKey, long aDefault) {
        return this.m_preferences.getLong(aKey, aDefault);
    }

    /**
     * Gets a long.
     * 
     * @param aKey
     *            A key to get the long for.
     * 
     * @return The stored long or 0 if none could be found.
     */
    protected long getLong(String aKey) {
        return this.getLong(aKey, 0);
    }

    /**
     * Gets a floating number.
     * 
     * @param aKey
     *            A key to get the floating number for.
     * @param aDefault
     *            A default value to use when there is no floating number stored for the key.
     * 
     * @return The stored floating number or aDefault.
     */
    protected float getFloat(String aKey, float aDefault) {
        return this.m_preferences.getFloat(aKey, aDefault);
    }

    /**
     * Gets a floating number.
     * 
     * @param aKey
     *            A key to get the floating number for.
     * 
     * @return The stored floating number or 0.0 when missing.
     */
    protected float getFloat(String aKey) {
        return this.getFloat(aKey, (float) 0.0);
    }

    /**
     * Gets a boolean.
     * 
     * @param aKey
     *            A key to get the boolean for.
     * @param aDefault
     *            A default value to use when there is no boolean stored for the key.
     * 
     * @return The stored boolean or aDefault.
     */
    protected boolean getBool(String aKey, boolean aDefault) {
        return this.m_preferences.getBoolean(aKey, aDefault);
    }

    /**
     * Gets a boolean.
     * 
     * @param aKey
     *            A key to get the boolean for.
     * 
     * @return The stored boolean or true when missing.
     */
    protected boolean getBool(String aKey) {
        return this.getBool(aKey, true);
    }

    /**
     * Deletes all stored data.
     */
    protected void deleteAll() {
        this.getEditor().clear().apply();
    }

    /**
     * Deletes the data for specific key.
     * 
     * @param aKey
     *            A key to delete the data for.
     */
    protected void deleteKey(String aKey) {
        this.getEditor().remove(aKey).apply();
    }

    /**
     * Checks if there is a locally stored data for a specific key.
     * 
     * @param aKey
     *            A key to check.
     * 
     * @return true if has there is data for the key; otherwise, false.
     */
    protected boolean hasKey(String aKey) {
        return this.m_preferences.contains(aKey);
    }

    /**
     * Flushes any cached data to disc. This method might halt the application, so care should be
     * taken when calling it.
     */
    protected void save() {
        if (this.m_editor != null) this.m_editor.commit();
    }

    /**
     * Stores a floating number in the storage.
     * 
     * @param aKey
     *            Key to store value for.
     * @param aValue
     *            A value to store.
     */
    protected void setFloat(String aKey, float aValue) {
    	this.getEditor().putFloat(aKey, aValue).apply();
    }

    /**
     * Stores an integer in the storage.
     * 
     * @param aKey
     *            Key to store value for.
     * @param aValue
     *            A value to store.
     */
    protected void setInt(String aKey, int aValue) {
    	this.getEditor().putInt(aKey, aValue).apply();
    }

    /**
     * Stores a long in the storage.
     * 
     * @param aKey
     *            Key to store value for.
     * @param aValue
     *            A value to store.
     */
    protected void setLong(String aKey, long aValue) {
    	this.getEditor().putLong(aKey, aValue).apply();
    }

    /**
     * Stores a String in the storage.
     * 
     * @param aKey
     *            Key to store value for.
     * @param aValue
     *            A value to store.
     */
    protected void setString(String aKey, String aValue) {
    	this.getEditor().putString(aKey, aValue).apply();
    }

    /**
     * Stores a boolean in the storage.
     * 
     * @param aKey
     *            Key to store value for.
     * @param aValue
     *            A value to store.
     */
    protected void setBool(String aKey, boolean aValue) {
        this.getEditor().putBoolean(aKey, aValue).apply();
    }
    
    //
    // PRIVATE METHODS
    //
    
    /**
     * Returns an editor to change settings. 
     *  
     * @return SharedPreferences.Editor instance
     */
    private SharedPreferences.Editor getEditor() {
    	if (this.m_editor == null) {
    		this.m_editor = m_preferences.edit();
    	}
    	return this.m_editor;
    }
}
