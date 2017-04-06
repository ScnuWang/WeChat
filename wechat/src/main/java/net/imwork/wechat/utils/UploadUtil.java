package net.imwork.wechat.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Jason.
 * Date 2017/4/6 13:16
 */
public class UploadUtil {
    /**
     * 上传临时文件
     * @param accessToken
     * @param type
     * @param mediaFileUrl 这里的文件地址是网上的可以访问的地址
     * @return
     */
    public static String uploadMediaTemp(String accessToken, String type, String mediaFileUrl) {
        String media_id = null;
        // 拼装请求地址
        String uploadMediaUrl = CommonAPI.UPLOAD_MEDIA_URL_TEMP.replaceAll("ACCESS_TOKEN", accessToken).replaceAll("TYPE",type);

        // 定义数据分隔符
        String boundary = "------------7dafc2e536604c8";
        try {
            URL uploadUrl = new URL(uploadMediaUrl);
            HttpURLConnection uploadConn = (HttpURLConnection) uploadUrl.openConnection();
            uploadConn.setDoOutput(true);
            uploadConn.setDoInput(true);
            uploadConn.setRequestMethod(CommonAPI.POST);
            // 设置请求头Content-Type
            uploadConn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            // 获取媒体文件上传的输出流（往微信服务器写数据）
            OutputStream outputStream = uploadConn.getOutputStream();
            //如果文件是从本地获取不需要这里
            URL mediaUrl = new URL(mediaFileUrl);
            HttpURLConnection meidaConn = (HttpURLConnection) mediaUrl.openConnection();
            meidaConn.setDoOutput(true);
            meidaConn.setRequestMethod(CommonAPI.GET);

            // 从请求头中获取内容类型
            String contentType = meidaConn.getHeaderField("Content-Type");
            // 根据内容类型判断文件扩展名
            String fileExt = CommonUtils.getFileExt(contentType);
            // 请求体开始
            outputStream.write(("--" + boundary + "\r\n").getBytes());
            outputStream.write(
                    String.format("Content-Disposition: form-data; name=\"media\"; filename=\"king%s\"\r\n", fileExt)
                            .getBytes());
            outputStream.write(String.format("Content-Type: %s\r\n\r\n", contentType).getBytes());

            // 获取媒体文件的输入流（读取文件）
            BufferedInputStream bis = new BufferedInputStream(meidaConn.getInputStream());
            byte[] buf = new byte[8096];
            int size = 0;
            while ((size = bis.read(buf)) != -1) {
                // 将媒体文件写到输出流（往微信服务器写数据）
                outputStream.write(buf, 0, size);
            }
            // 请求体结束
            outputStream.write(("\r\n--" + boundary + "--\r\n").getBytes());
            outputStream.close();
            bis.close();
            meidaConn.disconnect();

            // 获取媒体文件上传的输入流（从微信服务器读数据）
            InputStream inputStream = uploadConn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, CommonAPI.UTF8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer buffer = new StringBuffer();
            String str = null;
            while ((str = bufferedReader.readLine()) != null)
            {
                buffer.append(str);
            }
            bufferedReader.close();
            inputStreamReader.close();
            // 释放资源
            inputStream.close();
            inputStream = null;
            uploadConn.disconnect();

            // 使用Gson解析返回结果
            JsonObject jsonObject = new JsonParser().parse(buffer.toString()).getAsJsonObject();

            //错误情况下的返回JSON数据包示例如下（示例为无效媒体类型错误）：
            //{"errcode":40004,"errmsg":"invalid media type"}
            if(jsonObject.has("errcode")){
                if("40001".equals(jsonObject.get("errcode").getAsString())){
                    throw  new Exception(jsonObject.get("errcode").getAsString()+":"+jsonObject.get("errmsg").getAsString());
                }
            }

//			正确情况下的返回JSON数据包结果如下：
            // type等于thumb时的返回结果和其它类型不一样
//			{"type":"TYPE","media_id":"MEDIA_ID","created_at":123456789}
            media_id = jsonObject.get("media_id").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            media_id = null;
            System.out.println("上传临时媒体文件失败：{}");
        }
        return media_id;
    }
}
