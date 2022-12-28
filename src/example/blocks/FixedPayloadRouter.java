package example.blocks;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import example.blocks.Annotations.Load;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.logic.LAccess;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.*;

public class FixedPayloadRouter extends FixedPayloadConveyor{

	public @Load("@-over") TextureRegion overRegion;

    public FixedPayloadRouter(String name){
        super(name);

        outputsPayload = true;
        outputFacing = false;
        configurable = true;
        clearOnDoubleTap = true;

        config(Block.class, (PayloadRouterBuild tile, Block item) -> tile.sorted = item);
        config(UnitType.class, (PayloadRouterBuild tile, UnitType item) -> tile.sorted = item);
        configClear((PayloadRouterBuild tile) -> tile.sorted = null);
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        super.drawPlanRegion(plan, list);

        Draw.rect(overRegion, plan.drawx(), plan.drawy());
    }

    public boolean canSort(Block b){
        return b.isVisible() && b.size <= size && !(b instanceof CoreBlock) && !state.rules.isBanned(b) && b.environmentBuildable();
    }

    public boolean canSort(UnitType t){
        return !t.isHidden() && !t.isBanned() && t.supportsEnv(state.rules.env);
    }

    public class PayloadRouterBuild extends PayloadConveyorBuild {
        public @Nullable UnlockableContent sorted;
        public int recDir;
        public boolean matches;

        public float smoothRot;
        public float controlTime = -1f;

        @Override
        public void add(){
            super.add();
            smoothRot = rotdeg();
        }

        public void pickNext(){
            if(item != null && controlTime <= 0f){
                if(matches){
                    //when the item matches, always move forward.
                    rotation = recDir;
                    onProximityUpdate();
                }else{
                    int rotations = 0;
                    do{
                        rotation = (rotation + 1) % 4;
                        //if it doesn't match the sort item and this router is facing forward, skip this rotation
                        if(!matches && sorted != null && rotation == recDir){
                            rotation ++;
                        }
                        onProximityUpdate();

                        //force update to transfer if necessary
                        if(next instanceof PayloadConveyorBuild && !(next instanceof PayloadRouterBuild)){
                            next.updateTile();
                        }
                        //this condition intentionally uses "accept from itself" conditions, because payload conveyors only accept during the start
                        //"accept from self" conditions are for dropped payloads and are less restrictive
                    }while((blocked || next == null || !next.acceptPayload(next, item)) && ++rotations < 4);
                }
            }else{
                onProximityUpdate();
            }
        }

        @Override
        public void control(LAccess type, double p1, double p2, double p3, double p4){
            super.control(type, p1, p2, p3, p4);
            if(type == LAccess.config){
                int prev = rotation;
                rotation = Mathf.mod((int)p1, 4);
                //when manually controlled, routers do not turn automatically for a while, same as turrets
                controlTime = 60f * 6f;
                if(prev != rotation){
                    onProximityUpdate();
                }
            }
        }

        @Override
        public void onControlSelect(Unit player){
            super.onControlSelect(player);
            //this will immediately snap back if logic controlled
            recDir = rotation;
            checkMatch();
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            super.handlePayload(source, payload);
            if(controlTime < 0f){ //don't overwrite logic recDir
                recDir = source == null ? rotation : source.relativeTo(this);
            }
            checkMatch();
            pickNext();
        }

        public void checkMatch(){ // Replaced "item instanceof <Class>" to item.getClass().equals(<Class>.class)
        	boolean b1 = false;
        	boolean b2 = false;
        	if(item.getClass().equals(BuildPayload.class)) {
        		b1 = ((BuildPayload)item).block() == sorted;
        	}
        	
        	if(item.getClass().equals(UnitPayload.class)) {
        		b1 = ((UnitPayload)item).unit.type == sorted;
        	}
//            matches = sorted != null &&
//                    (item instanceof BuildPayload build && build.block() == sorted) ||
//                    (item instanceof UnitPayload unit && unit.unit.type == sorted);
            matches = sorted != null && (b1) || (b2);
        }

        @Override
        public void moveFailed(){
            pickNext();
        }

        @Override
        public void updateTile(){
            super.updateTile();

            controlTime -= Time.delta;
            smoothRot = Mathf.slerpDelta(smoothRot, rotdeg(), 0.2f);
        }

        @Override
        public void drawSelect(){
            if(sorted != null){
                float dx = x - size * tilesize/2f, dy = y + size * tilesize/2f, s = iconSmall / 4f;
                Draw.mixcol(Color.darkGray, 1f);
                Draw.rect(sorted.fullIcon, dx, dy - 1, s, s);
                Draw.reset();
                Draw.rect(sorted.fullIcon, dx, dy, s, s);
            }
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            float dst = 0.8f;

            Draw.mixcol(team.color, Math.max((dst - (Math.abs(fract() - 0.5f) * 2)) / dst, 0));
            Draw.rect(topRegion, x, y, smoothRot);
            Draw.reset();

            Draw.rect(overRegion, x, y);

            Draw.z(Layer.blockOver);

            if(item != null){
                item.draw();
            }
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(FixedPayloadRouter.this, table,
                content.blocks().select(FixedPayloadRouter.this::canSort).<UnlockableContent>as()
                .add(content.units().select(FixedPayloadRouter.this::canSort).as()),
                () -> (UnlockableContent)config(), this::configure);
        }

        @Override
        public Object config(){
            return sorted;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(sorted == null ? -1 : sorted.getContentType().ordinal());
            write.s(sorted == null ? -1 : sorted.id);
            write.b(recDir);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                byte ctype = read.b();
                short sort = read.s();
                sorted = ctype == -1 ? null : Vars.content.getByID(ContentType.all[ctype], sort);
                recDir = read.b();
                checkMatch();
            }
        }
    }
}
