(ns clojure.gamedev)

(def ^:dynamic *unit-scale* 1)

(def sound-asset-format "sounds/%s.wav")

(declare
 pretty-pst
 edn-read-string
 io-resource
 str-join
 str-upper-case
 str-replace
 str-replace-first
 str-split
 str-capitalize
 str-trim-newline
 signum
 set-difference
 pprint
 ^{:doc "Supports clojure.lang.ILookup (get), passing an asset-name string and returns the asset."}
 assets
 batch
 shape-drawer
 default-font
 cursors
 gui-viewport
 gui-viewport-width
 gui-viewport-height
 world-unit-scale
 world-viewport
 world-viewport-width
 world-viewport-height
 cached-map-renderer
 screens
 current-screen-key
 world-tiled-map
 explored-tile-corners
 world-grid
 tick-error
 paused?
 ids->eids
 content-grid
 ray-caster
 ^{:doc "The elapsed in-game-time in seconds (not counting when game is paused)."}
 elapsed-time
 ^{:doc "The game logic update delta-time. Different then forge.graphics/delta-time because it is bounded by a maximum value for entity movement speed."}
 world-delta
 player-eid
 schemas
 properties-file
 db-properties
 black
 white
 ->color
 ttfont
 load-assets
 sprite-batch
 equal?
 clamp
 degree->radians
 exit-app
 post-runnable
 frames-per-second
 delta-time
 button-just-pressed?
 key-just-pressed?
 key-pressed?
 set-input-processor
 internal-file
 ^{:doc "font, h-align, up? and scale are optional.
        h-align one of: :center, :left, :right. Default :center.
        up? renders the font over y, otherwise under.
        scale will multiply the drawn text size with the scale.
        `[{:keys [font x y text h-align up? scale]}`"}
 draw-text
 add-actor
 reset-stage
 grid2d
 v-scale
 v-normalise
 v-add
 v-length
 v-distance
 v-normalised?
 v-direction
 ^{:doc "converts theta of Vector2 to angle from top (top is 0 degree, moving left is 90 degree etc.), counterclockwise"}
 v-angle-from-vector
 overlaps?
 rect-contains?)

(defprotocol Acting
  (act [_]))

(defprotocol Drawing
  (draw [_]))

(defprotocol Group
  (children [_] "Returns an ordered list of child actors in this group.")
  (clear-children [_] "Removes all actors from this group and unfocuses them.")
  (add-actor! [_ actor] "Adds an actor as a child of this group, removing it from its previous parent. If the actor is already a child of this group, no changes are made.")
  (find-actor [_ name]))

(defprotocol HasUserObject
  (user-object [_]))

(defprotocol Batch
  (draw-texture-region [_ texture-region [x y] [w h] rotation color])
  (draw-on-viewport [_ viewport draw-fn]))

(defprotocol Disposable
  (dispose [_]))

(defprotocol Sound
  (play [_]))

(defprotocol HasVisible
  (set-visible [_ bool])
  (visible? [_]))

(defprotocol Screen
  (screen-enter   [_])
  (screen-exit    [_])
  (screen-render  [_])
  (screen-destroy [_]))

(defprotocol HasProperties
  (m-props ^MapProperties [_] "Returns instance of com.badlogic.gdx.maps.MapProperties")
  (get-property [_ key] "Pass keyword key, looks up in properties."))

(defprotocol GridCell
  (cell-blocked? [cell* z-order])
  (blocks-vision? [cell*])
  (occupied-by-other? [cell* eid]
                      "returns true if there is some occupying body with center-tile = this cell
                      or a multiple-cell-size body which touches this cell.")
  (nearest-entity          [cell* faction])
  (nearest-entity-distance [cell* faction]))

(defn safe-get [m k]
  (let [result (get m k ::not-found)]
    (if (= result ::not-found)
      (throw (IllegalArgumentException. (str "Cannot find " (pr-str k))))
      result)))

(defn recur-sort-map [m]
  (into (sorted-map)
        (zipmap (keys m)
                (map #(if (map? %)
                        (recur-sort-map %)
                        %)
                     (vals m)))))

; reduce-kv?
(defn apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

(defn bind-root [avar value]
  (clojure.lang.Var/.bindRoot avar value))

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

(defn mapvals [f m]
  (into {} (for [[k v] m]
             [k (f v)])))

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

(def dev-mode? (= (System/getenv "DEV_MODE") "true"))

(defn ->tile [position]
  (mapv int position))

(defn tile->middle [position]
  (mapv (partial + 0.5) position))

(defsystem app-create)

(defsystem app-dispose)
(defmethod app-dispose :default [_])

(defsystem app-render)
(defmethod app-render :default [_])

(defsystem app-resize)
(defmethod app-resize :default [_ w h])

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

(defn v-normal-vectors [[x y]]
  [[(- (float y))         x]
   [          y (- (float x))]])

(defn v-diagonal-direction? [[x y]]
  (and (not (zero? (float x)))
       (not (zero? (float y)))))

(defn rectangle? [{[x y] :left-bottom :keys [width height]}]
  (and x y width height))

(defn circle? [{[x y] :position :keys [radius]}]
  (and x y radius))

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

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defsystem component-info)
(defmethod component-info :default [_])

(declare info-color
         info-text-k-order)

(defn- apply-color [k info-text]
  (if-let [color (info-color k)]
    (str "[" color "]" info-text "[]")
    info-text))

(defn- sort-k-order [components]
  (sort-by (fn [[k _]] (or (index-of k info-text-k-order) 99))
           components))

(declare ^:dynamic *info-text-entity*)

(defn- remove-newlines [s]
  (let [new-s (-> s
                  (str-replace "\n\n" "\n")
                  (str-replace #"^\n" "")
                  str-trim-newline)]
    (if (= (count new-s) (count s))
      s
      (remove-newlines new-s))))

(defn info-text [components]
  (->> components
       sort-k-order
       (keep (fn [{k 0 v 1 :as component}]
               (str (try (binding [*info-text-entity* components]
                           (apply-color k (component-info component)))
                         (catch Throwable t
                           ; calling from property-editor where entity components
                           ; have a different data schema than after component/create
                           ; and info-text might break
                           (pr-str component)))
                    (when (map? v)
                      (str "\n" (info-text v))))))
       (str-join "\n")
       remove-newlines))

(defn k->pretty-name [k]
  (str-capitalize (name k)))

(defsystem ->v "Create component value. Default returns v.")
(defmethod ->v :default [[_ v]] v)

(defsystem e-create [_ eid])
(defmethod e-create :default [_ eid])

(defsystem e-destroy [_ eid])
(defmethod e-destroy :default [_ eid])

(defsystem e-tick [_ eid])
(defmethod e-tick :default [_ eid])

(defsystem render-below [_ entity])
(defmethod render-below :default [_ entity])

(defsystem render-default [_ entity])
(defmethod render-default :default [_ entity])

(defsystem render-above [_ entity])
(defmethod render-above :default [_ entity])

(defsystem render-info [_ entity])
(defmethod render-info :default [_ entity])

(defmacro with-err-str
  "Evaluates exprs in a context in which *err* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))

(defn ops-add    [ops other-ops] (merge-with + ops other-ops))
(defn ops-remove [ops other-ops] (merge-with - ops other-ops))

(defsystem op-apply [_ base-value])

(defmethod op-apply :op/inc [[_ value] base-value]
  (+ base-value value))

(defmethod op-apply :op/mult [[_ value] base-value]
  (* base-value (inc (/ value 100))))

(defsystem op-order)

(defmethod op-order :op/inc [_]
  0)

(defmethod op-order :op/mult [_]
  1)

(defn ops-apply [ops value]
  (reduce (fn [value op]
            (op-apply op value))
          value
          (sort-by op-order ops)))

(defsystem state-enter)
(defmethod state-enter :default [_])

(defsystem state-exit)
(defmethod state-exit :default [_])

(defsystem state-cursor)
(defmethod state-cursor :default [_])

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defsystem manual-tick)
(defmethod manual-tick :default [_])

(defsystem clicked-inventory-cell [_ cell])
(defmethod clicked-inventory-cell :default [_ cell])

(defsystem clicked-skillmenu-skill [_ skill])
(defmethod clicked-skillmenu-skill :default [_ skill])

(defsystem draw-gui-view [_])
(defmethod draw-gui-view :default [_])

(defn rectangle->tiles
  [{[x y] :left-bottom :keys [left-bottom width height]}]
  {:pre [left-bottom width height]}
  (let [x       (float x)
        y       (float y)
        width   (float width)
        height  (float height)
        l (int x)
        b (int y)
        r (int (+ x width))
        t (int (+ y height))]
    (set
     (if (or (> width 1) (> height 1))
       (for [x (range l (inc r))
             y (range b (inc t))]
         [x y])
       [[l b] [l t] [r b] [r t]]))))

(defn create-vs [components]
  (reduce (fn [m [k v]]
            (assoc m k (->v [k v])))
          {}
          components))

(let [cnt (atom 0)]
  (defn unique-number! []
    (swap! cnt inc)))

(defn mods-add    [mods other-mods] (merge-with ops-add    mods other-mods))
(defn mods-remove [mods other-mods] (merge-with ops-remove mods other-mods))

(defn async-pprint-spit! [file data]
  (.start
   (Thread.
    (fn []
      (binding [*print-level* nil]
        (->> data
             pprint
             with-out-str
             (spit file)))))))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (e-tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "e-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn tick-entities [entities]
  (run! tick-entity entities))

(defn current-screen []
  (and (bound? #'current-screen-key)
       (current-screen-key screens)))

(defn change-screen
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current-screen)]
    (screen-exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (bind-root #'current-screen-key new-k)
    (screen-enter screen)))

(defn screen-stage []
  (:stage (current-screen)))

(defn play-sound [sound-name]
  (->> sound-name
       (format sound-asset-format)
       (get assets)
       play))

(defn set-dock-icon [path]
  (.setIconImage (java.awt.Taskbar/getTaskbar)
                 (.getImage (java.awt.Toolkit/getDefaultToolkit)
                            (io-resource path))))

(defn schema-of [k]
  (safe-get schemas k))

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
          (keys schemas)))

