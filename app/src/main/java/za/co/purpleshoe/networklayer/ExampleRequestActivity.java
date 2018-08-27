package za.co.purpleshoe.networklayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import za.co.purpleshoe.networklayer.network.body.POSTExampleBody;
import za.co.purpleshoe.networklayer.network.request.POSTExampleRequest;
import za.co.purpleshoe.networklayer.network.response.POSTExampleResponse;

public class ExampleRequestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example_request);
        executeRequest();
    }

    public void executeRequest() {

        POSTExampleBody body = new POSTExampleBody();
        //set values for body

        POSTExampleRequest request = new POSTExampleRequest(this);
        request.setResponseListener(new Response.Listener<POSTExampleResponse>() {
                    @Override
                    public void onResponse(POSTExampleResponse response) {
                        //do something with the response
                    }
                })
                .setErrorResponseListener(new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //tod something with the error
                    }
                })
                .setBody(body)
                .addHeader("name", "value")//add to headers
                .addQuery("name", "value")//add to query string
                .send();//send this sucker

    }
}
