package com.seasonfif.dynamicdemo

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.seasonfif.dynamicdemo.LoadUtil.loadJar
import com.seasonfif.loadlib.Worker
import com.seasonfif.modulebase.IPerson

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var loadJar: Button? = null
    private var hotLoad: Button? = null
    private var loadPatch: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }


        loadJar = findViewById(R.id.load_jar)
        hotLoad = findViewById(R.id.hot_load)
        loadPatch = findViewById(R.id.load_patch)
        loadJar!!.setOnClickListener(this)
        hotLoad!!.setOnClickListener(this)
        loadPatch!!.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.load_jar -> {
                var worker = loadJar<IPerson>(this@MainActivity)
                Toast.makeText(this@MainActivity, worker?.eat(), Toast.LENGTH_SHORT).show()
            }

            R.id.hot_load -> {
                try {
                    var worker = Worker()
                    Toast.makeText(this@MainActivity, worker?.eat(), Toast.LENGTH_SHORT).show()
                }catch (e: Throwable){
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }

            R.id.load_patch -> {
                LoadUtil.hotLoad(this@MainActivity)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)

    }
}
