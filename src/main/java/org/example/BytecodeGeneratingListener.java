package org.example;

import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import rock.Rockstar;
import rock.RockstarBaseListener;

public class BytecodeGeneratingListener extends RockstarBaseListener {

    private final MethodCreator main;

    public BytecodeGeneratingListener(MethodCreator main) {
        super();
        this.main = main;
    }

    @Override
    public void enterOutputStmt(Rockstar.OutputStmtContext ctx) {

        String text = ctx.expression()
                         .getText();
        // This should probably be done in antlr, but for now, manually strip out the quotes around literals
        text = text.replaceAll("\"", "");
        Gizmo.systemOutPrintln(main, main.load(text));
    }
}