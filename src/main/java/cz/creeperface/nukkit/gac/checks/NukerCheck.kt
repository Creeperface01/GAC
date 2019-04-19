package cz.creeperface.nukkit.gac.checks

import cn.nukkit.Player
import cn.nukkit.block.Block
import cn.nukkit.level.Location
import cn.nukkit.math.AxisAlignedBB
import cn.nukkit.math.NukkitMath
import cn.nukkit.math.SimpleAxisAlignedBB
import cn.nukkit.math.Vector3
import cn.nukkit.utils.BlockIterator
import cz.creeperface.nukkit.gac.utils.mutable
import java.util.*

/**
 * Created by CreeperFace on 19. 12. 2016.
 */
object NukerCheck {

    fun run(p: Player, b: Block): Boolean {
        /*MovingObjectPosition mop = rayTraceBlocks(p.getLevel(), new Vector3(p.x, p.y + p.getEyeHeight(), p.z), b, false, true, false);

        return mop == null;*/
        val pos = p.location.add(0.0, p.eyeHeight.toDouble(), 0.0)
        var bb: AxisAlignedBB? = b.boundingBox?.mutable()

        if (bb == null) {
            bb = SimpleAxisAlignedBB(b.x, b.y, b.z, b.x + 1, b.y + 1, b.z + 1)
        }

        bb.expand(-0.01, -0.01, -0.01)

        //HashSet<Integer> sides = new HashSet<>();
        val positions = HashSet<Vector3>()

        if (pos.x > bb.maxX || pos.x < bb.minX) {
            if (pos.x > b.x) {
                //sides.add(Vector3.SIDE_EAST);
                positions.add(Vector3(bb.maxX, bb.maxY, bb.maxZ))
                positions.add(Vector3(bb.maxX, bb.maxY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.maxZ))
            } else {
                //sides.add(Vector3.SIDE_WEST);
                positions.add(Vector3(bb.minX, bb.maxY, bb.maxZ))
                positions.add(Vector3(bb.minX, bb.maxY, bb.minZ))
                positions.add(Vector3(bb.minX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.minX, bb.minY, bb.maxZ))
            }
        }

        if (pos.z > bb.maxZ || pos.z < bb.minZ) {
            if (pos.z > b.z) {
                //sides.add(Vector3.SIDE_SOUTH);
                positions.add(Vector3(bb.minX, bb.maxY, bb.maxZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.maxZ))
                positions.add(Vector3(bb.minX, bb.minY, bb.maxZ))
                positions.add(Vector3(bb.maxX, bb.maxY, bb.maxZ))
            } else {
                //sides.add(Vector3.SIDE_NORTH);
                positions.add(Vector3(bb.minX, bb.maxY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.minX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.maxY, bb.minZ))
            }
        }

        if (pos.y > bb.maxY || pos.y < bb.minY) {
            if (pos.y > b.y) {
                //sides.add(Vector3.SIDE_UP);
                positions.add(Vector3(bb.minX, bb.maxY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.maxY, bb.minZ))
                positions.add(Vector3(bb.minX, bb.maxY, bb.maxZ))
                positions.add(Vector3(bb.maxX, bb.maxY, bb.maxZ))
            } else {
                //sides.add(Vector3.SIDE_DOWN);
                positions.add(Vector3(bb.minX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.minX, bb.minY, bb.maxZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.maxZ))
            }
        }

        if (positions.isEmpty()) { //in block probably
            return true
        }

        //HashSet<BlockVector3> airPositions = new HashSet<>();
        //HashSet<BlockVector3> solidPositions = new HashSet<>();

        for (corner in positions) {
            val x = corner.x - pos.x
            val y = corner.y - pos.y
            val z = corner.z - pos.z

            val diff = Math.abs(x) + Math.abs(z)

            val yaw = Math.toDegrees(-Math.atan2(x / diff, z / diff))
            val pitch = if (y == 0.0) 0.toDouble() else Math.toDegrees(-Math.atan2(y, Math.sqrt(x * x + z * z)))
            val found = getTargetBlock(Location(pos.x, pos.y, pos.z, yaw, pitch, b.getLevel()), NukkitMath.ceilDouble(pos.distance(corner) + 2), arrayOf(Block.AIR))

            if (corner.distance(pos) <= 0.5 || found != null && found == b) {
                return true
            } else if (found == null) {
                //System.out.println("null found");
                return false
            }

            /*boolean found = targetBlock(new Location(pos.x, pos.y, pos.z, yaw, pitch, b.getLevel()), b);

            if(found) {
                return true;
            }*/
        }

        /*PlayerData data = ((SynapsePlayer) p).data; //TODO
        data.antiCheatData.nukerPoints++;

        MTCore.getInstance().doKickCheck(data, p);*/
        return false
    }

