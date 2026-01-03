package com.pizza.vibelauncher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    val userHandle: UserHandle? = null,
    val isWorkApp: Boolean = false
)

class AppLauncherViewModel : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()
    
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()
    
    private val _showAppPicker = MutableStateFlow(false)
    val showAppPicker: StateFlow<Boolean> = _showAppPicker.asStateFlow()
    
    private val _pickingDirection = MutableStateFlow("left")
    val pickingDirection: StateFlow<String> = _pickingDirection.asStateFlow()
    
    private val _leftSwipeApp = MutableStateFlow<AppInfo?>(null)
    val leftSwipeApp: StateFlow<AppInfo?> = _leftSwipeApp.asStateFlow()
    
    private val _rightSwipeApp = MutableStateFlow<AppInfo?>(null)
    val rightSwipeApp: StateFlow<AppInfo?> = _rightSwipeApp.asStateFlow()
    
    private val _upSwipeApp = MutableStateFlow<AppInfo?>(null)
    val upSwipeApp: StateFlow<AppInfo?> = _upSwipeApp.asStateFlow()
    
    private val _downSwipeApp = MutableStateFlow<AppInfo?>(null)
    val downSwipeApp: StateFlow<AppInfo?> = _downSwipeApp.asStateFlow()
    
    private val _longPressApp = MutableStateFlow<AppInfo?>(null)
    val longPressApp: StateFlow<AppInfo?> = _longPressApp.asStateFlow()
    
    private val _autoLaunchApp = MutableStateFlow<AppInfo?>(null)
    val autoLaunchApp: StateFlow<AppInfo?> = _autoLaunchApp.asStateFlow()
    
    private val _autoLaunchEnabled = MutableStateFlow(true)
    val autoLaunchEnabled: StateFlow<Boolean> = _autoLaunchEnabled.asStateFlow()

    private val _delayedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val delayedApps: StateFlow<List<AppInfo>> = _delayedApps.asStateFlow()

    private val _isDelayingLaunch = MutableStateFlow(false)
    val isDelayingLaunch: StateFlow<Boolean> = _isDelayingLaunch.asStateFlow()

    private val _delayTimerSeconds = MutableStateFlow(0)
    val delayTimerSeconds: StateFlow<Int> = _delayTimerSeconds.asStateFlow()

    private val _pendingLaunchApp = MutableStateFlow<AppInfo?>(null)
    val pendingLaunchApp: StateFlow<AppInfo?> = _pendingLaunchApp.asStateFlow()

    private val _delayDurationSeconds = MutableStateFlow(60)
    val delayDurationSeconds: StateFlow<Int> = _delayDurationSeconds.asStateFlow()
    
    private lateinit var sharedPrefs: SharedPreferences

    fun onSearchTextChanged(text: String) {
        try {
            _searchText.value = text
            filterApps(text)
        } catch (e: Exception) {
        }
    }

    private fun filterApps(query: String) {
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    _filteredApps.value = _allApps.value
                    return@launch
                }
                
                val searchQuery = query.lowercase().trim()
                val allApps = _allApps.value
                

                // Split apps into categories based on match quality
                val startsWithApps = mutableListOf<AppInfo>()
                val containsApps = mutableListOf<AppInfo>()
                
                allApps.forEach { app ->
                    try {
                        val appNameLower = app.appName.lowercase()

                        when {
                            // Apps that start with the query (highest priority)
                            appNameLower.startsWith(searchQuery) -> {
                                startsWithApps.add(app)
                            }
                            // Apps that contain the query anywhere in the name
                            appNameLower.contains(searchQuery) -> {
                                containsApps.add(app)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                
                // Sort each category alphabetically, then combine
                try {
                    startsWithApps.sortBy { it.appName.lowercase() }
                    containsApps.sortBy { it.appName.lowercase() }
                    
                    val filteredResults = startsWithApps + containsApps

                    _filteredApps.value = filteredResults
                    
                    // Auto-launch if only one result and auto-launch is enabled
                    if (filteredResults.size == 1 && _autoLaunchEnabled.value) {
                        _autoLaunchApp.value = filteredResults.first()
                    } else {
                        _autoLaunchApp.value = null
                    }
                } catch (e: Exception) {
                    _filteredApps.value = emptyList()
                }
                
            } catch (e: Exception) {
                _filteredApps.value = emptyList()
            }
        }
    }

    fun initializeSettings(context: Context) {
        sharedPrefs = context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)
        loadSavedSwipeApps()
        loadAutoLaunchSetting()
        loadDelayDuration()
    }
    
    private fun loadSavedSwipeApps() {
        val leftPackage = sharedPrefs.getString("left_swipe_package", null)
        val leftIsWork = sharedPrefs.getBoolean("left_swipe_is_work", false)
        val rightPackage = sharedPrefs.getString("right_swipe_package", null)
        val rightIsWork = sharedPrefs.getBoolean("right_swipe_is_work", false)
        val upPackage = sharedPrefs.getString("up_swipe_package", null)
        val upIsWork = sharedPrefs.getBoolean("up_swipe_is_work", false)
        val downPackage = sharedPrefs.getString("down_swipe_package", null)
        val downIsWork = sharedPrefs.getBoolean("down_swipe_is_work", false)
        val longPressPackage = sharedPrefs.getString("long_press_package", null)
        val longPressIsWork = sharedPrefs.getBoolean("long_press_is_work", false)

        // Find apps in current app list, matching both package name and work profile status
        val apps = _allApps.value
        _leftSwipeApp.value = apps.find { it.packageName == leftPackage && it.isWorkApp == leftIsWork }
        _rightSwipeApp.value = apps.find { it.packageName == rightPackage && it.isWorkApp == rightIsWork }
        _upSwipeApp.value = apps.find { it.packageName == upPackage && it.isWorkApp == upIsWork }
        _downSwipeApp.value = apps.find { it.packageName == downPackage && it.isWorkApp == downIsWork }
        _longPressApp.value = apps.find { it.packageName == longPressPackage && it.isWorkApp == longPressIsWork }
    }
    
    private fun loadAutoLaunchSetting() {
        _autoLaunchEnabled.value = sharedPrefs.getBoolean("auto_launch_enabled", true)
    }

    private fun loadDelayDuration() {
        _delayDurationSeconds.value = sharedPrefs.getInt("delay_duration_seconds", 60)
    }
    
    fun setAutoLaunchEnabled(enabled: Boolean) {
        _autoLaunchEnabled.value = enabled
        sharedPrefs.edit { putBoolean("auto_launch_enabled", enabled) }
    }

    fun setDelayDuration(seconds: Int) {
        _delayDurationSeconds.value = seconds
        sharedPrefs.edit { putInt("delay_duration_seconds", seconds) }
    }

    private fun loadDelayedApps() {
        val delayedPackagesSet = sharedPrefs.getStringSet("delayed_apps_packages", emptySet()) ?: emptySet()
        val allApps = _allApps.value
        
        val delayed = allApps.filter { app ->
            val key = "${app.packageName}|${app.isWorkApp}"
            delayedPackagesSet.contains(key)
        }
        _delayedApps.value = delayed
    }

    fun addDelayedApp(app: AppInfo) {
        val currentList = _delayedApps.value.toMutableList()
        if (currentList.none { it.packageName == app.packageName && it.userHandle == app.userHandle }) {
            currentList.add(app)
            _delayedApps.value = currentList
            saveDelayedApps()
        }
    }

    fun removeDelayedApp(app: AppInfo) {
        val currentList = _delayedApps.value.toMutableList()
        val wasRemoved = currentList.removeIf { it.packageName == app.packageName && it.userHandle == app.userHandle }
        if (wasRemoved) {
            _delayedApps.value = currentList
            saveDelayedApps()
        }
    }

    private fun saveDelayedApps() {
        val set = _delayedApps.value.map { "${it.packageName}|${it.isWorkApp}" }.toSet()
        sharedPrefs.edit { putStringSet("delayed_apps_packages", set) }
    }

    private var timerJob: kotlinx.coroutines.Job? = null

    private fun startLaunchDelay(app: AppInfo) {
        _pendingLaunchApp.value = app
        _delayTimerSeconds.value = _delayDurationSeconds.value
        _isDelayingLaunch.value = true
        
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_delayTimerSeconds.value > 0 && _isDelayingLaunch.value) {
                kotlinx.coroutines.delay(1000)
                if (!_isDelayingLaunch.value) return@launch
                _delayTimerSeconds.value -= 1
            }
        }
    }

    fun cancelLaunchDelay() {
        _isDelayingLaunch.value = false
        _pendingLaunchApp.value = null
        timerJob?.cancel()
    }

    fun completeDelayedLaunch(context: Context) {
        val app = _pendingLaunchApp.value
        if (app != null) {
            _isDelayingLaunch.value = false
            launchApp(context, app.packageName, app.userHandle, clearSearch = true, bypassDelay = true)
            _pendingLaunchApp.value = null
        }
    }
    
    private fun setLeftSwipeApp(app: AppInfo?) {
        _leftSwipeApp.value = app
        sharedPrefs.edit {
            putString("left_swipe_package", app?.packageName)
            putBoolean("left_swipe_is_work", app?.isWorkApp ?: false)
        }
    }
    
    private fun setRightSwipeApp(app: AppInfo?) {
        _rightSwipeApp.value = app
        sharedPrefs.edit {
            putString("right_swipe_package", app?.packageName)
            putBoolean("right_swipe_is_work", app?.isWorkApp ?: false)
        }
    }
    
    private fun setUpSwipeApp(app: AppInfo?) {
        _upSwipeApp.value = app
        sharedPrefs.edit {
            putString("up_swipe_package", app?.packageName)
            putBoolean("up_swipe_is_work", app?.isWorkApp ?: false)
        }
    }
    
    private fun setDownSwipeApp(app: AppInfo?) {
        _downSwipeApp.value = app
        sharedPrefs.edit {
            putString("down_swipe_package", app?.packageName)
            putBoolean("down_swipe_is_work", app?.isWorkApp ?: false)
        }
    }
    
    private fun setLongPressApp(app: AppInfo?) {
        _longPressApp.value = app
        sharedPrefs.edit {
            putString("long_press_package", app?.packageName)
            putBoolean("long_press_is_work", app?.isWorkApp ?: false)
        }
    }
    
    fun showSettings() {
        _showSettings.value = true
    }
    
    fun hideSettings() {
        _showSettings.value = false
    }
    
    fun openDefaultLauncherSettings(context: Context) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
        }
    }
    
    fun showAppPickerForLeft() {
        _pickingDirection.value = "left"
        _showAppPicker.value = true
    }
    
    fun showAppPickerForRight() {
        _pickingDirection.value = "right"
        _showAppPicker.value = true
    }
    
    fun showAppPickerForUp() {
        _pickingDirection.value = "up"
        _showAppPicker.value = true
    }
    
    fun showAppPickerForDown() {
        _pickingDirection.value = "down"
        _showAppPicker.value = true
    }
    
    fun showAppPickerForLongPress() {
        _pickingDirection.value = "longpress"
        _showAppPicker.value = true
    }

    fun showAppPickerForDelayed() {
        _pickingDirection.value = "delayed"
        _showAppPicker.value = true
    }
    
    fun hideAppPicker() {
        _showAppPicker.value = false
    }
    
    fun selectAppFromPicker(app: AppInfo) {
        when (_pickingDirection.value) {
            "left" -> setLeftSwipeApp(app)
            "right" -> setRightSwipeApp(app)
            "up" -> setUpSwipeApp(app)
            "down" -> setDownSwipeApp(app)
            "longpress" -> setLongPressApp(app)
            "delayed" -> addDelayedApp(app)
        }
        hideAppPicker()
    }
    
    fun onSwipeLeft(context: Context) {
        _leftSwipeApp.value?.let { app ->
            launchApp(context, app.packageName, app.userHandle)
        }
    }
    
    fun onSwipeRight(context: Context) {
        _rightSwipeApp.value?.let { app ->
            launchApp(context, app.packageName, app.userHandle)
        }
    }
    
    fun onSwipeUp(context: Context) {
        _upSwipeApp.value?.let { app ->
            launchApp(context, app.packageName, app.userHandle)
        }
    }
    
    fun onSwipeDown(context: Context) {
        _downSwipeApp.value?.let { app ->
            launchApp(context, app.packageName, app.userHandle)
        }
    }
    
    fun onLongPress(context: Context) {
        _longPressApp.value?.let { app ->
            launchApp(context, app.packageName, app.userHandle)
        }
    }

    fun loadInstalledApps(context: Context) {
        viewModelScope.launch {
            val packageManager = context.packageManager
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            
            val apps = withContext(Dispatchers.IO) {
                val allApps = mutableListOf<AppInfo>()
                
                // Get all user profiles (including work profile)
                val userProfiles = try {
                    userManager.userProfiles
                } catch (e: Exception) {
                    listOf(android.os.Process.myUserHandle())
                }

                userProfiles.forEach { userHandle ->
                    try {
                        val isWorkProfile = userHandle != android.os.Process.myUserHandle()

                        // Get apps for this user profile
                        val launcherActivities = try {
                            launcherApps.getActivityList(null, userHandle)
                        } catch (e: Exception) {
                            emptyList()
                        }

                        var successCount = 0
                        var errorCount = 0
                        
                        launcherActivities.forEach { activityInfo ->
                            try {
                                val appName = activityInfo.label.toString()
                                val packageName = activityInfo.applicationInfo.packageName

                                val icon = try {
                                    activityInfo.getBadgedIcon(0)
                                } catch (e: Exception) {
                                    // Fallback to package manager icon
                                    try {
                                        packageManager.getApplicationIcon(packageName)
                                    } catch (e2: Exception) {
                                        null
                                    }
                                }
                                
                                if (icon != null) {
                                    allApps.add(
                                        AppInfo(
                                            appName = appName,
                                            packageName = packageName,
                                            icon = icon,
                                            userHandle = userHandle,
                                            isWorkApp = isWorkProfile
                                        )
                                    )
                                    successCount++
                                }
                            } catch (e: Exception) {
                                errorCount++
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
                
                allApps
            }.sortedBy { it.appName.lowercase() }

            _allApps.value = apps
            loadSavedSwipeApps() // Reload swipe apps after loading all apps
            loadDelayedApps()
            filterApps(_searchText.value)
        }
    }

    fun clearSearchText() {
        _searchText.value = ""
        filterApps("")
    }

    fun launchApp(context: Context, packageName: String, userHandle: UserHandle? = null, clearSearch: Boolean = false, bypassDelay: Boolean = false) {
        try {
            // Check for delayed launch
            if (!bypassDelay) {
                val isDelayed = _delayedApps.value.any { it.packageName == packageName && (it.userHandle == userHandle || userHandle == null) }
                if (isDelayed) {
                    val app = _allApps.value.find { it.packageName == packageName && (it.userHandle == userHandle || userHandle == null) }
                    if (app != null) {
                        startLaunchDelay(app)
                        return
                    }
                }
            }

            if (userHandle != null && userHandle != android.os.Process.myUserHandle()) {
                // Launch work app using LauncherApps service
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                try {
                    val activities = launcherApps.getActivityList(packageName, userHandle)
                    if (activities.isNotEmpty()) {
                        launcherApps.startMainActivity(
                            activities[0].componentName,
                            userHandle,
                            null,
                            null
                        )
                    } else {
                    }
                } catch (e: Exception) {
                }
            } else {
                // Launch regular app
                val packageManager = context.packageManager
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                launchIntent?.let { intent ->
                    context.startActivity(intent)
                }
            }
            
            if (clearSearch) {
                clearSearchText()
            }
        } catch (e: Exception) {
        }
    }
}


class MainActivity : ComponentActivity() {
    private val viewModel: AppLauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set wallpaper flags for launcher
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        
        viewModel.initializeSettings(applicationContext)
        viewModel.loadInstalledApps(applicationContext)

        setContent {
            LauncherApp(viewModel = viewModel)
        }
    }

}

@Composable
fun LauncherApp(viewModel: AppLauncherViewModel) {
    val showSettings by viewModel.showSettings.collectAsState()
    val showAppPicker by viewModel.showAppPicker.collectAsState()
    val isDelayingLaunch by viewModel.isDelayingLaunch.collectAsState()
    
    // Base screen content
    if (showSettings) {
        SettingsScreen(viewModel = viewModel)
    } else {
        AppLauncherScreen(viewModel = viewModel)
    }
    
    // App picker overlay (renders on top)
    if (showAppPicker) {
        AppPickerDialog(viewModel = viewModel)
    }

    // Delay timer overlay (highest priority)
    if (isDelayingLaunch) {
        DelayTimerScreen(viewModel = viewModel)
    }
}

@Composable
fun AppLauncherScreen(viewModel: AppLauncherViewModel) {
    val searchText by viewModel.searchText.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val autoLaunchApp by viewModel.autoLaunchApp.collectAsState()
    val autoLaunchEnabled by viewModel.autoLaunchEnabled.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    
    // Auto-launch when only one result
    LaunchedEffect(autoLaunchApp) {
        autoLaunchApp?.let { app ->
            focusManager.clearFocus()
            viewModel.launchApp(context, app.packageName, app.userHandle, clearSearch = true)
        }
    }
    
    // Request focus on the search field when screen loads or resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                focusRequester.requestFocus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 32.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        viewModel.onLongPress(context)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartX = offset.x
                        dragStartY = offset.y
                    },
                    onDragEnd = {
                        // No action needed on drag end
                    }
                ) { change, _ ->
                    val deltaX = change.position.x - dragStartX
                    val deltaY = change.position.y - dragStartY
                    val minSwipeDistance = 200f
                    
                    // Determine if it's primarily horizontal or vertical swipe
                    if (abs(deltaX) > abs(deltaY)) {
                        // Horizontal swipe
                        if (abs(deltaX) > minSwipeDistance) {
                            if (deltaX > 0) {
                                // Swipe right
                                viewModel.onSwipeRight(context)
                            } else {
                                // Swipe left
                                viewModel.onSwipeLeft(context)
                            }
                        }
                    } else {
                        // Vertical swipe
                        if (abs(deltaY) > minSwipeDistance) {
                            if (deltaY > 0) {
                                // Swipe down
                                viewModel.onSwipeDown(context)
                            } else {
                                // Swipe up
                                viewModel.onSwipeUp(context)
                            }
                        }
                    }
                }
            }
    ) {
        // Settings button
        Button(
            onClick = { viewModel.showSettings() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("⚙")
        }
        // Search bar - absolute position from top
        OutlinedTextField(
            value = searchText,
            onValueChange = { viewModel.onSearchTextChanged(it) },
            placeholder = { Text("Search apps...", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 175.dp) // Fixed distance from top
                .background(
                    Color.Black.copy(alpha = 0.3f),
                    RoundedCornerShape(25.dp)
                )
                .focusRequester(focusRequester),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(25.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (filteredApps.size == 1 && autoLaunchEnabled) {
                        val app = filteredApps.first()
                        // If we don't clear the focus then the keyboard still shows when the user
                        // returns to the launcher.
                        focusManager.clearFocus()
                        viewModel.launchApp(context, app.packageName, app.userHandle, clearSearch = true)
                    }
                }
            )
        )
        
        // Results - absolute position below search bar
        if (searchText.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 240.dp) // Fixed distance from top (search bar + spacing)
            ) {
                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No apps found",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredApps.take(5), key = { "${it.packageName}_${it.userHandle?.hashCode() ?: 0}" }) { appInfo ->
                            AppListItem(appInfo = appInfo) {
                                // If we don't clear the focus then the keyboard still shows when the user
                                // returns to the launcher.
                                focusManager.clearFocus()
                                viewModel.launchApp(context, appInfo.packageName, appInfo.userHandle, clearSearch = true)
                            }
                        }
                    }
                }
            }
        }


    }
}

