package as2;

import org.joml.*;

public class Triangle {

    /*the original coordinates of the triangle, v0, v1, v2 in counterclockwise order*/
    Vector3f[] v;

    /*Per vertex values*/

    //color at each vertex;
    Vector3f[] color;

    //texture u,v
    Vector2f[] tex_coords;

    //normal vector for each vertex
    Vector3f[] normal;

    public Triangle() {
        v = new Vector3f[3];
        color = new Vector3f[3];
        tex_coords = new Vector2f[3];
        normal = new Vector3f[3];
    }

    Vector3f a() { return v[0]; }
    Vector3f b() { return v[1]; }
    Vector3f c() { return v[2]; }

    void setVertex(int ind, Vector3f ver) {
        v[ind] = ver;
    }

    void setNormal(int ind, Vector3f n) {
        normal[ind] = n;
    }

    void setColor(int ind, float r, float g, float b) {
        if((r<0.0) || (r>255.) ||
                (g<0.0) || (g>255.) ||
                (b<0.0) || (b>255.)) {
            throw new IllegalArgumentException("Invalid color values");
        }

        color[ind] = new Vector3f(r / 255, g / 255, b / 255);
    }

    Vector3f getColor() {
        return new Vector3f(color[0].x * 255, color[0].y * 255, color[0].z * 255);
    }

    void setTexCoord(int ind, float s, float t) {
        tex_coords[ind] = new Vector2f(s, t);
    }

    Vector4f[] toVector4() {
        Vector4f[] res = new Vector4f[v.length];
        for (int i = 0; i < res.length; i++) {
            Vector3f v3d = v[i];
            res[i] = new Vector4f(v3d.x, v3d.y, v3d.z, 1f);
        }
        return res;
    }
}
