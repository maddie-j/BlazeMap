package com.eerussianguy.blazemap.feature.waypoints.service;

import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.lib.InheritedBoolean;

public class LocalState {
    public static final LocalState ROOT = new RootLocalState();

    private LocalState parent;

    private InheritedBoolean mapVisibility = InheritedBoolean.DEFAULT;
    private InheritedBoolean inWorldVisibility = InheritedBoolean.DEFAULT;
    private InheritedBoolean beamVisibility = InheritedBoolean.DEFAULT;
    private InheritedBoolean labelVisibility = InheritedBoolean.DEFAULT;

    public LocalState() {
        this(ROOT);
    }

    public LocalState(LocalState parent) {
        this.parent = parent;
    }

    public LocalState(LocalState parent, byte serializedState) {
        this.parent = parent;
        deserializeVisibilityState(serializedState);
    }

    public void setParent(LocalState parent) {
        this.parent = parent;
    }

    // TODO: Stub
    public byte serializeVisibilityState() {
        // Stubbed to match previous map-only behaviour
        return (byte)mapVisibility.ordinal();
        // -----

        // Pack the 4 visibility state variables into a byte, 2 bits per var
        // return 0x00;
    }

    // TODO: Stub
    public void deserializeVisibilityState(byte state) {
        // Stubbed to match previous map-only behaviour
        this.mapVisibility = InheritedBoolean.values()[state];
        // -----

        // Unpack byte
        return;
    }


    public InheritedBoolean getMapVisibility() {
        return mapVisibility;
    }

    public InheritedBoolean getInWorldVisibility() {
        return inWorldVisibility;
    }

    public InheritedBoolean getBeamVisibility() {
        return beamVisibility;
    }

    public InheritedBoolean getLabelVisibility() {
        return labelVisibility;
    }


    public void setMapVisibility(InheritedBoolean visibility) {
        this.mapVisibility = visibility;
    }

    public void setInWorldVisibility(InheritedBoolean visibility) {
        this.inWorldVisibility = visibility;
    }

    public void setBeamVisibility(InheritedBoolean visibility) {
        // If setting the beam to false while only the beam is visible, toggle off the in-world rendering instead.
        // Otherwise, confusion when in-world rendering on but nothing visible, + returning to prev in-world state
        // when turning back on again.
        if (visibility == InheritedBoolean.FALSE && this.labelVisibility == InheritedBoolean.FALSE) {
            this.inWorldVisibility = InheritedBoolean.FALSE;
            return;
        }

        this.beamVisibility = visibility;
    }

    // TODO: Verify this logic works with cycling state buttons
    public void setLabelVisibility(InheritedBoolean visibility) {
        // See setBeamVisibility. Same logic, just for label instead of beam.
        if (visibility == InheritedBoolean.FALSE && this.beamVisibility == InheritedBoolean.FALSE) {
            this.inWorldVisibility = InheritedBoolean.FALSE;
            return;
        }

        this.labelVisibility = visibility;
    }


    public boolean isMapVisible() {
        return mapVisibility.getOrInherit(parent::isMapVisible);
    }

    public boolean isInWorldVisible() {
        return inWorldVisibility.getOrInherit(parent::isInWorldVisible);
    }

    public boolean isBeamVisible() {
        return inWorldVisibility.getOrInherit(parent::isInWorldVisible) && beamVisibility.getOrInherit(parent::isBeamVisible);
    }

    public boolean isLabelVisible() {
        return inWorldVisibility.getOrInherit(parent::isInWorldVisible) && labelVisibility.getOrInherit(parent::isLabelVisible);
    }

    // =================================================================================================================
    private static final class RootLocalState extends LocalState {
        private RootLocalState() {
            super(null);
        }

        @Override
        public InheritedBoolean getMapVisibility() {
            return InheritedBoolean.of(BlazeMapConfig.CLIENT.clientFeatures.displayWaypointsOnMap.get());
        }

        @Override
        public InheritedBoolean getInWorldVisibility() {
            return InheritedBoolean.of(BlazeMapConfig.CLIENT.clientFeatures.renderWaypointsInWorld.get());
        }

        @Override
        public InheritedBoolean getBeamVisibility() {
            return InheritedBoolean.of(BlazeMapConfig.CLIENT.clientFeatures.renderWaypointsInWorld.get());
        }

        @Override
        public InheritedBoolean getLabelVisibility() {
            return InheritedBoolean.of(BlazeMapConfig.CLIENT.clientFeatures.renderWaypointsInWorld.get());
        }


        @Override
        public boolean isMapVisible() {
            return getMapVisibility().getOrThrow();
        }

        @Override
        public boolean isInWorldVisible() {
            return getInWorldVisibility().getOrThrow();
        }

        @Override
        public boolean isBeamVisible() {
            return getInWorldVisibility().getOrThrow() && getBeamVisibility().getOrThrow();
        }

        @Override
        public boolean isLabelVisible() {
            return getInWorldVisibility().getOrThrow() && getLabelVisibility().getOrThrow();
        }
    }
}
