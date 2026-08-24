#include "overworld_seamless.h"
#include "load_gfx.h"
#include "assets.h"
#include "overworld.h"
#include "zelda_rtl.h"
#include "variables.h"
#include <string.h>

static uint16 g_pristine_world_tiles[2][512 * 512];
static uint16 g_global_world_tiles[2][512 * 512];
static bool g_global_world_initialized = false;

extern void Overworld_DecompressAndDrawOneQuadrant(uint16 *dst, int screen);
extern const uint16 kSecondaryOverlayPerOw[128];

void Overworld_InitGlobalWorldMap(void) {
  if (g_global_world_initialized)
    return;

  const uint16 *map8_table = GetMap16toMap8Table();
  uint16 temp_quadrant[64 * 32];

  for (int world = 0; world < 2; world++) {
    for (int s = 0; s < 64; s++) {
      int screen = s + (world ? 64 : 0);
      memset(temp_quadrant, 0, sizeof(temp_quadrant));
      Overworld_DecompressAndDrawOneQuadrant(temp_quadrant, screen);

      if (s == 0x33)
        temp_quadrant[340] = 0x20f;
      else if (s == 0x2f)
        temp_quadrant[1497] = 0x20f;

      if (save_ow_event_info[screen] & 2) {
        int pos = kSecondaryOverlayPerOw[screen] >> 1;
        temp_quadrant[pos + 0] = 0xdb4;
        temp_quadrant[pos + 1] = 0xdb5;
      }

      int col = s & 7;
      int row = s >> 3;
      int base_tile_x = col * 64;
      int base_tile_y = row * 64;

      for (int r = 0; r < 32; r++) {
        for (int c = 0; c < 32; c++) {
          uint16 map16 = temp_quadrant[r * 64 + c];
          const uint16 *m = map8_table + 4 * map16;
          int tx = base_tile_x + c * 2;
          int ty = base_tile_y + r * 2;

          g_pristine_world_tiles[world][ty * 512 + tx] = m[0];
          g_pristine_world_tiles[world][ty * 512 + tx + 1] = m[1];
          g_pristine_world_tiles[world][(ty + 1) * 512 + tx] = m[2];
          g_pristine_world_tiles[world][(ty + 1) * 512 + tx + 1] = m[3];
        }
      }
    }
  }

  memcpy(g_global_world_tiles, g_pristine_world_tiles, sizeof(g_global_world_tiles));
  g_global_world_initialized = true;
}

const uint16 *Overworld_GetGlobalTilemap(uint8 world) {
  if (!g_global_world_initialized)
    Overworld_InitGlobalWorldMap();
  return g_global_world_tiles[world & 1];
}

void Overworld_UpdateGlobalMap16(uint8 world, int world_x, int world_y, uint16 map16_value) {
  if (!g_global_world_initialized)
    return;
  world &= 1;
  int tx = (world_x >> 3) & 511;
  int ty = (world_y >> 3) & 511;
  const uint16 *m = GetMap16toMap8Table() + 4 * map16_value;
  g_global_world_tiles[world][ty * 512 + tx] = m[0];
  g_global_world_tiles[world][ty * 512 + tx + 1] = m[1];
  g_global_world_tiles[world][(ty + 1) * 512 + tx] = m[2];
  g_global_world_tiles[world][(ty + 1) * 512 + tx + 1] = m[3];
}

void Overworld_RestoreCleanWorldMap(uint8 world) {
  if (!g_global_world_initialized)
    Overworld_InitGlobalWorldMap();
  world &= 1;

  // Restore pristine baseline in memory without touching g_ram
  memcpy(g_global_world_tiles[world], g_pristine_world_tiles[world], sizeof(g_global_world_tiles[world]));

  // Re-apply all persistent memorized tile changes (lifted heavy rocks, bombed cave entrances, etc.)
  for (int i = 0, i_end = num_memorized_tiles >> 1; i != i_end; i++) {
    uint16 pos = memorized_tile_addr[i];
    uint16 value = memorized_tile_value[i];
    int local_col = (pos >> 1) & 63;
    int local_row = (pos >> 1) >> 6;
    int area = BYTE(current_area_of_player) >> 1;
    int world_x = kOverworld_OffsetBaseX[area] + local_col * 16;
    int world_y = kOverworld_OffsetBaseY[area] + local_row * 16;
    Overworld_UpdateGlobalMap16(world, world_x, world_y, value);
  }
}
