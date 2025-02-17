/*
 * Copyright (c) 2019 Walied K. Yassen, All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package me.waliedyassen.runescript.compiler.ast.expr;

import lombok.Getter;
import lombok.Setter;
import me.waliedyassen.runescript.commons.document.Range;
import me.waliedyassen.runescript.compiler.ast.visitor.AstVisitor;
import me.waliedyassen.runescript.compiler.symbol.impl.ArrayInfo;

/**
 * Represents an array expression.
 *
 * @author Walied K. Yassen
 */
public final class AstArrayExpression extends AstExpression {

    /**
     * The name of the array.
     */
    @Getter
    private final AstIdentifier name;

    /**
     * The the element index in the array.
     */
    @Getter
    private final AstExpression index;

    /**
     * The array symbol information.
     */
    @Getter
    @Setter
    private ArrayInfo array;

    /**
     * Constructs a new {@link AstArrayExpression} type object instance.
     *
     * @param range
     *         the node source code range.
     * @param name
     *         the name of the array.
     * @param index
     *         the element index in the array.
     */
    public AstArrayExpression(Range range, AstIdentifier name, AstExpression index) {
        super(range);
        this.name = name;
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E, S> E accept(AstVisitor<E, S> visitor) {
        return visitor.visit(this);
    }
}
