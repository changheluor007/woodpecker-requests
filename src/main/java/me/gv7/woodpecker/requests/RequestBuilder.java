package me.gv7.woodpecker.requests;

import me.gv7.woodpecker.requests.body.Part;
import me.gv7.woodpecker.requests.body.RequestBody;
import me.gv7.woodpecker.requests.config.CustomHttpHeaderConfig;
import me.gv7.woodpecker.requests.config.ProxyConfig;
import me.gv7.woodpecker.requests.config.TimeoutConfig;
import me.gv7.woodpecker.requests.exception.RequestsException;
import me.gv7.woodpecker.requests.executor.HttpExecutor;
import me.gv7.woodpecker.requests.executor.RequestExecutorFactory;
import me.gv7.woodpecker.requests.executor.SessionContext;
import me.gv7.woodpecker.requests.config.HttpConfigManager;
import net.dongliu.commons.collection.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;

/**
 * Http Request builder
 *
 * @author Liu Dong
 */
public final class RequestBuilder {
    public static boolean DEBUG = true;
    private boolean ignoreHttpConfig = false;
    String method = Methods.GET;
    URL url;
    Collection<? extends Map.Entry<String, ?>> headers = Lists.of();
    Collection<? extends Map.Entry<String, ?>> cookies = Lists.of();
    String userAgent = DefaultSettings.USER_AGENT;
    Collection<? extends Map.Entry<String, ?>> params = Lists.of();
    Charset charset = StandardCharsets.UTF_8;
    @Nullable
    RequestBody<?> body;
    boolean timeoutIsSet = false;
    int socksTimeout = DefaultSettings.SOCKS_TIMEOUT;
    int connectTimeout = DefaultSettings.CONNECT_TIMEOUT;
    @Nullable
    Proxy proxy;
    boolean followRedirect = false;
    int maxRedirectCount = 5;
    boolean acceptCompress = true;
    boolean verify = false;
    @Nullable
    BasicAuth basicAuth;
    @Nullable
    SessionContext sessionContext;
    boolean keepAlive = true;
    @Nullable
    KeyStore keyStore;

    private List<? extends Interceptor> interceptors = Collections.emptyList();

    RequestBuilder() {
    }

    RequestBuilder(Request request) {
        method = request.method();
        headers = request.headers();
        cookies = request.cookies();
        userAgent = request.userAgent();
        charset = request.charset();
        body = request.body();
        socksTimeout = request.socksTimeout();
        connectTimeout = request.connectTimeout();
        proxy = request.proxy();
        followRedirect = request.followRedirect();
        maxRedirectCount = request.maxRedirectCount();
        acceptCompress = request.acceptCompress();
        verify = request.verify();
        basicAuth = request.basicAuth();
        sessionContext = request.sessionContext();
        keepAlive = request.keepAlive();
        keyStore = request.keyStore();
        this.url = request.url();
        this.params = request.params();
    }

    public RequestBuilder method(String method) {
        this.method = Objects.requireNonNull(method);
        return this;
    }

    public RequestBuilder url(String url) {
        try {
            this.url = new URL(Objects.requireNonNull(url));
        } catch (MalformedURLException e) {
            throw new RequestsException("Resolve url error, url: " + url, e);
        }
        return this;
    }

    public RequestBuilder url(URL url) {
        this.url = Objects.requireNonNull(url);
        return this;
    }

    /**
     * Set request headers.
     * 2021.10.24 解决设置两次headers覆盖的问题
     */
    public RequestBuilder headers(Collection<? extends Map.Entry<String, ?>> headers) {
        Map<String, Object> newHeaders = new HashMap<>(getHeaders());
        for (Map.Entry<String, ?> next : headers) {
            newHeaders.put(next.getKey(), next.getValue());
        }
        this.headers = newHeaders.entrySet();
        return this;
    }