@Composable
fun AppListItem(appInfo: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageBitmap = remember(appInfo.icon) {
            try {
                appInfo.icon.toBitmap().asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }

        imageBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "${appInfo.appName} icon",
                modifier = Modifier.size(40.dp),
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(0f) }
                )
            )
        } ?: Box(Modifier.size(40.dp))

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = appInfo.appName,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

@Composable
fun SettingsScreen(viewModel: AppLauncherViewModel) {
    val leftSwipeApp by viewModel.leftSwipeApp.collectAsState()
    val rightSwipeApp by viewModel.rightSwipeApp.collectAsState()
    val upSwipeApp by viewModel.upSwipeApp.collectAsState()
    val downSwipeApp by viewModel.downSwipeApp.collectAsState()
    val longPressApp by viewModel.longPressApp.collectAsState()
    val autoLaunchEnabled by viewModel.autoLaunchEnabled.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button
        Button(
            onClick = { viewModel.hideSettings() },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("← Back")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Search Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Auto-launch toggle
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Auto Launch",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Automatically launch apps when search results show only one match",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = autoLaunchEnabled,
                    onCheckedChange = { viewModel.setAutoLaunchEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.White.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Delayed Apps",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "These apps will show a timer before launching to help reduce usage.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val delayDuration by viewModel.delayDurationSeconds.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Delay Duration",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${delayDuration / 60}m ${delayDuration % 60}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = delayDuration.toFloat(),
                    onValueChange = { viewModel.setDelayDuration(it.toInt()) },
                    valueRange = 5f..120f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        val delayedApps by viewModel.delayedApps.collectAsState()
        
        delayedApps.forEach { app ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (app.isWorkApp) "${app.appName} (Work)" else app.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Button(onClick = { viewModel.removeDelayedApp(app) }) {
                        Text("Remove")
                    }
                }
            }
        }
        
        Button(
            onClick = { viewModel.showAppPickerForDelayed() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Delayed App")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Swipe Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Swipe in any direction on the main screen to launch your selected apps",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // Left swipe setting
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Left Swipe App",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    leftSwipeApp?.let { app ->
                        if (app.isWorkApp) "${app.appName} (Work)" else app.appName
                    } ?: "Not set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.showAppPickerForLeft() }
                ) {
                    Text("Change")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Right swipe setting
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Right Swipe App",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    rightSwipeApp?.let { app ->
                        if (app.isWorkApp) "${app.appName} (Work)" else app.appName
                    } ?: "Not set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.showAppPickerForRight() }
                ) {
                    Text("Change")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Up swipe setting
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Up Swipe App",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    upSwipeApp?.let { app ->
                        if (app.isWorkApp) "${app.appName} (Work)" else app.appName
                    } ?: "Not set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.showAppPickerForUp() }
                ) {
                    Text("Change")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Down swipe setting
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Down Swipe App",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    downSwipeApp?.let { app ->
                        if (app.isWorkApp) "${app.appName} (Work)" else app.appName
                    } ?: "Not set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.showAppPickerForDown() }
                ) {
                    Text("Change")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Long Press Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Long press anywhere on the main screen to launch your selected app",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // Long press setting
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Long Press App",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    longPressApp?.let { app ->
                        if (app.isWorkApp) "${app.appName} (Work)" else app.appName
                    } ?: "Not set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.showAppPickerForLongPress() }
                ) {
                    Text("Change")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Set as Default Launcher",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Set as default launcher button
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Make this your default launcher app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.openDefaultLauncherSettings(context) }
                ) {
                    Text("Open Settings")
                }
            }
        }

    }
}

