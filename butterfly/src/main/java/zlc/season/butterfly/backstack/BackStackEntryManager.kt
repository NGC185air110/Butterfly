package zlc.season.butterfly.backstack

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import zlc.season.butterfly.AgileRequest
import zlc.season.butterfly.internal.ButterflyHelper.AGILE_REQUEST
import zlc.season.butterfly.internal.observeFragmentDestroy
import zlc.season.butterfly.internal.logd
import zlc.season.claritypotion.ActivityLifecycleCallbacksAdapter
import zlc.season.claritypotion.ClarityPotion.application

@Suppress("DEPRECATION")
class BackStackEntryManager {
    companion object {
        private const val KEY_SAVE_STATE = "butterfly_back_stack_state"
    }

    private val backStackEntryMap = mutableMapOf<String, MutableList<BackStackEntry>>()

    init {
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacksAdapter() {
            override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                val intentRequest = activity.intent.getParcelableExtra<AgileRequest>(AGILE_REQUEST)
                if (intentRequest != null) {
                    addEntry(activity, BackStackEntry(intentRequest))
                    activity.intent.removeExtra(AGILE_REQUEST)
                }
                if (savedInstanceState != null) {
                    restoreEntryList(activity, savedInstanceState)
                }
            }

            override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is FragmentActivity) {
                    activity.observeFragmentDestroy {
                        val uniqueId = it.tag
                        if (!uniqueId.isNullOrEmpty()) {
                            removeEntry(activity, uniqueId)
                        }
                    }
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = saveEntryList(activity, outState)

            override fun onActivityDestroyed(activity: Activity) = destroyEntryList(activity)
        })
    }

    @Synchronized
    private fun restoreEntryList(activity: Activity, savedState: Bundle) {
        val data = savedState.getParcelableArrayList<AgileRequest>(KEY_SAVE_STATE)
        if (data != null) {
            val entryList = data.map { BackStackEntry(it) }
            getEntryList(activity).addAll(entryList)

            "BackStack ---> ${activity.key()} restore entry list".logd()
            "BackStack ---> Result -> $backStackEntryMap".logd()
        }
    }

    @Synchronized
    private fun saveEntryList(activity: Activity, outState: Bundle) {
        val list = backStackEntryMap[activity.key()]
        if (!list.isNullOrEmpty()) {
            val savedData = list.mapTo(ArrayList()) { it.request }
            outState.putParcelableArrayList(KEY_SAVE_STATE, savedData)

            "BackStack ---> ${activity.key()} save entry list".logd()
        }
    }

    @Synchronized
    private fun destroyEntryList(activity: Activity) {
        backStackEntryMap.remove(activity.key())

        "BackStack ---> ${activity.key()} destroy entry list".logd()
        "BackStack ---> Result -> $backStackEntryMap".logd()
    }

    @Synchronized
    private fun removeEntry(activity: Activity, uniqueId: String) {
        val find = getEntryList(activity).find { it.request.uniqueId == uniqueId }
        if (find != null) {
            getEntryList(activity).remove(find)

            "BackStack ---> ${activity.key()} removeEntry -> $find".logd()
            "BackStack ---> Result -> $backStackEntryMap".logd()
        }
    }

    @Synchronized
    fun removeTopEntry(activity: Activity): BackStackEntry? {
        val topEntry = getEntryList(activity).removeLastOrNull()

        "BackStack ---> ${activity.key()} removeTopEntry -> $topEntry".logd()
        "BackStack ---> Result -> $backStackEntryMap".logd()

        return topEntry
    }

    @Synchronized
    fun removeEntries(activity: FragmentActivity, entryList: List<BackStackEntry>) {
        getEntryList(activity).removeAll(entryList)

        "BackStack ---> ${activity.key()} removeEntries -> $entryList".logd()
        "BackStack ---> Result -> $backStackEntryMap".logd()
    }

    @Synchronized
    fun addEntry(activity: Activity, entry: BackStackEntry) {
        val list = getEntryList(activity)

        if (isDialogEntry(entry)) {
            list.add(entry)
        } else {
            val dialogEntry = list.firstOrNull { isDialogEntry(it) }
            if (dialogEntry != null) {
                val index = list.indexOf(dialogEntry)
                list.add(index, entry)
            } else {
                list.add(entry)
            }
        }

        "BackStack ---> ${activity.key()} addEntry -> $entry".logd()
        "BackStack ---> Result -> $backStackEntryMap".logd()
    }

    @Synchronized
    fun getTopEntry(activity: Activity): BackStackEntry? {
        val entryList = getEntryList(activity)

        return if (entryList.isEmpty()) {
            null
        } else {
            return entryList.lastOrNull()
        }
    }

    @Synchronized
    fun getTopEntryList(activity: FragmentActivity, request: AgileRequest): MutableList<BackStackEntry> {
        val result = mutableListOf<BackStackEntry>()

        val backStackList = getEntryList(activity)
        val index = backStackList.indexOfLast { it.request.className == request.className }
        if (index != -1) {
            for (i in index until backStackList.size) {
                val entry = backStackList[i]
                result.add(entry)
            }
        }
        return result
    }

    @Synchronized
    private fun getEntryList(activity: Activity): MutableList<BackStackEntry> {
        var backStackList = backStackEntryMap[activity.key()]
        if (backStackList == null) {
            backStackList = mutableListOf()
            backStackEntryMap[activity.key()] = backStackList
        }

        return backStackList
    }

    private fun isDialogEntry(entry: BackStackEntry): Boolean {
        val cls = Class.forName(entry.request.className)
        return DialogFragment::class.java.isAssignableFrom(cls)
    }

    private fun Activity.key(): String {
        return "Activity@${hashCode()}"
    }
}