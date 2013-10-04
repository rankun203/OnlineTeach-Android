package org.rankun.onlineteach.android.test;

import android.test.ActivityInstrumentationTestCase2;
import org.rankun.onlineteach.android.*;

public class HelloAndroidActivityTest extends ActivityInstrumentationTestCase2<GateActivity> {

    public HelloAndroidActivityTest() {
        super(GateActivity.class); 
    }

    public void testActivity() {
    	GateActivity activity = getActivity();
        assertNotNull(activity);
    }
}

