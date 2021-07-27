# ConnectionService
This article provides a step-by-step tutorial to integrate a self-managed `ConnectionService` to a VoIP Android application based on Voximplant Android SDK.

## Manifest declarations and permissions
To integrate a self-managed `ConnectionService` it is required to declare the `MANAGE_OWN_CALLS` permission in the AndroidManifest.xml.
```XML
<uses-permission android:name="android.permission.MANAGE_OWN_CALLS" />
```
AndroidManifest.xml must declare a `ConnectionService` service. The telecom subsystem requires that the service declares the `BIND_TELECOM_CONNECTION_SERVICE` permission to be able to bind to it.
```XML
<service
    android:name=".services.CallConnectionService"
    android:permission="android.permission.BIND_TELECOM_CONNECTION_SERVICE">
    <intent-filter>
        <action android:name="android.telecom.ConnectionService" />
    </intent-filter>
</service>
```

## TelecomManager and PhoneAccount
`TelecomManager` uses a registered `PhoneAccount` to place a phone/VoIP call. To build a `PhoneAccount`, the app should supply a valid `PhoneAccountHandle` that references the connection service implementation `Telecom` will use to interact with the app.

A `PhoneAccountHandle` is made of the component name of the associated connection service and a string identifier that is unique across `PhoneAccountHandles` with the same component name. See `PhoneAccountHandler` on the [Android Developers](https://developer.android.com/reference/android/telecom/PhoneAccountHandle).

```Kotlin
val componentName = ComponentName(context, CallConnectionService::class.java)
val phoneAccountHandle = PhoneAccountHandle(componentName, APP_TAG)
val phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle)
telecomManager.registerPhoneAccount(phoneAccount)
```
For a self-managed `ConnectionService` it is required to set `PhoneAccount.CAPABILITY_SELF_MANAGED` to indicate that this `PhoneAccount` is responsible for managing its own `Connection`.

## Create and manage call connection

### Android Telecom Connection
A `Connection` represents a phone call or connection to a remote endpoint that carries voice and/or video traffic.

According to that, for each Voximplant call (`ICall`) it is required to create a new Android Telecom Connection. This way Voximplant `ICall` interface is used to manage a VoIP call and Telecom Connection registers this call in the telecom subsystem.

Every self-managed `Connection` must be specified with the following parameters:
```Kotlin
connectionProperties = PROPERTY_SELF_MANAGED
audioModeIsVoip = true
```

It is also possible to specify additional features such as put a call on hold or mute via `connectionCapabilities`. Full list of features on the [Android Developers](https://developer.android.com/reference/android/telecom/Connection).

Please find the details for creating a new Connection for outgoing and incoming VoIP calls below.

### Make outgoing call
To create an outgoing call, it is recommended to check that a `PhoneAccount` is registered and then invoke `TelecomManager.placeCall()` method.

A self-managed `ConnectionService` must add `EXTRA_PHONE_ACCOUNT_HANDLE` to the Bundle extra to specify its associated `PhoneAccountHandle`.

If the app is able to place an outgoing call, the telecom subsystem invokes the `onCreateOutgoingConnection()` method where the app must return a new `Connection` to represent a new outgoing call.

It is important to call `Connection.setActive()` once the outgoing call is connected. This will notify the telecom subsystem that the call is in progress.

<img src="../screenshots/schemes/connection_scheme_outgoing_call.png">

### Receive incoming call
When the app receives an incoming call via Voximplant SDK (`IClientIncomingCallListener.onIncomingCall()`), it is necessary to check the `PhoneAccount` registration in the system. It is required to invoke the `TelecomManager.addNewinsomingCall()` method to register a new incoming call.

The telecom subsystem binds to the app's `ConnectionService` and requests a new `Connection` instance for the incoming call by invoking `ConnectionService.onCreateIncomingConnection()` API. Upon receiving this event, the application needs to create a `Connection`.

For a self-managed connection service, the `Connection` will invoke `Connection.onShowIncomingCallUi()` event to let the application know when to display the incoming call UI. It is recommended to show a notification with full-screen intent.

The user may accept and reject the incoming call from the incoming call notification or from wearbale devices:
- If the user answers or rejects the call from the notification, it should be handled by the application via PendingIntent.
- If the user answers or rejects the call from a wearable device, the `Connection` will inform about this action by invoking `Connection.onAnswer()` or `Connection.onReject()` event.

To answer the call, it is required to call `ICall.answer()` API to establish the call connection using Voximplant Android SDK and update the `Connection` state to active once the call is connected (`ICallListener.onCallConnected()`).

To reject the call, the application must invoke `ICall.reject()` API. Voximplant Android SDK will invoke `ICallListener.onCallDisconnected()` event as soon as the call is ended and the application must set the `Connection` status to disconnected and destroy the `Connection`.

<img src="../screenshots/schemes/connection_scheme_incoming_call.png">

### Manage ongoing call
Voximplant Android SDK provides the following capabilities for an ongoing call:
- Put the call on hold
- Mute
- Change the audio device

Mute and audio device selection capabilities are fully managed by Voximplant Android SDK and do not require any `Connection` API call.

However, to put the call on hold, the application must change the `Connection` state via the `Connection.setOnHold()` API. Please consider that the `Connection` state must be changed only if `ICall.hold()` is completed succesfully. 

```Kotlin
call?.hold(hold, object : ICallCompletionHandler {
    override fun onComplete() {
        if (hold) {
            callConnection?.setOnHold()
        } else {
            callConnection?.setActive()
        }
    }

    override fun onFailure(e: CallException) {}
})
```

Please note:
- On devices running Android API level 28 or higher, when an incoming PSTN call comes and is answered by the user, the VoIP ongoing call will be put on hold by `TelecomManager` and could be resumed after the PSTN call ends.
- On devices running Android API level 27 or lower, only one app can maintain an ongoing call at any given time. This constraint means that the ongoing call will be terminated.

<img src="../screenshots/schemes/connection_scheme_ongoing_call.png">

### End ongoing call
The ongoing call may be ended by the user or the remote party. 

If the user ends the call, the application should call `ICall.hangup()` API and Voximplant Android SDK will invoke `ICallListener.onCallDisconnected()` event. 

If the remote party has ended the call, Voximplant Android SDK will inform the application by invoking `ICallListener.onCallDisconnected()` event.

Upon receiving `ICallListener.onCallDisconnected()` event it is reqired:
1. Change the `Connection` status to disconnected via `Connection.setDisconnected(DisconnectCause)`
2. Destroy the `Connection` via `Connection.destroy()` API to tears down the Connection object.

`DisconnectCause` describes the cause of a disconnected call. This always includes a code describing the generic cause of the disconnect. Optionally, it may include a label and/or description to display to the user. It is the responsibility of the `ConnectionService` to provide localized versions of the label and description. It also may contain a reason for the disconnect, which is intended for logging and not for display to the user.

See `DisconnectCause` on the [Android Developers](https://developer.android.com/reference/android/telecom/DisconnectCause).

<img src="../screenshots/schemes/connection_scheme_disconnect_cause.png">