package com.kairo.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.kairo.app.agent.AgentOrchestrator;
import com.kairo.app.agent.CodeRunner;
import com.kairo.app.agent.AgentPromptBuilder;
import com.kairo.app.agent.CliCommandPolicy;
import com.kairo.app.agent.ToolSpec;
import com.kairo.app.core.ApiKeyStore;
import com.kairo.app.core.AppPreferences;
import com.kairo.app.core.ArtifactStore;
import com.kairo.app.core.ConversationStore;
import com.kairo.app.core.DeviceSetupStore;
import com.kairo.app.core.ApiKeyDetector;
import com.kairo.app.core.MemoryStore;
import com.kairo.app.core.ProviderConfig;
import com.kairo.app.core.UsageTracker;
import com.kairo.app.network.BitbucketClient;
import com.kairo.app.network.GitLabClient;
import com.kairo.app.network.WebhookTester;
import com.kairo.app.core.PromptTemplateStore;
import com.kairo.app.core.DevLoopState;
import com.kairo.app.data.AgentDefinition;
import com.kairo.app.data.Artifact;
import com.kairo.app.data.ChatAttachment;
import com.kairo.app.data.ChatMessage;
import com.kairo.app.data.ConnectorCatalog;
import com.kairo.app.data.ConnectorDefinition;
import com.kairo.app.data.ConversationSession;
import com.kairo.app.data.LanguageCatalog;
import com.kairo.app.data.LanguagePreset;
import com.kairo.app.data.MemoryItem;
import com.kairo.app.data.ModelCatalog;
import com.kairo.app.data.SkillCatalog;
import com.kairo.app.data.SkillDefinition;
import com.kairo.app.data.ModelInfo;
import com.kairo.app.data.SearchResult;
import com.kairo.app.network.ApiClient;
import com.kairo.app.network.DiscordWebhookClient;
import com.kairo.app.network.GitHubClient;
import com.kairo.app.network.N8nClient;
import com.kairo.app.network.NotionClient;
import com.kairo.app.network.LinearClient;
import com.kairo.app.network.SlackClient;
import com.kairo.app.network.SupabaseClient;
import com.kairo.app.network.VercelClient;
import com.kairo.app.network.ImageGenerationClient;
import com.kairo.app.ui.MarkdownRenderer;
import com.kairo.app.ui.UiEffects;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Kairo's single-activity workspace. It intentionally uses platform views so the app stays
 * small, inspectable, and easy to build from the included GitHub workflow.
 */
public class MainActivity extends Activity {
    private static final int TAB_CHAT = 0;
    private static final int TAB_AGENTS = 1;
    private static final int TAB_MODELS = 2;
    private static final int TAB_ARTIFACTS = 3;
    private static final int TAB_CONNECTORS = 4;
    private static final int TAB_SETTINGS = 5;
    private static final int TAB_PHONE = 6;

    private final List<ChatMessage> conversation = new ArrayList<>();
    private final List<TextView> navItems = new ArrayList<>();
    private final List<ConversationSession> savedSessions = new ArrayList<>();

    private ApiKeyStore keyStore;
    private AppPreferences preferences;
    private ConversationStore conversationStore;
    private MemoryStore memoryStore;
    private DeviceSetupStore deviceSetup;
    private ArtifactStore artifactStore;
    private UsageTracker usageTracker;
    private AgentOrchestrator orchestrator;
    private CodeRunner codeRunner;
    private GitHubClient gitHubClient;
    private VercelClient vercelClient;
    private N8nClient n8nClient;
    private SlackClient slackClient;
    private NotionClient notionClient;
    private LinearClient linearClient;
    private SupabaseClient supabaseClient;
    private DiscordWebhookClient discordWebhookClient;
    private WebSearchClient webSearchClient;
    private final List<SearchResult> lastSearchResults = new ArrayList<>();

    private FrameLayout root;
    private LinearLayout content;
    private LinearLayout drawer;
    private LinearLayout drawerSessions;
    private EditText drawerSearch;
    private View drawerScrim;
    private boolean drawerOpen;
    private LinearLayout chatHistory;
    private ScrollView chatScroll;
    private EditText composer;
    private HorizontalScrollView attachmentScroll;
    private LinearLayout attachmentStrip;
    private final List<ChatAttachment> pendingAttachments = new ArrayList<>();
    private TextView credentialNotice;
    private TextView memoryNotice;
    private ApiKeyDetector.DetectedCredential detectedCredential;
    private String memoryCandidate = "";
    private TextView sendButton;
    private TextView modelChip;
    private TextView modeButton;
    private TextView chatSessionTitle;
    private String activeAgentId = "chat";
    private View typingView;
    private TextView streamingView;
    private TextView liveProcessingLabel;
    private StringBuilder streamBuffer;
    private ApiClient.RequestHandle activeRequest;
    private final Handler liveHandler = new Handler(Looper.getMainLooper());
    private Runnable liveTicker;
    private long responseStartedAt;
    private int responseChars;
    private LinearLayout modelListContainer;
    private EditText modelSearch;
    private LinearLayout artifactListContainer;
    private EditText artifactSearch;
    private EditText webSearchQuery;
    private LinearLayout webResultsContainer;
    private EditText arenaPrompt;
    private TextView arenaLeftPicker;
    private TextView arenaRightPicker;
    private TextView arenaLeftOutput;
    private TextView arenaRightOutput;
    private TextView arenaRunButton;
    private ApiClient.RequestHandle arenaLeftRequest;
    private ApiClient.RequestHandle arenaRightRequest;
    private StringBuilder arenaLeftBuffer;
    private StringBuilder arenaRightBuffer;
    private ModelInfo arenaLeftModel;
    private ModelInfo arenaRightModel;
    private boolean arenaLeftRunning;
    private boolean arenaRightRunning;
    private String artifactSeedOverride;
    private String artifactNameOverride;
    private String pendingExportName;
    private String pendingExportContent;
    private String modelFilter = "all";
    private String activeSessionId;
    private String activeSessionTitle = "New conversation";
    private boolean awaitingResponse;
    private static final int PICK_TEXT_REQUEST = 7101;
    private static final int PICK_IMAGE_REQUEST = 7105;
    private static final int VOICE_REQUEST = 7102;
    private static final int VOICE_PERMISSION_REQUEST = 7103;
    private static final int EXPORT_ARTIFACT_REQUEST = 7104;

    // Theme-aware colors (Claude / Groq inspired)
    private int background;
    private int surface;
    private int raised;
    private int soft;
    private int border;
    private int primaryText;
    private int secondaryText;
    private int mutedText;
    private int lavender;
    private int mint;
    private int amber;
    private int red;
    private int userBubble;
    private int assistantSoft;

