package com.leo.qiniu;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;

import java.io.File;
import java.util.Date;

public class UploadImageToQINiu {
    String FilePath = "";
    String key="";
    Auth auth = Auth.create(Config.AK, Config.SK);
    UploadManager uploadManager = new UploadManager();

    public String getUpToken(){
        return auth.uploadToken(Config.BUCKET);
    }

    public void upload()  throws Exception {
        try {
            Long begin = new Date().getTime();
            File file=new File("D:\\asdf");
            File [] images=file.listFiles();
            for (int i=0;i<images.length;i++){
                FilePath=images[i]+"";
                key=images[i].getName();
                Response res = uploadManager.put(FilePath, key, getUpToken());
                System.out.println(res.bodyString());
            }
            Long end = new Date().getTime();
            System.out.println("上传任务耗时："+(end-begin)/1000+"s");

        } catch (QiniuException e) {
            Response r = e.response;
            System.out.println(r.toString());
            try {
                System.out.println(r.bodyString());
            } catch (QiniuException e1) {
            }
        }
    }


    public static void main(String args[]) throws Exception{
        new UploadImageToQINiu().upload();
    }

}
