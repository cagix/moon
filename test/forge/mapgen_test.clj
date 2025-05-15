(ns forge.mapgen-test
  (:require [cdq.camera :as camera]
            [cdq.level.modules-core :as modules]
            [cdq.tiled :as tiled]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]))

(def state (atom nil))

(defn- show-whole-map! [camera tiled-map]
  (camera/set-position! camera
                        [(/ (tiled/tm-width  tiled-map) 2)
                         (/ (tiled/tm-height tiled-map) 2)])
  (camera/set-zoom camera
                   (camera/calculate-zoom camera
                                          :left [0 0]
                                          :top [0 (tiled/tm-height tiled-map)]
                                          :right [(tiled/tm-width tiled-map) 0]
                                          :bottom [0 0])))

(defn- current-data [] ; TODO just use vars
  #_(-> (screen/current)
        :sub-screen
        :current-data))

(def ^:private infotext
  "L: grid lines
  M: movement properties
  zoom: minus,equals
  ESCAPE: leave
  direction keys: move")

#_(defn- map-infos ^String [c]
  (let [tile (mapv int (viewport/mouse-position ctx/world-viewport))
        {:keys [tiled-map
                area-level-grid]} @(current-data)]
    (->> [infotext
          (str "Tile " tile)
          (when-not area-level-grid
            (str "Module " (mapv (comp int /)
                                 (viewport/mouse-position ctx/world-viewport)
                                 [modules/width modules/height])))
          (when area-level-grid
            (str "Creature id: " (tiled/property-value tiled-map :creatures tile :id)))
          (when area-level-grid
            (let [level (get area-level-grid tile)]
              (when (number? level)
                (str "Area level:" level))))
          (str "Movement properties " (tiled/movement-property tiled-map tile) "\n"
               (apply vector (tiled/movement-properties tiled-map tile)))]
         (remove nil?)
         (str/join "\n"))))

#_(defn- ->info-window []
  (let [label (ui/label "")
        window (ui/window {:title "Info" :rows [[label]]})]
    (.addActor window (proxy [Actor] []
                        (act [_delta]
                          (do
                           (.setText label (map-infos %))
                           (.pack window)))))
    (.setPosition window 0 (:height ctx/ui-viewport)) window))

(def ^:private camera-movement-speed 1)

; TODO textfield takes control !
; PLUS symbol shift & = symbol on keyboard not registered
#_(defn- camera-controls [camera]
  (let [apply-position (fn [idx f]
                         (camera/set-position! camera
                                               (update (camera/position camera)
                                                       idx
                                                       #(f % camera-movement-speed))))]
    (if (input/key-pressed? gdx/input :left)  (apply-position 0 -))
    (if (input/key-pressed? gdx/input :right) (apply-position 0 +))
    (if (input/key-pressed? gdx/input :up)    (apply-position 1 +))
    (if (input/key-pressed? gdx/input :down)  (apply-position 1 -))))

#_(defn- render-on-map [_context]
  (let [{:keys [tiled-map
                area-level-grid
                start-position
                show-movement-properties
                show-grid-lines]} @(current-data)
        visible-tiles (camera/visible-tiles (:camera ctx/world-viewport))
        [x y] (mapv int (viewport/mouse-position ctx/world-viewport))]
    (graphics/rectangle x y 1 1 :white)
    (when start-position
      (graphics/filled-rectangle (start-position 0) (start-position 1) 1 1 [1 0 1 0.9]))
    (when show-movement-properties
      (doseq [[x y] visible-tiles
              :let [prop (tiled/movement-property tiled-map [x y])]]
        (graphics/filled-circle [(+ x 0.5) (+ y 0.5)] 0.08 :black)
        (graphics/filled-circle [(+ x 0.5) (+ y 0.5)]
                          0.05
                          (case prop
                            "all"   :green
                            "air"   :orange
                            "none"  :red))))
    (when show-grid-lines
      (graphics/grid 0
               0
               (tiled/tm-width  tiled-map)
               (tiled/tm-height tiled-map) 1 1 [1 1 1 0.5]))))

(def ^:private world-id :worlds/uf-caves)

#_(defn- generate-screen-ctx [c properties]
  (let [{:keys [tiled-map start-position]} (generate-level (bb/build ctx/db world-id))
        atom-data (current-data)]
    (tiled/dispose (:tiled-map @atom-data))
    (swap! atom-data assoc
           :tiled-map tiled-map
           ;:area-level-grid area-level-grid
           :start-position start-position)
    (show-whole-map! (:camera ctx/world-viewport) tiled-map)
    (tiled/set-visible (tiled/get-layer tiled-map "creatures") true)))

#_(defn ->generate-map-window [c level-id]
  (ui/window {:title "Properties"
              :cell-defaults {:pad 10}
              :rows [[(ui/label (with-out-str (pprint (bb/build ctx/db level-id))))]
                     [(text-button "Generate" #(try (generate-screen-ctx c (db/build ctx/db level-id))
                                                    (catch Throwable t
                                                      (pretty-pst t)
                                                      (stage/show-error-window! ctx/stage t)
                                                      (println t))))]]
              :pack? true}))

(def ^:private zoom-speed 0.025)

#_(defn adjust-zoom [input camera] ; TODO this now in cdq.context available.
  (when (gdx/key-pressed? input :minus)  (camera/inc-zoom! camera    zoom-speed))
  (when (gdx/key-pressed? input :equals) (camera/inc-zoom! camera (- zoom-speed))))

(defn enter [_]
  #_(show-whole-map! c/camera (:tiled-map @current-data)))

(defn exit [_]
  #_(camera/reset-zoom! c/camera))

(defn render [_]
  #_(graphics/draw-tiled-map (:tiled-map @current-data)
                             (constantly Color/WHITE))
  #_(cdq.graphics/draw-on-world-view @state render-on-map)
  #_(if (gdx/key-just-pressed? :l)
      (swap! current-data update :show-grid-lines not))
  #_(if (gdx/key-just-pressed? :m)
      (swap! current-data update :show-movement-properties not))
  #_(adjust-zoom input c/camera)
  #_(camera-controls input c/camera))

#_(defn dispose [_]
  #_(dispose (:tiled-map @current-data)))

(comment
 (atom {:tiled-map (tiled/load-map modules/file)
        :show-movement-properties false
        :show-grid-lines false})
 )

#_(defn actors [_]
    [(->generate-map-window c world-id)
     (->info-window)])
