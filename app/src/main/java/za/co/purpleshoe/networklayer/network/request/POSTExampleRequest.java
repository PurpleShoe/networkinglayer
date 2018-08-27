package za.co.purpleshoe.networklayer.network.request;

import android.content.Context;

import com.android.volley.Request;

import za.co.purpleshoe.networklayer.network.response.POSTExampleResponse;

/**
 * Created by Tyler Hogarth on 2018/08/27
 */
public class POSTExampleRequest extends BaseRequest<POSTExampleResponse> {

    public POSTExampleRequest(Context context) {
        super(context);
    }

    @Override
    public int getRequestMethod() {
        return Request.Method.POST;
    }

    @Override
    public String getRequestUrl() {
        return BASE_URL + ENDPOINT_POST_EXAMPLE;
    }

    @Override
    public Class<POSTExampleResponse> getResponseClass() {
        return POSTExampleResponse.class;
    }
}
