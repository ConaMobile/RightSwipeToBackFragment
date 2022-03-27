package com.conamobile.rightswipetobackfragment.fragula

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.conamobile.rightswipetobackfragment.fragula.adapter.NavigatorAdapter
import com.conamobile.rightswipetobackfragment.fragula.common.Arg
import com.conamobile.rightswipetobackfragment.fragula.common.BundleBuilder
import com.conamobile.rightswipetobackfragment.fragula.common.FragmentNavigator
import com.conamobile.rightswipetobackfragment.fragula.common.SwipeDirection
import com.fragula.listener.OnFragmentNavigatorListener
import java.io.Serializable

class Navigator : FragmentNavigator {

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    private val fragmentManager: FragmentManager =
        (context as FragmentActivity).supportFragmentManager
    private var navigatorAdapter: NavigatorAdapter? = null
    private var onPageChangeListener: OnPageChangeListener? = null

    var currentFragment: Fragment? = null
        private set
    var previousFragment: Fragment? = null
        private set
    var isBlockTouchEvent: Boolean = false
        private set

    var onPageScrollStateChanged: ((state: Int) -> Unit)? = null
    var onPageSelected: ((position: Int) -> Unit)? = null
    var onNotifyDataChanged: ((fragmentCount: Int) -> Unit)? = null
    var onPageScrolled: ((position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit)? =
        null

    val fragmentCount: Int
        get() = navigatorAdapter?.count ?: 0

    val fragments: List<Fragment>
        get() = navigatorAdapter?.fragments ?: emptyList()

    init {
        initAdapter()
        offscreenPageLimit = OFFSCREEN_PAGE_LIMIT
    }

    @Deprecated("Now not used")
    fun init(fragmentManager: FragmentManager) {
    }

    /**
     * Adds a fragment to the Navigator. Can be used with Bundle arguments.
     * Use [BundleBuilder].
     *
     * For example:
     *  Navigator.addFragment(BlankFragment()) {
     *       "ARGUMENT_KEY_1" to "Example string"
     *       "ARGUMENTS_KEY_2" to 12345
     *  }
     */
    fun addFragment(
        fragment: Fragment,
        builder: (BundleBuilder.() -> Unit)? = null
    ) {
        builder?.let {
            val bb = BundleBuilder()
            bb.builder()
            fragment.arguments = bb.bundle
        }
        addFragment(fragment)
    }

    /**
     * Adds a fragment to the [NavigatorAdapter]
     * and goes to this fragment.
     */
    private fun addFragment(fragment: Fragment) {
        if (fragment.isAdded || navigatorAdapter == null)
            return

        if (navigatorAdapter!!.getFragmentsCount() > 0) {
            isBlockTouchEvent = true
        }
        navigatorAdapter!!.addFragment(fragment)
        post {
            goToNextFragment()
        }
    }

    /**
     * Replaces a fragment in the Navigator. Can be used with Bundle arguments.
     * Use [com.fragula.common.BundleBuilder].
     *
     * Example of replacing the current fragment:
     *  Navigator.replaceFragment(BlankFragment()) {
     *       "ARGUMENT_KEY_1" to "Example string"
     *       "ARGUMENTS_KEY_2" to 12345
     *  }
     *
     *
     * Example of replacing the target fragment:
     *  Navigator.replaceFragment(
     *       fragment = BlankFragment(),
     *       position = 2,
     *       builder = {
     *           "ARGUMENT_KEY_1" to "Example string"
     *           "ARGUMENTS_KEY_2" to 12345
     *       }
     *   )
     */
    fun replaceFragment(
        fragment: Fragment,
        position: Int? = null,
        builder: (BundleBuilder.() -> Unit)? = null
    ) {
        builder?.let {
            val bb = BundleBuilder()
            bb.builder()
            fragment.arguments = bb.bundle
        }
        navigatorAdapter?.replaceFragment(
            position ?: (navigatorAdapter?.count ?: 1) - 1,
            fragment
        )
    }

    private fun initAdapter() {
        navigatorAdapter = NavigatorAdapter(fragmentManager)
        setNavigatorChangeListener()
        adapter = navigatorAdapter
    }

    private fun setNavigatorChangeListener() {
        onPageChangeListener = object : OnPageChangeListener {
            var sumPositionAndPositionOffset = 0f
            var swipeDirection: SwipeDirection = SwipeDirection.NONE
            override fun onPageSelected(position: Int) {
                onPageSelected?.invoke(position)
            }

            override fun onNotifyDataChanged(itemCount: Int) {
                setCurrentFragment()
                setPreviousFragment()
                onNotifyDataChanged?.invoke(itemCount)
            }

            override fun onPageScrollStateChanged(state: Int) {
                isBlockTouchEvent = state == SCROLL_STATE_SETTLING
                when (state) {
                    SCROLL_STATE_IDLE -> {
                        if (navigatorAdapter == null) return
                        if (swipeDirection == SwipeDirection.LEFT) {
                            while ((navigatorAdapter!!.getSizeListOfFragments() - 1) > currentItem) {
                                navigatorAdapter?.removeLastFragment()
                            }
                            if (currentFragment != null && currentFragment is OnFragmentNavigatorListener) {
                                (currentFragment as OnFragmentNavigatorListener).onReturnedFragment()
                            }
                        } else {
                            if (currentFragment != null && currentFragment is OnFragmentNavigatorListener) {
                                (currentFragment as OnFragmentNavigatorListener).onOpenedFragment()
                            }
                        }
                    }
                }
                onPageScrollStateChanged?.invoke(state)
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                swipeDirection =
                    if (position + positionOffset < sumPositionAndPositionOffset) SwipeDirection.LEFT
                    else SwipeDirection.RIGHT
                sumPositionAndPositionOffset = position + positionOffset
                onPageScrolled?.invoke(position, positionOffset, positionOffsetPixels)
            }
        }
        addOnPageChangeListener(onPageChangeListener!!)
    }

    private fun setCurrentFragment() {
        if (navigatorAdapter == null) return
        if (navigatorAdapter!!.fragments.isNotEmpty()) {
            currentFragment = fragments.last()
        }
    }

    private fun setPreviousFragment() {
        if (navigatorAdapter == null) return
        previousFragment = if (fragmentCount > 1) {
            fragments[fragmentCount - 2]
        } else {
            null
        }
    }

    fun release() {
        onPageChangeListener?.let {
            removeOnPageChangeListener(it)
        }
        onPageChangeListener = null
        onPageScrollStateChanged = null
        onPageSelected = null
        onNotifyDataChanged = null
        onPageScrolled = null
        currentFragment = null
        previousFragment = null
    }

    @Deprecated("Use BundleBuilder")
    private fun getBundle(vararg args: Arg<*, *>): Bundle {
        val bundle = Bundle()
        for (arg in args) {
            val key = arg.key as String?
            val value = arg.value

            if (value is Boolean) {
                bundle.putBoolean(key, (value as Boolean?)!!)
            }
            if (value is Byte) {
                bundle.putByte(key, (value as Byte?)!!)
            }
            if (value is Char) {
                bundle.putChar(key, (value as Char?)!!)
            }
            if (value is Int) {
                bundle.putInt(key, (value as Int?)!!)
            }
            if (value is Long) {
                bundle.putLong(key, (value as Long?)!!)
            }
            if (value is Float) {
                bundle.putFloat(key, (value as Float?)!!)
            }
            if (value is Double) {
                bundle.putDouble(key, (value as Double?)!!)
            }
            if (value is String) {
                bundle.putString(key, value as String?)
            }
            if (value is Parcelable) {
                bundle.putParcelable(key, value as Parcelable?)
            }
            if (value is Serializable) {
                bundle.putSerializable(key, value as Serializable?)
            }
        }
        return bundle
    }

    @Deprecated(
        message = "Use addFragment with BundleBuilder",
        replaceWith = ReplaceWith(
            expression = "addFragment(fragment) { }",
            imports = ["com.conamobile.rightswipetobackfragment.fragula.Navigator"]
        )
    )
    fun addFragment(
        fragment: Fragment,
        vararg args: Arg<*, *>
    ) {
        fragment.arguments = getBundle(*args)
        addFragment(fragment)
    }

    @Deprecated(
        message = "Use replaceFragment with BundleBuilder",
        replaceWith = ReplaceWith(
            expression = "replaceFragment(newFragment) { }",
            imports = ["com.conamobile.rightswipetobackfragment.fragula.Navigator"]
        )
    )
    fun replaceCurrentFragment(newFragment: Fragment, vararg args: Arg<*, *>) {
        newFragment.arguments = getBundle(*args)
        replaceCurrentFragment(newFragment)
    }

    @Deprecated(
        message = "Use replaceFragment with BundleBuilder",
        replaceWith = ReplaceWith(
            expression = "replaceFragment(newFragment)",
            imports = ["com.conamobile.rightswipetobackfragment.fragula.Navigator"]
        )
    )
    fun replaceCurrentFragment(newFragment: Fragment) {
        navigatorAdapter?.replaceFragment((navigatorAdapter?.count ?: 1) - 1, newFragment)
    }

    @Deprecated(
        message = "Use replaceFragment with BundleBuilder",
        replaceWith = ReplaceWith(
            expression = "replaceFragment(newFragment, position) { }",
            imports = ["com.conamobile.rightswipetobackfragment.fragula.Navigator"]
        )
    )
    fun replaceFragmentByPosition(newFragment: Fragment, position: Int, vararg args: Arg<*, *>) {
        newFragment.arguments = getBundle(*args)
        navigatorAdapter?.replaceFragment(position, newFragment)
    }

    @Deprecated(
        message = "Use replaceFragment with BundleBuilder",
        replaceWith = ReplaceWith(
            expression = "replaceFragment(newFragment, position)",
            imports = ["com.conamobile.rightswipetobackfragment.fragula.Navigator"]
        )
    )
    fun replaceFragmentByPosition(newFragment: Fragment, position: Int) {
        navigatorAdapter?.replaceFragment(position, newFragment)
    }

    @Deprecated("Use Navigator.fragmentCount")
    fun fragmentsCount(): Int = navigatorAdapter?.count ?: 0

    @Deprecated("Use Navigator.fragments")
    fun fragments(): ArrayList<Fragment>? {
        return navigatorAdapter?.fragments
    }

    override fun onDetachedFromWindow() {
        release()
        super.onDetachedFromWindow()
    }

    companion object {
        private const val OFFSCREEN_PAGE_LIMIT = 100
    }
}