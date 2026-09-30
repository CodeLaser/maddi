package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.eclipse.collections.api.*;
import org.eclipse.collections.api.bag.*;
import org.eclipse.collections.api.bag.primitive.*;
import org.eclipse.collections.api.bag.sorted.ImmutableSortedBag;
import org.eclipse.collections.api.bag.sorted.MutableSortedBag;
import org.eclipse.collections.api.bimap.ImmutableBiMap;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.block.comparator.primitive.*;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.*;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.*;
import org.eclipse.collections.api.collection.primitive.*;
import org.eclipse.collections.api.iterator.*;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.*;
import org.eclipse.collections.api.map.*;
import org.eclipse.collections.api.map.primitive.ObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.ObjectLongMap;
import org.eclipse.collections.api.map.sorted.MutableSortedMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.partition.PartitionIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.*;
import org.eclipse.collections.api.set.sorted.ImmutableSortedSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.tuple.Pair;
import io.codelaser.maddi.annotation.FinalFields;
import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
@Independent(absent = true)
public class OrgEclipseCollectionsApi {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api";
    //public interface BooleanIterable implements PrimitiveIterable
    @FinalFields
    @Independent
    class BooleanIterable$ {
        @NotModified boolean allSatisfy(@Independent @NotModified BooleanPredicate arg0) { return false; }
        @NotModified boolean anySatisfy(@Independent @NotModified BooleanPredicate arg0) { return false; }
        @Independent @Modified LazyBooleanIterable asLazy() { return null; }
        @Independent @Modified BooleanIterator booleanIterator() { return null; }
        @Independent @Modified RichIterable<BooleanIterable> chunk(int size) { return null; }
        @Independent @Modified
        <V> RichIterable<V> collect(@Independent @Modified BooleanToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R collect(
            @Independent @Modified BooleanToObjectFunction<? extends V> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableBooleanCollection> R collectBoolean(
            @Independent @Modified BooleanToBooleanFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableByteCollection> R collectByte(
            @Independent @Modified BooleanToByteFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableCharCollection> R collectChar(
            @Independent @Modified BooleanToCharFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableDoubleCollection> R collectDouble(
            @Independent @Modified BooleanToDoubleFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableFloatCollection> R collectFloat(
            @Independent @Modified BooleanToFloatFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableIntCollection> R collectInt(
            @Independent @Modified BooleanToIntFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableLongCollection> R collectLong(
            @Independent @Modified BooleanToLongFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableShortCollection> R collectShort(
            @Independent @Modified BooleanToShortFunction function,
            @Independent @Modified R target) { return null; }
        @NotModified boolean contains(boolean arg0) { return false; }
        @NotModified boolean containsAll(@Independent @NotModified boolean ... source) { return false; }
        @NotModified boolean containsAll(@Independent @Modified BooleanIterable source) { return false; }
        @Modified boolean containsAny(@Independent @NotModified boolean ... source) { return false; }
        @Modified boolean containsAny(@Independent @Modified BooleanIterable source) { return false; }
        @Modified boolean containsNone(@Independent @NotModified boolean ... source) { return false; }
        @Modified boolean containsNone(@Independent @Modified BooleanIterable source) { return false; }
        @NotModified int count(@Independent @NotModified BooleanPredicate arg0) { return 0; }
        @NotModified
        boolean detectIfNone(@Independent @NotModified BooleanPredicate arg0, boolean arg1) { return false; }
        @Modified void each(@Independent @Modified BooleanProcedure arg0) { }
        @Independent @Modified
        <V, R extends Collection<V>> R flatCollect(
            @Independent @Modified BooleanToObjectFunction<? extends Iterable<V>> function,
            @Independent @Modified R target) { return null; }
        @Modified void forEach(@Independent @Modified BooleanProcedure procedure) { }
        @Independent @NotModified
        <T> T injectInto(
            @Independent @NotModified T arg0,
            @Independent @Modified ObjectBooleanToObjectFunction<? super T, ? extends T> arg1) { return null; }

        @Modified
        boolean injectIntoBoolean(
            boolean injectedValue,
            @Independent @Modified BooleanBooleanToBooleanFunction function) { return false; }

        @Modified
        byte injectIntoByte(byte injectedValue, @Independent @Modified ByteBooleanToByteFunction function) { return 0; }

        @Modified
        char injectIntoChar(char injectedValue, @Independent @Modified CharBooleanToCharFunction function) { return '\0'; }

        @Modified
        double injectIntoDouble(double injectedValue, @Independent @Modified DoubleBooleanToDoubleFunction function) {
            return 0.0;
        }

        @Modified
        float injectIntoFloat(float injectedValue, @Independent @Modified FloatBooleanToFloatFunction function) {
            return 0.0F;
        }

        @Modified
        int injectIntoInt(int injectedValue, @Independent @Modified IntBooleanToIntFunction function) { return 0; }

        @Modified
        long injectIntoLong(long injectedValue, @Independent @Modified LongBooleanToLongFunction function) { return 0L; }

        @Modified
        short injectIntoShort(short injectedValue, @Independent @Modified ShortBooleanToShortFunction function) {
            return 0;
        }
        @NotModified boolean noneSatisfy(@Independent @NotModified BooleanPredicate predicate) { return false; }
        @Modified boolean reduce(@Independent @Modified BooleanBooleanToBooleanFunction accumulator) { return false; }
        @Modified
        boolean reduceIfEmpty(@Independent @Modified BooleanBooleanToBooleanFunction accumulator, boolean defaultValue) {
            return false;
        }
        @Independent @Modified BooleanIterable reject(@Independent @NotModified BooleanPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableBooleanCollection> R reject(
            @Independent @NotModified BooleanPredicate predicate,
            @Independent @Modified R target) { return null; }
        @Independent @Modified BooleanIterable select(@Independent @NotModified BooleanPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableBooleanCollection> R select(
            @Independent @NotModified BooleanPredicate predicate,
            @Independent @Modified R target) { return null; }

        @Fluent @Independent @Modified
        BooleanIterable tap(@Independent @Modified BooleanProcedure procedure) { return null; }
        @Independent @NotModified boolean [] toArray() { return null; }
        @Independent @NotModified boolean [] toArray(@Independent @NotModified boolean [] target) { return null; }
        @Independent @NotModified MutableBooleanBag toBag() { return null; }
        @Independent @NotModified MutableBooleanList toList() { return null; }
        @Independent @NotModified MutableBooleanSet toSet() { return null; }
    }

    //public interface ByteIterable implements PrimitiveIterable
    @FinalFields
    @Independent
    class ByteIterable$ {
        @NotModified boolean allSatisfy(@Independent @NotModified BytePredicate arg0) { return false; }
        @NotModified boolean anySatisfy(@Independent @NotModified BytePredicate arg0) { return false; }
        @Independent @Modified LazyByteIterable asLazy() { return null; }
        @NotModified double average() { return 0.0; }
        @Modified double averageIfEmpty(double defaultValue) { return 0.0; }
        @Independent @Modified ByteIterator byteIterator() { return null; }
        @Independent @Modified RichIterable<ByteIterable> chunk(int size) { return null; }
        @Independent @Modified
        <V> RichIterable<V> collect(@Independent @Modified ByteToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R collect(
            @Independent @Modified ByteToObjectFunction<? extends V> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableBooleanCollection> R collectBoolean(
            @Independent @Modified ByteToBooleanFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableByteCollection> R collectByte(
            @Independent @Modified ByteToByteFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableCharCollection> R collectChar(
            @Independent @Modified ByteToCharFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableDoubleCollection> R collectDouble(
            @Independent @Modified ByteToDoubleFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableFloatCollection> R collectFloat(
            @Independent @Modified ByteToFloatFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableIntCollection> R collectInt(
            @Independent @Modified ByteToIntFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableLongCollection> R collectLong(
            @Independent @Modified ByteToLongFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableShortCollection> R collectShort(
            @Independent @Modified ByteToShortFunction function,
            @Independent @Modified R target) { return null; }
        @NotModified boolean contains(byte arg0) { return false; }
        @NotModified boolean containsAll(@Independent @NotModified byte ... source) { return false; }
        @NotModified boolean containsAll(@Independent @Modified ByteIterable source) { return false; }
        @Modified boolean containsAny(@Independent @NotModified byte ... source) { return false; }
        @Modified boolean containsAny(@Independent @Modified ByteIterable source) { return false; }
        @Modified boolean containsNone(@Independent @NotModified byte ... source) { return false; }
        @Modified boolean containsNone(@Independent @Modified ByteIterable source) { return false; }
        @NotModified int count(@Independent @NotModified BytePredicate arg0) { return 0; }
        @NotModified byte detectIfNone(@Independent @NotModified BytePredicate arg0, byte arg1) { return 0; }
        @Modified void each(@Independent @Modified ByteProcedure arg0) { }
        @Independent @Modified
        <V, R extends Collection<V>> R flatCollect(
            @Independent @Modified ByteToObjectFunction<? extends Iterable<V>> function,
            @Independent @Modified R target) { return null; }
        @Modified void forEach(@Independent @Modified ByteProcedure procedure) { }
        @Independent @NotModified
        <T> T injectInto(
            @Independent @NotModified T arg0,
            @Independent @Modified ObjectByteToObjectFunction<? super T, ? extends T> arg1) { return null; }

        @Modified
        boolean injectIntoBoolean(boolean injectedValue, @Independent @Modified BooleanByteToBooleanFunction function) {
            return false;
        }

        @Modified
        byte injectIntoByte(byte injectedValue, @Independent @Modified ByteByteToByteFunction function) { return 0; }

        @Modified
        char injectIntoChar(char injectedValue, @Independent @Modified CharByteToCharFunction function) { return '\0'; }

        @Modified
        double injectIntoDouble(double injectedValue, @Independent @Modified DoubleByteToDoubleFunction function) {
            return 0.0;
        }

        @Modified
        float injectIntoFloat(float injectedValue, @Independent @Modified FloatByteToFloatFunction function) {
            return 0.0F;
        }

        @Modified
        int injectIntoInt(int injectedValue, @Independent @Modified IntByteToIntFunction function) { return 0; }

        @Modified
        long injectIntoLong(long injectedValue, @Independent @Modified LongByteToLongFunction function) { return 0L; }

        @Modified
        short injectIntoShort(short injectedValue, @Independent @Modified ShortByteToShortFunction function) { return 0; }
        @NotModified byte max() { return 0; }
        @Modified byte maxIfEmpty(byte arg0) { return 0; }
        @NotModified double median() { return 0.0; }
        @Modified double medianIfEmpty(double defaultValue) { return 0.0; }
        @NotModified byte min() { return 0; }
        @Modified byte minIfEmpty(byte arg0) { return 0; }
        @NotModified boolean noneSatisfy(@Independent @NotModified BytePredicate predicate) { return false; }
        @Modified long reduce(@Independent @Modified LongByteToLongFunction accumulator) { return 0L; }
        @Modified
        long reduceIfEmpty(@Independent @Modified LongByteToLongFunction accumulator, long defaultValue) { return 0L; }
        @Independent @Modified ByteIterable reject(@Independent @NotModified BytePredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableByteCollection> R reject(
            @Independent @NotModified BytePredicate predicate,
            @Independent @Modified R target) { return null; }
        @Independent @Modified ByteIterable select(@Independent @NotModified BytePredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableByteCollection> R select(
            @Independent @NotModified BytePredicate predicate,
            @Independent @Modified R target) { return null; }
        @NotModified long sum() { return 0L; }
        @Independent @Modified IntSummaryStatistics summaryStatistics() { return null; }
        @Fluent @Independent @Modified
        ByteIterable tap(@Independent @Modified ByteProcedure procedure) { return null; }
        @Independent @NotModified byte [] toArray() { return null; }
        @Independent @NotModified byte [] toArray(@Independent @NotModified byte [] target) { return null; }
        @Independent @NotModified MutableByteBag toBag() { return null; }
        @Independent @NotModified MutableByteList toList() { return null; }
        @Independent @NotModified MutableByteSet toSet() { return null; }
        @Independent @Modified byte [] toSortedArray() { return null; }
        @Independent @NotModified MutableByteList toSortedList() { return null; }
        @Independent @NotModified
        MutableByteList toSortedList(@Independent @Modified ByteComparator comparator) { return null; }

        @Independent @Modified
        <T> MutableByteList toSortedListBy(@Independent @Modified ByteToObjectFunction<T> function) { return null; }

        @Independent @Modified
        <T> MutableByteList toSortedListBy(
            @Independent @Modified ByteToObjectFunction<T> function,
            @Independent @NotModified Comparator<? super T> comparator) { return null; }
    }

    //public interface CharIterable implements PrimitiveIterable
    @FinalFields
    @Independent
    class CharIterable$ {
        @NotModified boolean allSatisfy(@Independent @NotModified CharPredicate arg0) { return false; }
        @NotModified boolean anySatisfy(@Independent @NotModified CharPredicate arg0) { return false; }
        @Independent @Modified LazyCharIterable asLazy() { return null; }
        @NotModified double average() { return 0.0; }
        @Modified double averageIfEmpty(double defaultValue) { return 0.0; }
        @Independent @Modified CharIterator charIterator() { return null; }
        @Independent @Modified RichIterable<CharIterable> chunk(int size) { return null; }
        @Independent @Modified
        <V> RichIterable<V> collect(@Independent @Modified CharToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R collect(
            @Independent @Modified CharToObjectFunction<? extends V> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableBooleanCollection> R collectBoolean(
            @Independent @Modified CharToBooleanFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableByteCollection> R collectByte(
            @Independent @Modified CharToByteFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableCharCollection> R collectChar(
            @Independent @NotModified CharToCharFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableDoubleCollection> R collectDouble(
            @Independent @Modified CharToDoubleFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableFloatCollection> R collectFloat(
            @Independent @Modified CharToFloatFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableIntCollection> R collectInt(
            @Independent @Modified CharToIntFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableLongCollection> R collectLong(
            @Independent @Modified CharToLongFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableShortCollection> R collectShort(
            @Independent @Modified CharToShortFunction function,
            @Independent @Modified R target) { return null; }
        @NotModified boolean contains(char arg0) { return false; }
        @NotModified boolean containsAll(@Independent @NotModified char ... source) { return false; }
        @NotModified boolean containsAll(@Independent @Modified CharIterable source) { return false; }
        @Modified boolean containsAny(@Independent @NotModified char ... source) { return false; }
        @Modified boolean containsAny(@Independent @Modified CharIterable source) { return false; }
        @Modified boolean containsNone(@Independent @NotModified char ... source) { return false; }
        @Modified boolean containsNone(@Independent @Modified CharIterable source) { return false; }
        @NotModified int count(@Independent @NotModified CharPredicate arg0) { return 0; }
        @NotModified char detectIfNone(@Independent @NotModified CharPredicate arg0, char arg1) { return '\0'; }
        @Modified void each(@Independent @Modified CharProcedure arg0) { }
        @Independent @Modified
        <V, R extends Collection<V>> R flatCollect(
            @Independent @Modified CharToObjectFunction<? extends Iterable<V>> function,
            @Independent @Modified R target) { return null; }
        @Modified void forEach(@Independent @Modified CharProcedure procedure) { }
        @Independent @NotModified
        <T> T injectInto(
            @Independent @NotModified T arg0,
            @Independent @Modified ObjectCharToObjectFunction<? super T, ? extends T> arg1) { return null; }

        @Modified
        boolean injectIntoBoolean(boolean injectedValue, @Independent @Modified BooleanCharToBooleanFunction function) {
            return false;
        }

        @Modified
        byte injectIntoByte(byte injectedValue, @Independent @Modified ByteCharToByteFunction function) { return 0; }

        @Modified
        char injectIntoChar(char injectedValue, @Independent @Modified CharCharToCharFunction function) { return '\0'; }

        @Modified
        double injectIntoDouble(double injectedValue, @Independent @Modified DoubleCharToDoubleFunction function) {
            return 0.0;
        }

        @Modified
        float injectIntoFloat(float injectedValue, @Independent @Modified FloatCharToFloatFunction function) {
            return 0.0F;
        }

        @Modified
        int injectIntoInt(int injectedValue, @Independent @Modified IntCharToIntFunction function) { return 0; }

        @Modified
        long injectIntoLong(long injectedValue, @Independent @Modified LongCharToLongFunction function) { return 0L; }

        @Modified
        short injectIntoShort(short injectedValue, @Independent @Modified ShortCharToShortFunction function) { return 0; }
        @NotModified char max() { return '\0'; }
        @Modified char maxIfEmpty(char arg0) { return '\0'; }
        @NotModified double median() { return 0.0; }
        @Modified double medianIfEmpty(double defaultValue) { return 0.0; }
        @NotModified char min() { return '\0'; }
        @Modified char minIfEmpty(char arg0) { return '\0'; }
        @NotModified boolean noneSatisfy(@Independent @NotModified CharPredicate predicate) { return false; }
        @Modified long reduce(@Independent @Modified LongCharToLongFunction accumulator) { return 0L; }
        @Modified
        long reduceIfEmpty(@Independent @Modified LongCharToLongFunction accumulator, long defaultValue) { return 0L; }
        @Independent @Modified CharIterable reject(@Independent @NotModified CharPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableCharCollection> R reject(
            @Independent @NotModified CharPredicate predicate,
            @Independent @Modified R target) { return null; }
        @Independent @Modified CharIterable select(@Independent @NotModified CharPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableCharCollection> R select(
            @Independent @NotModified CharPredicate predicate,
            @Independent @Modified R target) { return null; }
        @NotModified long sum() { return 0L; }
        @Independent @Modified IntSummaryStatistics summaryStatistics() { return null; }
        @Fluent @Independent @Modified
        CharIterable tap(@Independent @Modified CharProcedure procedure) { return null; }
        @Independent @NotModified char [] toArray() { return null; }
        @Independent @NotModified char [] toArray(@Independent @NotModified char [] target) { return null; }
        @Independent @NotModified MutableCharBag toBag() { return null; }
        @Independent @NotModified MutableCharList toList() { return null; }
        @Independent @NotModified MutableCharSet toSet() { return null; }
        @Independent @Modified char [] toSortedArray() { return null; }
        @Independent @NotModified MutableCharList toSortedList() { return null; }
        @Independent @NotModified
        MutableCharList toSortedList(@Independent @Modified CharComparator comparator) { return null; }

        @Independent @Modified
        <T> MutableCharList toSortedListBy(@Independent @Modified CharToObjectFunction<T> function) { return null; }

        @Independent @Modified
        <T> MutableCharList toSortedListBy(
            @Independent @Modified CharToObjectFunction<T> function,
            @Independent @NotModified Comparator<? super T> comparator) { return null; }
    }

    //public interface DoubleIterable implements PrimitiveIterable
    @FinalFields
    @Independent
    class DoubleIterable$ {
        @NotModified boolean allSatisfy(@Independent @NotModified DoublePredicate arg0) { return false; }
        @NotModified boolean anySatisfy(@Independent @NotModified DoublePredicate arg0) { return false; }
        @Independent @Modified LazyDoubleIterable asLazy() { return null; }
        @NotModified double average() { return 0.0; }
        @Modified double averageIfEmpty(double defaultValue) { return 0.0; }
        @Independent @Modified RichIterable<DoubleIterable> chunk(int size) { return null; }
        @Independent @Modified
        <V> RichIterable<V> collect(@Independent @Modified DoubleToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R collect(
            @Independent @Modified DoubleToObjectFunction<? extends V> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableBooleanCollection> R collectBoolean(
            @Independent @Modified DoubleToBooleanFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableByteCollection> R collectByte(
            @Independent @Modified DoubleToByteFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableCharCollection> R collectChar(
            @Independent @Modified DoubleToCharFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableDoubleCollection> R collectDouble(
            @Independent @Modified DoubleToDoubleFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableFloatCollection> R collectFloat(
            @Independent @Modified DoubleToFloatFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableIntCollection> R collectInt(
            @Independent @Modified DoubleToIntFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableLongCollection> R collectLong(
            @Independent @Modified DoubleToLongFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableShortCollection> R collectShort(
            @Independent @Modified DoubleToShortFunction function,
            @Independent @Modified R target) { return null; }
        @NotModified boolean contains(double arg0) { return false; }
        @NotModified boolean containsAll(@Independent @NotModified double ... source) { return false; }
        @NotModified boolean containsAll(@Independent @Modified DoubleIterable source) { return false; }
        @Modified boolean containsAny(@Independent @NotModified double ... source) { return false; }
        @Modified boolean containsAny(@Independent @Modified DoubleIterable source) { return false; }
        @Modified boolean containsNone(@Independent @NotModified double ... source) { return false; }
        @Modified boolean containsNone(@Independent @Modified DoubleIterable source) { return false; }
        @NotModified int count(@Independent @NotModified DoublePredicate arg0) { return 0; }
        @NotModified double detectIfNone(@Independent @NotModified DoublePredicate arg0, double arg1) { return 0.0; }
        @Independent @Modified DoubleIterator doubleIterator() { return null; }
        @Modified void each(@Independent @Modified DoubleProcedure arg0) { }
        @Independent @Modified
        <V, R extends Collection<V>> R flatCollect(
            @Independent @Modified DoubleToObjectFunction<? extends Iterable<V>> function,
            @Independent @Modified R target) { return null; }
        @Modified void forEach(@Independent @Modified DoubleProcedure procedure) { }
        @Independent @NotModified
        <T> T injectInto(
            @Independent @NotModified T arg0,
            @Independent @Modified ObjectDoubleToObjectFunction<? super T, ? extends T> arg1) { return null; }

        @Modified
        boolean injectIntoBoolean(boolean injectedValue, @Independent @Modified BooleanDoubleToBooleanFunction function) {
            return false;
        }

        @Modified
        byte injectIntoByte(byte injectedValue, @Independent @Modified ByteDoubleToByteFunction function) { return 0; }

        @Modified
        char injectIntoChar(char injectedValue, @Independent @Modified CharDoubleToCharFunction function) { return '\0'; }

        @Modified
        double injectIntoDouble(double injectedValue, @Independent @Modified DoubleDoubleToDoubleFunction function) {
            return 0.0;
        }

        @Modified
        float injectIntoFloat(float injectedValue, @Independent @Modified FloatDoubleToFloatFunction function) {
            return 0.0F;
        }

        @Modified
        int injectIntoInt(int injectedValue, @Independent @Modified IntDoubleToIntFunction function) { return 0; }

        @Modified
        long injectIntoLong(long injectedValue, @Independent @Modified LongDoubleToLongFunction function) { return 0L; }

        @Modified
        short injectIntoShort(short injectedValue, @Independent @Modified ShortDoubleToShortFunction function) {
            return 0;
        }
        @NotModified double max() { return 0.0; }
        @Modified double maxIfEmpty(double arg0) { return 0.0; }
        @NotModified double median() { return 0.0; }
        @Modified double medianIfEmpty(double defaultValue) { return 0.0; }
        @NotModified double min() { return 0.0; }
        @Modified double minIfEmpty(double arg0) { return 0.0; }
        @NotModified boolean noneSatisfy(@Independent @NotModified DoublePredicate predicate) { return false; }
        @Modified double reduce(@Independent @Modified DoubleDoubleToDoubleFunction accumulator) { return 0.0; }
        @Modified
        double reduceIfEmpty(@Independent @Modified DoubleDoubleToDoubleFunction accumulator, double defaultValue) {
            return 0.0;
        }
        @Independent @Modified DoubleIterable reject(@Independent @NotModified DoublePredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableDoubleCollection> R reject(
            @Independent @NotModified DoublePredicate predicate,
            @Independent @Modified R target) { return null; }
        @Independent @Modified DoubleIterable select(@Independent @NotModified DoublePredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableDoubleCollection> R select(
            @Independent @NotModified DoublePredicate predicate,
            @Independent @Modified R target) { return null; }
        @NotModified double sum() { return 0.0; }
        @Independent @Modified DoubleSummaryStatistics summaryStatistics() { return null; }
        @Fluent @Independent @Modified
        DoubleIterable tap(@Independent @Modified DoubleProcedure procedure) { return null; }
        @Independent @NotModified double [] toArray() { return null; }
        @Independent @NotModified double [] toArray(@Independent @NotModified double [] target) { return null; }
        @Independent @NotModified MutableDoubleBag toBag() { return null; }
        @Independent @NotModified MutableDoubleList toList() { return null; }
        @Independent @NotModified MutableDoubleSet toSet() { return null; }
        @Independent @Modified double [] toSortedArray() { return null; }
        @Independent @NotModified MutableDoubleList toSortedList() { return null; }
        @Independent @NotModified
        MutableDoubleList toSortedList(@Independent @Modified DoubleComparator comparator) { return null; }

        @Independent @Modified
        <T> MutableDoubleList toSortedListBy(@Independent @Modified DoubleToObjectFunction<T> function) { return null; }

        @Independent @Modified
        <T> MutableDoubleList toSortedListBy(
            @Independent @Modified DoubleToObjectFunction<T> function,
            @Independent @NotModified Comparator<? super T> comparator) { return null; }
    }

    //public interface FloatIterable implements PrimitiveIterable
    @FinalFields
    @Independent
    class FloatIterable$ {
        @NotModified boolean allSatisfy(@Independent @NotModified FloatPredicate arg0) { return false; }
        @NotModified boolean anySatisfy(@Independent @NotModified FloatPredicate arg0) { return false; }
        @Independent @Modified LazyFloatIterable asLazy() { return null; }
        @NotModified double average() { return 0.0; }
        @Modified double averageIfEmpty(double defaultValue) { return 0.0; }
        @Independent @Modified RichIterable<FloatIterable> chunk(int size) { return null; }
        @Independent @Modified
        <V> RichIterable<V> collect(@Independent @Modified FloatToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R collect(
            @Independent @Modified FloatToObjectFunction<? extends V> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableBooleanCollection> R collectBoolean(
            @Independent @Modified FloatToBooleanFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableByteCollection> R collectByte(
            @Independent @Modified FloatToByteFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableCharCollection> R collectChar(
            @Independent @Modified FloatToCharFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableDoubleCollection> R collectDouble(
            @Independent @Modified FloatToDoubleFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableFloatCollection> R collectFloat(
            @Independent @Modified FloatToFloatFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableIntCollection> R collectInt(
            @Independent @Modified FloatToIntFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableLongCollection> R collectLong(
            @Independent @Modified FloatToLongFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableShortCollection> R collectShort(
            @Independent @Modified FloatToShortFunction function,
            @Independent @Modified R target) { return null; }
        @NotModified boolean contains(float arg0) { return false; }
        @NotModified boolean containsAll(@Independent @NotModified float ... source) { return false; }
        @NotModified boolean containsAll(@Independent @Modified FloatIterable source) { return false; }
        @Modified boolean containsAny(@Independent @NotModified float ... source) { return false; }
        @Modified boolean containsAny(@Independent @Modified FloatIterable source) { return false; }
        @Modified boolean containsNone(@Independent @NotModified float ... source) { return false; }
        @Modified boolean containsNone(@Independent @Modified FloatIterable source) { return false; }
        @NotModified int count(@Independent @NotModified FloatPredicate arg0) { return 0; }
        @NotModified float detectIfNone(@Independent @NotModified FloatPredicate arg0, float arg1) { return 0.0F; }
        @Modified void each(@Independent @Modified FloatProcedure arg0) { }
        @Independent @Modified
        <V, R extends Collection<V>> R flatCollect(
            @Independent @Modified FloatToObjectFunction<? extends Iterable<V>> function,
            @Independent @Modified R target) { return null; }
        @Independent @Modified FloatIterator floatIterator() { return null; }
        @Modified void forEach(@Independent @Modified FloatProcedure procedure) { }
        @Independent @NotModified
        <T> T injectInto(
            @Independent @NotModified T arg0,
            @Independent @Modified ObjectFloatToObjectFunction<? super T, ? extends T> arg1) { return null; }

        @Modified
        boolean injectIntoBoolean(boolean injectedValue, @Independent @Modified BooleanFloatToBooleanFunction function) {
            return false;
        }

        @Modified
        byte injectIntoByte(byte injectedValue, @Independent @Modified ByteFloatToByteFunction function) { return 0; }

        @Modified
        char injectIntoChar(char injectedValue, @Independent @Modified CharFloatToCharFunction function) { return '\0'; }

        @Modified
        double injectIntoDouble(double injectedValue, @Independent @Modified DoubleFloatToDoubleFunction function) {
            return 0.0;
        }

        @Modified
        float injectIntoFloat(float injectedValue, @Independent @Modified FloatFloatToFloatFunction function) {
            return 0.0F;
        }

        @Modified
        int injectIntoInt(int injectedValue, @Independent @Modified IntFloatToIntFunction function) { return 0; }

        @Modified
        long injectIntoLong(long injectedValue, @Independent @Modified LongFloatToLongFunction function) { return 0L; }

        @Modified
        short injectIntoShort(short injectedValue, @Independent @Modified ShortFloatToShortFunction function) { return 0; }
        @NotModified float max() { return 0.0F; }
        @Modified float maxIfEmpty(float arg0) { return 0.0F; }
        @NotModified double median() { return 0.0; }
        @Modified double medianIfEmpty(double defaultValue) { return 0.0; }
        @NotModified float min() { return 0.0F; }
        @Modified float minIfEmpty(float arg0) { return 0.0F; }
        @NotModified boolean noneSatisfy(@Independent @NotModified FloatPredicate predicate) { return false; }
        @Modified double reduce(@Independent @Modified DoubleFloatToDoubleFunction accumulator) { return 0.0; }
        @Modified
        double reduceIfEmpty(@Independent @Modified DoubleFloatToDoubleFunction accumulator, double defaultValue) {
            return 0.0;
        }
        @Independent @Modified FloatIterable reject(@Independent @NotModified FloatPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableFloatCollection> R reject(
            @Independent @NotModified FloatPredicate predicate,
            @Independent @Modified R target) { return null; }
        @Independent @Modified FloatIterable select(@Independent @NotModified FloatPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableFloatCollection> R select(
            @Independent @NotModified FloatPredicate predicate,
            @Independent @Modified R target) { return null; }
        @NotModified double sum() { return 0.0; }
        @Independent @Modified DoubleSummaryStatistics summaryStatistics() { return null; }
        @Fluent @Independent @Modified
        FloatIterable tap(@Independent @Modified FloatProcedure procedure) { return null; }
        @Independent @NotModified float [] toArray() { return null; }
        @Independent @NotModified float [] toArray(@Independent @NotModified float [] target) { return null; }
        @Independent @NotModified MutableFloatBag toBag() { return null; }
        @Independent @NotModified MutableFloatList toList() { return null; }
        @Independent @NotModified MutableFloatSet toSet() { return null; }
        @Independent @Modified float [] toSortedArray() { return null; }
        @Independent @NotModified MutableFloatList toSortedList() { return null; }
        @Independent @NotModified
        MutableFloatList toSortedList(@Independent @Modified FloatComparator comparator) { return null; }

        @Independent @Modified
        <T> MutableFloatList toSortedListBy(@Independent @Modified FloatToObjectFunction<T> function) { return null; }

        @Independent @Modified
        <T> MutableFloatList toSortedListBy(
            @Independent @Modified FloatToObjectFunction<T> function,
            @Independent @NotModified Comparator<? super T> comparator) { return null; }
    }

    //public interface IntIterable implements PrimitiveIterable
    @FinalFields
    @Independent
    class IntIterable$ {
        @NotModified boolean allSatisfy(@Independent @NotModified IntPredicate arg0) { return false; }
        @NotModified boolean anySatisfy(@Independent @NotModified IntPredicate arg0) { return false; }
        @Independent @Modified LazyIntIterable asLazy() { return null; }
        @NotModified double average() { return 0.0; }
        @Modified double averageIfEmpty(double defaultValue) { return 0.0; }
        @Independent @Modified RichIterable<IntIterable> chunk(int size) { return null; }
        @Independent @Modified
        <V> RichIterable<V> collect(@Independent @Modified IntToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R collect(
            @Independent @Modified IntToObjectFunction<? extends V> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableBooleanCollection> R collectBoolean(
            @Independent @Modified IntToBooleanFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableByteCollection> R collectByte(
            @Independent @Modified IntToByteFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableCharCollection> R collectChar(
            @Independent @Modified IntToCharFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableDoubleCollection> R collectDouble(
            @Independent @Modified IntToDoubleFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableFloatCollection> R collectFloat(
            @Independent @Modified IntToFloatFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableIntCollection> R collectInt(
            @Independent @NotModified IntToIntFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableLongCollection> R collectLong(
            @Independent @Modified IntToLongFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableShortCollection> R collectShort(
            @Independent @Modified IntToShortFunction function,
            @Independent @Modified R target) { return null; }
        @NotModified boolean contains(int arg0) { return false; }
        @NotModified boolean containsAll(@Independent @NotModified int ... source) { return false; }
        @NotModified boolean containsAll(@Independent @Modified IntIterable source) { return false; }
        @Modified boolean containsAny(@Independent @NotModified int ... source) { return false; }
        @Modified boolean containsAny(@Independent @Modified IntIterable source) { return false; }
        @Modified boolean containsNone(@Independent @NotModified int ... source) { return false; }
        @Modified boolean containsNone(@Independent @Modified IntIterable source) { return false; }
        @NotModified int count(@Independent @NotModified IntPredicate arg0) { return 0; }
        @NotModified int detectIfNone(@Independent @Modified IntPredicate arg0, int arg1) { return 0; }
        @Modified void each(@Independent @Modified IntProcedure arg0) { }
        @Independent @Modified
        <V, R extends Collection<V>> R flatCollect(
            @Independent @Modified IntToObjectFunction<? extends Iterable<V>> function,
            @Independent @Modified R target) { return null; }
        @Modified void forEach(@Independent @Modified IntProcedure procedure) { }
        @Independent @NotModified
        <T> T injectInto(
            @Independent @NotModified T arg0,
            @Independent @Modified ObjectIntToObjectFunction<? super T, ? extends T> arg1) { return null; }

        @Modified
        boolean injectIntoBoolean(boolean injectedValue, @Independent @Modified BooleanIntToBooleanFunction function) {
            return false;
        }

        @Modified
        byte injectIntoByte(byte injectedValue, @Independent @Modified ByteIntToByteFunction function) { return 0; }

        @Modified
        char injectIntoChar(char injectedValue, @Independent @Modified CharIntToCharFunction function) { return '\0'; }

        @Modified
        double injectIntoDouble(double injectedValue, @Independent @Modified DoubleIntToDoubleFunction function) {
            return 0.0;
        }

        @Modified
        float injectIntoFloat(float injectedValue, @Independent @Modified FloatIntToFloatFunction function) {
            return 0.0F;
        }

        @Modified
        int injectIntoInt(int injectedValue, @Independent @Modified IntIntToIntFunction function) { return 0; }

        @Modified
        long injectIntoLong(long injectedValue, @Independent @Modified LongIntToLongFunction function) { return 0L; }

        @Modified
        short injectIntoShort(short injectedValue, @Independent @Modified ShortIntToShortFunction function) { return 0; }
        @Independent @Modified IntIterator intIterator() { return null; }
        @NotModified int max() { return 0; }
        @Modified int maxIfEmpty(int arg0) { return 0; }
        @NotModified double median() { return 0.0; }
        @Modified double medianIfEmpty(double defaultValue) { return 0.0; }
        @NotModified int min() { return 0; }
        @Modified int minIfEmpty(int arg0) { return 0; }
        @NotModified boolean noneSatisfy(@Independent @NotModified IntPredicate predicate) { return false; }
        @Modified long reduce(@Independent @Modified LongIntToLongFunction accumulator) { return 0L; }
        @Modified
        long reduceIfEmpty(@Independent @Modified LongIntToLongFunction accumulator, long defaultValue) { return 0L; }
        @Independent @Modified IntIterable reject(@Independent @NotModified IntPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableIntCollection> R reject(
            @Independent @NotModified IntPredicate predicate,
            @Independent @Modified R target) { return null; }
        @Independent @Modified IntIterable select(@Independent @NotModified IntPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableIntCollection> R select(
            @Independent @NotModified IntPredicate predicate,
            @Independent @Modified R target) { return null; }
        @NotModified long sum() { return 0L; }
        @Independent @Modified IntSummaryStatistics summaryStatistics() { return null; }
        @Fluent @Independent @Modified IntIterable tap(@Independent @Modified IntProcedure procedure) { return null; }
        @Independent @NotModified int [] toArray() { return null; }
        @Independent @NotModified int [] toArray(@Independent @NotModified int [] target) { return null; }
        @Independent @NotModified MutableIntBag toBag() { return null; }
        @Independent @NotModified MutableIntList toList() { return null; }
        @Independent @NotModified MutableIntSet toSet() { return null; }
        @Independent @Modified int [] toSortedArray() { return null; }
        @Independent @NotModified MutableIntList toSortedList() { return null; }
        @Independent @NotModified
        MutableIntList toSortedList(@Independent @Modified IntComparator comparator) { return null; }

        @Independent @Modified
        <T> MutableIntList toSortedListBy(@Independent @Modified IntToObjectFunction<T> function) { return null; }

        @Independent @Modified
        <T> MutableIntList toSortedListBy(
            @Independent @Modified IntToObjectFunction<T> function,
            @Independent @NotModified Comparator<? super T> comparator) { return null; }
    }

    //public interface InternalIterable implements Iterable<T>
    @Independent(hc = true)
    class InternalIterable$<T> {
        //override from java.lang.Iterable
        @Modified
        void forEach(@Independent @Modified Consumer<? super T> consumer) { }
        @Modified void forEach(@Independent @Modified Procedure<? super T> arg0) { }
        @Modified
        <P> void forEachWith(
            @Independent @Modified Procedure2<? super T, ? super P> arg0,
            @Independent @Modified P arg1) { }
        @Modified void forEachWithIndex(@Independent @Modified ObjectIntProcedure<? super T> arg0) { }
    }

    //public interface LazyBooleanIterable implements BooleanIterable
    @FinalFields
    @Independent
    class LazyBooleanIterable$ {
        //override from org.eclipse.collections.api.BooleanIterable
        @Independent @Modified
        <V> LazyIterable<V> collect(@Independent @Modified BooleanToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        LazyBooleanIterable collectBoolean(@Independent @Modified BooleanToBooleanFunction arg0) { return null; }

        @Independent @Modified
        LazyByteIterable collectByte(@Independent @Modified BooleanToByteFunction arg0) { return null; }

        @Independent @Modified
        LazyCharIterable collectChar(@Independent @Modified BooleanToCharFunction arg0) { return null; }

        @Independent @Modified
        LazyDoubleIterable collectDouble(@Independent @Modified BooleanToDoubleFunction arg0) { return null; }

        @Independent @Modified
        LazyFloatIterable collectFloat(@Independent @Modified BooleanToFloatFunction arg0) { return null; }

        @Independent @Modified
        LazyIntIterable collectInt(@Independent @Modified BooleanToIntFunction arg0) { return null; }

        @Independent @Modified
        LazyLongIterable collectLong(@Independent @Modified BooleanToLongFunction arg0) { return null; }

        @Independent @Modified
        LazyShortIterable collectShort(@Independent @Modified BooleanToShortFunction arg0) { return null; }

        @Independent @Modified
        <V> LazyIterable<V> flatCollect(@Independent @Modified BooleanToObjectFunction<? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.BooleanIterable
        @Independent @Modified
        LazyBooleanIterable reject(@Independent @NotModified BooleanPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.BooleanIterable
        @Independent @Modified
        LazyBooleanIterable select(@Independent @NotModified BooleanPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.BooleanIterable
        @Fluent @Independent @Modified @NotNull
        LazyBooleanIterable tap(@Independent @Modified BooleanProcedure arg0) { return null; }
    }

    //public interface LazyByteIterable implements ByteIterable
    @FinalFields
    @Independent
    class LazyByteIterable$ {
        //override from org.eclipse.collections.api.ByteIterable
        @Independent @Modified
        <V> LazyIterable<V> collect(@Independent @Modified ByteToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        LazyBooleanIterable collectBoolean(@Independent @Modified ByteToBooleanFunction arg0) { return null; }

        @Independent @Modified
        LazyByteIterable collectByte(@Independent @Modified ByteToByteFunction arg0) { return null; }

        @Independent @Modified
        LazyCharIterable collectChar(@Independent @Modified ByteToCharFunction arg0) { return null; }

        @Independent @Modified
        LazyDoubleIterable collectDouble(@Independent @Modified ByteToDoubleFunction arg0) { return null; }

        @Independent @Modified
        LazyFloatIterable collectFloat(@Independent @Modified ByteToFloatFunction arg0) { return null; }

        @Independent @Modified
        LazyIntIterable collectInt(@Independent @Modified ByteToIntFunction arg0) { return null; }

        @Independent @Modified
        LazyLongIterable collectLong(@Independent @Modified ByteToLongFunction arg0) { return null; }

        @Independent @Modified
        LazyShortIterable collectShort(@Independent @Modified ByteToShortFunction arg0) { return null; }

        @Independent @Modified
        <V> LazyIterable<V> flatCollect(@Independent @Modified ByteToObjectFunction<? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ByteIterable
        @Independent @Modified
        LazyByteIterable reject(@Independent @NotModified BytePredicate arg0) { return null; }

        //override from org.eclipse.collections.api.ByteIterable
        @Independent @Modified
        LazyByteIterable select(@Independent @NotModified BytePredicate arg0) { return null; }

        //override from org.eclipse.collections.api.ByteIterable
        @Fluent @Independent @Modified @NotNull
        LazyByteIterable tap(@Independent @Modified ByteProcedure arg0) { return null; }
    }

    //public interface LazyCharIterable implements CharIterable
    @FinalFields
    @Independent
    class LazyCharIterable$ {
        //override from org.eclipse.collections.api.CharIterable
        @Independent @Modified
        <V> LazyIterable<V> collect(@Independent @Modified CharToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        LazyBooleanIterable collectBoolean(@Independent @Modified CharToBooleanFunction arg0) { return null; }

        @Independent @Modified
        LazyByteIterable collectByte(@Independent @Modified CharToByteFunction arg0) { return null; }

        @Independent @Modified
        LazyCharIterable collectChar(@Independent @NotModified CharToCharFunction arg0) { return null; }

        @Independent @Modified
        LazyDoubleIterable collectDouble(@Independent @Modified CharToDoubleFunction arg0) { return null; }

        @Independent @Modified
        LazyFloatIterable collectFloat(@Independent @Modified CharToFloatFunction arg0) { return null; }

        @Independent @Modified
        LazyIntIterable collectInt(@Independent @Modified CharToIntFunction arg0) { return null; }

        @Independent @Modified
        LazyLongIterable collectLong(@Independent @Modified CharToLongFunction arg0) { return null; }

        @Independent @Modified
        LazyShortIterable collectShort(@Independent @Modified CharToShortFunction arg0) { return null; }

        @Independent @Modified
        <V> LazyIterable<V> flatCollect(@Independent @Modified CharToObjectFunction<? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.CharIterable
        @Independent @Modified
        LazyCharIterable reject(@Independent @NotModified CharPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.CharIterable
        @Independent @Modified
        LazyCharIterable select(@Independent @NotModified CharPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.CharIterable
        @Fluent @Independent @Modified @NotNull
        LazyCharIterable tap(@Independent @Modified CharProcedure arg0) { return null; }
    }

    //public interface LazyDoubleIterable implements DoubleIterable
    @FinalFields
    @Independent
    class LazyDoubleIterable$ {
        //override from org.eclipse.collections.api.DoubleIterable
        @Independent @Modified
        <V> LazyIterable<V> collect(@Independent @Modified DoubleToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        LazyBooleanIterable collectBoolean(@Independent @Modified DoubleToBooleanFunction arg0) { return null; }

        @Independent @Modified
        LazyByteIterable collectByte(@Independent @Modified DoubleToByteFunction arg0) { return null; }

        @Independent @Modified
        LazyCharIterable collectChar(@Independent @Modified DoubleToCharFunction arg0) { return null; }

        @Independent @Modified
        LazyDoubleIterable collectDouble(@Independent @Modified DoubleToDoubleFunction arg0) { return null; }

        @Independent @Modified
        LazyFloatIterable collectFloat(@Independent @Modified DoubleToFloatFunction arg0) { return null; }

        @Independent @Modified
        LazyIntIterable collectInt(@Independent @Modified DoubleToIntFunction arg0) { return null; }

        @Independent @Modified
        LazyLongIterable collectLong(@Independent @Modified DoubleToLongFunction arg0) { return null; }

        @Independent @Modified
        LazyShortIterable collectShort(@Independent @Modified DoubleToShortFunction arg0) { return null; }

        @Independent @Modified
        <V> LazyIterable<V> flatCollect(@Independent @Modified DoubleToObjectFunction<? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.DoubleIterable
        @Independent @Modified
        LazyDoubleIterable reject(@Independent @NotModified DoublePredicate arg0) { return null; }

        //override from org.eclipse.collections.api.DoubleIterable
        @Independent @Modified
        LazyDoubleIterable select(@Independent @NotModified DoublePredicate arg0) { return null; }

        //override from org.eclipse.collections.api.DoubleIterable
        @Fluent @Independent @Modified @NotNull
        LazyDoubleIterable tap(@Independent @Modified DoubleProcedure arg0) { return null; }
    }

    //public interface LazyFloatIterable implements FloatIterable
    @FinalFields
    @Independent
    class LazyFloatIterable$ {
        //override from org.eclipse.collections.api.FloatIterable
        @Independent @Modified
        <V> LazyIterable<V> collect(@Independent @Modified FloatToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        LazyBooleanIterable collectBoolean(@Independent @Modified FloatToBooleanFunction arg0) { return null; }

        @Independent @Modified
        LazyByteIterable collectByte(@Independent @Modified FloatToByteFunction arg0) { return null; }

        @Independent @Modified
        LazyCharIterable collectChar(@Independent @Modified FloatToCharFunction arg0) { return null; }

        @Independent @Modified
        LazyDoubleIterable collectDouble(@Independent @Modified FloatToDoubleFunction arg0) { return null; }

        @Independent @Modified
        LazyFloatIterable collectFloat(@Independent @Modified FloatToFloatFunction arg0) { return null; }

        @Independent @Modified
        LazyIntIterable collectInt(@Independent @Modified FloatToIntFunction arg0) { return null; }

        @Independent @Modified
        LazyLongIterable collectLong(@Independent @Modified FloatToLongFunction arg0) { return null; }

        @Independent @Modified
        LazyShortIterable collectShort(@Independent @Modified FloatToShortFunction arg0) { return null; }

        @Independent @Modified
        <V> LazyIterable<V> flatCollect(@Independent @Modified FloatToObjectFunction<? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.FloatIterable
        @Independent @Modified
        LazyFloatIterable reject(@Independent @NotModified FloatPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.FloatIterable
        @Independent @Modified
        LazyFloatIterable select(@Independent @NotModified FloatPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.FloatIterable
        @Fluent @Independent @Modified @NotNull
        LazyFloatIterable tap(@Independent @Modified FloatProcedure arg0) { return null; }
    }

    //public interface LazyIntIterable implements IntIterable
    @FinalFields
    @Independent
    class LazyIntIterable$ {
        //override from org.eclipse.collections.api.IntIterable
        @Independent @Modified
        <V> LazyIterable<V> collect(@Independent @Modified IntToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        LazyBooleanIterable collectBoolean(@Independent @Modified IntToBooleanFunction arg0) { return null; }

        @Independent @Modified
        LazyByteIterable collectByte(@Independent @Modified IntToByteFunction arg0) { return null; }

        @Independent @Modified
        LazyCharIterable collectChar(@Independent @Modified IntToCharFunction arg0) { return null; }

        @Independent @Modified
        LazyDoubleIterable collectDouble(@Independent @Modified IntToDoubleFunction arg0) { return null; }

        @Independent @Modified
        LazyFloatIterable collectFloat(@Independent @Modified IntToFloatFunction arg0) { return null; }

        @Independent @Modified
        LazyIntIterable collectInt(@Independent @NotModified IntToIntFunction arg0) { return null; }

        @Independent @Modified
        LazyLongIterable collectLong(@Independent @Modified IntToLongFunction arg0) { return null; }

        @Independent @Modified
        LazyShortIterable collectShort(@Independent @Modified IntToShortFunction arg0) { return null; }

        @Independent @Modified
        <V> LazyIterable<V> flatCollect(@Independent @Modified IntToObjectFunction<? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.IntIterable
        @Independent @Modified
        LazyIntIterable reject(@Independent @NotModified IntPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.IntIterable
        @Independent @Modified
        LazyIntIterable select(@Independent @NotModified IntPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.IntIterable
        @Fluent @Independent @Modified @NotNull
        LazyIntIterable tap(@Independent @Modified IntProcedure arg0) { return null; }
    }

    //public interface LazyIterable implements RichIterable<T>
    @Independent(absent = true)
    class LazyIterable$<T> {
        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LazyIterable<RichIterable<T>> chunk(int arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified
        <V> LazyIterable<V> collect(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LazyBooleanIterable collectBoolean(@Independent @Modified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LazyByteIterable collectByte(@Independent @Modified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LazyCharIterable collectChar(@Independent @Modified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LazyDoubleIterable collectDouble(@Independent @Modified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LazyFloatIterable collectFloat(@Independent @Modified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> LazyIterable<V> collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent @Modified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LazyIntIterable collectInt(@Independent @Modified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LazyLongIterable collectLong(@Independent @Modified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LazyShortIterable collectShort(@Independent @Modified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <P, V> LazyIterable<V> collectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        @Independent @NotModified
        LazyIterable<T> concatenate(@Independent @NotModified Iterable<T> arg0) { return null; }
        @Independent(absent = true) @NotModified LazyIterable<T> distinct() { return null; }
        @Independent(absent = true) @NotModified LazyIterable<T> drop(int arg0) { return null; }
        @Independent(absent = true) @NotModified
        LazyIterable<T> dropWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @NotModified

        <V> LazyIterable<V> flatCollect(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <P, V> LazyIterable<V> flatCollectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @Modified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified
        T getFirst() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        <R extends Collection<T>> R into(@Independent(hc = true) @Modified R arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @NotModified
        LazyIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <P> LazyIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @NotModified
        LazyIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        <S> LazyIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <P> LazyIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @Independent(absent = true) @Modified LazyIterable<T> take(int arg0) { return null; }
        @Independent(absent = true) @NotModified
        LazyIterable<T> takeWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        LazyIterable<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        Object [] toArray() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        <E> E [] toArray(@Independent @Modified E [] array) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ImmutableBag<T> toImmutableBag() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ImmutableList<T> toImmutableList() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ImmutableSet<T> toImmutableSet() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <S> LazyIterable<Pair<T, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LazyIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface LazyLongIterable implements LongIterable
    @FinalFields
    @Independent
    class LazyLongIterable$ {
        //override from org.eclipse.collections.api.LongIterable
        @Independent @Modified
        <V> LazyIterable<V> collect(@Independent @Modified LongToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        LazyBooleanIterable collectBoolean(@Independent @Modified LongToBooleanFunction arg0) { return null; }

        @Independent @Modified
        LazyByteIterable collectByte(@Independent @Modified LongToByteFunction arg0) { return null; }

        @Independent @Modified
        LazyCharIterable collectChar(@Independent @Modified LongToCharFunction arg0) { return null; }

        @Independent @Modified
        LazyDoubleIterable collectDouble(@Independent @Modified LongToDoubleFunction arg0) { return null; }

        @Independent @Modified
        LazyFloatIterable collectFloat(@Independent @Modified LongToFloatFunction arg0) { return null; }

        @Independent @Modified
        LazyIntIterable collectInt(@Independent @Modified LongToIntFunction arg0) { return null; }

        @Independent @Modified
        LazyLongIterable collectLong(@Independent @Modified LongToLongFunction arg0) { return null; }

        @Independent @Modified
        LazyShortIterable collectShort(@Independent @Modified LongToShortFunction arg0) { return null; }

        @Independent @Modified
        <V> LazyIterable<V> flatCollect(@Independent @Modified LongToObjectFunction<? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.LongIterable
        @Independent @Modified
        LazyLongIterable reject(@Independent @NotModified LongPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.LongIterable
        @Independent @Modified
        LazyLongIterable select(@Independent @NotModified LongPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.LongIterable
        @Fluent @Independent @Modified @NotNull
        LazyLongIterable tap(@Independent @Modified LongProcedure arg0) { return null; }
    }

    //public interface LazyShortIterable implements ShortIterable
    @FinalFields
    @Independent
    class LazyShortIterable$ {
        //override from org.eclipse.collections.api.ShortIterable
        @Independent @Modified
        <V> LazyIterable<V> collect(@Independent @Modified ShortToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        LazyBooleanIterable collectBoolean(@Independent @Modified ShortToBooleanFunction arg0) { return null; }

        @Independent @Modified
        LazyByteIterable collectByte(@Independent @Modified ShortToByteFunction arg0) { return null; }

        @Independent @Modified
        LazyCharIterable collectChar(@Independent @Modified ShortToCharFunction arg0) { return null; }

        @Independent @Modified
        LazyDoubleIterable collectDouble(@Independent @Modified ShortToDoubleFunction arg0) { return null; }

        @Independent @Modified
        LazyFloatIterable collectFloat(@Independent @Modified ShortToFloatFunction arg0) { return null; }

        @Independent @Modified
        LazyIntIterable collectInt(@Independent @Modified ShortToIntFunction arg0) { return null; }

        @Independent @Modified
        LazyLongIterable collectLong(@Independent @Modified ShortToLongFunction arg0) { return null; }

        @Independent @Modified
        LazyShortIterable collectShort(@Independent @Modified ShortToShortFunction arg0) { return null; }

        @Independent @Modified
        <V> LazyIterable<V> flatCollect(@Independent @Modified ShortToObjectFunction<? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ShortIterable
        @Independent @Modified
        LazyShortIterable reject(@Independent @NotModified ShortPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.ShortIterable
        @Independent @Modified
        LazyShortIterable select(@Independent @NotModified ShortPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.ShortIterable
        @Fluent @Independent @Modified @NotNull
        LazyShortIterable tap(@Independent @Modified ShortProcedure arg0) { return null; }
    }

    //public interface LongIterable implements PrimitiveIterable
    @FinalFields
    @Independent
    class LongIterable$ {
        @NotModified boolean allSatisfy(@Independent @NotModified LongPredicate arg0) { return false; }
        @NotModified boolean anySatisfy(@Independent @NotModified LongPredicate arg0) { return false; }
        @Independent @Modified LazyLongIterable asLazy() { return null; }
        @NotModified double average() { return 0.0; }
        @Modified double averageIfEmpty(double defaultValue) { return 0.0; }
        @Independent @Modified RichIterable<LongIterable> chunk(int size) { return null; }
        @Independent @Modified
        <V> RichIterable<V> collect(@Independent @Modified LongToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R collect(
            @Independent @Modified LongToObjectFunction<? extends V> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableBooleanCollection> R collectBoolean(
            @Independent @Modified LongToBooleanFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableByteCollection> R collectByte(
            @Independent @Modified LongToByteFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableCharCollection> R collectChar(
            @Independent @Modified LongToCharFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableDoubleCollection> R collectDouble(
            @Independent @Modified LongToDoubleFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableFloatCollection> R collectFloat(
            @Independent @Modified LongToFloatFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableIntCollection> R collectInt(
            @Independent @Modified LongToIntFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableLongCollection> R collectLong(
            @Independent @Modified LongToLongFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableShortCollection> R collectShort(
            @Independent @Modified LongToShortFunction function,
            @Independent @Modified R target) { return null; }
        @NotModified boolean contains(long arg0) { return false; }
        @NotModified boolean containsAll(@Independent @NotModified long ... source) { return false; }
        @NotModified boolean containsAll(@Independent @Modified LongIterable source) { return false; }
        @Modified boolean containsAny(@Independent @NotModified long ... source) { return false; }
        @Modified boolean containsAny(@Independent @Modified LongIterable source) { return false; }
        @Modified boolean containsNone(@Independent @NotModified long ... source) { return false; }
        @Modified boolean containsNone(@Independent @Modified LongIterable source) { return false; }
        @NotModified int count(@Independent @NotModified LongPredicate arg0) { return 0; }
        @NotModified long detectIfNone(@Independent @Modified LongPredicate arg0, long arg1) { return 0L; }
        @Modified void each(@Independent @Modified LongProcedure arg0) { }
        @Independent @Modified
        <V, R extends Collection<V>> R flatCollect(
            @Independent @Modified LongToObjectFunction<? extends Iterable<V>> function,
            @Independent @Modified R target) { return null; }
        @Modified void forEach(@Independent @Modified LongProcedure procedure) { }
        @Independent @NotModified
        <T> T injectInto(
            @Independent @NotModified T arg0,
            @Independent @Modified ObjectLongToObjectFunction<? super T, ? extends T> arg1) { return null; }

        @Modified
        boolean injectIntoBoolean(boolean injectedValue, @Independent @Modified BooleanLongToBooleanFunction function) {
            return false;
        }

        @Modified
        byte injectIntoByte(byte injectedValue, @Independent @Modified ByteLongToByteFunction function) { return 0; }

        @Modified
        char injectIntoChar(char injectedValue, @Independent @Modified CharLongToCharFunction function) { return '\0'; }

        @Modified
        double injectIntoDouble(double injectedValue, @Independent @Modified DoubleLongToDoubleFunction function) {
            return 0.0;
        }

        @Modified
        float injectIntoFloat(float injectedValue, @Independent @Modified FloatLongToFloatFunction function) {
            return 0.0F;
        }

        @Modified
        int injectIntoInt(int injectedValue, @Independent @Modified IntLongToIntFunction function) { return 0; }

        @Modified
        long injectIntoLong(long injectedValue, @Independent @Modified LongLongToLongFunction function) { return 0L; }

        @Modified
        short injectIntoShort(short injectedValue, @Independent @Modified ShortLongToShortFunction function) { return 0; }
        @Independent @Modified LongIterator longIterator() { return null; }
        @NotModified long max() { return 0L; }
        @Modified long maxIfEmpty(long arg0) { return 0L; }
        @NotModified double median() { return 0.0; }
        @Modified double medianIfEmpty(double defaultValue) { return 0.0; }
        @NotModified long min() { return 0L; }
        @Modified long minIfEmpty(long arg0) { return 0L; }
        @NotModified boolean noneSatisfy(@Independent @NotModified LongPredicate predicate) { return false; }
        @Modified long reduce(@Independent @Modified LongLongToLongFunction accumulator) { return 0L; }
        @Modified
        long reduceIfEmpty(@Independent @Modified LongLongToLongFunction accumulator, long defaultValue) { return 0L; }
        @Independent @Modified LongIterable reject(@Independent @NotModified LongPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableLongCollection> R reject(
            @Independent @NotModified LongPredicate predicate,
            @Independent @Modified R target) { return null; }
        @Independent @Modified LongIterable select(@Independent @NotModified LongPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableLongCollection> R select(
            @Independent @NotModified LongPredicate predicate,
            @Independent @Modified R target) { return null; }
        @NotModified long sum() { return 0L; }
        @Independent @Modified LongSummaryStatistics summaryStatistics() { return null; }
        @Fluent @Independent @Modified
        LongIterable tap(@Independent @Modified LongProcedure procedure) { return null; }
        @Independent @NotModified long [] toArray() { return null; }
        @Independent @NotModified long [] toArray(@Independent @NotModified long [] target) { return null; }
        @Independent @NotModified MutableLongBag toBag() { return null; }
        @Independent @NotModified MutableLongList toList() { return null; }
        @Independent @NotModified MutableLongSet toSet() { return null; }
        @Independent @Modified long [] toSortedArray() { return null; }
        @Independent @NotModified MutableLongList toSortedList() { return null; }
        @Independent @NotModified
        MutableLongList toSortedList(@Independent @Modified LongComparator comparator) { return null; }

        @Independent @Modified
        <T> MutableLongList toSortedListBy(@Independent @Modified LongToObjectFunction<T> function) { return null; }

        @Independent @Modified
        <T> MutableLongList toSortedListBy(
            @Independent @Modified LongToObjectFunction<T> function,
            @Independent @NotModified Comparator<? super T> comparator) { return null; }
    }

    //public interface ParallelIterable
    @FinalFields
    @Independent(hc = true)
    class ParallelIterable$<T> {
        @Independent @Modified
        <K, V> MapIterable<K, V> aggregateBy(
            @Independent @Modified Function<? super T, ? extends K> arg0,
            @Independent @Modified Function0<? extends V> arg1,
            @Independent @Modified Function2<? super V, ? super T, ? extends V> arg2) { return null; }

        @Independent @Modified
        <K, V> MapIterable<K, V> aggregateInPlaceBy(
            @Independent @Modified Function<? super T, ? extends K> arg0,
            @Independent @Modified Function0<? extends V> arg1,
            @Independent @Modified Procedure2<? super V, ? super T> arg2) { return null; }
        @NotModified boolean allSatisfy(@Independent @Modified Predicate<? super T> arg0) { return false; }
        @NotModified
        <P> boolean allSatisfyWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return false; }
        @NotModified boolean anySatisfy(@Independent @Modified Predicate<? super T> arg0) { return false; }
        @NotModified
        <P> boolean anySatisfyWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return false; }
        @NotModified void appendString(@Independent @Modified Appendable appendable) { }
        @NotModified void appendString(@Independent @Modified Appendable appendable, String separator) { }
        @NotModified
        void appendString(@Independent @Modified Appendable arg0, String arg1, String arg2, String arg3) { }
        @Independent @Modified ParallelIterable<T> asUnique() { return null; }
        @Independent @Modified
        <V> ParallelIterable<V> collect(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        @Independent @Modified
        <V> ParallelIterable<V> collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent @Modified Function<? super T, ? extends V> arg1) { return null; }

        @Independent @Modified
        <P, V> ParallelIterable<V> collectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @Modified P arg1) { return null; }
        @NotModified int count(@Independent @Modified Predicate<? super T> arg0) { return 0; }
        @NotModified
        <P> int countWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return 0; }

        @Independent(hc = true) @NotModified
        T detect(@Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        T detectIfNone(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent @Modified Function0<? extends T> arg1) { return null; }

        @Independent(hc = true) @NotModified
        <P> T detectWith(@Independent @Modified Predicate2<? super T, ? super P> arg0, @Independent @NotModified P arg1) {
            return null;
        }

        @Independent(hc = true) @Modified
        <P> T detectWithIfNone(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1,
            @Independent @Modified Function0<? extends T> arg2) { return null; }

        @Independent @Modified
        <V> ParallelIterable<V> flatCollect(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }
        @Modified void forEach(@Independent @Modified Procedure<? super T> arg0) { }
        @Modified
        <P> void forEachWith(
            @Independent @Modified Procedure2<? super T, ? super P> arg0,
            @Independent @Modified P arg1) { }

        @Independent(hc = true) @Modified
        <V> Multimap<V, T> groupBy(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        @Independent(hc = true) @Modified
        <V> Multimap<V, T> groupByEach(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        @Independent(hc = true) @Modified
        <V> MapIterable<V, T> groupByUniqueKey(@Independent @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }
        @NotModified String makeString() { return null; }
        @NotModified String makeString(String separator) { return null; }
        @NotModified String makeString(String start, String separator, String end) { return null; }
        @NotModified
        String makeString(
            @Independent @Modified Function<? super T, Object> function,
            String start,
            String separator,
            String end) { return null; }
        @Independent(hc = true) @NotModified T max() { return null; }
        @Independent(hc = true) @NotModified
        T max(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        <V extends Comparable<? super V>> T maxBy(@Independent @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }
        @Independent(hc = true) @NotModified T min() { return null; }
        @Independent(hc = true) @NotModified
        T min(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        <V extends Comparable<? super V>> T minBy(@Independent @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }
        @NotModified boolean noneSatisfy(@Independent @Modified Predicate<? super T> arg0) { return false; }
        @NotModified
        <P> boolean noneSatisfyWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return false; }

        @Independent @Modified
        ParallelIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent @Modified
        <P> ParallelIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        @Independent @Modified
        ParallelIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }
        @Independent @Modified <S> ParallelIterable<S> selectInstancesOf(Class<S> arg0) { return null; }
        @Independent @Modified
        <P> ParallelIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @NotModified double sumOfDouble(@Independent @Modified DoubleFunction<? super T> arg0) { return 0.0; }
        @NotModified double sumOfFloat(@Independent @Modified FloatFunction<? super T> arg0) { return 0.0; }
        @NotModified long sumOfInt(@Independent @Modified IntFunction<? super T> arg0) { return 0L; }
        @NotModified long sumOfLong(@Independent @Modified LongFunction<? super T> arg0) { return 0L; }
        @Independent @NotModified Object [] toArray() { return null; }
        @Independent(hc = true) @NotModified
        <T1> T1 [] toArray(@Independent(hc = true) @Modified T1 [] arg0) { return null; }
        @Independent @NotModified MutableBag<T> toBag() { return null; }
        @Independent @NotModified MutableList<T> toList() { return null; }
        @Independent @NotModified
        <NK, NV> MutableMap<NK, NV> toMap(
            @Independent @Modified Function<? super T, ? extends NK> arg0,
            @Independent @Modified Function<? super T, ? extends NV> arg1) { return null; }
        @Independent @NotModified MutableSet<T> toSet() { return null; }
        @Independent @Modified MutableSortedBag<T> toSortedBag() { return null; }
        @Independent @Modified
        MutableSortedBag<T> toSortedBag(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent @Modified
        <V extends Comparable<? super V>> MutableSortedBag<T> toSortedBagBy(
            @Independent @Modified Function<? super T, ? extends V> arg0) { return null; }
        @Independent @NotModified MutableList<T> toSortedList() { return null; }
        @Independent @NotModified
        MutableList<T> toSortedList(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent @Modified
        <V extends Comparable<? super V>> MutableList<T> toSortedListBy(
            @Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        @Independent @NotModified
        <NK, NV> MutableSortedMap<NK, NV> toSortedMap(
            @Independent @NotModified Comparator<? super NK> arg0,
            @Independent @Modified Function<? super T, ? extends NK> arg1,
            @Independent @Modified Function<? super T, ? extends NV> arg2) { return null; }

        @Independent @NotModified
        <NK, NV> MutableSortedMap<NK, NV> toSortedMap(
            @Independent @Modified Function<? super T, ? extends NK> arg0,
            @Independent @Modified Function<? super T, ? extends NV> arg1) { return null; }
        @Independent @NotModified MutableSortedSet<T> toSortedSet() { return null; }
        @Independent @NotModified
        MutableSortedSet<T> toSortedSet(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent @Modified
        <V extends Comparable<? super V>> MutableSortedSet<T> toSortedSetBy(
            @Independent @Modified Function<? super T, ? extends V> arg0) { return null; }
    }

    //public interface PrimitiveIterable
    @FinalFields
    @Independent
    class PrimitiveIterable$ {
        @NotModified void appendString(@Independent @Modified Appendable appendable) { }
        @NotModified void appendString(@Independent @Modified Appendable appendable, String separator) { }
        @NotModified
        void appendString(@Independent @Modified Appendable arg0, String arg1, String arg2, String arg3) { }
        @NotModified boolean isEmpty() { return false; }
        @NotModified String makeString() { return null; }
        @NotModified String makeString(String separator) { return null; }
        @NotModified String makeString(String start, String separator, String end) { return null; }
        @NotModified boolean notEmpty() { return false; }
        @NotModified int size() { return 0; }
        //override from java.lang.Object
        @NotModified @NotNull
        public String toString() { return null; }
    }

    //public interface RichIterable implements InternalIterable<T>
    @Independent(absent = true)
    class RichIterable$<T> {
        @Independent @Modified
        <K, V> MapIterable<K, V> aggregateBy(
            @Independent @Modified Function<? super T, ? extends K> groupBy,
            @Independent @Modified Function0<? extends V> zeroValueFactory,
            @Independent @Modified Function2<? super V, ? super T, ? extends V> nonMutatingAggregator) { return null; }

        @Independent @Modified
        <K, V, R extends MutableMapIterable<K, V>> R aggregateBy(
            @Independent @Modified Function<? super T, ? extends K> groupBy,
            @Independent @Modified Function0<? extends V> zeroValueFactory,
            @Independent @Modified Function2<? super V, ? super T, ? extends V> nonMutatingAggregator,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <K, V> MapIterable<K, V> aggregateInPlaceBy(
            @Independent @Modified Function<? super T, ? extends K> groupBy,
            @Independent @Modified Function0<? extends V> zeroValueFactory,
            @Independent @Modified Procedure2<? super V, ? super T> mutatingAggregator) { return null; }
        @NotModified boolean allSatisfy(@Independent @Modified Predicate<? super T> arg0) { return false; }
        @NotModified
        <P> boolean allSatisfyWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return false; }
        @NotModified boolean anySatisfy(@Independent @Modified Predicate<? super T> arg0) { return false; }
        @NotModified
        <P> boolean anySatisfyWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return false; }
        @NotModified void appendString(@Independent @Modified Appendable appendable) { }
        @NotModified void appendString(@Independent @Modified Appendable appendable, String separator) { }
        @NotModified
        void appendString(@Independent @Modified Appendable arg0, String arg1, String arg2, String arg3) { }
        @Independent @Modified LazyIterable<T> asLazy() { return null; }
        @Independent @Modified RichIterable<RichIterable<T>> chunk(int arg0) { return null; }
        @Independent(absent = true) @Modified
        <V> RichIterable<V> collect(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        @Independent @Modified
        <V, R extends Collection<V>> R collect(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0,
            @Independent @Modified R arg1) { return null; }

        @Independent @Modified
        BooleanIterable collectBoolean(@Independent @Modified BooleanFunction<? super T> arg0) { return null; }

        @Independent @Modified
        <R extends MutableBooleanCollection> R collectBoolean(
            @Independent @Modified BooleanFunction<? super T> booleanFunction,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        ByteIterable collectByte(@Independent @Modified ByteFunction<? super T> arg0) { return null; }

        @Independent @Modified
        <R extends MutableByteCollection> R collectByte(
            @Independent @Modified ByteFunction<? super T> byteFunction,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        CharIterable collectChar(@Independent @Modified CharFunction<? super T> arg0) { return null; }

        @Independent @Modified
        <R extends MutableCharCollection> R collectChar(
            @Independent @Modified CharFunction<? super T> charFunction,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        DoubleIterable collectDouble(@Independent @Modified DoubleFunction<? super T> arg0) { return null; }

        @Independent @Modified
        <R extends MutableDoubleCollection> R collectDouble(
            @Independent @Modified DoubleFunction<? super T> doubleFunction,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        FloatIterable collectFloat(@Independent @Modified FloatFunction<? super T> arg0) { return null; }

        @Independent @Modified
        <R extends MutableFloatCollection> R collectFloat(
            @Independent @Modified FloatFunction<? super T> floatFunction,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <V> RichIterable<V> collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg1) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg1,
            @Independent @Modified R arg2) { return null; }

        @Independent @Modified
        IntIterable collectInt(@Independent @Modified IntFunction<? super T> arg0) { return null; }

        @Independent @Modified
        <R extends MutableIntCollection> R collectInt(
            @Independent @Modified IntFunction<? super T> intFunction,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        LongIterable collectLong(@Independent @Modified LongFunction<? super T> arg0) { return null; }

        @Independent @Modified
        <R extends MutableLongCollection> R collectLong(
            @Independent @Modified LongFunction<? super T> longFunction,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        ShortIterable collectShort(@Independent @Modified ShortFunction<? super T> arg0) { return null; }

        @Independent @Modified
        <R extends MutableShortCollection> R collectShort(
            @Independent @Modified ShortFunction<? super T> shortFunction,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <P, V> RichIterable<V> collectWith(
            @Independent(hc = true) @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @Modified P arg1) { return null; }

        @Independent @Modified
        <P, V, R extends Collection<V>> R collectWith(
            @Independent(hc = true) @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @Modified P arg1,
            @Independent @Modified R arg2) { return null; }
        @NotModified boolean contains(@Independent @Modified Object arg0) { return false; }
        @NotModified boolean containsAll(@Independent @Modified Collection<?> arg0) { return false; }
        @NotModified boolean containsAllArguments(@Independent @Modified Object ... arg0) { return false; }
        @NotModified boolean containsAllIterable(@Independent @Modified Iterable<?> arg0) { return false; }
        @Modified boolean containsAny(@Independent @Modified Collection<?> source) { return false; }
        @Modified boolean containsAnyIterable(@Independent @Modified Iterable<?> source) { return false; }
        @Modified
        <V> boolean containsBy(
            @Independent @Modified Function<? super T, ? extends V> function,
            @Independent @NotModified V value) { return false; }
        @Modified boolean containsNone(@Independent @Modified Collection<?> source) { return false; }
        @Modified boolean containsNoneIterable(@Independent @Modified Iterable<?> source) { return false; }
        @NotModified int count(@Independent @Modified Predicate<? super T> arg0) { return 0; }
        @Independent @Modified
        <V> Bag<V> countBy(@Independent @Modified Function<? super T, ? extends V> function) { return null; }

        @Independent @Modified
        <V, R extends MutableBagIterable<V>> R countBy(
            @Independent @Modified Function<? super T, ? extends V> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <V> Bag<V> countByEach(@Independent @Modified Function<? super T, ? extends Iterable<V>> function) {
            return null;
        }

        @Independent @Modified
        <V, R extends MutableBagIterable<V>> R countByEach(
            @Independent @Modified Function<? super T, ? extends Iterable<V>> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <V, P> Bag<V> countByWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends V> function,
            @Independent @Modified P parameter) { return null; }

        @Independent @Modified
        <V, P, R extends MutableBagIterable<V>> R countByWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends V> function,
            @Independent @Modified P parameter,
            @Independent @Modified R target) { return null; }

        @NotModified
        <P> int countWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return 0; }

        @Independent(hc = true) @NotModified
        T detect(@Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent @NotModified
        T detectIfNone(
            @Independent @Modified Predicate<? super T> predicate,
            @Independent @NotModified Function0<? extends T> function) { return null; }

        @Independent(hc = true) @Modified
        Optional<T> detectOptional(@Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        <P> T detectWith(@Independent @Modified Predicate2<? super T, ? super P> arg0, @Independent @NotModified P arg1) {
            return null;
        }

        @Independent(hc = true) @Modified
        <P> T detectWithIfNone(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1,
            @Independent @Modified Function0<? extends T> arg2) { return null; }

        @Independent(hc = true) @Modified
        <P> Optional<T> detectWithOptional(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @Modified void each(@Independent @Modified Procedure<? super T> arg0) { }
        @Independent(absent = true) @Modified
        <V> RichIterable<V> flatCollect(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R flatCollect(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0,
            @Independent @Modified R arg1) { return null; }

        @Independent @Modified
        <R extends MutableBooleanCollection> R flatCollectBoolean(
            @Independent @Modified Function<? super T, ? extends BooleanIterable> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableByteCollection> R flatCollectByte(
            @Independent @Modified Function<? super T, ? extends ByteIterable> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableCharCollection> R flatCollectChar(
            @Independent @Modified Function<? super T, ? extends CharIterable> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableDoubleCollection> R flatCollectDouble(
            @Independent @Modified Function<? super T, ? extends DoubleIterable> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableFloatCollection> R flatCollectFloat(
            @Independent @Modified Function<? super T, ? extends FloatIterable> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableIntCollection> R flatCollectInt(
            @Independent @Modified Function<? super T, ? extends IntIterable> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableLongCollection> R flatCollectLong(
            @Independent @Modified Function<? super T, ? extends LongIterable> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableShortCollection> R flatCollectShort(
            @Independent @Modified Function<? super T, ? extends ShortIterable> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <P, V> RichIterable<V> flatCollectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @Modified P parameter) { return null; }

        @Independent @Modified
        <P, V, R extends Collection<V>> R flatCollectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @Modified P parameter,
            @Independent @Modified R target) { return null; }

        //override from org.eclipse.collections.api.InternalIterable
        @Modified
        void forEach(@Independent @Modified Procedure<? super T> procedure) { }
        @Independent @NotModified T getAny() { return null; }
        @Independent(hc = true) @NotModified T getFirst() { return null; }
        @Independent(hc = true) @NotModified T getLast() { return null; }
        @Independent @NotModified T getOnly() { return null; }
        @Independent(absent = true) @Modified
        <V> Multimap<V, T> groupBy(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        @Independent(hc = true) @Modified
        <V, R extends MutableMultimap<V, T>> R groupBy(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0,
            @Independent(hc = true) @Modified R arg1) { return null; }

        @Independent @Modified
        <K, V, R extends MutableMultimap<K, V>> R groupByAndCollect(
            @Independent @Modified Function<? super T, ? extends K> groupByFunction,
            @Independent @Modified Function<? super T, ? extends V> collectFunction,
            @Independent @Modified R target) { return null; }

        @Independent(absent = true) @Modified
        <V> Multimap<V, T> groupByEach(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        @Independent(hc = true) @Modified
        <V, R extends MutableMultimap<V, T>> R groupByEach(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0,
            @Independent(hc = true) @Modified R arg1) { return null; }

        @Independent(hc = true) @Modified
        <V> MapIterable<V, T> groupByUniqueKey(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        @Independent(hc = true) @Modified
        <V, R extends MutableMapIterable<V, T>> R groupByUniqueKey(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0,
            @Independent(hc = true) @Modified R arg1) { return null; }

        @Independent(hc = true) @NotModified
        <IV> IV injectInto(
            @Independent(hc = true) @Modified IV arg0,
            @Independent(hc = true) @Modified Function2<? super IV, ? super T, ? extends IV> arg1) { return null; }

        @NotModified
        double injectInto(double arg0, @Independent @NotModified DoubleObjectToDoubleFunction<? super T> arg1) {
            return 0.0;
        }

        @NotModified
        float injectInto(float arg0, @Independent @NotModified FloatObjectToFloatFunction<? super T> arg1) { return 0.0F; }

        @NotModified
        int injectInto(int arg0, @Independent @NotModified IntObjectToIntFunction<? super T> arg1) { return 0; }

        @NotModified
        long injectInto(long arg0, @Independent @NotModified LongObjectToLongFunction<? super T> arg1) { return 0L; }

        @Modified
        double injectIntoDouble(
            double injectedValue,
            @Independent @NotModified DoubleObjectToDoubleFunction<? super T> function) { return 0.0; }

        @Modified
        float injectIntoFloat(
            float injectedValue,
            @Independent @NotModified FloatObjectToFloatFunction<? super T> function) { return 0.0F; }

        @Modified
        int injectIntoInt(int injectedValue, @Independent @NotModified IntObjectToIntFunction<? super T> function) {
            return 0;
        }

        @Modified
        long injectIntoLong(long injectedValue, @Independent @NotModified LongObjectToLongFunction<? super T> function) {
            return 0L;
        }

        @Independent(hc = true) @Modified
        <R extends Collection<T>> R into(@Independent(hc = true) @Modified R arg0) { return null; }
        @NotModified boolean isEmpty() { return false; }
        @NotModified String makeString() { return null; }
        @NotModified String makeString(String separator) { return null; }
        @NotModified String makeString(String start, String separator, String end) { return null; }
        @NotModified
        String makeString(
            @Independent @Modified Function<? super T, Object> function,
            String start,
            String separator,
            String end) { return null; }
        @Independent(hc = true) @NotModified T max() { return null; }
        @Independent(hc = true) @NotModified
        T max(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        <V extends Comparable<? super V>> T maxBy(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) { return null; }

        @Independent @Modified
        <V extends Comparable<? super V>> Optional<T> maxByOptional(
            @Independent @Modified Function<? super T, ? extends V> function) { return null; }
        @Independent @Modified Optional<T> maxOptional() { return null; }
        @Independent @Modified
        Optional<T> maxOptional(@Independent @NotModified Comparator<? super T> comparator) { return null; }
        @Independent(hc = true) @NotModified T min() { return null; }
        @Independent(hc = true) @NotModified
        T min(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        <V extends Comparable<? super V>> T minBy(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) { return null; }

        @Independent @Modified
        <V extends Comparable<? super V>> Optional<T> minByOptional(
            @Independent @Modified Function<? super T, ? extends V> function) { return null; }
        @Independent @Modified Optional<T> minOptional() { return null; }
        @Independent @Modified
        Optional<T> minOptional(@Independent @NotModified Comparator<? super T> comparator) { return null; }
        @NotModified boolean noneSatisfy(@Independent @Modified Predicate<? super T> arg0) { return false; }
        @NotModified
        <P> boolean noneSatisfyWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return false; }
        @NotModified boolean notEmpty() { return false; }
        @Independent(absent = true) @Modified
        PartitionIterable<T> partition(@Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent(absent = true) @Modified
        <P> PartitionIterable<T> partitionWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        @Independent @Modified
        Optional<T> reduce(@Independent @Modified BinaryOperator<T> accumulator) { return null; }

        @Independent @Modified
        <K> MapIterable<K, T> reduceBy(
            @Independent @Modified Function<? super T, ? extends K> groupBy,
            @Independent @NotModified Function2<? super T, ? super T, ? extends T> reduceFunction) { return null; }

        @Independent @Modified
        <K, R extends MutableMapIterable<K, T>> R reduceBy(
            @Independent @Modified Function<? super T, ? extends K> groupBy,
            @Independent @NotModified Function2<? super T, ? super T, ? extends T> reduceFunction,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R> R reduceInPlace(
            @Independent @Modified Supplier<R> supplier,
            @Independent @Modified BiConsumer<R, ? super T> accumulator) { return null; }

        @Independent @Modified
        <R, A> R reduceInPlace(@Independent @Modified Collector<? super T, A, R> collector) { return null; }

        @Independent(absent = true) @Modified
        RichIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent(hc = true) @Modified
        <R extends Collection<T>> R reject(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent(hc = true) @Modified R arg1) { return null; }

        @Independent(absent = true) @Modified
        <P> RichIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        @Independent(hc = true) @Modified
        <P, R extends Collection<T>> R rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1,
            @Independent(hc = true) @Modified R arg2) { return null; }

        @Independent(absent = true) @Modified
        RichIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent(hc = true) @Modified
        <R extends Collection<T>> R select(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent(hc = true) @Modified R arg1) { return null; }
        @Independent(absent = true) @Modified <S> RichIterable<S> selectInstancesOf(Class<S> arg0) { return null; }
        @Independent(absent = true) @Modified
        <P> RichIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        @Independent(hc = true) @Modified
        <P, R extends Collection<T>> R selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1,
            @Independent(hc = true) @Modified R arg2) { return null; }
        @NotModified int size() { return 0; }
        @Independent(hc = true) @Modified
        <V> ObjectDoubleMap<V> sumByDouble(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0,
            @Independent @Modified DoubleFunction<? super T> arg1) { return null; }

        @Independent(hc = true) @Modified
        <V> ObjectDoubleMap<V> sumByFloat(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0,
            @Independent @Modified FloatFunction<? super T> arg1) { return null; }

        @Independent @Modified
        <V> ObjectLongMap<V> sumByInt(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0,
            @Independent @Modified IntFunction<? super T> arg1) { return null; }

        @Independent @Modified
        <V> ObjectLongMap<V> sumByLong(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0,
            @Independent @Modified LongFunction<? super T> arg1) { return null; }
        @NotModified double sumOfDouble(@Independent @Modified DoubleFunction<? super T> arg0) { return 0.0; }
        @NotModified double sumOfFloat(@Independent @Modified FloatFunction<? super T> arg0) { return 0.0; }
        @NotModified long sumOfInt(@Independent @Modified IntFunction<? super T> arg0) { return 0L; }
        @NotModified long sumOfLong(@Independent @Modified LongFunction<? super T> arg0) { return 0L; }
        @Independent @Modified
        DoubleSummaryStatistics summarizeDouble(@Independent @Modified DoubleFunction<? super T> function) {
            return null;
        }

        @Independent @Modified
        DoubleSummaryStatistics summarizeFloat(@Independent @Modified FloatFunction<? super T> function) { return null; }

        @Independent @Modified
        IntSummaryStatistics summarizeInt(@Independent @Modified IntFunction<? super T> function) { return null; }

        @Independent @Modified
        LongSummaryStatistics summarizeLong(@Independent @Modified LongFunction<? super T> function) { return null; }
        @Independent @Modified RichIterable<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }
        @Independent @NotModified Object [] toArray() { return null; }
        @Independent @NotModified <E> E [] toArray(@Independent @Modified E [] array) { return null; }
        @Independent @NotModified MutableBag<T> toBag() { return null; }
        @Independent @Modified
        <NK, NV> MutableBiMap<NK, NV> toBiMap(
            @Independent(hc = true) @Modified Function<? super T, ? extends NK> arg0,
            @Independent(hc = true) @Modified Function<? super T, ? extends NV> arg1) { return null; }
        @Independent @Modified ImmutableBag<T> toImmutableBag() { return null; }
        @Independent @Modified
        <NK, NV> ImmutableBiMap<NK, NV> toImmutableBiMap(
            @Independent @Modified Function<? super T, ? extends NK> keyFunction,
            @Independent @Modified Function<? super T, ? extends NV> valueFunction) { return null; }
        @Independent @Modified ImmutableList<T> toImmutableList() { return null; }
        @Independent @Modified
        <NK, NV> ImmutableMap<NK, NV> toImmutableMap(
            @Independent @Modified Function<? super T, ? extends NK> keyFunction,
            @Independent @Modified Function<? super T, ? extends NV> valueFunction) { return null; }
        @Independent @Modified ImmutableSet<T> toImmutableSet() { return null; }
        @Independent @Modified ImmutableSortedBag<T> toImmutableSortedBag() { return null; }
        @Independent @Modified
        ImmutableSortedBag<T> toImmutableSortedBag(@Independent @Modified Comparator<? super T> comparator) {
            return null;
        }

        @Independent @Modified
        <V extends Comparable<? super V>> ImmutableSortedBag<T> toImmutableSortedBagBy(
            @Independent @NotModified Function<? super T, ? extends V> function) { return null; }
        @Independent @Modified ImmutableList<T> toImmutableSortedList() { return null; }
        @Independent @Modified
        ImmutableList<T> toImmutableSortedList(@Independent @NotModified Comparator<? super T> comparator) {
            return null;
        }

        @Independent @Modified
        <V extends Comparable<? super V>> ImmutableList<T> toImmutableSortedListBy(
            @Independent @NotModified Function<? super T, ? extends V> function) { return null; }
        @Independent @Modified ImmutableSortedSet<T> toImmutableSortedSet() { return null; }
        @Independent @Modified
        ImmutableSortedSet<T> toImmutableSortedSet(@Independent @NotModified Comparator<? super T> comparator) {
            return null;
        }

        @Independent @Modified
        <V extends Comparable<? super V>> ImmutableSortedSet<T> toImmutableSortedSetBy(
            @Independent @NotModified Function<? super T, ? extends V> function) { return null; }
        @Independent @NotModified MutableList<T> toList() { return null; }
        @Independent @NotModified
        <NK, NV> MutableMap<NK, NV> toMap(
            @Independent(hc = true) @Modified Function<? super T, ? extends NK> arg0,
            @Independent(hc = true) @Modified Function<? super T, ? extends NV> arg1) { return null; }

        @Independent @NotModified
        <NK, NV, R extends Map<NK, NV>> R toMap(
            @Independent @Modified Function<? super T, ? extends NK> keyFunction,
            @Independent @Modified Function<? super T, ? extends NV> valueFunction,
            @Independent @Modified R target) { return null; }
        @Independent @NotModified MutableSet<T> toSet() { return null; }
        @Independent @Modified MutableSortedBag<T> toSortedBag() { return null; }
        @Independent @Modified
        MutableSortedBag<T> toSortedBag(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent @Modified
        <V extends Comparable<? super V>> MutableSortedBag<T> toSortedBagBy(
            @Independent @Modified Function<? super T, ? extends V> function) { return null; }
        @Fluent @Independent @NotModified MutableList<T> toSortedList() { return null; }
        @Fluent @Independent @NotModified
        MutableList<T> toSortedList(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        @Fluent @Independent @Modified
        <V extends Comparable<? super V>> MutableList<T> toSortedListBy(
            @Independent @Modified Function<? super T, ? extends V> function) { return null; }

        @Independent @NotModified
        <NK, NV> MutableSortedMap<NK, NV> toSortedMap(
            @Independent @NotModified Comparator<? super NK> arg0,
            @Independent(hc = true) @Modified Function<? super T, ? extends NK> arg1,
            @Independent(hc = true) @Modified Function<? super T, ? extends NV> arg2) { return null; }

        @Independent @NotModified
        <NK, NV> MutableSortedMap<NK, NV> toSortedMap(
            @Independent(hc = true) @Modified Function<? super T, ? extends NK> arg0,
            @Independent(hc = true) @Modified Function<? super T, ? extends NV> arg1) { return null; }

        @Independent @Modified
        <KK extends Comparable<? super KK>, NK, NV> MutableSortedMap<NK, NV> toSortedMapBy(
            @Independent @Modified Function<? super NK, KK> sortBy,
            @Independent @Modified Function<? super T, ? extends NK> keyFunction,
            @Independent @Modified Function<? super T, ? extends NV> valueFunction) { return null; }
        @Independent @NotModified MutableSortedSet<T> toSortedSet() { return null; }
        @Independent @NotModified
        MutableSortedSet<T> toSortedSet(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent @Modified
        <V extends Comparable<? super V>> MutableSortedSet<T> toSortedSetBy(
            @Independent @Modified Function<? super T, ? extends V> function) { return null; }

        //override from java.lang.Object
        @NotModified @NotNull
        public String toString() { return null; }

        @Independent(hc = true) @Modified
        <S> RichIterable<Pair<T, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        @Independent @Modified
        <S, R extends Collection<Pair<T, S>>> R zip(
            @Independent @NotModified Iterable<S> arg0,
            @Independent @Modified R arg1) { return null; }
        @Independent(hc = true) @Modified RichIterable<Pair<T, Integer>> zipWithIndex() { return null; }
        @Independent @Modified
        <R extends Collection<Pair<T, Integer>>> R zipWithIndex(@Independent @Modified R arg0) { return null; }
    }

    //public interface ShortIterable implements PrimitiveIterable
    @FinalFields
    @Independent
    class ShortIterable$ {
        @NotModified boolean allSatisfy(@Independent @NotModified ShortPredicate arg0) { return false; }
        @NotModified boolean anySatisfy(@Independent @NotModified ShortPredicate arg0) { return false; }
        @Independent @Modified LazyShortIterable asLazy() { return null; }
        @NotModified double average() { return 0.0; }
        @Modified double averageIfEmpty(double defaultValue) { return 0.0; }
        @Independent @Modified RichIterable<ShortIterable> chunk(int size) { return null; }
        @Independent @Modified
        <V> RichIterable<V> collect(@Independent @Modified ShortToObjectFunction<? extends V> arg0) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R collect(
            @Independent @Modified ShortToObjectFunction<? extends V> function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableBooleanCollection> R collectBoolean(
            @Independent @Modified ShortToBooleanFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableByteCollection> R collectByte(
            @Independent @Modified ShortToByteFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableCharCollection> R collectChar(
            @Independent @Modified ShortToCharFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableDoubleCollection> R collectDouble(
            @Independent @Modified ShortToDoubleFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableFloatCollection> R collectFloat(
            @Independent @Modified ShortToFloatFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableIntCollection> R collectInt(
            @Independent @Modified ShortToIntFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableLongCollection> R collectLong(
            @Independent @Modified ShortToLongFunction function,
            @Independent @Modified R target) { return null; }

        @Independent @Modified
        <R extends MutableShortCollection> R collectShort(
            @Independent @Modified ShortToShortFunction function,
            @Independent @Modified R target) { return null; }
        @NotModified boolean contains(short arg0) { return false; }
        @NotModified boolean containsAll(@Independent @Modified ShortIterable source) { return false; }
        @NotModified boolean containsAll(@Independent @NotModified short ... source) { return false; }
        @Modified boolean containsAny(@Independent @Modified ShortIterable source) { return false; }
        @Modified boolean containsAny(@Independent @NotModified short ... source) { return false; }
        @Modified boolean containsNone(@Independent @Modified ShortIterable source) { return false; }
        @Modified boolean containsNone(@Independent @NotModified short ... source) { return false; }
        @NotModified int count(@Independent @NotModified ShortPredicate arg0) { return 0; }
        @NotModified short detectIfNone(@Independent @NotModified ShortPredicate arg0, short arg1) { return 0; }
        @Modified void each(@Independent @Modified ShortProcedure arg0) { }
        @Independent @Modified
        <V, R extends Collection<V>> R flatCollect(
            @Independent @Modified ShortToObjectFunction<? extends Iterable<V>> function,
            @Independent @Modified R target) { return null; }
        @Modified void forEach(@Independent @Modified ShortProcedure procedure) { }
        @Independent @NotModified
        <T> T injectInto(
            @Independent @NotModified T arg0,
            @Independent @Modified ObjectShortToObjectFunction<? super T, ? extends T> arg1) { return null; }

        @Modified
        boolean injectIntoBoolean(boolean injectedValue, @Independent @Modified BooleanShortToBooleanFunction function) {
            return false;
        }

        @Modified
        byte injectIntoByte(byte injectedValue, @Independent @Modified ByteShortToByteFunction function) { return 0; }

        @Modified
        char injectIntoChar(char injectedValue, @Independent @Modified CharShortToCharFunction function) { return '\0'; }

        @Modified
        double injectIntoDouble(double injectedValue, @Independent @Modified DoubleShortToDoubleFunction function) {
            return 0.0;
        }

        @Modified
        float injectIntoFloat(float injectedValue, @Independent @Modified FloatShortToFloatFunction function) {
            return 0.0F;
        }

        @Modified
        int injectIntoInt(int injectedValue, @Independent @Modified IntShortToIntFunction function) { return 0; }

        @Modified
        long injectIntoLong(long injectedValue, @Independent @Modified LongShortToLongFunction function) { return 0L; }

        @Modified
        short injectIntoShort(short injectedValue, @Independent @Modified ShortShortToShortFunction function) { return 0; }
        @NotModified short max() { return 0; }
        @Modified short maxIfEmpty(short arg0) { return 0; }
        @NotModified double median() { return 0.0; }
        @Modified double medianIfEmpty(double defaultValue) { return 0.0; }
        @NotModified short min() { return 0; }
        @Modified short minIfEmpty(short arg0) { return 0; }
        @NotModified boolean noneSatisfy(@Independent @NotModified ShortPredicate predicate) { return false; }
        @Modified long reduce(@Independent @Modified LongShortToLongFunction accumulator) { return 0L; }
        @Modified
        long reduceIfEmpty(@Independent @Modified LongShortToLongFunction accumulator, long defaultValue) { return 0L; }
        @Independent @Modified ShortIterable reject(@Independent @NotModified ShortPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableShortCollection> R reject(
            @Independent @NotModified ShortPredicate predicate,
            @Independent @Modified R target) { return null; }
        @Independent @Modified ShortIterable select(@Independent @NotModified ShortPredicate arg0) { return null; }
        @Independent @Modified
        <R extends MutableShortCollection> R select(
            @Independent @NotModified ShortPredicate predicate,
            @Independent @Modified R target) { return null; }
        @Independent @Modified ShortIterator shortIterator() { return null; }
        @NotModified long sum() { return 0L; }
        @Independent @Modified IntSummaryStatistics summaryStatistics() { return null; }
        @Fluent @Independent @Modified
        ShortIterable tap(@Independent @Modified ShortProcedure procedure) { return null; }
        @Independent @NotModified short [] toArray() { return null; }
        @Independent @NotModified short [] toArray(@Independent @NotModified short [] target) { return null; }
        @Independent @NotModified MutableShortBag toBag() { return null; }
        @Independent @NotModified MutableShortList toList() { return null; }
        @Independent @NotModified MutableShortSet toSet() { return null; }
        @Independent @Modified short [] toSortedArray() { return null; }
        @Independent @NotModified MutableShortList toSortedList() { return null; }
        @Independent @NotModified
        MutableShortList toSortedList(@Independent @Modified ShortComparator comparator) { return null; }

        @Independent @Modified
        <T> MutableShortList toSortedListBy(@Independent @Modified ShortToObjectFunction<T> function) { return null; }

        @Independent @Modified
        <T> MutableShortList toSortedListBy(
            @Independent @Modified ShortToObjectFunction<T> function,
            @Independent @NotModified Comparator<? super T> comparator) { return null; }
    }
}
