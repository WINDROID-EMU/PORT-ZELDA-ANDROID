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
    // Amostragem Sharp Bilinear
    vec2 texel = vTexCoord * TextureSize;
    vec2 texel_floor = floor(texel);
    vec2 texel_fract = texel - texel_floor;
    vec2 scale = OutputSize / TextureSize;
    vec2 region = clamp((texel_fract - 0.5) * scale + 0.5, 0.0, 1.0);
    vec2 uv = (texel_floor + region) / TextureSize;
    vec4 color = COMPAT_TEXTURE(Texture, uv);

    // Curva de Gamma
    color.rgb = pow(color.rgb, vec3(1.15));

    // Scanlines CRT autênticas
    float scanline = 0.5 + 0.5 * cos(vTexCoord.y * TextureSize.y * 6.2831853);
    float scan_weight = 0.70 + 0.30 * scanline;

    // Máscara de abertura RGB (Phosphor mask)
    float pixel_x = floor(vTexCoord.x * OutputSize.x);
    float mod_x = mod(pixel_x, 3.0);
    if (mod_x == 0.0) {
        color.gb *= 0.85;
    } else if (mod_x == 1.0) {
        color.rb *= 0.85;
    } else {
        color.rg *= 0.85;
    }

    color.rgb *= scan_weight;
    // Correção gama de saída
    color.rgb = pow(color.rgb, vec3(1.0 / 1.15));

    FragColor = color;
}

#endif
