package com.jieli.btsmart.data.model.bluetooth;

import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.response.TargetInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.utils.CHexConver;

import java.util.List;

/**
 * 设备信息
 * <p>
 * 作用：缓存当前连接设备的基本信息
 * </p>
 *
 * @author zqjasonZhong
 * @date 2020/5/12
 */
@SuppressWarnings("UnusedReturnValue")
public class DeviceInfo {
    //设备基本信息
    private int pid;        // 产品ID
    private int vid;        // 厂商ID
    private int uid;        // 客户ID
    private int volume;     // 设备当前音量
    private int maxVol;     // 设备最大音量
    private int quantity;   // 设备电量
    private int functionMask;// 功能支持掩码
    private byte curFunction = -1;// 当前模式
    private boolean hideNetRadio = false;//隐藏网络电台
    private int sdkType; //是否为标准sdk 默认false即默认为ai sdk
    private String edrAddr; //经典蓝牙地址

    //设备版本信息
    private int versionCode; //设备版本号
    private String versionName;  //设备版本名称
    private String protocolVersion;//协议版本
    //用于自定义版本信息
    private String customVersionMsg;

    //用于服务器校验产品信息 -
    private String authKey; //认证秘钥
    private String projectCode; //项目标识码

    //ai平台相关参数
    private int platform = -1;//AI平台类型（0：图灵；1：deepbrain；2：小米）
    private String license;//设备唯一标示

    //闹钟信息
    private AlarmListInfo alarmListInfo;
    //闹钟信息
    private List<DefaultAlarmBell> alarmDefaultBells;
    //闹钟数据结构版本
    private int alarmVersion;

    //闹钟拓展字段支持标识
    private byte alarmExpandFlag = 0x00;

    //音乐名称
    private MusicNameInfo musicNameInfo;
    //音乐状态
    private MusicStatusInfo musicStatusInfo;
    //Aux播放状态
    private boolean isAuxPlay;

    //可以播放的格式
    private String playFileFormat;

    //其他信息
    private boolean emitterSupport;//是否支持外设模式
    private int emitterMode;//0x00:普通模式  0x01:外设模式
    private int cluster;//当前播放的文件簇号
    private byte currentDevIndex;//当前播放文件的设备序号

    private boolean supportVolumeSync;//是否支持音量同步
    private boolean supportSearchDev; //是否支持查找设备
    private boolean supportSoundCard; //是否支持声卡功能
    private boolean supportExtFlashTransfer; //是否支持外挂Flash传输功能
    private boolean supportAnc; //是否支持主动降噪功能

    private EqPresetInfo eqPresetInfo;
    private EqInfo eqInfo;
    private LightControlInfo lightControlInfo;


    private EqInfo soundCardEqInfo;

    private boolean supportOfflineShow = false;//是否支持usb、sd、linein不在线时显示功能图标

    private boolean banEq = false;//禁止app调节设备eq
    private boolean fatFsException = false; //Fatfs系统是否异常

    private VoiceMode currentVoiceMode; //当前噪声处理模式信息
    private List<VoiceMode> voiceModeList; //所有噪声处理模式信息


    public static DeviceInfo convertFromTargetInfo(TargetInfoResponse targetInfo) {
        if (targetInfo == null) return null;
        return new DeviceInfo().setPid(targetInfo.getPid()).setVid(targetInfo.getVid())
                .setVolume(targetInfo.getVolume()).setMaxVol(targetInfo.getMaxVol())
                .setQuantity(targetInfo.getQuantity()).setFunctionMask(targetInfo.getFunctionMask())
                .setCurFunction(targetInfo.getCurFunction()).setHideRadio(targetInfo.isHideNetRadio())
                .setSdkType(targetInfo.getSdkType())
                .setVersionCode(targetInfo.getVersionCode()).setVersionName(targetInfo.getVersionName())
                .setProtocolVersion(targetInfo.getProtocolVersion()).setCustomVersionMsg(targetInfo.getCustomVersionMsg())
                .setAuthKey(targetInfo.getAuthKey()).setProjectCode(targetInfo.getProjectCode())
                .setPlatform(targetInfo.getPlatform()).setLicense(targetInfo.getLicense())
                .setEmitterSupport(targetInfo.isEmitterSupport()).setEmitterMode(targetInfo.getEmitterStatus())
                .setUid(targetInfo.getVid()).setEdrAddr(targetInfo.getEdrAddr())
                .setSupportOfflineShow(targetInfo.isSupportOfflineShow())
                .setSupportVolumeSync(targetInfo.isSupportVolumeSync())
                .setSupportSoundCard(targetInfo.isSupportSoundCard())
                .setSupportSearchDev(targetInfo.isSupportSearchDevice())
                .setBanEq(targetInfo.isBanEq())
                .setSupportSearchDev(targetInfo.isSupportSearchDevice())
                .setSupportExtFlashTransfer(targetInfo.isSupportExternalFlashTransfer())
                .setSupportAnc(targetInfo.isSupportAnc());

    }

