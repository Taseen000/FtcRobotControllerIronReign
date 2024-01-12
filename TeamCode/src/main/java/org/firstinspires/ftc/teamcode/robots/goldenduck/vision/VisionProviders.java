package org.firstinspires.ftc.teamcode.robots.goldenduck.vision;

import org.firstinspires.ftc.teamcode.robots.goldenduck.vision.VisionProvider;
import org.firstinspires.ftc.teamcode.robots.goldenduck.vision.provider.AprilTagProvider;
import org.firstinspires.ftc.teamcode.robots.goldenduck.vision.provider.GDPropDetectorProvider;
import org.firstinspires.ftc.teamcode.robots.goldenduck.vision.provider.OpenCVProvider;
import org.firstinspires.ftc.teamcode.robots.goldenduck.vision.provider.dummy.LeftDummyProvider;
import org.firstinspires.ftc.teamcode.robots.goldenduck.vision.provider.dummy.MiddleDummyProvider;
import org.firstinspires.ftc.teamcode.robots.goldenduck.vision.provider.dummy.RightDummyProvider;

public class VisionProviders {
    public static final Class<? extends VisionProvider>[] VISION_PROVIDERS =
            new Class[]{AprilTagProvider.class,  OpenCVProvider.class, GDPropDetectorProvider.class, LeftDummyProvider.class, MiddleDummyProvider.class, RightDummyProvider.class,};


    public static final int DEFAULT_PROVIDER_INDEX = 0;
}
