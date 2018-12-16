package com.liveperson.sample.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.liveperson.infra.CampaignInfo;
import com.liveperson.infra.ConversationViewParams;
import com.liveperson.infra.ICallback;
import com.liveperson.infra.Infra;
import com.liveperson.infra.InitLivePersonProperties;
import com.liveperson.infra.LPAuthenticationParams;
import com.liveperson.infra.LPConversationsHistoryStateToDisplay;
import com.liveperson.infra.callbacks.InitLivePersonCallBack;
import com.liveperson.messaging.sdk.api.LivePerson;
import com.liveperson.messaging.sdk.api.model.ConsumerProfile;
import com.liveperson.sample.app.Utils.SampleAppStorage;
import com.liveperson.sample.app.Utils.SampleAppUtils;
import com.liveperson.sample.app.notification.NotificationUI;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * ***** Sample app class - Not related to Messaging SDK ****
 * <p>
 * The main activity of the sample app
 */
public class MessagingActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = MessagingActivity.class.getSimpleName();

	public static final String CAMPAIGN_ID_KEY = "campaignId";
	public static final String ENGAGEMENT_ID_KEY = "engagementId";
	public static final String SESSION_ID_KEY = "sessionId";
	public static final String VISITOR_ID_KEY = "visitorId";
	public static final String ENGAGEMENT_CONTEXT_ID_KEY = "engagementContextId";

	private Switch mAuthenticationSwitch;

	private EditText mFirstNameView;
    private EditText mLastNameView;
    private EditText mPhoneNumberView;
    private EditText mAuthCodeView;
    private EditText mPublicKey;
    private EditText mJWTView;

    private Button mOpenConversationButton;

    private TextView mTime;
    private TextView mDate;

    private CheckBox mCallbackToastCheckBox;
    private CheckBox mReadOnlyModeCheckBox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        initSampleAppViews();
        initOpenConversationButton();
        initStartFragmentButton();

        handlePush(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handlePush(intent);
    }


    /**
     * Init Views
     */
    private void initSampleAppViews() {

        mAuthenticationSwitch = findViewById(R.id.auth_switch);
        mAuthenticationSwitch.setOnCheckedChangeListener(this);

        mFirstNameView = findViewById(R.id.first_name);
        mFirstNameView.setText(SampleAppStorage.getInstance(this).getFirstName());

        mLastNameView = findViewById(R.id.last_name);
        mLastNameView.setText(SampleAppStorage.getInstance(this).getLastName());

        mPhoneNumberView = findViewById(R.id.phone_number);
        mPhoneNumberView.setText(SampleAppStorage.getInstance(this).getPhoneNumber());

        mAuthCodeView = findViewById(R.id.auth_code);
        mAuthCodeView.setText(SampleAppStorage.getInstance(this).getAuthCode());

        mPublicKey = findViewById(R.id.public_key);
        mPublicKey.setText(SampleAppStorage.getInstance(this).getPublicKey());

        mJWTView = findViewById(R.id.jwt_input);
        mJWTView.setText(SampleAppStorage.getInstance(this).getJWT());

        String sdkVersion = String.format("SDK version %1$s ", LivePerson.getSDKVersion());
        ((TextView) findViewById(R.id.sdk_version)).setText(sdkVersion);



    }





    /**
     * Save the user input such as: account, first name, last name, phone number and auth code
     */
    private void saveAccountAndUserSettings() {
        String firstName = mFirstNameView.getText().toString().trim();
        String lastName = mLastNameView.getText().toString().trim();
        String phoneNumber = mPhoneNumberView.getText().toString().trim();
        String authCode = mAuthCodeView.getText().toString().trim();
        String publicKey = mPublicKey.getText().toString().trim();
        String jwt = mJWTView.getText().toString().trim();

        SampleAppStorage.getInstance(this).setFirstName(firstName);
        SampleAppStorage.getInstance(this).setLastName(lastName);
        SampleAppStorage.getInstance(this).setPhoneNumber(phoneNumber);
        SampleAppStorage.getInstance(this).setAuthCode(authCode);
        SampleAppStorage.getInstance(this).setJWT(jwt);
        SampleAppStorage.getInstance(this).setPublicKey(publicKey);
    }

    /**
     * Set the listener on the "open_conversation" button (Activity mode)
     */
    private void initOpenConversationButton() {
        mOpenConversationButton = findViewById(R.id.button_start_activity);
        mOpenConversationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Sample app setting - used to initialize the SDK with "Activity mode" when entering from push notification
                SampleAppStorage.getInstance(MessagingActivity.this).setSDKMode(SampleAppStorage.SDKMode.ACTIVITY);
                SampleAppUtils.disableButtonAndChangeText(mOpenConversationButton, getString(R.string.initializing));
                saveAccountAndUserSettings();
                removeNotification();
                initActivityConversation();
            }
        });
    }

    /**
     * Set the listener on the "Open Fragment" button (Fragment mode)
     */
    private void initStartFragmentButton() {
        Button openFragmentButton = findViewById(R.id.button_start_fragment);
        openFragmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Sample app setting - used to initialize the SDK with "Fragment mode" when entering from push notification
                SampleAppStorage.getInstance(MessagingActivity.this).setSDKMode(SampleAppStorage.SDKMode.FRAGMENT);
                saveAccountAndUserSettings();
                removeNotification();
                MainApplication.getInstance().setShowToastOnCallback(mCallbackToastCheckBox.isChecked());
                openFragmentContainer();
            }
        });
    }

    private void removeNotification() {
        NotificationUI.hideNotification(this);
    }

    /**
     * Initialize the Messaging SDK and start the SDK in "Activity Mode"
     */
    private void initActivityConversation() {

        LivePerson.initialize(MessagingActivity.this, new InitLivePersonProperties(SampleAppStorage.getInstance(MessagingActivity.this).getAccount(), SampleAppStorage.SDK_SAMPLE_FCM_APP_ID, new InitLivePersonCallBack() {
            @Override
            public void onInitSucceed() {
                Log.i(TAG, "SDK initialize completed with Activity mode");
                //we are not setting a call back here - we'll listen to callbacks with broadcast receiver
                // in main application class.
                //setCallBack();
                MainApplication.getInstance().setShowToastOnCallback(mCallbackToastCheckBox.isChecked());
                // you can't register pusher before initialization
                SampleAppUtils.handleGCMRegistration(MessagingActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        openActivity();
                        SampleAppUtils.enableButtonAndChangeText(mOpenConversationButton, getString(R.string.open_activity));
                    }
                });
            }

            @Override
            public void onInitFailed(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SampleAppUtils.enableButtonAndChangeText(mOpenConversationButton, getString(R.string.open_activity));
                        Toast.makeText(MessagingActivity.this, "Init Failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }));
    }

    /**
     * Start {@link FragmentContainerActivity} that handles the SDK the Messaging SDK and start the SDK in "Fragment Mode"
     */
    private void openFragmentContainer() {

    	Intent in = new Intent(MessagingActivity.this, FragmentContainerActivity.class);
        in.putExtra(Infra.KEY_READ_ONLY, isReadOnly());
        startActivity(in);
    }

    /**
     * Calling to "showConversation" API
     */
    private void openActivity() {

        String authCode = SampleAppStorage.getInstance(MessagingActivity.this).getAuthCode();
        String publicKey = SampleAppStorage.getInstance(MessagingActivity.this).getPublicKey();

        LPAuthenticationParams authParams = new LPAuthenticationParams();
        authParams.setAuthKey(authCode);
        authParams.addCertificatePinningKey(publicKey);

		CampaignInfo campaignInfo = SampleAppUtils.getCampaignInfo(this);
        ConversationViewParams params = getParams‎().setCampaignInfo(campaignInfo).setReadOnlyMode(isReadOnly());
		LivePerson.showConversation(MessagingActivity.this, authParams, params);

		ConsumerProfile consumerProfile = new ConsumerProfile.Builder()
                .setFirstName(mFirstNameView.getText().toString())
                .setLastName(mLastNameView.getText().toString())
                .setPhoneNumber(mPhoneNumberView.getText().toString())
                .build();
        LivePerson.setUserProfile(consumerProfile);

        //Constructing the notification builder for the upload/download foreground service and passing it to the SDK.
        Notification.Builder uploadBuilder = NotificationUI.createUploadNotificationBuilder(getApplicationContext());
        Notification.Builder downloadBuilder = NotificationUI.createDownloadNotificationBuilder(getApplicationContext());
        LivePerson.setImageServiceUploadNotificationBuilder(uploadBuilder);
        LivePerson.setImageServiceDownloadNotificationBuilder(downloadBuilder);
    }

    @NonNull
    private ConversationViewParams getParams‎() {
        return new ConversationViewParams(isReadOnly()).setHistoryConversationsStateToDisplay(LPConversationsHistoryStateToDisplay.ALL);
    }

    private boolean isReadOnly() {
        return mReadOnlyModeCheckBox.isChecked();
    }

    /**
     * If we initiated from a push message we show the screen that was in use the previous session (fragment/activity)
     * Activity mode is the default
     *
     * @param intent
     */
    private void handlePush(Intent intent) {
        boolean isFromPush = intent.getBooleanExtra(NotificationUI.NOTIFICATION_EXTRA, false);

        //Check if we came from Push Notification
        if (isFromPush) {
            clearPushNotifications();
            switch (SampleAppStorage.getInstance(this).getSDKMode()) {
                //Initialize the SDK with "Activity mode"
                case ACTIVITY:
                    initActivityConversation();
                    break;
                //Initialize the SDK with "Fragment mode"
                case FRAGMENT:
                    openFragmentContainer();
                    break;
            }
        }
    }

    /**
     * Hide any shown notification
     */
    private void clearPushNotifications() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NotificationUI.PUSH_NOTIFICATION_ID);
    }

    /**
     * @param language
     * @param country
     */
    protected void createLocale(String language, @Nullable String country) {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        Locale customLocale;

        if (TextUtils.isEmpty(language)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                language = resources.getConfiguration().getLocales().get(0).getCountry();
            } else {
                language = resources.getConfiguration().locale.getCountry();
            }
        }

        if (TextUtils.isEmpty(country)) {
            customLocale = new Locale(language);
        } else {
            customLocale = new Locale(language, country);
        }
        Locale.setDefault(customLocale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(customLocale);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        } else {
            configuration.locale = customLocale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }

        Locale locale = getLocale();
        Log.d(TAG, "country = " + locale.getCountry() + ", language = " + locale.getLanguage());

    }

    /**
     * @return
     */
    private Locale getLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return getResources().getConfiguration().getLocales().get(0);
        } else {
            return getResources().getConfiguration().locale;
        }
    }

    IntentFilter unreadMessagesCounterFilter = new IntentFilter(LivePerson.ACTION_LP_UPDATE_NUM_UNREAD_MESSAGES_ACTION);
    BroadcastReceiver unreadMessagesCounter = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int newValue = intent.getIntExtra(LivePerson.ACTION_LP_UPDATE_NUM_UNREAD_MESSAGES_EXTRA, 0);
            Log.d(TAG, "Got new value for unread messages counter: " + newValue);
            updateToolBar(newValue);
        }
    };

    private void updateToolBar(final int newValue) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (newValue > 0) {
                    setTitle(getResources().getString(R.string.messaging_title) + " (" + newValue + ") ");
                } else {
                    setTitle(R.string.messaging_title);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateToolBar(LivePerson.getNumUnreadMessages(SampleAppStorage.getInstance(MessagingActivity.this).getAccount()));
        registerReceiver(unreadMessagesCounter, unreadMessagesCounterFilter);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(unreadMessagesCounter);
        super.onPause();
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked){
            mJWTView.setVisibility(View.VISIBLE);
            mAuthCodeView.setVisibility(View.VISIBLE);
            mPublicKey.setVisibility(View.VISIBLE);
        }
        else {
            mJWTView.setVisibility(View.GONE);
            mAuthCodeView.setVisibility(View.GONE);
            mPublicKey.setVisibility(View.GONE);
        }

    }



}
