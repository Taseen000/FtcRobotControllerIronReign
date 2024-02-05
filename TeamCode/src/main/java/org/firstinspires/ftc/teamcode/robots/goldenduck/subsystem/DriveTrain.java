package org.firstinspires.ftc.teamcode.robots.goldenduck.subsystem;


import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.Rotation2d;
import com.acmerobotics.roadrunner.Vector2d;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDCoefficients;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.robots.csbot.rr_stuff.MecanumDrive;
import org.firstinspires.ftc.teamcode.robots.csbot.util.Constants;
import org.firstinspires.ftc.teamcode.robots.goldenduck.subsystem.Robot;
import org.firstinspires.ftc.teamcode.robots.goldenduck.subsystem.Subsystem;
import org.firstinspires.ftc.teamcode.util.PIDController;

import java.util.HashMap;
import java.util.Map;

import static org.firstinspires.ftc.teamcode.robots.csbot.CenterStage_6832.alliance;
import static org.firstinspires.ftc.teamcode.robots.csbot.util.Utils.wrapAngle;

@Config(value = "GD_DRIVETRAIN")
public class DriveTrain extends MecanumDrive implements Subsystem {
    public Robot robot;
    public boolean trajectoryIsActive;

    public Articulation articulation;

    public double imuRoadrunnerError;
    public double imuAngle;
    private double targetHeading, targetVelocity = 0;
    public static PIDController headingPID;
    public static PIDCoefficients HEADING_PID_PWR = new PIDCoefficients(0, .05, 0);
    public static double HEADING_PID_TOLERANCE = .05; //this is a percentage of the input range .063 of 2PI is 1 degree
    public double PIDCorrection, PIDError;
    public static int turnToTest = 0;
    public static double turnToSpeed=.4; //max angular speed for turn
    public boolean isHumanIsDriving() {
        return humanIsDriving;
    }

    public void setHumanIsDriving(boolean humanIsDriving) {
        this.humanIsDriving = humanIsDriving;
    }

    private boolean humanIsDriving=false;
    public enum Articulation{
        BACKSTAGE_DRIVE,
        WING_DRIVE,
    }

    public void YellowBack () {
        drive(0,10,0);
    }

    public DriveTrain(HardwareMap hardwareMap, Robot robot, boolean simulated) {
        super(hardwareMap, new Pose2d(0, 0, 0));
        this.robot = robot;
        trajectoryIsActive = false;

        headingPID = new PIDController(HEADING_PID_PWR);
        headingPID.setInputRange(0, 360);
        headingPID.setOutputRange(-1, 1);
        headingPID.setIntegralCutIn(10);
        headingPID.setContinuous(true);
        headingPID.setTolerance(HEADING_PID_TOLERANCE);
        headingPID.enable();
    }
    //end constructor

    @Override
    public void update(Canvas c) {
        imuAngle = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES) + (alliance.getMod()? -90 : 90);
        imuRoadrunnerError = imuAngle - Math.toDegrees(pose.heading.log());
        updatePoseEstimate();
        //test imu based turning from dashboard - todo comment out when not needed
        if (turnToTest!=0) turnUntilDegreesIMU(turnToTest,turnToSpeed); //can target any angle but zero
    }

    public void drive(double x, double y, double theta) {
        trajectoryIsActive = false;
        setDrivePowers(new PoseVelocity2d(
                new Vector2d(
                        -y,
                        -x
                ),
                theta
        ));
        updatePoseEstimate();
    }

    public void fieldOrientedDrive(double x, double y, double theta, boolean isRed){
        // Create a vector from the gamepad x/y inputs
        // Then, rotate that vector by the inverse of that heading
        Vector2d input = new Vector2d(
                x,
               y);
        //additional rotation needed if blue alliance perspective
        Rotation2d heading =(!isRed) ? pose.heading: pose.heading.plus(Math.PI);
        input = heading.inverse().times(
                new Vector2d(-input.x, input.y));
        setDrivePowers(new PoseVelocity2d(input,-theta ));
        updatePoseEstimate();
    }

    //request a turn in degrees units
    //this is an absolute (non-relative) implementation.
    //it's not relative to where you started.
    //the direction of the turn will favor the shortest approach
    public boolean turnUntilDegreesIMU(double turnAngle, double maxSpeed) {
        targetHeading = wrapAngle(turnAngle);
        headingPID.setPID(HEADING_PID_PWR);
        headingPID.setInput(imuAngle);
        headingPID.setSetpoint(targetHeading);
        headingPID.setOutputRange(-maxSpeed, maxSpeed);
        headingPID.setTolerance(HEADING_PID_TOLERANCE);
        double correction = headingPID.performPID();
        PIDCorrection = correction;
        PIDError = headingPID.getError();
        setHumanIsDriving(false);
        if(headingPID.onTarget()){
            //turn meets accuracy target
            //todo is this a good time to update pose heading from imu?
            //stop
            setDrivePowers(new PoseVelocity2d(new Vector2d(0, 0), 0));
            return true;
        }else{
            headingPID.enable();
            setDrivePowers(new PoseVelocity2d(new Vector2d(0, 0), correction));
            return false;
        }
    }
    public void setPose(Constants.Position start) {
        pose = start.getPose();
    }

    public void setPose(Pose2d pose) {
        this.pose = pose;
    }

    @Override
    public void stop() {
        drive(0, 0, 0);
    }

    @Override
    public Map<String, Object> getTelemetry(boolean debug) {

        Map<String, Object> telemetryMap = new HashMap<>();
        telemetryMap.put("x in fieldCoords", pose.position.x / Constants.FIELD_INCHES_PER_GRID);
        telemetryMap.put("y in fieldCoords", pose.position.y / Constants.FIELD_INCHES_PER_GRID);
        telemetryMap.put("x in inches", pose.position.x);
        telemetryMap.put("y in inches", pose.position.y);
        telemetryMap.put("heading", Math.toDegrees(pose.heading.log()));
        telemetryMap.put("Left Odometry Pod:\t", leftFront.getCurrentPosition());
        telemetryMap.put("Right Odometry Pod:\t", rightFront.getCurrentPosition());
        telemetryMap.put("Cross Odometry Pod:\t", rightBack.getCurrentPosition());
        telemetryMap.put("imu vs roadrunner:\t", imuRoadrunnerError);
        telemetryMap.put("imu:\t", imuAngle);
        telemetryMap.put("PID Error:\t", PIDError);
        telemetryMap.put("PID Correction:\t", PIDCorrection);
        telemetryMap.put("Left Front Motor Power", leftFront.getPower());
        telemetryMap.put("Left Back Motor Power", leftBack.getPower());
        telemetryMap.put("Right Front Motor Power", rightFront.getPower());
        telemetryMap.put("Right Back Motor Power", rightBack.getPower());
        return telemetryMap;
    }

    @Override
    public String getTelemetryName() {
        return "DRIVETRAIN";
    }
}