package epidemiology;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Recovered {

    private Grid<Object> grid;
    private ContinuousSpace<Object> space;

    public Recovered(ContinuousSpace<Object> space, Grid<Object> grid) {
        this.space = space;
        this.grid = grid;
    }

    // No need for a recover method here, as recovery is handled in the Infected class
}