    public int getPid() {
        return pid;
    }

    public DeviceInfo setPid(int pid) {
        this.pid = pid;
        return this;
    }

    public int getVid() {
        return vid;
    }

    public DeviceInfo setVid(int vid) {
        this.vid = vid;
        return this;
    }

    public int getVolume() {
        return volume;
    }

    public DeviceInfo setVolume(int volume) {
        this.volume = volume;
        return this;
    }

    public int getMaxVol() {
        return maxVol;
    }

    public DeviceInfo setMaxVol(int maxVol) {
        this.maxVol = maxVol;
        return this;
    }

    public int getQuantity() {
        return quantity;
    }

    public DeviceInfo setQuantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    public int getFunctionMask() {
        return functionMask;
    }

    public DeviceInfo setFunctionMask(int functionMask) {
        this.functionMask = functionMask;
        return this;
    }

    public byte getCurFunction() {
        return curFunction;
    }

    public DeviceInfo setCurFunction(byte curFunction) {
        this.curFunction = curFunction;
        return this;
    }

    public boolean isHideRadio() {
        return hideNetRadio;
    }

    public DeviceInfo setHideRadio(boolean hideNetRadio) {
        this.hideNetRadio = hideNetRadio;
        return this;
    }

    public int getSdkType() {
        return sdkType;
    }

