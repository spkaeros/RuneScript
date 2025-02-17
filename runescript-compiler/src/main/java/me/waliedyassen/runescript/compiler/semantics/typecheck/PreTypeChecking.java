/*
 * Copyright (c) 2019 Walied K. Yassen, All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package me.waliedyassen.runescript.compiler.semantics.typecheck;

import lombok.RequiredArgsConstructor;
import me.waliedyassen.runescript.compiler.ast.AstAnnotation;
import me.waliedyassen.runescript.compiler.ast.AstParameter;
import me.waliedyassen.runescript.compiler.ast.AstScript;
import me.waliedyassen.runescript.compiler.ast.expr.AstArrayExpression;
import me.waliedyassen.runescript.compiler.ast.expr.AstGosub;
import me.waliedyassen.runescript.compiler.ast.expr.AstVariableExpression;
import me.waliedyassen.runescript.compiler.ast.stmt.*;
import me.waliedyassen.runescript.compiler.ast.visitor.AstTreeVisitor;
import me.waliedyassen.runescript.compiler.semantics.SemanticChecker;
import me.waliedyassen.runescript.compiler.semantics.SemanticError;
import me.waliedyassen.runescript.compiler.semantics.scope.Scope;
import me.waliedyassen.runescript.compiler.symbol.impl.script.Annotation;
import me.waliedyassen.runescript.compiler.symbol.impl.variable.VariableInfo;
import me.waliedyassen.runescript.compiler.symbol.SymbolTable;
import me.waliedyassen.runescript.compiler.type.Type;
import me.waliedyassen.runescript.compiler.type.primitive.PrimitiveType;
import me.waliedyassen.runescript.compiler.util.VariableScope;
import me.waliedyassen.runescript.compiler.util.trigger.TriggerProperties;
import me.waliedyassen.runescript.compiler.util.trigger.TriggerType;

import java.util.*;

/**
 * Contains all of the procedures and functions that will be applied right before we perform our type checking semantic
 * checks.
 *
 * @author Walied K. Yassen
 */
@RequiredArgsConstructor
public final class PreTypeChecking extends AstTreeVisitor {

    /**
     * The scopes stack.
     */
    private final Stack<Scope> scopes = new Stack<>();

    /**
     * The owner {@link SemanticChecker} object.
     */
    private final SemanticChecker checker;

    /**
     * The symbol table to register the declared scripts in.
     */
    private final SymbolTable symbolTable;

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstScript script) {
        // create the annotations list.
        Map<String, Annotation> annotations;
        if (script.getAnnotations().size() > 0) {
            annotations = new HashMap<>();
            for (var annotation : script.getAnnotations()) {
                var name = annotation.getName().getText().toLowerCase();
                if (annotations.containsKey(name)) {
                    checker.reportError(new SemanticError(annotation.getName(), String.format("The annotation %s is already defined for this script", name)));
                } else {
                    annotations.put(name, new Annotation(name, annotation.getValue().getValue()));
                }
            }
        } else {
            annotations = Collections.emptyMap();
        }
        // resolve the script trigger type.
        var triggerName = script.getTrigger();
        var trigger = TriggerType.forRepresentation(triggerName.getText());
        // check if the script trigger type is a valid trigger type, if not produce and error.
        if (trigger == null) {
            checker.reportError(new SemanticError(triggerName, String.format("%s cannot be resolved to a trigger", triggerName.getText())));
        } else {
            // check if the trigger returning support matches the definition.
            if (script.getType() != PrimitiveType.VOID && !trigger.hasProperty(TriggerProperties.RETURNING)) {
                checker.reportError(new SemanticError(triggerName, String.format("The trigger type '%s' does not allow return values", trigger.getRepresentation())));
            }
            // check if the script is already defined in the symbol table
            // and define it if it was not, or produce an error if it was a duplicate.
            var name = script.getName();
            if (symbolTable.lookupScript(trigger, name.getText()) != null) {
                checker.reportError(new SemanticError(name, String.format("The script '%s' is already defined", name.getText())));
            } else {
                symbolTable.defineScript(annotations, trigger, name.getText(), script.getType(), Arrays.stream(script.getParameters()).map(AstParameter::getType).toArray(Type[]::new));
            }
        }
        return super.visit(script);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstParameter parameter) {
        scopes.lastElement().declareLocalVariable(parameter.getName().getText(), parameter.getType());
        return super.visit(parameter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstVariableDeclaration declaration) {
        var name = declaration.getName();
        var variable = resolveVariable(VariableScope.LOCAL, name.getText());
        if (variable != null) {
            checker.reportError(new SemanticError(name, String.format("Duplicate local variable %s", name.getText())));
        } else {
            variable = scopes.lastElement().declareLocalVariable(name.getText(), declaration.getType());
            declaration.setVariable(variable);
        }
        return super.visit(declaration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstVariableInitializer variableInitializer) {
        var name = variableInitializer.getName();
        var variable = resolveVariable(variableInitializer.getScope(), name.getText());
        if (variable == null) {
            checker.reportError(new SemanticError(variableInitializer, String.format("%s cannot be resolved to a variable", name.getText())));
        } else {
            variableInitializer.setVariable(variable);
        }
        return super.visit(variableInitializer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstVariableExpression variableExpression) {
        var name = variableExpression.getName();
        var variable = resolveVariable(variableExpression.getScope(), name.getText());
        if (variable == null) {
            checker.reportError(new SemanticError(name, String.format("%s cannot be resolved to a variable", name.getText())));
        } else {
            variableExpression.setVariable(variable);
        }
        return super.visit(variableExpression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstArrayDeclaration declaration) {
        var name = declaration.getName();
        var array = scopes.lastElement().getArray(name.getText());
        if (array != null) {
            checker.reportError(new SemanticError(name, String.format("Duplicate array %s", name.getText())));
        } else {
            array = scopes.lastElement().declareArray(name.getText(), declaration.getType());
            declaration.setArray(array);
        }
        return super.visit(declaration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstArrayInitializer arrayInitializer) {
        var name = arrayInitializer.getName();
        var array = scopes.lastElement().getArray(name.getText());
        if (array == null) {
            checker.reportError(new SemanticError(name, String.format("%s cannot be resolved to an array", name.getText())));
        } else {
            arrayInitializer.setArray(array);
        }
        return super.visit(arrayInitializer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstArrayExpression arrayExpression) {
        var name = arrayExpression.getName();
        var array = scopes.lastElement().getArray(name.getText());
        if (array == null) {
            checker.reportError(new SemanticError(name, String.format("%s cannot be resolved to an array", name.getText())));
        } else {
            arrayExpression.setArray(array);
        }
        return super.visit(arrayExpression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enter(AstBlockStatement blockStatement) {
        var scope = scopes.lastElement().createChild();
        scopes.push(scope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exit(AstBlockStatement blockStatement) {
        scopes.pop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enter(AstScript script) {
        scopes.push(new Scope(null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exit(AstScript script) {
        scopes.pop();
    }

    /**
     * Attempts to resolve the variable with the given {@link VariableScope scope} and {@code name}.
     *
     * @param scope
     *         the variable scope to resolve.
     * @param name
     *         the variable name to resolve.
     *
     * @return the resolved {@link VariableInfo} if it was present otherwise {@code null}.
     */
    private VariableInfo resolveVariable(VariableScope scope, String name) {
        if (scope == VariableScope.LOCAL) {
            return scopes.lastElement().getLocalVariable(name);
        } else {
            return symbolTable.lookupVariable(name);
        }
    }
}