    /**
     * Set request headers.
     * 2021.10.24 解决设置两次headers覆盖的问题| 直接调用上方headers,不用修改
     */
    @SafeVarargs
    public final RequestBuilder headers(Map.Entry<String, ?>... headers) {
        headers(Lists.of(headers));
        return this;
    }

    /**
     * Set request headers.
     * 2021.10.24 解决设置两次headers覆盖的问题
     */
    public final RequestBuilder headers(Map<String, ?> map) {
        Map<String, Object> newHeaders = new HashMap<>(getHeaders());
        newHeaders.putAll(map);
        this.headers = newHeaders.entrySet();
        return this;
    }

    /**
     * Set request cookies.
     */
    public RequestBuilder cookies(Collection<? extends Map.Entry<String, ?>> cookies) {
        this.cookies = cookies;
        return this;
    }

    /**
     * Set request cookies.
     */
    @SafeVarargs
    public final RequestBuilder cookies(Map.Entry<String, ?>... cookies) {
        cookies(Lists.of(cookies));
        return this;
    }

    /**
     * Set request cookies.
     */
    public final RequestBuilder cookies(Map<String, ?> map) {
        this.cookies = map.entrySet();
        return this;
    }

    public RequestBuilder userAgent(String userAgent) {
        this.userAgent = Objects.requireNonNull(userAgent);
        return this;
    }

    /**
     * Set url query params.
     */
    public RequestBuilder params(Collection<? extends Map.Entry<String, ?>> params) {
        this.params = params;
        return this;
    }

    /**
     * Set url query params.
     */
    @SafeVarargs
    public final RequestBuilder params(Map.Entry<String, ?>... params) {
        this.params = Lists.of(params);
        return this;
    }

    /**
     * Set url query params.
     */
    public final RequestBuilder params(Map<String, ?> map) {
        this.params = map.entrySet();
        return this;
    }

    /**
     * Set charset used to encode request params or forms. Default UTF8.
     */
    public RequestBuilder requestCharset(Charset charset) {
        this.charset = charset;
        return this;
    }

    /**
     * Set charset used to encode request params or forms. Default UTF8.
     */
    public RequestBuilder charset(Charset charset) {
        this.charset = charset;
        return this;
    }

    /**
     * Set request body
     */
    public RequestBuilder body(@Nullable RequestBody<?> body) {
        this.body = body;
        return this;
    }

    /**
     * Set www-form-encoded body. Only for Post
     *
     * @deprecated use {@link #body(Collection)} instead
     */
    @Deprecated
    public RequestBuilder forms(Collection<? extends Map.Entry<String, ?>> params) {
        body = RequestBody.form(params);
        return this;
    }

    /**
     * Set www-form-encoded body. Only for Post
     *
     * @deprecated use {@link #body(Map.Entry[])} instead
     */
    @Deprecated
    @SafeVarargs
    public final RequestBuilder forms(Map.Entry<String, ?>... formBody) {
        return forms(Lists.of(formBody));
    }

    /**
     * Set www-form-encoded body. Only for Post
     *
     * @deprecated use {@link #body(Map)} instead
     */
    @Deprecated
    public RequestBuilder forms(Map<String, ?> formBody) {
        return forms(formBody.entrySet());
    }


    /**
     * Set www-form-encoded body. Only for Post
     */
    public RequestBuilder body(Collection<? extends Map.Entry<String, ?>> params) {
        body = RequestBody.form(params);
        return this;
    }

    /**
     * Set www-form-encoded body. Only for Post
     */
    @SafeVarargs
    public final RequestBuilder body(Map.Entry<String, ?>... formBody) {
        return body(Lists.of(formBody));
    }

    /**
     * Set www-form-encoded body. Only for Post
     */
    public RequestBuilder body(Map<String, ?> formBody) {
        return body(formBody.entrySet());
    }

    /**
     * Set string body
     */
    public RequestBuilder body(String str) {
        body = RequestBody.text(str);
        return this;
    }

