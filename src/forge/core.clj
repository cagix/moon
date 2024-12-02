(in-ns 'clojure.core)

(require '[clj-commons.pretty.repl :as pretty-repl]
         '[clojure.edn :as edn]
         '[clojure.pprint]
         '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[clojure.math :as math]
         '[data.grid2d :as g2d]
         '[forge.screen :as screen] ; FIXME breaks reloading
         '[malli.core :as m]
         '[malli.error :as me]
         '[malli.generator :as mg])

(import '(com.badlogic.gdx Gdx)
        '(com.badlogic.gdx.assets AssetManager)
        '(com.badlogic.gdx.audio Sound)
        '(com.badlogic.gdx.graphics Color Pixmap)
        '(com.badlogic.gdx.scenes.scene2d Actor Stage)
        '(com.badlogic.gdx.math MathUtils Circle Intersector Rectangle Vector2)
        '(com.badlogic.gdx.utils Align Scaling Disposable ScreenUtils))

(comment
 ; Change namespace to forge.core then call this.  And start with lein repl and without :main set so other ns don't get loaded
 (->> *ns*
      ns-publics
      (remove (fn [[k v]] (:macro (meta v))))
      (map (fn [s] (str "\"" (name (first s)) "\"")))
      (str-join ", ")
      (spit "vimrc_names")))

(def signum math/signum)

(def io-resource io/resource)

(def edn-read-string edn/read-string)

(def str-split         str/split)
(def str-join          str/join)
(def capitalize        str/capitalize)
(def str-replace       str/replace)
(def str-replace-first str/replace-first)
(def str-trim-newline  str/trim-newline)

(defn pretty-pst [t]
  (binding [*print-level* 3]
    (pretty-repl/pretty-pst t 24)))

(def pprint clojure.pprint/pprint)

(defmacro defsystem
  {:arglists '([name docstring? params?])}
  [name-sym & args]
  (let [docstring (if (string? (first args))
                    (first args))
        params (if (string? (first args))
                 (second args)
                 (first args))
        params (if (nil? params)
                 '[_]
                 params)]
    (when (zero? (count params))
      (throw (IllegalArgumentException. "First argument needs to be component.")))
    (when-let [avar (resolve name-sym)]
      (println "WARNING: Overwriting defsystem:" avar))
    `(defmulti ~(vary-meta name-sym assoc :params (list 'quote params))
       ~(str "[[defsystem]] `" (str params) "`"
             (when docstring (str "\n\n" docstring)))
       (fn [[k#] & _args#]
         k#))))

(declare ^:dynamic *k*)

(defn- add-methods [system-vars ns-sym k & {:keys [optional?]}]
  (doseq [system-var system-vars
          :let [method-var (ns-resolve ns-sym (:name (meta system-var)))]]
    (assert (or optional? method-var)
            (str "Cannot find required `" (:name (meta system-var)) "` function in " ns-sym))
    (when method-var
      (assert (keyword? k))
      (assert (var? method-var) (pr-str method-var))
      (let [system @system-var]
        (when (k (methods system))
          (println "WARNING: Overwriting method" (:name (meta method-var)) "on" k))
        (clojure.lang.MultiFn/.addMethod system k (fn call-method [[k & vs] & args]
                                                    (binding [*k* k]
                                                      (apply method-var (into (vec vs) args)))))))))

(defn install-component [component-systems ns-sym k]
  (require ns-sym)
  (add-methods (:required component-systems) ns-sym k)
  (add-methods (:optional component-systems) ns-sym k :optional? true))

(defn mapvals [f m]
  (into {} (for [[k v] m]
             [k (f v)])))

(defn safe-get [m k]
  (let [result (get m k ::not-found)]
    (if (= result ::not-found)
      (throw (IllegalArgumentException. (str "Cannot find " (pr-str k))))
      result)))

(defn bind-root [avar value]
  (clojure.lang.Var/.bindRoot avar value))

(defn equal? [a b]
  (MathUtils/isEqual a b))

(defn clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

(defn degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn gdx-align [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

(defn gdx-scaling [k]
  (case k
    :fill Scaling/fill))

(defn- gdx-field [klass-str k]
  (eval (symbol (str "com.badlogic.gdx." klass-str "/" (str/replace (str/upper-case (name k)) "-" "_")))))

(defn gdx-color
  ([r g b]
   (gdx-color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(def ^Color black Color/BLACK)
(def ^Color white Color/WHITE)

(defn ->gdx-color ^Color [c]
  (cond (= Color (class c)) c
        (keyword? c) (gdx-field "graphics.Color" c)
        (vector? c) (apply gdx-color c)
        :else (throw (ex-info "Cannot understand color" c))))

(def ^:private k->input-button (partial gdx-field "Input$Buttons"))
(def ^:private k->input-key    (partial gdx-field "Input$Keys"))

(defn button-just-pressed?
  ":left, :right, :middle, :back or :forward."
  [b]
  (.isButtonJustPressed Gdx/input (k->input-button b)))

(defn key-just-pressed?
  "See [[key-pressed?]]."
  [k]
  (.isKeyJustPressed Gdx/input (k->input-key k)))

(defn key-pressed?
  "For options see [libgdx Input$Keys docs](https://javadoc.io/doc/com.badlogicgames.gdx/gdx/latest/com/badlogic/gdx/Input.Keys.html).
  Keys are upper-cased and dashes replaced by underscores.
  For example Input$Keys/ALT_LEFT can be used with :alt-left.
  Numbers via :num-3, etc."
  [k]
  (.isKeyPressed Gdx/input (k->input-key k)))

(declare ^AssetManager asset-manager)

(defn play-sound [name]
  (Sound/.play (get asset-manager (str "sounds/" name ".wav"))))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(defn exit-app []
  (.exit Gdx/app))

(def dispose Disposable/.dispose)

(def visible? Actor/.isVisible)

(defn clear-screen [color]
  (ScreenUtils/clear color))

(defn internal-file [path]
  (.internal Gdx/files path))

(defn gdx-cursor [[file [hotspot-x hotspot-y]]]
  (let [pixmap (Pixmap. (internal-file (str "cursors/" file ".png")))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (dispose pixmap)
    cursor))

(declare screens
         ^:private current-screen-key)

(defn current-screen []
  (and (bound? #'current-screen-key)
       (current-screen-key screens)))

(defn change-screen
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current-screen)]
    (screen/exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (bind-root #'current-screen-key new-k)
    (screen/enter screen)))

(defn screen-stage ^Stage []
  (:stage (current-screen)))

(defn add-actor [actor]
  (.addActor (screen-stage) actor))

(defn reset-stage [new-actors]
  (.clear (screen-stage))
  (run! add-actor new-actors))

(defn set-input-processor [processor]
  (.setInputProcessor Gdx/input processor))

(defn post-runnable [runnable]
  (.postRunnable Gdx/app runnable))

(declare db-schemas
         db-properties-file
         db-properties)

(defn schema-of [k]
  (safe-get db-schemas k))

(defn schema-type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defmulti malli-form schema-type)
(defmethod malli-form :default [schema] schema)

(defmethod malli-form :s/number  [_] number?)
(defmethod malli-form :s/nat-int [_] nat-int?)
(defmethod malli-form :s/int     [_] int?)
(defmethod malli-form :s/pos     [_] pos?)
(defmethod malli-form :s/pos-int [_] pos-int?)

(defmethod malli-form :s/sound [_] :string)

(defmethod malli-form :s/image [_]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod malli-form :s/animation [_]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defmethod malli-form :s/one-to-one [[_ property-type]]
  [:qualified-keyword {:namespace (type->id-namespace property-type)}])

(defmethod malli-form :s/one-to-many [[_ property-type]]
  [:set [:qualified-keyword {:namespace (type->id-namespace property-type)}]])

(defn- attribute-form
  "Can define keys as just keywords or with schema-props like [:foo {:optional true}]."
  [ks]
  (for [k ks
        :let [k? (keyword? k)
              schema-props (if k? nil (k 1))
              k (if k? k (k 0))]]
    (do
     (assert (keyword? k))
     (assert (or (nil? schema-props) (map? schema-props)) (pr-str ks))
     [k schema-props (malli-form (schema-of k))])))

(defn- map-form [ks]
  (apply vector :map {:closed true} (attribute-form ks)))

(defmethod malli-form :s/map [[_ ks]]
  (map-form ks))

(defmethod malli-form :s/map-optional [[_ ks]]
  (map-form (map (fn [k] [k {:optional true}]) ks)))

(defn- namespaced-ks [ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys db-schemas)))

(defmethod malli-form :s/components-ns [[_ ns-name-k]]
  (malli-form [:s/map-optional (namespaced-ks ns-name-k)]))

(defn property-type [{:keys [property/id]}]
  (keyword "properties" (namespace id)))

(defn property-types []
  (filter #(= "properties" (namespace %))
          (keys db-schemas)))

(defn schema-of-property [property]
  (schema-of (property-type property)))

(defn- invalid-ex-info [m-schema value]
  (ex-info (str (me/humanize (m/explain m-schema value)))
           {:value value
            :schema (m/form m-schema)}))

(defn validate! [property]
  (let [m-schema (-> property
                     schema-of-property
                     malli-form
                     m/schema)]
    (when-not (m/validate m-schema property)
      (throw (invalid-ex-info m-schema property)))))

(defn k->default-value [k]
  (let [schema (schema-of k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema-type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (mg/generate (malli-form schema) {:size 3}))))

(defn- async-pprint-spit! [properties]
  (.start
   (Thread.
    (fn []
      (binding [*print-level* nil]
        (->> properties
             pprint
             with-out-str
             (spit db-properties-file)))))))

(defn- recur-sort-map [m]
  (into (sorted-map)
        (zipmap (keys m)
                (map #(if (map? %)
                        (recur-sort-map %)
                        %)
                     (vals m)))))

(defn- async-write-to-file! []
  (->> db-properties
       vals
       (sort-by property-type)
       (map recur-sort-map)
       doall
       async-pprint-spit!))

(defn get-raw [id]
  (safe-get db-properties id))

(defn all-raw [type]
  (->> (vals db-properties)
       (filter #(= type (property-type %)))))

(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file
   :sub-image-bounds})

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

(declare build)

(defmulti edn->value (fn [schema v]
                       (when schema  ; undefined-data-ks
                         (schema-type schema))))
(defmethod edn->value :default [_schema v] v)

(defmethod edn->value :s/one-to-many [_ property-ids]
  (set (map build property-ids)))

(defmethod edn->value :s/one-to-one [_ property-id]
  (build property-id))

(defn- build* [property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema-of k)
                                 (catch Throwable _t
                                   (swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build* v)
                         v)]
                 (try (edn->value schema v)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

(defn build [id]
  (build* (get-raw id)))

(defn build-all [type]
  (map build* (all-raw type)))

(defn db-update! [{:keys [property/id] :as property}]
  {:pre [(contains? property :property/id)
         (contains? db-properties id)]}
  (validate! property)
  (alter-var-root #'db-properties assoc id property)
  (async-write-to-file!))

(defn db-delete! [property-id]
  {:pre [(contains? db-properties property-id)]}
  (alter-var-root #'db-properties dissoc property-id)
  (async-write-to-file!))

(defn db-migrate [property-type update-fn]
  (doseq [id (map :property/id (all-raw property-type))]
    (println id)
    (alter-var-root #'db-properties update id update-fn))
  (async-write-to-file!))

(defn property->image [{:keys [entity/image entity/animation]}]
  (or image
      (first (:frames animation))))

(defn index-of [k ^clojure.lang.PersistentVector v]
  (let [idx (.indexOf v k)]
    (if (= -1 idx)
      nil
      idx)))

(defn find-first
  "Returns the first item of coll for which (pred item) returns logical true.
  Consumes sequences up to the first match, will consume the entire sequence
  and return nil if no match is found."
  [pred coll]
  (first (filter pred coll)))

(defn ->tile [position]
  (mapv int position))

(defn tile->middle [position]
  (mapv (partial + 0.5) position))

(defn safe-merge [m1 m2]
  {:pre [(not-any? #(contains? m1 %) (keys m2))]}
  (merge m1 m2))

; libgdx fn is available:
; (MathUtils/isEqual 1 (length v))
(defn- approx-numbers [a b epsilon]
  (<=
    (Math/abs (float (- a b)))
    epsilon))

(defn- round-n-decimals [^double x n]
  (let [z (Math/pow 10 n)]
    (float
      (/
        (Math/round (float (* x z)))
        z))))

(defn readable-number [^double x]
  {:pre [(number? x)]} ; do not assert (>= x 0) beacuse when using floats x may become -0.000...000something
  (if (or
        (> x 5)
        (approx-numbers x (int x) 0.001)) ; for "2.0" show "2" -> simpler
    (int x)
    (round-n-decimals x 2)))

(defn define-order [order-k-vector]
  (apply hash-map (interleave order-k-vector (range))))

(defn sort-by-order [coll get-item-order-k order]
  (sort-by #((get-item-order-k %) order) < coll))

#_(defn order-contains? [order k]
  ((apply hash-set (keys order)) k))

#_(deftest test-order
  (is
    (= (define-order [:a :b :c]) {:a 0 :b 1 :c 2}))
  (is
    (order-contains? (define-order [:a :b :c]) :a))
  (is
    (not
      (order-contains? (define-order [:a :b :c]) 2)))
  (is
    (=
      (sort-by-order [:c :b :a :b] identity (define-order [:a :b :c]))
      '(:a :b :b :c)))
  (is
    (=
      (sort-by-order [:b :c :null :null :a] identity (define-order [:c :b :a :null]))
      '(:c :b :a :null :null))))

(defn k->pretty-name [k]
  (str/capitalize (name k)))

(defn assoc-ks [m ks v]
  (if (empty? ks)
    m
    (apply assoc m (interleave ks (repeat v)))))

(defn truncate [s limit]
  (if (> (count s) limit)
    (str (subs s 0 limit) "...")
    s))

(defn ->edn-str [v]
  (binding [*print-level* nil]
    (pr-str v)))

(def dev-mode? (= (System/getenv "DEV_MODE") "true"))

(defn indexed ; from clojure.contrib.seq-utils (discontinued in 1.3)
  "Returns a lazy sequence of [index, item] pairs, where items come
 from 's' and indexes count up from zero.

 (indexed '(a b c d)) => ([0 a] [1 b] [2 c] [3 d])"
  [s]
  (map vector (iterate inc 0) s))

(defn utils-positions ; from clojure.contrib.seq-utils (discontinued in 1.3)
  "Returns a lazy sequence containing the positions at which pred
	 is true for items in coll."
  [pred coll]
  (for [[idx elt] (indexed coll) :when (pred elt)] idx))

(defmacro when-seq [[aseq bind] & body]
  `(let [~aseq ~bind]
     (when (seq ~aseq)
       ~@body)))

(defn dissoc-in [m ks]
  (assert (> (count ks) 1))
  (update-in m (drop-last ks) dissoc (last ks)))

(defsystem handle [_ ctx])

(defsystem applicable? [_ ctx])

(defsystem useful?          [_  ctx])
(defmethod useful? :default [_ _ctx] true)

(defsystem render-effect           [_  ctx])
(defmethod render-effect :default  [_ _ctx])

(defn effects-applicable? [ctx effects]
  (seq (filter #(applicable? % ctx) effects)))

(defn effects-useful? [ctx effects]
  (->> effects
       (effects-applicable? ctx)
       (some #(useful? % ctx))))

(defn effects-do! [ctx effects]
  (run! #(handle % ctx)
        (effects-applicable? ctx effects)))

(defn effects-render [ctx effects]
  (run! #(render-effect % ctx)
        effects))

;; rename to 'shuffle', rand and rand-int without the 's'-> just use with require :as.
;; maybe even remove the when coll pred?
;; also maybe *random* instead passing it everywhere? but not sure about that
(defn sshuffle
  "Return a random permutation of coll"
  ([coll random]
    (when coll
      (let [al (java.util.ArrayList. ^java.util.Collection coll)]
        (java.util.Collections/shuffle al random)
        (clojure.lang.RT/vector (.toArray al)))))
  ([coll]
    (sshuffle coll (java.util.Random.))))

(defn srand
  ([random] (.nextFloat ^java.util.Random random))
  ([n random] (* n (srand random))))

(defn srand-int [n random]
  (int (srand n random)))

(defn create-seed []
  (.nextLong (java.util.Random.)))

; TODO assert int?
(defn rand-int-between
  "returns a random integer between lower and upper bounds inclusive."
  ([[lower upper]]
    (rand-int-between lower upper))
  ([lower upper]
    (+ lower (rand-int (inc (- upper lower))))))

(defn rand-float-between [[lower upper]]
  (+ lower (rand (- upper lower))))

; TODO use 0-1 not 0-100 internally ? just display it different?
; TODO assert the number between 0 and 100
(defn percent-chance
  "perc is number between 0 and 100."
  ([perc random]
    (< (srand random)
       (/ perc 100)))
  ([perc]
    (percent-chance perc (java.util.Random.))))
; TODO Random. does not return a number between 0 and 100?

(defmacro if-chance
  ([n then]
    `(if-chance ~n ~then nil))
  ([n then else]
    `(if (percent-chance ~n) ~then ~else)))

(defmacro when-chance [n & more]
  `(when (percent-chance ~n)
     ~@more))

(defn get-rand-weighted-item
  "given a sequence of items and their weight, returns a weighted random item.
 for example {:a 5 :b 1} returns b only in about 1 of 6 cases"
  [weights]
  (let [result (rand-int (reduce + (map #(% 1) weights)))]
    (loop [r 0
           items weights]
      (let [[item weight] (first items)
            r (+ r weight)]
        (if (> r result)
          item
          (recur (int r) (rest items)))))))

(defn get-rand-weighted-items [n group]
  (repeatedly n #(get-rand-weighted-item group)))

(comment
  (frequencies (get-rand-weighted-items 1000 {:a 1 :b 5 :c 4}))
  (frequencies (repeatedly 1000 #(percent-chance 90))))

(defn high-weighted "for values of x 0-1 returns y values 0-1 with higher value of y than a linear function"
  [x]
  (- 1 (Math/pow (- 1 x) 2)))

(defn high-weighted-rand-int [n]
  (int (* n (high-weighted (rand)))))

(defn high-weighted-rand-nth [coll]
  (nth coll (high-weighted-rand-int (count coll))))

(defn- m-v2
  ([[x y]] (Vector2. x y))
  ([x y]   (Vector2. x y)))

(defn- ->p [^Vector2 v]
  [(.x ^Vector2 v) (.y ^Vector2 v)])

(defn v-scale [v n]
  (->p (.scl ^Vector2 (m-v2 v) (float n))))

(defn v-normalise [v]
  (->p (.nor ^Vector2 (m-v2 v))))

(defn v-add [v1 v2]
  (->p (.add ^Vector2 (m-v2 v1)
             ^Vector2 (m-v2 v2))))

(defn v-length [v]
  (.len ^Vector2 (m-v2 v)))

(defn v-distance [v1 v2]
  (.dst ^Vector2 (m-v2 v1) ^Vector2 (m-v2 v2)))

(defn v-normalised? [v]
  (equal? 1 (v-length v)))

(defn v-normal-vectors [[x y]]
  [[(- (float y))         x]
   [          y (- (float x))]])

(defn v-direction [[sx sy] [tx ty]]
  (v-normalise [(- (float tx) (float sx))
                (- (float ty) (float sy))]))

(defn v-angle-from-vector
  "converts theta of Vector2 to angle from top (top is 0 degree, moving left is 90 degree etc.), counterclockwise"
  [v]
  (.angleDeg (m-v2 v) (Vector2. 0 1)))

(comment

 (pprint
  (for [v [[0 1]
           [1 1]
           [1 0]
           [1 -1]
           [0 -1]
           [-1 -1]
           [-1 0]
           [-1 1]]]
    [v
     (.angleDeg (m-v2 v) (Vector2. 0 1))
     (get-angle-from-vector (m-v2 v))]))

 )

(defn v-diagonal-direction? [[x y]]
  (and (not (zero? (float x)))
       (not (zero? (float y)))))

(defn- rectangle? [{[x y] :left-bottom :keys [width height]}]
  (and x y width height))

(defn- circle? [{[x y] :position :keys [radius]}]
  (and x y radius))

(defn- m->shape [m]
  (cond
   (rectangle? m) (let [{:keys [left-bottom width height]} m
                        [x y] left-bottom]
                    (Rectangle. x y width height))

   (circle? m) (let [{:keys [position radius]} m
                     [x y] position]
                 (Circle. x y radius))

   :else (throw (Error. (str m)))))

(defmulti ^:private overlaps?* (fn [a b] [(class a) (class b)]))

(defmethod overlaps?* [Circle Circle]
  [^Circle a ^Circle b]
  (Intersector/overlaps a b))

(defmethod overlaps?* [Rectangle Rectangle]
  [^Rectangle a ^Rectangle b]
  (Intersector/overlaps a b))

(defmethod overlaps?* [Rectangle Circle]
  [^Rectangle rect ^Circle circle]
  (Intersector/overlaps circle rect))

(defmethod overlaps?* [Circle Rectangle]
  [^Circle circle ^Rectangle rect]
  (Intersector/overlaps circle rect))

(defn overlaps? [a b]
  (overlaps?* (m->shape a) (m->shape b)))

(defn rect-contains? [rectangle [x y]]
  (Rectangle/.contains (m->shape rectangle) x y))

(defn circle->outer-rectangle [{[x y] :position :keys [radius] :as circle}]
  {:pre [(circle? circle)]}
  (let [radius (float radius)
        size (* radius 2)]
    {:left-bottom [(- (float x) radius)
                   (- (float y) radius)]
     :width  size
     :height size}))

(def ^:private offsets [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]])

; using this instead of g2d/get-8-neighbour-positions, because `for` there creates a lazy seq.
(defn get-8-neighbour-positions [position]
  (mapv #(mapv + position %) offsets))

#_(defn- get-8-neighbour-positions [[x y]]
  (mapv (fn [tx ty]
          [tx ty])
   (range (dec x) (+ x 2))
   (range (dec y) (+ y 2))))

(def grid2d g2d/create-grid)

(defmacro defmethods [k & sys-impls]
  `(do
    ~@(for [[sys & fn-body] sys-impls
            :let [sys-var (resolve sys)]]
        `(do
          (when (get (methods @~sys-var) ~k)
            (println "WARNING: Overwriting defmethod" ~k "on" ~sys-var))
          (defmethod ~sys ~k ~(symbol (str (name (symbol sys-var)) "." (name k)))
            ~@fn-body)))
    ~k))
