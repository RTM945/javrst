import nu.pattern.OpenCV;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_32FC3;
import static org.opencv.core.CvType.CV_8UC3;

public class Main {

    public static Matrix4d getViewMatrix(Vector3d eye) {
        /*Matrix4d m = new Matrix4d();
        m.lookAt(eye, new Vector3d(0, 0, 0), new Vector3d(0, 1, 0));
        return m;*/
        return new Matrix4d (
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                -eye.x, -eye.y, -eye.z, 1 // joml的矩阵需要转置
        );
    }

    public Matrix4d getModelMatrix(double angle) {
        double rad = Math.toRadians(angle);
        return new Matrix4d(
                Math.cos(rad), -Math.sin(rad), 0, 0,
                Math.sin(rad), Math.cos(rad), 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        );
    }

    public static Matrix4d getProjectionMatrix(double fov, double aspectRatio, double near, double far) {
        double tanHalfFOV = Math.tan(Math.toRadians(fov) / 2.0);
        double zRange = far - near;

        return new Matrix4d(
                1 / (aspectRatio * tanHalfFOV), 0, 0, 0,
                0, 1 / tanHalfFOV, 0, 0,
                0, 0, -(far + near) / zRange, -1,
                0, 0, -(2 * near * far / zRange), 0
        );
        /*Matrix4d m = new Matrix4d();
        m.perspective(Math.toRadians(fov), aspectRatio, near, far);
        return m;*/
    }

    public static Matrix4d getRotation(Vector3d axis, float angle) {
        double rad = Math.toRadians(angle);
        double c = Math.cos(rad);
        double s = Math.sin(rad);
        double oneminusc = 1.0 - c;
        double xy = axis.x * axis.y;
        double yz = axis.y * axis.z;
        double xz = axis.x * axis.z;
        double xs = axis.x * s;
        double ys = axis.y * s;
        double zs = axis.z * s;
        double f00 = axis.x * axis.x * oneminusc + c;
        double f01 = xy * oneminusc + zs;
        double f02 = xz * oneminusc - ys;
        double f10 = xy * oneminusc - zs;
        double f11 = axis.y * axis.y * oneminusc + c;
        double f12 = yz * oneminusc + xs;
        double f20 = xz * oneminusc + ys;
        double f21 = yz * oneminusc - xs;
        double f22 = axis.z * axis.z * oneminusc + c;

        return new Matrix4d (
                f00, f01, f02, 0,
                f10, f11, f12, 0,
                f20, f21, f22, 0,
                0,   0,   0,   1);
    }

    static ByteBuffer vectors2data(List<Vector3d> vectorList) {
        float[] data = new float[vectorList.size() * 3];
        int idx = 0;
        for(Vector3d vector : vectorList) {
            data[idx++] = (float) vector.x;
            data[idx++] = (float) vector.y;
            data[idx++] = (float) vector.z;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * Double.BYTES)
                .order(ByteOrder.nativeOrder());
        byteBuffer.asFloatBuffer().put(data);
        return byteBuffer;
    }

    public static void main(String[] args) throws Exception {

        OpenCV.loadLocally();

        float angle = 0;
        float theta = 0;
        float phi = 0;

        Rasterizer r = new Rasterizer(700, 700);
        Vector3d eye_pos = new Vector3d(0, 0, 5);
        List<Vector3d> pos = new ArrayList<>();
        pos.add(new Vector3d(2, 0, -2));
        pos.add(new Vector3d(0, 2, -2));
        pos.add(new Vector3d(-2, 0, -2));
        List<Vector3i> indices = new ArrayList<>();
        indices.add(new Vector3i(0, 1, 2));
        int pos_id = r.loadPositions(pos);
        int ind_id = r.loadIndices(indices);

        int key = 0;
        int frame_count = 0;
        Vector3d axis = new Vector3d(0, 0, 1);

        HighGui.namedWindow("javrst");
        Mat image = null;
        while (key != 27) {
            r.clear(Rasterizer.Buffers.COLOR | Rasterizer.Buffers.DEPTH);
            r.setModel(getRotation(axis, angle));
            r.setView(getViewMatrix(eye_pos));
            r.setProjection(getProjectionMatrix(45, 1, -0.1, -50));

            r.draw(pos_id, ind_id, Rasterizer.Primitive.TRIANGLE);
            List<Vector3d> vectorList = r.frameBuffer();

            image = new Mat(700, 700, CV_32FC3, vectors2data(vectorList));
            image.convertTo(image, CV_8UC3, 1.0f);
            HighGui.imshow("javrst", image);
            key = HighGui.waitKey(10);
            System.out.println("frame count: " + frame_count++ + "\n");

//            angle += 10;
//            theta += 10;
//            phi += 10;
            /*axis.x = Math.sin(Math.toRadians(theta)) * Math.cos(Math.toRadians(phi));
            axis.y = Math.sin(Math.toRadians(theta)) * Math.sin(Math.toRadians(phi));
            axis.z = Math.cos(Math.toRadians(phi));*/
        }
        image.release();

        /*Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("javrst");
        shell.setLayout(new FillLayout());
        shell.setSize(640, 480);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();*/
    }
}
