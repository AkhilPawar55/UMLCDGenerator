# UMLCDGenerator

This project generates UML class diagram for Alloy modeling language.
Under the hood I have used JavaCC a parser generator to generate a parser automatically for me, given the grammar and reproduction rules for the language.
Once the parser parses through the code, the AST is used to generate a UML class diagram using the conversion rules to transform language constructs to 
UML class diagram elements.

I have used PlantUML scripts to generate textual representation of UML class diagrams. Once the scripts are ready we can run them using Plant UML program
to render the UML class diagrams for the input Alloy code.