(defmethod malli-form :s/components-ns [[_ ns-name-k]]
  (malli-form [:s/map-optional (namespaced-ks ns-name-k)]))

(defn property-type [{:keys [property/id]}] ; TODO name clashes
  (keyword "properties" (namespace id)))

(defn property-types []
  (filter #(= "properties" (namespace %))
          (keys schemas)))

(defn schema-of-property [property]
  (schema-of (property-type property)))

(defn get-raw [id]
  (safe-get db-properties id))

(defn all-raw [type]
  (->> (vals db-properties)
       (filter #(= type (property-type %)))))

(defn async-write-to-file! []
  (->> db-properties
       vals
       (sort-by property-type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! properties-file)))

(declare build)

(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file
   :sub-image-bounds})

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

(defn property->image [{:keys [entity/image entity/animation]}]
  (or image
      (first (:frames animation))))

(defprotocol Property
  (validate! [_]))

(require '[malli.generator :as mg])

(defn k->default-value [k]
  (let [schema (schema-of k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema-type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (mg/generate (malli-form schema) {:size 3}))))

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

(defn db-init [{:keys [schema properties]}]
  (bind-root #'schemas (-> schema io-resource slurp edn-read-string))
  (bind-root #'properties-file (io-resource properties))
  (let [properties (-> properties-file slurp edn-read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! validate! properties)
    (bind-root #'db-properties (zipmap (map :property/id properties) properties))))

(defmacro defn-impl [name-sym & fn-body]
  `(bind-root (var ~name-sym) (fn ~name-sym ~@fn-body)))

(defmacro def-impl [name-sym value]
  `(bind-root (var ~name-sym) ~value))

(defmethods :app/default-font
  (app-create [[_ font]]
    (bind-root #'default-font (ttfont font)))
  (app-dispose [_]
    (dispose default-font)))

(defmethods :app/assets
  (app-create [[_ folder]]
    (bind-root #'assets (load-assets folder)))
  (app-dispose [_]
    (dispose assets)))

(defmethods :app/sprite-batch
  (app-create [_]
    (bind-root #'batch (sprite-batch)))
  (app-dispose [_]
    (dispose batch)))

(defmacro app-do [& exprs]
  `(post-runnable (fn [] ~@exprs)))

(defn find-actor-with-id [group id]
  (let [actors (children group)
        ids (keep user-object actors)]
    (assert (or (empty? ids)
                (apply distinct? ids)) ; TODO could check @ add
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (user-object %)) actors))))

(defrecord StageScreen [stage sub-screen]
  Screen
  (screen-enter [_]
    (set-input-processor stage)
    (screen-enter sub-screen))

  (screen-exit [_]
    (set-input-processor nil)
    (screen-exit sub-screen))

  (screen-render [_]
    (act stage)
    (screen-render sub-screen)
    (draw stage))

  (screen-destroy [_]
    (dispose stage)
    (screen-destroy sub-screen)))
