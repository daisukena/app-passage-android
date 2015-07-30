package hive.android.hebron;

import android.app.Activity;
import android.app.Application;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ApplicationTestCase;

import com.robotium.solo.Solo;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ActivityInstrumentationTestCase2<TestActivity> {

    private Solo solo;

    public ApplicationTest() {
        super(TestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // should do after all components initialized
        setSolo(new Solo(getInstrumentation(), getActivity()));

        setActivityInitialTouchMode(true);
    }

    public void testInputValidEmailAndPassword() throws InterruptedException {
        getSolo().waitForActivity("olololol", Integer.MAX_VALUE);
    }


    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public Solo getSolo() {
        return solo;
    }

    public void setSolo(Solo solo) {
        this.solo = solo;
    }

}