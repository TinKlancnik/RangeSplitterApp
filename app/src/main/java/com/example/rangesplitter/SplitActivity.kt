package com.example.rangesplitter

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplitActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

        // Hide the action bar
        supportActionBar?.hide()

        // Make the navigation bar black
        window.navigationBarColor = resources.getColor(android.R.color.black)

        val spinner = findViewById<Spinner>(R.id.spinnerValues)
        val items = arrayOf(3, 5)
        val adapter = ArrayAdapter(this, R.layout.spinner_item, items)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(0)

        val rangeTopEditText = findViewById<EditText>(R.id.editTextRangeTop)
        val rangeLowEditText = findViewById<EditText>(R.id.editTextRangeLow)
        val calculateButton = findViewById<Button>(R.id.calculateButton)
        val resultTextView = findViewById<TextView>(R.id.resultTextView)

        calculateButton.setOnClickListener {
            val rangeTop = rangeTopEditText.text.toString().toFloatOrNull() ?: 0f
            val rangeLow = rangeLowEditText.text.toString().toFloatOrNull() ?: 0f
            val numberOfValues = spinner.selectedItem as Int
            val results = calculateNumbers(rangeLow, rangeTop, numberOfValues)
            resultTextView.text = results.joinToString("\n")

            closekey(it)
        }
    }
    private fun closekey(view: View){
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun calculateNumbers(low: Float, top: Float, count: Int): List<Float> {
        val step = (top - low) / (count - 1)
        return List(count) { low + it * step }
    }
}
