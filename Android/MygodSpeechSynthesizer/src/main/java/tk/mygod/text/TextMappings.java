package tk.mygod.text;

import java.util.ArrayList;

/**
 * Add markers and convert offsets from two texts super conveniently!
 * @author Mygod
 */
public class TextMappings {
    private static class Mapping {
        private Mapping(int source, int target) {
            sourceOffset = source;
            targetOffset = target;
        }

        private int sourceOffset, targetOffset;
    }

    private ArrayList<Mapping> mappings = new ArrayList<>();

    /**
     * Add a mapping between two offsets.
     * @param source Source offset.
     * @param target Target offset.
     */
    public void addMapping(int source, int target) {
        int size = mappings.size();
        if (size > 0) { // time & space improvement
            Mapping last = mappings.get(size - 1);
            if (source == last.sourceOffset && target == last.targetOffset) return;
        }
        mappings.add(new Mapping(source, target));
    }

    /**
     * Get source offset from text offset. Takes O(log n) where n is the number of tags. Thread-safe.
     * @param targetOffset Target offset.
     * @param preferLeft If there is an tag at the specified offset, go as left as possible.
     *                   Otherwise, go as right as possible.
     * @return Source offset.
     */
    public int getSourceOffset(int targetOffset, boolean preferLeft) {
        int l = 0, r = mappings.size();
        while (l < r) {
            int mid = (l + r) >> 1;
            int pos = mappings.get(mid).targetOffset;
            if (targetOffset < pos || targetOffset == pos && preferLeft) r = mid;
            else l = mid + 1;
        }
        Mapping mapping = mappings.get(preferLeft ? l : l - 1);
        return mapping.sourceOffset + targetOffset - mapping.targetOffset;
    }

    /**
     * Get target offset from text offset. Takes O(log n) where n is the number of tags. Thread-safe.
     * @param sourceOffset Source offset.
     * @param preferLeft If there is an tag at the specified offset, go as left as possible.
     *                   Otherwise, go as right as possible.
     * @return Target offset.
     */
    public int getTargetOffset(int sourceOffset, boolean preferLeft) {
        int l = 0, r = mappings.size();
        while (l < r) {
            int mid = (l + r) >> 1;
            int pos = mappings.get(mid).sourceOffset;
            if (sourceOffset < pos || sourceOffset == pos && preferLeft) r = mid;
            else l = mid + 1;
        }
        Mapping mapping = mappings.get(preferLeft ? l : l - 1);
        return mapping.targetOffset + sourceOffset - mapping.sourceOffset;
    }
}
