import com.google.gson.Gson;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class douyin {

    private static List<RootBean> rootBeans = new ArrayList<>();
    private static String url = "https://www.iesdouyin.com/web/api/v2/aweme/post/?sec_uid=MS4wLjABAAAAyt5PK9_Pn_n_aHlhFogO7gK0QamQw2xoF3CaG0HBNFM&count=21&max_cursor=0&aid=1128&_signature=SdyvORAVFCA6KqVWd.Qb8Encry&dytk=0b1dec0a60b422a1038502f9e9f45483";
    private static ExecutorService executor = new ThreadPoolExecutor(10, 10,
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue(100));


    public static void main(String[] args) throws Exception {
        getDownList(url, 0);
        for (RootBean rootbean : rootBeans) {
            List<AwemeList> awemeLists = rootbean.aweme_list;
            for (AwemeList awemeList : awemeLists) {
                String downLoadUrl = awemeList.video.download_addr.url_list.get(0);
                if (downLoadUrl != null && !downLoadUrl.isEmpty()) {
                    downLoadUrl = downLoadUrl.replace("play", "playwm");
                } else {
                    continue;
                }
                String finalDownLoadUrl = downLoadUrl;
                executor.execute(() -> downLoad(finalDownLoadUrl, awemeList.desc));

            }
        }
    }

    private static void getDownList(String urls, long maxCursor) throws IOException {
        String[] qureys = urls.split("&");
        String requestUrl = "";
        for (String qurey : qureys) {
            if (!qurey.contains("max_cursor")) {
                requestUrl += qurey + "&";
            } else {
                requestUrl += "max_cursor=" + maxCursor + "&";
            }
        }
        requestUrl = requestUrl.substring(0, requestUrl.length() - 1);
        System.out.println(requestUrl);
        Connection.Response document = Jsoup.connect(requestUrl)
                .ignoreContentType(true)
                .userAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)")
                .execute();
        Gson gson = new Gson();
        RootBean rootBean = gson.fromJson(document.body(), RootBean.class);
        System.out.println("has_more:" + rootBean.has_more + ",max_cursor:" + rootBean.max_cursor);
        rootBeans.add(rootBean);
        if (rootBean.has_more && !rootBean.aweme_list.isEmpty()) {
            getDownList(url, rootBean.max_cursor);
        }
    }

    private static void downLoad(String downLadUrl, String name) {
        try {
            System.out.println(downLadUrl);
            Connection.Response downLoadUrl = Jsoup.connect(downLadUrl)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .execute();
            Connection.Response response = Jsoup.connect(downLoadUrl.url().toString())
                    .timeout(1500000)
                    .maxBodySize(0)
                    .followRedirects(false)
                    .ignoreContentType(true)
                    .execute();
            BufferedInputStream stream = response.bodyStream();
            File file = new File("/Users/xingguolei/douyin/" + name + ".mp4");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            int len;
            byte[] buf = new byte[1024];
            while ((len = stream.read(buf)) != -1) {
                fileOutputStream.write(buf, 0, len);
            }
            stream.close();
            stream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
