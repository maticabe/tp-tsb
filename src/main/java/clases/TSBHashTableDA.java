package clases;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Clase para emular la funcionalidad de la clase java.util.Hashtable provista
 * en forma nativa por Java. Una TSBHashtable usa un arreglo de listas de la
 * clase TSBArrayList a modo de buckets (o listas de desborde) para resolver las
 * colisiones que pudieran presentarse.
 * <p>
 * Se almacenan en la tabla pares de objetos (key, value), en donde el objeto
 * key actúa como clave para identificar al objeto value. La tabla no admite
 * repetición de claves (no se almacenarán dos pares de objetos con la misma
 * clave). Tampoco acepta referencias nulas (tanto para las key como para los
 * values): no será insertado un par (key, value) si alguno de ambos objetos es
 * null.
 * <p>
 * Se ha emulado tanto como ha sido posible el comportamiento de la clase ya
 * indicada java.util.Hashtable. En esa clase, el parámetro loadFactor se usa
 * para determinar qué tan llena está la tabla antes de lanzar un proceso de
 * rehash: si loadFactor es 0.75, entonces se hará un rehash cuando la cantidad
 * de casillas ocupadas en el arreglo de soporte sea un 75% del tamaño de ese
 * arreglo. En nuestra clase TSBHashtable, mantuvimos el concepto de loadFactor
 * (ahora llamado load_factor) pero con una interpretación distinta: en nuestro
 * modelo, se lanza un rehash si la cantidad promedio de valores por lista es
 * mayor a cierto número constante y pequeño, que asociamos al load_factor para
 * mantener el espíritu de la implementación nativa. En nuestro caso, si el
 * valor load_factor es 0.8 entonces se lanzará un rehash si la cantidad
 * promedio de valores por lista es mayor a 0.8 * 10 = 8 elementos por lista.
 *
 * @param <K> el tipo de los objetos que serán usados como clave en la tabla.
 * @param <V> el tipo de los objetos que serán los valores de la tabla.
 * @author Ing. Valerio Frittelli.
 * @version Septiembre de 2017.
 */
public class TSBHashTableDA<K, V> implements Map<K, V>, Cloneable, Serializable {

    // el tamaño máximo que podrá tener el arreglo de soprte...
    private final static int MAX_SIZE = Integer.MAX_VALUE;

    //************************ Atributos privados (estructurales).

    // la tabla hash: el arreglo que contiene las listas de desborde...
    private Entry<K, V>[] table;

    // el tamaño inicial de la tabla (tamaño con el que fue creada)...
    private int initial_capacity;

    // la cantidad de objetos que contiene la tabla en TODAS sus listas...
    private int count;

    // el factor de carga para calcular si hace falta un rehashing...
    private float load_factor;

    // Arreglo que almacenará el estado de la casilla
    private int[] fieldState;


    //************************ Atributos privados (para gestionar las vistas).

    /*
     * (Tal cual están definidos en la clase java.util.Hashtable)
     * Cada uno de estos campos se inicializa para contener una instancia de la
     * vista que sea más apropiada, la primera vez que esa vista es requerida.
     * La vista son objetos stateless (no se requiere que almacenen datos, sino
     * que sólo soportan operaciones), y por lo tanto no es necesario crear más
     * de una de cada una.
     */

    //************************ Atributos protegidos (control de iteración).

    // conteo de operaciones de cambio de tamaño (fail-fast iterator).
    protected transient int modCount;


    //************************ Constructores.

    /**
     * Crea una tabla vacía, con la capacidad inicial igual a 11 y con factor
     * de carga igual a 0.8f.
     */
    public TSBHashTableDA() {
        this(59, 0.8f);
    }

    /**
     * Crea una tabla vacía, con la capacidad inicial indicada y con factor
     * de carga igual a 0.8f.
     *
     * @param initial_capacity la capacidad inicial de la tabla.
     */
    public TSBHashTableDA(int initial_capacity) {
        this(initial_capacity, 0.8f);
    }

