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
    vec2 texel = vTexCoord * TextureSize;
    vec2 texel_floor = floor(texel);
    vec2 texel_fract = texel - texel_floor;
    vec2 scale = OutputSize / TextureSize;
    vec2 region = clamp((texel_fract - 0.5) * scale + 0.5, 0.0, 1.0);
    vec2 uv = (texel_floor + region) / TextureSize;
    FragColor = COMPAT_TEXTURE(Texture, uv);
}

#endif
