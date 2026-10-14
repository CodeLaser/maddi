namespace Org.Example.Flow;
internal sealed class Loops {
    internal static int Search(int[][] grid, int target) {
        int found = -1;
        for (int i = 0; i < grid.Length; i++) {
            for (int j = 0; j < grid[i].Length; j++) {
                if (grid[i][j] < 0) { goto outer_continue; }
                if (grid[i][j] == target) {
                    found = i;
                    goto outer_break;
                }
            }
            outer_continue: ;
        }
        outer_break: ;
        return found;
    }

    internal static long Sum(params int[] values) {
        long total = 0;
        foreach (int v in values) { total += v; }
        int k = 0;
        do { k++; } while (k < 3);
        while (k > 0) { k--; }
        return total;
    }
}
