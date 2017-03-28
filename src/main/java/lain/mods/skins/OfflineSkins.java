package lain.mods.skins;

import lain.mods.skins.api.ISkin;
import lain.mods.skins.api.ISkinProviderService;
import lain.mods.skins.api.SkinProviderAPI;
import lain.mods.skins.providers.CustomCachedCapeProvider;
import lain.mods.skins.providers.CustomCachedSkinProvider;
import lain.mods.skins.providers.MojangCachedCapeProvider;
import lain.mods.skins.providers.MojangCachedSkinProvider;
import lain.mods.skins.providers.UserManagedCapeProvider;
import lain.mods.skins.providers.UserManagedSkinProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.mojang.authlib.GameProfile;

@Mod(modid = "offlineskins", useMetadata = true, acceptedMinecraftVersions = "[1.11],[1.11.2]")
public class OfflineSkins
{

    @SideOnly(Side.CLIENT)
    public static ResourceLocation bindTexture(GameProfile profile, ResourceLocation result)
    {
        if (SkinData.isDefaultSkin(result) && profile != null)
        {
            ISkin skin = skinService.getSkin(profile);
            if (skin != null && skin.isSkinReady())
                return skin.getSkinLocation();
        }
        return result;
    }

    @SideOnly(Side.CLIENT)
    public static ResourceLocation getLocationCape(AbstractClientPlayer player, ResourceLocation result)
    {
        if (result == null && capeService != null)
        {
            ISkin cape = capeService.getSkin(player.getGameProfile());
            if (cape != null && cape.isSkinReady())
                return cape.getSkinLocation();
        }
        return result;
    }

    @SideOnly(Side.CLIENT)
    public static ResourceLocation getLocationSkin(AbstractClientPlayer player, ResourceLocation result)
    {
        if (SkinPass)
            return result;

        if (usingDefaultSkin(player) && skinService != null)
        {
            ISkin skin = skinService.getSkin(player.getGameProfile());
            if (skin != null && skin.isSkinReady())
                return skin.getSkinLocation();
        }
        return result;
    }

    @SideOnly(Side.CLIENT)
    public static String getSkinType(AbstractClientPlayer player, String result)
    {
        if (usingDefaultSkin(player) && skinService != null)
        {
            ISkin skin = skinService.getSkin(player.getGameProfile());
            if (skin != null && skin.isSkinReady())
                return skin.getSkinType();
        }
        return result;
    }

    @SideOnly(Side.CLIENT)
    public static boolean usingDefaultSkin(AbstractClientPlayer player)
    {
        try
        {
            SkinPass = true;
            return SkinData.isDefaultSkin(player.getLocationSkin());
        }
        finally
        {
            SkinPass = false;
        }
    }

    private static boolean SkinPass = false;

    @SideOnly(Side.CLIENT)
    public static ISkinProviderService skinService;
    @SideOnly(Side.CLIENT)
    public static ISkinProviderService capeService;

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void handleClientTicks(TickEvent.ClientTickEvent event)
    {
        if (skinService == null && capeService == null)
            return;

        if (event.phase == TickEvent.Phase.START)
        {
            World world = Minecraft.getMinecraft().world;
            if (world != null && world.playerEntities != null && !world.playerEntities.isEmpty())
            {
                for (Object obj : world.playerEntities)
                {
                    // This should keep skins/capes loaded.
                    if (obj instanceof AbstractClientPlayer)
                    {
                        if (skinService != null)
                            skinService.getSkin(((AbstractClientPlayer) obj).getGameProfile());
                        if (capeService != null)
                            capeService.getSkin(((AbstractClientPlayer) obj).getGameProfile());
                    }
                }
            }
        }
    }

    @Mod.EventHandler
    public void init(FMLPreInitializationEvent event)
    {
        if (event.getSide().isClient())
        {
            Configuration config = new Configuration(event.getSuggestedConfigurationFile());
            boolean useCrafatar = config.getBoolean("useCrafatar", Configuration.CATEGORY_CLIENT, true, "Use Crafatar skin cache.");
            String useCustomSkin = config.getString("useCustomSkin", Configuration.CATEGORY_CLIENT, "http://106.245.251.91:11280/skins/%s.png", "Use custom skin url. ex) http://test.com/skin_%s.png");
            String useCustomCape = config.getString("useCustomCape", Configuration.CATEGORY_CLIENT, "http://106.245.251.91:11280/capes/%s.png", "Use custom cape url. ex) http://test.com/cape_%s.png");
            String skinPriority = config.getString("skinPriority", Configuration.CATEGORY_CLIENT, "local, custom, crafatar, mojang", "Priority of skin repository.");
            if (config.hasChanged())
                config.save();

            String priorList[] = skinPriority.toLowerCase().split("[\\s,.]+");

            skinService = SkinProviderAPI.createService();
            capeService = SkinProviderAPI.createService();

            for (String s : priorList) {
                if (s.equals("local")) {
                    skinService.register(new UserManagedSkinProvider());
                    capeService.register(new UserManagedCapeProvider());
                } else
                if (s.equals("custom")) {
                    if (useCustomSkin.length() > 5)
                        skinService.register(new CustomCachedSkinProvider(useCustomSkin));
                    if (useCustomCape.length() > 5)
                        skinService.register(new CustomCachedCapeProvider(useCustomCape));
                } else
                if (s.equals("crafatar")) {
                    if (useCrafatar) {
                        skinService.register(new CustomCachedSkinProvider("https://crafatar.com/skins/%s"));
                        capeService.register(new CustomCachedCapeProvider("https://crafatar.com/capes/%s"));
                    }
                } else
                if (s.equals("mojang")) {
                    skinService.register(new MojangCachedSkinProvider());
                    capeService.register(new MojangCachedCapeProvider());
                }
            }
            

            MinecraftForge.EVENT_BUS.register(this);
        }
    }

}