    /**
     * Crea una tabla vacía, con la capacidad inicial indicada y con el factor
     * de carga indicado. Si la capacidad inicial indicada por initial_capacity
     * es menor o igual a 0, la tabla será creada de tamaño 11. Si el factor de
     * carga indicado es negativo o cero, se ajustará a 0.8f.
     *
     * @param initial_capacity la capacidad inicial de la tabla.
     * @param load_factor      el factor de carga de la tabla.
     */
    public TSBHashTableDA(int initial_capacity, float load_factor) {
        if (load_factor <= 0) {
            load_factor = 0.8f;
        }
        if (initial_capacity <= 0) {
            initial_capacity = 59;
        } else {
            if (initial_capacity > TSBHashTableDA.MAX_SIZE) {
                initial_capacity = TSBHashTableDA.MAX_SIZE;
            }
        }

        this.table = new Entry[initial_capacity];
        this.fieldState = new int[initial_capacity];

        this.initial_capacity = initial_capacity;
        this.load_factor = load_factor;
        this.modCount = 0;
        this.count = 0;
    }

    /**
     * Crea una tabla a partir del contenido del Map especificado.
     *
     * @param t el Map a partir del cual se creará la tabla.
     */
    public TSBHashTableDA(Map<? extends K, ? extends V> t) {
        this(59, 0.8f);
        this.putAll(t);
    }


    //************************ Implementación de métodos especificados por Map.

    /**
     * Retorna la cantidad de elementos contenidos en la tabla.
     *
     * @return la cantidad de elementos de la tabla.
     */
    @Override
    public int size() {
        return this.count;
    }

    /**
     * Determina si la tabla está vacía (no contiene ningún elemento).
     *
     * @return true si la tabla está vacía.
     */
    @Override
    public boolean isEmpty() {
        return (this.count == 0);
    }

    /**
     * Determina si la clave key está en la tabla.
     *
     * @param key la clave a verificar.
     * @return true si la clave está en la tabla.
     * @throws NullPointerException si la clave es null.
     */
    @Override
    public boolean containsKey(Object key) {
        return (this.get((K) key) != null);
    }

    /**
     * Determina si alguna clave de la tabla está asociada al objeto value que
     * entra como parámetro. Equivale a contains().
     *
     * @param value el objeto a buscar en la tabla.
     * @return true si alguna clave está asociada efectivamente a ese value.
     */
    @Override
    public boolean containsValue(Object value) {
        return this.contains(value);
    }

