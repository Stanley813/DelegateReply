package cn.sh.ideal.util;

import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class HttpUtils {
	
	private RequestConfig config;
	
	private String cookies = "";
	
	private HttpClientContext context = new HttpClientContext(new BasicHttpContext());
	
	public HttpUtils() {
        config = RequestConfig.custom().setConnectTimeout(0).setSocketTimeout(0).build();
    }
	
	public HttpUtils(int connTimeout, int reqTimeout, String proxyHost, int proxyPort) {
        config = RequestConfig.custom().setConnectTimeout(connTimeout).setSocketTimeout(reqTimeout).setProxy(new HttpHost(proxyHost, proxyPort)).setAuthenticationEnabled(true).build();
    }
	
	/**
     * 设置HTTP头
     *
     * @param headerParams http头消息
     * @return 头信息
     */
    public static Header[] getHeaders(Map<String, String> headerParams) {
        HeaderGroup headerGroup = new HeaderGroup();
        for (String key : headerParams.keySet()) {
            BasicHeader header = new BasicHeader(key, headerParams.get(key));
            headerGroup.addHeader(header);
        }
        return headerGroup.getAllHeaders();
    }
    

    /**
     * 采用get方式提交请求
     * @param url 请求地址
     * @param isSSL
     * @return
     * @throws Exception
     */
    public String sendByGet(String url, boolean isSSL, String charset) throws Exception {
        return sendByGet(url, null, isSSL, charset);
    }
    public String sendByGet(String url, boolean isSSL, Map<String, String> params, String charset) throws Exception {
    	URIBuilder uriBuilder = new URIBuilder(url);
        for (String key : params.keySet()) {
            uriBuilder=uriBuilder.setParameter(key,params.get(key));
        }
        URI uri = uriBuilder.build();
        return sendByGet(uri.toString(), null, isSSL, charset);
    }
    
    public String sendByGet(String url, Header[] headers, boolean isSSL, String charset) throws Exception {
        return sndGetMsg(isSSL ? generateSSLClient() : HttpClients.createDefault(), url, headers, charset);
    }
    
    
    /**
     * 通过GET方式提交数据，非HTTPS方式
     * @param url
     * @param headers
     * @return
     * @throws Exception
     */
    public String sendByGet(String url, Header[] headers) throws Exception {
        return sendByGet(url, headers, false);
    }

    /**
     * 采用get方式提交请求
     * @param url 请求地址
     * @return
     * @throws Exception
     */
    public String sendByGet(String url, boolean isSSL) throws Exception {
        return sendByGet(url, null, isSSL);
    }

    /**
     * 通过GET方式提交数据
     * @param url
     * @param headers
     * @param isSSL
     * @return
     * @throws Exception
     */
    public String sendByGet(String url, Header[] headers, boolean isSSL) throws Exception {
        return sndGetMsg(isSSL ? generateSSLClient() : HttpClients.createDefault(), url, headers, null);
    }
    
    public String sendByGet(String url, Header[] headers, String proxyName, String proxyPsw, boolean isSSL) throws Exception {
        return sndGetMsg(createProxyHttpClient(proxyName, proxyPsw, isSSL), url, headers, null);
    }

    public String sendByGet(String url, Map<String, String> params, Header[] headers, String proxyName, String proxyPsw, boolean isSSL) throws Exception {
        URIBuilder uriBuilder = new URIBuilder(url);
        for (String key : params.keySet()) {
            uriBuilder=uriBuilder.setParameter(key,params.get(key));
        }
        URI uri = uriBuilder.build();
        return sndGetMsg(createProxyHttpClient(proxyName, proxyPsw, isSSL), uri.toString(), headers, null);
    }
    
    /**
     * 生成HTTPS的客户端
     * @return
     * @throws Exception
     */
    public CloseableHttpClient generateSSLClient() throws Exception {
        return HttpClients.custom().setSSLSocketFactory(initSSLFactory()).build();
    }

    public SSLConnectionSocketFactory initSSLFactory() throws Exception {
        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            // 信任所有
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
            }
        }).build();
        return new SSLConnectionSocketFactory(sslContext);
    }
    
    /**
     * 创建有代理客户端
     * @param proxyName 用户名
     * @param proxyPsw 密码
     * @param isSSL 是否HTTPS
     * @return HttpClient
     */
    public CloseableHttpClient createProxyHttpClient(String proxyName, String proxyPsw, boolean isSSL) throws Exception {
        CredentialsProvider credentials = new BasicCredentialsProvider();
        credentials.setCredentials(AuthScope.ANY, new NTCredentials(proxyName, proxyPsw, "", ""));
        HttpClientBuilder builder = HttpClients.custom().setDefaultCredentialsProvider(credentials);
        return isSSL ? builder.setSSLSocketFactory(initSSLFactory()).build() : builder.build();
    }
    
    /**
     * 采用get方式提交请求
     * @param url 请求地址
     * @param headers 请求头信息
     * @return
     * @throws Exception
     */
    public String sndGetMsg(CloseableHttpClient httpClient, String url, Header[] headers, String charset) throws Exception {
        HttpGet httpGet = null;
        CloseableHttpResponse response = null;
        try {
            httpGet = new HttpGet(url);
            httpGet.setHeaders(headers);
            httpGet.setConfig(config);
            if (context.getCookieStore() != null) {
            	List<Cookie> cookies = context.getCookieStore().getCookies();
            	System.out.println("req");
            	for(Cookie cookie : cookies) {
            		System.out.println(String.format("name is %s, value is %s", cookie.getName(), cookie.getValue()));
            	}
            }
            response = httpClient.execute(httpGet, context);
            if (context.getCookieStore() != null) {
            	List<Cookie> cookies = context.getCookieStore().getCookies();
            	System.out.println("response");
            	for(Cookie cookie : cookies) {
            		System.out.println(String.format("name is %s, value is %s", cookie.getName(), cookie.getValue()));
            	}
            }
            HttpEntity entity = response.getEntity();
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_MOVED_PERMANENTLY == statusCode || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                // 重定向
                Header resHeader = response.getFirstHeader("location");
                if (resHeader != null) {
                    String redirectUrl = resHeader.getValue();
                    return sndGetMsg(httpClient, redirectUrl, headers, charset);
                }
            }
            if (HttpStatus.SC_OK != statusCode) {
            	throw new ClientProtocolException("响应码状态不是200");
            }
            /*
            Header header = response.getFirstHeader("Set-Cookie");
            cookies = header.getValue();
            */
            List<Header> headerList = Arrays.asList(response.getHeaders("Set-Cookie"));
            for(Header header : headerList) {
                System.out.println(String.format("name is %s, value is %s", header.getName(), header.getValue()));
                cookies = cookies + header.getValue();
            }
            if (charset == null || charset.length() == 0) {
                return EntityUtils.toString(entity);
            } else {
                return EntityUtils.toString(entity, charset);
            }
        } finally {
            if (response != null) {
                response.close();
            }
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

	/**
	 * @return the cookies
	 */
	public String getCookies() {
		return cookies;
	}

	/**
	 * @param cookies the cookies to set
	 */
	public void setCookies(String cookies) {
		this.cookies = cookies;
	}

	/**
	 * @return the config
	 */
	public RequestConfig getConfig() {
		return config;
	}

	/**
	 * @param config the config to set
	 */
	public void setConfig(RequestConfig config) {
		this.config = config;
	}

    
    
}