@Composable
fun AppPickerDialog(viewModel: AppLauncherViewModel) {
    val allApps by viewModel.allApps.collectAsState()
    val pickingDirection by viewModel.pickingDirection.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { viewModel.hideAppPicker() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .align(Alignment.Center)
                .clickable { /* Prevent closing when clicking on card */ },
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = when(pickingDirection) {
                         "longpress" -> "Select app for long press"
                         "delayed" -> "Select app to delay"
                         else -> "Select app for $pickingDirection swipe"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn {
                    items(allApps, key = { "${it.packageName}_${it.userHandle?.hashCode() ?: 0}" }) { app ->
                        AppPickerItem(
                            app = app,
                            onClick = { viewModel.selectAppFromPicker(app) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.hideAppPicker() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun AppPickerItem(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageBitmap = remember(app.icon) {
            try {
                app.icon.toBitmap().asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }

        imageBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "${app.appName} icon",
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(0f) }
                )
            )
        } ?: Box(Modifier.size(32.dp))

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = if (app.isWorkApp) "${app.appName} (Work)" else app.appName,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
fun DelayTimerScreen(viewModel: AppLauncherViewModel) {
    val remainingSeconds by viewModel.delayTimerSeconds.collectAsState()
    val pendingApp by viewModel.pendingLaunchApp.collectAsState()
    val context = LocalContext.current
    
    // Auto-complete when timer hits 0
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds == 0) {
            viewModel.completeDelayedLaunch(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .pointerInput(Unit) {
                detectTapGestures { /* Block touches */ }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            pendingApp?.let { app ->
                val imageBitmap = remember(app.icon) {
                    try {
                        app.icon.toBitmap().asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }

                imageBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                         colorFilter = ColorFilter.colorMatrix(
                            ColorMatrix().apply { setToSaturation(0f) }
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Opening ${app.appName} in...",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "$remainingSeconds",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = { viewModel.cancelLaunchDelay() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    }
}