    /*private static MovingObjectPosition rayTraceBlocks(Level level, Vector3 pos1, Vector3 pos2, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
        int i = NukkitMath.floorDouble(pos2.x);
        int j = NukkitMath.floorDouble(pos2.y);
        int k = NukkitMath.floorDouble(pos2.z);
        int l = NukkitMath.floorDouble(pos1.x);

        int i1 = NukkitMath.floorDouble(pos1.y);
        int j1 = NukkitMath.floorDouble(pos1.z);
        Vector3 blockpos = new Vector3(l, i1, j1);
        Block block = level.getBlock(blockpos);


        if ((!ignoreBlockWithoutBoundingBox || block.getBoundingBox() != null) && block.getId() == 0) {
            MovingObjectPosition ob = rayTrace(block, pos1, pos2);

            if (ob != null) {
                return ob;
            }
        }

        MovingObjectPosition movingObjectPosition = null;
        int k1 = 200;

        while (k1-- >= 0) {
            if (Double.isNaN(pos1.x) || Double.isNaN(pos1.y) || Double.isNaN(pos1.z)) {
                return null;
            }

            if (l == i && i1 == j && j1 == k) {
                return returnLastUncollidableBlock ? movingObjectPosition : null;
            }

            boolean flag2 = true;
            boolean flag = true;
            boolean flag1 = true;
            double d0 = 999.0D;
            double d1 = 999.0D;
            double d2 = 999.0D;

            if (i > l) {
                d0 = (double) l + 1.0D;
            } else if (i < l) {
                d0 = (double) l + 0.0D;
            } else {
                flag2 = false;
            }

            if (j > i1) {
                d1 = (double) i1 + 1.0D;
            } else if (j < i1) {
                d1 = (double) i1 + 0.0D;
            } else {
                flag = false;
            }

            if (k > j1) {
                d2 = (double) j1 + 1.0D;
            } else if (k < j1) {
                d2 = (double) j1 + 0.0D;
            } else {
                flag1 = false;
            }

            double d3 = 999.0D;
            double d4 = 999.0D;
            double d5 = 999.0D;
            double d6 = pos2.x - pos1.x;
            double d7 = pos2.y - pos1.y;
            double d8 = pos2.z - pos1.z;

            if (flag2) {
                d3 = (d0 - pos1.x) / d6;
            }

            if (flag) {
                d4 = (d1 - pos1.y) / d7;
            }

            if (flag1) {
                d5 = (d2 - pos1.z) / d8;
            }

            if (d3 == -0.0D) {
                d3 = -1.0E-4D;
            }

            if (d4 == -0.0D) {
                d4 = -1.0E-4D;
            }

            if (d5 == -0.0D) {
                d5 = -1.0E-4D;
            }

            int side;

            if (d3 < d4 && d3 < d5) {
                side = i > l ? Vector3.SIDE_WEST : Vector3.SIDE_EAST;
                pos1 = new Vector3(d0, pos1.y + d7 * d3, pos1.z + d8 * d3);
            } else if (d4 < d5) {
                side = j > i1 ? Vector3.SIDE_DOWN : Vector3.SIDE_UP;
                pos1 = new Vector3(pos1.x + d6 * d4, d1, pos1.z + d8 * d4);
            } else {
                side = k > j1 ? Vector3.SIDE_NORTH : Vector3.SIDE_SOUTH;
                pos1 = new Vector3(pos1.x + d6 * d5, pos1.y + d7 * d5, d2);
            }

            l = NukkitMath.floorDouble(pos1.x) - (side == Vector3.SIDE_EAST ? 1 : 0);
            i1 = NukkitMath.floorDouble(pos1.y) - (side == Vector3.SIDE_UP ? 1 : 0);
            j1 = NukkitMath.floorDouble(pos1.z) - (side == Vector3.SIDE_SOUTH ? 1 : 0);
            blockpos = new Vector3(l, i1, j1);
            Block block1 = level.getBlock(blockpos);

            if (!ignoreBlockWithoutBoundingBox || block1.getId() == Block.NETHER_PORTAL || block1.getBoundingBox() != null) {
                if (block1.getId() == 0) {
                    MovingObjectPosition raytraceresult1 = rayTrace(block1, pos1, pos2);

                    if (raytraceresult1 != null) {
                        return raytraceresult1;
                    }
                } else {
                    movingObjectPosition = new MovingObjectPosition();
                    movingObjectPosition.typeOfHit = -1;
                    movingObjectPosition.blockX = blockpos.getFloorX();
                    movingObjectPosition.blockY = blockpos.getFloorY();
                    movingObjectPosition.blockZ = blockpos.getFloorZ();
                    movingObjectPosition.hitVector = pos1;
                    movingObjectPosition.sideHit = side;
                }
            }
        }

        return returnLastUncollidableBlock ? movingObjectPosition : null;
    }

    private static MovingObjectPosition rayTrace(Block b, Vector3 pos1, Vector3 pos2) {
        Vector3 pos3 = pos1.subtract(b.getX(), b.getY(), b.getZ());
        Vector3 pos4 = pos2.subtract(b.getX(), b.getY(), b.getZ());

        MovingObjectPosition result = b.getBoundingBox().calculateIntercept(pos3, pos4);
        if (result == null) {
            return null;
        }

        MovingObjectPosition mp = new MovingObjectPosition();
        mp.blockX = NukkitMath.floorDouble(b.x);
        mp.blockY = NukkitMath.floorDouble(b.y);
        mp.blockZ = NukkitMath.floorDouble(b.z);
        mp.hitVector = result.hitVector.add(b.x, b.y, b.z);
        mp.sideHit = result.sideHit;
        mp.typeOfHit = 0;

        return mp;
    }*/

