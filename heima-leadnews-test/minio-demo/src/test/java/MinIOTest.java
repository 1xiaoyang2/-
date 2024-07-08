
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.io.FileInputStream;

public class MinIOTest {


    public static void main(String[] args) {

        FileInputStream fileInputStream = null;
        try {

            fileInputStream =  new FileInputStream
                    ("D:\\BaiduNetdiskDownload\\hmtt\\day2\\资料\\模板文件\\plugins\\js\\index.js");;

            //1.创建minio链接客户端
            MinioClient minioClient = MinioClient.builder().credentials("minio", "minio123")
                    .endpoint("http://192.168.88.130:9000").build();
            //2.上传
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .object("plugins/js/index.js")//文件名
                    .contentType("text/js")//文件类型
                    .bucket("leadnews")//桶名词  与minio创建的名词一致
                    .stream(fileInputStream, fileInputStream.available(), -1) //文件流
                    .build();
            minioClient.putObject(putObjectArgs);

//            System.out.println("http://192.168.88.130:9000/leadnews/ak47.jpg");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}