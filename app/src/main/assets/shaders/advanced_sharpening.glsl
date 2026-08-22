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

void main()
{
    // Sharp bilinear texel coordinates
    vec2 texel = vTexCoord * TextureSize;
    vec2 texel_floor = floor(texel);
    vec2 texel_fract = texel - texel_floor;
    vec2 scale = OutputSize / TextureSize;
    vec2 region = clamp((texel_fract - 0.5) * scale + 0.5, 0.0, 1.0);
    vec2 uv = (texel_floor + region) / TextureSize;

    vec2 dx = vec2(1.0 / TextureSize.x, 0.0);
    vec2 dy = vec2(0.0, 1.0 / TextureSize.y);

    vec3 a = COMPAT_TEXTURE(Texture, uv - dy).rgb;
    vec3 b = COMPAT_TEXTURE(Texture, uv - dx).rgb;
    vec3 c = COMPAT_TEXTURE(Texture, uv).rgb;
    vec3 d = COMPAT_TEXTURE(Texture, uv + dx).rgb;
    vec3 e = COMPAT_TEXTURE(Texture, uv + dy).rgb;

    vec3 min_c = min(min(a, b), min(min(d, e), c));
    vec3 max_c = max(max(a, b), max(max(d, e), c));

    // Contrast Adaptive Sharpening
    vec3 amp = clamp(min(min_c, 2.0 - max_c) / max(max_c, vec3(0.001)), 0.0, 1.0);
    vec3 w = sqrt(amp) * -0.25;
    vec3 result = (a * w + b * w + c + d * w + e * w) / (1.0 + 4.0 * w);

    FragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}

#endif
