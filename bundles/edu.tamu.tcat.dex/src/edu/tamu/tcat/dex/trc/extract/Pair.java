package edu.tamu.tcat.dex.trc.extract;

@Deprecated
public class Pair<A, B>
{
   public A first;
   public B second;

   public static <A,B> Pair<A,B> of(A first, B second)
   {
      Pair<A,B> pair = new Pair<>();
      pair.first = first;
      pair.second = second;
      return pair;
   }
}
