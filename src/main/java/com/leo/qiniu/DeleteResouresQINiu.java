package com.leo.qiniu;

import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;
import com.qiniu.util.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author kz
 * 删除七牛文件资源
 */
public class DeleteResouresQINiu {
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

    public List<FileInfo> queryResource() throws QiniuException {
        System.out.println("获取文件列表中……");
        List<FileInfo> infoArr = new ArrayList<>();
        FileListing fileListing = queryFiles("");
        addFiles(fileListing,infoArr);
        String market = fileListing.marker;
        while(!StringUtils.isNullOrEmpty(market)){
            fileListing = queryFiles(market);
            market = fileListing.marker;
            addFiles(fileListing,infoArr);
        }
        System.out.println("总文件数量:"+infoArr.size());
        return infoArr;
    }

    public void addFiles(FileListing fileListing,List<FileInfo> infos) throws QiniuException {
        for(FileInfo file : fileListing.items){
            infos.add(file);
        }
    }

    @Test
    public void deleteFiles() throws Exception {
        List<FileInfo> fileInfos =queryResource();
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(100,1000,1, TimeUnit.DAYS,new LinkedBlockingQueue<Runnable>());
        for(FileInfo file:fileInfos){
            bucketManager.delete(Config.BUCKET,file.key);
            poolExecutor.execute(new Delete(file));
        }
        //用于控制当前线程不中断
        System.in.read();
    }

    class Delete implements Runnable{
        FileInfo file;

        public Delete(FileInfo file){
            this.file = file;
        }
        @Override
        public void run() {
            try {
                bucketManager.delete(Config.BUCKET,file.key);
                System.out.println(String.format("[删除成功] ==>%s", file.key));
            } catch (QiniuException e) {
                e.printStackTrace();
            }
        }
    }


}
