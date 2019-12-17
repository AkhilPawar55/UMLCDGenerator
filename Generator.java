import com.sun.org.apache.xpath.internal.Arg;

import javax.management.relation.Relation;
import javax.sql.rowset.Predicate;
import java.io.*;
import java.util.*;

public class Generator {

    HashMap<String, UMLClass> classList = new HashMap<>();
    static int count = -1;

    public static void main(String[] args) throws Exception, IOException {
        BufferedReader bd = new BufferedReader(new FileReader(args[0]));

        String inputLine = null;
        StringBuilder builder = new StringBuilder();

        while((inputLine = bd.readLine()) != null) {
            builder.append(inputLine);
            builder.append(" ");
        }

        String s = builder.toString();

        StringReader srd = new StringReader(s);
        Specification result = new Parser(srd).Sp();

        Generator generator = new Generator();

        // first iterate through the signatures
        for(int i = 0; i < result.paragraph_list.size(); i++)
        {

            Exp p = result.paragraph_list.get(i);

            if(p instanceof Signature){

                Signature sig = (Signature) p;

                String x = sig.name.toString();
                x = x.replace("(","");
                x = x.replace(")","");


                // check if the class name is a set of classes
                // i.e of the form class1 class2 ... classN

                Vector<String> sigList = new Vector<>();

                if(x.contains(" ")){

                    // split the classes and process each class

                    String[] arr = x.split(" ");

                    for(String a: arr){

                        sigList.add(a);
                    }
                }else{

                    sigList.add(x);
                }

                for(int l = 0; l < sigList.size(); l++){

                    // check if a class of corresp signature exists

                    if(!generator.classList.containsKey(sigList.get(l))) {

                        String clsName = "";
                        String pName = "";
                        String wName = "";

                        if(sigList.get(l) != null)
                            clsName = sigList.get(l);

                        if(sig.parentClass != null) {

                            if(sig.parentClass.toString().contains("/")){

                                String[] arr = sig.parentClass.toString().split("/");
                                pName = arr[arr.length-1];
                                pName = pName.replace("(","");
                                pName = pName.replace(")","");
                                pName = pName.replace(" ","");
                            }else{

                                pName = sig.parentClass.toString();
                            }

                            // If class does not exist for the parent class make one

                            if(!generator.classList.containsKey(sig.parentClass.toString())){

                                String pPName = "";

                                UMLClass pCls = new UMLClass(pName, pPName, ++count);

                                generator.classList.put(pName, pCls);
                            }
                        }

                        if(sig.wholeClass != null) {

                            if(sig.wholeClass.toString().contains("/")){

                                String[] arr = sig.wholeClass.toString().split("/");
                                wName = arr[arr.length-1];
                                wName = pName.replace("(","");
                                wName = pName.replace(")","");
                                wName = pName.replace(" ","");
                            }else{

                                wName = sig.wholeClass.toString();
                            }

                            // If class does not exist for the parent class make one

                            if(!generator.classList.containsKey(sig.wholeClass.toString())){

                                String wWName = "";

                                UMLClass wCls = new UMLClass(wName, wWName, ++count);

                                generator.classList.put(wName, wCls);
                            }
                        }

                        UMLClass cls = new UMLClass(clsName, pName, ++count);

                        cls.setWholeClass(wName);

                        if(cls.parentClass != "") {
                            if (sig.extension) {
                                UMLRelation relation = new UMLRelation();

                                relation.setClassFrom(clsName);
                                relation.setClassTo(pName);

                                relation.setRelationType(RelationshipType.GENERALIZATION);
                                cls.relationList.add(relation);
                            }
                        }
                        if(cls.wholeClass != "") {
                            if(sig.composition) {
                                UMLRelation relation = new UMLRelation();

                                relation.setClassFrom(clsName);
                                relation.setClassTo(wName);

                                relation.setRelationType(RelationshipType.AGGREGATION);
                                cls.relationList.add(relation);
                            }
                        }

                        // check if abstract
                        for(int j = 0; j < sig.qualifierList.size(); j++){

                            if(sig.qualifierList.get(j) instanceof Abstract)
                            {

                                cls.setAbstract();
                            }
                        }

                        // iterate through body
                        for(int k = 0; k < sig.bodyList.size(); k++){

                            Decl declaration = (Decl) sig.bodyList.get(k);

                            Vector<String> nameList = new Vector<>();

                            x = declaration.name.toString();
                            x = x.replace("(","");
                            x = x.replace(")","");

                            if(x.contains(" ")){

                                // split the classes and process each class

                                String[] arr = x.split(" ");

                                for(String a: arr){

                                    nameList.add(a);
                                }
                            }else{

                                nameList.add(x);
                            }

                            String type = "";
                            String classFrom = clsName;
                            String classTo = "";

                            UMLRelation relation = new UMLRelation();
                            relation.setClassFrom(classFrom);

                            // if exp is of type unary expression
                            if(declaration.expr instanceof UnaryExp){

                                UnaryExp exp = (UnaryExp) declaration.expr;

                                if(exp.op instanceof Lone){

                                    relation.setCardinality(Arity.ZERO_OR_ONE);
                                }
                                else if(exp.op instanceof Some){

                                    relation.setCardinality(Arity.ONE_OR_MANY);
                                }
                                else if(exp.op instanceof One){

                                    relation.setCardinality(Arity.ONE);
                                }

                                if(exp.exp instanceof BinaryExp){

                                    if(((BinaryExp) exp.exp).op instanceof Plus || ((BinaryExp) exp.exp).op instanceof And){

                                        // Create a new abstract class
                                        // such that the two classes extends
                                        // the class of their union
                                        // name that class as NewClass#
                                        // maintain the number;
                                        String unnamedClassName = ((BinaryExp) exp.exp).left.toString() + "Or" + ((BinaryExp) exp.exp).right.toString();
                                        UMLClass unnamedClass = new UMLClass(unnamedClassName, "", ++count);
                                        unnamedClass.setAbstract();
                                        generator.classList.put(unnamedClassName, unnamedClass);

                                        // Create two relations for left and right class
                                        // Check if the class exits

                                        String search = "";
                                        if(cls.attribList.get(((BinaryExp) exp.exp).left.toString()) != null){

                                            search = cls.attribList.get(((BinaryExp) exp.exp).left.toString()).type;
                                        }
                                        else{

                                            search = ((BinaryExp) exp.exp).left.toString();
                                        }
                                        if(!generator.classList.containsKey(search)){

                                            // Create the class
                                            if(search.equals(cls.name)){

                                                UMLRelation rel = new UMLRelation();
                                                rel.setClassFrom(search);
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassTo(unnamedClassName);
                                                cls.relationList.add(rel);
                                            }
                                            else{

                                                UMLClass leftClass = new UMLClass(search, unnamedClassName, ++count);
                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                leftClass.relationList.add(rel);
                                                generator.classList.put(leftClass.name, leftClass);
                                            }
                                        }
                                        else{

                                            UMLRelation rel = new UMLRelation();
                                            rel.setRelationType(RelationshipType.GENERALIZATION);
                                            rel.setClassFrom(search);
                                            rel.setClassTo(unnamedClassName);
                                            generator.classList.get(search).relationList.add(rel);
                                        }

                                        // Check if the class exits

                                        search = "";
                                        if(cls.attribList.get(((BinaryExp) exp.exp).right.toString()) != null){

                                            search = cls.attribList.get(((BinaryExp) exp.exp).right.toString()).type;
                                        }
                                        else{

                                            search = ((BinaryExp) exp.exp).right.toString();
                                        }
                                        if(!generator.classList.containsKey(search)){

                                            // Create the class
                                            if(search.equals(cls.name)){

                                                UMLRelation rel = new UMLRelation();
                                                rel.setClassFrom(search);
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassTo(unnamedClassName);
                                                cls.relationList.add(rel);
                                            }else{

                                                UMLClass rightClass = new UMLClass(search, unnamedClassName, ++count);
                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                rightClass.relationList.add(rel);
                                                generator.classList.put(rightClass.name, rightClass);
                                            }
                                        }
                                        else{

                                            UMLRelation rel = new UMLRelation();
                                            rel.setClassFrom(search);
                                            rel.setRelationType(RelationshipType.GENERALIZATION);
                                            rel.setClassTo(unnamedClassName);
                                            generator.classList.get(search).relationList.add(rel);
                                        }

                                        // Assign type as new type
                                        type = unnamedClassName;
                                    }
                                }
                                else if(exp.exp instanceof Id){

                                    if(cls.attribList.get(exp.exp.toString()) != null){

                                        type = cls.attribList.get(exp.exp.toString()).type;
                                    }else{

                                        type = exp.exp.toString();
                                    }

                                }
                            }else if(declaration.expr instanceof Id){

                                if(cls.attribList.get(declaration.expr.toString()) != null){

                                    type = cls.attribList.get(declaration.expr.toString()).type;
                                }else{

                                    type = declaration.expr.toString();
                                }
                            }else if(declaration.expr instanceof BinaryExp){

                                BinaryExp expr = (BinaryExp) declaration.expr;

                                if(expr.op instanceof ArrowOpExp){

                                    ArrowOpExp arrowOpExp = (ArrowOpExp) expr.op;
                                    // Associate the two classes of the arrow op first

                                    String leftClass = expr.left.toString();

                                    if(!generator.classList.containsKey(leftClass)){

                                        if(cls.attribList.containsKey(leftClass)){

                                            leftClass = cls.attribList.get(leftClass).type;
                                        }
                                    }

                                    if(expr.right instanceof BinaryExp){

                                        if(((BinaryExp) expr.right).op instanceof Plus || ((BinaryExp) expr.right).op instanceof And){

                                            BinaryExp exp = ((BinaryExp) expr.right);
                                            // Create a new abstract class
                                            // such that the two classes extends
                                            // the class of their union
                                            // name that class as NewClass#
                                            // maintain the number;
                                            String unnamedClassName = ((BinaryExp) exp).left.toString() + "Or"+ ((BinaryExp) exp).right.toString();
                                            UMLClass unnamedClass = new UMLClass(unnamedClassName, "", ++count);
                                            unnamedClass.setAbstract();
                                            generator.classList.put(unnamedClassName, unnamedClass);

                                            // Create two relations for left and right class
                                            // Check if the class exits

                                            String search = "";
                                            if(cls.attribList.get(((BinaryExp) exp).left.toString()) != null){

                                                search = cls.attribList.get(((BinaryExp) exp).left.toString()).type;
                                            }
                                            else{

                                                search = ((BinaryExp) exp).left.toString();
                                            }
                                            if(!generator.classList.containsKey(search)){

                                                // Create the class
                                                if(search.equals(cls.name)){

                                                    UMLRelation rel = new UMLRelation();
                                                    rel.setClassFrom(search);
                                                    rel.setRelationType(RelationshipType.GENERALIZATION);
                                                    rel.setClassTo(unnamedClassName);
                                                    cls.relationList.add(rel);
                                                }
                                                else{

                                                    UMLClass Class = new UMLClass(search, unnamedClassName, ++count);
                                                    UMLRelation rel = new UMLRelation();
                                                    rel.setRelationType(RelationshipType.GENERALIZATION);
                                                    rel.setClassFrom(search);
                                                    rel.setClassTo(unnamedClassName);
                                                    Class.relationList.add(rel);
                                                    generator.classList.put(Class.name, Class);
                                                }
                                            }
                                            else{

                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                generator.classList.get(search).relationList.add(rel);
                                            }
                                            // Check if the class exits

                                            search = "";
                                            if(cls.attribList.get(((BinaryExp) exp).right.toString()) != null){

                                                search = cls.attribList.get(((BinaryExp) exp).right.toString()).type;
                                            }
                                            else{

                                                search = ((BinaryExp) exp).right.toString();
                                            }
                                            if(!generator.classList.containsKey(search)){

                                                // Create the class
                                                if(search.equals(cls.name)){

                                                    UMLRelation rel = new UMLRelation();
                                                    rel.setClassFrom(search);
                                                    rel.setRelationType(RelationshipType.GENERALIZATION);
                                                    rel.setClassTo(unnamedClassName);
                                                    cls.relationList.add(rel);
                                                }
                                                else{

                                                    UMLClass Class = new UMLClass(search, unnamedClassName, ++count);
                                                    UMLRelation rel = new UMLRelation();
                                                    rel.setRelationType(RelationshipType.GENERALIZATION);
                                                    rel.setClassFrom(search);
                                                    rel.setClassTo(unnamedClassName);
                                                    Class.relationList.add(rel);
                                                    generator.classList.put(Class.name, Class);
                                                }
                                            }
                                            else{

                                                UMLRelation rel = new UMLRelation();
                                                rel.setClassFrom(search);
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassTo(unnamedClassName);
                                                generator.classList.get(search).relationList.add(rel);
                                            }

                                            // Assign type as new type
                                            type = unnamedClassName;
                                        }
                                    }
                                    else if(expr.right instanceof Id){

                                        if(cls.attribList.get(expr.right.toString()) != null){

                                            type = cls.attribList.get(expr.right.toString()).type;
                                        }else{

                                            type = expr.right.toString();
                                        }
                                    }

                                    String rightClass = type;

                                    // Check if the classes exist
                                    if(!generator.classList.containsKey(leftClass)){

                                        UMLClass Class = new UMLClass(leftClass, " ", ++count);
                                        UMLRelation rel = new UMLRelation();
                                        rel.setRelationType(RelationshipType.ASSOCIATION);
                                        rel.setClassFrom(leftClass);
                                        rel.setClassTo(rightClass);
                                        if(arrowOpExp.right instanceof Lone){

                                            rel.setCardinality(Arity.ZERO_OR_ONE);
                                        }
                                        else if(arrowOpExp.right instanceof Some){

                                            rel.setCardinality(Arity.ONE_OR_MANY);
                                        }
                                        else if(arrowOpExp.right instanceof One){

                                            rel.setCardinality(Arity.ONE);
                                        }
                                        Class.relationList.add(rel);
                                        generator.classList.put(Class.name, Class);
                                    }else{
                                        UMLRelation rel = new UMLRelation();
                                        rel.setRelationType(RelationshipType.ASSOCIATION);
                                        rel.setClassFrom(leftClass);
                                        rel.setClassTo(rightClass);
                                        if(arrowOpExp.right instanceof Lone){

                                            rel.setCardinality(Arity.ZERO_OR_ONE);
                                        }
                                        else if(arrowOpExp.right instanceof Some){

                                            rel.setCardinality(Arity.ONE_OR_MANY);
                                        }
                                        else if(arrowOpExp.right instanceof One){

                                            rel.setCardinality(Arity.ONE);
                                        }
                                        generator.classList.get(leftClass).relationList.add(rel);
                                    }

                                    // Associate the two classes in Arrow op to the third class

                                    relation.setClassTo(leftClass);
                                    relation.setClassTo(rightClass);
                                    relation.setRelationType(RelationshipType.DEPENDENCY);

                                    cls.relationList.add(relation);
                                }else {

                                    if(expr.right instanceof BinaryExp){

                                        if(((BinaryExp) expr.right).op instanceof Plus || ((BinaryExp) expr.right).op instanceof And){

                                            BinaryExp exp = ((BinaryExp) expr.right);
                                            // Create a new abstract class
                                            // such that the two classes extends
                                            // the class of their union
                                            // name that class as NewClass#
                                            // maintain the number;
                                            String unnamedClassName = ((BinaryExp) exp).left.toString() + "Or"+ ((BinaryExp) exp).right.toString();
                                            UMLClass unnamedClass = new UMLClass(unnamedClassName, "", ++count);
                                            unnamedClass.setAbstract();
                                            generator.classList.put(unnamedClassName, unnamedClass);

                                            // Create two relations for left and right class
                                            // Check if the class exits

                                            String search = "";
                                            if(cls.attribList.get(((BinaryExp) exp).left.toString()) != null){

                                                search = cls.attribList.get(((BinaryExp) exp).left.toString()).type;
                                            }
                                            else{

                                                search = ((BinaryExp) exp).left.toString();
                                            }
                                            if(!generator.classList.containsKey(search)){

                                                // Create the class
                                                if(search.equals(cls.name)){

                                                    UMLRelation rel = new UMLRelation();
                                                    rel.setClassFrom(search);
                                                    rel.setRelationType(RelationshipType.GENERALIZATION);
                                                    rel.setClassTo(unnamedClassName);
                                                    cls.relationList.add(rel);
                                                }
                                                else{

                                                    UMLClass leftClass = new UMLClass(search, unnamedClassName, ++count);
                                                    UMLRelation rel = new UMLRelation();
                                                    rel.setRelationType(RelationshipType.GENERALIZATION);
                                                    rel.setClassFrom(search);
                                                    rel.setClassTo(unnamedClassName);
                                                    leftClass.relationList.add(rel);
                                                    generator.classList.put(leftClass.name, leftClass);
                                                }
                                            }
                                            else{

                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                generator.classList.get(search).relationList.add(rel);
                                            }
                                            // Check if the class exits

                                            search = "";
                                            if(cls.attribList.get(((BinaryExp) exp).right.toString()) != null){

                                                search = cls.attribList.get(((BinaryExp) exp).right.toString()).type;
                                            }
                                            else{

                                                search = ((BinaryExp) exp).right.toString();
                                            }
                                            if(!generator.classList.containsKey(search)){

                                                // Create the class
                                                if(search.equals(cls.name)){

                                                    UMLRelation rel = new UMLRelation();
                                                    rel.setClassFrom(search);
                                                    rel.setRelationType(RelationshipType.GENERALIZATION);
                                                    rel.setClassTo(unnamedClassName);
                                                    cls.relationList.add(rel);
                                                }
                                                else{

                                                    UMLClass rightClass = new UMLClass(search, unnamedClassName, ++count);
                                                    UMLRelation rel = new UMLRelation();
                                                    rel.setRelationType(RelationshipType.GENERALIZATION);
                                                    rel.setClassFrom(search);
                                                    rel.setClassTo(unnamedClassName);
                                                    rightClass.relationList.add(rel);
                                                    generator.classList.put(rightClass.name, rightClass);
                                                }
                                            }
                                            else{

                                                UMLRelation rel = new UMLRelation();
                                                rel.setClassFrom(search);
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassTo(unnamedClassName);
                                                generator.classList.get(search).relationList.add(rel);
                                            }

                                            // Assign type as new type
                                            type = unnamedClassName;
                                        }
                                    }
                                    else if(expr.right instanceof Id){

                                        if(cls.attribList.get(expr.right.toString()) != null){

                                            type = cls.attribList.get(expr.right.toString()).type;
                                        }else{

                                            type = expr.right.toString();
                                        }
                                    }
                                }
                            }

                            classTo = type;

                            if(declaration.expr instanceof BinaryExp){

                                BinaryExp exp = (BinaryExp) declaration.expr;

                                if(!(exp.op instanceof ArrowOpExp)){

                                    relation.setClassTo(classTo);

                                    cls.relationList.add(relation);
                                }
                            }else if(declaration.expr instanceof UnaryExp){

                                UnaryExp exp = (UnaryExp) declaration.expr;

                                relation.setClassTo(classTo);

                                cls.relationList.add(relation);
                            }

                            for(int n = 0; n < nameList.size(); n++){

                                UMLAttribute attribute = new UMLAttribute();
                                attribute.setName(nameList.get(n));
                                attribute.setType(type);

                                cls.attribList.put(nameList.get(n), attribute);
                            }
                        }

                        HashMap<String, String> PriorCondClassList = new HashMap<>();
                        HashMap<String, String> PriorCondClassArityList = new HashMap<>();
                        HashMap<String, String> PostCondClassList = new HashMap<>();
                        HashMap<String, String> PostCondClassArityList = new HashMap<>();

                        // Iterate through block
                        for(int b = 0; b < sig.blockList.size(); b++){

                            if(sig.blockList.get(b) instanceof UnaryExp){

                                UnaryExp ue = (UnaryExp) ((UnaryExp) sig.blockList.get(b)).op;

                                if(ue instanceof UnaryExp){

                                    if(ue.exp instanceof Decl){

                                        Decl decl = (Decl) ue.exp;

                                        if(decl.expr instanceof Id){

                                            PriorCondClassList.put(decl.name.toString(), decl.expr.toString());
                                        }

                                        PriorCondClassArityList.put(decl.name.toString(), ((UnaryExp)ue).op.toString() );
                                    }
                                }

                                Exp bb = (BlockOrBar) ((UnaryExp) sig.blockList.get(b)).exp;

                                while(bb instanceof BlockOrBar){

                                    if(((BlockOrBar)bb).expr instanceof UnaryExp){

                                        ue = (UnaryExp) ((BlockOrBar)bb).expr;

                                        if(ue.exp instanceof BinaryExp){

                                            BinaryExp be = (BinaryExp) ue.exp;

                                            if(be.op instanceof Dot){

                                                if(be.right instanceof Id){

                                                    UMLAttribute attrib = generator.classList.get(PriorCondClassList.get(be.left.toString())).attribList.get(be.right.toString());
                                                    if(attrib != null)
                                                        PostCondClassList.put(be.left.toString(), attrib.type);
                                                }
                                                else if(be.right instanceof UnaryExp){

                                                    if(((UnaryExp)be.right).exp instanceof Id){

                                                        UMLAttribute attrib = generator.classList.get(PriorCondClassList.get(be.left.toString())).attribList.get(((UnaryExp)be.right).exp.toString());
                                                        if(attrib != null){

                                                            PostCondClassList.put(be.left.toString(), attrib.type);
                                                        }
                                                    }
                                                }
                                            }

                                            PostCondClassArityList.put(be.left.toString(), ue.op.toString());

                                            bb = ue.exp;
                                        }
                                        else if(ue.exp instanceof BlockOrBar){

                                            if(ue.op instanceof UnaryExp){

                                                UnaryExp internal_ue = (UnaryExp) ue.op;

                                                if(internal_ue.exp instanceof Decl){

                                                    Decl decl = (Decl) internal_ue.exp;

                                                    if(decl.expr instanceof Id){

                                                        PriorCondClassList.put(decl.name.toString(), decl.expr.toString());
                                                    }

                                                    PriorCondClassArityList.put(decl.name.toString(), ((UnaryExp)internal_ue).op.toString() );
                                                }
                                            }

                                            bb = ue.exp;
                                        }
                                    }
                                    else if(((BlockOrBar) bb).expr instanceof BinaryExp){

                                        Exp be = (BinaryExp) ((BinaryExp)((BlockOrBar) bb).expr).right;
                                        String var = "";

                                        while (be instanceof BinaryExp){

                                            var = ((BinaryExp)be).left.toString();
                                            be = ((BinaryExp)be).right;
                                        }

                                        if(be instanceof UnaryExp){

                                            if (((UnaryExp) be).exp instanceof Id) {

                                                UMLAttribute attrib = generator.classList.get(PriorCondClassList.get(var)).attribList.get(((UnaryExp)be).exp.toString());
                                                if(attrib != null){

                                                    PostCondClassList.put(var, attrib.type);
                                                }
                                            }
                                        }

                                        PostCondClassArityList.put(var, ((BinaryExp)((BlockOrBar) bb).expr).op.toString());

                                        if(be instanceof UnaryExp){

                                            bb = ((UnaryExp) be).exp;
                                        }
                                        else{

                                            bb = be;
                                        }
                                    }
                                }
                            }
                        }

                        for(Map.Entry element: PriorCondClassList.entrySet()){

                            String key = (String)element.getKey();

                            if(!(PostCondClassList.containsKey(key) && PostCondClassArityList.containsKey(key))){

                                break;
                            }

                            UMLRelation rel = new UMLRelation();

                            rel.setClassFrom(PriorCondClassList.get(key));
                            String cardinality = PostCondClassArityList.get(key);
                            if(cardinality.equals("lone")){

                                rel.setCardinality(Arity.ZERO_OR_ONE);
                            }
                            else if(cardinality.equals("some")){

                                rel.setCardinality(Arity.ONE_OR_MANY);
                            }
                            else if(cardinality.equals("one")){

                                rel.setCardinality(Arity.ONE);
                            }
                            rel.setClassTo(PostCondClassList.get(key));

                            if(generator.classList.containsKey(PriorCondClassList.get(key))){

                                generator.classList.get(PriorCondClassList.get(key)).relationList.add(rel);
                            }
                            else if(PriorCondClassList.get(key).equals(cls.name)){

                                cls.relationList.add(rel);
                            }
                            // next

                            rel = new UMLRelation();

                            rel.setClassFrom(PostCondClassList.get(key));
                            cardinality = PriorCondClassArityList.get(key);
                            if(cardinality.equals("lone")){

                                rel.setCardinality(Arity.ZERO_OR_ONE);
                            }
                            else if(cardinality.equals("some")){

                                rel.setCardinality(Arity.ONE_OR_MANY);
                            }
                            else if(cardinality.equals("one")){

                                rel.setCardinality(Arity.ONE);
                            }
                            else if(cardinality.equals("in")){

                                rel.setRelationType(RelationshipType.AGGREGATION);
                            }
                            rel.setClassTo(PriorCondClassList.get(key));

                            if(generator.classList.containsKey(PostCondClassList.get(key))){

                                generator.classList.get(PostCondClassList.get(key)).relationList.add(rel);
                            }
                            else if(PostCondClassList.get(key).equals(cls.name)){

                                cls.relationList.add(rel);
                            }
                        }

                        generator.classList.put(clsName, cls);
                    }
                    else{   // If class already existing

                        // check if parent class
                        if(sig.parentClass != null) {

                            generator.classList.get(sigList.get(l)).setParentClass(sig.parentClass.toString());

                            boolean found = false;

                            Iterator<UMLRelation> it =  generator.classList.get(sigList.get(l)).relationList.iterator();
                            while(it.hasNext()){

                                if(it.next().classTo.equals(sig.parentClass.toString())){

                                    found = true;
                                    break;
                                }
                            }

                            if(!found){

                                UMLRelation relation = new UMLRelation();

                                relation.setClassFrom(sigList.get(l));
                                relation.setClassTo(sig.parentClass.toString());
                                if(sig.extension)
                                    relation.setRelationType(RelationshipType.GENERALIZATION);
                                generator.classList.get(sigList.get(l)).relationList.add(relation);
                            }
                        }

                        if(sig.wholeClass != null) {

                            generator.classList.get(sigList.get(l)).setParentClass(sig.wholeClass.toString());

                            boolean found = false;

                            Iterator<UMLRelation> it =  generator.classList.get(sigList.get(l)).relationList.iterator();
                            while(it.hasNext()){

                                if(it.next().classTo.equals(sig.wholeClass.toString())){

                                    found = true;
                                    break;
                                }
                            }

                            if(!found){

                                UMLRelation relation = new UMLRelation();

                                relation.setClassFrom(sigList.get(l));
                                relation.setClassTo(sig.wholeClass.toString());
                                if(sig.composition)
                                    relation.setRelationType(RelationshipType.AGGREGATION);
                                generator.classList.get(sigList.get(l)).relationList.add(relation);
                            }
                        }

                        // check if abstract
                        for(int j = 0; j < sig.qualifierList.size(); j++){

                            if(sig.qualifierList.get(j) instanceof Abstract)
                            {

                                generator.classList.get(sigList.get(l)).setAbstract();
                            }
                        }

                        // iterate through body
                        for(int k = 0; k < sig.bodyList.size(); k++){

                            Decl declaration = (Decl) sig.bodyList.get(k);

                            Vector<String> nameList = new Vector<>();

                            x = declaration.name.toString();
                            x = x.replace("(","");
                            x = x.replace(")","");

                            if(x.contains(" ")){

                                // split the classes and process each class

                                String[] arr = x.split(" ");

                                for(String a: arr){

                                    nameList.add(a);
                                }
                            }else{

                                nameList.add(x);
                            }

                            String type = "";
                            String classFrom = generator.classList.get(sigList.get(l)).name;
                            String classTo = "";

                            UMLRelation relation = new UMLRelation();
                            relation.setClassFrom(classFrom);

                            // if exp is of type unary expression
                            if(declaration.expr instanceof UnaryExp){

                                UnaryExp exp = (UnaryExp) declaration.expr;

                                if(exp.op instanceof Lone){

                                    relation.setCardinality(Arity.ZERO_OR_ONE);
                                }
                                else if(exp.op instanceof Some){

                                    relation.setCardinality(Arity.ONE_OR_MANY);
                                }
                                else if(exp.op instanceof One){

                                    relation.setCardinality(Arity.ONE);
                                }

                                if(exp.exp instanceof BinaryExp){

                                    if(((BinaryExp) exp.exp).op instanceof Plus || ((BinaryExp) exp.exp).op instanceof And){

                                        // Create a new abstract class
                                        // such that the two classes extends
                                        // the class of their union
                                        // name that class as NewClass#
                                        // maintain the number;
                                        String unnamedClassName = ((BinaryExp) exp.exp).left.toString() + "Or" + ((BinaryExp) exp.exp).right.toString();
                                        UMLClass unnamedClass = new UMLClass(unnamedClassName, "", ++count);
                                        unnamedClass.setAbstract();
                                        generator.classList.put(unnamedClassName, unnamedClass);

                                        // Create two relations for left and right class
                                        // Check if the class exits

                                        String search = "";
                                        if(generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp.exp).left.toString()) != null){

                                            search = generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp.exp).left.toString()).type;
                                        }
                                        else{

                                            search = ((BinaryExp) exp.exp).left.toString();
                                        }
                                        if(!generator.classList.containsKey(search)){

                                            // Create the class
                                            UMLClass leftClass = new UMLClass(search, unnamedClassName, ++count);
                                            UMLRelation rel = new UMLRelation();
                                            rel.setRelationType(RelationshipType.GENERALIZATION);
                                            rel.setClassFrom(search);
                                            rel.setClassTo(unnamedClassName);
                                            leftClass.relationList.add(rel);
                                            generator.classList.put(leftClass.name, leftClass);
                                        }
                                        else{

                                            UMLRelation rel = new UMLRelation();
                                            rel.setRelationType(RelationshipType.GENERALIZATION);
                                            rel.setClassFrom(search);
                                            rel.setClassTo(unnamedClassName);
                                            generator.classList.get(search).relationList.add(rel);
                                        }
                                        // Check if the class exits

                                        search = "";
                                        if(generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp.exp).right.toString()) != null){

                                            search = generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp.exp).right.toString()).type;
                                        }
                                        else{

                                            search = ((BinaryExp) exp.exp).right.toString();
                                        }
                                        if(!generator.classList.containsKey(search)){

                                            // Create the class
                                            UMLClass rightClass = new UMLClass(search, unnamedClassName, ++count);
                                            UMLRelation rel = new UMLRelation();
                                            rel.setRelationType(RelationshipType.GENERALIZATION);
                                            rel.setClassFrom(search);
                                            rel.setClassTo(unnamedClassName);
                                            rightClass.relationList.add(rel);
                                            generator.classList.put(rightClass.name, rightClass);
                                        }
                                        else{

                                            UMLRelation rel = new UMLRelation();
                                            rel.setRelationType(RelationshipType.GENERALIZATION);
                                            rel.setClassFrom(search);
                                            rel.setClassTo(unnamedClassName);
                                            generator.classList.get(search).relationList.add(rel);
                                        }

                                        // Assign type as new type
                                        type = unnamedClassName;
                                    }
                                }
                                else if(exp.exp instanceof Id){

                                    if(generator.classList.get(sigList.get(l)).attribList.get(exp.exp.toString()) != null){

                                        type = generator.classList.get(sigList.get(l)).attribList.get(exp.exp.toString()).type;
                                    }else{

                                        type = exp.exp.toString();
                                    }
                                }
                            }else if(declaration.expr instanceof Id){

                                if(generator.classList.get(sigList.get(l)).attribList.get(declaration.expr.toString()) != null){

                                    type = generator.classList.get(sigList.get(l)).attribList.get(declaration.expr.toString()).type;
                                }else{

                                    type = declaration.expr.toString();
                                }
                            }
                            else if(declaration.expr instanceof BinaryExp){

                                BinaryExp expr = (BinaryExp) declaration.expr;

                                if(expr.op instanceof ArrowOpExp){

                                    ArrowOpExp arrowOpExp = (ArrowOpExp) expr.op;
                                    // Associate the two classes of the arrow op first

                                    String leftClass = expr.left.toString();

                                    if(!generator.classList.containsKey(leftClass)){

                                        if(generator.classList.get(sigList.get(l)).attribList.containsKey(leftClass)){

                                            leftClass = generator.classList.get(sigList.get(l)).attribList.get(leftClass).type;
                                        }
                                    }

                                    if(expr.right instanceof BinaryExp){

                                        BinaryExp exp = (BinaryExp) expr.right;

                                        if(((BinaryExp) exp).op instanceof Plus || ((BinaryExp) exp).op instanceof And){

                                            // Create a new abstract class
                                            // such that the two classes extends
                                            // the class of their union
                                            // name that class as NewClass#
                                            // maintain the number;
                                            String unnamedClassName = ((BinaryExp) exp).left.toString() + "Or" + ((BinaryExp) exp).right.toString();
                                            UMLClass unnamedClass = new UMLClass(unnamedClassName, "", ++count);
                                            unnamedClass.setAbstract();
                                            generator.classList.put(unnamedClassName, unnamedClass);

                                            // Create two relations for left and right class
                                            // Check if the class exits

                                            String search = "";
                                            if(generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp).left.toString()) != null){

                                                search = generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp).left.toString()).type;
                                            }
                                            else{

                                                search = ((BinaryExp) exp).left.toString();
                                            }
                                            if(!generator.classList.containsKey(search)){

                                                // Create the class
                                                UMLClass Class = new UMLClass(search, unnamedClassName, ++count);
                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                Class.relationList.add(rel);
                                                generator.classList.put(Class.name, Class);
                                            }
                                            else{

                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                generator.classList.get(search).relationList.add(rel);
                                            }
                                            // Check if the class exits
                                            search = "";
                                            if(generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp).right.toString()) != null){

                                                search = generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp).right.toString()).type;
                                            }
                                            else{

                                                search = ((BinaryExp) exp).right.toString();
                                            }
                                            if(!generator.classList.containsKey(search)){

                                                // Create the class
                                                UMLClass Class = new UMLClass(search, unnamedClassName, ++count);
                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                Class.relationList.add(rel);
                                                generator.classList.put(Class.name, Class);
                                            }
                                            else{

                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                generator.classList.get(search).relationList.add(rel);
                                            }
                                            // Assign type as new type
                                            type = unnamedClassName;
                                        }
                                    }
                                    else if(expr.right instanceof Id){

                                        if(generator.classList.get(sigList.get(l)).attribList.get(expr.right.toString()) != null){

                                            type = generator.classList.get(sigList.get(l)).attribList.get(expr.right.toString()).type;
                                        }else{

                                            type = expr.right.toString();
                                        }
                                    }

                                    String rightClass = type;

                                    // Check if the classes exist
                                    if(!generator.classList.containsKey(leftClass)){

                                        UMLClass Class = new UMLClass(leftClass, " ", ++count);
                                        UMLRelation rel = new UMLRelation();
                                        rel.setRelationType(RelationshipType.ASSOCIATION);
                                        rel.setClassFrom(leftClass);
                                        rel.setClassTo(rightClass);
                                        if(arrowOpExp.right instanceof Lone){

                                            rel.setCardinality(Arity.ZERO_OR_ONE);
                                        }
                                        else if(arrowOpExp.right instanceof Some){

                                            rel.setCardinality(Arity.ONE_OR_MANY);
                                        }
                                        else if(arrowOpExp.right instanceof One){

                                            rel.setCardinality(Arity.ONE);
                                        }
                                        Class.relationList.add(rel);
                                        generator.classList.put(Class.name, Class);
                                    }else{
                                        UMLRelation rel = new UMLRelation();
                                        rel.setRelationType(RelationshipType.ASSOCIATION);
                                        rel.setClassFrom(leftClass);
                                        rel.setClassTo(rightClass);
                                        if(arrowOpExp.right instanceof Lone){

                                            rel.setCardinality(Arity.ZERO_OR_ONE);
                                        }
                                        else if(arrowOpExp.right instanceof Some){

                                            rel.setCardinality(Arity.ONE_OR_MANY);
                                        }
                                        else if(arrowOpExp.right instanceof One){

                                            rel.setCardinality(Arity.ONE);
                                        }
                                        generator.classList.get(leftClass).relationList.add(rel);
                                    }

                                    // Associate the two classes in Arrow op to the third class

                                    relation.setClassTo(leftClass);
                                    relation.setClassTo(rightClass);
                                    relation.setRelationType(RelationshipType.DEPENDENCY);

                                    generator.classList.get(sigList.get(l)).relationList.add(relation);
                                }
                                else{

                                    if(expr.right instanceof BinaryExp){

                                        BinaryExp exp = (BinaryExp) expr.right;

                                        if(((BinaryExp) exp).op instanceof Plus || ((BinaryExp) exp).op instanceof And){

                                            // Create a new abstract class
                                            // such that the two classes extends
                                            // the class of their union
                                            // name that class as NewClass#
                                            // maintain the number;
                                            String unnamedClassName = ((BinaryExp) exp).left.toString() + "Or" + ((BinaryExp) exp).right.toString();
                                            UMLClass unnamedClass = new UMLClass(unnamedClassName, "", ++count);
                                            unnamedClass.setAbstract();
                                            generator.classList.put(unnamedClassName, unnamedClass);

                                            // Create two relations for left and right class
                                            // Check if the class exits

                                            String search = "";
                                            if(generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp).left.toString()) != null){

                                                search = generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp).left.toString()).type;
                                            }
                                            else{

                                                search = ((BinaryExp) exp).left.toString();
                                            }
                                            if(!generator.classList.containsKey(search)){

                                                // Create the class
                                                UMLClass leftClass = new UMLClass(search, unnamedClassName, ++count);
                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                leftClass.relationList.add(rel);
                                                generator.classList.put(leftClass.name, leftClass);
                                            }
                                            else{

                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                generator.classList.get(search).relationList.add(rel);
                                            }
                                            // Check if the class exits
                                            search = "";
                                            if(generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp).right.toString()) != null){

                                                search = generator.classList.get(sigList.get(l)).attribList.get(((BinaryExp) exp).right.toString()).type;
                                            }
                                            else{

                                                search = ((BinaryExp) exp).right.toString();
                                            }
                                            if(!generator.classList.containsKey(search)){

                                                // Create the class
                                                UMLClass rightClass = new UMLClass(search, unnamedClassName, ++count);
                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                rightClass.relationList.add(rel);
                                                generator.classList.put(rightClass.name, rightClass);
                                            }
                                            else{

                                                UMLRelation rel = new UMLRelation();
                                                rel.setRelationType(RelationshipType.GENERALIZATION);
                                                rel.setClassFrom(search);
                                                rel.setClassTo(unnamedClassName);
                                                generator.classList.get(search).relationList.add(rel);
                                            }
                                            // Assign type as new type
                                            type = unnamedClassName;
                                        }
                                    }
                                    else if(expr.right instanceof Id){

                                        if(generator.classList.get(sigList.get(l)).attribList.get(expr.right.toString()) != null){

                                            type = generator.classList.get(sigList.get(l)).attribList.get(expr.right.toString()).type;
                                        }else{

                                            type = expr.right.toString();
                                        }
                                    }
                                }
                            }

                            classTo = type;

                            if(declaration.expr instanceof BinaryExp){

                                BinaryExp exp = (BinaryExp) declaration.expr;

                                if(!(exp.op instanceof ArrowOpExp)){

                                    relation.setClassTo(classTo);

                                    generator.classList.get(sigList.get(l)).relationList.add(relation);
                                }
                            }
                            else if(declaration.expr instanceof UnaryExp){

                                UnaryExp exp = (UnaryExp) declaration.expr;

                                relation.setClassTo(classTo);

                                generator.classList.get(sigList.get(l)).relationList.add(relation);
                            }

                            for(int n = 0; n < nameList.size(); n++){

                                UMLAttribute attribute = new UMLAttribute();
                                attribute.setName(nameList.get(n));
                                attribute.setType(type);
                                generator.classList.get(sigList.get(l)).attribList.put(nameList.get(n), attribute);
                            }
                        }

                        HashMap<String, String> PriorCondClassList = new HashMap<>();
                        HashMap<String, String> PriorCondClassArityList = new HashMap<>();
                        HashMap<String, String> PostCondClassList = new HashMap<>();
                        HashMap<String, String> PostCondClassArityList = new HashMap<>();

                        // Iterate through block
                        for(int b = 0; b < sig.blockList.size(); b++){

                            if(sig.blockList.get(b) instanceof UnaryExp){

                                UnaryExp ue = (UnaryExp) ((UnaryExp) sig.blockList.get(b)).op;

                                if(ue instanceof UnaryExp){

                                    if(ue.exp instanceof Decl){

                                        Decl decl = (Decl) ue.exp;

                                        if(decl.expr instanceof Id){

                                            PriorCondClassList.put(decl.name.toString(), decl.expr.toString());
                                        }

                                        PriorCondClassArityList.put(decl.name.toString(), ((UnaryExp)ue).op.toString() );
                                    }
                                }

                                Exp bb = (BlockOrBar) ((UnaryExp) sig.blockList.get(b)).exp;

                                while(bb instanceof BlockOrBar){

                                    if(((BlockOrBar)bb).expr instanceof UnaryExp){

                                        ue = (UnaryExp) ((BlockOrBar)bb).expr;

                                        if(ue.exp instanceof BinaryExp){

                                            BinaryExp be = (BinaryExp) ue.exp;

                                            if(be.op instanceof Dot){

                                                if(be.right instanceof Id){

                                                    UMLAttribute attrib = generator.classList.get(PriorCondClassList.get(be.left.toString())).attribList.get(be.right.toString());
                                                    if(attrib != null)
                                                        PostCondClassList.put(be.left.toString(), attrib.type);
                                                }
                                                else if(be.right instanceof UnaryExp){

                                                    if(((UnaryExp)be.right).exp instanceof Id){

                                                        UMLAttribute attrib = generator.classList.get(PriorCondClassList.get(be.left.toString())).attribList.get(((UnaryExp)be.right).exp.toString());
                                                        if(attrib != null){

                                                            PostCondClassList.put(be.left.toString(), attrib.type);
                                                        }
                                                    }
                                                }
                                            }

                                            PostCondClassArityList.put(be.left.toString(), ue.op.toString());

                                            bb = ue.exp;
                                        }
                                        else if(ue.exp instanceof BlockOrBar){

                                            if(ue.op instanceof UnaryExp){

                                                UnaryExp internal_ue = (UnaryExp) ue.op;

                                                if(internal_ue.exp instanceof Decl){

                                                    Decl decl = (Decl) internal_ue.exp;

                                                    if(decl.expr instanceof Id){

                                                        PriorCondClassList.put(decl.name.toString(), decl.expr.toString());
                                                    }

                                                    PriorCondClassArityList.put(decl.name.toString(), ((UnaryExp)internal_ue).op.toString() );
                                                }
                                            }

                                            bb = ue.exp;
                                        }
                                    }
                                    else if(((BlockOrBar) bb).expr instanceof BinaryExp){

                                        Exp be = (BinaryExp) ((BinaryExp)((BlockOrBar) bb).expr).right;
                                        String var = "";

                                        while (be instanceof BinaryExp){

                                            var = ((BinaryExp)be).left.toString();
                                            be = ((BinaryExp)be).right;
                                        }

                                        if(be instanceof UnaryExp){

                                            if (((UnaryExp) be).exp instanceof Id) {

                                                UMLAttribute attrib = generator.classList.get(PriorCondClassList.get(var)).attribList.get(((UnaryExp)be).exp.toString());
                                                if(attrib != null){

                                                    PostCondClassList.put(var, attrib.type);
                                                }
                                            }
                                        }

                                        PostCondClassArityList.put(var, ((BinaryExp)((BlockOrBar) bb).expr).op.toString());

                                        if(be instanceof UnaryExp){

                                            bb = ((UnaryExp) be).exp;
                                        }
                                        else{

                                            bb = be;
                                        }
                                    }
                                }
                            }
                        }

                        for(Map.Entry element: PriorCondClassList.entrySet()){

                            String key = (String)element.getKey();

                            if(!(PostCondClassList.containsKey(key) && PostCondClassArityList.containsKey(key))){

                                break;
                            }

                            UMLRelation rel = new UMLRelation();

                            rel.setClassFrom(PriorCondClassList.get(key));
                            String cardinality = PostCondClassArityList.get(key);
                            if(cardinality.equals("lone")){

                                rel.setCardinality(Arity.ZERO_OR_ONE);
                            }
                            else if(cardinality.equals("some")){

                                rel.setCardinality(Arity.ONE_OR_MANY);
                            }
                            else if(cardinality.equals("one")){

                                rel.setCardinality(Arity.ONE);
                            }
                            rel.setClassTo(PostCondClassList.get(key));

                            if(generator.classList.containsKey(PriorCondClassList.get(key))){

                                generator.classList.get(PriorCondClassList.get(key)).relationList.add(rel);
                            }
                            else if(PriorCondClassList.get(key).equals(generator.classList.get(sigList.get(l)).name)){

                                generator.classList.get(sigList.get(l)).relationList.add(rel);
                            }
                            // next

                            rel = new UMLRelation();

                            rel.setClassFrom(PostCondClassList.get(key));
                            cardinality = PriorCondClassArityList.get(key);
                            if(cardinality.equals("lone")){

                                rel.setCardinality(Arity.ZERO_OR_ONE);
                            }
                            else if(cardinality.equals("some")){

                                rel.setCardinality(Arity.ONE_OR_MANY);
                            }
                            else if(cardinality.equals("one")){

                                rel.setCardinality(Arity.ONE);
                            }
                            else if(cardinality.equals("in")){

                                rel.setRelationType(RelationshipType.AGGREGATION);
                            }
                            rel.setClassTo(PriorCondClassList.get(key));

                            if(generator.classList.containsKey(PostCondClassList.get(key))){

                                generator.classList.get(PostCondClassList.get(key)).relationList.add(rel);
                            }
                            else if(PostCondClassList.get(key).equals(generator.classList.get(sigList.get(l)).name)){

                                generator.classList.get(sigList.get(l)).relationList.add(rel);
                            }
                        }
                    }
                }
            }
        }

        // second iterate through predicates and functions
        for(int i = 0; i < result.paragraph_list.size(); i++)
        {

            Exp p = result.paragraph_list.get(i);

            if(p instanceof Function){

                Function f = (Function) p;
                String ret_type = "";

                if(f.isPred){

                    ret_type = "boolean";
                }
                HashMap<String, String> ArgumentList = new HashMap<>();

                for(int j = 0; j < f.arguments.size(); j++){

                    Decl arg = (Decl) f.arguments.get(j);

                    if(arg.expr instanceof Id){

                        String name = arg.name.toString();

                        name = name.replace("(","");
                        name = name.replace(")","");

                        Vector<String> argList = new Vector<>();

                        if(name.contains(" ")){

                            // split the classes and process each class

                            String[] arr = name.split(" ");

                            for(String a: arr){

                                argList.add(a);
                            }
                        }else{

                            argList.add(name);
                        }

                        for(int k = 0; k < argList.size(); k++){

                            ArgumentList.put(argList.get(k), arg.expr.toString());
                        }
                    }
                }
                for(int j = 0; j < f.block.size(); j++){

                    Exp block_exp = f.block.get(j);

                    if(block_exp instanceof BinaryExp) {

                        BinaryExp exp = (BinaryExp) block_exp;

                        if(exp.op instanceof EqualSign && exp.left instanceof BinaryExp){

                            exp = (BinaryExp) exp.left;
                            if(exp.op instanceof Dot){

                                String var = exp.left.toString();
                                if(ArgumentList.containsKey(var)){

                                    generator.classList.get(ArgumentList.get(var)).functionList.add(new UMLFunction(f.name.toString(), ret_type, ArgumentList));
                                }

                            }
                        }else if(exp.op instanceof Dot){

                            String var = exp.left.toString();
                            if(ArgumentList.containsKey(var)){

                                generator.classList.get(ArgumentList.get(var)).functionList.add(new UMLFunction(f.name.toString(), ret_type, ArgumentList));
                            }
                        }else if(exp.op instanceof Inclusion){

                            String var = exp.left.toString();
                            if(ArgumentList.containsKey(var)){

                                generator.classList.get(ArgumentList.get(var)).functionList.add(new UMLFunction(f.name.toString(), ret_type, ArgumentList));
                            }
                        }
                    }
                    if(block_exp instanceof Decl){

                        Decl exp = (Decl) block_exp;

                        if(exp.expr instanceof BinaryExp){

                            BinaryExp binaryExp = (BinaryExp) exp.expr;
                            String var = binaryExp.left.toString();
                            if(ArgumentList.containsKey(var)){

                                generator.classList.get(ArgumentList.get(var)).functionList.add(new UMLFunction(f.name.toString(), ret_type, ArgumentList));
                            }
                        }
                    }
                }
            }
        }

        // write to a file in the format understood by plantUML

        try( java.io.BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))){

            StringBuilder sb = new StringBuilder();
            sb.append("@startuml\n");
            sb.append("scale 1024*768\n");
            int seq = 0;

            while(seq < generator.classList.size()){

                for(Map.Entry element: generator.classList.entrySet()) {

                    UMLClass umlClass = (UMLClass) element.getValue();

                    if(seq == umlClass.order){

                        for(UMLRelation rel : umlClass.relationList) {

                            // class to
                            if (rel.classTo.size() > 1) {

                                sb.append("(");
                                sb.append(rel.classTo.get(0));
                                sb.append(",");
                                sb.append(rel.classTo.get(1));
                                sb.append(")");
                            } else {

                                sb.append(rel.classTo.get(0));
                            }

                            // class to label
                            if (rel.cardinality == Arity.ONE) {

                                sb.append(" \"1\"");
                            } else if (rel.cardinality == Arity.ONE_OR_MANY) {

                                sb.append(" \"1..*\"");
                            } else if (rel.cardinality == Arity.ZERO_OR_ONE) {

                                sb.append(" \"0..1\"");
                            } else if (rel.cardinality == Arity.ZERO_OR_MANY) {

                                sb.append(" \"0..*\"");
                            }

                            // relation type
                            if (rel.relationType == RelationshipType.GENERALIZATION) {

                                sb.append(" <|-- ");
                            } else if (rel.relationType == RelationshipType.COMPOSITION) {

                                sb.append(" *-- ");
                            } else if (rel.relationType == RelationshipType.DEPENDENCY) {

                                sb.append(" .. ");
                            } else if (rel.relationType == RelationshipType.AGGREGATION) {

                                sb.append(" o-- ");
                            }else if (rel.relationType == RelationshipType.ASSOCIATION) {

                                if (umlClass.isAbstract) {

                                    sb.append(" <--  ");
                                } else {

                                    sb.append(" <-- ");
                                }
                            }

                            // class from
                            sb.append(rel.classFrom);

                            //next line
                            sb.append("\n");
                        }

                        seq++;
                        break;
                    }
                }
            }


            for(Map.Entry element: generator.classList.entrySet()) {

                UMLClass umlClass = (UMLClass) element.getValue();

                if(umlClass.isAbstract){

                    //class name
                    sb.append("abstract class "+umlClass.name);
                }
                else{

                    //class name
                    sb.append("class "+umlClass.name);
                }

                if(umlClass.attribList.size() != 0 || umlClass.functionList.size() != 0){

                    sb.append(" {\n");

                    for(Map.Entry attribElement: umlClass.attribList.entrySet()){

                        // ident
                        sb.append("\t");

                        UMLAttribute attribute = (UMLAttribute) attribElement.getValue();

                        sb.append("+"+attribute.name+": "+attribute.type);
                        sb.append("\n");
                    }

                    // partition between methods and fields
                    sb.append("\t__\n");

                    for(int i = 0; i < umlClass.functionList.size(); i++){

                        // ident
                        sb.append("\t");

                        UMLFunction function = umlClass.functionList.get(i);
                        sb.append("+");
                        if(function.return_type.equals("")){
                            sb.append("void");
                        }else{
                            sb.append(function.return_type);
                        }
                        sb.append(" "+function.name+"(");
                        for(Map.Entry argElement: function.ArgumentList.entrySet()){

                            sb.append(" ");
                            String arg = (String) argElement.getKey();
                            String type = (String) argElement.getValue();
                            sb.append(arg+":"+type);
                        }
                        sb.append(" )");
                        sb.append("\n");
                    }

                    sb.append("}\n");
                }
                else{
                    sb.append("\n");
                }
            }

            //end uml
            sb.append("@enduml");

            writer.write(sb.toString());
        }
        catch(IOException io){

            System.out.println(io.getMessage());
        }

        // run plantuml
        try
        {
            Runtime.getRuntime().exec("cmd /c start cmd.exe /K \" java -jar plantuml.jar -verbose output.txt\"");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}