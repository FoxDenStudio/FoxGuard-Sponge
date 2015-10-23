package tk.elektrofuchse.fox.foxguard.factory;

import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;

/**
 * Created by Fox on 10/22/2015.
 */
public interface IFlagSetFactory extends IFGFactory{


    IFlagSet createFlagSet();
}
