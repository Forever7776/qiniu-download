package com.leo.qiniu;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UploadImageToQINiu {
    String FilePath = "D:\\qiniu\\images";

    Auth auth = Auth.create(Config.AK, Config.SK);
    UploadManager uploadManager = new UploadManager();

    public String getUpToken(){
        return auth.uploadToken(Config.BUCKET);
    }

    public void getFiles()  throws Exception {
        String key="";
        File file=new File(FilePath);
        File [] images=file.listFiles();
        if(images.length>0){
            System.out.printf(String.format("文件目录有:%s个文件",images.length));
            ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(100,1000,1, TimeUnit.DAYS,new LinkedBlockingQueue<Runnable>());
            for (int i=0;i<images.length;i++){
                FilePath=images[i]+"";
                key=images[i].getName();
                poolExecutor.execute(new GoUpload(key));
            }
            //用于控制当前线程不中断
            System.in.read();
        }else{
            System.out.printf("文件夹为空");
        }
    }

    class GoUpload implements Runnable {
        String key;
        public GoUpload(String key){
            this.key =key;
        }
        @Override
        public void run() {
            Response res = null;
            try {
                res = uploadManager.put(FilePath, key, getUpToken());
                System.out.println(String.format("======>>:%s上传成功，服务器ip:%s", key, res.address));
            } catch (QiniuException e) {
                e.printStackTrace();
            }

        }
    }


    public static void main(String args[]) throws Exception{
        new UploadImageToQINiu().getFiles();
    }

}
