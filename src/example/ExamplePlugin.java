package example;

import static mindustry.type.ItemStack.with;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.OS;
import arc.util.Structs;
import example.blocks.FixedBlocks;
import example.blocks.FixedPayloadConveyor;
import mindustry.Vars;
import mindustry.ai.BaseRegistry;
import mindustry.ai.BlockIndexer;
import mindustry.ai.ControlPathfinder;
import mindustry.ai.Pathfinder;
import mindustry.ai.WaveSpawner;
import mindustry.async.AsyncCore;
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
import mindustry.core.*;
import mindustry.editor.MapEditor;
import mindustry.entities.EntityCollisions;
import mindustry.game.*;
import mindustry.game.EventType.ContentInitEvent;
import mindustry.gen.Groups;
import mindustry.graphics.CacheLayer;
import mindustry.logic.GlobalVars;
import mindustry.maps.Map;
import mindustry.maps.Maps;
import mindustry.mod.*;
import mindustry.net.BeControl;
import mindustry.type.Category;
import mindustry.ui.dialogs.LanguageDialog;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.PayloadConveyor;
import mindustry.world.blocks.payloads.PayloadRouter;
import mindustry.game.EventType.*;

import static arc.Core.settings;
import static mindustry.Vars.*;

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
    	varsInit();
    	try {
			contentCreateBaseContent();
		} catch (Exception e) {
			Log.err(e);
			e.printStackTrace();
		}
        content.createModContent();
        content.init();
        bases.load();
        Events.fire(new ServerLoadEvent());
        
        Log.info(state.hasSpawns());
    }
    
    
    private void contentCreateBaseContent() throws Exception {
        TeamEntries.load();
        Items.load();
        StatusEffects.load();
        Liquids.load();
        Bullets.load();
        UnitTypes.load();

//        Blocks.load();
        FixedBlocks.load();
        Blocks.payloadConveyor = FixedBlocks.payloadConveyor;
        Blocks.payloadRouter = FixedBlocks.payloadRouter;
        Blocks.reinforcedPayloadConveyor = FixedBlocks.reinforcedPayloadConveyor;
        Blocks.reinforcedPayloadRouter = FixedBlocks.reinforcedPayloadRouter;

        Field[] defaultFields = Blocks.class.getFields();
        Field[] fixedFields = FixedBlocks.class.getFields();
        if(fixedFields.length !=  defaultFields.length) {
			throw new Exception("default fields count != fixed fields count");
        }
        try {
        	for (int i = 0; i < fixedFields.length; i++) {
        		if(defaultFields[i].getName().equals(fixedFields[i].getName())) {
        			Object value = fixedFields[i].get(FixedBlocks.class);
        			defaultFields[i].set(Blocks.class, value);
        		} else {
        			throw new Exception("default fields not equals fixed fields");
        		}
        	}
        } catch (IllegalArgumentException | IllegalAccessException e) {
        	throw e; 
        }
        
        Loadouts.load();
        Weathers.load();
        Planets.load();
        SectorPresets.load();
        SerpuloTechTree.load();
        ErekirTechTree.load();
	}

	private void varsInit() {

        Groups.init();

        if(loadLocales){
            //load locales
            String[] stra = Core.files.internal("locales").readString().split("\n");
            locales = new Locale[stra.length];
            for(int i = 0; i < locales.length; i++){
                String code = stra[i];
                if(code.contains("_")){
                    locales[i] = new Locale(code.split("_")[0], code.split("_")[1]);
                }else{
                    locales[i] = new Locale(code);
                }
            }

            Arrays.sort(locales, Structs.comparing(LanguageDialog::getDisplayName, String.CASE_INSENSITIVE_ORDER));
            locales = Seq.with(locales).add(new Locale("router")).toArray(Locale.class);
        }

        Version.init();
        CacheLayer.init();

        if(!headless){
            Log.info("[Mindustry] Version: @", Version.buildString());
        }

        dataDirectory = settings.getDataDirectory();
        screenshotDirectory = dataDirectory.child("screenshots/");
        customMapDirectory = dataDirectory.child("maps/");
        mapPreviewDirectory = dataDirectory.child("previews/");
        saveDirectory = dataDirectory.child("saves/");
        tmpDirectory = dataDirectory.child("tmp/");
        modDirectory = dataDirectory.child("mods/");
        schematicDirectory = dataDirectory.child("schematics/");
        bebuildDirectory = dataDirectory.child("be_builds/");
        emptyMap = new Map(new StringMap());

        tree = new FileTree();
        mods = new Mods();
        
    	// . . .
    	
        content = new ContentLoader();
        
    	waves = new Waves();
        collisions = new EntityCollisions();
        world = new World();
        universe = new Universe();
        becontrol = new BeControl();
        asyncCore = new AsyncCore();
        if(!headless) editor = new MapEditor();

        maps = new Maps();
        spawner = new WaveSpawner();
        indexer = new BlockIndexer();
        pathfinder = new Pathfinder();
        controlPath = new ControlPathfinder();
        fogControl = new FogControl();
        bases = new BaseRegistry();
        logicVars = new GlobalVars();
        javaPath =
            new Fi(OS.prop("java.home")).child("bin/java").exists() ? new Fi(OS.prop("java.home")).child("bin/java").absolutePath() :
            Core.files.local("jre/bin/java").exists() ? Core.files.local("jre/bin/java").absolutePath() : // Unix
            Core.files.local("jre/bin/java.exe").exists() ? Core.files.local("jre/bin/java.exe").absolutePath() : // Windows
            "java";

        state = new GameState();

        mobile = Core.app.isMobile() || testMobile;
        ios = Core.app.isIOS();
        android = Core.app.isAndroid();

        modDirectory.mkdirs();

        Events.on(ContentInitEvent.class, e -> {
            emptyTile = new Tile(Short.MAX_VALUE - 20, Short.MAX_VALUE - 20);
        });
//
        mods.load();
        maps.load();
        
	}
}
