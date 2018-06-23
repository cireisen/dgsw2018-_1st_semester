package kr.hs.dgsw.gpsmake;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.net.URL;
import java.net.URLConnection;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

    String[] loadname=new String[100];
    String[] load= new String[100];
    int[] traveltime = new int[100];
    //private GPSTask gps;
    Button btnShowLocation;
    EditText etResponse;
    EditText longtitudetext;
    EditText latitudetext;
    private double longtitude = 0;
    private double latitude = 0;
    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private final int PERMISSIONS_ACCESS_COARSE_LOCATION = 1001;
    private boolean isAccessFineLocation = false;
    private boolean isAccessCoarseLocation = false;
    private boolean isPermission = false;
    private String key="1522480427240";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etResponse = findViewById(R.id.etResponse);
        longtitudetext = findViewById(R.id.longtitudeview);
        latitudetext = findViewById(R.id.latitudeview);
        btnShowLocation = findViewById(R.id.btnShowLocation);

        btnShowLocation.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View view) {
                try {
                    new GetGPSXML(getLocation()).execute();
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        });
        callPermission();
    }

    @SuppressLint("StaticFieldLeak")
    public class GetGPSXML extends AsyncTask<Void, Integer, Void> {

        private Location location;
        GetGPSXML(Location location) {
            this.location = location;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Log.d("TAG", "onClick: ");
                if (!isPermission) {
                    callPermission();
                    return null;
                }

                if (location != null) {
                    longtitude = location.getLongitude();
                    latitude = location.getLatitude();
                }
                longtitudetext.setText(String.valueOf(longtitude));
                latitudetext.setText(String.valueOf(latitude));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(
                                getApplicationContext(),
                                "당신의 위치 - \n위도: " + latitude + "\n경도: " + longtitude,
                                Toast.LENGTH_LONG).show();
                    }
                });

                String xmlUrl = "http://openapi.its.go.kr/api/NTrafficInfo?key="+key+"&ReqType=2&MinX=" + (longtitude - 0.05f) + "&MaxX=" + (longtitude + 0.05f) + "&MinY=" + (latitude - 0.05f) + "&MaxY=" + (latitude + 0.05f);
                Log.d("xml", xmlUrl);
                URL url = new URL(xmlUrl);
                URLConnection connection = url.openConnection();
                int cnt=0;
                Document doc = parseXML(connection.getInputStream());
                NodeList checkNULL = doc.getElementsByTagName("response");
                Node Nul = checkNULL.item(0).getFirstChild();
                if (Nul.getTextContent().equals("NULL"))
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            etResponse.setText("주위에 고속도로가 없습니다.");
                        }
                    });
                NodeList descNodes = doc.getElementsByTagName("data");

                for(int i = 0 ; i <traveltime.length;i++)
                    traveltime[i] = 0;
                for (int i = 0; i < descNodes.getLength(); i++) {
                    for (Node node = descNodes.item(i).getFirstChild(); node != null; node = node.getNextSibling())
                        if (node.getNodeName().equals("roadnametext")) {
                            loadname[i]=node.getTextContent();
                        }
                }
                for (int i = 0; i < descNodes.getLength(); i++) {
                    Log.d("xml", "a");
                    for (Node node = descNodes.item(i).getFirstChild(); node != null; node = node.getNextSibling())
                        if (node.getNodeName().equals("roadnametext")) {
                            Log.d("loadname", node.getTextContent());
                            loadname[i]=node.getTextContent();
                        }

                }
                for(int i = 0 ;i<descNodes.getLength();i++)
                {
                    if(loadname[i]!=null)
                        if(i==0)
                        {
                            load[cnt] = loadname[i];
                            cnt++;
                        }

                        else if(loadname[i].equals(loadname[i-1])) {
                        }
                        else
                        {
                            load[cnt] = loadname[i];
                            cnt++;
                        }
                }
                for (int i = 0; i < descNodes.getLength(); i++) {
                    for (Node node = descNodes.item(i).getFirstChild(); node != null; node = node.getNextSibling())
                        if (node.getNodeName().equals("roadnametext")) {
                            for(int j = 0 ;j<load.length;j++)
                                if(node.getTextContent().equals(load[j]))
                                    for(Node node2 = descNodes.item(i).getFirstChild(); node2!=null;node2 = node2.getNextSibling())
                                    {
                                        if(node2.getNodeName().equals("traveltime"))
                                            traveltime[j] += Integer.parseInt(node2.getTextContent());
                                    }
                        }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }
    public String[] getloadnames()
    {
        return this.loadname;
    }
    public int[] getTraveltime()
    {
        return this.traveltime;
    }
    private Location getLocation() {
        // 네트워크 사용유무
        boolean isNetworkEnabled;

        /*//상태값
        boolean isGetLocation = false;*/

        Location location = null;

        LocationManager locationManager;

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(
                        this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                        this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            return null;
        }
        try {
            locationManager = (LocationManager)
                    this.getSystemService(LOCATION_SERVICE);

            // 현재 네트워크 상태 값 알아오기
            isNetworkEnabled = locationManager != null && locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (isNetworkEnabled) {
                //isGetLocation = true;
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        60000, 10, new SimpleLocationListener() {
                        });
                Log.d("TAG", "NETWORKENABLED");


                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                Log.d("TAG", "a");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    private Document parseXML(InputStream stream) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory objDocumentBuilderFactory;
        DocumentBuilder objDocumentBuilder;
        Document doc;

        objDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
        objDocumentBuilder = objDocumentBuilderFactory.newDocumentBuilder();

        doc = objDocumentBuilder.parse(stream);

        return doc;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_ACCESS_FINE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            isAccessFineLocation = true;

        } else if (requestCode == PERMISSIONS_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            isAccessCoarseLocation = true;
        }

        if (isAccessFineLocation && isAccessCoarseLocation) {
            isPermission = true;
        }
    }

    /*public static String GET(String url) {
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputStream to string
            if (inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }*/

    /*private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder result = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null)
            result.append(line);

        inputStream.close();
        return result.toString();

    }*/

    /*private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG).show();
            etResponse.setText(result);
        }
    }*/

    // 전화번호 권한 요청
    private void callPermission() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_ACCESS_FINE_LOCATION);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_ACCESS_COARSE_LOCATION);
        } else {
            isPermission = true;
        }
    }
}