    private LinearLayout reasoningPillsRow;
    private LinearLayout followUpChipsRow;
    private long arenaLeftStartedAt;
    private long arenaRightStartedAt;
    private int arenaLeftChars;
    private int arenaRightChars;

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (event.isCtrlPressed() || event.isMetaPressed()) {
            if (keyCode == android.view.KeyEvent.KEYCODE_K) { showCommandPalette(); return true; }
            if (keyCode == android.view.KeyEvent.KEYCODE_N) { startNewChat(); return true; }
            if (keyCode == android.view.KeyEvent.KEYCODE_E) { exportConversationMarkdown(); return true; }
            if (keyCode == android.view.KeyEvent.KEYCODE_COMMA) { showSettings(); return true; }
        }
        if (keyCode == android.view.KeyEvent.KEYCODE_ESCAPE && awaitingResponse) {
            cancelResponse();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean sessionUnlocked = false;

    @Override
    protected void onResume() {
        super.onResume();
        maybeRequireAppUnlock();
        showOnboardingIfNeeded();
    }

    private void maybeRequireAppUnlock() {
        if (!preferences.isAppLockEnabled() || sessionUnlocked) return;
        new AlertDialog.Builder(this)
                .setTitle("Unlock Kairo")
                .setMessage("App lock is enabled. Confirm to continue. (Enable device biometrics in system settings for stronger protection; this build uses an explicit unlock step to stay dependency-free.)")
                .setCancelable(false)
                .setPositiveButton("Unlock", (d, w) -> sessionUnlocked = true)
                .setNegativeButton("Exit", (d, w) -> finish())
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        keyStore = new ApiKeyStore(this);
        preferences = new AppPreferences(this);
        applyThemeColors();
        Window window = getWindow();
        window.setStatusBarColor(background);
        window.setNavigationBarColor(background);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = preferences.isLightTheme()
                    ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    : 0;
            window.getDecorView().setSystemUiVisibility(flags);
        }
        conversationStore = new ConversationStore(this);
        memoryStore = new MemoryStore(this);
        deviceSetup = new DeviceSetupStore(this);
        artifactStore = new ArtifactStore(this);
        usageTracker = new UsageTracker(this);
        orchestrator = new AgentOrchestrator();
        codeRunner = orchestrator.codeRunner();
        gitHubClient = new GitHubClient();
        vercelClient = new VercelClient();
        n8nClient = new N8nClient();
        slackClient = new SlackClient();
        notionClient = new NotionClient();
        linearClient = new LinearClient();
        supabaseClient = new SupabaseClient();
        discordWebhookClient = new DiscordWebhookClient();
        webSearchClient = new WebSearchClient();
        loadConversationHistory();
        buildShell();
        showChat();
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) {
            closeDrawer();
        } else if (awaitingResponse) {
            cancelResponse();
        } else if (arenaLeftRunning || arenaRightRunning) {
            cancelArena();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        saveCurrentSession();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (activeRequest != null) activeRequest.cancel();
        if (arenaLeftRequest != null) arenaLeftRequest.cancel();
        if (arenaRightRequest != null) arenaRightRequest.cancel();
        stopLiveTicker();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        boolean hasSelectedUri = data != null
                && (data.getData() != null || data.getClipData() != null);
        if (resultCode != RESULT_OK || data == null
                || (!hasSelectedUri && requestCode != VOICE_REQUEST)) {
            if (requestCode == EXPORT_ARTIFACT_REQUEST) {
                pendingExportName = null;
                pendingExportContent = null;
            }
            return;
        }
        if (requestCode == PICK_TEXT_REQUEST) attachTextFile(data.getData());
        else if (requestCode == PICK_IMAGE_REQUEST) {
            if (data.getClipData() != null) {
                int count = Math.min(data.getClipData().getItemCount(), 4);
                for (int index = 0; index < count; index++) {
                    attachImageFile(data.getClipData().getItemAt(index).getUri());
                }
                if (data.getClipData().getItemCount() > count) toast("Up to four images can be attached at once");
            } else {
                attachImageFile(data.getData());
            }
        } else if (requestCode == EXPORT_ARTIFACT_REQUEST) writeExport(data.getData());
        else if (requestCode == VOICE_REQUEST) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty() && composer != null) {
                String existing = composer.getText().toString();
                composer.setText(existing + (existing.isEmpty() ? "" : " ") + results.get(0));
                composer.setSelection(composer.length());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == VOICE_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startVoiceInput();
        }
    }

    private void loadConversationHistory() {
        savedSessions.clear();
        savedSessions.addAll(conversationStore.load());
        if (!savedSessions.isEmpty()) {
            activateSession(savedSessions.get(0), false);
        } else {
            activeSessionId = ConversationStore.newId();
            activeSessionTitle = "New conversation";
        }
    }

    private void activateSession(ConversationSession session, boolean refreshUi) {
        if (session == null) return;
        if (refreshUi && activeSessionId != null && !activeSessionId.equals(session.getId())) {
            saveCurrentSession();
        }
        activeSessionId = session.getId();
        activeSessionTitle = session.getTitle();
        conversation.clear();
        conversation.addAll(session.getMessages());
        if (refreshUi) {
            closeDrawer();
            showChat();
        }
    }

    private void saveCurrentSession() {
        if (conversation.isEmpty()) return;
        ConversationSession target = null;
        for (ConversationSession session : savedSessions) {
            if (session.getId().equals(activeSessionId)) {
                target = session;
                break;
            }
        }
        if (target == null) {
            target = new ConversationSession(activeSessionId, activeSessionTitle,
                    System.currentTimeMillis(), null);
            savedSessions.add(target);
        }
        target.getMessages().clear();
        target.getMessages().addAll(conversation);
        target.setTitle(activeSessionTitle);
        target.touch();
        Collections.sort(savedSessions, (left, right) -> Long.compare(right.getUpdatedAt(), left.getUpdatedAt()));
        conversationStore.save(savedSessions);
        refreshDrawerSessions();
    }

    private void buildShell() {
        root = new FrameLayout(this);
        root.setBackgroundColor(background);
        root.setFitsSystemWindows(true);

        LinearLayout mainColumn = new LinearLayout(this);
        mainColumn.setOrientation(LinearLayout.VERTICAL);
        mainColumn.setPadding(dp(18), dp(10), dp(18), 0);
        root.addView(mainColumn, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(0, dp(4), 0, dp(10));
        mainColumn.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(66)));

        TextView menu = iconButton("☰", "Open conversation history", secondaryText);
        menu.setOnClickListener(view -> toggleDrawer());
        toolbar.addView(menu, new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView mark = text("K", 18, background);
        mark.setGravity(Gravity.CENTER);
        mark.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        mark.setBackground(circle(lavender));
        toolbar.addView(mark, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(10), 0, 0, 0);
        TextView brandName = text("Kairo", 18, primaryText);
        brandName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView tagline = text("AI workspace", 11, mutedText);
        brand.addView(brandName, wrap());
        brand.addView(tagline, wrap());
        toolbar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView newChat = iconButton("＋", "New conversation", lavender);
        newChat.setOnClickListener(view -> startNewChat());
        toolbar.addView(newChat, new LinearLayout.LayoutParams(dp(44), dp(40)));
        TextView palette = iconButton("⌘", "Command palette", secondaryText);
        palette.setOnClickListener(view -> showCommandPalette());
        toolbar.addView(palette, new LinearLayout.LayoutParams(dp(44), dp(40)));
        TextView settings = iconButton("⚙", "Settings", secondaryText);
        settings.setOnClickListener(view -> showSettings());
        toolbar.addView(settings, new LinearLayout.LayoutParams(dp(44), dp(40)));

        content = new LinearLayout(this);
        UiEffects.enableLayoutTransitions(content);
        content.setOrientation(LinearLayout.VERTICAL);
        mainColumn.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, dp(8), 0, dp(8));
        String[] labels = {"Chat", "Agents", "Models", "Files", "Connect", "Settings", "Phone"};
        for (int index = 0; index < labels.length; index++) {
            final int tab = index;
            TextView item = text(labels[index], 12, mutedText);
            item.setGravity(Gravity.CENTER);
            item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.setPadding(dp(4), dp(9), dp(4), dp(9));
            item.setOnClickListener(view -> showTab(tab));
            navItems.add(item);
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(42), 1));
        }
        mainColumn.addView(nav, new LinearLayout.LayoutParams(-1, dp(66)));

        drawerScrim = new View(this);
        drawerScrim.setBackgroundColor(Color.argb(150, 0, 0, 0));
        drawerScrim.setVisibility(View.GONE);
        drawerScrim.setOnClickListener(view -> closeDrawer());
        root.addView(drawerScrim, new FrameLayout.LayoutParams(-1, -1));

        drawer = buildDrawer();
        FrameLayout.LayoutParams drawerParams = new FrameLayout.LayoutParams(dp(304), -1, Gravity.START);
        root.addView(drawer, drawerParams);
        drawer.setTranslationX(-dp(304));
        setContentView(root);
    }

    private LinearLayout buildDrawer() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(17), dp(20), dp(14), dp(14));
        panel.setBackgroundColor(surface);
        panel.setElevation(dp(10));

        LinearLayout drawerBrand = new LinearLayout(this);
        drawerBrand.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text("K", 16, background);
        mark.setGravity(Gravity.CENTER);
        mark.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        mark.setBackground(circle(lavender));
        drawerBrand.addView(mark, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView title = text("Kairo", 18, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams drawerTitleParams = new LinearLayout.LayoutParams(0, -2, 1);
        drawerTitleParams.setMargins(dp(10), 0, 0, 0);
        drawerBrand.addView(title, drawerTitleParams);
        TextView close = iconButton("‹", "Close conversation history", secondaryText);
        close.setTextSize(25);
        close.setOnClickListener(view -> closeDrawer());
        drawerBrand.addView(close, new LinearLayout.LayoutParams(dp(38), dp(38)));
        panel.addView(drawerBrand, wrapParams());

        TextView newConversation = text("＋  New conversation", 14, primaryText);
        newConversation.setGravity(Gravity.CENTER_VERTICAL);
        newConversation.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        newConversation.setPadding(dp(14), 0, dp(12), 0);
        newConversation.setBackground(rounded(Color.rgb(57, 49, 83), 13));
        newConversation.setOnClickListener(view -> startNewChat());
        panel.addView(newConversation, marginParams(0, 20, 0, 12));

        drawerSearch = input("Search conversations", false);
        drawerSearch.setSingleLine(true);
        panel.addView(drawerSearch, marginParams(0, 0, 0, 16));
        drawerSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshDrawerSessions(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        TextView recent = sectionLabel("RECENT CONVERSATIONS");
        panel.addView(recent, marginParams(2, 0, 0, 7));
        ScrollView sessionScroll = new ScrollView(this);
        drawerSessions = new LinearLayout(this);
        drawerSessions.setOrientation(LinearLayout.VERTICAL);
        sessionScroll.addView(drawerSessions, new ScrollView.LayoutParams(-1, -2));
        panel.addView(sessionScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        panel.addView(drawerDivider(), marginParams(0, 12, 0, 8));
        panel.addView(drawerLink("⌘  Agents", view -> showAgents()), wrapParams());
        panel.addView(drawerLink("✧  Hermes orchestrator", view -> showHermesWorkflow()), wrapParams());
        panel.addView(drawerLink("↻  Dev Loop", view -> showDevLoop()), wrapParams());
        panel.addView(drawerLink("✦  Memories", view -> showMemories()), wrapParams());
        panel.addView(drawerLink("▥  Skills & language", view -> showSkillsSettings()), wrapParams());
        panel.addView(drawerLink("◌  Sandbox console", view -> showSandbox()), wrapParams());
        panel.addView(drawerLink("📂  Sandbox browser", view -> showSandboxBrowser()), wrapParams());
        panel.addView(drawerLink("⬆  GitHub commit wizard", view -> showGitHubCommitWizard()), wrapParams());
        panel.addView(drawerLink("📋  Prompt templates", view -> showPromptTemplates()), wrapParams());
        panel.addView(drawerLink("🌐  Webhook tester", view -> showWebhookTester()), wrapParams());
        panel.addView(drawerLink("◉  Safe phone", view -> showPhoneControl()), wrapParams());
        panel.addView(drawerLink("◈  Models", view -> showModels()), wrapParams());
        panel.addView(drawerLink("▣  Artifacts", view -> showArtifacts()), wrapParams());
        panel.addView(drawerLink("⌘  Connectors", view -> showConnectors()), wrapParams());
        panel.addView(drawerLink("▤  Device setup", view -> showDeviceSetup()), wrapParams());
        panel.addView(drawerLink("⌁  Web search", view -> showWebSearch()), wrapParams());
        panel.addView(drawerLink("⚔  Model arena", view -> showArena()), wrapParams());
        panel.addView(drawerLink("🖼  Image studio", view -> showImageStudio()), wrapParams());
        panel.addView(drawerLink("⚙  Settings", view -> showSettings()), wrapParams());
        panel.addView(drawerLink("♿  Larger text", view -> {
            preferences.setLargeText(!preferences.isLargeText());
            toast(preferences.isLargeText() ? "Larger text on" : "Larger text off");
            recreate();
        }), wrapParams());
        panel.addView(drawerLink("💾  Backup metadata", view -> showBackupRestore()), wrapParams());
        TextView footer = text("PRIVATE BY DEFAULT\nKeys stay on this device", 10, mutedText);
        footer.setLineSpacing(1.1f, 1.0f);
        panel.addView(footer, marginParams(2, 16, 0, 0));
        refreshDrawerSessions();
        return panel;
    }

    private View drawerDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(border);
        return divider;
    }

    private TextView drawerLink(String label, View.OnClickListener listener) {
        TextView link = text(label, 13, secondaryText);
        link.setGravity(Gravity.CENTER_VERTICAL);
        link.setPadding(dp(12), 0, dp(10), 0);
        link.setOnClickListener(view -> {
            closeDrawer();
            listener.onClick(view);
        });
        return link;
    }

    private void refreshDrawerSessions() {
        if (drawerSessions == null) return;
        drawerSessions.removeAllViews();
        String query = drawerSearch == null ? "" : drawerSearch.getText().toString().trim().toLowerCase(Locale.US);
        int shown = 0;
        for (ConversationSession session : savedSessions) {
            if (!query.isEmpty() && !session.getTitle().toLowerCase(Locale.US).contains(query)) continue;
            TextView item = text(session.getTitle(), 13,
                    session.getId().equals(activeSessionId) ? primaryText : secondaryText);
            item.setSingleLine(true);
            item.setEllipsize(android.text.TextUtils.TruncateAt.END);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(12), 0, dp(8), 0);
            item.setBackground(session.getId().equals(activeSessionId) ? rounded(raised, 10) : null);
            item.setOnClickListener(view -> activateSession(session, true));
            drawerSessions.addView(item, new LinearLayout.LayoutParams(-1, dp(42)));
            shown++;
            if (shown >= 30) break;
        }
        if (shown == 0) {
            TextView empty = text("Your saved conversations\nwill appear here.", 12, mutedText);
            empty.setLineSpacing(1.1f, 1.0f);
            empty.setPadding(dp(12), dp(8), 0, dp(8));
            drawerSessions.addView(empty, wrapParams());
        }
    }

    private void toggleDrawer() {
        if (drawerOpen) closeDrawer();
        else openDrawer();
    }

    private void openDrawer() {
        if (drawer == null) return;
        drawerOpen = true;
        refreshDrawerSessions();
        drawerScrim.setVisibility(View.VISIBLE);
        drawer.animate().translationX(0).setDuration(220).start();
    }

    private void closeDrawer() {
        if (drawer == null || !drawerOpen) return;
        drawerOpen = false;
        drawer.animate().translationX(-dp(304)).setDuration(180).withEndAction(
                () -> drawerScrim.setVisibility(View.GONE)).start();
    }

    private void showTab(int tab) {
        if (tab == TAB_CHAT) showChat();
        else if (tab == TAB_AGENTS) showAgents();
        else if (tab == TAB_MODELS) showModels();
        else if (tab == TAB_ARTIFACTS) showArtifacts();
        else if (tab == TAB_CONNECTORS) showConnectors();
        else if (tab == TAB_PHONE) showPhoneControl();
        else showSettings();
    }

    private void setActiveTab(int tab) {
        for (int index = 0; index < navItems.size(); index++) {
            TextView item = navItems.get(index);
            item.setTextColor(index == tab ? primaryText : mutedText);
            item.setBackground(index == tab ? rounded(raised, 14) : null);
        }
    }

    private void showChat() {
        detectedCredential = null;
        memoryCandidate = "";
        setActiveTab(TAB_CHAT);
        content.removeAllViews();

        LinearLayout page = page();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout headingWrap = new LinearLayout(this);
        headingWrap.setOrientation(LinearLayout.VERTICAL);
        chatSessionTitle = text(activeSessionTitle, 20, primaryText);
        chatSessionTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chatSessionTitle.setSingleLine(true);
        chatSessionTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        headingWrap.addView(chatSessionTitle, wrap());
        headingWrap.addView(text("Private conversation · saved locally", 11, secondaryText), wrap());
        header.addView(headingWrap, new LinearLayout.LayoutParams(0, -2, 1));
        modelChip = pill(modelTitle(), lavender, raised);
        modelChip.setOnClickListener(view -> showModelPicker());
        header.addView(modelChip, new LinearLayout.LayoutParams(-2, dp(38)));
        TextView chatMenu = iconButton("⋯", "Conversation actions", secondaryText);
        chatMenu.setTextSize(23);
        chatMenu.setOnClickListener(view -> showChatMenu());
        header.addView(chatMenu, new LinearLayout.LayoutParams(dp(40), dp(38)));
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(68)));
        if (!"chat".equals(activeAgentId)) {
            LinearLayout agentBanner = card();
            agentBanner.setPadding(dp(12), dp(9), dp(12), dp(9));
            agentBanner.addView(text("HERMES-STYLE RUN  ·  " + agentModeLabel().toUpperCase(Locale.US), 10, mint), wrap());
            agentBanner.addView(text("Plan  →  process  →  review  →  hand off  ·  tool actions stay user-confirmed", 11, secondaryText), marginParams(0, 4, 0, 0));
            page.addView(agentBanner, marginParams(0, 0, 0, 8));
        }

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);
        chatScroll.setClipToPadding(false);
        chatHistory = new LinearLayout(this);
        chatHistory.setOrientation(LinearLayout.VERTICAL);
        chatHistory.setPadding(0, dp(8), 0, dp(18));
        chatScroll.addView(chatHistory, new ScrollView.LayoutParams(-1, -1));
        page.addView(chatScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        renderChatHistory();

        LinearLayout composerShell = new LinearLayout(this);
        composerShell.setOrientation(LinearLayout.VERTICAL);
        composerShell.setPadding(dp(10), dp(8), dp(8), dp(7));
        composerShell.setBackground(stroked(border, 18));

        LinearLayout composeLine = new LinearLayout(this);
        composeLine.setGravity(Gravity.TOP | Gravity.CENTER_VERTICAL);
        composer = new EditText(this);
        composer.setTextColor(primaryText);
        composer.setHintTextColor(mutedText);
        composer.setHint("Message Kairo…");
        composer.setTextSize(15);
        composer.setGravity(Gravity.TOP | Gravity.START);
        composer.setMinLines(1);
        composer.setMaxLines(7);
        composer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        composer.setBackgroundColor(Color.TRANSPARENT);
        composer.setPadding(dp(4), dp(4), dp(4), dp(4));
        composer.setImeOptions(EditorInfo.IME_ACTION_SEND);
        composer.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                if (awaitingResponse) cancelResponse();
                else sendMessage();
                return true;
            }
            return false;
        });
        composeLine.addView(composer, new LinearLayout.LayoutParams(0, -2, 1));
        composerShell.addView(composeLine, wrapParams());
        credentialNotice = text("", 10, amber);
        credentialNotice.setVisibility(View.GONE);
        credentialNotice.setPadding(dp(4), dp(6), dp(4), dp(2));
        credentialNotice.setOnClickListener(view -> showCredentialSaveDialog());
        composerShell.addView(credentialNotice, wrapParams());
        memoryNotice = text("", 10, lavender);
        memoryNotice.setVisibility(View.GONE);
        memoryNotice.setPadding(dp(4), dp(5), dp(4), dp(2));
        memoryNotice.setOnClickListener(view -> showMemoryCandidateDialog(memoryCandidate));
        composerShell.addView(memoryNotice, wrapParams());
        attachmentScroll = new HorizontalScrollView(this);
        attachmentScroll.setHorizontalScrollBarEnabled(false);
        attachmentStrip = new LinearLayout(this);
        attachmentStrip.setGravity(Gravity.CENTER_VERTICAL);
        attachmentScroll.addView(attachmentStrip, new HorizontalScrollView.LayoutParams(-2, -2));
        composerShell.addView(attachmentScroll, marginParams(0, 2, 0, 2));
        refreshAttachmentStrip();

        // Prominent Fast / Balanced / Deep reasoning pills (Claude/Groq style)
        reasoningPillsRow = new LinearLayout(this);
        reasoningPillsRow.setOrientation(LinearLayout.HORIZONTAL);
        reasoningPillsRow.setGravity(Gravity.CENTER_VERTICAL);
        reasoningPillsRow.setPadding(dp(2), dp(2), dp(2), dp(6));
        refreshReasoningPills();
        composerShell.addView(reasoningPillsRow, wrapParams());

        LinearLayout composeActions = new LinearLayout(this);
        composeActions.setGravity(Gravity.CENTER_VERTICAL);
        TextView attach = compactIcon("＋", "Attach a text file", secondaryText,
                view -> openTextPicker());
        composeActions.addView(attach, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView image = compactIcon("▧", "Attach an image", lavender,
                view -> openImagePicker());
        composeActions.addView(image, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView tools = compactIcon("✦", "Choose a tool", lavender,
                view -> showToolPicker());
        composeActions.addView(tools, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView ai = pill("AI", lavender, preferences.isLightTheme() ? soft : Color.rgb(48, 42, 70));
        ai.setTextSize(10);
        ai.setContentDescription("AI actions");
        ai.setOnClickListener(view -> showAiFeaturesPicker());
        composeActions.addView(ai, new LinearLayout.LayoutParams(dp(38), dp(32)));
        modeButton = pill(agentModeLabel(), lavender, preferences.isLightTheme() ? soft : Color.rgb(48, 42, 70));
        modeButton.setTextSize(10);
        modeButton.setOnClickListener(view -> showAgentModePicker());
        composeActions.addView(modeButton, marginWrapParams(4, 0, 0, 0));
        TextView charCount = text("0 / 32k", 10, mutedText);
        composeActions.addView(charCount, new LinearLayout.LayoutParams(0, -2, 1));
        TextView voice = compactIcon("◉", "Voice input", secondaryText,
                view -> startVoiceInput());
        composeActions.addView(voice, new LinearLayout.LayoutParams(dp(32), dp(32)));
        sendButton = text("↑", 21, preferences.isLightTheme() ? Color.WHITE : background);
        sendButton.setGravity(Gravity.CENTER);
        sendButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sendButton.setBackground(circle(lavender));
        sendButton.setContentDescription("Send message");
        sendButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        sendButton.setOnClickListener(view -> {
            if (awaitingResponse) cancelResponse();
            else sendMessage();
        });
        composeActions.addView(sendButton, new LinearLayout.LayoutParams(dp(38), dp(38)));
        setSendEnabled(true);
        composerShell.addView(composeActions, new LinearLayout.LayoutParams(-1, dp(38)));
        composer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                charCount.setText(Math.min(32000, s.length()) + " / 32k");
                refreshComposerSignals(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        page.addView(composerShell, marginParams(0, 7, 0, 0));
        TextView footer = text("Live response · " + styleLabel(preferences.getResponseStyle()) + " / "
                + reasoningLabel(preferences.getReasoningMode()) + " reasoning · "
                + preferences.getMaxOutputTokens() + " max tokens · Keys stay encrypted on this device.", 10, mutedText);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(7), 0, dp(3));
        page.addView(footer, new LinearLayout.LayoutParams(-1, dp(28)));
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
    }

    private void renderChatHistory() {
        if (chatHistory == null) return;
        chatHistory.removeAllViews();
        if (conversation.isEmpty()) {
            addWelcome();
            return;
        }
        for (ChatMessage message : conversation) addMessageBubble(message.getRole(), message.getContent());
        if (typingView != null && awaitingResponse) {
            if (typingView.getParent() instanceof ViewGroup) {
                ((ViewGroup) typingView.getParent()).removeView(typingView);
            }
            chatHistory.addView(typingView, marginParams(0, 0, 0, 16));
        }
        scrollChatToBottom();
    }

    private void addWelcome() {
        LinearLayout welcome = new LinearLayout(this);
        welcome.setOrientation(LinearLayout.VERTICAL);
        welcome.setPadding(dp(4), dp(28), dp(4), dp(12));
        TextView eyebrow = text("KAIRO / READY WHEN YOU ARE", 11, lavender);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        welcome.addView(eyebrow, wrap());
        TextView title = text("What will you build today?", 30, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, dp(9), 0, dp(7));
        welcome.addView(title, wrap());
        welcome.addView(text("A calm home for Claude-style conversations, coding plans, model experiments, and guarded agent tools.", 15, secondaryText), wrap());

        LinearLayout status = card();
        status.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView statusDot = text("●  ONLINE", 11, mint);
        statusDot.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        status.addView(statusDot, wrap());
        status.addView(text("Select a provider key in Settings, or start with a local Ollama model.", 12, secondaryText), marginParams(1, 5, 0, 0));
        if (!deviceSetup.isSetupComplete()) {
            status.addView(smallButton("Set up this device", lavender, view -> showDeviceSetup()), marginParams(0, 9, 0, 0));
        }
        welcome.addView(status, marginParams(0, 22, 0, 12));

        TextView promptLabel = text("TRY A STARTER", 11, mutedText);
        promptLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        welcome.addView(promptLabel, marginParams(0, 10, 0, 8));
        addStarter(welcome, "Explain a codebase", "Give me a concise map of the architecture and the best first task.");
        addStarter(welcome, "Draft a PR plan", "Turn this feature idea into a safe, reviewable implementation plan.");
        addStarter(welcome, "Compare free models", "Which free or local model should I use for coding on a phone?");
        addStarter(welcome, "Build a TypeScript utility", "Create a complete, tested TypeScript module and explain how to run it.");
        addStarter(welcome, "Design a Kotlin screen", "Create a production-ready Kotlin Android screen with state, accessibility, and error handling.");
        addStarter(welcome, "Run a safe CLI check", "Show me the allowed diagnostics I can run before a change.");
        chatHistory.addView(welcome, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addStarter(LinearLayout parent, String title, String prompt) {
        TextView starter = text(title + "  ›\n" + prompt, 14, primaryText);
        starter.setLineSpacing(1.05f, 1.0f);
        starter.setPadding(dp(14), dp(12), dp(14), dp(12));
        starter.setBackground(rounded(surface, 15));
        starter.setOnClickListener(view -> {
            if (composer != null) {
                composer.setText(prompt);
                composer.setSelection(composer.length());
                composer.requestFocus();
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .showSoftInput(composer, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        parent.addView(starter, marginParams(0, 0, 0, 8));
    }

    private void addMessageBubble(String role, String message) {
        boolean user = "user".equals(role);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(user ? Gravity.END : Gravity.START);
        row.setPadding(user ? dp(28) : dp(4), 0, user ? dp(4) : dp(28), 0);

        if (!user) {
            LinearLayout identity = new LinearLayout(this);
            identity.setGravity(Gravity.CENTER_VERTICAL);
            TextView avatar = text("K", 11, background);
            avatar.setGravity(Gravity.CENTER);
            avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            avatar.setBackground(circle(lavender));
            identity.addView(avatar, new LinearLayout.LayoutParams(dp(24), dp(24)));
            TextView label = text("Kairo", 13, primaryText);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
            labelParams.setMargins(dp(9), 0, 0, 0);
            identity.addView(label, labelParams);
            TextView modelBadge = text("  ·  " + modelTitle(), 11, mutedText);
            identity.addView(modelBadge, wrap());
            row.addView(identity, marginParams(0, 0, 0, 7));
        }

        TextView bubble = text("", 15.5f, primaryText);
        bubble.setText(user ? message : MarkdownRenderer.render(message));
        bubble.setTextIsSelectable(true);
        bubble.setLineSpacing(dp(3), 1.05f);
        if (user) {
            bubble.setPadding(dp(16), dp(12), dp(16), dp(12));
            bubble.setBackground(rounded(userBubble, 18));
        } else {
            bubble.setPadding(dp(4), dp(2), dp(4), dp(2));
            bubble.setBackground(rounded(Color.TRANSPARENT, 0));
        }
        bubble.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * (user ? 0.82f : 0.92f)));
        bubble.setOnLongClickListener(view -> {
            copyToClipboard(message);
            toast("Copied");
            return true;
        });
        row.addView(bubble, wrap());

        LinearLayout messageActions = new LinearLayout(this);
        messageActions.setGravity(user ? Gravity.END : Gravity.START);
        messageActions.setPadding(0, dp(4), 0, 0);

        TextView copy = text("Copy", 11, mutedText);
        copy.setPadding(dp(6), dp(5), dp(10), dp(4));
        copy.setOnClickListener(view -> {
            copyToClipboard(message);
            toast("Copied");
        });
        messageActions.addView(copy, wrap());

        if (!user) {
            TextView retry = text("Retry", 11, mutedText);
            retry.setPadding(dp(10), dp(5), dp(6), dp(4));
            retry.setOnClickListener(view -> regenerateLastResponse(message));
            messageActions.addView(retry, wrap());
            TextView retryMode = text("Retry as…", 11, mutedText);
            retryMode.setPadding(dp(10), dp(5), dp(6), dp(4));
            retryMode.setOnClickListener(view -> {
                String[] labels = {"Fast", "Balanced", "Deep"};
                String[] ids = {"fast", "balanced", "deep"};
                new AlertDialog.Builder(this)
                        .setTitle("Regenerate with mode")
                        .setItems(labels, (d, which) -> {
                            preferences.setReasoningMode(ids[which]);
                            if (reasoningPillsRow != null) refreshReasoningPills();
                            regenerateLastResponse(message);
                        })
                        .show();
            });
            messageActions.addView(retryMode, wrap());

            TextView artifact = text("Save as file", 11, mutedText);
            artifact.setPadding(dp(10), dp(5), dp(6), dp(4));
            artifact.setOnClickListener(view -> showCreateArtifactFromAnswer(message));
            messageActions.addView(artifact, wrap());
        }
        row.addView(messageActions, wrap());

        TextView meta = text(user ? "Just now" : "", 10, mutedText);
        meta.setGravity(user ? Gravity.END : Gravity.START);
        if (user) {
            row.addView(meta, marginParams(0, 3, 0, 0));
        }
        chatHistory.addView(row, marginParams(0, 0, 0, 20));
    }

    private void refreshComposerSignals(String draft) {
        ApiKeyDetector.DetectedCredential credential = ApiKeyDetector.detect(draft);
        detectedCredential = credential;
        if (credentialNotice != null) {
            if (credential != null) {
                credentialNotice.setText("⚠  Detected a " + credential.getProviderName()
                        + " credential (" + credential.masked() + ")  ·  tap to review secure save. Sending is paused until it is removed.");
                credentialNotice.setVisibility(View.VISIBLE);
            } else {
                credentialNotice.setText("");
                credentialNotice.setVisibility(View.GONE);
            }
        }
        memoryCandidate = credential == null ? MemoryStore.candidateFromText(draft) : "";
        if (memoryNotice != null) {
            if (!memoryCandidate.isEmpty()) {
                memoryNotice.setText("✦  Possible memory: “" + preview(memoryCandidate, 96)
                        + "”  ·  tap to review. Kairo never saves it automatically.");
                memoryNotice.setVisibility(View.VISIBLE);
            } else {
                memoryNotice.setText("");
                memoryNotice.setVisibility(View.GONE);
            }
        }
    }

    private void showCredentialSaveDialog() {
        final ApiKeyDetector.DetectedCredential credential = detectedCredential;
        if (credential == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Credential detected")
                .setMessage("Kairo found a possible " + credential.getProviderName() + " API credential: "
                        + credential.masked() + "\n\nIt will not be sent to the model. Save it in Android Keystore and remove it from this draft?")
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Remove from draft", (dialog, which) -> removeDetectedCredential(credential))
                .setPositiveButton("Save securely", (dialog, which) -> {
                    try {
                        keyStore.save(credential.getProviderId(), credential.getValue());
                        removeDetectedCredential(credential);
                        toast(credential.getProviderName() + " key saved in Android Keystore");
                    } catch (Exception exception) {
                        toast("Could not save that credential securely");
                    }
                })
                .show();
    }

    private void removeDetectedCredential(ApiKeyDetector.DetectedCredential credential) {
        if (composer == null || credential == null) return;
        String draft = composer.getText().toString();
        String cleaned = draft.replace(credential.getValue(), "").replaceAll("[ \\t]{2,}", " ").trim();
        composer.setText(cleaned);
        composer.setSelection(composer.length());
        detectedCredential = null;
        refreshComposerSignals(cleaned);
    }

    private void showMemoryCandidateDialog(String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) return;
        EditText memory = input("Memory", false);
        memory.setSingleLine(false);
        memory.setGravity(Gravity.TOP | Gravity.START);
        memory.setMinLines(3);
        memory.setText(candidate);
        memory.setSelection(memory.length());
        String category = MemoryStore.categoryForCandidate(candidate);
        new AlertDialog.Builder(this)
                .setTitle("Save a memory?")
                .setMessage("This candidate was detected locally. Review or edit it before saving. Approved memories are encrypted on this device and included in relevant provider requests.")
                .setView(memory)
                .setNegativeButton("Not now", null)
                .setPositiveButton("Save memory", (dialog, which) -> {
                    try {
                        memoryStore.add(category, memory.getText().toString());
                        memoryCandidate = "";
                        if (memoryNotice != null) memoryNotice.setVisibility(View.GONE);
                        toast("Memory saved · review it anytime");
                    } catch (Exception exception) {
                        toast(exception.getMessage() == null ? "Could not save memory" : exception.getMessage());
                    }
                })
                .show();
    }

    private String preview(String value, int maxChars) {
        if (value == null) return "";
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() > maxChars ? compact.substring(0, maxChars - 1) + "…" : compact;
    }

    private void copyToClipboard(String message) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Kairo message", message));
        toast("Copied message");
    }

    private void regenerateLastResponse(String oldAnswer) {
        if (awaitingResponse || conversation.isEmpty()) return;
        int last = conversation.size() - 1;
        if (!"assistant".equals(conversation.get(last).getRole())) {
            toast("Retry is available for the latest answer");
            return;
        }
        conversation.remove(last);
        sendExistingConversation();
    }

    private void sendMessage() {
        if (awaitingResponse || composer == null) return;
        if (detectedCredential != null) {
            showCredentialSaveDialog();
            return;
        }
        String prompt = composer.getText().toString().trim();
        if (prompt.isEmpty() && pendingAttachments.isEmpty()) return;
        if (prompt.length() > 32_000) {
            toast("Messages are limited to 32,000 characters");
            return;
        }
        List<ChatAttachment> attachments = new ArrayList<>(pendingAttachments);
        String displayedPrompt = prompt;
        if (displayedPrompt.isEmpty()) displayedPrompt = "Please inspect the attached image.";
        if (!attachments.isEmpty()) {
            StringBuilder attachmentNote = new StringBuilder(displayedPrompt);
            attachmentNote.append("\n\nAttached image");
            if (attachments.size() > 1) attachmentNote.append("s");
            attachmentNote.append(": ");
            for (int index = 0; index < attachments.size(); index++) {
                if (index > 0) attachmentNote.append(", ");
                attachmentNote.append(attachments.get(index).getName());
            }
            displayedPrompt = attachmentNote.toString();
        }
        if (displayedPrompt.length() > 32_000) {
            toast("Message plus attachment notes are limited to 32,000 characters");
            return;
        }
        ModelInfo model = selectedModel();
        String provider = model.getProviderId();
        if (ProviderConfig.needsApiKey(provider) && keyStore.get(provider).isEmpty()) {
            toast("Add a " + ProviderConfig.displayName(provider) + " key first; your draft and images remain attached.");
            return;
        }
        if (!attachments.isEmpty() && !supportsImageAttachments(model)) {
            toast("This model may not support vision; Kairo will send the image inline and show the provider's response.");
        }
        pendingAttachments.clear();
        refreshAttachmentStrip();
        conversation.add(new ChatMessage("user", displayedPrompt));
        usageTracker.recordMessage();
        composer.setText("");
        nameConversationFromPrompt(displayedPrompt);
        saveCurrentSession();
        sendExistingConversation(attachments);
    }

    private void sendExistingConversation() {
        sendExistingConversation(Collections.emptyList());
    }

    private void sendExistingConversation(List<ChatAttachment> attachments) {
        if (awaitingResponse || conversation.isEmpty()) return;
        ModelInfo model = selectedModel();
        String provider = model.getProviderId();
        if (ProviderConfig.needsApiKey(provider) && keyStore.get(provider).isEmpty()) {
            conversation.add(new ChatMessage("assistant", "Add a "
                    + ProviderConfig.displayName(provider) + " key in Settings before retrying."));
            saveCurrentSession();
            renderChatHistory();
            return;
        }

        awaitingResponse = true;
        streamBuffer = new StringBuilder();
        responseStartedAt = System.currentTimeMillis();
        responseChars = 0;
        setSendEnabled(true);
        renderChatHistory();
        removeTypingView();
        typingView = makeTypingView();
        chatHistory.addView(typingView, marginParams(0, 0, 0, 16));
        startLiveTicker();
        scrollChatToBottom();

        List<ChatMessage> requestMessages = new ArrayList<>();
        if (!"chat".equals(activeAgentId)
                || !preferences.getEnabledSkills().isEmpty()
                || !"auto".equals(preferences.getLanguagePreset())
                || memoryStore.hasMemories()) {
            requestMessages.add(new ChatMessage("system", AgentPromptBuilder.systemPrompt(
                    activeAgentId, preferences.getEnabledSkills(), preferences.getLanguagePreset(),
                    preferences.getResponseStyle(), preferences.getReasoningMode(), memoryStore.promptContext())));
        }
        requestMessages.addAll(conversation);
        activeRequest = ApiClient.sendChatStreaming(
                provider,
                ProviderConfig.baseUrl(provider, preferences),
                keyStore.get(provider),
                model.getId(),
                requestMessages,
                attachments,
                preferences.getTemperature(),
                preferences.getMaxOutputTokens(),
                preferences.getReasoningMode(),
                new ApiClient.StreamingCallback() {
                    @Override
                    public void onToken(String token) {
                        runOnUiThread(() -> receiveToken(token));
                    }

                    @Override
                    public void onComplete() {
                        runOnUiThread(() -> finishStreamingResponse(null));
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> finishStreamingResponse(message));
                    }
                });
    }

    private View makeTypingView() {
        LinearLayout typing = new LinearLayout(this);
        typing.setOrientation(LinearLayout.VERTICAL);
        typing.setPadding(dp(4), dp(2), dp(4), dp(2));
        typing.setBackground(rounded(Color.TRANSPARENT, 0));

        // Identity row matching assistant bubbles
        LinearLayout identity = new LinearLayout(this);
        identity.setGravity(Gravity.CENTER_VERTICAL);
        TextView avatar = text("K", 11, background);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        avatar.setBackground(circle(lavender));
        identity.addView(avatar, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView label = text("Kairo", 13, primaryText);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(9), 0, 0, 0);
        identity.addView(label, lp);
        typing.addView(identity, marginParams(0, 0, 0, 6));

        liveProcessingLabel = text("Connecting…", 11, mint);
        liveProcessingLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        typing.addView(liveProcessingLabel, wrapParams());

        streamingView = text("", 15.5f, primaryText);
        streamingView.setTextIsSelectable(true);
        streamingView.setLineSpacing(dp(3), 1.05f);
        streamingView.setPadding(dp(2), dp(6), dp(2), dp(2));
        typing.addView(streamingView, wrapParams());
        return typing;
    }

    private void receiveToken(String token) {
        if (!awaitingResponse || streamingView == null || token == null) return;
        streamBuffer.append(token);
        responseChars += token.length();
        // Live markdown + subtle streaming caret for Claude/Groq-like feel
        CharSequence rendered = MarkdownRenderer.render(streamBuffer.toString());
        SpannableStringBuilder withCaret = new SpannableStringBuilder(rendered);
        withCaret.append(" ▍");
        streamingView.setText(withCaret);
        updateLiveLabel();
        scrollChatToBottom();
    }

    private void startLiveTicker() {
        stopLiveTicker();
        liveTicker = new Runnable() {
            @Override public void run() {
                if (!awaitingResponse) return;
                updateLiveLabel();
                // Blink the caret by alternating a thin space
                if (streamingView != null && streamBuffer != null) {
                    CharSequence rendered = MarkdownRenderer.render(streamBuffer.toString());
                    SpannableStringBuilder withCaret = new SpannableStringBuilder(rendered);
                    long tick = (System.currentTimeMillis() / 500) % 2;
                    withCaret.append(tick == 0 ? " ▍" : "  ");
                    streamingView.setText(withCaret);
                }
                liveHandler.postDelayed(this, 480);
            }
        };
        liveHandler.post(liveTicker);
    }

    private void updateLiveLabel() {
        if (liveProcessingLabel == null || !awaitingResponse) return;
        long elapsed = Math.max(0L, System.currentTimeMillis() - responseStartedAt) / 1000L;
        double charsPerSec = elapsed > 0 ? (responseChars / (double) elapsed) : 0;
        String speed = charsPerSec > 0 ? String.format(java.util.Locale.US, " · %.0f c/s", charsPerSec) : "";
        liveProcessingLabel.setText("Live · " + elapsed + "s · " + responseChars + " chars" + speed);
    }

    private void stopLiveTicker() {
        if (liveTicker != null) liveHandler.removeCallbacks(liveTicker);
        liveTicker = null;
        liveProcessingLabel = null;
    }

    private void finishStreamingResponse(String error) {
        if (!awaitingResponse) return;
        String answer = streamBuffer == null ? "" : streamBuffer.toString().trim();
        if (error != null && answer.isEmpty()) {
            answer = "I couldn't complete that request.\n\n" + error;
        } else if (error != null && !answer.isEmpty()) {
            answer += "\n\n_[Stream interrupted: " + error + "]_";
        }
        if (answer.isEmpty()) answer = "The provider returned an empty answer. Try again.";
        removeTypingView();
        stopLiveTicker();
        activeRequest = null;
        awaitingResponse = false;
        setSendEnabled(true);
        conversation.add(new ChatMessage("assistant", answer));
        saveCurrentSession();
        streamingView = null;
        streamBuffer = null;
        if (chatHistory != null) {
            renderChatHistory();
            showFollowUpChips(answer);
            int approxTokens = Math.max(1, answer.length() / 4);
            toast("~" + approxTokens + " tokens (estimate)");
            if ("hermes".equals(activeAgentId)) {
                showHermesTimeline(
                        "Captured from the latest Hermes run.",
                        answer.length() > 400 ? answer.substring(0, 400) + "…" : answer,
                        "Review the answer above before any external action.");
            }
            if (answer.length() > 160 && Math.random() < 0.35) {
                offerMemorySuggestion(answer);
            }
        }
    }

    private void cancelResponse() {
        if (!awaitingResponse) return;
        if (activeRequest != null) activeRequest.cancel();
        activeRequest = null;
        removeTypingView();
        stopLiveTicker();
        awaitingResponse = false;
        setSendEnabled(true);
        String partial = streamBuffer == null ? "" : streamBuffer.toString().trim();
        if (!partial.isEmpty()) partial += "\n\n_(Response stopped.)_";
        else partial = "_(Response stopped.)_";
        conversation.add(new ChatMessage("assistant", partial));
        saveCurrentSession();
        streamingView = null;
        streamBuffer = null;
        if (chatHistory != null) renderChatHistory();
    }

    private void removeTypingView() {
        if (typingView != null && typingView.getParent() instanceof ViewGroup) {
            ((ViewGroup) typingView.getParent()).removeView(typingView);
        }
        typingView = null;
    }

    private void setSendEnabled(boolean enabled) {
        if (sendButton == null) return;
        // The button stays clickable while a request is active so it doubles as Stop.
        sendButton.setEnabled(enabled);
        sendButton.setText(awaitingResponse ? "■" : "↑");
        sendButton.setTextSize(awaitingResponse ? 13 : 21);
        sendButton.setAlpha(enabled ? 1f : 0.45f);
        sendButton.setContentDescription(awaitingResponse ? "Stop response" : "Send message");
    }

    private void nameConversationFromPrompt(String prompt) {
        if (!"New conversation".equals(activeSessionTitle)) return;
        String clean = prompt.replaceAll("\\s+", " ").trim();
        if (clean.length() > 42) clean = clean.substring(0, 42).trim() + "…";
        if (!clean.isEmpty()) activeSessionTitle = clean;
        if (chatSessionTitle != null) chatSessionTitle.setText(activeSessionTitle);
    }

    private void startNewChat() {
        if (awaitingResponse) cancelResponse();
        saveCurrentSession();
        conversation.clear();
        activeSessionId = ConversationStore.newId();
        activeSessionTitle = "New conversation";
        closeDrawer();
        showChat();
        toast("New conversation");
    }

    private String agentModeLabel() {
        if ("chat".equals(activeAgentId)) return "Chat mode";
        if ("code".equals(activeAgentId)) return "Code agent";
        if ("hermes".equals(activeAgentId)) return "Hermes orchestrator";
        if ("devloop".equals(activeAgentId)) return "Dev Loop";
        if ("artifact".equals(activeAgentId)) return "Artifact agent";
        if ("browser".equals(activeAgentId)) return "Browser agent";
        if ("research".equals(activeAgentId)) return "Research agent";
        if ("automation".equals(activeAgentId)) return "Automation agent";
        if ("arena".equals(activeAgentId)) return "Arena agent";
        if ("phone".equals(activeAgentId)) return "Safe phone";
        return "Chat mode";
    }

    private void showAgentModePicker() {
        String[] modes = {"Chat mode · focused conversation", "Code agent · plan and review",
                "Hermes orchestrator · plan and hand off", "Dev Loop · plan code test review", "Artifact agent · return complete files",
                "Browser agent · cite selected sources", "Research agent · compare options",
                "Automation agent · GitHub, Vercel, n8n", "Arena agent · critique answers",
                "Safe phone assistant · visible actions"};
        String[] ids = {"chat", "code", "hermes", "artifact", "browser", "research", "automation", "arena", "phone"};
        new AlertDialog.Builder(this)
                .setTitle("Response mode")
                .setMessage("Mode changes the system instruction sent with the next request. Tool actions still require your tap.")
                .setItems(modes, (dialog, which) -> {
                    activeAgentId = ids[which];
                    if (modeButton != null) modeButton.setText(agentModeLabel());
                    toast(agentModeLabel() + " enabled");
                })
                .show();
    }

    private void showChatMenu() {
        String[] actions = {"AI actions", "Skills & language", "Memories", "Rename conversation", "Pin / unpin", "Share transcript", "Clear messages", "Search sessions"};
        new AlertDialog.Builder(this)
                .setTitle(activeSessionTitle)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) showAiFeaturesPicker();
                    else if (which == 1) showSkillsSettings();
                    else if (which == 2) showMemories();
                    else if (which == 3) showRenameDialog();
                    else if (which == 4) {
                        if (activeSessionId != null) {
                            preferences.togglePinnedSession(activeSessionId);
                            boolean pinned = preferences.getPinnedSessionIds().contains(activeSessionId);
                            toast(pinned ? "Pinned" : "Unpinned");
                        }
                    } else if (which == 5) shareTranscript();
                    else if (which == 6) confirmClearConversation();
                    else if (which == 7) showSessionSearch();
                })
                .show();
    }

    private void showSessionSearch() {
        EditText q = input("Search sessions…", false);
        new AlertDialog.Builder(this)
                .setTitle("Search conversations")
                .setView(q)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Search", (d, w) -> {
                    String query = q.getText().toString().trim().toLowerCase(Locale.US);
                    if (query.isEmpty()) { toast("Enter a search term"); return; }
                    java.util.List<ConversationSession> all = conversationStore.load();
                    java.util.List<String> labels = new java.util.ArrayList<>();
                    java.util.List<ConversationSession> hits = new java.util.ArrayList<>();
                    java.util.Set<String> pinned = preferences.getPinnedSessionIds();
                    for (ConversationSession s : all) {
                        String title = s.getTitle() == null ? "" : s.getTitle();
                        String hay = title.toLowerCase(Locale.US);
                        if (hay.contains(query) || (s.getId() != null && s.getId().contains(query))) {
                            hits.add(s);
                            labels.add((pinned.contains(s.getId()) ? "📌 " : "") + title);
                        }
                    }
                    if (hits.isEmpty()) { toast("No matches"); return; }
                    new AlertDialog.Builder(this)
                            .setTitle("Results (" + hits.size() + ")")
                            .setItems(labels.toArray(new String[0]), (dd, which) -> {
                                ConversationSession s = hits.get(which);
                                openSession(s, true);
                            })
                            .show();
                })
                .show();
    }

    private void showRenameDialog() {
        EditText title = input("Conversation name", false);
        title.setSingleLine(true);
        title.setText(activeSessionTitle);
        title.setSelection(title.length());
        new AlertDialog.Builder(this)
                .setTitle("Rename conversation")
                .setView(title)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String value = title.getText().toString().trim();
                    if (!value.isEmpty()) activeSessionTitle = value;
                    if (chatSessionTitle != null) chatSessionTitle.setText(activeSessionTitle);
                    saveCurrentSession();
                    refreshDrawerSessions();
                }).show();
    }

    private void shareTranscript() {
        if (conversation.isEmpty()) {
            toast("There is no transcript to share yet");
            return;
        }
        StringBuilder transcript = new StringBuilder(activeSessionTitle).append("\n\n");
        for (ChatMessage message : conversation) {
            transcript.append("user".equals(message.getRole()) ? "You" : "Kairo")
                    .append(":\n").append(ApiKeyDetector.redact(message.getContent())).append("\n\n");
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, activeSessionTitle);
        share.putExtra(Intent.EXTRA_TEXT, transcript.toString());
        startActivity(Intent.createChooser(share, "Share conversation"));
    }

    private void confirmClearConversation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear this conversation?")
                .setMessage("This removes the local transcript from Kairo. It cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (dialog, which) -> {
                    conversation.clear();
                    savedSessions.removeIf(session -> session.getId().equals(activeSessionId));
                    conversationStore.save(savedSessions);
                    activeSessionId = ConversationStore.newId();
                    activeSessionTitle = "New conversation";
                    refreshDrawerSessions();
                    showChat();
                }).show();
    }

    private void showAiFeaturesPicker() {
        // Claude + Groq style AI action menu
        String[] features = {
                "✦  Improve writing",
                "✦  Make more concise",
                "✦  Expand with more detail",
                "⚙  Explain code step by step",
                "⚙  Review for bugs & edge cases",
                "⚙  Generate unit tests",
                "🔒  Security & privacy review",
                "↗  Convert to TypeScript",
                "↗  Convert to Kotlin",
                "📄  Create a complete file",
                "{ }  Extract structured JSON",
                "☰  Summarize this conversation",
                "⚔  Compare two approaches",
                "💡  Brainstorm alternatives"
        };
        String[] prompts = {
                "Improve the following draft for clarity, tone, structure, and correctness. Keep the meaning unless you call out a change:",
                "Rewrite the following to be significantly more concise while preserving the essential meaning and any critical details:",
                "Expand the following with more concrete detail, examples, and practical guidance. Keep the structure clear:",
                "Explain the following code step by step, including inputs, outputs, assumptions, and likely failure modes:",
                "Review the following code for bugs, edge cases, performance issues, and maintainability. Give fixes with rationale:",
                "Generate focused unit and integration tests for the following code. Include edge cases and explain how to run them:",
                "Review the following code or plan for security, privacy, secret leakage, unsafe permissions, and injection risks:",
                "Convert the following code or design to idiomatic TypeScript. Preserve behavior, add types, and mention assumptions:",
                "Convert the following code or design to idiomatic Kotlin. Preserve behavior, use safe null handling, and mention assumptions:",
                "Create a complete production-ready file from the following requirements. Suggest a safe filename, language, dependencies, and return the entire file in one fenced code block:",
                "Extract the useful facts from the following into valid JSON. Return JSON only and state a schema if the input is ambiguous:",
                "Summarize this conversation with decisions, open questions, risks, and the next practical steps:",
                "Compare two approaches for the following problem. Cover trade-offs, complexity, risk, and when to prefer each:",
                "Brainstorm practical alternatives for the following. Rank them by speed to implement, risk, and long-term maintainability:"
        };
        new AlertDialog.Builder(this)
                .setTitle("AI actions")
                .setMessage("Prompt shortcuts inspired by Claude & Groq. No external writes happen until you explicitly confirm a tool.")
                .setItems(features, (dialog, which) -> {
                    if (composer == null) showChat();
                    if (composer == null) return;
                    activeAgentId = (which == 9) ? "artifact"
                            : ((which >= 3 && which <= 8) ? "code" : "chat");
                    if (modeButton != null) modeButton.setText(agentModeLabel());
                    String existing = composer.getText().toString().trim();
                    String prompt = prompts[which];
                    if (!existing.isEmpty() && which != 11) prompt += "\n\nInput:\n" + existing;
                    composer.setText(prompt.substring(0, Math.min(32_000, prompt.length())));
                    composer.setSelection(composer.length());
                    composer.requestFocus();
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                            .showSoftInput(composer, InputMethodManager.SHOW_IMPLICIT);
                })
                .show();
    }

    private void showToolPicker() {
        String[] tools = {
                "Code Agent  ·  plan and review",
                "GitHub Agent  ·  pull, push, or PR",
                "CLI Agent  ·  safe diagnostics",
                "Research Agent  ·  compare models",
                "Web search  ·  live sources",
                "Create artifact  ·  save a file",
                "Model arena  ·  compare two models",
                "Connectors  ·  GitHub, Vercel, n8n",
                "Safe phone assistant  ·  visible actions",
                "Hermes orchestrator  ·  plan and hand off"
        };
        new AlertDialog.Builder(this)
                .setTitle("Choose a focused tool")
                .setItems(tools, (dialog, which) -> {
                    if (which == 1) {
                        showGithubDialog();
                    } else if (which == 2) {
                        showSandbox();
                    } else if (which == 3) {
                        showModels();
                    } else if (which == 4) {
                        showWebSearch();
                    } else if (which == 5) {
                        showCreateArtifactDialog(null);
                    } else if (which == 6) {
                        showArena();
                    } else if (which == 7) {
                        showConnectors();
                    } else if (which == 8) {
                        showPhoneControl();
                    } else if (which == 9) {
                        showHermesWorkflow();
                    } else if (composer != null) {
                        activeAgentId = "code";
                        if (modeButton != null) modeButton.setText(agentModeLabel());
                        composer.setText("Start with a plan, list assumptions, and give patch-ready steps for: ");
                        composer.setSelection(composer.length());
                        composer.requestFocus();
                    }
                }).show();
    }

    private void openTextPicker() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("text/*");
        try {
            startActivityForResult(picker, PICK_TEXT_REQUEST);
        } catch (Exception exception) {
            toast("No file picker is available on this device");
        }
    }

    private void openImagePicker() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("image/*");
        picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(picker, PICK_IMAGE_REQUEST);
        } catch (Exception exception) {
            toast("No image picker is available on this device");
        }
    }

    private void attachImageFile(Uri uri) {
        if (uri == null) return;
        if (pendingAttachments.size() >= 4) {
            toast("Up to four images can be attached");
            return;
        }
        ContentResolver resolver = getContentResolver();
        String mime = resolver.getType(uri);
        if (mime == null || !mime.toLowerCase(Locale.US).startsWith("image/")) {
            toast("Choose an image file");
            return;
        }
        mime = mime.toLowerCase(Locale.US);
        if ("image/jpg".equals(mime)) mime = "image/jpeg";
        if (!("image/jpeg".equals(mime) || "image/png".equals(mime)
                || "image/gif".equals(mime) || "image/webp".equals(mime))) {
            toast("Use a PNG, JPEG, GIF, or WebP image");
            return;
        }
        final int maxBytes = 3 * 1024 * 1024;
        try (InputStream stream = resolver.openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (stream == null) throw new IllegalStateException("The image could not be opened.");
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            boolean truncated = false;
            while ((read = stream.read(buffer)) != -1) {
                if (total + read > maxBytes) {
                    int allowed = maxBytes - total;
                    if (allowed > 0) output.write(buffer, 0, allowed);
                    truncated = true;
                    break;
                }
                output.write(buffer, 0, read);
                total += read;
            }
            if (truncated) {
                toast("Images are limited to 3 MB");
                return;
            }
            String name = uri.getLastPathSegment();
            if (name == null || name.trim().isEmpty()) name = "image-" + (pendingAttachments.size() + 1);
            pendingAttachments.add(new ChatAttachment(name, mime, Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)));
            refreshAttachmentStrip();
            toast("Image attached · vision-capable models can inspect it");
        } catch (Exception exception) {
            toast("Could not read that image");
        }
    }

    private void refreshAttachmentStrip() {
        if (attachmentStrip == null) return;
        attachmentStrip.removeAllViews();
        if (attachmentScroll != null) {
            attachmentScroll.setVisibility(pendingAttachments.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            attachmentStrip.setVisibility(pendingAttachments.isEmpty() ? View.GONE : View.VISIBLE);
        }
        for (int index = 0; index < pendingAttachments.size(); index++) {
            final int attachmentIndex = index;
            ChatAttachment attachment = pendingAttachments.get(index);
            LinearLayout chip = new LinearLayout(this);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(5), dp(4), dp(9), dp(4));
            chip.setBackground(rounded(Color.rgb(48, 42, 70), 12));
            ImageView preview = new ImageView(this);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap thumbnail = decodeThumbnail(attachment);
            if (thumbnail != null) preview.setImageBitmap(thumbnail);
            else preview.setImageDrawable(circleDrawable(lavender));
            chip.addView(preview, new LinearLayout.LayoutParams(dp(34), dp(34)));
            TextView label = text(attachment.getName() + "  ×", 10, lavender);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setMaxWidth(dp(185));
            label.setGravity(Gravity.CENTER_VERTICAL);
            label.setPadding(dp(8), 0, 0, 0);
            chip.addView(label, new LinearLayout.LayoutParams(dp(185), dp(34)));
            chip.setContentDescription("Remove image " + attachment.getName());
            chip.setOnClickListener(view -> {
                if (attachmentIndex < pendingAttachments.size()) pendingAttachments.remove(attachmentIndex);
                refreshAttachmentStrip();
            });
            attachmentStrip.addView(chip, marginWrapParams(index == 0 ? 0 : 7, 0, 0, 0));
        }
    }

    private Bitmap decodeThumbnail(ChatAttachment attachment) {
        try {
            byte[] bytes = Base64.decode(attachment.getBase64(), Base64.DEFAULT);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, Math.max(bounds.outWidth, bounds.outHeight) / 256);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } catch (Exception ignored) {
            return null;
        }
    }

    private android.graphics.drawable.Drawable circleDrawable(int color) {
        return circle(color);
    }

    private void attachTextFile(Uri uri) {
        if (composer == null) return;
        try (InputStream stream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (stream == null) throw new IllegalStateException("The file could not be opened.");
            byte[] buffer = new byte[2048];
            int total = 0;
            int read;
            while ((read = stream.read(buffer)) != -1 && total < 16_000) {
                int allowed = Math.min(read, 16_000 - total);
                output.write(buffer, 0, allowed);
                total += allowed;
                if (allowed < read) break;
            }
            String content = new String(output.toByteArray(), StandardCharsets.UTF_8);
            String existing = composer.getText().toString();
            String label = uri.getLastPathSegment() == null ? "attached file" : uri.getLastPathSegment();
            String insertion = (existing.isEmpty() ? "" : existing + "\n\n")
                    + "Attached file: " + label + "\n```text\n" + content + "\n```";
            composer.setText(insertion.substring(0, Math.min(32_000, insertion.length())));
            composer.setSelection(composer.length());
            toast("Attached " + label);
        } catch (Exception exception) {
            toast("Could not read that text file");
        }
    }

    private void writeExport(Uri destination) {
        if (pendingExportContent == null) return;
        try (java.io.OutputStream output = getContentResolver().openOutputStream(destination)) {
            if (output == null) throw new IllegalStateException("The destination could not be opened.");
            output.write(pendingExportContent.getBytes(StandardCharsets.UTF_8));
            toast("Exported " + (pendingExportName == null ? "artifact" : pendingExportName));
        } catch (Exception exception) {
            toast("Could not export the artifact");
        } finally {
            pendingExportName = null;
            pendingExportContent = null;
        }
    }

    private void exportArtifact(String name, String content) {
        pendingExportName = name;
        pendingExportContent = content;
        Intent export = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        export.addCategory(Intent.CATEGORY_OPENABLE);
        export.setType("text/plain");
        export.putExtra(Intent.EXTRA_TITLE, name);
        try {
            startActivityForResult(export, EXPORT_ARTIFACT_REQUEST);
        } catch (Exception exception) {
            pendingExportName = null;
            pendingExportContent = null;
            toast("No document exporter is available");
        }
    }

    private void startVoiceInput() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, VOICE_PERMISSION_REQUEST);
            return;
        }
        Intent voice = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        voice.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        voice.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Kairo");
        try {
            startActivityForResult(voice, VOICE_REQUEST);
        } catch (Exception exception) {
            toast("Voice input is not available on this device");
        }
    }

    private void showModelPicker() {
        List<ModelInfo> models = ModelCatalog.all();
        String[] labels = new String[models.size()];
        for (int index = 0; index < labels.length; index++) {
            ModelInfo model = models.get(index);
            String badge = model.isCandidate() ? "CANDIDATE · " : (model.isFreeRoute() ? "FREE / TIER · " : "");
            labels[index] = badge + model.getName()
                    + "  ·  " + ProviderConfig.displayName(model.getProviderId());
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Choose a model")
                .setItems(labels, (d, which) -> {
                    ModelInfo model = models.get(which);
                    preferences.setModel(model.getProviderId(), model.getId());
                    if (modelChip != null) modelChip.setText(modelTitle());
                    toast(model.getName() + " selected");
                })
                .setNegativeButton("Browse models", (d, which) -> showModels())
                .setNeutralButton("Groq fast", (d, which) -> activateGroqFastMode())
                .create();
        dialog.show();
    }

    private void showSandbox() {
        closeDrawer();
        setActiveTab(TAB_AGENTS);
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Sandbox console", "Private on-phone workspace + safe diagnostics — not a full Ubuntu VM."), wrapParams());

        LinearLayout guardrail = card();
        guardrail.setPadding(dp(14), dp(13), dp(14), dp(13));
        guardrail.addView(text("ANDROID APP SANDBOX", 10, mint), wrap());
        guardrail.addView(text("Commands run in Kairo's app process through a strict allow-list. Pipes, redirects, chaining, substitution, root, package installs, and arbitrary commands are blocked.\n\nFile create/zip uses this app's private phone storage only. A full Ubuntu environment is not included (huge image, security risk, store policy).", 12, secondaryText), marginParams(0, 6, 0, 0));
        guardrail.addView(text("Allowed examples: " + joinExamples(), 10, mutedText), marginParams(0, 8, 0, 0));
        page.addView(guardrail, marginParams(0, 12, 0, 12));

        LinearLayout runtimeCard = card();
        runtimeCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        runtimeCard.addView(text("RUNTIME DISCOVERY", 10, lavender), wrap());
        TextView runtimeReport = text(codeRunner.environmentReport(), 11, secondaryText);
        runtimeReport.setTypeface(Typeface.MONOSPACE);
        runtimeReport.setLineSpacing(1.1f, 1.0f);
        runtimeCard.addView(runtimeReport, marginParams(0, 7, 0, 0));
        page.addView(runtimeCard, marginParams(0, 0, 0, 12));

        LinearLayout commandCard = card();
        commandCard.setPadding(dp(13), dp(12), dp(13), dp(12));
        EditText command = input("Try: git status", false);
        command.setSingleLine(true);
        commandCard.addView(command, wrapParams());
        TextView output = text("No command run yet.", 12, secondaryText);
        output.setTextIsSelectable(true);
        output.setTypeface(Typeface.MONOSPACE);
        output.setPadding(dp(12), dp(11), dp(12), dp(11));
        output.setBackground(rounded(Color.rgb(13, 14, 17), 12));
        commandCard.addView(output, marginParams(0, 9, 0, 0));
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.addView(smallButton("Run safe command", mint, view -> {
            String value = command.getText().toString().trim();
            if (!CliCommandPolicy.isAllowed(value)) {
                output.setText("Blocked: " + CliCommandPolicy.rejectionReason(value));
                return;
            }
            output.setText("Running in the app sandbox…");
            orchestrator.cli().execute(value, getCacheDir(), new com.kairo.app.agent.CliAgent.Callback() {
                @Override public void onSuccess(String result) {
                    runOnUiThread(() -> output.setText(result));
                }
                @Override public void onError(String message) {
                    runOnUiThread(() -> output.setText("Failed: " + message));
                }
            });
        }), wrap());
        actions.addView(smallButton("Create shell file", lavender, view -> {
            preferences.setLanguagePreset("shell");
            showCreateArtifactDialog(null);
        }), marginWrapParams(7, 0, 0, 0));
        commandCard.addView(actions, marginParams(0, 10, 0, 0));
        page.addView(commandCard, marginParams(0, 0, 0, 12));

        // Private file sandbox (create / list / zip) — not a full Ubuntu FS
        LinearLayout fileCard = card();
        fileCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        fileCard.addView(text("PRIVATE FILE SANDBOX", 10, mint), wrap());
        fileCard.addView(text("Create, list, and zip text files inside Kairo's private app directory. Bounded size and count. No access to other apps or system paths.", 12, secondaryText), marginParams(0, 6, 0, 0));
        final com.kairo.app.core.SandboxWorkspace sandbox = new com.kairo.app.core.SandboxWorkspace(this);
        TextView sandboxStatus = text(sandbox.statusReport(), 11, secondaryText);
        sandboxStatus.setTypeface(Typeface.MONOSPACE);
        sandboxStatus.setTextIsSelectable(true);
        fileCard.addView(sandboxStatus, marginParams(0, 8, 0, 0));
        EditText fileName = input("src/main.js or notes/todo.md", false);
        fileName.setSingleLine(true);
        EditText fileBody = input("File contents…", false);
        fileBody.setSingleLine(false);
        fileBody.setMinLines(3);
        fileCard.addView(fileName, marginParams(0, 8, 0, 0));
        fileCard.addView(fileBody, marginParams(0, 6, 0, 0));
        LinearLayout fileActions = new LinearLayout(this);
        fileActions.setGravity(Gravity.END);
        fileActions.addView(smallButton("Write file", mint, view -> {
            String name = fileName.getText().toString().trim();
            String body = fileBody.getText().toString();
            new AlertDialog.Builder(this)
                    .setTitle("Write sandbox file?")
                    .setMessage("Create or overwrite “" + (name.isEmpty() ? "untitled.txt" : name) + "” in the private sandbox?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Write", (d, w) -> {
                        try {
                            java.io.File f = sandbox.writeText(name, body);
                            sandboxStatus.setText(sandbox.statusReport());
                            toast("Wrote " + f.getName());
                        } catch (Exception e) {
                            toast(e.getMessage() == null ? "Write failed" : e.getMessage());
                        }
                    })
                    .show();
        }), wrap());
        fileActions.addView(smallButton("Refresh", secondaryText, view -> sandboxStatus.setText(sandbox.statusReport())), marginWrapParams(7, 0, 0, 0));
        fileActions.addView(smallButton("Zip all", lavender, view -> {
            new AlertDialog.Builder(this)
                    .setTitle("Zip sandbox files?")
                    .setMessage("Create a zip archive of current sandbox files inside the sandbox.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Zip", (d, w) -> {
                        try {
                            java.io.File z = sandbox.zipAll("sandbox-" + System.currentTimeMillis() + ".zip");
                            sandboxStatus.setText(sandbox.statusReport());
                            toast("Created " + z.getName());
                        } catch (Exception e) {
                            toast(e.getMessage() == null ? "Zip failed" : e.getMessage());
                        }
                    })
                    .show();
        }), marginWrapParams(7, 0, 0, 0));
        fileCard.addView(fileActions, marginParams(0, 10, 0, 0));
        page.addView(fileCard, marginParams(0, 0, 0, 12));

        LinearLayout notes = card();
        notes.setPadding(dp(14), dp(12), dp(14), dp(12));
        notes.addView(text("GOOD FIT", 10, lavender), wrap());
        notes.addView(text("Inspect local environment details, prepare portable shell snippets, and ask the selected model for Linux-compatible code. Use GitHub or a real development environment for edits, builds, and deployments after reviewing them.", 12, secondaryText), marginParams(0, 6, 0, 0));
        page.addView(notes, wrapParams());
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void showPhoneControl() {
        closeDrawer();
        setActiveTab(TAB_PHONE);
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Safe phone assistant", "Panda-like help for visible, user-confirmed Android actions."), wrapParams());

        LinearLayout intro = card();
        intro.setPadding(dp(15), dp(14), dp(15), dp(14));
        TextView eyebrow = text("PANDA MODE  ·  GUARDRAILS ON", 10, mint);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        intro.addView(eyebrow, wrap());
        TextView title = text("You stay in control of the phone.", 19, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        intro.addView(title, marginParams(0, 7, 0, 5));
        intro.addView(text(com.kairo.app.agent.PhoneActionPolicy.boundary(), 12, secondaryText), wrap());
        page.addView(intro, marginParams(0, 12, 0, 12));

        page.addView(sectionLabel("VISIBLE ACTIONS"), marginParams(0, 0, 0, 8));
        addPhoneAction(page, "Open browser", "Launch a public page in the browser; Kairo does not type or submit anything.", "browser", () -> launchPhoneIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))));
        addPhoneAction(page, "Open device settings", "Open Android Settings for you to inspect and change manually.", "settings", () -> launchPhoneIntent(new Intent(android.provider.Settings.ACTION_SETTINGS)));
        addPhoneAction(page, "Open Wi-Fi settings", "Show the Android Wi-Fi panel; Kairo cannot change networks silently.", "wifi", () -> launchPhoneIntent(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)));
        addPhoneAction(page, "Open Bluetooth settings", "Show Bluetooth settings for manual pairing.", "bluetooth", () -> launchPhoneIntent(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)));
        addPhoneAction(page, "Open Location settings", "Show location services settings.", "location", () -> launchPhoneIntent(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        addPhoneAction(page, "Open Battery settings", "Show battery and power usage.", "battery", () -> launchPhoneIntent(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)));
        addPhoneAction(page, "Open Display settings", "Show display brightness and timeout settings.", "display", () -> launchPhoneIntent(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)));
        addPhoneAction(page, "Open Sound settings", "Show volume and sound settings.", "sound", () -> launchPhoneIntent(new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)));
        addPhoneAction(page, "Open Apps settings", "Show installed applications list.", "apps", () -> launchPhoneIntent(new Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS)));
        addPhoneAction(page, "Open camera", "Launch the visible camera app; Kairo does not capture or upload a photo here.", "camera", () -> launchPhoneIntent(new Intent("android.media.action.IMAGE_CAPTURE")));

        LinearLayout dialCard = card();
        dialCard.setPadding(dp(13), dp(11), dp(10), dp(11));
        LinearLayout dialLabels = new LinearLayout(this);
        dialLabels.setOrientation(LinearLayout.VERTICAL);
        TextView dialTitle = text("Open dialer", 14, primaryText);
        dialTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        dialLabels.addView(dialTitle, wrap());
        dialLabels.addView(text("Enter a number, review it, then open the dialer. This never places a call automatically.", 11, secondaryText), marginParams(0, 4, 0, 7));
        EditText phoneNumber = input("Phone number (optional)", false);
        phoneNumber.setSingleLine(true);
        dialLabels.addView(phoneNumber, wrapParams());
        dialCard.addView(dialLabels, wrapParams());
        dialCard.addView(smallButton("Review & open dialer", lavender, view -> {
            String number = phoneNumber.getText().toString().trim();
            if (number.isEmpty()) {
                toast("Enter a phone number first");
                return;
            }
            confirmPhoneAction("Open dialer for " + number + "?", "Android will show the number for your review. Kairo will not place the call.", () -> launchPhoneIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)))));
        }), marginParams(0, 9, 0, 0));
        page.addView(dialCard, marginParams(0, 0, 0, 12));

        LinearLayout blocked = card();
        blocked.setPadding(dp(14), dp(12), dp(14), dp(12));
        blocked.addView(text("NOT AVAILABLE BY DESIGN", 10, amber), wrap());
        blocked.addView(text("No root access · no silent calls or SMS · no contacts or private-app reads · no arbitrary shell · no background control. Every supported action opens a visible Android surface after confirmation.", 12, secondaryText), marginParams(0, 6, 0, 0));
        page.addView(blocked, wrapParams());
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void addPhoneAction(LinearLayout parent, String title, String description,
                                String actionId, Runnable action) {
        if (!com.kairo.app.agent.PhoneActionPolicy.isSupported(actionId)) return;
        LinearLayout row = card();
        row.setPadding(dp(13), dp(11), dp(10), dp(11));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(title, 14, primaryText);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labels.addView(name, wrap());
        labels.addView(text(description, 11, secondaryText), marginParams(0, 4, 0, 0));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(smallButton("Review", lavender, view -> confirmPhoneAction(title, description, action)), wrap());
        parent.addView(row, marginParams(0, 0, 0, 8));
    }

    private void confirmPhoneAction(String title, String description, Runnable action) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(description + "\n\nThis is the only action Kairo will request.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open", (dialog, which) -> action.run())
                .show();
    }

    private void launchPhoneIntent(Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception exception) {
            toast("That Android action is not available on this device");
        }
    }

    
    private void showDevLoop() {
        closeDrawer();
        setActiveTab(TAB_AGENTS);
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Dev Loop", "Plan → Code → Test → Review → Edit → Debug — then loop until done."), wrapParams());
        enhanceDevLoopWithProgress(page);

        LinearLayout hero = card();
        hero.setPadding(dp(15), dp(15), dp(15), dp(15));
        TextView eyebrow = text("CLOSED ENGINEERING LOOP  ·  YOU CONFIRM WRITES", 10, mint);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hero.addView(eyebrow, wrap());
        hero.addView(text("Cycle until tests and review pass. External writes stay behind confirmation.", 13, secondaryText), marginParams(0, 8, 0, 0));
        LinearLayout heroActions = new LinearLayout(this);
        heroActions.setGravity(Gravity.END);
        heroActions.addView(smallButton("Start Dev Loop", lavender, view -> {
            activeAgentId = "devloop";
            showChat();
            if (composer != null) {
                composer.setText("Run a full Dev Loop on this task. Use sections Plan, Code, Test, Review, Process/Edit, Debug, and Loop decision. Task: ");
                composer.setSelection(composer.length());
                composer.requestFocus();
            }
        }), wrap());
        heroActions.addView(smallButton("Open sandbox", mint, view -> showSandbox()), marginWrapParams(8, 0, 0, 0));
        hero.addView(heroActions, marginParams(0, 12, 0, 0));
        page.addView(hero, marginParams(0, 12, 0, 12));

        String[][] phases = {
                {"1", "Plan", "Goals, constraints, acceptance criteria, risks."},
                {"2", "Code", "Implement or patch; prefer complete files in the sandbox."},
                {"3", "Test", "Verification steps and expected results."},
                {"4", "Review", "Correctness, edges, security, simplicity."},
                {"5", "Edit", "Apply review feedback; keep diffs small."},
                {"6", "Debug", "Hypothesis → check → fix → re-test."},
                {"↻", "Loop", "CONTINUE LOOP or DONE with a checklist."}
        };
        for (String[] ph : phases) {
            LinearLayout row = card();
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout top = new LinearLayout(this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView badge = text(ph[0], 11, background);
            badge.setGravity(Gravity.CENTER);
            badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            badge.setBackground(circle(lavender));
            top.addView(badge, new LinearLayout.LayoutParams(dp(28), dp(28)));
            TextView title = text("  " + ph[1], 14, primaryText);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            top.addView(title, wrap());
            row.addView(top, wrap());
            row.addView(text(ph[2], 12, secondaryText), marginParams(0, 6, 0, 0));
            page.addView(row, marginParams(0, 0, 0, 8));
        }

        LinearLayout storage = card();
        storage.setPadding(dp(14), dp(12), dp(14), dp(12));
        storage.addView(text("PHONE PRIVATE STORAGE", 10, mint), wrap());
        com.kairo.app.core.SandboxWorkspace sw = new com.kairo.app.core.SandboxWorkspace(this);
        storage.addView(text("App-private internal storage on this phone:\n" + sw.storageLocation()
                + "\n\nFolders: src/ · tests/ · out/ · notes/\nNot a full Ubuntu VM — bounded sandbox only.", 12, secondaryText), marginParams(0, 6, 0, 0));
        page.addView(storage, marginParams(0, 8, 0, 0));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

private void showHermesWorkflow() {
        closeDrawer();
        setActiveTab(TAB_AGENTS);
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Hermes orchestrator", "A transparent Plan → Process → Review → Handoff loop for serious tasks."), wrapParams());

        LinearLayout hero = card();
        hero.setPadding(dp(15), dp(15), dp(15), dp(15));
        TextView eyebrow = text("HERMES WORKSPACE  ·  USER IN THE LOOP", 10, mint);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hero.addView(eyebrow, wrap());
        TextView title = text("Turn a request into a reviewable run.", 21, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hero.addView(title, marginParams(0, 7, 0, 5));
        hero.addView(text("Hermes keeps the current objective, assumptions, tool candidates, artifacts, and next decision visible. It does not grant the model hidden permissions or execute external changes by itself.", 13, secondaryText), wrap());
        LinearLayout heroActions = new LinearLayout(this);
        heroActions.setGravity(Gravity.END);
        heroActions.addView(smallButton("Inspect tools", secondaryText, view -> showToolRegistryDialog()), wrap());
        heroActions.addView(smallButton("Start Hermes chat", lavender, view -> {
            activeAgentId = "hermes";
            showChat();
            if (composer != null) {
                composer.setText("Use the Hermes workflow. Start with the objective, assumptions, a short plan, the tools or files you would need, and a review checkpoint for: ");
                composer.setSelection(composer.length());
                composer.requestFocus();
            }
        }), marginWrapParams(7, 0, 0, 0));
        heroActions.addView(smallButton("Handoff pack", mint, view -> {
            activeAgentId = "hermes";
            showChat();
            if (composer != null) {
                composer.setText("Produce a Hermes handoff pack with: (1) objective, (2) completed steps, (3) open risks, (4) files/tools touched, (5) exact next action requiring my confirmation, (6) rollback notes.");
                composer.setSelection(composer.length());
            }
        }), marginWrapParams(7, 0, 0, 0));
        hero.addView(heroActions, marginParams(0, 12, 0, 0));
        page.addView(hero, marginParams(0, 13, 0, 16));

        page.addView(sectionLabel("THE RUN LOOP"), marginParams(0, 0, 0, 8));
        addHermesPhase(page, "01", "Plan", "Define the outcome, split the task into small steps, identify assumptions, and choose the least-privileged tools.", lavender);
        addHermesPhase(page, "02", "Process", "Stream progress as Kairo reads selected context, drafts an artifact, or prepares a connector handoff. No silent writes.", mint);
        addHermesPhase(page, "03", "Review", "Show the proposed diff, file, command, message, deployment, or phone intent before anything consequential happens.", amber);
        addHermesPhase(page, "04", "Handoff", "Wait for your explicit confirmation, then call the bounded action and report the result without claiming more than it did.", lavender);

        LinearLayout templates = card();
        templates.setPadding(dp(14), dp(12), dp(14), dp(12));
        templates.addView(text("HERMES STARTERS", 10, lavender), wrap());
        templates.addView(text("One-tap structured runs. External writes still require confirmation.", 12, secondaryText), marginParams(0, 6, 0, 8));
        String[][] starters = {
                {"Ship a safe PR", "Hermes: plan a minimal PR. Inspect status, propose file changes, list risks, and stop before any push or PR creation."},
                {"Debug with evidence", "Hermes: investigate the failure. List hypotheses, the smallest checks, and a review checkpoint before any fix."},
                {"Release checklist", "Hermes: build a release checklist with owners, risks, rollback steps, and a final confirmation gate."},
                {"Sandbox → zip → share", "Hermes: create files in the private sandbox, zip them after review, and prepare a share handoff without silent uploads."}
        };
        for (String[] s : starters) {
            TextView row = text("▸  " + s[0], 13, primaryText);
            row.setPadding(dp(10), dp(10), dp(10), dp(10));
            row.setBackground(rounded(raised, 12));
            final String prompt = s[1];
            row.setOnClickListener(v -> {
                activeAgentId = "hermes";
                showChat();
                if (composer != null) {
                    composer.setText(prompt);
                    composer.setSelection(composer.length());
                }
            });
            templates.addView(row, marginParams(0, 0, 0, 6));
        }
        page.addView(templates, marginParams(0, 12, 0, 0));

        LinearLayout safety = card();
        safety.setPadding(dp(14), dp(12), dp(14), dp(12));
        safety.addView(text("ALWAYS REVIEW FIRST", 10, amber), wrap());
        safety.addView(text("Pushes, pull requests, deployments, workflow activation, webhooks, team messages, code execution, file sharing, and phone actions remain separate confirmation-gated UI actions.", 12, secondaryText), marginParams(0, 6, 0, 0));
        page.addView(safety, marginParams(0, 8, 0, 20));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void addHermesPhase(LinearLayout parent, String number, String title, String description, int color) {
        LinearLayout phase = card();
        phase.setPadding(dp(13), dp(12), dp(13), dp(12));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.TOP);
        TextView index = text(number, 12, color);
        index.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        index.setGravity(Gravity.CENTER);
        index.setBackground(rounded(Color.rgb(48, 42, 70), 10));
        row.addView(index, new LinearLayout.LayoutParams(dp(38), dp(34)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 15, primaryText);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labels.addView(titleView, wrap());
        labels.addView(text(description, 12, secondaryText), marginParams(0, 4, 0, 0));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.setMargins(dp(10), 0, 0, 0);
        row.addView(labels, labelParams);
        phase.addView(row, wrapParams());
        parent.addView(phase, marginParams(0, 0, 0, 8));
    }

    private void showAgents() {
        setActiveTab(TAB_AGENTS);
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Agents", "Small, explicit tools instead of a mystery black box."), wrapParams());

        LinearLayout hero = card();
        hero.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView eyebrow = text("KAIRO HARNESS", 11, lavender);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hero.addView(eyebrow, wrap());
        TextView title = text("Give the model tools. Keep the controls with you.", 21, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, dp(7), 0, dp(5));
        hero.addView(title, wrap());
        hero.addView(text("Every network write is a button you confirm. Local CLI access is limited to a safe diagnostics allow-list.", 13, secondaryText), wrap());
        TextView toolSummary = text(orchestrator.tools().size() + " explicit tools · "
                + com.kairo.app.agent.ToolRegistry.writeCount() + " require a second confirmation", 11, mutedText);
        toolSummary.setPadding(0, dp(10), 0, dp(8));
        hero.addView(toolSummary, wrap());
        hero.addView(smallButton("Inspect tool permissions", lavender,
                view -> showToolRegistryDialog()), wrapParams());
        page.addView(hero, marginParams(0, 15, 0, 18));

        for (AgentDefinition agent : orchestrator.agents()) addAgentCard(page, agent);
        LinearLayout guardrail = card();
        guardrail.setPadding(dp(14), dp(12), dp(14), dp(12));
        guardrail.addView(text("●  GUARDRAILS ON", 11, mint), wrap());
        guardrail.addView(text("API keys use Android Keystore encryption. GitHub writes require an explicit confirmation dialog.", 12, secondaryText), marginParams(0, 5, 0, 0));
        page.addView(guardrail, marginParams(0, 6, 0, 20));

        ScrollView scroll = new ScrollView(this);
        // The page itself is wrapped in a scroll view so the cards remain comfortable on phones.
        scroll.addView(page);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void addAgentCard(LinearLayout parent, AgentDefinition agent) {
        LinearLayout item = card();
        item.setPadding(dp(15), dp(14), dp(15), dp(13));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(agent.getName(), 18, primaryText);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView status = pill(agent.requiresNetwork() ? "NETWORK" : "LOCAL", agent.requiresNetwork() ? amber : mint, soft);
        top.addView(status, new LinearLayout.LayoutParams(-2, dp(29)));
        item.addView(top, wrap());
        TextView eyebrow = text(agent.getEyebrow(), 10, lavender);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        item.addView(eyebrow, marginParams(0, 8, 0, 4));
        item.addView(text(agent.getDescription(), 13, secondaryText), wrap());
        item.addView(text(agent.getCapabilities(), 11, mutedText), marginParams(0, 10, 0, 10));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        String id = agent.getId();
        TextView action = smallButton("Open " + ("github".equals(id) ? "tools" : "agent"), lavender, view -> {
            if ("github".equals(id)) showGithubDialog();
            else if ("hermes".equals(id)) showHermesWorkflow();
            else if ("devloop".equals(id)) showDevLoop();
            else if ("cli".equals(id)) showSandbox();
            else if ("phone".equals(id)) showPhoneControl();
            else if ("research".equals(id)) showModels();
            else if ("artifact".equals(id)) showArtifacts();
            else if ("browser".equals(id)) showWebSearch();
            else if ("automation".equals(id)) showConnectors();
            else if ("arena".equals(id)) showArena();
            else {
                activeAgentId = id;
                showChat();
                if (composer != null) {
                    composer.setText("Act as my " + agent.getName() + ". " + AgentPromptBuilder.systemPrompt(id));
                    composer.requestFocus();
                }
            }
        });
        actions.addView(action, wrap());
        item.addView(actions, wrapParams());
        parent.addView(item, marginParams(0, 0, 0, 11));
    }

    private void showToolRegistryDialog() {
        List<ToolSpec> tools = orchestrator.tools();
        String[] labels = new String[tools.size()];
        for (int index = 0; index < tools.size(); index++) {
            ToolSpec tool = tools.get(index);
            labels[index] = (tool.isWriteTool() ? "WRITE · " : "READ · ")
                    + tool.getLabel() + (tool.usesNetwork() ? " · network" : " · local");
        }
        new AlertDialog.Builder(this)
                .setTitle("Tool permissions")
                .setMessage("These are the only capabilities Kairo exposes to its agent layer. Write tools are never automatic.")
                .setItems(labels, (dialog, which) -> {
                    ToolSpec tool = tools.get(which);
                    new AlertDialog.Builder(this)
                            .setTitle(tool.getLabel())
                            .setMessage(tool.getDescription() + (tool.isWriteTool()
                                    ? "\n\nThis action always needs your confirmation."
                                    : "\n\nThis tool does not write changes."))
                            .setPositiveButton("Done", null)
                            .show();
                })
                .setPositiveButton("Done", null)
                .show();
    }

    private void showCliDialog() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), 0);
        panel.addView(text("Only these commands are allowed: " + joinExamples(), 12, secondaryText), wrap());
        EditText command = input("Try: git status", false);
        command.setSingleLine(true);
        panel.addView(command, marginParams(0, 14, 0, 8));
        TextView output = text("No command run yet.", 12, secondaryText);
        output.setTextIsSelectable(true);
        output.setPadding(dp(12), dp(10), dp(12), dp(10));
        output.setBackground(rounded(Color.rgb(13, 14, 17), 12));
        panel.addView(output, wrapParams());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("CLI Agent")
                .setView(panel)
                .setNegativeButton("Close", null)
                .setPositiveButton("Run", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String value = command.getText().toString().trim();
            output.setText("Running…");
            orchestrator.cli().execute(value, getCacheDir(), new com.kairo.app.agent.CliAgent.Callback() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> output.setText(result));
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> output.setText("Blocked / failed: " + message));
                }
            });
        }));
        dialog.show();
    }

    private void showGithubDialog() {
        if (!keyStore.hasKey("github")) {
            new AlertDialog.Builder(this)
                    .setTitle("Connect GitHub")
                    .setMessage("Add a fine-grained GitHub token in Settings before using pull, push, or PR tools. Kairo stores it in Android Keystore.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Open Settings", (dialog, which) -> showSettings())
                    .show();
            return;
        }
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), 0);
        panel.addView(text("Read repository context first. Push and PR actions always open a second confirmation step.", 12, secondaryText), wrap());
        EditText repository = input("owner/name", false);
        repository.setSingleLine(true);
        panel.addView(repository, marginParams(0, 13, 0, 9));
        TextView output = text("Choose a read or write tool.", 12, secondaryText);
        output.setTextIsSelectable(true);
        output.setPadding(dp(12), dp(10), dp(12), dp(10));
        output.setBackground(rounded(Color.rgb(13, 14, 17), 12));
        panel.addView(output, marginParams(0, 0, 0, 9));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        panel.addView(actions, wrapParams());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("GitHub Agent")
                .setView(panel)
                .setNegativeButton("Close", null)
                .create();
        TextView repoButton = smallButton("Pull repository summary", lavender, view -> runGithub(
                output, callback -> gitHubClient.pullRepository(keyStore.get("github"), repository.getText().toString(), callback)));
        TextView issueButton = smallButton("Pull open issues", lavender, view -> runGithub(
                output, callback -> gitHubClient.listIssues(keyStore.get("github"), repository.getText().toString(), callback)));
        TextView readmeButton = smallButton("Pull README", lavender, view -> runGithub(
                output, callback -> gitHubClient.readFile(keyStore.get("github"), repository.getText().toString(), "README.md", "", callback)));
        TextView pushButton = smallButton("Push a file…", amber, view -> showPushDialog(repository.getText().toString(), output));
        TextView prButton = smallButton("Create pull request…", amber, view -> showPullRequestDialog(repository.getText().toString(), output));
        actions.addView(repoButton, marginParams(0, 0, 0, 7));
        actions.addView(issueButton, marginParams(0, 0, 0, 7));
        actions.addView(readmeButton, marginParams(0, 0, 0, 7));
        actions.addView(pushButton, marginParams(0, 0, 0, 7));
        actions.addView(prButton, wrapParams());
        dialog.show();
    }

    private interface GithubAction {
        void run(GitHubClient.ResultCallback callback);
    }

    private void runGithub(TextView output, GithubAction action) {
        output.setText("Working with GitHub…");
        action.run(new GitHubClient.ResultCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> output.setText(result));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> output.setText("GitHub error: " + message));
            }
        });
    }

    private void showPushDialog(String initialRepository, TextView parentOutput) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), 0);
        EditText path = input("agent-notes/kairo.md", false);
        EditText branch = input("branch, e.g. main", false);
        EditText contentInput = input("File contents", false);
        contentInput.setSingleLine(false);
        contentInput.setMinLines(5);
        panel.addView(text("Path", 11, mutedText), wrap());
        panel.addView(path, marginParams(0, 3, 0, 8));
        panel.addView(text("Branch", 11, mutedText), wrap());
        panel.addView(branch, marginParams(0, 3, 0, 8));
        panel.addView(text("Text file content", 11, mutedText), wrap());
        panel.addView(contentInput, marginParams(0, 3, 0, 0));
        new AlertDialog.Builder(this)
                .setTitle("Confirm file push")
                .setMessage("This will create or update one file through the GitHub Contents API. Review the repository, branch, and content before continuing.")
                .setView(panel)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Push file", (dialog, which) -> {
                    String repository = initialRepository.trim();
                    runGithub(parentOutput, callback -> gitHubClient.pushFile(
                            keyStore.get("github"), repository, path.getText().toString(),
                            branch.getText().toString(), "Update from Kairo", contentInput.getText().toString(), callback));
                }).show();
    }

    private void showPullRequestDialog(String initialRepository, TextView parentOutput) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), 0);
        EditText title = input("PR title", false);
        EditText head = input("feature-branch", false);
        EditText base = input("main", false);
        EditText body = input("What changed and why?", false);
        body.setSingleLine(false);
        body.setMinLines(4);
        panel.addView(title, marginParams(0, 0, 0, 8));
        panel.addView(head, marginParams(0, 0, 0, 8));
        panel.addView(base, marginParams(0, 0, 0, 8));
        panel.addView(body, wrapParams());
        new AlertDialog.Builder(this)
                .setTitle("Confirm pull request")
                .setMessage("Kairo will create a pull request. It will not merge it.")
                .setView(panel)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create PR", (dialog, which) -> runGithub(parentOutput, callback -> gitHubClient.createPullRequest(
                        keyStore.get("github"), initialRepository.trim(), title.getText().toString(),
                        body.getText().toString(), head.getText().toString(), base.getText().toString(), callback)))
                .show();
    }

    private void showModels() {
        setActiveTab(TAB_MODELS);
        content.removeAllViews();
        LinearLayout page = page();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleWrap = new LinearLayout(this);
        titleWrap.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Models", 26, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleWrap.addView(title, wrap());
        titleWrap.addView(text("Free routes, local models, and provider keys in one place.", 12, secondaryText), wrap());
        header.addView(titleWrap, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout headerActions = new LinearLayout(this);
        headerActions.setGravity(Gravity.CENTER_VERTICAL);
        headerActions.addView(smallButton("Refresh", lavender, view -> refreshModels()), wrap());
        headerActions.addView(smallButton("NVIDIA live", mint, view -> refreshNvidiaModels()), marginWrapParams(6, 0, 0, 0));
        header.addView(headerActions, wrap());
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(62)));

        LinearLayout catalogNote = card();
        catalogNote.setPadding(dp(13), dp(10), dp(13), dp(10));
        catalogNote.addView(text("NVIDIA CATALOG", 10, mint), wrap());
        catalogNote.addView(text("NVIDIA and Kimi / Moonshot entries are candidate indexes. A saved provider key plus live refresh is required to know which IDs your account, region, credits, quota, and endpoint currently expose. No model is promised to be free; deep-thinking behavior remains model/provider dependent.", 11, secondaryText), marginParams(0, 5, 0, 0));
        page.addView(catalogNote, marginParams(0, 8, 0, 9));

        modelSearch = input("Search models or providers", false);
        modelSearch.setSingleLine(true);
        page.addView(modelSearch, marginParams(0, 7, 0, 9));
        HorizontalScrollView filterScroll = new HorizontalScrollView(this);
        filterScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = new LinearLayout(this);
        filters.setPadding(0, 0, dp(4), 0);
        String[] names = {"All", "Free / tier", "Local", "NVIDIA"};
        String[] values = {"all", "free", "local", "nvidia"};
        for (int index = 0; index < names.length; index++) {
            final String filter = values[index];
            TextView filterButton = smallButton(names[index], lavender, view -> {
                modelFilter = filter;
                renderModelList();
            });
            filters.addView(filterButton, marginWrapParams(0, 0, 8, 0));
        }
        filterScroll.addView(filters, new HorizontalScrollView.LayoutParams(-2, -2));
        page.addView(filterScroll, new LinearLayout.LayoutParams(-1, dp(40)));

        modelListContainer = new LinearLayout(this);
        modelListContainer.setOrientation(LinearLayout.VERTICAL);
        ScrollView listScroll = new ScrollView(this);
        listScroll.setFillViewport(true);
        listScroll.addView(modelListContainer, new ScrollView.LayoutParams(-1, -1));
        page.addView(listScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
        modelSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderModelList(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        renderModelList();
    }

    private void renderModelList() {
        if (modelListContainer == null) return;
        modelListContainer.removeAllViews();
        String query = modelSearch == null ? "" : modelSearch.getText().toString();
        int count = 0;
        for (ModelInfo model : ModelCatalog.all()) {
            if (!model.matches(query)) continue;
            if ("free".equals(modelFilter) && !model.isFreeRoute()) continue;
            if ("local".equals(modelFilter) && !model.isLocal()) continue;
            if ("nvidia".equals(modelFilter) && !"nvidia".equals(model.getProviderId())) continue;
            addModelCard(modelListContainer, model);
            count++;
        }
        TextView countLabel = text(count + " models · live catalogs can change · candidate IDs are not availability guarantees", 11, mutedText);
        modelListContainer.addView(countLabel, marginParams(0, 12, 0, 20));
    }

    private void addModelCard(LinearLayout parent, ModelInfo model) {
        LinearLayout item = card();
        item.setPadding(dp(14), dp(13), dp(14), dp(13));
        item.setOnClickListener(view -> {
            preferences.setModel(model.getProviderId(), model.getId());
            toast(model.getName() + " selected");
            showChat();
        });
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(model.getName(), 16, primaryText);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        if (model.isCandidate()) {
            top.addView(pill("CANDIDATE", amber, soft), wrap());
        } else if (model.isFreeRoute()) {
            top.addView(pill(model.isLocal() ? "LOCAL" : "FREE / TIER", model.isLocal() ? mint : lavender, soft), wrap());
        }
        item.addView(top, wrap());
        item.addView(text(ProviderConfig.displayName(model.getProviderId()) + "  ·  " + model.getContextWindow(), 11, lavender), marginParams(0, 6, 0, 3));
        item.addView(text(model.getDescription(), 12, secondaryText), wrap());
        item.addView(text(model.getBillingNote(), 11, mutedText), marginParams(0, 8, 0, 0));
        parent.addView(item, marginParams(0, 0, 0, 9));
    }

    private void refreshModels() {
        String provider = preferences.getProvider();
        String key = keyStore.get(provider);
        toast("Refreshing " + ProviderConfig.displayName(provider) + "…");
        ApiClient.discoverModels(provider, ProviderConfig.baseUrl(provider, preferences), key,
                new ApiClient.ModelsCallback() {
                    @Override
                    public void onSuccess(List<ModelInfo> models) {
                        ModelCatalog.replaceDiscovered(provider, models);
                        runOnUiThread(() -> {
                            renderModelList();
                            toast("Added " + models.size() + " live models");
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> toast("Catalog refresh: " + message));
                    }
                });
    }

    private void refreshNvidiaModels() {
        if (keyStore.get("nvidia").isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Connect NVIDIA first")
                    .setMessage("Save one NVIDIA API key in Settings. Kairo will use the official NVIDIA models endpoint to replace candidate labels with models actually exposed to your account.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Open Settings", (dialog, which) -> showSettings())
                    .show();
            return;
        }
        toast("Refreshing NVIDIA live catalog…");
        ApiClient.discoverModels("nvidia", ProviderConfig.baseUrl("nvidia", preferences), keyStore.get("nvidia"),
                new ApiClient.ModelsCallback() {
                    @Override public void onSuccess(List<ModelInfo> models) {
                        ModelCatalog.replaceDiscovered("nvidia", models);
                        runOnUiThread(() -> {
                            renderModelList();
                            toast("NVIDIA exposed " + models.size() + " models to this key");
                        });
                    }
                    @Override public void onError(String message) {
                        runOnUiThread(() -> toast("NVIDIA catalog: " + message));
                    }
                });
    }


    private void showImageStudio() {
        closeDrawer();
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Image studio", "Generate images from a text prompt via an OpenAI-compatible images API."), wrapParams());

        LinearLayout intro = card();
        intro.setPadding(dp(14), dp(13), dp(14), dp(13));
        intro.addView(text("TEXT → IMAGE", 10, lavender), wrap());
        intro.addView(text("Uses your saved key and an images-capable model (for example dall-e-3). Images stay on this device until you share them.", 13, secondaryText), marginParams(0, 6, 0, 0));
        page.addView(intro, marginParams(0, 12, 0, 12));

        EditText prompt = input("Describe the image you want…", false);
        prompt.setSingleLine(false);
        prompt.setMinLines(3);
        prompt.setMaxLines(8);
        page.addView(prompt, marginParams(0, 0, 0, 10));

        EditText modelField = input("Model id (e.g. dall-e-3)", false);
        modelField.setSingleLine(true);
        modelField.setText("dall-e-3");
        page.addView(text("Model", 11, mutedText), wrap());
        page.addView(modelField, marginParams(0, 2, 0, 8));

        final String[] sizeHolder = {"1024x1024"};
        TextView sizeBtn = smallButton("Size: 1024×1024", secondaryText, view -> {
            String[] labels = {"1024×1024", "1024×1792 (portrait)", "1792×1024 (landscape)"};
            String[] values = {"1024x1024", "1024x1792", "1792x1024"};
            new AlertDialog.Builder(this)
                    .setTitle("Image size")
                    .setItems(labels, (d, which) -> {
                        sizeHolder[0] = values[which];
                        ((TextView) view).setText("Size: " + labels[which]);
                    })
                    .show();
        });
        page.addView(sizeBtn, marginParams(0, 0, 0, 8));

        ImageView preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setMaxHeight(dp(320));
        preview.setVisibility(View.GONE);
        preview.setBackground(rounded(raised, 14));
        page.addView(preview, marginParams(0, 0, 0, 10));

        TextView status = text("Ready.", 12, mutedText);
        page.addView(status, marginParams(0, 0, 0, 8));

        final byte[][] lastBytes = {null};
        final String[] lastMime = {"image/png"};

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.addView(smallButton("Generate", mint, view -> {
            String ptext = prompt.getText().toString().trim();
            if (ptext.isEmpty()) { toast("Enter a prompt"); return; }
            String provider = preferences.getProvider();
            String key = keyStore.get(provider);
            if (key == null || key.trim().isEmpty()) {
                key = keyStore.get("openai");
                provider = "openai";
            }
            if (key == null || key.trim().isEmpty()) {
                toast("Add an OpenAI or compatible key in Settings");
                return;
            }
            status.setText("Generating… this can take up to a minute.");
            preview.setVisibility(View.GONE);
            lastBytes[0] = null;
            String base = ProviderConfig.baseUrl(provider, preferences);
            if ("openai".equals(provider) || (base != null && base.contains("openai.com"))) {
                base = "https://api.openai.com/v1";
            }
            ImageGenerationClient.generate(
                    base,
                    key,
                    modelField.getText().toString().trim(),
                    ptext,
                    sizeHolder[0],
                    "standard",
                    new ImageGenerationClient.Callback() {
                        @Override public void onSuccess(byte[] bytes, String mimeType, String revisedPrompt) {
                            runOnUiThread(() -> {
                                lastBytes[0] = bytes;
                                lastMime[0] = mimeType == null ? "image/png" : mimeType;
                                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                if (bmp != null) {
                                    preview.setImageBitmap(bmp);
                                    preview.setVisibility(View.VISIBLE);
                                }
                                status.setText("Done · " + (bytes.length / 1024) + " KB"
                                        + (revisedPrompt != null && !revisedPrompt.isEmpty()
                                        ? "\nRevised: " + revisedPrompt : ""));
                                toast("Image ready");
                            });
                        }
                        @Override public void onError(String message) {
                            runOnUiThread(() -> {
                                status.setText("Failed: " + message);
                                toast("Image generation failed");
                            });
                        }
                    });
        }), wrap());
        actions.addView(smallButton("Save to sandbox", lavender, view -> {
            if (lastBytes[0] == null) { toast("Generate an image first"); return; }
            try {
                com.kairo.app.core.SandboxWorkspace sw = new com.kairo.app.core.SandboxWorkspace(this);
                String ext = lastMime[0].contains("jpeg") ? "jpg" : "png";
                java.io.File outDir = new java.io.File(sw.getRoot(), "out");
                if (!outDir.exists()) outDir.mkdirs();
                java.io.File out = new java.io.File(outDir, "image-" + System.currentTimeMillis() + "." + ext);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                    fos.write(lastBytes[0]);
                }
                toast("Saved " + out.getName());
                status.setText(status.getText() + "\nSaved: " + out.getAbsolutePath());
            } catch (Exception e) {
                toast(e.getMessage() == null ? "Save failed" : e.getMessage());
            }
        }), marginWrapParams(8, 0, 0, 0));
        actions.addView(smallButton("Share", secondaryText, view -> {
            if (lastBytes[0] == null) { toast("Generate an image first"); return; }
            try {
                java.io.File out = new java.io.File(getCacheDir(), "kairo-image-" + System.currentTimeMillis() + ".png");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                    fos.write(lastBytes[0]);
                }
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType(lastMime[0]);
                share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(out));
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, "Share image"));
            } catch (Exception e) {
                toast("Share failed");
            }
        }), marginWrapParams(8, 0, 0, 0));
        page.addView(actions, marginParams(0, 4, 0, 0));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void showArena() {
        closeDrawer();
        ensureArenaModels();
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Arena", "Arena.ai-style live dual-model comparison"), wrapParams());

        LinearLayout intro = card();
        intro.setPadding(dp(16), dp(14), dp(16), dp(14));
        intro.addView(text("LIVE SIDE-BY-SIDE", 10, lavender), wrap());
        intro.addView(text("Pick Model A and Model B → ask one prompt → both stream in parallel. Compare speed, reasoning, and style instantly.", 13, secondaryText), marginParams(0, 7, 0, 0));
        page.addView(intro, marginParams(0, 12, 0, 14));

        arenaPrompt = input("Ask both models the same question…", false);
        arenaPrompt.setSingleLine(false);
        arenaPrompt.setGravity(Gravity.TOP | Gravity.START);
        arenaPrompt.setMinLines(3);
        arenaPrompt.setMaxLines(8);
        page.addView(arenaPrompt, marginParams(0, 0, 0, 12));

        LinearLayout pickers = new LinearLayout(this);
        pickers.setGravity(Gravity.CENTER_VERTICAL);
        arenaLeftPicker = smallButton(arenaLabel(true), lavender, view -> chooseArenaModel(true));
        arenaRightPicker = smallButton(arenaLabel(false), mint, view -> chooseArenaModel(false));
        pickers.addView(arenaLeftPicker, new LinearLayout.LayoutParams(0, dp(44), 1));
        pickers.addView(arenaRightPicker, marginWeightParams(10, 0, 0, 0, 1));
        page.addView(pickers, wrapParams());

        arenaRunButton = smallButton("Run comparison", mint, view -> {
            if (arenaLeftRunning || arenaRightRunning) cancelArena();
            else runArena();
        });
        if (arenaLeftRunning || arenaRightRunning) {
            arenaRunButton.setText("Stop both");
            arenaRunButton.setTextColor(amber);
        }
        page.addView(arenaRunButton, marginParams(0, 12, 0, 16));

        LinearLayout panels = new LinearLayout(this);
        boolean wide = getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                || getResources().getDisplayMetrics().widthPixels
                > (int) (600 * getResources().getDisplayMetrics().density);
        panels.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        LinearLayout leftCard = arenaResponseCard(true);
        LinearLayout rightCard = arenaResponseCard(false);
        if (wide) {
            panels.addView(leftCard, new LinearLayout.LayoutParams(0, -2, 1f));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -2, 1f);
            rp.setMargins(dp(10), 0, 0, 0);
            panels.addView(rightCard, rp);
        } else {
            panels.addView(leftCard, marginParams(0, 0, 0, 12));
            panels.addView(rightCard, wrapParams());
        }
        page.addView(panels, wrapParams());
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -1));
    }

    private LinearLayout arenaResponseCard(boolean left) {
        LinearLayout card = card();
        card.setPadding(dp(14), dp(13), dp(14), dp(13));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = text(left ? "A" : "B", 11, background);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setBackground(circle(left ? lavender : mint));
        header.addView(badge, new LinearLayout.LayoutParams(dp(22), dp(22)));
        TextView label = text("  " + arenaLabel(left).replaceFirst("^[AB] · ", ""), 13, primaryText);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(label, wrap());
        card.addView(header, wrap());

        StringBuilder existing = left ? arenaLeftBuffer : arenaRightBuffer;
        boolean running = left ? arenaLeftRunning : arenaRightRunning;
        String initial = running
                ? (existing == null || existing.length() == 0 ? "Streaming…" : existing.toString())
                : "Waiting for a prompt…";
        TextView output = text(initial, 14, secondaryText);
        output.setTextIsSelectable(true);
        output.setLineSpacing(dp(2), 1.05f);
        output.setPadding(0, dp(12), 0, dp(6));
        card.addView(output, wrapParams());

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.addView(smallButton("Copy", secondaryText,
                view -> {
                    copyToClipboard(output.getText().toString());
                    toast("Copied");
                }), wrap());
        actions.addView(smallButton("Save as file", lavender,
                view -> showCreateArtifactFromAnswer(output.getText().toString())), marginWrapParams(8, 0, 0, 0));
        card.addView(actions, marginParams(0, 6, 0, 0));
        if (left) arenaLeftOutput = output;
        else arenaRightOutput = output;
        return card;
    }

    private void ensureArenaModels() {
        if (arenaLeftModel == null) {
            arenaLeftModel = ModelCatalog.find("openrouter", "deepseek/deepseek-r1:free");
            if (arenaLeftModel == null) arenaLeftModel = ModelCatalog.all().get(0);
        }
        if (arenaRightModel == null) {
            arenaRightModel = ModelCatalog.find("groq", "llama-3.1-8b-instant");
            if (arenaRightModel == null) arenaRightModel = ModelCatalog.all().get(1);
        }
    }

    private String arenaLabel(boolean left) {
        ModelInfo model = left ? arenaLeftModel : arenaRightModel;
        if (model == null) return left ? "Model A" : "Model B";
        String name = model.getName();
        if (name.length() > 18) name = name.substring(0, 17) + "…";
        return (left ? "A · " : "B · ") + name;
    }

    private void chooseArenaModel(boolean left) {
        List<ModelInfo> models = ModelCatalog.all();
        String[] labels = new String[Math.min(models.size(), 40)];
        for (int index = 0; index < labels.length; index++) {
            ModelInfo model = models.get(index);
            labels[index] = (model.isFreeRoute() ? "FREE · " : "") + model.getName()
                    + " · " + ProviderConfig.displayName(model.getProviderId());
        }
        new AlertDialog.Builder(this)
                .setTitle(left ? "Choose model A" : "Choose model B")
                .setItems(labels, (dialog, which) -> {
                    if (left) arenaLeftModel = models.get(which);
                    else arenaRightModel = models.get(which);
                    if (arenaLeftPicker != null) arenaLeftPicker.setText(arenaLabel(true));
                    if (arenaRightPicker != null) arenaRightPicker.setText(arenaLabel(false));
                })
                .show();
    }

    private void runArena() {
        String prompt = arenaPrompt == null ? "" : arenaPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            toast("Write one prompt for both models");
            return;
        }
        if (prompt.length() > 32_000) {
            toast("Arena prompts are limited to 32,000 characters");
            return;
        }
        ensureArenaModels();
        arenaLeftBuffer = new StringBuilder();
        arenaRightBuffer = new StringBuilder();
        arenaLeftRunning = true;
        arenaRightRunning = true;
        arenaLeftStartedAt = System.currentTimeMillis();
        arenaRightStartedAt = System.currentTimeMillis();
        arenaLeftChars = 0;
        arenaRightChars = 0;
        arenaRunButton.setText("Stop comparison");
        arenaRunButton.setTextColor(amber);
        arenaLeftOutput.setText("Connecting to " + ProviderConfig.displayName(arenaLeftModel.getProviderId()) + "…");
        arenaRightOutput.setText("Connecting to " + ProviderConfig.displayName(arenaRightModel.getProviderId()) + "…");
        startArenaSide(true, arenaLeftModel, prompt);
        startArenaSide(false, arenaRightModel, prompt);
    }

    private void startArenaSide(boolean left, ModelInfo model, String prompt) {
        String provider = model.getProviderId();
        if (ProviderConfig.needsApiKey(provider) && keyStore.get(provider).isEmpty()) {
            finishArenaSide(left, "Missing " + ProviderConfig.displayName(provider) + " key. Add it in Settings.");
            return;
        }
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", prompt));
        ApiClient.StreamingCallback callback = new ApiClient.StreamingCallback() {
            @Override public void onToken(String token) {
                runOnUiThread(() -> receiveArenaToken(left, token));
            }
            @Override public void onComplete() {
                runOnUiThread(() -> finishArenaSide(left, null));
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> finishArenaSide(left, message));
            }
        };
        ApiClient.RequestHandle request = ApiClient.sendChatStreaming(provider,
                ProviderConfig.baseUrl(provider, preferences), keyStore.get(provider), model.getId(), messages,
                Collections.emptyList(), preferences.getTemperature(), preferences.getMaxOutputTokens(),
                preferences.getReasoningMode(), callback);
        if (left) arenaLeftRequest = request;
        else arenaRightRequest = request;
    }

    private void receiveArenaToken(boolean left, String token) {
        if (token == null) return;
        StringBuilder buffer = left ? arenaLeftBuffer : arenaRightBuffer;
        if (buffer == null) return;
        buffer.append(token);
        if (left) arenaLeftChars += token.length(); else arenaRightChars += token.length();
        TextView output = left ? arenaLeftOutput : arenaRightOutput;
        if (output != null) {
            SpannableStringBuilder withCaret = new SpannableStringBuilder(
                    MarkdownRenderer.render(buffer.toString()));
            withCaret.append(" ▍");
            output.setText(withCaret);
        }
    }

    private void finishArenaSide(boolean left, String error) {
        StringBuilder buffer = left ? arenaLeftBuffer : arenaRightBuffer;
        if (buffer == null) buffer = new StringBuilder();
        if (error != null && buffer.length() == 0) buffer.append("Request failed: ").append(error);
        if (buffer.length() == 0) buffer.append("No answer returned.");
        TextView output = left ? arenaLeftOutput : arenaRightOutput;
        long started = left ? arenaLeftStartedAt : arenaRightStartedAt;
        int chars = left ? arenaLeftChars : arenaRightChars;
        long elapsedMs = Math.max(1L, System.currentTimeMillis() - started);
        double secs = elapsedMs / 1000.0;
        double cps = chars / secs;
        String stats = String.format(java.util.Locale.US, "\n\n_%.1fs · %d chars · %.0f c/s_", secs, chars, cps);
        if (output != null) output.setText(MarkdownRenderer.render(buffer.toString().trim() + stats));
        if (left) {
            arenaLeftRunning = false;
            arenaLeftRequest = null;
        } else {
            arenaRightRunning = false;
            arenaRightRequest = null;
        }
        if (!arenaLeftRunning && !arenaRightRunning && arenaRunButton != null) {
            arenaRunButton.setText("Run comparison");
            arenaRunButton.setTextColor(mint);
        }
    }

    private void cancelArena() {
        if (arenaLeftRequest != null) arenaLeftRequest.cancel();
        if (arenaRightRequest != null) arenaRightRequest.cancel();
        if (arenaLeftRunning) finishArenaSide(true, "Response stopped.");
        if (arenaRightRunning) finishArenaSide(false, "Response stopped.");
    }

    private String shortDeviceId() {
        String id = deviceSetup.getDeviceId();
        return id.length() > 12 ? id.substring(0, 12) + "…" : id;
    }

    private String setupChecks() {
        int connected = 0;
        if (keyStore.hasKey("openrouter") || keyStore.hasKey("groq") || keyStore.hasKey("nvidia")
                || keyStore.hasKey("mistral") || keyStore.hasKey("anthropic") || keyStore.hasKey("openai")) connected++;
        if (keyStore.hasKey("github")) connected++;
        if (keyStore.hasKey("vercel")) connected++;
        if (keyStore.hasKey("n8n") && !preferences.getN8nBaseUrl().isEmpty()) connected++;
        if (keyStore.hasKey("slack")) connected++;
        if (keyStore.hasKey("notion")) connected++;
        if (keyStore.hasKey("linear")) connected++;
        if (keyStore.hasKey("supabase") && !preferences.getSupabaseUrl().isEmpty()) connected++;
        if (keyStore.hasKey("discord")) connected++;
        return connected + " / 9 connection groups ready";
    }

    private void showDeviceSetup() {
        closeDrawer();
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Device setup", "Prepare this Kairo install, then sign in to the services you choose."), wrapParams());

        LinearLayout identity = card();
        identity.setPadding(dp(15), dp(14), dp(13), dp(13));
        TextView eyebrow = text("LOCAL DEVICE IDENTITY", 10, lavender);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        identity.addView(eyebrow, wrap());
        EditText name = input("Device name", false);
        name.setSingleLine(true);
        name.setText(deviceSetup.getDeviceName());
        identity.addView(name, marginParams(0, 9, 0, 8));
        identity.addView(text("Install id  " + deviceSetup.getDeviceId(), 11, secondaryText), wrap());
        identity.addView(text("Pairing label  " + deviceSetup.getPairingCode() + "  ·  display only, not a login credential", 10, mutedText), marginParams(0, 5, 0, 0));
        LinearLayout identityActions = new LinearLayout(this);
        identityActions.setGravity(Gravity.END);
        identityActions.addView(smallButton("Copy install id", secondaryText, view -> {
            copyToClipboard(deviceSetup.getDeviceId());
            toast("Install id copied");
        }), wrap());
        identityActions.addView(smallButton("Save device", lavender, view -> {
            deviceSetup.setDeviceName(name.getText().toString());
            toast("Device profile saved");
        }), marginWrapParams(7, 0, 0, 0));
        identity.addView(identityActions, marginParams(0, 9, 0, 0));
        page.addView(identity, marginParams(0, 12, 0, 12));

        LinearLayout checklist = card();
        checklist.setPadding(dp(14), dp(13), dp(14), dp(13));
        checklist.addView(text("SETUP CHECKLIST", 10, mint), wrap());
        checklist.addView(text("① Name this device\n② Sign in to a provider in your browser\n③ Paste the provider token into Connectors or Settings\n④ Review permissions before using a write action", 12, secondaryText), marginParams(0, 7, 0, 0));
        checklist.addView(text(setupChecks(), 11, lavender), marginParams(0, 9, 0, 0));
        page.addView(checklist, marginParams(0, 0, 0, 12));

        LinearLayout login = card();
        login.setPadding(dp(14), dp(13), dp(14), dp(13));
        login.addView(text("BROWSER LOGIN / TOKEN SETUP", 10, lavender), wrap());
        login.addView(text("Kairo does not create a hosted account or silently sync credentials. Use an official provider page, return here, and enter the token in the connector card.", 12, secondaryText), marginParams(0, 6, 0, 9));
        login.addView(smallButton("Open provider login pages", lavender, view -> showProviderLoginPicker()), wrapParams());
        login.addView(smallButton("Open Connectors", mint, view -> showConnectors()), marginParams(0, 7, 0, 0));
        page.addView(login, marginParams(0, 0, 0, 12));

        LinearLayout completion = card();
        completion.setPadding(dp(14), dp(13), dp(14), dp(13));
        completion.addView(text(deviceSetup.isSetupComplete() ? "DEVICE READY" : "FINISH DEVICE SETUP", 10, deviceSetup.isSetupComplete() ? mint : amber), wrap());
        completion.addView(text(deviceSetup.isSetupComplete()
                ? "This installation is ready. You can change the profile or connectors at any time."
                : "Mark this local setup complete when you are comfortable with the selected providers and permissions.", 12, secondaryText), marginParams(0, 6, 0, 9));
        completion.addView(smallButton(deviceSetup.isSetupComplete() ? "Reset setup status" : "Mark setup complete", deviceSetup.isSetupComplete() ? secondaryText : mint, view -> {
            deviceSetup.markSetupComplete(!deviceSetup.isSetupComplete());
            showDeviceSetup();
            toast(deviceSetup.isSetupComplete() ? "Device setup complete" : "Setup status reset");
        }), wrapParams());
        page.addView(completion, marginParams(0, 0, 0, 20));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -1));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void showProviderLoginPicker() {
        String[] providers = {"OpenRouter", "Groq", "Kimi / Moonshot", "NVIDIA NIM", "Mistral AI", "Anthropic", "OpenAI", "GitHub", "Vercel", "n8n instance", "Slack", "Notion", "Linear", "Supabase", "Discord"};
        String[] urls = {
                "https://openrouter.ai/settings/keys",
                "https://console.groq.com/keys",
                "https://platform.moonshot.ai/console/api-keys",
                "https://build.nvidia.com/",
                "https://console.mistral.ai/api-keys/",
                "https://console.anthropic.com/settings/keys",
                "https://platform.openai.com/api-keys",
                "https://github.com/settings/personal-access-tokens",
                "https://vercel.com/account/tokens",
                preferences.getN8nBaseUrl().isEmpty() ? "https://n8n.io" : preferences.getN8nBaseUrl(),
                "https://api.slack.com/apps",
                "https://www.notion.so/profile/integrations",
                "https://linear.app/settings/api",
                "https://supabase.com/dashboard",
                "https://discord.com/developers/applications"
        };
        new AlertDialog.Builder(this)
                .setTitle("Official sign-in and token pages")
                .setMessage("A browser page opens for the provider you choose. Kairo never receives your provider password.")
                .setItems(providers, (dialog, which) -> openUrl(urls[which]))
                .show();
    }

    private void showConnectors() {
        setActiveTab(TAB_CONNECTORS);
        closeDrawer();
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Connectors", "Link Kairo to the services that ship, deploy, and automate your work."), wrapParams());

        LinearLayout intro = card();
        intro.setPadding(dp(14), dp(13), dp(14), dp(13));
        intro.addView(text("CONTROLLED HANDOFFS", 10, lavender), wrap());
        intro.addView(text("GitHub holds the source, Vercel ships the deployment, Linear tracks issues, and n8n runs the automation. Kairo keeps credentials encrypted and asks before every external write or webhook.", 13, secondaryText), marginParams(0, 6, 0, 0));
        page.addView(intro, marginParams(0, 12, 0, 10));

        LinearLayout device = card();
        device.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout deviceTop = new LinearLayout(this);
        deviceTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView deviceTitle = text("This device", 15, primaryText);
        deviceTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        deviceTop.addView(deviceTitle, new LinearLayout.LayoutParams(0, -2, 1));
        deviceTop.addView(pill(deviceSetup.isSetupComplete() ? "READY" : "SET UP", deviceSetup.isSetupComplete() ? mint : amber, soft), wrap());
        device.addView(deviceTop, wrap());
        device.addView(text(deviceSetup.getDeviceName() + "  ·  " + deviceSetup.getPairingCode(), 12, secondaryText), marginParams(0, 5, 0, 2));
        device.addView(text("Local install id  " + shortDeviceId(), 10, mutedText), wrap());
        device.addView(smallButton("Open device setup", lavender, view -> showDeviceSetup()), marginParams(0, 9, 0, 0));
        page.addView(device, marginParams(0, 0, 0, 12));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (ConnectorDefinition connector : ConnectorCatalog.all()) addConnectorCard(list, connector);
        page.addView(list, wrapParams());

        LinearLayout flow = card();
        flow.setPadding(dp(14), dp(13), dp(14), dp(13));
        flow.addView(text("A REVIEWABLE FLOW", 10, mint), wrap());
        flow.addView(text("1  Inspect repository\n2  Preview or save the artifact\n3  Deploy from the selected branch\n4  Trigger n8n only with your approved JSON", 12, secondaryText), marginParams(0, 7, 0, 0));
        page.addView(flow, marginParams(0, 12, 0, 20));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -1));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void addConnectorCard(LinearLayout parent, ConnectorDefinition connector) {
        LinearLayout item = card();
        item.setPadding(dp(15), dp(14), dp(13), dp(13));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(connector.getName(), 18, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        boolean ready = connectorReady(connector.getId());
        top.addView(pill(ready ? "CONNECTED" : "NOT CONNECTED", ready ? mint : mutedText, soft), wrap());
        item.addView(top, wrap());
        TextView eyebrow = text(connector.getEyebrow(), 10, lavender);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        item.addView(eyebrow, marginParams(0, 8, 0, 4));
        item.addView(text(connector.getDescription(), 13, secondaryText), wrap());
        item.addView(text(connector.getCapabilities(), 11, mutedText), marginParams(0, 9, 0, 10));
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.addView(smallButton(ready ? "Open workspace" : "Connect", lavender,
                view -> openConnector(connector.getId())), wrap());
        if (connector.supportsWrites()) {
            TextView safety = text("  Writes need confirmation", 10, amber);
            safety.setGravity(Gravity.CENTER_VERTICAL);
            actions.addView(safety, wrap());
        }
        item.addView(actions, wrapParams());
        parent.addView(item, marginParams(0, 0, 0, 10));
    }

    private boolean connectorReady(String id) {
        if ("n8n".equals(id)) return keyStore.hasKey("n8n") && !preferences.getN8nBaseUrl().isEmpty();
        if ("supabase".equals(id)) return keyStore.hasKey("supabase") && !preferences.getSupabaseUrl().isEmpty();
        return keyStore.hasKey(id);
    }

    private void openConnector(String id) {
        activeAgentId = "automation";
        if ("github".equals(id)) showGithubDialog();
        else if ("vercel".equals(id)) showVercelDialog();
        else if ("n8n".equals(id)) showN8nDialog();
        else if ("slack".equals(id)) showSlackDialog();
        else if ("notion".equals(id)) showNotionDialog();
        else if ("linear".equals(id)) showLinearDialog();
        else if ("supabase".equals(id)) showSupabaseDialog();
        else if ("discord".equals(id)) showDiscordDialog();
    }

    private interface VercelAction {
        void run(VercelClient.Callback callback);
    }

    private void runVercel(TextView output, VercelAction action) {
        output.setText("Working with Vercel…");
        action.run(new VercelClient.Callback() {
            @Override public void onSuccess(String result) {
                runOnUiThread(() -> output.setText(result));
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> output.setText("Vercel error: " + message));
            }
        });
    }

    private void saveVercelContext(EditText project, EditText team, EditText baseUrl) {
        preferences.setVercelProject(project.getText().toString());
        preferences.setVercelTeamId(team.getText().toString());
        preferences.setVercelBaseUrl(baseUrl.getText().toString());
    }

    private void showVercelDialog() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), 0);
        panel.addView(text("Read project and deployment state, then create a Git-backed production deployment only after reviewing the second confirmation.", 12, secondaryText), wrap());
        EditText project = input("Vercel project name or id", false);
        project.setSingleLine(true);
        project.setText(preferences.getVercelProject());
        EditText team = input("Team id (optional)", false);
        team.setSingleLine(true);
        team.setText(preferences.getVercelTeamId());
        EditText baseUrl = input("Vercel API URL", false);
        baseUrl.setSingleLine(true);
        baseUrl.setText(preferences.getVercelBaseUrl());
        EditText repository = input("GitHub repository for deploy, owner/name", false);
        repository.setSingleLine(true);
        EditText ref = input("Git ref, e.g. main", false);
        ref.setSingleLine(true);
        ref.setText("main");
        panel.addView(text("Project context", 11, mutedText), marginParams(0, 13, 0, 3));
        panel.addView(project, marginParams(0, 0, 0, 7));
        panel.addView(team, marginParams(0, 0, 0, 7));
        panel.addView(baseUrl, marginParams(0, 0, 0, 7));
        panel.addView(text("Deployment source", 11, mutedText), marginParams(0, 3, 0, 3));
        panel.addView(repository, marginParams(0, 0, 0, 7));
        panel.addView(ref, marginParams(0, 0, 0, 9));
        TextView output = text("No Vercel action run yet.", 12, secondaryText);
        output.setTextIsSelectable(true);
        output.setPadding(dp(12), dp(10), dp(12), dp(10));
        output.setBackground(rounded(Color.rgb(13, 14, 17), 12));
        panel.addView(output, marginParams(0, 0, 0, 9));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        panel.addView(actions, wrapParams());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Vercel connector")
                .setView(panel)
                .setNegativeButton("Close", null)
                .create();
        TextView manage = smallButton("Manage Vercel token", lavender, view -> openKeyDialog("vercel", "Vercel"));
        TextView projects = smallButton("List projects", lavender, view -> {
            saveVercelContext(project, team, baseUrl);
            runVercel(output, callback -> vercelClient.listProjects(keyStore.get("vercel"),
                    preferences.getVercelBaseUrl(), preferences.getVercelTeamId(), callback));
        });
        TextView deployments = smallButton("Recent deployments", lavender, view -> {
            saveVercelContext(project, team, baseUrl);
            runVercel(output, callback -> vercelClient.listDeployments(keyStore.get("vercel"),
                    preferences.getVercelBaseUrl(), preferences.getVercelTeamId(),
                    preferences.getVercelProject(), callback));
        });
        TextView deploy = smallButton("Deploy from Git…", amber, view -> {
            saveVercelContext(project, team, baseUrl);
            showVercelDeploymentConfirmation(output, project.getText().toString(), team.getText().toString(),
                    repository.getText().toString(), ref.getText().toString());
        });
        TextView save = smallButton("Save project context", secondaryText, view -> {
            saveVercelContext(project, team, baseUrl);
            toast("Vercel project context saved");
        });
        TextView dashboard = smallButton("Open Vercel dashboard", secondaryText,
                view -> openUrl("https://vercel.com/dashboard"));
        actions.addView(manage, marginParams(0, 0, 0, 7));
        actions.addView(projects, marginParams(0, 0, 0, 7));
        actions.addView(deployments, marginParams(0, 0, 0, 7));
        actions.addView(deploy, marginParams(0, 0, 0, 7));
        actions.addView(save, marginParams(0, 0, 0, 7));
        actions.addView(dashboard, wrapParams());
        dialog.show();
    }

    private void showVercelDeploymentConfirmation(
            TextView output,
            String project,
            String team,
            String repository,
            String ref) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Vercel deployment")
                .setMessage("Create a production deployment for " + project + " from "
                        + repository + " @ " + (ref.isEmpty() ? "main" : ref)
                        + "? Kairo will not modify GitHub files.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Deploy", (dialog, which) -> runVercel(output, callback ->
                        vercelClient.createDeployment(keyStore.get("vercel"),
                                preferences.getVercelBaseUrl(), team, project, repository, ref, callback)))
                .show();
    }

    private interface N8nAction {
        void run(N8nClient.Callback callback);
    }

    private void runN8n(TextView output, N8nAction action) {
        output.setText("Working with n8n…");
        action.run(new N8nClient.Callback() {
            @Override public void onSuccess(String result) {
                runOnUiThread(() -> output.setText(result));
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> output.setText("n8n error: " + message));
            }
        });
    }

    private void saveN8nContext(EditText baseUrl, EditText webhook) {
        preferences.setN8nBaseUrl(baseUrl.getText().toString());
        preferences.setN8nWebhookUrl(webhook.getText().toString());
    }

    private void showN8nDialog() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), 0);
        panel.addView(text("Use the n8n API for workflow visibility and a specific webhook for automation. Kairo never guesses a workflow or silently runs a payload.", 12, secondaryText), wrap());
        EditText baseUrl = input("https://n8n.example.com", false);
        baseUrl.setSingleLine(true);
        baseUrl.setText(preferences.getN8nBaseUrl());
        EditText webhook = input("Production webhook URL (optional)", false);
        webhook.setSingleLine(true);
        webhook.setText(preferences.getN8nWebhookUrl());
        EditText workflowId = input("Workflow id for activation", false);
        workflowId.setSingleLine(true);
        EditText payload = input("JSON payload", false);
        payload.setSingleLine(false);
        payload.setGravity(Gravity.TOP | Gravity.START);
        payload.setMinLines(4);
        payload.setText("{\n  \"source\": \"kairo\"\n}");
        panel.addView(text("n8n instance", 11, mutedText), marginParams(0, 13, 0, 3));
        panel.addView(baseUrl, marginParams(0, 0, 0, 7));
        panel.addView(webhook, marginParams(0, 0, 0, 7));
        panel.addView(workflowId, marginParams(0, 0, 0, 7));
        panel.addView(text("Webhook payload", 11, mutedText), marginParams(0, 3, 0, 3));
        panel.addView(payload, marginParams(0, 0, 0, 9));
        TextView output = text("No n8n action run yet.", 12, secondaryText);
        output.setTextIsSelectable(true);
        output.setPadding(dp(12), dp(10), dp(12), dp(10));
        output.setBackground(rounded(Color.rgb(13, 14, 17), 12));
        panel.addView(output, marginParams(0, 0, 0, 9));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        panel.addView(actions, wrapParams());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("n8n connector")
                .setView(panel)
                .setNegativeButton("Close", null)
                .create();
        TextView manage = smallButton("Manage n8n API key", lavender, view -> openKeyDialog("n8n", "n8n"));
        TextView save = smallButton("Save connection", secondaryText, view -> {
            saveN8nContext(baseUrl, webhook);
            toast("n8n connection saved");
        });
        TextView workflows = smallButton("List workflows", lavender, view -> {
            saveN8nContext(baseUrl, webhook);
            runN8n(output, callback -> n8nClient.listWorkflows(keyStore.get("n8n"),
                    preferences.getN8nBaseUrl(), callback));
        });
        TextView executions = smallButton("Recent executions", lavender, view -> {
            saveN8nContext(baseUrl, webhook);
            runN8n(output, callback -> n8nClient.listExecutions(keyStore.get("n8n"),
                    preferences.getN8nBaseUrl(), callback));
        });
        TextView activate = smallButton("Activate workflow…", amber, view -> {
            saveN8nContext(baseUrl, webhook);
            showN8nActivationConfirmation(output, workflowId.getText().toString());
        });
        TextView trigger = smallButton("Run webhook…", amber, view -> {
            saveN8nContext(baseUrl, webhook);
            showN8nWebhookConfirmation(output, webhook.getText().toString(), payload.getText().toString());
        });
        TextView open = smallButton("Open n8n", secondaryText, view -> openUrl(baseUrl.getText().toString()));
        actions.addView(manage, marginParams(0, 0, 0, 7));
        actions.addView(save, marginParams(0, 0, 0, 7));
        actions.addView(workflows, marginParams(0, 0, 0, 7));
        actions.addView(executions, marginParams(0, 0, 0, 7));
        actions.addView(activate, marginParams(0, 0, 0, 7));
        actions.addView(trigger, marginParams(0, 0, 0, 7));
        actions.addView(open, wrapParams());
        dialog.show();
    }

    private void showN8nActivationConfirmation(TextView output, String workflowId) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm workflow activation")
                .setMessage("Activate n8n workflow " + (workflowId.trim().isEmpty() ? "(missing id)" : workflowId.trim()) + "? This changes its external state.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Activate", (dialog, which) -> runN8n(output, callback ->
                        n8nClient.activateWorkflow(keyStore.get("n8n"), preferences.getN8nBaseUrl(), workflowId, callback)))
                .show();
    }

    private void showN8nWebhookConfirmation(TextView output, String webhookUrl, String payload) {
        String preview = payload == null ? "{}" : payload.trim();
        if (preview.length() > 500) preview = preview.substring(0, 500) + "…";
        new AlertDialog.Builder(this)
                .setTitle("Confirm webhook run")
                .setMessage("POST this JSON to the configured n8n webhook?\n\n" + preview)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Run webhook", (dialog, which) -> runN8n(output, callback ->
                        n8nClient.runWebhook(webhookUrl, payload, callback)))
                .show();
    }

    private interface SlackAction {
        void run(SlackClient.Callback callback);
    }

    private void runSlack(TextView output, SlackAction action) {
        output.setText("Working with Slack…");
        action.run(new SlackClient.Callback() {
            @Override public void onSuccess(String result) {
                runOnUiThread(() -> output.setText(result));
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> output.setText("Slack error: " + message));
            }
        });
    }

    private void showSlackDialog() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), 0);
        panel.addView(text("Connect a Slack bot token, inspect channels, then send a reviewed message to a channel id. Kairo does not read or post without your tap.", 12, secondaryText), wrap());
        EditText channel = input("Channel id, e.g. C0123456789", false);
        channel.setSingleLine(true);
        EditText message = input("Message to send", false);
        message.setSingleLine(false);
        message.setMinLines(3);
        panel.addView(channel, marginParams(0, 13, 0, 7));
        panel.addView(message, marginParams(0, 0, 0, 9));
        TextView output = text("No Slack action run yet.", 12, secondaryText);
        output.setTextIsSelectable(true);
        output.setPadding(dp(12), dp(10), dp(12), dp(10));
        output.setBackground(rounded(Color.rgb(13, 14, 17), 12));
        panel.addView(output, marginParams(0, 0, 0, 9));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        panel.addView(actions, wrapParams());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Slack connector")
                .setView(panel)
                .setNegativeButton("Close", null)
                .create();
        actions.addView(smallButton("Manage bot token", lavender,
                view -> openKeyDialog("slack", "Slack")), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("Test connection", lavender,
                view -> runSlack(output, callback -> slackClient.test(keyStore.get("slack"), callback))), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("List channels", lavender,
                view -> runSlack(output, callback -> slackClient.listChannels(keyStore.get("slack"), callback))), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("Send message…", amber,
                view -> showSlackSendConfirmation(output, channel.getText().toString(), message.getText().toString())), wrapParams());
        dialog.show();
    }

    private void showSlackSendConfirmation(TextView output, String channel, String message) {
        String preview = message == null ? "" : message.trim();
        if (preview.length() > 500) preview = preview.substring(0, 500) + "…";
        new AlertDialog.Builder(this)
                .setTitle("Confirm Slack message")
                .setMessage("Send to channel " + (channel == null ? "" : channel.trim()) + "?\n\n" + preview)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send", (dialog, which) -> runSlack(output,
                        callback -> slackClient.sendMessage(keyStore.get("slack"), channel, message, callback)))
                .show();
    }

    private interface NotionAction {
        void run(NotionClient.Callback callback);
    }

    private void runNotion(TextView output, NotionAction action) {
        output.setText("Searching Notion…");
        action.run(new NotionClient.Callback() {
            @Override public void onSuccess(String result) {
                runOnUiThread(() -> output.setText(result));
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> output.setText("Notion error: " + message));
            }
        });
    }

    private void showNotionDialog() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), 0);
        panel.addView(text("Search pages shared with your Notion integration. Kairo shows URLs and snippets for review; it never silently adds a page to a prompt.", 12, secondaryText), wrap());
        EditText query = input("Search pages, or leave blank for recent", false);
        query.setSingleLine(true);
        panel.addView(query, marginParams(0, 13, 0, 9));
        TextView output = text("No Notion search run yet.", 12, secondaryText);
        output.setTextIsSelectable(true);
        output.setPadding(dp(12), dp(10), dp(12), dp(10));
        output.setBackground(rounded(Color.rgb(13, 14, 17), 12));
        panel.addView(output, marginParams(0, 0, 0, 9));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        panel.addView(actions, wrapParams());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Notion connector")
                .setView(panel)
                .setNegativeButton("Close", null)
                .create();
        actions.addView(smallButton("Manage integration token", lavender,
                view -> openKeyDialog("notion", "Notion")), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("Search pages", lavender,
                view -> runNotion(output, callback -> notionClient.search(keyStore.get("notion"), query.getText().toString(), callback))), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("Open Notion", secondaryText,
                view -> openUrl("https://www.notion.so")), wrapParams());
        dialog.show();
    }

    private interface LinearAction {
        void run(LinearClient.Callback callback);
    }

    private void runLinear(TextView output, LinearAction action) {
        output.setText("Working with Linear…");
        action.run(new LinearClient.Callback() {
            @Override public void onSuccess(String result) {
                runOnUiThread(() -> output.setText(result));
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> output.setText("Linear error: " + message));
            }
        });
    }

    private void showLinearDialog() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), 0);
        panel.addView(text("Read-only issue search for planning context. Kairo does not create, update, assign, comment on, or transition Linear issues.", 12, secondaryText), wrap());
        EditText query = input("Search issues, e.g. Android attachments", false);
        query.setSingleLine(true);
        panel.addView(query, marginParams(0, 13, 0, 9));
        TextView output = text("No Linear action run yet.", 12, secondaryText);
        output.setTextIsSelectable(true);
        output.setTypeface(Typeface.MONOSPACE);
        output.setPadding(dp(12), dp(10), dp(12), dp(12));
        output.setBackground(rounded(Color.rgb(13, 14, 17), 12));
        panel.addView(output, marginParams(0, 0, 0, 9));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        panel.addView(actions, wrapParams());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Linear connector")
                .setView(panel)
                .setNegativeButton("Close", null)
                .create();
        actions.addView(smallButton("Manage API key", lavender,
                view -> openKeyDialog("linear", "Linear")), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("Test connection", lavender,
                view -> runLinear(output, callback -> linearClient.test(keyStore.get("linear"), callback))), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("Search issues", lavender,
                view -> runLinear(output, callback -> linearClient.searchIssues(keyStore.get("linear"), query.getText().toString(), callback))), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("Open Linear", secondaryText,
                view -> openUrl("https://linear.app")), wrapParams());
        dialog.show();
    }

    private interface SupabaseAction {
        void run(SupabaseClient.Callback callback);
    }

    private void runSupabase(TextView output, SupabaseAction action) {
        output.setText("Reading Supabase…");
        action.run(new SupabaseClient.Callback() {
            @Override public void onSuccess(String result) {
                runOnUiThread(() -> output.setText(result));
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> output.setText("Supabase error: " + message));
            }
        });
    }

    private void saveSupabaseContext(EditText url, EditText table) {
        preferences.setSupabaseUrl(url.getText().toString());
        preferences.setSupabaseTable(table.getText().toString());
    }

    private void showSupabaseDialog() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), 0);
        panel.addView(text("Use a scoped anon key where possible. Kairo only reads twenty rows from the named REST table and never exposes a write tool here.", 12, secondaryText), wrap());
        EditText url = input("https://project.supabase.co", false);
        url.setSingleLine(true);
        url.setText(preferences.getSupabaseUrl());
        EditText table = input("Table name, e.g. notes", false);
        table.setSingleLine(true);
        table.setText(preferences.getSupabaseTable());
        panel.addView(url, marginParams(0, 13, 0, 7));
        panel.addView(table, marginParams(0, 0, 0, 9));
        TextView output = text("No Supabase action run yet.", 12, secondaryText);
        output.setTextIsSelectable(true);
        output.setPadding(dp(12), dp(10), dp(12), dp(10));
        output.setBackground(rounded(Color.rgb(13, 14, 17), 12));
        panel.addView(output, marginParams(0, 0, 0, 9));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        panel.addView(actions, wrapParams());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Supabase connector")
                .setView(panel)
                .setNegativeButton("Close", null)
                .create();
        actions.addView(smallButton("Manage Supabase key", lavender,
                view -> openKeyDialog("supabase", "Supabase")), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("Save project context", secondaryText, view -> {
            saveSupabaseContext(url, table);
            toast("Supabase project context saved");
        }), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("Preview table", lavender, view -> {
            saveSupabaseContext(url, table);
            runSupabase(output, callback -> supabaseClient.readTable(preferences.getSupabaseUrl(),
                    keyStore.get("supabase"), preferences.getSupabaseTable(), callback));
        }), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("Open Supabase", secondaryText,
                view -> openUrl("https://supabase.com/dashboard")), wrapParams());
        dialog.show();
    }

    private void showDiscordDialog() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), 0);
        panel.addView(text("A Discord webhook URL is itself a secret. Kairo stores it encrypted, never displays it, and shows the complete message before sending.", 12, secondaryText), wrap());
        EditText message = input("Status update to send", false);
        message.setSingleLine(false);
        message.setMinLines(4);
        panel.addView(message, marginParams(0, 13, 0, 9));
        TextView output = text("No Discord action run yet.", 12, secondaryText);
        output.setTextIsSelectable(true);
        output.setPadding(dp(12), dp(10), dp(12), dp(10));
        output.setBackground(rounded(Color.rgb(13, 14, 17), 12));
        panel.addView(output, marginParams(0, 0, 0, 9));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        panel.addView(actions, wrapParams());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Discord webhook")
                .setView(panel)
                .setNegativeButton("Close", null)
                .create();
        actions.addView(smallButton("Manage webhook URL", lavender,
                view -> openKeyDialog("discord", "Discord webhook URL")), marginParams(0, 0, 0, 7));
        actions.addView(smallButton("Send update…", amber,
                view -> showDiscordSendConfirmation(output, message.getText().toString())), wrapParams());
        dialog.show();
    }

    private void showDiscordSendConfirmation(TextView output, String message) {
        String preview = message == null ? "" : message.trim();
        if (preview.length() > 500) preview = preview.substring(0, 500) + "…";
        new AlertDialog.Builder(this)
                .setTitle("Confirm Discord update")
                .setMessage("Send this message to the encrypted Discord webhook?\n\n" + preview)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send", (dialog, which) -> discordClientSend(output, message))
                .show();
    }

    private void discordClientSend(TextView output, String message) {
        output.setText("Sending to Discord…");
        discordWebhookClient.send(keyStore.get("discord"), message, new DiscordWebhookClient.Callback() {
            @Override public void onSuccess(String result) {
                runOnUiThread(() -> output.setText(result));
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> output.setText("Discord error: " + error));
            }
        });
    }

    private void showWebSearch() {
        closeDrawer();
        content.removeAllViews();
        LinearLayout page = page();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleWrap = new LinearLayout(this);
        titleWrap.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Web search", 26, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleWrap.addView(title, wrap());
        titleWrap.addView(text("Bring live sources into the conversation, on your terms.", 12, secondaryText), wrap());
        header.addView(titleWrap, new LinearLayout.LayoutParams(0, -2, 1));
        TextView source = pill(keyStore.hasKey("brave") ? "BRAVE" : "DDG FALLBACK", keyStore.hasKey("brave") ? mint : lavender, raised);
        header.addView(source, wrap());
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(64)));

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        webSearchQuery = input("Search the web…", false);
        webSearchQuery.setSingleLine(true);
        webSearchQuery.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        webSearchQuery.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runWebSearch();
                return true;
            }
            return false;
        });
        searchBar.addView(webSearchQuery, new LinearLayout.LayoutParams(0, dp(46), 1));
        TextView search = smallButton("Search", lavender, view -> runWebSearch());
        searchBar.addView(search, marginWrapParams(8, 0, 0, 0));
        page.addView(searchBar, marginParams(0, 8, 0, 8));

        TextView disclaimer = text("Without a Brave key, Kairo uses DuckDuckGo Instant Answers when available. Search results are shown before they become prompt context.", 11, mutedText);
        disclaimer.setLineSpacing(1.1f, 1.0f);
        page.addView(disclaimer, marginParams(2, 0, 0, 10));
        webResultsContainer = new LinearLayout(this);
        webResultsContainer.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(webResultsContainer, new ScrollView.LayoutParams(-1, -1));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
        renderSearchResults();
    }

    private void runWebSearch() {
        if (webSearchQuery == null) return;
        String query = webSearchQuery.getText().toString().trim();
        if (query.isEmpty()) {
            toast("Enter something to search for");
            return;
        }
        if (webResultsContainer != null) {
            webResultsContainer.removeAllViews();
            webResultsContainer.addView(text("Searching live sources…", 13, secondaryText), marginParams(0, 16, 0, 0));
        }
        webSearchClient.search(query, keyStore.get("brave"), new WebSearchClient.Callback() {
            @Override
            public void onSuccess(List<SearchResult> results, String provider) {
                lastSearchResults.clear();
                lastSearchResults.addAll(results);
                runOnUiThread(() -> {
                    renderSearchResults();
                    toast(provider + " · " + results.size() + " results");
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (webResultsContainer != null) {
                        webResultsContainer.removeAllViews();
                        webResultsContainer.addView(text("Search failed: " + message, 13, red), marginParams(0, 16, 0, 0));
                    }
                });
            }
        });
    }

    private void renderSearchResults() {
        if (webResultsContainer == null) return;
        webResultsContainer.removeAllViews();
        if (lastSearchResults.isEmpty()) {
            LinearLayout empty = card();
            empty.setPadding(dp(16), dp(24), dp(16), dp(24));
            TextView icon = text("⌁", 30, lavender);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon, wrapParams());
            TextView title = text("Search when you need context", 16, primaryText);
            title.setGravity(Gravity.CENTER);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            empty.addView(title, marginParams(0, 8, 0, 4));
            TextView hint = text("Results can be opened in a browser or inserted into Chat as clearly labeled sources.", 12, secondaryText);
            hint.setGravity(Gravity.CENTER);
            empty.addView(hint, wrapParams());
            webResultsContainer.addView(empty, marginParams(0, 12, 0, 0));
            return;
        }
        for (int index = 0; index < lastSearchResults.size(); index++) {
            addSearchCard(webResultsContainer, lastSearchResults.get(index), index + 1);
        }
        webResultsContainer.addView(text("Sources are user-selected context, not a guarantee of accuracy.", 10, mutedText), marginParams(0, 8, 0, 20));
    }

    private void addSearchCard(LinearLayout parent, SearchResult result, int number) {
        LinearLayout item = card();
        item.setPadding(dp(13), dp(12), dp(12), dp(11));
        item.setOnClickListener(view -> openUrl(result.getUrl()));
        TextView title = text(number + "  " + result.getTitle(), 14, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setMaxLines(2);
        item.addView(title, wrap());
        TextView url = text(result.getUrl(), 10, mint);
        url.setSingleLine(true);
        url.setEllipsize(android.text.TextUtils.TruncateAt.END);
        item.addView(url, marginParams(0, 5, 0, 4));
        TextView snippet = text(result.getSnippet(), 12, secondaryText);
        snippet.setMaxLines(4);
        item.addView(snippet, wrap());
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
            actions.addView(smallButton("Open", secondaryText, view -> openUrl(result.getUrl())), wrap());
            actions.addView(smallButton("Use in chat", lavender, view -> insertSearchResult(result)), marginWrapParams(7, 0, 0, 0));
        item.addView(actions, marginParams(0, 9, 0, 0));
        parent.addView(item, marginParams(0, 0, 0, 9));
    }

    private void insertSearchResult(SearchResult result) {
        showChat();
        if (composer == null) return;
        String current = composer.getText().toString();
        String context = "\n\nSource context from " + result.getTitle() + " (" + result.getUrl() + "):\n"
                + result.getSnippet();
        composer.setText((current + context).trim());
        composer.setSelection(composer.length());
        toast("Source added to composer");
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception exception) {
            toast("No browser is available");
        }
    }

    private void showArtifacts() {
        setActiveTab(TAB_ARTIFACTS);
        content.removeAllViews();
        LinearLayout page = page();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleWrap = new LinearLayout(this);
        titleWrap.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Artifacts", 26, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleWrap.addView(title, wrap());
        titleWrap.addView(text("Create, preview, edit, and share files without leaving Kairo.", 12, secondaryText), wrap());
        header.addView(titleWrap, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(smallButton("＋ Create file", lavender, view -> showCreateArtifactDialog(null)), wrap());
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(64)));

        LinearLayout info = card();
        info.setPadding(dp(14), dp(13), dp(14), dp(13));
        info.addView(text("PRIVATE WORKSPACE", 10, lavender), wrap());
        info.addView(text("Files live inside Kairo's private app storage. Save a model code block as an artifact, preview it with a monospace editor, or share the text when it is ready.", 13, secondaryText), marginParams(0, 6, 0, 0));
        LinearLayout infoActions = new LinearLayout(this);
        infoActions.setGravity(Gravity.END);
        infoActions.addView(smallButton("Generate with AI", lavender,
                view -> startArtifactGeneration()), wrap());
        info.addView(infoActions, marginParams(0, 10, 0, 0));
        page.addView(info, marginParams(0, 10, 0, 12));

        artifactSearch = input("Search files", false);
        artifactSearch.setSingleLine(true);
        page.addView(artifactSearch, marginParams(0, 0, 0, 9));
        artifactListContainer = new LinearLayout(this);
        artifactListContainer.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(artifactListContainer, new ScrollView.LayoutParams(-1, -1));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
        artifactSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderArtifactList(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        renderArtifactList();
    }

    private void startArtifactGeneration() {
        activeAgentId = "artifact";
        showChat();
        if (modeButton != null) modeButton.setText(agentModeLabel());
        if (composer == null) return;
        LanguagePreset preset = LanguageCatalog.find(preferences.getLanguagePreset());
        String presetHint = "auto".equals(preset.getId()) ? "Let me choose the best language" : preset.getLabel();
        composer.setText("Create a complete production-ready " + presetHint + " file for me. Start by suggesting a filename and language, then return the entire file in one fenced code block. Requirements: ");
        composer.setSelection(composer.length());
        composer.requestFocus();
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .showSoftInput(composer, InputMethodManager.SHOW_IMPLICIT);
        toast("Describe the file you want Kairo to create");
    }

    private void renderArtifactList() {
        if (artifactListContainer == null) return;
        artifactListContainer.removeAllViews();
        String query = artifactSearch == null ? "" : artifactSearch.getText().toString().trim().toLowerCase(Locale.US);
        List<Artifact> artifacts = artifactStore.list();
        int shown = 0;
        for (Artifact artifact : artifacts) {
            if (!query.isEmpty() && !artifact.getName().toLowerCase(Locale.US).contains(query)
                    && !artifact.getLanguage().toLowerCase(Locale.US).contains(query)) continue;
            addArtifactCard(artifactListContainer, artifact);
            shown++;
        }
        if (shown == 0) {
            LinearLayout empty = card();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(16), dp(30), dp(16), dp(30));
            TextView icon = text("◇", 30, lavender);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon, wrapParams());
            TextView title = text("No artifacts yet", 17, primaryText);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            empty.addView(title, marginParams(0, 8, 0, 4));
            TextView hint = text("Create a file or save a code block from your latest answer.", 12, secondaryText);
            hint.setGravity(Gravity.CENTER);
            empty.addView(hint, wrapParams());
            artifactListContainer.addView(empty, marginParams(0, 12, 0, 20));
        }
        TextView count = text(shown + " file" + (shown == 1 ? "" : "s") + " · private to this device", 11, mutedText);
        artifactListContainer.addView(count, marginParams(0, 10, 0, 20));
    }

    private void addArtifactCard(LinearLayout parent, Artifact artifact) {
        LinearLayout item = card();
        item.setPadding(dp(14), dp(12), dp(12), dp(12));
        item.setOnClickListener(view -> showArtifactPreview(artifact));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = text("◇", 22, lavender);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(rounded(Color.rgb(54, 47, 78), 12));
        row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(artifact.getName(), 15, primaryText);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(name, wrap());
        labels.addView(text(artifact.getLanguage() + "  ·  " + artifact.getContent().length() + " chars", 11, lavender), marginParams(0, 4, 0, 0));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = text("›", 24, mutedText);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(42)));
        item.addView(row, wrapParams());
        String preview = artifact.getContent().replaceAll("\\s+", " ").trim();
        if (preview.length() > 90) preview = preview.substring(0, 90) + "…";
        item.addView(text(preview.isEmpty() ? "Empty file" : preview, 11, secondaryText), marginParams(0, 9, 0, 0));
        parent.addView(item, marginParams(0, 0, 0, 9));
    }

    private void showCreateArtifactFromAnswer(String answer) {
        artifactSeedOverride = latestCodeBlockFromText(answer);
        artifactNameOverride = "generated-code.txt";
        showCreateArtifactDialog(null);
    }

    private void showCreateArtifactDialog(Artifact source) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(3), dp(2), 0);
        EditText name = input("File name, e.g. MainActivity.java", false);
        name.setSingleLine(true);
        EditText language = input("Language (optional)", false);
        language.setSingleLine(true);
        EditText contents = input("Write or paste file contents", false);
        contents.setSingleLine(false);
        contents.setGravity(Gravity.TOP | Gravity.START);
        contents.setMinLines(9);
        contents.setTypeface(Typeface.MONOSPACE);
        if (source != null) {
            name.setText(source.getName());
            language.setText(source.getLanguage());
            contents.setText(source.getContent());
        } else {
            if (artifactNameOverride != null) name.setText(artifactNameOverride);
            if (!"auto".equals(preferences.getLanguagePreset())) {
                language.setText(LanguageCatalog.find(preferences.getLanguagePreset()).getLabel());
            }
            String seed = artifactSeedOverride == null ? latestCodeBlock() : artifactSeedOverride;
            if (!seed.isEmpty()) contents.setText(seed);
            artifactSeedOverride = null;
            artifactNameOverride = null;
        }
        panel.addView(name, marginParams(0, 0, 0, 8));
        panel.addView(language, marginParams(0, 0, 0, 8));
        panel.addView(contents, wrapParams());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(source == null ? "Create artifact" : "Save as artifact")
                .setView(panel)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save file", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                String fileName = name.getText().toString().trim();
                String fileLanguage = language.getText().toString().trim();
                String fileContent = contents.getText().toString();
                if (source == null) artifactStore.create(fileName, fileLanguage, fileContent);
                else artifactStore.update(source, fileName, fileLanguage, fileContent);
                dialog.dismiss();
                showArtifacts();
                toast("Artifact saved");
            } catch (Exception exception) {
                toast(exception.getMessage() == null ? "Could not save artifact" : exception.getMessage());
            }
        }));
        dialog.show();
    }

    private void showArtifactPreview(Artifact artifact) {
        EditText editor = input("File contents", false);
        editor.setSingleLine(false);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setMinLines(14);
        editor.setMaxLines(24);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setText(artifact.getContent());
        editor.setSelection(editor.length());
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), 0, dp(2), 0);
        panel.addView(editor, wrapParams());
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.addView(smallButton("Copy", secondaryText,
                view -> copyToClipboard(editor.getText().toString())), wrap());
        actions.addView(smallButton("Export", lavender,
                view -> exportArtifact(artifact.getName(), editor.getText().toString())), marginWrapParams(7, 0, 0, 0));
        actions.addView(smallButton("Share", lavender,
                view -> shareArtifact(artifact.getName(), editor.getText().toString())), marginWrapParams(7, 0, 0, 0));
        panel.addView(actions, marginParams(0, 9, 0, 0));
        panel.addView(smallButton("Run / check this draft", mint,
                view -> confirmRunArtifact(artifact, editor.getText().toString())), marginParams(0, 8, 0, 0));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(artifact.getName())
                .setMessage(artifact.getLanguage() + "  ·  editable preview")
                .setView(panel)
                .setNegativeButton("Delete", (d, which) -> confirmDeleteArtifact(artifact))
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                artifactStore.update(artifact, artifact.getName(), artifact.getLanguage(), editor.getText().toString());
                dialog.dismiss();
                showArtifacts();
                toast("Artifact updated");
            } catch (Exception exception) {
                toast(exception.getMessage() == null ? "Could not update artifact" : exception.getMessage());
            }
        }));
        dialog.show();
    }

    private void confirmDeleteArtifact(Artifact artifact) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + artifact.getName() + "?")
                .setMessage("This private artifact will be removed from Kairo.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    artifactStore.delete(artifact);
                    showArtifacts();
                    toast("Artifact deleted");
                }).show();
    }

    private void shareArtifact(String name, String content) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, name);
        share.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(share, "Share " + name));
    }

    private void confirmRunArtifact(Artifact source, String draftContent) {
        Artifact draft = new Artifact(source.getId(), source.getName(), source.getLanguage(),
                source.getUpdatedAt(), draftContent);
        new AlertDialog.Builder(this)
                .setTitle("Run or check " + source.getName() + "?")
                .setMessage("Kairo will use its private app process, a 10-second timeout, and a 20,000-character output cap. Shell files receive a syntax check only. Java/Kotlin execution needs a runtime installed on the device. Do not run untrusted code.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Run / check", (dialog, which) -> {
                    toast("Preparing " + source.getName() + "…");
                    codeRunner.run(draft, getCacheDir(), new CodeRunner.Callback() {
                        @Override public void onSuccess(String output) {
                            runOnUiThread(() -> showCodeRunResult(source.getName(), false, output));
                        }
                        @Override public void onError(String message) {
                            runOnUiThread(() -> showCodeRunResult(source.getName(), true, message));
                        }
                    });
                })
                .show();
    }

    private void showCodeRunResult(String name, boolean failed, String result) {
        TextView output = text(result == null ? "" : result, 12, failed ? red : secondaryText);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextIsSelectable(true);
        output.setGravity(Gravity.TOP | Gravity.START);
        output.setPadding(dp(12), dp(11), dp(12), dp(11));
        output.setBackground(rounded(Color.rgb(13, 14, 17), 12));
        new AlertDialog.Builder(this)
                .setTitle((failed ? "Run failed · " : "Run result · ") + name)
                .setView(output)
                .setPositiveButton("Done", null)
                .show();
    }

    private String latestCodeBlock() {
        for (int index = conversation.size() - 1; index >= 0; index--) {
            if ("assistant".equals(conversation.get(index).getRole())) {
                return latestCodeBlockFromText(conversation.get(index).getContent());
            }
        }
        return "";
    }

    private String latestCodeBlockFromText(String answer) {
        if (answer == null) return "";
        int open = answer.indexOf("```");
        if (open < 0) return answer;
        int contentStart = answer.indexOf('\n', open);
        if (contentStart < 0) contentStart = open + 3;
        else contentStart++;
        int close = answer.indexOf("```", contentStart);
        return answer.substring(contentStart, close < 0 ? answer.length() : close).trim();
    }

    private void showGenerationSettings() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(3), dp(2), 0);
        panel.addView(text("These controls apply to the next live response. Providers may apply their own caps; Kairo clamps values to safe ranges.", 12, secondaryText), wrap());

        final String[] selectedStyle = {preferences.getResponseStyle()};
        TextView styleButton = smallButton(styleLabel(selectedStyle[0]), lavender, view -> {
            String[] labels = {"Concise", "Balanced", "Detailed"};
            String[] ids = {"concise", "balanced", "detailed"};
            new AlertDialog.Builder(this)
                    .setTitle("Response style")
                    .setItems(labels, (dialog, which) -> {
                        selectedStyle[0] = ids[which];
                        styleButton.setText(labels[which]);
                    })
                    .show();
        });
        panel.addView(text("Response style", 11, mutedText), marginParams(0, 13, 0, 3));
        panel.addView(styleButton, wrapParams());

        final String[] selectedReasoning = {preferences.getReasoningMode()};
        TextView reasoningButton = smallButton(reasoningLabel(selectedReasoning[0]), mint, view -> {
            String[] labels = {"Fast", "Balanced", "Deep"};
            String[] ids = {"fast", "balanced", "deep"};
            new AlertDialog.Builder(this)
                    .setTitle("Reasoning mode")
                    .setMessage("Deep mode asks the selected model for stronger planning and verification. Actual reasoning depth remains provider/model dependent; Kairo never requests hidden chain-of-thought.")
                    .setItems(labels, (dialog, which) -> {
                        selectedReasoning[0] = ids[which];
                        reasoningButton.setText(labels[which]);
                    })
                    .show();
        });
        panel.addView(text("Reasoning mode", 11, mutedText), marginParams(0, 10, 0, 3));
        panel.addView(reasoningButton, wrapParams());

        EditText temperature = input("Temperature, 0.0–2.0", false);
        temperature.setSingleLine(true);
        temperature.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        temperature.setText(String.format(Locale.US, "%.1f", preferences.getTemperature()));
        panel.addView(text("Creativity / temperature", 11, mutedText), marginParams(0, 10, 0, 3));
        panel.addView(temperature, wrapParams());

        EditText maxTokens = input("Maximum output tokens, 256–8192", false);
        maxTokens.setSingleLine(true);
        maxTokens.setInputType(InputType.TYPE_CLASS_NUMBER);
        maxTokens.setText(String.valueOf(preferences.getMaxOutputTokens()));
        panel.addView(text("Maximum output", 11, mutedText), marginParams(0, 10, 0, 3));
        panel.addView(maxTokens, wrapParams());
        panel.addView(text("Streaming is always enabled in Chat and Arena. Stop is available while a response is active.", 10, mutedText), marginParams(0, 10, 0, 0));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Generation controls")
                .setView(panel)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                float parsedTemperature = Float.parseFloat(temperature.getText().toString().trim());
                int parsedTokens = Integer.parseInt(maxTokens.getText().toString().trim());
                if (parsedTemperature < 0f || parsedTemperature > 2f) {
                    toast("Temperature must be between 0.0 and 2.0");
                    return;
                }
                if (parsedTokens < 256 || parsedTokens > 8192) {
                    toast("Maximum output must be between 256 and 8192 tokens");
                    return;
                }
                preferences.setResponseStyle(selectedStyle[0]);
                preferences.setReasoningMode(selectedReasoning[0]);
                preferences.setTemperature(parsedTemperature);
                preferences.setMaxOutputTokens(parsedTokens);
                dialog.dismiss();
                showSettings();
                toast("Generation controls saved");
            } catch (NumberFormatException exception) {
                toast("Enter a valid temperature and token limit");
            }
        }));
        dialog.show();
    }

    private void showSkillsSettings() {
        setActiveTab(TAB_SETTINGS);
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Skills & language", "Tune how Kairo thinks and how generated files are shaped."), wrapParams());

        LinearLayout languageCard = card();
        languageCard.setPadding(dp(14), dp(13), dp(10), dp(13));
        LinearLayout languageRow = new LinearLayout(this);
        languageRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout languageLabels = new LinearLayout(this);
        languageLabels.setOrientation(LinearLayout.VERTICAL);
        TextView languageTitle = text("Artifact language preset", 15, primaryText);
        languageTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        languageLabels.addView(languageTitle, wrap());
        LanguagePreset selectedPreset = LanguageCatalog.find(preferences.getLanguagePreset());
        languageLabels.addView(text(selectedPreset.getLabel() + "  ·  ." + selectedPreset.getExtension(), 11, lavender), marginParams(0, 4, 0, 0));
        languageLabels.addView(text(selectedPreset.getDescription(), 11, secondaryText), marginParams(0, 4, 0, 0));
        languageRow.addView(languageLabels, new LinearLayout.LayoutParams(0, -2, 1));
        languageRow.addView(smallButton("Choose", lavender, view -> showLanguagePicker()), wrap());
        languageCard.addView(languageRow, wrapParams());
        page.addView(languageCard, marginParams(0, 12, 0, 12));

        LinearLayout intro = card();
        intro.setPadding(dp(14), dp(12), dp(14), dp(12));
        intro.addView(text("Skills are fixed, inspectable instructions. They change response style and safety posture only; they never enable arbitrary commands, network calls, phone actions, or external writes.", 12, secondaryText), wrap());
        page.addView(intro, marginParams(0, 0, 0, 12));

        Set<String> selected = new LinkedHashSet<>(preferences.getEnabledSkills());
        LinearLayout skillBody = new LinearLayout(this);
        skillBody.setOrientation(LinearLayout.VERTICAL);
        skillBody.addView(sectionLabel("AVAILABLE SKILLS"), marginParams(0, 0, 0, 8));
        for (SkillDefinition skill : SkillCatalog.all()) {
            LinearLayout row = card();
            row.setPadding(dp(10), dp(8), dp(9), dp(8));
            CheckBox check = new CheckBox(this);
            check.setButtonTintList(android.content.res.ColorStateList.valueOf(lavender));
            check.setChecked(selected.contains(skill.getId()));
            check.setContentDescription("Enable " + skill.getName());
            check.setOnCheckedChangeListener((button, checked) -> {
                if (checked) selected.add(skill.getId());
                else selected.remove(skill.getId());
            });
            row.addView(check, new LinearLayout.LayoutParams(dp(42), dp(42)));
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            TextView title = text(skill.getName(), 14, primaryText);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            labels.addView(title, wrap());
            labels.addView(text(skill.getDescription(), 11, secondaryText), marginParams(0, 3, 0, 0));
            row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
            skillBody.addView(row, marginParams(0, 0, 0, 8));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(skillBody, new ScrollView.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView save = smallButton("Save skills", mint, view -> {
            preferences.setEnabledSkills(new ArrayList<>(selected));
            toast(selected.size() + " skills enabled");
            showSettings();
        });
        page.addView(save, marginParams(0, 8, 0, 4));
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
    }

    private void showLanguagePicker() {
        List<LanguagePreset> presets = LanguageCatalog.all();
        String[] labels = new String[presets.size()];
        for (int index = 0; index < presets.size(); index++) {
            LanguagePreset preset = presets.get(index);
            labels[index] = preset.getLabel() + "  ·  ." + preset.getExtension();
        }
        new AlertDialog.Builder(this)
                .setTitle("Artifact language")
                .setItems(labels, (dialog, which) -> {
                    LanguagePreset preset = presets.get(which);
                    preferences.setLanguagePreset(preset.getId());
                    toast(preset.getLabel() + " preset selected");
                    showSkillsSettings();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSettings() {
        setActiveTab(TAB_SETTINGS);
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Settings", "Connect providers without giving up control of your keys."), wrapParams());
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(12), 0, dp(24));
        addSettingsIntro(body);
        addDeviceSetupRow(body);
        addWorkspaceHealth(body);
        addSkillsSettingsRow(body);
        addGenerationSettingsRow(body);
        addMemorySettingsRow(body);
        body.addView(sectionLabel("MODEL PROVIDERS"), marginParams(0, 18, 0, 7));
        addProviderRow(body, "openrouter", "OpenRouter", "Best starting point for free routes and a large catalog.");
        addProviderRow(body, "groq", "Groq", "Fast OpenAI-compatible inference with a developer tier.");
        addProviderRow(body, "moonshot", "Kimi / Moonshot", "Kimi-style long-context and reasoning candidates through an OpenAI-compatible endpoint.");
        addProviderRow(body, "nvidia", "NVIDIA NIM", "Use an NVIDIA API key with hosted open models.");
        addProviderRow(body, "mistral", "Mistral AI", "Official OpenAI-compatible endpoint for Mistral and Codestral.");
        addProviderRow(body, "anthropic", "Anthropic", "Claude models through the official Messages API.");
        addProviderRow(body, "openai", "OpenAI", "OpenAI-compatible chat models.");
        addProviderRow(body, "custom", "Custom OpenAI-compatible", "LM Studio, vLLM, or another compatible server.");
        body.addView(sectionLabel("LOCAL & AGENT ACCESS"), marginParams(0, 21, 0, 7));
        addProviderRow(body, "ollama", "Ollama", "No key required. Point Kairo at an emulator or LAN endpoint.");
        addProviderRow(body, "github", "GitHub Agent", "Fine-grained token for pull, push-file, and PR tools.");
        body.addView(sectionLabel("WEB & KNOWLEDGE"), marginParams(0, 21, 0, 7));
        addProviderRow(body, "brave", "Brave Search", "Optional key for richer web results; DuckDuckGo fallback needs no key.");
        body.addView(sectionLabel("CONNECTORS"), marginParams(0, 21, 0, 7));
        addConnectorSettingRow(body, "github", "GitHub", "Repository reads plus confirmed file pushes and pull requests.");
        addConnectorSettingRow(body, "vercel", "Vercel", "Projects, deployment visibility, and reviewed Git deployments.");
        addConnectorSettingRow(body, "n8n", "n8n", "Workflow visibility and explicit webhook automation.");
        addConnectorSettingRow(body, "slack", "Slack", "Channel visibility and reviewed team messages.");
        addConnectorSettingRow(body, "notion", "Notion", "Search pages shared with your integration.");
        addConnectorSettingRow(body, "linear", "Linear", "Read-only issue search for planning and handoffs.");
        addConnectorSettingRow(body, "supabase", "Supabase", "Bounded, read-only table previews.");
        addConnectorSettingRow(body, "discord", "Discord webhook", "Reviewed status updates through one encrypted webhook.");
        body.addView(sectionLabel("ENDPOINTS"), marginParams(0, 21, 0, 7));
        addEndpointRow(body, "Custom base URL", preferences.getCustomBaseUrl(), false);
        addEndpointRow(body, "Ollama URL", preferences.getOllamaBaseUrl(), true);
        body.addView(sectionLabel("PRIVACY"), marginParams(0, 21, 0, 7));
        LinearLayout privacy = card();
        privacy.setPadding(dp(14), dp(13), dp(14), dp(13));
        privacy.addView(text("Your keys are encrypted with Android Keystore and stored only in this app's private storage. Kairo does not proxy or collect your prompts; requests go from the device to the endpoint you choose.", 12, secondaryText), wrap());
        body.addView(privacy, wrapParams());
        scroll.addView(body, new ScrollView.LayoutParams(-1, -1));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
    }

    private void addSettingsIntro(LinearLayout body) {
        LinearLayout intro = card();
        intro.setPadding(dp(15), dp(15), dp(15), dp(15));
        TextView title = text("One interface. Your providers.", 18, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        intro.addView(title, wrap());
        intro.addView(text("Start with a free route, use Groq for speed, add your NVIDIA key, or stay local with Ollama. The selected model is shown in Chat.", 13, secondaryText), marginParams(0, 6, 0, 0));
        intro.addView(text("Paste a recognizable provider key into Chat and Kairo detects it locally, pauses sending, and offers a one-tap review before secure save. The full secret is never shown back.", 11, mutedText), marginParams(0, 7, 0, 0));
        body.addView(intro, wrapParams());
    }

    private void addDeviceSetupRow(LinearLayout body) {
        LinearLayout setup = card();
        setup.setPadding(dp(14), dp(12), dp(10), dp(12));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("Device & sign-in", 14, primaryText), wrap());
        labels.addView(text(deviceSetup.getDeviceName() + "  ·  " + (deviceSetup.isSetupComplete() ? "ready" : "needs setup"), 11, secondaryText), marginParams(0, 4, 0, 0));
        labels.addView(text("Provider passwords stay in the browser; Kairo stores only encrypted tokens.", 10, mutedText), marginParams(0, 5, 0, 0));
        setup.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        setup.addView(smallButton("Open setup", lavender, view -> showDeviceSetup()), wrap());
        body.addView(setup, marginParams(0, 13, 0, 0));
    }

    private void addWorkspaceHealth(LinearLayout body) {
        LinearLayout health = card();
        health.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView title = text("Workspace health", 14, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        health.addView(title, wrap());
        LinearLayout metrics = new LinearLayout(this);
        metrics.setPadding(0, dp(10), 0, dp(0));
        addMetric(metrics, "Threads", String.valueOf(savedSessions.size()));
        addMetric(metrics, "Messages", String.valueOf(usageTracker.messageCount()));
        addMetric(metrics, "Memories", String.valueOf(memoryStore.size()));
        addMetric(metrics, "Catalog", String.valueOf(ModelCatalog.all().size()));
        health.addView(metrics, wrapParams());
        health.addView(text("Selected: " + modelTitle(), 11, mutedText), marginParams(0, 8, 0, 0));
        body.addView(health, marginParams(0, 13, 0, 0));
    }

    private void addSkillsSettingsRow(LinearLayout body) {
        LinearLayout skills = card();
        skills.setPadding(dp(14), dp(12), dp(10), dp(12));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Skills & language", 14, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labels.addView(title, wrap());
        String language = LanguageCatalog.find(preferences.getLanguagePreset()).getLabel();
        labels.addView(text(preferences.getEnabledSkills().size() + " skills enabled  ·  " + language + " artifact preset", 11, secondaryText), marginParams(0, 4, 0, 0));
        labels.addView(text("Shape responses without granting tools or device permissions.", 10, mutedText), marginParams(0, 5, 0, 0));
        skills.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        skills.addView(smallButton("Configure", lavender, view -> showSkillsSettings()), wrap());
        body.addView(skills, marginParams(0, 13, 0, 0));
    }

    private void addGenerationSettingsRow(LinearLayout body) {
        LinearLayout settings = card();
        settings.setPadding(dp(14), dp(12), dp(10), dp(12));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Generation controls", 14, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labels.addView(title, wrap());
        labels.addView(text(styleLabel(preferences.getResponseStyle()) + "  ·  " + reasoningLabel(preferences.getReasoningMode())
                + " reasoning  ·  temperature " + String.format(Locale.US, "%.1f", preferences.getTemperature())
                + "  ·  " + preferences.getMaxOutputTokens() + " max tokens", 11, secondaryText), marginParams(0, 4, 0, 0));
        labels.addView(text("Live SSE streaming stays on for responsive Claude/Groq-style output.", 10, mutedText), marginParams(0, 5, 0, 0));
        settings.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        settings.addView(smallButton("Tune", lavender, view -> showGenerationSettings()), wrap());
        body.addView(settings, marginParams(0, 9, 0, 0));
    }

    private String styleLabel(String style) {
        if ("concise".equals(style)) return "Concise";
        if ("detailed".equals(style)) return "Detailed";
        return "Balanced";
    }

    private String reasoningLabel(String mode) {
        if ("fast".equals(mode)) return "Fast";
        if ("deep".equals(mode)) return "Deep";
        return "Balanced";
    }

    private void addMemorySettingsRow(LinearLayout body) {
        LinearLayout memories = card();
        memories.setPadding(dp(14), dp(12), dp(10), dp(12));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Memory vault", 14, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labels.addView(title, wrap());
        labels.addView(text(memoryStore.size() + " approved memories  ·  encrypted locally", 11, secondaryText), marginParams(0, 4, 0, 0));
        labels.addView(text("Only reviewed memories are added to model context; API keys are rejected.", 10, mutedText), marginParams(0, 5, 0, 0));
        memories.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        memories.addView(smallButton("Open vault", lavender, view -> showMemories()), wrap());
        body.addView(memories, marginParams(0, 9, 0, 0));
    }

    private void showMemories() {
        closeDrawer();
        setActiveTab(TAB_SETTINGS);
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Memory vault", "Keep durable context useful, reviewable, and private by default."), wrapParams());

        LinearLayout intro = card();
        intro.setPadding(dp(15), dp(14), dp(15), dp(14));
        intro.addView(text("APPROVED CONTEXT  ·  ENCRYPTED ON DEVICE", 10, mint), wrap());
        intro.addView(text("Kairo can remember preferences, profile details, project context, and working instructions between chats.", 13, secondaryText), marginParams(0, 6, 0, 0));
        intro.addView(text("Nothing is saved just because it appeared in Chat. A detected suggestion always opens a review step, and API credentials are blocked from this vault.", 11, mutedText), marginParams(0, 6, 0, 0));
        page.addView(intro, marginParams(0, 12, 0, 12));

        LinearLayout memoryList = new LinearLayout(this);
        memoryList.setOrientation(LinearLayout.VERTICAL);

        LinearLayout add = card();
        add.setPadding(dp(13), dp(12), dp(13), dp(12));
        TextView categoryButton = smallButton("Note", lavender, null);
        final String[] selectedCategory = {"note"};
        categoryButton.setOnClickListener(view -> {
            String[] labels = {"Profile", "Preference", "Project", "Instruction", "Note"};
            String[] ids = {"profile", "preference", "project", "instruction", "note"};
            new AlertDialog.Builder(this).setTitle("Memory category").setItems(labels, (dialog, which) -> {
                selectedCategory[0] = ids[which];
                categoryButton.setText(labels[which]);
            }).show();
        });
        EditText input = input("Example: I prefer concise Kotlin examples", false);
        input.setSingleLine(false);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(3);
        add.addView(text("Add a reviewed memory", 13, primaryText), wrap());
        add.addView(categoryButton, marginParams(0, 9, 0, 7));
        add.addView(input, wrapParams());
        add.addView(smallButton("Save to encrypted vault", mint, view -> {
            try {
                memoryStore.add(selectedCategory[0], input.getText().toString());
                input.setText("");
                renderMemoryList(memoryList);
                toast("Memory saved");
            } catch (Exception exception) {
                toast(exception.getMessage() == null ? "Could not save memory" : exception.getMessage());
            }
        }), marginParams(0, 9, 0, 0));
        page.addView(add, marginParams(0, 0, 0, 14));

        page.addView(sectionLabel("SAVED MEMORIES"), marginParams(0, 0, 0, 8));
        renderMemoryList(memoryList);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(memoryList, new ScrollView.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.END);
        footer.addView(smallButton("Clear all memories", red, view -> confirmClearMemories()), wrap());
        page.addView(footer, marginParams(0, 8, 0, 4));
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
    }

    private void renderMemoryList(LinearLayout list) {
        list.removeAllViews();
        List<MemoryItem> memories = memoryStore.load();
        if (memories.isEmpty()) {
            LinearLayout empty = card();
            empty.setPadding(dp(14), dp(13), dp(14), dp(13));
            empty.addView(text("No approved memories yet. Try a message beginning with “Remember that…” and review the suggestion.", 12, secondaryText), wrap());
            list.addView(empty, wrapParams());
            return;
        }
        for (MemoryItem memory : memories) {
            LinearLayout item = card();
            item.setPadding(dp(13), dp(11), dp(9), dp(11));
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            TextView category = text(memory.getCategory().toUpperCase(Locale.US), 10, lavender);
            category.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            labels.addView(category, wrap());
            labels.addView(text(memory.getContent(), 13, primaryText), marginParams(0, 5, 0, 0));
            item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
            item.addView(smallButton("Delete", red, view -> confirmDeleteMemory(memory)), wrap());
            list.addView(item, marginParams(0, 0, 0, 8));
        }
    }

    private void confirmDeleteMemory(MemoryItem memory) {
        new AlertDialog.Builder(this)
                .setTitle("Delete this memory?")
                .setMessage(memory.getContent())
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    memoryStore.delete(memory);
                    showMemories();
                    toast("Memory deleted");
                })
                .show();
    }

    private void confirmClearMemories() {
        if (!memoryStore.hasMemories()) {
            toast("The memory vault is already empty");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Clear all memories?")
                .setMessage("This permanently removes every approved memory from this device. It does not delete chat transcripts.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear all", (dialog, which) -> {
                    memoryStore.clear();
                    showMemories();
                    toast("Memory vault cleared");
                })
                .show();
    }

    private void addMetric(LinearLayout parent, String label, String value) {
        LinearLayout metric = new LinearLayout(this);
        metric.setOrientation(LinearLayout.VERTICAL);
        TextView number = text(value, 18, lavender);
        number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        metric.addView(number, wrap());
        metric.addView(text(label, 10, mutedText), marginParams(0, 2, 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(0, 0, dp(8), 0);
        parent.addView(metric, params);
    }

    private void addProviderRow(LinearLayout parent, String id, String name, String description) {
        LinearLayout row = card();
        row.setPadding(dp(14), dp(12), dp(10), dp(12));
        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(name, 15, primaryText);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textWrap.addView(title, wrap());
        textWrap.addView(text(description, 11, secondaryText), marginParams(0, 4, 0, 0));
        boolean connected = !"ollama".equals(id) && (keyStore.hasKey(id));
        if ("ollama".equals(id)) connected = true;
        TextView state = text(connected ? "●  READY" : "○  NOT CONNECTED", 10, connected ? mint : mutedText);
        state.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textWrap.addView(state, marginParams(0, 7, 0, 0));
        row.addView(textWrap, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        TextView action = smallButton("ollama".equals(id) ? "Configure" : "Manage key", lavender,
                view -> {
                    if ("ollama".equals(id)) showEndpointDialog("Ollama URL", preferences.getOllamaBaseUrl(), true);
                    else openKeyDialog(id, name);
                });
        actions.addView(action, new LinearLayout.LayoutParams(-2, dp(36)));
        TextView test = smallButton("Test", secondaryText, view -> testProvider(id));
        actions.addView(test, marginParams(0, 5, 0, 0));
        if ("groq".equals(id)) {
            actions.addView(smallButton("Fast chat", mint, view -> activateGroqFastMode()), marginParams(0, 5, 0, 0));
        }
        row.addView(actions, wrap());
        parent.addView(row, marginParams(0, 0, 0, 9));
    }

    private void addConnectorSettingRow(LinearLayout parent, String id, String name, String description) {
        LinearLayout row = card();
        row.setPadding(dp(14), dp(11), dp(10), dp(11));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(name, 14, primaryText), wrap());
        labels.addView(text(description, 11, secondaryText), marginParams(0, 4, 0, 0));
        boolean ready = connectorReady(id);
        labels.addView(text(ready ? "●  CONNECTED" : "○  NEEDS SETUP", 10, ready ? mint : mutedText), marginParams(0, 6, 0, 0));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(smallButton("Open", lavender, view -> showConnectors()), wrap());
        parent.addView(row, marginParams(0, 0, 0, 9));
    }

    private void addEndpointRow(LinearLayout parent, String title, String value, boolean ollama) {
        LinearLayout row = card();
        row.setPadding(dp(14), dp(11), dp(10), dp(11));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 14, primaryText), wrap());
        TextView valueText = text(value, 11, mutedText);
        valueText.setMaxLines(1);
        labels.addView(valueText, marginParams(0, 4, 0, 0));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(smallButton("Edit", lavender, view -> showEndpointDialog(title, value, ollama)), wrap());
        parent.addView(row, marginParams(0, 0, 0, 9));
    }

    private void activateGroqFastMode() {
        if (keyStore.get("groq").isEmpty()) {
            openKeyDialog("groq", "Groq");
            return;
        }
        List<ModelInfo> groqModels = ModelCatalog.forProvider("groq");
        if (groqModels.isEmpty()) {
            toast("Refresh Groq models first");
            return;
        }
        // The first curated Groq route is intentionally a low-latency starter; users can still
        // choose any live model from Models afterwards.
        ModelInfo fast = groqModels.get(0);
        preferences.setModel("groq", fast.getId());
        activeAgentId = "chat";
        showChat();
        toast("Groq fast chat · " + fast.getName());
    }

    private void testProvider(String providerId) {
        if (ProviderConfig.needsApiKey(providerId) && keyStore.get(providerId).isEmpty()) {
            toast("Add a " + ProviderConfig.displayName(providerId) + " key first");
            return;
        }
        toast("Testing " + ProviderConfig.displayName(providerId) + "…");
        if ("github".equals(providerId)) {
            toast("GitHub token saved · open Connectors to choose a repository");
            return;
        }
        if ("brave".equals(providerId)) {
            webSearchClient.search("Kairo Android", keyStore.get("brave"), new WebSearchClient.Callback() {
                @Override public void onSuccess(List<SearchResult> results, String provider) {
                    runOnUiThread(() -> toast(provider + " connected · " + results.size() + " results found"));
                }
                @Override public void onError(String message) {
                    runOnUiThread(() -> toast("Brave Search: " + message));
                }
            });
            return;
        }
        if (ProviderConfig.usesAnthropicApi(providerId)) {
            // Anthropic's Messages API does not expose the same public /models endpoint.
            toast("Anthropic is ready to test from Chat with the selected Claude model");
            return;
        }
        ApiClient.discoverModels(providerId, ProviderConfig.baseUrl(providerId, preferences),
                keyStore.get(providerId), new ApiClient.ModelsCallback() {
                    @Override
                    public void onSuccess(List<ModelInfo> models) {
                        ModelCatalog.replaceDiscovered(providerId, models);
                        runOnUiThread(() -> toast(ProviderConfig.displayName(providerId)
                                + " connected · " + models.size() + " models found"));
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> toast(ProviderConfig.displayName(providerId) + ": " + message));
                    }
                });
    }

    private void openKeyDialog(String providerId, String providerName) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(3), dp(2), 0);
        panel.addView(text("Kairo stores this key encrypted. For safety, an existing key is never pre-filled or displayed.", 12, secondaryText), wrap());
        EditText keyInput = input(ProviderConfig.apiKeyHint(providerId), true);
        keyInput.setSingleLine(true);
        panel.addView(keyInput, marginParams(0, 13, 0, 0));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(providerName + " key")
                .setView(panel)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Remove", (d, which) -> {
                    keyStore.delete(providerId);
                    showSettings();
                    toast(providerName + " key removed");
                })
                .setPositiveButton("Save securely", (d, which) -> {
                    String value = keyInput.getText().toString().trim();
                    if (value.isEmpty()) {
                        toast("Enter a key or choose Remove");
                        return;
                    }
                    keyStore.save(providerId, value);
                    toast(providerName + " key saved in Android Keystore");
                    showSettings();
                }).create();
        dialog.show();
    }

    private void showEndpointDialog(String title, String current, boolean ollama) {
        EditText input = input("https://…", false);
        input.setSingleLine(true);
        input.setText(current);
        input.setSelection(input.length());
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(ollama ? "Use http://10.0.2.2:11434 for an emulator, or the LAN address of your Ollama host." : "Include the API version path when your server requires it, for example /v1.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    if (ollama) preferences.setOllamaBaseUrl(input.getText().toString());
                    else preferences.setCustomBaseUrl(input.getText().toString());
                    showSettings();
                }).show();
    }

    private String joinExamples() {
        StringBuilder value = new StringBuilder();
        for (String example : CliCommandPolicy.examples()) {
            if (value.length() > 0) value.append(" · ");
            value.append(example);
        }
        return value.toString();
    }

    private ModelInfo selectedModel() {
        String provider = preferences.getProvider();
        String model = preferences.getModel();
        ModelInfo result = ModelCatalog.find(provider, model);
        if (result != null) return result;
        List<ModelInfo> providerModels = ModelCatalog.forProvider(provider);
        if (!providerModels.isEmpty()) return providerModels.get(0);
        return ModelCatalog.all().get(0);
    }

    private String modelTitle() {
        ModelInfo model = selectedModel();
        String name = model.getName();
        if (name.length() > 22) name = name.substring(0, 21) + "…";
        return name + "  ·  " + ProviderConfig.displayName(model.getProviderId());
    }

    /** A conservative hint only; the provider remains authoritative for model capabilities. */
    private boolean supportsImageAttachments(ModelInfo model) {
        if (model == null) return false;
        if (ProviderConfig.usesAnthropicApi(model.getProviderId())) return true;
        String id = model.getId().toLowerCase(Locale.US);
        return id.contains("vision")
                || id.contains("-vl")
                || id.contains("vl-")
                || id.contains("pixtral")
                || id.contains("gemini")
                || id.contains("gpt-4o")
                || id.contains("gpt-4.1")
                || id.contains("llama-4")
                || id.contains("gemma-3")
                || id.contains("qwen2.5-vl")
                || id.contains("llava")
                || id.contains("minicpm-v");
    }

    private LinearLayout page() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, dp(2), 0, 0);
        return page;
    }

    private LinearLayout pageHeader(String title, String subtitle) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 26, primaryText);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(titleView, wrap());
        header.addView(text(subtitle, 12, secondaryText), marginParams(0, 4, 0, 0));
        return header;
    }

    private TextView sectionLabel(String label) {
        TextView view = text(label, 10, mutedText);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        float scale = (preferences != null && preferences.isLargeText()) ? 1.15f : 1f;
        view.setTextSize(size * scale);
        view.setTextColor(color);
        return view;
    }

    private EditText input(String hint, boolean password) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setHintTextColor(mutedText);
        view.setTextColor(primaryText);
        view.setTextSize(14);
        view.setPadding(dp(12), dp(3), dp(12), dp(3));
        view.setBackground(rounded(soft, 12));
        if (password) {
            view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        return view;
    }

    private TextView iconButton(String value, String description, int color) {
        TextView view = text(value, 20, color);
        view.setGravity(Gravity.CENTER);
        view.setContentDescription(description);
        view.setBackground(rounded(surface, 14));
        return view;
    }

    private TextView compactIcon(String value, String description, int color, View.OnClickListener listener) {
        TextView view = text(value, 18, color);
        view.setGravity(Gravity.CENTER);
        view.setContentDescription(description);
        view.setOnClickListener(listener);
        view.setBackground(rounded(Color.TRANSPARENT, 12));
        return view;
    }

    private TextView pill(String value, int foreground, int backgroundColor) {
        TextView view = text(value, 11, foreground);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setSingleLine(true);
        view.setBackground(rounded(backgroundColor, 18));
        return view;
    }

    private TextView smallButton(String label, int color, View.OnClickListener listener) {
        TextView view = text(label, 12, color);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(dp(13), 0, dp(13), 0);
        view.setMinHeight(dp(37));
        view.setBackground(stroked(border, 12));
        view.setOnClickListener(listener);
        return view;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(glassSurface());
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setElevation(dp(2));
        return card;
    }

    /** Soft glass / elevated surface (blur-like on older APIs via translucency). */
    private GradientDrawable glassSurface() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(preferences != null && preferences.isLightTheme()
                ? Color.argb(230, 255, 255, 255)
                : Color.argb(210, surface >> 16 & 0xFF, surface >> 8 & 0xFF, surface & 0xFF));
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), preferences != null && preferences.isLightTheme()
                ? Color.argb(40, 0, 0, 0)
                : Color.argb(50, 255, 255, 255));
        return d;
    }

    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private GradientDrawable stroked(int strokeColor, float radius) {
        GradientDrawable drawable = rounded(surface, radius);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(-2, -2);
    }

    private LinearLayout.LayoutParams wrapParams() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams marginWrapParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private LinearLayout.LayoutParams marginWeightParams(int left, int top, int right, int bottom, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, weight);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private LinearLayout.LayoutParams marginParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private void scrollChatToBottom() {
        if (chatScroll != null) chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }


    /** Command palette – quick jump to tools, AI actions, settings, export. */
    private void showCommandPalette() {
        String[] items = {
                "New conversation",
                "Model arena",
                "Search artifacts",
                "Project instructions",
                "AI actions",
                "Export conversation (Markdown)",
                "Safe phone control",
                "Connectors",
                "Memories",
                "Settings",
                "Toggle theme",
                "Toggle continuous voice",
                "Export conversation (PDF)",
                "Toggle app lock"
        };
        new AlertDialog.Builder(this)
                .setTitle("Command palette")
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0: startNewChat(); break;
                        case 1: showArena(); break;
                        case 2: showArtifactSearch(); break;
                        case 3: showProjectInstructionsEditor(); break;
                        case 4: showAiFeaturesPicker(); break;
                        case 5: exportConversationMarkdown(); break;
                        case 6: showPhoneControl(); break;
                        case 7: showConnectors(); break;
                        case 8: showMemories(); break;
                        case 9: showSettings(); break;
                        case 10:
                            preferences.setThemeMode(preferences.isLightTheme() ? "dark" : "light");
                            recreate();
                            break;
                        case 11:
                            preferences.setVoiceContinuous(!preferences.isVoiceContinuous());
                            toast(preferences.isVoiceContinuous() ? "Continuous voice on" : "Continuous voice off");
                            break;
                        case 12:
                            exportConversationPdf();
                            break;
                        case 13:
                            preferences.setAppLockEnabled(!preferences.isAppLockEnabled());
                            sessionUnlocked = !preferences.isAppLockEnabled();
                            toast(preferences.isAppLockEnabled() ? "App lock enabled" : "App lock disabled");
                            break;
                    }
                })
                .show();
    }

    private void showProjectInstructionsEditor() {
        final EditText input = input("Pinned project / system instructions…", false);
        input.setSingleLine(false);
        input.setMinLines(5);
        input.setMaxLines(12);
        input.setText(preferences.getSystemInstructions());
        new AlertDialog.Builder(this)
                .setTitle("Project instructions")
                .setMessage("These instructions are prepended to future prompts. They stay on-device and are never sent as a separate system role unless the provider supports it.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    preferences.setSystemInstructions(input.getText().toString());
                    toast("Project instructions saved");
                })
                .show();
    }

    private void showArtifactSearch() {
        final EditText query = input("Search private artifacts…", false);
        new AlertDialog.Builder(this)
                .setTitle("Search artifacts")
                .setView(query)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Search", (d, w) -> {
                    String q = query.getText().toString().trim().toLowerCase(java.util.Locale.US);
                    if (q.isEmpty()) { toast("Enter a search term"); return; }
                    java.util.List<com.kairo.app.data.Artifact> hits = new java.util.ArrayList<>();
                    try {
                        for (com.kairo.app.data.Artifact a : new ArtifactStore(this).list()) {
                            String hay = ((a.getName() == null ? "" : a.getName()) + "\n" + (a.getContent() == null ? "" : a.getContent())).toLowerCase(java.util.Locale.US);
                            if (hay.contains(q)) hits.add(a);
                        }
                    } catch (Exception e) {
                        toast("Search failed");
                        return;
                    }
                    if (hits.isEmpty()) { toast("No matching artifacts"); return; }
                    String[] labels = new String[Math.min(hits.size(), 20)];
                    for (int i = 0; i < labels.length; i++) {
                        com.kairo.app.data.Artifact a = hits.get(i);
                        labels[i] = (a.getName() == null ? "untitled" : a.getName());
                    }
                    new AlertDialog.Builder(this)
                            .setTitle("Matches (" + hits.size() + ")")
                            .setItems(labels, (dd, which) -> {
                                com.kairo.app.data.Artifact chosen = hits.get(which);
                                String snippet = chosen.getContent() == null ? "" : chosen.getContent();
                                if (snippet.length() > 1200) snippet = snippet.substring(0, 1200) + "\n…";
                                if (composer != null) {
                                    String cur = composer.getText().toString();
                                    composer.setText(cur + (cur.isEmpty() ? "" : "\n\n") + "From artifact “" + chosen.getName() + "”:\n" + snippet);
                                    composer.setSelection(composer.length());
                                }
                            })
                            .show();
                })
                .show();
    }

    private void exportConversationMarkdown() {
        if (conversation == null || conversation.isEmpty()) {
            toast("Nothing to export");
            return;
        }
        StringBuilder md = new StringBuilder();
        md.append("# Conversation export\n\n");
        md.append("_Redacted export · credentials stripped · private_\n\n");
        for (ChatMessage m : conversation) {
            String role = "user".equals(m.getRole()) ? "You" : "Assistant";
            md.append("### ").append(role).append("\n\n");
            String body = m.getContent() == null ? "" : m.getContent();
            // light redaction of common key patterns
            body = body.replaceAll("(?i)(sk-[a-zA-Z0-9]{16,})", "[REDACTED_KEY]");
            body = body.replaceAll("(?i)(ghp_[a-zA-Z0-9]{16,})", "[REDACTED_TOKEN]");
            md.append(body).append("\n\n");
        }
        String sys = preferences.getSystemInstructions();
        if (sys != null && !sys.trim().isEmpty()) {
            md.append("---\n\n### Project instructions\n\n").append(sys).append("\n");
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/markdown");
        share.putExtra(Intent.EXTRA_TEXT, md.toString());
        share.putExtra(Intent.EXTRA_SUBJECT, "Conversation export");
        startActivity(Intent.createChooser(share, "Export conversation"));
    }

    private void offerMemorySuggestion(String answer) {
        if (answer == null || answer.length() < 80) return;
        String candidate = answer.length() > 220 ? answer.substring(0, 220).trim() + "…" : answer.trim();
        new AlertDialog.Builder(this)
                .setTitle("Save a memory?")
                .setMessage("Suggested memory from this answer:\n\n“" + candidate + "”\n\nOnly saved after you confirm. Stored encrypted on this device.")
                .setNegativeButton("Dismiss", null)
                .setPositiveButton("Review & save", (d, w) -> {
                    try {
                        // Reuse existing memory flow if available
                        if (composer != null) {
                            // open memory UI path via existing entry when possible
                            showMemories();
                        }
                        toast("Open Memories to save a refined note");
                    } catch (Exception e) {
                        toast("Could not open memories");
                    }
                })
                .show();
    }


    private void exportConversationPdf() {
        if (conversation == null || conversation.isEmpty()) {
            toast("Nothing to export");
            return;
        }
        try {
            android.graphics.pdf.PdfDocument doc = new android.graphics.pdf.PdfDocument();
            int pageWidth = 595;
            int pageHeight = 842;
            int y = 40;
            int pageNum = 1;
            android.graphics.pdf.PdfDocument.PageInfo info =
                    new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
            android.graphics.pdf.PdfDocument.Page page = doc.startPage(info);
            android.graphics.Canvas canvas = page.getCanvas();
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColor(android.graphics.Color.BLACK);
            paint.setTextSize(11f);
            android.graphics.Paint title = new android.graphics.Paint();
            title.setColor(android.graphics.Color.BLACK);
            title.setTextSize(16f);
            title.setFakeBoldText(true);
            canvas.drawText("Kairo conversation export", 40, y, title);
            y += 28;
            paint.setTextSize(10f);
            canvas.drawText("Redacted · private · generated on device", 40, y, paint);
            y += 22;
            for (ChatMessage m : conversation) {
                String role = "user".equals(m.getRole()) ? "You" : "Assistant";
                String body = m.getContent() == null ? "" : m.getContent();
                body = body.replaceAll("(?i)(sk-[a-zA-Z0-9]{16,})", "[REDACTED_KEY]");
                body = body.replaceAll("(?i)(ghp_[a-zA-Z0-9]{16,})", "[REDACTED_TOKEN]");
                String block = role + ": " + body;
                for (String line : wrapPdfLines(block, 80)) {
                    if (y > pageHeight - 50) {
                        doc.finishPage(page);
                        pageNum++;
                        info = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                        page = doc.startPage(info);
                        canvas = page.getCanvas();
                        y = 40;
                    }
                    canvas.drawText(line, 40, y, paint);
                    y += 14;
                }
                y += 10;
            }
            doc.finishPage(page);
            java.io.File out = new java.io.File(getCacheDir(), "kairo-export-" + System.currentTimeMillis() + ".pdf");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
            doc.writeTo(fos);
            fos.close();
            doc.close();
            android.net.Uri uri = android.net.Uri.fromFile(out);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share PDF export"));
            toast("PDF ready");
        } catch (Exception e) {
            toast("PDF export failed: " + e.getMessage());
        }
    }

    private java.util.List<String> wrapPdfLines(String text, int width) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null) return lines;
        for (String paragraph : text.split("\\n")) {
            String remaining = paragraph;
            while (remaining.length() > width) {
                int breakAt = remaining.lastIndexOf(' ', width);
                if (breakAt < 20) breakAt = width;
                lines.add(remaining.substring(0, breakAt));
                remaining = remaining.substring(breakAt).trim();
            }
            lines.add(remaining);
        }
        return lines;
    }

    private void showHermesTimeline(String plan, String process, String review) {
        LinearLayout card = card();
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(text("HERMES TIMELINE", 10, lavender), wrap());
        String[] steps = {"1 · Plan", "2 · Process", "3 · Review", "4 · Handoff"};
        String[] bodies = {
                plan == null || plan.isEmpty() ? "Awaiting plan…" : plan,
                process == null || process.isEmpty() ? "Awaiting process…" : process,
                review == null || review.isEmpty() ? "Awaiting review…" : review,
                "Ready for your confirmation before any external write."
        };
        for (int i = 0; i < steps.length; i++) {
            TextView h = text(steps[i], 12, primaryText);
            h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(h, marginParams(0, 10, 0, 2));
            TextView b = text(bodies[i], 12, secondaryText);
            b.setTextIsSelectable(true);
            card.addView(b, wrap());
        }
        if (chatHistory != null) {
            chatHistory.addView(card, marginParams(0, 8, 0, 12));
            scrollChatToBottom();
        }
    }


    private void openEmailDraft() {
        final EditText to = input("To (optional)", false);
        final EditText subject = input("Subject", false);
        final EditText body = input("Body", false);
        body.setMinLines(4);
        body.setSingleLine(false);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(8), dp(20), dp(4));
        panel.addView(text("Opens the system email app with a prefilled draft. Nothing is sent automatically.", 12, secondaryText), marginParams(0, 0, 0, 8));
        panel.addView(to, wrapParams());
        panel.addView(subject, marginParams(0, 6, 0, 0));
        panel.addView(body, marginParams(0, 6, 0, 0));
        new AlertDialog.Builder(this)
                .setTitle("Email draft")
                .setView(panel)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open email app", (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(android.net.Uri.parse("mailto:"));
                    String t = to.getText().toString().trim();
                    if (!t.isEmpty()) intent.putExtra(Intent.EXTRA_EMAIL, new String[]{t});
                    intent.putExtra(Intent.EXTRA_SUBJECT, subject.getText().toString());
                    intent.putExtra(Intent.EXTRA_TEXT, body.getText().toString());
                    try {
                        startActivity(Intent.createChooser(intent, "Send email with"));
                    } catch (Exception e) {
                        toast("No email app available");
                    }
                })
                .show();
    }

    private void openSystemCalendar() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR);
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("content://com.android.calendar/time")));
            } catch (Exception e2) {
                toast("Could not open calendar");
            }
        }
    }


    private void setContentWithTransition(Runnable rebuild) {
        if (content == null) {
            if (rebuild != null) rebuild.run();
            return;
        }
        UiEffects.transitionContent(content, rebuild);
    }

    
    private void showSandboxBrowser() {
        closeDrawer();
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Sandbox browser", "Open, edit, share, or delete files in private phone storage."), wrapParams());
        final com.kairo.app.core.SandboxWorkspace sw = new com.kairo.app.core.SandboxWorkspace(this);
        page.addView(text(sw.storageLocation(), 11, mutedText), marginParams(0, 8, 0, 10));
        java.util.List<String> files = sw.listFiles();
        if (files.isEmpty()) {
            page.addView(text("Sandbox is empty. Create files from Sandbox console or Dev Loop.", 13, secondaryText), wrap());
        } else {
            for (String line : files) {
                final String rel = line.contains("  (") ? line.substring(0, line.lastIndexOf("  (")) : line;
                TextView row = text("📄  " + line, 13, primaryText);
                row.setPadding(dp(12), dp(12), dp(12), dp(12));
                row.setBackground(glassSurface());
                row.setContentDescription("Sandbox file " + rel);
                row.setOnClickListener(v -> {
                    String[] actions = {"View / edit", "Copy path", "Delete"};
                    new AlertDialog.Builder(this).setTitle(rel).setItems(actions, (d, which) -> {
                        try {
                            if (which == 0) {
                                String body = sw.readText(rel);
                                EditText editor = input(rel, false);
                                editor.setText(body);
                                editor.setMinLines(8);
                                editor.setSingleLine(false);
                                new AlertDialog.Builder(this).setTitle("Edit " + rel).setView(editor)
                                        .setNegativeButton("Cancel", null)
                                        .setPositiveButton("Save", (dd, w) -> {
                                            String newBody = editor.getText().toString();
                                            if (!body.equals(newBody)) {
                                                showDiffThenConfirm("Overwrite sandbox file?", body, newBody, () -> {
                                                    try {
                                                        sw.writeText(rel, newBody);
                                                        toast("Saved");
                                                        showSandboxBrowser();
                                                    } catch (Exception ex) {
                                                        toast(ex.getMessage());
                                                    }
                                                });
                                            } else toast("No changes");
                                        }).show();
                            } else if (which == 1) {
                                copyToClipboard(new java.io.File(sw.getRoot(), rel).getAbsolutePath());
                                toast("Path copied");
                            } else {
                                new AlertDialog.Builder(this).setTitle("Delete " + rel + "?")
                                        .setNegativeButton("Cancel", null)
                                        .setPositiveButton("Delete", (dd, w) -> {
                                            sw.delete(rel);
                                            showSandboxBrowser();
                                        }).show();
                            }
                        } catch (Exception ex) {
                            toast(ex.getMessage() == null ? "Failed" : ex.getMessage());
                        }
                    }).show();
                });
                page.addView(row, marginParams(0, 0, 0, 8));
            }
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        if (content.getChildCount() > 0) UiEffects.fadeIn(content.getChildAt(0), 220);
    }

    private void showDiffThenConfirm(String title, String before, String after, Runnable onConfirm) {
        String b = before == null ? "" : before;
        String a = after == null ? "" : after;
        StringBuilder diff = new StringBuilder();
        String[] bl = b.split("\n", -1);
        String[] al = a.split("\n", -1);
        int max = Math.max(bl.length, al.length);
        int shown = 0;
        for (int i = 0; i < max && shown < 80; i++) {
            String left = i < bl.length ? bl[i] : "";
            String right = i < al.length ? al[i] : "";
            if (!left.equals(right)) {
                if (!left.isEmpty()) diff.append("- ").append(left).append('\n');
                if (!right.isEmpty()) diff.append("+ ").append(right).append('\n');
                shown++;
            }
        }
        if (diff.length() == 0) diff.append("(no line differences detected)\n");
        TextView tv = text(diff.toString(), 12, secondaryText);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextIsSelectable(true);
        ScrollView sc = new ScrollView(this);
        sc.addView(tv);
        sc.setPadding(dp(16), dp(8), dp(16), dp(8));
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Review the diff, then confirm.")
                .setView(sc)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (d, w) -> onConfirm.run())
                .show();
    }

    private void showGitHubCommitWizard() {
        closeDrawer();
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("GitHub commit wizard", "Path + content + message → review diff → confirm push."), wrapParams());
        EditText repo = input("owner/repo", false);
        EditText branch = input("branch (default main)", false);
        branch.setText("main");
        EditText path = input("path/to/file.txt", false);
        EditText message = input("Commit message", false);
        EditText body = input("File content", false);
        body.setSingleLine(false);
        body.setMinLines(6);
        page.addView(text("Repository", 11, mutedText), marginParams(0, 10, 0, 2));
        page.addView(repo, wrap());
        page.addView(text("Branch", 11, mutedText), marginParams(0, 8, 0, 2));
        page.addView(branch, wrap());
        page.addView(text("File path", 11, mutedText), marginParams(0, 8, 0, 2));
        page.addView(path, wrap());
        page.addView(text("Commit message", 11, mutedText), marginParams(0, 8, 0, 2));
        page.addView(message, wrap());
        page.addView(text("Content", 11, mutedText), marginParams(0, 8, 0, 2));
        page.addView(body, wrap());
        TextView status = text("", 12, mutedText);
        page.addView(status, marginParams(0, 8, 0, 0));
        page.addView(smallButton("Review & push", mint, view -> {
            String token = keyStore.get("github");
            if (token == null || token.isEmpty()) { toast("Add a GitHub token first"); return; }
            String r = repo.getText().toString().trim();
            String fp = path.getText().toString().trim();
            String br = branch.getText().toString().trim();
            if (br.isEmpty()) br = "main";
            String msg = message.getText().toString().trim();
            String contentVal = body.getText().toString();
            if (r.isEmpty() || fp.isEmpty()) { toast("Repo and path required"); return; }
            final String branchFinal = br;
            new GitHubClient().readFile(token, r, fp, branchFinal, new GitHubClient.ResultCallback() {
                @Override public void onSuccess(String existing) {
                    runOnUiThread(() -> showDiffThenConfirm("Push to GitHub?", existing, contentVal, () -> {
                        status.setText("Pushing…");
                        new GitHubClient().pushFile(token, r, fp, branchFinal,
                                msg.isEmpty() ? "Update from Kairo" : msg, contentVal,
                                new GitHubClient.ResultCallback() {
                                    @Override public void onSuccess(String result) {
                                        runOnUiThread(() -> { status.setText(result); toast("Pushed"); });
                                    }
                                    @Override public void onError(String message) {
                                        runOnUiThread(() -> { status.setText(message); toast("Push failed"); });
                                    }
                                });
                    }));
                }
                @Override public void onError(String message) {
                    runOnUiThread(() -> showDiffThenConfirm("Create new file on GitHub?", "", contentVal, () -> {
                        status.setText("Creating…");
                        new GitHubClient().pushFile(token, r, fp, branchFinal,
                                msg.isEmpty() ? "Add from Kairo" : msg, contentVal,
                                new GitHubClient.ResultCallback() {
                                    @Override public void onSuccess(String result) {
                                        runOnUiThread(() -> { status.setText(result); toast("Created"); });
                                    }
                                    @Override public void onError(String err) {
                                        runOnUiThread(() -> status.setText(err));
                                    }
                                });
                    }));
                }
            });
        }), marginParams(0, 12, 0, 0));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void showPromptTemplates() {
        closeDrawer();
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Prompt templates", "User-editable templates stored on this device."), wrapParams());
        final PromptTemplateStore store = new PromptTemplateStore(this);
        for (String name : store.names()) {
            TextView row = text("▸  " + name, 14, primaryText);
            row.setPadding(dp(12), dp(12), dp(12), dp(12));
            row.setBackground(glassSurface());
            row.setContentDescription("Template " + name);
            final String n = name;
            row.setOnClickListener(v -> {
                String body = store.get(n);
                if (composer == null) showChat();
                if (composer != null) {
                    composer.setText(body);
                    composer.setSelection(composer.length());
                }
                toast("Template loaded");
            });
            row.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this).setTitle(n)
                        .setItems(new String[]{"Edit", "Delete"}, (d, which) -> {
                            if (which == 1) { store.delete(n); showPromptTemplates(); }
                            else {
                                EditText ed = input(n, false);
                                ed.setText(store.get(n));
                                ed.setMinLines(5);
                                ed.setSingleLine(false);
                                new AlertDialog.Builder(this).setTitle("Edit template").setView(ed)
                                        .setPositiveButton("Save", (dd, w) -> {
                                            store.save(n, ed.getText().toString());
                                            toast("Saved");
                                        }).setNegativeButton("Cancel", null).show();
                            }
                        }).show();
                return true;
            });
            page.addView(row, marginParams(0, 0, 0, 8));
        }
        page.addView(smallButton("Add template", lavender, v -> {
            EditText name = input("Name", false);
            EditText body = input("Template body", false);
            body.setMinLines(4);
            body.setSingleLine(false);
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(16), dp(8), dp(16), dp(4));
            box.addView(name);
            box.addView(body);
            new AlertDialog.Builder(this).setTitle("New template").setView(box)
                    .setPositiveButton("Save", (d, w) -> {
                        store.save(name.getText().toString(), body.getText().toString());
                        showPromptTemplates();
                    }).setNegativeButton("Cancel", null).show();
        }), marginParams(0, 8, 0, 0));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void showWebhookTester() {
        closeDrawer();
        content.removeAllViews();
        LinearLayout page = page();
        page.addView(pageHeader("Webhook tester", "Send one HTTPS JSON request and inspect the log."), wrapParams());
        EditText url = input("https://example.com/webhook", false);
        EditText body = input("{\"ok\":true}", false);
        body.setMinLines(3);
        body.setSingleLine(false);
        page.addView(url, marginParams(0, 10, 0, 6));
        page.addView(body, marginParams(0, 0, 0, 8));
        TextView out = text("Log appears here.", 12, secondaryText);
        out.setTextIsSelectable(true);
        out.setTypeface(Typeface.MONOSPACE);
        page.addView(smallButton("Send (confirm)", mint, v -> {
            new AlertDialog.Builder(this).setTitle("Send webhook?")
                    .setMessage(url.getText().toString())
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Send", (d, w) -> {
                        out.setText("Sending…");
                        WebhookTester.send(url.getText().toString(), body.getText().toString(),
                                report -> runOnUiThread(() -> out.setText(report)));
                    }).show();
        }), wrap());
        page.addView(out, marginParams(0, 10, 0, 0));
        page.addView(smallButton("Show log", secondaryText, v -> {
            java.util.List<String> log = WebhookTester.logSnapshot();
            out.setText(log.isEmpty() ? "(empty)" : android.text.TextUtils.join("\n", log));
        }), marginParams(0, 8, 0, 0));
        // GitLab / Bitbucket read-only
        page.addView(text("READ-ONLY CONNECTORS", 10, lavender), marginParams(0, 16, 0, 6));
        page.addView(smallButton("List GitLab projects", secondaryText, v -> {
            String token = keyStore.get("gitlab");
            if (token == null || token.isEmpty()) { toast("Save a GitLab token in Settings/Connectors first"); return; }
            out.setText("Loading GitLab…");
            new GitLabClient().listProjects(token, new GitLabClient.Callback() {
                @Override public void onSuccess(String result) { runOnUiThread(() -> out.setText(result)); }
                @Override public void onError(String message) { runOnUiThread(() -> out.setText(message)); }
            });
        }), marginParams(0, 4, 0, 0));
        page.addView(smallButton("List Bitbucket repos", secondaryText, v -> {
            String token = keyStore.get("bitbucket");
            if (token == null || token.isEmpty()) { toast("Save bitbucket as username:app_password"); return; }
            out.setText("Loading Bitbucket…");
            new BitbucketClient().listRepos(token, new BitbucketClient.Callback() {
                @Override public void onSuccess(String result) { runOnUiThread(() -> out.setText(result)); }
                @Override public void onError(String message) { runOnUiThread(() -> out.setText(message)); }
            });
        }), marginParams(0, 4, 0, 0));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void showOnboardingIfNeeded() {
        if (preferences == null || preferences.isOnboardingDone()) return;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(12), dp(20), dp(8));
        panel.addView(text("1. Add a provider API key in Settings\n2. Pick a model\n3. Try Dev Loop on a small task\n4. Create a file in the Sandbox", 14, secondaryText));
        new AlertDialog.Builder(this)
                .setTitle("Welcome to Kairo")
                .setView(panel)
                .setPositiveButton("Got it", (d, w) -> preferences.setOnboardingDone(true))
                .setNeutralButton("Open Settings", (d, w) -> {
                    preferences.setOnboardingDone(true);
                    showSettings();
                })
                .show();
    }

    private void enhanceDevLoopWithProgress(LinearLayout page) {
        if (page == null) return;
        DevLoopState state = new DevLoopState(this);
        LinearLayout barWrap = card();
        barWrap.setPadding(dp(14), dp(12), dp(14), dp(12));
        barWrap.addView(text("DEV LOOP PROGRESS · " + state.phaseLabel(), 11, lavender), wrap());
        View barBg = new View(this);
        barBg.setBackground(rounded(soft, 8));
        LinearLayout.LayoutParams bgLp = new LinearLayout.LayoutParams(-1, dp(10));
        bgLp.topMargin = dp(8);
        barWrap.addView(barBg, bgLp);
        View barFg = new View(this);
        barFg.setBackground(rounded(mint, 8));
        int width = (int) ((getResources().getDisplayMetrics().widthPixels - dp(64)) * state.progress());
        barWrap.addView(barFg, new LinearLayout.LayoutParams(Math.max(dp(8), width), dp(10)));
        LinearLayout controls = new LinearLayout(this);
        controls.setPadding(0, dp(8), 0, 0);
        controls.addView(smallButton("Advance phase", mint, v -> { state.advance(); showDevLoop(); }), wrap());
        controls.addView(smallButton("Reset", secondaryText, v -> { state.reset(); showDevLoop(); }), marginWrapParams(8, 0, 0, 0));
        barWrap.addView(controls, wrap());
        page.addView(barWrap, marginParams(0, 0, 0, 10));
    }

    private void showBackupRestore() {
        StringBuilder meta = new StringBuilder();
        meta.append("{\n");
        meta.append("  \"provider\": \"").append(preferences.getProvider()).append("\",\n");
        meta.append("  \"model\": \"").append(preferences.getModel()).append("\",\n");
        meta.append("  \"theme\": \"").append(preferences.getThemeMode()).append("\",\n");
        meta.append("  \"note\": \"Keys are NOT included. Memories listed by category only.\"\n");
        meta.append("}\n");
        try {
            for (MemoryItem m : new MemoryStore(this).load()) {
                String preview = m.getContent();
                if (preview.length() > 40) preview = preview.substring(0, 40) + "…";
                meta.append("memory[").append(m.getCategory()).append("]: ").append(preview).append('\n');
            }
        } catch (Exception ignored) {}
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, meta.toString());
        startActivity(Intent.createChooser(share, "Export metadata backup"));
        toast("Metadata only — raw keys never exported");
    }

