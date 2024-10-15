package com.robertx22.mine_and_slash.maps.feature;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.mine_and_slash.maps.MapData;
import com.robertx22.mine_and_slash.maps.generator.BuiltRoom;
import com.robertx22.mine_and_slash.maps.generator.DungeonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public class DungeonFeature {

    public static boolean place(MapData mapData, LevelAccessor level, RandomSource rand, BlockPos pos) {
        return generateStructure(mapData, level, new ChunkPos(pos), rand);
    }

    public static boolean generatePiece(LevelAccessor world, BlockPos position, RandomSource random, Rotation rota, ResourceLocation id) {

        var template = world.getServer().getStructureManager().get(id).get();
        StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(Mirror.NONE)
                .setRotation(rota)
                .setIgnoreEntities(false);

        settings.setBoundingBox(settings.getBoundingBox());


        if (template == null) {
            ExileLog.get().warn("FATAL ERROR: Structure does not exist (" + id + ")");
            return false;
        }

        // next if the structure is to be rotated then it must also be offset, because rotating a structure also moves it
        if (rota == Rotation.COUNTERCLOCKWISE_90) {
            // west: rotate CCW and push +Z
            settings.setRotation(Rotation.COUNTERCLOCKWISE_90);
            position = position.offset(0, 0, template.getSize().getZ() - 1);
        } else if (rota == Rotation.CLOCKWISE_90) {
            // east rotate CW and push +X
            settings.setRotation(Rotation.CLOCKWISE_90);
            position = position.offset(template.getSize().getX() - 1, 0, 0);
        } else if (rota == Rotation.CLOCKWISE_180) {
            // south: rotate 180 and push both +X and +Z
            settings.setRotation(Rotation.CLOCKWISE_180);
            position = position.offset(template.getSize().getX() - 1, 0, template.getSize().getZ() - 1);
        } else //if (nextRoom.rotation == Rotation.NONE)
        {                // north: no rotation
            settings.setRotation(Rotation.NONE);
        }


        return template.placeInWorld((ServerLevelAccessor) world, position, position, settings, random, Block.UPDATE_CLIENTS);

    }

    private static boolean generateStructure(MapData mapData, LevelAccessor world, ChunkPos cpos, RandomSource random) {


        DungeonBuilder builder = new DungeonBuilder(0, cpos);
        builder.build();

        if (!builder.builtDungeon.hasRoomForChunk(cpos)) {
            return false;
        }

        var bpos = cpos.getMiddleBlockPosition(50);

        ChunkPos start = MapData.getStartChunk(bpos);

        // if its the start of the dungeon, we init some stuff
        if (cpos.equals(start)) {
            // todo
        }

        BuiltRoom room = builder.builtDungeon.getRoomForChunk(cpos);


        if (room == null) {
            return false;
        }

        if (!room.room.isBarrier) {
            // we make sure only valid rooms are added to the total
            mapData.leagues.totalGenDungeonChunks++;
        }

      
        BlockPos position = cpos.getBlockAt(0, 50, 0);

        generatePiece(world, position, random, room.data.rotation, room.getStructure());

        return true;


    }

}
