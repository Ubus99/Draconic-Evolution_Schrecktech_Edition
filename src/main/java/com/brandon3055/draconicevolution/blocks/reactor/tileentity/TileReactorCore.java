package com.brandon3055.draconicevolution.blocks.reactor.tileentity;

import com.brandon3055.brandonscore.blocks.TileBCBase;
import com.brandon3055.brandonscore.lib.Vec3I;
import com.brandon3055.brandonscore.network.PacketTileMessage;
import com.brandon3055.brandonscore.network.wrappers.*;
import com.brandon3055.brandonscore.utils.FacingUtils;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.GuiHandler;
import com.brandon3055.draconicevolution.blocks.reactor.ReactorEffectHandler;
import com.brandon3055.draconicevolution.utils.LogHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Created by brandon3055 on 6/11/2016.
 */
public class TileReactorCore extends TileBCBase implements ITickable {

    //region =========== Structure Fields ============

    public static final int COMPONENT_MAX_DISTANCE = 8;
    public final SyncableVec3I[] componentPositions = new SyncableVec3I[6]; //Invalid position is 0, 0, 0
    private final SyncableEnum<Axis> stabilizerAxis = new SyncableEnum<>(Axis.Y, true, false);
    public final SyncableBool structureValid = new SyncableBool(false, true, false);
    public final SyncableString structureError = new SyncableString("", true, false);
    /**
     * This controls the activation (fade in/fade out) of the beam and stabilizer animations.
     */
    public final SyncableDouble animationState = new SyncableDouble(0, true, false);

    //endregion ======================================

    //region =========== Core Logic Fields ===========

    /**
     * This is the current operational state of the reactor.
     */
    public final SyncableEnum<ReactorState> reactorState = new SyncableEnum<>(ReactorState.COLD, true, false);

    /**
     * Remaining fuel that is yet to be consumed by the reaction.
     */
    public final SyncableDouble reactableFuel = new SyncableDouble(0, false, true);
    /**
     * Fuel that has been converted to chaos by the reaction.
     */
    public final SyncableDouble convertedFuel = new SyncableDouble(0, false, true);
    public final SyncableDouble temperature = new SyncableDouble(20, false, true);
    public static final double MAX_TEMPERATURE = 10000;

    public final SyncableDouble shieldCharge = new SyncableDouble(0, false, true);
    public final SyncableDouble maxShieldCharge = new SyncableDouble(0, false, true);

    /**
     * This is how saturated the core is with energy.
     */
    public final SyncableInt saturation = new SyncableInt(0, false, true);
    public final SyncableInt maxSaturation = new SyncableInt(0, false, true);

    public double tempDrainFactor;
    public double generationRate;
    public int fieldDrain;
    public double fieldInputRate;
    public double fuelUseRate;

    public final SyncableBool startupInitialized = new SyncableBool(false, false, true);

    //endregion ======================================

    private final ReactorEffectHandler effectHandler;

    public TileReactorCore() {
        for (int i = 0; i < componentPositions.length; i++) {
            registerSyncableObject(componentPositions[i] = new SyncableVec3I(new Vec3I(0, 0, 0), true, false));
        }

        registerSyncableObject(stabilizerAxis);
        registerSyncableObject(structureValid);
        registerSyncableObject(reactorState);
        registerSyncableObject(structureError);
        registerSyncableObject(animationState);

        registerSyncableObject(reactableFuel);
        registerSyncableObject(convertedFuel);
        registerSyncableObject(temperature);
        registerSyncableObject(shieldCharge);
        registerSyncableObject(maxShieldCharge);
        registerSyncableObject(saturation);
        registerSyncableObject(maxSaturation);
        registerSyncableObject(startupInitialized);

        effectHandler = DraconicEvolution.proxy.createReactorFXHandler(this);
    }

    //region Update Logic

    @Override
    public void update() {
        detectAndSendChanges();
        animationState.value = 1;//(net.minecraft.util.math.MathHelper.sin(ClientEventHandler.elapsedTicks / 20F) + 1F) / 2F;

        updateCoreLogic();

        if (worldObj.isRemote && effectHandler != null) {
            effectHandler.updateEffects();
        }
    }

    //endregion

    //region ################# Core Logic ##################

    private void updateCoreLogic() {

        switch (reactorState.value) {
            case INVALID:
                break;
            case COLD:
                updateOfflineState();
                break;
            case WARMING_UP:
                initializeStartup();
                break;
//            case AT_TEMP:
//                break;
            case RUNNING:
                updateOnlineState();
                break;
            case SHUTDOWN:
                updateOnlineState();
                break;
            case COOLING:
                updateOfflineState();
                break;
        }

    }