    private fun getDirection(pos: Vector3, pos2: Vector3): Vector3 {
        val x = pos2.x - pos.x
        val y = pos2.y - pos.y
        val z = pos2.z - pos.z

        val diff = Math.abs(x) + Math.abs(z)

        val yaw2 = Math.toDegrees(-Math.atan2(x / diff, z / diff))
        val pitch2 = if (y == 0.0) 0.toDouble() else Math.toDegrees(-Math.atan2(y, Math.sqrt(x * x + z * z)))

        val pitch = (pitch2 + 90) * Math.PI / 180
        val yaw = (yaw2 + 90) * Math.PI / 180
        val x2 = Math.sin(pitch) * Math.cos(yaw)
        val z2 = Math.sin(pitch) * Math.sin(yaw)
        val y2 = Math.cos(pitch)
        return Vector3(x2, y2, z2).normalize()
    }

    private fun getTargetBlock(pos: Location, maxDistance: Int, transparent: Array<Int>?): Block? {
        try {
            val blocks = getLineOfSight(pos, maxDistance, 1, transparent)
            val block = blocks[0]

            if (transparent != null && transparent.size != 0) {
                if (Arrays.binarySearch(transparent, block.id) < 0) {
                    return block
                }
            } else {
                return block
            }
        } catch (ignored: Exception) {

        }

        return null
    }

