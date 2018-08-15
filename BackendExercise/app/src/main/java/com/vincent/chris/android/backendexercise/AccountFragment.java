package com.vincent.chris.android.backendexercise;

import android.content.Context;
import android.content.Intent;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobile.api.CloudLogicAPI;
import com.amazonaws.mobile.api.CloudLogicAPIConfiguration;
import com.amazonaws.mobile.api.CloudLogicAPIFactory;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils;
import com.amazonaws.mobile.auth.userpools.CognitoUserPoolsSignInProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfigurable;
import com.amazonaws.mobileconnectors.apigateway.ApiRequest;
import com.amazonaws.mobileconnectors.apigateway.ApiResponse;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.models.nosql.BikerDO;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by Chris on 2/8/2018.
 */

public class AccountFragment extends Fragment {
    Button mEnterButton, mSignOutButton;
    EditText mEditLogin, mEditPassword, mEditFirstName, mEditLastName;
    CognitoUserPool mUserPool;
    CognitoUserDetails mUserDetails;

    private static String LOG_TAG;

    private CloudLogicAPIConfiguration apiConfiguration;
    public static final int DEFAULT_API_INDEX = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_account, container, false);
        mEnterButton = rootView.findViewById(R.id.enterButton);
        mSignOutButton = rootView.findViewById(R.id.signOutButton);
        mEditLogin = rootView.findViewById(R.id.editLogin);
        mEditPassword = rootView.findViewById(R.id.editPassword);
        mEditFirstName = rootView.findViewById(R.id.editFirstName);
        mEditLastName = rootView.findViewById(R.id.editLastName);
        mUserPool = new CognitoUserPool(
                getContext(),
                "us-west-2_EtPzER66I",
                "40idh984nluif70dmitofcts3o",
                "1mm69ggrnlqvdnqv2k762vkrit0abmpsv29kjsa85es0l3dc8o7c", Regions.US_WEST_2);
        getUserDetails();
        mEnterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    JSONObject bodyJSON = new JSONObject();
                    bodyJSON.put("userId",mEditLogin.getText().toString());
                    bodyJSON.put("firstName",mEditFirstName.getText().toString());
                    bodyJSON.put("lastName",mEditLastName.getText().toString());
                    bodyJSON.put("password",mEditPassword.getText().toString());
                    bodyJSON.put("date_lastAccess",new Date().toString());
                    String body = bodyJSON.toString();
                    //invokeRequest(body,"/verifyUser",BackendExerciseActivity.BIKERS_API_INDEX);
                    invokeRequest(body,"/createUser",BackendExerciseActivity.BIKERS_API_INDEX);
                    storeUserIdInLocalStorage();
                    //refresh Route Fragment view
                    refreshRouteFragment();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        mSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUserPool.getCurrentUser().signOut();
                Intent intent = new Intent(getContext(),AuthenticatorActivity.class);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void refreshRouteFragment() {
        FragmentManager fm = getFragmentManager();
        for (Fragment frag: fm.getFragments())
        {
            if (frag instanceof RouteFragment)
                ((RouteFragment)frag).getUserIdFromStorage();
        }
    }

    private void storeUserIdInLocalStorage() throws IOException {
        //store userId (email) in local storage.
        String FILENAME = "userId";
        String userId = mEditLogin.getText().toString();
        FileOutputStream fos = getActivity().openFileOutput(FILENAME, Context.MODE_PRIVATE);
        fos.write(userId.getBytes());
        fos.close();
    }

    private void getUserDetails() {
        //get signed-in User's details
        CognitoUser user = mUserPool.getCurrentUser();
        user.getDetailsInBackground(new GetDetailsHandler() {
            @Override
            public void onSuccess(CognitoUserDetails cognitoUserDetails) {
                Log.d("Backend", "Got details");
                mUserDetails = cognitoUserDetails;
                mEditLogin.setText(cognitoUserDetails.getAttributes().getAttributes().get("email"));
                String []names = cognitoUserDetails.getAttributes().getAttributes().get("given_name").split("\\s");
                mEditFirstName.setText(names[0]);
                mEditLastName.setText(names[names.length - 1]);
                try {
                    storeUserIdInLocalStorage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                refreshRouteFragment();
            }

            @Override
            public void onFailure(Exception exception) {
                Log.d("Backend", "No details");
                Log.d("Backend", exception.toString());
            }
        });
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
                        if(path.equals("/createUser")) {
                            setResponseBodyText(responseData);
                        }
                        if(path.equals("/verifyUser"))
                        {
                            setResponseBodyTextVerify(responseData);
                        }

                    }

                } catch (final Exception exception) {

                    Log.e(LOG_TAG, exception.getMessage(), exception);
                    exception.printStackTrace();
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //take response and DISPLAY IT
                            setResponseBodyText(exception.getMessage());
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
     * receives the JSON string and dumps it to the logger
     * @param text */
    private void setResponseBodyText(final String text) {

        ThreadUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Log.d("Backend",text);
            }

        });

    }
    private void setResponseBodyTextVerify(final String text) throws JSONException {

        JSONObject response = new JSONObject(text);
        if(response.optString("errorMessage") != null && response.optString("errorMessage").equals("Failed verification, passwords not the same"))
        {
            Toast.makeText(getContext(), "Password is incorrect", Toast.LENGTH_SHORT).show();
        }
        ThreadUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Log.d("Backend",text);
            }

        });

    }
}
