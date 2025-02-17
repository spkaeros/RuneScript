/*
 * Copyright (c) 2019 Walied K. Yassen, All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package me.waliedyassen.runescript.compiler.codegen.writer.bytecode;

import me.waliedyassen.runescript.compiler.codegen.block.Block;
import me.waliedyassen.runescript.compiler.codegen.block.Label;
import me.waliedyassen.runescript.compiler.codegen.local.Local;
import me.waliedyassen.runescript.compiler.codegen.script.Script;
import me.waliedyassen.runescript.compiler.codegen.sw.SwitchCase;
import me.waliedyassen.runescript.compiler.codegen.sw.SwitchTable;
import me.waliedyassen.runescript.compiler.codegen.writer.CodeWriter;
import me.waliedyassen.runescript.compiler.stack.StackType;

import java.util.*;

/**
 * Represents a {@link CodeWriter} implementation that writes to asm bytecode format and outputs an {@link
 * BytecodeScript} object containing all the data for the byte code.
 *
 * @author Walied K. Yassen
 */
public final class BytecodeCodeWriter extends CodeWriter<BytecodeScript> {

    /**
     * An empty list to save some lines of code that compares if the variable stack is present or not.
     */
    private static final List<Local> EMPTY = Collections.emptyList();

    /**
     * {@inheritDoc}
     */
    @Override
    public BytecodeScript write(Script script) {
        // Build the address table of the blocks.
        final var addressTable = buildAddressTable(script.getBlocks());
        // Build the index table of the local variables.
        final var localTable = buildLocalTable(script.getParameters(), script.getVariables());
        // Calculate the local variables and  parameters count.
        var numIntParameters = script.getParameters().getOrDefault(StackType.INT, EMPTY).size();
        var numStringParameters = script.getParameters().getOrDefault(StackType.STRING, EMPTY).size();
        var numLongParameters = script.getParameters().getOrDefault(StackType.LONG, EMPTY).size();
        var numIntLocals = script.getVariables().getOrDefault(StackType.INT, EMPTY).size() + numIntParameters;
        var numStringLocals = script.getVariables().getOrDefault(StackType.STRING, EMPTY).size() + numStringParameters;
        var numLongLocals = script.getVariables().getOrDefault(StackType.LONG, EMPTY).size() + numLongParameters;
        //
        var switchTables = new LinkedList<Hashtable<Integer, Integer>>();
        final var instructions = new ArrayList<BytecodeInstruction>();
        for (var label : script.getBlocks().keySet()) {
            var $block = script.getBlocks().get(label);
            for (var $instruction : $block.getInstructions()) {
                var address = instructions.size();
                var operand = $instruction.getOperand();
                if (operand instanceof Label) {
                    operand = addressTable.get(operand) - address - 1;
                } else if (operand instanceof SwitchTable) {
                    var jumps = new Hashtable<Integer, Integer>();
                    for (SwitchCase $case : ((SwitchTable) operand).getCases()) {
                        var jump = addressTable.get($case.getLabel()) - address - 1;
                        for (var key : $case.getKeys()) {
                            jumps.put(key, jump);
                        }
                    }
                    operand = switchTables.size();
                    switchTables.add(jumps);
                } else if (operand instanceof Local) {
                    operand = localTable.get(operand);
                } else if (operand instanceof Integer) {
                    // NOOP
                } else if (operand instanceof String) {
                    // NOOP
                } else if (operand instanceof Long) {
                    // NOOP
                } else {
                    throw new UnsupportedOperationException("Unsupported operand type: " + operand + ", for opcode: " + $instruction.getOpcode());
                }
                if (operand == null) {
                    throw new IllegalStateException("Null operands are not allowed");
                }
                instructions.add(new BytecodeInstruction($instruction.getOpcode().getCode(), $instruction.getOpcode().isLarge(), operand));
            }
        }
        // Create the container object and return it.
        return new BytecodeScript(script.getName(), numIntParameters, numStringParameters, numLongParameters, numIntLocals, numStringLocals, numLongLocals, instructions.toArray(BytecodeInstruction[]::new), switchTables);
    }

    /**
     * Builds the index table of the specified local variables nad parameters.
     *
     * @param parameters
     *         the parameters to  build the index table for.
     * @param variables
     *         the local variables to build the index table for.
     *
     * @return the index table as a {@link Map} object.
     */
    private Map<Local, Integer> buildLocalTable(Map<StackType, List<Local>> parameters, Map<StackType, List<Local>> variables) {
        final Map<StackType, List<Local>>[] combined = new Map[]{parameters, variables};
        var table = new HashMap<Local, Integer>();
        var numInts = 0;
        var numStrings = 0;
        var numLongs = 0;
        for (var map : combined) {
            for (var stackType : map.keySet()) {
                for (var local : map.get(stackType)) {
                    switch (stackType) {
                        case INT:
                            table.put(local, numInts++);
                            break;
                        case STRING:
                            table.put(local, numStrings++);
                            break;
                        case LONG:
                            table.put(local, numLongs++);
                            break;
                    }
                }
            }
        }
        return table;
    }

    /**
     * Builds the address table for the specified map of {@link Block blocks}.
     *
     * @param blocks
     *         the map of blocks to build the address table for.
     *
     * @return the address table as a {@link Map} object.
     */
    private Map<Label, Integer> buildAddressTable(Map<Label, Block> blocks) {
        var table = new HashMap<Label, Integer>();
        var address = 0;
        for (var label : blocks.keySet()) {
            var block = blocks.get(label);
            table.put(label, address);
            address += block.getInstructions().size();
        }
        return table;
    }
}

