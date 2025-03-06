# NotifyETA
Using NotificationListenerService to get the ETA of googlemaps and send updates to contacts.

# References
* [Purpose of this app](#purpose-of-this-app)
* [Permissions for the app](#permissions-for-the-app)
* [HIGHLY Suggested Permissions](#highly-suggested-permissions)
* [How to setup the app for a drive](#how-to-setup-the-app-for-a-drive)



## Purpose of this app
This app will automate notifying the ETA of your drive on Google Maps to people you select using SMS and your contact list.
* There is one configuation on this app, a toggle that will change the final message of your drive depending on whether you have to walk after you've parked or if your destination is where you parked.
* This app should not have too many manual configurations as it should be a quick start before driving so the user can just let the app run and not worry about configuring anything.

## Permissions for the app
* The app ask to read your notifications since that is how it will read your current ETA on Google Maps (permission will warn that the app will read all your notifications but the repo will verify that the app only filters for google maps information).
* The app ask for SMS permisssions to be able to automate sending updates of your ETA to your selected contacts.
* The app ask for Contacts permissions to be able to list your contacts and save who you select to then notify them.

## HIGHLY Suggested Permissions
* Turn off battery optimization on the app to prevent the app from getting killed during your drive (then it will stop notifying your contacts).
* Some android phones have additional Power Usage Settings to manage Launch Settings, such as Running in the background, make sure that is managed manually and not automatically so the app won't be killed in the background.
Note: If the app is managed automatically to save power on your phone, there is a big chance that if your phone screen is off or not on the app, that the messages will not be sent until you go back on the NotifyETA app.

## How to setup the app for a drive
* NotifyETA should be opened first to select the contacts you want to notify before you start the drive.
* Once you have selected your contacts and you are happy with the configuration switch (toggle on if you are walking after you park or leave it off if you are not). Then the NotifyETA has a button to open Google Maps to start your drive.
  * You can also start your drive on Android Auto after you have setup the NotifyApp to your liking.
