package uk.ac.ed.inf;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;


public class WebClient
{
    private static final HttpClient client = HttpClient.newHttpClient();
    private final String machine;
    private final String port;

    /**
     * Class constructor. Simply sets the values of the machine and port on which the web server is running.
     * @param machine the machine name
     * @param port the port number
     */
    public WebClient(String machine, String port)
    {
        this.machine = machine;
        this.port = port;
    }

    public HttpRequest buildServerRequest(String resource)
    {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://" + machine + ":"+ port + resource))
                .build();
    }

    public String getStringResponse(HttpRequest request)
    {
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("HTTP request failed with status code: " + response.statusCode());
                System.exit(1);
            } else {
                return response.body();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        assert false;
        return null;
    }
}