    public DeviceInfo setSdkType(int sdkType) {
        this.sdkType = sdkType;
        return this;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public DeviceInfo setVersionCode(int versionCode) {
        this.versionCode = versionCode;
        return this;
    }

    public String getVersionName() {
        return versionName;
    }

    public DeviceInfo setVersionName(String versionName) {
        this.versionName = versionName;
        return this;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public DeviceInfo setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
        return this;
    }

    public String getCustomVersionMsg() {
        return customVersionMsg;
    }

    public DeviceInfo setCustomVersionMsg(String customVersionMsg) {
        this.customVersionMsg = customVersionMsg;
        return this;
    }

    public String getAuthKey() {
        return authKey;
    }

    public DeviceInfo setAuthKey(String authKey) {
        this.authKey = authKey;
        return this;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public DeviceInfo setProjectCode(String projectCode) {
        this.projectCode = projectCode;
        return this;
    }

    public int getPlatform() {
        return platform;
    }

    public DeviceInfo setPlatform(int platform) {
        this.platform = platform;
        return this;
    }

    public String getLicense() {
        return license;
    }

    public DeviceInfo setLicense(String license) {
        this.license = license;
        return this;
    }

    public AlarmListInfo getAlarmListInfo() {
        return alarmListInfo;
    }

    public DeviceInfo setAlarmListInfo(AlarmListInfo alarmListInfo) {
        this.alarmListInfo = alarmListInfo;
        return this;
    }

    public String getPlayFileFormat() {
        return playFileFormat;
    }

    public DeviceInfo setPlayFileFormat(String playFileFormat) {
        this.playFileFormat = playFileFormat;
        return this;
    }

    public boolean isEmitterSupport() {
        return emitterSupport;
    }

    public DeviceInfo setEmitterSupport(boolean emitterSupport) {
        this.emitterSupport = emitterSupport;
        return this;
    }

    public int getEmitterMode() {
        return emitterMode;
    }

    public DeviceInfo setEmitterMode(int emitterMode) {
        this.emitterMode = emitterMode;
        return this;
    }

    public MusicNameInfo getMusicNameInfo() {
        return musicNameInfo;
    }

    public DeviceInfo setMusicNameInfo(MusicNameInfo musicNameInfo) {
        this.musicNameInfo = musicNameInfo;
        if (musicNameInfo != null) {
            this.cluster = musicNameInfo.getCluster();
        }
        return this;
    }

    public MusicStatusInfo getMusicStatusInfo() {
        return musicStatusInfo;
    }

    public DeviceInfo setMusicStatusInfo(MusicStatusInfo musicStatusInfo) {
        this.musicStatusInfo = musicStatusInfo;
        if (musicStatusInfo != null) {
            this.currentDevIndex = (byte) musicStatusInfo.getCurrentDev();
        }
        return this;
    }

    public boolean isAuxPlay() {
        return isAuxPlay;
    }

    public DeviceInfo setAuxPlay(boolean auxPlay) {
        isAuxPlay = auxPlay;
        return this;
    }

    public int getUid() {
        return uid;
    }

    public DeviceInfo setUid(int uid) {
        this.uid = uid;
        return this;
    }

    public String getEdrAddr() {
        return edrAddr;
    }

    public DeviceInfo setEdrAddr(String edrAddr) {
        this.edrAddr = edrAddr;
        return this;
    }


    public boolean isBtEnable() {
        return functionMask == 0 || checkDevModeExist(AttrAndFunCode.SYS_INFO_FUNCTION_BT);
    }

    public boolean isDevMusicEnable() {
        return checkDevModeExist(AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC);
    }

    public boolean isRTCEnable() {
        return checkDevModeExist(AttrAndFunCode.SYS_INFO_FUNCTION_RTC);
    }

    public boolean isAuxEnable() {
        return checkDevModeExist(AttrAndFunCode.SYS_INFO_FUNCTION_AUX);
    }

    public boolean isFmEnable() {
        return checkDevModeExist(AttrAndFunCode.SYS_INFO_FUNCTION_FM);
    }

    public boolean isLightEnable() {
        return checkDevModeExist(AttrAndFunCode.SYS_INFO_FUNCTION_LIGHT);
    }

    public boolean isFmTxEnable() {
        return checkDevModeExist(AttrAndFunCode.SYS_INFO_FUNCTION_FMTX);
    }

    public boolean isEQEnable() {
        return true; //checkDevModeExist(AttrAndFunCode.SYS_INFO_FUNCTION_EQ);
    }

    public boolean isSPDIFEnable() {
        return checkDevModeExist(AttrAndFunCode.SYS_INFO_FUNCTION_SPDIF);
    }

    public boolean isPCSlaveEnable() {
        return checkDevModeExist(AttrAndFunCode.SYS_INFO_FUNCTION_PC_SLAVE);
    }

    public DeviceInfo setCluster(int cluster) {
        this.cluster = cluster;
        return this;
    }

    public int getCluster() {
        return cluster;
    }

    public DeviceInfo setCurrentDevIndex(byte currentDevIndex) {
        this.currentDevIndex = currentDevIndex;
        return this;
    }

    public byte getCurrentDevIndex() {
        return currentDevIndex;
    }

    /**
     * 检查设备模式的存在
     *
     * @param mode 设备模式
     *             <p>具体参考: {@link AttrAndFunCode} 的 FUNCTION 字段,
     *             不包括{@link AttrAndFunCode#SYS_INFO_FUNCTION_PUBLIC}</p>
     * @return 结果
     */
    public boolean checkDevModeExist(byte mode) {
        if (mode > 32) return false;
        byte value = CHexConver.intToByte((int) Math.pow(2, CHexConver.byteToInt(mode)));
        return (functionMask & value) == value;
    }

    public DeviceInfo setEqInfo(EqInfo eqInfo) {
        this.eqInfo = eqInfo;
        return this;
    }

    public EqInfo getEqInfo() {
        return eqInfo;
    }

    public LightControlInfo getLightControlInfo() {
        return lightControlInfo;
    }

    public DeviceInfo setLightControlInfo(LightControlInfo lightControlInfo) {
        this.lightControlInfo = lightControlInfo;
        return this;
    }

    public DeviceInfo setEqPresetInfo(EqPresetInfo eqPresetInfo) {
        this.eqPresetInfo = eqPresetInfo;
        return this;
    }

    public EqPresetInfo getEqPresetInfo() {
        return eqPresetInfo;
    }


    public boolean isSupportOfflineShow() {
        return supportOfflineShow;
    }

    public DeviceInfo setSupportOfflineShow(boolean supportOfflineShow) {
        this.supportOfflineShow = supportOfflineShow;
        return this;
    }

    public DeviceInfo setSupportVolumeSync(boolean supportVolumeSync) {
        this.supportVolumeSync = supportVolumeSync;
        return this;
    }

    public boolean isSupportVolumeSync() {
        return supportVolumeSync;
    }

    public boolean isSupportSearchDev() {
        return supportSearchDev;
    }

    public DeviceInfo setSupportSearchDev(boolean supportSearchDev) {
        this.supportSearchDev = supportSearchDev;
        return this;
    }

    public DeviceInfo setAlarmDefaultBells(List<DefaultAlarmBell> alarmDefaultBells) {
        this.alarmDefaultBells = alarmDefaultBells;
        return this;
    }

    public List<DefaultAlarmBell> getAlarmDefaultBells() {
        return alarmDefaultBells;
    }

    public DeviceInfo setAlarmVersion(int alarmVersion) {
        this.alarmVersion = alarmVersion;
        return this;
    }

    public int getAlarmVersion() {
        return alarmVersion;
    }

    public DeviceInfo setSupportSoundCard(boolean supportSoundCard) {
        this.supportSoundCard = supportSoundCard;
        return this;
    }


    public boolean isSupportSoundCard() {
        return supportSoundCard;
    }


    public DeviceInfo setSoundCardEqInfo(EqInfo micEqInfo) {
        this.soundCardEqInfo = micEqInfo;
        return this;
    }

    public EqInfo getSoundCardEqInfo() {
        return soundCardEqInfo;
    }

    public DeviceInfo setBanEq(boolean banEq) {
        this.banEq = banEq;
        return this;
    }

    public boolean isBanEq() {
        return banEq;
    }

    public boolean isSupportExtFlashTransfer() {
        return supportExtFlashTransfer;
    }

    public DeviceInfo setSupportExtFlashTransfer(boolean supportExtFlashTransfer) {
        this.supportExtFlashTransfer = supportExtFlashTransfer;
        return this;
    }

    public boolean isFatFsException() {
        return fatFsException;
    }

    public DeviceInfo setFatFsException(boolean fatFsException) {
        this.fatFsException = fatFsException;
        return this;
    }

    public boolean isSupportAnc() {
        return supportAnc;
    }

    public DeviceInfo setSupportAnc(boolean supportAnc) {
        this.supportAnc = supportAnc;
        return this;
    }

    public VoiceMode getCurrentVoiceMode() {
        return currentVoiceMode;
    }

    public DeviceInfo setCurrentVoiceMode(VoiceMode currentVoiceMode) {
        this.currentVoiceMode = currentVoiceMode;
        return this;
    }

    public List<VoiceMode> getVoiceModeList() {
        return voiceModeList;
    }

    public DeviceInfo setVoiceModeList(List<VoiceMode> voiceModeList) {
        this.voiceModeList = voiceModeList;
        return this;
    }

    public DeviceInfo setAlarmExpandFlag(byte alarmExpandFlag) {
        this.alarmExpandFlag = alarmExpandFlag;
        return this;
    }

    public byte getAlarmExpandFlag() {
        return alarmExpandFlag;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "pid=" + pid +
                ", vid=" + vid +
                ", uid=" + uid +
                ", volume=" + volume +
                ", maxVol=" + maxVol +
                ", quantity=" + quantity +
                ", functionMask=" + functionMask +
                ", curFunction=" + curFunction +
                ", sdkType=" + sdkType +
                ", edrAddr='" + edrAddr + '\'' +
                ", versionCode=" + versionCode +
                ", versionName='" + versionName + '\'' +
                ", protocolVersion='" + protocolVersion + '\'' +
                ", customVersionMsg='" + customVersionMsg + '\'' +
                ", authKey='" + authKey + '\'' +
                ", projectCode='" + projectCode + '\'' +
                ", platform=" + platform +
                ", license='" + license + '\'' +
                ", alarmListInfo=" + alarmListInfo +
                ", musicNameInfo=" + musicNameInfo +
                ", musicStatusInfo=" + musicStatusInfo +
                ", isAuxPlay=" + isAuxPlay +
                ", playFileFormat='" + playFileFormat + '\'' +
                ", emitterSupport=" + emitterSupport +
                ", emitterMode=" + emitterMode +
                ", cluster=" + cluster +
                ", currentDevIndex=" + currentDevIndex +
                ", supportVolumeSync=" + supportVolumeSync +
                ", supportSearchDev=" + supportSearchDev +
                ", eqPresetInfo=" + eqPresetInfo +
                ", eqInfo=" + eqInfo +
                ", supportOfflineShow=" + supportOfflineShow +
                ", alarmVersion=" + alarmVersion +
                ", supportSoundCard=" + supportSoundCard +
                ", supportExtFlashTransfer=" + supportExtFlashTransfer +
                ", fatFsException=" + fatFsException +
                ", isSupportAnc=" + supportAnc +
                ", currentVoiceMode=" + currentVoiceMode +
                ", voiceModeList=" + voiceModeList +
                ", alarmExpandFlag=" + alarmExpandFlag +
                '}';
    }
}
