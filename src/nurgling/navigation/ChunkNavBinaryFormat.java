package nurgling.navigation;

import haven.Coord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static nurgling.navigation.ChunkNavConfig.*;

/**
 * Binary serialization format for ChunkNavData.
 *
 * Format per .chunk file:
 * - Header (64 bytes): version, flags, gridId, timestamps, neighbors
 * - Walkability grid (10,000 bytes): 40,000 cells at 2 bits each
 * - Observed grid (5,000 bytes): 40,000 cells at 1 bit each
 * - Edge arrays (100 bytes): 800 booleans at 1 bit each
 * - Portals (variable): count + portal data
 * - Connected chunks (variable): count + gridIds
 * - Reachable areas (variable): count + areaIds
 */
public class ChunkNavBinaryFormat {

    // Magic number to identify valid chunk files
    public static final int MAGIC = 0x434E4156; // "CNAV" in ASCII

    // Current binary format version
    // V1: original format (64-byte header without instanceId)
    // V2: added instanceId (8 bytes) after neighbors, header now 72 bytes
    public static final short BINARY_VERSION = 2;

    // Header size in bytes (V2: 72 bytes with instanceId)
    public static final int HEADER_SIZE = 72;

    // Grid sizes
    public static final int WALKABILITY_BYTES = CELLS_PER_EDGE * CELLS_PER_EDGE / 4; // 2 bits per cell
    public static final int OBSERVED_BYTES = CELLS_PER_EDGE * CELLS_PER_EDGE / 8;    // 1 bit per cell
    public static final int EDGE_BYTES = (4 * CELLS_PER_EDGE) / 8;                   // 4 edges, 1 bit per point

    // Layer byte values
    public static final byte LAYER_OUTSIDE = 0;
    public static final byte LAYER_INSIDE = 1;
    public static final byte LAYER_CELLAR = 2;

    // Portal type byte values
    public static final byte PORTAL_DOOR = 0;
    public static final byte PORTAL_GATE = 1;
    public static final byte PORTAL_STAIRS_UP = 2;
    public static final byte PORTAL_STAIRS_DOWN = 3;
    public static final byte PORTAL_CELLAR = 4;
    public static final byte PORTAL_MINE_ENTRANCE = 5;
    public static final byte PORTAL_MINEHOLE = 6;
    public static final byte PORTAL_LADDER = 7;
    public static final byte PORTAL_UNKNOWN = 8;

    /**
     * Write a chunk to binary format.
     */
    public static void writeChunk(ChunkNavData chunk, DataOutputStream out) throws IOException {
        // Write magic number
        out.writeInt(MAGIC);

        // Write header
        out.writeShort(BINARY_VERSION);
        out.writeShort(0); // flags (reserved)
        out.writeLong(chunk.gridId);
        out.writeLong(chunk.lastUpdated);
        out.writeFloat(chunk.confidence);
        out.writeByte(layerToByte(chunk.layer));
        out.writeLong(chunk.neighborNorth);
        out.writeLong(chunk.neighborSouth);
        out.writeLong(chunk.neighborEast);
        out.writeLong(chunk.neighborWest);

        // V2: instanceId (8 bytes)
        out.writeLong(chunk.instanceId);

        // Padding to reach 72 bytes header
        // Magic(4) + version(2) + flags(2) + gridId(8) + lastUpdated(8) + confidence(4) +
        // layer(1) + neighbors(4*8=32) + instanceId(8) = 69 bytes, need 3 bytes padding
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);

        // Write walkability grid (bit-packed, 2 bits per cell)
        writeWalkabilityGrid(chunk.walkability, out);

        // Write observed grid (bit-packed, 1 bit per cell)
        writeObservedGrid(chunk.observed, out);

        // Write edge arrays (bit-packed, 1 bit per point)
        writeEdges(chunk, out);

        // Write portals
        writePortals(chunk.portals, out);

        // Write connected chunks
        writeConnectedChunks(chunk.connectedChunks, out);

