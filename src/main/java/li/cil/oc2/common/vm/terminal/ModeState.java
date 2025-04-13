package li.cil.oc2.common.vm.terminal;

import li.cil.ceres.api.Serialized;

@Serialized
public class ModeState {
    public boolean KAM = false;
    public boolean IRM = false;
    public boolean SRM = false;
    public boolean LNM = false;

    public boolean getMode(int mode) {
        return switch (mode) {
            case 2 -> KAM;
            case 4 -> IRM;
            case 12 -> SRM;
            case 20 -> LNM;
            default -> throw new IndexOutOfBoundsException();
        };
    }
}
