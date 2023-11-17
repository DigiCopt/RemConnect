package com.inovace.remconnect

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_BATTERY_OPTIMIZATIONS = 1234
    }
    private lateinit var schlname: AppCompatTextView
    private lateinit var hamburgerMenuButton: AppCompatImageButton
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var checkmarkIcon: AppCompatImageView
    private var notifyg = false
    private lateinit var roundedButton: AppCompatButton
    val db = Firebase.firestore
    private lateinit var nametext: AppCompatTextView
    private lateinit var welcome:AppCompatTextView


    private val requestPermLaunchNotify: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                notifyg = true
                initializeAppFunctionality()
            } else {
                notifyg = false
                showPermDialog("Notification Permission")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isBatteryOptimizationDisabled()) {
            // If not, request to disable it
            requestDisableBatteryOptimization();
        }

        setContentView(R.layout.activity_main)
        checkmarkIcon=findViewById(R.id.checkmarkIcon)
        roundedButton = findViewById(R.id.roundedButton)
        roundedButton.setOnClickListener{
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
        DoInit()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()

        // Check if the user is authenticated
        val currentUser: FirebaseUser? = firebaseAuth.currentUser
        if (currentUser == null) {
            // User is not authenticated, redirect to LoginActivity
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish() // Close the MainActivity
        } else {
            // User is authenticated, proceed with the app functionality
            initializeAppFunctionality()

            }
    }
    private fun initializeAppFunctionality() {
        schlname=findViewById(R.id.schlname)
        nametext=findViewById(R.id.nametext)
        welcome=findViewById(R.id.welcome)
        hamburgerMenuButton = findViewById(R.id.menu)
        hamburgerMenuButton.setOnClickListener { v -> showPopupMenu(v) }
        nametext.text = firebaseAuth.currentUser?.displayName
        val firstname= firebaseAuth.currentUser?.displayName?.split("\\s+".toRegex())
        if (firstname != null) {
            welcome.text="Hello "+firstname.first()+","
        }
        // Request FCM token
        requestFCMToken()
        fetchSchlnameFromFirestore()

        // Set click listeners for buttons
        val logoutButton = findViewById<AppCompatImageButton>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            // Log out the user
            firebaseAuth.signOut()

            // Redirect to the login page
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish() // Close the MainActivity
        }
        if (!notifyg) {
            requestPermNotify()
        } else {
            Toast.makeText(this@MainActivity, "Notification Permission Granted", Toast.LENGTH_SHORT).show()
        }
    }
//End of initappfunc
private fun fetchSchlnameFromFirestore() {
    val user = firebaseAuth.currentUser
    val useremail: String? = user?.email

    if (useremail != null) {
        val userDocument = db.collection("users").document(useremail)

        userDocument.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val schlnameValue = document.getString("school")
                    updateSchlnameUI(schlnameValue)
                } else {
                    Log.d("Firestore", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("Firestore", "get failed with ", exception)
            }
    }
}

    private fun updateSchlnameUI(schlnameValue: String?) {
        schlname = findViewById(R.id.schlname)
        schlname.text = schlnameValue
    }
    private fun isBatteryOptimizationDisabled(): Boolean {
        val packageName = packageName
        val pm = getSystemService(POWER_SERVICE) as PowerManager?
        return pm?.isIgnoringBatteryOptimizations(packageName) == true
    }
    private fun requestDisableBatteryOptimization() {
        val packageName = packageName
        val intent = Intent()
        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATIONS)
    }

    fun DoInit(){
        val bset=isAccessibilitySettingsOn(this)
        if(bset==true) {
            checkmarkIcon.isVisible=true
            roundedButton.isEnabled=false
            roundedButton.text="Accessibility Permission Granted"
        } else {
            roundedButton.isEnabled=true
            roundedButton.text="Accessibility Permission Pending!!"
        }
    }
    override fun onResume(){

        DoInit()
        super.onResume()
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        val menu = popupMenu.menu

        // Add menu items here
        menu.add("Item 1")
        menu.add("Item 2")
        menu.add("Item 3")
        popupMenu.setOnMenuItemClickListener { // Handle menu item clicks here
            true
        }
        popupMenu.show()
    }
    private fun isAccessibilitySettingsOn(mContext: Context): Boolean {
        var accessibilityEnabled = 0
        val service = packageName + "/" + MyAccessibilityService::class.java.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                mContext.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: SettingNotFoundException) {
        }
        val mStringColonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                mContext.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        } else {
        }
        return false
    }
//

    private fun requestPermNotify() {
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notifyg = true
            initializeAppFunctionality()
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Log.d("Permission", "inside else...first time not allowed")
            } else {
                Log.d("Permission", "second time denied")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermLaunchNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val token = task.result
                Log.d("FCMToken", "Token: $token")
                val useremail: String? = firebaseAuth.currentUser?.email
                val userdb = db.collection("users").document(useremail.toString())
                userdb
                    .update("token", token)
                    .addOnSuccessListener { Log.d("update", "DocumentSnapshot successfully updated!") }
                    .addOnFailureListener { e -> Log.w("update", "Error updating document", e) }

            } else {
                // Handle the failure to get the FCM token, e.g., logging or showing an error message.
            }

        }
    }

    private fun showPermDialog(perm_desc: String) {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Alert for Permission")
            .setPositiveButton("Settings") { dialogInterface: DialogInterface?, i: Int ->
                val rintent = Intent()
                rintent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                rintent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                startActivity(rintent)
            }
            .setNegativeButton("Exit") { dialogInterface: DialogInterface?, i: Int -> }
            .show()
    }
}
