/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) gravityfox - https://gravityfox.net/
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.gravityfox.foxguard.commands.util;

import com.flowpowered.math.vector.Vector3i;
import net.gravityfox.foxguard.handlers.IHandler;
import net.gravityfox.foxguard.regions.IRegion;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class InternalCommandState {

    public List<IRegion> selectedRegions = new LinkedList<>();
    public List<IHandler> selectedHandlers = new LinkedList<>();
    public List<Vector3i> positions = new ArrayList<>();

    public void flush() {
        selectedRegions.clear();
        selectedHandlers.clear();
        positions.clear();
    }

    public void flush(StateField field) {
        switch (field) {
            case REGIONS:
                selectedRegions.clear();
                return;
            case HANDLERS:
                selectedHandlers.clear();
                return;
            case POSITIONS:
                positions.clear();
                return;
        }
    }

    public void flush(StateField... fields) {
        for (StateField field : fields) {
            this.flush(field);
        }
    }


    public enum StateField {
        REGIONS,
        HANDLERS,
        POSITIONS
    }
}
