/*
 * Copyright (c) 2019 Walied K. Yassen, All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package me.waliedyassen.runescript.compiler.ast.expr;

import lombok.Getter;
import me.waliedyassen.runescript.commons.document.Range;
import me.waliedyassen.runescript.compiler.ast.visitor.AstVisitor;

/**
 * Represents an AST command expression.
 *
 * @author Walied K. Yassen
 */
public final class AstCommand extends AstExpression {

    /**
     * The name of the command.
     */
    @Getter
    private final AstIdentifier name;

    /**
     * The arugments of the command.
     */
    @Getter
    private final AstExpression[] arguments;

    /**
     * Whether or not the command is alternative.
     */
    @Getter
    private final boolean alternative;

    /**
     * Constructs a new {@link AstExpression} type object instance.
     *
     * @param range
     *         the expression source code range.
     * @param name
     *         the command name.
     * @param arguments
     *         the command arguments.
     * @param alternative
     *         whether or not this command expression is alternative.
     */
    public AstCommand(Range range, AstIdentifier name, AstExpression[] arguments, boolean alternative) {
        super(range);
        this.name = name;
        this.arguments = arguments;
        this.alternative = alternative;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <E, S> E accept(AstVisitor<E, S> visitor) {
        return visitor.visit(this);
    }
}
