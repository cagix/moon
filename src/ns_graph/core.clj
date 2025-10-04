(ns ns-graph.core
  (:import
    [com.badlogic.gdx ApplicationAdapter Gdx InputAdapter]
    [com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration]
    [com.badlogic.gdx.graphics GL20 OrthographicCamera Color]
    [com.badlogic.gdx.graphics.glutils ShapeRenderer ShapeRenderer$ShapeType]
    [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
    [com.badlogic.gdx.math Vector2 Vector3]))

; TODO -
; * external imports
; * java imports
; * change color, icon

;; ------------------------------------------------------------
;; Graph simulation (force-directed layout)
;; ------------------------------------------------------------

(defn vec-add [[x1 y1] [x2 y2]] [(+ x1 x2) (+ y1 y2)])
(defn vec-sub [[x1 y1] [x2 y2]] [(- x1 x2) (- y1 y2)])
(defn vec-scale [[x y] s] [(* x s) (* y s)])
(defn vec-len [[x y]] (Math/sqrt (+ (* x x) (* y y))))
(defn vec-norm [v]
  (let [len (vec-len v)]
    (if (zero? len) [0 0] (vec-scale v (/ 1 len)))))

(defn random-graph [n]
  {:nodes (mapv (fn [i]
                  {:id (str "ns" i)
                   :pos [(- (rand 800) 400) (- (rand 800) 400)]
                   :vel [0 0]
                   ;:mass 1.0
                   })
                (range n))
   :edges (for [a (range n)
                b (range (inc a) n)
                :when (< (rand) 0.05)]
            [(str "ns" a) (str "ns" b)])})

(comment
 (set! *print-level* 9)
 (clojure.pprint/pprint
  (random-graph 2))

 (let [{:keys [nodes edges]} (random-graph 2)
        id->node (into {} (map (juxt :id identity) nodes))
        id-node2 (zipmap (map :id nodes)
                         nodes)
       ]
   [id->node
    id-node2
    ]
   )
 )

(defn step! [graph dt]
  (let [{:keys [nodes edges]} graph
        id->node (zipmap (map :id nodes) nodes)
        forces (atom (zipmap (map :id nodes)
                             (repeat [0 0])))]  ;; initial zero forces

    ;; --- repulsion
    (doseq [a nodes,
            b nodes
            :when (not= (:id a) (:id b))]
      (let [delta (vec-sub (:pos a) (:pos b))
            dist (max 1.0 (vec-len delta))
            force (/ 3000 (* dist dist))
            dir   (vec-norm delta)
            fvec  (vec-scale dir force)
            old-f (@forces (:id a))]
        (swap! forces assoc (:id a) (vec-add old-f fvec)))) ; accumulate

    ;; --- attraction (edges)
    (doseq [[aid bid] edges]
      (let [pa (:pos (id->node aid))
            pb (:pos (id->node bid))
            delta (vec-sub pa pb)
            dist (vec-len delta)
            force (* 0.01 dist)
            dir   (vec-norm delta)
            fvec  (vec-scale dir force)]
        (swap! forces assoc aid (vec-sub (@forces aid) fvec))
        (swap! forces assoc bid (vec-add (@forces bid) fvec))))

    ;; --- integrate positions
    (let [nodes' (mapv (fn [node]
                         (let [f (@forces (:id node))
                               v (:vel node)
                               p (:pos node)
                               v' (vec-scale (vec-add v (vec-scale f dt)) 0.9)
                               p' (vec-add p v')]
                           (assoc node :vel v' :pos p')))
                       nodes)]
      (assoc graph :nodes nodes'))))


;; ------------------------------------------------------------
;; LibGDX app setup
;; ------------------------------------------------------------

(defn new-state []
  {:graph (random-graph 20)
   :camera (doto (OrthographicCamera. 1280 720)
             (.translate 0 0 0)
             (.update))
   :shapes (ShapeRenderer.)
   :batch (SpriteBatch.)
   :font (BitmapFont.)
   :drag? false
   :last-mouse (Vector3.)})

(def state (atom nil))

(defn update! [dt]
  (swap! state update :graph step! dt))

(defn render! []
  (let [{:keys [^OrthographicCamera camera
                ^ShapeRenderer shapes
                ^SpriteBatch batch
                ^BitmapFont font
                graph]} @state
        nodes (:nodes graph)
        edges (:edges graph)]
    (.glClearColor Gdx/gl 0.1 0.1 0.1 1)
    (.glClear Gdx/gl (bit-or GL20/GL_COLOR_BUFFER_BIT GL20/GL_DEPTH_BUFFER_BIT))
    (.setProjectionMatrix shapes (.combined camera))

    ;; --- draw edges
    (.begin shapes ShapeRenderer$ShapeType/Line)
    (.setColor shapes Color/LIGHT_GRAY)
    (doseq [[a b] edges]
      (let [na (some #(when (= (:id %) a) %) nodes)
            nb (some #(when (= (:id %) b) %) nodes)
            [sx sy] (:pos na)
            [ex ey] (:pos nb)
            ]
        (.line shapes sx sy ex ey)))
    (.end shapes)

    ;; --- draw nodes
    (.begin shapes ShapeRenderer$ShapeType/Filled)
    (.setColor shapes Color/WHITE)
    (doseq [{:keys [pos]} nodes]
      (.circle shapes (pos 0) (pos 1) 6))
    (.end shapes)

    ;; --- draw labels
    (.setProjectionMatrix batch (.combined camera))
    (.begin batch)
    (doseq [{:keys [id pos]} nodes]
      (.draw font batch
             (str id)
             (float (+ (pos 0) 10))
             (float (+ (pos 1) 10))))
    (.end batch)))

(def input-processor
  (proxy [InputAdapter] []
    (touchDown [x y pointer button]
      (swap! state assoc :drag? true)
      (.set ^Vector3 (:last-mouse @state) x y 0)
      false)
    (touchUp [x y pointer button]
      (swap! state assoc :drag? false)
      false)
    (touchDragged [x y pointer]
      (let [{:keys [^OrthographicCamera camera drag? ^Vector3 last-mouse]} @state]
        (when drag?
          (let [dx (- x (.x last-mouse))
                dy (- y (.y last-mouse))]
            (.translate camera (- dx) dy 0)
            (.update camera)
            (.set last-mouse x y 0))))
      false)
    (scrolled [amountX amountY]
      (let [{:keys [^OrthographicCamera camera]} @state]
        (set! (.zoom camera)
              (max 0.1 (min 10.0 (+ (.zoom camera) (* 0.1 amountY)))))
        (.update camera))
      false)))

(def app
  (proxy [ApplicationAdapter] []
    (create []
      (reset! state (new-state))
      (.setInputProcessor Gdx/input input-processor))
    (render []
      (update! 0.016)
      (render!))
    (resize [w h]
      (let [{:keys [^OrthographicCamera camera]} @state]
        (set! (.viewportWidth camera) w)
        (set! (.viewportHeight camera) h)
        (.update camera)))))

;; ------------------------------------------------------------
;; Desktop launcher
;; ------------------------------------------------------------

(defn -main [& _]
  (.set org.lwjgl.system.Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (let [cfg (Lwjgl3ApplicationConfiguration.)]
    (.setTitle cfg "Namespace Graph (2D Force Layout)")
    (.setWindowedMode cfg 1280 720)
    (Lwjgl3Application. app cfg)))
