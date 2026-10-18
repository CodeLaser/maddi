using Maddi.JavaCompat;
using System.Collections.Generic;
namespace Org.Example.Longtypes;
internal sealed class Notifiables {
    internal static Notifiable<S> Build<S>(object scoreDirector, object variableListener, int globalOrder) {
        if (variableListener is ListenerOfAnEvenLongerNameStill<object, object> basicVariableListener) {
            return new Notifiable<S>(
                scoreDirector,
                (ListenerOfAnEvenLongerNameStill<S, object>) basicVariableListener,
                basicVariableListener()
                    ? new OrderedSetOfSomeLength<INotificationWithALongName<S, IChangeEventOfAVeryLongName<object>, ListenerOfAnEvenLongerNameStill<S, object>>>()
                    :
                    new List<INotificationWithALongName<S, IChangeEventOfAVeryLongName<object>, ListenerOfAnEvenLongerNameStill<S, object>>>(),
                globalOrder);
        }
        return null;
    }
    internal interface IChangeEventOfAVeryLongName<T> { }
    internal delegate bool ListenerOfAnEvenLongerNameStill<S, T>();
    internal sealed class Notifiable<S> {
        internal Notifiable(
            object scoreDirector,
            ListenerOfAnEvenLongerNameStill<S, object> listener,
            ICollection<INotificationWithALongName<S, IChangeEventOfAVeryLongName<object>, ListenerOfAnEvenLongerNameStill<S, object>>> queue,
            int globalOrder) { }
    }
    internal interface INotificationWithALongName<S, E, L> { }
    internal sealed class OrderedSetOfSomeLength<E> : JavaSet<E> {
        public override IJavaIterator<E> Iterator() => new JavaIterator<E>(new List<E>());
        public override int Size() => 0;
    }
}
