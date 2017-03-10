package com.skylark.xuelong_li.howold;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by XueLong_Li on 2016/11/13.
 */

public class FaceppDetect {

    public interface CallBack {
        void success(JSONObject result);

        void error(FaceppParseException exception);
    }

    //由于使用了匿名内部类 所以参数最好用final
    public static void detect(final Bitmap bm, final CallBack callBack) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    //创建请求 request
                    //当两个都为true时 请求的连接是http://apicn.faceplusplus.com/v2/
                    HttpRequests requests = new HttpRequests(Constant.KEY, Constant.SECRET, true, true);
                    //bitmap转换为二进制
                    Bitmap bmSmall = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    //将bmSmall压缩到stream中
                    bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] arrays = stream.toByteArray();

                    PostParameters params = new PostParameters();
                    params.setImg(arrays);
                    JSONObject jsonObject = requests.detectionDetect(params);

                    //
                    Log.e("TAG", jsonObject.toString());

                    if (callBack != null) {
                        callBack.success(jsonObject);
                    }

                } catch (FaceppParseException e) {
                    e.printStackTrace();

                    if (callBack != null) {
                        callBack.error(e);
                    }

                }

            }
        }).start();

    }

}
