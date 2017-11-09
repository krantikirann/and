package com.example.android.wearable.wear.wearaccessibilityapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.MessagingStyle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

public class NotificationsActivity extends WearableActivity {

    public static final int NOTIFICATION_ID = 888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new NotificationsPrefsFragment())
                .commit();
    }

    public static class NotificationsPrefsFragment extends PreferenceFragment {

        private static final String TAG = "NotificationsActivity";
        private NotificationManagerCompat mNotificationManagerCompat;
        private boolean mActionOn;   //if true, displays in-line action
        private boolean mAvatarOn;   //if true, displays avatar of messenger

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.prefs_notifications);

            mNotificationManagerCompat = NotificationManagerCompat.from(getActivity());

            final SwitchPreference mActionSwitchPref = (SwitchPreference) findPreference(
                    getString(R.string.key_pref_action));
            final SwitchPreference mAvatarSwitchPref = (SwitchPreference) findPreference(
                    getString(R.string.key_pref_avatar));
            Preference mPushNotificationPref = findPreference(
                    getString(R.string.key_pref_push_notification));

            initInLineAction(mActionSwitchPref);
            initAvatar(mAvatarSwitchPref);
            initPushNotification(mPushNotificationPref);
        }

        public void initInLineAction(SwitchPreference switchPref) {
            switchPref.setChecked(true);
            mActionOn = switchPref.isChecked();
            switchPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mActionOn = (Boolean) newValue;
                    return true;
                }
            });
        }

        public void initAvatar(SwitchPreference switchPref) {
            switchPref.setChecked(true);
            mAvatarOn = switchPref.isChecked();
            switchPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mAvatarOn = (Boolean) newValue;
                    return true;
                }
            });
        }

        public void initPushNotification(Preference pref) {
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    generateMessagingStyleNotification();
                    return true;
                }
            });
        }

        /*
        * Generates a MESSAGING_STYLE Notification that supports both Wear 1.+ and Wear 2.0. For
        * devices on API level 24 (Wear 2.0) and after, displays MESSAGING_STYLE. Otherwise,
        * displays a basic BIG_TEXT_STYLE.
        *
        * IMPORTANT NOTE:
        * Notification Styles behave slightly different on Wear 2.0 when they are launched by a
        * native/local Wear app, i.e., they will NOT expand when the user taps them but will
        * instead take the user directly into the local app for the richest experience. In
        * contrast, a bridged Notification launched from the phone will expand with the style
        * details (whether there is a local app or not).
        *
        * If you want to enable an action on your Notification without launching the app, you can
        * do so with the setHintDisplayActionInline() feature (shown below), but this only allows
        * one action.
        *
        * If you wish to replicate the original experience of a bridged notification, please
        * review the generateBigTextStyleNotification() method above to see how.
        */
        private void generateMessagingStyleNotification() {
            Log.d(TAG, "generateMessagingStyleNotification()");

            // Main steps for building a MESSAGING_STYLE notification:
            //      0. Get your data
            //      1. Build the MESSAGING_STYLE
            //      2. Set up main Intent for notification
            //      3. Set up RemoteInput (users can input directly from notification)
            //      4. Build and issue the notification

            // 0. Get your data (everything unique per Notification)
            MockDatabase.MessagingStyleCommsAppData messagingStyleCommsAppData =
                    MockDatabase.getMessagingStyleData();

            // 1. Build the Notification.Style (MESSAGING_STYLE)
            String contentTitle = messagingStyleCommsAppData.getContentTitle();

            MessagingStyle messagingStyle = new NotificationCompat
                    .MessagingStyle(messagingStyleCommsAppData.getReplayName())
                    // You could set a different title to appear when the messaging style
                    // is supported on device (24+) if you wish. In our case, we use the same
                    // title.
                    .setConversationTitle(contentTitle);

            // Adds all Messages
            // Note: Messages include the text, timestamp, and sender
            for (MessagingStyle.Message message : messagingStyleCommsAppData.getMessages()) {
                messagingStyle.addMessage(message);
            }

            // 2. Set up main Intent for notification
            Intent notifyIntent = new Intent(getActivity(), MessagingMainActivity.class);

            PendingIntent mainPendingIntent =
                    PendingIntent.getActivity(
                            getActivity(),
                            0,
                            notifyIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );


            // 3. Set up a RemoteInput Action, so users can input (keyboard, drawing, voice)
            // directly from the notification without entering the app.

            // Create the RemoteInput specifying this key.
            String replyLabel = getString(R.string.reply_label);
            RemoteInput remoteInput = new RemoteInput.Builder(MessagingIntentService.EXTRA_REPLY)
                    .setLabel(replyLabel)
                    .build();

            // Create PendingIntent for service that handles input.
            Intent replyIntent = new Intent(getActivity(), MessagingIntentService.class);
            replyIntent.setAction(MessagingIntentService.ACTION_REPLY);
            PendingIntent replyActionPendingIntent =
                    PendingIntent.getService(getActivity(), 0, replyIntent, 0);

            // Enable action to appear inline on Wear 2.0 (24+). This means it will appear over the
            // lower portion of the Notification for easy action (only possible for one action).
            final NotificationCompat.Action.WearableExtender inlineActionForWear2 =
                    new NotificationCompat.Action.WearableExtender()
                            .setHintDisplayActionInline(mActionOn)
                            .setHintLaunchesActivity(false);

            NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                    R.drawable.reply,
                    replyLabel,
                    replyActionPendingIntent)
                    .addRemoteInput(remoteInput)
                    // Allows system to generate replies by context of conversation
                    .setAllowGeneratedReplies(true)
                    // Add WearableExtender to enable inline actions
                    .extend(inlineActionForWear2)
                    .build();


            // 4. Build and issue the notification

            // Because we want this to be a new notification (not updating current notification),
            // we create a new Builder. Later, we update this same notification, so we need to save
            // this Builder globally (as outlined earlier).

            NotificationCompat.Builder notificationCompatBuilder =
                    new NotificationCompat.Builder(getActivity());

            GlobalNotificationBuilder
                    .setNotificationCompatBuilderInstance(notificationCompatBuilder);

            // Builds and issues notification
            notificationCompatBuilder
                    // MESSAGING_STYLE sets title and content for Wear 1.+ and Wear 2.0 devices.
                    .setStyle(messagingStyle)
                    .setContentTitle(contentTitle)
                    .setContentText(messagingStyleCommsAppData.getContentText())
                    .setSmallIcon(R.drawable.watch)
                    .setContentIntent(mainPendingIntent)

                    // Number of new notifications for API <24 (Wear 1.+) devices
                    .setSubText(Integer.toString(messagingStyleCommsAppData
                            .getNumberOfNewMessages()))

                    .addAction(replyAction)
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setPriority(Notification.PRIORITY_HIGH)

                    // Hides content on the lock-screen
                    .setVisibility(Notification.VISIBILITY_PRIVATE);

            notificationCompatBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(),
                    mAvatarOn ? R.drawable.avatar : R.drawable.watch));


            // If the phone is in "Do not disturb mode, the user will still be notified if
            // the sender(s) is starred as a favorite.
            for (String name : messagingStyleCommsAppData.getParticipants()) {
                notificationCompatBuilder.addPerson(name);
            }

            Notification notification = notificationCompatBuilder.build();
            mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);

            // Close app to demonstrate notification in steam.
            getActivity().finish();
        }
    }
}