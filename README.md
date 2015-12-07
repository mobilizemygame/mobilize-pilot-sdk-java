# mobilize-pilot-sdk-java
Mobilize Pilot Java SDK

## Introduction

IQUSDK is a class that encapsulates the IQU SDK and offers various methods and properties to communicate with the IQU server.

All public methods in IQU SDK are thread safe.

## Installation

#### Installation in Eclipse

Either copy the file from the *bin/* folder to the *libs* folder of the application project or copy the files (preserving the folder structure) from the *src/* folder to the *src/* folder in the project.

#### Installation in Android Studio

Copy the file from the *bin/* folder to the *app/libs/* folder in the Android Studio Project. It might take a few seconds before the jar is processed; if the jar is not recognized either restart Android Studio or open module settings of the app, go to the dependencies tab and add a reference to the jar file.

Alternatively the source files from *src/* can be copied to the source folder in the Android Studio Project.

#### Help files

The *doc/* folder contains html help files generated from the source.

## Quick start

1. Import the IQUSDK files via `import com.iqu.sdk.IQUSDK;`
2. Methods and properties can be accessed trough the static `IQUSDK.instance()` method.
3. Call one of the `IQUSDK.instance().start()` methods to start the IQU SDK.
4. Add additional Ids via `IQUSDK.instance().setFacebookId()`, `IQUSDK.instance().setGooglePlusId()`, `IQUSDK.instance().setTwitterId()` or `IQUSDK.instance().setCustomId()`.
5. Start calling analytic tracking methods to send messages to the server.
6. Update the `payable` property to indicate the player is busy with a payable action.
7. The IQU SDK needs to be notified when the application is minimized to the background, is activated from the background. Call `IQUSDK.instance().pause()` from within the `Activity.onPause()` method and `IQUSDK.instance().resume()` from the `Activity.onResume()` method. 
8. To stop the update thread and release references and resources call `IQUSDK.instance.terminate()`; after this call the SDK reverts back to an uninitialized state. The `IQUSDK.instance()` method will return a new instance and one of the start methods has to be called again to start the SDK.

## Network communication

The IQU SDK uses a separate thread to send messages to the server (to prevent blocking the main thread). This means that there might be a small delay before messages are actually sent to the server. The maximum delay is determined by the `updateInterval` property.

If the SDK fails to send a message to the IQU server, messages are queued and are sent when the server is available again. The queued messages are stored in persistent storage so they still can be resent after an application restart.

While the IQU SDK is paused (because of a call to `IQUSDK.instance().pause()`) no messages are sent. Messages created by one of the `trackXXXXX` methods are placed in the internal message queue but will only be sent once `IQUSDK.instance().resume()` is called.

## Ids

The SDK supports various ids which are included with every tracking message sent to the server. See `IQUIdType` for the types supported by the SDK. Use `IQUSDK.instance().getId()` to get an id value.

Some ids are determined by the SDK itself, other ids must be set via one of the following methods: `IQUSDK.instance().setFacebookId()`, `IQUSDK.instance().setGooglePlusId()`, `IQUSDK.instance().setTwitterId()` or `IQUSDK.instance().setCustomId()`

The SDK supports Google Play services and tries to obtain the advertising id and limited ad tracking setting. The SDK will disable the analytic methods if it successfully obtained the limit ad tracking value and the Android user turned this option on (see the `analyticsEnabled` property).

The SDK does not use direct links to Google Play methods and classes but instead uses reflection to obtain the advertising ID and limit ad tracking. The SDK will not generate any errors if the Google Play jar files are not included within the application.

## Informational properties

The IQU SDK offers the following informational properties:

- `analyticsEnabled` indicates if the the analytics part of the IQU SDK is enabled. When the disabled, the `IQUSDK.instance().trackXXXXX()` methods will not send messages. The analytics are disabled when the user enabled limit ad tracking with the Google Play services.
- `serverAvailable` to get information if the messages were sent successfully or not.

## Testing

The IQU SDK contains the following properties to help with testing the SDK:

- `logEnabled` property to turn logging on or off.
- `log` property which will be filled with messages from various methods.
- `testMode` property to test the SDK without any server interaction or to simulate an offline situation with the server not being available.

The `IQUSDK.java` file defines a DEBUG constant, if no testing is required this constant can be set to false to allow the compiler optimization to excluded debug specific code.

## Advance timing

The IQU SDK offers several properties to adjust the various timings:

- `updateInterval` property determines the time between the internal update calls.
- `sendTimeout` property determines the maximum time sending a message to the server may take.
- `ceckServerInterval` property determines the time between checks for server availability. If sending of data fails, the update thread will wait the time, as set by this property, before trying to send the data again.
