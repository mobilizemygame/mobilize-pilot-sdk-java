package com.iqu.sdk;

import android.annotation.SuppressLint;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;

/**
 * IQUNetwork takes care of sending data to the IQU server.
 */
@SuppressWarnings("unused")
class IQUNetwork {
    //
    // PROTECTED CONSTS
    //

    /**
     * Name in JSONObject of field containing integer response code.
     */
    protected static final String CODE = "RESPONSE_CODE";

    /**
     * Name in JSONObject of field containing error description.
     */
    protected static final String ERROR = "RESPONSE_ERROR";

    //
    // PRIVATE CONSTS
    //

    /**
     * URL to service (must end with /)
     */
    private static final String URL = "https://tracker.iqugroup.com/v3/";

    /**
     * Used to convert data to hexadecimal format
     */
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    //
    // PRIVATE TYPES
    //

    /**
     * The class used by doSend to communicate between threads.
     */
    private class SendInformation {
        /**
         * Connection to send data
         */
        public HttpsURLConnection connection;

        /**
         * Indicates if sending finished (successful or not)
         */
        public boolean finished;

        /**
         * Contains error that was generated if sending failed.
         */
        public Exception error;
    }

    //
    // PRIVATE VARS
    //

    /**
     * The service url to use
     */
    private String m_serviceUrl;

    /**
     * The API key
     */
    private String m_apiKey;

    /**
     * The secret key (used to generate HMAC hash with)
     */
    private String m_secretKey;

    /**
     * When true stop sending.
     */
    private boolean m_cancel;

    //
    // PROTECTED METHODS
    //

    /**
     * Initializes a new instance of the class.
     * 
     * @param anApiKey
     *            API key
     * @param aSecretKey
     *            Secret key
     */
    protected IQUNetwork(String anApiKey, String aSecretKey) {
        // for now just copy, defined just in case the future supports multiple
        // urls
        this.m_serviceUrl = URL;
        // copy key and secret remove spaces, carriage returns and line feeds
        this.m_apiKey = anApiKey.replace("\n", "").replace("\r", "").replace(" ", "");
        this.m_secretKey = aSecretKey.replace("\n", "").replace("\r", "").replace(" ", "");
        // don't cancel sending
        this.m_cancel = false;
    }

    /**
     * Cleans up references and resources. 
     */
    protected void destroy() {
    }

    /**
     * Tries to send one or more messages to server.
     * 
     * @param aMessages
     *            MessageQueue to send
     * 
     * @return <code>true</code> if sending was successful, <code>false</code>
     *         if not.
     */
    @SuppressLint("DefaultLocale")
    protected boolean send(IQUMessageQueue aMessages) {
        JSONObject result = this.sendSigned(this.m_serviceUrl, aMessages.toJSONString());
        return !result.has(ERROR)
          && result.optString("status", "failed").toLowerCase().equals("ok");
    }

    /**
     * Tries to send a small message to the server to see if it is reachable.
     * 
     * @return <code>true</code> when message could be sent, <code>false</code>
     *         if not.
     */
    protected boolean checkServer() {
        JSONObject result = this.send(this.m_serviceUrl + "?ping", null);
        return !result.has(ERROR);
    }

    /**
     * Cancels current IO (if any). The cancellation will take max 10
     * milliseconds.
     */
    protected void cancelSend() {
        this.m_cancel = true;
    }
    
    //
    // PROTECTED PROPERTIES
    //

    /**
     * Returns cancelled state. This property is true when the last call to
     * sendQueue or one of the send methods was cancelled by cancelSend. A new
     * call to sendQueue or one of the send methods will reset this property to
     * false.
     * <p>
     * Default value is false.
     * </p>
     * 
     * @return current cancelled property value.
     */
    protected boolean isCancelled() {
        return this.m_cancel;
    }

    //
    // PRIVATE METHODS
    //

