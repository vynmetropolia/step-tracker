package com.example.steptracker.Fragment

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.steptracker.Object.InternalFileStorageManager.reportDateFile
import com.example.steptracker.Object.InternalFileStorageManager.reportStepFile
import com.example.steptracker.Object.InternalFileStorageManager.stepFile
import com.example.steptracker.R
import com.example.steptracker.sensorsHandler.StepDetector
import com.example.steptracker.sensorsHandler.StepListener
import kotlinx.android.synthetic.main.fragment_today.*
import java.time.LocalDate


class TodayFragment : Fragment(), SensorEventListener, StepListener {

    private var simpleStepDetector: StepDetector? = null
    private var sensorManager: SensorManager? = null
    private var numSteps: Int = 0
    var stepFileList = mutableListOf<String>()
    var reportStepFileList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        readDataFromFile()
        circleTv.text = numSteps.toString()
        sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        simpleStepDetector = StepDetector()
        simpleStepDetector!!.registerListener(this)

        startBtn.setOnClickListener(View.OnClickListener {
            Toast.makeText(context, "Counter is started !", Toast.LENGTH_SHORT).show()
            counterState.text = ""
            sensorManager!!.registerListener(
                this,
                sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST
            )
        })
        pauseBtn.setOnClickListener(View.OnClickListener {
            Toast.makeText(context, "Counter is paused !", Toast.LENGTH_SHORT).show()
            counterState.text = getString(R.string.isPaused)
            sensorManager!!.unregisterListener(this)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_today, container, false)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector!!.updateAccelerometer(
                event.timestamp,
                event.values[0],
                event.values[1],
                event.values[2]
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun step(timeNs: Long) {

        numSteps++
        circleTv.text = numSteps.toString()
        writeDataToFile()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun writeDataToFile() {

        val current = LocalDate.now()
        //mode private = rewrite the file. mode_append = add content to the file
        activity!!.openFileOutput(stepFile, Context.MODE_PRIVATE).use {
            it.write("$numSteps\n".toByteArray())
            it.write("$current\n".toByteArray())    //also write day to compare
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readDataFromFile() {
        activity!!.openFileOutput(stepFile, Context.MODE_APPEND).use {
            it.write("a line test".toByteArray())
        }   //avoid error when open file

        activity!!.openFileInput(stepFile)?.bufferedReader()?.useLines { lines ->
            lines.forEach { stepFileList.add(it) }  //Store data in file to a list
            if (stepFileList.size > 1) {    //check if file is null or empty.
                val current = LocalDate.now()
                if (current.toString() == stepFileList[1])  //Check if it is still a same day
                    numSteps = Integer.parseInt(stepFileList[0])    //Get today step
                else {

                    numSteps = 0    //init a new day record
                    activity!!.openFileOutput(reportStepFile, Context.MODE_APPEND).use {
                        it.write("${stepFileList[0]}\n".toByteArray())
                    }
                    activity!!.openFileInput(reportStepFile)?.bufferedReader()?.useLines { lines ->
                        lines.forEach { reportStepFileList.add(it) }    //Get a record
                    }
                    if (reportStepFileList.size < 8) {  //Check if it is already 7 days in record
                        activity!!.openFileOutput(reportStepFile, Context.MODE_PRIVATE).use {
                            //write again from the beginning due to the test
                            for (i in 0..reportStepFileList.size - 1) {
                                it.write("${reportStepFileList[i]}\n".toByteArray())
                            }
                        }
                        activity!!.openFileOutput(reportDateFile, Context.MODE_APPEND).use {
                            //current.minusDays(1)
                            it.write("${current.minusDays(1)}\n".toByteArray())    // no need for date file because no test here
                        }
                    }
                }
            }
        }


    }
}