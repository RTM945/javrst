import org.joml.*;

import java.lang.Math;
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

    Matrix4d model;
    Matrix4d view;
    Matrix4d projection;

    Map<Integer, List<Vector3d>> posBuf = new HashMap<>();
    Map<Integer, List<Vector3i>> indBuf = new HashMap<>();

    List<Vector3d> frameBuf;
    List<Double> depthBuf;


    int posBufId;
    int indBufId;

    int nextId = 0;

    int getNextId() {
        return nextId++;
    }

    int width, height;
    int total;

    public Rasterizer(int w, int h) {
        width = w;
        height = h;
        total = w * h;
        frameBuf = new ArrayList<>(total);
        depthBuf = new ArrayList<>(total);
    }

    int loadPositions(List<Vector3d> positions) {
        int id = getNextId();
        posBuf.put(id, positions);
        return id;
    }

    int loadIndices(List<Vector3i> indices) {
        int id = getNextId();
        indBuf.put(id, indices);
        return id;
    }

    void setModel(Matrix4d m) {
        model = m;
    }

    void setView(Matrix4d v) {
        view = v;
    }

    void setProjection(Matrix4d p) {
        projection = p;
    }

    void setPixel(Vector2i point, Vector3d color) {
        if (point.x < 0 || point.x >= width ||
                point.y < 0 || point.y >= height) {
            return;
        }
        int ind = (height - point.y()) * width + point.x();
        frameBuf.set(ind, color);
    }

    void clear(int buff) {
        if ((buff & Buffers.COLOR) == Buffers.COLOR) {
            frameBuf.clear();
            for (int i = 0; i < total; i++) {
                frameBuf.add(new Vector3d(0, 0, 0));
            }
            /*for (Vector3d frame : frameBuf) {
                frame.set(0, 0, 0);
            }*/
        }
        if ((buff & Buffers.DEPTH) == Buffers.DEPTH) {
            Collections.fill(depthBuf, Double.MAX_VALUE);
        }
    }

    List<Vector3d> frameBuffer() {
        return frameBuf;
    }

    int getIndex(int x, int y) {
        return (height - y) * width + x;
    }

    void draw(int posBuffer, int indBuffer, Primitive type) {
        if (type != Primitive.TRIANGLE) {
            throw new IllegalArgumentException("Drawing primitives other than triangle is not implemented yet!");
        }
        List<Vector3d> buf = posBuf.get(posBuffer);
        List<Vector3i> ind = indBuf.get(indBuffer);

        double f1 = (100 - 0.1) / 2.0;
        double f2 = (100 + 0.1) / 2.0;

        Matrix4d mvp = projection.mul(view).mul(model);
        for (Vector3i i : ind) {
            Triangle t = new Triangle();
            Vector4d[] v = {
                    to_vec4(buf.get(i.x), 1.0).mul(mvp),
                    to_vec4(buf.get(i.y), 1.0).mul(mvp),
                    to_vec4(buf.get(i.z), 1.0).mul(mvp)
            };
            for (Vector4d vec : v) {
                vec.div(vec.w());
            }
            for (Vector4d vert : v) {
                vert.x = 0.5 * width * (vert.x() + 1.0);
                vert.y = 0.5 * height * (vert.y() + 1.0);
                vert.z = vert.z() * f1 + f2;
            }
            for (int j = 0; j < 3; ++j) {
                t.setVertex(j, new Vector3d(v[j].x, v[j].y, v[j].z));
            }
            t.setColor(0, 255.0,  0.0,  0.0);
            t.setColor(1, 0.0  ,255.0,  0.0);
            t.setColor(2, 0.0  ,  0.0,255.0);
            rasterize_wireframe(t);
        }
    }

    Vector4d to_vec4(Vector3d v3, double w) {
        return new Vector4d(v3.x(), v3.y(), v3.z(), w);
    }

    void rasterize_wireframe(Triangle t) {
        draw_line(t.c(), t.a());
        draw_line(t.c(), t.b());
        draw_line(t.b(), t.a());
    }

    void draw_line(Vector3d begin, Vector3d end) {
        double x1 = begin.x();
        double y1 = begin.y();
        double x2 = end.x();
        double y2 = end.y();

        Vector3d line_color = new Vector3d(255, 255, 255);

        int x, y, dx, dy, dx1, dy1, px, py, xe, ye;

        dx = (int) (x2 - x1);
        dy = (int) (y2 - y1);
        dx1 = Math.abs(dx);
        dy1 = Math.abs(dy);
        px = 2 * dy1 - dx1;
        py = 2 * dx1 - dy1;

        if (dy1 <= dx1) {
            if (dx >= 0) {
                x = (int) x1;
                y = (int) y1;
                xe = (int) x2;
            } else {
                x = (int) x2;
                y = (int) y2;
                xe = (int) x1;
            }
            Vector2i point = new Vector2i(x, y);
            setPixel(point, line_color);
            while (x < xe) {
                x = x + 1;
                if (px < 0) {
                    px = px + 2 * dy1;
                } else {
                    if ((dx < 0 && dy < 0) || (dx > 0 && dy > 0)) {
                        y = y + 1;
                    } else {
                        y = y - 1;
                    }
                    px = px + 2 * (dy1 - dx1);
                }
                Vector2i point1 = new Vector2i(x, y);
                setPixel(point1, line_color);
            }
        } else {
            if (dy >= 0) {
                x = (int) x1;
                y = (int) y1;
                ye = (int) y2;
            } else {
                x = (int) x2;
                y = (int) y2;
                ye = (int) y1;
            }
            Vector2i point = new Vector2i(x, y);
            setPixel(point, line_color);
            while (y < ye) {
                y = y + 1;
                if (py <= 0) {
                    py = py + 2 * dx1;
                } else {
                    if ((dx < 0 && dy < 0) || (dx > 0 && dy > 0)) {
                        x = x + 1;
                    } else {
                        x = x - 1;
                    }
                    py = py + 2 * (dx1 - dy1);
                }
                Vector2i point1 = new Vector2i(x, y);
                setPixel(point1, line_color);
            }
        }
    }
}