    /**
     * Update the reactors offline state.
     * This is responsible for things like returning the core temperature to minimum and draining remaining charge after the reactor shuts down.
     */
    private void updateOfflineState() {
        if (temperature.value > 20) {
            temperature.value -= 0.5;
        }
        if (shieldCharge.value > 0) {
            shieldCharge.value -= maxShieldCharge.value * 0.0005;
        }
        else if (shieldCharge.value < 0) {
            shieldCharge.value = 0;
        }
        if (saturation.value > 0) {
            saturation.value -= maxSaturation.value * 0.000001D;
        }
        else if (saturation.value < 0) {
            saturation.value = 0;
        }
    }

    /**
     * This method is fired when the reactor enters the warm up state.
     * The first time this method is fired if initializes all of the reactors key fields.
     */
    private void initializeStartup() {
        if (!startupInitialized.value) {
            double totalFuel = reactableFuel.value + convertedFuel.value;
            maxShieldCharge.value = totalFuel * 96.45061728395062 * 100;
            maxSaturation.value = (int) (totalFuel * 96.45061728395062 * 1000);

            if (saturation.value > maxSaturation.value) {
                saturation.value = maxSaturation.value;
            }

            if (shieldCharge.value > maxShieldCharge.value) {
                shieldCharge.value = maxShieldCharge.value;
            }

            startupInitialized.value = true;
        }
    }

    private void updateOnlineState() {
        double coreSat = (double) saturation.value / (double) maxSaturation.value;         //1 = Max Saturation
        double negCSat = (1D - coreSat) * 99D;                                             //99 = Min Saturation. I believe this tops out at 99 because at 100 things would overflow and break.
        double temp50 = (temperature.value / MAX_TEMPERATURE) * 50;                       //50 = Max Temp. Why? TBD
        double tFuel = convertedFuel.value + reactableFuel.value;                        //Total Fuel.
        double convLVL = ((convertedFuel.value / tFuel) * 1.3D) - 0.3D;                    //Conversion Level sets how much the current conversion level boosts power gen. Range: -0.3 to 1.0

        //region ============= Temperature Calculation =============

        double tempOffset = 444.7;    //Adjusts where the temp falls to at 100% saturation

        //The exponential temperature rise which increases as the core saturation goes down
        double tempRiseExpo = (negCSat * negCSat * negCSat) / (100 - negCSat) + tempOffset; //This is just terrible... I cant believe i wrote this stuff...

        //This is used to add resistance as the temp rises because the hotter something gets the more energy it takes to get it hotter
        double tempRiseResist = (temp50 * temp50 * temp50 * temp50) / (100 - temp50);       //^ Mostly Correct... The hotter an object gets the faster it dissipates heat into its surroundings to the more energy it takes to compensate for that energy loss.

        //This puts all the numbers together and gets the value to raise or lower the temp by this tick. This is dealing with very big numbers so the result is divided by 10000
        double riseAmount = (tempRiseExpo - (tempRiseResist * (1D - convLVL)) + convLVL * 1000) / 10000;

        //Apply energy calculations.
        if (reactorState.value == ReactorState.SHUTDOWN) {
            if (temperature.value <= 2001) {
                reactorState.value = ReactorState.COOLING;
                startupInitialized.value = false;
                return;
            }
            if (saturation.value >= maxSaturation.value * 0.99D && reactableFuel.value > 0D) {
                temperature.value -= 1D - convLVL;
            }
            else {
                temperature.value += riseAmount * 10;
            }
        }
        else {
            temperature.value += riseAmount * 10;
        }

        //endregion ================================================

        //region ============= Energy Calculation =============

        int baseMaxRFt = (int) ((maxSaturation.value / 1000D) * DEConfig.reactorOutputMultiplier * 1.5D);
        int maxRFt = (int) (baseMaxRFt * (1D + (convLVL * 2)));
        generationRate = (1D - saturation.value) * maxRFt;
        saturation.value += generationRate;

        //endregion ===========================================

        //region ============= Shield Calculation =============

        fieldDrain = (int) Math.min(tempDrainFactor * (1D - convLVL) * (baseMaxRFt / 10.923556), (double) Integer.MAX_VALUE); //<(baseMaxRFt/make smaller to increase field power drain)

        double fieldNegPercent = 1D - (shieldCharge.value / maxShieldCharge.value);
        fieldInputRate = fieldDrain / fieldNegPercent;

        shieldCharge.value -= fieldDrain;

        //endregion ===========================================

        //region ============== Fuel Calculation ==============

        fuelUseRate = tempDrainFactor * (1D - convLVL) * (0.001 * DEConfig.reactorFuelUsageMultiplier); //<Last number is base fuel usage rate
        if (reactableFuel.value > 0) {
            convertedFuel.value += fuelUseRate;
            reactableFuel.value -= fuelUseRate;
        }

        //endregion ===========================================

        //region Explosion
//        if ((shieldCharge.value <= 0) && !hasExploded) {
//            hasExploded = true;
//            goBoom();
//        }
        //endregion ======
    }