    /**
     * Set binary body
     */
    public RequestBuilder body(byte[] bytes) {
        body = RequestBody.bytes(bytes);
        return this;
    }

    /**
     * Set input body
     */
    @Deprecated
    public RequestBuilder body(InputStream input) {
        body = RequestBody.inputStream(input);
        return this;
    }

    /**
     * For send application/json post request.
     * Must have jackson or gson in classpath, or a runtime exception will be raised
     */
    public RequestBuilder jsonBody(Object value) {
        body = RequestBody.json(value);
        return this;
    }

    /**
     * Set tcp socks timeout in milliseconds. Default is 5000
     */
    public RequestBuilder socksTimeout(int timeout) {
        checkTimeout(timeout);
        this.socksTimeout = timeout;
        return this;
    }

    /**
     * Set tcp connect timeout in milliseconds. Default is 3000
     */
    public RequestBuilder connectTimeout(int timeout) {
        checkTimeout(timeout);
        this.connectTimeout = timeout;
        return this;
    }

    private static void checkTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout should not less than 0");
        }
    }

    /**
     * set proxy
     */
    public RequestBuilder proxy(@Nullable Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    /**
     * Set auto handle redirect. default true
     */
    public RequestBuilder followRedirect(boolean followRedirect) {
        this.followRedirect = followRedirect;
        return this;
    }

    /**
     * Set max redirects count
     */
    public RequestBuilder maxRedirectCount(int maxRedirectCount) {
        this.maxRedirectCount = maxRedirectCount;
        return this;
    }

    /**
     * Set accept compressed response. default true
     * @deprecated use {@link #acceptCompress(boolean)}
     */
    @Deprecated
    public RequestBuilder compress(boolean compress) {
        this.acceptCompress = compress;
        return this;
    }

    /**
     * Set accept compressed response. default true
     */
    public RequestBuilder acceptCompress(boolean acceptCompress) {
        this.acceptCompress = acceptCompress;
        return this;
    }

    /**
     * Check ssl cert. default true
     */
    public RequestBuilder verify(boolean verify) {
        this.verify = verify;
        return this;
    }

    /**
     * Add a custom additional keyStore contains X509 Certificate, for ssl connection trust validation.
     */
    public RequestBuilder keyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
        return this;
    }

    /**
     * If reuse http connection. default true
     */
    public RequestBuilder keepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    /**
     * Set http basicAuth by BasicAuth(DigestAuth/NTLMAuth not supported now)
     */
    public RequestBuilder basicAuth(String user, String password) {
        this.basicAuth = new BasicAuth(user, password);
        return this;
    }

    /**
     * Set http basicAuth by BasicAuth(DigestAuth/NTLMAuth not supported now)
     */
    public RequestBuilder basicAuth(BasicAuth basicAuth) {
        this.basicAuth = basicAuth;
        return this;
    }

    /**
     * Set ignore httpconfig
     * @param ignoreHttpConfig
     * @return
     */
    public RequestBuilder ignoreHttpConfig(boolean ignoreHttpConfig) {
        this.ignoreHttpConfig = ignoreHttpConfig;
        return this;
    }

    Request build() {
        if(!ignoreHttpConfig && proxy == null) {
            // config proxy
            ProxyConfig proxyConfig = HttpConfigManager.getProxyConfig();
            if (proxyConfig.isEnable()) {
                if (proxyConfig.getProtocol().equals("http")) {
                    proxy(Proxies.httpProxy(proxyConfig.getHost(), proxyConfig.getPort()));
                } else if (proxyConfig.getProtocol().equals("socks")) {
                    proxy(Proxies.socksProxy(proxyConfig.getHost(), proxyConfig.getPort()));
                }
            }

            // config timeout
            TimeoutConfig timeoutConfig = HttpConfigManager.getTimeoutConfig();
            if (timeoutConfig.isEnableMandatoryTimeout()) {
                timeout(timeoutConfig.getMandatoryTimeout());
            } else if (!timeoutIsSet && timeoutConfig.getDefaultTimeout() != 0) {
                timeout(timeoutConfig.getDefaultTimeout());
            }

            // config useragent
            String userAgent = HttpConfigManager.getUserAgent();
            if (userAgent != null) {
                userAgent(userAgent);
            }

            // config custom http header
            CustomHttpHeaderConfig customHttpHeaderConfig = HttpConfigManager.getCustomHttpHeaderConfig();
            LinkedHashMap<String, String> customHttpHeaders = customHttpHeaderConfig.getCustomHttpHeaders();
            //customHttpHeaders.put("TestHeader","test");
            if (customHttpHeaderConfig.isOverwriteHttpHeader()) { // 强行覆盖旧Http头
                Map<String, Object> oldHeaders = getHeaders();
                Map<String, Object> newHeaders = new HashMap<String, Object>();
                newHeaders.putAll(oldHeaders);
                newHeaders.putAll(customHttpHeaders);
                headers(newHeaders);
            } else { // 不覆盖(暂时未实现)
                customHttpHeaders.remove("Host"); // 改模式不能覆盖Host头
                LinkedHashMap<String, Object> newHeaders = new LinkedHashMap<String, Object>();
                newHeaders.putAll(customHttpHeaders);
                newHeaders.putAll(getHeaders());
                headers(newHeaders);
            }
        }
        return new Request(this);
    }

    public Object getHeader(String headerKey){
        Iterator<? extends Map.Entry<String,?>> iterator = this.headers.iterator();
        while (iterator.hasNext()){
            Map.Entry<String,?> obj = iterator.next();
            String keyName = obj.getKey();
            if(keyName.equals(headerKey)){
                return obj.getValue();
            }
        }
        return null;
    }

    public LinkedHashMap<String,Object> getHeaders(){
        LinkedHashMap<String,Object> headers = new LinkedHashMap<String,Object>();
        Iterator<? extends Map.Entry<String,?>> iterator = this.headers.iterator();
        while (iterator.hasNext()){
            Map.Entry<String,?> obj = iterator.next();
            String keyName = obj.getKey();
            Object value = obj.getValue();
            headers.put(keyName,value);
        }
        return headers;
    }

    /**
     * build http request, and send out
     */
    public RawResponse send() {
        Request request = build();
        RequestExecutorFactory factory = RequestExecutorFactory.getInstance();
        HttpExecutor executor = factory.getHttpExecutor();
        return new InterceptorChain(interceptors, executor).proceed(request);
    }

    /**
     * Set both connect timeout and socks timeout in milliseconds
     */
    public RequestBuilder timeout(int timeout) {
        timeoutIsSet = true;
        checkTimeout(timeout);
        return connectTimeout(timeout).socksTimeout(timeout);
    }

    /**
     * Set connect timeout and socks timeout in milliseconds
     */
    public RequestBuilder timeout(int connectTimeout, int socksTimeout) {
        return connectTimeout(connectTimeout).socksTimeout(socksTimeout);
    }

    /**
     * Set multiPart body. Only form multi-part post.
     *
     * @see #multiPartBody(Collection)
     */
    public final RequestBuilder multiPartBody(Part<?>... parts) {
        return multiPartBody(Lists.of(parts));
    }

    /**
     * Set multiPart body. Only form multi-part post.
     */
    public RequestBuilder multiPartBody(Collection<Part<?>> parts) {
        return body(RequestBody.multiPart(parts));
    }


    /**
     * Set interceptors
     */
    public RequestBuilder interceptors(List<? extends Interceptor> interceptors) {
        this.interceptors = interceptors;
        return this;
    }

    /**
     * Set interceptors
     */
    public RequestBuilder interceptors(Interceptor... interceptors) {
        return interceptors(Lists.of(interceptors));
    }

    /**
     * This method is only for internal use!
     */
    RequestBuilder sessionContext(@Nullable SessionContext sessionContext) {
        this.sessionContext = sessionContext;
        return this;
    }
}