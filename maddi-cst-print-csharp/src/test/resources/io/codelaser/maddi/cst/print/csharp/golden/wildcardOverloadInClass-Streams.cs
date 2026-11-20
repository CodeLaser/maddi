namespace A;
internal sealed class Streams {
    internal abstract class Base<A> : IStream<A> {
        public int GroupBy<K, X>(IMapper<A, K> key, ICollector<A, X> collector) {
            collector.Finish();
            return 1;
        }
        private int GroupBy<K1, K2>(IMapper<A, K1> keyA, IMapper<A, K2> keyB) => 2;
    }

    internal interface ICollector<A, R, out X> : ICollector<A, X> {
        X Finish();
        int Size();
        X ICollector<A, X>.Finish() => Finish();
        int ICollector<A, X>.Size() => Size();
    }
    internal interface ICollector<A, out X> {
        X Finish();
        int Size();
    }

    internal sealed class Holder<A> {
        internal ICollector<A, string> any;
        internal string Done() => any.Finish();
    }

    internal interface IMapper<in A, out K> {
        K Map(A a);
        int Arity();
    }
    internal interface IStream<A> { int GroupBy<K, X>(IMapper<A, K> key, ICollector<A, X> collector); }
}
