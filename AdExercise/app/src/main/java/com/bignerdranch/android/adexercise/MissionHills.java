package com.bignerdranch.android.adexercise;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by Christian on 3/5/2018.
 */

public class MissionHills extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mission_hills);
        Button playCourse = (Button) findViewById(R.id.play_course_button);
        playCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WelcomeCourse.class);
                startActivity(intent);
            }
        });
    }
}