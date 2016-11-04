package net.foxdenstudio.sponge.foxguard.plugin.region;


import net.foxdenstudio.sponge.foxcore.plugin.selection.ISelection;

/**
 * Selectable regions are regions that can be converted into an {@link ISelection}.
 */
public interface ISelectableRegion<T extends ISelection> extends IRegion {

    T toSelection();
}
