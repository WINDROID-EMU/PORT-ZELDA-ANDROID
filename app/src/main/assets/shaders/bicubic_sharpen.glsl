#if defined(VERTEX)

#if __VERSION__ >= 130
#define COMPAT_VARYING out
#define COMPAT_ATTRIBUTE in
#define COMPAT_TEXTURE texture
#else
#define COMPAT_VARYING varying
#define COMPAT_ATTRIBUTE attribute
#define COMPAT_TEXTURE texture2D
#endif

COMPAT_ATTRIBUTE vec4 VertexCoord;
COMPAT_ATTRIBUTE vec4 TexCoord;
COMPAT_VARYING vec2 vTexCoord;

uniform mat4 MVPMatrix;
uniform vec2 OutputSize;
uniform vec2 TextureSize;
uniform vec2 InputSize;

void main()
{
    gl_Position = MVPMatrix * VertexCoord;
    vTexCoord = TexCoord.xy;
}

#elif defined(FRAGMENT)

#if __VERSION__ >= 130
#define COMPAT_VARYING in
#define COMPAT_TEXTURE texture
out vec4 FragColor;
#else
#define COMPAT_VARYING varying
#define COMPAT_TEXTURE texture2D
#define FragColor gl_FragColor
#endif

uniform sampler2D Texture;
uniform vec2 OutputSize;
uniform vec2 TextureSize;
uniform vec2 InputSize;
uniform int FrameCount;

COMPAT_VARYING vec2 vTexCoord;

vec4 cubic(float v) {
    vec4 n = vec4(1.0, 2.0, 3.0, 4.0) - v;
    vec4 s = n * n * n;
    float x = s.x;
    float y = s.y - 4.0 * s.x;
    float z = s.z - 4.0 * s.y + 6.0 * s.x;
    float w = 6.0 - x - y - z;
    return vec4(x, y, z, w) * (1.0 / 6.0);
}

void main()
{
    vec2 inv_tex = 1.0 / TextureSize;
    vec2 p = vTexCoord * TextureSize - 0.5;
    vec2 f = fract(p);
    vec2 index = floor(p);

    vec4 xcubic = cubic(f.x);
    vec4 ycubic = cubic(f.y);

    vec4 c = vec4(index.x - 0.5, index.x + 1.5, index.y - 0.5, index.y + 1.5);
    vec4 s = vec4(xcubic.x + xcubic.y, xcubic.z + xcubic.w, ycubic.x + ycubic.y, ycubic.z + ycubic.w);
    vec4 offset = c + vec4(xcubic.y, xcubic.w, ycubic.y, ycubic.w) / s;

    vec4 sample0 = COMPAT_TEXTURE(Texture, vec2(offset.x, offset.z) * inv_tex);
    vec4 sample1 = COMPAT_TEXTURE(Texture, vec2(offset.y, offset.z) * inv_tex);
    vec4 sample2 = COMPAT_TEXTURE(Texture, vec2(offset.x, offset.w) * inv_tex);
    vec4 sample3 = COMPAT_TEXTURE(Texture, vec2(offset.y, offset.w) * inv_tex);

    float sx = s.x / (s.x + s.y);
    float sy = s.z / (s.z + s.w);

    vec4 res = mix(mix(sample3, sample2, sx), mix(sample1, sample0, sx), sy);
    FragColor = res;
}

#endif