        // Write reachable areas
        writeReachableAreas(chunk.reachableAreaIds, out);
    }

    /**
     * Read a chunk from binary format.
     */
    public static ChunkNavData readChunk(DataInputStream in) throws IOException {
        // Read and verify magic number
        int magic = in.readInt();
        if (magic != MAGIC) {
            throw new IOException("Invalid chunk file: bad magic number");
        }

        // Read header
        short version = in.readShort();
        if (version > BINARY_VERSION) {
            throw new IOException("Unsupported chunk version: " + version);
        }

        short flags = in.readShort(); // reserved
        long gridId = in.readLong();
        long lastUpdated = in.readLong();
        float confidence = in.readFloat();
        byte layerByte = in.readByte();
        long neighborNorth = in.readLong();
        long neighborSouth = in.readLong();
        long neighborEast = in.readLong();
        long neighborWest = in.readLong();

        // V2: read instanceId (V1 files don't have it)
        long instanceId = 0;
        if (version >= 2) {
            instanceId = in.readLong();
        }

        // Skip padding
        in.readByte();
        in.readByte();
        in.readByte();

        // Create chunk
        ChunkNavData chunk = new ChunkNavData();
        chunk.gridId = gridId;
        chunk.lastUpdated = lastUpdated;
        chunk.confidence = confidence;
        chunk.layer = byteToLayer(layerByte);
        chunk.neighborNorth = neighborNorth;
        chunk.neighborSouth = neighborSouth;
        chunk.neighborEast = neighborEast;
        chunk.neighborWest = neighborWest;
        chunk.instanceId = instanceId;

        // Read walkability grid
        readWalkabilityGrid(in, chunk.walkability);

        // Read observed grid
        readObservedGrid(in, chunk.observed);

        // Read edge arrays
        readEdges(in, chunk);

        // Read portals
        chunk.portals = readPortals(in);

        // Read connected chunks
        chunk.connectedChunks = readConnectedChunks(in);

        // Read reachable areas
        chunk.reachableAreaIds = readReachableAreas(in);

        // Recompute section counts from observed data
        chunk.recomputeSectionCounts();

        return chunk;
    }

    // ============== Walkability Grid ==============

    private static void writeWalkabilityGrid(byte[][] grid, DataOutputStream out) throws IOException {
        // Pack 4 cells per byte (2 bits each, values 0-2)
        // Order: iterate x then y, same as JSON flat array
        byte[] packed = new byte[WALKABILITY_BYTES];
        int byteIndex = 0;
        int bitOffset = 6; // Start at high bits
        byte currentByte = 0;

        for (int x = 0; x < CELLS_PER_EDGE; x++) {
            for (int y = 0; y < CELLS_PER_EDGE; y++) {
                int value = grid[x][y] & 0x03; // Ensure 2 bits
                currentByte |= (value << bitOffset);
                bitOffset -= 2;

                if (bitOffset < 0) {
                    packed[byteIndex++] = currentByte;
                    currentByte = 0;
                    bitOffset = 6;
                }
            }
        }

        out.write(packed);
    }

    private static void readWalkabilityGrid(DataInputStream in, byte[][] grid) throws IOException {
        byte[] packed = new byte[WALKABILITY_BYTES];
        in.readFully(packed);

        int byteIndex = 0;
        int bitOffset = 6;

        for (int x = 0; x < CELLS_PER_EDGE; x++) {
            for (int y = 0; y < CELLS_PER_EDGE; y++) {
                int value = (packed[byteIndex] >> bitOffset) & 0x03;
                grid[x][y] = (byte) value;
                bitOffset -= 2;

                if (bitOffset < 0) {
                    byteIndex++;
                    bitOffset = 6;
                }
            }
        }
    }

    // ============== Observed Grid ==============

    private static void writeObservedGrid(boolean[][] grid, DataOutputStream out) throws IOException {
        // Pack 8 cells per byte (1 bit each)
        byte[] packed = new byte[OBSERVED_BYTES];
        int byteIndex = 0;
        int bitOffset = 7;
        byte currentByte = 0;

        for (int x = 0; x < CELLS_PER_EDGE; x++) {
            for (int y = 0; y < CELLS_PER_EDGE; y++) {
                if (grid[x][y]) {
                    currentByte |= (1 << bitOffset);
                }
                bitOffset--;

                if (bitOffset < 0) {
                    packed[byteIndex++] = currentByte;
                    currentByte = 0;
                    bitOffset = 7;
                }
            }
        }

        out.write(packed);
    }

    private static void readObservedGrid(DataInputStream in, boolean[][] grid) throws IOException {
        byte[] packed = new byte[OBSERVED_BYTES];
        in.readFully(packed);

        int byteIndex = 0;
        int bitOffset = 7;

        for (int x = 0; x < CELLS_PER_EDGE; x++) {
            for (int y = 0; y < CELLS_PER_EDGE; y++) {
                grid[x][y] = ((packed[byteIndex] >> bitOffset) & 0x01) == 1;
                bitOffset--;

                if (bitOffset < 0) {
                    byteIndex++;
                    bitOffset = 7;
                }
            }
        }
    }

    // ============== Edges ==============

    private static void writeEdges(ChunkNavData chunk, DataOutputStream out) throws IOException {
        // Pack all 4 edges, 200 booleans each = 800 bits = 100 bytes
        byte[] packed = new byte[EDGE_BYTES];
        int byteIndex = 0;
        int bitOffset = 7;
        byte currentByte = 0;

        EdgePoint[][] allEdges = {chunk.northEdge, chunk.southEdge, chunk.eastEdge, chunk.westEdge};

        for (EdgePoint[] edge : allEdges) {
            for (int i = 0; i < CELLS_PER_EDGE; i++) {
                if (edge[i] != null && edge[i].walkable) {
                    currentByte |= (1 << bitOffset);
                }
                bitOffset--;

                if (bitOffset < 0) {
                    packed[byteIndex++] = currentByte;
                    currentByte = 0;
                    bitOffset = 7;
                }
            }
        }

        out.write(packed);
    }

    private static void readEdges(DataInputStream in, ChunkNavData chunk) throws IOException {
        byte[] packed = new byte[EDGE_BYTES];
        in.readFully(packed);

        int byteIndex = 0;
        int bitOffset = 7;

        EdgePoint[][] allEdges = {chunk.northEdge, chunk.southEdge, chunk.eastEdge, chunk.westEdge};

        // Edge local coordinates based on direction
        int edgeIndex = 0;
        for (EdgePoint[] edge : allEdges) {
            for (int i = 0; i < CELLS_PER_EDGE; i++) {
                boolean walkable = ((packed[byteIndex] >> bitOffset) & 0x01) == 1;

                // Calculate local coordinates based on edge direction
                Coord localCoord;
                switch (edgeIndex) {
                    case 0: // North
                        localCoord = new Coord(i, 0);
                        break;
                    case 1: // South
                        localCoord = new Coord(i, CELLS_PER_EDGE - 1);
                        break;
                    case 2: // East
                        localCoord = new Coord(CELLS_PER_EDGE - 1, i);
                        break;
                    case 3: // West
                        localCoord = new Coord(0, i);
                        break;
                    default:
                        localCoord = new Coord(0, 0);
                }

                edge[i] = new EdgePoint(i, walkable, localCoord);

                bitOffset--;
                if (bitOffset < 0) {
                    byteIndex++;
                    bitOffset = 7;
                }
            }
            edgeIndex++;
        }
    }

    // ============== Portals ==============

    private static void writePortals(List<ChunkPortal> portals, DataOutputStream out) throws IOException {
        out.writeShort(portals.size());

        for (ChunkPortal portal : portals) {
            // gobName
            byte[] nameBytes = portal.gobName != null ?
                portal.gobName.getBytes(StandardCharsets.UTF_8) : new byte[0];
            out.writeShort(nameBytes.length);
            out.write(nameBytes);

            // gobHash
            byte[] hashBytes = portal.gobHash != null ?
                portal.gobHash.getBytes(StandardCharsets.UTF_8) : new byte[0];
            out.writeShort(hashBytes.length);
            out.write(hashBytes);

            // portalType
            out.writeByte(portalTypeToByte(portal.type));

            // localCoord
            out.writeShort(portal.localCoord != null ? portal.localCoord.x : 0);
            out.writeShort(portal.localCoord != null ? portal.localCoord.y : 0);

            // connectsToGridId
            out.writeLong(portal.connectsToGridId);

            // exitLocalCoord (optional - write -1,-1 if null)
            out.writeShort(portal.exitLocalCoord != null ? portal.exitLocalCoord.x : -1);
            out.writeShort(portal.exitLocalCoord != null ? portal.exitLocalCoord.y : -1);

            // lastTraversed
            out.writeLong(portal.lastTraversed);
        }
    }

    private static List<ChunkPortal> readPortals(DataInputStream in) throws IOException {
        int count = in.readShort() & 0xFFFF;
        List<ChunkPortal> portals = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            ChunkPortal portal = new ChunkPortal();

            // gobName
            int nameLen = in.readShort() & 0xFFFF;
            if (nameLen > 0) {
                byte[] nameBytes = new byte[nameLen];
                in.readFully(nameBytes);
                portal.gobName = new String(nameBytes, StandardCharsets.UTF_8);
            }

            // gobHash
            int hashLen = in.readShort() & 0xFFFF;
            if (hashLen > 0) {
                byte[] hashBytes = new byte[hashLen];
                in.readFully(hashBytes);
                portal.gobHash = new String(hashBytes, StandardCharsets.UTF_8);
            }

            // portalType - always re-classify from gobName to fix legacy data
            // where type was null/unknown and incorrectly loaded as DOOR
            byte storedType = in.readByte();
            ChunkPortal.PortalType reclassified = ChunkPortal.classifyPortal(portal.gobName);
            if (reclassified != null) {
                portal.type = reclassified;
            } else {
                portal.type = byteToPortalType(storedType);
            }

            // localCoord
            int localX = in.readShort();
            int localY = in.readShort();
            portal.localCoord = new Coord(localX, localY);

            // connectsToGridId
            portal.connectsToGridId = in.readLong();

            // exitLocalCoord
            int exitX = in.readShort();
            int exitY = in.readShort();
            if (exitX >= 0 && exitY >= 0) {
                portal.exitLocalCoord = new Coord(exitX, exitY);
            }

            // lastTraversed
            portal.lastTraversed = in.readLong();

            portals.add(portal);
        }

        return portals;
    }

    // ============== Connected Chunks ==============

    private static void writeConnectedChunks(Set<Long> chunks, DataOutputStream out) throws IOException {
        out.writeShort(chunks.size());
        for (Long gridId : chunks) {
            out.writeLong(gridId);
        }
    }

    private static Set<Long> readConnectedChunks(DataInputStream in) throws IOException {
        int count = in.readShort() & 0xFFFF;
        Set<Long> chunks = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            chunks.add(in.readLong());
        }
        return chunks;
    }

    // ============== Reachable Areas ==============

    private static void writeReachableAreas(Set<Integer> areas, DataOutputStream out) throws IOException {
        out.writeShort(areas.size());
        for (Integer areaId : areas) {
            out.writeInt(areaId);
        }
    }

    private static Set<Integer> readReachableAreas(DataInputStream in) throws IOException {
        int count = in.readShort() & 0xFFFF;
        Set<Integer> areas = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            areas.add(in.readInt());
        }
        return areas;
    }

    // ============== Conversion Helpers ==============

    private static byte layerToByte(String layer) {
        if (layer == null) return LAYER_OUTSIDE;
        switch (layer) {
            case "inside": return LAYER_INSIDE;
            case "cellar": return LAYER_CELLAR;
            default: return LAYER_OUTSIDE;
        }
    }

    private static String byteToLayer(byte b) {
        switch (b) {
            case LAYER_INSIDE: return "inside";
            case LAYER_CELLAR: return "cellar";
            default: return "outside";
        }
    }

    private static byte portalTypeToByte(ChunkPortal.PortalType type) {
        if (type == null) return PORTAL_UNKNOWN;
        switch (type) {
            case DOOR: return PORTAL_DOOR;
            case GATE: return PORTAL_GATE;
            case STAIRS_UP: return PORTAL_STAIRS_UP;
            case STAIRS_DOWN: return PORTAL_STAIRS_DOWN;
            case CELLAR: return PORTAL_CELLAR;
            case MINE_ENTRANCE: return PORTAL_MINE_ENTRANCE;
            case MINEHOLE: return PORTAL_MINEHOLE;
            case LADDER: return PORTAL_LADDER;
            default: return PORTAL_UNKNOWN;
        }
    }

    private static ChunkPortal.PortalType byteToPortalType(byte b) {
        switch (b) {
            case PORTAL_DOOR: return ChunkPortal.PortalType.DOOR;
            case PORTAL_GATE: return ChunkPortal.PortalType.GATE;
            case PORTAL_STAIRS_UP: return ChunkPortal.PortalType.STAIRS_UP;
            case PORTAL_STAIRS_DOWN: return ChunkPortal.PortalType.STAIRS_DOWN;
            case PORTAL_CELLAR: return ChunkPortal.PortalType.CELLAR;
            case PORTAL_MINE_ENTRANCE: return ChunkPortal.PortalType.MINE_ENTRANCE;
            case PORTAL_MINEHOLE: return ChunkPortal.PortalType.MINEHOLE;
            case PORTAL_LADDER: return ChunkPortal.PortalType.LADDER;
            default: return null; // Unknown type - don't default to DOOR
        }
    }
}
