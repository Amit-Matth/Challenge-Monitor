package com.amitmatth.challengemonitor.ui

import android.animation.ObjectAnimator
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.ActivityMainBinding
import com.amitmatth.challengemonitor.receiver.AutoSkipSchedulerReceiver
import com.amitmatth.challengemonitor.ui.fragments.*
import com.amitmatth.challengemonitor.viewmodel.ChallengeViewModel
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri
import kotlin.math.min
import androidx.appcompat.app.ActionBar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ChallengeViewModel
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var prefs: SharedPreferences

    private var inflatedCustomToolbarView: View? = null

    private lateinit var navItemsContainer: LinearLayout
    private lateinit var navCalendar: LinearLayout
    private lateinit var navMine: LinearLayout
    private lateinit var activeIndicator: View

    private val topLevelDestinations = setOf(
        HomeFragment::class.java.name,
        DashBoardFragment::class.java.name
    )

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val AUTO_SKIP_ALARM_REQUEST_CODE = 20240315
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        scheduleAutoSkipWorkerUsingAlarmManager()
        setupUI()
        handleOnboarding(savedInstanceState)
        setupListeners()
        startFuturisticAnimations()
    }

    private fun initializeComponents() {
        setSupportActionBar(binding.toolbar)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        viewModel = ViewModelProvider(this)[ChallengeViewModel::class.java]

        navItemsContainer = binding.navItemsContainerLinearLayout
        navCalendar = binding.navHome
        navMine = binding.navStats
        activeIndicator = binding.activeIndicator
    }

    private fun scheduleAutoSkipWorkerUsingAlarmManager() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AutoSkipSchedulerReceiver::class.java).apply {
            action = AutoSkipSchedulerReceiver.ACTION_SCHEDULE_AUTO_SKIP
        }

        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            AUTO_SKIP_ALARM_REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        try {
            alarmManager.cancel(pendingIntent)

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Log.d(
                "MainActivity",
                "AutoSkipSchedulerReceiver alarm set to repeat daily around 23:50. First run: ${sdf.format(calendar.time)}"
            )
        } catch (e: SecurityException) {
            Log.e("MainActivity", "SecurityException while scheduling auto skip alarm: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("MainActivity", "Exception while scheduling auto skip alarm: ${e.message}", e)
        }
    }


    private fun setupUI() {
        supportActionBar?.apply {
            setDisplayShowCustomEnabled(true)
            setDisplayShowTitleEnabled(false)

            val inflater = LayoutInflater.from(this@MainActivity)
            inflatedCustomToolbarView = inflater.inflate(R.layout.layout_custom_toolbar, binding.toolbar, false)

            val lp = ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT
            )
            setCustomView(inflatedCustomToolbarView, lp)
        }

        setupNavigationDrawer()
        updateCustomToolbarContent()

        loadNavHeaderData()
        setupNavHeaderClick()
        selectNavigationItem(navCalendar)
    }

    private fun handleOnboarding(savedInstanceState: Bundle?) {
        val isOnboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

        if (!isOnboardingCompleted) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), HomeFragment::class.java.name)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.drawer_override_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
        val isTopLevel = currentFragment?.let {
            topLevelDestinations.contains(it::class.java.name)
        } ?: true

        val drawerEndMenuItem = menu.findItem(R.id.action_open_drawer_end)
        drawerEndMenuItem?.isVisible = !isTopLevel

        if (drawerEndMenuItem?.isVisible == true) {
            drawerEndMenuItem.icon?.colorFilter = PorterDuffColorFilter(
                ContextCompat.getColor(this, R.color.nav_icon_active),
                PorterDuff.Mode.SRC_ATOP
            )
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setupListeners() {
        binding.createChallenge.setOnClickListener {
            animateFAB()
            loadFragment(CreateChallengeFragment(), CreateChallengeFragment::class.java.name)
        }
        navCalendar.setOnClickListener {
            animateNavItem(navCalendar)
            selectNavigationItem(navCalendar)
            val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
            if (currentFragment !is HomeFragment) {
                loadFragment(HomeFragment(), HomeFragment::class.java.name, true)
            }
        }
        navMine.setOnClickListener {
            animateNavItem(navMine)
            selectNavigationItem(navMine)
            val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
            if (currentFragment !is DashBoardFragment) {
                loadFragment(DashBoardFragment(), DashBoardFragment::class.java.name, true)
            }
        }
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem.itemId)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            updateToolbarNavigation()
            updateCustomToolbarContent()
        }
        onBackPressedDispatcher.addCallback(this) {
            handleBackPress()
        }
    }

    private fun selectNavigationItem(selectedItem: LinearLayout) {
        navCalendar.isSelected = false
        navMine.isSelected = false
        selectedItem.isSelected = true
        animateActiveIndicator(selectedItem)
        updateNavigationIcons()
    }

    private fun animateActiveIndicator(selectedItemView: View) {
        selectedItemView.post {
            val selectedItemCenterXInParent = selectedItemView.left + selectedItemView.width / 2f
            val indicatorTargetLeftInParentCoordinates =
                selectedItemCenterXInParent - activeIndicator.width / 2f
            val desiredIndicatorLeftAbsolute =
                navItemsContainer.left + indicatorTargetLeftInParentCoordinates
            val initialIndicatorMarginStart =
                (activeIndicator.layoutParams as? FrameLayout.LayoutParams)?.marginStart ?: 0
            val initialIndicatorLeftAbsolute = initialIndicatorMarginStart.toFloat()
            val translationX = desiredIndicatorLeftAbsolute - initialIndicatorLeftAbsolute
            ObjectAnimator.ofFloat(activeIndicator, "translationX", translationX).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    private fun updateNavigationIcons() {
        binding.navHomeIcon.refreshDrawableState()
        binding.navStatsIcon.refreshDrawableState()
        binding.navHomeText.refreshDrawableState()
        binding.navStatsText.refreshDrawableState()
    }

    private fun animateNavItem(item: View) {
        item.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            item.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
        createRippleEffect(item)
    }

    private fun animateFAB() {
        binding.createChallenge.animate().scaleX(0.9f).scaleY(0.9f).setDuration(250)
            .setInterpolator(DecelerateInterpolator()).withEndAction {
            binding.createChallenge.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150)
                .withEndAction {
                    binding.createChallenge.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
        }.start()
        binding.pulseRing.animate().scaleX(1.5f).scaleY(1.5f).alpha(0f).setDuration(250)
            .setInterpolator(DecelerateInterpolator()).withEndAction {
            binding.pulseRing.scaleX = 1f
            binding.pulseRing.scaleY = 1f
            binding.pulseRing.alpha = 0.6f
            val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)
            binding.pulseRing.startAnimation(pulseAnimation)
        }.start()
    }

    private fun createRippleEffect(view: View) {
        val rippleContainer = binding.navBackgroundContainer
        val ripple = View(this)
        val startDiameterPx = try {
            resources.getDimensionPixelSize(R.dimen.nav_item_ripple_start_size_centered)
        } catch (_: Resources.NotFoundException) {
            16
        }
        ripple.layoutParams = FrameLayout.LayoutParams(startDiameterPx, startDiameterPx)
        ripple.background = ContextCompat.getDrawable(this, R.drawable.ring_outline_effect)
        val viewLocation = IntArray(2); view.getLocationInWindow(viewLocation)
        val containerLocation = IntArray(2); rippleContainer.getLocationInWindow(containerLocation)
        val relativeLeft = viewLocation[0] - containerLocation[0]
        val relativeTop = viewLocation[1] - containerLocation[1]
        ripple.x = relativeLeft + (view.width / 2f) - (startDiameterPx / 2f)
        ripple.y = relativeTop + (view.height / 2f) - (startDiameterPx / 2f)
        rippleContainer.addView(ripple)
        val targetExpansionDiameterPx = min(view.width, view.height).toFloat() * 2.0f
        val endScale =
            if (startDiameterPx > 0) targetExpansionDiameterPx / startDiameterPx.toFloat() else 0f
        if (endScale > 0) {
            ripple.animate().scaleX(endScale).scaleY(endScale).alpha(0f).setDuration(350)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction { rippleContainer.removeView(ripple) }.start()
        } else {
            rippleContainer.removeView(ripple)
        }
    }

    private fun startFuturisticAnimations() {
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)
        binding.pulseRing.startAnimation(pulseAnimation)
    }

    private fun handleNavigationItemSelected(itemId: Int): Boolean {
        val fragmentClass = when (itemId) {
            R.id.nav_skipped -> SkippedFragment::class.java
            R.id.nav_logged -> LoggedFragment::class.java
            R.id.nav_notLogged -> NotLoggedFragment::class.java
            R.id.nav_followed_challenges -> FollowedFragment::class.java
            R.id.nav_unfollowed_challenges -> UnFollowedFragment::class.java
            R.id.nav_streaks -> StreaksFragment::class.java
            R.id.nav_completed -> CompletedFragment::class.java
            R.id.nav_settings -> SettingsFragment::class.java
            R.id.nav_help -> HelpFragment::class.java
            else -> return false
        }
        val fragment = fragmentClass.getDeclaredConstructor().newInstance() as Fragment
        loadFragment(fragment, fragment::class.java.name)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun handleBackPress() {
        when {
            binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> binding.drawerLayout.closeDrawer(
                GravityCompat.START
            )

            supportFragmentManager.backStackEntryCount > 0 -> supportFragmentManager.popBackStack()
            else -> {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
                if (currentFragment is HomeFragment || currentFragment == null) {
                    finish()
                } else {
                    loadFragment(HomeFragment(), HomeFragment::class.java.name, true)
                    selectNavigationItem(navCalendar)
                }
            }
        }
    }

    private fun setupNavigationDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
    }

    private fun updateToolbarNavigation() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
        val isTopLevel =
            currentFragment?.let { topLevelDestinations.contains(it::class.java.name) } ?: true

        drawerToggle.isDrawerIndicatorEnabled = isTopLevel
        supportActionBar?.setDisplayHomeAsUpEnabled(!isTopLevel)

        drawerToggle.drawerArrowDrawable.colorFilter = PorterDuffColorFilter(
            ContextCompat.getColor(this, R.color.nav_icon_active),
            PorterDuff.Mode.SRC_ATOP
        )

        if (isTopLevel) {
            drawerToggle.toolbarNavigationClickListener = null
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            drawerToggle.toolbarNavigationClickListener = View.OnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }

        drawerToggle.syncState()

        binding.toolbar.navigationIcon?.colorFilter = PorterDuffColorFilter(
            ContextCompat.getColor(this, R.color.nav_icon_active),
            PorterDuff.Mode.SRC_ATOP
        )

        invalidateOptionsMenu()
    }

    private fun updateCustomToolbarContent() {
        val greetingTextView = inflatedCustomToolbarView?.findViewById<TextView>(R.id.greetingText)
        val quoteTextView =
            inflatedCustomToolbarView?.findViewById<TextView>(R.id.dateText)
        val currentName = prefs.getString("user_name", "Amit")

        greetingTextView?.text = getString(R.string.toolbar_greeting_format, currentName)

        val quotes = listOf(
            "Your progress matters 🚀",
            "One step at a time 💡",
            "Stay consistent 🔑",
            "Little wins, big changes ✨",
            "Discipline beats motivation ⚡",
            "Keep moving forward ➡️",
            "Small steps, big impact 🌱",
            "Dream. Plan. Do. ✅",
            "Success loves consistency 🔥",
            "You're closer than you think 🌟"
        )

        val randomQuote = quotes.random()
        quoteTextView?.text = randomQuote
    }


    fun loadFragment(fragment: Fragment, tag: String, isReplacingRoot: Boolean = false) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.main_container, fragment, tag)
        if (isReplacingRoot) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        } else {
            if (currentFragment == null || currentFragment.tag != tag) {
                if (currentFragment != null) {
                    ft.addToBackStack(tag)
                }
            }
        }
        ft.commit()
        binding.root.post {
            updateToolbarNavigation()
            updateCustomToolbarContent()
            updateBottomNavigationState()
        }
    }

    private fun updateBottomNavigationState() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
        when (currentFragment) {
            is HomeFragment -> selectNavigationItem(navCalendar)
            is DashBoardFragment -> selectNavigationItem(navMine)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        when (item.itemId) {
            R.id.action_open_drawer_end -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupNavHeaderClick() {
        val headerView = binding.navigationView.getHeaderView(0)
        val profileImageView: ImageView = headerView.findViewById(R.id.nav_header_profile_image)
        val userNameTextView: TextView = headerView.findViewById(R.id.nav_header_user_name)
        val editProfileIcon: ImageView = headerView.findViewById(R.id.nav_header_edit_profile_icon)
        val clickListener = View.OnClickListener {
            loadFragment(EditProfileFragment(), EditProfileFragment::class.java.name)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        profileImageView.setOnClickListener(clickListener)
        userNameTextView.setOnClickListener(clickListener)
        editProfileIcon.setOnClickListener(clickListener)
    }

    fun loadNavHeaderData() {
        val headerView = binding.navigationView.getHeaderView(0)
        val profileImageView: ImageView = headerView.findViewById(R.id.nav_header_profile_image)
        val userNameTextView: TextView = headerView.findViewById(R.id.nav_header_user_name)
        val dateTextViewH: TextView = headerView.findViewById(R.id.nav_header_date_text)
        val currentName = prefs.getString("user_name", "User Name")
        val imageUriString = prefs.getString("profile_image_uri", null)
        userNameTextView.text = currentName
        dateTextViewH.text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        loadProfileImage(profileImageView, imageUriString)
        updateCustomToolbarContent()
    }

    private fun loadProfileImage(imageView: ImageView, imageUriString: String?) {
        val imageSource = imageUriString?.toUri() ?: R.drawable.outline_account_circle_24

        Glide.with(this).load(imageSource).circleCrop()
            .placeholder(R.drawable.outline_account_circle_24)
            .error(R.drawable.outline_account_circle_24).into(imageView)

        val navIconTintColorStateList = ContextCompat.getColorStateList(this, R.color.nav_icon_selector)
        binding.navStatsIcon.imageTintList = navIconTintColorStateList
        updateNavigationIcons()
    }
}
