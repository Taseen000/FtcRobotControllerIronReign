package org.firstinspires.ftc.teamcode.robots.csbot;
import static org.firstinspires.ftc.teamcode.robots.csbot.CenterStage_6832.alliance;
import static org.firstinspires.ftc.teamcode.robots.csbot.CenterStage_6832.robot;
import static org.firstinspires.ftc.teamcode.robots.csbot.util.Constants.FIELD_INCHES_PER_GRID;
import static org.firstinspires.ftc.teamcode.robots.csbot.util.Utils.P2D;
import static org.firstinspires.ftc.teamcode.robots.r2v2.util.Utils.withinError;
import static org.firstinspires.ftc.teamcode.robots.r2v2.util.Utils.wrapAngle;

import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.*;

import org.firstinspires.ftc.teamcode.robots.csbot.subsystem.Robot;

import java.lang.Math;
import java.util.*;

public class Field {

    public static final double FIELD_INCHES_PER_GRID = 23.5;
    public boolean finalized = false;
    List<Zone> zones;
    List<SubZone> subZones;


    //indexes correspond to red route numbering
    public List<CrossingRoute> crossingRoutes = new ArrayList<>();

    POI HANG;
    POI HANG_PREP;
    POI WING_INTAKE;
    POI SCORE;

    POI APRILTAG1, APRILTAG2, APRILTAG3, APRILTAG4, APRILTAG5, APRILTAG6;


    //all values are in field grids
    public static final double MAX_Y_VALUE = 3;
    public static final double MAX_X_VALUE = 3;
    public static final double MIN_X_VALUE = -3;
    public static final double MIN_Y_VALUE = -3;

    public static final int STANDARD_HEADING = 180;



    public enum Zone {
        AUDIENCE(Field.MIN_X_VALUE, -1.5, Field.MIN_Y_VALUE, Field.MAX_Y_VALUE, "AUDIENCE"),
        BACKSTAGE(.501, Field.MAX_X_VALUE, Field.MIN_Y_VALUE, Field.MAX_Y_VALUE, "BACKSTAGE"),
        RIGGING(-1.5, .501, Field.MIN_Y_VALUE, Field.MAX_Y_VALUE, "RIGGING");

        public double x1;
        public double x2;
        public double y1;
        public double y2;
        public final String name;

        Zone(double x1, double x2, double y1, double y2, String name) {
            this.name = name;
            this.x1 = x1;
            this.x2 = x2;
            this.y1 = y1;
            this.y2 = y2;
        }

        public static ArrayList<Zone> getNamedZones() {
            ArrayList<Zone> temp = new ArrayList<>();
            temp.addAll(Arrays.asList(RIGGING, BACKSTAGE, AUDIENCE));
            return temp;
        }

        public boolean withinZone(Pose2d pose) {
            List<Double> xVals = Arrays.asList(pose.position.x/FIELD_INCHES_PER_GRID, x1, x2);
            List<Double> yVals = Arrays.asList(pose.position.y/FIELD_INCHES_PER_GRID, y1, y2);

            if(
                //if the pose is not an extrema of the list, it's within the bounds of the list
                    !(
                            Collections.min(xVals) == pose.position.x/FIELD_INCHES_PER_GRID || Collections.max(xVals) == (pose.position.x/FIELD_INCHES_PER_GRID) ||
                                    Collections.min(yVals) == (pose.position.y/FIELD_INCHES_PER_GRID) || Collections.max(yVals) == (pose.position.y/FIELD_INCHES_PER_GRID)
                    )
            )
            {
                return true;
            }
            return false;
        }
    }

    public boolean isRed = true;

