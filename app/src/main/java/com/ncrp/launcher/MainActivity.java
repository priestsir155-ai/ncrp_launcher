package com.ncrp.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {

    // =========================================================================
    // NCRP DEFAULT CONFIGURATION (Automatically updated from GitHub JSON URL)
    // =========================================================================
    public static String REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/YOUR_GITHUB_USERNAME/NCRP-Launcher/main/launcher_config.json";

    private String serverName = "NEON CITY ROLEPLAY";
    private String mainServerIp = "15.235.128.112";
    private int mainServerPort = 7047;
    private String testServerIp = "15.235.128.112";
    private int testServerPort = 7777;

    private int remoteSkinsVersion = 1;
    private String skinsZipUrl = "";
    private String whatsappUrl = "https://chat.whatsapp.com/";
    private String discordUrl = "https://discord.gg/";
    private String youtubeUrl = "https://youtube.com/";
    private String instaUrl = "https://instagram.com/";
    private String webUrl = "https://open.mp";

    private String news1Title = "Next Update — Custom Skins & open.mp Engine";
    private String news1Body = "Welcome to NEON CITY ROLEPLAY (15.235.128.112:7047)! Custom dresses, skins, and map assets download automatically via this launcher.";
    private String news2Title = "NCRP Gang Turf Wars & New Factions";
    private String news2Body = "Set your Roleplay Nickname (Firstname_Lastname) in Player Profile and press PLAY NOW to join Neon City Roleplay!";

    // Theme Palette: NCRP Neon Blue & Crimson Red on Dark Glass
    private static final int COLOR_BG_DARK = Color.parseColor("#0B0F19");
    private static final int COLOR_CARD_BG = Color.parseColor("#CC141B2D");
    private static final int COLOR_NEON_BLUE = Color.parseColor("#00D2FF");
    private static final int COLOR_DEEP_BLUE = Color.parseColor("#1D4ED8");
    private static final int COLOR_NEON_RED = Color.parseColor("#FF2A54");
    private static final int COLOR_DEEP_RED = Color.parseColor("#BE123C");
    private static final int COLOR_TEXT_WHITE = Color.parseColor("#F8FAFC");
    private static final int COLOR_TEXT_MUTED = Color.parseColor("#94A3B8");
    private static final int COLOR_ONLINE_GREEN = Color.parseColor("#10B981");

    private SharedPreferences prefs;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private FrameLayout rootContainer;
    private View splashScreenView;
    private View homeScreenView;
    private View serversScreenView;
    private View settingsScreenView;
    private View bottomNavView;

    private TextView tvServerOnlineBadge;
    private TextView tvServerListCount;
    private TextView tvPlayerNameDisplay;
    private TextView tvSplashStatus;
    private TextView tvSplashPercent;
    private ProgressBar splashProgressBar;

    // Settings state
    private int settingFontSize = 7;
    private int settingMaxLines = 10;
    private int settingFpsLimit = 120;
    private boolean settingShowFps = true;
    private boolean settingSystemKeyboard = true;
    private boolean settingChatKill = true;
    private boolean settingAutoAim = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUI();

        prefs = getSharedPreferences("NCRP_LAUNCHER_PREFS", Context.MODE_PRIVATE);
        loadSavedSettings();

        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(COLOR_BG_DARK);

        ImageView bgImage = new ImageView(this);
        bgImage.setImageResource(R.drawable.ncrp_bg);
        bgImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bgImage.setAlpha(0.55f);
        rootContainer.addView(bgImage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Build all 4 screens matching KPSamp layout in NCRP Blue & Red theme
        splashScreenView = buildSplashScreen();
        homeScreenView = buildHomeScreen();
        serversScreenView = buildServersScreen();
        settingsScreenView = buildSettingsScreen();
        bottomNavView = buildBottomNavigation();

        rootContainer.addView(homeScreenView);
        rootContainer.addView(serversScreenView);
        rootContainer.addView(settingsScreenView);
        rootContainer.addView(bottomNavView);
        rootContainer.addView(splashScreenView);

        setContentView(rootContainer);
        showScreen("SPLASH");

        // Start initialization & remote skin check
        startStartupFlow();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void loadSavedSettings() {
        settingFontSize = prefs.getInt("font_size", 7);
        settingMaxLines = prefs.getInt("max_lines", 10);
        settingFpsLimit = prefs.getInt("fps_limit", 120);
        settingShowFps = prefs.getBoolean("show_fps", true);
        settingSystemKeyboard = prefs.getBoolean("system_keyboard", true);
        settingChatKill = prefs.getBoolean("chat_kill", true);
        settingAutoAim = prefs.getBoolean("auto_aim", false);
    }

    private void showScreen(String screen) {
        splashScreenView.setVisibility("SPLASH".equals(screen) ? View.VISIBLE : View.GONE);
        homeScreenView.setVisibility("HOME".equals(screen) ? View.VISIBLE : View.GONE);
        serversScreenView.setVisibility("SERVERS".equals(screen) ? View.VISIBLE : View.GONE);
        settingsScreenView.setVisibility("SETTINGS".equals(screen) ? View.VISIBLE : View.GONE);
        bottomNavView.setVisibility("SPLASH".equals(screen) ? View.GONE : View.VISIBLE);
    }

    // =========================================================================
    // SCREEN 1: SPLASH & AUTO SKIN/DRESS UPDATER SCREEN
    // =========================================================================
    private View buildSplashScreen() {
        FrameLayout layout = new FrameLayout(this);

        LinearLayout centerBox = new LinearLayout(this);
        centerBox.setOrientation(LinearLayout.VERTICAL);
        centerBox.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams cbParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        cbParams.setMargins(dp(40), 0, dp(40), dp(30));

        // NCRP Logo Row
        LinearLayout logoRow = new LinearLayout(this);
        logoRow.setOrientation(LinearLayout.HORIZONTAL);
        logoRow.setGravity(Gravity.CENTER);

        TextView tvNC = new TextView(this);
        tvNC.setText("NC");
        tvNC.setTextSize(TypedValue.COMPLEX_UNIT_SP, 68);
        tvNC.setTypeface(Typeface.DEFAULT_BOLD);
        tvNC.setTextColor(COLOR_NEON_RED);
        tvNC.setShadowLayer(18f, 0f, 0f, COLOR_NEON_RED);

        TextView tvRP = new TextView(this);
        tvRP.setText("SAMP");
        tvRP.setTextSize(TypedValue.COMPLEX_UNIT_SP, 68);
        tvRP.setTypeface(Typeface.DEFAULT_BOLD);
        tvRP.setTextColor(COLOR_NEON_BLUE);
        tvRP.setShadowLayer(18f, 0f, 0f, COLOR_NEON_BLUE);

        logoRow.addView(tvNC);
        logoRow.addView(tvRP);
        centerBox.addView(logoRow);

        // Glowing Red-Blue Divider Line
        View accentLine = new View(this);
        accentLine.setBackground(makeGradient(COLOR_NEON_RED, COLOR_NEON_BLUE, dp(4), 0, 0));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(dp(340), dp(4));
        lineParams.setMargins(0, dp(4), 0, dp(10));
        centerBox.addView(accentLine, lineParams);

        TextView tvSub = new TextView(this);
        tvSub.setText("N E O N   C I T Y   R O L E P L A Y");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvSub.setTypeface(Typeface.DEFAULT_BOLD);
        tvSub.setTextColor(COLOR_TEXT_WHITE);
        centerBox.addView(tvSub);

        layout.addView(centerBox, cbParams);

        // Bottom Loading Bar Section
        LinearLayout bottomBox = new LinearLayout(this);
        bottomBox.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams bbParams = new FrameLayout.LayoutParams(
                dp(560), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        bbParams.setMargins(0, 0, 0, dp(28));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        tvSplashStatus = new TextView(this);
        tvSplashStatus.setText("CHECKING UPDATES & CUSTOM SKINS...");
        tvSplashStatus.setTextColor(COLOR_TEXT_WHITE);
        tvSplashStatus.setTypeface(Typeface.DEFAULT_BOLD);
        tvSplashStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        LinearLayout.LayoutParams stParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        statusRow.addView(tvSplashStatus, stParams);

        tvSplashPercent = new TextView(this);
        tvSplashPercent.setText("0%");
        tvSplashPercent.setTextColor(COLOR_NEON_BLUE);
        tvSplashPercent.setTypeface(Typeface.DEFAULT_BOLD);
        tvSplashPercent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        statusRow.addView(tvSplashPercent);
        bottomBox.addView(statusRow);

        splashProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        splashProgressBar.setMax(100);
        splashProgressBar.setProgress(15);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(10));
        pbParams.setMargins(0, dp(8), 0, dp(10));
        bottomBox.addView(splashProgressBar, pbParams);

        TextView tvTip = new TextView(this);
        tvTip.setText("TIP · Type /help in game to see every command");
        tvTip.setTextColor(COLOR_TEXT_MUTED);
        tvTip.setGravity(Gravity.CENTER);
        tvTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        bottomBox.addView(tvTip);

        layout.addView(bottomBox, bbParams);
        return layout;
    }

    // =========================================================================
    // SCREEN 2: HOME DASHBOARD (News, Play Now, Player Profile, Quick Connect)
    // =========================================================================
    private View buildHomeScreen() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dp(22), dp(14), dp(22), dp(64));

        // Top Bar: Logo + SERVER ONLINE pill + Copyright
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout brandCol = new LinearLayout(this);
        brandCol.setOrientation(LinearLayout.VERTICAL);
        TextView tvLogo = new TextView(this);
        tvLogo.setText("NCRP");
        tvLogo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        tvLogo.setTypeface(Typeface.DEFAULT_BOLD);
        tvLogo.setTextColor(COLOR_NEON_RED);
        TextView tvBrandSub = new TextView(this);
        tvBrandSub.setText("NEON CITY ROLEPLAY");
        tvBrandSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvBrandSub.setTextColor(COLOR_NEON_BLUE);
        brandCol.addView(tvLogo);
        brandCol.addView(tvBrandSub);
        topBar.addView(brandCol);

        tvServerOnlineBadge = new TextView(this);
        tvServerOnlineBadge.setText("●  SERVER ONLINE · 15.235.128.112:7047");
        tvServerOnlineBadge.setTextColor(COLOR_ONLINE_GREEN);
        tvServerOnlineBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvServerOnlineBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvServerOnlineBadge.setPadding(dp(14), dp(6), dp(14), dp(6));
        tvServerOnlineBadge.setBackground(makeCardDrawable(Color.parseColor("#AA0F172A"), COLOR_NEON_BLUE, dp(20), dp(1)));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeParams.setMargins(dp(24), 0, 0, 0);
        topBar.addView(tvServerOnlineBadge, badgeParams);

        mainLayout.addView(topBar);

        // Two-Column Body: Left (News & Updates) + Right (Play Now, Profile, Socials)
        LinearLayout bodyRow = new LinearLayout(this);
        bodyRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        bodyParams.setMargins(0, dp(10), 0, 0);

        // Left Column: NEWS & UPDATES Card
        LinearLayout leftCard = new LinearLayout(this);
        leftCard.setOrientation(LinearLayout.VERTICAL);
        leftCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        leftCard.setBackground(makeCardDrawable(COLOR_CARD_BG, COLOR_NEON_RED, dp(16), dp(1)));
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.15f);
        leftParams.setMargins(0, 0, dp(14), 0);

        TextView tvNewsHeader = new TextView(this);
        tvNewsHeader.setText("● NEWS & UPDATES  ·  NCRP NEWSROOM");
        tvNewsHeader.setTextColor(COLOR_NEON_BLUE);
        tvNewsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvNewsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        leftCard.addView(tvNewsHeader);

        // Featured News Item 1
        LinearLayout newsCard1 = new LinearLayout(this);
        newsCard1.setOrientation(LinearLayout.VERTICAL);
        newsCard1.setPadding(dp(14), dp(12), dp(14), dp(12));
        newsCard1.setBackground(makeCardDrawable(Color.parseColor("#DD1E293B"), COLOR_NEON_BLUE, dp(12), dp(1)));
        LinearLayout.LayoutParams nc1Params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nc1Params.setMargins(0, dp(10), 0, dp(8));

        TextView tvN1Badge = new TextView(this);
        tvN1Badge.setText("FEATURED · LATEST");
        tvN1Badge.setTextColor(COLOR_NEON_RED);
        tvN1Badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvN1Badge.setTypeface(Typeface.DEFAULT_BOLD);
        newsCard1.addView(tvN1Badge);

        TextView tvN1Title = new TextView(this);
        tvN1Title.setText("📅 " + news1Title);
        tvN1Title.setTextColor(COLOR_TEXT_WHITE);
        tvN1Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvN1Title.setTypeface(Typeface.DEFAULT_BOLD);
        newsCard1.addView(tvN1Title);

        TextView tvN1Sub = new TextView(this);
        tvN1Sub.setText("Tap to read the full story →");
        tvN1Sub.setTextColor(COLOR_TEXT_MUTED);
        tvN1Sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        newsCard1.addView(tvN1Sub);

        newsCard1.setOnClickListener(v -> showInfoDialog(news1Title, news1Body));
        leftCard.addView(newsCard1, nc1Params);

        // Secondary News Item 2
        LinearLayout newsCard2 = new LinearLayout(this);
        newsCard2.setOrientation(LinearLayout.VERTICAL);
        newsCard2.setPadding(dp(14), dp(10), dp(14), dp(10));
        newsCard2.setBackground(makeCardDrawable(Color.parseColor("#AA1E293B"), Color.parseColor("#334155"), dp(12), dp(1)));
        TextView tvN2Title = new TextView(this);
        tvN2Title.setText("🎬 " + news2Title + "   ›");
        tvN2Title.setTextColor(COLOR_TEXT_WHITE);
        tvN2Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvN2Title.setTypeface(Typeface.DEFAULT_BOLD);
        newsCard2.addView(tvN2Title);
        newsCard2.setOnClickListener(v -> showInfoDialog(news2Title, news2Body));
        leftCard.addView(newsCard2);

        bodyRow.addView(leftCard, leftParams);

        // Right Column: PLAY NOW + PLAYER PROFILE + QUICK CONNECT
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);

        // 1. PLAY NOW Button (Neon Red to Blue Gradient)
        LinearLayout btnPlayNow = new LinearLayout(this);
        btnPlayNow.setOrientation(LinearLayout.VERTICAL);
        btnPlayNow.setGravity(Gravity.CENTER_VERTICAL);
        btnPlayNow.setPadding(dp(20), dp(12), dp(20), dp(12));
        btnPlayNow.setBackground(makeGradient(COLOR_NEON_RED, COLOR_DEEP_BLUE, dp(28), COLOR_NEON_BLUE, dp(1)));
        TextView tvPlayTitle = new TextView(this);
        tvPlayTitle.setText("▶   PLAY NOW");
        tvPlayTitle.setTextColor(COLOR_TEXT_WHITE);
        tvPlayTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvPlayTitle.setTypeface(Typeface.DEFAULT_BOLD);
        TextView tvPlaySub = new TextView(this);
        tvPlaySub.setText("      CHOOSE A SERVER  ·  15.235.128.112:7047");
        tvPlaySub.setTextColor(Color.parseColor("#E2E8F0"));
        tvPlaySub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnPlayNow.addView(tvPlayTitle);
        btnPlayNow.addView(tvPlaySub);
        btnPlayNow.setOnClickListener(v -> showScreen("SERVERS"));
        rightCol.addView(btnPlayNow);

        // 2. PLAYER PROFILE Card
        LinearLayout profileCard = new LinearLayout(this);
        profileCard.setOrientation(LinearLayout.VERTICAL);
        profileCard.setPadding(dp(16), dp(10), dp(16), dp(10));
        profileCard.setBackground(makeCardDrawable(COLOR_CARD_BG, COLOR_NEON_BLUE, dp(14), dp(1)));
        LinearLayout.LayoutParams profParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        profParams.setMargins(0, dp(10), 0, dp(10));

        TextView tvProfHeader = new TextView(this);
        tvProfHeader.setText("👤  PLAYER PROFILE");
        tvProfHeader.setTextColor(COLOR_NEON_RED);
        tvProfHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvProfHeader.setTypeface(Typeface.DEFAULT_BOLD);
        profileCard.addView(tvProfHeader);

        LinearLayout profRow = new LinearLayout(this);
        profRow.setOrientation(LinearLayout.HORIZONTAL);
        profRow.setGravity(Gravity.CENTER_VERTICAL);
        profRow.setPadding(0, dp(6), 0, 0);

        tvPlayerNameDisplay = new TextView(this);
        String savedName = prefs.getString("player_name", "Neon_Citizen");
        tvPlayerNameDisplay.setText(savedName + "
ROLEPLAY CHARACTER");
        tvPlayerNameDisplay.setTextColor(COLOR_TEXT_WHITE);
        tvPlayerNameDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvPlayerNameDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        profRow.addView(tvPlayerNameDisplay, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnEditName = new Button(this);
        btnEditName.setText("CHANGE NAME");
        btnEditName.setTextColor(COLOR_TEXT_WHITE);
        btnEditName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnEditName.setBackground(makeCardDrawable(Color.parseColor("#1E293B"), COLOR_NEON_RED, dp(18), dp(1)));
        btnEditName.setOnClickListener(v -> promptNicknameDialog());
        profRow.addView(btnEditName);

        profileCard.addView(profRow);
        rightCol.addView(profileCard, profParams);

        // 3. QUICK CONNECT Card (WhatsApp, Discord, YouTube, Insta, NCRP Web)
        LinearLayout socialCard = new LinearLayout(this);
        socialCard.setOrientation(LinearLayout.VERTICAL);
        socialCard.setPadding(dp(12), dp(8), dp(12), dp(10));
        socialCard.setBackground(makeCardDrawable(COLOR_CARD_BG, Color.parseColor("#334155"), dp(14), dp(1)));

        TextView tvSocHeader = new TextView(this);
        tvSocHeader.setText("QUICK CONNECT");
        tvSocHeader.setTextColor(COLOR_TEXT_MUTED);
        tvSocHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvSocHeader.setTypeface(Typeface.DEFAULT_BOLD);
        socialCard.addView(tvSocHeader);

        LinearLayout socRow = new LinearLayout(this);
        socRow.setOrientation(LinearLayout.HORIZONTAL);
        socRow.setPadding(0, dp(6), 0, 0);

        socRow.addView(makeSocialBtn("WHATSAPP", "#064E3B", "#10B981", () -> openUrl(whatsappUrl)),
                new LinearLayout.LayoutParams(0, dp(44), 1f));
        socRow.addView(makeSocialBtn("DISCORD", "#1E1B4B", "#6366F1", () -> openUrl(discordUrl)),
                new LinearLayout.LayoutParams(0, dp(44), 1f));
        socRow.addView(makeSocialBtn("YOUTUBE", "#450A0A", "#EF4444", () -> openUrl(youtubeUrl)),
                new LinearLayout.LayoutParams(0, dp(44), 1f));
        socRow.addView(makeSocialBtn("INSTA", "#4A044E", "#EC4899", () -> openUrl(instaUrl)),
                new LinearLayout.LayoutParams(0, dp(44), 1f));
        socRow.addView(makeSocialBtn("NCRP WEB", "#0C4A6E", "#00D2FF", () -> openUrl(webUrl)),
                new LinearLayout.LayoutParams(0, dp(44), 1f));

        socialCard.addView(socRow);
        rightCol.addView(socialCard);

        bodyRow.addView(rightCol, rightParams);
        mainLayout.addView(bodyRow, bodyParams);
        return mainLayout;
    }

    private View makeSocialBtn(String label, String bgHex, String borderHex, Runnable onClick) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(COLOR_TEXT_WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setBackground(makeCardDrawable(Color.parseColor(bgHex), Color.parseColor(borderHex), dp(10), dp(1)));
        tv.setOnClickListener(v -> onClick.run());
        return tv;
    }

    // =========================================================================
    // SCREEN 3: SERVERS LIST SCREEN (Like Photo 6)
    // =========================================================================
    private View buildServersScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(26), dp(18), dp(26), dp(70));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView tvSub = new TextView(this);
        tvSub.setText("NCRP  ·  CHOOSE WHERE TO PLAY");
        tvSub.setTextColor(COLOR_NEON_RED);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSub.setTypeface(Typeface.DEFAULT_BOLD);
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Servers");
        tvTitle.setTextColor(COLOR_TEXT_WHITE);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvSub);
        titleCol.addView(tvTitle);
        headerRow.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnBack = new Button(this);
        btnBack.setText("← BACK");
        btnBack.setTextColor(COLOR_TEXT_WHITE);
        btnBack.setBackground(makeCardDrawable(Color.parseColor("#1E293B"), COLOR_NEON_BLUE, dp(20), dp(1)));
        btnBack.setOnClickListener(v -> showScreen("HOME"));
        headerRow.addView(btnBack);
        layout.addView(headerRow);

        // Server Card 1: Main NEON CITY ROLEPLAY Server (15.235.128.112:7047)
        LinearLayout srv1 = new LinearLayout(this);
        srv1.setOrientation(LinearLayout.HORIZONTAL);
        srv1.setGravity(Gravity.CENTER_VERTICAL);
        srv1.setPadding(dp(20), dp(16), dp(20), dp(16));
        srv1.setBackground(makeCardDrawable(COLOR_CARD_BG, COLOR_NEON_BLUE, dp(16), dp(2)));
        LinearLayout.LayoutParams s1Params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        s1Params.setMargins(0, dp(18), 0, dp(14));

        LinearLayout s1Info = new LinearLayout(this);
        s1Info.setOrientation(LinearLayout.VERTICAL);
        TextView s1Name = new TextView(this);
        s1Name.setText("●  NEON CITY ROLEPLAY (MAIN)");
        s1Name.setTextColor(COLOR_TEXT_WHITE);
        s1Name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        s1Name.setTypeface(Typeface.DEFAULT_BOLD);
        TextView s1Ip = new TextView(this);
        s1Ip.setText("15.235.128.112:7047   ·   Mode: open.mp (Android & PC)");
        s1Ip.setTextColor(COLOR_NEON_BLUE);
        s1Ip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        s1Info.addView(s1Name);
        s1Info.addView(s1Ip);
        srv1.addView(s1Info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        tvServerListCount = new TextView(this);
        tvServerListCount.setText("👤 ONLINE / 100");
        tvServerListCount.setTextColor(COLOR_ONLINE_GREEN);
        tvServerListCount.setTypeface(Typeface.DEFAULT_BOLD);
        tvServerListCount.setPadding(dp(14), dp(8), dp(14), dp(8));
        tvServerListCount.setBackground(makeCardDrawable(Color.parseColor("#064E3B"), COLOR_ONLINE_GREEN, dp(18), dp(1)));
        srv1.addView(tvServerListCount);

        Button btnPlay1 = new Button(this);
        btnPlay1.setText("PLAY");
        btnPlay1.setTextColor(COLOR_TEXT_WHITE);
        btnPlay1.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlay1.setBackground(makeGradient(COLOR_NEON_RED, COLOR_DEEP_RED, dp(20), COLOR_NEON_RED, dp(1)));
        LinearLayout.LayoutParams bp1 = new LinearLayout.LayoutParams(dp(95), dp(42));
        bp1.setMargins(dp(12), 0, 0, 0);
        btnPlay1.setOnClickListener(v -> launchSampGame(mainServerIp, mainServerPort));
        srv1.addView(btnPlay1, bp1);

        layout.addView(srv1, s1Params);

        // Server Card 2: Test / Dev Server
        LinearLayout srv2 = new LinearLayout(this);
        srv2.setOrientation(LinearLayout.HORIZONTAL);
        srv2.setGravity(Gravity.CENTER_VERTICAL);
        srv2.setPadding(dp(20), dp(16), dp(20), dp(16));
        srv2.setBackground(makeCardDrawable(COLOR_CARD_BG, COLOR_NEON_RED, dp(16), dp(1)));

        LinearLayout s2Info = new LinearLayout(this);
        s2Info.setOrientation(LinearLayout.VERTICAL);
        TextView s2Name = new TextView(this);
        s2Name.setText("🔒  NEON CITY ROLEPLAY (TEST SERVER)");
        s2Name.setTextColor(COLOR_TEXT_WHITE);
        s2Name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        s2Name.setTypeface(Typeface.DEFAULT_BOLD);
        TextView s2Ip = new TextView(this);
        s2Ip.setText("15.235.128.112:7777   ·   Mode: Android Test");
        s2Ip.setTextColor(COLOR_TEXT_MUTED);
        s2Ip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        s2Info.addView(s2Name);
        s2Info.addView(s2Ip);
        srv2.addView(s2Info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnPlay2 = new Button(this);
        btnPlay2.setText("PLAY");
        btnPlay2.setTextColor(COLOR_TEXT_WHITE);
        btnPlay2.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlay2.setBackground(makeGradient(COLOR_DEEP_BLUE, COLOR_NEON_RED, dp(20), COLOR_NEON_BLUE, dp(1)));
        btnPlay2.setOnClickListener(v -> launchSampGame(testServerIp, testServerPort));
        srv2.addView(btnPlay2, bp1);

        layout.addView(srv2);
        return layout;
    }

    // =========================================================================
    // SCREEN 4: SETTINGS SCREEN (Like Photos 3, 4, 5)
    // =========================================================================
    private View buildSettingsScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(80));

        TextView tvHeader = new TextView(this);
        tvHeader.setText("⚙   Settings  ·  NCRP Mobile Configuration");
        tvHeader.setTextColor(COLOR_TEXT_WHITE);
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(tvHeader);

        // Section: CHAT SETTINGS & PERFORMANCE
        LinearLayout card1 = new LinearLayout(this);
        card1.setOrientation(LinearLayout.VERTICAL);
        card1.setPadding(dp(16), dp(14), dp(16), dp(14));
        card1.setBackground(makeCardDrawable(COLOR_CARD_BG, COLOR_NEON_BLUE, dp(14), dp(1)));
        LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cParams.setMargins(0, dp(12), 0, dp(12));

        TextView tvSec1 = new TextView(this);
        tvSec1.setText("💬  CHAT & PERFORMANCE SETTINGS (FPS: 30 / 60 / 90 / 120)");
        tvSec1.setTextColor(COLOR_NEON_BLUE);
        tvSec1.setTypeface(Typeface.DEFAULT_BOLD);
        tvSec1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        card1.addView(tvSec1);

        // FPS Selector Row
        LinearLayout fpsRow = new LinearLayout(this);
        fpsRow.setOrientation(LinearLayout.HORIZONTAL);
        fpsRow.setGravity(Gravity.CENTER_VERTICAL);
        fpsRow.setPadding(0, dp(8), 0, dp(8));
        TextView tvFpsLabel = new TextView(this);
        tvFpsLabel.setText("FPS Limit (30 / 60 / 90 / 120):");
        tvFpsLabel.setTextColor(COLOR_TEXT_WHITE);
        tvFpsLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        fpsRow.addView(tvFpsLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final int[] fpsOptions = {30, 60, 90, 120};
        for (int fpsVal : fpsOptions) {
            Button b = new Button(this);
            b.setText(String.valueOf(fpsVal));
            b.setTextColor(COLOR_TEXT_WHITE);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            b.setBackground(makeCardDrawable(
                    settingFpsLimit == fpsVal ? COLOR_NEON_RED : Color.parseColor("#1E293B"),
                    COLOR_NEON_BLUE, dp(10), dp(1)));
            b.setOnClickListener(v -> {
                settingFpsLimit = fpsVal;
                Toast.makeText(this, "FPS Limit set to " + fpsVal, Toast.LENGTH_SHORT).show();
            });
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(56), dp(36));
            bp.setMargins(dp(4), 0, 0, 0);
            fpsRow.addView(b, bp);
        }
        card1.addView(fpsRow);

        // Toggle Switches (Show FPS, System Keyboard, Chat Kill, Auto Aim)
        card1.addView(makeSwitchRow("Show FPS Counter on Screen", settingShowFps, val -> settingShowFps = val));
        card1.addView(makeSwitchRow("System Keyboard in Game", settingSystemKeyboard, val -> settingSystemKeyboard = val));
        card1.addView(makeSwitchRow("Chat Kill Messages", settingChatKill, val -> settingChatKill = val));
        card1.addView(makeSwitchRow("Auto Aim Assist", settingAutoAim, val -> settingAutoAim = val));

        layout.addView(card1, cParams);

        // Action Buttons Row 1: RESET TO DEFAULT | SAVE SETTINGS
        LinearLayout actRow1 = new LinearLayout(this);
        actRow1.setOrientation(LinearLayout.HORIZONTAL);
        Button btnReset = new Button(this);
        btnReset.setText("↺  RESET TO DEFAULT");
        btnReset.setTextColor(COLOR_TEXT_WHITE);
        btnReset.setBackground(makeCardDrawable(Color.parseColor("#1E293B"), COLOR_NEON_BLUE, dp(22), dp(1)));
        btnReset.setOnClickListener(v -> {
            settingFontSize = 7;
            settingMaxLines = 10;
            settingFpsLimit = 120;
            settingShowFps = true;
            settingSystemKeyboard = true;
            saveSettingsToDisk();
            Toast.makeText(this, "Settings reset to default!", Toast.LENGTH_SHORT).show();
        });

        Button btnSave = new Button(this);
        btnSave.setText("💾  SAVE SETTINGS");
        btnSave.setTextColor(COLOR_TEXT_WHITE);
        btnSave.setTypeface(Typeface.DEFAULT_BOLD);
        btnSave.setBackground(makeGradient(COLOR_NEON_RED, COLOR_DEEP_BLUE, dp(22), COLOR_NEON_RED, dp(1)));
        btnSave.setOnClickListener(v -> {
            saveSettingsToDisk();
            Toast.makeText(this, "Settings saved to SAMP/settings.ini!", Toast.LENGTH_SHORT).show();
        });

        LinearLayout.LayoutParams arParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        arParams.setMargins(dp(6), dp(6), dp(6), dp(6));
        actRow1.addView(btnReset, arParams);
        actRow1.addView(btnSave, arParams);
        layout.addView(actRow1);

        // Action Buttons Row 2: SWITCH GAME FILE | REINSTALL ASSETS (Custom Skins/Dresses)
        LinearLayout actRow2 = new LinearLayout(this);
        actRow2.setOrientation(LinearLayout.HORIZONTAL);
        Button btnSwitch = new Button(this);
        btnSwitch.setText("🔄  SWITCH GAME FILE");
        btnSwitch.setTextColor(COLOR_TEXT_WHITE);
        btnSwitch.setBackground(makeCardDrawable(Color.parseColor("#1E293B"), Color.parseColor("#10B981"), dp(22), dp(1)));
        btnSwitch.setOnClickListener(v -> showInfoDialog("Switch Game File",
                "NCRP Launcher automatically syncs with your installed GTA:SA / SA-MP data folder and overlays all NCRP custom dresses, skins, and UI files."));

        Button btnReinstall = new Button(this);
        btnReinstall.setText("⬇  REINSTALL CUSTOM SKINS / ASSETS");
        btnReinstall.setTextColor(COLOR_TEXT_WHITE);
        btnReinstall.setTypeface(Typeface.DEFAULT_BOLD);
        btnReinstall.setBackground(makeGradient(COLOR_DEEP_RED, COLOR_NEON_RED, dp(22), COLOR_NEON_BLUE, dp(1)));
        btnReinstall.setOnClickListener(v -> {
            prefs.edit().putInt("installed_skins_version", 0).apply();
            showScreen("SPLASH");
            startStartupFlow();
        });

        actRow2.addView(btnSwitch, arParams);
        actRow2.addView(btnReinstall, arParams);
        layout.addView(actRow2);

        scroll.addView(layout);
        return scroll;
    }

    private interface BoolCallback { void onChanged(boolean v); }

    private View makeSwitchRow(String title, boolean initial, BoolCallback cb) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(COLOR_TEXT_WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(initial);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> cb.onChanged(isChecked));
        row.addView(sw);
        return row;
    }

    // =========================================================================
    // BOTTOM NAVIGATION BAR (Home, Play, Settings)
    // =========================================================================
    private View buildBottomNavigation() {
        FrameLayout wrapper = new FrameLayout(this);
        LinearLayout navPill = new LinearLayout(this);
        navPill.setOrientation(LinearLayout.HORIZONTAL);
        navPill.setGravity(Gravity.CENTER);
        navPill.setPadding(dp(20), dp(6), dp(20), dp(6));
        navPill.setBackground(makeCardDrawable(Color.parseColor("#EE0F172A"), COLOR_NEON_BLUE, dp(30), dp(1)));

        FrameLayout.LayoutParams pillParams = new FrameLayout.LayoutParams(
                dp(260), dp(50), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        pillParams.setMargins(0, 0, 0, dp(10));

        Button bHome = new Button(this);
        bHome.setText("🏠");
        bHome.setTextColor(COLOR_TEXT_WHITE);
        bHome.setBackgroundColor(Color.TRANSPARENT);
        bHome.setOnClickListener(v -> showScreen("HOME"));

        Button bPlay = new Button(this);
        bPlay.setText("▶");
        bPlay.setTextColor(COLOR_TEXT_WHITE);
        bPlay.setBackground(makeGradient(COLOR_NEON_RED, COLOR_NEON_BLUE, dp(22), COLOR_TEXT_WHITE, dp(1)));
        bPlay.setOnClickListener(v -> showScreen("SERVERS"));

        Button bSet = new Button(this);
        bSet.setText("⚙");
        bSet.setTextColor(COLOR_TEXT_WHITE);
        bSet.setBackgroundColor(Color.TRANSPARENT);
        bSet.setOnClickListener(v -> showScreen("SETTINGS"));

        navPill.addView(bHome, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        navPill.addView(bPlay, new LinearLayout.LayoutParams(dp(54), dp(40)));
        navPill.addView(bSet, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        wrapper.addView(navPill, pillParams);
        return wrapper;
    }

    // =========================================================================
    // AUTO SKIN / DRESS DOWNLOADER & SAMP UDP QUERY ENGINE
    // =========================================================================
    private void startStartupFlow() {
        executor.execute(() -> {
            updateSplashProgress("CONNECTING TO NCRP CLOUD...", 20);
            fetchRemoteConfig();

            int installedVer = prefs.getInt("installed_skins_version", 0);
            if (skinsZipUrl != null && skinsZipUrl.startsWith("http")
                    && !skinsZipUrl.contains("YOUR_GITHUB_USERNAME")
                    && remoteSkinsVersion > installedVer) {
                updateSplashProgress("DOWNLOADING NEW NCRP DRESSES & SKINS (v" + remoteSkinsVersion + ")...", 40);
                boolean ok = downloadAndExtractSkinsZip(skinsZipUrl);
                if (ok) {
                    prefs.edit().putInt("installed_skins_version", remoteSkinsVersion).apply();
                }
            } else {
                for (int p = 30; p <= 90; p += 20) {
                    updateSplashProgress("ALMOST THERE... LOADING NCRP ASSETS", p);
                    try { Thread.sleep(220); } catch (InterruptedException ignored) {}
                }
            }

            saveSettingsToDisk();
            updateSplashProgress("READY!", 100);
            mainHandler.postDelayed(() -> {
                showScreen("HOME");
                querySampServerLive();
            }, 300);
        });
    }

    private void updateSplashProgress(String status, int percent) {
        mainHandler.post(() -> {
            tvSplashStatus.setText(status);
            tvSplashPercent.setText(percent + "%");
            splashProgressBar.setProgress(percent);
        });
    }

    private void fetchRemoteConfig() {
        if (REMOTE_CONFIG_URL == null || REMOTE_CONFIG_URL.contains("YOUR_GITHUB_USERNAME")) {
            return;
        }
        try {
            URL url = new URL(REMOTE_CONFIG_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                mainServerIp = json.optString("main_server_ip", mainServerIp);
                mainServerPort = json.optInt("main_server_port", mainServerPort);
                remoteSkinsVersion = json.optInt("skins_version", remoteSkinsVersion);
                skinsZipUrl = json.optString("skins_zip_url", skinsZipUrl);

                JSONObject soc = json.optJSONObject("social_links");
                if (soc != null) {
                    whatsappUrl = soc.optString("whatsapp", whatsappUrl);
                    discordUrl = soc.optString("discord", discordUrl);
                    youtubeUrl = soc.optString("youtube", youtubeUrl);
                    instaUrl = soc.optString("instagram", instaUrl);
                    webUrl = soc.optString("website", webUrl);
                }
            }
        } catch (Exception ignored) {}
    }

    private boolean downloadAndExtractSkinsZip(String zipUrl) {
        File tempZip = new File(getExternalFilesDir(null), "ncrp_custom_pack.zip");
        try {
            URL url = new URL(zipUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            int total = conn.getContentLength();

            InputStream in = new BufferedInputStream(conn.getInputStream());
            FileOutputStream out = new FileOutputStream(tempZip);
            byte[] buf = new byte[8192];
            int count;
            long downloaded = 0;
            while ((count = in.read(buf)) != -1) {
                downloaded += count;
                out.write(buf, 0, count);
                if (total > 0) {
                    int pct = 40 + (int) ((downloaded * 50L) / total);
                    updateSplashProgress("DOWNLOADING SKINS & MODS... " + (downloaded / 1024) + " KB", Math.min(90, pct));
                }
            }
            out.flush();
            out.close();
            in.close();

            updateSplashProgress("EXTRACTING CUSTOM DRESSES & SKINS...", 92);
            // Extract into all standard SA-MP Android game directories
            File[] targetDirs = getSampGameDirectories();
            for (File dir : targetDirs) {
                extractZipToDirectory(tempZip, dir);
            }
            tempZip.delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private File[] getSampGameDirectories() {
        File extRoot = Environment.getExternalStorageDirectory();
        return new File[] {
                getExternalFilesDir(null),
                new File(extRoot, "Android/data/ru.unisamp_mobile.game/files"),
                new File(extRoot, "Android/data/ro.alynsampmobile.game/files"),
                new File(extRoot, "Android/data/com.rockstargames.gtasa/files")
        };
    }

    private void extractZipToDirectory(File zipFile, File targetDir) {
        if (targetDir == null) return;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry ze;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File outFile = new File(targetDir, ze.getName());
                if (ze.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    File parent = outFile.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (Exception ignored) {}
    }

    // Live SA-MP / open.mp UDP Query ('i' opcode) for 15.235.128.112:7047
    private void querySampServerLive() {
        executor.execute(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(2500);
                InetAddress addr = InetAddress.getByName(mainServerIp);
                byte[] ipBytes = addr.getAddress();
                byte[] packet = new byte[11];
                packet[0] = 'S'; packet[1] = 'A'; packet[2] = 'M'; packet[3] = 'P';
                System.arraycopy(ipBytes, 0, packet, 4, 4);
                packet[8] = (byte) (mainServerPort & 0xFF);
                packet[9] = (byte) ((mainServerPort >> 8) & 0xFF);
                packet[10] = 'i';

                long t0 = System.currentTimeMillis();
                socket.send(new DatagramPacket(packet, packet.length, addr, mainServerPort));

                byte[] recvBuf = new byte[512];
                DatagramPacket resp = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(resp);
                long ping = System.currentTimeMillis() - t0;

                ByteBuffer bb = ByteBuffer.wrap(resp.getData(), 11, resp.getLength() - 11).order(ByteOrder.LITTLE_ENDIAN);
                bb.get(); // password byte
                int players = bb.getShort() & 0xFFFF;
                int maxPlayers = bb.getShort() & 0xFFFF;

                mainHandler.post(() -> {
                    tvServerOnlineBadge.setText("●  SERVER ONLINE  ·  " + players + "/" + maxPlayers + "  (" + ping + "ms)");
                    tvServerListCount.setText("👤 " + players + " / " + maxPlayers);
                });
            } catch (Exception ignored) {}
        });
    }

    // =========================================================================
    // WRITE SAMP/settings.ini AND LAUNCH GAME
    // =========================================================================
    private void saveSettingsToDisk() {
        prefs.edit()
                .putInt("font_size", settingFontSize)
                .putInt("max_lines", settingMaxLines)
                .putInt("fps_limit", settingFpsLimit)
                .putBoolean("show_fps", settingShowFps)
                .putBoolean("system_keyboard", settingSystemKeyboard)
                .putBoolean("chat_kill", settingChatKill)
                .putBoolean("auto_aim", settingAutoAim)
                .apply();

        String playerName = prefs.getString("player_name", "Neon_Citizen");
        String iniContent = "[client]
"
                + "name = " + playerName + "
"
                + "host = " + mainServerIp + "
"
                + "port = " + mainServerPort + "
"
                + "password = 

"
                + "[gui]
"
                + "Font = Arial.ttf
"
                + "FontSize = " + settingFontSize + ".0
"
                + "ChatMaxLines = " + settingMaxLines + "
"
                + "fps = " + settingFpsLimit + "
"
                + "Timestamp = 1
"
                + "androidKeyboard = " + (settingSystemKeyboard ? 1 : 0) + "
";

        for (File dir : getSampGameDirectories()) {
            if (dir == null) continue;
            try {
                File sampDir = new File(dir, "SAMP");
                sampDir.mkdirs();
                File settingsFile = new File(sampDir, "settings.ini");
                FileWriter fw = new FileWriter(settingsFile, false);
                fw.write(iniContent);
                fw.close();
            } catch (Exception ignored) {}
        }
    }

    private void launchSampGame(String ip, int port) {
        String playerName = prefs.getString("player_name", "Neon_Citizen");
        if (playerName.trim().isEmpty() || "Neon_Citizen".equals(playerName)) {
            promptNicknameDialog();
            return;
        }
        mainServerIp = ip;
        mainServerPort = port;
        saveSettingsToDisk();

        String[] gamePackages = {
                "ru.unisamp_mobile.game",
                "ro.alynsampmobile.game",
                "ro.alynsampmobile.launcher",
                "com.rockstargames.gtasa"
        };
        PackageManager pm = getPackageManager();
        for (String pkg : gamePackages) {
            Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
            if (launchIntent != null) {
                launchIntent.putExtra("host", ip);
                launchIntent.putExtra("port", port);
                launchIntent.putExtra("name", playerName);
                startActivity(launchIntent);
                return;
            }
        }

        showInfoDialog("Connect to NEON CITY ROLEPLAY",
                "Your NCRP settings & custom skins have been saved!

"
                + "• Server IP: " + ip + ":" + port + "
"
                + "• Nickname: " + playerName + "
"
                + "• FPS Limit: " + settingFpsLimit + " FPS

"
                + "Install the SA-MP Mobile Game Engine APK to launch directly into Neon City Roleplay.");
    }

    private void promptNicknameDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Enter Your Roleplay Name (Firstname_Lastname)");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(prefs.getString("player_name", ""));
        input.setHint("e.g. Ser_Ramanan");
        b.setView(input);
        b.setPositiveButton("SAVE", (d, w) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                prefs.edit().putString("player_name", name).apply();
                tvPlayerNameDisplay.setText(name + "
ROLEPLAY CHARACTER");
                saveSettingsToDisk();
            }
        });
        b.setNegativeButton("CANCEL", null);
        b.show();
    }

    private void showInfoDialog(String title, String msg) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Link: " + url, Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // UI STYLING HELPERS
    // =========================================================================
    private GradientDrawable makeCardDrawable(int bgColor, int strokeColor, int radiusPx, int strokeWidthPx) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(radiusPx);
        gd.setStroke(strokeWidthPx, strokeColor);
        return gd;
    }

    private GradientDrawable makeGradient(int startColor, int endColor, int radiusPx, int strokeColor, int strokeWidthPx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, new int[]{startColor, endColor});
        gd.setCornerRadius(radiusPx);
        if (strokeWidthPx > 0) gd.setStroke(strokeWidthPx, strokeColor);
        return gd;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
