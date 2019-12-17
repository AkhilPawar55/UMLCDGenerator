import java.util.*;

public class UMLClass {

    int order;
    String name;
    String parentClass;
    String wholeClass;
    Vector<UMLFunction> functionList;
    HashMap<String, UMLAttribute> attribList;
    HashSet<UMLRelation> relationList;
    boolean isAbstract;

    UMLClass(String c_name, String p_name, int number){

        order = number;
        name = c_name;
        parentClass = p_name;
        functionList = new Vector<>();
        attribList = new HashMap<>();
        relationList = new HashSet<>();
        isAbstract = false;
    }

    public void setName(String name){

        this.name = name;
    }

    public void setParentClass(String parentClass){

        this.parentClass = parentClass;
    }

    public void setAbstract(){

        this.isAbstract = true;
    }

    public void setWholeClass(String w){

        wholeClass = w;
    }
}
