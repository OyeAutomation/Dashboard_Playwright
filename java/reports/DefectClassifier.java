package reports;

public class DefectClassifier {

    public static String classify(Throwable e){

        if(e==null)
            return "Unknown";

        String message = e.getMessage();

        if (message == null || message.trim().isEmpty()) {
            message = e.toString();
        }

        String msg = message.toLowerCase();

        if(msg.contains("assert")
                || msg.contains("validation"))
            return "Functional Failure -> Defect";

        if(msg.contains("data")
                || msg.contains("environment"))
            return "Data Issue -> Blocked";

        if(msg.contains("staleelement")
                || msg.contains("timeout")
                || msg.contains("nosuchelement"))
            return "Automation Issue -> Script Failure";

        return "Needs Review";
    }

}