        public void init_loop() {
        isRed = alliance.getMod();
    }
    public void finalizeField() {
        finalized = true;
        zones = Zone.getNamedZones();
        subZones = SubZone.getNamedSubZones(isRed);
        int allianceMultiplier = isRed? 1 : -1;
        crossingRoutes.add(new CrossingRoute(P2D(-1.5, -2.5 * allianceMultiplier, STANDARD_HEADING), P2D(.5, -2.5 * allianceMultiplier, STANDARD_HEADING), (isRed? 0: 6), this));
        crossingRoutes.add(new CrossingRoute(P2D(-1.5, -1.5 * allianceMultiplier, STANDARD_HEADING), P2D(.5, -1.5  * allianceMultiplier, STANDARD_HEADING), (isRed? 1: 5), this));
        crossingRoutes.add(new CrossingRoute(P2D(-1.5,  -.5 * allianceMultiplier, STANDARD_HEADING), P2D(.5,  -.5* allianceMultiplier, STANDARD_HEADING), (isRed? 2: 4), this));
//          DIAGONAL ROUTE
        crossingRoutes.add(new CrossingRoute(P2D(-1.5, .5 * allianceMultiplier, isRed? 153.434948823 : 206.565051177), P2D(.5, -.5 * allianceMultiplier, isRed? 153.434948823 : 206.565051177), 3, this));
//
        crossingRoutes.add(new CrossingRoute(P2D(-1.5, .5 * allianceMultiplier, STANDARD_HEADING), P2D(.5, .5 * allianceMultiplier, STANDARD_HEADING), (isRed? 4: 2), this));
        crossingRoutes.add(new CrossingRoute(P2D(-1.5, 1.5 * allianceMultiplier, STANDARD_HEADING), P2D(.5, 1.5 * allianceMultiplier, STANDARD_HEADING), (isRed? 5: 1), this));
        crossingRoutes.add(new CrossingRoute(P2D(-1.5, 2.5 * allianceMultiplier, STANDARD_HEADING), P2D(.5, 2.5 * allianceMultiplier, STANDARD_HEADING), (isRed? 6: 0), this));


        HANG = isRed? POI.HANG : POI.HANG.flipOnX();
        HANG_PREP = isRed? POI.HANG_PREP : POI.HANG_PREP.flipOnX();
        WING_INTAKE = isRed? POI.WING_INTAKE : POI.WING_INTAKE.flipOnX();
        SCORE = isRed? POI.SCORE : POI.SCORE.flipOnX();
        APRILTAG1 = POI.APRILTAG1;
        APRILTAG2 = POI.APRILTAG2;
        APRILTAG3 = POI.APRILTAG3;
        APRILTAG4 = POI.APRILTAG4;
        APRILTAG5 = POI.APRILTAG5;
        APRILTAG6 = POI.APRILTAG6;
    }

    public void update(TelemetryPacket packet, Robot robot) {
        //handling dashboard fieldOverlay
        Zone zone = getZone(robot.driveTrain.pose);
        Canvas c = packet.fieldOverlay();
        if (zone != null) {
            double zoneX = Math.min(zone.x1, zone.x2) * FIELD_INCHES_PER_GRID;
            double zoneY = Math.min(zone.y1, zone.y2) * FIELD_INCHES_PER_GRID;
            double zoneHeight = Math.max(zone.x1, zone.x2) * FIELD_INCHES_PER_GRID - zoneX;
            double zoneWidth = Math.max(zone.y1, zone.y2) * FIELD_INCHES_PER_GRID - zoneY;
            if (System.currentTimeMillis()/1000 % 2 == 0) {
                c.setFill("green");
                c.fillRect(zoneX, zoneY, zoneHeight, zoneWidth);

            }
        }
    }

    public SequentialAction pathToPOI(Pose2d robotPosition, Robot robot, POI poi, int preferredRouteIndex){

        Zone startZone = getZone(robotPosition);
        Zone endZone = poi.getZone();
        //robot does nothing if startzone is in rigging
        if(startZone == Zone.RIGGING)
            return new SequentialAction();

        TrajectoryActionBuilder actionBuilder = robot.driveTrain.actionBuilder(robotPosition);

        boolean needsCrossingRoute = !startZone.equals(endZone);

        boolean reverseSplines = startZone.equals(Zone.AUDIENCE);


        if (needsCrossingRoute) {
            CrossingRoute preferredRoute = crossingRoutes.get(preferredRouteIndex);
            //spline to CrossingRoute
            actionBuilder =
                    actionBuilder
                            .setReversed(reverseSplines)
                            .splineTo(preferredRoute.getEntryPose(robotPosition).position, preferredRoute.ROUTE_HEADING_RAD + (reverseSplines ? Math.PI : 0));

            //run preferredRoute
            actionBuilder =
                    preferredRoute.addToPath(actionBuilder, robotPosition);
        }
        //spline to destination
        actionBuilder =
                actionBuilder
                        .setReversed(reverseSplines)
                        .splineToSplineHeading(poi.getPose(), poi.getPose().heading);

        return new SequentialAction(
                actionBuilder.build()
        );
    }

