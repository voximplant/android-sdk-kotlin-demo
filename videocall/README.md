# Voximplant Video Call Demo (Android)

This demo demonstrates basic video call and screen sharing functionality of the Voximplant Android SDK. 
The application supports video calls between this Android app and other apps that use any Voximplant SDK.
Based on MVVM architectural pattern.

## Features
The application is able to:
- log in to the Voximplant Cloud
- make an video call
- receive an incoming call
- put a call on hold / take it off hold
- change an audio device (speaker, receiver, wired headset, bluetooth headset) during a call
- mute audio during a call
- enable/disable sending video
- share screen
- change video camera
- receive push notifications (requires additional setup)
- turn off the touch screen during calls when your ear is close to the screen (proximity sensor usage)

#### Android 9
Because of the [limited access to sensors in background](https://developer.android.com/about/versions/pie/android-9.0-changes-all#bg-sensor-access) in Android 9,
we have made a foreground service to access microphone while app is in the background.

See the following file for code details:
- [CallService](src/main/java/com/voximplant/demos/kotlin/video_call/services/CallService.kt) (also used to work with proximity sensor)

#### Android 10
Because of the [restrictions on starting activities from the background](https://developer.android.com/guide/components/activities/background-starts) in Android 10,
we have made NotificationHelper class build and show full screen notifications

See the following file for code details:
- [NotificationHelper](src/main/java/com/voximplant/demos/kotlin/video_call/utils/NotificationHelper.kt)

## Install the app

### From the source code

1. Clone this repository
2. Select VideoCall and build the project using Android Studio

### Download the application build

Use the [invite link](https://appdistribution.firebase.dev/i/a5de9a57807a0f49) to get access the latest builds and subscribe for the application updates.

| :warning: &nbsp;&nbsp; Please consider that you need to set up a Voximplant account to make calls. Please follow the instructions below. |
| :--- |

| :warning: &nbsp;&nbsp; Push notifications require additional setup. If the application is built from the source code, [set up push notifications](https://voximplant.com/docs/howtos/sdks/push_notifications/android_sdk). If the application was installed from the invite link, push notifications cannot be configured. |
| :--- |

## Getting started
To get started, you'll need to [register](https://voximplant.com) a free Voximplant developer account.

You'll need the following:
- Voximplant application
- two Voximplant users
- VoxEngine scenario
- routing setup

### Automatic
We've implemented a special template to enable you to quickly use the demo – just 
install [SDK tutorial](https://manage.voximplant.com/marketplace/sdk_tutorial) from our marketplace:
<img src="../screenshots/market.png" width=800>

### Manual
You can set up it manually using our [quickstart guide](https://voximplant.com/docs/references/articles/quickstart) and tutorials

#### VoxEngine scenario example:
  ```
  require(Modules.PushService);
  VoxEngine.addEventListener(AppEvents.CallAlerting, (e) => {
  const newCall = VoxEngine.callUserDirect(
    e.call, 
    e.destination,
    e.callerid,
    e.displayName,
    null
  );
  VoxEngine.easyProcess(e.call, newCall, ()=>{}, true);
  });
  ```

## Usage

### User login
<img src="../screenshots/videocall_login.png" width=300>

Log in using:
* Voximplant user name in the format `user@app.account`
* password

See the following files for code details:
- [AuthService](src/main/java/com/voximplant/demos/kotlin/video_call/services/AuthService.kt)
- [LoginPackage](src/main/java/com/voximplant/demos/kotlin/video_call/stories/login)

### Make or receive calls
<img src="../screenshots/videocall_managing_call.png" width=600>

Enter a Voximplant user name to the input field and press "Call" button to make a call.

See the following files for code details:
- [VoximplantCallManager](src/main/java/com/voximplant/demos/kotlin/video_call/services/VoximplantCallManager.kt)
- [MainPackage](src/main/java/com/voximplant/demos/kotlin/video_call/stories/main)
- [incomingCallPackage](src/main/java/com/voximplant/demos/kotlin/video_call/stories/incoming_call)

### Call controls
<table>
  <tr>
    <td>Ongoing call</td>
    <td>Screen sharing</td>
    <td>Audio settings</td>
  </tr>
  <tr>
    <td><img src="../screenshots/videocall_ongoing_call.png" width=400></td>
    <td><img src="../screenshots/videocall_screen_sharing.png" width=400></td>
    <td><img src="../screenshots/videocall_audio_settings.png" width=400></td>
  </tr>
</table>

Mute, hold, change an audio device or video sending during a call.

See the following files for code details:
- [CallPackage](src/main/java/com/voximplant/demos/kotlin/video_call/stories/call)

## Useful links
1. [Quickstart](https://voximplant.com/docs/introduction)
2. [Voximplant Android SDK reference](https://voximplant.com/docs/references/androidsdk)
3. [Using Voximplant Android SDK](https://voximplant.com/docs/howtos/sdks/installing/android_sdk)
4. [HowTo's](https://voximplant.com/docs/howtos) 
5. [Push Notifications Tutorial](https://voximplant.com/docs/howtos/sdks/push_notifications/android_sdk)

## Have a question
- contact us via `support@voximplant.com`
- create an issue
- join our developer [community](https://discord.gg/sfCbT5u)