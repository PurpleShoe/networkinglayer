package za.co.purpleshoe.networklayer.network.request;

import android.content.Context;
import android.support.annotation.IntDef;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import za.co.purpleshoe.networklayer.network.response.BaseResponse;
import za.co.purpleshoe.networklayer.network.body.BaseBody;

/**
 * Created by Tyler Hogarth on 2018/08/27
 */
public abstract class BaseRequest<T extends BaseResponse> {

    /**
     * Set to true if you want to log out the request data such as the headers or POST body
     */
    private static final boolean LOGGING = true;

    public static final String BASE_URL = "http://www.example.com/";

    public static final String ENDPOINT_POST_EXAMPLE = "endpoint/";

    public static final String HEADER_KEY_AUTHORIZATION = "Authorization";
    public static final String HEADER_KEY_CONTENT_TYPE = "Content-Type";

    public static final String HEADER_VALUE_CONTENT_TYPE = "application/json";

    public static final int RETRY_TIMEOUT = 30000;
    public static final int RETRY_COUNT = 0;
    public static final float RETRY_BACKOFF = 1f;

    private static RequestQueue requestQueue;

    protected Context context;
    protected static final Gson gson = new Gson();
    private Response.Listener<T> listener;
    private Response.ErrorListener errorListener;

    /**
     * The headers that are part of the HTTP request.
     */
    protected Map<String, String> headers = new HashMap<>();
    /**
     * The query string parameters that are part of the HTTP request.
     */
    protected Map<String, String> queryString = new HashMap<>();

    protected BaseBody body;

    @IntDef({Request.Method.DEPRECATED_GET_OR_POST,
            Request.Method.GET,
            Request.Method.POST,
            Request.Method.PUT,
            Request.Method.DELETE,
            Request.Method.HEAD,
            Request.Method.OPTIONS,
            Request.Method.TRACE,
            Request.Method.PATCH})
    @interface Method {}

    @Method()
    public abstract int getRequestMethod();

    /**
     * A fully qualified url WITHOUT query string parameters. {@link #queryString} and
     * {@link #buildUrl()} will be used append the query string parameters.
     *
     * @return
     */
    public abstract String getRequestUrl();

    public abstract Class<T> getResponseClass();

    public BaseRequest(Context context) {
        this.context = context;
    }

    public BaseRequest setResponseListener(Response.Listener<T> listener) {
        this.listener = listener;
        return this;
    }

    public BaseRequest setErrorResponseListener(Response.ErrorListener errorListener) {
        this.errorListener = errorListener;
        return this;
    }

    public BaseRequest setBody(BaseBody body) {
        this.body = body;
        return this;
    }

    public void send() {
        VolleyRequest request = new VolleyRequest(buildUrl());
        getRequestQueue(context).add(request);
    }

    /**
     * We add on any query string parameters to the end of the url.
     *
     * example http://url.com?name=value
     *
     * @return The fully fledged url to be used to construct the {@link Request#getUrl()}
     */
    private String buildUrl() {
        //print contents

        if (getRequestUrl() == null) {
            throw new NullPointerException("URL is null. Did you extend getRequestUrl() in your Request class?");
        }

        StringBuilder builder = new StringBuilder(getRequestUrl());
        if (queryString.size() > 0) {

            builder.append("?");

            for (String key : queryString.keySet()) {
                builder.append(key);
                builder.append("=");
                builder.append(queryString.get(key));
                builder.append("&");
            }
            builder.delete(builder.length() - 1, builder.length());//remove the extra "&"
        }

        return builder.toString();
    }

    public BaseRequest addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public BaseRequest addQuery(String name, String value) {
        queryString.put(name, value);
        return this;
    }

    public BaseBody getBody() {
        return body;
    }

    public Response.ErrorListener getResponseErrorListener() {
        return errorListener;
    }

    public class VolleyRequest extends Request<T> {

        final String TAG = BaseRequest.this.getClass().getSimpleName();

        String url;

        public VolleyRequest(String url) {
            super(getRequestMethod(), url, getResponseErrorListener());

            this.url = url;

            setRetryPolicy(new DefaultRetryPolicy(RETRY_TIMEOUT,
                    RETRY_COUNT,
                    RETRY_BACKOFF));

            if (LOGGING) {
                Log.i(TAG,
                        "URL " + getMethodString() + ": " + url);
            }
        }

        @Override
        protected Response<T> parseNetworkResponse(NetworkResponse response) {
            try {

                String json = new String(response.data, Charset.forName("UTF-8"));

                if (LOGGING) {
                    Log.i(TAG,
                            "JSON: " + json);
                }

                return Response.success(gson.fromJson(json, getResponseClass()), HttpHeaderParser.parseCacheHeaders(response));
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
                return Response.error(new ParseError(e));
            } catch (NullPointerException e) {
                throw new NullPointerException("Response Class is null. Did you implement getResponseClass() in your Request class?");
            }
        }