    public Pose2d getAprilTagPose(int id) {
        switch(id){
            case 1:
                return APRILTAG1.pose;
            case 2:
                return APRILTAG2.pose;
            case 3:
                return APRILTAG3.pose;
            case 4:
                return APRILTAG4.pose;
            case 5:
                return APRILTAG5.pose;
            case 6:
                return APRILTAG6.pose;
        }
        return null;
    }

    public Zone getZone(Pose2d robotPosition) {
        for(Zone k : zones) {
            if(k.withinZone(robotPosition))
                return k;
        }
        return null;
    }
    public static double wrapAngle(double angle) {
        return ((angle % 360) + 360) % 360;
    }

    //get SubZones at robotPosition
    public ArrayList<SubZone> getSubZones(Pose2d robotPosition) {
        ArrayList<SubZone> temp = new ArrayList<>();
        for(SubZone k : subZones) {
            if(k.withinSubZone(robotPosition))
                temp.add(k);
        }
        return temp;
    }


    //get POIs at robotPosition
    public POI getPOI(Pose2d robotPosition) {
        List<POI> temp = Arrays.asList(HANG, HANG_PREP, SCORE, WING_INTAKE);
        for(POI poi : temp) {
            if(poi.atPOI(robotPosition))
                return poi;
        }
        return null;
    }

    public static Pose2d P2D (double x, double y, double deg) {
        return new Pose2d(x * FIELD_INCHES_PER_GRID, y * FIELD_INCHES_PER_GRID, Math.toRadians(deg));
    }
}

class CrossingRoute {
    Pose2d audienceSide;
    Pose2d backstageSide;
    double ROUTE_HEADING_RAD;
    int index;
    Field field;

    public CrossingRoute(Pose2d audienceSide, Pose2d backstageSide, int index, Field field){
        this.audienceSide = audienceSide;
        this.backstageSide = backstageSide;
        //should be the same for both poses because crossing routes are linear
        ROUTE_HEADING_RAD = audienceSide.heading.log();
        this.index = index;
        this.field = field;
    }

    //return the shortest distance between a given pose and either endpoint in field grids
    public double distanceToRoute(Pose2d pose) {
        double distanceToAudienceSide = Math.hypot(Math.abs(pose.position.x - audienceSide.position.x), Math.abs(pose.position.y - audienceSide.position.x)) / FIELD_INCHES_PER_GRID;
        double distanceToBackstageSide = Math.hypot(Math.abs(pose.position.x - backstageSide.position.x), Math.abs(pose.position.y - backstageSide.position.x)) / FIELD_INCHES_PER_GRID;
        return distanceToBackstageSide > distanceToAudienceSide?  distanceToAudienceSide : distanceToBackstageSide;
    }

    //return the closest endpoint to a given pose
    public Pose2d getEntryPose(Pose2d pose) {
        if(field.getZone(pose).equals(Field.Zone.AUDIENCE))
            return audienceSide;
        return backstageSide;
    }

    //returns the farthest endpoint to a given pose
    public Pose2d getExitPose(Pose2d pose) {
        if(field.getZone(pose).equals(Field.Zone.AUDIENCE))
            return backstageSide;
        return audienceSide;
    }

    public int getIndex() {
        return index;
    }

    //assumes robot is at the start of a crossing route and adds the whole route path to the actionBuilder
    public TrajectoryActionBuilder addToPath(TrajectoryActionBuilder actionBuilder, Pose2d startPose) {
        actionBuilder = actionBuilder.strafeTo(getExitPose(startPose).position);
        return actionBuilder;
    }

    public static boolean withinError(double value, double target, double error){
        return (Math.abs(target-value) <= error);
    }

}







