/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
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

package tk.elektrofuchse.fox.foxguard.commands.util;

import com.flowpowered.math.vector.Vector3i;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 8/18/2015.
 * Project: foxguard
 */
public class InternalCommandState {

    public List<IRegion> selectedRegions = new LinkedList<>();
    public List<IFlagSet> selectedFlagSets = new LinkedList<>();
    public List<Vector3i> positions = new ArrayList<>();

    public void flush() {
        selectedRegions.clear();
        selectedFlagSets.clear();
        positions.clear();
    }

    public void flush(StateField field) {
        switch (field) {
            case REGIONS:
                selectedRegions.clear();
                break;
            case FLAGSETS:
                selectedFlagSets.clear();
                break;
            case POSITIONS:
                positions.clear();
                break;
        }
    }

    public void flush(StateField... fields) {
        for (StateField field : fields) {
            this.flush(field);
        }
    }


    public enum StateField {
        REGIONS,
        FLAGSETS,
        POSITIONS
    }
}
