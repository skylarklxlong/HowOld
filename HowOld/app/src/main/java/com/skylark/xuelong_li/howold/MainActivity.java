package com.skylark.xuelong_li.howold;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static android.R.attr.id;
import static android.graphics.BitmapFactory.decodeFile;
import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICK_CODE = 0X1001;
    private static final int MSG_SUCCESS = 0X1002;
    private static final int MSG_ERROR = 0X1003;
    private static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 0X1004;
    private ImageView imageView;
    private Button btnGetImage, btnDetect;
    private TextView tv_Tip, tv_age_and_gender;
    private View mWaiting;

    private String mCurrentPhotoStr;
    //压缩后的图片
    private Bitmap mPhotoImg;
    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        initData();

        initEvent();

        mPaint = new Paint();
    }

    private void initEvent() {

        btnGetImage.setOnClickListener(this);
        btnDetect.setOnClickListener(this);
    }

    private void initData() {


    }

    public void initView() {
        imageView = (ImageView) findViewById(R.id.iv_iamge);
        btnGetImage = (Button) findViewById(R.id.btn_get_image);
        btnDetect = (Button) findViewById(R.id.btn_detect);
        tv_Tip = (TextView) findViewById(R.id.tv_tip);
        mWaiting = findViewById(R.id.fl_waiting);
        tv_age_and_gender = (TextView) findViewById(R.id.tv_age_and_gender);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        ContentResolver resolver = getContentResolver();

        if (requestCode == PICK_CODE) {
            if (data != null) {
                Uri uri = data.getData();
                Log.e("TAG", uri.toString());

                Log.e("TAG", uri.toString().substring(7));

                mCurrentPhotoStr =  uri.toString().substring(7);

//                try {
//                    Bitmap bmqqqqq = MediaStore.Images.Media.getBitmap(resolver, uri);
//                    imageView.setImageBitmap(bmqqqqq);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                //这样获取路径一直出问题
//                String[] proj = {MediaStore.Images.Media.DATA};
//                Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
//                cursor.moveToFirst();
//                //拿到索引
//                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
//                //拿到图片的路径
//                mCurrentPhotoStr = cursor.getString(idx);
//                Log.e("TAG", mCurrentPhotoStr + "=======");
//
//
//                cursor.close();

                //face++的sdk要求(图片转换成二进制)图片不能大于3M
                //所以要对图片进行压缩操作
                resizePhoto();

                imageView.setImageBitmap(mPhotoImg);
                tv_Tip.setText("Click Detect ==>");

            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 压缩图片
     */
    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //至获取图片的尺寸信息 而不去加载图片
        options.inJustDecodeBounds = true;

        decodeFile(mCurrentPhotoStr, options);

        //拿到一个合适的缩放比 高和宽尽可能小于1024  注意最好不要用手机屏幕去修改缩放比  因为不同手机分辨率不一
        double ratio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024f);

        //Math.ceil(x) 返回一个大于x的最小整数
        options.inSampleSize = (int) Math.ceil(ratio);
        options.inJustDecodeBounds = false;
        //压缩后的图片
        mPhotoImg = BitmapFactory.decodeFile(mCurrentPhotoStr, options);

    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_SUCCESS:
                    //先隐藏progressbar
                    mWaiting.setVisibility(View.GONE);
                    JSONObject rs = (JSONObject) msg.obj;
                    //解析json数据
                    prepareRsBitmap(rs);
                    //将绘制的结果复制给mPhotoImg了 就是有脸部信息的气泡框框
                    imageView.setImageBitmap(mPhotoImg);
                    break;
                case MSG_ERROR:
                    //先隐藏progressbar
                    mWaiting.setVisibility(View.GONE);
                    String errormsg = (String) msg.obj;
                    if (TextUtils.isEmpty(errormsg)) {
                        tv_Tip.setText("Error");
                    } else {
                        tv_Tip.setText(errormsg);
                    }

                    break;
            }

            super.handleMessage(msg);
        }
    };

    private void prepareRsBitmap(JSONObject rs) {

        Bitmap bitmap = Bitmap.createBitmap(mPhotoImg.getWidth(), mPhotoImg.getHeight(), mPhotoImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mPhotoImg, 0, 0, null); //将原图绘制在新的bitmap上

        try {
            JSONArray faces = rs.getJSONArray("face");
            int faceCount = faces.length();//拿到图片中有多少张脸
            tv_Tip.setText("find " + faceCount);

            for (int i = 0; i < faceCount; i++) {
                //拿到每一个脸的信息
                JSONObject face = faces.getJSONObject(i);
                //解析face的信息
                JSONObject position = face.getJSONObject("position");
                JSONObject center = position.getJSONObject("center");

                // 脸的中心位置  x表示相对于图片x方向(宽)的百分比  y表示相对于图片y方向(高)的百分比
                float x = (float) center.getDouble("x");
                float y = (float) center.getDouble("y");
                //脸的宽和高
                float width = (float) position.getDouble("width");
                float height = (float) position.getDouble("height");
                //转换为在图片中的实际值
                x = x / 100 * bitmap.getWidth();
                y = y / 100 * bitmap.getHeight();
                width = width / 100 * bitmap.getWidth();
                height = height / 100 * bitmap.getHeight();
                //开始绘制脸部的气泡
                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(3);//宽度为3像素
                //画box
                canvas.drawLine(x - width / 2, y - height / 2, x - width / 2, y + height / 2, mPaint);//左边竖线
                canvas.drawLine(x - width / 2, y - height / 2, x + width / 2, y - height / 2, mPaint);//上边横线
                canvas.drawLine(x + width / 2, y - height / 2, x + width / 2, y + height / 2, mPaint);//右边竖线
                canvas.drawLine(x - width / 2, y + height / 2, x + width / 2, y + height / 2, mPaint);//下边横线

                //get age and gender
                JSONObject attribute = face.getJSONObject("attribute");
                JSONObject ageObj = attribute.getJSONObject("age");
                JSONObject genderObj = attribute.getJSONObject("gender");

                int age = ageObj.getInt("value");
                String gender = genderObj.getString("value");

                Bitmap ageAndgenderBitmap = BuildAgeAndGenderBitmap(age, "Male".equals(gender));

                //要控制气泡的大小 不能写死 根据图片占imgeview的比例来控制气泡的大小
                int agegenderWidth = ageAndgenderBitmap.getWidth();
                int agegenderHeight = ageAndgenderBitmap.getHeight();

                if (bitmap.getWidth() < imageView.getWidth() && bitmap.getHeight() < imageView.getHeight()) {
                    float ratio = Math.max(bitmap.getWidth() * 1.0f / imageView.getWidth() * 1.0f,
                            bitmap.getHeight() * 1.0f / imageView.getHeight() * 1.0f);
                    //按比例来绘制
                    ageAndgenderBitmap = Bitmap.createScaledBitmap(ageAndgenderBitmap,
                            (int) (agegenderWidth * ratio), (int) (agegenderHeight * ratio), false);
                }

                // 将气泡绘制出来  图片的左边和上边
                canvas.drawBitmap(ageAndgenderBitmap, x - ageAndgenderBitmap.getWidth() / 2,
                        y - height / 2 - ageAndgenderBitmap.getHeight(), null);

                mPhotoImg = bitmap;//将绘制好的bitmap再赋值给mPhotoImg


            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private Bitmap BuildAgeAndGenderBitmap(int age, boolean IsMale) {

        tv_age_and_gender.setText(age + "");
        if (IsMale) {
            tv_age_and_gender.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null, null);

        } else {
            tv_age_and_gender.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
        }

        tv_age_and_gender.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv_age_and_gender.getDrawingCache());
        tv_age_and_gender.destroyDrawingCache();

        return bitmap;
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.btn_get_image:

                //android 6.0 弹出申请权限对话框
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_CALL_PHONE);
                } else {
                    choosePhoto();

                }


                break;
            case R.id.btn_detect:
                //显示progressbar
                mWaiting.setVisibility(View.VISIBLE);

                if (mCurrentPhotoStr != null && !mCurrentPhotoStr.trim().equals("")) {
                    // 当图片路径不为空时 直接调用压缩图片的方法
                    resizePhoto();
                } else {
                    //当用户不点击 get image按钮时  设置默认的图片
                    mPhotoImg = BitmapFactory.decodeResource(getResources(), R.drawable.t4);
                }

                FaceppDetect.detect(mPhotoImg, new FaceppDetect.CallBack() {
                    @Override
                    public void success(JSONObject result) {
                        Message msg = Message.obtain();
                        msg.what = MSG_SUCCESS;
                        msg.obj = result;
                        mHandler.sendMessage(msg);
                    }

                    @Override
                    public void error(FaceppParseException exception) {
                        Message msg = Message.obtain();
                        msg.what = MSG_ERROR;
                        msg.obj = exception.getErrorMessage();
                        mHandler.sendMessage(msg);
                    }
                });
                break;

        }

    }

    private void choosePhoto() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_CALL_PHONE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                choosePhoto();
            } else {
                // Permission Denied
                Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
/**
 * {
 * "face": [
 * {
 * "position": {
 * "mouth_right": {
 * "y": 41.644187,
 * "x": 52.004667
 * },
 * "mouth_left": {
 * "y": 40.521379,
 * "x": 45.826333
 * },
 * "center": {
 * "y": 34.482759,
 * "x": 49.666667
 * },
 * "height": 22.660099,
 * "width": 15.333333,
 * "nose": {
 * "y": 35.549852,
 * "x": 49.227
 * },
 * "eye_left": {
 * "y": 29.57133,
 * "x": 45.772667
 * },
 * "eye_right": {
 * "y": 30.156552,
 * "x": 53.647333
 * }
 * },
 * "attribute": {
 * "race": {
 * "value": "Asian",
 * "confidence": 95.0296
 * },
 * "gender": {
 * "value": "Male",
 * "confidence": 99.8971
 * },
 * "smiling": {
 * "value": 1.69544
 * },
 * "age": {
 * "value": 12,
 * "range": 5
 * }
 * },
 * "tag": "",
 * "face_id": "4862e6f618cf9715fe378d1662173827"
 * }
 * ],
 * "session_id": "dd8cc87b0d9e45af9102ac4ce265c494",
 * "img_height": 203,
 * "img_width": 300,
 * "img_id": "3b6fbdbe3047bc62be410d2025e5b8de",
 * "url": null,
 * "response_code": 200
 * }
 */
