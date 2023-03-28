package as2;

import org.joml.*;
import org.joml.Math;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class Rasterizer {

    static class Buffers {
        public static final int COLOR = 1;
        public static final int DEPTH = 2;
    }

    enum Primitive {
        LINE,
        TRIANGLE;
    }

    Matrix4f model;
    Matrix4f view;
    Matrix4f projection;

    Matrix4f mvp;

    int pos_id = 0;
    int ind_id = 0;

    int next_id = 0;
    int get_next_id() { return next_id++; }

    int width, height;
    int total;

    int ssaa = 0;

    Map<Integer, Vector3f[]> pos_buf = new HashMap<>();
    Map<Integer, Vector3i[]> ind_buf = new HashMap<>();
    Map<Integer, Vector3f[]> col_buf = new HashMap<>();

    Vector3f[] frame_buf;
    Vector3f[] color_buf;
    float[] depth_buf;

    public Rasterizer(int w, int h) {
        width = w;
        height = h;
        total = w * h;
        frame_buf = new Vector3f[total];
        color_buf = new Vector3f[total];
        depth_buf = new float[total];
    }

    int load_positions(Vector3f[] positions) {
        int id = get_next_id();
        pos_buf.put(id, positions);
        return id;
    }

    int load_indices(Vector3i[] indices) {
        int id = get_next_id();
        ind_buf.put(id, indices);
        return id;
    }

    int load_colors(Vector3f[] colors) {
        int id = get_next_id();
        col_buf.put(id, colors);
        return id;
    }

    void set_model(Matrix4f m) {
        model = m;
    }

    void set_view(Matrix4f v) {
        view = v;
    }

    void set_projection(Matrix4f p) {
        projection = p;
    }

    void set_mvp(Matrix4f mvp) {
        this.mvp = mvp;
    }

    Vector4f to_vec4(Vector3f v3, float w) {
        return new Vector4f(v3.x(), v3.y(), v3.z(), w);
    }

    public boolean insideTriangle(float x, float y, Vector3f[] _v) {
        boolean has_neg, has_pos;
        float d1, d2, d3;

        d1 = (x - _v[1].x()) * (_v[0].y() - _v[1].y()) - (_v[0].x() - _v[1].x()) * (y - _v[1].y());
        d2 = (x - _v[2].x()) * (_v[1].y() - _v[2].y()) - (_v[1].x() - _v[2].x()) * (y - _v[2].y());
        d3 = (x - _v[0].x()) * (_v[2].y() - _v[0].y()) - (_v[2].x() - _v[0].x()) * (y - _v[0].y());
        has_neg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        has_pos = (d1 > 0) || (d2 > 0) || (d3 > 0);

        return !(has_neg && has_pos);
    }

    public Vector3f computeBarycentric2D(float x, float y, Vector3f[] v) {
        float c1 = (x*(v[1].y() - v[2].y()) + (v[2].x() - v[1].x())*y + v[1].x()*v[2].y() - v[2].x()*v[1].y()) / (v[0].x()*(v[1].y() - v[2].y()) + (v[2].x() - v[1].x())*v[0].y() + v[1].x()*v[2].y() - v[2].x()*v[1].y());
        float c2 = (x*(v[2].y() - v[0].y()) + (v[0].x() - v[2].x())*y + v[2].x()*v[0].y() - v[0].x()*v[2].y()) / (v[1].x()*(v[2].y() - v[0].y()) + (v[0].x() - v[2].x())*v[1].y() + v[2].x()*v[0].y() - v[0].x()*v[2].y());
        float c3 = (x*(v[0].y() - v[1].y()) + (v[1].x() - v[0].x())*y + v[0].x()*v[1].y() - v[1].x()*v[0].y()) / (v[2].x()*(v[0].y() - v[1].y()) + (v[1].x() - v[0].x())*v[2].y() + v[0].x()*v[1].y() - v[1].x()*v[0].y());
        return new Vector3f(c1,c2,c3);
    }

    void draw(int pos_id, int ind_id, int col_id, Primitive type) {
        Vector3f[] buf = pos_buf.get(pos_id);
        Vector3i[] ind = ind_buf.get(ind_id);
        Vector3f[] col = col_buf.get(col_id);

        float f1 = (float) ((50 - 0.1) / 2);
        float f2 = (float) ((50 + 0.1) / 2);

//        Matrix4f mvp = projection.mul(view).mul(model);

        for (Vector3i i : ind) {
            Triangle t = new Triangle();
            Vector4f[] v = {
                    to_vec4(buf[i.x], 1.0f).mul(mvp),
                    to_vec4(buf[i.y], 1.0f).mul(mvp),
                    to_vec4(buf[i.z], 1.0f).mul(mvp)
            };
            for (Vector4f vec : v) {
                vec.div(vec.w());
            }
            for (Vector4f vec : v) {
                vec.x = 0.5f * width * (vec.x + 1);
                vec.y = 0.5f * height * (vec.y + 1);
                vec.z = vec.z * f1 + f2;
            }
            for (int j = 0; j < 3; ++j) {
                t.setVertex(j, new Vector3f(v[j].x, v[j].y, v[j].z));
            }
            Vector3f col_x = col[i.x];
            Vector3f col_y = col[i.y];
            Vector3f col_z = col[i.z];

            t.setColor(0, col_x.x, col_x.y, col_x.z);
            t.setColor(1, col_y.x, col_y.y, col_y.z);
            t.setColor(2, col_z.x, col_z.y, col_z.z);

            rasterize_triangle(t);
        }

    }

    private void rasterize_triangle(Triangle t) {
        Vector4f[] v = t.toVector4();

        float x1 = t.a().x();
        float x2 = t.b().x();
        float x3 = t.c().x();

        float y1 = t.a().y();
        float y2 = t.b().y();
        float y3 = t.c().y();

        float minx = Math.min(x1, Math.min(x2, x3));
        float miny = Math.min(y1, Math.min(y2, y3));
        float maxx = Math.max(x1, Math.max(x2, x3));
        float maxy = Math.max(y1, Math.max(y2, y3));

        if (ssaa > 0) {

        } else {
            for (int x = (int) minx; x < maxx; x++) {
                for (int y = (int) miny; y < maxy; y++) {
                    if (insideTriangle(x, y, t.v)) {
                        Vector3f tuple = computeBarycentric2D(x, y, t.v);
                        float alpha = tuple.x;
                        float beta = tuple.y;
                        float gamma = tuple.z;
                        float w_reciprocal = 1f / (alpha / v[0].w() + beta / v[1].w() + gamma / v[2].w());
                        float z_interpolated = alpha * v[0].z() / v[0].w() + beta * v[1].z() / v[1].w() + gamma * v[2].z() / v[2].w();
                        z_interpolated *= w_reciprocal;
                        int idx = get_index(x, y);
                        if (z_interpolated < depth_buf[idx]) {
                            depth_buf[idx] = z_interpolated;
                            set_pixel(new Vector3f(x, y, 0), t.getColor());
                        }
                    }
                }
            }
        }
    }

    int get_index(float x, float y) {
        return (int) ((height- 1 - y) * width + x);
    }

    void set_pixel(Vector3f point, Vector3f color) {
        int idx = get_index(point.x, point.y);
        frame_buf[idx] = color;
    }

    void clear(int buff) {
        if ((buff & Buffers.COLOR) == Buffers.COLOR) {
            Arrays.fill(frame_buf, new Vector3f(0, 0, 0));
            Arrays.fill(color_buf, new Vector3f(0, 0, 0));
        }
        if ((buff & Buffers.DEPTH) == Buffers.DEPTH) {
            Arrays.fill(depth_buf, Float.POSITIVE_INFINITY);
        }
    }

    ByteBuffer frame_buffer_data() {
        float[] data = new float[frame_buf.length * 3];
        int idx = 0;
        for(Vector3f vector : frame_buf) {
            data[idx++] = vector.x;
            data[idx++] = vector.y;
            data[idx++] = vector.z;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * Double.BYTES)
                .order(ByteOrder.nativeOrder());
        byteBuffer.asFloatBuffer().put(data);
        return byteBuffer;
    }
}
