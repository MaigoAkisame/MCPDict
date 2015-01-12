package maigosoft.mcpdict;

public interface Masks {
    public static final int MASK_MC             = 0x1;
    public static final int MASK_PU             = 0x2;
    public static final int MASK_CT             = 0x4;
    public static final int MASK_KR             = 0x8;
    public static final int MASK_VN             = 0x10;
    public static final int MASK_JP_GO          = 0x20;
    public static final int MASK_JP_KAN         = 0x40;
    public static final int MASK_JP_TOU         = 0x80;
    public static final int MASK_JP_KWAN        = 0x100;
    public static final int MASK_JP_OTHER       = 0x200;
    public static final int MASK_JP_ALL         = MASK_JP_GO | MASK_JP_KAN | MASK_JP_TOU | MASK_JP_KWAN | MASK_JP_OTHER;
    public static final int MASK_ALL_READINGS   = MASK_MC | MASK_PU | MASK_CT | MASK_KR | MASK_VN | MASK_JP_ALL;

    public static final int MASK_FAVORITE       = 0x400;
}