    private fun getLineOfSight(pos: Location, maxDistance: Int, maxLength: Int, transparent: Array<Int>?): Array<Block> {
        var maxDistance = maxDistance
        var transparent = transparent
        if (maxDistance > 120) {
            maxDistance = 120
        }

        if (transparent != null && transparent.size == 0) {
            transparent = null
        }

        val blocks = ArrayList<Block>()

        val direction = pos.directionVector
        val itr = BlockIterator(pos.level, pos, direction, 0.0, maxDistance)

        while (itr.hasNext()) {
            val block = itr.next()
            blocks.add(block)

            if (maxLength != 0 && blocks.size > maxLength) {
                blocks.removeAt(0)
            }

            val id = block.id

            /*if(id != 0) {
                Vector3 intersectsPos = direction.multiply(block.x / direction.x);

                System.out.println("int pos: "+intersectsPos.x+":"+intersectsPos.y+":"+intersectsPos.z+"    blockpos: "+block.x+":"+block.y+":"+block.z+"     "+block.getName());

                if(block.getBoundingBox() != null && block.getBoundingBox().isVectorInside(intersectsPos)) {
                    System.out.println("vector inside");
                    break;
                }
            }*/

            if (!block.isTransparent) {
                break
            }

            /*if (transparent == null) {
                if (!block.isTransparent()) {
                    break;
                }
            } else {
                if (Arrays.binarySearch(transparent, id) < 0) {
                    break;
                }
            }*/
        }

        return blocks.toTypedArray()
    }

    private fun targetBlock(pos: Location, blockPos: Vector3): Boolean {
        val direction = pos.directionVector.add(pos.x, pos.y, pos.z)

        var currentVector = direction.clone()
        //System.out.println("startVector: " + currentVector);

        while (currentVector.distance(direction) < 9) {
            val block = pos.getLevel().getBlock(currentVector)

            if (block == blockPos) {
                return true
            }

            if (block.id != 0 && block.boundingBox != null && block.boundingBox.isVectorInside(currentVector)) {
                //System.out.println("solid: " + block.getName());
                return false
            }

            val absX = Math.abs(currentVector.x)
            val absY = Math.abs(currentVector.y)
            val absZ = Math.abs(currentVector.z)

            var diffX = Math.ceil(absX) - absX
            var diffZ = Math.ceil(absZ) - absZ
            var diffY = Math.ceil(absY) - absY

            if (diffX == 0.0) {
                diffX = 1.0
            }

            if (diffY == 0.0) {
                diffY = 1.0
            }

            if (diffZ == 0.0) {
                diffZ = 1.0
            }

            val currentAmplifier: Double

            if (diffX < diffZ) {
                if (diffY < diffX) {
                    currentAmplifier = (absY + diffY) / absY
                } else {
                    currentAmplifier = (absX + diffX) / absX
                }
            } else {
                if (diffY < diffZ) {
                    currentAmplifier = (absY + diffY) / absY
                } else {
                    currentAmplifier = (absZ + diffZ) / absZ
                }
            }

            currentVector = currentVector.multiply(currentAmplifier)
            /*currentVector.x = (Math.abs(currentVector.x) + currentAmplifier) * (currentVector.x / (Math.abs(currentVector.x)));
            currentVector.y = (Math.abs(currentVector.y) + currentAmplifier) * (currentVector.y / (Math.abs(currentVector.y)));
            currentVector.z = (Math.abs(currentVector.z) + currentAmplifier) * (currentVector.z / (Math.abs(currentVector.z)));*/

            //System.out.println("diff: " + currentAmplifier + "     " + currentVector.toString());
        }

        //System.out.println("distance false");
        return false
    }
}
