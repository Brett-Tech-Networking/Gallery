## Gallery: Privacy policy

Welcome to the Gallery app for Android

This is an open source Android app developed by Brett Tech Networking The source code is available on GitHub, the app is also available on Google Play.

As an avid Android user myself, I take privacy very seriously.
I know how irritating it is when apps collect your data without your knowledge.

I hereby state, to the best of my knowledge and belief, that I have not programmed this app to collect any personally identifiable information. All data (app preferences (like theme, etc.) and photos) created by you (the user) is stored on your device only, and can be simply erased by clearing the app's data or uninstalling it. 

### Explanation of permissions requested in the app

The list of permissions required by the app can be found in the `AndroidManifest.xml` file:

https://github.com/Brett-Tech-Networking/Gallery/blob/main/app/src/main/AndroidManifest.xml
<br/>

| Permission | Why it is required |
| :---: | --- |
| `android.permission.WRITE_EXTERNAL_STORAGE` | This is required to store and modify photos/videos on the device localy for access within the app |
| `android.permission.READ_EXTERNAL_STORAGE` | This is required to read the stored Photos on the device within the app. |
| `android.permission.READ_MEDIA_IMAGES` | This is required so the user may access there device gallery and existing images |
| `REQUEST_DELETE_PACKAGES` | This allows the user to delete images from there device via this application |
| `SET_WALLPAPER` | This is used to allow the user to set the selected image as there device wallpaper | 
| `USE_BIOMETRIC` | This is used to allow users to lock there images in our Secure Folder using biometrics, this only uses already set device biometrics and does not store or retain this outside the users device |
 <hr style="border:1px solid gray">

If you find any security vulnerability that has been inadvertently caused by me, or have any question regarding how the app protectes your privacy, please send me an email and I will surely try to fix it/help you.

Yours sincerely,  
Brett Hudson  
Brett Tech Networking  
android-app@brett-techrepair.com