        @Override
        protected void deliverResponse(T response) {
            if (listener != null) {
                listener.onResponse(response);
            }
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            super.getHeaders();

            headers.put(HEADER_KEY_CONTENT_TYPE, HEADER_VALUE_CONTENT_TYPE);

            //print contents
            StringBuilder builder = new StringBuilder("HEADERS: ");
            for (String key : headers.keySet()) {
                builder.append(key);
                builder.append("=");
                builder.append(headers.get(key));
                builder.append(", ");
            }
            builder.delete(builder.length() - 2, builder.length());//remove the extra ", "
            if (LOGGING) {
                Log.i(TAG,
                        builder.toString());
            }
            return headers;
        }

        protected Map<String, String> getQueryString() {

            //print contents
            StringBuilder builder = new StringBuilder("PARAMS: ");
            if (queryString.size() == 0) {
                builder.append("NONE");
            } else {
                for (String key : queryString.keySet()) {

                    builder.append(key);
                    builder.append("=");
                    builder.append(queryString.get(key));
                    builder.append(", ");
                }
                builder.delete(builder.length() - 2, builder.length());//remove the extra ", "
            }
            if (LOGGING) {
                Log.i(TAG,
                        builder.toString());
            }
            return queryString;
        }

        @Override
        public byte[] getBody() throws AuthFailureError {

            if (getMethod() == Request.Method.POST && body == null) {
                throw new IllegalStateException(TAG + ": POST methods must provide a body");
            }

            if (body != null) {

                String bodyStr = body.generateBody();
                if (LOGGING) {
                    Log.i(TAG,
                            "BODY: " + (bodyStr.isEmpty() ? "NONE" : bodyStr));
                }
                return bodyStr.getBytes();
            }

            return super.getBody();
        }

        @Override
        public void deliverError(VolleyError error) {
            printError(error);

            if (error != null && error.networkResponse != null) {

                try {

                    String body = new String(error.networkResponse.data,"UTF-8");
                    BaseResponse response = gson.fromJson(body, getResponseClass());

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            super.deliverError(error);
        }

        @Override
        public String getBodyContentType() {
            //We need to tell volley what the content type of the body is so we can send raw json
            return "application/json; charset=utf-8";
        }
    }

    private void printError(VolleyError error) {

        if (error != null && error.networkResponse != null) {

            String body;
            //get status code here
            final String statusCode = String.valueOf(error.networkResponse.statusCode);
            //get response body and parse with appropriate encoding
            try {

                body = new String(error.networkResponse.data,"UTF-8");
                Log.e(getClass().getSimpleName(), statusCode + ":\n" + body);

            } catch (UnsupportedEncodingException e) {

                e.printStackTrace();
                Log.e(getClass().getSimpleName(), "ERROR: Request Failed!");
            }

        } else {

            Log.e(getClass().getSimpleName(), "ERROR: Request Failed!");
        }
    }

    public String getMethodString() {

        switch (getRequestMethod()) {

            case Request.Method.GET:
                return "GET";

            case Request.Method.POST:
                return "POST";

            case Request.Method.PUT:
                return "PUT";

            case Request.Method.DELETE:
                return "DELETE";

            case Request.Method.HEAD:
                return "HEAD";

            case Request.Method.OPTIONS:
                return "OPTIONS";

            case Request.Method.TRACE:
                return "TRACE";

            case Request.Method.PATCH:
                return "PATCH";


            default:
                return "UNKNOWN METHOD";
        }
    }

    /**
     * Gets the request queue for volley.
     * @return
     */
    public static RequestQueue getRequestQueue(Context context) {
        if (requestQueue == null) {
            HurlStack stack = new HurlStack(null, createSslSocketFactory());
            requestQueue = Volley.newRequestQueue(context, stack);
        }
        return requestQueue;
    }

    private static SSLSocketFactory createSslSocketFactory() {
        TrustManager[] byPassTrustManagers = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        }};

        SSLContext sslContext = null;
        SSLSocketFactory sslSocketFactory = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, byPassTrustManagers, new SecureRandom());
            sslSocketFactory = sslContext.getSocketFactory();

        } catch (NoSuchAlgorithmException e) {
            Log.e("SSLSocketFactory", e.getMessage());
            e.printStackTrace();
        } catch (KeyManagementException e) {
            Log.e("SSLSocketFactory", e.getMessage());
            e.printStackTrace();
        }

        return sslSocketFactory;
    }
}
