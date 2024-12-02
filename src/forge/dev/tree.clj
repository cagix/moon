(ns ^:no-doc forge.dev.tree
  (:require [forge.core :refer :all]
            [forge.dev.app-values-tree :refer [ns-value-vars]]
            [forge.world :refer [world-grid mouseover-entity]]))

(defn- mouseover-grid-cell []
  @(get world-grid (mapv int (world-mouse-position))))

(comment

 (show-tree-view! (mouseover-entity))
 (show-tree-view! (mouseover-grid-cell))
 (show-tree-view! (ns-value-vars #{"forge"}))

 ; Idea:
 ; * Generate the tree as data-structure first
 ; Then pass to the ui
 ; So can test it locally?
 ; or some interface for tree-node & on-clicked

 )

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
    (.add node (t-node (label (str (->v-str element)))))))

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
    (add-map-nodes! node (children->str-map (children (.getRoot v))) level))

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
       (let [node (t-node (label (labelstr k v)))]
         (.add parent-node node) ; no t-node-add!: tree cannot be casted to tree-node ... , Tree itself different .add
         #_(when (instance? clojure.lang.Atom v) ; StackOverFLow
           (add-nodes node level @v))
         (add-nodes node level v))
       (catch Throwable t
         (throw (ex-info "" {:k k :v v} t))
         #_(.add parent-node (t-node (label (str "[RED] "k " - " t))))

         )))))

(defn- scroll-pane-cell [rows]
  (let [table (ui-table {:rows rows
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
  (let [tree (ui-tree)]
    (add-map-nodes! tree (into (sorted-map) m) 0)
    (add-actor
     (ui-window {:title "Tree View"
                 :close-button? true
                 :close-on-escape? true
                 :center? true
                 :rows [[(scroll-pane-cell [[tree]])]]
                 :pack? true}))
    nil))
