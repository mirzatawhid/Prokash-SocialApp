package com.dcoder.prokash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.dcoder.prokash.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false

    private lateinit var binding:ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        binding.apply {
            toggle=ActionBarDrawerToggle(this@MainActivity,binding.drawerLayout,binding.toolbar,R.string.open,R.string.close)
            drawerLayout.addDrawerListener(toggle)
            toggle.syncState()

            setSupportActionBar(toolbar)

            navView.setNavigationItemSelectedListener{
                when(it.itemId){
                    R.id.nav_profile ->{
                        Toast.makeText(this@MainActivity,"Profile pressed",Toast.LENGTH_LONG).show()
                    }
                    R.id.nav_settings ->{}
                    R.id.nav_about_us ->{}
                    R.id.nav_share ->{}
                    R.id.nav_logout ->{
                        signOut()
                    }
                }
                true
            }
        }

        binding.mainProfile.setOnClickListener{
            startActivity(Intent(this@MainActivity,ProfileActivity::class.java))
        }

        binding.submitBtn.setOnClickListener{
            startActivity(Intent(this@MainActivity,ComplaintSubmissionActivity::class.java))
        }

    }

    private fun signOut(){
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.your_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this,gso).signOut()
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this,LoginActivity::class.java))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)){
            true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

    }

}