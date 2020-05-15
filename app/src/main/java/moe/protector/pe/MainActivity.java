package moe.protector.pe;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import de.hdodenhof.circleimageview.CircleImageView;
import moe.protector.pe.activity.FragmentActivity;
import moe.protector.pe.activity.HtmlActivity;
import moe.protector.pe.activity.LoginActivity;
import moe.protector.pe.fragment.ErrorFragment;
import moe.protector.pe.fragment.LogFragment;
import moe.protector.pe.fragment.MainFragment;
import moe.protector.pe.fragment.TaskFragment;
import moe.protector.pe.game.Setting;
import moe.protector.pe.game.TaskManager;
import moe.protector.pe.game.UserData;
import moe.protector.pe.service.MainService;
import moe.protector.pe.util.Config;
import moe.protector.pe.util.EventBusUtil;
import moe.protector.pe.util.FileUtil;

import static moe.protector.pe.activity.FragmentActivity.ERROR_FRAGMENT;
import static moe.protector.pe.util.EventBusUtil.EVENT_FLEET_CHANGE;
import static moe.protector.pe.util.EventBusUtil.EVENT_LOGIN_FINISH;
import static moe.protector.pe.util.EventBusUtil.EVENT_RES_CHANGE;
import static moe.protector.pe.util.EventBusUtil.EVENT_TASK_CHANGE;

public class MainActivity extends AppCompatActivity  {
    private static final String TAG = "MainActivity";
    private UserData userData = UserData.getInstance();
    private DrawerLayout mDrawerLayout;
    private boolean isConnected = false;
    private MainService.MainBinder mainBinder;
    MaterialViewPager materialViewPager;
    public static final int TASK_CHANGE = 1;
    //=========================================
    private ViewPager mViewPager;
    private RadioGroup mTabRadioGroup;
    private List<Fragment> mFragments;
    private FragmentPagerAdapter mAdapter;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new EventBusUtil("MainActivity.onActivityResult", EVENT_RES_CHANGE).post();
        setContentView(R.layout.activity_main);
        // 绑定服务
        Intent bindIntent = new Intent(this, MainService.class);
        isConnected = bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
        // 初始化设置
        Setting.getInstance().init();
        initView();
        // 判断是否登录成功的, 唤起登录activity
        if (!Config.hasLogin) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(intent, LoginActivity.REQUEST_CODE);
        }
    }

    private void initView() {
        // find view
        mViewPager = findViewById(R.id.main_vp);
        mTabRadioGroup = findViewById(R.id.tabs_rg);
        // init fragment
        mFragments = new ArrayList<>(4);
        mFragments.add(MainFragment.getInstance());
        mFragments.add(TaskFragment.getInstance());
        mFragments.add(LogFragment.newInstance());
        mFragments.add(ErrorFragment.getInstance());
        // init view pager
        mAdapter = new MyFragmentPagerAdapter(getSupportFragmentManager(), mFragments);
        mViewPager.setAdapter(mAdapter);
        // register listener
        mViewPager.addOnPageChangeListener(mPageChangeListener);
        mTabRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }

    private ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            RadioButton radioButton = (RadioButton) mTabRadioGroup.getChildAt(position);
            radioButton.setChecked(true);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            for (int i = 0; i < group.getChildCount(); i++) {
                if (group.getChildAt(i).getId() == checkedId) {
                    mViewPager.setCurrentItem(i);
                    return;
                }
            }
        }
    };

    private class MyFragmentPagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> mList;

        public MyFragmentPagerAdapter(FragmentManager fm, List<Fragment> list) {
            super(fm);
            this.mList = list;
        }

        @Override
        public Fragment getItem(int position) {
            return this.mList == null ? null : this.mList.get(position);
        }

        @Override
        public int getCount() {
            return this.mList == null ? 0 : this.mList.size();
        }
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case LoginActivity.REQUEST_CODE: // 登录界面的返回值
                if (resultCode == RESULT_OK) {
                    new EventBusUtil("MainActivity.onActivityResult", EVENT_LOGIN_FINISH, "登录完成").post();
                    new EventBusUtil("MainActivity.onActivityResult", EVENT_RES_CHANGE).post();
                    new EventBusUtil("MainActivity.onActivityResult", EVENT_FLEET_CHANGE).post();
                    new EventBusUtil("MainActivity.onActivityResult", EVENT_TASK_CHANGE).post();
                } else {
                    finish();
                }
                break;
            case HtmlActivity.REQUEST_CODE:  // h5界面返回值
                if (resultCode == TASK_CHANGE) {  // 更新任务界面, 更新ui"));
                    new EventBusUtil("MainActivity.onActivityResult", EVENT_TASK_CHANGE, "任务发生更新, 更新ui").post();
                }
                break;

        }

    }

    long lastPressTime = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (new Date().getTime() - lastPressTime < 1000) {
            moveTaskToBack(false);
        } else {
            lastPressTime = new Date().getTime();  // 重置lastPressTime
            View v = getWindow().getDecorView().findViewById(R.id.mainLayoutId);
            Snackbar.make(v, "再按一次返回桌面", Snackbar.LENGTH_SHORT)
                    .setAction("退出程序", v1 -> {
                        mainBinder.cancelNotification();
                        Intent intent = new Intent(this, MainService.class);
                        stopService(intent);
                        finish();
                        System.exit(0);
                    })
                    .show();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:  // 点击home键
                mDrawerLayout.openDrawer(Gravity.START);
                break;
            case R.id.menu_play:
                TaskManager.isRun = !TaskManager.isRun;
                item.setIcon(TaskManager.isRun ? R.drawable.play : R.drawable.stop);
                View v = getWindow().getDecorView().findViewById(R.id.mainLayoutId);
                Snackbar.make(v, "已" + (TaskManager.isRun ? "开始" : "停止") + "任务", Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case R.id.menu_download:
                Uri uri = Uri.parse(Config.SETU[materialViewPager.getViewPager().getCurrentItem()].replace("mw690", "large"));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
        }
        return true;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mainBinder = (MainService.MainBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginFinish(EventBusUtil util) {
        if (util.getCode() == EVENT_LOGIN_FINISH) {
            // 启动服务
            startService(new Intent(MainActivity.this, MainService.class));
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
                boolean isIgnoreBattery = pm.isIgnoringBatteryOptimizations("moe.protector.pe");
                Log.i(TAG, isIgnoreBattery? "保活": "未保活");
                if (!isIgnoreBattery) {
                    new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("后台运行")
                            .setContentText("为保证此软件在后台正常运行\n向您请求'忽略电池优化'权限\n请在下一个对话框中点击确定")
                            .setConfirmText("确定")
                            .setConfirmClickListener(sweetAlertDialog -> {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                                sweetAlertDialog.cancel();
                            })
                            .setCancelText("算了")
                            .setCancelClickListener(SweetAlertDialog::cancel)
                            .show();
                }
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConnected) {
            unbindService(serviceConnection);
            isConnected = false;
        }
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResChange(EventBusUtil util) {
        // 刷新界面信息
        if (util.getCode() == EVENT_RES_CHANGE) {
            try {
                TextView textView = findViewById(R.id.tv_username);
                textView.setText(userData.userBaseData.friendVo.username);
                textView = findViewById(R.id.tv_sign);
                textView.setText(userData.userBaseData.friendVo.sign);
                CircleImageView imageView = findViewById(R.id.img_mine);
                Bitmap b = FileUtil.getImageFromAssetsFile("html/images/head/" + userData.userBaseData.friendVo.avatar_cid + ".png");
                imageView.setImageBitmap(b);
            } catch (Exception e) {
            }
        }
    }

}
