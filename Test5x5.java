import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Test5x5 {
    public static void main(String[] args) throws Exception {
        PuzzleGame g = new PuzzleGame("tester", 5);
        Field boardF = PuzzleGame.class.getDeclaredField("board");
        boardF.setAccessible(true);
        int[] board = (int[]) boardF.get(g);

        // 1. board length
        if (board.length != 25) throw new AssertionError("board length=" + board.length);

        // 2. shuffled board is a permutation of 0..24
        int[] sorted = board.clone();
        Arrays.sort(sorted);
        for (int i = 0; i < 25; i++) {
            if (sorted[i] != i) throw new AssertionError("not a permutation: " + Arrays.toString(sorted));
        }

        // 3. solved-state check works
        for (int i = 0; i < 24; i++) board[i] = i + 1;
        board[24] = 0;
        Method isSolved = PuzzleGame.class.getDeclaredMethod("isSolved");
        isSolved.setAccessible(true);
        if (!(boolean) isSolved.invoke(g)) throw new AssertionError("solved board not detected");

        // 4. adjacency at corners/center
        Method nbr = PuzzleGame.class.getDeclaredMethod("neighborsOf", int.class);
        nbr.setAccessible(true);
        java.util.List<?> corner = (java.util.List<?>) nbr.invoke(g, 0);
        java.util.List<?> inner = (java.util.List<?>) nbr.invoke(g, 12);
        if (corner.size() != 2) throw new AssertionError("corner neighbors=" + corner.size());
        if (inner.size() != 4) throw new AssertionError("inner neighbors=" + inner.size());

        System.out.println("TEST_5x5_OK  shuffled=" + Arrays.toString(board));
        System.exit(0);
    }
}
