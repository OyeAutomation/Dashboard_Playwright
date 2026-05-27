package reports;

import com.aventstack.extentreports.ExtentTest;

import java.util.Map;

public class ExtentTestManager {

    private static final ThreadLocal<Map<String,String>> dataThread = new ThreadLocal<>();

    public static void setData(Map<String,String> data){
        dataThread.set(data);
    }

    public static Map<String,String> getData(){
        return dataThread.get();
    }

    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();

    public static ExtentTest getTest() {
        return testThread.get();
    }

    public static void setTest(ExtentTest test) {
        testThread.set(test);
    }

    public static void unload() {
        testThread.remove();
        dataThread.remove();
    }
}