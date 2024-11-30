(in-ns 'clojure.core)

; Rationale:
; * I define the same language/aliases/imports in 100 namespaces!
; * I create my own environment
; e.g. db/get everywhere - just 'build' ...
; or even pprint
; or error
; or str-split
; or v-add ...

#_(require '[clj-commons.pretty.repl :as pretty-repl])
(require '[clojure.pprint])
(require '[clojure.string :as str])
(require '[forge.screen :as screen])
(import 'com.badlogic.gdx.math.MathUtils)
(import '(com.badlogic.gdx Gdx)
        '(com.badlogic.gdx.assets AssetManager)
        '(com.badlogic.gdx.audio Sound)
        '(com.badlogic.gdx.graphics Color Pixmap)
        '(com.badlogic.gdx.scenes.scene2d Actor Stage)
        '(com.badlogic.gdx.utils Align Scaling Disposable ScreenUtils))

#_(defn pretty-pst [t]
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
