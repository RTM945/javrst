import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;

public class Triangle {

    Vector3d[] v = new Vector3d[3];
    Vector3d[] color = new Vector3d[3];
    Vector2d[] texCoords = new Vector2d[3];
    Vector3d[] normal = new Vector3d[3];

    Vector3d a() {return v[0];}
    Vector3d b() {return v[1];}
    Vector3d c() {return v[2];}

    void setVertex(int ind, Vector3d ver) {
        v[ind] = ver;
    }

    void setNormal(int ind, Vector3d n) {
        normal[ind] = n;
    }

    void setColor(int ind, double r, double g, double b) {
        if ((r < 0.0) || (r > 255.) ||
                (g < 0.0) || (g > 255.) ||
                (b < 0.0) || (b > 255.)) {
            throw new IllegalArgumentException("Invalid color values");
        }
        color[ind] = new Vector3d(r / 255., g / 255., b / 255.);
    }

    void setTexCoord(int ind, float s, float t) {
        texCoords[ind] = new Vector2d(s, t);
    }

    Vector4d[] toVector4() {
        Vector4d[] res = new Vector4d[v.length];
        for (int i = 0; i < res.length; i++) {
            Vector3d v3d = v[i];
            res[i] = new Vector4d(v3d.x, v3d.y, v3d.z, 1.0);
        }
        return res;
    }

}
