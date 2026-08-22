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

const float truerange = 0.05;

float diff(vec4 c1, vec4 c2) {
    vec4 d = abs(c1 - c2);
    return (d.r + d.g + d.b) > truerange ? 1.0 : 0.0;
}

void main()
{
    vec2 pos = vTexCoord * TextureSize;
    vec2 fp = fract(pos);
    vec2 dx = vec2(1.0 / TextureSize.x, 0.0);
    vec2 dy = vec2(0.0, 1.0 / TextureSize.y);
    vec2 base = (floor(pos) + 0.5) / TextureSize;

    vec4 c  = COMPAT_TEXTURE(Texture, base);
    vec4 c1 = COMPAT_TEXTURE(Texture, base - dx - dy);
    vec4 c2 = COMPAT_TEXTURE(Texture, base - dy);
    vec4 c3 = COMPAT_TEXTURE(Texture, base + dx - dy);
    vec4 c4 = COMPAT_TEXTURE(Texture, base - dx);
    vec4 c5 = COMPAT_TEXTURE(Texture, base + dx);
    vec4 c6 = COMPAT_TEXTURE(Texture, base - dx + dy);
    vec4 c7 = COMPAT_TEXTURE(Texture, base + dy);
    vec4 c8 = COMPAT_TEXTURE(Texture, base + dx + dy);

    vec4 res = c;

    if (fp.x < 0.5 && fp.y < 0.5) {
        if (diff(c2, c4) == 0.0 && diff(c, c2) != 0.0 && diff(c2, c1) == 0.0)
            res = mix(c, c2, 0.5);
    } else if (fp.x >= 0.5 && fp.y < 0.5) {
        if (diff(c2, c5) == 0.0 && diff(c, c2) != 0.0 && diff(c2, c3) == 0.0)
            res = mix(c, c2, 0.5);
    } else if (fp.x < 0.5 && fp.y >= 0.5) {
        if (diff(c7, c4) == 0.0 && diff(c, c7) != 0.0 && diff(c7, c6) == 0.0)
            res = mix(c, c7, 0.5);
    } else {
        if (diff(c7, c5) == 0.0 && diff(c, c7) != 0.0 && diff(c7, c8) == 0.0)
            res = mix(c, c7, 0.5);
    }

    FragColor = res;
}

#endif
