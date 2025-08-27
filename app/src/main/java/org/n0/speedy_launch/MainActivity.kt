package org.n0.speedy_launch

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import java.util.ArrayList
import java.util.Locale

class MainActivity : Activity() {

    lateinit var prefs: SharedPreferences
    lateinit var prefEditor: SharedPreferences.Editor

    lateinit var appListView: ListView
    lateinit var leftPrefsListView: ListView
    lateinit var rightPrefsListView: ListView

    lateinit var searchKeyEdit: EditText

    lateinit var leftBtn1: Button
    lateinit var leftBtn2: Button
    lateinit var rightBtn1: Button
    lateinit var rightBtn2: Button
    lateinit var alertDialogBuilder: AlertDialog.Builder

    private lateinit var packageNamesArrList: ArrayList<String>
    private lateinit var appAdapter: ArrayAdapter<String>
    private lateinit var packageList: List<Array<String>>
    private lateinit var packageManager: PackageManager

    private var searchString: String = ""
    private var tempShowApp: Boolean = false

    private val leftPrefsArr = arrayOf("∫", "∃", "∈", "∉", "⊂", "⊗", "∀")
    private val rightPrefsArr = arrayOf("∞", "∧", "∨", "⊂", "∑", "∏", "⊕")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = Color.TRANSPARENT
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        appListView = findViewById(R.id.appListView)
        searchKeyEdit = findViewById(R.id.searchKeyEdit)
        leftBtn1 = findViewById(R.id.leftBtn1)
        leftBtn2 = findViewById(R.id.leftBtn2)
        rightBtn1 = findViewById(R.id.rightBtn1)
        rightBtn2 = findViewById(R.id.rightBtn2)
        leftPrefsListView = findViewById(R.id.leftPrefs)
        rightPrefsListView = findViewById(R.id.rightPrefs)
        alertDialogBuilder = AlertDialog.Builder(this)

        packageManager = packageManager

        packageNamesArrList = ArrayList()
        appAdapter = ArrayAdapter(this, R.layout.main_listview, R.id.mnTxtVw, ArrayList())

        val leftPrefsAdapter = ArrayAdapter(this, R.layout.main_listview, R.id.mnTxtVw, rightPrefsArr)
        val rightPrefsAdapter = ArrayAdapter(this, R.layout.main_listview, R.id.mnTxtVw, leftPrefsArr)

        leftPrefsListView.adapter = leftPrefsAdapter
        rightPrefsListView.adapter = rightPrefsAdapter

        searchString = ""
        updateAppList()

        appListView.setOnItemClickListener { _, _, i, _ ->
            launch(packageNamesArrList[i])
        }

        appListView.setOnItemLongClickListener { _, _, i, _ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:" + packageNamesArrList[i])
                if (getAppNameFromPkgName(packageNamesArrList[i]) == "") {
                    throw Exception("package deleted")
                }
                startActivity(intent)
            } catch (e: Exception) {
                showAppDeleted()
            }
            true
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefEditor = prefs.edit()

        leftBtn1.setOnLongClickListener {
            prefEditor.putBoolean("skeptic", !prefs.getBoolean("skeptic", false)).apply()
            true
        }

        leftBtn1.setOnClickListener {
            prefEditor.putBoolean("show_menu", !prefs.getBoolean("show_menu", false)).apply()
            filterAppList()
        }

        leftBtn2.setOnClickListener {
            buttonPrefsFlow("left_2")
        }

        rightBtn1.setOnClickListener {
            buttonPrefsFlow("right_1")
        }

        rightBtn2.setOnClickListener {
            buttonPrefsFlow("right_2")
        }

        leftPrefsListView.setOnItemClickListener { adapterView, _, i, _ ->
            buttonPrefsFlow(adapterView.getItemAtPosition(i) as String)
        }

        rightPrefsListView.setOnItemClickListener { adapterView, _, i, _ ->
            buttonPrefsFlow(adapterView.getItemAtPosition(i) as String)
        }

        leftBtn2.setOnLongClickListener {
            buttonPrefsFlow("left_2_long")
            true
        }

        rightBtn1.setOnLongClickListener {
            buttonPrefsFlow("right_1_long")
            true
        }

        rightBtn2.setOnLongClickListener {
            buttonPrefsFlow("right_2_long")
            true
        }

        leftPrefsListView.setOnItemLongClickListener { adapterView, _, i, _ ->
            buttonPrefsFlow("${adapterView.getItemAtPosition(i)}_long")
            true
        }

        rightPrefsListView.setOnItemLongClickListener { adapterView, _, i, _ ->
            buttonPrefsFlow("${adapterView.getItemAtPosition(i)}_long")
            true
        }

        searchKeyEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                setKey(s.toString().lowercase(Locale.getDefault()))
            }

            override fun afterTextChanged(s: Editable) {}
        })

        if (!prefs.getBoolean("help_shown", false)) {
            showHelp()
            prefEditor.putBoolean("help_shown", true)
            prefEditor.apply()
        }
    }

    private fun showHelp() {
        updateAppList()
        filterAppList()
        alertDialogBuilder.setMessage(R.string.help)
        alertDialogBuilder.setPositiveButton(R.string.go) { _, _ -> }
        alertDialogBuilder.create().show()
    }

    private fun showAppDeleted() {
        updateAppList()
        filterAppList()
        alertDialogBuilder.setMessage(R.string.appDeleted)
        alertDialogBuilder.setPositiveButton(R.string.go) { _, _ -> }
        alertDialogBuilder.create().show()
    }

    private fun setKey(s: String) {
        searchString = s
        filterAppList()
    }

    private fun buttonPrefsFlow(key: String) {
        val pkgName = prefs.getString(key, "") ?: ""
        if (pkgName != "") {
            val appName = getAppNameFromPkgName(pkgName)
            if (appName == "") {
                prefEditor.putString(key, "").apply()
                showAppDeleted()
                changeOnPressAppDialog(key)
            } else {
                if (prefs.getBoolean("skeptic", false)) {
                    launchOrChangePref(key, appName, pkgName)
                } else {
                    launch(pkgName)
                }
            }
        } else {
            changeOnPressAppDialog(key)
        }
    }

    private fun launchOrChangePref(key: String, appName: String, packageName: String) {
        alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(appName)
        alertDialogBuilder.setPositiveButton(R.string.go) { _, _ -> launch(packageName) }
        alertDialogBuilder.setNegativeButton(R.string.change) { _, _ -> changeOnPressAppDialog(key) }
        alertDialogBuilder.create().show()
    }

    private fun changeOnPressAppDialog(key: String) {
        tempShowApp = true
        alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(R.layout.main_listview)
        alertDialogBuilder.setAdapter(appListView.adapter) { _, i1 ->
            prefEditor.putString(key, packageNamesArrList[i1])
            prefEditor.apply()
            tempShowApp = false
            filterAppList()
        }
        alertDialogBuilder.setOnCancelListener {
            tempShowApp = false
            filterAppList()
        }
        filterAppList()
        alertDialogBuilder.create().show()
    }

    private fun getAppNameFromPkgName(pkgName: String): String {
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkgName, 0)) as String
        } catch (ne: PackageManager.NameNotFoundException) {
            ne.printStackTrace()
            ""
        }
    }

    override fun onBackPressed() {
        searchString = ""
        searchKeyEdit.text.clear()
    }

    private fun launch(nm: String) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(appListView.windowToken, 0)
        if (nm == "org.n0.speedy_launch") {
            showHelp()
        } else {
            try {
                startActivity(packageManager.getLaunchIntentForPackage(nm))
            } catch (e: Exception) {
                showAppDeleted()
            }
        }
    }

    private fun updateAppList() {
        val resolveInfoPackageList = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.MATCH_ALL
        )
        resolveInfoPackageList.sortWith(ResolveInfo.DisplayNameComparator(packageManager))
        packageList = resolveInfoPackageList.map { resolveInfo ->
            arrayOf(resolveInfo.activityInfo.packageName, resolveInfo.loadLabel(packageManager) as String)
        }
    }

    private fun clearList() {
        appAdapter.clear()
        packageNamesArrList.clear()
    }

    private fun fetchAllApps() {
        searchString = ""
        searchKeyEdit.text.clear()
        clearList()
        if (!tempShowApp && searchString.isEmpty() && !prefs.getBoolean("show_menu", false)) {
            showApps()
            return
        }
        for (resolver in packageList) {
            appAdapter.add(resolver[1])
            packageNamesArrList.add(resolver[0])
        }
        showApps()
    }

    private fun filterAppList() {
        clearList()
        if (!tempShowApp && searchString.isEmpty() && !prefs.getBoolean("show_menu", false)) {
            return
        }
        for (resolver in packageList) {
            if (resolver[1].lowercase(Locale.getDefault()).contains(searchString)) {
                appAdapter.add(resolver[1])
                packageNamesArrList.add(resolver[0])
            }
        }
        appListView.setSelection(0)
    }

    private fun showApps() {
        appListView.adapter = appAdapter
        appListView.setSelection(0)
    }

    override fun onResume() {
        super.onResume()
        fetchAllApps()
    }
}

