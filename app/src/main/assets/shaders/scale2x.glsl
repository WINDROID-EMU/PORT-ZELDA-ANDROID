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

bool eq(vec4 a, vec4 b) {
    return length(a.rgb - b.rgb) < 0.05;
}

void main()
{
    vec2 pos = vTexCoord * TextureSize;
    vec2 f = fract(pos);
    vec2 inv_tex = 1.0 / TextureSize;
    vec2 base = (floor(pos) + 0.5) * inv_tex;

    vec4 E = COMPAT_TEXTURE(Texture, base);
    vec4 A = COMPAT_TEXTURE(Texture, base - vec2(0.0, inv_tex.y));
    vec4 B = COMPAT_TEXTURE(Texture, base - vec2(inv_tex.x, 0.0));
    vec4 C = COMPAT_TEXTURE(Texture, base + vec2(inv_tex.x, 0.0));
    vec4 D = COMPAT_TEXTURE(Texture, base + vec2(0.0, inv_tex.y));

    vec4 color = E;
    if (f.x < 0.5 && f.y < 0.5) {
        if (eq(B, A) && !eq(B, D) && !eq(A, C)) color = A;
    } else if (f.x >= 0.5 && f.y < 0.5) {
        if (eq(A, C) && !eq(A, B) && !eq(C, D)) color = C;
    } else if (f.x < 0.5 && f.y >= 0.5) {
        if (eq(D, B) && !eq(D, C) && !eq(B, A)) color = B;
    } else {
        if (eq(C, D) && !eq(C, A) && !eq(D, B)) color = D;
    }

    FragColor = color;
}

#endif
