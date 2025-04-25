(ns cdq.dev
  (:require [cdq.application :as app]
            cdq.graphics
            [cdq.db :as db]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [cdq.ui.group :refer [children]]
            [cdq.ui.stage :as stage]
            [cdq.ui :refer [t-node scroll-pane] :as ui]
            [cdq.world :as world])
  (:import (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.scenes.scene2d Group Stage)))

(comment

 (print-app-values-tree "app-values-tree.clj" #{"cdq"})

 ; use app/post-runnable! to get proper error messages in console

 (app/post-runnable! (fn [context] (show-tree-view! "Application Context" context)))
 (app/post-runnable! (fn [context] (show-table-view "Application Context" context)))

 (show-tree-view! "Mouseover Entity" @(:cdq.context/mouseover-eid @app/state))
 (show-tree-view! "Mouseover Grid Cell" (mouseover-grid-cell @app/state))
 (show-tree-view! "Ns vaue Vars" (ns-value-vars #{"cdq"}))

 (clojure.pprint/pprint
  (ns-value-vars #{"cdq"})
  )

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
 (app/post-runnable! #(world/spawn-creature %
                                            {:position [35 73]
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


(defn- learn-skill! [{:keys [cdq.context/player-eid
                             cdq/db] :as c}
                     skill-id]
  (world/add-skill c
                   player-eid
                   (db/build db skill-id c)))

(defn- create-item! [{:keys [cdq.context/player-eid
                             cdq/db] :as c}
                     item-id]
  (world/spawn-item c
                    (:position @player-eid)
                    (db/build db item-id c)))

(defn- mouseover-grid-cell [{:keys [cdq.context/grid
                                    cdq.graphics/world-viewport]}]
  @(grid (mapv int (cdq.graphics/world-mouse-position world-viewport))))

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
    (.add node (t-node (ui/label (str (->v-str element)))))))

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

  (when (instance? Stage v)
    (add-map-nodes! node (children->str-map (children (stage/root v))) level))

  (when (instance? Group v)
    (add-map-nodes! node (children->str-map (children v)) level))

  (when (and (var? v)
             (instance? AssetManager @v))
    (add-map-nodes! node (bean @v) level)))

(comment
 (let [vis-image (first (children (.getRoot (stage-get))))]
   (supers (class vis-image))
   (str vis-image)
   )
 )

; TODO lazy/only on click extend ...
; * handle clicks
; * add nodes then ...

(defn- add-map-nodes! [parent-node m level]
  ;(println "Level: " level " - go deeper? " (< level 4))
  (when (< level 2)
    (doseq [[k v] (into (sorted-map) m)]
      ;(println "add-map-nodes! k " k)
      (try
       (let [node (t-node (ui/label (labelstr k v)))]
         (.add parent-node node) ; no t-node-add!: tree cannot be casted to tree-node ... , Tree itself different .add

         #_(when (instance? clojure.lang.Atom v) ; StackOverFLow
           (add-nodes node level @v))

         #_(add-nodes node level v)

         )

       (catch Throwable t
         (throw (ex-info "" {:k k :v v} t))
         #_(.add parent-node (t-node (ui/label (str "[RED] "k " - " t))))

         )))))

(defn- scroll-pane-cell [rows]
  (let [viewport (:cdq.graphics/ui-viewport @app/state)
        table (ui/table {:rows rows
                         :cell-defaults {:pad 1}
                         :pack? true})
        scroll-pane (scroll-pane table)]
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
  (stage/add-actor (:cdq.context/stage @app/state)
                   (scroll-pane-window title
                                       (generate-table m))))

(defn- show-tree-view! [title m]
  {:pre [(map? m)]}
  (stage/add-actor (:cdq.context/stage @app/state)
                   (scroll-pane-window title
                                       (generate-tree m))))

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