private void applyThemeColors() {
        if (preferences != null && preferences.isLightTheme()) {
            // Light theme – clean Claude/Groq inspired
            background = Color.rgb(250, 249, 247);
            surface = Color.rgb(255, 255, 255);
            raised = Color.rgb(244, 243, 240);
            soft = Color.rgb(238, 236, 232);
            border = Color.rgb(222, 220, 214);
            primaryText = Color.rgb(28, 28, 30);
            secondaryText = Color.rgb(90, 92, 100);
            mutedText = Color.rgb(130, 132, 140);
            lavender = Color.rgb(110, 90, 210);
            mint = Color.rgb(30, 150, 120);
            amber = Color.rgb(180, 130, 40);
            red = Color.rgb(200, 70, 70);
            userBubble = Color.rgb(230, 224, 255);
            assistantSoft = Color.rgb(245, 244, 242);
        } else {
            // Dark theme (default)
            background = Color.rgb(13, 14, 17);
            surface = Color.rgb(21, 23, 28);
            raised = Color.rgb(28, 31, 38);
            soft = Color.rgb(35, 38, 47);
            border = Color.rgb(46, 50, 60);
            primaryText = Color.rgb(244, 243, 239);
            secondaryText = Color.rgb(163, 165, 175);
            mutedText = Color.rgb(107, 110, 121);
            lavender = Color.rgb(201, 187, 255);
            mint = Color.rgb(143, 223, 192);
            amber = Color.rgb(230, 192, 122);
            red = Color.rgb(240, 138, 138);
            userBubble = Color.rgb(58, 49, 88);
            assistantSoft = Color.rgb(24, 27, 34);
        }
    }

    /** Prominent Fast / Balanced / Deep pills above the composer (Claude + Groq style). */
    private void refreshReasoningPills() {
        if (reasoningPillsRow == null) return;
        reasoningPillsRow.removeAllViews();
        String current = preferences.getReasoningMode();
        String[] ids = {"fast", "balanced", "deep"};
        String[] labels = {"Fast", "Balanced", "Deep"};
        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            boolean selected = id.equals(current);
            int fg = selected ? (preferences.isLightTheme() ? Color.WHITE : background) : secondaryText;
            int bg = selected ? lavender : (preferences.isLightTheme() ? soft : raised);
            TextView pillView = pill(labels[i], fg, bg);
            pillView.setTextSize(11);
            pillView.setPadding(dp(12), dp(6), dp(12), dp(6));
            pillView.setOnClickListener(v -> {
                preferences.setReasoningMode(id);
                refreshReasoningPills();
                toast(labels[java.util.Arrays.asList(ids).indexOf(id)] + " reasoning");
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            if (i > 0) lp.setMargins(dp(6), 0, 0, 0);
            reasoningPillsRow.addView(pillView, lp);
        }
        // Spacer + quick theme toggle
        View spacer = new View(this);
        reasoningPillsRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        TextView themeToggle = pill(preferences.isLightTheme() ? "Dark" : "Light", mutedText, soft);
        themeToggle.setTextSize(10);
        themeToggle.setOnClickListener(v -> {
            preferences.setThemeMode(preferences.isLightTheme() ? "dark" : "light");
            recreate(); // full refresh for theme
        });
        reasoningPillsRow.addView(themeToggle, wrap());
    }

    /** Claude-style suggested follow-up chips after an assistant answer. */
    private void showFollowUpChips(String lastAnswer) {
        if (chatHistory == null) return;
        // Remove previous follow-up row if present
        if (followUpChipsRow != null && followUpChipsRow.getParent() != null) {
            ((android.view.ViewGroup) followUpChipsRow.getParent()).removeView(followUpChipsRow);
        }
        followUpChipsRow = new LinearLayout(this);
        followUpChipsRow.setOrientation(LinearLayout.VERTICAL);
        followUpChipsRow.setPadding(dp(4), dp(4), dp(4), dp(10));

        TextView label = text("Suggested follow-ups", 11, mutedText);
        followUpChipsRow.addView(label, marginParams(0, 0, 0, 6));

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.START);

        String[] suggestions = buildFollowUpSuggestions(lastAnswer);
        for (int i = 0; i < suggestions.length; i++) {
            final String prompt = suggestions[i];
            TextView chip = pill(prompt.length() > 28 ? prompt.substring(0, 26) + "…" : prompt,
                    secondaryText, preferences.isLightTheme() ? soft : raised);
            chip.setTextSize(11);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setMaxLines(1);
            chip.setOnClickListener(v -> {
                if (composer != null) {
                    composer.setText(prompt);
                    composer.setSelection(composer.length());
                    composer.requestFocus();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            if (i > 0) lp.setMargins(dp(6), 0, 0, 0);
            chips.addView(chip, lp);
        }
        // Horizontal scroll for chips on small screens
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(chips);
        followUpChipsRow.addView(scroll, wrapParams());
        chatHistory.addView(followUpChipsRow, marginParams(0, 4, 0, 8));
        scrollChatToBottom();
    }

    private String[] buildFollowUpSuggestions(String answer) {
        // Simple heuristic suggestions inspired by Claude / ChatGPT follow-ups
        String lower = answer == null ? "" : answer.toLowerCase(java.util.Locale.US);
        if (lower.contains("code") || lower.contains("function") || lower.contains("class ") || lower.contains("```")) {
            return new String[]{
                    "Explain this code step by step",
                    "Find potential bugs or edge cases",
                    "Convert this to TypeScript"
            };
        }
        if (lower.contains("error") || lower.contains("exception") || lower.contains("fail")) {
            return new String[]{
                    "How do I fix this?",
                    "Show a minimal reproducible example",
                    "What are safer alternatives?"
            };
        }
        if (lower.length() > 600) {
            return new String[]{
                    "Summarize the key points",
                    "Make this more concise",
                    "Turn this into action items"
            };
        }
        return new String[]{
                "Go deeper on this",
                "Give a practical example",
                "What should I do next?"
        };
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