    /**
     * Convert byte array to hex string.
     * <p>
     * Source:
     * http://stackoverflow.com/questions/9655181/convert-from-byte-array
     * -to-hex-string-in-java
     * </p>
     * 
     * @param aBytes
     *            Byte array to convert
     * 
     * @return Hexadecimal representation of data
     */
    private String bytesToHex(byte[] aBytes) {
        char[] hexChars = new char[aBytes.length * 2];
        for (int j = 0; j < aBytes.length; j++) {
            int v = aBytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Convert ASCII text to byte array.
     * 
     * @param aText
     *            Text to convert.
     * 
     * @return byte array
     */
    private byte[] getBytes(String aText) {
        try {
            return aText.getBytes("US-ASCII");
        }
        catch(Exception error) {
            return null;
        }
    }

    /**
     * Convert the contents of an input stream to a string, the method will also
     * close the input stream once all data is obtained.
     * 
     * @param anInputStream
     *            Input stream to get data from
     * 
     * @return input stream as text or null if an IOException occurred.
     */
    @SuppressWarnings("ReturnInsideFinallyBlock")
    private String streamToString(InputStream anInputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(anInputStream));
        StringBuilder builder = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (IOException e) {
            return null;
        } finally {
            try {
                anInputStream.close();
            } catch (IOException e) {
                return null;
            }
        }
        return builder.toString();
    }

    /**
     * Generate a HMAC512 hash.
     * 
     * @param aSecret
     *            API secret
     * @param aPostContent
     *            the content of the message.
     * 
     * @return The HMAC512 hash as hexadecimal String.
     */
    private String generateHMACSHA512(String aSecret, String aPostContent) {
        try {
            // create HMAC hash and return its hexadecimal representation
            Mac hmac;
            hmac = Mac.getInstance("HmacSHA512");
            byte[] secret = this.getBytes(aSecret);
            if (secret == null) {
              return "";
            }
            SecretKeySpec secretKey = new SecretKeySpec(secret, "HmacSHA512");
            hmac.init(secretKey);
            return this.bytesToHex(hmac.doFinal(this.getBytes(aPostContent)));
        } catch (Exception error) {
            if (IQUSDK.DEBUG) {
                IQUSDK.instance().addLog(
                  "[Network] error generating mac: "
                    + error.getClass().getName()
                    + ": " + error.getMessage()
                );
            }
        }
        return "";
    }

    /**
     * Sleep for 1 second, unless IO got cancelled.
     */
    @SuppressWarnings("EmptyCatchBlock")
    private void sleepThread() {
        try {
            // sleep 1000ms (unless cancel is activated)
            for (int count = 0; count < 100; count++) {
                if (this.m_cancel)
                    break;
                Thread.sleep(10);
            }
        } catch (Exception error) {
        }
    }

    /**
     * Simulate offline behavior. The method waits for 1 second and then returns
     * a JSONObject with only an error field.
     * 
     * @param anUrl
     *            URL to send to
     * @param aPostContent
     *            POST data to send
     * 
     * @return JSONObject with only an error field.
     */
    private JSONObject simulateOffline(String anUrl, String aPostContent) {
        if (IQUSDK.DEBUG) {
            IQUSDK.instance().addLog("[Network] simulating offline state (server not available)");
        }
        try {
            // wait 1 second
            this.sleepThread();
            // return object with only error message
            JSONObject result = new JSONObject();
            result.put(ERROR,
                    "simulating offline (IQU.instance().getTestMode() == IQUTestMode.SIMULATE_OFFLINE)");
            return result;
        } catch (Exception error) {
            return new JSONObject();
        }
    }

    /**
     * Simulate a server IO. The IO is always successful.
     * 
     * @param anUrl
     *            URL to post to
     * @param aPostContent
     *            Content to post
     * @return JSONObject instance with simulated result data.
     */
    private JSONObject simulateServer(String anUrl, String aPostContent) {
        if (IQUSDK.DEBUG) {
            IQUSDK.instance().addLog("[Network] simulating successful server response");
        }
        try {
            // wait 1 second
            this.sleepThread();
            // return default result
            JSONObject result = new JSONObject("{" + "\"request_id\":\"2a7-558bf465ed65-b79a84\","
                    + "\"time\":\"2015-06-26 12:00:00 UTC\"," + "\"status\":\"ok\"}");
            result.put(CODE, 200);
            return result;
        } catch (Exception error) {
            return new JSONObject();
        }

    }

    /**
     * Creates a HttpsURLConnection instance from url.
     * 
     * @param anUrl
     *            URL to use
     * 
     * @return HttpsURLConnection instance
     */
    @SuppressLint("TrulyRandom")
	private HttpsURLConnection createConnection(String anUrl) throws Exception {
        // get URL and connection
        URL url = new URL(anUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
     // Create the SSL connection
        SSLContext sc;
        sc = SSLContext.getInstance("TLS");
        sc.init(null, null, new java.security.SecureRandom());
        conn.setSSLSocketFactory(sc.getSocketFactory());
        return conn;
    }

    /**
     * Initializes a connection to send data to the server.
     * 
     * @param aConnection
     *            Connection to initialize.
     * 
     * @param aPostContent
     *            POST content using JSON formatting or null if there is no post
     *            content.
     * 
     * @throws Exception
     *             An exception is thrown if an error occurred while
     *             initializing the instance.
     */
    private void initConnection(HttpsURLConnection aConnection, String aPostContent)
            throws Exception {
        // initialize connection
        aConnection.setDoInput(true);
        aConnection.setRequestProperty("Content-Type", "application/json");
        aConnection.setRequestProperty("SdkVersion", IQUSDK.SDK_VERSION);
        aConnection.setRequestProperty("SdkType", "Android");
        if (aPostContent == null) {
            aConnection.setDoOutput(false);
            aConnection.setRequestMethod("GET");
        } else {
            aConnection.setDoOutput(true);
            aConnection.setRequestMethod("POST");
            OutputStream stream = aConnection.getOutputStream();
            stream.write(aPostContent.getBytes("UTF-8"));
            stream.close();
        }
    }

    /**
     * Sends the data over the connection. The method will wait until the
     * sending has finished or a time-out has occurred or the network was
     * ordered to cancel any IO from another thread.
     * 
     * @param aConnection
     *            Connection that has been set up to send data.
     * 
     * @throws Exception
     *             An exception is thrown if the sending of data failed in some
     *             way.
     */
    @SuppressWarnings("UnusedAssignment")
    private void sendData(HttpsURLConnection aConnection) throws Exception {
        // create structure shared between send thread and this thread
        final SendInformation information = new SendInformation();
        information.connection = aConnection;
        information.error = null;
        information.finished = false;
        // determine system time the sending of the data must be finished before
        long endTime = System.currentTimeMillis() + IQUSDK.instance().getSendTimeout();
        // send data using a separate thread
        Thread sendThread = new Thread() {
            @Override
            public void run() {
                try {
                    information.connection.connect();
                } catch (Exception error) {
                    information.error = error;
                }
                information.finished = true;
            }
        };
        sendThread.start();
        // wait for io to finish or timeout or getting cancelled from
        // another thread
        while (!information.finished && (System.currentTimeMillis() < endTime) && !this.m_cancel) {
            Thread.sleep(10);
        }
        // clear reference to thread
        sendThread = null;
        // sending was cancelled?
        if (this.m_cancel) {
            throw new Exception("Sending was cancelled from other thread.");
        }
        // not finished, i.e. timeout error occurred?
        else if (!information.finished) {
            // yes, throw exception
            throw new Exception("Time out sending (max time allowed = "
                    + String.valueOf(IQUSDK.instance().getSendTimeout()) + "ms");
        }
        // exception occurred while trying to send data?
        else if (information.error != null) {
            // re-throw it
            throw information.error;
        }
    }

    /**
     * Processes the response and tries to parse the response as JSON data. The
     * method will add two custom fields to the result; see CODE and ERROR
     * constants.
     * <p>
     * If a parse error occurs, the result will contain a code with value -1
     * </p>
     * 
     * @param aConnection
     *            Connection to process response off
     * 
     * @return JSONObject instance
     * 
     * @throws Exception
     *             An exception can be thrown while obtaining response
     *             information.
     */
    private JSONObject processResponse(HttpsURLConnection aConnection) throws Exception {
        // get response
        int code = aConnection.getResponseCode();
        // result to return
        JSONObject result;
        // replace 100 with 200
        if (code == 100) {
            code = 200;
        }
        // get stream (depending on response code)
        InputStream stream = code >= 400 ? aConnection.getErrorStream() : aConnection
                .getInputStream();
        // get text
        String resultContent = this.streamToString(stream);
        // no content?
        if ((resultContent == null) || (resultContent.length() == 0)) {
            result = new JSONObject();
        } else {
            try {
                result = new JSONObject(resultContent);
            } catch (Exception error) {
                result = new JSONObject();
                code = -1;
                result.put(ERROR, "invalid JSON format");
            }
        }
        result.put(CODE, code);
        if (code > 399) {
            result.put(ERROR, "server response code " + code);
        }
        if (IQUSDK.DEBUG) {
            IQUSDK.instance().addLog("[Network] [Response Headers] "
                    + aConnection.getHeaderFields().toString());
            IQUSDK.instance().addLog("[Network] [Response Body] " + resultContent);
            IQUSDK.instance().addLog("[Network] [Code] " + String.valueOf(code));
        }
        return result;
    }

    /**
     * Send a signed message to the server. A HMAC256 hash is generated from the
     * post content.
     * 
     * @param anUrl
     *            URL to send to.
     * 
     * @param aPostContent
     *            Content posted to the server URL.
     * 
     * @return JSONObject result from server.
     */
    private JSONObject sendSigned(String anUrl, String aPostContent) {
        String signature = this.generateHMACSHA512(this.m_secretKey, aPostContent);
        return this.send(anUrl + "?api_key=" + this.m_apiKey + "&signature=" + signature,
                aPostContent);
    }

    /**
     * Sends a request to the server and processes the result.
     * <p>
     * If an error occurred while sending, the result will contain a field
     * ERROR.
     * </p>
     * <p>
     * The response code (if any) is stored in the field CODE.
     * </p>
     * 
     * @param anUrl
     *            URL to send request to
     * @param aPostContent
     *            POST content to send or null if there is no POST content.
     * 
     * @return JSONObject instance with result returned from server.
     */
    @SuppressWarnings({"incomplete-switch", "EmptyCatchBlock"})
    private JSONObject send(String anUrl, String aPostContent) {
        // debug
        if (IQUSDK.DEBUG) {
            IQUSDK.instance().addLog("[Network] [Sending] " + anUrl);
            if (aPostContent != null) {
                IQUSDK.instance().addLog("[Network] [Content] " + aPostContent.replace("\n", ""));
            }
        }
        // reset cancel
        this.m_cancel = false;
        // handle test mode
        switch (IQUSDK.instance().getTestMode()) {
            case SIMULATE_OFFLINE:
                return this.simulateOffline(anUrl, aPostContent);
            case SIMULATE_SERVER:
                return this.simulateServer(anUrl, aPostContent);
        }
        // contains result
        JSONObject result = null;
        try {
            // create connection
            HttpsURLConnection connection = this.createConnection(anUrl);
            try {
                // initialize connection
                this.initConnection(connection, aPostContent);
                // send data
                this.sendData(connection);
                // get result
                result = this.processResponse(connection);
            } finally {
                // make sure connection is disconnected
                connection.disconnect();
            }
        } catch (Exception error) {
            // error occurred during IO, use new result and store only error
            result = new JSONObject();
            try {
                result.put(ERROR, error.getMessage());
            } catch (Exception putError) {
            }
            if (IQUSDK.DEBUG) {
                IQUSDK.instance().addLog("[Network] [Error] " + error.getMessage());
            }
        }
        return result;
    }
}
