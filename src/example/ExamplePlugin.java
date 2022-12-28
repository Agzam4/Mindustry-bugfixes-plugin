package example;

import static mindustry.type.ItemStack.with;

import arc.util.Log;
import example.blocks.FixedBlocks;
import example.blocks.FixedPayloadConveyor;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Bullets;
import mindustry.content.ErekirTechTree;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.Loadouts;
import mindustry.content.Planets;
import mindustry.content.SectorPresets;
import mindustry.content.SerpuloTechTree;
import mindustry.content.StatusEffects;
import mindustry.content.TeamEntries;
import mindustry.content.UnitTypes;
import mindustry.content.Weathers;
import mindustry.core.ContentLoader;
import mindustry.mod.*;
import mindustry.type.Category;
import mindustry.world.blocks.payloads.PayloadConveyor;
import mindustry.world.blocks.payloads.PayloadRouter;

public class ExamplePlugin extends Plugin{

   /**
    * @author Agzam4
    * The plugin fix bugs:
    * > router bug (v140.4)
    */
	
	@Override
	public void loadContent() {

		super.loadContent();
	}
	
    @Override
    public void init() {

        Vars.content = new ContentLoader();
        
//    	Blocks.payloadConveyor = new FixedPayloadConveyor("payload-conveyor"){{
//            requirements(Category.units, with(Items.graphite, 10, Items.copper, 10));
//            canOverdrive = false;
//        }};
        
//        Blocks.payloadRouter = new PayloadRouter("payload-router"){{
//            requirements(Category.units, with(Items.graphite, 15, Items.copper, 10));
//            canOverdrive = false;
//        }};

//        Blocks.reinforcedPayloadConveyor = new FixedPayloadConveyor("reinforced-payload-conveyor"){{
//            requirements(Category.units, with(Items.tungsten, 10));
//            moveTime = 35f;
//            canOverdrive = false;
//            health = 800;
//            researchCostMultiplier = 4f;
//            underBullets = true;
//        }};
        
//      Blocks.reinforcedPayloadRouter = new PayloadRouter("reinforced-payload-router"){{
//          requirements(Category.units, with(Items.tungsten, 15));
//          moveTime = 35f;
//          health = 800;
//          canOverdrive = false;
//          researchCostMultiplier = 4f;
//          underBullets = true;
//      }};
        
        // From ContentLoader.createBaseContent()
        TeamEntries.load();
        Items.load();
        StatusEffects.load();
        Liquids.load();
        Bullets.load();
        UnitTypes.load();
        
        FixedBlocks.load();
        Blocks.payloadConveyor = FixedBlocks.payloadConveyor;
        Blocks.payloadRouter = FixedBlocks.payloadRouter;
        Blocks.reinforcedPayloadConveyor = FixedBlocks.reinforcedPayloadConveyor;
        Blocks.reinforcedPayloadRouter = FixedBlocks.reinforcedPayloadRouter;
        
        Loadouts.load();
        Weathers.load();
        Planets.load();
        SectorPresets.load();
        SerpuloTechTree.load();
        ErekirTechTree.load();

        // From ServerLauncher.java
        Vars.mods.loadScripts();
        Vars.content.createModContent();
        Vars.content.init();

		Log.info("Content inited!");
    }
}
