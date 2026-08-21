import sys

def decompress(data, offset):
    dst = bytearray(0x10000)
    dst_idx = 0
    src_idx = offset
    while True:
        cmd = data[src_idx]
        src_idx += 1
        if cmd == 0xff:
            return dst_idx
        if (cmd & 0xe0) != 0xe0:
            length = (cmd & 0x1f) + 1
            cmd &= 0xe0
        else:
            length = data[src_idx]
            src_idx += 1
            length += ((cmd & 3) << 8) + 1
            cmd = (cmd << 3) & 0xe0
        
        if cmd == 0:
            for _ in range(length):
                dst[dst_idx] = data[src_idx]
                dst_idx += 1
                src_idx += 1
        elif (cmd & 0x80):
            offs = data[src_idx] | (data[src_idx+1] << 8)
            src_idx += 2
            for _ in range(length):
                if offs >= len(dst):
                    print(f"OUT OF BOUNDS: offs={offs} at dst_idx={dst_idx}")
                    return dst_idx
                dst[dst_idx] = dst[offs]
                dst_idx += 1
                offs += 1
        elif not (cmd & 0x40):
            v = data[src_idx]
            src_idx += 1
            for _ in range(length):
                dst[dst_idx] = v
                dst_idx += 1
        elif not (cmd & 0x20):
            lo = data[src_idx]
            hi = data[src_idx+1]
            src_idx += 2
            for _ in range(length):
                dst[dst_idx] = lo
                dst_idx += 1
                length -= 1
                if length == 0: break
                dst[dst_idx] = hi
                dst_idx += 1
        else:
            v = data[src_idx]
            src_idx += 1
            for _ in range(length):
                dst[dst_idx] = v
                dst_idx += 1
                v += 1
                
with open("app/src/main/assets/zelda3_assets.dat", "rb") as f:
    data = f.read()

num_assets = int.from_bytes(data[80:84], "little")

offset = 88 + num_assets * 4 + int.from_bytes(data[84:88], "little")
for i in range(num_assets):
    size = int.from_bytes(data[88 + i*4 : 92 + i*4], "little")
    offset = (offset + 3) & ~3
    if i == 64:
        asset64_offset = offset
        break
    offset += size

idx = 0x58
mx = int.from_bytes(data[asset64_offset + size - 2 : asset64_offset + size], "little")
if mx < 8192:
    if idx == 0: left_off = mx * 2
    else: left_off = mx * 2 + int.from_bytes(data[asset64_offset + idx*2 - 2 : asset64_offset + idx*2], "little")
    if idx == mx: right_off = size - 2
    else: right_off = mx * 2 + int.from_bytes(data[asset64_offset + idx*2 : asset64_offset + idx*2 + 2], "little")
else:
    mx -= 8192
    if idx == 0: left_off = mx * 4
    else: left_off = mx * 4 + int.from_bytes(data[asset64_offset + idx*4 - 4 : asset64_offset + idx*4], "little")
    if idx == mx: right_off = size - 2
    else: right_off = mx * 4 + int.from_bytes(data[asset64_offset + idx*4 : asset64_offset + idx*4 + 4], "little")

print(f"Index {idx} offset {left_off} to {right_off}")
decomp_size = decompress(data, asset64_offset + left_off)
print(f"Decompressed size: {decomp_size}")
