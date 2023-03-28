package as2;

import nu.pattern.OpenCV;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;

import static org.opencv.core.CvType.CV_32FC3;
import static org.opencv.core.CvType.CV_8UC3;

public class Main {

    public static Matrix4f get_view_matrix(Vector3f eye) {
        return new Matrix4f()
                .lookAt(eye, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
    }

    public static Matrix4f get_model_matrix(double angle) {
        return new Matrix4f();
    }

    public static Matrix4f get_projection_matrix(float fov, float aspectRatio, float near, float far) {
        return new Matrix4f()
                .perspective((float) Math.toRadians(fov), aspectRatio, near, far);
    }

    public static void main(String[] args) {

        OpenCV.loadLocally();

        Rasterizer r = new Rasterizer(700, 700);

        Vector3f eye_pos = new Vector3f(0, 0, 5);

        Vector3f[] pos = {
                new Vector3f(2, 0, -2),
                new Vector3f(0, 2, -2),
                new Vector3f(-2, 0, -2),
                new Vector3f(3.5f, -1, -5),
                new Vector3f(2.5f, 1.5f, -5),
                new Vector3f(-1, 0.5f, -5)
        };

        Vector3i[] ind = {
                new Vector3i(0, 1, 2),
                new Vector3i(3, 4, 5)
        };

        Vector3f[] cols = {
                new Vector3f(217f, 238f, 185f),
                new Vector3f(217f, 238f, 185f),
                new Vector3f(217f, 238f, 185f),
                new Vector3f(185f, 217f, 238f),
                new Vector3f(185f, 217f, 238f),
                new Vector3f(185f, 217f, 238f)
        };

        int pos_id = r.load_positions(pos);
        int ind_id = r.load_indices(ind);
        int col_id = r.load_colors(cols);

        int key = 0;
        int frame_count = 0;

        float angle = 0;

        HighGui.namedWindow("javrst");
        Mat image = null;

        while(key != 27) {
            r.clear(Rasterizer.Buffers.COLOR | Rasterizer.Buffers.DEPTH);

            /*r.set_model(get_model_matrix(angle));
            r.set_view(get_view_matrix(eye_pos));
            r.set_projection(get_projection_matrix(45, 1, 0.1f, 50));*/

            r.set_mvp(
                    new Matrix4f()
                    .perspectiveLH((float) Math.toRadians(45.0f), 1.0f, 0.1f, 50.0f)
                    .lookAtLH(0.0f, 0.0f, 5.0f,
                            0.0f, 0.0f, 0.0f,
                            0.0f, 1.0f, 0.0f)
            );

            r.draw(pos_id, ind_id, col_id, Rasterizer.Primitive.TRIANGLE);

            image = new Mat(700, 700, CV_32FC3, r.frame_buffer_data());
            image.convertTo(image, CV_8UC3, 1.0f);
            HighGui.imshow("javrst", image);

            System.out.println("frame count: " + frame_count++ + "\n");
            key = HighGui.waitKey(10);
        }
        image.release();
    }
}
