package com.example.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.di.LexisContainer
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutLexis
import com.example.ui.components.ErrorBanner
import com.example.ui.dictionary.DictionaryScreen
import com.example.ui.dictionary.DictionaryViewModel
import com.example.ui.encyclopedia.EncyclopediaScreen
import com.example.ui.encyclopedia.EncyclopediaViewModel
import com.example.ui.home.HomeStatsScreen
import com.example.ui.home.StatsViewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.practice.PracticeHubScreen
import com.example.ui.practice.PracticeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private data class NavDest(
    val labelAr: String,
    val labelEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val DESTINATIONS = listOf(
    NavDest("الإحصائيات", "Stats", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    NavDest("القاموس", "Words", Icons.AutoMirrored.Rounded.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_dictionary"),
    NavDest("التدريب", "Practice", Icons.Rounded.Psychology, Icons.Outlined.Psychology, "nav_practice"),
    NavDest("القواعد", "Grammar", Icons.Rounded.School, Icons.Outlined.School, "nav_encyclopedia"),
    NavDest("المكتبة", "Library", Icons.Rounded.AutoStories, Icons.Outlined.AutoStories, "nav_library")
)

@Composable
fun AppNavigation(
    container: LexisContainer,
    arabicUi: Boolean
) {
    val firebaseAuth = container.firebaseAuth
    var currentUser by remember { mutableStateOf(firebaseAuth?.currentUser) }
    var seeded by remember { mutableStateOf(false) }
    var seedError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        container.assetSeeder.ensureSeeded()
            .onSuccess { seeded = true }
            .onFailure { seedError = it.localizedMessage ?: "تعذر تجهيز قاعدة بيانات المفردات المحلية" }
    }

    DisposableEffect(firebaseAuth) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        firebaseAuth?.addAuthStateListener(listener)
        onDispose { firebaseAuth?.removeAuthStateListener(listener) }
    }

    if (seedError != null) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            ErrorBanner(message = seedError!!)
        }
        return
    }

    if (!seeded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text(
                    text = if (arabicUi) "جارٍ تجهيز 5,000 كلمة و1,244 فعلاً و500 قصة ومحادثة..." else "Preparing Oxford 5000 & library...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    val user = currentUser
    if (firebaseAuth != null && user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = firebaseAuth.currentUser },
            arabicUi = arabicUi
        )
    } else {
        key(user?.uid ?: "local_user") {
            MainLexisScaffold(
                container = container,
                currentUserId = user?.uid ?: "local_user",
                displayName = user?.displayName,
                arabicUi = arabicUi
            )
        }
    }
}

@Composable
private fun MainLexisScaffold(
    container: LexisContainer,
    currentUserId: String,
    displayName: String?,
    arabicUi: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val factory = remember(currentUserId, container) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when (modelClass) {
                    StatsViewModel::class.java -> StatsViewModel(
                        container.userProgressRepository,
                        container.prefsRepository,
                        currentUserId,
                        displayName
                    ) as T
                    DictionaryViewModel::class.java -> DictionaryViewModel(
                        container.dao,
                        container.userProgressRepository,
                        container.aiContentRepository,
                        currentUserId
                    ) as T
                    PracticeViewModel::class.java -> PracticeViewModel(
                        container.dao,
                        container.userProgressRepository,
                        currentUserId
                    ) as T
                    EncyclopediaViewModel::class.java -> EncyclopediaViewModel(
                        container.dao,
                        container.userProgressRepository,
                        currentUserId
                    ) as T
                    LibraryViewModel::class.java -> LibraryViewModel(
                        container.dao,
                        container.userProgressRepository,
                        container.aiContentRepository,
                        currentUserId
                    ) as T
                    else -> throw IllegalArgumentException("Unknown VM: ${modelClass.name}")
                }
            }
        }
    }

    val statsVm: StatsViewModel = viewModel(key = "stats_$currentUserId", factory = factory)
    val dictVm: DictionaryViewModel = viewModel(key = "dict_$currentUserId", factory = factory)
    val practiceVm: PracticeViewModel = viewModel(key = "practice_$currentUserId", factory = factory)
    val encVm: EncyclopediaViewModel = viewModel(key = "enc_$currentUserId", factory = factory)
    val libraryVm: LibraryViewModel = viewModel(key = "lib_$currentUserId", factory = factory)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        if (useRail) {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(12.dp))
                        DESTINATIONS.forEachIndexed { idx, dest ->
                            val selected = selectedTab == idx
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.labelAr
                                    )
                                },
                                label = { Text(if (arabicUi) dest.labelAr else dest.labelEn, maxLines = 1) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        ActiveTabContent(
                            selectedTab = selectedTab,
                            statsVm = statsVm,
                            dictVm = dictVm,
                            practiceVm = practiceVm,
                            encVm = encVm,
                            libraryVm = libraryVm,
                            container = container,
                            arabicUi = arabicUi,
                            onNavigateTab = { selectedTab = it },
                            onSignOut = { signOutLexis(credentialManager, scope) }
                        )
                    }
                }
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        DESTINATIONS.forEachIndexed { idx, dest ->
                            val selected = selectedTab == idx
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.labelAr
                                    )
                                },
                                label = {
                                    Text(
                                        text = if (arabicUi) dest.labelAr else dest.labelEn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ActiveTabContent(
                        selectedTab = selectedTab,
                        statsVm = statsVm,
                        dictVm = dictVm,
                        practiceVm = practiceVm,
                        encVm = encVm,
                        libraryVm = libraryVm,
                        container = container,
                        arabicUi = arabicUi,
                        onNavigateTab = { selectedTab = it },
                        onSignOut = { signOutLexis(credentialManager, scope) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTabContent(
    selectedTab: Int,
    statsVm: StatsViewModel,
    dictVm: DictionaryViewModel,
    practiceVm: PracticeViewModel,
    encVm: EncyclopediaViewModel,
    libraryVm: LibraryViewModel,
    container: LexisContainer,
    arabicUi: Boolean,
    onNavigateTab: (Int) -> Unit,
    onSignOut: () -> Unit
) {
    when (selectedTab) {
        0 -> HomeStatsScreen(statsVm, onNavigateTab, onSignOut)
        1 -> DictionaryScreen(dictVm, container.audioEngine, arabicUi)
        2 -> PracticeHubScreen(practiceVm, container.audioEngine, arabicUi)
        3 -> EncyclopediaScreen(encVm, container.audioEngine, arabicUi)
        else -> LibraryScreen(libraryVm, container.audioEngine, arabicUi)
    }
}
