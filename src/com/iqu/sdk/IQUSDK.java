package com.iqu.sdk;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * IQUSDK is a class that encapsulates the IQU SDK and offers various methods
 * and properties to communicate with the IQU server.
 * <p>
 * All public methods in IQU SDK are thread safe.
 * </p>
 * <p/>
 * <h3>Quick start</h3>
 * <ol>
 * <li>Methods and properties can be accessed trough the static
 * {@link #instance()} method.</li>
 * <li>Call {@link #start(Application, String, String)},
 * {@link #start(Application, String, String, boolean)},
 * {@link #start(Application, String, String, String)} or
 * {@link #start(Application, String, String, String, boolean)} to start the IQU
 * SDK.</li>
 * <li>Add additional Ids via {@link #setFacebookId(String)},
 * {@link #setGooglePlusId(String)}, {@link #setTwitterId(String)} or
 * {@link #setCustomId(String)}.</li>
 * <li>Start calling analytic tracking methods to send messages to the server.</li>
 * <li>Update the {@link #getPayable() payable} property to indicate the player
 * is busy with a payable action.</li>
 * <li>The IQU SDK needs to be notified when the application is minimized to the
 * background or is activated from the background. Call
 * {@link #pause()} from within the Activity.onPause() method and
 * {@link #resume()} from the Activity.onResume() method.</li>
 * <li>To stop the update thread and release references and resources call
 * {@link #terminate()}; after this call the SDK reverts back to an
 * uninitialized state. The {@link #instance()} method will return a new instance
 * and one of the start methods has to be called again to start the SDK.</li>
 * </ol>
 * <h3>Network communication</h3>
 * <p>
 * The IQU SDK uses a separate thread to send messages to the server (to prevent
 * blocking the main thread). This means that there might be a small delay
 * before messages are actually sent to the server. The maximum delay is
 * determined by the {@link #getUpdateInterval() updateInterval} property.
 * </p>
 * <p>
 * If the SDK fails to send a message to the IQU server, messages are queued and
 * are sent when the server is available again. The queued messages are stored
 * in persistent storage so they still can be resent after an application
 * restart.
 * </p>
 * <p>
 * While the IQU SDK is paused (because of a call to {@link #pause()}) no
 * messages are sent. Messages created by one of the trackXXXXX methods are
 * placed in the internal message queue but will only be sent once
 * {@link #resume()} is called.
 * </p>
 * <h3>Ids</h3>
 * <p>
 * The SDK supports various ids which are included with every tracking message
 * sent to the server. See {@link com.iqu.sdk.IQUIdType} for the types supported
 * by the SDK. Use {@link #getId(IQUIdType)} to get an id value.
 * </p>
 * <p>
 * Some ids are determined by the SDK itself, other ids must be set via one of
 * the following methods: {@link #setFacebookId(String)},
 * {@link #setGooglePlusId(String)}, {@link #setTwitterId(String)} or
 * {@link #setCustomId(String)}
 * </p>
 * <p>
 * The SDK supports Google Play services and tries to obtain the advertising id
 * and limited ad tracking setting. The SDK will disable the analytic methods if
 * it successfully obtained the limit ad tracking value and the Android user
 * turned this option on (see {@link #getAnalyticsEnabled()}).
 * </p>
 * <p>
 * The SDK does not use direct links to Google Play methods and classes but
 * instead uses reflection to obtain the advertising ID and limit ad tracking.
 * The SDK will not generate any errors if the Google Play jar files are not
 * included within the application.
 * </p>
 * <h3>Informational properties</h3>
 * <p>
 * The IQU SDK offers the following informational properties:
 * </p>
 * <ul>
 * <li>{@link #getAnalyticsEnabled()} indicates if the the analytics part of the
 * IQU SDK is enabled. When the disabled, the trackXXXXX methods will not send
 * messages. The analytics are disabled when the user enabled limit ad tracking
 * with the Google Play services.</li>
 * <li>
 * {@link #getServerAvailable()} to get information if the messages were sent
 * successfully or not.</li>
 * </ul>
 * <h3>Testing</h3>
 * <p>
 * The IQU SDK contains the following properties to help with testing the SDK:
 * </p>
 * <ul>
 * <li>{@link #getLogEnabled() logEnabled} property to turn logging on or off.</li>
 * <li>{@link #getLog() log} property which will be filled with messages from
 * various methods.</li>
 * <li>{@link #getTestMode() testMode} property to test the SDK without any
 * server interaction or to simulate an offline situation with the server not
 * being available.</li>
 * </ul>
 * <p>
 * The <i>IQUSDK.java</i> file defines a <code>DEBUG</code> constant, if no
 * testing is required this constant can be set to <code>false</code> to allow
 * the compiler optimization to excluded debug specific code.
 * </p>
 * <h3>Advance timing</h3>
 * <p>
 * The IQU SDK offers several properties to adjust the various timings:
 * </p>
 * <ul>
 * <li>{@link #getUpdateInterval()} property determines the time between the
 * internal update calls.</li>
 * <li>{@link #getSendTimeout()} property determines the maximum time sending a
 * message to the server may take.</li>
 * <li>{@link #getCheckServerInterval()} property determines the time between
 * checks for server availability. If sending of data fails, the update thread
 * will wait the time, as set by this property, before trying to send the data
 * again.</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class IQUSDK {
  //
  // PUBLIC CONSTS
  //

  /**
   * SDK version.
   */
  public final static String SDK_VERSION = "1.0.1";

  //
  // PROTECTED CONSTS
  //

  /**
   * Set this variable to false to disable support for log and testMode
   * property.
   */
  protected final static boolean DEBUG = true;

  //
  // PRIVATE CONSTS
  //

  /**
   * Local storage key for the the SDK id.
   */
  private final static String SDK_ID_KEY = "IQU_SDK_ID";

  /**
   * Initial update interval value
   */
  private final static long DEFAULT_UPDATE_INTERVAL = 200;

  /**
   * Initial send timeout value
   */
  private final static long DEFAULT_SEND_TIMEOUT = 20000;

  /**
   * Initial interval in milliseconds between server available checks
   */
  private final static long DEFAULT_CHECK_SERVER_INTERVAL = 2000;

  /**
   * Interval in milliseconds between heartbeat messages
   */
  private final static long HEARTBEAT_INTERVAL = 60000;

  /**
   * Event type values.
   */
  private final static String EVENT_REVENUE = "revenue";
  private final static String EVENT_HEARTBEAT = "heartbeat";
  private final static String EVENT_ITEM_PURCHASE = "item_purchase";
  private final static String EVENT_TUTORIAL = "tutorial";
  private final static String EVENT_MILESTONE = "milestone";
  private final static String EVENT_MARKETING = "marketing";
  private final static String EVENT_USER_ATTRIBUTE = "user_attribute";
  private final static String EVENT_COUNTRY = "country";
  private final static String EVENT_PLATFORM = "platform";

  //
  // PRIVATE VARIABLES
  //

  /**
   * See property definition.
   */
  private static IQUSDK m_instance;

  /**
   * See property definition.
   */
  private boolean m_initialized;

  /**
   * See property definition.
   */
  private boolean m_logEnabled;

  /**
   * See property definition.
   */
  private String m_log;

  /**
   * See property definition.
   */
  private IQUTestMode m_testMode;

  /**
   * See property definition.
   */
  private boolean m_serverAvailable;

  /**
   * See property definition.
   */
  private long m_sendTimeout;

  /**
   * See property definition.
   */
  private long m_updateInterval;

  /**
   * See property definition.
   */
  private boolean m_analyticsEnabled;

  /**
   * See property definition.
   */
  private long m_checkServerInterval;

  /**
   * See property definition.
   */
  private boolean m_payable;

  /**
   * Contains the various ids
   */
  private IQUIds m_ids;

  /**
   * The paused state of the application
   */
  private boolean m_updateThreadPaused;

  /**
   * When true doUpdate call is busy.
   */
  private boolean m_updateThreadBusy;

  /**
   * When true update() should sleep and recheck this value.
   */
  private boolean m_updateThreadWait;

  /**
   * Thread used to call update()
   */
  private Thread m_updateThread;

  /**
   * While this value is true the thread keeps running in an infinite loop
   */
  private boolean m_updateThreadRunning;

  /**
   * Network part of IQU SDK.
   */
  private IQUNetwork m_network;

  /**
   * Local storage part of IQU SDK.
   */
  private IQULocalStorage m_localStorage;

  /**
   * Contains the application using the SDK.
   */
  private Application m_application;

  /**
   * Contains messages that are pending to be sent.
   */
  private IQUMessageQueue m_pendingMessages;

  /**
   * Contains messages currently being sent.
   */
  private IQUMessageQueue m_sendingMessages;

  /**
   * Time before a new server check is allowed.
   */
  private long m_checkServerTime;

  /**
   * Time of last heartbeat message.
   */
  private long m_heartbeatTime;

  /**
   * True if update call has never been called before.
   */
  private boolean m_firstUpdateCall;

  /**
   * Formats date and time values for use by the server.
   */
  private final SimpleDateFormat m_dateFormat;

  /**
   * Used to handle access to a properties from multiple threads.
   */
  private final Object m_propertySemaphore;

  /**
   * Used to handle access to pending messages from multiple threads.
   */
  private final Object m_pendingMessagesSemaphore;

  /**
   * Used to handle access to log from multiple threads.
   */
  private final Object m_logSemaphore;

  /**
   * Used to handle access to ids from multiple threads.
   */
  private final Object m_idsSemaphore;

  //
  // PRIVATE CONSTRUCTOR
  //

  /**
   * Creates the instance and initializes all private variables.
   */
  @SuppressLint("SimpleDateFormat")
  private IQUSDK() {
    this.m_analyticsEnabled = true;
    this.m_application = null;
    this.m_checkServerInterval = DEFAULT_CHECK_SERVER_INTERVAL;
    this.m_checkServerTime = -DEFAULT_CHECK_SERVER_INTERVAL;
    this.m_dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
    this.m_firstUpdateCall = true;
    this.m_heartbeatTime = -HEARTBEAT_INTERVAL;
    this.m_ids = new IQUIds();
    this.m_initialized = false;
    this.m_localStorage = null;
    this.m_log = "";
    this.m_logEnabled = false;
    this.m_network = null;
    this.m_payable = true;
    this.m_pendingMessages = null;
    this.m_propertySemaphore = new Object();
    this.m_pendingMessagesSemaphore = new Object();
    this.m_logSemaphore = new Object();
    this.m_idsSemaphore = new Object();
    this.m_sendingMessages = null;
    this.m_sendTimeout = DEFAULT_SEND_TIMEOUT;
    this.m_serverAvailable = true;
    this.m_testMode = IQUTestMode.NONE;
    this.m_updateInterval = DEFAULT_UPDATE_INTERVAL;
    this.m_updateThread = null;
    this.m_updateThreadBusy = false;
    this.m_updateThreadPaused = false;
    this.m_updateThreadRunning = true;
    this.m_updateThreadWait = false;
  }

  //
  // PUBLIC METHODS
  //

  /**
   * Calls {@link #start(Application, String, String, boolean)} with
   * <code>true</code> for payable parameter.
   *
   * @param anApplication
   *   Application using the SDK
   * @param anApiKey
   *   API key
   * @param aSecretKey
   *   Secret key
   */
  public synchronized void start(Application anApplication, String anApiKey, String aSecretKey) {
    this.start(anApplication, anApiKey, aSecretKey, true);
  }

  /**
   * Starts the SDK using and sets the {@link #getPayable() payable} property
   * to the specified value.
   * <p>
   * If the SDK is already started, another call to this method will be
   * ignored.
   * </p>
   *
   * @param anApplication
   *   Application using the SDK
   * @param anApiKey
   *   API key
   * @param aSecretKey
   *   Secret key
   * @param aPayable
   *   Initial payable value
   */
  public synchronized void start(Application anApplication, String anApiKey, String aSecretKey,
                                 boolean aPayable) {
    this.initialize(anApplication, anApiKey, aSecretKey, aPayable);
  }

  /**
   * Calls {@link #start(Application, String, String, String, boolean)} with
   * <code>true</code> for payable parameter.
   *
   * @param anApplication
   *   Application using the SDK
   * @param anApiKey
   *   API key
   * @param aSecretKey
   *   Secret key
   * @param aCustomId
   *   A custom id that the SDK should use.
   */
  public synchronized void start(Application anApplication, String anApiKey, String aSecretKey,
                                 String aCustomId) {
    this.start(anApplication, anApiKey, aSecretKey, aCustomId, true);
  }

  /**
   * Calls {@link #start(Application, String, String, boolean)} and then calls
   * {@link #setCustomId(String)} to store aCustomId.
   * <p>
   * If the SDK is already started, another call to this method will only
   * update the custom id.
   * </p>
   *
   * @param anApplication
   *   Application using the SDK
   * @param anApiKey
   *   API key
   * @param aSecretKey
   *   Secret key
   * @param aCustomId
   *   A custom id that the SDK should use.
   * @param aPayable
   *   Initial payable value
   */
  public synchronized void start(Application anApplication, String anApiKey, String aSecretKey,
                                 String aCustomId, boolean aPayable) {
    this.start(anApplication, anApiKey, aSecretKey, aPayable);
    this.setCustomId(aCustomId);
  }

  /**
   * Call this method from Activity's onPause; it pauses the internal update
   * thread and saves local storage to persistent storage.
   * <p>
   * It will wait for the update thread to finish if a call to update is
   * active.
   * </p>
   * While the update thread is paused no messages are sent to the server.
   */
  public void pause() {
    // pause the update thread
    this.pauseUpdateThread();
    // save pending data to persistent storage
    if (this.m_localStorage != null) {
      this.m_localStorage.save();
    }
    // save pending messages to persistent storage (in case someone stops
    // the app from outside).
    if (this.m_pendingMessages != null) {
      synchronized (this.m_pendingMessagesSemaphore) {
        this.m_pendingMessages.save();
      }
    }
  }

  /**
   * Call this method from Activity's onResume; it resumes the update thread.
   */
  public void resume() {
    this.m_updateThreadPaused = false;
  }

  /**
   * This method stops the update thread and destroys the current IQUSDK
   * instance.
   * <p>
   * Accessing {@link #instance()} after call to this method will create a new
   * IQU instance. {@link #start(Application, String, String)} needs to be
   * called again to initialize the new instance.
   * </p>
   */
  public void terminate() {
    // destroy update thread and any reference to object instances
    this.destroyUpdateThread();
    this.clearReferences();
    // clear reference to singleton
    synchronized (IQUSDK.class) {
      IQUSDK.m_instance = null;
    }
  }

  //
  // PUBLIC ID METHODS
  //

  /**
   * Return id for a certain type. If the id is not known (yet), the method
   * will return an empty string.
   *
   * @param aType
   *   Type to get id for.
   *
   * @return stored id value or empty string if it not (yet) known.
   */
  public String getId(IQUIdType aType) {
    synchronized (this.m_idsSemaphore) {
      return this.m_ids.get(aType);
    }
  }

  /**
   * Sets the Facebook id the SDK should use.
   *
   * @param anId
   *   Facebook ID.
   */
  public void setFacebookId(String anId) {
    this.setId(IQUIdType.FACEBOOK, anId);
  }

  /**
   * Removes the current used Facebook id.
   */
  public void clearFacebookId() {
    this.setId(IQUIdType.FACEBOOK, "");
  }

  /**
   * Sets the Google+ id the SDK should use.
   *
   * @param anId
   *   Google+ ID.
   */
  public void setGooglePlusId(String anId) {
    this.setId(IQUIdType.GOOGLE_PLUS, anId);
  }

  /**
   * Removes the current used Google+ id.
   */
  public void clearGooglePlusId() {
    this.setId(IQUIdType.GOOGLE_PLUS, "");
  }

  /**
   * Sets the Twitter id the SDK should use.
   *
   * @param anId
   *   Twitter ID.
   */
  public void setTwitterId(String anId) {
    this.setId(IQUIdType.TWITTER, anId);
  }

  /**
   * Removes the current used Twitter id.
   */
  public void clearTwitterId() {
    this.setId(IQUIdType.TWITTER, "");
  }

  /**
   * Sets the custom id the SDK should use.
   *
   * @param anId
   *   Custom ID.
   */
  public void setCustomId(String anId) {
    this.setId(IQUIdType.CUSTOM, anId);
  }

  /**
   * Removes the current used custom id.
   */
  public void clearCustomId() {
    this.setId(IQUIdType.CUSTOM, "");
  }

  //
  // PUBLIC ANALYTIC METHODS
  //

  /**
   * Tracks payment made by the user.
   * <p>
   * If the IQU SDK has not been initialized or {@link #getAnalyticsEnabled()}
   * returns <code>false</code>, this method will do nothing.
   * </p>
   *
   * @param anAmount
   *   Amount
   * @param aCurrency
   *   Currency code (ISO 4217 standard)
   * @param aReward
   *   Name of reward or null if there no such value
   */
  public void trackRevenue(float anAmount, String aCurrency, String aReward) {
    // exit if analytics are disabled
    if (!this.getAnalyticsEnabled()) {
      return;
    }
    try {
      JSONObject event = this.createEvent(EVENT_REVENUE, true);
      event.put("amount", anAmount);
      event.put("currency", aCurrency);
      if (aReward != null) {
        event.put("reward", aReward);
      }
      this.addEvent(event);
    }
    catch (Exception ignored) {
    }
  }

  /**
   * Tracks revenue, just calls {@link #trackRevenue(float, String, String)}
   * with null for aReward.
   * <p>
   * If the IQU SDK has not been initialized or {@link #getAnalyticsEnabled()}
   * returns <code>false</code>, this method will do nothing.
   * </p>
   *
   * @param anAmount
   *   Amount
   * @param aCurrency
   *   Currency code (ISO 4217 standard)
   */
  public void trackRevenue(float anAmount, String aCurrency) {
    this.trackRevenue(anAmount, aCurrency, null);
  }

  /**
   * Tracks payment made by the user including an amount in a virtual
   * currency.
   * <p>
   * If the IQU SDK has not been initialized or {@link #getAnalyticsEnabled()}
   * returns <code>false</code>, this method will do nothing.
   * </p>
   *
   * @param anAmount
   *   Amount
   * @param aCurrency
   *   Currency code (ISO 4217 standard)
   * @param aVirtualCurrencyAmount
   *   Amount of virtual currency rewarded with this purchase
   * @param aReward
   *   Name of reward or null if there no such value
   */
  public void trackRevenue(float anAmount, String aCurrency, float aVirtualCurrencyAmount,
                           String aReward) {
    // exit if analytics are disabled
    if (!this.getAnalyticsEnabled()) {
      return;
    }
    try {
      JSONObject event = this.createEvent(EVENT_REVENUE, true);
      event.put("amount", anAmount);
      event.put("currency", aCurrency);
      event.put("vc_amount", aVirtualCurrencyAmount);
      if (aReward != null) {
        event.put("reward", aReward);
      }
      this.addEvent(event);
    }
    catch (Exception ignored) {
    }
  }

  /**
   * Tracks revenue, just calls
   * {@link #trackRevenue(float, String, float, String)} with null for
   * aReward.
   * <p>
   * If the IQU SDK has not been initialized or {@link #getAnalyticsEnabled()}
   * returns <code>false</code>, this method will do nothing.
   * </p>
   *
   * @param anAmount
   *   Amount
   * @param aCurrency
   *   Currency code (ISO 4217 standard)
   * @param aVirtualCurrencyAmount
   *   Amount of virtual currency rewarded with this purchase
   */
  public void trackRevenue(float anAmount, String aCurrency, float aVirtualCurrencyAmount) {
    this.trackRevenue(anAmount, aCurrency, aVirtualCurrencyAmount, null);
  }

  /**
   * Tracks an item purchase.
   * <p>
   * If the IQU SDK has not been initialized or {@link #getAnalyticsEnabled()}
   * returns <code>false</code>, this method will do nothing.
   * </p>
   *
   * @param aName
   *   Name of item
   */
  public void trackItemPurchase(String aName) {
    // exit if analytics are disabled
    if (!this.getAnalyticsEnabled()) {
      return;
    }
    try {
      JSONObject event = this.createEvent(EVENT_ITEM_PURCHASE, true);
      event.put("name", aName);
      this.addEvent(event);
    }
    catch (Exception ignored) {
    }
  }

  /**
   * Tracks an item purchase including amount in virtual currency.
   * <p>
   * If the IQU SDK has not been initialized or {@link #getAnalyticsEnabled()}
   * returns <code>false</code>, this method will do nothing.
   * </p>
   *
   * @param aName
   *   Name of item
   * @param aVirtualCurrencyAmount
   *   Amount of virtual currency rewarded with this purchase
   */
  public void trackItemPurchase(String aName, float aVirtualCurrencyAmount) {
    // exit if analytics are disabled
    if (!this.getAnalyticsEnabled()) {
      return;
    }
    try {
      JSONObject event = this.createEvent(EVENT_ITEM_PURCHASE, true);
      event.put("name", aName);
      event.put("vc_amount", aVirtualCurrencyAmount);
      this.addEvent(event);
    }
    catch (Exception ignored) {
    }
  }

  /**
   * Tracks tutorial progression achieved by the user.
   * <p>
   * If the IQU SDK has not been initialized or {@link #getAnalyticsEnabled()}
   * returns <code>false</code>, this method will do nothing.
   * </p>
   *
   * @param aStep
   *   Step name or number of the tutorial.
   */
  public void trackTutorial(String aStep) {
    // exit if analytics are disabled
    if (!this.getAnalyticsEnabled()) {
      return;
    }
    try {
      JSONObject event = this.createEvent(EVENT_TUTORIAL, true);
      event.put("step", aStep);
      this.addEvent(event);
    }
    catch (Exception ignored) {
    }
  }

  /**
   * Tracks a milestone achieved by the user, e.g. if the user achieved a
   * level.
   * <p>
   * If the IQU SDK has not been initialized or {@link #getAnalyticsEnabled()}
   * returns <code>false</code>, this method will do nothing.
   * </p>
   *
   * @param aName
   *   Milestone name
   * @param aValue
   *   Value of the milestone
   */
  public void trackMilestone(String aName, String aValue) {
    // exit if analytics are disabled
    if (!this.getAnalyticsEnabled()) {
      return;
    }
    try {
      JSONObject event = this.createEvent(EVENT_MILESTONE, true);
      event.put("name", aName);
      event.put("value", aValue);
      this.addEvent(event);
    }
    catch (Exception ignored) {
    }
  }

  /**
   * Tracks a marketing source. All parameters are optional, if a value is not
   * known null must be used.
   * <p>
   * If the IQU SDK has not been initialized or {@link #getAnalyticsEnabled()}
   * returns <code>false</code>, this method will do nothing.
   * </p>
   *
   * @param aPartner
   *   Marketing partner name or null if there is none.
   * @param aCampaign
   *   Marketing campaign name or null if there is none.
   * @param anAd
   *   Marketing ad name or null if there is none.
   * @param aSubId
   *   Marketing partner sub id or null if there is none.
   * @param aSubSubId
   *   Marketing partner sub sub id or null if there is none.
   */
  public void trackMarketing(String aPartner, String aCampaign, String anAd, String aSubId,
                             String aSubSubId) {
    // exit if analytics are disabled
    if (!this.getAnalyticsEnabled()) {
      return;
    }
    try {
      JSONObject event = this.createEvent(EVENT_MARKETING, false);
      this.putField(event, "partner", aPartner);
      this.putField(event, "campaign", aPartner);
      this.putField(event, "ad", aPartner);
      this.putField(event, "subid", aPartner);
      this.putField(event, "subsubid", aPartner);
      this.addEvent(event);
    }
    catch (Exception ignored) {
    }
  }

  /**
   * Tracks an user attribute, e.g. gender or birthday.
   * <p>
   * If the IQU SDK has not been initialized or {@link #getAnalyticsEnabled()}
   * returns <code>false</code>, this method will do nothing.
   * </p>
   *
   * @param aName
   *   Name of the user attribute, e.g. gender
   * @param aValue
   *   Value of the user attribute, e.g. female
   */
  public void trackUserAttribute(String aName, String aValue) {
    // exit if analytics are disabled
    if (!this.getAnalyticsEnabled()) {
      return;
    }
    try {
      JSONObject event = this.createEvent(EVENT_USER_ATTRIBUTE, false);
      event.put("name", aName);
      event.put("value", aValue);
      this.addEvent(event);
    }
    catch (Exception ignored) {
    }
  }

  /**
   * Tracks the country of the user, only required for S2S implementations.
   * <p>
   * If the IQU SDK has not been initialized or {@link #getAnalyticsEnabled()}
   * returns <code>false</code>, this method will do nothing.
   * </p>
   *
   * @param aCountry
   *   Country as specified in ISO3166-1 alpha-2, e.g. US, NL, DE
   */
  public void trackCountry(String aCountry) {
    // exit if analytics are disabled
    if (!this.getAnalyticsEnabled()) {
      return;
    }
    try {
      JSONObject event = this.createEvent(EVENT_COUNTRY, false);
      event.put("value", aCountry);
      this.addEvent(event);
    }
    catch (Exception ignored) {
    }
  }

  //
  // PUBLIC PROPERTIES
  //

  /**
   * Gets the singleton IQU instance. If no instance exists a new instance
   * will be created.
   *
   * @return singleton IQU instance.
   */
  public static IQUSDK instance() {
    if (IQUSDK.m_instance == null) {
      synchronized (IQUSDK.class) {
        if (IQUSDK.m_instance == null) {
          IQUSDK.m_instance = new IQUSDK();
        }
      }
    }
    return IQUSDK.m_instance;
  }

  /**
   * Just calls the {@link #instance()} method and returns its result.
   *
   * @return result from instance() call.
   */
  public static IQUSDK getInstance() {
    return IQUSDK.instance();
  }

  /**
   * Returns the analytics enabled state. When not enabled, all calls to the
   * tracking methods are ignored and no messages are sent to the server.
   * <p>
   * On Android devices the enabled state depends on the limit ad tracking. If
   * the user turned on limit ad tracking, the analyticsEnabled property will
   * return <code>false</code>.
   * </p>
   *
   * @return analytics enabled state
   */
  public boolean getAnalyticsEnabled() {
    synchronized (this.m_propertySemaphore) {
      return this.m_analyticsEnabled;
    }
  }

  /**
   * Returns initialized state. After a call to
   * {@link #start(Application, String, String)} this property will return
   * <code>true</code>.
   *
   * @return <code>false</code> until a <code>start()</code> method has been
   * called.
   */
  public boolean getInitialized() {
    synchronized (this.m_propertySemaphore) {
      return this.m_initialized;
    }
  }

  /**
   * Returns if a payable event is active or not.
   * <p>
   * The default value is <code>true</code>.
   * </p>
   *
   * @return payable active
   */
  public boolean getPayable() {
    synchronized (this.m_propertySemaphore) {
      return this.m_payable;
    }
  }

  /**
   * Turns the enable event on or off.
   *
   * @param aValue
   *   New payable value.
   */
  public void setPayable(boolean aValue) {
    synchronized (this.m_propertySemaphore) {
      this.m_payable = aValue;
    }
  }

  /**
   * Returns the time in milliseconds to wait between update calls.
   * <p>
   * Default value is 200.
   * </p>
   *
   * @return current updateInterval property value
   */
  public long getUpdateInterval() {
    synchronized (this.m_propertySemaphore) {
      return this.m_updateInterval;
    }
  }

  /**
   * Sets the updateInterval property. This new value will be used with the
   * next wait call.
   * <p>
   * This value determines the maximum delay between creating messages and
   * sending them.
   * </p>
   *
   * @param aValue
   *   New value to use (minimum allowed value is 10, maximum allowed
   *   value is 60000).
   */
  public void setUpdateInterval(long aValue) {
    synchronized (this.m_propertySemaphore) {
      this.m_updateInterval = Math.min(60000, Math.max(10, aValue));
    }
  }

  /**
   * This property determines the maximum time in milliseconds sending a
   * message to the IQU server is allowed to take.
   * <p>
   * If there is no response by the IQU server within this time, the SDK
   * assumes the server is not reachable and will set the
   * {@link #getServerAvailable() serverAvailable} property to
   * <code>false</code> .
   * </p>
   * <p>
   * Default value is 20000 (20 seconds).
   * </p>
   *
   * @return current sendTimeout property value
   */
  public long getSendTimeout() {
    synchronized (this.m_propertySemaphore) {
      return this.m_sendTimeout;
    }
  }

  /**
   * Changes the sendTimout property value. The minimum value allowed is 100.
   *
   * @param aValue
   *   New value to use.
   */
  public void setSendTimeout(long aValue) {
    synchronized (this.m_propertySemaphore) {
      this.m_sendTimeout = Math.max(100, aValue);
    }
  }

  /**
   * This property determines the time between server availability checks in
   * milliseconds.
   * <p>
   * This property is used once the sending of a message fails. The
   * checkServerInterval property determines the time the SDK waits before
   * checking the availability of the server and trying to resend the
   * messages.
   * </p>
   * <p>
   * The default value is 2000 (2 seconds).
   * </p>
   *
   * @return interval time for server check
   */
  public long getCheckServerInterval() {
    synchronized (this.m_propertySemaphore) {
      return this.m_checkServerInterval;
    }
  }

  /**
   * Changes the checkServerInterval property. The minimum value allowed is
   * 100.
   *
   * @param aValue
   *   New value to use.
   */
  public void setCheckServerInterval(long aValue) {
    synchronized (this.m_propertySemaphore) {
      this.m_checkServerInterval = Math.max(100, aValue);
    }
  }

  /**
   * Gets the current log enabled value.
   *
   * @return current log enabled value.
   */
  public boolean getLogEnabled() {
    synchronized (this.m_logSemaphore) {
      return this.m_logEnabled;
    }
  }

  /**
   * Turns the log on or off. When turned on, various IQU SDK methods will add
   * information to the {@link #getLog() log} property.
   * <p>
   * The current log will be cleared when turning off the logging.
   * </p>
   *
   * @param aValue
   *   Use <code>true</code> to logging on, <code>false</code> to
   *   turn logging off.
   */
  public void setLogEnabled(boolean aValue) {
    synchronized (this.m_logSemaphore) {
      if (DEBUG) {
        this.m_logEnabled = aValue;
        if (!aValue) {
          this.m_log = "";
        }
      }
    }
  }

  /**
   * Gets the current log.
   *
   * @return full log.
   */
  public String getLog() {
    synchronized (this.m_logSemaphore) {
      return this.m_log;
    }
  }

  /**
   * Returns the sever availability state. The state is updated when messages
   * are sent to the server.
   * <p>
   * While the server is not available, messages will be queued to be sent
   * once the server is available again.
   * </p>
   * <p>
   * If the server is not available and there are pending messages, the class
   * will check the server availability at regular intervals. Once the server
   * becomes available again, the messages are sent to the server.
   * </p>
   *
   * @return <code>true</code> when the last message was sent successful or
   * <code>false</code> if the message could not be sent.
   */
  public boolean getServerAvailable() {
    synchronized (this.m_propertySemaphore) {
      return this.m_serverAvailable;
    }
  }

  /**
   * Gets the current test mode property value.
   * <p>
   * The default value is {@link IQUTestMode#NONE}
   * </p>
   *
   * @return current test mode.
   */
  public IQUTestMode getTestMode() {
    synchronized (this.m_propertySemaphore) {
      return this.m_testMode;
    }
  }

  /**
   * Sets the test mode property. The test mode property can be used during
   * development.
   * <p>
   * Use {@link IQUTestMode#SIMULATE_SERVER} to prevent any network traffic.
   * </p>
   * <p>
   * Use {@link IQUTestMode#SIMULATE_OFFLINE} to test the SDK behaviour while
   * the server is not available.
   * </p>
   * <p>
   * To go back to normal operation use {@link IQUTestMode#NONE}.
   * </p>
   *
   * @param aValue
   *   New test mode to use.
   */
  public void setTestMode(IQUTestMode aValue) {
    synchronized (this.m_propertySemaphore) {
      if (DEBUG) {
        this.m_testMode = aValue;
      }
    }
  }

  //
  // PROTECTED PROPERTIES
  //

  /**
   * Returns the network instance. This property is available after init() has
   * been called.
   *
   * @return network instance
   */
  protected IQUNetwork network() {
    return this.m_network;
  }

  /**
   * Returns the local storage instance. This property is available after
   * init() has been called.
   *
   * @return local storage instance
   */
  protected IQULocalStorage localStorage() {
    return this.m_localStorage;
  }

  /**
   * Returns the application using the SDK. This property is available after
   * init() has been called.
   *
   * @return Application instance.
   */
  protected Application application() {
    return this.m_application;
  }

  //
  // PROTECTED METHODS
  //

  /**
   * Add message to log.
   *
   * @param aMessage
   *   Message to add to the log.
   */
  protected void addLog(String aMessage) {
    synchronized (this.m_logSemaphore) {
      if (DEBUG) {
        if (this.m_logEnabled) {
          this.m_log = this.m_log + aMessage + "\n";
        }
      }
    }
  }

  //
  // PRIVATE INITIALIZERS METHODS
  //

  /**
   * Initializes the instance. This method takes care of all initialization
   * except obtaining the id.
   *
   * @param anApplication
   *   Application that is using the SDK
   * @param anApiKey
   *   API key
   * @param aSecretKey
   *   API secret
   * @param aPayable
   *   Initial payable value.
   */
  private void initialize(Application anApplication, String anApiKey, String aSecretKey,
                          boolean aPayable) {
    // exit if already initialized
    if (this.getInitialized()) {
      if (DEBUG) {
        this.addLog("[Init] WARNING: already initialized");
      }
      return;
    }
    // store reference to application
    this.m_application = anApplication;
    // create local storage
    this.m_localStorage = new IQULocalStorage(anApplication.getSharedPreferences("IQU_SDK", 0));
    // create network
    this.m_network = new IQUNetwork(anApiKey, aSecretKey);
    // create message queues
    this.m_pendingMessages = new IQUMessageQueue();
    this.m_sendingMessages = new IQUMessageQueue();
    // update properties
    this.setPayable(aPayable);
    // retrieve or create an unique ID
    this.obtainSdkId();
    // start update thread
    this.startUpdateThread();
    // update properties
    this.setInitialized(true);
    // debug
    if (DEBUG) {
      this.addLog("[Init] IQU SDK is initialized");
    }
  }

  /**
   * Initializes the SDK further from within the update thread. This method is
   * called the first time update is called from within the update thread.
   */
  private void initializeFromUpdateThread() {
    // try to get advertising id and limit ad tracking
    this.obtainAdvertisingId();
    // if analytics are not allowed, clear pending messages to remove any
    // tracking messages added after initialize and before this method is
    // called.
    if (!this.getAnalyticsEnabled()) {
      synchronized (this.m_pendingMessagesSemaphore) {
        this.m_pendingMessages.clear(false);
      }
    }
    // load stored messages and prepend them to pending messages.
    this.loadMessages();
    // add platform message (if none exists) and analytics is allowed
    if (!this.messagesHasEventType(EVENT_PLATFORM) && this.getAnalyticsEnabled()) {
      this.trackPlatform();
    }
  }

  /**
   * Clears references to used instances.
   */
  private void clearReferences() {
    this.m_application = null;
    if (this.m_localStorage != null) {
      this.m_localStorage.destroy();
      this.m_localStorage = null;
    }
    if (this.m_network != null) {
      this.m_network.destroy();
      this.m_network = null;
    }
    if (this.m_pendingMessages != null) {
      this.m_pendingMessages.destroy();
      this.m_pendingMessages = null;
    }
    if (this.m_sendingMessages != null) {
      this.m_sendingMessages.destroy();
      this.m_sendingMessages = null;
    }
    if (this.m_ids != null) {
      this.m_ids.destroy();
      this.m_ids = null;
    }
  }

  //
  // PRIVATE PROPERTY SETTERS
  //

  /**
   * Sets enabled property value.
   *
   * @param aValue
   *   New value to use
   */
  private void setInitialized(boolean aValue) {
    synchronized (this.m_propertySemaphore) {
      this.m_initialized = aValue;
    }
  }

  /**
   * Sets analyticsEnabled property value.
   *
   * @param aValue
   *   New value to use
   */
  private void setAnalyticsEnabled(boolean aValue) {
    synchronized (this.m_propertySemaphore) {
      this.m_analyticsEnabled = aValue;
    }
  }

  /**
   * Sets serverAvailable property value.
   *
   * @param aValue
   *   New value to use
   */
  private void setServerAvailable(boolean aValue) {
    synchronized (this.m_propertySemaphore) {
      this.m_serverAvailable = aValue;
    }
  }

  //
  // PRIVATE ID METHODS
  //

  /**
   * Store a new id or update existing id with new value.
   *
   * @param aType
   *   Type to store id for.
   * @param aValue
   *   Value to store.
   */
  private void setId(IQUIdType aType, String aValue) {
    synchronized (this.m_idsSemaphore) {
      this.m_ids.set(aType, aValue);
    }
    if (this.getInitialized()) {
      synchronized (this.m_pendingMessagesSemaphore) {
        this.m_pendingMessages.updateId(aType, aValue);
      }
    }
  }

  /**
   * Initializes the id managed by the SDK. Try to retrieve it from local
   * storage, if it fails create a new id.
   */
  private void obtainSdkId() {
    // get id from local storage
    String id = this.m_localStorage.getString(SDK_ID_KEY, "");
    // create new id and store it when none was found
    if (id.length() == 0) {
      id = UUID.randomUUID().toString();
      this.m_localStorage.setString(SDK_ID_KEY, id);
    }
    // set SDK id
    this.setId(IQUIdType.SDK, id);
  }

  /**
   * Try to obtain the advertising id from Google Play. This method must not
   * be called from the main UI thread.
   */
  private void obtainAdvertisingId() {
    // based on sample code at
    // http://developer.android.com/google/play-services/id.html
    try {
      // use reflection to call classes (in case SDK user did not include
      // Google Play jar files).
      @SuppressWarnings("rawtypes")
      Class adIdClientClass = Class
        .forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
      @SuppressWarnings("unchecked")
      Method getAdvertisingIdInfoMethod = adIdClientClass.getDeclaredMethod(
        "getAdvertisingIdInfo", Context.class);
      Object adInfo = getAdvertisingIdInfoMethod.invoke(null, this.m_application);
      @SuppressWarnings("rawtypes")
      Class adInfoClass = adInfo.getClass();
      @SuppressWarnings("unchecked")
      Method getIdMethod = adInfoClass.getDeclaredMethod("getId");
      @SuppressWarnings("unchecked")
      Method isLimitAdTrackingEnabledMethod = adInfoClass
        .getDeclaredMethod("isLimitAdTrackingEnabled");
      this.setId(IQUIdType.ANDROID_ADVERTISING, getIdMethod.invoke(adInfo).toString());
      this.setAnalyticsEnabled(!(Boolean) isLimitAdTrackingEnabledMethod.invoke(adInfo));
      this.setId(IQUIdType.ANDROID_AD_TRACKING, this.getAnalyticsEnabled() ? "1" : "0");
    }
    catch (Exception e) {
      if (IQUSDK.DEBUG) {
        this.addLog("[Device] Exception occured: " + e.getClass().getName() + ": "
          + e.getMessage());
      }
    }
  }

  //
  // PRIVATE THREAD METHODS
  //

  /**
   * Starts the update thread.
   */
  private void startUpdateThread() {
    this.m_updateThreadRunning = true;
    this.m_updateThread = new Thread() {
      @Override
      public void run() {
        if (IQUSDK.DEBUG) {
          IQUSDK.this.addLog("[Thread] update thread started");
        }
        while (IQUSDK.this.m_updateThreadRunning) {
          try {
            // call update
            IQUSDK.this.update();
            // update might have changed the running variable
            if (IQUSDK.this.m_updateThreadRunning) {
              synchronized (this) {
                this.wait(IQUSDK.this.getUpdateInterval());
              }
            }
          }
          catch (Exception ignored) {
          }
        }
        if (IQUSDK.DEBUG) {
          IQUSDK.this.addLog("[Thread] update thread stopped");
        }
      }
    };
    this.m_updateThread.start();
  }

  /**
   * Pauses the update thread, set thread paused to true and wait for the
   * update thread to finish.
   */
  private void pauseUpdateThread() {
    // prevent update from doing anything (when update call starts while
    // processing this code)
    this.m_updateThreadWait = true;
    try {
      // application is paused now (any call to update will return
      // immediately)
      this.m_updateThreadPaused = true;
      // cancel any IO being executed
      if (this.m_network != null) {
        this.m_network.cancelSend();
      }
    }
    finally {
      // unblock update, if it was waiting it will exit immediately
      // because of m_threadPaused
      this.m_updateThreadWait = false;
    }
    // wait for update thread to finish current update call
    this.waitForUpdateThread();
  }

  /**
   * Destroys the update thread (if any).
   */
  @SuppressWarnings("SynchronizeOnNonFinalField")
  private void destroyUpdateThread() {
    // stop update thread (if one is active)
    if (this.m_updateThread != null) {
      if (!this.m_updateThreadPaused) {
        this.pauseUpdateThread();
      }
      // stop running the thread
      this.m_updateThreadRunning = false;
      // notify update thread (in case it is waiting)
      synchronized (this.m_updateThread) {
        this.m_updateThread.notify();
      }
      // wait for update thread to finish
      try {
        this.m_updateThread.join();
      }
      catch (Exception ignored) {
      }
      // clear reference to update thread
      this.m_updateThread = null;
    }
  }

  /**
   * Waits for the update thread to finish to current update call.
   */
  private void waitForUpdateThread() {
    while (this.m_updateThreadBusy) {
      try {
        Thread.sleep(10);
      }
      catch (Exception ignored) {
      }
    }
  }

  /**
   * Updates IQU SDK, this method is called from a separate thread context.
   */
  private void update() {
    // need to wait?
    while (this.m_updateThreadWait) {
      try {
        Thread.sleep(10);
      }
      catch (Exception ignored) {
      }
    }
    // exit if paused
    if (this.m_updateThreadPaused) {
      return;
    }
    // busy now
    this.m_updateThreadBusy = true;
    // make sure m_updateThreadBusy gets reset to false
    try {
      // first time?
      if (this.m_firstUpdateCall) {
        // yes, finish initialization of the SDK
        this.initializeFromUpdateThread();
        // make sure this block is only called once
        this.m_firstUpdateCall = false;
      }
      // process pending messages
      this.processPendingMessages();
    }
    finally {
      // update is no longer busy
      this.m_updateThreadBusy = false;
    }
  }

  //
  // PRIVATE MESSAGE METHODS
  //

  /**
   * Loads previously saved messages and prepend them to pending messages.
   */
  private void loadMessages() {
    IQUMessageQueue storedMessages = new IQUMessageQueue();
    storedMessages.load();
    synchronized (this.m_pendingMessagesSemaphore) {
      this.m_pendingMessages.prepend(storedMessages, true);
    }
    storedMessages.destroy();
  }

  /**
   * Processes the pending messages (if any) and try to send them to the
   * server.
   */
  private void processPendingMessages() {
    // wait till other threads are finished accessing pending message queue.
    synchronized (this.m_pendingMessagesSemaphore) {
      // move messages from pending messages to sending messages; this
      // will clear the pending message queue. The sending messages queue
      // is always empty before this call.
      // The queue property in every message is not updated, since
      // messages will not change while they are in the sending queue.
      this.m_sendingMessages.prepend(this.m_pendingMessages, false);
    }
    // check if a new heartbeat message needs to be created
    this.trackHeartbeat(this.m_sendingMessages);
    // any message that needs to be sent?
    if (!this.m_sendingMessages.isEmpty()) {
      // server is available?
      if (this.checkServer()) {
        // try to send the messages
        this.sendMessages(this.m_sendingMessages);
      }
      else {
        // server not reachable, call save because new messages might
        // have been added since the previous call to this method.
        this.m_sendingMessages.save();
      }
    }
    // wait till other threads are finished accessing pending message queue.
    synchronized (this.m_pendingMessagesSemaphore) {
      // move any failed messages to the front of the pending messages
      // (this will also clear sending messages queue)
      // The queue property of every message in the sending queue is still
      // pointing to the pending message queue so no need to update it.
      this.m_pendingMessages.prepend(this.m_sendingMessages, false);
    }
  }

  /**
   * Tries to send the messages to the server. When successful the messages
   * get destroyed, else the messages get saved. This method will also update
   * the serverAvailable property.
   *
   * @param aMessages
   *   Messages to send to the server.
   */
  private void sendMessages(IQUMessageQueue aMessages) {
    // try to send messages to the server
    if (this.m_network.send(aMessages)) {
      // messages were sent successfully, so destroy them (including
      // persistent storage)
      aMessages.clear(true);
      // update property
      this.setServerAvailable(true);
    }
    else {
      if (DEBUG) {
        this.addLog("[Network] server is not available");
      }
      // messages were not sent, save them to persistent storage
      aMessages.save();
      // update property
      this.setServerAvailable(false);
    }
  }

  /**
   * Adds a message to the pending message list. The method is thread safe
   * blocking any access to the pending message queue while it's busy adding
   * the message.
   *
   * @param aMessage
   *   Message to add.
   */
  private void addMessage(IQUMessage aMessage) {
    // only add if IQU SDK has been initialized.
    if (this.getInitialized()) {
      synchronized (this.m_pendingMessagesSemaphore) {
        this.m_pendingMessages.add(aMessage);
      }
    }
    else {
      // message was not added, destroy the instance
      aMessage.destroy();
    }
  }

  //
  // PRIVATE EVENT METHODS
  //

  /**
   * Creates a message from an event and it to the pending queue.
   *
   * @param anEvent
   *   Event to create message for.
   */
  private void addEvent(JSONObject anEvent) {
    IQUMessage message;
    synchronized (this.m_idsSemaphore) {
      message = new IQUMessage(this.m_ids, anEvent);
    }
    this.addMessage(message);
  }

  /**
   * Checks if pending messages contain at least one message of a certain
   * type.
   *
   * @param aType
   *   Type to check.
   *
   * @return <code>true</code> if at least one message exists,
   * <code>false</code> if not.
   */
  private boolean messagesHasEventType(String aType) {
    // check messages
    boolean result;
    // prevent other threads from accessing pending messages
    synchronized (this.m_pendingMessagesSemaphore) {
      result = this.m_pendingMessages.hasEventType(aType);
    }
    return result;
  }

  /**
   * Creates an event with a certain type and adds optionally a timestamp for
   * the current date and time.
   *
   * @param anEventType
   *   Type to use
   * @param anAddTimestamp
   *   When <code>true</code> add "timestamp" field.
   *
   * @return JSONObject instance containing event
   */
  private JSONObject createEvent(String anEventType, boolean anAddTimestamp) {
    JSONObject result = new JSONObject();
    try {
      result.put("type", anEventType);
      if (anAddTimestamp) {
        result.put("timestamp", this.m_dateFormat.format(new Date()));
      }
    }
    catch (Exception ignored) {
    }
    return result;
  }

  //
  // PRIVATE TRACKING METHODS
  //

  /**
   * Checks if enough time has passed since last heartbeat message. If it has
   * the method adds a new heartbeat message.
   *
   * @param aMessages
   *   Message queue to add the heartbeat message to.
   */
  private void trackHeartbeat(IQUMessageQueue aMessages) {
    long currentTime = System.currentTimeMillis();
    if (currentTime > this.m_heartbeatTime + HEARTBEAT_INTERVAL) {
      JSONObject event = this.createEvent(EVENT_HEARTBEAT, true);
      try {
        event.put("is_payable", this.m_payable);
      }
      catch (Exception ignored) {
      }
      synchronized (this.m_idsSemaphore) {
        aMessages.add(new IQUMessage(this.m_ids, event));
      }
      this.m_heartbeatTime = currentTime;
    }
  }

  /**
   * Tracks the platform of the user.
   */
  private void trackPlatform() {
    try {
      JSONObject event = this.createEvent(EVENT_PLATFORM, false);
      this.putField(event, "manufacturer", Build.MANUFACTURER);
      this.putField(event, "device_brand", Build.BRAND);
      this.putField(event, "device_model", Build.MODEL);
      TelephonyManager manager = (TelephonyManager) this.m_application
        .getSystemService(Context.TELEPHONY_SERVICE);
      if (manager != null) {
        this.putField(event, "device_carrier", manager.getNetworkOperatorName());
      }
      this.putField(event, "os_name", "android");
      this.putField(event, "os_version", Build.VERSION.RELEASE);
      int layout = this.m_application.getResources().getConfiguration().screenLayout;
      switch (layout & Configuration.SCREENLAYOUT_SIZE_MASK) {
        case Configuration.SCREENLAYOUT_SIZE_LARGE:
          event.put("screen_size", "large");
          break;
        case Configuration.SCREENLAYOUT_SIZE_NORMAL:
          event.put("screen_size", "normal");
          break;
        case Configuration.SCREENLAYOUT_SIZE_SMALL:
          event.put("screen_size", "small");
          break;
        case Configuration.SCREENLAYOUT_SIZE_XLARGE:
          event.put("screen_size", "xlarge");
          break;
      }
      WindowManager wm = (WindowManager) this.m_application
        .getSystemService(Context.WINDOW_SERVICE);
      if (wm != null) {
        Display display = wm.getDefaultDisplay();
        if (display != null) {
          DisplayMetrics metrics = new DisplayMetrics();
          display.getMetrics(metrics);
          event.put("screen_size_width", metrics.widthPixels);
          event.put("screen_size_height", metrics.heightPixels);
        }
      }
      event.put("screen_size_dpi",
        this.m_application.getResources().getDisplayMetrics().density * 160f);
      this.addEvent(event);
    }
    catch (Exception ignored) {
    }
  }

  //
  // PRIVATE SUPPORT METHODS
  //

  /**
   * Puts a field and value into a JSONObject if the value is not null and not
   * an empty string.
   *
   * @param anObject
   *   JSON object to put value in
   * @param aName
   *   Name of value
   * @param aValue
   *   Value to put in
   *
   * @throws JSONException
   *   if an error occurs storing the value
   */
  private void putField(JSONObject anObject, String aName, String aValue) throws JSONException {
    if (aValue == null) {
      return;
    }
    if (aValue.length() == 0) {
      return;
    }
    anObject.put(aName, aValue);
  }

  /**
   * Checks if the server is available. If the server was not available the
   * code will only check if enough time has passed since the last check.
   *
   * @return <code>true</code> if the server is available, <code>false</code>
   * if not.
   */
  @SuppressWarnings("ConstantConditions")
  private boolean checkServer() {
    // server is not available since last check action?
    if (!this.getServerAvailable()) {
      // get current time
      long currentTime = System.currentTimeMillis();
      // not enough time has passed since last check?
      if (currentTime < this.m_checkServerTime) {
        // not enough time has passed, don't check, just assume server
        // is
        // still not available
        return false;
      }
      // store new time
      this.m_checkServerTime = currentTime + this.getCheckServerInterval();
      // check if the server is reachable and return result
      boolean result = this.m_network.checkServer();
      // debug
      if (DEBUG && result) {
        this.addLog("[Network] server is available");
      }
      // return check server result
      return result;

    }
    else {
      // don't perform any checks, if server became unavailable this will
      // be detected when sending the current pending messages.
      return true;
    }
  }
}
