package com.vincent.chris.android.backendexercise;

import android.app.ActionBar;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobile.api.CloudLogicAPI;
import com.amazonaws.mobile.api.CloudLogicAPIConfiguration;
import com.amazonaws.mobile.api.CloudLogicAPIFactory;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.apigateway.ApiRequest;
import com.amazonaws.mobileconnectors.apigateway.ApiResponse;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.models.nosql.BikerDO;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Created by Chris on 2/8/2018.
 */

public class RouteFragment extends Fragment {
    public static final int TEXT_SIZE = 10;
    TableLayout mRouteTable;
    TableRow mHeaderRow;
    Button mNewRouteButton;
    TextView mEditNameLabel;
    TextView mEditStartLatLabel;
    TextView mEditEndLatLabel;
    TextView mEditStartLngLabel;
    TextView mEditEndLngLabel;
    EditText mEditName;
    EditText mEditStartLat;
    EditText mEditEndLat;
    EditText mEditStartLng;
    EditText mEditEndLng;

    CognitoUserPool mUserPool;
    CognitoUser mUser;
    CognitoUserDetails mUserDetails;
    private String mUserId;
    private JSONObject mResponseObj;

    private CloudLogicAPIConfiguration apiConfiguration;

    private static String LOG_TAG;
    private boolean mButtonsShowing = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_routes, container, false);
        mRouteTable = rootView.findViewById(R.id.routeTable);
        mHeaderRow= rootView.findViewById(R.id.headerRow);
        mEditNameLabel = rootView.findViewById(R.id.editNameLabel);
        mEditName = rootView.findViewById(R.id.editName);
        mEditStartLatLabel = rootView.findViewById(R.id.editStartLatLabel);
        mEditEndLatLabel = rootView.findViewById(R.id.editEndLatLabel);
        mEditStartLat = rootView.findViewById(R.id.editStartLat);
        mEditEndLat = rootView.findViewById(R.id.editEndLat);   
        mEditStartLngLabel = rootView.findViewById(R.id.editStartLngLabel);   
        mEditEndLngLabel = rootView.findViewById(R.id.editEndLngLabel);   
        mEditStartLng = rootView.findViewById(R.id.editStartLng); 
        mEditEndLng = rootView.findViewById(R.id.editEndLng);   
        mNewRouteButton = rootView.findViewById(R.id.newRouteButton);
        mNewRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 if(!mButtonsShowing) {
                     showButtons();
                     mNewRouteButton.setText("Enter");
                 }
                 else
                 {
                     JSONObject requestObj = new JSONObject();
                     try {
                         requestObj.put("name",mEditName.getText().toString());
                         requestObj.put("start_latitude",mEditStartLat.getText().toString());
                         requestObj.put("stop_latitude",mEditEndLat.getText().toString());
                         requestObj.put("start_longitude",mEditStartLng.getText().toString());
                         requestObj.put("stop_longitude",mEditEndLng.getText().toString());
                         requestObj.put("userId",mUserId);
                         requestObj.put("routeId", UUID.randomUUID().toString());
                         requestObj.put("category", "none");
                         requestObj.put("number_traveled", 0);
                         invokeRequest(requestObj.toString(),"/storeRoute",BackendExerciseActivity.ROUTES_API_INDEX);
                     } catch (JSONException e) {
                         e.printStackTrace();
                     }
                 }
            }
        });
        return rootView;
    }

    private void showButtons() {
        mEditNameLabel.setVisibility(View.VISIBLE);
        mEditStartLatLabel.setVisibility(View.VISIBLE);
        mEditEndLatLabel.setVisibility(View.VISIBLE);
        mEditStartLngLabel.setVisibility(View.VISIBLE);
        mEditEndLngLabel.setVisibility(View.VISIBLE);
        mEditName.setVisibility(View.VISIBLE);
        mEditStartLat.setVisibility(View.VISIBLE);
        mEditEndLat.setVisibility(View.VISIBLE);
        mEditStartLng.setVisibility(View.VISIBLE);
        mEditEndLng.setVisibility(View.VISIBLE);
        mButtonsShowing = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        //getUserIdFromStorage();
    }

    public void getUserIdFromStorage() {
        //get userId from local storage
        String FILENAME = "userId";
        byte [] buffer = new byte[256];
        try {
            FileInputStream fis = getActivity().openFileInput(FILENAME);
            while (fis.available() > 0)
            fis.read(buffer);
            mUserId = new String(buffer).replace("\0","");
            JSONObject body = new JSONObject();
            body.put("userId",mUserId);
            invokeRequest(body.toString(),"/getRoutes",BackendExerciseActivity.ROUTES_API_INDEX);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }


    private void populateTable(String routeString) throws JSONException {
        //remove all other rows except header
        Log.d(TAG, "populateTable: " + mRouteTable.getChildCount());
        for (int i = 0; i < mRouteTable.getChildCount(); i++)
        {
            if(mRouteTable.getChildAt(i) != mHeaderRow)
                mRouteTable.removeViewAt(i);
        }
        //add rows
        JSONArray routes = new JSONArray(routeString);
        for (int i = 0; i < routes.length(); i++)
        {
            JSONObject route = routes.getJSONObject(i);
            Log.i(TAG, "populateTable: " + route.toString());
            //TODO fix formatting
            TableRow row = createTableRow(route);
            mRouteTable.addView(row);
        }
        mNewRouteButton.setEnabled(true);
    }

    @NonNull
    private TableRow createTableRow(final JSONObject route) {
        TableRow row = new TableRow(getContext());
        TextView name = new TextView(getContext());
        final Button useButton = new Button(getContext());
        useButton.setText("Go");
        useButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
        useButton.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        useButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    useRoute(route);
                    //update "Used" column for the route
                    String temp =((TextView) ((TableRow) useButton.getParent()).getChildAt(6)).getText().toString();
                    Integer tempInt = Integer.parseInt(temp) + 1;
                    ((TextView) ((TableRow) useButton.getParent()).getChildAt(6)).setText(tempInt.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        name.setText(route.optString("name"));
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
        TextView startLat = new TextView(getContext());
        startLat.setText(route.optString("start_latitude"));
        startLat.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
        TextView startLng = new TextView(getContext());
        startLng.setText(route.optString("start_longitude"));
        startLng.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
        TextView endLat = new TextView(getContext());
        endLat.setText(route.optString("stop_latitude"));
        endLat.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
        TextView endLng = new TextView(getContext());
        endLng.setText(route.optString("stop_longitude"));
        endLng.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
        TextView used = new TextView(getContext());
        used.setText(route.optString("number_traveled"));
        used.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
        row.addView(useButton,0);
        row.addView(name,1);
        row.addView(startLat,2);
        row.addView(startLng,3);
        row.addView(endLat,4);
        row.addView(endLng,5);
        row.addView(used,6);
        return row;
    }
    private void useRoute(JSONObject route) throws JSONException {
        JSONObject requestObj = new JSONObject();
        requestObj.put("routeId",route.optString("routeId"));
        requestObj.put("userId",mUserId);
        invokeRequest(requestObj.toString(),"/useRoute",BackendExerciseActivity.ROUTES_API_INDEX);
    }

    public CloudLogicAPI createAPIClient(final Class<?> clientClass) {
        for (final CloudLogicAPIConfiguration config : CloudLogicAPIFactory.getAPIs()) {
            if (config.getClientClass().equals(clientClass)) {
                return config.getClient();
            }
        }

        throw new IllegalArgumentException("Unable to find API client for class: " + clientClass.getName());
    }
    /**
     * invokeRequest to AWS performed on separate Thread rather than using AsyncTask
     *Makes /getRoutes request to AWS Lambda backend that retrieves all routs associated with
     * userId specified in the body class variable. Gets results and call setBodyText method
     * to display response.
     */
    private void invokeRequest(String body, final String path, int apiIndex) {
        LOG_TAG = "Invoke";
        final String method = "POST";
        final String queryStringText = "?lang=en_US";
        final Map<String, String> parameters = convertQueryStringToParameters(queryStringText);
        apiConfiguration = CloudLogicAPIFactory.getAPIs()[apiIndex];
        final CloudLogicAPI client = this.createAPIClient(apiConfiguration.getClientClass());
        final Map<String, String> headers = new HashMap<String, String>();
        final byte[] content = body.getBytes(StringUtils.UTF8);
        ApiRequest tmpRequest =
                new ApiRequest(client.getClass().getSimpleName())
                        .withPath(path)
                        .withHttpMethod(HttpMethodName.valueOf(method))
                        .withHeaders(headers)
                        .addHeader("Content-Type", "application/json")
                        .withParameters(parameters);

        final ApiRequest request;

        // Only set body if it has content.
        if (body.length() > 0) {
            request = tmpRequest
                    .addHeader("Content-Length", String.valueOf(content.length))
                    .withBody(content);
        } else {
            request = tmpRequest;
        }
        // Make network call on background thread
        new Thread(new Runnable() {

            Exception exception = null;

            @Override
            public void run() {

                try {

                    Log.d(LOG_TAG, "Invoking API w/ Request : " + request.getHttpMethod() + ":" + request.getPath());
                    long startTime = System.currentTimeMillis();

                    //THIS EXECUTES THE REQUEST
                    final ApiResponse response = client.execute(request);
                    final long latency = System.currentTimeMillis() - startTime;

                    final InputStream responseContentStream = response.getContent();  //GRAB THE RESPONSE
                    if (responseContentStream != null) {
                        final String responseData = IOUtils.toString(responseContentStream);
                        Log.d(LOG_TAG, "Response : " + responseData);
                        if (path.equals("/getRoutes"))
                        {
                            setResponseBodyText(responseData);
                        }
                        if(path.equals("/useRoute"))
                        {
                            createUseRouteToast(responseData);
                        }
                        if(path.equals("/storeRoute"))
                        {
                            createStoreRouteToast(responseData);
                        }
                    }

                } catch (final Exception exception) {

                    Log.e(LOG_TAG, exception.getMessage(), exception);
                    exception.printStackTrace();
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //take response and DISPLAY IT
                            Log.e(LOG_TAG,exception.getMessage());
                        }
                    });

                }

            }


        }).start();

    }



    /** * setups parameters of query string in Map format to pass to AWS request
     * @param queryStringText
     * @return */
    private Map<String,String> convertQueryStringToParameters(String queryStringText) {

        while (queryStringText.startsWith("?") && queryStringText.length() > 1) {

            queryStringText = queryStringText.substring(1);
        }

        final UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
        sanitizer.setAllowUnregisteredParamaters(true);
        sanitizer.parseQuery(queryStringText);

        final List<UrlQuerySanitizer.ParameterValuePair> pairList = sanitizer.getParameterList();
        final Map<String, String> parameters = new HashMap<>();

        for (final UrlQuerySanitizer.ParameterValuePair pair : pairList) {

            Log.d(LOG_TAG, pair.mParameter + " = " + pair.mValue);
            parameters.put(pair.mParameter, pair.mValue);

        }

        return parameters;


    }
    /**
     * receives the JSON string and uses it to populate the table
     * @param text */
    private void setResponseBodyText(final String text) {
        ThreadUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    mResponseObj= new JSONObject(text);
                    populateTable(mResponseObj.getString("routes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        });
    }
    private void createUseRouteToast(final String text)
    {
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try{
                    Log.d(TAG,text);
                    mResponseObj = new JSONObject(text);
                    String routeObj = mResponseObj.getString("routes");
                    Log.d(TAG,routeObj);
                    JSONObject route = new JSONObject(routeObj);
                    String toastText = "The following route will begin. This route has now been used " + route.optString("number_traveled") + " time(s).\n";
                    toastText += route.optString("name") + "\t";
                    toastText += "Start: " + route.optString("start_latitude") + ", " + route.optString("start_longitude") + "\t";
                    toastText += "End: " + route.optString("stop_latitude") + ", " + route.optString("stop_longitude");
                    Toast.makeText(getContext(), toastText, Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private void createStoreRouteToast(final String text) {
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), "Route created", Toast.LENGTH_SHORT).show();
                try {
                    mResponseObj = new JSONObject(text);
                    JSONObject route = mResponseObj.getJSONObject("requestBody");
                    mRouteTable.addView(createTableRow(route));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
