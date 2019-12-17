import java.util.HashMap;

public class UMLFunction {

    String name;
    String return_type;
    HashMap<String, String> ArgumentList;

    UMLFunction(){

        name = "";
        return_type = "";
    }

    UMLFunction(String n, String ret_type, HashMap<String, String> arg_list){

        name = n;
        return_type = ret_type;
        ArgumentList = arg_list;
    }

    public void setReturn_type(String r){

        return_type = r;
    }
}
