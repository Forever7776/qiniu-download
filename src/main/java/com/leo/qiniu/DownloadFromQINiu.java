package com.leo.qiniu;

import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;
import com.qiniu.util.StringUtils;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadFromQINiu {
    Auth auth;
    BucketManager bucketManager;

    @Before
    public void loadConfig(){
        auth = Auth.create(Config.AK,Config.SK);
        bucketManager = new BucketManager(auth);
    }

    public FileListing queryFiles(String marker) throws QiniuException {
        return bucketManager.listFiles(Config.BUCKET,"",marker,1000,"");
    }
    public void addToList(FileListing fileListing,List<FileInfo> infos){
        for(FileInfo file : fileListing.items){
            infos.add(file);
        }
    }


    public List<FileInfo> queryResource() throws QiniuException {
        System.out.println("获取文件列表中……");
        List<FileInfo> infoArr = new ArrayList<>();
        FileListing fileListing = queryFiles("");
        addToList(fileListing,infoArr);
        String market = fileListing.marker;
        while(!StringUtils.isNullOrEmpty(market)){
            fileListing = queryFiles(market);
            market = fileListing.marker;
            addToList(fileListing,infoArr);
        }
        System.out.println("总文件数量:"+infoArr.size());
        return infoArr;
    }

    @Test
    public void downFile() throws IOException {
        List<FileInfo> fileInfoList = queryResource();
        Integer size = 100;//这个用于控制单个线程的下载数量
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(50,1000,1, TimeUnit.DAYS,new LinkedBlockingQueue<Runnable>());

        for(int i=0,len=fileInfoList.size()/size;i<len;i++){
            Integer start = i*size,
                end = start+size;
            List<FileInfo> list = fileInfoList.subList(start,end);
            //创建线程去跑下载
            poolExecutor.execute(new GoDown(list));
        }
        //用于控制当前线程不中断
        System.in.read();
    }

    /**
     * 存到相应目录中
     * @param response
     * @param key
     * @throws IOException
     */
    public void saveToPath(Response response,String key) throws IOException {
        String downPath = "D://qiniu";
        InputStream is = null;
        byte[] buf = new byte[2048];
        int len =0;
        FileOutputStream fos = null;
        try{
            is = response.body().byteStream();
            final long total = response.body().contentLength();
            System.out.println("文件大小:"+total);

            File dir = new File(downPath);

            if(!dir.exists()) dir.mkdir();
            File file = new File(dir,key);
            fos = new FileOutputStream(file);
            while((len = is.read(buf))!=-1){
                fos.write(buf,0,len);
            }
            fos.flush();
        }finally {
            try{
                if(is!=null){
                    is.close();
                }
            }catch (Exception e){
            }
            try{
                if(fos!=null){
                    fos.close();
                }
            }catch (Exception e){
            }
        }
    }


    class GoDown implements Runnable{
        List<FileInfo> list;

        public GoDown(List<FileInfo> fileInfos){
            list = fileInfos;
        }

        @Override
        public void run() {
            OkHttpClient okHttpClient = new OkHttpClient();
            for(FileInfo fileInfo : list) {
                try {
                    System.out.println(String.format("[正在下载] ==>%s", fileInfo.key));
                    String url = Config.DOMAIN+ fileInfo.key;
                    Request request = new Request.Builder()
                            .url(url)
                            .build();
                    Call call = okHttpClient.newCall(request);

                    Response response = call.execute();
                    saveToPath(response,fileInfo.key);

                }catch (Exception e){
                    e.printStackTrace();
                }


            }
        }
    }
}
