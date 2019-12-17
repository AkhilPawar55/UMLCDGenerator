import java.util.Vector;

public class UMLRelation {

    RelationshipType relationType;
    Vector<String> classTo;
    String classFrom;
    Arity cardinality;

    public UMLRelation(){

        this.relationType = RelationshipType.ASSOCIATION;
        this.classTo = new Vector<>();
        this.classFrom = null;
        this.cardinality = Arity.ONE_OR_MANY;
    }

    public void setRelationType(RelationshipType relationType){

        this.relationType = relationType;
    }

    public void setClassTo(String classTo){

        this.classTo.add(classTo);;
    }

    public void setClassFrom(String classFrom){

        this.classFrom = classFrom;
    }

    public void setCardinality(Arity cardinality){

        this.cardinality = cardinality;
    }
}