    public boolean canCharge() {
        if (!worldObj.isRemote && !validateStructure()) {
            return false;
        }
        return (reactorState.value == ReactorState.COLD || reactorState.value == ReactorState.COOLING) && reactableFuel.value + convertedFuel.value >= 144;
    }

    public boolean canActivate() {
        if (!worldObj.isRemote && !validateStructure()) {
            return false;
        }
        return (reactorState.value == ReactorState.WARMING_UP || reactorState.value == ReactorState.SHUTDOWN) && temperature.value >= 2000 && saturation.value >= maxSaturation.value / 2 && shieldCharge.value >= maxShieldCharge.value / 2;
    }

    public boolean canStop() {
        return reactorState.value == ReactorState.RUNNING;
    }

    //region Notes for V2 Logic
    /*
    *
    * Calculation Order: WIP
    *
    * 1: Calculate conversion modifier
    *
    * 2: Saturation calculations
    *
    * 3: Temperature Calculations
    *
    * 4: Energy Calculations then recalculate saturation so the new value is reflected in the shield calculations
    *
    * 5: Shield Calculation
    *
    *
    */// endregion*/


    //endregion ############################################

    //region ############## User Interaction ###############

    private static final byte ID_CHARGE = 0;
    private static final byte ID_ACTIVATE = 1;
    private static final byte ID_SHUTDOWN = 2;

    public void chargeReactor() {
        if (worldObj.isRemote) {
            sendPacketToServer(new PacketTileMessage(this, (byte) 0, ID_CHARGE, false));
        }
        else if (canCharge()) {
            reactorState.value = ReactorState.WARMING_UP;
        }
    }

    public void activateReactor() {
        if (worldObj.isRemote) {
            sendPacketToServer(new PacketTileMessage(this, (byte) 0, ID_ACTIVATE, false));
        }
        else if (canActivate()) {
            reactorState.value = ReactorState.RUNNING;
        }
    }

    public void shutdownReactor() {
        if (worldObj.isRemote) {
            sendPacketToServer(new PacketTileMessage(this, (byte) 0, ID_SHUTDOWN, false));
        }
        else if (canStop()) {
            reactorState.value = ReactorState.SHUTDOWN;
        }
    }

