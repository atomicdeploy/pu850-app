package com.pandcaspian.indicator.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.pandcaspian.indicator.BuildConfig;
import com.pandcaspian.indicator.R;

/**
 * Settings Activity with elegant design
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        setupToolbar();
        setupSettings();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }
    }
    
    private void setupSettings() {
        // Version info
        TextView versionValue = findViewById(R.id.versionValue);
        versionValue.setText(BuildConfig.VERSION_NAME);
        
        // Developer info
        TextView developerValue = findViewById(R.id.developerValue);
        developerValue.setText(R.string.settings_developer_name);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
