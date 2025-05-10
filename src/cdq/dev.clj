(ns cdq.dev
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.world :as world]
            [clojure.gdx :as gdx]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.scene2d.ui :as ui]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]))

(comment

 (print-app-values-tree "app-values-tree.clj" #{"cdq"})

 ; use post-runnable! to get proper error messages in console

 (show-tree-view! "Mouseover Entity" (:mouseover-eid ctx/world))
 (show-tree-view! "Mouseover Grid Cell" (mouseover-grid-cell))
 (show-tree-view! "Ns vaue Vars" (ns-value-vars #{"cdq"}))

 (spit "ns_value_vars.edn"
       (with-out-str
        (clojure.pprint/pprint
         (ns-value-vars #{"cdq"}))))

 ; Idea:
 ; * Generate the tree as data-structure first
 ; Then pass to the ui
 ; So can test it locally?
 ; or some interface for tree-node & on-clicked


 (do
  (learn-skill! :skills/projectile)
  (learn-skill! :skills/spawn)
  (learn-skill! :skills/meditation)
  (learn-skill! :skills/death-ray)
  (learn-skill! :skills/convert)
  (learn-skill! :skills/blood-curse)
  (learn-skill! :skills/slow)
  (learn-skill! :skills/bow)
  (learn-skill! :skills/double-fireball))

 ; Testing effect/target-entity
 ; stops when target not exists anymore
 ; or out of range

 ; 1. start application
 ; 2. start world
 ; 3. create creature
 (gdx/post-runnable!
  (g/spawn-creature {:position [35 73]
                     :creature-id :creatures/dragon-red
                     :components {:entity/fsm {:fsm :fsms/npc
                                               :initial-state :npc-sleeping}
                                  :entity/faction :evil}}))

 (learn-skill! :skills/bow) ; 1.5 seconds attacktime
 (post-tx! [:e/destroy (ids->eids 168)]) ; TODO how to get id ?
 ; check state is normal ... omg...

 ; start world - small empty test room
 ; 2 creatures - player?
 ; start skill w. applicable needs target (bow)
 ; this command:
 ; check skill has stopped using
 ; => this is basically a test for 'forge.entity.active'

 )


(defn- learn-skill! [_context skill-id]
  (g/add-skill ctx/player-eid (db/build ctx/db skill-id)))

(defn- create-item! [_context item-id]
  (g/spawn-item (:position @ctx/player-eid) (db/build ctx/db item-id)))

(defn- mouseover-grid-cell []
  @(world/cell ctx/world (mapv int (graphics/world-mouse-position ctx/graphics))))

(defn- class->label-str [class]
  (case class
    clojure.lang.LazySeq ""
    (str class)))

(defn- ->v-str [v]
  (cond
   (number? v) v
   (keyword? v) v
   (string? v) (pr-str v)
   (boolean? v) v
   (instance? clojure.lang.Atom v) (str "[LIME] Atom [] [GRAY]" (class @v) "[]")
   (map? v) (str (class v))
   (and (vector? v) (< (count v) 3)) (pr-str v)
   (vector? v) (str "Vector " (count v))
   (var? v) (str "[GOLD]" (:name (meta v)) "[] : " (->v-str @v))
   :else (str "[ORANGE]" (class->label-str v) "[]")))

(defn- k->label-str [k]
  (str "[LIGHT_GRAY]:"
       (when-let [ns (namespace k)] (str ns "/"))
       "[][WHITE]"
       (name k)
       "[]"))

; TODO truncate ...
(defn- labelstr [k v]
  (str (if (keyword? k) (k->label-str k) k)
       ": [GOLD]"
       (str (->v-str v)) "[]"))

(defn- add-elements! [node elements]
  (doseq [element elements]
    (.add node (ui/tree-node (ui/label (str (->v-str element)))))))

#_(let [ns-sym (first (first (into {} (ns-value-vars))))]
  ;(map ->v-str vars)
  )

(declare add-map-nodes!)

(defn- children->str-map [children]
  (zipmap (map str children)
          children))

(defn- add-nodes [node level v]
  (when (map? v)
    (add-map-nodes! node v (inc level)))

  (when (coll? v)
    (add-elements! node v))

  (when (instance? com.badlogic.gdx.scenes.scene2d.Stage v)
    (add-map-nodes! node (children->str-map (group/children (stage/root v))) level))

  (when (instance? com.badlogic.gdx.scenes.scene2d.Group v)
    (add-map-nodes! node (children->str-map (group/children v)) level))

  (when (and (var? v)
             (instance? com.badlogic.gdx.assets.AssetManager @v))
    (add-map-nodes! node (bean @v) level)))

(comment
 (let [vis-image (first (.getChildren (.getRoot (stage-get))))]
   (supers (class vis-image))
   (str vis-image)
   )
 )

; TODO lazy/only on click extend ...
; * handle clicks
; * add nodes then ...

(defn- add-map-nodes! [parent-tree-node m level]
  ;(println "Level: " level " - go deeper? " (< level 4))
  (when (< level 2)
    (doseq [[k v] (into (sorted-map) m)]
      ;(println "add-map-nodes! k " k)
      (try
       (let [node (ui/tree-node (ui/label (labelstr k v)))]
         (.add parent-tree-node node) ; no tree-node-add!: tree cannot be casted to tree-node ... , Tree itself different .add

         #_(when (instance? clojure.lang.Atom v) ; StackOverFLow
           (add-nodes node level @v))

         #_(add-nodes node level v)

         )

       (catch Throwable t
         (throw (ex-info "" {:k k :v v} t))
         #_(.add parent-tree-node (ui/tree-node (ui/label (str "[RED] "k " - " t))))

         )))))

(defn- scroll-pane-cell [rows]
  (let [viewport (:ui-viewport ctx/graphics)
        table (ui/table {:rows rows
                         :cell-defaults {:pad 1}
                         :pack? true})
        scroll-pane (ui/scroll-pane table)]
    {:actor scroll-pane
     :width  800 ; (- (:width viewport) 100) ; (+ 100 (/ (:width viewport) 2))
     :height 800 ; (- (:height viewport) 200) ; (- (:height viewport) 50) #_(min (- (:height viewport) 50) (height table))
     }))

(defn- generate-tree [m]
  (doto (ui/tree)
    (add-map-nodes! (into (sorted-map) m)
                    0)))

(defn- scroll-pane-window [title content]
  (ui/window {:title title
              :close-button? true
              :close-on-escape? true
              :center? true
              :rows [[(scroll-pane-cell [[content]])]]
              :pack? true}))

(defn- generate-table [m]
  (ui/table {:rows (for [[k v] (sort-by key m)]
                     [(ui/label (k->label-str k))
                      (ui/label (str (class v)))])}))

(defn- show-table-view [title m]
  {:pre [(map? m)]}
  (g/add-actor (scroll-pane-window title (generate-table m))))

(defn- show-tree-view! [title m]
  {:pre [(map? m)]}
  (g/add-actor (scroll-pane-window title (generate-tree m))))

(defn get-namespaces [packages]
  (filter #(packages (first (str/split (name (ns-name %)) #"\.")))
          (all-ns)))

(defn get-vars [nmspace condition]
  (for [[sym avar] (ns-interns nmspace)
       :when (condition avar)]
    avar))

(defn- protocol? [value]
  (and (instance? clojure.lang.PersistentArrayMap value)
       (:on value)))

(defn- get-non-fn-vars [nmspace]
  (get-vars nmspace (fn [avar]
                      (let [value @avar]
                        (not (or (fn? value)
                                 (instance? clojure.lang.MultiFn value)
                                 (protocol? value)
                                 ; anonymous class (proxy)
                                 (instance? java.lang.Class value)))))))

(defn ns-value-vars
  "Returns a map of ns-name to value-vars (non-function vars).
  Use to understand the state of your application.

  Example: `(ns-value-vars #{\"forge\"})`"
  [packages]
  (into {} (for [nmspace (get-namespaces packages)
                 :let [value-vars (get-non-fn-vars nmspace)]
                 :when (seq value-vars)]
             [(ns-name nmspace) value-vars])))

(defn print-app-values-tree [file namespaces-set]
  (spit file
        (with-out-str
         (pprint
          (for [[ns-name vars] (ns-value-vars namespaces-set)]
            [ns-name (map #(:name (meta %)) vars)])))))
