package com.example.myfirstapplication.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.myfirstapplication.Fragment.ContactsFragment;
import com.example.myfirstapplication.Fragment.MeFragment;
import com.example.myfirstapplication.Fragment.MessageFragment;
import com.example.myfirstapplication.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // 缓存Fragment实例
    private MessageFragment messageFragment;
    private ContactsFragment contactsFragment;
    private MeFragment meFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 初始化Fragment
        messageFragment = new MessageFragment();
        contactsFragment = new ContactsFragment();
        meFragment = new MeFragment();

        // 默认加载消息Fragment（匹配菜单中的ID：nav_message）
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, messageFragment)
                    .commit();
            // 手动选中消息Tab（关键：ID必须和菜单中一致）
            bottomNav.setSelectedItemId(R.id.nav_message);
        }

        // Tab点击监听（全部匹配实际ID：nav_message/nav_contacts/nav_me）
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment targetFragment = null;
            int itemId = item.getItemId();

            // 修复：将 nav_messages → nav_message（匹配你的菜单ID）
            if (itemId == R.id.nav_message) {
                targetFragment = messageFragment;
            } else if (itemId == R.id.nav_contacts) {
                targetFragment = contactsFragment;
            } else if (itemId == R.id.nav_me) {
                targetFragment = meFragment;
            }

            // 仅当Fragment未显示时才替换
            if (targetFragment != null && !targetFragment.isVisible()) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, targetFragment)
                        .commit();
            }
            return true;
        });
    }
}