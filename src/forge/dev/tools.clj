(ns forge.dev.tools
  (:require [anvil.app :refer [post-runnable]]
            [anvil.db :as db]
            [anvil.graphics :refer [gui-viewport-width gui-viewport-height world-mouse-position]]
            [anvil.ui :refer [t-node scroll-pane] :as ui]
            [anvil.world :refer [player-eid]]
            [clojure.gdx.scene2d.group :refer [children]]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.vis-ui :as vis]
            [forge.entity.skills :refer [add-skill]]
            [forge.screens.stage :refer [add-actor]]
            [forge.world :refer [spawn-creature spawn-item]]
            [forge.world.grid :refer [world-grid]]
            [forge.world.mouseover-entity :refer [mouseover-entity]])
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

(comment

 (print-vimrc-names-forge-core-publics 'forge.core "vimrc")

 (print-app-values-tree "app-values-tree.clj")

 (show-tree-view! (mouseover-entity))
 (show-tree-view! (mouseover-grid-cell))
 (show-tree-view! (ns-value-vars #{"forge"}))

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
 (post-runnable
  (spawn-creature {:position [35 73]
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

(defn- learn-skill! [skill-id]
  (post-runnable
   (swap! player-eid add-skill (db/build skill-id))))

(defn- create-item! [item-id]
  (post-runnable
   (spawn-item (:position @player-eid) (db/build item-id))))

(defn- mouseover-grid-cell []
  @(get world-grid (mapv int (world-mouse-position))))

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

; TODO truncate ...
(defn- labelstr [k v]
  (str
   (if (keyword? k)
     (str "[LIGHT_GRAY]:" (when-let [ns (namespace k)] (str ns "/")) " [] [WHITE]" (name k) "[]")
     k)
   ": [GOLD]" (str (->v-str v)) "[]"))

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
    (add-map-nodes! node (children->str-map (children (Stage/.getRoot v))) level))

  (when (instance? com.badlogic.gdx.scenes.scene2d.Group v)
    (add-map-nodes! node (children->str-map (children v)) level))

  (when (and (var? v)
             (instance? com.badlogic.gdx.assets.AssetManager @v))
    (add-map-nodes! node (bean @v) level)))

(comment
 (let [vis-image (first (children (.getRoot (stage-get))))]
   (supers (class vis-image))
   (str vis-image)
   )
 )

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
         (add-nodes node level v))
       (catch Throwable t
         (throw (ex-info "" {:k k :v v} t))
         #_(.add parent-node (t-node (ui/label (str "[RED] "k " - " t))))

         )))))

(defn- scroll-pane-cell [rows]
  (let [table (ui/table {:rows rows
                         :cell-defaults {:pad 1}
                         :pack? true})
        scroll-pane (scroll-pane table)]
    {:actor scroll-pane
     :width (/ gui-viewport-width 2)
     :height
     (- gui-viewport-height 50)
     #_(min (- gui-viewport-height 50) (height table))}))

(defn- show-tree-view! [m]
  {:pre [(map? m)]}
  (let [tree (vis/tree)]
    (add-map-nodes! tree (into (sorted-map) m) 0)
    (add-actor
     (ui/window {:title "Tree View"
                 :close-button? true
                 :close-on-escape? true
                 :center? true
                 :rows [[(scroll-pane-cell [[tree]])]]
                 :pack? true}))
    nil))

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

(defn print-app-values-tree [file]
  (spit file
        (with-out-str
         (pprint
          (for [[ns-name vars] (ns-value-vars #{"forge"})]
            [ns-name (map #(:name (meta %)) vars)])))))

(defn print-vimrc-names-forge-core-publics [ns-sym file]
  (->> (find-ns ns-sym)
       ns-publics
       (remove (fn [[k v]]
                 (or (:macro (meta v))
                     (instance? java.lang.Class @v))))
       (map (fn [s] (str "\"" (name (first s)) "\"")))
       (str/join ", ")
       (spit file)))
