package tk.elektrofuchse.fox.foxguard.flags.util;

/**
 * Created by Fox on 8/17/2015.
 */
public enum FlagState {
    PASSTHROUGH,
    TRUE,
    FALSE;

    public static FlagState newState(FlagState oldState, FlagState state){
        return state.ordinal() > oldState.ordinal() ? state : oldState;
    }
}
