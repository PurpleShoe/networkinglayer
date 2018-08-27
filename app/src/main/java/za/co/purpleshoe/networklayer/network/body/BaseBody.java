package za.co.purpleshoe.networklayer.network.body;

import com.google.gson.Gson;

/**
 * Created by Tyler Hogarth on 2018/08/27
 */
public class BaseBody {

    public String generateBody() {
        return new Gson().toJson(this);
    }
}