    private boolean verifyPlayerPermission(EntityPlayer player) {
        PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, null, pos, EnumFacing.UP, player.getLookVec());
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    public void onComponentClicked(EntityPlayer player, TileReactorComponent component) {
        if (!worldObj.isRemote && verifyPlayerPermission(player)) {
            player.openGui(DraconicEvolution.instance, GuiHandler.GUIID_REACTOR, worldObj, pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @Override
    public void receivePacketFromClient(PacketTileMessage packet, EntityPlayerMP client) {
        if (!verifyPlayerPermission(client)) {
            return;
        }
        else if (packet.getIndex() == 0 && packet.byteValue == ID_CHARGE) {
            chargeReactor();
        }
        else if (packet.getIndex() == 0 && packet.byteValue == ID_ACTIVATE) {
            activateReactor();
        }
        else if (packet.getIndex() == 0 && packet.byteValue == ID_SHUTDOWN) {
            shutdownReactor();
        }
    }

    //endregion ############################################

    //region ################# Multi-block #################

    /**
     * Called when the core is poked by a reactor component.
     * If the structure is already initialized this validates the structure.
     * Otherwise it attempts to initialize the structure.
     *
     * @param component The component that poked the core.
     */
    public void pokeCore(TileReactorComponent component, EnumFacing pokeFrom) {
        LogHelper.dev("Reactor: Core Poked");
        if (structureValid.value) {
            //If the component is an unbound injector and there is no component bound on the same side then bind it.
            if (component instanceof TileReactorEnergyInjector && !component.isBound.value) {
                TileEntity tile = worldObj.getTileEntity(getOffsetPos(componentPositions[pokeFrom.getIndex()].vec));
                if (tile == this) {
                    componentPositions[pokeFrom.getIndex()].vec = getOffsetVec(component.getPos());
                    component.bindToCore(this);
                    LogHelper.dev("Reactor: Injector Added!");
                }
            }

            validateStructure();
        }
        else {
            attemptInitialization();
        }
    }

    /**
     * Called when a component is physically broken
     */
    public void componentBroken(TileReactorComponent component, EnumFacing componentSide) {
        if (!structureValid.value) {
            return;
        }

        if (component instanceof TileReactorEnergyInjector) {
            LogHelper.dev("Reactor: Component broken! (Injector)");
            TileEntity tile = worldObj.getTileEntity(getOffsetPos(componentPositions[componentSide.getIndex()].vec));

            if (tile == component || tile == null) {
                LogHelper.dev("Reactor: -Removed");
                componentPositions[componentSide.getIndex()].vec.set(0, 0, 0);
            }
        }
        else if (reactorState.value != ReactorState.COLD) {
            LogHelper.dev("Reactor: Component broken, Structure Invalidated (Unsafe!!!!)");
            //TODO Make big explosion!!!! (If the reactor was running of course)
            structureValid.value = false;
        }
        else {
            LogHelper.dev("Reactor: Component broken, Structure Invalidated (Safe)");
            structureValid.value = false;
        }
    }

    //region Initialization

    /**
     * Will will check if the structure is valid and if so will initialize the structure.
     */
    public void attemptInitialization() {
        LogHelper.dev("Reactor: Attempt Initialization");

        if (!findComponents()) {
            return;
        }

        if (!checkStabilizerAxis()) {
            return;
        }

        if (!bindComponents()) {
            return;
        }

        structureValid.value = true;
        LogHelper.dev("Reactor: Structure Successfully Initialized!\n");
    }

    /**
     * Finds all Reactor Components available to this core and
     *
     * @return true if exactly 4 stabilizers were found.
     */
    public boolean findComponents() {
        LogHelper.dev("Reactor: Find Components");
        int stabilizersFound = 0;
        for (EnumFacing facing : EnumFacing.VALUES) {
            componentPositions[facing.getIndex()].vec.set(0, 0, 0);
            for (int i = 1; i < COMPONENT_MAX_DISTANCE; i++) {
                BlockPos searchPos = pos.offset(facing, i);

                if (!worldObj.isAirBlock(searchPos)) {
                    TileEntity searchTile = worldObj.getTileEntity(searchPos);
                    LogHelper.dev("Reactor: -Found: " + searchTile);

                    if (searchTile instanceof TileReactorComponent && ((TileReactorComponent) searchTile).facing.value == facing.getOpposite() && i >= 2) {
                        componentPositions[facing.getIndex()].vec = getOffsetVec(searchPos);
                    }

                    if (searchTile instanceof TileReactorStabilizer) {
                        stabilizersFound++;
                    }

                    break;
                }
            }
        }

        return stabilizersFound == 4;
    }

    /**
     * Checks the layout of the stabilizers and sets the stabilizer axis accordingly.
     *
     * @return true if the stabilizer configuration is valid.
     */
    public boolean checkStabilizerAxis() {
        LogHelper.dev("Reactor: Check Stabilizer Axis");
        for (Axis axis : Axis.values()) {
            boolean axisValid = true;
            for (EnumFacing facing : FacingUtils.getFacingsAroundAxis(axis)) {
                TileEntity tile = worldObj.getTileEntity(getOffsetPos(componentPositions[facing.getIndex()].vec));
                //The facing check should not be needed here but does not heart to be to careful.
                if (!(tile instanceof TileReactorStabilizer && ((TileReactorStabilizer) tile).facing.value == facing.getOpposite())) {
                    axisValid = false;
                    break;
                }
            }

            if (axisValid) {
                stabilizerAxis.value = axis;
                LogHelper.dev("Reactor: -Found Valid Axis: " + axis);
                return true;
            }
        }

        return false;
    }

    /**
     * At this point we know there are at least 4 stabilizers in a valid configuration and possibly some injectors.
     * This method binds them to the core.
     *
     * @return false if failed to bind all 4 stabilizers. //Just in case...
     */
    public boolean bindComponents() {
        LogHelper.dev("Reactor: Binding Components");
        int stabilizersBound = 0;
        for (int i = 0; i < 6; i++) {
            TileEntity tile = worldObj.getTileEntity(getOffsetPos(componentPositions[i].vec));
            if (tile instanceof TileReactorComponent) {
                ((TileReactorComponent) tile).bindToCore(this);

                if (tile instanceof TileReactorStabilizer) {
                    stabilizersBound++;
                }
            }
        }

        return stabilizersBound == 4;
    }

    //endregion

    //region Structure Validation

    /**
     * Checks if the structure is still valid and carries out the appropriate action if it is not.
     */
    public boolean validateStructure() {
        LogHelper.dev("Reactor: Validate Structure");
        for (EnumFacing facing : FacingUtils.getFacingsAroundAxis(stabilizerAxis.value)) {
            BlockPos pos = getOffsetPos(componentPositions[facing.getIndex()].vec);
            if (!worldObj.getChunkFromBlockCoords(pos).isLoaded()) {
                return true;
            }

            TileEntity tile = worldObj.getTileEntity(pos);
            LogHelper.dev("Reactor: Validate Stabilizer: " + tile);
            if (!(tile instanceof TileReactorStabilizer) || !((TileReactorStabilizer) tile).getCorePos().equals(this.pos)) {
                LogHelper.dev("Reactor: Structure Validation Failed!!!");
                return false;
            }
        }

//Dont think i need to do anything with the injectors.
//        for (EnumFacing facing : FacingUtils.getAxisFaces(stabilizerAxis.value)) {
//            TileEntity tile
//        }
        LogHelper.dev("Reactor: Structure Validated!");
        return true;
    }

    //endregion

    //region Getters & Setters

    private BlockPos getOffsetPos(Vec3I vec) {
        return pos.subtract(vec.getPos());
    }

    private Vec3I getOffsetVec(BlockPos offsetPos) {
        return new Vec3I(pos.subtract(offsetPos));
    }

    public TileReactorComponent getComponent(EnumFacing facing) {
        TileEntity tile = worldObj.getTileEntity(getOffsetPos(componentPositions[facing.getIndex()].vec));

        if (tile instanceof TileReactorComponent && ((TileReactorComponent) tile).facing.value == facing.getOpposite()) {
            return (TileReactorComponent) tile;
        }

        return null;
    }

    //endregion

    //endregion ############################################

    //region ################# Other Logic ##################

    public int injectEnergy(int RF) {
        int received = 0;
        if (reactorState.value == ReactorState.WARMING_UP) {
            if (!startupInitialized.value) {
                return 0;
            }
            if (shieldCharge.value < (maxShieldCharge.value / 2)) {
                received = Math.min(RF, (int) (maxShieldCharge.value / 2) - (int) shieldCharge.value + 1);
                shieldCharge.value += received;
                if (shieldCharge.value > (maxShieldCharge.value / 2)) {
                    shieldCharge.value = (maxShieldCharge.value / 2);
                }
            }
            else if (saturation.value < (maxSaturation.value / 2)) {
                received = Math.min(RF, (maxSaturation.value / 2) - saturation.value);
                saturation.value += received;
            }
            else if (temperature.value < 2000) {
                received = RF;
                temperature.value += ((double) received / (1000D + (reactableFuel.value * 10)));
                if (temperature.value > 2500) {
                    temperature.value = 2500;
                }
            }
        }
        else if (reactorState.value == ReactorState.RUNNING || reactorState.value == ReactorState.SHUTDOWN) {
            shieldCharge.value += (RF * (1D - (shieldCharge.value / maxShieldCharge.value)));
            if (shieldCharge.value > maxShieldCharge.value) {
                shieldCharge.value = maxShieldCharge.value;
            }
            return RF;
        }
        return received;
    }

    //endregion #############################################

    //region Rendering

    @Override
    public boolean shouldRenderInPass(int pass) {
        return true;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 40960.0D;
    }

    //endregion

    public enum ReactorState {
        INVALID(false), /**
         * The reactor is offline and cold.
         * In this state it is possible to add/remove fuel.
         */
        COLD(false), /**
         * Reactor is heating up in preparation for startup.
         */
        WARMING_UP(true),
        //AT_TEMP(true), Dont think i need this i can just have a "Can Start" check that checks the reactor is in the warm up state and temp is at minimum required startup temp.
        /**
         * Reactor is online.
         */
        RUNNING(true), /**
         * The reactor is shutting down..
         */
        SHUTDOWN(true), /**
         * The reactor is offline but is still cooling down.
         */
        COOLING(false);

        private final boolean reactorActive;

        /**
         * @param reactorActive Indicates that the reactor is in any state other that COLD or INVALID. If it is active in any way this is true.
         */
        ReactorState(boolean reactorActive) {
            this.reactorActive = reactorActive;
        }

        public boolean isReactorActive() {
            return reactorActive;
        }
    }
}