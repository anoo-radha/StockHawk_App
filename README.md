# StockHawk
Android Application for stock details.
StockHawk application for Android with a collection widget. The stock information is updated every hour(using GCMTaskService).

Executed the improvements from User Feedback!
#### User Feedback for Stock Hawk:
* Right now I can't use this app with my screen reader. My friends love it, so I would love to download it, but the buttons don't tell my screen reader what they do.
* We need to prepare Stock Hawk for the Egypt release. Make sure our translators know what to change and make sure the Arabic script will format nicely.
* Stock Hawk allows me to track the current price of stocks, but to track their prices over time, I need to use an external program. It would be wonderful if you could show more detail on a stock, including its price over time.
* I use a lot of widgets on my Android device, and I would love to have a widget that displays my stock quotes on my home screen.
* I found a bug in your app. Right now when I search for a stock quote that doesn't exist, the app crashes.
* When I opened this app for the first time without a network connection, it was a confusing blank screen. I would love a message that tells me why the screen is blank or whether my stock quotes are out of date.

User feedbacks were used to enhance the application. 

*Implements:* IntentService for background communication to content provider and API;  Share Action Provider; GCMTaskService;  RecyclerView;  AppWidgetProvider;  CursorAdapter;  AppBarLayout;  SharedPreferences;  RTL;  Accessibility; [Schematic library](https://github.com/SimonVT/schematic) for creating ContentProvider; FloatingActionButton; [Stetho](http://facebook.github.io/stetho/")  
This is an assignment for udacity AND-P3:Advanced Android Development.

How to Run the Application:

This app can be run on Android Studio.

1. Download the .zip file from the github.
2. Extract the .zip file’s contents to a convenient location on your hard disk.
3. In the Android Studio, click on File->New->Import Project and select the unzipped project folder.
  Now all of the project files will be displayed in the project structure. Press the ‘Run’ button and choose the running device to run the application.
