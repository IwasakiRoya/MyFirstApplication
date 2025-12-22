package com.example.myfirstapplication;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 默认显示“消息”页面
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new MessageFragment()).commit();

        // 设置点击监听
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_messages) {
                selectedFragment = new MessageFragment();
            } else if (itemId == R.id.nav_contacts) {
                selectedFragment = new ContactFragment();
            } else if (itemId == R.id.nav_me) {
                selectedFragment = new MeFragment();
            }

            if (selectedFragment != null) {
                // 使用 FragmentManager 替换当前容器里的 Fragment
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }
}