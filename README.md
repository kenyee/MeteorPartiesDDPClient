Meteor.js Android Meteor Parties Demo
=======================================

This is a sample Android application that uses the Android DDP Client library
to talk to Meteor.js' Party sample application (modified to store GPS party
locations instead of screen coordinates).

Usage
-----
To build this app, you have to add an Android library reference (not a Java
library reference) in Eclipse to the Android DDP Library.  Remember to get
your own Google Maps API key and put it into values/maps_api_key.xml

On phones, you have to click on the map marker callout to get to the party details.

Design
------
This is a simple application to illustrate usage of the DDP libraries 
so it has minimal usage of external libraries.
Standard tablet support is handled by this application by using a two-pane
UI for use on tablets via fragments.  On phones, it reverts to a single activity 
for the map and another activity for looking at Party RSVPs.  

The DDP State is kept in a singleton named MyDDPState which is initialized 
in MyApplication which is enabled in AndroidManifest.xml.  The default
data storage implementation in DDPStateSingleton is lightly wrappered by 
Party objects to present a POJO API to the rest of the application.

Because DDP is an async networking protocol, you can make network calls from the UI
layer without needing to run them in background tasks.  However, this is
complicated by the need to handle the callbacks when the async call finishes.
Android's LocalBroadcastManager allows an elegant way to handle events,
or you can use various other 3rd party eventbus systems like Otto, eventbus,
or guice.

Builds can be done via Eclipse or via the build.gradle.  If you're building in
Eclipse, you need to grab the 
[java-ddp-client](https://github.com/kenyee/java-ddp-client)
and
[android-ddp-client](https://github.com/kenyee/android-ddp-client)
projects and put them into your Eclipse workspace.  If you're using Gradle,
they will be pulled down via their artifacts in Maven Central and you don't have
to clutter up your workspace with them.

To-Do
-----
* Add parties by long pressing on map.
* Invite users to a private party.
