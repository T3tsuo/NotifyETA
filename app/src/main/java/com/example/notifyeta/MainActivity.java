package com.example.notifyeta;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private TextView printNumber;
    private static final int REQUEST_READ_CONTACTS = 2;
    private static final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;
    private ActivityResultLauncher<Intent> pickContactLauncher;
    private ArrayList<String> phoneNumbers;
    private ArrayList<String> contactNames;
    private ReceiveBroadcastReceiver imageChangeBroadcastReceiver;
    public EtaTimer etaTimer;
    public boolean finalMessage;
    public boolean done;
    public boolean timerRunning;
    public String eta_left;
    public String time;
    CountDownTimer timerToSend;
    public Double eta_threshold;
    SwitchCompat walkingSwitch;

    @SuppressLint("QueryPermissionsNeeded")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        phoneNumbers = new ArrayList<>();
        contactNames = new ArrayList<>();
        etaTimer = new EtaTimer(0.0);
        finalMessage = false;
        done = false;
        timerRunning = false;
        timerToSend = new MyCountDownTimer(0, 0);
        walkingSwitch = findViewById(R.id.walkingSwitch);

        pickContactLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri contactUri = data.getData();
                            if (contactUri != null) {
                                String contactId = contactUri.getLastPathSegment();
                                getContactDetails(contactId);
                            }
                        }
                    }
                });

        // If the user did not turn the notification listener service on we prompt him to do so
        if (!isNotificationServiceEnabled()) {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        }


        Button contactBtn = findViewById(R.id.contactBtn);
        Button openMapsBtn = findViewById(R.id.openMapsButton);
        printNumber = findViewById(R.id.printNumber);

        contactBtn.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSION_REQUEST_CODE);
            }
            pickContact();
        });

        openMapsBtn.setOnClickListener(view -> {
                String uri = "geo:"; // Just "geo:" will open to the current location

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");

                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Google Maps is not installed.", Toast.LENGTH_SHORT).show();
                }
        });

        // Finally we register a receiver to tell the MainActivity when a notification has been received
        imageChangeBroadcastReceiver = new ReceiveBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.notifyeta");
        registerReceiver(imageChangeBroadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(imageChangeBroadcastReceiver);
        timerToSend.cancel();
    }

    private boolean isNotificationServiceEnabled(){
        String packageName = getPackageName();
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(packageName);
    }

    private void sendSms(String msg) {
        try {
            for (String phoneNumber: phoneNumbers) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, msg, null, null);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void sendEtaToContacts() {
        String message;
        // if this isn't the first message
        if (etaTimer.getEtaTimer() != 0) {
            if (convertToMinDouble(eta_left) > 5) {
                // once the timer is finished check our current eta to see if we are 5 mins away
                // if we are not, we are far away to create a regular message
                message = "I should be there in " + eta_left + " near " + time;
                // calculate the new timer that we will set
                etaTimer.calculateNewEtaTimer(convertToMinDouble(eta_left));
            } else {
                // we are close so send a shorter message and make sure the next message is will be 'i am here'
                message = "I should be there in " + eta_left;
                finalMessage = true;
            }
        } else {
            // if this is the first message of the app
            // let them know we are leaving now
            message = "Leaving now, I should be there in " + eta_left + " near " + time;
            // calculate the new timer that we will set
            etaTimer.calculateNewEtaTimer(convertToMinDouble(eta_left));
        }

        // send messages to contact(s)
        sendSms(message);
    }


    private void pickContact() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                Intent pickContactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                pickContactLauncher.launch(pickContactIntent); // launch the activity using the launcher
            } catch (Exception e) {
                Toast.makeText(this, "Need a valid Contacts App", Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickContact();
            }
        }
    }

    private void getContactDetails(String contactId) {
        Cursor cursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null,  // projection (columns to retrieve) - null gets all
                ContactsContract.Contacts._ID + " = ?", // selection (WHERE clause)
                new String[]{contactId}, // selection arguments
                null // sort order
        );

        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            String hasPhone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));

            if (hasPhone.equals("1")) {
                Cursor phoneCursor = getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null
                );

                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    phoneNumbers.add(phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    phoneCursor.close();
                }
            }
            contactNames.add(name);

            // remove brackets around list
            printNumber.setText(contactNames.toString().replace("[", "").replace("]", ""));
            // Use the retrieved contact details (name, phoneNumber, etc.)
            try{
                Log.d("Contact Details", "Name: " + name + ", Phone: " + phoneNumbers.get(phoneNumbers.size() - 1));
            } catch (Exception e) {
                Toast.makeText(this, "Error with chosen contact", Toast.LENGTH_SHORT).show();
            }

            cursor.close();
        }
    }

    public class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onFinish() {
            timerRunning = false;
            sendEtaToContacts();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            // if we are driving early and hit the threshold when the timer should go off
            // we send the message
            if (convertToMinDouble(eta_left) <= eta_threshold) {
                timerToSend.cancel();
                timerToSend.onFinish();
            }
        }
    }

    /**
     * Image Change Broadcast Receiver.
     * * We use this Broadcast Receiver to notify the Main Activity when
     * a new notification has arrived, so it can properly change the
     * notification image
     **/
    public class ReceiveBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String driveData = intent.getStringExtra("subText");

            // if we have driving mode data based on the circle
            if (driveData != null && (driveData.contains(" · ") || driveData.contains("finished"))) {
                driveMode(driveData);
            }

        }
    }

    public void driveMode(String text) {
        // if we are currently driving and aren't waiting to send the last message
        if (!text.equals("finished") && !finalMessage && !done) {
            // update our variables with new notification
            updateEta(text);
            // if we managed to get the data and we are not currently waiting to send a message
            if (!eta_left.isEmpty() && !time.isEmpty() && !timerRunning) {
                // eta threshold is when the message should be sent if we are not going expected speed
                eta_threshold = convertToMinDouble(eta_left) - etaTimer.getEtaTimer();
                // if next eta to send message is 5mins or less, set it to 2mins
                if (eta_threshold <= 5) {
                    eta_threshold = 2.0;
                }
                startTimerToSendMessage();
            }
        } else if (text.equals("finished") && finalMessage && !done) {
            // if we are there or if the user ends the trip manually and we are near
            timerToSend.cancel();
            String message;
            // if we are walking after we park, the final message will let them know
            if (walkingSwitch.isChecked()) {
                message = "Just parked, walking over now";
            } else{
                // if we are not walking, just say that we are there
                message = "I am here";
            }
            sendSms(message);
            // make sure nothing runs again
            done = true;
            message = "Arrived";
            printNumber.setText(message);
        }
    }

    public void startTimerToSendMessage() {
        // create a timer with the timer we calculated to wait and send a notification to the user
        if (eta_threshold == 2.0) {
            // timer so that it should send message when eta has 2mins left
            timerToSend = new MyCountDownTimer(Double.valueOf(60000 * convertToMinDouble(eta_left) - eta_threshold).longValue(), 1000);
            timerRunning = true;
            timerToSend.start();
        } else {
            timerToSend = new MyCountDownTimer(Double.valueOf(60000 * etaTimer.getEtaTimer()).longValue(), 1000);
            timerRunning = true;
            timerToSend.start();
        }

    }

    public void updateEta(String text) {
        eta_left = "";
        time = "";
        // get the data, but make sure we are getting enough data
        String[] temp = text.split(" · ");
        // if we are getting eta, distance and time then grab the data
        if (temp.length == 3) {
            eta_left = temp[0];
            time = temp[2];
        }
    }

    /*
     * Convert day hour or minute to double minute.
     */
    public double convertToMinDouble(String eta_left) {
        if (eta_left.contains(" d")) {
            String[] temp = eta_left.split(" d ");
            // day * 24 hours * 60mins to get mins
            Double dayMin = Double.parseDouble(temp[0]) * 24 * 60;
            // * 60 to get minutes
            Double hourMin = Double.parseDouble(temp[1].split(" hr")[0]) * 60;
            return dayMin + hourMin;
        } else if (eta_left.contains("hr")) {
            String[] temp = eta_left.split(" hr ");
            // hours multiplied by 60 to get mins
            Double hourMin = Double.parseDouble(temp[0]) * 60;
            Double min = Double.parseDouble(temp[1].split(" min")[0]);
            return hourMin + min;
        } else if (eta_left.contains("min")) {
            return Double.parseDouble(eta_left.split(" min")[0]);
        }

        return 0;
    }
}