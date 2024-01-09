package org.example;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ResultHandle;

public interface Checker {
    BranchResult doCheck(ResultHandle equalityCheck);
}
