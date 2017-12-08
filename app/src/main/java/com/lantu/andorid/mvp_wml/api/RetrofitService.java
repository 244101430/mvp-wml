package com.lantu.andorid.mvp_wml.api;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.lantu.andorid.mvp_wml.AndroidApplication;
import com.lantu.andorid.mvp_wml.api.bean.NewsInfo;
import com.lantu.andorid.mvp_wml.api.bean.NoahBean;
import com.lantu.andorid.mvp_wml.utils.MD5Util;
import com.lantu.andorid.mvp_wml.utils.NetUtil;
import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by long on 2016/8/22.
 * 整个网络通信服务的启动控制，必须先调用初始化函数才能正常使用网络通信接口
 */
public class RetrofitService {

    private static final String HEAD_LINE_NEWS = "T1348647909107";

    //设缓存有效期为1天
    static final long CACHE_STALE_SEC = 60 * 60 * 24 * 1;
    //查询缓存的Cache-Control设置，为if-only-cache时只查询缓存而不会请求服务器，max-stale可以配合设置缓存失效时间
    private static final String CACHE_CONTROL_CACHE = "only-if-cached, max-stale=" + CACHE_STALE_SEC;
    //查询网络的Cache-Control设置
    //(假如请求了服务器并在a时刻返回响应结果，则在max-age规定的秒数内，浏览器将不会发送对应的请求到服务器，数据由缓存直接返回)
    static final String CACHE_CONTROL_NETWORK = "Cache-Control: public, max-age=3600";
    // 避免出现 HTTP 403 Forbidden，参考：http://stackoverflow.com/questions/13670692/403-forbidden-with-java-but-not-web-browser
    static final String AVOID_HTTP403_FORBIDDEN = "User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";

    /**
     * 新闻地址
     */
    private static final String NEWS_HOST = "http://c.3g.163.com/";
    private static ITestApi sNewsService;

    /**
     *  诺亚地址
     */
    private static final String NOAH_HOST = "http://gw-dh.kingifa.com/";
    private static INoahApi sNoahService;

    /**
     * 蓝图地址
     */
    private static final String LANTU_HOST = "https://www.mylantu.net/wp-content/themes/RespondoPro-child-theme/";
    private static ILantuApi sLantuService;

    // 递增页码
    private static final int INCREASE_PAGE = 20;


    private RetrofitService() {
        throw new AssertionError();
    }

    /**
     * 初始化网络通信服务
     */
    public static void init() {
        // 指定缓存路径,缓存大小100Mb
        Cache cache = new Cache(new File(AndroidApplication.getContext().getCacheDir(), "HttpCache"),
                1024 * 1024 * 100);
        OkHttpClient okHttpClient = new OkHttpClient.Builder().cache(cache)
                .retryOnConnectionFailure(true)
                .addInterceptor(sLoggingInterceptor)
                .addInterceptor(sRewriteCacheControlInterceptor)
                .addNetworkInterceptor(sRewriteCacheControlInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(NEWS_HOST)
                .build();
        sNewsService = retrofit.create(ITestApi.class);

        Retrofit retrofitNoah = new Retrofit.Builder()
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(NOAH_HOST)
                .build();
        sNoahService = retrofitNoah.create(INoahApi.class);


        OkHttpClient okHttpClient2 = new OkHttpClient.Builder().cache(cache)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();





        Retrofit retrofitLantu = new Retrofit.Builder()
                .client(okHttpClient2)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(LANTU_HOST)
                .build();
        sLantuService = retrofitLantu.create(ILantuApi.class);
    }

    /**
     * 云端响应头拦截器，用来配置缓存策略
     * Dangerous interceptor that rewrites the server's cache-control header.
     */
    private static final Interceptor sRewriteCacheControlInterceptor = new Interceptor() {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            if (!NetUtil.isNetworkAvailable(AndroidApplication.getContext())) {
                request = request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build();
                Logger.e("no network");
            }
            Response originalResponse = chain.proceed(request);

            if (NetUtil.isNetworkAvailable(AndroidApplication.getContext())) {
                //有网的时候读接口上的@Headers里的配置，你可以在这里进行统一的设置
                String cacheControl = request.cacheControl().toString();
                return originalResponse.newBuilder()
                        .header("Cache-Control", cacheControl)
                        .removeHeader("Pragma")
                        .build();
            } else {
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, " + CACHE_CONTROL_CACHE)
                        .removeHeader("Pragma")
                        .build();
            }
        }
    };

