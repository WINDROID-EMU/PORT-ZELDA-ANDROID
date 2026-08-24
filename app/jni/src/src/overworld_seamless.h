#ifndef OVERWORLD_SEAMLESS_H_
#define OVERWORLD_SEAMLESS_H_

#include "types.h"

#ifdef __cplusplus
extern "C" {
#endif

void Overworld_InitGlobalWorldMap(void);
const uint16 *Overworld_GetGlobalTilemap(uint8 world);
void Overworld_UpdateGlobalMap16(uint8 world, int world_x, int world_y, uint16 map16_value);
void Overworld_RestoreCleanWorldMap(uint8 world);

#ifdef __cplusplus
}
#endif

#endif  // OVERWORLD_SEAMLESS_H_