    /**
     * Determina si alguna clave de la tabla está asociada al objeto value que
     * entra como parámetro. Equivale a containsValue().
     *
     * @param value el objeto a buscar en la tabla.
     * @return true si alguna clave está asociada efectivamente a ese value.
     */
    public boolean contains(Object value) {
        if (value == null) return false;

        Iterator<Map.Entry<K, V>> iterator = entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, V> entry = iterator.next();
            if (value.equals(entry.getValue())) return true;
        }
        return false;
    }

    /**
     * Retorna el objeto al cual está asociada la clave key en la tabla, o null
     * si la tabla no contiene ningún objeto asociado a esa clave.
     *
     * @param key la clave que será buscada en la tabla.
     * @return el objeto asociado a la clave especificada (si existe la clave) o
     * null (si no existe la clave en esta tabla).
     * @throws NullPointerException si key es null.
     * @throws ClassCastException   si la clase de key no es compatible con la
     *                              tabla.
     */
    @Override
    public V get(Object key) {
        if (key == null) throw new NullPointerException("get(): parámetro null");

        int hashingIndex = h((K) key);
        int quadraticIndex = hashingIndex;
        int j = 1;
        V value = null;

        while (fieldState[quadraticIndex] != 0) {
            if (fieldState[quadraticIndex] == 1) {
                Entry<K, V> entry = this.table[quadraticIndex];

                if (key.equals(entry.getKey())) {
                    value = entry.getValue();
                    return value;
                }
            }

            quadraticIndex += j * j;
            j++;
            if (quadraticIndex >= table.length) {
                quadraticIndex %= table.length;
            }

        }
        return value;
    }

    /**
     * Asocia el valor (value) especificado, con la clave (key) especificada en
     * esta tabla. Si la tabla contenía previamente un valor asociado para la
     * clave, entonces el valor anterior es reemplazado por el nuevo (y en este
     * caso el tamaño de la tabla no cambia).
     *
     * @param key   la clave del objeto que se quiere agregar a la tabla.
     * @param value el objeto que se quiere agregar a la tabla.
     * @return el objeto anteriormente asociado a la clave si la clave ya
     * estaba asociada con alguno, o null si la clave no estaba antes
     * asociada a ningún objeto.
     * @throws NullPointerException si key es null o value es null.
     */
    @Override
    public V put(K key, V value) {
        if (key == null || value == null) throw new NullPointerException("put(): parámetro null");

        int qi = h(key);
        int firstTombstone = -1;
        int j = 1;
        V old = null;

        // Checkeo si la key existía
        while (fieldState[qi] == 1) {
            // Si la casilla está cerrada en la posición aux checkeo si es el mismo elemento
            if (fieldState[qi] == 1) {
                Entry<K, V> entry = table[qi];
                // Si es el mismo value lo sobreescribo y devuelvo el anterior
                if (key.equals(entry.getKey())) {
                    old = entry.getValue();
                    entry.setValue(value);
                    count++;
                    modCount++;
                    return old;
                }
            }
            // Si en el camino encuentro un index tumba lo guardo
            if (fieldState[qi] >= 0) firstTombstone = qi;

            // Calculamos el nuevo index con el método quadratic probing
            qi += j * j;
            j++;
            if (qi >= table.length) {
                qi %= table.length;
            }
        }
        // Si el index firstTombstone existe sobreescribimos el index cuadrático
        if (firstTombstone >= 0) qi = firstTombstone;

        // Si el index está en estado abierto o tumba
        table[qi] = new Entry<>(key, value);
        fieldState[qi] = 1;

        count++;
        modCount++;

        // Check del load factor
        float lf = (float) count / table.length;
        if (lf >= load_factor) rehash();

        return old;
    }

    @Override
    public V remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<K> keySet() {
        return null;
    }

    @Override
    public Collection<V> values() {
        return null;
    }

    /**
     * Retorna un Set (conjunto) a modo de vista de todos los pares (key, value)
     * contenidos en la tabla. El conjunto está respaldado por la tabla, por lo
     * que los cambios realizados en la tabla serán reflejados en el conjunto, y
     * viceversa. Si la tabla es modificada mientras un iterador está actuando
     * sobre el conjunto vista, el resultado de la iteración será indefinido
     * (salvo que la modificación sea realizada por la operación remove() propia
     * del iterador, o por la operación setValue() realizada sobre una entrada
     * de la tabla que haya sido retornada por el iterador). El conjunto vista
     * provee métodos para eliminar elementos, y esos métodos a su vez
     * eliminan el correspondiente par (key, value) de la tabla (a través de las
     * operaciones Iterator.remove(), Set.remove(), removeAll(), retainAll()
     * and clear()). El conjunto vista no soporta las operaciones add() y
     * addAll() (si se las invoca, se lanzará una UnsuportedOperationException).
     *
     * @return un conjunto (un Set) a modo de vista de todos los objetos
     * mapeados en la tabla.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (entrySet == null) {
            // entrySet = Collections.synchronizedSet(new EntrySet());
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    /**
     * Función de hashing
     *
     * @param k clave
     * @param s tamaño de la tabla
     * @return índice válido de acuerdo a los parámetros pasados.
     */
    private int h(int k, int s) {
        if (k < 0) k *= -1;
        return k % s;
    }

    /**
     * Función de hashing (overladed)
     *
     * @param key clave
     * @param s   tamaño de la tabla
     * @return índice válido de acuerdo a los parámetros pasados.
     */
    private int h(K key, int s) {
        return h(key.hashCode(), s);
    }

    /**
     * Función de hashing (overloaded)
     *
     * @param key clave
     * @return índice válido de la tabla de acuerdo a la clave que se pasó.
     */
    private int h(K key) {
        return h(key, table.length);
    }

    /**
     * Incrementa el tamaño de la tabla y reorganiza su contenido. Se invoca
     * automaticamente cuando se detecta que la cantidad promedio de nodos por
     * lista supera a cierto el valor critico dado por (10 * load_factor). Si el
     * valor de load_factor es 0.8, esto implica que el límite antes de invocar
     * rehash es de 8 nodos por lista en promedio, aunque seria aceptable hasta
     * unos 10 nodos por lista.
     */
    private void rehash() {
        int oldLenght = table.length;
        int newLenght = nextPrime(oldLenght * 2 + 1);

        // Creamos las nueva tabla y los estados de la misma
        Entry<K, V>[] auxTable = new Entry[newLenght];
        int[] auxFieldState = new int[newLenght];

        Arrays.fill(auxFieldState, 0);

        // Como la tabla cambio incrementamos el contador de modificaciones
        modCount++;

        // Recorremos la tabla anterior y redispersamos en la nueva tabla
        for (int i = 0; i < table.length; i++) {
            // Checkeo cada index cerrado y para él obtengo un nuevo valor de hash para la nueva tabla
            if (fieldState[i] == 1) {
                Entry<K, V> old = table[i];

                K key = old.getKey();
                int qi = h(key, auxTable.length), j = 1;

                while (auxFieldState[qi] != 0) {
                    qi += j * j;
                    j++;
                    if (qi >= auxTable.length) {
                        qi %= auxTable.length;
                    }
                }

                // Insertamos el nuevo array
                auxTable[qi] = old;
                auxFieldState[qi] = 1;
            }
        }
        table = auxTable;
        fieldState = auxFieldState;
    }

    /**
     * Calcula el siguiente número primo del parametro. Es conveniente usar una tabla de tamaño equivalente a un
     * número primo para evitar la tendencia de que los hash de enteros grandes tengan divisores comunes con el
     * tamaño de la tabla hash, que provocaría colisiones.
     *
     * @param n número del cual se quiere obtener el siguiente primo
     * @return el siguiente número primo al parámetro
     */
    private int nextPrime(int n) {
        if (n % 2 == 0) n++;
        while (!isPrime(n)) {
            n += 2;
        }
        return n;
    }

    /**
     * Determina si un número es primo
     *
     * @param n el número que se quiere saber si es primo
     * @return true si es primo, false si no lo es
     */
    private boolean isPrime(int n) {
        if (n <= 1) return false;

        for (int i = 2; i < n; i++) {
            if (n % i == 0) return false;
        }

        return true;
    }

    //************************ Clases Internas.
    /*
     * Clase interna que representa los pares de objetos que se almacenan en la
     * tabla hash: son instancias de esta clase las que realmente se guardan en
     * en cada una de las listas del arreglo table que se usa como soporte de
     * la tabla. Lanzará una IllegalArgumentException si alguno de los dos
     * parámetros es null.
     */
    private static class Entry<K, V> implements Map.Entry<K, V> {
        private K key;
        private V value;

        public Entry(K key, V value) {
            if (key == null || value == null) throw new IllegalArgumentException("Entry(): parámetro null...");

            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            if (value == null) {
                throw new IllegalArgumentException("setValue(): parámetro null...");
            }

            V old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 61 * hash + Objects.hashCode(this.key);
            hash = 61 * hash + Objects.hashCode(this.value);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }

            final Entry other = (Entry) obj;
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return Objects.equals(this.value, other.value);
        }

        @Override
        public String toString() {
            return "(" + key.toString() + ", " + value.toString() + ")";
        }
    }

    /*
     * Clase interna que representa una vista de todas los Claves mapeadas en la
     * tabla: si la vista cambia, cambia también la tabla que le da respaldo, y
     * viceversa. La vista es stateless: no mantiene estado alguno (es decir, no
     * contiene datos ella misma, sino que accede y gestiona directamente datos
     * de otra fuente), por lo que no tiene atributos y sus métodos gestionan en
     * forma directa el contenido de la tabla. Están soportados los metodos para
     * eliminar un objeto (remove()), eliminar todo el contenido (clear) y la
     * creación de un Iterator (que incluye el método Iterator.remove()).
     */
    private transient Set<K> keySet = null;
    private transient Set<Map.Entry<K, V>> entrySet = null;
    private transient Collection<V> values = null;

    private class KeySet extends AbstractSet<K> {

        @Override
        public Iterator<K> iterator() {
            return new KeySetIterator();
        }

        @Override
        public int size() {
            return TSBHashTableDA.this.count;
        }

        @Override
        public boolean contains(Object o) {
            return TSBHashTableDA.this.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return (TSBHashTableDA.this.remove(o) != null);
        }

        @Override
        public void clear() {
            TSBHashTableDA.this.clear();
        }

        private class KeySetIterator implements Iterator<K> {

            private int currentEntry;
            private int lastEntry;
            private boolean isNextOK;
            private int expectedModCount;

            /*
             * Crea un iterador comenzando en la primera lista. Activa el mecanismo
             * fail-fast.
             */
            public KeySetIterator() {
                currentEntry = 0;
                lastEntry = 0;
                isNextOK = false;
                expectedModCount = TSBHashTableDA.this.modCount;
            }

            /*
             * Determina si hay al menos un elemento en la tabla que no haya sido retornado
             * por next().
             */
            @Override
            public boolean hasNext() {
                // variable auxiliar t y s para simplificar accesos...
                Entry<K, V>[] t = TSBHashTableDA.this.table;
                int[] s = TSBHashTableDA.this.fieldState;

                if (currentEntry >= t.length) {
                    return false;
                }

                // busco el siguiente indice cerrado
                int next_entry = currentEntry + 1;
                for (int i = next_entry; i < t.length; i++) {
                    if (s[i] == 1) return true;
                }

                // Si no encontro ninguno retorno false
                return false;
            }

            @Override
            public K next() {
                // control: fail-fast iterator...
                if (TSBHashTableDA.this.modCount != expectedModCount) {
                    throw new ConcurrentModificationException("next(): modificación inesperada de tabla...");
                }

                if (!hasNext()) {
                    throw new NoSuchElementException("next(): no existe el elemento pedido...");
                }

                // variable auxiliar t y s para simplificar accesos...
                Entry<K, V>[] t = TSBHashTableDA.this.table;
                int[] s = TSBHashTableDA.this.fieldState;

                // busco el siguiente indice cerrado
                int nextEntry = currentEntry;
                for (nextEntry++; s[nextEntry] != 1; nextEntry++) ; // TODO: buscar implementación alternativa

                // Actualizo los indices
                lastEntry = currentEntry;
                currentEntry = nextEntry;

                // avisar que next() fue invocado con éxito...
                isNextOK = true;

                // y retornar la clave del elemento alcanzado...

                return t[currentEntry].getKey();
            }

            /*
             * Remueve el elemento actual de la tabla, dejando el iterador en la
             * posición anterior al que fue removido. El elemento removido es el
             * que fue retornado la última vez que se invocó a next(). El método
             * sólo puede ser invocado una vez por cada invocación a next().
             */
            @Override
            public void remove() {
                // Control: fail-fast iterator
                if (TSBHashTableDA.this.modCount != expectedModCount)
                    throw new ConcurrentModificationException("remove(): modificación concurrente no permitida");

                if (!isNextOK)
                    throw new IllegalArgumentException("remove(): debe invocar a next() antes de remove()...");

                // eliminar el objeto que retornó next() la última vez...
                TSBHashTableDA.this.table[currentEntry] = null;
                TSBHashTableDA.this.fieldState[currentEntry] = 2;

                // quedar apuntando al anterior al que se retornará...
                currentEntry = lastEntry;

                // avisar que el remove() válido para next() ya se activó...
                isNextOK = false;

                // la tabla tiene un elemento menos
                TSBHashTableDA.this.count--;

                // fail-fast iterator: todo en orden...
                TSBHashTableDA.this.modCount++;
                expectedModCount++;
            }
        }
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntrySetIterator();
        }

        @Override
        public int size() {
            return TSBHashTableDA.this.count;
        }

        @Override
        public boolean contains(Object o) {
            if (o == null) return false;
            if (!(o instanceof Entry)) return false;

            Entry<K, V>[] t = TSBHashTableDA.this.table;
            int[] s = TSBHashTableDA.this.fieldState;

            Entry<K, V> entry = (Entry<K, V>) o;

            int qi = TSBHashTableDA.this.h(entry.getKey());
            int j = 1;

            // Búsqueda del entry
            while (s[qi] != 0) {
                // Si la casilla qi está cerrada checkeo si es el mismo
                if (s[qi] == 1) {
                    Entry<K, V> tableEntry = t[qi];
                    if (tableEntry.equals(entry)) return true;
                }

                // nuevo index
                qi += j * j;
                j++;
                if (qi >= t.length) qi %= t.length;
            }
            return false;
        }

        @Override
        public void clear() {
            TSBHashTableDA.this.clear();
        }

        @Override
        public boolean remove(Object o) {
            if (o == null) throw new NullPointerException("remove(): parámetro null");
            if (!(o instanceof Entry)) {
                return false;
            }

            Entry<K, V>[] t = TSBHashTableDA.this.table;
            int[] s = TSBHashTableDA.this.fieldState;

            Entry<K, V> entry = (Entry<K, V>) o;

            int qi = TSBHashTableDA.this.h(entry.getKey());
            int j = 1;

            // Búsqueda del objeto
            while (s[qi] != 0) {
                if (s[qi] == 1) {
                    Entry<K, V> tableEntry = t[qi];

                    // Eliminación si se encuentra
                    if (tableEntry.equals(entry)) {
                        t[qi] = null;
                        s[qi] = 2;

                        TSBHashTableDA.this.count--;
                        TSBHashTableDA.this.modCount++;

                        return true;
                    }
                }
                // nuevo index
                qi += j * j;
                j++;
                if (qi >= t.length) qi %= t.length;
            }

            return false;
        }

        private class EntrySetIterator implements Iterator<Map.Entry<K, V>> {

            private int currentEntry;
            private int lastEntry;
            private boolean isNextOK;
            private int expectedModCount;

            public EntrySetIterator() {
                currentEntry = -1;
                lastEntry = 0;
                isNextOK = false;
                expectedModCount = TSBHashTableDA.this.modCount;
            }

            /*
             * Determina si hay al menos un elemento en la tabla que no haya
             * sido retornado por next().
             */
            @Override
            public boolean hasNext() {
                Entry<K, V>[] t = TSBHashTableDA.this.table;
                int[] s = TSBHashTableDA.this.fieldState;
                //Si el index currenEntry es mayor al tamaño de la tabla retornamos false
                if (currentEntry >= t.length) return false;

                // Buscamos el siguiente index cerrado
                int nextEntry = currentEntry + 1;
                for (int i = nextEntry; i < t.length; i++) {
                    if (s[i] == 1) return true;
                }
                // Retornamos false si no se encontró
                return false;
            }

            /*
             * Retorna el siguiente elemento disponible en la tabla.
             */
            @Override
            public Entry<K, V> next() {
                // Control: fail-fast iterator...
                if (TSBHashTableDA.this.modCount != expectedModCount)
                    throw new ConcurrentModificationException("next(): modificación concurrente no permitida");

                if (!hasNext()) throw new NoSuchElementException("next(): no existe el elemento pedido");

                Entry<K, V>[] t = TSBHashTableDA.this.table;
                int[] s = TSBHashTableDA.this.fieldState;

                // Búsqueda del siguiente index cerrado
                int nextEntry = currentEntry;
                for (nextEntry++; s[nextEntry] != 1; nextEntry++) ; // TODO: buscar implementación alternativa

                lastEntry = currentEntry;
                currentEntry = nextEntry;

                isNextOK = true;

                return t[currentEntry];
            }

            @Override
            public void remove() {
                if (!isNextOK) throw new IllegalStateException("remove(): debe invocar a next() antes de remove()...");

                // Eliminamos el objeto de la tabla que retornó next() la última vez...
                TSBHashTableDA.this.table[currentEntry] = null;
                TSBHashTableDA.this.fieldState[currentEntry] = 2;

                // queda apuntando al anterior al que se retornó...
                currentEntry = lastEntry;

                // avisar que el remove() válido para next() ya se activó...
                isNextOK = false;

                // la tabla tiene un elementon menos...
                TSBHashTableDA.this.count--;

                // fail_fast iterator: todo en orden...
                TSBHashTableDA.this.modCount++;
                expectedModCount++;
            }
        }
    }

    // TODO: ValueCollection


    /**
     * Retorna un hash code para la tabla completa.
     *
     * @return un hash code para la tabla.
     */
    @Override
    public int hashCode() {
        if (this.isEmpty()) return 0;

        return Arrays.hashCode(table);
    }

    /**
     * Determina si esta tabla es igual al objeto espeficicado.
     *
     * @param obj el objeto a comparar con esta tabla.
     * @return true si los objetos son iguales.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Map)) {
            return false;
        }

        Map<K, V> t = (Map<K, V>) obj;
        if (t.size() != this.size()) {
            return false;
        }

        try {
            Iterator<Map.Entry<K, V>> i = this.entrySet.iterator();
            while (i.hasNext()) {
                Map.Entry<K, V> e = i.next();
                K key = e.getKey();
                V value = e.getValue();
                if (t.get(key) == null) {
                    return false;
                } else {
                    if (!value.equals(t.get(key))) {
                        return false;
                    }
                }
            }
        } catch (ClassCastException e) {
            return false;
        }

        return true;
    }

    /**
     * Retorna una copia superficial de la tabla.
     *
     * @return una copia superficial de la tabla.
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {

        TSBHashtable<K, V> t = new TSBHashtable<>(this.table.length, this.load_factor);

        for (Map.Entry<K, V> entry : this.entrySet()) {
            t.put(entry.getKey(), entry.getValue());
        }

        return t;
    }

    /**
     * Devuelve el contenido de la tabla en forma de String.
     *
     * @return una cadena con el contenido completo de la tabla.
     */
    @Override
    public String toString() {

        StringBuilder str = new StringBuilder();
        str.append("\nTabla: [\n");
        for (int i = 0; i < this.table.length; i++) {
            if (this.table[i] == null) {
                str.append("\t\n");
            } else {
                str.append("\t").append(this.table[i].toString()).append("\n");
            }
        }
        str.append("}");

        return str.toString();
    }
}
