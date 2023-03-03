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
        Matrix4d m = new Matrix4d();
        m.lookAt(eye, new Vector3d(0, 0, 0), new Vector3d(0, 1, 0));
        return m;
        /*return new Matrix4d (
                1, 0, 0, -eye.x,
                0, 1, 0, -eye.y,
                0, 0, 1, -eye.z,
                0, 0, 0, 1
        );*/
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

    public static Matrix4d getProjectionMatrix(double fov, double aspectRatio,
                                               double zNear, double zFar) {
        Matrix4d m = new Matrix4d();
        m.perspective(Math.toRadians(fov), aspectRatio, zNear, zFar);
        return m;
        /*double rad = Math.toRadians(fov);
        double t = zNear * Math.tan(rad / 2);
        double r = t * aspectRatio;
        Matrix4d projection = new Matrix4d(
                zNear, 0, 0, 0,
                0, zNear, 0, 0,
                0, 0, zFar + zNear, -zFar * zNear,
                0, 0, 1, 0
        );
        Matrix4d translate = new Matrix4d(
                1 / r, 0, 0, 0,
                0, 1 / t, 0, 0,
                0, 0, 2 / (zNear - zFar), -(zNear + zFar) / (zNear - zFar),
                0, 0, 0, 1
        );

        projection = translate.mul(projection);
        return projection;*/
    }

    public static Matrix4d getRotation(Vector3d axis, float angle) {
        Matrix4d id = new Matrix4d();
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
        // n[3] not used
        double f10 = xy * oneminusc - zs;
        double f11 = axis.y * axis.y * oneminusc + c;
        double f12 = yz * oneminusc + xs;
        // n[7] not used
        double f20 = xz * oneminusc + ys;
        double f21 = yz * oneminusc - xs;
        double f22 = axis.z * axis.z * oneminusc + c;

        double t00 = id.m00() * f00 + id.m10() * f01 + id.m20() * f02;
        double t01 = id.m01() * f00 + id.m11() * f01 + id.m21() * f02;
        double t02 = id.m02() * f00 + id.m12() * f01 + id.m22() * f02;
        double t03 = id.m03() * f00 + id.m13() * f01 + id.m23() * f02;
        double t10 = id.m00() * f10 + id.m10() * f11 + id.m20() * f12;
        double t11 = id.m01() * f10 + id.m11() * f11 + id.m21() * f12;
        double t12 = id.m02() * f10 + id.m12() * f11 + id.m22() * f12;
        double t13 = id.m03() * f10 + id.m13() * f11 + id.m23() * f12;

        id.m20(id.m00() * f20 + id.m10() * f21 + id.m20() * f22);
        id.m21(id.m01()* f20 + id.m11() * f21 + id.m21() * f22);
        id.m22(id.m02() * f20 + id.m12() * f21 + id.m22() * f22);
        id.m23(id.m03() * f20 + id.m13() * f21 + id.m23() * f22);

        id.m00(t00);
        id.m01(t01);
        id.m02(t02);
        id.m03(t03);
        id.m10(t10);
        id.m11(t11);
        id.m12(t12);
        id.m13(t13);

        return id;
    }

    public static Matrix4d getRotation1(Vector3d axis, float angle) {
        Matrix4d matrix = new Matrix4d();
        double radians = Math.toRadians(angle);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = axis.x;
        double y = axis.y;
        double z = axis.z;

        double oneminusc = 1 - cos;

        matrix.m00(cos + (oneminusc) * x * x);
        matrix.m01((oneminusc) * x * y - sin * z);
        matrix.m02((oneminusc) * x * z + sin * y);

        matrix.m10((oneminusc) * y * x + sin * z);
        matrix.m11(cos + (oneminusc) * y * y);
        matrix.m12((oneminusc) * y * z - sin * x);

        matrix.m20((oneminusc) * z * x - sin * y);
        matrix.m21((oneminusc) * z * y + sin * x);
        matrix.m22(cos + (oneminusc) * z * z);

        matrix.m33(1.0);

        return matrix;
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

            angle += 10;
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
