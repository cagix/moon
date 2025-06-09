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
    (add-map-nodes! node (children->str-map (Group/.getChildren (stage/root v))) level))

  (when (instance? com.badlogic.gdx.scenes.scene2d.Group v)
    (add-map-nodes! node (children->str-map (Group/.getChildren v)) level))

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
  (doto (VisTree.)
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
  (stage/add-actor! ctx/stage (scroll-pane-window title (generate-table m))))

(defn- show-tree-view! [title m]
  {:pre [(map? m)]}
  (stage/add-actor! ctx/stage (scroll-pane-window title (generate-tree m))))
