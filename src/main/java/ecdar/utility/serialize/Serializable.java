package ecdar.utility.serialize;


import com.google.gson.JsonObject;

public interface Serializable {

    JsonObject serialize();

    void deserialize(JsonObject json);

}
