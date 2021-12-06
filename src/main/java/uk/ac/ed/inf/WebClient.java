package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;

/**
 * Class to handle all direct interaction with the web server.
 */
public class WebClient
{
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private final String MACHINE;
    private final String PORT;
    private final String CONNECTION_ERROR_MESSAGE =
            "Web server connection failed. Check server is running and port number is correct.";

    /**
     * Class constructor. Simply sets the values of the machine and port on which the web server is running.
     * @param machine the machine name
     * @param port the port number
     */
    public WebClient(String machine, String port)
    {
        this.MACHINE = machine;
        this.PORT = port;
    }

    public HttpRequest buildServerRequest(String resource)
    {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://" + MACHINE + ":"+ PORT + resource))
                .build();
    }

    public String getStringResponse(HttpRequest request)
    {
        try {
            HttpResponse<String> response = CLIENT.send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("HTTP request failed with status code: " + response.statusCode());
                System.exit(1);
            } else {
                return response.body();
            }
        } catch (Exception e)
        {
            System.err.println(CONNECTION_ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1);
        }
        assert false;
        return null;
    }

    public ArrayList<Shop> getMenus()
    {
        HttpRequest request = buildServerRequest("/menus/menus.json");
        String response = getStringResponse(request);

        Type listType = new TypeToken<ArrayList<Shop>>() {}.getType();

        return new Gson().fromJson(response, listType);
    }
}
