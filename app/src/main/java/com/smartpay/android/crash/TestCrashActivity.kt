package com.smartpay.android.crash

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartpay.android.R

/**
 * Test activity to trigger various types of crashes for testing the crash reporting system
 */
class TestCrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Simple layout with crash test buttons
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(32, 32, 32, 32)
        }

        // Title
        val titleView = android.widget.TextView(this).apply {
            text = "Crash Test Activity"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(titleView)

        // Warning text
        val warningView = android.widget.TextView(this).apply {
            text = "⚠️ Warning: These buttons will crash the app for testing purposes!"
            setTextColor(android.graphics.Color.RED)
            setPadding(0, 0, 0, 32)
        }
        layout.addView(warningView)

        // Runtime Exception button
        val runtimeButton = Button(this).apply {
            text = "Trigger RuntimeException"
            setOnClickListener {
                Toast.makeText(this@TestCrashActivity, "Triggering crash in 2 seconds...", Toast.LENGTH_SHORT).show()
                it.postDelayed({
                    throw RuntimeException("Simulated crash for testing AI Security Agent")
                }, 2000)
            }
        }
        layout.addView(runtimeButton)

        // Null Pointer Exception button
        val nullPointerButton = Button(this).apply {
            text = "Trigger NullPointerException"
            setOnClickListener {
                Toast.makeText(this@TestCrashActivity, "Triggering crash in 2 seconds...", Toast.LENGTH_SHORT).show()
                it.postDelayed({
                    @Suppress("CAST_NEVER_SUCCEEDS")
                    val nullString: String? = null as? String
                    nullString!!.length // This will throw NullPointerException
                }, 2000)
            }
        }
        layout.addView(nullPointerButton)

        // Array Index Out of Bounds button
        val arrayButton = Button(this).apply {
            text = "Trigger ArrayIndexOutOfBoundsException"
            setOnClickListener {
                Toast.makeText(this@TestCrashActivity, "Triggering crash in 2 seconds...", Toast.LENGTH_SHORT).show()
                it.postDelayed({
                    val array = intArrayOf(1, 2, 3)
                    val invalid = array[10] // This will throw ArrayIndexOutOfBoundsException
                }, 2000)
            }
        }
        layout.addView(arrayButton)

        // Illegal State Exception button
        val illegalStateButton = Button(this).apply {
            text = "Trigger IllegalStateException"
            setOnClickListener {
                Toast.makeText(this@TestCrashActivity, "Triggering crash in 2 seconds...", Toast.LENGTH_SHORT).show()
                it.postDelayed({
                    check(false) { "Simulated illegal state for testing" }
                }, 2000)
            }
        }
        layout.addView(illegalStateButton)

        // Out of Memory Error button (use with caution!)
        val oomButton = Button(this).apply {
            text = "Trigger OutOfMemoryError (Use Carefully!)"
            setOnClickListener {
                Toast.makeText(this@TestCrashActivity, "Triggering OOM in 2 seconds...", Toast.LENGTH_SHORT).show()
                it.postDelayed({
                    val list = mutableListOf<ByteArray>()
                    while (true) {
                        // Allocate 10MB chunks until OOM
                        list.add(ByteArray(10 * 1024 * 1024))
                    }
                }, 2000)
            }
        }
        layout.addView(oomButton)

        // Security Exception button
        val securityButton = Button(this).apply {
            text = "Trigger SecurityException"
            setOnClickListener {
                Toast.makeText(this@TestCrashActivity, "Triggering crash in 2 seconds...", Toast.LENGTH_SHORT).show()
                it.postDelayed({
                    // Simulate security exception
                    throw SecurityException("Simulated security violation for testing")
                }, 2000)
            }
        }
        layout.addView(securityButton)

        // Custom SmartPay Exception button
        val customButton = Button(this).apply {
            text = "Trigger Custom SmartPay Exception"
            setOnClickListener {
                Toast.makeText(this@TestCrashActivity, "Triggering crash in 2 seconds...", Toast.LENGTH_SHORT).show()
                it.postDelayed({
                    throw SmartPayTestException("Payment processing failed: Insufficient balance in wallet")
                }, 2000)
            }
        }
        layout.addView(customButton)

        setContentView(layout)
    }

    /**
     * Custom exception for SmartPay specific testing
     */
    class SmartPayTestException(message: String) : Exception(message)
}