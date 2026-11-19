package me.exz.omniocular.util;

import java.lang.reflect.Method;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;

public final class GTCompat {

    private static volatile boolean initialized = false;

    private static Class<?> IGregTechTileEntity;
    private static Class<?> IGregTechDeviceInformation;
    private static Method getMetaTileEntity;
    private static Method isGivingInformation;
    private static Method getInfoData;
    private static Method getInfoMap;
    private static Method decode;

    private GTCompat() {}

    private static void init() {
        if (initialized) return;
        synchronized (GTCompat.class) {
            if (initialized) return;
            ClassLoader loader = GTCompat.class.getClassLoader();
            IGregTechTileEntity = find(loader, "gregtech.api.interfaces.tileentity.IGregTechTileEntity");
            IGregTechDeviceInformation = find(loader, "gregtech.api.interfaces.tileentity.IGregTechDeviceInformation");

            if (IGregTechTileEntity != null) {
                getMetaTileEntity = findMethod(IGregTechTileEntity, "getMetaTileEntity");
            }
            if (IGregTechDeviceInformation != null) {
                isGivingInformation = findMethod(IGregTechDeviceInformation, "isGivingInformation");
                getInfoData = findMethod(IGregTechDeviceInformation, "getInfoData");
                getInfoMap = findMethod(IGregTechDeviceInformation, "getInfoMap");
                decode = findMethod(IGregTechDeviceInformation, "decode", String.class);
            }
            initialized = true;
        }
    }

    /**
     * 若该方块实体是 GT 机器，则把设备信息注入到 tag 中。
     */
    public static void appendInfo(TileEntity te, NBTTagCompound tag) {
        init();
        if (te == null || tag == null
            || IGregTechTileEntity == null
            || IGregTechDeviceInformation == null
            || getMetaTileEntity == null) {
            return;
        }
        try {
            if (!IGregTechTileEntity.isInstance(te)) return;

            Object mte = getMetaTileEntity.invoke(te);
            if (mte == null || !IGregTechDeviceInformation.isInstance(mte)) return;

            // isGivingInformation 是可选能力；调用失败或返回 false 时跳过
            if (isGivingInformation != null) {
                Object giving = isGivingInformation.invoke(mte);
                if (Boolean.FALSE.equals(giving)) return;
            }

            if (getInfoData != null) {
                Object data = getInfoData.invoke(mte);
                if (data instanceof String[]) {
                    NBTTagList list = new NBTTagList();
                    for (String line : (String[]) data) {
                        if (line != null) list.appendTag(new NBTTagString(line));
                    }
                    if (list.tagCount() > 0) tag.setTag("ooInfo", list);
                }
            }

            if (getInfoMap != null) {
                Object map = getInfoMap.invoke(mte);
                if (map instanceof Map) {
                    NBTTagCompound mapTag = new NBTTagCompound();
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
                        if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                            mapTag.setString((String) entry.getKey(), (String) entry.getValue());
                        }
                    }
                    if (!mapTag.hasNoTags()) tag.setTag("ooInfoMap", mapTag);
                }
            }
            tag.setBoolean("ooInfoDecodable", decode != null);
        } catch (Throwable ignored) {}
    }

    private static Class<?> find(ClassLoader loader, String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (Throwable e) {
            return null;
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... params) {
        try {
            return owner.getMethod(name, params);
        } catch (Throwable e) {
            return null;
        }
    }
}
