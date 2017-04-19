package com.tujun.myapplication.bletooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.tujun.myapplication.bletooth.utils.ClsUtils;
import com.tujun.myapplication.bletooth.utils.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    @BindView(R.id.button)
    Button button;
    @BindView(R.id.button_client)
    Button buttonClient;
    @BindView(R.id.lvDevices)
    ListView lvDevices;
    @BindView(R.id.ConstraintLayout)
    android.support.constraint.ConstraintLayout ConstraintLayout;
    @BindView(R.id.et_client)
    EditText etClient;
    @BindView(R.id.button_clientFile)
    Button buttonClientFile;
    @BindView(R.id.button_clientImg)
    Button buttonClientImg;

    private String TAG = "MainActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private List<String> bluetoothDevices = new ArrayList<String>();
    private ArrayAdapter<String> arrayAdapter;
    private final UUID MY_UUID = UUID
            .fromString("abcd1234-ab12-ab12-ab12-abcdef123456");//随便定义一个
    private BluetoothSocket clientSocket;
    private BluetoothDevice device;
    private OutputStream os;
    private InputStream clientIs;//蓝牙客服端输出流；
    private AcceptThread acceptThread;
    private final String NAME = "Bluetooth_Socket";
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;
    private InputStream is;//蓝牙服务端输入流
    private OutputStream serverOs;//蓝牙服务端输出流；
    private static String ioString = "01";
    private static String ioFile = "02";
    private static String ioImg = "03";
    File file;
    //图片存储路径
    File fileImg;
    //蓝牙获取的文件存储路径
    File serverFile;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0x01:
                    Toast.makeText(getApplicationContext(), String.valueOf(msg.obj));
                    break;
                case 0x02:
                    Toast.makeText(getApplicationContext(), String.valueOf(msg.obj));
                    break;
                case 0x03:
                    Toast.makeText(getApplicationContext(), String.valueOf(msg.obj));
                    break;
                case 0x04:
                    Toast.makeText(getApplicationContext(), String.valueOf(msg.obj));
                    break;
            }
        }
    };
    //蓝牙客服端读取数据；
    byte[] clientByte=new byte[100];
    int clientcount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        //动态申请Sdcard读写权限；
        verifyStoragePermissions(this);
        //获取应用扩展内存路径
        File diskFile = this.getExternalCacheDir();
        String files = diskFile.getPath();
        Log.e(TAG, files);
        String text = String.format("%s/%s", files, "text.txt");
        //存储文件路径
        serverFile = new File(String.format("%s/%s", getApplicationContext().getExternalCacheDir().getPath(), "text1.txt"));
        //存储图片路径
        fileImg = new File(String.format("%s/%s", getApplicationContext().getExternalCacheDir().getPath(), "1.jpg"));
        //上传文件路径
        file = new File(text);
        //判断文件是否存在，不存在时创建新文件
        if (!file.exists()) {
            Log.e(TAG, "file is not exists");
            boolean isSuccess = false;
            //写入文字
            try {
                isSuccess = file.createNewFile();
                Log.e(TAG, file.getPath());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            OutputStream outf = new FileOutputStream(file);
                            BufferedOutputStream boutf = new BufferedOutputStream(outf);
                            try {
                                boutf.write("写入数据成功fff写入数据成功fff写入数据成功fff写入数据成功fff".getBytes("utf-8"));
                                boutf.flush();
                                boutf.close();
                                Log.e(TAG, "写入数据成功");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (isSuccess) {
                Log.e(TAG, "file mkdirs success");
            }
        }

//        Log.e(TAG,String.format("%s,%s","hi","what is your name") );
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        mBluetoothAdapter.enable();
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        startActivity(discoverableIntent);

        acceptThread = new AcceptThread();
        acceptThread.start();
        // 设置广播信息过滤
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setProgressBarVisibility(true);
                setTitle("正在扫描……");
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                    setTitle("从新扫描……");
                }
                mBluetoothAdapter.startDiscovery();
            }
        });
        buttonClientFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    os.write(ioFile.getBytes("utf-8"));
                    os.flush();
                    //上传的数据最大990；
                    BufferedInputStream bin = new BufferedInputStream(new FileInputStream(file));
                    byte[] bytes = new byte[990];
                    int count = 0;
                    long time1 = System.currentTimeMillis();
                    while ((count = bin.read(bytes, 0, bytes.length)) != -1) {
                        //上传数据，不能是os.write(bytes);
                        os.write(bytes, 0, count);
                        os.flush();
                        Log.e(TAG, new String(bytes, 0, count, "UTF-8"));
                    }
                    long time2 = System.currentTimeMillis();
                    Log.e(TAG, (time2 - time1) + "");
                    Log.e(TAG, "蓝牙传输数据完成");
                    clientcount=clientIs.read(clientByte);
                    Message msg = new Message();
                    msg.what = 0x02;
                    msg.obj = new String(clientByte, 0, clientcount, "utf-8");
                    handler.sendMessage(msg);
                    bin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        buttonClientImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    os.write(ioImg.getBytes("utf-8"));
                    os.flush();
                    BufferedInputStream bin = new BufferedInputStream(new FileInputStream(fileImg));
                    byte[] bytes = new byte[990];
                    int count = 0;
                    long time1 = System.currentTimeMillis();
                    while ((count = bin.read(bytes, 0, bytes.length)) != -1) {
                        os.write(bytes, 0, count);
                        os.flush();
                        Log.e(TAG, new String(bytes, 0, count, "UTF-8"));
                    }
                    long time2 = System.currentTimeMillis();
                    Log.e(TAG, (time2 - time1) + "");
                    clientcount=clientIs.read(clientByte);
                    Message msg = new Message();
                    msg.what = 0x03;
                    msg.obj = new String(clientByte, 0, clientcount, "utf-8");
                    handler.sendMessage(msg);
                    Log.e(TAG, "蓝牙图片传输完成");
                    bin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        buttonClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    os.write(ioString.getBytes("utf-8"));
                    os.flush();
                    os.write(etClient.getText().toString().trim().getBytes("utf-8"));
                    os.flush();
                    etClient.setText("");
                    clientcount=clientIs.read(clientByte);
                    Message msg = new Message();
                    msg.what = 0x04;
                    msg.obj = new String(clientByte, 0, clientcount, "utf-8");
                    handler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                bluetoothDevices.add(device.getName() + ":" + device.getAddress());
            }
        }
        arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, bluetoothDevices);
        lvDevices.setAdapter(arrayAdapter);
        lvDevices.setOnItemClickListener(this);//Activity实现OnItemClickListener接口
        //每搜索到一个设备就会发送一个该广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);
        //当全部搜索完后发送该广播
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        this.registerReceiver(receiver, filter);
    }

    //动态申请sdcard读取和写入软件；
    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permissios = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            // 没有写的权限，去申请写的权限，会弹出对话框
            if (permissios != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        String pin = "1234";  //此处为你要连接的蓝牙设备的初始密钥，一般为1234或0000

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //如果找到这个设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //获得这个设备的信息
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //如果没有被配对过，再添加
                boolean getname = false;
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    //查看是否有添加过
                    for (int i = 0; i < bluetoothDevices.size(); i++) {
                        if (bluetoothDevices.get(i).equals(device.getName() + ":" + device.getAddress())) {
                            getname = true;
                        }
                    }
                    if (!getname) {
                        bluetoothDevices.add(device.getName() + ":" + device.getAddress());
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setTitle("扫描完成");
            } else if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e(TAG, "确认配对");

                try {
                    //1.确认配对
                    Log.e(TAG, "p1");
                    ClsUtils.setPairingConfirmation(device.getClass(), device, true);
                    //2.终止有序广播
                    Log.e(TAG, "p2");
                    Log.i("order...", "isOrderedBroadcast:" + isOrderedBroadcast() + ",isInitialStickyBroadcast:" + isInitialStickyBroadcast());
                    abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                    //3.调用setPin方法进行配对...
                    Log.e(TAG, "p3");
                    boolean ret = ClsUtils.setPin(device.getClass(), device, pin);
                    Log.e(TAG, "p4");
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String s = arrayAdapter.getItem(position);
        String address = s.substring(s.indexOf(":") + 1).trim();//把地址解析出来
        //主动连接蓝牙服务端
        try {
            //停止搜索
                mBluetoothAdapter.cancelDiscovery();
            if (device == null) {
                //获得远程设备
                device = mBluetoothAdapter.getRemoteDevice(address);
            }
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                Log.e(TAG, "请求配对");
                ClsUtils.createBond(device.getClass(), device);
            }
            try {
                if (clientSocket == null) {
                    Log.e(TAG, "clientSocket is not null");

                    //创建客户端蓝牙Socket
                    clientSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    //开始连接蓝牙，如果没有配对则弹出对话框提示我们进行配对
                    clientSocket.connect();
                    //获得输出流（客户端指向服务端输出文本）
                    os = clientSocket.getOutputStream();
                    clientIs=clientSocket.getInputStream();
                }
            } catch (Exception e) {
                Log.e(TAG, "客服端断开");
            }
            if (os != null) {
                Log.e(TAG, "os is not null");
                //往服务端写信息
                //utf-8格式数字英文站一个字节，中文站三个字节；
                os.write(ioString.getBytes("utf-8"));
                os.flush();
                os.write("蓝牙信息来了".getBytes("utf-8"));
                os.flush();
                clientcount=clientIs.read(clientByte);
                Message msg = new Message();
                msg.what = 0x04;
                msg.obj = new String(clientByte, 0, clientcount, "utf-8");
                handler.sendMessage(msg);
            }
        } catch (Exception e) {

        }
    }
    public void init(){
        acceptThread = new AcceptThread();
        acceptThread.start();
    }
    class AcceptThread extends Thread {
        int count=0;
        public AcceptThread() {
            try {
                serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (Exception e) {
            }
        }

        public void run() {
            try {
                Log.e(TAG, "s1");
                //链接蓝牙
                socket = serverSocket.accept();
                Log.e(TAG, "s2");
                serverSocket.close();
                Log.e(TAG, "s3");
                Log.e(TAG, "链接成功");
                //获取io流
                is = socket.getInputStream();
                serverOs=socket.getOutputStream();
                while (true) {
                    Log.e(TAG, "1");
                    byte[] buffer = new byte[2];
                    byte[] bufferdata = new byte[990];
                    Log.e(TAG, "2");
                    count = is.read(buffer);
                    String s = new String(buffer, 0, count);
                    Log.e(TAG, s);
                    //判断传入的数据是什么类型的
                    if (s.equals(ioString)) {
                        Log.e(TAG, "传入字符串");
                        count = is.read(bufferdata);
                        serverOs.write("字符串传输完成".getBytes("utf-8"));
                        Log.e(TAG, count + "s");
                        Message msg = new Message();
                        msg.what = 0x01;
                        msg.obj = new String(bufferdata, 0, count, "utf-8");
                        handler.sendMessage(msg);
                    } else if (s.equals(ioFile)) {
                        Log.e(TAG, "传入文件");
                        if (!serverFile.exists()) {
                            serverFile.createNewFile();
                        }
                        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(serverFile));
                        boolean boofile = true;
                        while (boofile) {
                            if ((count = is.read(bufferdata)) == 990) {
                                boofile = true;
                            } else {
                                boofile = false;
                            }
                            Log.e(TAG, count + "f");
                            Log.e(TAG, "3");
                            bout.write(bufferdata, 0, count);
                            bout.flush();
                        }
                        serverOs.write("文件传输完成".getBytes("utf-8"));
                        Log.e(TAG, "4");
                        bout.close();
                    } else if (s.equals(ioImg)) {
                        Log.e(TAG, "传入图片");
                        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(fileImg));
                        boolean boofile = true;
                        long time1 = System.currentTimeMillis();
                        //循环获取数据，
                        while (boofile) {
                            //最大的获取990个字节，当获取的数据小于990个字节说明是最后一次上传的数据；结束循环等待下次上传的信息；
                            if ((count = is.read(bufferdata)) == 990) {
                                boofile = true;
                            } else {
                                boofile = false;
                            }
                            Log.e(TAG, count + "f");
                            Log.e(TAG, "5");
                            bout.write(bufferdata, 0, count);
                            bout.flush();
                        }
                        long time2 = System.currentTimeMillis();
                        serverOs.write("图片传输完成".getBytes("utf-8"));
                        //大概数据上传的速率为100k/s;
                        Log.e(TAG, (time2 - time1) + "");
                        Log.e(TAG, "6");
                        bout.close();
                    }
                }
                //catch执行出错时处理；
            } catch (Exception e) {
                if(count!=0){
                    init();
                }
                try {
                    Log.e(TAG, "serverSocket" );
                    serverSocket.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            }

        }
    }
}
