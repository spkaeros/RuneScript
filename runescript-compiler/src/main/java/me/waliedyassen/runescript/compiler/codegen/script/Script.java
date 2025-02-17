/*
 * Copyright (c) 2019 Walied K. Yassen, All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package me.waliedyassen.runescript.compiler.codegen.script;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.waliedyassen.runescript.compiler.codegen.block.Block;
import me.waliedyassen.runescript.compiler.codegen.block.Label;
import me.waliedyassen.runescript.compiler.codegen.local.Local;
import me.waliedyassen.runescript.compiler.codegen.sw.SwitchTable;
import me.waliedyassen.runescript.compiler.stack.StackType;

import java.util.*;

/**
 * Represents a single generated bytecode level script.
 *
 * @author Walied K. Yassen
 */
@RequiredArgsConstructor
public final class Script {

    /**
     * The full name of the script.
     */
    @Getter
    private final String name;

    /**
     * The blocks that are registered within our script.
     */
    @Getter
    private final Map<Label, Block> blocks;

    /**
     * A map of all the script parameters.
     */
    @Getter
    private final Map<StackType, List<Local>> parameters;

    /**
     * A map of all the script local variables.
     */
    @Getter
    private final Map<StackType, List<Local>> variables;

    /**
     * A list of all the script switch tables.
     */
    @Getter
    private final List<SwitchTable> switchTables;

    /**
     * Checks whether or not the specified labels are next to each other.
     *
     * @param first
     *         the first label to check if the second label is after.
     * @param second
     *         the second label to check if its after the first label.
     *
     * @return <code>true</code> if they are next to each other otherwise <code>false</code>.
     */
    public boolean isNextTo(Label first, Label second) {
        // TODO: Perhaps find a better way to do this? I am not sure if it requires changing the blocks field type.
        var check_next = false;
        for (var label : blocks.keySet()) {
            if (check_next) {
                return second == label;
            } else if (label == first) {
                check_next = true;
            }
        }
        return false;
    }
}