    /**
     * 打印返回的json数据拦截器
     */
    private static final Interceptor sLoggingInterceptor = new Interceptor() {

        @Override
        public Response intercept(Chain chain) throws IOException {
            final Request request = chain.request();
            Buffer requestBuffer = new Buffer();
            if (request.body() != null) {
                request.body().writeTo(requestBuffer);
            } else {
                Logger.d("LogTAG", "request.body() == null");
            }
            //打印url信息
            Logger.w(request.url() + (request.body() != null ? "?" + _parseParams(request.body(), requestBuffer) : ""));
            final Response response = chain.proceed(request);

            return response;
        }
    };

    @NonNull
    private static String _parseParams(RequestBody body, Buffer requestBuffer) throws UnsupportedEncodingException {
        if (body.contentType() != null && !body.contentType().toString().contains("multipart")) {
            return URLDecoder.decode(requestBuffer.readUtf8(), "UTF-8");
        }
        return "null";
    }

    /************************************ NEWS API *******************************************/
    /**
     * 获取新闻列表
     *
     * @return
     */
    public static Observable<List<NewsInfo>> getNewsList(String newsId, int page) {
        String type;
        if (newsId.equals(HEAD_LINE_NEWS)) {
            type = "headline";
        } else {
            type = "list";
        }
        return sNewsService.getNewsList(type, newsId, page * INCREASE_PAGE)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(_flatMapNews(newsId));
    }

    /************************************ Noah API *******************************************/
    /**
     * 请求Noah例子
     *
     * @return
     * @throws JSONException
     */
    public static Observable<String> getNoahStr() throws JSONException {

        String temp = "[{\"jsonrpc\":\"2.0\",\"id\":\"sid\",\"method\":\"etc.get_session\",\"params\":{}},{\"jsonrpc\":\"2.0\",\"id\":\"0.dadmnwqiqdihnq4whn6i27qfr\",\"method\":\"order.notice_main_class_list\",\"params\":{}}]";
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), temp);
        return sNoahService.postNoah(NoahSignUtil.sign(temp), body)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(_flatMapStr());
    }

    /************************************ LANTU API *******************************************/

    public static Observable<String> getLanTu(){
        String temp = "{\"userName\":\"123\",\"password\":\"123\"}";
        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), temp);
        return sLantuService.postLantu(body)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(_flatMapStr());
    }


    /******************************************* 转换器 **********************************************/

    /**
     * 类型转换
     *
     * @param typeStr 新闻类型
     * @return
     */
    private static Func1<Map<String, List<NewsInfo>>, Observable<List<NewsInfo>>> _flatMapNews(final String typeStr) {
        return new Func1<Map<String, List<NewsInfo>>, Observable<List<NewsInfo>>>() {
            @Override
            public Observable<List<NewsInfo>> call(Map<String, List<NewsInfo>> newsListMap) {
                return Observable.from(newsListMap.values());
            }
        };
    }

    /**
     * 类型转换 ResponseBody -> String
     *
     * @return
     */
    private static Func1<ResponseBody, String> _flatMapStr() {
        return new Func1<ResponseBody, String>() {
            @Override
            public String call(ResponseBody responseBody) {

                try {
                    return new String(responseBody.bytes(), "UTF-8");
                } catch (IOException e) {
                    return null;
                }
            }
        };
    }

}
