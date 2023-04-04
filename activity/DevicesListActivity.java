package com.bluetooth.heart.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluetooth.heart.app.App;
import com.bluetooth.heart.log.ZLog;
import com.bluetooth.heart.utils.Utils;
import com.bluetooth.smart.heart.BuildConfig;
import com.bluetooth.smart.heart.R;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.tbruyelle.rxpermissions3.RxPermissions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DevicesListActivity extends AppCompatActivity {


    private SmartRefreshLayout mSmartRefreshLayout = null;
    private RecyclerView mRecyclerView = null;
    private DeviceAdapter mDeviceAdapter = null;
    private List<SearchResult> mList = new ArrayList<>();
    private Map<String, String> mMap = new HashMap<>();
    private String DEVICE_NAME = "HUST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices_list);

        findViewById(R.id.test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ControlActivity.class);
                startActivity(intent);
            }
        });

        if(BuildConfig.DEBUG)
        {
            findViewById(R.id.test).setVisibility(View.VISIBLE);
        }


        mSmartRefreshLayout = findViewById(R.id.refresh_layout);
        mSmartRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                mList.clear();
                mMap.clear();
                mDeviceAdapter.notifyDataSetChanged();
                searchDevice();
                mSmartRefreshLayout.finishRefresh();
            }
        });


        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mDeviceAdapter = new DeviceAdapter(R.layout.layout_device_item, mList);
        mRecyclerView.setAdapter(mDeviceAdapter);
        mDeviceAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
                App.getInstance().mClient.stopSearch();
                Intent intent = new Intent(getApplicationContext(), ControlActivity.class);
                intent.putExtra(Utils.DATA, mList.get(position));
                startActivity(intent);
            }
        });

        requestPermissions();


    }

    private void requestPermissions() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
                .request(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (granted) { // Always true pre-M
                        // I can control the camera now

                        if (App.getInstance().mClient.isBluetoothOpened()) {
                            searchDevice();
                        } else {
                            App.getInstance().mClient.openBluetooth();
                        }
                        initLog();
                    } else {
                        // Oups permission denied
                    }
                });
    }

    private void initLog()
    {
        //getExternalFilesDir
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ZLog.Init(String.format("%s/log/",  Environment.getExternalStorageDirectory().getPath()));
        }
        else
        {
            ZLog.Init(String.format("%s/log/",  getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)));
        }

    }

    private void searchDevice() {
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(5000, 1).build();

        App.getInstance().mClient.search(request, mSearchResponse);
    }

    private final SearchResponse mSearchResponse = new SearchResponse() {
        @Override
        public void onSearchStarted() {
            BluetoothLog.w("MainActivity.onSearchStarted");
        }

        @Override
        public void onDeviceFounded(SearchResult device) {
            if (device.getName() != null && device.getName().toLowerCase().contains(DEVICE_NAME.toLowerCase())) {
                if (!mMap.containsKey(device.getAddress())) {
                    mMap.put(device.getAddress(), device.getAddress());
                    mList.add(device);
                    mDeviceAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onSearchStopped() {
            BluetoothLog.w("MainActivity.onSearchStopped");
        }

        @Override
        public void onSearchCanceled() {
            BluetoothLog.w("MainActivity.onSearchCanceled");
        }
    };


    class DeviceAdapter extends BaseQuickAdapter<SearchResult, BaseViewHolder> {

        public DeviceAdapter(int layoutResId, @Nullable List<SearchResult> data) {
            super(layoutResId, data);
        }

        @Override
        public int getItemCount() {
            return super.getItemCount();
        }

        @Override
        protected void convert(@NotNull BaseViewHolder baseViewHolder, SearchResult searchResult) {
            baseViewHolder.setText(R.id.name, searchResult.getName());
            baseViewHolder.setImageResource(R.id.rssi, getSingnalImage(Math.abs(searchResult.rssi)));
        }
    }


    private int getSingnalImage(int rssi) {

        int status = rssi / 20;
        BluetoothLog.d(String.valueOf(rssi));
        switch (status) {
            case 0:
                return R.drawable.signal_5;
            case 1:
                return R.drawable.signal_4;
            case 2:
                return R.drawable.signal_3;
            case 3:
                return R.drawable.signal_2;
            case 4:
                return R.drawable.signal_1;
        }

        return R.drawable.signal_4;
    }

    private void test() {
        byte[] data = {0x00, 0x00, (byte) 0xF0, 0x08, (byte) 0xFF, (byte) 0xFF, 0x25, 0x1A, 0x00, 0x00, (byte) 0xD6, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x63, 0x27};

        byte[] tem = new byte[2];
        tem[0] = data[2];
        tem[1] = data[3];
        int value = Utils.byteToInt(tem, 2);
        Utils.LogOut(String.valueOf(value));
        tem[0] = data[4];
        tem[1] = data[5];
        value = Utils.byteToInt(tem, 2);
        Utils.LogOut(String.valueOf(value));

        tem = new byte[4];
        tem[0] = data[6];
        tem[1] = data[7];
        tem[2] = data[8];
        tem[3] = data[9];
        value = Utils.byteToInt(tem, 4);
        Utils.LogOut(String.valueOf(value));

        tem = new byte[4];
        tem[0] = data[10];
        tem[1] = data[11];
        tem[2] = data[12];
        tem[3] = data[13];
        value = Utils.byteToInt(tem, 4);
        Utils.LogOut(String.valueOf(value));
    }
}