package hellfirepvp.astralsorcery.common.block;

import hellfirepvp.astralsorcery.common.registry.RegistryItems;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockAttunementRelay
 * Created by HellFirePvP
 * Date: 30.11.2016 / 13:16
 */
public class BlockAttunementRelay extends Block {

    public BlockAttunementRelay() {
        super(Material.GLASS, MapColor.QUARTZ);
        setHardness(0.5F);
        setHarvestLevel("pickaxe", 0);
        setResistance(1.0F);
        setSoundType(SoundType.GLASS);
        setCreativeTab(RegistryItems.creativeTabAstralSorcery);
    }
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean canRenderInLayer(BlockRenderLayer layer) {
        return layer == BlockRenderLayer.SOLID || layer == BlockRenderLayer.TRANSLUCENT;
    }
}
